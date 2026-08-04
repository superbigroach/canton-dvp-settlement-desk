package com.lucilla.settlement.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request/response payloads for {@link SettlementController}.
 *
 * <p>Parties and contract ids are plain strings: the caller supplies the actual
 * on-ledger party ids (as allocated on the sandbox / participant) and the
 * contract ids returned by earlier calls. That keeps the desk honest — there is
 * no hidden party-name magic; the REST surface mirrors the Ledger API's own
 * identifiers.
 */
public final class Dtos {

    private Dtos() {
    }

    // ---- Parties ----------------------------------------------------------

    public record PartyResponse(
            String party, String displayName, String label, boolean isLocal) {
    }

    // ---- Instruments & Holdings ------------------------------------------

    public record IssueInstrumentRequest(
            @NotBlank String issuer,
            String depository,               // defaults to issuer when blank
            @NotBlank String id,
            String version,                  // defaults to "1" when blank
            @NotBlank String kind,           // Equity | Cash | CryptoWrapped
            String description,
            BigDecimal referencePrice) {     // optional (null for cash)
    }

    public record IssueHoldingRequest(
            @NotBlank String issuer,
            @NotBlank String instrumentId,
            @NotBlank String owner,
            @NotNull @Positive BigDecimal amount) {
    }

    public record HoldingResponse(
            String contractId, String issuer, String instrumentId, String owner,
            BigDecimal amount, List<String> disclosedTo) {
    }

    // ---- Bilateral DvP ----------------------------------------------------

    public record ProposeDvpRequest(
            @NotBlank String proposer,       // the SELLER: delivers asset, receives cash
            @NotBlank String counterparty,   // the BUYER: delivers cash, receives asset
            @NotBlank String auditor,
            @NotBlank String assetHoldingCid,
            @NotBlank String cashHoldingCid,
            @NotBlank String assetInstrument,
            @NotNull @Positive BigDecimal assetAmount,
            @NotBlank String cashInstrument,
            @NotNull @Positive BigDecimal cashAmount) {
    }

    public record AcceptDvpRequest(
            @NotBlank String counterparty) { // acts as; must be the proposal's counterparty
    }

    public record SettleDvpRequest(
            @NotBlank String proposer) {     // acts as; must be the agreement's proposer
    }

    // ---- Market-on-Close --------------------------------------------------

    public record OpenAuctionRequest(
            @NotBlank String operator,
            @NotBlank String auditor,
            @NotBlank String instrumentId,
            @NotBlank String cashInstrument,
            String session,                  // Open | Close (defaults to Close when blank)
            @NotNull @Positive BigDecimal referencePrice,
            @NotNull List<@NotBlank String> participants,
            String liquidityProvider,        // optional: the designated DLP (null/blank = none)
            String fixingRef) {              // optional: a committee NavFixing cid to bind the close to
    }

    /**
     * Lodge a sealed order into an EXISTING auction, with the backing holding already
     * provisioned by the caller.
     *
     * <p>THE ORDER TYPE. {@code orderType} is {@code Market} (unpriced MOC) or
     * {@code Limit} (LOC), and it may be omitted: with no discriminator the type is
     * inferred from {@code limitPrice}, and an ABSENT OR NULL LIMIT IS A MARKET ORDER,
     * never a rejection. A market order takes the price the book prints, ranks ahead of
     * every limit order, and can never be cancelled for being away from the cross.
     */
    public record SubmitOrderRequest(
            @NotBlank String trader,
            @NotBlank String side,           // Buy | Sell
            @NotNull @Positive BigDecimal quantity,
            String orderType,                // Market | Limit (blank = infer from limitPrice)
            @Positive BigDecimal limitPrice, // null/absent = unpriced market-on-close
            @NotBlank String holdingCid) {   // pre-committed cash (Buy) or asset (Sell)
    }

    public record CloseAuctionRequest(
            @NotBlank String operator,
            @NotNull List<@NotBlank String> buyOrderCids,
            @NotNull List<@NotBlank String> sellOrderCids) {
    }

    // ---- One-click Buy/Sell (server-orchestrated DvP) ---------------------

    /**
     * A whole bilateral trade in one call: the desk resolves each side's holding
     * (splitting/merging to the exact leg amount) then runs propose → accept →
     * settle server-side. {@code auditor} defaults to "Auditor" when blank.
     */
    public record TradeRequest(
            @NotBlank String buyer,          // delivers cash, receives the asset
            @NotBlank String seller,         // delivers the asset, receives cash
            String auditor,                  // defaults to "Auditor"
            @NotBlank String assetInstrument,
            @NotNull @Positive BigDecimal assetAmount,
            @NotBlank String cashInstrument,
            @NotNull @Positive BigDecimal cashAmount) {
    }

