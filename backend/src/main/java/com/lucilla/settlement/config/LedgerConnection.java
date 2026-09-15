package com.lucilla.settlement.config;

import com.daml.ledger.api.v2.admin.PartyManagementServiceGrpc;
import com.daml.ledger.api.v2.admin.PartyManagementServiceGrpc.PartyManagementServiceBlockingStub;
import com.daml.ledger.rxjava.DamlLedgerClient;
import com.lucilla.settlement.ledger.LedgerErrors;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContext;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLException;

/**
 * Owns the single {@link DamlLedgerClient} and its lifecycle.
 *
 * <p>The connection is established <b>lazily</b> on first use (not at application
 * startup) so the service can boot even when the ledger is not yet reachable —
 * exactly what you want in Kubernetes, where the backend pod may start before the
 * participant, and when running {@code bootRun} without a sandbox up. The first
 * request that touches the ledger triggers the connect; a failed connect surfaces
 * as a clean error to that request and is retried on the next one.
 *
 * <p><b>Token handling.</b> The bearer token is attached by ONE channel interceptor that
 * reads {@link #activeToken()} on every call. {@link TokenRefresher} swaps the token with
 * {@link #updateToken(String)} and the very next call carries it — no reconnect, no
 * dropped streams, and no per-submission {@code withAccessToken} (which would add a second
 * Authorization header). A static {@code LEDGER_JWT} flows through the same path.
 *
 * <p>Plaintext vs TLS, the optional {@code :authority} override, and the token source are
 * chosen entirely from {@link LedgerProperties}.
 */
@Component
public class LedgerConnection {

    private static final Logger log = LoggerFactory.getLogger(LedgerConnection.class);

    /** 64 MiB — gRPC max inbound message size (default 10 MiB is too small for big ACS snapshots). */
    private static final int MAX_INBOUND_BYTES = 64 * 1024 * 1024;

