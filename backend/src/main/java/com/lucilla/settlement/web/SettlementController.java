package com.lucilla.settlement.web;

import com.daml.ledger.javaapi.data.Transaction;
import com.lucilla.settlement.ledger.Accrual;
import com.lucilla.settlement.ledger.LedgerCommands;
import com.lucilla.settlement.ledger.LedgerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * REST surface of the settlement desk. Each endpoint maps a request to a Daml
 * Ledger API command (built by {@link LedgerCommands}) submitted under the right
 * {@code actAs} party by {@link LedgerService}, and returns the resulting
 * contract id(s).
 *
 * <p>Endpoints:
 * <pre>
 *   POST /api/instruments                 issue an Instrument (actAs issuer)
 *   POST /api/holdings                    issue a Holding      (actAs issuer)
 *   GET  /api/holdings?party=             holdings visible to a party
 *   POST /api/dvp/propose                 create a DvPProposal (actAs proposer)
 *   POST /api/dvp/{cid}/accept            accept -> DvPAgreement (actAs counterparty)
 *   POST /api/dvp/{cid}/settle            settle both legs atomically (actAs proposer)
 *   POST /api/auction                     open a ClosingAuction (actAs operator)
 *   POST /api/auction/{cid}/order         submit a sealed order (actAs trader)
 *   POST /api/auction/{cid}/close         seal + run the uniform-price cross (actAs operator)
 *   GET  /api/moc/imbalance               the net residual — MANDATED PROVIDERS ONLY
 *   GET  /api/moc/mandate/terms           the venue's OPEN offer of the liquidity seat
 *   POST /api/moc/mandate/terms           post that offer (actAs operator)
 *   POST /api/moc/mandate/accept          take up the seat (actAs provider)
 *   GET  /api/moc/mandate                 your own live obligation over a book
 *   POST /api/committee                   stand up a K-of-N NAV committee
 *   POST /api/committee/{cid}/propose             propose a SNAPSHOT fix (non-accruing)
 *   POST /api/committee/{cid}/propose-accruing    propose an ACCRUING fix (rate + day count)
 *   POST /api/fixing/{cid}/confirm        another member attests (accumulating multisig)
 *   POST /api/fixing/{cid}/finalize       promote to an official NavFixing
 *   GET  /api/fixings                     official fixes + the value now
 *   GET  /api/fixing/{cid}/nav            THE ACCRUED NAV, with its working shown
 * </pre>
 */
@RestController
@RequestMapping("/api")
public class SettlementController {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(SettlementController.class);

    private final LedgerService ledger;
    private final com.lucilla.settlement.ledger.MarketData marketData;

    public SettlementController(LedgerService ledger,
                                com.lucilla.settlement.ledger.MarketData marketData) {
        this.ledger = ledger;
        this.marketData = marketData;
    }

    /**
     * CANDIDATE marks from the outside world — for pre-filling a proposal, nothing more.
     *
     * <p>The desk has no oracle: an official mark is a {@code NavFixing} carrying K-of-N
     * signatures, and this endpoint writes nothing to the ledger. It exists so a
     * committee member proposes today's real price instead of one typed from memory.
     * An empty list means the feed is unreachable — propose the mark by hand.
     */
    @GetMapping("/marks/live")
    public List<Dtos.LiveMarkResponse> liveMarks() {
        return marketData.liveMarks().stream()
                .map(m -> new Dtos.LiveMarkResponse(m.instrumentId(), m.symbol(), m.price(),
                        m.source(), String.valueOf(m.asOf()), m.note()))
                .toList();
    }

    // ---- Parties ----------------------------------------------------------

    /**
     * The parties this ledger knows about, for the UI's party picker. Party ids
     * carry a per-run Canton namespace suffix, so they are resolved live — never
     * hardcoded.
     */
    @GetMapping("/parties")
    public List<Dtos.PartyResponse> parties() {
        return ledger.listParties().stream()
                .map(p -> new Dtos.PartyResponse(p.party(), p.displayName(), p.label(), p.isLocal()))
                .toList();
    }

    /**
     * The receipts VISIBLE to the acting party — queried as that party, so the ledger's own
     * need-to-know rules decide what comes back. Auditor sees settlements without the holdings;
     * a party sees its own trades; an outsider (Eve) sees an empty list.
     */
    @GetMapping("/receipts")
    public List<Dtos.ReceiptResponse> receipts(@RequestParam String party) {
        String p = ledger.resolveParty(party);
        return ledger.receiptsVisibleTo(p).stream()
                .map(r -> new Dtos.ReceiptResponse(
                        r.contractId(), r.kind(), r.headline(), r.settledAt(), r.visibleTo()))
                .toList();
    }

    // ---- Instruments ------------------------------------------------------

    /** The published instruments (id, kind, reference/close price) for the pickers. */
    @GetMapping("/instruments")
    public List<Dtos.InstrumentResponse> instruments() {
        return ledger.instrumentsVisibleTo(ledger.resolveParty("Issuer")).stream()
                .map(i -> new Dtos.InstrumentResponse(
                        i.id(), i.kind(), i.description(), i.referencePrice()))
                .toList();
    }

    @PostMapping("/instruments")
    public ResponseEntity<Dtos.CidResponse> issueInstrument(
            @Valid @RequestBody Dtos.IssueInstrumentRequest req) {
        // RESOLVE THE LABELS. A party id carries a per-run Canton namespace suffix, so
        // "Issuer" is not a party — submitting it raw fails UNKNOWN_SUBMITTERS ("the
        // participant is not connected to any synchronizer where the given submitters
        // are known"), which reads like a node problem and is not one. Every other
        // endpoint resolves; these two did not.
        String issuer = ledger.resolveParty(req.issuer());
        String depository = ledger.resolveParty(blankTo(req.depository(), req.issuer()));
        String version = blankTo(req.version(), "1");
        String description = req.description() == null ? "" : req.description();
        var cmd = LedgerCommands.createInstrument(
                issuer, depository, req.id(), version, req.kind(), description,
                Optional.ofNullable(req.referencePrice()));
        String cid = ledger.submitForCreated(issuer, cmd, LedgerCommands.instrumentTemplateId());
        return created(new Dtos.CidResponse(cid));
    }

    // ---- Holdings ---------------------------------------------------------

    @PostMapping("/holdings")
    public ResponseEntity<Dtos.CidResponse> issueHolding(
            @Valid @RequestBody Dtos.IssueHoldingRequest req) {
        // Same label-resolution rule as POST /instruments above.
        String issuer = ledger.resolveParty(req.issuer());
        String owner = ledger.resolveParty(req.owner());
        var cmd = LedgerCommands.createHolding(
                issuer, req.instrumentId(), owner, req.amount());
        String cid = ledger.submitForCreated(issuer, cmd, LedgerCommands.holdingTemplateId());
        return created(new Dtos.CidResponse(cid));
    }

    @GetMapping("/holdings")
    public List<Dtos.HoldingResponse> holdings(@RequestParam String party) {
        // Accept either a full party id (from the picker) or a friendly label.
        String resolved = ledger.resolveParty(party);
        return ledger.holdingsVisibleTo(resolved).stream()
                .map(h -> new Dtos.HoldingResponse(
                        h.contractId(), h.issuer(), h.instrumentId(), h.owner(),
                        h.amount(), h.disclosedTo()))
                .toList();
    }

    // ---- Bilateral DvP ----------------------------------------------------

    @PostMapping("/dvp/propose")
    public ResponseEntity<Dtos.CidResponse> propose(@Valid @RequestBody Dtos.ProposeDvpRequest req) {
        var cmd = LedgerCommands.createDvPProposal(
                req.proposer(), req.counterparty(), req.auditor(),
                req.assetHoldingCid(), req.cashHoldingCid(),
                req.assetInstrument(), req.assetAmount(),
                req.cashInstrument(), req.cashAmount());
        String cid = ledger.submitForCreated(req.proposer(), cmd, LedgerCommands.dvpProposalTemplateId());
        return created(new Dtos.CidResponse(cid));
    }

    @PostMapping("/dvp/{cid}/accept")
    public ResponseEntity<Dtos.CidResponse> accept(
            @PathVariable String cid, @Valid @RequestBody Dtos.AcceptDvpRequest req) {
        var cmd = LedgerCommands.acceptProposal(cid);
        String agreementCid = ledger.submitForCreated(
                req.counterparty(), cmd, LedgerCommands.dvpAgreementTemplateId());
        return created(new Dtos.CidResponse(agreementCid));
    }

    @PostMapping("/dvp/{cid}/settle")
    public Dtos.SettleResponse settle(
            @PathVariable String cid, @Valid @RequestBody Dtos.SettleDvpRequest req) {
        var cmd = LedgerCommands.settleAgreement(cid);
        Transaction tree = ledger.submit(req.proposer(), cmd);
        List<String> receipts = ledger.createdOf(tree, LedgerCommands.settlementReceiptTemplateId());
        List<String> holdings = ledger.createdOf(tree, LedgerCommands.holdingTemplateId());
        return new Dtos.SettleResponse(receipts.isEmpty() ? null : receipts.get(0), holdings);
    }

    // ---- One-click Buy/Sell (server-orchestrated DvP) ---------------------

    /**
     * Execute a whole bilateral trade in one call. The desk resolves each side's
     * holding to the exact leg amount (splitting/merging as needed), then runs
     * propose (seller) → accept (buyer) → settle (seller) — all against the live
     * ledger. Returns the settlement receipt id and the agreed economics.
     */
    @PostMapping("/trade")
    public Dtos.TradeResponse trade(@Valid @RequestBody Dtos.TradeRequest req) {
        String seller = ledger.resolveParty(req.seller());
        String buyer = ledger.resolveParty(req.buyer());
        String auditor = ledger.resolveParty(blankTo(req.auditor(), "Auditor"));
        if (seller.equals(buyer)) {
            throw new IllegalArgumentException("buyer and seller must be different parties");
        }
        // A trade is THREE submissions (propose → accept → settle) under three different
        // acting parties. Log the FULL party ids up front so that when one of the three
        // fails, the log already says which party the failing actAs claim belongs to.
        log.info("TRADE start seller={} buyer={} auditor={} asset={} {} cash={} {}",
                seller, buyer, auditor, req.assetAmount(), req.assetInstrument(),
                req.cashAmount(), req.cashInstrument());

        // Auto-resolve the exact holdings each leg will move.
        String assetCid = ledger.provisionExactHolding(seller, req.assetInstrument(), req.assetAmount());
        String cashCid = ledger.provisionExactHolding(buyer, req.cashInstrument(), req.cashAmount());

        // propose → accept → settle, each under the correct actAs party.
        String proposalCid = ledger.submitForCreated(seller,
                LedgerCommands.createDvPProposal(seller, buyer, auditor, assetCid, cashCid,
                        req.assetInstrument(), req.assetAmount(),
                        req.cashInstrument(), req.cashAmount()),
                LedgerCommands.dvpProposalTemplateId());
        String agreementCid = ledger.submitForCreated(buyer,
                LedgerCommands.acceptProposal(proposalCid),
                LedgerCommands.dvpAgreementTemplateId());
        Transaction tree = ledger.submit(seller, LedgerCommands.settleAgreement(agreementCid));
        List<String> receipts = ledger.createdOf(tree, LedgerCommands.settlementReceiptTemplateId());
        log.info("TRADE done proposal={} agreement={} receipt={}", proposalCid, agreementCid,
                receipts.isEmpty() ? "NONE" : receipts.get(0));

        BigDecimal unitPrice = req.cashAmount()
                .divide(req.assetAmount(), 10, java.math.RoundingMode.HALF_UP)
                .stripTrailingZeros();
        return new Dtos.TradeResponse(
                receipts.isEmpty() ? null : receipts.get(0),
                req.buyer(), req.seller(),
                req.assetInstrument(), req.assetAmount(),
                req.cashInstrument(), req.cashAmount(), unitPrice);
    }

