package com.lucilla.settlement.config;

import com.daml.ledger.rxjava.DamlLedgerClient;
import io.grpc.netty.GrpcSslContexts;
import io.netty.handler.ssl.SslContext;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLException;

/**
 * Owns the single {@link DamlLedgerClient} (Ledger API v2, bindings 3.x) and its
 * lifecycle. DEVNET build.
 *
 * <p>The connection is established <b>lazily</b> on first use so the service can boot
 * even when the ledger is not yet reachable. Plaintext vs. TLS + JWT is chosen from
 * {@link LedgerProperties} (devnet = TLS + JWT).
 *
 * <p><b>Difference from the local (2.9.4) build:</b> the 3.x rxjava bindings drop the
 * v1 admin (party-management) gRPC stubs, and on the shared devnet node the parties
 * are pre-allocated and fixed anyway — so this build has NO admin channel; the party
 * roster is supplied via configuration ({@code ledger.parties}) and read back in
 * {@code LedgerService.listParties()}. JWT auth is attached by the client builder's
 * {@code withAccessToken}, so no manual gRPC interceptor is needed either.
 */
@Component
public class LedgerConnection {

    private static final Logger log = LoggerFactory.getLogger(LedgerConnection.class);

    /** 64 MiB — gRPC max inbound message size (default 10 MiB is too small for big ACS snapshots). */
    private static final int MAX_INBOUND_BYTES = 64 * 1024 * 1024;

    private final LedgerProperties props;
    private volatile DamlLedgerClient client;

    /**
     * The access token actually used on the wire. Starts as the configured JWT and
     * is swapped by {@link #updateToken(String)} when TokenRefresher mints a fresh
     * one (hosted demo). Falls back to {@code props.getJwt()} when null.
     */
    private volatile String currentToken;

    public LedgerConnection(LedgerProperties props) {
        this.props = props;
    }

    /** The token to present to the ledger — the refreshed one if we have it, else the static JWT. */
    public String activeToken() {
        String t = currentToken;
        return (t != null && !t.isBlank()) ? t : props.getJwt();
    }

    /** True when we currently hold a non-blank access token (static or refreshed). */
    public boolean hasActiveToken() {
        String t = activeToken();
        return t != null && !t.isBlank();
    }

    /**
     * Swap in a freshly-minted access token and force a reconnect so subsequent
     * calls authenticate with it. Called by TokenRefresher on the hosted demo.
     */
    public synchronized void updateToken(String newToken) {
        if (newToken == null || newToken.isBlank()) {
            return;
        }
        this.currentToken = newToken;
        // Drop the current client; the next get() lazily reconnects with the new token.
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                log.warn("Error closing ledger client during token refresh", e);
            } finally {
                client = null;
            }
        }
        log.info("Ledger access token refreshed; client will reconnect on next use.");
    }

    /** Returns a connected client, connecting on first call. Thread-safe (double-checked). */
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

    private DamlLedgerClient connect() {
        String token = activeToken();
        boolean hasToken = token != null && !token.isBlank();
        log.info("Connecting to Ledger API at {}:{} (tls={}, jwt={})",
                props.getHost(), props.getPort(), props.isTls(), hasToken ? "yes" : "no");

        DamlLedgerClient.Builder builder =
                DamlLedgerClient.newBuilder(props.getHost(), props.getPort())
                        .withMaxInboundMessageSize(MAX_INBOUND_BYTES);

        if (props.isTls()) {
            builder = builder.withSslContext(clientTls());
        }
        if (hasToken) {
            builder = builder.withAccessToken(token);
        }

        DamlLedgerClient c = builder.build();
        c.connect();
        // v2 (bindings 3.x) dropped the ledger-id handshake/getLedgerId(); a
        // successful connect() is the confirmation.
        log.info("Connected to Ledger API v2 at {}:{}.", props.getHost(), props.getPort());
        return c;
    }

    private SslContext clientTls() {
        try {
            // System trust store — the devnet participant presents a CA-signed cert.
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
    }
}
