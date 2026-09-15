package com.lucilla.settlement.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Locale;

/**
 * Ledger API connection settings, bound from {@code ledger.*} in
 * {@code application.yml} (overridable by env vars, e.g. {@code LEDGER_HOST},
 * {@code LEDGER_PORT}, {@code LEDGER_TLS}, {@code LEDGER_JWT}).
 *
 * <p>The SAME jar targets very different ledgers with only config changes:
 * <ul>
 *   <li><b>Local sandbox</b> — {@code host=localhost port=6865 tls=false} and no
 *       token. Plaintext gRPC, no auth. This is the default.</li>
 *   <li><b>Shared hosted participant (Noders-style)</b> — {@code tls=true}, a Keycloak
 *       access token plus an offline refresh token ({@code auth-mode=refresh}), and a
 *       fixed party roster in {@code parties}.</li>
 *   <li><b>Our own validator node / Splice LocalNet</b> — plaintext on a private
 *       network, a self-minted HS256 token ({@code auth-mode=hmac}) the desk re-mints
 *       on a schedule, and the roster of {@code *-crossdesk} parties.</li>
 *   <li><b>OIDC-protected participant</b> — {@code auth-mode=client-credentials}
 *       against the IdP's token endpoint.</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "ledger")
public class LedgerProperties {

    /** How the desk obtains the bearer token it presents to the participant. */
    public enum AuthMode {
        /** No token (local sandbox). */
        NONE,
        /** A fixed token from {@code jwt}. Expires; fine for a short session only. */
        STATIC,
        /** Keycloak/OIDC offline refresh token exchanged on a schedule (Noders-style). */
        REFRESH,
        /** OAuth2 client-credentials grant on a schedule (participant started with OIDC auth). */
        CLIENT_CREDENTIALS,
        /** Self-minted HS256 token (participant auth-service unsafe-jwt-hmac-256). DevNet/LocalNet only. */
        HMAC
    }

    /** Ledger API host (Canton participant / sandbox). */
    private String host = "localhost";

    /** Ledger API gRPC port. Sandbox default is 6865. */
    private int port = 6865;

    /** Use TLS for the gRPC channel. false = plaintext (local sandbox / private network only). */
    private boolean tls = false;

    /**
     * Optional {@code :authority} override for the gRPC channel. Needed only when the
     * Ledger API sits behind a name-based reverse proxy (the Splice compose nginx routes
     * gRPC by {@code grpc-ledger-api.localhost}). Blank = derive from host:port.
     */
    private String authority = "";

    /**
     * Optional JWT bearer token. Empty/blank = no auth (local sandbox). On a real
     * participant this token authorises the application's actAs/readAs parties.
     */
    private String jwt = "";

    /** Ledger API application id (= user id in Ledger API v2) stamped on every submission. */
    private String applicationId = "canton-dvp-desk";

    /** Seconds to wait for the initial channel connect + ledger-id handshake. */
    private long connectTimeoutSeconds = 10;

    /**
     * Party roster — comma-separated {@code Label=partyId} pairs. When set, it is the
     * source of truth for the party picker and label resolution, and the party-management
     * admin service is never called (a non-admin ledger user cannot call it, and on a
     * validator node it would also return the operator, DSO and wallet parties). Blank =
     * list parties live from the ledger (local sandbox).
     */
    private String parties = "";

    /**
     * {@code auto} (default) picks the mode from what is configured: an HMAC secret →
     * hmac; a refresh token → refresh; a client secret → client-credentials; a JWT →
     * static; nothing → none.
     */
    private String authMode = "auto";

    // --- refresh / client-credentials (OIDC) --------------------------------
    /** OIDC token endpoint (Keycloak / Auth0 / ...). */
    private String tokenEndpoint = "";
    /** OIDC client id. */
    private String clientId = "";
    /** OIDC client secret (client-credentials only). Inject from a secret store. */
    private String clientSecret = "";
    /** OIDC offline refresh token (refresh mode). Inject from a secret store. */
    private String refreshToken = "";
    /** How often the token is renewed, in seconds (must be below the token lifetime). */
    private long refreshSeconds = 1800;

    // --- token claims (hmac + client-credentials) ---------------------------
    /** {@code aud} claim. Must equal the participant's {@code target-audience}. */
    private String tokenAudience = "";
    /** Optional {@code scope} (client-credentials; hmac adds it as a claim when set). */
    private String tokenScope = "";
    /** Ledger API user id placed in {@code sub} (hmac). Also forced as the application id. */
    private String tokenSubject = "";
    /** Shared HS256 secret (hmac). Inject from Secret Manager — never a file in git. */
    private String hmacSecret = "";
    /** Lifetime of a self-minted token, in seconds. */
    private long tokenTtlSeconds = 3600;

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public boolean isTls() { return tls; }
    public void setTls(boolean tls) { this.tls = tls; }

    public String getAuthority() { return authority; }
    public void setAuthority(String authority) { this.authority = authority; }

    public boolean hasAuthority() { return notBlank(authority); }

    public String getJwt() { return jwt; }
    public void setJwt(String jwt) { this.jwt = jwt; }

    /**
     * The application id stamped on submissions. In Ledger API v2 this is the user id and
     * a participant rejects a command whose user id differs from the token's {@code sub}.
     * In hmac mode the desk mints that {@code sub} itself, so it is enforced here rather
     * than trusted to two env vars being set in lockstep.
     */
    public String getApplicationId() {
        if (effectiveAuthMode() == AuthMode.HMAC && notBlank(tokenSubject)) {
            return tokenSubject;
        }
        return applicationId;
    }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }

    public long getConnectTimeoutSeconds() { return connectTimeoutSeconds; }
    public void setConnectTimeoutSeconds(long connectTimeoutSeconds) {
        this.connectTimeoutSeconds = connectTimeoutSeconds;
    }

    public String getParties() { return parties; }
    public void setParties(String parties) { this.parties = parties; }

    public String getAuthMode() { return authMode; }
    public void setAuthMode(String authMode) { this.authMode = authMode; }

    public String getTokenEndpoint() { return tokenEndpoint; }
    public void setTokenEndpoint(String tokenEndpoint) { this.tokenEndpoint = tokenEndpoint; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public long getRefreshSeconds() { return refreshSeconds; }
    public void setRefreshSeconds(long refreshSeconds) { this.refreshSeconds = refreshSeconds; }

    public String getTokenAudience() { return tokenAudience; }
    public void setTokenAudience(String tokenAudience) { this.tokenAudience = tokenAudience; }

    public String getTokenScope() { return tokenScope; }
    public void setTokenScope(String tokenScope) { this.tokenScope = tokenScope; }

    public String getTokenSubject() { return tokenSubject; }
    public void setTokenSubject(String tokenSubject) { this.tokenSubject = tokenSubject; }

    public String getHmacSecret() { return hmacSecret; }
    public void setHmacSecret(String hmacSecret) { this.hmacSecret = hmacSecret; }

    public long getTokenTtlSeconds() { return tokenTtlSeconds; }
    public void setTokenTtlSeconds(long tokenTtlSeconds) { this.tokenTtlSeconds = tokenTtlSeconds; }

    /** True when a non-blank static JWT is configured. */
    public boolean hasJwt() {
        return notBlank(jwt);
    }

    /** True when a party roster is configured (it then replaces the live party listing). */
    public boolean hasPartyRoster() {
        return notBlank(parties);
    }

    /** The mode actually in force, resolving {@code auto}. */
    public AuthMode effectiveAuthMode() {
        String m = authMode == null ? "" : authMode.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        switch (m) {
            case "none": return AuthMode.NONE;
            case "static": return AuthMode.STATIC;
            case "refresh": return AuthMode.REFRESH;
            case "client-credentials": return AuthMode.CLIENT_CREDENTIALS;
            case "hmac": return AuthMode.HMAC;
            case "", "auto": break;
            default:
                throw new IllegalStateException("ledger.auth-mode must be one of auto, none, static, "
                        + "refresh, client-credentials, hmac — got '" + authMode + "'");
        }
        if (notBlank(hmacSecret)) {
            return AuthMode.HMAC;
        }
        if (notBlank(refreshToken) && notBlank(tokenEndpoint)) {
            return AuthMode.REFRESH;
        }
        if (notBlank(clientSecret) && notBlank(tokenEndpoint)) {
            return AuthMode.CLIENT_CREDENTIALS;
        }
        return hasJwt() ? AuthMode.STATIC : AuthMode.NONE;
    }

    /** True when the desk renews the token itself (so it never goes stale). */
    public boolean hasRenewingToken() {
        AuthMode m = effectiveAuthMode();
        return m == AuthMode.REFRESH || m == AuthMode.CLIENT_CREDENTIALS || m == AuthMode.HMAC;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
