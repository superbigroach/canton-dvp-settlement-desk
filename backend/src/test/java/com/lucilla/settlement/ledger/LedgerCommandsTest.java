package com.lucilla.settlement.ledger;

import com.daml.ledger.javaapi.data.Command;
import com.daml.ledger.javaapi.data.CreateCommand;
import com.daml.ledger.javaapi.data.ExerciseCommand;
import com.lucilla.settlement.model.marketonclose.Side;
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
                "Close", new BigDecimal("255.0"), List.of("Alice", "Bob")));

        assertThat(cmd.getTemplateId().getEntityName()).isEqualTo("ClosingAuction");
        // operator, auditor, instrumentId, cashInstrument, session, referencePrice, participants, isOpen
        assertThat(cmd.getCreateArguments().getFields()).hasSize(8);
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
                new BigDecimal("10.0"), new BigDecimal("260.0"), "cash#1"));
        assertThat(cmd.getChoice()).isEqualTo("SubmitOrder");
        assertThat(cmd.getContractId()).isEqualTo("auction#1");
        assertThat(cmd.getTemplateId().getEntityName()).isEqualTo("ClosingAuction");
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