    public record TradeResponse(
            String receiptCid,
            String buyer,
            String seller,
            String assetInstrument,
            BigDecimal assetAmount,
            String cashInstrument,
            BigDecimal cashAmount,
            BigDecimal unitPrice) {
    }

    // ---- Simple Market-on-Close (auto-resolve auction + committed holding) --

    /**
     * Send one sealed order to the close — DEAD SIMPLE: just the asset, the side,
     * and the amount. No counterparty: the desk resolves (or opens) the auction and
     * auto-commits the backing holding (cash for a Buy, the asset for a Sell). The
     * acting party is {@code trader} (the logged-in party from the switcher).
     *
     * <p>THE ORDER TYPE, AND WHY AN ABSENT LIMIT IS NOT AN ERROR. The overwhelming
     * majority of real closing volume is UNPRICED market-on-close — index funds and
     * rebalancers whose mandate IS the official print — so an order that names no
     * limit is the NORMAL case, not a malformed one. {@code orderType=Market} (or a
     * blank discriminator with no {@code limitPrice}) lodges an unpriced MOC;
     * {@code orderType=Limit} with a price lodges an LOC that walks away from anything
     * worse than its limit.
     */
    public record MocOrderRequest(
            @NotBlank String trader,
            @NotBlank String side,           // Buy | Sell
            @NotNull @Positive BigDecimal quantity,
            @NotBlank String instrumentId,
            String cashInstrument,            // defaults to "USDC" when blank
            String session,                   // Open | Close (defaults to Close when blank)
            // Market | Limit. Blank/absent = INFER from limitPrice below.
            String orderType,
            // The worst price this order will accept (Buy: max, Sell: min). OPTIONAL:
            // absent or null means an UNPRICED MARKET-ON-CLOSE order, which trades at
            // whatever the book prints and is allocated ahead of every limit order.
            // Supplying limits AWAY from the anchor is what lets the uncross discover a
            // price other than the reference.
            //
            // NOTE for buyers: the ledger reserves quantity * the worst price the order
            // can legally execute at. For a LIMIT that is quantity * THIS limit (never
            // quantity * referencePrice — the cross may print above the anchor). For a
            // MARKET order there is no limit, so it is quantity * the TOP OF THE VENUE'S
            // PRICE COLLAR, which is the highest price the close can legally print.
            // Unspent cash comes back as change at settlement either way.
            @Positive BigDecimal limitPrice) {
    }

    public record InstrumentResponse(
            String id, String kind, String description, BigDecimal referencePrice) {
    }

    public record MocOrderResponse(
            String orderCid,
            String auctionCid,
            boolean openedAuction,
            BigDecimal closingPrice) {
    }

    /**
     * A resting sealed order as the acting party is entitled to see it.
     *
     * <p>{@code limitPrice} is NULL for an unpriced market-on-close order — that is the
     * order TYPE showing through, not missing data, and a UI must render it as
     * <b>"MOC"</b> rather than as blank, 0, or the anchor. {@code orderType} carries the
     * same fact as a label so the blotter never has to infer it.
     */
    public record MocOrderView(
            String contractId, String trader, String side,
            BigDecimal quantity, BigDecimal limitPrice, String orderType) { // "MOC" | "LOC"
    }

    public record MocStateResponse(
            String auctionCid, String instrumentId, String cashInstrument, String session,
            BigDecimal referencePrice, boolean isOpen, List<MocOrderView> orders,
            // The dark-pool hint: how many OTHER traders' orders rest HIDDEN from the
            // acting party (0 for the venue, which sees the full book). Never reveals
            // their side/size/price — only that sealed interest exists.
            int othersResting) {
    }

    /** Trader withdraws their own resting order (actAs the trader). */
    public record WithdrawOrderRequest(
            @NotBlank String trader) {
    }

    /** Venue clears the resting book for an instrument/session (actAs the venue). */
    public record ClearBookRequest(
            @NotBlank String instrumentId,
            String cashInstrument,            // defaults to "USDC" when blank
            String session) {                 // Open | Close (defaults to Close when blank)
    }

    public record MocFillView(
            String trader, String side, BigDecimal quantity, BigDecimal price) {
    }

    public record ClearBookResponse(int cleared) {
    }

    public record MocCloseResponse(
            String settlementBatchCid, String session, BigDecimal closingPrice,
            List<MocFillView> fills) {
    }