    // ---- Simple Market-on-Close ------------------------------------------

    /**
     * Lodge one sealed order into the close. Opens (or reuses) the open auction
     * for the instrument, auto-commits the trader's cash (Buy) or asset (Sell)
     * holding, and submits the sealed order. All orchestration is server-side.
     */
    @PostMapping("/moc/order")
    public ResponseEntity<Dtos.MocOrderResponse> mocOrder(
            @Valid @RequestBody Dtos.MocOrderRequest req) {
        String trader = ledger.resolveParty(req.trader());
        String venue = ledger.resolveParty("Venue");
        String auditor = ledger.resolveParty("Auditor");
        String cashInstrument = blankTo(req.cashInstrument(), "USDC");
        String session = LedgerCommands.session(req.session());
        var sideEnum = LedgerCommands.side(req.side());
        boolean isBuy = "buy".equalsIgnoreCase(req.side().trim());
        log.info("MOC ORDER start trader={} side={} qty={} instrument={} cash={} session={} "
                        + "orderType={} limit={}",
                trader, req.side(), req.quantity(), req.instrumentId(), cashInstrument, session,
                blankTo(req.orderType(), "(inferred)"), req.limitPrice());

        // Find an open auction for this instrument/cash/session, else open a fresh one
        // whose participant set is every known trader (so anyone in the picker can join).
        // The auction's referencePrice is the venue's published ANCHOR (the prior close
        // / the committee's NAV fix) — it is NOT the price the cross prints at. RunClose
        // DISCOVERS the clearing price from the sealed book (volume-maximising uncross)
        // and the anchor survives only as one more candidate and the last tie-break.
        // Opening (MOO) and closing (MOC) sessions rest in SEPARATE books.
        var open = ledger.auctionsVisibleTo(venue).stream()
                .filter(a -> a.isOpen()
                        && a.instrumentId().equals(req.instrumentId())
                        && a.cashInstrument().equals(cashInstrument)
                        && a.session().equals(session))
                .findFirst();
        String auctionCid;
        BigDecimal closingPrice;
        boolean opened;
        List<String> roster;
        if (open.isPresent()) {
            auctionCid = open.get().contractId();
            closingPrice = open.get().referencePrice();
            roster = open.get().participants();
            opened = false;
        } else {
            closingPrice = ledger.referencePriceOf("Issuer", req.instrumentId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            req.instrumentId() + " has no published reference price to close at"));
            List<String> participants = ledger.listParties().stream()
                    .map(p -> p.party())
                    .filter(p -> !LedgerService.labelOf(p).equalsIgnoreCase("sandbox"))
                    .toList();
            // The seed's conventional provider (Bank) still goes into the auction's
            // liquidityProvider field, but that field is INERT — it designates nobody
            // and buys nobody anything. Disclosure is gated on an accepted mandate.
            Optional<String> dlp = defaultLiquidityProvider();
            auctionCid = ledger.submitForCreated(venue,
                    LedgerCommands.createAuction(venue, auditor, req.instrumentId(),
                            cashInstrument, session, closingPrice, participants, dlp, Optional.empty()),
                    LedgerCommands.closingAuctionTemplateId());
            roster = participants;
            opened = true;
        }

        // POST THE SEAT WITH THE BOOK. A book whose terms were never posted has a seat
        // nobody can take, and therefore no route by which anyone may ever be shown its
        // residual — the offer and the auction it is about belong in the same breath.
        //
        // Run for an ALREADY-OPEN book too, not only a freshly opened one: a book that
        // predates the mandate model would otherwise keep an uncontestable seat forever.
        // Idempotent — it returns after one query when live terms already stand.
        ensureMandateTerms(venue, auditor, req.instrumentId(), cashInstrument, session,
                closingPrice, roster);

        // THE ORDER TYPE. Empty = an unpriced MARKET-on-close: it takes whatever the
        // book prints, counts at every candidate price, and is allocated ahead of every
        // limit order. A stated limit is a LIMIT-on-close, and a limit AWAY from the
        // anchor is what lets the uncross print somewhere other than the reference —
        // the whole point of price discovery.
        //
        // AN ABSENT LIMIT IS AN ORDER TYPE, NOT A MISSING FIELD. This used to silently
        // pin an unpriced request to the anchor, i.e. quietly convert it into a limit
        // order at the reference — which cancels itself the moment the book crosses
        // anywhere else, exactly the opposite of what a market order is for.
        Optional<BigDecimal> limitPrice =
                LedgerCommands.orderType(req.orderType(), req.limitPrice());

        // Pre-commit the trader's holding.
        //
        // BUY SIDE — THE BUYING-POWER FORMULA, WHICH IS NOT quantity * anchor.
        // The ledger reserves the worst price the order can LEGALLY execute at, and
        // this pre-check has to size the committed holding to exactly the same number
        // or SubmitOrder aborts with "committed holding is too small to back this
        // order":
        //   * LIMIT buy  -> quantity * limit. A buy can never execute above its own
        //     limit, so the limit is the exact worst case. (Not the anchor: RunClose
        //     discovers the price from the book and it can print ABOVE the anchor.)
        //   * MARKET buy -> quantity * collarHigh(anchor), the TOP OF THE PRICE COLLAR.
        //     An unpriced buy has no limit of its own, so the only bound on what it can
        //     be asked to pay is the venue's collar — RunClose clamps the print into
        //     anchor +/- collarBand(anchor) and cannot settle outside it. Reserving at
        //     the ANCHOR here would under-fund every market buy by up to the collar's
        //     width and reject orders that are perfectly fundable at the boundary.
        // Both cases live in LedgerCommands.buyReservation so there is one formula.
        String holdingCid = isBuy
                ? ledger.provisionAtLeastHolding(trader, cashInstrument,
                        LedgerCommands.buyReservation(limitPrice, closingPrice, req.quantity()))
                // A SELL delivers `quantity` of the asset whatever it prints — the asset
                // leg is price-independent, so MOC and LOC reserve identically.
                : ledger.provisionAtLeastHolding(trader, req.instrumentId(), req.quantity());

