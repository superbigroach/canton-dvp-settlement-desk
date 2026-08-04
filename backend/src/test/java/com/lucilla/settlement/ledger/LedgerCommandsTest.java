package com.lucilla.settlement.ledger;

import com.daml.ledger.javaapi.data.Command;
import com.daml.ledger.javaapi.data.CreateCommand;
import com.daml.ledger.javaapi.data.ExerciseCommand;
import com.lucilla.settlement.model.marketonclose.Side;
import com.lucilla.settlement.model.marketonclose.SubmitOrder;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure unit tests for {@link LedgerCommands} — the DTO -> Ledger API command
 * mapping. No ledger, no Spring, no network: every assertion inspects the
 * command object the mapping produced (its template id, choice name, target
 * contract id, argument shape). This is the TDD core of the backend and runs in
 * the default {@code ./gradlew build}.
 */
class LedgerCommandsTest {

    private static Command only(com.daml.ledger.javaapi.data.codegen.Update<?> update) {
        List<Command> cmds = update.commands();
        assertThat(cmds).hasSize(1);
        return cmds.get(0);
    }

    private static CreateCommand asCreate(com.daml.ledger.javaapi.data.codegen.Update<?> update) {
        return only(update).asCreateCommand().orElseThrow();
    }

    private static ExerciseCommand asExercise(com.daml.ledger.javaapi.data.codegen.Update<?> update) {
        return only(update).asExerciseCommand().orElseThrow();
    }

    // ---- Creates ----------------------------------------------------------

    @Test
    void createInstrument_buildsInstrumentCreate() {
        var cmd = asCreate(LedgerCommands.createInstrument(
                "Issuer", "Issuer", "cETH", "1", "CryptoWrapped",
                "Wrapped ETH", Optional.of(new BigDecimal("2500.0"))));

        assertThat(cmd.getTemplateId().getModuleName()).isEqualTo("Instrument");
        assertThat(cmd.getTemplateId().getEntityName()).isEqualTo("Instrument");
        // issuer, depository, id, version, kind, description, referencePrice
        assertThat(cmd.getCreateArguments().getFields()).hasSize(7);
    }

    @Test
    void createHolding_buildsHoldingCreateWithFiveFields() {
        var cmd = asCreate(LedgerCommands.createHolding(
                "Issuer", "USD", "Alice", new BigDecimal("2550.0")));

        assertThat(cmd.getTemplateId().getEntityName()).isEqualTo("Holding");
        // issuer, instrumentId, owner, amount, disclosedTo
        assertThat(cmd.getCreateArguments().getFields()).hasSize(5);
    }

    @Test
    void createDvPProposal_buildsProposalCreate() {
        var cmd = asCreate(LedgerCommands.createDvPProposal(
                "Bob", "Alice", "Auditor",
                "asset#1", "cash#1", "DEMO:AAPL", new BigDecimal("10.0"),
                "USD", new BigDecimal("2550.0")));

        assertThat(cmd.getTemplateId().getEntityName()).isEqualTo("DvPProposal");
        assertThat(cmd.getCreateArguments().getFields()).hasSize(9);
    }

    @Test
    void createAuction_buildsClosingAuctionCreate() {
        var cmd = asCreate(LedgerCommands.createAuction(
                "Venue", "Auditor", "DEMO:AAPL", "USD",
                "Close", new BigDecimal("255.0"), List.of("Alice", "Bob"),
                Optional.of("Bank"), Optional.empty()));

        assertThat(cmd.getTemplateId().getEntityName()).isEqualTo("ClosingAuction");
        // operator, auditor, instrumentId, cashInstrument, session, referencePrice,
        // participants, liquidityProvider, fixingRef, submittedCount, cancelledCount, isOpen
        assertThat(cmd.getCreateArguments().getFields()).hasSize(12);
    }

