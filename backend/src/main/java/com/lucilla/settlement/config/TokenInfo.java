package com.lucilla.settlement.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What can safely be SAID about an access token: that there is one, and when it runs
 * out. Never what it is.
 *
 * <p>An expired bearer token is one of the two ways this desk goes dark mid-demo (the
 * other is pointing at a stale package), and it announces itself as an opaque
 * {@code UNAUTHENTICATED} — so "how long has this token got left?" has to be answerable
 * in one glance. It is answered by decoding the JWT's own {@code exp} claim.
 *
 * <p><b>THE TOKEN VALUE NEVER LEAVES THIS CLASS.</b> The raw string is a constructor
 * argument and a local variable and nothing else: it is not stored in a field, not
 * returned by any accessor, and {@link #toString()} is overridden so that even an
 * accidental {@code log.info("{}", tokenInfo)} cannot print it. The only facts that
 * escape are presence, expiry, and how long is left. There is deliberately no getter
 * for the token, the signature, or any other claim that could identify the bearer.
 *
 * <p>The signature is NOT verified — this is a diagnostic, not an authorisation
 * decision. The participant remains the only authority on whether a token is good; all
 * this does is let a human see the expiry the participant is about to object to.
 */
public final class TokenInfo {

    /** JWTs put {@code exp} in the payload as seconds since the epoch. */
    private static final Pattern EXP = Pattern.compile("\"exp\"\\s*:\\s*(\\d+)");

    /** No token configured at all — the local-sandbox case, which is not a fault. */
    public static final TokenInfo ABSENT = new TokenInfo(false, null, false);

    private final boolean present;
    private final Instant expiresAt;
    private final boolean readable;

    private TokenInfo(boolean present, Instant expiresAt, boolean readable) {
        this.present = present;
        this.expiresAt = expiresAt;
        this.readable = readable;
    }

    /**
     * Inspect a bearer token WITHOUT retaining it.
     *
     * @param token the raw JWT, or null/blank when none is configured. It is read here
     *              and immediately forgotten — no reference to it survives this call.
     */
    public static TokenInfo of(String token) {
        if (token == null || token.isBlank()) {
            return ABSENT;
        }
        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            // Present, but not a JWT we can read (an opaque token is legitimate).
            return new TokenInfo(true, null, false);
        }
        try {
            String payload = new String(
                    Base64.getUrlDecoder().decode(padded(parts[1])), StandardCharsets.UTF_8);
            Matcher m = EXP.matcher(payload);
            if (!m.find()) {
                return new TokenInfo(true, null, false);
            }
            return new TokenInfo(true, Instant.ofEpochSecond(Long.parseLong(m.group(1))), true);
        } catch (RuntimeException e) {
            // A token we cannot decode is still a token. Report presence, claim nothing.
            return new TokenInfo(true, null, false);
        }
    }

    /** Is a token configured at all? */
    public boolean present() {
        return present;
    }

    /** Could the expiry be read out of it? (false for an opaque or unparseable token) */
    public boolean readable() {
        return readable;
    }

    /** When it expires, or null when unknown. */
    public Instant expiresAt() {
        return expiresAt;
    }

    /** True only when we KNOW it has expired. Unknown expiry is not "expired". */
    public boolean expired() {
        return expiresAt != null && !expiresAt.isAfter(Instant.now());
    }

    /** Seconds of life left, or null when the expiry is unknown. Negative once expired. */
    public Long secondsRemaining() {
        return expiresAt == null ? null : Duration.between(Instant.now(), expiresAt).toSeconds();
    }

    /** One human sentence for a startup banner or a diagnostic page. */
    public String summary() {
        if (!present) {
            return "absent (no JWT configured — correct for a local plaintext sandbox)";
        }
        if (!readable || expiresAt == null) {
            return "present (opaque — no readable exp claim, so expiry is unknown)";
        }
        long left = secondsRemaining();
        if (left <= 0) {
            return "present but EXPIRED at " + expiresAt + " (" + (-left) + "s ago) — the "
                    + "participant will answer UNAUTHENTICATED until it is replaced";
        }
        return "present, expires " + expiresAt + " (in " + humanise(left) + ")";
    }

    /**
     * Deliberately does NOT include the token. This override is the last line of
     * defence against a stray interpolation putting a bearer token in a log file.
     */
    @Override
    public String toString() {
        return "TokenInfo[" + summary() + "]";
    }

    private static String humanise(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        if (h > 0) {
            return h + "h" + m + "m";
        }
        if (m > 0) {
            return m + "m" + (seconds % 60) + "s";
        }
        return seconds + "s";
    }

    /** Base64url in a JWT is unpadded; the JDK decoder wants the padding. */
    private static String padded(String s) {
        int remainder = s.length() % 4;
        return remainder == 0 ? s : s + "====".substring(remainder);
    }
}