    private static final Metadata.Key<String> AUTHORIZATION =
            Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);

    private final LedgerProperties props;
    private volatile DamlLedgerClient client;
    private volatile ManagedChannel adminChannel;

    /** The renewed token, when a {@link TokenRefresher} mode is active. Null = use props.jwt. */
    private volatile String currentToken;

    public LedgerConnection(LedgerProperties props) {
        this.props = props;
    }

    /**
     * Returns a connected client, connecting on first call. Idempotent and
     * thread-safe (double-checked locking).
     */
    public DamlLedgerClient get() {
        DamlLedgerClient c = client;
        if (c != null) {
            return c;
        }
        synchronized (this) {
            if (client == null) {
                client = connect();
            }
            return client;
        }
    }

    public LedgerProperties properties() {
        return props;
    }

    /**
     * The access token presented to the ledger: the renewed one when a refresher is active,
     * else the static JWT, else blank (local sandbox).
     *
     * <p><b>Callers must never log or serialise the return value</b> — {@link TokenInfo#of(String)}
     * exists precisely so that expiry can be reported without the token ever being rendered.
     */
    public String activeToken() {
        String t = currentToken;
        return (t != null && !t.isBlank()) ? t : props.getJwt();
    }

    /** True when a non-blank access token is currently held. */
    public boolean hasActiveToken() {
        String t = activeToken();
        return t != null && !t.isBlank();
    }

    /** Swap in a freshly minted/refreshed token. Takes effect on the next gRPC call. */
    public void updateToken(String newToken) {
        if (newToken != null && !newToken.isBlank()) {
            this.currentToken = newToken;
        }
    }

    /**
     * A blocking stub for the ledger's <b>party-management</b> admin service.
     *
     * <p>The high-level rxjava {@link DamlLedgerClient} does not expose the admin
     * services, so we open our OWN gRPC channel to the same Ledger API host:port and
     * drive the generated {@link PartyManagementServiceGrpc} stub directly. Only used
     * when no {@code LEDGER_PARTIES} roster is configured (local sandbox). Lazily built.
     */
    public PartyManagementServiceBlockingStub partyManagement() {
        return PartyManagementServiceGrpc.newBlockingStub(adminChannel());
    }

    private ManagedChannel adminChannel() {
        ManagedChannel c = adminChannel;
        if (c != null) {
            return c;
        }
        synchronized (this) {
            if (adminChannel == null) {
                log.info("Opening admin gRPC channel to {}:{} (tls={})",
                        props.getHost(), props.getPort(), props.isTls());
                NettyChannelBuilder b = channelBuilder();
                if (props.isTls()) {
                    b = b.sslContext(clientTls());
                } else {
                    b = b.usePlaintext();
                }
                adminChannel = b.build();
            }
            return adminChannel;
        }
    }

    /** Host/port, message size, optional authority override, and the bearer interceptor. */
    private NettyChannelBuilder channelBuilder() {
        NettyChannelBuilder b = NettyChannelBuilder
                .forAddress(props.getHost(), props.getPort())
                .maxInboundMessageSize(MAX_INBOUND_BYTES)
                .intercept(bearerInterceptor());
        if (props.hasAuthority()) {
            b = b.overrideAuthority(props.getAuthority().trim());
        }
        return b;
    }

    /** Attaches {@code Authorization: Bearer <activeToken()>} to every call when a token is held. */
    private ClientInterceptor bearerInterceptor() {
        return new ClientInterceptor() {
            @Override
            public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
                    MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
                return new ForwardingClientCall.SimpleForwardingClientCall<>(
                        next.newCall(method, callOptions)) {
                    @Override
                    public void start(Listener<RespT> responseListener, Metadata headers) {
                        String token = activeToken();
                        if (token != null && !token.isBlank()) {
                            headers.put(AUTHORIZATION, "Bearer " + token);
                        }
                        super.start(responseListener, headers);
                    }
                };
            }
        };
    }

    private DamlLedgerClient connect() {
        // Token PRESENCE and EXPIRY only — TokenInfo cannot render the token itself.
        String tokenSummary = TokenInfo.of(activeToken()).summary();
        log.info("Connecting to Ledger API at {}:{} (tls={}, authority={}, auth-mode={}, token: {})",
                props.getHost(), props.getPort(), props.isTls(),
                props.hasAuthority() ? props.getAuthority() : "(default)",
                props.effectiveAuthMode(), tokenSummary);

        // The active-contract-set snapshot for a busy party can exceed the 10 MiB gRPC
        // default (observed a 47 MiB snapshot on a freshly-seeded ledger).
        DamlLedgerClient.Builder builder = DamlLedgerClient.newBuilder(channelBuilder())
                .withMaxInboundMessageSize(MAX_INBOUND_BYTES);
        if (props.isTls()) {
            builder = builder.withSslContext(clientTls());
        }

        DamlLedgerClient c = builder.build();
        try {
            c.connect();
        } catch (RuntimeException e) {
            // The lazy connect is the FIRST thing that fails when the endpoint, the TLS
            // setting or the token is wrong, and by default it fails inside whichever
            // request happened to touch the ledger first. Say what was attempted.
            LedgerErrors.Failure f = LedgerErrors.of(e);
            log.error("LEDGER CONNECT FAILED endpoint={}:{} tls={} token: {} status={} "
                            + "description=\"{}\" | {}",
                    props.getHost(), props.getPort(), props.isTls(), tokenSummary,
                    f.codeLabel(), LedgerErrors.truncate(f.description()), f.hint());
            throw e;
        }
        // Ledger API v2 dropped the ledger-id handshake (and getLedgerId() with it);
        // a successful connect() is the confirmation.
        log.info("Connected to Ledger API v2 at {}:{}.", props.getHost(), props.getPort());
        return c;
    }

    private SslContext clientTls() {
        try {
            // System trust store (a real Canton participant presents a CA-signed
            // cert). For a private CA, mount it and extend this with .trustManager.
            return GrpcSslContexts.forClient().build();
        } catch (SSLException e) {
            throw new IllegalStateException("Failed to build client TLS context", e);
        }
    }

    @PreDestroy
    public synchronized void close() {
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                log.warn("Error closing ledger client", e);
            } finally {
                client = null;
            }
        }
        if (adminChannel != null) {
            try {
                adminChannel.shutdownNow();
            } catch (Exception e) {
                log.warn("Error closing admin channel", e);
            } finally {
                adminChannel = null;
            }
        }
    }
}
