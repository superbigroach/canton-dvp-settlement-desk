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

/**
 * DEVNET HOSTED DEMO ONLY — keeps the Keycloak access token fresh.
 *
 * <p>On a long-running hosted deployment (Cloud Run) the initial access token
 * expires (~3h). When {@code ledger.refresh-token} + {@code ledger.token-endpoint}
 * + {@code ledger.client-id} are configured, this component exchanges the OIDC
 * <b>offline refresh token</b> for a fresh access token on a schedule (and once at
 * startup) and hands it to {@link LedgerConnection#updateToken(String)} so the
 * desk stays connected to the participant without any manual re-login.
 *
 * <p>Keycloak rotates refresh tokens, so we keep using the newest one returned.
 * If nothing is configured (local / static-token use) this component is inert.
 */
@Component
public class TokenRefresher {

    private static final Logger log = LoggerFactory.getLogger(TokenRefresher.class);

    private final LedgerProperties props;
    private final LedgerConnection connection;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    /** The current refresh token — starts from config, updated as Keycloak rotates it. */
    private volatile String refreshToken;

    public TokenRefresher(LedgerProperties props, LedgerConnection connection) {
        this.props = props;
        this.connection = connection;
        this.refreshToken = props.getRefreshToken();
    }

    /** Mint a first access token before the desk serves any request. Non-fatal on failure. */
    @PostConstruct
    public void initialRefresh() {
        if (!props.hasRefresh()) {
            log.info("Token auto-refresh disabled (no refresh token configured).");
            return;
        }
        log.info("Token auto-refresh enabled; performing initial refresh (every {}s thereafter).",
                props.getRefreshSeconds());
        refreshOnce();
    }

    /** Periodic refresh. fixedDelayString is in ms; guarded so it no-ops when unconfigured. */
    @Scheduled(fixedDelayString = "#{${ledger.refresh-seconds:1800} * 1000}",
               initialDelayString = "#{${ledger.refresh-seconds:1800} * 1000}")
    public void scheduledRefresh() {
        if (props.hasRefresh()) {
            refreshOnce();
        }
    }

    private void refreshOnce() {
        String rt = refreshToken;
        if (rt == null || rt.isBlank()) {
            return;
        }
        try {
            String body = "grant_type=refresh_token"
                    + "&client_id=" + enc(props.getClientId())
                    + "&refresh_token=" + enc(rt);
            HttpRequest req = HttpRequest.newBuilder(URI.create(props.getTokenEndpoint()))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                log.warn("Token refresh failed ({}): {}", resp.statusCode(),
                        truncate(resp.body()));
                return;
            }
            JsonNode json = mapper.readTree(resp.body());
            String access = json.path("access_token").asText(null);
            String newRt = json.path("refresh_token").asText(null);
            if (access == null || access.isBlank()) {
                log.warn("Token refresh returned no access_token.");
                return;
            }
            connection.updateToken(access);
            if (newRt != null && !newRt.isBlank()) {
                this.refreshToken = newRt;   // Keycloak rotated it — keep the newest.
            }
        } catch (Exception e) {
            log.warn("Token refresh error: {}", e.getMessage());
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    private static String truncate(String s) {
        if (s == null) {
            return "";
        }
        return s.length() > 200 ? s.substring(0, 200) + "…" : s;
    }
}
