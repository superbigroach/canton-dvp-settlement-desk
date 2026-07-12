package com.lucilla.settlement.web;

import com.daml.ledger.javaapi.data.TransactionTree;
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
 * </pre>
 */
@RestController
@RequestMapping("/api")
public class SettlementController {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(SettlementController.class);

    private final LedgerService ledger;

    public SettlementController(LedgerService ledger) {
        this.ledger = ledger;
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
        String depository = blankTo(req.depository(), req.issuer());
        String version = blankTo(req.version(), "1");
        String description = req.description() == null ? "" : req.description();
        var cmd = LedgerCommands.createInstrument(
                req.issuer(), depository, req.id(), version, req.kind(), description,
                Optional.ofNullable(req.referencePrice()));
        String cid = ledger.submitForCreated(req.issuer(), cmd, LedgerCommands.instrumentTemplateId());
        return created(new Dtos.CidResponse(cid));
    }

    // ---- Holdings ---------------------------------------------------------

    @PostMapping("/holdings")
    public ResponseEntity<Dtos.CidResponse> issueHolding(
            @Valid @RequestBody Dtos.IssueHoldingRequest req) {
        var cmd = LedgerCommands.createHolding(
                req.issuer(), req.instrumentId(), req.owner(), req.amount());
        String cid = ledger.submitForCreated(req.issuer(), cmd, LedgerCommands.holdingTemplateId());
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
        TransactionTree tree = ledger.submit(req.proposer(), cmd);
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
        TransactionTree tree = ledger.submit(seller, LedgerCommands.settleAgreement(agreementCid));
        List<String> receipts = ledger.createdOf(tree, LedgerCommands.settlementReceiptTemplateId());

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

        // Find an open auction for this instrument/cash/session, else open a fresh one
        // whose participant set is every known trader (so anyone in the picker can join).
        // The cross price is NEVER supplied by the trader: it is the instrument's
        // published reference price ("price is what it is at the open/close"). Opening
        // (MOO) and closing (MOC) sessions rest in SEPARATE books.
        var open = ledger.auctionsVisibleTo(venue).stream()
                .filter(a -> a.isOpen()
                        && a.instrumentId().equals(req.instrumentId())
                        && a.cashInstrument().equals(cashInstrument)
                        && a.session().equals(session))
                .findFirst();
        String auctionCid;
        BigDecimal closingPrice;
        boolean opened;
        if (open.isPresent()) {
            auctionCid = open.get().contractId();
            closingPrice = open.get().referencePrice();
            opened = false;
        } else {
            closingPrice = ledger.referencePriceOf("Issuer", req.instrumentId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            req.instrumentId() + " has no published reference price to close at"));
            List<String> participants = ledger.listParties().stream()
                    .map(p -> p.party())
                    .filter(p -> !LedgerService.labelOf(p).equalsIgnoreCase("sandbox"))
                    .toList();
            // Designate the seed's liquidity provider (Bank) as this auction's DLP, so
            // the venue can disclose the NET imbalance to it — and to nobody else — to
            // attract offsetting liquidity without leaking the book. Absent a Bank party
            // the auction simply opens with no DLP (a plain dark pool).
            Optional<String> dlp = defaultLiquidityProvider();
            auctionCid = ledger.submitForCreated(venue,
                    LedgerCommands.createAuction(venue, auditor, req.instrumentId(),
                            cashInstrument, session, closingPrice, participants, dlp),
                    LedgerCommands.closingAuctionTemplateId());
            opened = true;
        }

        // The trader sets no price; the order's limit is pinned to the close so it
        // always crosses (Buy limit >= close, Sell limit <= close are both met).
        BigDecimal limitPrice = closingPrice;

        // Pre-commit the trader's holding: cash worth qty*close for a Buy, else the asset.
        String holdingCid = isBuy
                ? ledger.provisionAtLeastHolding(trader, cashInstrument,
                        req.quantity().multiply(closingPrice))
                : ledger.provisionAtLeastHolding(trader, req.instrumentId(), req.quantity());