    /**
     * COMPLETE-ORDER COMMITMENT: a brand-new book starts with both counters at zero.
     * These are ledger-maintained (SubmitOrder / WithdrawOrder / ClearOrder move them)
     * and RunClose asserts the supplied book equals their difference, so opening an
     * auction with anything other than 0/0 would let the venue pre-cook the count.
     */
    @Test
    void createAuction_startsBookCountersAtZero() {
        var cmd = asCreate(LedgerCommands.createAuction(
                "Venue", "Auditor", "DEMO:AAPL", "USD",
                "Close", new BigDecimal("255.0"), List.of("Alice", "Bob"),
                Optional.empty(), Optional.empty()));

        var fields = cmd.getCreateArguments().getFieldsMap();
        assertThat(fields.get("submittedCount").asInt64().orElseThrow().getValue()).isZero();
        assertThat(fields.get("cancelledCount").asInt64().orElseThrow().getValue()).isZero();
    }

    /**
     * A trader's withdrawal must go THROUGH the auction, not through
     * {@code SealedOrder.Cancel}: only the auction choice books the cancellation into
     * {@code cancelledCount} in the same transaction, keeping the count and the live
     * book in step so a later RunClose can still satisfy its completeness assertion.
     */
    @Test
    void withdrawOrder_exercisesWithdrawOrderOnTheAuction() {
        var cmd = asExercise(LedgerCommands.withdrawOrder("auction#1", "Alice", "order#9"));
        assertThat(cmd.getChoice()).isEqualTo("WithdrawOrder");
        assertThat(cmd.getContractId()).isEqualTo("auction#1");
        assertThat(cmd.getTemplateId().getEntityName()).isEqualTo("ClosingAuction");
    }

    /** The venue's clear likewise routes through the auction so cancelledCount moves with it. */
    @Test
    void clearOrder_exercisesClearOrderOnTheAuction() {
        var cmd = asExercise(LedgerCommands.clearOrder("auction#1", "order#9"));
        assertThat(cmd.getChoice()).isEqualTo("ClearOrder");
        assertThat(cmd.getContractId()).isEqualTo("auction#1");
        assertThat(cmd.getTemplateId().getEntityName()).isEqualTo("ClosingAuction");
    }

    @Test
    void publishImbalance_exercisesPublishImbalanceOnGivenAuction() {
        var cmd = asExercise(LedgerCommands.publishImbalance(
                "auction#1", List.of("order#1", "order#2"), "mandate#7"));
        assertThat(cmd.getChoice()).isEqualTo("PublishImbalance");
        assertThat(cmd.getContractId()).isEqualTo("auction#1");
        assertThat(cmd.getTemplateId().getEntityName()).isEqualTo("ClosingAuction");
    }

    /**
     * THE GATE TRAVELS WITH THE CALL. Imbalance disclosure is no longer a standing
     * privilege of a field on the auction — it demands a live {@code LiquidityMandate},
     * and the mandate's contract id has to reach the ledger in the choice argument or
     * the whole obligation model is decorative. Assert the argument carries it, not
     * merely that the choice name is right.
     */
    @Test
    void publishImbalance_carriesTheMandateContractIdInTheChoiceArgument() {
        var cmd = asExercise(LedgerCommands.publishImbalance(
                "auction#1", List.of("order#1"), "mandate#7"));
        var arg = cmd.getChoiceArgument().asRecord().orElseThrow().getFieldsMap();
        assertThat(arg.get("mandateCid").asContractId().orElseThrow().getValue())
                .isEqualTo("mandate#7");
        assertThat(arg.get("restingOrders").asList().orElseThrow().toList(v -> v)).hasSize(1);
    }

    /**
     * The seat is POSTED, not awarded: terms are created with EMPTY {@code accepted}
     * and {@code barred} lists. Those are ledger facts only {@code AcceptTerms} and
     * {@code BarProvider} may move — a client that could seed them could pre-bar a
     * competitor out of the offer before anyone had a chance to take it.
     */
    @Test
    void postMandateTerms_createsAnOpenOfferWithNobodyAcceptedOrBarred() {
        var cmd = asCreate(LedgerCommands.postMandateTerms(
                "Venue", "Auditor", "DEMO:cETH", "USDC", "Close",
                new java.math.BigDecimal("2400"), new java.math.BigDecimal("5"), 200L,
                java.time.Instant.parse("2030-01-01T00:00:00Z"),
                List.of("Bank", "Carol", "Bank")));
        assertThat(cmd.getTemplateId().getEntityName()).isEqualTo("MandateTerms");
        var fields = cmd.getCreateArguments().getFieldsMap();
        assertThat(fields.get("accepted").asList().orElseThrow().toList(v -> v)).isEmpty();
        assertThat(fields.get("barred").asList().orElseThrow().toList(v -> v)).isEmpty();
        // A duplicated party would trip the template's own `ensure`; dedupe here so the
        // caller's roster does not have to be pre-cleaned.
        assertThat(fields.get("eligible").asList().orElseThrow().toList(v -> v)).hasSize(2);
    }

