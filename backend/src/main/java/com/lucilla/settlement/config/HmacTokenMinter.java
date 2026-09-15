package com.lucilla.settlement.config;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Base64;

/**
 * Mints the HS256 Ledger API token a participant configured with the
 * {@code unsafe-jwt-hmac-256} auth service accepts — the same shape Splice LocalNet's own
 * validator uses ({@code participant-client.ledger-api.auth-config.type = "self-signed"}):
 * an audience-based user token, {@code {"sub": <ledger user>, "aud": <target-audience>,
 * "iat", "exp"}}.
 *
 * <p><b>DevNet / LocalNet only.</b> Anyone holding the shared secret can mint a token for
 * ANY user on that participant, including its admin. See
 * {@code deploy/own-devnet-validator/README.md} Phase 3 for why this was chosen for DevNet
 * and what replaces it before TestNet/MainNet.
 *
 * <p>Pure function, no Spring, no logging — the secret and the token never leave this
 * class except as the return value.
 */
public final class HmacTokenMinter {

    private HmacTokenMinter() {
    }

    public static String mint(String secret, String subject, String audience, String scope,
            Instant issuedAt, long ttlSeconds) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("ledger.hmac-secret is empty");
        }
        if (subject == null || subject.isBlank()) {
            throw new IllegalStateException("ledger.token-subject (the Ledger API user id) is empty");
        }
        if (ttlSeconds <= 0) {
            throw new IllegalStateException("ledger.token-ttl-seconds must be positive");
        }
        long iat = issuedAt.getEpochSecond();
        StringBuilder payload = new StringBuilder("{\"sub\":\"").append(json(subject)).append('"');
        if (audience != null && !audience.isBlank()) {
            payload.append(",\"aud\":\"").append(json(audience)).append('"');
        }
        if (scope != null && !scope.isBlank()) {
            payload.append(",\"scope\":\"").append(json(scope)).append('"');
        }
        payload.append(",\"iat\":").append(iat).append(",\"exp\":").append(iat + ttlSeconds).append('}');

        String signingInput = b64("{\"alg\":\"HS256\",\"typ\":\"JWT\"}") + "." + b64(payload.toString());
        return signingInput + "." + sign(secret, signingInput);
    }

    static String sign(String secret, String signingInput) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(signingInput.getBytes(StandardCharsets.US_ASCII)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 unavailable", e);
        }
    }

    private static String b64(String s) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    /** Minimal JSON string escaping — user ids and audiences are plain ASCII in practice. */
    private static String json(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }
}
