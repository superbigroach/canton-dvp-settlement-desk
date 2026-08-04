package com.lucilla.settlement.config;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TokenInfo}, whose whole job is to say useful things about an
 * access token WITHOUT ever revealing it.
 *
 * <p>The load-bearing assertions here are the negative ones: that no rendering of a
 * TokenInfo — its {@code summary()} or its {@code toString()} — can contain any part of
 * the token. Those are what make it safe to interpolate one into a log line, which is
 * the only reason this class exists rather than logging {@code jwt.substring(0, 8)} at
 * the call site, which is exactly how tokens end up in log aggregators.
 */
class TokenInfoTest {

    /** A JWT-shaped string whose payload carries {@code exp}. Signature is irrelevant. */
    private static String jwtExpiring(Instant exp) {
        String header = b64("{\"alg\":\"RS256\",\"typ\":\"JWT\"}");
        String payload = b64("{\"sub\":\"desk\",\"exp\":" + exp.getEpochSecond() + "}");
        return header + "." + payload + ".c2lnbmF0dXJlLXZhbHVl";
    }

    private static String b64(String s) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void absentTokenIsNotAFault() {
        assertThat(TokenInfo.of(null).present()).isFalse();
        assertThat(TokenInfo.of("   ").present()).isFalse();
        // The local sandbox runs with no token at all, and the summary says so plainly
        // rather than reading like a misconfiguration.
        assertThat(TokenInfo.of(null).summary()).contains("local plaintext sandbox");
        assertThat(TokenInfo.of(null).expired()).isFalse();
    }

    @Test
    void readsTheExpiryOutOfAJwt() {
        Instant exp = Instant.now().plusSeconds(3600);
        TokenInfo info = TokenInfo.of(jwtExpiring(exp));

        assertThat(info.present()).isTrue();
        assertThat(info.readable()).isTrue();
        assertThat(info.expired()).isFalse();
        assertThat(info.expiresAt().getEpochSecond()).isEqualTo(exp.getEpochSecond());
        assertThat(info.secondsRemaining()).isBetween(3500L, 3600L);
        assertThat(info.summary()).contains("expires");
    }

    @Test
    void anExpiredTokenSaysSoLoudly() {
        TokenInfo info = TokenInfo.of(jwtExpiring(Instant.now().minusSeconds(600)));

        assertThat(info.expired()).isTrue();
        assertThat(info.secondsRemaining()).isNegative();
        assertThat(info.summary()).contains("EXPIRED");
        assertThat(info.summary()).contains("UNAUTHENTICATED");
    }

    @Test
    void anOpaqueTokenIsPresentButUnreadable() {
        TokenInfo info = TokenInfo.of("not-a-jwt-just-an-opaque-string");

        assertThat(info.present()).isTrue();
        assertThat(info.readable()).isFalse();
        assertThat(info.expiresAt()).isNull();
        assertThat(info.secondsRemaining()).isNull();
        // Unknown expiry is NOT "expired" — claiming otherwise would send someone
        // hunting for a token problem that is not there.
        assertThat(info.expired()).isFalse();
    }

    @Test
    void garbageAfterTheDotsDoesNotThrow() {
        TokenInfo info = TokenInfo.of("aaa.!!!not-base64!!!.ccc");

        assertThat(info.present()).isTrue();
        assertThat(info.readable()).isFalse();
    }

    /** THE ONE THAT MATTERS: no rendering may contain the token. */
    @Test
    void neitherSummaryNorToStringCanLeakTheToken() {
        String secretPayload = b64("{\"sub\":\"desk\",\"exp\":"
                + Instant.now().plusSeconds(60).getEpochSecond() + "}");
        String token = b64("{\"alg\":\"RS256\"}") + "." + secretPayload
                + ".SUPERSECRETSIGNATUREVALUE";

        TokenInfo info = TokenInfo.of(token);

        assertThat(info.summary()).doesNotContain(token);
        assertThat(info.summary()).doesNotContain("SUPERSECRETSIGNATUREVALUE");
        assertThat(info.summary()).doesNotContain(secretPayload);
        assertThat(info.toString()).doesNotContain(token);
        assertThat(info.toString()).doesNotContain("SUPERSECRETSIGNATUREVALUE");
        assertThat(info.toString()).doesNotContain(secretPayload);
    }
}