    /**
     * The NET imbalance of a sealed book — the MANDATED LIQUIDITY PROVIDER view.
     *
     * <p>Returned by {@code GET /moc/imbalance} ONLY to a party the ledger lets see the
     * {@link com.lucilla.settlement.model.marketonclose.ImbalanceDisclosure}, which is
     * now exactly: a provider holding a LIVE {@code LiquidityMandate} over this book,
     * and the venue. It reveals the AGGREGATE only — never any individual order or
     * trader identity.
     *
     * <p>{@code mandateRequired=true} with {@code disclosed=false} is the one answer
     * worth reading carefully: the caller is not being refused for who it is, it is
     * being told the privilege now has a price — accept the venue's open
     * {@code MandateTerms} and the same number is yours. That is a 4xx with a route
     * out of it, not a dead end, and the UI turns it into the Accept action.
     */
    public record MocImbalanceResponse(
            boolean disclosed,               // did the acting party get the aggregate?
            String instrumentId,
            String cashInstrument,
            String session,                  // "Open" | "Close"
            String netSide,                  // "Buy" | "Sell" | "Flat" (heavy side)
            BigDecimal netQuantity,          // magnitude of the imbalance (>= 0)
            BigDecimal referencePrice,       // the uniform price the cross will print at
            String liquidityProvider,        // label of the provider it was disclosed to
            boolean mandateRequired,         // true = no live mandate; accept terms to see it
            String note) {                   // human-readable context for the UI
    }

    // ---- The contestable liquidity mandate --------------------------------

    /**
     * One open offer of the liquidity seat, as shown to a prospective provider.
     *
     * <p>{@code openToActingParty} is the ledger's own answer to "may I take this?" —
     * eligible, not already holding a mandate off these terms, and not barred for
     * having failed a commitment this session. The party lists are LABELS, for display.
     */
    public record MandateTermsResponse(
            String contractId,
            String instrumentId,
            String cashInstrument,
            String session,                  // "Open" | "Close"
            BigDecimal anchorPrice,          // must equal the auction's published anchor
            BigDecimal commitmentSize,       // units of imbalance a provider undertakes to absorb
            long maxBandBps,                 // how far from the anchor it undertakes to stand
            String expiresAt,                // ISO-8601; terms close here, and so do their mandates
            List<String> eligible,           // who may take the seat (the trading roster)
            List<String> accepted,           // who already holds a live mandate off these terms
            List<String> barred,             // who forfeited it this session by missing a commitment
            boolean openToActingParty,
            String note) {
    }

    /** The venue posts an offer of the seat for one book. Numbers default if omitted. */
    public record PostMandateTermsRequest(
            @NotBlank String instrumentId,
            String cashInstrument,           // defaults to "USDC"
            String session,                  // defaults to "Close"
            BigDecimal commitmentSize,       // defaults to the desk's standard commitment
            Long maxBandBps,                 // defaults to the desk's standard band
            Integer ttlHours) {              // how long the offer (and its mandates) run
    }

    /**
     * A registered participant takes up the seat.
     *
     * <p>{@code termsCid} is OPTIONAL and normally omitted: the server resolves the
     * open terms for the book itself. Naming one is only useful when several offers
     * are live over the same book.
     */
    public record AcceptMandateRequest(
            @NotBlank String provider,
            @NotBlank String instrumentId,
            String cashInstrument,           // defaults to "USDC"
            String session,                  // defaults to "Close"
            String termsCid) {
    }

    /**
     * The acting party's OWN live obligation over one book.
     *
     * <p>{@code held=false} means no mandate — the caller sees no imbalance until it
     * accepts terms. {@code expired=true} distinguishes "you never took the seat" from
     * "your seat ran out", which are different problems with different fixes.
     * {@code shownSide}/{@code peakShownQty} are the running record the ledger stamped
     * on this mandate: what the provider was actually shown, and therefore owes.
     */
    public record MandateResponse(
            boolean held,
            String contractId,
            String provider,                 // label
            String instrumentId,
            String cashInstrument,
            String session,
            BigDecimal anchorPrice,
            BigDecimal commitmentSize,
            long maxBandBps,
            String expiresAt,                // ISO-8601
            String shownSide,                // "Buy" | "Sell" | "Flat" — side of the PEAK shown
            BigDecimal peakShownQty,         // largest imbalance this mandate has been shown
            long disclosuresSeen,
            boolean expired,
            String note) {
    }

    // ---- Decentralised operator: committee-attested NAV -------------------

    /** Stand up a K-of-N NAV committee (the decentralised operator). */
    public record CreateCommitteeRequest(
            @NotBlank String admin,
            @NotNull List<@NotBlank String> members,
            @Positive int threshold,
            String auditor,                  // defaults to "Auditor"
            String label) {                  // defaults to "NAV Committee"
    }

