package com.lucilla.settlement.ledger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SignerProtocol#rejectionReason} for every v2 seat: which conditions each seat
 * may claim, what the venue must bring, and how the reserve model narrows the issuer.
 *
 * <p>The instrument→model registry is process-global, so every test here saves it, sets
 * a known map (CBTC attested, cETH on-chain, TEQ custodial) and restores it afterwards.
 */
class SignerProtocolSeatsTest {

    static final String ALL_SEATS = "[issuer, lender, venue, custodian, transfer-agent, operator]";

    Map<String, SignerProtocol.ReserveModel> saved;

    @BeforeEach
    void configureModels() {
        saved = SignerProtocol.reserveModels();
        SignerProtocol.configureReserveModels(Map.of(
                "CBTC", "attested", "cETH", "onchain-verifiable", "TEQ", "custodial"));
    }

    @AfterEach
    void restoreModels() {
        Map<String, String> back = new LinkedHashMap<>();
        saved.forEach((k, v) -> back.put(k, v.wire()));
        SignerProtocol.configureReserveModels(back);
    }

    static String reject(String role, List<String> checks, boolean lo, boolean hi) {
        return SignerProtocol.rejectionReason(role, checks, lo, hi);
    }

    static String reject(String role, List<String> checks, boolean lo, boolean hi, String instrument) {
        return SignerProtocol.rejectionReason(role, checks, lo, hi, instrument);
    }

    static List<String> names(SignerProtocol.Role r) {
        return r.conditions().stream().map(SignerProtocol.Condition::name).toList();
    }

    // ---- the published protocol -----------------------------------------------------

    @Test
    @DisplayName("v2 publishes six seats in the document's order, and stamps v2 on every reference")
    void sixSeatsAndTheVersion() {
        assertThat(SignerProtocol.VERSION).isEqualTo("SIGNER_PROTOCOL v2");
        assertThat(SignerProtocol.roles()).extracting(SignerProtocol.Role::key)
                .containsExactly("issuer", "lender", "venue", "custodian", "transfer-agent", "operator");
        assertThat(SignerProtocol.refFor("custodian")).isEqualTo("SIGNER_PROTOCOL v2 custodian");
        assertThat(SignerProtocol.refFor(" Transfer-Agent ")).isEqualTo("SIGNER_PROTOCOL v2 transfer-agent");
        assertThat(SignerProtocol.role(" Transfer-Agent ").key()).isEqualTo("transfer-agent");
        assertThat(SignerProtocol.role("auditor")).isNull();
        assertThat(SignerProtocol.role(null)).isNull();
    }

