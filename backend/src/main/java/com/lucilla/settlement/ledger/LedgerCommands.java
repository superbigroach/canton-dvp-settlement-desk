package com.lucilla.settlement.ledger;

import com.daml.ledger.javaapi.data.codegen.Update;
import com.lucilla.settlement.model.holding.Holding;
import com.lucilla.settlement.model.marketonclose.ordercommitment.ReserveHolding;
import com.lucilla.settlement.model.instrument.Instrument;
import com.lucilla.settlement.model.marketonclose.ClosingAuction;
import com.lucilla.settlement.model.marketonclose.ImbalanceDisclosure;
import com.lucilla.settlement.model.marketonclose.SealedOrder;
import com.lucilla.settlement.model.marketonclose.Side;
import com.lucilla.settlement.model.liquiditymandate.LiquidityMandate;
import com.lucilla.settlement.model.liquiditymandate.MandateTerms;
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
import com.lucilla.settlement.model.tokenstandarddvp.TokenStandardHolding;
import com.lucilla.settlement.model.tokenstandarddvp.TokenStandardRegistry;

import java.math.BigDecimal;
import java.time.Instant;
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
     * Open a ClosingAuction.
     *
     * <p><b>{@code liquidityProvider} IS NOW INERT.</b> The field still exists on
     * {@code ClosingAuction} — it is part of the frozen field list — but no ledger
     * code reads it any more. Imbalance disclosure is gated on a live
     * {@code LiquidityMandate} the recipient SIGNED (see {@link #publishImbalance}),
     * not on a party name the venue wrote unilaterally into its own contract. Whatever
     * is passed here designates nobody and buys nobody anything; the seat is taken by
     * accepting {@link #postMandateTerms posted terms}. Kept for wire compatibility
     * and because the field is still displayed as historical context.
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
                // COMPLETE-ORDER COMMITMENT — a brand-new book has had nothing lodged
                // and nothing cancelled. The ledger maintains both counters from here
                // (SubmitOrder increments the first, WithdrawOrder/ClearOrder the
                // second) and RunClose asserts the supplied book numbers exactly
                // submittedCount - cancelledCount, so neither may be set by the client.
                /* submittedCount = */ 0L,
                /* cancelledCount = */ 0L,
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

    /**
     * Lodge a sealed order into the book.
     *
     * <p><b>CONSUMING.</b> {@code SubmitOrder} archives the auction and re-creates it
     * with {@code submittedCount} incremented, returning
     * {@code (new ClosingAuction cid, SealedOrder cid)}. The auction cid passed in is
     * therefore <b>dead</b> once this succeeds: callers submitting several orders MUST
     * thread the NEW auction cid (read it out of the transaction with
     * {@code closingAuctionTemplateId()}) into the next call, or they will exercise a
     * consumed contract.
     *
     * <p><b>THE ORDER TYPE IS THE {@code limitPrice} OPTIONAL.</b>
     * {@code Optional.of(p)} is a LIMIT-ON-CLOSE (LOC) and behaves exactly as before;
     * {@link Optional#empty()} is an unpriced MARKET-ON-CLOSE (MOC) — "fill me at the
     * close, whatever it is". An MOC counts at every candidate price, ranks ahead of
     * every limit order, and is never cancelled for being away from the cross.
     *
     * <p>The trader's backing is reserved by the ledger at:
     * <ul>
     *   <li>SELL — {@code quantity} of the ASSET, price-independent, MOC or LOC alike;</li>
     *   <li>LIMITED BUY — {@code quantity * limitPrice} of cash (never
     *       {@code referencePrice}: the cross is discovered from the book and can print
     *       above the anchor);</li>
     *   <li>UNPRICED BUY — {@code quantity *} {@link #collarHigh(BigDecimal)} of the
     *       anchor, because an MOC has no limit of its own and the venue's price collar
     *       is the only bound on what it can be asked to pay. Size the committed
     *       holding with {@link #buyReservation(Optional, BigDecimal, BigDecimal)}.</li>
     * </ul>
     */
    public static Update<?> submitOrder(
            String auctionCid, String trader, Side side,
            BigDecimal quantity, Optional<BigDecimal> limitPrice, String holdingCid) {
        // CIP-56 migration: SubmitOrder's 5th argument is now an OrderCommitment variant,
        // not a bare holding cid. ReserveHolding is the LEGACY path — the venue carves a
        // reserved slice out of the trader's own Holding. The other variant,
        // DeclareTokenHolding, is the Token Standard path: a standard holding cannot be
        // locked before the print exists (the registry's factory demands a concrete
        // TransferLeg, which needs a price), so a CIP-56 order DECLARES its backing and
        // allocates after the cross. See daml/TokenSettlement.daml.
        return new ClosingAuction.ContractId(auctionCid)
                .exerciseSubmitOrder(trader, side, quantity, limitPrice,
                        new ReserveHolding(new Holding.ContractId(holdingCid)));
    }

    // ---- The venue price collar (MIRRORED from MarketOnClose.daml) ----------
    //
    // These four numbers exist on the ledger and are re-stated here for ONE reason:
    // sizing an unpriced BUY's cash reservation BEFORE the order is sent. They must
    // stay in step with `collarBps` / `collarFloor` / `collarBand` in
    // daml/MarketOnClose.daml — if the model's constants move, move these.
    //
    // Nasdaq's construction: the band is the GREATER of a percentage of the anchor and
    // an absolute floor (a pure percentage is meaningless on a low-priced instrument).

    /** Half-width of the collar in basis points of the anchor (Daml: {@code collarBps}). */
    public static final BigDecimal COLLAR_BPS = new BigDecimal("1000");

    /** Absolute floor on the collar half-width (Daml: {@code collarFloor}). */
    public static final BigDecimal COLLAR_FLOOR = new BigDecimal("0.50");

    private static final BigDecimal BASIS_POINTS = new BigDecimal("10000");

    /**
     * Daml {@code Decimal} is fixed at 10 decimal places and its arithmetic rounds
     * half-EVEN, so every mirrored calculation below rounds the same way. Matching the
     * ledger bit-for-bit matters: this figure is compared against a trader's balance,
     * and a reservation a hair ABOVE the ledger's own would reject a fundable order
     * while one a hair BELOW would provision a holding the ledger then rejects.
     */
    private static final int DECIMAL_SCALE = 10;

    private static BigDecimal damlDecimal(BigDecimal v) {
        return v.setScale(DECIMAL_SCALE, java.math.RoundingMode.HALF_EVEN);
    }

    /**
     * The collar's half-width around {@code anchor}: {@code max(floor, anchor * bps)}
     * (Daml: {@code collarBand}).
     */
    public static BigDecimal collarBand(BigDecimal anchor) {
        BigDecimal pct = damlDecimal(damlDecimal(anchor.multiply(COLLAR_BPS))
                .divide(BASIS_POINTS, DECIMAL_SCALE, java.math.RoundingMode.HALF_EVEN));
        return pct.max(COLLAR_FLOOR);
    }

    /**
     * The HIGHEST price this auction can legally print: {@code anchor + collarBand}.
     *
     * <p>This is the bound that makes an unpriced BUY fundable at all. {@code RunClose}
     * clamps the discovered price into {@code anchor ± collarBand anchor}, so no close
     * can present an MOC buyer with a price above this — reserving here is sufficient
     * BY CONSTRUCTION, and because the boundary is a REACHABLE print it is also tight,
     * exactly the way a limit is tight for an LOC. Unspent cash returns as change.
     */
    public static BigDecimal collarHigh(BigDecimal anchor) {
        return anchor.add(collarBand(anchor));
    }

    /**
     * Cash a BUY must have committed before it can rest in the book — <b>the one
     * formula every buying-power pre-check must use</b>.
     *
     * <p>{@code limitPrice} present (LOC) → {@code quantity * limit}: a limited buy can
     * only ever execute at or inside its own limit. {@code limitPrice} empty (MOC) →
     * {@code quantity * collarHigh(anchor)}: an unpriced buy has no limit, so the top of
     * the venue's collar is its worst case. Using {@code quantity * anchor} for an MOC
     * UNDER-states the requirement and using {@code quantity * limit} is not even
     * defined — either way the check wrongly rejects, or wrongly accepts, orders the
     * ledger would not.
     */
    public static BigDecimal buyReservation(
            Optional<BigDecimal> limitPrice, BigDecimal anchor, BigDecimal quantity) {
        BigDecimal worstPrice = limitPrice.orElseGet(() -> collarHigh(anchor));
        return damlDecimal(quantity.multiply(worstPrice));
    }

    /**
     * Resolve the ORDER TYPE from a REST payload into the ledger's {@code Optional}.
     *
     * <p>The wire carries two hints — an optional {@code orderType} discriminator and an
     * optional {@code limitPrice} — and this is the single place they are reconciled:
     * <ul>
     *   <li>no discriminator → INFER: a stated limit is an LOC, <b>an absent or null
     *       limit is an MOC</b>. An unpriced order is a first-class order type, never a
     *       malformed one, so the absent case is a market order and not a rejection;</li>
     *   <li>{@code Market} / {@code MOC} / {@code Mkt} → unpriced, whatever was sent in
     *       {@code limitPrice} (the ticket hides the field in Market mode);</li>
     *   <li>{@code Limit} / {@code LOC} / {@code Lmt} → priced, and here a MISSING price
     *       genuinely is malformed: the caller asserted a limit order and then named no
     *       limit. That is a contradiction in the request itself, not an absent field
     *       to be defaulted.</li>
     * </ul>
     */
    public static Optional<BigDecimal> orderType(String rawType, BigDecimal limitPrice) {
        String t = rawType == null ? "" : rawType.trim().toLowerCase();
        if (t.isEmpty()) {
            return Optional.ofNullable(limitPrice);
        }
        switch (t) {
            case "market", "moc", "moo", "mkt":
                return Optional.empty();
            case "limit", "loc", "lmt":
                if (limitPrice == null) {
                    throw new IllegalArgumentException(
                            "orderType=Limit requires a limitPrice (omit orderType, or send "
                                    + "orderType=Market, for an unpriced market-on-close order)");
                }
                return Optional.of(limitPrice);
            default:
                throw new IllegalArgumentException(
                        "orderType must be Market or Limit, got: " + rawType);
        }
    }

    /** Seal the order window; returns the new (sealed) auction contract id. */
    public static Update<?> closeBidding(String auctionCid) {
        return new ClosingAuction.ContractId(auctionCid).exerciseCloseBidding();
    }

    /**
     * Trader withdraws their OWN resting order; unlocks the reserved holding.
     *
     * <p>Routed THROUGH the auction ({@code ClosingAuction.WithdrawOrder}), not through
     * {@code SealedOrder.Cancel} directly. Cancellation has to book itself into the
     * auction's {@code cancelledCount} in the SAME transaction, otherwise the count
     * over-states the live book and {@code RunClose} — which asserts
     * {@code buys + sells == submittedCount - cancelledCount} — can never run again.
     * {@code SealedOrder.Cancel} is now controlled by {@code trader, operator}
     * jointly, and this choice is the only place those two authorities meet.
     *
     * <p><b>CONSUMING on the auction</b>: returns
     * {@code (new ClosingAuction cid, unlocked Holding cid)} — thread the new auction
     * cid forward. Actor is the trader.
     */
    public static Update<?> withdrawOrder(String auctionCid, String trader, String orderCid) {
        return new ClosingAuction.ContractId(auctionCid)
                .exerciseWithdrawOrder(trader, new SealedOrder.ContractId(orderCid));
    }

    /**
     * Venue clears a resting order off the book (operator-controlled).
     *
     * <p>Routed THROUGH the auction ({@code ClosingAuction.ClearOrder}) for the same
     * reason as {@link #withdrawOrder}: it archives the order AND increments
     * {@code cancelledCount} atomically. Exercising {@code SealedOrder.VenueCancel}
     * directly is fail-safe but self-defeating — the count would then demand more
     * orders than exist and no close could run.
     *
     * <p><b>CONSUMING on the auction</b>: returns the new ClosingAuction cid, which
     * must be threaded into the next clear. Actor is the operator.
     */
    public static Update<?> clearOrder(String auctionCid, String orderCid) {
        return new ClosingAuction.ContractId(auctionCid)
                .exerciseClearOrder(new SealedOrder.ContractId(orderCid));
    }

    /**
     * Run the uniform-price cross over the sealed book → a SettlementBatch.
     *
     * <p><b>THE LISTS MUST BE THE COMPLETE BOOK.</b> {@code RunClose} asserts
     * {@code length buys + length sells == submittedCount - cancelledCount} and
     * rejects duplicates, so passing a price-filtered subset ALWAYS aborts the close.
     * Pass every live order for this (operator, instrument, cash unit, session); the
     * ledger discovers the clearing price from the whole book and simply does not
     * trade the orders that sit away from the print (it cancels them on close).
     */
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
     * to the provider named on {@code mandateCid} — nobody else observes the resulting
     * {@link ImbalanceDisclosure}, and the individual orders are never copied onto it.
     *
     * <p><b>THE MANDATE IS THE GATE, and it is not optional.</b> The auction's
     * {@code liquidityProvider} field is no longer read by the ledger: disclosure now
     * requires a LIVE {@code LiquidityMandate} covering exactly this book — same venue,
     * instrument, cash leg and session — whose {@code anchorPrice} equals the auction's
     * published {@code referencePrice}, held by a registered participant. A provider
     * cannot read the residual and then decide whether to be obligated about it; it
     * signed the obligation first, blind, and the number is what the obligation is for.
     *
     * <p><b>THIS CONSUMES THE MANDATE.</b> {@code PublishImbalance} is nonconsuming on
     * the auction but stamps what was shown onto the mandate (via {@code NoteDisclosure}),
     * which archives it and creates a successor — and the choice returns only the
     * disclosure cid. So a caller publishing twice to the same provider MUST re-read
     * that provider's mandate in between; never cache one across calls. The resolution
     * is server-side by design (see {@code SettlementController.resolveMandate}): a raw
     * contract id from an untrusted client is not an authorisation.
     */
    public static Update<?> publishImbalance(
            String auctionCid, List<String> restingOrderCids, String mandateCid) {
        List<SealedOrder.ContractId> resting = restingOrderCids.stream()
                .map(SealedOrder.ContractId::new).toList();
        return new ClosingAuction.ContractId(auctionCid)
                .exercisePublishImbalance(resting, new LiquidityMandate.ContractId(mandateCid));
    }

    /** Archive a stale imbalance disclosure (operator is its sole signatory). */
    public static Update<?> archiveImbalance(String disclosureCid) {
        return new ImbalanceDisclosure.ContractId(disclosureCid).exerciseArchive();
    }

    // ---- The contestable liquidity mandate: terms -> acceptance -> obligation ---
    //
    // The seat that buys sight of the imbalance is POSTED, not awarded. `MandateTerms`
    // is an open offer observable by every registered participant; `AcceptTerms` is
    // controlled by the PROVIDER, so the venue consents once, publicly, to whoever
    // shows up rather than privately to one name. Several providers may hold live
    // mandates over the same book at the same time and all are shown the same number.

    /**
     * The venue's DEFAULT posted commitment, in units of the instrument.
     *
     * <p>These two constants are the terms this desk posts when it opens a book
     * unattended (see {@code SettlementController.ensureMandateTerms}); a venue that
     * wants different numbers posts them explicitly through
     * {@code POST /api/moc/mandate/terms}. They are deliberately modest: the point of
     * the offer is that entry is CHEAP, and a commitment nobody can meet is a seat
     * nobody contests.
     */
    public static final BigDecimal DEFAULT_COMMITMENT_SIZE = new BigDecimal("5");

    /** The default band a posted provider undertakes to stand in: 200bps of the anchor. */
    public static final long DEFAULT_MAX_BAND_BPS = 200L;

    /**
     * Post an open offer of the liquidity seat for one book (operator-signed).
     *
     * <p>{@code accepted} and {@code barred} always start EMPTY — they are ledger
     * facts that only {@code AcceptTerms} and {@code BarProvider} may move, and a
     * client that could seed them could pre-bar a competitor. {@code anchorPrice} must
     * equal the auction's published {@code referencePrice}: the band a provider
     * promises to stand in is measured from the number the venue published, so a
     * mandate struck against any other anchor is a promise about a different auction
     * and {@code PublishImbalance} rejects it.
     */
    public static Update<?> postMandateTerms(
            String operator, String auditor, String instrumentId, String cashInstrument,
            String session, BigDecimal anchorPrice, BigDecimal commitmentSize,
            long maxBandBps, Instant expiresAt, List<String> eligible) {
        return new MandateTerms(
                operator, auditor, instrumentId, cashInstrument, session,
                anchorPrice, commitmentSize, maxBandBps, expiresAt,
                eligible.stream().distinct().toList(),
                /* accepted = */ List.of(),
                /* barred   = */ List.of())
                .create();
    }

    /**
     * A registered participant takes up the seat → {@code (successor terms, mandate)}.
     *
     * <p><b>CONSUMING on the terms</b>: {@code accepted} has to move in the same
     * transaction as the acceptance, so the terms cid passed in is dead once this
     * succeeds and a second acceptance must re-read the successor. The ACTOR is the
     * provider, never the venue — that is the whole contestability property.
     *
     * <p>Acceptance is BLIND: nothing about the resting book is visible from here, so
     * a participant undertakes the duty before it can see the number the duty is about.
     */
    public static Update<?> acceptMandateTerms(String termsCid, String provider) {
        return new MandateTerms.ContractId(termsCid).exerciseAcceptTerms(provider);
    }

    public static com.daml.ledger.javaapi.data.Identifier mandateTermsTemplateId() {
        return MandateTerms.TEMPLATE_ID;
    }

    public static com.daml.ledger.javaapi.data.Identifier liquidityMandateTemplateId() {
        return LiquidityMandate.TEMPLATE_ID;
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

    /**
     * A member proposes an ACCRUING fix — it attests the INPUTS to a value that keeps
     * moving, rather than a number that is already stale when it is signed.
     *
     * <p>A SEPARATE CHOICE, NOT A WIDER {@link #proposeFixing}. {@code ProposeFixing}
     * above still takes exactly the six arguments it always took and still produces a
     * non-accruing snapshot ({@code ratePerAnnum = 0.0}, day count {@code "NONE"}), so
     * every existing call site keeps working unchanged. Accrual is opt-in and this is
     * how a committee opts in: four attested inputs — base, rate, convention, origin —
     * from which the ledger derives every later value.
     *
     * <p>{@code accrualFrom} is ATTESTED rather than clocked: a NAV struck "as of 16:00"
     * is a fact about 16:00 even when the last member signs at 16:07, and taking the
     * ledger clock instead would silently under-accrue the fund by the length of the
     * committee's own signing round.
     */
    public static Update<?> proposeAccruingFixing(
            String committeeCid, String proposer, String instrumentId, String cashInstrument,
            String session, BigDecimal price, String rationale,
            BigDecimal ratePerAnnum, String dayCount, Instant accrualFrom) {
        return new OperatorCommittee.ContractId(committeeCid)
                .exerciseProposeAccruingFixing(proposer, instrumentId, cashInstrument, session,
                        price, rationale, ratePerAnnum, dayCount, accrualFrom);
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

    // ---- CIP-56 token standard (TokenStandardDvp.daml) --------------------
    // The registry's on-ledger half. These two creates are all the off-ledger
    // Registry API needs in order to have something real to serve: the factory
    // contract it discloses, and the holdings whose sum IS an instrument's total
    // supply. Everything else on the token-standard path is driven by wallets
    // exercising the standard's own interface choices, never by this desk.

    /**
     * Create the registry's factory contract — the ONE contract that implements both
     * {@code TransferInstructionV1.TransferFactory} and
     * {@code AllocationInstructionV1.AllocationFactory}.
     *
     * <p>{@code admin} is its only signatory and it has NO observers, which is exactly
     * why the off-ledger API exists: a wallet cannot see this contract on its own
     * stream and must receive it as an explicitly disclosed contract.
     */
    public static Update<?> createTokenStandardRegistry(String admin) {
        return new TokenStandardRegistry(admin).create();
    }

    /**
     * Mint a free (unlocked) {@code TokenStandardHolding}.
     *
     * <p>Requires the authority of BOTH the registry admin and the owner — the standard's
     * holding is co-signed, unlike the legacy issuer-only {@code Holding}. CIP-56 v1 says
     * nothing about issuance, so this is a registry-specific step by design, not by
     * omission.
     */
    public static Update<?> createTokenStandardHolding(
            String admin, String owner, String instrumentId, BigDecimal amount) {
        return new TokenStandardHolding(
                admin, owner,
                new com.lucilla.settlement.model.splice.api.token.holdingv1.InstrumentId(
                        admin, instrumentId),
                amount, Optional.empty())
                .create();
    }

    public static com.daml.ledger.javaapi.data.Identifier tokenStandardRegistryTemplateId() {
        return TokenStandardRegistry.TEMPLATE_ID;
    }

    public static com.daml.ledger.javaapi.data.Identifier tokenStandardHoldingTemplateId() {
        return TokenStandardHolding.TEMPLATE_ID;
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
