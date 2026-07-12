package com.lucilla.settlement.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Ledger API connection settings, bound from {@code ledger.*} in
 * {@code application.yml} (overridable by env vars, e.g. {@code LEDGER_HOST},
 * {@code LEDGER_PORT}, {@code LEDGER_TLS}, {@code LEDGER_JWT}).
 *
 * <p>The SAME jar targets two very different ledgers with only config changes:
 * <ul>
 *   <li><b>Local sandbox</b> — {@code host=localhost port=6865 tls=false} and no
 *       JWT. Plaintext gRPC, no auth. This is the default.</li>
 *   <li><b>Real Canton participant</b> — {@code tls=true} and a {@code jwt}
 *       bearer token scoping {@code actAs}/{@code readAs}. TLS + JWT.</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "ledger")
public class LedgerProperties {

    /** Ledger API host (Canton participant / sandbox). */
    private String host = "localhost";

    /** Ledger API gRPC port. Sandbox default is 6865. */
    private int port = 6865;

    /** Use TLS for the gRPC channel. false = plaintext (local sandbox only). */
    private boolean tls = false;

    /**
     * Optional JWT bearer token. Empty/blank = no auth (local sandbox). On a real
     * participant this token authorises the application's actAs/readAs parties.
     */
    private String jwt = "";

    /** Ledger API application id stamped on every command submission. */
    private String applicationId = "canton-dvp-desk";

    /** Seconds to wait for the initial channel connect + ledger-id handshake. */
    private long connectTimeoutSeconds = 10;

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public boolean isTls() { return tls; }
    public void setTls(boolean tls) { this.tls = tls; }

    public String getJwt() { return jwt; }
    public void setJwt(String jwt) { this.jwt = jwt; }

    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }

    public long getConnectTimeoutSeconds() { return connectTimeoutSeconds; }
    public void setConnectTimeoutSeconds(long connectTimeoutSeconds) {
        this.connectTimeoutSeconds = connectTimeoutSeconds;
    }

    /** True when a non-blank JWT is configured (real participant with auth). */
    public boolean hasJwt() {
        return jwt != null && !jwt.isBlank();
    }
}
