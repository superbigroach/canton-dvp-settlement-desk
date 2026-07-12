package com.lucilla.settlement.ledger;

import com.daml.ledger.javaapi.data.codegen.Update;
import com.lucilla.settlement.model.holding.Holding;
import com.lucilla.settlement.model.instrument.Instrument;
import com.lucilla.settlement.model.marketonclose.ClosingAuction;
import com.lucilla.settlement.model.marketonclose.SealedOrder;
import com.lucilla.settlement.model.marketonclose.Side;
import com.lucilla.settlement.model.settlement.DvPAgreement;
import com.lucilla.settlement.model.settlement.DvPProposal;
import com.lucilla.settlement.model.settlement.SettlementBatch;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Pure mapping from desk requests to Daml Ledger API commands, built with the
 * generated Java bindings.
 *
 * <p><b>No ledger, no I/O, no Spring.</b> Every method returns a codegen
 * {@link Update} — a {@code HasCommands} carrying exactly one Create/Exercise
 * command — so this whole class is deterministically unit-testable by inspecting
 * the produced command's template id, choice name, and arguments (see
 * {@code LedgerCommandsTest}). {@link LedgerService} takes these Updates and
 * submits them under the correct {@code actAs} party.
 *
 * <p>Contract ids arrive as strings from REST and are wrapped in the generated,
 * type-safe {@code ContractId} classes here, which is what makes an
 * "exercise Accept on a DvPProposal" impossible to confuse with, say, a Holding.
 */
public final class LedgerCommands {

    private LedgerCommands() {
    }

    // ---- Instrument (reference data) --------------------------------------

    public static Update<?> createInstrument(
            String issuer, String depository, String id, String version,
            String kind, String description, Optional<BigDecimal> referencePrice) {
        return new Instrument(issuer, depository, id, version, kind, description, referencePrice)
                .create();
    }

    // ---- Holding (balance) ------------------------------------------------

    public static Update<?> createHolding(
            String issuer, String instrumentId, String owner, BigDecimal amount) {
        return new Holding(issuer, instrumentId, owner, amount, List.of()).create();
    }

    /** Split a holding into (exact, change); used to size a leg to an agreed amount. */
    public static Update<?> splitHolding(String holdingCid, BigDecimal splitAmount) {
        return new Holding.ContractId(holdingCid).exerciseSplit(splitAmount);
    }

    /** Merge {@code otherCid} into {@code baseCid} (same issuer/owner/instrument). */
    public static Update<?> mergeHolding(String baseCid, String otherCid) {
        return new Holding.ContractId(baseCid)
                .exerciseMerge(new Holding.ContractId(otherCid));
    }

    // ---- Bilateral DvP ----------------------------------------------------

    public static Update<?> createDvPProposal(
            String proposer, String counterparty, String auditor,
            String assetHoldingCid, String cashHoldingCid,
            String assetInstrument, BigDecimal assetAmount,
            String cashInstrument, BigDecimal cashAmount) {
        return new DvPProposal(
                proposer, counterparty, auditor,
                new Holding.ContractId(assetHoldingCid),
                new Holding.ContractId(cashHoldingCid),
                assetInstrument, assetAmount, cashInstrument, cashAmount)
                .create();
    }

    /** Counterparty accepts a proposal → a bilaterally-signed DvPAgreement. */
    public static Update<?> acceptProposal(String proposalCid) {
        return new DvPProposal.ContractId(proposalCid).exerciseAccept();
    }

    /** Proposer settles the agreement → both legs move atomically. */
    public static Update<?> settleAgreement(String agreementCid) {
        return new DvPAgreement.ContractId(agreementCid).exerciseSettle();
    }

    // ---- Market-on-Close auction ------------------------------------------

    public static Update<?> createAuction(
            String operator, String auditor, String instrumentId, String cashInstrument,
            String session, BigDecimal referencePrice, List<String> participants) {
        return new ClosingAuction(
                operator, auditor, instrumentId, cashInstrument,
                session, referencePrice, participants, /* isOpen = */ Boolean.TRUE)
                .create();
    }

    /** Normalise a caller session hint to the ledger's "Open" | "Close" label. */
    public static String session(String raw) {
        if (raw == null || raw.isBlank()) {
            return "Close";
        }
        return switch (raw.trim().toLowerCase()) {
            case "open", "opening", "moo" -> "Open";
            case "close", "closing", "moc" -> "Close";
            default -> throw new IllegalArgumentException(
                    "session must be Open or Close, got: " + raw);
        };
    }

    public static Update<?> submitOrder(
            String auctionCid, String trader, Side side,
            BigDecimal quantity, BigDecimal limitPrice, String holdingCid) {
        return new ClosingAuction.ContractId(auctionCid)
                .exerciseSubmitOrder(trader, side, quantity, limitPrice,
                        new Holding.ContractId(holdingCid));
    }

    /** Seal the order window; returns the new (sealed) auction contract id. */
    public static Update<?> closeBidding(String auctionCid) {
        return new ClosingAuction.ContractId(auctionCid).exerciseCloseBidding();
    }

    /** Run the uniform-price cross over the sealed book → a SettlementBatch. */
    public static Update<?> runClose(
            String sealedAuctionCid, List<String> buyOrderCids, List<String> sellOrderCids) {
        List<SealedOrder.ContractId> buys = buyOrderCids.stream()
                .map(SealedOrder.ContractId::new).toList();
        List<SealedOrder.ContractId> sells = sellOrderCids.stream()
                .map(SealedOrder.ContractId::new).toList();
        return new ClosingAuction.ContractId(sealedAuctionCid).exerciseRunClose(buys, sells);
    }

    // Template ids exposed for callers that need to locate created contracts of a
    // given type in a transaction tree (see LedgerService).
    public static com.daml.ledger.javaapi.data.Identifier instrumentTemplateId() {
        return Instrument.TEMPLATE_ID;
    }

    public static com.daml.ledger.javaapi.data.Identifier holdingTemplateId() {
        return Holding.TEMPLATE_ID;
    }

    public static com.daml.ledger.javaapi.data.Identifier dvpProposalTemplateId() {
        return DvPProposal.TEMPLATE_ID;
    }

    public static com.daml.ledger.javaapi.data.Identifier dvpAgreementTemplateId() {
        return DvPAgreement.TEMPLATE_ID;
    }

    public static com.daml.ledger.javaapi.data.Identifier closingAuctionTemplateId() {
        return ClosingAuction.TEMPLATE_ID;
    }

    public static com.daml.ledger.javaapi.data.Identifier sealedOrderTemplateId() {
        return SealedOrder.TEMPLATE_ID;
    }

    public static com.daml.ledger.javaapi.data.Identifier settlementBatchTemplateId() {
        return SettlementBatch.TEMPLATE_ID;
    }

    public static com.daml.ledger.javaapi.data.Identifier settlementReceiptTemplateId() {
        return com.lucilla.settlement.model.settlement.SettlementReceipt.TEMPLATE_ID;
    }

    public static Side side(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("side is required (Buy or Sell)");
        }
        return switch (raw.trim().toLowerCase()) {
            case "buy" -> Side.BUY;
            case "sell" -> Side.SELL;
            default -> throw new IllegalArgumentException("side must be Buy or Sell, got: " + raw);
        };
    }
}