        String orderCid = ledger.submitForCreated(trader,
                LedgerCommands.submitOrder(auctionCid, trader, sideEnum,
                        req.quantity(), limitPrice, holdingCid),
                LedgerCommands.sealedOrderTemplateId());
        return created(new Dtos.MocOrderResponse(orderCid, auctionCid, opened, closingPrice));
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
                .map(o -> new Dtos.MocOrderView(
                        o.contractId(), o.trader(), o.side(), o.quantity(), o.limitPrice()))
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
     * close (exercise Cancel, actAs the trader). The reserved backing is unlocked
     * back to a free, private holding. Only the order's owner may withdraw it.
     */
    @PostMapping("/moc/order/{orderCid}/withdraw")
    public ResponseEntity<Dtos.CidResponse> withdrawOrder(
            @PathVariable String orderCid, @Valid @RequestBody Dtos.WithdrawOrderRequest req) {
        String trader = ledger.resolveParty(req.trader());
        TransactionTree tree = ledger.submit(trader, LedgerCommands.cancelOrder(orderCid));
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
        int cleared = 0;
        for (var o : orders) {
            try {
                ledger.submit(venue, LedgerCommands.venueCancelOrder(o.contractId()));
                cleared++;
            } catch (RuntimeException e) {
                // A concurrently-consumed order is already gone — skip it cleanly
                // rather than failing the whole clear.
                log.warn("skip clearing order {}: {}", o.contractId(), e.getMessage());
            }
        }
        return new Dtos.ClearBookResponse(cleared);
    }

    /**
     * Run the close as the venue: auto-discover the eligible sealed orders for this
     * auction, seal the window, and cross them at the uniform closing price. Returns
     * the batch id and the fills.
     */
    @PostMapping("/moc/{auctionCid}/close")
    public Dtos.MocCloseResponse mocClose(@PathVariable String auctionCid) {
        String venue = ledger.resolveParty("Venue");
        var auction = ledger.auctionsVisibleTo(venue).stream()
                .filter(a -> a.contractId().equals(auctionCid))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("no such auction: " + auctionCid));
        BigDecimal close = auction.referencePrice();

        var orders = ledger.sealedOrdersVisibleTo(venue).stream()
                .filter(o -> o.operator().equals(venue)
                        && o.instrumentId().equals(auction.instrumentId())
                        && o.cashInstrument().equals(auction.cashInstrument())
                        && o.session().equals(auction.session()))
                .toList();
        // Only orders that are in-the-money at the close cross; the rest would abort it.
        List<String> buyCids = orders.stream()
                .filter(o -> o.side().equalsIgnoreCase("Buy") && o.limitPrice().compareTo(close) >= 0)
                .map(com.lucilla.settlement.ledger.LedgerService.OrderView::contractId).toList();
        List<String> sellCids = orders.stream()
                .filter(o -> o.side().equalsIgnoreCase("Sell") && o.limitPrice().compareTo(close) <= 0)
                .map(com.lucilla.settlement.ledger.LedgerService.OrderView::contractId).toList();
        if (buyCids.isEmpty() || sellCids.isEmpty()) {
            throw new IllegalArgumentException("the close needs at least one eligible buy AND one "
                    + "eligible sell at the closing price " + close.stripTrailingZeros().toPlainString()
                    + " (have " + buyCids.size() + " buys, " + sellCids.size() + " sells)");
        }

