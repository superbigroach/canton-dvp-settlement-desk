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

    /**
     * DEVNET party roster — comma-separated {@code label=partyId} pairs. The 3.x
     * bindings drop the party-management admin service, and on the shared devnet node
     * the parties are pre-allocated and fixed, so we configure them here instead of
     * querying the ledger. Read back by {@code LedgerService.listParties()}.
     * e.g. {@code "Issuer=issuer-crossdesk::1220...,Bank=bank-crossdesk::1220...,..."}
     */
    private String parties = "";

    public String getParties() { return parties; }
    public void setParties(String parties) { this.parties = parties; }

    // --- Hosted-demo token refresh (Keycloak) -------------------------------
    // On a long-running hosted deployment the initial JWT expires (~3h). If a
    // refreshToken + tokenEndpoint + clientId are supplied, TokenRefresher swaps
    // in a fresh access token on a schedule so the demo stays connected. Leave
    // refreshToken blank to disable (local/static-token use).

    /** OIDC token endpoint (Keycloak) used to refresh the access token. */
    private String tokenEndpoint = "";
    /** OIDC public client id the token was issued to. */
    private String clientId = "";
    /** OIDC offline refresh token (grant_type=refresh_token). Blank = no refresh. */
    private String refreshToken = "";
    /** How often to refresh, in seconds (must be < the access-token lifetime). */
    private long refreshSeconds = 1800;

    public String getTokenEndpoint() { return tokenEndpoint; }
    public void setTokenEndpoint(String tokenEndpoint) { this.tokenEndpoint = tokenEndpoint; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public long getRefreshSeconds() { return refreshSeconds; }
    public void setRefreshSeconds(long refreshSeconds) { this.refreshSeconds = refreshSeconds; }

    /** True when auto-refresh is configured (hosted demo). */
    public boolean hasRefresh() {
        return refreshToken != null && !refreshToken.isBlank()
                && tokenEndpoint != null && !tokenEndpoint.isBlank();
    }

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
