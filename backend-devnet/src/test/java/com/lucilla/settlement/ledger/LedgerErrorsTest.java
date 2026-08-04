package com.lucilla.settlement.ledger;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests for {@link LedgerErrors} — the classification of a ledger failure.
 * No ledger, no Spring, no network: every assertion is against a hand-built
 * {@link StatusRuntimeException} carrying a description in the shape Canton actually
 * returns.
 *
 * <p>These matter more than they look. The whole value of the error path is that a
 * {@code PERMISSION_DENIED} arrives with the handles a node operator can search for and
 * that a Daml {@code assertMsg} arrives as the sentence the model wrote — if the parsing
 * silently stopped matching, both would degrade to unreadable gRPC scaffolding at
 * exactly the wrong moment, and nothing else would notice.
 */
class LedgerErrorsTest {

    /** The shape Canton returns for an authorization refusal: redacted, with a handle. */
    private static final String PERMISSION_DENIED_DESC =
            "PERMISSION_DENIED(7,9f3ab21c): An error occurred. Please contact the operator "
                    + "and inquire about the request 9f3ab21c-4d55-4a1e-9a77-1f0c2b8de001";

    /** The shape Canton returns when the MODEL rejects a command. */
    private static final String INTERPRETATION_DESC =
            "DAML_INTERPRETATION_ERROR(9,0a1b2c3d): Interpretation error: Error: Unhandled "
                    + "Daml exception: DA.Exception.AssertionFailed:AssertionFailed@f1e2d3c4"
                    + "{ message = \"committed holding is the wrong instrument\" }";

    private static StatusRuntimeException grpc(Status.Code code, String description) {
        return new StatusRuntimeException(code.toStatus().withDescription(description));
    }

    // ---- PERMISSION_DENIED: the one Canton refuses to explain ---------------

    @Test
    void permissionDenied_keepsTheHandlesAnOperatorCanSearchFor() {
        LedgerErrors.Failure f = LedgerErrors.of(
                grpc(Status.Code.PERMISSION_DENIED, PERMISSION_DENIED_DESC));

        assertThat(f.code()).isEqualTo(Status.Code.PERMISSION_DENIED);
        assertThat(f.cantonCode()).isEqualTo("PERMISSION_DENIED");
        assertThat(f.permissionDenied()).isTrue();
        assertThat(f.businessRejection()).isFalse();
        // The correlation id is the ONLY thing that links this to the participant's log.
        assertThat(f.correlationId()).isEqualTo("9f3ab21c");
        // And the hint has to say where to look, not merely that it failed.
        assertThat(f.hint()).contains("PARTICIPANT'S LOG");
        assertThat(f.hint()).contains("actAs");
    }

    @Test
    void permissionDenied_userMessageCarriesTheCommandId() {
        LedgerErrors.Failure f = LedgerErrors.of(
                grpc(Status.Code.PERMISSION_DENIED, PERMISSION_DENIED_DESC));

        String msg = LedgerErrors.userMessage(f, "cmd-1234");

        assertThat(msg).contains("PERMISSION_DENIED");
        assertThat(msg).contains("command id cmd-1234");
        assertThat(msg).contains("correlation id 9f3ab21c");
        assertThat(msg).contains("node operator");
    }

    // ---- Daml rejections: the model speaking, and it must not be mangled -----

    @Test
    void damlAssertion_isExtractedVerbatimAndClassifiedAsABusinessRejection() {
        LedgerErrors.Failure f = LedgerErrors.of(
                grpc(Status.Code.INVALID_ARGUMENT, INTERPRETATION_DESC));

        assertThat(f.damlMessage()).isEqualTo("committed holding is the wrong instrument");
        assertThat(f.businessRejection()).isTrue();
        // The user sees the model's own sentence — no gRPC scaffolding at all.
        assertThat(LedgerErrors.userMessage(f, "cmd-1"))
                .isEqualTo("committed holding is the wrong instrument");
    }