    /** A member proposes an official price fix (becomes the first attestor). */
    public record ProposeFixingRequest(
            @NotBlank String proposer,
            @NotBlank String instrumentId,
            String cashInstrument,           // defaults to "USDC"
            String session,                  // Open | Close (defaults to Close)
            @NotNull @Positive BigDecimal price,
            String rationale) {              // why (source / method)
    }

    /**
     * A member proposes an ACCRUING fix — it attests the INPUTS to a value that keeps
     * moving, not a number that is stale the moment it is signed.
     *
     * <p>Four things a human must agree, because everything else is derivable:
     * {@code price} (the base NAV the mark applies from), {@code ratePerAnnum}
     * (0.036 = 3.6%/yr, and it MAY be negative — EUR/CHF/JPY money markets printed
     * negative rates for seven years), {@code dayCount} ("ACT/360" or "ACT/365F", which
     * differ by 1.389% on the same rate and therefore cannot be assumed), and
     * {@code accrualFrom} (the instant the mark applies from).
     *
     * <p>{@code accrualFrom} is an ISO-8601 instant and is OPTIONAL — omitted, the desk
     * uses its own clock. Sending it is the honest thing whenever the mark describes a
     * moment the committee has already passed: a NAV "as of 16:00" is a fact about
     * 16:00 even if the last signature lands at 16:07.
     */
    public record ProposeAccruingFixingRequest(
            @NotBlank String proposer,
            @NotBlank String instrumentId,
            String cashInstrument,           // defaults to "USDC"
            String session,                  // Open | Close (defaults to Close)
            @NotNull @Positive BigDecimal price,     // the BASE NAV the accrual starts from
            String rationale,
            @NotNull BigDecimal ratePerAnnum,        // may be negative; > -1.0 (validated)
            @NotBlank String dayCount,               // ACT/360 | ACT/365F
            String accrualFrom) {             // ISO-8601; defaults to now
    }

    /**
     * The proposal, echoed back WITH THE RECIPE each member is being asked to confirm.
     *
     * <p>Confirming a price without the rate that will move it would be attesting a
     * third of a number, so the recipe travels with the proposal rather than being
     * something the UI remembers on its own.
     */
    public record FixingProposalResponse(
            String contractId,
            String instrumentId, String cashInstrument, String session,
            String basePrice,                // the ATTESTED BASE, as at accrualFrom
            String ratePerAnnum,
            String dayCount,
            String accrualFrom,              // ISO-8601
            long accrualFromEpochMicros,
            boolean accruing) {              // false = a pure snapshot (rate 0, "NONE")
    }

    /** Another member adds its attestation. */
    public record ConfirmFixingRequest(
            @NotBlank String member) {
    }

    /** Promote a threshold-attested proposal to an official NavFixing. */
    public record FinalizeFixingRequest(
            @NotBlank String proposer,
            List<@NotBlank String> publishTo) {   // venues to disclose the fix to (e.g. the auction operator)
    }

    // ---- Continuous accrual: the committee attests, the ledger computes ----
    //
    // WHY EVERY DECIMAL BELOW IS A STRING. Daml `Decimal` is fixed-point at exactly ten
    // decimal places. Serialising one as a JSON *number* hands it to JavaScript as a
    // float64, and a float64 is not that value — it is the nearest double to it. The
    // whole claim of this feature is that the figure ticking on a screen is the SAME
    // number the ledger would compute, so the wire format carries the ledger's own
    // digits and the browser re-parses them into exact fixed-point integers. A string is
    // the honest encoding of a Decimal; a number is a lossy one.

    /**
     * An official {@code NavFixing} with the accrual recipe on it, plus the value RIGHT
     * NOW derived from that recipe.
     *
     * <p>{@code basePrice} is what one share was worth AT {@code accrualFrom} — the
     * attested base, not the answer. {@code navNow} is the answer, and it is derived
     * (never stored) by the same arithmetic the ledger runs.
     */
    public record FixingResponse(
            String contractId,
            List<String> attestors,          // labels — the signature set IS the proof
            long threshold,
            String instrumentId, String cashInstrument, String session,
            String basePrice,                // ATTESTED base, as at accrualFrom
            String rationale,
            String ratePerAnnum,             // 0 = a pure snapshot; the old behaviour exactly
            String dayCount,                 // ACT/360 | ACT/365F | NONE
            String accrualFrom,              // ISO-8601 — attested, NOT the ledger clock
            long accrualFromEpochMicros,
            String finalizedAt,              // ISO-8601 — when the LEDGER saw it (a different fact)
            List<String> publishedTo,        // labels of the venues the fix reaches
            boolean accruing,
            String navNow,                   // derived at `asOf`
            String accrued,                  // navNow - basePrice
            String asOf,                     // ISO-8601 — the desk clock this was derived at
            long asOfEpochMicros,
            long elapsedMicros) {
    }

