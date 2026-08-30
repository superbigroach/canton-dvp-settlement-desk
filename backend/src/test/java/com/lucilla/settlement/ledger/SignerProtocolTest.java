package com.lucilla.settlement.ledger;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SignerProtocol}, the layer between a document nothing reads and a
 * ledger field that accepts anything.
 *
 * <p>{@code SignerCheck.checksPassed} is {@code [Text]} on-chain by design — the protocol
 * gains conditions faster than a DAR can be upgraded. The cost of that choice is that the
 * ledger alone cannot tell a real attestation from {@code ["whatever"]}, and both look
 * identical on the record forever. These tests are the evidence that the edge closes that
 * gap: a condition must belong to the seat claiming it, and the venue must bring the one
 * assertion that is checked against reality.
 *
 * <p>The load-bearing assertions are the negative ones. A protocol that accepts everything
 * is prose, not a rule.
 */
class SignerProtocolTest {

    @Test
    void everyRoleIsPublishedWithConditionsAndAStatementOfWhatItUniquelyKnows() {
        // The UI renders from this, so an empty or unexplained seat would ship a checkbox
        // list with nothing in it and a signer with no way to know what they are claiming.
        assertThat(SignerProtocol.roles())
                .extracting(SignerProtocol.Role::key)
                .containsExactly("issuer", "lender", "venue", "operator");

        for (SignerProtocol.Role r : SignerProtocol.roles()) {
            assertThat(r.conditions()).as("conditions for %s", r.key()).isNotEmpty();
            assertThat(r.uniquelyKnows()).as("what %s uniquely knows", r.key()).isNotBlank();
            for (SignerProtocol.Condition c : r.conditions()) {
                assertThat(c.name()).isNotBlank();
                assertThat(c.passesWhen()).as("condition %s must say when it passes", c.name())
                        .isNotBlank();
            }
        }
    }

    @Test
    void aValidLenderAttestationStands() {
        assertThat(SignerProtocol.rejectionReason("lender",
                List.of("independent-mark-within-tolerance", "book-acceptance"), false, false))
                .isNull();
    }

    @Test
    void aConditionFromAnotherSeatIsRefused() {
        // THE POINT OF THE CLASS. `attestor-quorum` is a real condition — it is simply not
        // one a lender is in any position to verify. Without this check, a signer could
        // claim the issuer's evidence and the record would carry it as though it were true.
        String why = SignerProtocol.rejectionReason("lender", List.of("attestor-quorum"), false, false);
        assertThat(why).contains("attestor-quorum").contains("lender");
    }

    @Test
    void anUnknownRoleIsRefusedAndSaysWhatIsExpected() {
        // A signer told "unknown role" plus the list fixes it in seconds. One told
        // "invalid request" opens a ticket.
        String why = SignerProtocol.rejectionReason("auditor", List.of("book-acceptance"), false, false);
        assertThat(why).contains("auditor").contains("issuer").contains("venue");
    }

    @Test
    void anEmptyChecklistIsAVoteAndIsRefused() {
        assertThat(SignerProtocol.rejectionReason("issuer", List.of(), false, false))
                .contains("at least one condition");
        assertThat(SignerProtocol.rejectionReason("issuer", null, false, false))
                .contains("at least one condition");
    }

    @Test
    void aRepeatedConditionIsRefused() {
        assertThat(SignerProtocol.rejectionReason("issuer",
                List.of("attestor-quorum", "attestor-quorum"), false, false))
                .contains("twice");
    }

    @Test
    void theVenueMustBringItsTradedRange() {
        // The venue's range is the only claim the ledger checks against reality. A venue
        // attesting without one is quietly signing as though it were a seat with nothing
        // to observe, which is the seat's entire purpose discarded.
        assertThat(SignerProtocol.rejectionReason("venue", List.of("traded-range"), false, false))
                .contains("observedLow").contains("observedHigh");
        assertThat(SignerProtocol.rejectionReason("venue", List.of("traded-range"), true, false))
                .isNotNull();
        assertThat(SignerProtocol.rejectionReason("venue", List.of("traded-range"), true, true))
                .isNull();
    }

    @Test
    void onlyTheVenueSuppliesARange() {
        // An issuer sending a range has either mislabelled its seat or is reporting a
        // market it does not observe. Either way the attestation is not what it claims.
        assertThat(SignerProtocol.rejectionReason("issuer", List.of("attestor-quorum"), true, true))
                .contains("only the venue");
    }

    @Test
    void theProtocolReferenceIsStampedPerSeatAndCarriesTheVersion() {
        // A fixing is read under the version in force when it was struck, so the stamp is
        // what keeps an old attestation interpretable after the document moves on.
        assertThat(SignerProtocol.refFor("venue"))
                .startsWith(SignerProtocol.VERSION)
                .endsWith("venue");
    }
}
