package com.lucilla.settlement.ledger;

import com.daml.ledger.javaapi.data.codegen.Update;
import com.lucilla.settlement.model.holding.Holding;
import com.lucilla.settlement.model.instrument.Instrument;
import com.lucilla.settlement.model.marketonclose.ClosingAuction;
import com.lucilla.settlement.model.marketonclose.ImbalanceDisclosure;
import com.lucilla.settlement.model.marketonclose.SealedOrder;
import com.lucilla.settlement.model.marketonclose.Side;
import com.lucilla.settlement.model.settlement.DvPAgreement;
import com.lucilla.settlement.model.settlement.DvPProposal;
import com.lucilla.settlement.model.settlement.SettlementBatch;
import com.lucilla.settlement.model.governance.OperatorCommittee;
import com.lucilla.settlement.model.governance.FixingProposal;
import com.lucilla.settlement.model.governance.NavFixing;
import com.lucilla.settlement.model.basket.BasketDefinition;
import com.lucilla.settlement.model.basket.Component;
import com.lucilla.settlement.model.basket.CreationOrder;
import com.lucilla.settlement.model.basket.CreationAgreement;
import com.lucilla.settlement.model.basket.RedemptionOrder;
import com.lucilla.settlement.model.basket.RedemptionAgreement;
import com.lucilla.settlement.model.basket.BasketReceipt;

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

    /**
     * Open a ClosingAuction. {@code liquidityProvider} optionally designates ONE
     * party as the auction's Designated Liquidity Provider — the only party (besides
     * the venue) the net imbalance is ever disclosed to (see {@link #publishImbalance}).
     * Pass {@link Optional#empty()} for a plain dark-pool auction with no DLP.
     */
    public static Update<?> createAuction(
            String operator, String auditor, String instrumentId, String cashInstrument,
            String session, BigDecimal referencePrice, List<String> participants,
            Optional<String> liquidityProvider, Optional<String> fixingRefCid) {
        return new ClosingAuction(
                operator, auditor, instrumentId, cashInstrument,
                session, referencePrice, participants, liquidityProvider,
                // COMMITTEE-ATTESTED CLOSE: bind the auction to a K-of-N NavFixing so
                // RunClose proves the price is a committee attestation, not the venue's
                // unilateral number. Empty = a plain venue-priced close (unchanged).
                fixingRefCid.map(NavFixing.ContractId::new),
                /* isOpen = */ Boolean.TRUE)
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

    /** Trader withdraws their OWN resting order; unlocks the reserved holding. */
    public static Update<?> cancelOrder(String orderCid) {
        return new SealedOrder.ContractId(orderCid).exerciseCancel();
    }

    /** Venue clears a resting order off the book (operator-controlled). */
    public static Update<?> venueCancelOrder(String orderCid) {
        return new SealedOrder.ContractId(orderCid).exerciseVenueCancel();
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

    // ---- Designated Liquidity Provider: selective net-imbalance disclosure ---

    /**
     * Compute the NET imbalance of the resting book and disclose ONLY that aggregate
     * to the auction's Designated Liquidity Provider (operator-controlled). The DLP —
     * and only the DLP — will observe the resulting {@link ImbalanceDisclosure}; the
     * individual orders are never copied onto it.
     */
    public static Update<?> publishImbalance(String auctionCid, List<String> restingOrderCids) {
        List<SealedOrder.ContractId> resting = restingOrderCids.stream()
                .map(SealedOrder.ContractId::new).toList();
        return new ClosingAuction.ContractId(auctionCid).exercisePublishImbalance(resting);
    }

    /** Archive a stale imbalance disclosure (operator is its sole signatory). */
    public static Update<?> archiveImbalance(String disclosureCid) {
        return new ImbalanceDisclosure.ContractId(disclosureCid).exerciseArchive();
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

    public static com.daml.ledger.javaapi.data.Identifier imbalanceDisclosureTemplateId() {
        return ImbalanceDisclosure.TEMPLATE_ID;
    }

    public static com.daml.ledger.javaapi.data.Identifier settlementBatchTemplateId() {
        return SettlementBatch.TEMPLATE_ID;
    }

    public static com.daml.ledger.javaapi.data.Identifier settlementReceiptTemplateId() {
        return com.lucilla.settlement.model.settlement.SettlementReceipt.TEMPLATE_ID;
    }

    // ---- Governance: the decentralised-operator NAV committee (K-of-N) ------

    /** Stand up a K-of-N OperatorCommittee (the decentralised operator). */
    public static Update<?> createCommittee(
            String admin, List<String> members, int threshold, String auditor, String label) {
        return new OperatorCommittee(admin, members, (long) threshold, auditor, label).create();
    }

    /** A member proposes an official price fix (it becomes the first attestor). */
    public static Update<?> proposeFixing(
            String committeeCid, String proposer, String instrumentId, String cashInstrument,
            String session, BigDecimal price, String rationale) {
        return new OperatorCommittee.ContractId(committeeCid)
                .exerciseProposeFixing(proposer, instrumentId, cashInstrument, session, price, rationale);
    }

    /** Another member adds its attestation (accumulating multisig). */
    public static Update<?> confirmFixing(String proposalCid, String member) {
        return new FixingProposal.ContractId(proposalCid).exerciseConfirm(member);
    }

    /** Promote a threshold-attested proposal to an official NavFixing. */
    public static Update<?> finalizeFixing(String proposalCid, List<String> publishTo) {
        return new FixingProposal.ContractId(proposalCid).exerciseFinalizeFixing(publishTo);
    }

    public static com.daml.ledger.javaapi.data.Identifier operatorCommitteeTemplateId() {
        return OperatorCommittee.TEMPLATE_ID;
    }

    public static com.daml.ledger.javaapi.data.Identifier fixingProposalTemplateId() {
        return FixingProposal.TEMPLATE_ID;
    }

    public static com.daml.ledger.javaapi.data.Identifier navFixingTemplateId() {
        return NavFixing.TEMPLATE_ID;
    }

    // ---- Basket / ETF builder: in-kind creation & redemption ---------------

    /** A component leg of the creation unit: {@code unitsPerShare} of an instrument. */
    public static Component basketComponent(String instrumentId, BigDecimal unitsPerShare) {
        return new Component(instrumentId, unitsPerShare);
    }

    /** Define a basket (ETF): its creation unit and authorised participants. */
    public static Update<?> createBasket(
            String administrator, String auditor, String basketId, String description,
            String cashInstrument, List<Component> components, List<String> participants) {
        return new BasketDefinition(administrator, auditor, basketId, description,
                cashInstrument, components, participants).create();
    }

    /** AP requests to create {@code shares} units, delivering the underlyings. */
    public static Update<?> requestCreation(
            String basketCid, String ap, BigDecimal shares, List<String> componentHoldingCids) {
        List<Holding.ContractId> cids = componentHoldingCids.stream()
                .map(Holding.ContractId::new).toList();
        return new BasketDefinition.ContractId(basketCid).exerciseRequestCreation(ap, shares, cids);
    }

    /** Administrator approves a creation request → a bilaterally-signed agreement. */
    public static Update<?> approveCreation(String orderCid) {
        return new CreationOrder.ContractId(orderCid).exerciseApproveCreation();
    }

    /** Administrator processes a creation: pull underlyings + mint shares, atomically. */
    public static Update<?> processCreation(String agreementCid) {
        return new CreationAgreement.ContractId(agreementCid).exerciseProcessCreation();
    }

    /** AP requests to redeem {@code shares}, returning its basket-token holding. */
    public static Update<?> requestRedemption(
            String basketCid, String ap, BigDecimal shares, String basketHoldingCid) {
        return new BasketDefinition.ContractId(basketCid)
                .exerciseRequestRedemption(ap, shares, new Holding.ContractId(basketHoldingCid));
    }

    /** Administrator approves a redemption, supplying the custody underlyings to return. */
    public static Update<?> approveRedemption(String orderCid, List<String> custodyHoldingCids) {
        List<Holding.ContractId> cids = custodyHoldingCids.stream()
                .map(Holding.ContractId::new).toList();
        return new RedemptionOrder.ContractId(orderCid).exerciseApproveRedemption(cids);
    }

    /** Administrator processes a redemption: burn shares + return underlyings, atomically. */
    public static Update<?> processRedemption(String agreementCid) {
        return new RedemptionAgreement.ContractId(agreementCid).exerciseProcessRedemption();
    }

    public static com.daml.ledger.javaapi.data.Identifier basketDefinitionTemplateId() {
        return BasketDefinition.TEMPLATE_ID;
    }

    public static com.daml.ledger.javaapi.data.Identifier creationOrderTemplateId() {
        return CreationOrder.TEMPLATE_ID;
    }

    public static com.daml.ledger.javaapi.data.Identifier creationAgreementTemplateId() {
        return CreationAgreement.TEMPLATE_ID;
    }

    public static com.daml.ledger.javaapi.data.Identifier redemptionOrderTemplateId() {
        return RedemptionOrder.TEMPLATE_ID;
    }

    public static com.daml.ledger.javaapi.data.Identifier redemptionAgreementTemplateId() {
        return RedemptionAgreement.TEMPLATE_ID;
    }

    public static com.daml.ledger.javaapi.data.Identifier basketReceiptTemplateId() {
        return BasketReceipt.TEMPLATE_ID;
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
