package com.lucilla.settlement.ledger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every committee seat, every condition, under every reserve model: the numbers that
 * pass, the boundary that fails, and the EXACT problem text the desk hands back.
 *
 * <p>The existing {@link SignerEvidenceTest} covers the v1 seats loosely ("contains
 * 25h"). This class pins the boundary itself — exactly 24h passes, 24h01m fails — and
 * the full refusal string, so a wording change or an off-by-one in a rule is a red test
 * and not a surprise on a signer's screen. The v2 seats (custodian, transfer-agent) and
 * the venue's {@code no-prints-attested} had no evidence tests at all.
 */
class SignerEvidenceSeatMatrixTest {

    static final Instant NOW = Instant.parse("2026-09-22T15:00:00Z");
    static final BigDecimal PRICE = new BigDecimal("65000");
    static final SignerEvidence.Tolerances T25 = SignerEvidence.Tolerances.defaults();
    static final Duration ONE_MINUTE = Duration.ofMinutes(1);

    static SignerEvidence.Result one(String seat, String condition, Map<String, Object> block) {
        return one(seat, condition, block, T25, PRICE);
    }

    static SignerEvidence.Result one(String seat, String condition, Map<String, Object> block,
            SignerEvidence.Tolerances tol) {
        return one(seat, condition, block, tol, PRICE);
    }

    static SignerEvidence.Result one(String seat, String condition, Map<String, Object> block,
            SignerEvidence.Tolerances tol, BigDecimal proposal) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put(condition, block);
        return SignerEvidence.verify(seat, List.of(condition), evidence, proposal, tol, NOW);
    }

    static void passes(SignerEvidence.Result r, String condition) {
        assertThat(r.ok()).as("expected a pass, got %s", r.problems()).isTrue();
        assertThat(r.verified()).containsKey(condition);
    }

    static void refused(SignerEvidence.Result r, String exactProblem) {
        assertThat(r.ok()).isFalse();
        assertThat(r.problems()).containsExactly(exactProblem);
        assertThat(r.verified()).isEmpty();
    }

    static String at(Instant i) {
        return i.toString();
    }

    static Instant ago(Duration d) {
        return NOW.minus(d);
    }

    // ---- issuer -------------------------------------------------------------------

    static Map<String, Object> issuerGood() {
        return Map.of(
                "attestor-quorum", Map.of("quorumSigners", 7, "quorumThreshold", 7),
                "reserves-current", Map.of("reservesAsOf", at(ago(Duration.ofHours(24)))),
                "reserves-cover-supply", Map.of("reserves", 1000, "supply", 1000),
                "redemption-queue-clear", Map.of("queueDepth", 10, "maxQueueDepth", 10));
    }

    static Map<String, Object> issuerBoundaryViolated() {
        return Map.of(
                "attestor-quorum", Map.of("quorumSigners", 6, "quorumThreshold", 7),
                "reserves-current", Map.of("reservesAsOf", at(ago(Duration.ofHours(24).plus(ONE_MINUTE)))),
                "reserves-cover-supply", Map.of("reserves", 999, "supply", 1000),
                "redemption-queue-clear", Map.of("queueDepth", 11, "maxQueueDepth", 10));
    }

    @Nested
    @DisplayName("issuer")
    class Issuer {

        @Test
        @DisplayName("attestor-quorum: signers == threshold passes; one fewer is refused naming both numbers")
        void attestorQuorumBoundary() {
            passes(one("issuer", "attestor-quorum", Map.of("quorumSigners", 7, "quorumThreshold", 7)), "attestor-quorum");
            refused(one("issuer", "attestor-quorum", Map.of("quorumSigners", 6, "quorumThreshold", 7)),
                    "attestor-quorum: quorumSigners 6 is below quorumThreshold 7");
            refused(one("issuer", "attestor-quorum", Map.of("quorumSigners", 6, "quorumThreshold", 0)),
                    "attestor-quorum: quorumThreshold must be positive");
        }

        @Test
        @DisplayName("reserves-current: exactly freshnessHours old passes; one minute over is refused; the window is the asset's")
        void reservesCurrentFreshnessBoundary() {
            var exactly = one("issuer", "reserves-current", Map.of("reservesAsOf", at(ago(Duration.ofHours(24)))));
            passes(exactly, "reserves-current");
            assertThat(exactly.verified().get("reserves-current").get("ageHours")).isEqualTo(new BigDecimal("24.00"));

            refused(one("issuer", "reserves-current",
                            Map.of("reservesAsOf", at(ago(Duration.ofHours(24).plus(ONE_MINUTE))))),
                    "reserves-current: the proof-of-reserve is 24h old; it must be under 24h");

            // A slower model declares a wider window: 24h01m now passes and 48h01m is the refusal.
            var t48 = new SignerEvidence.Tolerances(25, 25, 48);
            passes(one("issuer", "reserves-current",
                    Map.of("reservesAsOf", at(ago(Duration.ofHours(24).plus(ONE_MINUTE)))), t48), "reserves-current");
            refused(one("issuer", "reserves-current",
                            Map.of("reservesAsOf", at(ago(Duration.ofHours(48).plus(ONE_MINUTE)))), t48),
                    "reserves-current: the proof-of-reserve is 48h old; it must be under 48h");

            // Clock skew: 5 minutes ahead is tolerated, 6 is "in the future".
            passes(one("issuer", "reserves-current", Map.of("reservesAsOf", at(NOW.plus(Duration.ofMinutes(5))))),
                    "reserves-current");
            refused(one("issuer", "reserves-current", Map.of("reservesAsOf", at(NOW.plus(Duration.ofMinutes(6))))),
                    "reserves-current: reservesAsOf 2026-09-22T15:06:00Z is in the future");
        }

        @Test
        @DisplayName("reserves-cover-supply: reserves == supply passes at coverage 1; one unit short is refused")
        void reservesCoverSupplyBoundary() {
            var r = one("issuer", "reserves-cover-supply", Map.of("reserves", 1000, "supply", 1000));
            passes(r, "reserves-cover-supply");
            assertThat((BigDecimal) r.verified().get("reserves-cover-supply").get("coverage")).isEqualByComparingTo("1");

            refused(one("issuer", "reserves-cover-supply", Map.of("reserves", 999, "supply", 1000)),
                    "reserves-cover-supply: reserves 999 do not cover supply 1000");
            refused(one("issuer", "reserves-cover-supply", Map.of("reserves", "999.999999", "supply", 1000)),
                    "reserves-cover-supply: reserves 999.999999 do not cover supply 1000");
            refused(one("issuer", "reserves-cover-supply", Map.of("reserves", -1, "supply", 0)),
                    "reserves-cover-supply: reserves and supply must be non-negative");
        }

        @Test
        @DisplayName("redemption-queue-clear: depth == max passes; one deeper is refused")
        void redemptionQueueBoundary() {
            passes(one("issuer", "redemption-queue-clear", Map.of("queueDepth", 10, "maxQueueDepth", 10)),
                    "redemption-queue-clear");
            refused(one("issuer", "redemption-queue-clear", Map.of("queueDepth", 11, "maxQueueDepth", 10)),
                    "redemption-queue-clear: queueDepth 11 exceeds maxQueueDepth 10");
            refused(one("issuer", "redemption-queue-clear", Map.of("queueDepth", -1, "maxQueueDepth", 10)),
                    "redemption-queue-clear: depths must be non-negative");
        }

        @ParameterizedTest(name = "reserve model {0}")
        @EnumSource(SignerProtocol.ReserveModel.class)
        @DisplayName("under every reserve model the issuer's checklist verifies with good numbers and every boundary fails by name")
        void everyModelsChecklistVerifiesAndTheModelNeverChangesTheRule(SignerProtocol.ReserveModel model) {
            SignerProtocol.Role issuer = SignerProtocol.issuerFor(model);
            List<String> names = issuer.conditions().stream().map(SignerProtocol.Condition::name).toList();
            if (model == SignerProtocol.ReserveModel.ATTESTED) {
                assertThat(names).containsExactly("attestor-quorum", "reserves-current",
                        "reserves-cover-supply", "redemption-queue-clear");
            } else {
                assertThat(names).containsExactly("reserves-current", "reserves-cover-supply", "redemption-queue-clear");
            }
            for (SignerProtocol.Condition c : issuer.conditions()) {
                assertThat(c.evidence().required()).as("%s needs evidence", c.name()).isTrue();
                assertThat(c.evidence().verifiedBy()).as("%s is server-verified", c.name()).isEqualTo("server");
            }

            var good = SignerEvidence.verify("issuer", names, issuerGood(), PRICE, T25, NOW);
            assertThat(good.ok()).as(good.problems().toString()).isTrue();
            assertThat(good.verified().keySet()).containsExactlyElementsOf(names);

            var bad = SignerEvidence.verify("issuer", names, issuerBoundaryViolated(), PRICE, T25, NOW);
            assertThat(bad.verified()).isEmpty();
            assertThat(bad.problems()).hasSize(names.size()).contains(
                    "reserves-current: the proof-of-reserve is 24h old; it must be under 24h",
                    "reserves-cover-supply: reserves 999 do not cover supply 1000",
                    "redemption-queue-clear: queueDepth 11 exceeds maxQueueDepth 10");
            if (model == SignerProtocol.ReserveModel.ATTESTED) {
                assertThat(bad.problems()).contains("attestor-quorum: quorumSigners 6 is below quorumThreshold 7");
            }
        }
    }

    // ---- lender -------------------------------------------------------------------

    @Nested
    @DisplayName("lender")
    class Lender {

        @Test
        @DisplayName("independent mark: exactly 25.00 bp passes; 25.08 bp is refused either side, naming mark, deviation and tolerance")
        void independentMarkBoundary() {
            var edge = one("lender", "independent-mark-within-tolerance", Map.of("independentMark", "65162.5"));
            passes(edge, "independent-mark-within-tolerance");
            assertThat(edge.verified().get("independent-mark-within-tolerance").get("deviationBps"))
                    .isEqualTo(new BigDecimal("25.00"));
            assertThat(edge.verified().get("independent-mark-within-tolerance").get("toleranceBps")).isEqualTo(25);

            refused(one("lender", "independent-mark-within-tolerance", Map.of("independentMark", 65163)),
                    "independent-mark-within-tolerance: your mark 65163 is 25.08 bp from the proposal 65000; "
                            + "your declared tolerance is 25 bp");
            refused(one("lender", "independent-mark-within-tolerance", Map.of("independentMark", 64837)),
                    "independent-mark-within-tolerance: your mark 64837 is 25.08 bp from the proposal 65000; "
                            + "your declared tolerance is 25 bp");
            passes(one("lender", "independent-mark-within-tolerance", Map.of("independentMark", 65163),
                    new SignerEvidence.Tolerances(26, 26)), "independent-mark-within-tolerance");

            refused(one("lender", "independent-mark-within-tolerance", Map.of("independentMark", 0)),
                    "independent-mark-within-tolerance: independentMark must be positive");
            refused(one("lender", "independent-mark-within-tolerance", Map.of("independentMark", 65000), T25, null),
                    "independent-mark-within-tolerance: the proposal has no positive price to compare against");
        }

        @Test
        @DisplayName("liquidations: worst == tolerance passes; 1 bp over is refused; sign is ignored; none run is always fine")
        void liquidationsBoundary() {
            passes(one("lender", "liquidations-consistent", Map.of("liquidationsToday", 3, "worstDeviationBps", 25)),
                    "liquidations-consistent");
            refused(one("lender", "liquidations-consistent", Map.of("liquidationsToday", 3, "worstDeviationBps", 26)),
                    "liquidations-consistent: the worst liquidation deviated 26 bp from the mark; "
                            + "your declared tolerance is 25 bp");
            refused(one("lender", "liquidations-consistent", Map.of("liquidationsToday", 1, "worstDeviationBps", -26)),
                    "liquidations-consistent: the worst liquidation deviated 26 bp from the mark; "
                            + "your declared tolerance is 25 bp");
            passes(one("lender", "liquidations-consistent", Map.of("liquidationsToday", 0, "worstDeviationBps", 900)),
                    "liquidations-consistent");
            passes(one("lender", "liquidations-consistent", Map.of("liquidationsToday", 3, "worstDeviationBps", 26),
                    new SignerEvidence.Tolerances(25, 26)), "liquidations-consistent");
            refused(one("lender", "liquidations-consistent", Map.of("liquidationsToday", -1, "worstDeviationBps", 0)),
                    "liquidations-consistent: liquidationsToday must be non-negative");
        }

        @Test
        @DisplayName("book acceptance: 5 minutes ahead is clock skew; 5m01s is the future")
        void bookAcceptanceBoundary() {
            passes(one("lender", "book-acceptance", Map.of("acceptedAt", at(NOW.plus(Duration.ofMinutes(5))))),
                    "book-acceptance");
            refused(one("lender", "book-acceptance", Map.of("acceptedAt", at(NOW.plus(Duration.ofMinutes(5).plusSeconds(1))))),
                    "book-acceptance: acceptedAt 2026-09-22T15:05:01Z is in the future");
        }

        @Test
        @DisplayName("the whole lender checklist verifies together, and every boundary fails together")
        void fullChecklist() {
            List<String> names = List.of("independent-mark-within-tolerance", "liquidations-consistent", "book-acceptance");
            var good = SignerEvidence.verify("lender", names, Map.of(
                    "independent-mark-within-tolerance", Map.of("independentMark", "65162.5"),
                    "liquidations-consistent", Map.of("liquidationsToday", 2, "worstDeviationBps", 25),
                    "book-acceptance", Map.of("acceptedAt", at(NOW))), PRICE, T25, NOW);
            assertThat(good.ok()).as(good.problems().toString()).isTrue();
            assertThat(good.verified().keySet()).containsExactlyElementsOf(names);

            var bad = SignerEvidence.verify("lender", names, Map.of(
                    "independent-mark-within-tolerance", Map.of("independentMark", 65163),
                    "liquidations-consistent", Map.of("liquidationsToday", 2, "worstDeviationBps", 26),
                    "book-acceptance", Map.of("acceptedAt", at(NOW.plus(Duration.ofMinutes(6))))), PRICE, T25, NOW);
            assertThat(bad.problems()).containsExactly(
                    "independent-mark-within-tolerance: your mark 65163 is 25.08 bp from the proposal 65000; "
                            + "your declared tolerance is 25 bp",
                    "liquidations-consistent: the worst liquidation deviated 26 bp from the mark; "
                            + "your declared tolerance is 25 bp",
                    "book-acceptance: acceptedAt 2026-09-22T15:06:00Z is in the future");
        }
    }

    // ---- venue --------------------------------------------------------------------

    @Nested
    @DisplayName("venue")
    class Venue {

        @Test
        @DisplayName("traded-range, spread and volume are not server rules: nothing is verified here and nothing is refused")
        void tradedRangeIsTheLedgersToCheck() {
            var r = SignerEvidence.verify("venue",
                    List.of("traded-range", "spread-within-tolerance", "sufficient-volume"), null, PRICE, T25, NOW);
            assertThat(r.ok()).isTrue();
            assertThat(r.verified()).isEmpty();
        }

        @Test
        @DisplayName("no-prints-attested: a quote bracketing the proposal passes (inclusive); a quote that excludes it is refused")
        void noPrintsAttestedBoundary() {
            var quoted = one("venue", "no-prints-attested", Map.of("bestBid", 64900, "bestAsk", 65100));
            passes(quoted, "no-prints-attested");
            assertThat(quoted.verified().get("no-prints-attested").get("quoted")).isEqualTo(BigDecimal.ONE);

            passes(one("venue", "no-prints-attested", Map.of("bestBid", 65000, "bestAsk", 65100)), "no-prints-attested");
            passes(one("venue", "no-prints-attested", Map.of("bestBid", 64900, "bestAsk", 65000)), "no-prints-attested");

            refused(one("venue", "no-prints-attested", Map.of("bestBid", 65001, "bestAsk", 65100)),
                    "no-prints-attested: the proposal 65000 sits outside your quoted 65001 / 65100");
            refused(one("venue", "no-prints-attested", Map.of("bestBid", 64800, "bestAsk", 64999)),
                    "no-prints-attested: the proposal 65000 sits outside your quoted 64800 / 64999");
            refused(one("venue", "no-prints-attested", Map.of("bestBid", 65200, "bestAsk", 65100)),
                    "no-prints-attested: bestBid 65200 is above bestAsk 65100");
            refused(one("venue", "no-prints-attested", Map.of("bestBid", -1, "bestAsk", 0)),
                    "no-prints-attested: bestBid and bestAsk must be non-negative (0 means no quote)");
            refused(one("venue", "no-prints-attested", Map.of("bestBid", 64900)),
                    "no-prints-attested: evidence incomplete — needs [bestBid (number), bestAsk (number)], got [bestBid]");
        }

        @Test
        @DisplayName("no-prints-attested: an empty or one-sided book is a truthful, weaker answer — passes with quoted = 0")
        void noPrintsWithAnEmptyBook() {
            var empty = one("venue", "no-prints-attested", Map.of("bestBid", 0, "bestAsk", 0));
            passes(empty, "no-prints-attested");
            assertThat(empty.verified().get("no-prints-attested").get("quoted")).isEqualTo(BigDecimal.ZERO);
            passes(one("venue", "no-prints-attested", Map.of("bestBid", 0, "bestAsk", 65100)), "no-prints-attested");
            // no proposal price to contradict: the quote is recorded and nothing is refused
            passes(one("venue", "no-prints-attested", Map.of("bestBid", 64900, "bestAsk", 65100), T25, null),
                    "no-prints-attested");
        }
    }

    // ---- custodian ----------------------------------------------------------------

    static Map<String, Object> custodianGood() {
        return Map.of(
                "holdings-current", Map.of("statementAsOf", at(ago(Duration.ofHours(24)))),
                "holdings-cover-supply", Map.of("holdings", 1000, "supply", 1000),
                "no-encumbrance", Map.of("encumbered", 0));
    }

    @Nested
    @DisplayName("custodian")
    class Custodian {

        @Test
        @DisplayName("holdings-current: exactly freshnessHours old passes; one minute over is refused with the declared limit")
        void holdingsCurrentFreshnessBoundary() {
            var exactly = one("custodian", "holdings-current", Map.of("statementAsOf", at(ago(Duration.ofHours(24)))));
            passes(exactly, "holdings-current");
            assertThat(exactly.verified().get("holdings-current").get("ageHours")).isEqualTo(new BigDecimal("24.00"));

            refused(one("custodian", "holdings-current",
                            Map.of("statementAsOf", at(ago(Duration.ofHours(24).plus(ONE_MINUTE))))),
                    "holdings-current: the statement is 24.02h old, over the 24h limit declared for this asset");

            var t72 = new SignerEvidence.Tolerances(25, 25, 72);
            passes(one("custodian", "holdings-current",
                    Map.of("statementAsOf", at(ago(Duration.ofHours(24).plus(ONE_MINUTE)))), t72), "holdings-current");
            refused(one("custodian", "holdings-current",
                            Map.of("statementAsOf", at(ago(Duration.ofHours(72).plus(ONE_MINUTE)))), t72),
                    "holdings-current: the statement is 72.02h old, over the 72h limit declared for this asset");

            refused(one("custodian", "holdings-current", Map.of("statementAsOf", at(NOW.plus(Duration.ofMinutes(6))))),
                    "holdings-current: statementAsOf 2026-09-22T15:06:00Z is in the future");
        }

        @Test
        @DisplayName("holdings-cover-supply: holdings == supply passes; one unit short is refused")
        void holdingsCoverSupplyBoundary() {
            passes(one("custodian", "holdings-cover-supply", Map.of("holdings", 1000, "supply", 1000)),
                    "holdings-cover-supply");
            refused(one("custodian", "holdings-cover-supply", Map.of("holdings", 999, "supply", 1000)),
                    "holdings-cover-supply: holdings 999 is below issued supply 1000");
            refused(one("custodian", "holdings-cover-supply", Map.of("holdings", 1000, "supply", -1)),
                    "holdings-cover-supply: holdings and supply must be non-negative");
        }

        @Test
        @DisplayName("no-encumbrance: zero passes; any encumbrance at all is refused with the amount")
        void noEncumbranceBoundary() {
            passes(one("custodian", "no-encumbrance", Map.of("encumbered", 0)), "no-encumbrance");
            refused(one("custodian", "no-encumbrance", Map.of("encumbered", 5)),
                    "no-encumbrance: 5 units are pledged, lent or encumbered");
            refused(one("custodian", "no-encumbrance", Map.of("encumbered", "0.000001")),
                    "no-encumbrance: 0.000001 units are pledged, lent or encumbered");
            refused(one("custodian", "no-encumbrance", Map.of("encumbered", -1)),
                    "no-encumbrance: encumbered must be non-negative");
        }

        @ParameterizedTest(name = "reserve model {0}")
        @EnumSource(SignerProtocol.ReserveModel.class)
        @DisplayName("the custodian seat is the same under every reserve model, and its whole checklist verifies")
        void custodianIsModelIndependent(SignerProtocol.ReserveModel model) {
            SignerProtocol.Role seat = SignerProtocol.roleFor("custodian", model);
            List<String> names = seat.conditions().stream().map(SignerProtocol.Condition::name).toList();
            assertThat(names).containsExactly("holdings-current", "holdings-cover-supply", "no-encumbrance");
            var r = SignerEvidence.verify("custodian", names, custodianGood(), PRICE, T25, NOW);
            assertThat(r.ok()).as(r.problems().toString()).isTrue();
            assertThat(r.verified().keySet()).containsExactlyElementsOf(names);
        }
    }

    // ---- transfer agent -----------------------------------------------------------

    @Nested
    @DisplayName("transfer-agent")
    class TransferAgent {

        @Test
        @DisplayName("shares-outstanding-reconciled: equal counts pass (numerically); any mismatch is refused naming both")
        void sharesOutstandingBoundary() {
            passes(one("transfer-agent", "shares-outstanding-reconciled",
                    Map.of("registerShares", 1_000_000, "ledgerShares", 1_000_000)), "shares-outstanding-reconciled");
            passes(one("transfer-agent", "shares-outstanding-reconciled",
                    Map.of("registerShares", "1000000.0", "ledgerShares", 1_000_000)), "shares-outstanding-reconciled");
            refused(one("transfer-agent", "shares-outstanding-reconciled",
                            Map.of("registerShares", 1_000_000, "ledgerShares", 1_000_001)),
                    "shares-outstanding-reconciled: register shows 1000000 but the ledger shows 1000001");
            refused(one("transfer-agent", "shares-outstanding-reconciled",
                            Map.of("registerShares", 1_000_001, "ledgerShares", 1_000_000)),
                    "shares-outstanding-reconciled: register shows 1000001 but the ledger shows 1000000");
            refused(one("transfer-agent", "shares-outstanding-reconciled",
                            Map.of("registerShares", -1, "ledgerShares", 0)),
                    "shares-outstanding-reconciled: share counts must be non-negative");
        }

        @Test
        @DisplayName("fees-accrued: zero or positive passes; negative, absent or missing is refused")
        void feesAccruedBoundary() {
            passes(one("transfer-agent", "fees-accrued", Map.of("accruedFees", 0)), "fees-accrued");
            passes(one("transfer-agent", "fees-accrued", Map.of("accruedFees", "12.5")), "fees-accrued");
            refused(one("transfer-agent", "fees-accrued", Map.of("accruedFees", "-0.01")),
                    "fees-accrued: accruedFees cannot be negative");
            refused(one("transfer-agent", "fees-accrued", Map.of("fees", 1)),
                    "fees-accrued: evidence incomplete — needs [accruedFees (number)], got [fees]");
            refused(SignerEvidence.verify("transfer-agent", List.of("fees-accrued"), Map.of(), PRICE, T25, NOW),
                    "fees-accrued: evidence missing — supply [accruedFees (number)]");
        }

        @ParameterizedTest(name = "reserve model {0}")
        @EnumSource(SignerProtocol.ReserveModel.class)
        @DisplayName("the transfer-agent seat is the same under every reserve model, and its whole checklist verifies")
        void transferAgentIsModelIndependent(SignerProtocol.ReserveModel model) {
            SignerProtocol.Role seat = SignerProtocol.roleFor("transfer-agent", model);
            List<String> names = seat.conditions().stream().map(SignerProtocol.Condition::name).toList();
            assertThat(names).containsExactly("shares-outstanding-reconciled", "fees-accrued");
            var r = SignerEvidence.verify("transfer-agent", names, Map.of(
                    "shares-outstanding-reconciled", Map.of("registerShares", 5000, "ledgerShares", 5000),
                    "fees-accrued", Map.of("accruedFees", 3)), PRICE, T25, NOW);
            assertThat(r.ok()).as(r.problems().toString()).isTrue();
            assertThat(r.verified().keySet()).containsExactlyElementsOf(names);
        }
    }

    // ---- seat boundaries ----------------------------------------------------------

    @Nested
    @DisplayName("seat boundaries")
    class SeatBoundaries {

        @Test
        @DisplayName("the operator brings nothing: no evidence required, nothing verified, nothing refused")
        void operatorBringsNothing() {
            assertThat(SignerEvidence.required("operator")).isFalse();
            var r = SignerEvidence.verify("operator", List.of("inputs-published", "composition-reconciled"),
                    null, PRICE, T25, NOW);
            assertThat(r.ok()).isTrue();
            assertThat(r.verified()).isEmpty();
        }

        @Test
        @DisplayName("which seats must bring evidence")
        void requiredPerSeat() {
            for (String seat : List.of("issuer", "lender", "venue", "custodian", "transfer-agent")) {
                assertThat(SignerEvidence.required(seat)).as(seat).isTrue();
            }
            assertThat(SignerEvidence.required("operator")).isFalse();
            assertThat(SignerEvidence.required("auditor")).isFalse();
            assertThat(SignerEvidence.required(null)).isFalse();
        }

        @ParameterizedTest(name = "{0} claiming {1}")
        @CsvSource({
                "issuer, holdings-current",
                "issuer, shares-outstanding-reconciled",
                "lender, attestor-quorum",
                "venue, reserves-current",
                "custodian, reserves-cover-supply",
                "custodian, shares-outstanding-reconciled",
                "transfer-agent, no-encumbrance",
                "transfer-agent, holdings-cover-supply",
                "operator, fees-accrued",
        })
        @DisplayName("a seat cannot verify another seat's condition, even with perfect numbers for it")
        void aSeatCannotVerifyAnotherSeatsCondition(String seat, String foreign) {
            Map<String, Object> perfect = new LinkedHashMap<>();
            perfect.putAll(issuerGood());
            perfect.putAll(custodianGood());
            perfect.put("shares-outstanding-reconciled", Map.of("registerShares", 1, "ledgerShares", 1));
            perfect.put("fees-accrued", Map.of("accruedFees", 0));
            var r = SignerEvidence.verify(seat, List.of(foreign), perfect, PRICE, T25, NOW);
            assertThat(r.problems()).containsExactly("condition '" + foreign + "' is not one the " + seat + " seat verifies");
            assertThat(r.verified()).isEmpty();
        }

        @Test
        @DisplayName("an unknown seat is refused by name")
        void unknownRole() {
            var r = SignerEvidence.verify("auditor", List.of("book-acceptance"), null, PRICE, T25, NOW);
            assertThat(r.problems()).containsExactly("unknown signer role 'auditor'");
        }

        @Test
        @DisplayName("the 422 schema for the v2 seats names every field and who verifies it")
        void schemaForV2Seats() {
            Map<String, Object> custodian = SignerEvidence.schemaFor("custodian", null);
            assertThat(custodian).containsOnlyKeys("holdings-current", "holdings-cover-supply", "no-encumbrance");
            Map<String, Object> ta = SignerEvidence.schemaFor("transfer-agent", null);
            assertThat(ta).containsOnlyKeys("shares-outstanding-reconciled", "fees-accrued");
            for (Map<String, Object> s : List.of(custodian, ta)) {
                for (Object v : s.values()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> c = (Map<String, Object>) v;
                    assertThat(c).containsEntry("required", true).containsEntry("verifiedBy", "server");
                }
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> noPrints = (Map<String, Object>) SignerEvidence.schemaFor("venue", null).get("no-prints-attested");
            assertThat(noPrints).containsEntry("verifiedBy", "server");
            assertThat(noPrints.get("fields").toString()).contains("bestBid").contains("bestAsk");
            @SuppressWarnings("unchecked")
            Map<String, Object> range = (Map<String, Object>) SignerEvidence.schemaFor("venue", null).get("traded-range");
            assertThat(range).containsEntry("verifiedBy", "ledger");
        }

        @Test
        @DisplayName("freshness: the two-arg tolerance is 24h; zero or negative hours fall back to 24h; a declared window is honoured")
        void freshnessWindow() {
            assertThat(new SignerEvidence.Tolerances(25, 25).freshness()).isEqualTo(Duration.ofHours(24));
            assertThat(new SignerEvidence.Tolerances(25, 25, 0).freshness()).isEqualTo(Duration.ofHours(24));
            assertThat(new SignerEvidence.Tolerances(25, 25, -3).freshness()).isEqualTo(Duration.ofHours(24));
            assertThat(new SignerEvidence.Tolerances(25, 25, 72).freshness()).isEqualTo(Duration.ofHours(72));
            assertThat(SignerEvidence.Tolerances.defaults().freshnessHours()).isEqualTo(24);
        }
    }
}