    @Test
    @DisplayName("every seat but the operator requires evidence; only the venue requires an observed range")
    void evidenceAndRangeRequirements() {
        for (SignerProtocol.Role r : SignerProtocol.roles()) {
            assertThat(r.requiresEvidence()).as("%s requires evidence", r.key()).isEqualTo(!r.key().equals("operator"));
            assertThat(r.requiresObservedRange()).as("%s requires a range", r.key()).isEqualTo(r.key().equals("venue"));
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(SignerProtocol.ReserveModel.class)
    @DisplayName("rolesFor(model) swaps only the issuer; every other seat is the same object under every model")
    void rolesForSwapsOnlyTheIssuer(SignerProtocol.ReserveModel model) {
        List<SignerProtocol.Role> roles = SignerProtocol.rolesFor(model);
        assertThat(roles).extracting(SignerProtocol.Role::key)
                .containsExactly("issuer", "lender", "venue", "custodian", "transfer-agent", "operator");
        assertThat(roles.get(0)).isSameAs(SignerProtocol.issuerFor(model));
        for (int i = 1; i < roles.size(); i++) {
            assertThat(roles.get(i)).isSameAs(SignerProtocol.roles().get(i));
        }
        List<String> issuer = names(SignerProtocol.issuerFor(model));
        if (model == SignerProtocol.ReserveModel.ATTESTED) {
            assertThat(issuer).containsExactly("attestor-quorum", "reserves-current",
                    "reserves-cover-supply", "redemption-queue-clear");
        } else {
            assertThat(issuer).containsExactly("reserves-current", "reserves-cover-supply", "redemption-queue-clear");
        }
        assertThat(names(SignerProtocol.roleFor("custodian", model)))
                .containsExactly("holdings-current", "holdings-cover-supply", "no-encumbrance");
        assertThat(names(SignerProtocol.roleFor("transfer-agent", model)))
                .containsExactly("shares-outstanding-reconciled", "fees-accrued");
    }

    @Test
    @DisplayName("the reserve-model registry: unknown wire names, unknown instruments and blanks all fall back to ATTESTED")
    void reserveModelRegistry() {
        assertThat(SignerProtocol.ReserveModel.fromWire("ONCHAIN-VERIFIABLE")).isEqualTo(SignerProtocol.ReserveModel.ONCHAIN_VERIFIABLE);
        assertThat(SignerProtocol.ReserveModel.fromWire("custodial")).isEqualTo(SignerProtocol.ReserveModel.CUSTODIAL);
        assertThat(SignerProtocol.ReserveModel.fromWire("something-else")).isEqualTo(SignerProtocol.ReserveModel.ATTESTED);
        assertThat(SignerProtocol.ReserveModel.fromWire(null)).isEqualTo(SignerProtocol.ReserveModel.ATTESTED);

        assertThat(SignerProtocol.reserveModelOf("cETH")).isEqualTo(SignerProtocol.ReserveModel.ONCHAIN_VERIFIABLE);
        assertThat(SignerProtocol.reserveModelOf("ceth")).isEqualTo(SignerProtocol.ReserveModel.ONCHAIN_VERIFIABLE);
        assertThat(SignerProtocol.reserveModelOf(" CETH ")).isEqualTo(SignerProtocol.ReserveModel.ONCHAIN_VERIFIABLE);
        assertThat(SignerProtocol.reserveModelOf("TEQ")).isEqualTo(SignerProtocol.ReserveModel.CUSTODIAL);
        assertThat(SignerProtocol.reserveModelOf("CBTC")).isEqualTo(SignerProtocol.ReserveModel.ATTESTED);
        assertThat(SignerProtocol.reserveModelOf("UNKNOWN")).isEqualTo(SignerProtocol.ReserveModel.ATTESTED);
        assertThat(SignerProtocol.reserveModelOf(null)).isEqualTo(SignerProtocol.ReserveModel.ATTESTED);
        assertThat(SignerProtocol.reserveModelOf("  ")).isEqualTo(SignerProtocol.ReserveModel.ATTESTED);
        assertThat(SignerProtocol.reserveModels()).containsOnlyKeys("CBTC", "CETH", "TEQ");

        assertThat(SignerProtocol.issuerFor(null)).isSameAs(SignerProtocol.issuerFor(SignerProtocol.ReserveModel.ATTESTED));
        assertThat(SignerProtocol.roleFor("auditor", SignerProtocol.ReserveModel.CUSTODIAL)).isNull();
    }

    // ---- each seat's own checklist stands ------------------------------------------

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"issuer", "lender", "custodian", "transfer-agent", "operator"})
    @DisplayName("a non-venue seat naming its whole checklist, with no range, stands")
    void aSeatsOwnChecklistStands(String seat) {
        assertThat(reject(seat, names(SignerProtocol.role(seat)), false, false)).isNull();
        // and any single one of them
        for (String c : names(SignerProtocol.role(seat))) {
            assertThat(reject(seat, List.of(c), false, false)).as("%s alone for %s", c, seat).isNull();
        }
    }

    @Test
    @DisplayName("the venue's traded checklist stands with a full range; its no-prints claim stands alone without one")
    void theVenuesOwnChecklistStands() {
        assertThat(reject("venue", List.of("traded-range", "spread-within-tolerance", "sufficient-volume"), true, true))
                .isNull();
        assertThat(reject("venue", List.of("traded-range"), true, true)).isNull();
        assertThat(reject("venue", List.of("no-prints-attested"), false, false)).isNull();
    }

    // ---- seat naming another seat's condition --------------------------------------