    /** Taking the seat is exercised on the TERMS, by the provider — never by the venue. */
    @Test
    void acceptMandateTerms_exercisesAcceptTermsOnTheOffer() {
        var cmd = asExercise(LedgerCommands.acceptMandateTerms("terms#1", "Bank"));
        assertThat(cmd.getChoice()).isEqualTo("AcceptTerms");
        assertThat(cmd.getContractId()).isEqualTo("terms#1");
        assertThat(cmd.getTemplateId().getEntityName()).isEqualTo("MandateTerms");
        assertThat(cmd.getChoiceArgument().asRecord().orElseThrow().getFieldsMap()
                .get("provider").asParty().orElseThrow().getValue()).isEqualTo("Bank");
    }

    @Test
    void imbalanceDisclosureTemplateId_namesTheTemplate() {
        assertThat(LedgerCommands.imbalanceDisclosureTemplateId().getEntityName())
                .isEqualTo("ImbalanceDisclosure");
    }

    // ---- Exercises: choice name + target contract id round-trip -----------

    @Test
    void acceptProposal_exercisesAcceptOnGivenProposal() {
        var cmd = asExercise(LedgerCommands.acceptProposal("proposal#42"));
        assertThat(cmd.getChoice()).isEqualTo("Accept");
        assertThat(cmd.getContractId()).isEqualTo("proposal#42");
        assertThat(cmd.getTemplateId().getEntityName()).isEqualTo("DvPProposal");
    }

    @Test
    void settleAgreement_exercisesSettleOnGivenAgreement() {
        var cmd = asExercise(LedgerCommands.settleAgreement("agreement#7"));
        assertThat(cmd.getChoice()).isEqualTo("Settle");
        assertThat(cmd.getContractId()).isEqualTo("agreement#7");
        assertThat(cmd.getTemplateId().getEntityName()).isEqualTo("DvPAgreement");
    }

    @Test
    void submitOrder_exercisesSubmitOrderOnGivenAuction() {
        var cmd = asExercise(LedgerCommands.submitOrder(
                "auction#1", "Alice", Side.BUY,
                new BigDecimal("10.0"), Optional.of(new BigDecimal("260.0")), "cash#1"));
        assertThat(cmd.getChoice()).isEqualTo("SubmitOrder");
        assertThat(cmd.getContractId()).isEqualTo("auction#1");
        assertThat(cmd.getTemplateId().getEntityName()).isEqualTo("ClosingAuction");
    }

    /** An UNPRICED market-on-close order: the limit is absent, and that is legal. */
    @Test
    void submitOrder_acceptsAnUnpricedMarketOnCloseOrder() {
        var cmd = asExercise(LedgerCommands.submitOrder(
                "auction#1", "Alice", Side.BUY,
                new BigDecimal("10.0"), Optional.empty(), "cash#1"));
        assertThat(cmd.getChoice()).isEqualTo("SubmitOrder");
        var arg = SubmitOrder.valueDecoder().decode(cmd.getChoiceArgument());
        assertThat(arg.limitPrice).isEmpty();
    }

    // ---- order type: an absent limit is MARKET, never a rejection ----------

    @Test
    void orderType_absentDiscriminatorInfersFromTheLimit() {
        // No limit and no discriminator -> an unpriced market-on-close order.
        assertThat(LedgerCommands.orderType(null, null)).isEmpty();
        assertThat(LedgerCommands.orderType("  ", null)).isEmpty();
        // A stated limit and no discriminator -> a limit-on-close order.
        assertThat(LedgerCommands.orderType(null, new BigDecimal("260.0")))
                .contains(new BigDecimal("260.0"));
    }

    @Test
    void orderType_marketIsUnpricedEvenIfALimitIsSent() {
        assertThat(LedgerCommands.orderType("Market", new BigDecimal("260.0"))).isEmpty();
        assertThat(LedgerCommands.orderType("moc", null)).isEmpty();
    }

