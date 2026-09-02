package com.lucilla.settlement.ledger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The issuer's and lender's conditions are no longer ticks. Each rule in
 * docs/SIGNER_PROTOCOL.md §2a/§2b is applied to the numbers the signer supplies, and the
 * refusal names the number that failed. The load-bearing assertions are the refusals.
 */
class SignerEvidenceTest {

    static final Instant NOW = Instant.parse("2026-09-02T15:00:00Z");
    static final BigDecimal PRICE = new BigDecimal("65000");
    static final SignerEvidence.Tolerances T25 = SignerEvidence.Tolerances.defaults();

    static Map<String, Object> issuerAllGood() {
        return Map.of(
                "attestor-quorum", Map.of("quorumSigners", 8, "quorumThreshold", 7),
                "reserves-current", Map.of("reservesAsOf", "2026-09-02T09:00:00Z"),
                "reserves-cover-supply", Map.of("reserves", "1250.5", "supply", 1250),
                "redemption-queue-clear", Map.of("queueDepth", 0, "maxQueueDepth", 10));
    }

    @Test
    @DisplayName("an issuer with all four conditions verified: the numbers and what was derived land on the record")
    void issuerPasses() {
        var r = SignerEvidence.verify("issuer", List.of("attestor-quorum", "reserves-current",
                "reserves-cover-supply", "redemption-queue-clear"), issuerAllGood(), PRICE, T25, NOW);
        assertThat(r.ok()).as(r.problems().toString()).isTrue();
        assertThat(r.verified()).containsKeys("attestor-quorum", "reserves-current", "reserves-cover-supply", "redemption-queue-clear");
        assertThat(r.verified().get("reserves-current").get("ageHours")).isEqualTo(new BigDecimal("6.00"));
        assertThat(r.verified().get("reserves-cover-supply").get("coverage").toString()).startsWith("1.0004");
        assertThat(r.verified().get("attestor-quorum").get("quorumSigners")).isEqualTo(new BigDecimal("8"));
    }

    @Test
    @DisplayName("quorum below threshold is refused, naming both numbers")
    void quorumFails() {
        var r = SignerEvidence.verify("issuer", List.of("attestor-quorum"),
                Map.of("attestor-quorum", Map.of("quorumSigners", 5, "quorumThreshold", 7)), PRICE, T25, NOW);
        assertThat(r.ok()).isFalse();
        assertThat(r.problems().get(0)).contains("5").contains("7").contains("attestor-quorum");
    }

    @Test
    @DisplayName("a proof-of-reserve older than 24h is refused; one from the future is refused too")
    void reservesStale() {
        var stale = SignerEvidence.verify("issuer", List.of("reserves-current"),
                Map.of("reserves-current", Map.of("reservesAsOf", "2026-09-01T14:00:00Z")), PRICE, T25, NOW);
        assertThat(stale.problems().get(0)).contains("25h").contains("24h");
        var future = SignerEvidence.verify("issuer", List.of("reserves-current"),
                Map.of("reserves-current", Map.of("reservesAsOf", "2026-09-03T14:00:00Z")), PRICE, T25, NOW);
        assertThat(future.problems().get(0)).contains("future");
        var epoch = SignerEvidence.verify("issuer", List.of("reserves-current"),
                Map.of("reserves-current", Map.of("reservesAsOf", NOW.minusSeconds(3600).toEpochMilli())), PRICE, T25, NOW);
        assertThat(epoch.ok()).isTrue();
    }

    @Test
    @DisplayName("reserves below supply and a queue past its depth are refused")
    void coverageAndQueue() {
        var r = SignerEvidence.verify("issuer", List.of("reserves-cover-supply", "redemption-queue-clear"),
                Map.of("reserves-cover-supply", Map.of("reserves", 1000, "supply", 1001),
                        "redemption-queue-clear", Map.of("queueDepth", 11, "maxQueueDepth", 10)), PRICE, T25, NOW);
        assertThat(r.problems()).hasSize(2);
        assertThat(r.problems().get(0)).contains("do not cover");
        assertThat(r.problems().get(1)).contains("exceeds");
    }

    @Test
    @DisplayName("the lender's mark: within 25 bp passes, beyond it is refused, and a wider declared tolerance admits it")
    void independentMark() {
        var ok = SignerEvidence.verify("lender", List.of("independent-mark-within-tolerance"),
                Map.of("independent-mark-within-tolerance", Map.of("independentMark", 65100)), PRICE, T25, NOW);
        assertThat(ok.ok()).isTrue();
        assertThat(ok.verified().get("independent-mark-within-tolerance").get("deviationBps")).isEqualTo(new BigDecimal("15.38"));
        assertThat(ok.verified().get("independent-mark-within-tolerance").get("toleranceBps")).isEqualTo(25);

        var far = SignerEvidence.verify("lender", List.of("independent-mark-within-tolerance"),
                Map.of("independent-mark-within-tolerance", Map.of("independentMark", "64800")), PRICE, T25, NOW);
        assertThat(far.ok()).isFalse();
        assertThat(far.problems().get(0)).contains("30.77 bp").contains("25 bp");

        var wide = SignerEvidence.verify("lender", List.of("independent-mark-within-tolerance"),
                Map.of("independent-mark-within-tolerance", Map.of("independentMark", "64800")), PRICE,
                new SignerEvidence.Tolerances(50, 50), NOW);
        assertThat(wide.ok()).isTrue();
    }

