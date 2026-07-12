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

    private final LedgerProperties props;
    private volatile DamlLedgerClient client;

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

    private DamlLedgerClient connect() {
        log.info("Connecting to Ledger API at {}:{} (tls={}, jwt={})",
                props.getHost(), props.getPort(), props.isTls(), props.hasJwt() ? "yes" : "no");

        DamlLedgerClient.Builder builder =
                DamlLedgerClient.newBuilder(props.getHost(), props.getPort());

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
    }
}