        // SubmitOrder is CONSUMING: it archives the auction and re-creates it with
        // submittedCount incremented. Read BOTH new contracts out of the one
        // transaction — the order to hand back, and the successor auction, because the
        // cid we just exercised is now dead and the next order must target the new one.
        Transaction tree = ledger.submit(trader,
                LedgerCommands.submitOrder(auctionCid, trader, sideEnum,
                        req.quantity(), limitPrice, holdingCid));
        String orderCid = ledger.createdOf(tree, LedgerCommands.sealedOrderTemplateId())
                .stream().findFirst().orElseThrow(() ->
                        new IllegalStateException("submit produced no sealed order"));
        String newAuctionCid = ledger.createdOf(tree, LedgerCommands.closingAuctionTemplateId())
                .stream().findFirst().orElse(auctionCid);
        // SubmitOrder is consuming: the auction cid the order was sent to is now DEAD.
        // Logging both ids is what makes a later CONTRACT_NOT_FOUND traceable to the
        // moment the successor was minted.
        log.info("MOC ORDER done order={} auction {} -> {} (openedNewBook={}) backing={}",
                orderCid, auctionCid, newAuctionCid, opened, holdingCid);
        return created(new Dtos.MocOrderResponse(orderCid, newAuctionCid, opened, closingPrice));
    }

    /**
     * The order-book view of the close — PARTY-AWARE (the dark-pool property).
     * The book is queried from the ACS <b>as the acting party</b>, so Daml's own
     * visibility does the filtering:
     * <ul>
     *   <li>acting as the <b>Venue</b> (operator, signs every order) → the FULL book;</li>
     *   <li>acting as a <b>trader</b> → ONLY that trader's own resting orders
     *       (a SealedOrder is signed by operator + trader and observed by nobody
     *       else, so a rival's order is simply not visible).</li>
     * </ul>
     * A muted {@code othersResting} count is added for the trader's view (how many
     * other sealed orders exist, without any of their details).
     */
    @GetMapping("/moc/state")
    public Dtos.MocStateResponse mocState(
            @RequestParam String instrumentId,
            @RequestParam(required = false, defaultValue = "USDC") String cashInstrument,
            @RequestParam(required = false, defaultValue = "Close") String session,
            @RequestParam(required = false) String actingAs) {
        String venue = ledger.resolveParty("Venue");
        String sess = LedgerCommands.session(session);
        // Default to the venue when no acting party is supplied (back-compat).
        String acting = (actingAs == null || actingAs.isBlank())
                ? venue : ledger.resolveParty(actingAs);
        boolean isVenue = acting.equals(venue);

        // The auction is observed by the operator + participants, so the acting
        // party (venue or a registered trader) sees it directly on its own stream.
        var open = ledger.auctionsVisibleTo(acting).stream()
                .filter(a -> a.isOpen()
                        && a.instrumentId().equals(instrumentId)
                        && a.cashInstrument().equals(cashInstrument)
                        && a.session().equals(sess))
                .findFirst();
        if (open.isEmpty()) {
            return new Dtos.MocStateResponse(
                    null, instrumentId, cashInstrument, sess, null, false, List.of(), 0);
        }
        var a = open.get();
        // PRIVACY ENFORCED AT THE LEDGER: query the book AS THE ACTING PARTY.
        List<Dtos.MocOrderView> orders = ledger.sealedOrdersVisibleTo(acting).stream()
                .filter(o -> o.operator().equals(venue)
                        && o.instrumentId().equals(instrumentId)
                        && o.cashInstrument().equals(cashInstrument)
                        && o.session().equals(sess))
                // limitPrice stays NULL for an unpriced market-on-close order — the
                // order type showing through, which the UI renders as "MOC". Coercing
                // it to 0 or to the anchor would put a price on the wire that the order
                // does not have.
                .map(o -> new Dtos.MocOrderView(
                        o.contractId(), o.trader(), o.side(), o.quantity(),
                        o.limitPrice(), o.orderType()))
                .toList();
        // Dark-pool hint (traders only): count of OTHER resting orders, no details.
        int othersResting = 0;
        if (!isVenue) {
            long total = ledger.sealedOrdersVisibleTo(venue).stream()
                    .filter(o -> o.operator().equals(venue)
                            && o.instrumentId().equals(instrumentId)
                            && o.cashInstrument().equals(cashInstrument)
                            && o.session().equals(sess))
                    .count();
            othersResting = Math.max(0, (int) total - orders.size());
        }
        return new Dtos.MocStateResponse(a.contractId(), instrumentId, cashInstrument, a.session(),
                a.referencePrice(), a.isOpen(), orders, othersResting);
    }

    /**
     * Withdraw a resting sealed order — a trader pulls their OWN order before the
     * close. The reserved backing is unlocked back to a free, private holding. Only
     * the order's owner may withdraw it.
     *
     * <p>Goes through {@code ClosingAuction.WithdrawOrder} rather than
     * {@code SealedOrder.Cancel}: the archive and the {@code cancelledCount} bump
     * must land in ONE transaction, or the book count would over-state the live book
     * and no subsequent close could ever satisfy RunClose's completeness assertion.
     * (Cancel is now controlled by trader AND operator jointly for exactly this
     * reason; the auction choice is where both authorities meet.)
     */
    @PostMapping("/moc/order/{orderCid}/withdraw")
    public ResponseEntity<Dtos.CidResponse> withdrawOrder(
            @PathVariable String orderCid, @Valid @RequestBody Dtos.WithdrawOrderRequest req) {
        String trader = ledger.resolveParty(req.trader());
        // Locate the order's own book, then the OPEN auction carrying its count. The
        // trader is a registered participant, so both are on the trader's own stream.
        var order = ledger.sealedOrdersVisibleTo(trader).stream()
                .filter(o -> o.contractId().equals(orderCid))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "no resting order " + orderCid + " is visible to "
                                + LedgerService.labelOf(trader)));
        String auctionCid = ledger.auctionsVisibleTo(trader).stream()
                .filter(a -> a.isOpen()
                        && a.operator().equals(order.operator())
                        && a.instrumentId().equals(order.instrumentId())
                        && a.cashInstrument().equals(order.cashInstrument())
                        && a.session().equals(order.session()))
                .findFirst()
                .map(LedgerService.AuctionView::contractId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "the book for " + order.instrumentId() + " is already sealed — "
                                + "orders can only be withdrawn while the window is open"));
        Transaction tree = ledger.submit(trader,
                LedgerCommands.withdrawOrder(auctionCid, trader, orderCid));
        List<String> unlocked = ledger.createdOf(tree, LedgerCommands.holdingTemplateId());
        return created(new Dtos.CidResponse(unlocked.isEmpty() ? orderCid : unlocked.get(0)));
    }

    /**
     * Clear the resting book for an instrument/session as the venue — the operator
     * cancels every resting order (exercise VenueCancel, actAs the venue). Used to
     * reset a book (e.g. after aborted retries) so the next cross starts clean.
     * Returns the number of orders cleared.
     */
    @PostMapping("/moc/clear")
    public Dtos.ClearBookResponse clearBook(@Valid @RequestBody Dtos.ClearBookRequest req) {
        String venue = ledger.resolveParty("Venue");
        String cashInstrument = blankTo(req.cashInstrument(), "USDC");
        String sess = LedgerCommands.session(req.session());
        var orders = ledger.sealedOrdersVisibleTo(venue).stream()
                .filter(o -> o.operator().equals(venue)
                        && o.instrumentId().equals(req.instrumentId())
                        && o.cashInstrument().equals(cashInstrument)
                        && o.session().equals(sess))
                .toList();
        // Each clear goes through ClosingAuction.ClearOrder so the archive and the
        // cancelledCount bump are atomic. That choice is CONSUMING on the auction, so
        // the successor cid must be threaded into the next iteration — clearing N
        // orders walks the auction through N successive contracts.
        var auction = ledger.auctionsVisibleTo(venue).stream()
                .filter(a -> a.isOpen()
                        && a.operator().equals(venue)
                        && a.instrumentId().equals(req.instrumentId())
                        && a.cashInstrument().equals(cashInstrument)
                        && a.session().equals(sess))
                .findFirst();
        if (auction.isEmpty()) {
            return new Dtos.ClearBookResponse(0);
        }
        String auctionCid = auction.get().contractId();
        int cleared = 0;
        for (var o : orders) {
            try {
                auctionCid = ledger.submitForCreated(venue,
                        LedgerCommands.clearOrder(auctionCid, o.contractId()),
                        LedgerCommands.closingAuctionTemplateId());
                cleared++;
            } catch (RuntimeException e) {
                // A concurrently-consumed order is already gone — skip it cleanly
                // rather than failing the whole clear. The auction cid is unchanged
                // when the exercise failed, so the loop can carry on with it.
                log.warn("skip clearing order {}: {}", o.contractId(), e.getMessage());
            }
        }
        return new Dtos.ClearBookResponse(cleared);
    }

    /**
     * Run the close as the venue: gather the COMPLETE sealed book for this auction,
     * seal the window, and uncross it. Returns the batch id and the fills.
     *
     * <p>The returned {@code closingPrice} is <b>DISCOVERED</b> — the volume-maximising
     * uniform price the ledger derived from the sealed book — not the auction's
     * reference/anchor price, which it may legitimately print above or below.
     */
    @PostMapping("/moc/{auctionCid}/close")
    public Dtos.MocCloseResponse mocClose(@PathVariable String auctionCid) {
        String venue = ledger.resolveParty("Venue");
        var auction = ledger.auctionsVisibleTo(venue).stream()
                .filter(a -> a.contractId().equals(auctionCid))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("no such auction: " + auctionCid));
        // THE COMPLETE BOOK, NOT A FILTERED SUBSET. RunClose asserts
        //   length buys + length sells == submittedCount - cancelledCount
        // so every live order for this book must be supplied. It used to be correct to
        // pass only the orders in-the-money at the reference price; now the price is
        // DISCOVERED from the book, so pre-filtering would (a) hide the very orders
        // that set the price and (b) fail the completeness assertion outright. Orders
        // away from the eventual print are expected — the ledger cancels them on close.
        var orders = ledger.sealedOrdersVisibleTo(venue).stream()
                .filter(o -> o.operator().equals(venue)
                        && o.instrumentId().equals(auction.instrumentId())
                        && o.cashInstrument().equals(auction.cashInstrument())
                        && o.session().equals(auction.session()))
                .toList();
        List<String> buyCids = orders.stream()
                .filter(o -> o.side().equalsIgnoreCase("Buy"))
                .map(com.lucilla.settlement.ledger.LedgerService.OrderView::contractId).toList();
        List<String> sellCids = orders.stream()
                .filter(o -> o.side().equalsIgnoreCase("Sell"))
                .map(com.lucilla.settlement.ledger.LedgerService.OrderView::contractId).toList();
        // A cross needs interest on both sides; the ledger decides at WHAT price they
        // meet (and aborts with "there is no crossing volume" if they never do).
        if (buyCids.isEmpty() || sellCids.isEmpty()) {
            throw new IllegalArgumentException("the close needs at least one resting buy AND one "
                    + "resting sell (have " + buyCids.size() + " buys, " + sellCids.size()
                    + " sells)");
        }
        // Fail EARLY and legibly if what we can see is not the whole book. RunClose
        // would reject this too, but as an opaque Daml assertion; here we can say which
        // way it is out. A mismatch means an order was archived without going through
        // WithdrawOrder/ClearOrder (so the count over-states the book), or the ACS read
        // raced an in-flight submission.
        long expected = auction.liveOrderCount();
        long supplied = (long) buyCids.size() + sellCids.size();
        if (supplied != expected) {
            throw new IllegalStateException("the close must run over the COMPLETE book: the"
                    + " ledger counts " + expected + " live order(s) (" + auction.submittedCount()
                    + " submitted − " + auction.cancelledCount() + " cancelled) but " + supplied
                    + " are visible. Re-run the close once the book settles, or clear it"
                    + " (POST /api/moc/clear) and re-seed.");
        }

        log.info("MOC CLOSE start venue={} auction={} instrument={} session={} book={} buys + "
                        + "{} sells (ledger expects {} live)",
                venue, auctionCid, auction.instrumentId(), auction.session(),
                buyCids.size(), sellCids.size(), expected);
        String sealedCid = ledger.submitForCreated(venue,
                LedgerCommands.closeBidding(auctionCid), LedgerCommands.closingAuctionTemplateId());
        Transaction tree = ledger.submit(venue,
                LedgerCommands.runClose(sealedCid, buyCids, sellCids));
        var batch = ledger.batchOf(tree).orElseThrow(() ->
                new IllegalStateException("close produced no settlement batch"));
        List<Dtos.MocFillView> fills = batch.fills().stream()
                .map(f -> new Dtos.MocFillView(f.trader(), f.side(), f.quantity(), f.price()))
                .toList();
        log.info("MOC CLOSE done batch={} sealed={} discoveredPrice={} fills={}",
                batch.contractId(), sealedCid, batch.closingPrice(), fills.size());
        return new Dtos.MocCloseResponse(batch.contractId(), auction.session(),
                batch.closingPrice(), fills);
    }

    // ---- The contestable mandate + selective imbalance disclosure ----------

    /**
     * The NET imbalance of the resting book — the MANDATED LIQUIDITY PROVIDER view.
     *
     * <p>Liquidity without leakage: a lit auction publishes the imbalance to the whole
     * market (leaking it); a dark pool leaks nothing but risks a no-fill. This endpoint
     * discloses the AGGREGATE net imbalance to a party that has SIGNED AN OBLIGATION to
     * absorb it — and to the venue — and to nobody else.
     *
     * <p><b>WHAT CHANGED.</b> The privilege used to follow the auction's
     * {@code liquidityProvider} field: a party name the venue wrote into its own
     * contract, which owed nothing and had agreed to nothing. It now follows a live
     * {@code LiquidityMandate}, co-signed by the provider, sized (`commitmentSize`) and
     * banded (`maxBandBps`) against the venue's published anchor. The field is inert and
     * is not consulted here.
     *
     * <p><b>THE MANDATE IS RESOLVED SERVER-SIDE — never taken from the caller.</b> A
     * contract id off the wire is not an authorisation: anyone could quote someone
     * else's. The desk queries the ledger for the ACTING PARTY's own live mandate over
     * exactly this book (venue, instrument, cash leg, session) at exactly this anchor,
     * and publishes against that. Re-resolved on EVERY call because
     * {@code PublishImbalance} consumes the mandate as it stamps the disclosure onto it.
     *
     * <p><b>NO MANDATE IS A 4xx WITH A ROUTE OUT.</b> A caller holding no mandate gets
     * {@code 403} and {@code mandateRequired=true} — not an empty imbalance, which would
     * read as "the book is balanced", and not a 500. The venue asking when NO provider
     * has taken the seat gets {@code 409}: nothing is wrong with the request, there is
     * simply nobody the residual may lawfully be shown to.
     */
    @GetMapping("/moc/imbalance")
    public ResponseEntity<Dtos.MocImbalanceResponse> mocImbalance(
            @RequestParam String instrumentId,
            @RequestParam(required = false, defaultValue = "USDC") String cashInstrument,
            @RequestParam(required = false, defaultValue = "Close") String session,
            @RequestParam(required = false) String actingAs) {
        String venue = ledger.resolveParty("Venue");
        String sess = LedgerCommands.session(session);
        String acting = (actingAs == null || actingAs.isBlank())
                ? venue : ledger.resolveParty(actingAs);
        boolean actingIsVenue = acting.equals(venue);

        // 1) The open book. No auction is not an error — there is simply nothing yet.
        var open = ledger.auctionsVisibleTo(venue).stream()
                .filter(a -> a.isOpen()
                        && a.instrumentId().equals(instrumentId)
                        && a.cashInstrument().equals(cashInstrument)
                        && a.session().equals(sess))
                .findFirst();
        if (open.isEmpty()) {
            return ResponseEntity.ok(new Dtos.MocImbalanceResponse(false, instrumentId,
                    cashInstrument, sess, "Flat", BigDecimal.ZERO, null, null, false,
                    "no auction is open for this book"));
        }
        var a = open.get();

        // 2) THE GATE. Resolve the mandate that authorises this disclosure, server-side.
        MandateGate gate = resolveMandate(acting, venue, instrumentId, cashInstrument, sess,
                a.referencePrice());
        if (!gate.granted()) {
            // A refusal here is the MANDATE GATE working, not a fault — log it as such
            // so it is not mistaken for the ledger failing during a demo.
            log.info("IMBALANCE denied acting={} book={}/{}/{} reason={}",
                    acting, instrumentId, cashInstrument, sess, gate.reason());
            return ResponseEntity
                    .status(actingIsVenue ? HttpStatus.CONFLICT : HttpStatus.FORBIDDEN)
                    .body(new Dtos.MocImbalanceResponse(false, instrumentId, cashInstrument,
                            sess, null, null, a.referencePrice(), null, true, gate.reason()));
        }
        var mandate = gate.mandate();
        String providerLabel = LedgerService.labelOf(mandate.provider());

        // 3) As the VENUE, refresh THIS PROVIDER's disclosure from the CURRENT book:
        //    archive its stale snapshot, then recompute + publish.
        //
        //    Scoped to the one provider on purpose. Several providers may hold live
        //    mandates over the same book — that plurality is the point — so archiving
        //    every disclosure for the book would wipe a competitor's view each time
        //    anyone refreshed theirs.
        for (var stale : ledger.imbalancesVisibleTo(venue)) {
            if (stale.operator().equals(venue) && stale.instrumentId().equals(instrumentId)
                    && stale.cashInstrument().equals(cashInstrument)
                    && stale.session().equals(sess)
                    && stale.liquidityProvider().equals(providerLabel)) {
                try {
                    ledger.submit(venue, LedgerCommands.archiveImbalance(stale.contractId()));
                } catch (RuntimeException e) {
                    log.warn("skip archiving stale imbalance {}: {}", stale.contractId(), e.getMessage());
                }
            }
        }
        // THE COMPLETE BOOK. PublishImbalance now asserts the same completeness RunClose
        // does — the disclosed number sizes someone's duty, so it cannot be computed
        // over a hand-picked subset.
        List<String> restingCids = ledger.sealedOrdersVisibleTo(venue).stream()
                .filter(o -> o.operator().equals(venue)
                        && o.instrumentId().equals(instrumentId)
                        && o.cashInstrument().equals(cashInstrument)
                        && o.session().equals(sess))
                .map(com.lucilla.settlement.ledger.LedgerService.OrderView::contractId).toList();
        ledger.submit(venue, LedgerCommands.publishImbalance(
                a.contractId(), restingCids, mandate.contractId()));

        // 4) As the ACTING PARTY, read the disclosure back. The LEDGER enforces who may
        //    see it: the mandated provider and the venue do, nobody else does.
        var visible = ledger.imbalancesVisibleTo(acting).stream()
                .filter(d -> d.operator().equals(venue) && d.instrumentId().equals(instrumentId)
                        && d.cashInstrument().equals(cashInstrument) && d.session().equals(sess)
                        && d.liquidityProvider().equals(providerLabel))
                .findFirst();
        if (visible.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new Dtos.MocImbalanceResponse(
                    false, instrumentId, cashInstrument, sess, null, null, a.referencePrice(),
                    providerLabel, true, "the net imbalance is disclosed only to a provider "
                            + "holding a live liquidity mandate over this book, and the venue"));
        }
        var d = visible.get();
        String mag = d.netQuantity().stripTrailingZeros().toPlainString();
        String note = "Flat".equals(d.netSide())
                ? "book is balanced — no offsetting liquidity needed"
                : "net " + d.netSide().toUpperCase() + " imbalance of " + mag + " " + instrumentId
                        + " @ " + d.referencePrice().stripTrailingZeros().toPlainString()
                        + " - offset to clear the cross";
        return ResponseEntity.ok(new Dtos.MocImbalanceResponse(true, instrumentId, cashInstrument,
                sess, d.netSide(), d.netQuantity(), d.referencePrice(), providerLabel, false, note));
    }

    /**
     * The outcome of the mandate gate: the obligation that authorises a disclosure, or
     * the reason there is none — phrased for a human, because the reason is the whole
     * product here. "You are not the DLP" was a dead end; "accept the posted terms" is
     * an open door, and that difference is the contestability story.
     */
    private record MandateGate(LedgerService.MandateView mandate, String reason) {
        static MandateGate allow(LedgerService.MandateView m) {
            return new MandateGate(m, null);
        }

        static MandateGate deny(String why) {
            return new MandateGate(null, why);
        }

        boolean granted() {
            return mandate != null;
        }
    }

    /**
     * Resolve the live {@code LiquidityMandate} that entitles {@code acting} to this
     * book's residual — the ONE place the desk decides who may be shown an imbalance.
     *
     * <p>Queried AS the acting party, so the ledger's own visibility does the first
     * half of the work: a provider co-signed its mandates and sees them; it cannot see
     * anyone else's. The venue is the other signatory on every mandate over its books,
     * so when the VENUE asks it may publish against any live one (it is refreshing a
     * provider's view, which is the venue's normal operation).
     *
     * <p>A mandate must match the book on all four identifying fields AND on the
     * anchor: {@code PublishImbalance} asserts {@code anchorPrice == referencePrice},
     * because the band a provider promised to stand in is measured from the number the
     * venue published. A mandate struck at another anchor is a promise about another
     * auction, and it is better to say so here than to let the ledger abort with it.
     *
     * <p>Liveness is checked on WALL CLOCK for the message only; the LEDGER re-checks
     * on its own clock inside the choice and is the authority. If the two ever disagree
     * the ledger wins and the rejection surfaces as a 422 with its own reason.
     */
    private MandateGate resolveMandate(String acting, String venue, String instrumentId,
            String cashInstrument, String sess, BigDecimal anchor) {
        boolean actingIsVenue = acting.equals(venue);
        Instant now = Instant.now();

        List<LedgerService.MandateView> forBook = ledger.mandatesVisibleTo(acting).stream()
                // A provider is judged on its OWN obligations. The venue is entitled to
                // publish against any of them — it co-signed every one.
                .filter(m -> actingIsVenue || m.provider().equals(acting))
                .filter(m -> m.operator().equals(venue)
                        && m.instrumentId().equals(instrumentId)
                        && m.cashInstrument().equals(cashInstrument)
                        && m.session().equals(sess))
                .toList();

        var usable = forBook.stream()
                .filter(m -> m.liveAt(now) && m.covers(venue, instrumentId, cashInstrument, sess, anchor))
                .findFirst();
        if (usable.isPresent()) {
            return MandateGate.allow(usable.get());
        }

        String noLive = actingIsVenue
                ? "no provider holds a LIVE mandate over this book"
                : "you hold no LIVE mandate over this book";
        String wrongAnchor = actingIsVenue
                ? "no provider holds a mandate struck against THIS auction's anchor"
                : "your mandate is not struck against THIS auction's anchor";
        if (forBook.isEmpty()) {
            return MandateGate.deny(actingIsVenue
                    ? "no provider holds a liquidity mandate over this book — imbalance "
                            + "disclosure now requires an ACCEPTED LiquidityMandate, so there is "
                            + "nobody the residual may lawfully be shown to. Post terms "
                            + "(POST /api/moc/mandate/terms) and let a participant accept them."
                    : "imbalance disclosure now requires an ACCEPTED liquidity mandate covering "
                            + "this book, and you hold none. Accept the venue's open terms "
                            + "(GET /api/moc/mandate/terms) to be shown the residual — the seat "
                            + "obliges you to absorb up to the posted commitment, within the "
                            + "posted band of the published anchor.");
        }
        var live = forBook.stream().filter(m -> m.liveAt(now)).findFirst();
        if (live.isEmpty()) {
            var expired = forBook.get(0);
            return MandateGate.deny(noLive + ": it expired at " + expired.expiresAt()
                    + ". An expired obligation buys no disclosure — accept the venue's current "
                    + "terms to take the seat again.");
        }
        return MandateGate.deny(wrongAnchor + ": it is anchored at "
                + live.get().anchorPrice().stripTrailingZeros().toPlainString()
                + " and this book publishes "
                + (anchor == null ? "?" : anchor.stripTrailingZeros().toPlainString())
                + ". A mandate is a promise measured from one PUBLISHED anchor, so it does not "
                + "carry to a book quoted against another. Accept the venue's current terms.");
    }

    /**
     * The venue's OPEN OFFER of the liquidity seat for a book — what a participant with
     * no mandate is shown instead of the imbalance.
     *
     * <p>Read AS the acting party: {@code MandateTerms} is observed by every eligible
     * participant, so an outsider gets an empty list from the LEDGER rather than from a
     * filter here. Expired offers are dropped — they can no longer be accepted.
     */
    @GetMapping("/moc/mandate/terms")
    public List<Dtos.MandateTermsResponse> mandateTerms(
            @RequestParam String instrumentId,
            @RequestParam(required = false, defaultValue = "USDC") String cashInstrument,
            @RequestParam(required = false, defaultValue = "Close") String session,
            @RequestParam(required = false) String actingAs) {
        String venue = ledger.resolveParty("Venue");
        String sess = LedgerCommands.session(session);
        String acting = (actingAs == null || actingAs.isBlank())
                ? venue : ledger.resolveParty(actingAs);
        Instant now = Instant.now();
        return ledger.mandateTermsVisibleTo(acting).stream()
                .filter(t -> t.operator().equals(venue)
                        && t.instrumentId().equals(instrumentId)
                        && t.cashInstrument().equals(cashInstrument)
                        && t.session().equals(sess)
                        && t.expiresAt().isAfter(now))
                .map(t -> termsResponse(t, acting))
                .toList();
    }

    /**
     * The venue posts an offer of the seat (operator-signed, publicly observable).
     *
     * <p>{@code eligible} is the auction's own participant roster, not a separate
     * membership class: if you may lodge an order in this book you may bid for the
     * right to see its residual, and {@code PublishImbalance} re-checks participation
     * at disclosure time so the roster and the seat cannot drift apart. There is no fee
     * anywhere on this path.
     */
    @PostMapping("/moc/mandate/terms")
    public ResponseEntity<Dtos.CidResponse> postMandateTerms(
            @Valid @RequestBody Dtos.PostMandateTermsRequest req) {
        String venue = ledger.resolveParty("Venue");
        String auditor = ledger.resolveParty("Auditor");
        String cash = blankTo(req.cashInstrument(), "USDC");
        String sess = LedgerCommands.session(req.session());
        BigDecimal anchor = anchorFor(venue, req.instrumentId(), cash, sess);
        BigDecimal commitment = req.commitmentSize() == null
                ? LedgerCommands.DEFAULT_COMMITMENT_SIZE : req.commitmentSize();
        long band = req.maxBandBps() == null
                ? LedgerCommands.DEFAULT_MAX_BAND_BPS : req.maxBandBps();
        int ttl = req.ttlHours() == null || req.ttlHours() <= 0 ? DEFAULT_TERMS_TTL_HOURS : req.ttlHours();
        String cid = ledger.submitForCreated(venue,
                LedgerCommands.postMandateTerms(venue, auditor, req.instrumentId(), cash, sess,
                        anchor, commitment, band, Instant.now().plus(Duration.ofHours(ttl)),
                        rosterFor(venue, req.instrumentId(), cash, sess)),
                LedgerCommands.mandateTermsTemplateId());
        return created(new Dtos.CidResponse(cid));
    }

    /**
     * A registered participant TAKES UP the seat → a signed {@code LiquidityMandate}.
     *
     * <p><b>The actor is the provider, never the venue.</b> That is the entire
     * difference from the field this replaced: the venue posted terms it was willing to
     * have taken by anybody on its roster, and the participant — not the venue — decides
     * to take them. Any number of participants may hold live mandates over the same book
     * at once, and each is shown the same number; competition, not paperwork, is what
     * disciplines the price the residual gets absorbed at.
     *
     * <p><b>Acceptance is BLIND.</b> Nothing about the resting book is visible from
     * here, and there is no other route to the imbalance, so a participant undertakes
     * the duty BEFORE it can see the number the duty is about. There is no
     * read-then-decide.
     */
    @PostMapping("/moc/mandate/accept")
    public ResponseEntity<Dtos.CidResponse> acceptMandate(
            @Valid @RequestBody Dtos.AcceptMandateRequest req) {
        String venue = ledger.resolveParty("Venue");
        String provider = ledger.resolveParty(req.provider());
        String cash = blankTo(req.cashInstrument(), "USDC");
        String sess = LedgerCommands.session(req.session());
        Instant now = Instant.now();

        // Resolve the offer server-side unless the caller named one (only useful when
        // several offers are live over one book).
        String termsCid = (req.termsCid() == null || req.termsCid().isBlank())
                ? ledger.mandateTermsVisibleTo(provider).stream()
                        .filter(t -> t.operator().equals(venue)
                                && t.instrumentId().equals(req.instrumentId())
                                && t.cashInstrument().equals(cash)
                                && t.session().equals(sess)
                                && t.expiresAt().isAfter(now)
                                && t.openTo(provider))
                        .map(LedgerService.MandateTermsView::contractId)
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(
                                "no open mandate terms for " + req.instrumentId() + " (" + sess
                                        + ") that " + LedgerService.labelOf(provider)
                                        + " may accept — it may already hold a mandate off these "
                                        + "terms, be barred for a missed commitment this session, "
                                        + "or not be a registered participant of this book"))
                : req.termsCid();

        String mandateCid = ledger.submitForCreated(provider,
                LedgerCommands.acceptMandateTerms(termsCid, provider),
                LedgerCommands.liquidityMandateTemplateId());
        return created(new Dtos.CidResponse(mandateCid));
    }

    /**
     * The acting party's OWN live obligation over a book: what it committed to, how far
     * from the anchor it undertook to stand, when the seat runs out, and the running
     * record of what it has actually been shown.
     */
    @GetMapping("/moc/mandate")
    public Dtos.MandateResponse myMandate(
            @RequestParam String instrumentId,
            @RequestParam(required = false, defaultValue = "USDC") String cashInstrument,
            @RequestParam(required = false, defaultValue = "Close") String session,
            @RequestParam(required = false) String actingAs) {
        String venue = ledger.resolveParty("Venue");
        String sess = LedgerCommands.session(session);
        String acting = (actingAs == null || actingAs.isBlank())
                ? venue : ledger.resolveParty(actingAs);
        Instant now = Instant.now();

        var forBook = ledger.mandatesVisibleTo(acting).stream()
                .filter(m -> m.provider().equals(acting)
                        && m.operator().equals(venue)
                        && m.instrumentId().equals(instrumentId)
                        && m.cashInstrument().equals(cashInstrument)
                        && m.session().equals(sess))
                .toList();
        var live = forBook.stream().filter(m -> m.liveAt(now)).findFirst();
        var chosen = live.or(() -> forBook.stream().findFirst());
        if (chosen.isEmpty()) {
            return new Dtos.MandateResponse(false, null, LedgerService.labelOf(acting),
                    instrumentId, cashInstrument, sess, null, null, 0L, null, null, null, 0L, false,
                    "no liquidity mandate over this book — accept the venue's open terms to take "
                            + "the seat, and with it sight of the residual");
        }
        var m = chosen.get();
        boolean held = live.isPresent();
        String note = held
                ? "live mandate: absorb up to " + m.commitmentSize().stripTrailingZeros().toPlainString()
                        + " " + instrumentId + " within " + m.maxBandBps() + "bps of "
                        + m.anchorPrice().stripTrailingZeros().toPlainString() + ", until " + m.expiresAt()
                : "this mandate expired at " + m.expiresAt()
                        + " — an expired obligation buys no disclosure";
        return new Dtos.MandateResponse(held, m.contractId(), LedgerService.labelOf(m.provider()),
                m.instrumentId(), m.cashInstrument(), m.session(), m.anchorPrice(),
                m.commitmentSize(), m.maxBandBps(), String.valueOf(m.expiresAt()), m.shownSide(),
                m.peakShownQty(), m.disclosuresSeen(), !held, note);
    }

    /** How long an unattended posted offer (and every mandate struck off it) runs. */
    private static final int DEFAULT_TERMS_TTL_HOURS = 24;

    private Dtos.MandateTermsResponse termsResponse(
            LedgerService.MandateTermsView t, String acting) {
        boolean open = t.openTo(acting);
        String note = open
                ? "open seat: absorb up to " + t.commitmentSize().stripTrailingZeros().toPlainString()
                        + " " + t.instrumentId() + " within " + t.maxBandBps() + "bps of "
                        + t.anchorPrice().stripTrailingZeros().toPlainString()
                        + " — accept to be shown the book's net residual"
                : t.barred().contains(acting)
                        ? "barred from these terms this session: a recorded MandatePerformance "
                                + "shows a commitment was missed"
                        : t.accepted().contains(acting)
                                ? "you already hold a live mandate off these terms"
                                : "only a registered participant of this book may take up the seat";
        return new Dtos.MandateTermsResponse(t.contractId(), t.instrumentId(), t.cashInstrument(),
                t.session(), t.anchorPrice(), t.commitmentSize(), t.maxBandBps(),
                String.valueOf(t.expiresAt()),
                t.eligible().stream().map(LedgerService::labelOf).toList(),
                t.accepted().stream().map(LedgerService::labelOf).toList(),
                t.barred().stream().map(LedgerService::labelOf).toList(),
                open, note);
    }

    /**
     * The anchor a mandate for this book must be struck against: the OPEN auction's
     * published {@code referencePrice} if there is one, else the instrument's published
     * reference. {@code PublishImbalance} asserts the two are equal, so guessing here
     * would produce terms whose mandates can never buy a disclosure.
     */
    private BigDecimal anchorFor(String venue, String instrumentId, String cash, String sess) {
        return ledger.auctionsVisibleTo(venue).stream()
                .filter(a -> a.isOpen() && a.instrumentId().equals(instrumentId)
                        && a.cashInstrument().equals(cash) && a.session().equals(sess))
                .map(LedgerService.AuctionView::referencePrice)
                .findFirst()
                .orElseGet(() -> ledger.referencePriceOf("Issuer", instrumentId)
                        .orElseThrow(() -> new IllegalArgumentException(instrumentId
                                + " has no published reference price to anchor a mandate to")));
    }

    /** Who may take the seat: the book's own participant roster, else every known party. */
    private List<String> rosterFor(String venue, String instrumentId, String cash, String sess) {
        var roster = ledger.auctionsVisibleTo(venue).stream()
                .filter(a -> a.isOpen() && a.instrumentId().equals(instrumentId)
                        && a.cashInstrument().equals(cash) && a.session().equals(sess))
                .map(LedgerService.AuctionView::participants)
                .filter(p -> !p.isEmpty())
                .findFirst();
        return roster.orElseGet(() -> ledger.listParties().stream()
                        .map(LedgerService.PartyView::party)
                        .filter(p -> !LedgerService.labelOf(p).equalsIgnoreCase("sandbox"))
                        .toList())
                .stream().distinct().toList();
    }

    /**
     * Post the standard offer for a book that has just been opened unattended, unless a
     * live one already stands.
     *
     * <p>A book with no posted terms has an UNCONTESTABLE seat — nobody can take it, so
     * nobody can ever be shown the residual. Opening a book and posting its terms belong
     * together. Failure is logged and swallowed: a missing offer must never take down
     * order submission, and the seat can always be posted explicitly afterwards.
     */
    private void ensureMandateTerms(String venue, String auditor, String instrumentId,
            String cash, String sess, BigDecimal anchor, List<String> participants) {
        try {
            Instant now = Instant.now();
            boolean alreadyPosted = ledger.mandateTermsVisibleTo(venue).stream()
                    .anyMatch(t -> t.operator().equals(venue)
                            && t.instrumentId().equals(instrumentId)
                            && t.cashInstrument().equals(cash)
                            && t.session().equals(sess)
                            && t.anchorPrice().compareTo(anchor) == 0
                            && t.expiresAt().isAfter(now));
            if (alreadyPosted) {
                return;
            }
            List<String> eligible = participants.stream().distinct().toList();
            if (eligible.isEmpty()) {
                return;   // MandateTerms.ensure rejects an empty roster, and rightly
            }
            ledger.submit(venue, LedgerCommands.postMandateTerms(venue, auditor, instrumentId,
                    cash, sess, anchor, LedgerCommands.DEFAULT_COMMITMENT_SIZE,
                    LedgerCommands.DEFAULT_MAX_BAND_BPS,
                    now.plus(Duration.ofHours(DEFAULT_TERMS_TTL_HOURS)), eligible));
        } catch (RuntimeException e) {
            log.warn("could not post mandate terms for {} ({}): {}", instrumentId, sess, e.getMessage());
        }
    }

    /**
     * The seed's conventional liquidity provider (Bank), if that party exists.
     *
     * <p>Still written onto {@code ClosingAuction.liquidityProvider}, which is now an
     * INERT field: no ledger code reads it and it designates nobody. Kept because the
     * field is still on the template and the UI shows it as historical context — the
     * seat itself is taken by accepting posted terms, by whoever wants it.
     */
    private Optional<String> defaultLiquidityProvider() {
        try {
            return Optional.of(ledger.resolveParty("Bank"));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    // ---- Market-on-Close --------------------------------------------------

    @PostMapping("/auction")
    public ResponseEntity<Dtos.CidResponse> openAuction(
            @Valid @RequestBody Dtos.OpenAuctionRequest req) {
        // Optionally designate a liquidity provider (resolved to a full party id).
        Optional<String> dlp = (req.liquidityProvider() == null || req.liquidityProvider().isBlank())
                ? Optional.empty()
                : Optional.of(ledger.resolveParty(req.liquidityProvider()));
        // Optionally bind the auction to a committee-attested NavFixing (its contract id).
        Optional<String> fixingRef = (req.fixingRef() == null || req.fixingRef().isBlank())
                ? Optional.empty()
                : Optional.of(req.fixingRef());
        String sess = LedgerCommands.session(req.session());
        var cmd = LedgerCommands.createAuction(
                req.operator(), req.auditor(), req.instrumentId(), req.cashInstrument(),
                sess, req.referencePrice(), req.participants(), dlp, fixingRef);
        String cid = ledger.submitForCreated(req.operator(), cmd, LedgerCommands.closingAuctionTemplateId());
        // Post the liquidity seat alongside the book, on the SAME anchor the auction
        // published — a book with no posted terms has a seat nobody can take, and so no
        // route by which anyone may ever be shown its residual.
        ensureMandateTerms(req.operator(), req.auditor(), req.instrumentId(),
                req.cashInstrument(), sess, req.referencePrice(), req.participants());
        return created(new Dtos.CidResponse(cid));
    }

    /**
     * Lodge a sealed order into an existing auction with a caller-supplied holding.
     *
     * <p>The order type comes from {@code orderType} / {@code limitPrice}: an absent or
     * null limit is an UNPRICED market-on-close order, not a bad request. The caller
     * owns the backing here, so it must size a BUY's holding itself — {@code quantity *
     * limit} for a limit order and {@code quantity * } the top of the venue's price
     * collar for a market order (see {@code LedgerCommands.buyReservation}). Prefer
     * {@code POST /api/moc/order}, which provisions the holding for you.
     */
    @PostMapping("/auction/{cid}/order")
    public ResponseEntity<Dtos.CidResponse> submitOrder(
            @PathVariable String cid, @Valid @RequestBody Dtos.SubmitOrderRequest req) {
        var side = LedgerCommands.side(req.side());
        var limitPrice = LedgerCommands.orderType(req.orderType(), req.limitPrice());
        var cmd = LedgerCommands.submitOrder(
                cid, req.trader(), side, req.quantity(), limitPrice, req.holdingCid());
        String orderCid = ledger.submitForCreated(req.trader(), cmd, LedgerCommands.sealedOrderTemplateId());
        return created(new Dtos.CidResponse(orderCid));
    }

    /**
     * Seal the window (CloseBidding) then run the uniform-price cross (RunClose)
     * over the supplied sealed orders. Two exercises submitted by the operator;
     * returns the sealed auction handle and the resulting SettlementBatch.
     *
     * <p><b>The caller must supply the COMPLETE book</b> — every live buy and sell for
     * this auction, with no duplicates. RunClose asserts the lists number exactly
     * {@code submittedCount - cancelledCount}, so a price-filtered subset aborts the
     * close. Prefer {@code POST /api/moc/{auctionCid}/close}, which assembles the book
     * for you.
     */
    @PostMapping("/auction/{cid}/close")
    public Dtos.CloseResponse close(
            @PathVariable String cid, @Valid @RequestBody Dtos.CloseAuctionRequest req) {
        // 1) Seal the order window; the sealed auction is a fresh contract id.
        String sealedCid = ledger.submitForCreated(
                req.operator(), LedgerCommands.closeBidding(cid),
                LedgerCommands.closingAuctionTemplateId());
        // 2) Cross the sealed book at the published close -> SettlementBatch.
        String batchCid = ledger.submitForCreated(
                req.operator(),
                LedgerCommands.runClose(sealedCid, req.buyOrderCids(), req.sellOrderCids()),
                LedgerCommands.settlementBatchTemplateId());
        return new Dtos.CloseResponse(batchCid, sealedCid);
    }

    // ---- Decentralised operator: committee-attested NAV -------------------

    /** Stand up a K-of-N NAV committee (the decentralised operator). */
    @PostMapping("/committee")
    public ResponseEntity<Dtos.CidResponse> createCommittee(
            @Valid @RequestBody Dtos.CreateCommitteeRequest req) {
        String admin = ledger.resolveParty(req.admin());
        String auditor = ledger.resolveParty(blankTo(req.auditor(), "Auditor"));
        List<String> members = req.members().stream().map(ledger::resolveParty).toList();
        var cmd = LedgerCommands.createCommittee(admin, members, req.threshold(), auditor,
                blankTo(req.label(), "NAV Committee"));
        String cid = ledger.submitForCreated(admin, cmd, LedgerCommands.operatorCommitteeTemplateId());
        return created(new Dtos.CidResponse(cid));
    }

    /** A member proposes an official price fix (it becomes the first attestor). */
    @PostMapping("/committee/{cid}/propose")
    public ResponseEntity<Dtos.CidResponse> proposeFixing(
            @PathVariable String cid, @Valid @RequestBody Dtos.ProposeFixingRequest req) {
        String proposer = ledger.resolveParty(req.proposer());
        var cmd = LedgerCommands.proposeFixing(cid, proposer, req.instrumentId(),
                blankTo(req.cashInstrument(), "USDC"), LedgerCommands.session(req.session()),
                req.price(), blankTo(req.rationale(), ""));
        String propCid = ledger.submitForCreated(proposer, cmd, LedgerCommands.fixingProposalTemplateId());
        return created(new Dtos.CidResponse(propCid));
    }

    /**
     * A member proposes an ACCRUING fix — the committee attests the INPUTS, the ledger
     * computes every later value.
     *
     * <p>A SEPARATE ENDPOINT FROM {@code /propose}, exactly as {@code ProposeAccruingFixing}
     * is a separate choice from {@code ProposeFixing}. The snapshot path above is
     * untouched and still produces a non-accruing mark; this one attests a rate, a
     * day-count convention and the instant the mark applies from, and from then on the
     * fund's value is derived from the clock rather than re-struck.
     *
     * <p>THE DAY COUNT IS VALIDATED HERE THE WAY THE LEDGER VALIDATES IT. Governance.daml
     * REJECTS an unsupported convention at proposal rather than defaulting one, because
     * silently falling back to ACT/360 on a typo would mis-accrue a sterling fund by
     * 1.389% of its yield forever, under a signed attestation saying otherwise. Mirroring
     * that check here turns a ledger abort (422, after a submission) into a form
     * rejection (400, with the reason and the supported list) — same verdict, better
     * error, and no wasted command.
     */
    @PostMapping("/committee/{cid}/propose-accruing")
    public ResponseEntity<Dtos.FixingProposalResponse> proposeAccruingFixing(
            @PathVariable String cid, @Valid @RequestBody Dtos.ProposeAccruingFixingRequest req) {
        String proposer = ledger.resolveParty(req.proposer());
        String dayCount = normaliseDayCount(req.dayCount());
        // Mirror the ledger's own gates BEFORE submitting anything.
        Accrual.validateRecipe(req.price(), req.ratePerAnnum(), dayCount);
        Instant accrualFrom = parseInstant(req.accrualFrom(), Instant.now());

        var cmd = LedgerCommands.proposeAccruingFixing(cid, proposer, req.instrumentId(),
                blankTo(req.cashInstrument(), "USDC"), LedgerCommands.session(req.session()),
                req.price(), blankTo(req.rationale(), ""),
                req.ratePerAnnum(), dayCount, accrualFrom);
        String propCid = ledger.submitForCreated(proposer, cmd, LedgerCommands.fixingProposalTemplateId());

        return created(new Dtos.FixingProposalResponse(propCid,
                req.instrumentId(), blankTo(req.cashInstrument(), "USDC"),
                LedgerCommands.session(req.session()),
                Accrual.wire(req.price()), Accrual.wire(req.ratePerAnnum()), dayCount,
                accrualFrom.toString(), Accrual.epochMicros(accrualFrom),
                req.ratePerAnnum().signum() != 0));
    }

    /**
     * Another member confirms — the accumulating-multisig step. Returns the NEW
     * proposal contract id (each confirmation archives and re-creates the proposal
     * with one more signature). Repeat until the threshold is reached.
     */
    @PostMapping("/fixing/{cid}/confirm")
    public ResponseEntity<Dtos.CidResponse> confirmFixing(
            @PathVariable String cid, @Valid @RequestBody Dtos.ConfirmFixingRequest req) {
        String member = ledger.resolveParty(req.member());
        String next = ledger.submitForCreated(member,
                LedgerCommands.confirmFixing(cid, member), LedgerCommands.fixingProposalTemplateId());
        return created(new Dtos.CidResponse(next));
    }

    /**
     * Finalise a threshold-attested proposal into an official NavFixing (fails with a
     * clear error if fewer than the committee threshold have attested). The fix is
     * disclosed to {@code publishTo} (e.g. the auction venue that will print at it).
     */
    @PostMapping("/fixing/{cid}/finalize")
    public ResponseEntity<Dtos.FinalizeFixingResponse> finalizeFixing(
            @PathVariable String cid, @Valid @RequestBody Dtos.FinalizeFixingRequest req) {
        String proposer = ledger.resolveParty(req.proposer());
        List<String> publishTo = (req.publishTo() == null ? List.<String>of() : req.publishTo())
                .stream().map(ledger::resolveParty).toList();
        String fixCid = ledger.submitForCreated(proposer,
                LedgerCommands.finalizeFixing(cid, publishTo), LedgerCommands.navFixingTemplateId());

        // PUBLISH THE ATTESTED MARK, or the fix is a number nothing values against.
        //
        // A NavFixing is the attestation; the fund's NAV per share is Σ(unitsPerShare ×
        // mark) read off the Instrument. Without this step those are two disconnected
        // numbers — the committee strikes a price and the basket keeps valuing at
        // whatever the issuer last published, which is exactly the single-venue mark the
        // committee exists to replace. So the official fix is written back to the
        // instrument, and create/redeem then happens AT the attested number.
        //
        // Deliberately NON-FATAL: the fixing itself is already final and disclosed. If
        // the mark cannot be republished (no such instrument, or we cannot act as its
        // issuer) that is reported in the response rather than failing a settled
        // attestation and leaving the caller unsure which half happened.
        var fix = ledger.navFixingById(proposer, fixCid);
        String note;
        boolean updated = false;
        BigDecimal newMark = fix.price();
        try {
            // Query AS THE ISSUER, not as the proposer. An Instrument is signed by its
            // issuer and observed by the depository — a committee member is neither, so
            // looking it up as the proposer returns nothing and the mark silently never
            // updates. Same convention as referencePriceOf, which the fund NAV uses.
            var ref = ledger.instrumentRefOf(ledger.resolveParty("Issuer"), fix.instrumentId());
            if (ref.isEmpty()) {
                note = "no live Instrument named " + fix.instrumentId()
                        + " — the fix stands, but nothing values against it yet";
            } else {
                ledger.submit(ref.get().issuer(),
                        LedgerCommands.setReferencePrice(ref.get().contractId(), newMark));
                updated = true;
                note = "mark republished at the attested fix";
                log.info("FIXING {} finalised; {} mark -> {} (attested by {} of {})",
                        fixCid, fix.instrumentId(), newMark, fix.attestors().size(), fix.threshold());
            }
        } catch (RuntimeException e) {
            note = "the fix is final, but the mark could not be republished: " + e.getMessage();
            log.warn("FIXING {} finalised but {} mark NOT updated: {}",
                    fixCid, fix.instrumentId(), e.toString());
        }
        return created(new Dtos.FinalizeFixingResponse(
                fixCid, fix.instrumentId(), newMark, updated, note));
    }

    // ---- Continuous accrual: what the fund is worth RIGHT NOW ---------------

    /**
     * The official fixes visible to the acting party, each with its accrual recipe and
     * the value derived from that recipe as of the desk's clock.
     *
     * <p>Queried AS the acting party, so the ledger's own disclosure rules decide what
     * comes back: a NavFixing reaches its attestors, the auditor, and exactly the venues
     * it was published to. An outsider sees an empty list.
     */
    @GetMapping("/fixings")
    public List<Dtos.FixingResponse> fixings(@RequestParam(required = false) String actingAs) {
        String acting = ledger.resolveParty(blankTo(actingAs, "Auditor"));
        Instant now = Instant.now();
        return ledger.navFixingsVisibleTo(acting).stream()
                .sorted(Comparator.comparing((LedgerService.NavFixingView v) -> v.finalizedAt()).reversed())
                .map(f -> toFixingResponse(f, now))
                .toList();
    }

    /**
     * THE ACCRUED NAV, WITH ITS WORKING SHOWN.
     *
     * <p>The value one share is worth at {@code at} (default: now), together with every
     * input it was derived from — attested base, rate, day-count convention, origin,
     * the convention's year length in microseconds, and the elapsed time — so a caller
     * can REPRODUCE the number rather than take it on trust. That matters more here
     * than anywhere else on this desk: the browser reproduces exactly this arithmetic
     * to tick the figure between polls, and a client that computed it a different
     * (equally reasonable) way would drift away from the ledger.
     *
     * <p>It also answers the auction question. {@code RunClose} requires the auction's
     * anchor to be consistent with the NAV accrued to the close — at or below it, and no
     * more than one basis point behind. Pass {@code anchor} to judge a specific number;
     * omit it and the desk finds the live auction for this fix's instrument/session and
     * judges the anchor it actually published. Either way the verdict comes from
     * {@code Accrual.anchorConsistentWithNav}, which is Governance.daml's function.
     *
     * @param at optional ISO-8601 instant to value at (a backfill or a replay); default now
     */
    @GetMapping("/fixing/{cid}/nav")
    public Dtos.AccruedNavResponse accruedNav(
            @PathVariable String cid,
            @RequestParam(required = false) String actingAs,
            @RequestParam(required = false) String at,
            @RequestParam(required = false) BigDecimal anchor) {
        String acting = ledger.resolveParty(blankTo(actingAs, "Auditor"));
        LedgerService.NavFixingView f = ledger.navFixingById(acting, cid);

        Instant asOf = parseInstant(at, Instant.now());
        long elapsed = Accrual.elapsedMicrosFrom(f.accrualFrom(), asOf);
        long yearMicros = Accrual.dayCountYearMicros(f.dayCount()).orElse(360L * Accrual.MICROS_PER_DAY);
        BigDecimal accrued = Accrual.accruedAmount(f.price(), f.ratePerAnnum(), f.dayCount(), elapsed);
        BigDecimal navNow = Accrual.navAt(f.price(), f.ratePerAnnum(), f.dayCount(), f.accrualFrom(), asOf);
        // What a WHOLE DAY of this recipe adds — the "1bp/day at 3.6% ACT/360" number,
        // computed rather than quoted, so the staleness budget below reads as a duration.
        BigDecimal perDay = Accrual.accruedAmount(
                f.price(), f.ratePerAnnum(), f.dayCount(), Accrual.MICROS_PER_DAY);

        // ---- the auction binding ------------------------------------------
        BigDecimal anchorPrice = anchor;
        String anchorCid = null;
        String note;
        if (anchorPrice == null) {
            var live = ledger.auctionsVisibleTo(acting).stream()
                    .filter(a -> a.instrumentId().equals(f.instrumentId())
                            && a.cashInstrument().equals(f.cashInstrument())
                            && a.session().equals(f.session()))
                    .findFirst();
            if (live.isPresent()) {
                anchorPrice = live.get().referencePrice();
                anchorCid = live.get().contractId();
            }
        }
        String anchorDrift = null;
        String staleBudget = null;
        Boolean consistent = null;
        if (anchorPrice == null) {
            note = "no live auction is anchored on this fix — the binding is checked at RunClose";
        } else {
            BigDecimal drift = Accrual.scaled(navNow).subtract(Accrual.scaled(anchorPrice));
            BigDecimal budget = Accrual.staleBudget(navNow);
            consistent = Accrual.anchorConsistentWithNav(navNow, anchorPrice);
            anchorDrift = Accrual.wire(drift);
            staleBudget = Accrual.wire(budget);
            if (consistent) {
                note = "anchor is within the 1bp staleness budget — RunClose would accept it";
            } else if (drift.signum() < 0) {
                // Above the accrual: not staleness at all, and no elapsed time explains it.
                note = "anchor is AHEAD of the accrued NAV — the venue is pricing value the fund "
                        + "has not earned. RunClose rejects this outright.";
            } else {
                note = "anchor is more than 1bp behind the accrued NAV — stale. RunClose rejects it "
                        + "until the anchor is re-published or a fresher fix is struck.";
            }
        }

        return new Dtos.AccruedNavResponse(
                f.contractId(), f.instrumentId(), f.cashInstrument(), f.session(),
                Accrual.wire(f.price()), Accrual.wire(f.ratePerAnnum()), f.dayCount(),
                f.accrualFrom().toString(), Accrual.epochMicros(f.accrualFrom()),
                asOf.toString(), Accrual.epochMicros(asOf), elapsed, yearMicros,
                Accrual.wire(accrued), Accrual.wire(navNow), Accrual.wire(perDay), f.accruing(),
                f.attestors().stream().map(LedgerService::labelOf).toList(), f.threshold(),
                anchorPrice == null ? null : Accrual.wire(anchorPrice), anchorCid,
                anchorDrift, staleBudget, consistent, note);
    }

    /** Flatten a fixing + the value derived from its recipe at {@code now}. */
    private static Dtos.FixingResponse toFixingResponse(LedgerService.NavFixingView f, Instant now) {
        BigDecimal navNow = Accrual.navAt(f.price(), f.ratePerAnnum(), f.dayCount(), f.accrualFrom(), now);
        return new Dtos.FixingResponse(
                f.contractId(),
                f.attestors().stream().map(LedgerService::labelOf).toList(), f.threshold(),
                f.instrumentId(), f.cashInstrument(), f.session(),
                Accrual.wire(f.price()), f.rationale(),
                Accrual.wire(f.ratePerAnnum()), f.dayCount(),
                f.accrualFrom().toString(), Accrual.epochMicros(f.accrualFrom()),
                f.finalizedAt().toString(),
                f.publishedTo().stream().map(LedgerService::labelOf).toList(),
                f.accruing(),
                Accrual.wire(navNow),
                Accrual.wire(Accrual.scaled(navNow).subtract(Accrual.scaled(f.price()))),
                now.toString(), Accrual.epochMicros(now),
                Accrual.elapsedMicrosFrom(f.accrualFrom(), now));
    }

    /**
     * Normalise a day-count convention for comparison, WITHOUT being helpful about it.
     * Case and surrounding whitespace are noise; "ACT/ACT" is not a typo of anything and
     * is refused by {@link Accrual#validateRecipe} rather than nudged into ACT/360.
     */
    private static String normaliseDayCount(String raw) {
        return raw == null ? "" : raw.trim().toUpperCase().replace(" ", "");
    }

    /** Parse an optional ISO-8601 instant, else {@code fallback}. A bad one is a 400. */
    private static Instant parseInstant(String raw, Instant fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Instant.parse(raw.trim());
        } catch (java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "not an ISO-8601 instant (e.g. 2026-08-05T14:00:00Z): " + raw);
        }
    }

    // ---- Basket / ETF builder ---------------------------------------------

    /** Define a basket (ETF): its creation unit and authorised participants. */
    @PostMapping("/basket")
    public ResponseEntity<Dtos.BasketResponse> defineBasket(
            @Valid @RequestBody Dtos.DefineBasketRequest req) {
        String admin = ledger.resolveParty(req.administrator());
        String auditor = ledger.resolveParty(blankTo(req.auditor(), "Auditor"));
        String cash = blankTo(req.cashInstrument(), "USDC");
        var comps = req.components().stream()
                .map(c -> LedgerCommands.basketComponent(c.instrumentId(), c.unitsPerShare()))
                .toList();
        List<String> parts = req.participants().stream().map(ledger::resolveParty).toList();
        var cmd = LedgerCommands.createBasket(admin, auditor, req.basketId(),
                blankTo(req.description(), req.basketId()), cash, comps, parts);
        String cid = ledger.submitForCreated(admin, cmd, LedgerCommands.basketDefinitionTemplateId());
        return created(new Dtos.BasketResponse(cid, req.basketId(), req.administrator(), cash,
                req.components(), req.participants()));
    }

    /** The baskets visible to the acting party (defaults to the auditor's full view). */
    @GetMapping("/baskets")
    public List<Dtos.BasketResponse> baskets(@RequestParam(required = false) String actingAs) {
        String acting = (actingAs == null || actingAs.isBlank())
                ? ledger.resolveParty("Auditor") : ledger.resolveParty(actingAs);
        return ledger.basketsVisibleTo(acting).stream()
                .map(b -> new Dtos.BasketResponse(b.contractId(), b.basketId(),
                        LedgerService.labelOf(b.administrator()), b.cashInstrument(),
                        b.components().stream()
                                .map(c -> new Dtos.ComponentDto(c.instrumentId(), c.unitsPerShare()))
                                .toList(),
                        b.participants().stream().map(LedgerService::labelOf).toList()))
                .toList();
    }

    /**
     * IN-KIND CREATION — one call. The desk resolves the exact underlying basket
     * (unitsPerShare × shares of each component) from the AP's holdings, then runs
     * request (AP) → approve (administrator) → process (administrator). The underlyings
     * move into custody and the shares are minted to the AP, atomically. Returns the
     * minted-shares handle, the receipt, and the current NAV per share.
     */
    @PostMapping("/basket/create")
    public Dtos.BasketCreateResponse createBasketUnits(
            @Valid @RequestBody Dtos.BasketCreateRequest req) {
        String ap = ledger.resolveParty(req.ap());
        var basket = basketByIdVisibleTo(ap, req.basketId());
        String admin = basket.administrator();
        BigDecimal shares = req.shares();
        log.info("BASKET CREATE start basket={} ap={} admin={} shares={} components={}",
                req.basketId(), ap, admin, shares, basket.components().size());

        // Provision each underlying leg to the exact creation-unit amount, in order.
        List<String> componentCids = new ArrayList<>();
        for (var c : basket.components()) {
            componentCids.add(ledger.provisionExactHolding(
                    ap, c.instrumentId(), c.unitsPerShare().multiply(shares)));
        }

        String orderCid = ledger.submitForCreated(ap,
                LedgerCommands.requestCreation(basket.contractId(), ap, shares, componentCids),
                LedgerCommands.creationOrderTemplateId());
        String agreementCid = ledger.submitForCreated(admin,
                LedgerCommands.approveCreation(orderCid),
                LedgerCommands.creationAgreementTemplateId());
        Transaction tree = ledger.submit(admin, LedgerCommands.processCreation(agreementCid));

        List<String> receipts = ledger.createdOf(tree, LedgerCommands.basketReceiptTemplateId());
        String mintedCid = ledger.createdHoldingsOf(tree).stream()
                .filter(h -> h.instrumentId().equals(req.basketId()) && h.owner().equals(ap))
                .map(LedgerService.HoldingView::contractId).findFirst().orElse(null);
        BigDecimal nav = navPerShareOf(basket).orElse(null);
        log.info("BASKET CREATE done basket={} order={} agreement={} receipt={} mintedShares={}",
                req.basketId(), orderCid, agreementCid,
                receipts.isEmpty() ? "NONE" : receipts.get(0), mintedCid);
        return new Dtos.BasketCreateResponse(
                receipts.isEmpty() ? null : receipts.get(0), mintedCid, shares, nav);
    }

    /**
     * IN-KIND REDEMPTION — one call. The AP commits {@code shares} of the basket
     * token; the administrator supplies the exact custody underlyings; then
     * request (AP) → approve (administrator) → process (administrator) burns the shares
     * and returns the underlyings, atomically. Returns the receipt and the returned
     * underlying holdings.
     */
    @PostMapping("/basket/redeem")
    public Dtos.BasketRedeemResponse redeemBasketUnits(
            @Valid @RequestBody Dtos.BasketRedeemRequest req) {
        String ap = ledger.resolveParty(req.ap());
        var basket = basketByIdVisibleTo(ap, req.basketId());
        String admin = basket.administrator();
        BigDecimal shares = req.shares();
        log.info("BASKET REDEEM start basket={} ap={} admin={} shares={}",
                req.basketId(), ap, admin, shares);

        String basketHoldingCid = ledger.provisionAtLeastHolding(ap, req.basketId(), shares);
        String orderCid = ledger.submitForCreated(ap,
                LedgerCommands.requestRedemption(basket.contractId(), ap, shares, basketHoldingCid),
                LedgerCommands.redemptionOrderTemplateId());

        // The administrator provides the exact custody underlyings to return.
        List<String> custodyCids = new ArrayList<>();
        for (var c : basket.components()) {
            custodyCids.add(ledger.provisionExactHolding(
                    admin, c.instrumentId(), c.unitsPerShare().multiply(shares)));
        }
        String agreementCid = ledger.submitForCreated(admin,
                LedgerCommands.approveRedemption(orderCid, custodyCids),
                LedgerCommands.redemptionAgreementTemplateId());
        Transaction tree = ledger.submit(admin, LedgerCommands.processRedemption(agreementCid));

        List<String> receipts = ledger.createdOf(tree, LedgerCommands.basketReceiptTemplateId());
        List<String> returned = ledger.createdHoldingsOf(tree).stream()
                .filter(h -> h.owner().equals(ap) && !h.instrumentId().equals(req.basketId()))
                .map(LedgerService.HoldingView::contractId).toList();
        log.info("BASKET REDEEM done basket={} order={} agreement={} receipt={} returnedLegs={}",
                req.basketId(), orderCid, agreementCid,
                receipts.isEmpty() ? "NONE" : receipts.get(0), returned.size());
        return new Dtos.BasketRedeemResponse(
                receipts.isEmpty() ? null : receipts.get(0), shares, returned);
    }

    /**
     * NAV per share = Σ (unitsPerShare × close price), with the per-component
     * breakdown. Marks are the instruments' published reference/close prices — in
     * this desk, struck by the K-of-N committee — so the NAV is credibly neutral.
     */
    @GetMapping("/basket/nav")
    public Dtos.NavResponse basketNav(
            @RequestParam String basketId, @RequestParam(required = false) String actingAs) {
        String acting = (actingAs == null || actingAs.isBlank())
                ? ledger.resolveParty("Auditor") : ledger.resolveParty(actingAs);
        var basket = basketByIdVisibleTo(acting, basketId);
        List<Dtos.NavLeg> legs = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        boolean complete = true;
        for (var c : basket.components()) {
            BigDecimal price = ledger.referencePriceOf("Issuer", c.instrumentId()).orElse(null);
            BigDecimal value = price == null ? null : c.unitsPerShare().multiply(price);
            if (price == null) {
                complete = false;
            } else {
                total = total.add(value);
            }
            legs.add(new Dtos.NavLeg(c.instrumentId(), c.unitsPerShare(), price, value));
        }
        return new Dtos.NavResponse(basketId, complete ? total : null,
                basket.cashInstrument(), legs, complete);
    }

    /**
     * THE TWO NAVs A FUND ACTUALLY HAS, side by side.
     *
     * <p>A real ETF runs both and they are not in competition:
     * <ul>
     *   <li><b>Official NAV</b> — struck from attested marks, signed, and the number
     *       creations and redemptions legally settle at. It moves only when the
     *       committee strikes it.</li>
     *   <li><b>Indicative NAV (iNAV)</b> — recomputed continuously from current
     *       market data, informational, binding on nobody. Exchanges disseminate one
     *       roughly every fifteen seconds.</li>
     * </ul>
     *
     * <p>Marking a volatile asset once a day and settling against it all the next day
     * would be indefensible — which is exactly why the indicative side exists. And
     * publishing a streamed number as the official one would be equally wrong: it is
     * the signatures, not the freshness, that make a NAV bindable.
     *
     * <p>Each leg is valued by what that asset actually is:
     * <ul>
     *   <li>a <b>wrapped crypto</b> leg → live spot for its underlying (BitSafe's own
     *       integration guidance for cBTC is a BTC-USD feed);</li>
     *   <li>a <b>money-market</b> leg → its accrued value from the committee's recipe.
     *       It has no live price to stream: an MMF's NAV is struck and then <i>earned</i>,
     *       so {@code navAt} derives it at this instant. That is not a stale number —
     *       it is the correct one.</li>
     * </ul>
     * A leg with neither a feed nor an accrual falls back to its attested mark, and
     * {@code live} says whether anything actually moved.
     */
    @GetMapping("/basket/nav/indicative")
    public Dtos.IndicativeNavResponse basketIndicativeNav(
            @RequestParam String basketId, @RequestParam(required = false) String actingAs) {
        String acting = (actingAs == null || actingAs.isBlank())
                ? ledger.resolveParty("Auditor") : ledger.resolveParty(actingAs);
        var basket = basketByIdVisibleTo(acting, basketId);
        Instant asOf = Instant.now();

        // The committee's accruing fixes, newest first, so a money-market leg is valued
        // from the most recent recipe its attestors signed.
        var fixings = ledger.navFixingsVisibleTo(acting);

        List<Dtos.IndicativeNavLeg> legs = new ArrayList<>();
        BigDecimal official = BigDecimal.ZERO;
        BigDecimal indicative = BigDecimal.ZERO;
        boolean complete = true;
        boolean anyLive = false;

        for (var c : basket.components()) {
            BigDecimal mark = ledger.referencePriceOf("Issuer", c.instrumentId()).orElse(null);

            BigDecimal now = null;
            String basis = "attested mark";
            var feed = marketData.liveMarkOf(c.instrumentId());
            if (feed.isPresent()) {
                now = feed.get().price();
                basis = feed.get().source() + " " + feed.get().symbol();
                anyLive = true;
            } else {
                var accruing = fixings.stream()
                        .filter(f -> f.instrumentId().equals(c.instrumentId()) && f.accruing())
                        .max(Comparator.comparing(LedgerService.NavFixingView::finalizedAt));
                if (accruing.isPresent()) {
                    var f = accruing.get();
                    now = Accrual.navAt(f.price(), f.ratePerAnnum(), f.dayCount(), f.accrualFrom(), asOf);
                    basis = "accrued @ " + f.ratePerAnnum() + "/yr " + f.dayCount();
                    anyLive = true;
                } else {
                    now = mark;
                }
            }

            BigDecimal officialValue = mark == null ? null : c.unitsPerShare().multiply(mark);
            BigDecimal indicativeValue = now == null ? null : c.unitsPerShare().multiply(now);
            if (mark == null || now == null) {
                complete = false;
            } else {
                official = official.add(officialValue);
                indicative = indicative.add(indicativeValue);
            }
            legs.add(new Dtos.IndicativeNavLeg(
                    c.instrumentId(), c.unitsPerShare(), mark, officialValue, now, indicativeValue, basis));
        }

        BigDecimal drift = null;
        if (complete && official.signum() != 0) {
            drift = indicative.subtract(official)
                    .multiply(new BigDecimal("10000"))
                    .divide(official, 2, java.math.RoundingMode.HALF_EVEN);
        }
        return new Dtos.IndicativeNavResponse(
                basketId, basket.cashInstrument(),
                complete ? official : null, complete ? indicative : null,
                drift, legs, complete, anyLive, asOf.toString());
    }

    /** Find a basket by id among those visible to a party, else a clear 400. */
    private LedgerService.BasketView basketByIdVisibleTo(String party, String basketId) {
        return ledger.basketsVisibleTo(party).stream()
                .filter(b -> b.basketId().equals(basketId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "no basket '" + basketId + "' is visible to " + LedgerService.labelOf(party)));
    }

    /** NAV per share from the components' published close prices, or empty if any mark is missing. */
    private Optional<BigDecimal> navPerShareOf(LedgerService.BasketView basket) {
        BigDecimal total = BigDecimal.ZERO;
        for (var c : basket.components()) {
            Optional<BigDecimal> p = ledger.referencePriceOf("Issuer", c.instrumentId());
            if (p.isEmpty()) {
                return Optional.empty();
            }
            total = total.add(c.unitsPerShare().multiply(p.get()));
        }
        return Optional.of(total);
    }

    // ---- helpers ----------------------------------------------------------

    private static <T> ResponseEntity<T> created(T body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    private static String blankTo(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    @SuppressWarnings("unused")
    private static BigDecimal req(BigDecimal v) {
        return v;
    }
}
