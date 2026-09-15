package com.lucilla.settlement.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

/**
 * Keeps the Ledger API token fresh so a long-running deployment never degrades on an
 * expired static JWT (the failure that took the hosted desk dark in the Noders era).
 *
 * <ul>
 *   <li>{@code hmac} — mint a new HS256 token locally (own DevNet node, Splice LocalNet).
 *       No network call, nothing that can be revoked out from under the desk.</li>
 *   <li>{@code refresh} — exchange the Keycloak offline refresh token (Noders-style);
 *       Keycloak rotates it, so the newest one is kept.</li>
 *   <li>{@code client-credentials} — OAuth2 client-credentials grant (a participant
 *       started with OIDC auth).</li>
 * </ul>
 *
 * Runs once at startup and then every {@code ledger.refresh-seconds}. Inert for
 * {@code none}/{@code static}. Tokens and secrets are never logged — only
 * {@link TokenInfo#summary()}.
 */
@Component
public class TokenRefresher {

    private static final Logger log = LoggerFactory.getLogger(TokenRefresher.class);

    private final LedgerProperties props;
    private final LedgerConnection connection;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    /** Current refresh token — starts from config, updated as Keycloak rotates it. */
    private volatile String refreshToken;

    public TokenRefresher(LedgerProperties props, LedgerConnection connection) {
        this.props = props;
        this.connection = connection;
        this.refreshToken = props.getRefreshToken();
    }

    @PostConstruct
    public void initialRefresh() {
        LedgerProperties.AuthMode mode = props.effectiveAuthMode();
        if (!props.hasRenewingToken()) {
            log.info("Ledger token renewal: off (auth-mode={}).", mode);
            return;
        }
        if (mode == LedgerProperties.AuthMode.HMAC
                && props.getTokenTtlSeconds() <= props.getRefreshSeconds()) {
            log.warn("ledger.token-ttl-seconds ({}) <= ledger.refresh-seconds ({}): tokens will "
                    + "expire before they are renewed.", props.getTokenTtlSeconds(), props.getRefreshSeconds());
        }
        log.info("Ledger token renewal: auth-mode={}, every {}s.", mode, props.getRefreshSeconds());
        refreshOnce();
    }

    @Scheduled(fixedDelayString = "#{${ledger.refresh-seconds:1800} * 1000}",
               initialDelayString = "#{${ledger.refresh-seconds:1800} * 1000}")
    public void scheduledRefresh() {
        if (props.hasRenewingToken()) {
            refreshOnce();
        }
    }

    void refreshOnce() {
        try {
            String token = switch (props.effectiveAuthMode()) {
                case HMAC -> HmacTokenMinter.mint(props.getHmacSecret(), props.getTokenSubject(),
                        props.getTokenAudience(), props.getTokenScope(), Instant.now(),
                        props.getTokenTtlSeconds());
                case REFRESH -> fromRefreshToken();
                case CLIENT_CREDENTIALS -> fromClientCredentials();
                default -> null;
            };
            if (token != null && !token.isBlank()) {
                connection.updateToken(token);
                log.info("Ledger token renewed: {}", TokenInfo.of(token).summary());
            }
        } catch (Exception e) {
            log.warn("Ledger token renewal failed ({}): {}", props.effectiveAuthMode(), e.getMessage());
        }
    }

    private String fromRefreshToken() throws Exception {
        String rt = refreshToken;
        if (rt == null || rt.isBlank()) {
            return null;
        }
        JsonNode json = post("grant_type=refresh_token"
                + "&client_id=" + enc(props.getClientId())
                + "&refresh_token=" + enc(rt));
        if (json == null) {
            return null;
        }
        String newRt = json.path("refresh_token").asText(null);
        if (newRt != null && !newRt.isBlank()) {
            this.refreshToken = newRt;
        }
        return json.path("access_token").asText(null);
    }

    private String fromClientCredentials() throws Exception {
        StringBuilder body = new StringBuilder("grant_type=client_credentials")
                .append("&client_id=").append(enc(props.getClientId()))
                .append("&client_secret=").append(enc(props.getClientSecret()));
        if (!props.getTokenAudience().isBlank()) {
            body.append("&audience=").append(enc(props.getTokenAudience()));
        }
        if (!props.getTokenScope().isBlank()) {
            body.append("&scope=").append(enc(props.getTokenScope()));
        }
        JsonNode json = post(body.toString());
        return json == null ? null : json.path("access_token").asText(null);
    }

    private JsonNode post(String form) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(props.getTokenEndpoint()))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() / 100 != 2) {
            log.warn("Token endpoint answered {}: {}", resp.statusCode(), truncate(redactTokens(resp.body())));
            return null;
        }
        return mapper.readTree(resp.body());
    }

    private static String enc(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    private static String truncate(String s) {
        return s == null ? "" : (s.length() > 200 ? s.substring(0, 200) + "…" : s);
    }

    /** Blank any JSON field whose NAME contains "token" before it can reach a log. */
    static String redactTokens(String body) {
        if (body == null) {
            return "";
        }
        return body.replaceAll("(?i)(\"[a-z0-9_]*token[a-z0-9_]*\"\\s*:\\s*\")[^\"]*(\")", "$1<redacted>$2");
    }
}
