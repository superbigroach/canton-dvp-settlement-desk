package com.lucilla.settlement.config;

import com.daml.ledger.api.v1.admin.PartyManagementServiceGrpc;
import com.daml.ledger.api.v1.admin.PartyManagementServiceGrpc.PartyManagementServiceBlockingStub;
import com.daml.ledger.rxjava.DamlLedgerClient;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ForwardingClientCall;
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
 * <p>Plaintext (local sandbox) vs. TLS + JWT (real Canton participant) is chosen
 * entirely from {@link LedgerProperties}.
 */
@Component
public class LedgerConnection {

    private static final Logger log = LoggerFactory.getLogger(LedgerConnection.class);

    /** 64 MiB — gRPC max inbound message size (default 10 MiB is too small for big ACS snapshots). */
    private static final int MAX_INBOUND_BYTES = 64 * 1024 * 1024;

    private final LedgerProperties props;
    private volatile DamlLedgerClient client;
    private volatile ManagedChannel adminChannel;

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
     * A blocking stub for the ledger's <b>party-management</b> admin service.
     *
     * <p>The high-level rxjava {@link DamlLedgerClient} in bindings 2.9.4 does not
     * expose the admin services, so we open our OWN gRPC channel to the same
     * Ledger API host:port and drive the generated {@link PartyManagementServiceGrpc}
     * stub directly. Same plaintext-vs-TLS + optional JWT bearer story as the main
     * client, driven entirely from {@link LedgerProperties}. Lazily built.
     */
    public PartyManagementServiceBlockingStub partyManagement() {
        return PartyManagementServiceGrpc.newBlockingStub(authed(adminChannel()));
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
                NettyChannelBuilder b = NettyChannelBuilder
                        .forAddress(props.getHost(), props.getPort())
                        .maxInboundMessageSize(MAX_INBOUND_BYTES);
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

    /** Attaches an {@code Authorization: Bearer <jwt>} header when a JWT is set. */
    private Channel authed(ManagedChannel base) {
        if (!props.hasJwt()) {
            return base;
        }
        final String token = props.getJwt();
        ClientInterceptor bearer = new ClientInterceptor() {
            @Override
            public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
                    MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
                return new ForwardingClientCall.SimpleForwardingClientCall<>(
                        next.newCall(method, callOptions)) {
                    @Override
                    public void start(Listener<RespT> responseListener, Metadata headers) {
                        Metadata.Key<String> auth =
                                Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);
                        headers.put(auth, "Bearer " + token);
                        super.start(responseListener, headers);
                    }
                };
            }
        };
        return ClientInterceptors.intercept(base, bearer);
    }

    private DamlLedgerClient connect() {
        log.info("Connecting to Ledger API at {}:{} (tls={}, jwt={})",
                props.getHost(), props.getPort(), props.isTls(), props.hasJwt() ? "yes" : "no");

        DamlLedgerClient.Builder builder =
                DamlLedgerClient.newBuilder(props.getHost(), props.getPort())
                        // The active-contract-set snapshot for a busy party can exceed
                        // the 10 MiB gRPC default (observed a 47 MiB snapshot on a
                        // freshly-seeded ledger). Give the client generous headroom.
                        .withMaxInboundMessageSize(MAX_INBOUND_BYTES);

        if (props.isTls()) {
            builder = builder.withSslContext(clientTls());
        }
        if (props.hasJwt()) {
            builder = builder.withAccessToken(props.getJwt());
        }

        DamlLedgerClient c = builder.build();
        c.connect();
        log.info("Connected. ledgerId={}", c.getLedgerId());
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