    /**
     * THE ACCRUED NAV, WITH ITS WORKING SHOWN — the derived value plus every input it
     * came from, so a consumer can reproduce it rather than trust it.
     *
     * <p>{@code yearMicros} is the day-count convention's year length in microseconds,
     * and it is returned because it is the one input that is NOT attested but IS
     * load-bearing: ACT/360 and ACT/365F disagree by 1.389% on the same rate.
     *
     * <p>{@code anchor…} binds this to the auction. {@code RunClose} requires the
     * auction's anchor to be consistent with the NAV ACCRUED TO THE CLOSE — at or below
     * it, and no more than one basis point behind — so these fields answer "would the
     * ledger let this auction run right now?" with the ledger's own function rather
     * than an assertion. {@code anchor} is null when no live auction for this
     * instrument/session is visible and none was supplied.
     */
    public record AccruedNavResponse(
            String contractId,
            String instrumentId, String cashInstrument, String session,
            // --- the four attested inputs ---
            String basePrice,
            String ratePerAnnum,
            String dayCount,
            String accrualFrom,
            long accrualFromEpochMicros,
            // --- the derivation ---
            String asOf,
            long asOfEpochMicros,
            long elapsedMicros,
            long yearMicros,
            String accrued,                  // basePrice * (rate * elapsed) / yearMicros
            String navNow,                   // max(0, basePrice + accrued)
            String perDay,                   // what a whole day of this recipe adds
            boolean accruing,
            // --- the attestation ---
            List<String> attestors,
            long threshold,
            // --- the auction binding ---
            String anchor,                   // the live auction's published anchor, or null
            String anchorAuctionCid,
            String anchorDrift,              // navNow - anchor (positive = the anchor is behind)
            String staleBudget,              // navNow * 1bp — how far behind it may legally be
            Boolean anchorConsistent,        // null when there is no anchor to judge
            String anchorNote) {
    }

    // ---- Basket / ETF builder --------------------------------------------

    /** One component leg of a basket's creation unit. */
    public record ComponentDto(
            @NotBlank String instrumentId,
            @NotNull @Positive BigDecimal unitsPerShare) {
    }

    /** Define a basket (ETF): its creation unit and authorised participants. */
    public record DefineBasketRequest(
            @NotBlank String administrator,
            String auditor,                  // defaults to "Auditor"
            @NotBlank String basketId,
            String description,
            String cashInstrument,           // defaults to "USDC"
            @NotNull List<ComponentDto> components,
            @NotNull List<@NotBlank String> participants) {
    }

    public record BasketResponse(
            String basketCid, String basketId, String administrator, String cashInstrument,
            List<ComponentDto> components, List<String> participants) {
    }

    /** In-kind creation: an AP creates {@code shares} units, delivering the underlyings. */
    public record BasketCreateRequest(
            @NotBlank String basketId,
            @NotBlank String ap,
            @NotNull @Positive BigDecimal shares) {
    }

    public record BasketCreateResponse(
            String receiptCid, String mintedSharesCid, BigDecimal shares, BigDecimal navPerShare) {
    }

    /** In-kind redemption: an AP returns {@code shares}, receiving the underlyings back. */
    public record BasketRedeemRequest(
            @NotBlank String basketId,
            @NotBlank String ap,
            @NotNull @Positive BigDecimal shares) {
    }

    public record BasketRedeemResponse(
            String receiptCid, BigDecimal shares, List<String> returnedHoldingCids) {
    }

    /** NAV per share and its component breakdown (marks from the instruments' close prices). */
    public record NavLeg(
            String instrumentId, BigDecimal unitsPerShare, BigDecimal price, BigDecimal value) {
    }

    public record NavResponse(
            String basketId, BigDecimal navPerShare, String cashInstrument,
            List<NavLeg> legs, boolean complete) {
    }

    /** A receipt as the acting party sees it, with WHO can see it (the privacy proof). */
    public record ReceiptResponse(
            String contractId, String kind, String headline, String settledAt,
            List<String> visibleTo) {
    }

    // ---- Generic responses ------------------------------------------------

    public record CidResponse(String contractId) {
    }

    public record SettleResponse(
            String receiptCid,
            List<String> createdHoldingCids) {
    }

    public record CloseResponse(
            String settlementBatchCid,
            String sealedAuctionCid) {
    }
}