    @Test
    void damlMessage_handlesTheOtherRealModelRejections() {
        assertThat(LedgerErrors.damlMessageOf(
                "DAML_INTERPRETATION_ERROR(9,aa): … { message = \"the venue cannot clear "
                        + "orders once the book is sealed\" }"))
                .isEqualTo("the venue cannot clear orders once the book is sealed");

        assertThat(LedgerErrors.damlMessageOf(
                "DAML_INTERPRETATION_ERROR(9,bb): … { message = \"discovered price is outside "
                        + "the venue's price collar\" }"))
                .isEqualTo("discovered price is outside the venue's price collar");

        // A non-Daml failure must NOT be reported as one.
        assertThat(LedgerErrors.damlMessageOf(PERMISSION_DENIED_DESC)).isNull();
        assertThat(LedgerErrors.damlMessageOf(null)).isNull();
    }

    // ---- The codes that mean different things --------------------------------

    @Test
    void contractNotFound_saysTheIdIsStaleRatherThanMissing() {
        LedgerErrors.Failure f = LedgerErrors.of(grpc(Status.Code.NOT_FOUND,
                "CONTRACT_NOT_FOUND(11,7c1d): Contract could not be found with id 00abc"));

        assertThat(f.cantonCode()).isEqualTo("CONTRACT_NOT_FOUND");
        assertThat(f.codeLabel()).isEqualTo("CONTRACT_NOT_FOUND");
        assertThat(f.hint()).contains("STALE");
    }

    @Test
    void eachCodeGetsItsOwnOperationalHint() {
        assertThat(LedgerErrors.hintFor(Status.Code.UNAVAILABLE, null))
                .contains("unreachable");
        assertThat(LedgerErrors.hintFor(Status.Code.ABORTED, null))
                .contains("LOST A RACE");
        assertThat(LedgerErrors.hintFor(Status.Code.UNAUTHENTICATED, null))
                .contains("expired");
        assertThat(LedgerErrors.hintFor(Status.Code.INVALID_ARGUMENT, null))
                .contains("malformed");
        // Distinct advice, not one recycled sentence.
        assertThat(LedgerErrors.hintFor(Status.Code.UNAVAILABLE, null))
                .isNotEqualTo(LedgerErrors.hintFor(Status.Code.ABORTED, null));
    }

    // ---- Looking THROUGH the wrappers ---------------------------------------

    @Test
    void aWrappedStatusRuntimeExceptionIsStillClassified() {
        Throwable wrapped = new RuntimeException("rxjava wrapper",
                new IllegalStateException("another layer",
                        grpc(Status.Code.PERMISSION_DENIED, PERMISSION_DENIED_DESC)));

        LedgerErrors.Failure f = LedgerErrors.of(wrapped);

        assertThat(f.code()).isEqualTo(Status.Code.PERMISSION_DENIED);
        assertThat(f.correlationId()).isEqualTo("9f3ab21c");
    }

    @Test
    void aTimeoutIsNotPretendingToBeAGrpcStatus() {
        LedgerErrors.Failure f = LedgerErrors.of(
                new RuntimeException("timed out", new TimeoutException("30s")));

        assertThat(f.code()).isNull();
        assertThat(f.cantonCode()).isEqualTo("CLIENT_TIMEOUT");
        assertThat(f.hint()).contains("MAY still have been accepted");
    }

    // ---- Describing what was submitted --------------------------------------

    @Test
    void describe_namesTheTemplateAndChoiceActuallyPutOnTheWire() {
        assertThat(LedgerErrors.describe(LedgerCommands.createHolding(
                "Issuer", "USDC", "Alice", new BigDecimal("100.0"))))
                .isEqualTo("create Holding:Holding");

        assertThat(LedgerErrors.describe(LedgerCommands.settleAgreement("agreement#1")))
                .startsWith("exercise Settlement:DvPAgreement.Settle on agreement#1");
    }

    @Test
    void describeArgs_rendersTheArgumentsAndNeverBlowsUp() {
        String args = LedgerErrors.describeArgs(LedgerCommands.createHolding(
                "Issuer", "USDC", "Alice", new BigDecimal("100.0")));

        assertThat(args).contains("Alice");
        assertThat(LedgerErrors.describe(null)).isEqualTo("(no commands)");
        assertThat(LedgerErrors.describeArgs(null)).isEmpty();
    }

    @Test
    void truncate_marksWhatItCutRatherThanElidingSilently() {
        String long1 = "x".repeat(5000);
        assertThat(LedgerErrors.truncate(long1)).endsWith("…(truncated)");
        assertThat(LedgerErrors.truncate("short")).isEqualTo("short");
        assertThat(LedgerErrors.truncate(null)).isEmpty();
    }
}