        String sealedCid = ledger.submitForCreated(venue,
                LedgerCommands.closeBidding(auctionCid), LedgerCommands.closingAuctionTemplateId());
        TransactionTree tree = ledger.submit(venue,
                LedgerCommands.runClose(sealedCid, buyCids, sellCids));
        var batch = ledger.batchOf(tree).orElseThrow(() ->
                new IllegalStateException("close produced no settlement batch"));
        List<Dtos.MocFillView> fills = batch.fills().stream()
                .map(f -> new Dtos.MocFillView(f.trader(), f.side(), f.quantity(), f.price()))
                .toList();
        return new Dtos.MocCloseResponse(batch.contractId(), auction.session(),
                batch.closingPrice(), fills);
    }

    // ---- Designated Liquidity Provider: selective imbalance disclosure -----

    /**
     * The NET imbalance of the sealed book — the DESIGNATED LIQUIDITY PROVIDER view.
     *
     * <p>Liquidity without leakage: a lit auction publishes the imbalance to the
     * whole market (leaking it); a dark pool leaks nothing but risks a no-fill. This
     * endpoint discloses the AGGREGATE net imbalance to exactly ONE party — the
     * auction's designated liquidity provider — and to the venue, and to nobody else.
     *
     * <p>Enforcement is at the LEDGER, not here. The venue first refreshes the
     * {@code ImbalanceDisclosure} from the current book (it is observable only by the
     * DLP), then we read it back <b>as the acting party</b>: the DLP and the venue see
     * it; a normal trader sees nothing and gets {@code 403}. Individual orders and
     * trader identities are never revealed — only side + magnitude.
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

        // 1) Find the open auction (as venue) for this book that HAS a designated DLP.
        var open = ledger.auctionsVisibleTo(venue).stream()
                .filter(a -> a.isOpen()
                        && a.instrumentId().equals(instrumentId)
                        && a.cashInstrument().equals(cashInstrument)
                        && a.session().equals(sess))
                .findFirst();
        if (open.isEmpty() || open.get().liquidityProvider() == null) {
            // No DLP auction open → there is nothing to disclose to anyone.
            return ResponseEntity.ok(new Dtos.MocImbalanceResponse(false, instrumentId,
                    cashInstrument, sess, "Flat", BigDecimal.ZERO, open.map(
                            com.lucilla.settlement.ledger.LedgerService.AuctionView::referencePrice)
                            .orElse(null), null,
                    "no designated-liquidity-provider auction is open for this book"));
        }
        var a = open.get();
        String lpLabel = LedgerService.labelOf(a.liquidityProvider());

        // 2) As the VENUE, refresh the disclosure from the CURRENT book: archive any
        //    stale snapshot, then recompute + publish the net aggregate. The created
        //    ImbalanceDisclosure is observable only by the DLP (and the venue).
        for (var stale : ledger.imbalancesVisibleTo(venue)) {
            if (stale.operator().equals(venue) && stale.instrumentId().equals(instrumentId)
                    && stale.cashInstrument().equals(cashInstrument) && stale.session().equals(sess)) {
                try {
                    ledger.submit(venue, LedgerCommands.archiveImbalance(stale.contractId()));
                } catch (RuntimeException e) {
                    log.warn("skip archiving stale imbalance {}: {}", stale.contractId(), e.getMessage());
                }
            }
        }
        List<String> restingCids = ledger.sealedOrdersVisibleTo(venue).stream()
                .filter(o -> o.operator().equals(venue)
                        && o.instrumentId().equals(instrumentId)
                        && o.cashInstrument().equals(cashInstrument)
                        && o.session().equals(sess))
                .map(com.lucilla.settlement.ledger.LedgerService.OrderView::contractId).toList();
        ledger.submit(venue, LedgerCommands.publishImbalance(a.contractId(), restingCids));

        // 3) As the ACTING PARTY, read the disclosure. The LEDGER enforces who may see
        //    it: the DLP and the venue do; any other trader sees nothing → 403.
        var visible = ledger.imbalancesVisibleTo(acting).stream()
                .filter(d -> d.operator().equals(venue) && d.instrumentId().equals(instrumentId)
                        && d.cashInstrument().equals(cashInstrument) && d.session().equals(sess))
                .findFirst();
        if (visible.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new Dtos.MocImbalanceResponse(
                    false, instrumentId, cashInstrument, sess, null, null, a.referencePrice(),
                    lpLabel, "the net imbalance is disclosed only to the designated liquidity "
                            + "provider (" + lpLabel + ") and the venue"));
        }
        var d = visible.get();
        String mag = d.netQuantity().stripTrailingZeros().toPlainString();
        String note = "Flat".equals(d.netSide())
                ? "book is balanced — no offsetting liquidity needed"
                : "net " + d.netSide().toUpperCase() + " imbalance of " + mag + " " + instrumentId
                        + " @ " + d.referencePrice().stripTrailingZeros().toPlainString()
                        + " - offset to clear the cross";
        return ResponseEntity.ok(new Dtos.MocImbalanceResponse(true, instrumentId, cashInstrument,
                sess, d.netSide(), d.netQuantity(), d.referencePrice(), lpLabel, note));
    }

    /** The seed's default Designated Liquidity Provider (Bank), if that party exists. */
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
        var cmd = LedgerCommands.createAuction(
                req.operator(), req.auditor(), req.instrumentId(), req.cashInstrument(),
                LedgerCommands.session(req.session()), req.referencePrice(), req.participants(), dlp);
        String cid = ledger.submitForCreated(req.operator(), cmd, LedgerCommands.closingAuctionTemplateId());
        return created(new Dtos.CidResponse(cid));
    }

    @PostMapping("/auction/{cid}/order")
    public ResponseEntity<Dtos.CidResponse> submitOrder(
            @PathVariable String cid, @Valid @RequestBody Dtos.SubmitOrderRequest req) {
        var side = LedgerCommands.side(req.side());
        var cmd = LedgerCommands.submitOrder(
                cid, req.trader(), side, req.quantity(), req.limitPrice(), req.holdingCid());
        String orderCid = ledger.submitForCreated(req.trader(), cmd, LedgerCommands.sealedOrderTemplateId());
        return created(new Dtos.CidResponse(orderCid));
    }

    /**
     * Seal the window (CloseBidding) then run the uniform-price cross (RunClose)
     * over the supplied sealed orders. Two exercises submitted by the operator;
     * returns the sealed auction handle and the resulting SettlementBatch.
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
