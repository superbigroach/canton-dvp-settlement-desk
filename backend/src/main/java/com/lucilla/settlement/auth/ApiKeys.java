package com.lucilla.settlement.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Signer API keys — {@code ck_} + 32 random bytes, hashed with SHA-256 at rest.
 *
 * <p>The key is shown once at creation and never stored; a leaked users file yields
 * hashes, not credentials. Lookup is by hash, so verifying costs one digest.
 */
public final class ApiKeys {

    public static final String PREFIX = "ck_";
    private static final SecureRandom RANDOM = new SecureRandom();

    private ApiKeys() {}

    public static String generate() {
        byte[] b = new byte[32];
        RANDOM.nextBytes(b);
        return PREFIX + HexFormat.of().formatHex(b);
    }

    public static boolean looksLikeKey(String bearer) {
        return bearer != null && bearer.startsWith(PREFIX);
    }

    public static String hash(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(key.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
