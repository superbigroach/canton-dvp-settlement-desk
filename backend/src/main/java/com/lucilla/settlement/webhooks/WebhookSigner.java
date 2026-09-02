package com.lucilla.settlement.webhooks;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * The outbound signature — docs/PRODUCT-PLAN.md §5:
 * {@code X-CrossDesk-Signature: sha256=HMAC(secret, body)}, HMAC-SHA256 over the exact
 * request body bytes, hex-encoded. A receiver recomputes it over the raw body it got
 * and compares in constant time.
 */
public final class WebhookSigner {

    public static final String HEADER = "X-CrossDesk-Signature";
    private static final String PREFIX = "sha256=";

    private WebhookSigner() {}

    public static String signature(String secret, byte[] body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec((secret == null ? "" : secret).getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"));
            return PREFIX + HexFormat.of().formatHex(mac.doFinal(body));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 unavailable", e);
        }
    }

    public static String signature(String secret, String body) {
        return signature(secret, body.getBytes(StandardCharsets.UTF_8));
    }

    /** What a receiver does. */
    public static boolean verify(String secret, byte[] body, String header) {
        if (header == null) return false;
        byte[] expected = signature(secret, body).getBytes(StandardCharsets.UTF_8);
        byte[] given = header.trim().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, given);
    }
}