    @Test
    void orderType_limitRequiresAPriceAndUnknownTypesAreRejected() {
        assertThat(LedgerCommands.orderType("Limit", new BigDecimal("99.5")))
                .contains(new BigDecimal("99.5"));
        assertThatThrownBy(() -> LedgerCommands.orderType("Limit", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LedgerCommands.orderType("stop", new BigDecimal("1.0")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---- buying power: the collar bounds an unpriced BUY -------------------

    @Test
    void buyReservation_limitedBuyReservesQuantityTimesItsOwnLimit() {
        assertThat(LedgerCommands.buyReservation(
                Optional.of(new BigDecimal("260.0")), new BigDecimal("250.0"), new BigDecimal("10.0")))
                .isEqualByComparingTo(new BigDecimal("2600.0"));
    }

    @Test
    void buyReservation_unpricedBuyReservesTheTopOfTheCollarNotTheAnchor() {
        // anchor 250 -> band = max(0.50, 10% of 250) = 25 -> collar high = 275.
        // Reserving at the ANCHOR (2500) would under-fund this order by 250.
        assertThat(LedgerCommands.collarBand(new BigDecimal("250.0")))
                .isEqualByComparingTo(new BigDecimal("25.0"));
        assertThat(LedgerCommands.collarHigh(new BigDecimal("250.0")))
                .isEqualByComparingTo(new BigDecimal("275.0"));
        assertThat(LedgerCommands.buyReservation(
                Optional.empty(), new BigDecimal("250.0"), new BigDecimal("10.0")))
                .isEqualByComparingTo(new BigDecimal("2750.0"));
    }

    @Test
    void collarBand_usesTheAbsoluteFloorOnALowPricedInstrument() {
        // 10% of 2.00 is 0.20, below the 0.50 floor — the floor wins (Nasdaq's rule).
        assertThat(LedgerCommands.collarBand(new BigDecimal("2.0")))
                .isEqualByComparingTo(new BigDecimal("0.50"));
        assertThat(LedgerCommands.buyReservation(
                Optional.empty(), new BigDecimal("2.0"), new BigDecimal("100.0")))
                .isEqualByComparingTo(new BigDecimal("250.0"));
    }

    @Test
    void closeBidding_exercisesCloseBidding() {
        var cmd = asExercise(LedgerCommands.closeBidding("auction#1"));
        assertThat(cmd.getChoice()).isEqualTo("CloseBidding");
        assertThat(cmd.getContractId()).isEqualTo("auction#1");
    }

    @Test
    void runClose_exercisesRunCloseWithBothOrderLists() {
        var cmd = asExercise(LedgerCommands.runClose(
                "sealed#1", List.of("buy#1", "buy#2"), List.of("sell#1")));
        assertThat(cmd.getChoice()).isEqualTo("RunClose");
        assertThat(cmd.getContractId()).isEqualTo("sealed#1");
        assertThat(cmd.getTemplateId().getEntityName()).isEqualTo("ClosingAuction");
    }

    // ---- side() parsing ---------------------------------------------------

    @Test
    void side_parsesBuyAndSellCaseInsensitively() {
        assertThat(LedgerCommands.side("Buy")).isEqualTo(Side.BUY);
        assertThat(LedgerCommands.side("sell")).isEqualTo(Side.SELL);
        assertThat(LedgerCommands.side("  BUY ")).isEqualTo(Side.BUY);
    }

    @Test
    void side_rejectsUnknownOrNull() {
        assertThatThrownBy(() -> LedgerCommands.side("hold"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LedgerCommands.side(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---- template id helpers name the right templates ---------------------

    @Test
    void templateIdHelpers_pointAtTheExpectedTemplates() {
        assertThat(LedgerCommands.holdingTemplateId().getEntityName()).isEqualTo("Holding");
        assertThat(LedgerCommands.settlementReceiptTemplateId().getEntityName())
                .isEqualTo("SettlementReceipt");
        assertThat(LedgerCommands.settlementBatchTemplateId().getEntityName())
                .isEqualTo("SettlementBatch");
        assertThat(LedgerCommands.sealedOrderTemplateId().getEntityName()).isEqualTo("SealedOrder");
    }
}
