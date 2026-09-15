package com.lucilla.settlement.config;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The own-node / LocalNet wiring: token-mode resolution, the HS256 token a
 * {@code unsafe-jwt-hmac-256} participant accepts, the applicationId == sub rule, and the
 * party roster.
 */
class LedgerAuthConfigTest {

    private static String payloadOf(String jwt) {
        return new String(Base64.getUrlDecoder().decode(jwt.split("\\.")[1]), StandardCharsets.UTF_8);
    }

    @Test
    void autoModeResolvesFromWhatIsConfigured() {
        LedgerProperties p = new LedgerProperties();
        assertThat(p.effectiveAuthMode()).isEqualTo(LedgerProperties.AuthMode.NONE);

        p.setJwt("a.b.c");
        assertThat(p.effectiveAuthMode()).isEqualTo(LedgerProperties.AuthMode.STATIC);
        assertThat(p.hasRenewingToken()).isFalse();

        p.setTokenEndpoint("https://idp/token");
        p.setClientSecret("s");
        assertThat(p.effectiveAuthMode()).isEqualTo(LedgerProperties.AuthMode.CLIENT_CREDENTIALS);

        // The Noders env set (endpoint + refresh token + static JWT) keeps meaning "refresh".
        p.setRefreshToken("rt");
        assertThat(p.effectiveAuthMode()).isEqualTo(LedgerProperties.AuthMode.REFRESH);

        p.setHmacSecret("secret");
        assertThat(p.effectiveAuthMode()).isEqualTo(LedgerProperties.AuthMode.HMAC);
        assertThat(p.hasRenewingToken()).isTrue();

        p.setAuthMode("static");
        assertThat(p.effectiveAuthMode()).isEqualTo(LedgerProperties.AuthMode.STATIC);
        p.setAuthMode("client_credentials");
        assertThat(p.effectiveAuthMode()).isEqualTo(LedgerProperties.AuthMode.CLIENT_CREDENTIALS);
        p.setAuthMode("bogus");
        assertThatThrownBy(p::effectiveAuthMode).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void hmacModeForcesApplicationIdToTheTokenSubject() {
        LedgerProperties p = new LedgerProperties();
        p.setApplicationId("canton-dvp-desk");
        assertThat(p.getApplicationId()).isEqualTo("canton-dvp-desk");

        p.setHmacSecret("secret");
        p.setTokenSubject("crossdesk-backend");
        assertThat(p.getApplicationId()).isEqualTo("crossdesk-backend");
    }

    @Test
    void hmacTokenIsAnAudienceBasedUserTokenWithAValidSignature() {
        Instant now = Instant.ofEpochSecond(1_800_000_000L);
        String jwt = HmacTokenMinter.mint("unsafe", "crossdesk-backend",
                "https://canton.network.global", "", now, 3600);

        String[] parts = jwt.split("\\.");
        assertThat(parts).hasSize(3);
        assertThat(new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8))
                .isEqualTo("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        assertThat(payloadOf(jwt)).isEqualTo("{\"sub\":\"crossdesk-backend\","
                + "\"aud\":\"https://canton.network.global\",\"iat\":1800000000,\"exp\":1800003600}");
        // Signature recomputes over header.payload with the same secret, and not with another.
        assertThat(parts[2]).isEqualTo(HmacTokenMinter.sign("unsafe", parts[0] + "." + parts[1]));
        assertThat(parts[2]).isNotEqualTo(HmacTokenMinter.sign("other", parts[0] + "." + parts[1]));
        // TokenInfo can read the expiry without rendering the token.
        assertThat(TokenInfo.of(jwt).expiresAt()).isEqualTo(now.plusSeconds(3600));
        assertThat(TokenInfo.of(jwt).summary()).doesNotContain(parts[2]);
    }

    @Test
    void hmacMintingRefusesMissingInputs() {
        Instant now = Instant.now();
        assertThatThrownBy(() -> HmacTokenMinter.mint("", "u", "a", "", now, 60))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> HmacTokenMinter.mint("s", " ", "a", "", now, 60))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> HmacTokenMinter.mint("s", "u", "a", "", now, 0))
                .isInstanceOf(IllegalStateException.class);
        assertThat(payloadOf(HmacTokenMinter.mint("s", "u\"x", "", "daml_ledger_api", now, 60)))
                .contains("\"sub\":\"u\\\"x\"").contains("\"scope\":\"daml_ledger_api\"").doesNotContain("aud");
    }

    @Test
    void rosterParsesLabelsAndRejectsShortIds() {
        List<PartyRoster.Entry> r = PartyRoster.parse(
                " Issuer=issuer-crossdesk::1220ab , Venue=bank-crossdesk::1220ab,,Bank=bank-crossdesk::1220ab");
        assertThat(r).containsExactly(
                new PartyRoster.Entry("Issuer", "issuer-crossdesk::1220ab"),
                new PartyRoster.Entry("Venue", "bank-crossdesk::1220ab"),
                new PartyRoster.Entry("Bank", "bank-crossdesk::1220ab"));
        assertThat(PartyRoster.parse("")).isEmpty();
        assertThatThrownBy(() -> PartyRoster.parse("Issuer=issuer-crossdesk"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("full party id");
        assertThatThrownBy(() -> PartyRoster.parse("Issuer"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void refresherRedactsTokenFieldsBeforeLogging() {
        assertThat(TokenRefresher.redactTokens("{\"access_token\":\"abc\",\"error\":\"x\"}"))
                .isEqualTo("{\"access_token\":\"<redacted>\",\"error\":\"x\"}");
    }
}