    @ParameterizedTest(name = "{0} claiming {1}")
    @CsvSource({
            "issuer,         book-acceptance,               '[attestor-quorum, reserves-current, reserves-cover-supply, redemption-queue-clear]'",
            "issuer,         holdings-current,              '[attestor-quorum, reserves-current, reserves-cover-supply, redemption-queue-clear]'",
            "lender,         attestor-quorum,               '[independent-mark-within-tolerance, liquidations-consistent, book-acceptance]'",
            "lender,         shares-outstanding-reconciled, '[independent-mark-within-tolerance, liquidations-consistent, book-acceptance]'",
            "venue,          reserves-current,              '[traded-range, spread-within-tolerance, sufficient-volume, no-prints-attested]'",
            "custodian,      traded-range,                  '[holdings-current, holdings-cover-supply, no-encumbrance]'",
            "custodian,      shares-outstanding-reconciled, '[holdings-current, holdings-cover-supply, no-encumbrance]'",
            "custodian,      reserves-cover-supply,         '[holdings-current, holdings-cover-supply, no-encumbrance]'",
            "transfer-agent, no-encumbrance,                '[shares-outstanding-reconciled, fees-accrued]'",
            "transfer-agent, holdings-cover-supply,         '[shares-outstanding-reconciled, fees-accrued]'",
            "operator,       fees-accrued,                  '[inputs-published, composition-reconciled]'",
            "operator,       independent-mark-within-tolerance, '[inputs-published, composition-reconciled]'",
    })
    @DisplayName("a seat naming another seat's condition is refused, and told exactly what it may claim")
    void aSeatNamingAnotherSeatsConditionIsRefused(String seat, String foreign, String expectedList) {
        boolean venue = seat.equals("venue");
        assertThat(reject(seat, List.of(foreign), venue, venue))
                .isEqualTo("condition '" + foreign + "' is not one the " + seat + " seat verifies; expected any of " + expectedList);
        // A foreign condition hidden among genuine ones is still refused — the first bad one names itself.
        List<String> own = names(SignerProtocol.role(seat));
        assertThat(reject(seat, List.of(own.get(0), foreign), venue, venue))
                .startsWith("condition '" + foreign + "' is not one the " + seat + " seat verifies");
    }

    // ---- unknown / empty / duplicate --------------------------------------------------