    @Test
    @DisplayName("liquidations: none run is fine; a deviation past the tolerance is refused")
    void liquidations() {
        var none = SignerEvidence.verify("lender", List.of("liquidations-consistent"),
                Map.of("liquidations-consistent", Map.of("liquidationsToday", 0, "worstDeviationBps", 0)), PRICE, T25, NOW);
        assertThat(none.ok()).isTrue();
        var bad = SignerEvidence.verify("lender", List.of("liquidations-consistent"),
                Map.of("liquidations-consistent", Map.of("liquidationsToday", 3, "worstDeviationBps", 40)), PRICE, T25, NOW);
        assertThat(bad.problems().get(0)).contains("40 bp").contains("25 bp");
        var own = SignerEvidence.verify("lender", List.of("liquidations-consistent"),
                Map.of("liquidations-consistent", Map.of("liquidationsToday", 3, "worstDeviationBps", 40)), PRICE,
                SignerEvidence.Tolerances.from(Map.of("markBps", 25, "liquidationBps", "45")), NOW);
        assertThat(own.ok()).isTrue();
    }

    @Test
    @DisplayName("book acceptance needs a timestamp that is not in the future")
    void bookAcceptance() {
        var ok = SignerEvidence.verify("lender", List.of("book-acceptance"),
                Map.of("book-acceptance", Map.of("acceptedAt", "2026-09-02T14:59:00Z")), PRICE, T25, NOW);
        assertThat(ok.ok()).isTrue();
        var future = SignerEvidence.verify("lender", List.of("book-acceptance"),
                Map.of("book-acceptance", Map.of("acceptedAt", "2026-09-02T16:00:00Z")), PRICE, T25, NOW);
        assertThat(future.problems().get(0)).contains("future");
        var garbage = SignerEvidence.verify("lender", List.of("book-acceptance"),
                Map.of("book-acceptance", Map.of("acceptedAt", "yesterday")), PRICE, T25, NOW);
        assertThat(garbage.problems().get(0)).contains("incomplete").contains("acceptedAt");
    }

    @Test
    @DisplayName("a missing block, or a bare tick, names the fields the condition needs")
    void missingEvidence() {
        var r = SignerEvidence.verify("lender", List.of("book-acceptance", "independent-mark-within-tolerance"),
                Map.of("low", 1, "high", 2), PRICE, T25, NOW);
        assertThat(r.problems()).hasSize(2);
        assertThat(r.problems().get(0)).contains("book-acceptance").contains("missing").contains("acceptedAt (instant)");
        var none = SignerEvidence.verify("issuer", List.of("attestor-quorum"), null, PRICE, T25, NOW);
        assertThat(none.problems().get(0)).contains("quorumSigners (integer)");
    }

    @Test
    @DisplayName("the venue's evidence is the ledger's to check; the operator has none — neither is refused here")
    void venueAndOperatorAreNotServerChecked() {
        assertThat(SignerEvidence.required("issuer")).isTrue();
        assertThat(SignerEvidence.required("lender")).isTrue();
        assertThat(SignerEvidence.required("venue")).isTrue();   // the range — but verifiedBy is the ledger
        assertThat(SignerEvidence.required("operator")).isFalse();
        var venue = SignerEvidence.verify("venue", List.of("traded-range", "sufficient-volume"), null, PRICE, T25, NOW);
        assertThat(venue.ok()).isTrue();
        assertThat(venue.verified()).isEmpty();
        @SuppressWarnings("unchecked")
        Map<String, Object> range = (Map<String, Object>) SignerEvidence.schemaFor("venue", null).get("traded-range");
        assertThat(range).containsEntry("verifiedBy", "ledger");
    }

    @Test
    @DisplayName("the schema the 422 carries names every field, its type and the rule")
    void schema() {
        Map<String, Object> s = SignerEvidence.schemaFor("lender", List.of("independent-mark-within-tolerance"));
        assertThat(s).containsOnlyKeys("independent-mark-within-tolerance");
        @SuppressWarnings("unchecked")
        Map<String, Object> c = (Map<String, Object>) s.get("independent-mark-within-tolerance");
        assertThat(c).containsEntry("required", true).containsEntry("verifiedBy", "server");
        assertThat(c.get("rule").toString()).contains("markBps").contains("25");
        assertThat(c.get("fields").toString()).contains("independentMark").contains("number");
        assertThat(SignerEvidence.schemaFor("issuer", null)).hasSize(4);
        assertThat(SignerEvidence.schemaFor("nobody", null)).isEmpty();
    }

    @Test
    @DisplayName("tolerances come from signer settings, with 25 bp as the default and the liquidation tolerance following the mark")
    void tolerances() {
        assertThat(SignerEvidence.Tolerances.from(null)).isEqualTo(new SignerEvidence.Tolerances(25, 25));
        assertThat(SignerEvidence.Tolerances.from(Map.of("markBps", 40))).isEqualTo(new SignerEvidence.Tolerances(40, 40));
        assertThat(SignerEvidence.Tolerances.from(Map.of("toleranceBps", "10", "liquidationBps", 30)))
                .isEqualTo(new SignerEvidence.Tolerances(10, 30));
        assertThat(SignerEvidence.Tolerances.from(Map.of("markBps", "not a number"))).isEqualTo(new SignerEvidence.Tolerances(25, 25));
    }
}
