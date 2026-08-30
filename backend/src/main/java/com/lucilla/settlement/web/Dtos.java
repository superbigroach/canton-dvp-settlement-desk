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

    /**
     * A balance, and what it is worth NOW.
     *
     * <p>{@code mark} is the instrument's current value per unit. For an ordinary asset
     * that is its attested mark. For an ACCRUING instrument it is the value derived
     * from the committee's recipe at this instant, which is not the same number as the
     * struck base: a money-market fund share is bought at 1.00 and is worth more by
     * lunchtime. USYC, the real instrument this models, works exactly this way, and
     * Circle describes it as accruing "via token price increases" — the holder's
     * balance never changes, the price does. Valuing such a holding at its struck base
     * would show a static number on the one screen where a holder would look.
     */
    public record HoldingResponse(
            String contractId, String issuer, String instrumentId, String owner,
            BigDecimal amount, List<String> disclosedTo,
            BigDecimal mark, BigDecimal value, boolean accruing) {
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

    /**
     * A member proposes a WRAPPED-ASSET fix — the benchmark print and the par ratio,
     * signed separately.
     *
     * <p>CF Benchmarks prices BTC. It has never priced cBTC. A wrapped asset is a claim
     * on something priced elsewhere, and its value is {@code benchmarkPrice × parFactor}
     * where the factor is the market's confidence that redemption works. Marking it at
     * par is not a fact, it is an assertion — and it is the assertion that has broken
     * every wrapped asset that ever broke.
     *
     * <p>So the two travel as SEPARATE FIELDS and the struck price is their product,
     * computed on-ledger rather than sent in. That is what makes an issuer's refusal
     * legible: it is not disputing the price of Bitcoin, it is declining to attest par.
     * See {@code docs/SIGNER_PROTOCOL.md} §2a.
     */
    public record ProposeWrappedFixingRequest(
            @NotBlank String proposer,
            @NotBlank String instrumentId,           // the WRAPPED asset, e.g. "CBTC"
            String cashInstrument,                   // defaults to "USDC"
            String session,                          // Open | Close (defaults to Close)
            @NotNull @Positive BigDecimal benchmarkPrice,  // e.g. the CME CF BRR print
            @NotNull @Positive BigDecimal parFactor,       // 1.0 = at par; < 1.0 = a discount
            @NotBlank String rationale) {            // MUST cite the benchmark and its strike
    }

    /** The wrapped fix echoed back with the factor visible, not folded into the price. */
    public record WrappedFixingProposalResponse(
            String contractId,
            String instrumentId, String cashInstrument, String session,
            String benchmarkPrice,
            String parFactor,
            String strikePrice,              // benchmarkPrice × parFactor, as struck
            String discountBps,              // (1 - parFactor) × 10000, signed
            String rationale) {
    }

    /**
     * The signer protocol as data, so the UI renders exactly what the API will accept.
     *
     * <p>Hard-coding the conditions in the frontend would let the screen drift from the
     * rule, and a signer ticking a box that the backend then refuses is the fastest way
     * to teach someone their seat is decorative.
     */
    public record SignerProtocolResponse(
            String version,
            List<SignerRoleView> roles) {
    }

    /** One seat: what only it can see, and what it is therefore able to attest. */
    public record SignerRoleView(
            String key,                      // issuer | lender | venue | operator
            String title,
            String uniquelyKnows,
            List<SignerConditionView> conditions,
            boolean requiresObservedRange) { // venue only — the range the ledger checks
    }

    /** One named condition, and the plain statement of when it passes. */
    public record SignerConditionView(
            String name,
            String passesWhen) {
    }

    /** Another member adds its attestation. */
    public record ConfirmFixingRequest(
            @NotBlank String member) {
    }

    /**
     * Another member attests, AND records the named conditions it verified.
     *
     * <p>{@code docs/SIGNER_PROTOCOL.md}. No signer is ever asked for an opinion about
     * the price — each asserts a fact only it can see, so signing is cheap enough to
     * automate and a refusal names what broke. {@code checksPassed} carries the named
     * conditions from the protocol document (e.g. {@code attestor-quorum},
     * {@code book-acceptance}, {@code traded-range}).
     *
     * <p>{@code observedLow}/{@code observedHigh} are the VENUE's traded range, and are
     * enforced on-ledger: a venue cannot attest a price its own book never printed, and
     * an inverted or half-specified range is refused. That is the sharpest guard in the
     * protocol and the only one the ledger itself checks.
     */
    public record ConfirmWithChecksRequest(
            @NotBlank String member,
            @NotBlank String role,                   // issuer | lender | venue | operator
            String protocolRef,                      // defaults to "SIGNER_PROTOCOL v1 <role>"
            @NotNull List<@NotBlank String> checksPassed,  // at least one, enforced on-ledger
            BigDecimal observedLow,                  // venue only; both or neither
            BigDecimal observedHigh) {
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

    /**
     * One component leg of a basket's creation unit.
     *
     * <p>{@code expectedIssuer} pins WHOSE version of the instrument the fund accepts.
     * It is optional; omit it for "any issuer", which is what the demo baskets use. Set
     * it — to a party label or a full party id — for a fund that means to hold a named
     * issuer's asset, because {@code instrumentId} alone is just a string and any party
     * can mint a holding wearing that name.
     */
    public record ComponentDto(
            @NotBlank String instrumentId,
            @NotNull @Positive BigDecimal unitsPerShare,
            String expectedIssuer) {

        /** A leg with no issuer constraint. */
        public ComponentDto(String instrumentId, BigDecimal unitsPerShare) {
            this(instrumentId, unitsPerShare, null);
        }
    }

    /**
     * Define a basket (ETF): its creation unit, its authorised participants and — optionally
     * — the flat fee charged on each creation and redemption.
     *
     * <p>The three fee fields are all optional. Omit them for a fee-free basket. A
     * chargeable fee with no {@code feeReceiver}, or a negative fee, is refused on-ledger
     * by the template's {@code ensure} — the API does not duplicate that rule.
     */
    public record DefineBasketRequest(
            @NotBlank String administrator,
            String auditor,                  // defaults to "Auditor"
            @NotBlank String basketId,
            String description,
            String cashInstrument,           // defaults to "USDC"
            @NotNull List<ComponentDto> components,
            @NotNull List<@NotBlank String> participants,
            String feeReceiver,              // party paid the fee; the venue operator
            BigDecimal creationFee,          // flat, per creation, in cashInstrument
            BigDecimal redemptionFee) {      // flat, per redemption, in cashInstrument
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

    /**
     * One leg, valued twice: at the attested mark, and as of right now.
     *
     * <p>{@code basis} says WHERE the current number came from — a named feed, an
     * accrual recipe, or "attested mark" when the leg has neither and therefore
     * cannot move between fixings.
     */
    public record IndicativeNavLeg(
            String instrumentId,
            BigDecimal unitsPerShare,
            BigDecimal officialPrice,
            BigDecimal officialValue,
            BigDecimal indicativePrice,
            BigDecimal indicativeValue,
            String basis) {
    }

    /**
     * The two NAVs a fund actually has.
     *
     * <p>{@code official} is what create/redeem settles at — struck from marks the
     * committee signed. {@code indicative} is what the fund is worth right now.
     * {@code driftBps} is the gap between them in basis points, and it is the honest
     * measure of how stale the official strike has become: a wide drift on a volatile
     * book is the signal to strike again, not something to hide.
     */
    public record IndicativeNavResponse(
            String basketId,
            String cashInstrument,
            BigDecimal officialNavPerShare,
            BigDecimal indicativeNavPerShare,
            BigDecimal driftBps,
            List<IndicativeNavLeg> legs,
            boolean complete,
            /** False when nothing could be revalued — every leg fell back to its mark. */
            boolean live,
            String asOf) {
    }

    /**
     * A CANDIDATE mark from an outside feed. Not an official price — nothing values
     * against it until a committee attests it. {@code note} carries the assumption
     * being made (e.g. that a wrapped token marks at its underlying's spot).
     */
    public record LiveMarkResponse(
            String instrumentId, String symbol, BigDecimal price,
            String source, String asOf, String note) {
    }

    /**
     * A finalised fix, and what happened to the instrument's mark.
     *
     * <p>{@code markUpdated} is the bit that matters: the attested price is written back
     * to the Instrument so the fund's NAV per share values against the committee's number
     * rather than the issuer's last unilateral one. It can be false without the fix being
     * invalid — {@code note} says why.
     */
    public record FinalizeFixingResponse(
            String contractId,
            String instrumentId,
            BigDecimal attestedPrice,
            boolean markUpdated,
            String note) {
    }

    // =====================================================================
    // LEVERAGED LONG / SHORT (daml/Perpetual.daml)
    // =====================================================================

    public record PerpMarketRequest(
            @NotBlank String instrumentId,
            String cashInstrument,          // defaults to USDC
            BigDecimal indexPrice,          // defaults to the instrument's attested mark
            BigDecimal maxLeverage,         // defaults to 10x
            BigDecimal maintenanceMarginBps) {  // defaults to 500 (5%)
    }

    /**
     * A market, with the two numbers that describe its risk: {@code skew} is
     * {@code openLong - openShort}, the directional exposure the venue's pool is
     * carrying, and {@code fundingRate} is the lever that closes it.
     */
    public record PerpMarketResponse(
            String contractId, String instrumentId, String cashInstrument,
            BigDecimal indexPrice, BigDecimal fundingRate, BigDecimal fundingRateCap,
            BigDecimal maxLeverage, BigDecimal maintenanceMarginBps,
            BigDecimal openLong, BigDecimal openShort, BigDecimal skew,
            boolean insured, boolean isOpen) {
    }

    public record PerpFundRequest(
            @NotBlank String instrumentId,
            String cashInstrument,
            @NotNull @Positive BigDecimal amount) {
    }

    public record PerpIndexRequest(
            @NotBlank String instrumentId,
            String cashInstrument,
            /** Omit to read the instrument's attested mark, which is the safe path. */
            BigDecimal indexPrice) {
    }

    public record PerpFundingRequest(
            @NotBlank String instrumentId,
            String cashInstrument,
            /** What the perpetual itself last traded at. The rate is DERIVED from it. */
            @NotNull @Positive BigDecimal perpMark) {
    }

    public record PerpOpenRequest(
            @NotBlank String trader,
            @NotBlank String side,          // Long | Short
            @NotNull @Positive BigDecimal size,
            @NotBlank String instrumentId,
            String cashInstrument,
            @NotNull @Positive BigDecimal collateral) {
    }

    public record PerpCloseRequest(@NotBlank String trader) {
    }

    public record PerpCollateralRequest(
            @NotBlank String trader,
            @NotNull @Positive BigDecimal extra) {
    }

    /**
     * A position, marked to the market's index.
     *
     * <p>{@code liquidationPrice} is the index at which equity meets the maintenance
     * floor — the number a trader most wants and that no venue shows prominently
     * enough. {@code liquidatable} is that comparison made now.
     */
    public record PerpPositionResponse(
            String contractId, String trader, String instrumentId, String cashInstrument,
            String side, BigDecimal size, BigDecimal entryPrice, BigDecimal markPrice,
            BigDecimal collateral, BigDecimal notional, BigDecimal leverage,
            BigDecimal unrealisedPnl, BigDecimal equity, BigDecimal maintenance,
            BigDecimal liquidationPrice, boolean liquidatable,
            /** False = the market's index could not be read, so every marked figure
                above is null and none of them may be treated as reassuring. */
            boolean marked,
            String openedAt, String lastFundingAt) {
    }

    /** What a close or a liquidation actually realised. */
    public record PerpCloseResponse(
            String contractId, String side, BigDecimal size,
            BigDecimal entryPrice, BigDecimal exitPrice,
            BigDecimal realisedPnl, BigDecimal payout,
            PerpMarketResponse market) {
    }

    // =====================================================================
    // THE CONTINUOUS SESSION (daml/ContinuousBook.daml)
    // =====================================================================

    public record BookOrderRequest(
            @NotBlank String trader,
            @NotBlank String side,            // Bid | Ask (Buy | Sell also accepted)
            @NotNull @Positive BigDecimal quantity,
            @NotBlank String instrumentId,
            String cashInstrument,            // defaults to "USDC" when blank
            // ABSENT = an unpriced MARKET order. It takes whatever the book shows and
            // its remainder is killed; it may never rest. A stated limit RESTS at that
            // price and never trades worse than it.
            BigDecimal limitPrice,
            // GTC | IOC. Blank defaults to GTC for a limit order; an unpriced order is
            // forced to IOC by the ledger regardless of what is sent here.
            String timeInForce) {
    }

    /**
     * What one placement did. A placement may fill immediately (the venue matches it
     * against the resting ladder in the same request), so this carries the executions.
     */
    public record BookOrderResponse(
            String orderCid,               // null when the order filled completely
            String bookCid,                // the SUCCESSOR book — every choice is consuming
            boolean openedNewBook,
            BigDecimal referencePrice,
            List<BookFillView> fills,
            BigDecimal filledQuantity,
            BigDecimal restingQuantity) {
    }

    /** One fill, at the MAKER's posted price. */
    public record BookFillView(
            BigDecimal price, BigDecimal quantity, BigDecimal cashAmount,
            String buyer, String seller, String aggressor, String maker) {
    }

    /**
     * The book as ONE party may see it. Acting as the venue returns the whole ladder;
     * acting as a trader returns only that trader's own orders — the ledger decides,
     * not this endpoint.
     */
    public record BookStateResponse(
            String bookCid, String instrumentId, String cashInstrument,
            BigDecimal referencePrice, BigDecimal bandLow, BigDecimal bandHigh,
            boolean isOpen, Long liveCount, Long nextSeq,
            List<BookOrderView> bids,
            List<BookOrderView> asks,
            BigDecimal bestBid, BigDecimal bestAsk) {
    }

    public record BookOrderView(
            String contractId, String trader, String side, BigDecimal quantity,
            BigDecimal limitPrice, String timeInForce, Long seqNo) {
    }

    public record BookTapeView(
            String contractId, String instrumentId, String cashInstrument,
            BigDecimal price, BigDecimal quantity, String printedAt, Long matchSeq) {
    }

    public record BookConfirmView(
            String contractId, String trader, String instrumentId, String cashInstrument,
            String side, BigDecimal quantity, BigDecimal price, BigDecimal cashAmount,
            String liquidity, String tradedAt) {
    }

    public record BookSessionRequest(
            @NotBlank String instrumentId,
            String cashInstrument,
            BigDecimal referencePrice,   // defaults to the instrument's published mark
            BigDecimal bandFraction) {   // defaults to 0.10
    }

    public record BookCancelRequest(
            @NotBlank String trader) {
    }
}