    @Test
    @DisplayName("an unknown seat is refused and told all six seats")
    void unknownRole() {
        assertThat(reject("auditor", List.of("book-acceptance"), false, false))
                .isEqualTo("unknown signer role 'auditor'; expected one of " + ALL_SEATS);
        assertThat(reject("", List.of("book-acceptance"), false, false))
                .isEqualTo("unknown signer role ''; expected one of " + ALL_SEATS);
        assertThat(reject(null, List.of("book-acceptance"), false, false))
                .isEqualTo("unknown signer role 'null'; expected one of " + ALL_SEATS);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"issuer", "lender", "venue", "custodian", "transfer-agent", "operator"})
    @DisplayName("an empty or null checklist is a vote, not an attestation, for every seat")
    void emptyChecklist(String seat) {
        boolean venue = seat.equals("venue");
        assertThat(reject(seat, List.of(), venue, venue)).isEqualTo("a signer must name at least one condition it verified");
        assertThat(reject(seat, null, venue, venue)).isEqualTo("a signer must name at least one condition it verified");
    }

    @Test
    @DisplayName("a condition named twice is refused, whitespace notwithstanding; a blank entry is not a condition")
    void duplicateAndBlankConditions() {
        assertThat(reject("custodian", List.of("holdings-current", "holdings-current"), false, false))
                .isEqualTo("condition 'holdings-current' was named twice");
        assertThat(reject("custodian", List.of("holdings-current", " holdings-current "), false, false))
                .isEqualTo("condition 'holdings-current' was named twice");
        assertThat(reject("transfer-agent", List.of("fees-accrued", "shares-outstanding-reconciled", "fees-accrued"), false, false))
                .isEqualTo("condition 'fees-accrued' was named twice");
        assertThat(reject("venue", List.of("traded-range", "traded-range"), true, true))
                .isEqualTo("condition 'traded-range' was named twice");
        assertThat(reject("issuer", Arrays.asList("attestor-quorum", null), false, false))
                .startsWith("condition '' is not one the issuer seat verifies");
        assertThat(reject("issuer", List.of(""), false, false))
                .startsWith("condition '' is not one the issuer seat verifies");
    }

    // ---- the venue's range -------------------------------------------------------------

    static final String NEEDS_RANGE = "the venue seat must supply both observedLow and observedHigh — "
            + "the traded range is the only assertion the ledger checks. "
            + "If your book showed no trades in the window, claim 'no-prints-attested' instead";

    @Test
    @DisplayName("a venue with half a range is refused before the ledger sees it")
    void venueWithHalfARange() {
        assertThat(reject("venue", List.of("traded-range"), true, false)).isEqualTo(NEEDS_RANGE);
        assertThat(reject("venue", List.of("traded-range"), false, true)).isEqualTo(NEEDS_RANGE);
        assertThat(reject("venue", List.of("sufficient-volume"), true, false)).isEqualTo(NEEDS_RANGE);
    }

    @Test
    @DisplayName("a venue with no range and no no-prints-attested is refused: it is attesting nothing observable")
    void venueWithNoRangeAndNoAbsence() {
        assertThat(reject("venue", List.of("traded-range"), false, false)).isEqualTo(NEEDS_RANGE);
        assertThat(reject("venue", List.of("spread-within-tolerance", "sufficient-volume"), false, false)).isEqualTo(NEEDS_RANGE);
    }

    @Test
    @DisplayName("no-prints-attested and traded-range are mutually exclusive, whichever way round and with or without a range")
    void venueCannotClaimBothPrintsAndNoPrints() {
        String both = "a venue cannot claim both 'traded-range' and 'no-prints-attested' — "
                + "either the book traded in the window or it did not";
        assertThat(reject("venue", List.of("traded-range", "no-prints-attested"), false, false)).isEqualTo(both);
        assertThat(reject("venue", List.of("no-prints-attested", "traded-range"), false, false)).isEqualTo(both);
        assertThat(reject("venue", List.of("traded-range", "no-prints-attested"), true, true)).isEqualTo(both);
    }

    @Test
    @DisplayName("no-prints-attested is the whole claim: volume or spread cannot ride along")
    void noPrintsExcludesVolumeAndSpread() {
        String whole = "'no-prints-attested' is the whole claim when nothing traded; "
                + "volume and spread conditions cannot also be asserted";
        assertThat(reject("venue", List.of("no-prints-attested", "sufficient-volume"), false, false)).isEqualTo(whole);
        assertThat(reject("venue", List.of("spread-within-tolerance", "no-prints-attested"), false, false)).isEqualTo(whole);
    }

    @Test
    @DisplayName("no-prints-attested with any observed range at all is a contradiction")
    void noPrintsWithARange() {
        String contradiction = "'no-prints-attested' means the window had no trades; do not also send an observed range";
        assertThat(reject("venue", List.of("no-prints-attested"), true, true)).isEqualTo(contradiction);
        assertThat(reject("venue", List.of("no-prints-attested"), true, false)).isEqualTo(contradiction);
        assertThat(reject("venue", List.of("no-prints-attested"), false, true)).isEqualTo(contradiction);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"issuer", "lender", "custodian", "transfer-agent", "operator"})
    @DisplayName("only the venue supplies an observed range — a full or half range from any other seat is refused")
    void nonVenueWithARange(String seat) {
        String first = names(SignerProtocol.role(seat)).get(0);
        assertThat(reject(seat, List.of(first), true, true)).isEqualTo("only the venue seat supplies an observed range");
        assertThat(reject(seat, List.of(first), true, false)).isEqualTo("only the venue seat supplies an observed range");
        assertThat(reject(seat, List.of(first), false, true)).isEqualTo("only the venue seat supplies an observed range");
    }

    // ---- the issuer under each reserve model ---------------------------------------------

    static final String ISSUER_WITHOUT_QUORUM =
            "condition 'attestor-quorum' is not one the issuer seat verifies; expected any of "
                    + "[reserves-current, reserves-cover-supply, redemption-queue-clear]";

    @Test
    @DisplayName("an issuer under onchain-verifiable asked for attestor-quorum is refused: not a condition of this seat under this model")
    void issuerUnderOnchainVerifiableCannotClaimQuorum() {
        assertThat(reject("issuer", List.of("attestor-quorum"), false, false, "cETH")).isEqualTo(ISSUER_WITHOUT_QUORUM);
        assertThat(reject("issuer", List.of("attestor-quorum"), false, false, "ceth")).isEqualTo(ISSUER_WITHOUT_QUORUM);
        assertThat(reject("issuer", List.of("reserves-current", "attestor-quorum"), false, false, "cETH"))
                .isEqualTo(ISSUER_WITHOUT_QUORUM);
        // the three it CAN prove stand
        assertThat(reject("issuer", List.of("reserves-current", "reserves-cover-supply", "redemption-queue-clear"),
                false, false, "cETH")).isNull();
    }

    @Test
    @DisplayName("an issuer under custodial likewise has no attestor quorum to report")
    void issuerUnderCustodialCannotClaimQuorum() {
        assertThat(reject("issuer", List.of("attestor-quorum"), false, false, "TEQ")).isEqualTo(ISSUER_WITHOUT_QUORUM);
        assertThat(reject("issuer", List.of("reserves-current", "reserves-cover-supply", "redemption-queue-clear"),
                false, false, "TEQ")).isNull();
    }

    @Test
    @DisplayName("an issuer under attested — configured, unconfigured, or no instrument at all — may claim the quorum")
    void issuerUnderAttestedMayClaimQuorum() {
        assertThat(reject("issuer", List.of("attestor-quorum"), false, false, "CBTC")).isNull();
        assertThat(reject("issuer", List.of("attestor-quorum"), false, false, "NEVER-CONFIGURED")).isNull();
        assertThat(reject("issuer", List.of("attestor-quorum"), false, false, null)).isNull();
        assertThat(reject("issuer", List.of("attestor-quorum"), false, false)).isNull();
    }

    @Test
    @DisplayName("the reserve model narrows only the issuer: every other seat is held to the same list on every instrument")
    void modelDoesNotChangeOtherSeats() {
        for (String instrument : Arrays.asList("CBTC", "cETH", "TEQ", null)) {
            assertThat(reject("custodian", List.of("holdings-current", "holdings-cover-supply", "no-encumbrance"),
                    false, false, instrument)).as("custodian on %s", instrument).isNull();
            assertThat(reject("transfer-agent", List.of("shares-outstanding-reconciled", "fees-accrued"),
                    false, false, instrument)).as("transfer-agent on %s", instrument).isNull();
            assertThat(reject("lender", List.of("book-acceptance"), false, false, instrument)).isNull();
            assertThat(reject("venue", List.of("no-prints-attested"), false, false, instrument)).isNull();
            assertThat(reject("lender", List.of("attestor-quorum"), false, false, instrument))
                    .startsWith("condition 'attestor-quorum' is not one the lender seat verifies");
        }
    }

    // ---- custodian / transfer-agent conditions belong to those seats only -------------------

    @Test
    @DisplayName("custodian and transfer-agent conditions are accepted only for those seats")
    void custodianAndTransferAgentConditionsAreTheirs() {
        assertThat(reject("custodian", List.of("holdings-current", "holdings-cover-supply", "no-encumbrance"), false, false)).isNull();
        assertThat(reject("transfer-agent", List.of("shares-outstanding-reconciled", "fees-accrued"), false, false)).isNull();

        for (String other : List.of("issuer", "lender", "venue", "transfer-agent", "operator")) {
            boolean venue = other.equals("venue");
            for (String c : List.of("holdings-current", "holdings-cover-supply", "no-encumbrance")) {
                assertThat(reject(other, List.of(c), venue, venue)).as("%s claiming %s", other, c)
                        .startsWith("condition '" + c + "' is not one the " + other + " seat verifies");
            }
        }
        for (String other : List.of("issuer", "lender", "venue", "custodian", "operator")) {
            boolean venue = other.equals("venue");
            for (String c : List.of("shares-outstanding-reconciled", "fees-accrued")) {
                assertThat(reject(other, List.of(c), venue, venue)).as("%s claiming %s", other, c)
                        .startsWith("condition '" + c + "' is not one the " + other + " seat verifies");
            }
        }
    }
}
