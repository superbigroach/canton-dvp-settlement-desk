package com.lucilla.settlement.web;

import com.daml.ledger.javaapi.data.Transaction;
import com.lucilla.settlement.ledger.LedgerCommands;
import com.lucilla.settlement.ledger.LedgerService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * THE CONTINUOUS SESSION — the desk's other half.
 *
 * <p>The auction collects sealed interest and prints once. This is where limit
 * interest <b>rests</b> between those prints, matched by price then time, settled
 * bilaterally at the maker's price the instant it is crossed. It exists because a
 * closing auction does not manufacture a price out of nothing — it inherits one from
 * the resting ladder, which is exactly what unpriced market-on-close flow walks into.
 * See the header of {@code daml/ContinuousBook.daml} for the full argument.
 *
 * <pre>
 *   POST /api/book/session          open a session for an instrument (idempotent)
 *   GET  /api/book/state            the ladder AS the acting party sees it
 *   POST /api/book/order            place — and immediately cross, if it is aggressive
 *   POST /api/book/order/{cid}/cancel   pull an order, return its backing
 *   GET  /api/book/tape             the public tape: price, size, time, no identities
 *   GET  /api/book/confirms         your bilateral confirms (Maker/Taker)
 *   POST /api/book/close            halt: no new orders, no matching, cancels still work
 *   POST /api/book/open             resume
 * </pre>
 *
 * <p><b>What this class decides, and what it does not.</b> Price-time priority,
 * self-match prevention, the band, and the atomicity of a sweep are all in the choice
 * bodies — every Canton validator re-executes them. The ONE piece of judgement here is
 * {@link #ladderFor}, which proposes the contra ladder; {@code MatchOrder} then
 * <em>asserts</em> that ladder is genuinely best-first rather than trusting it. A venue
 * that proposed a self-serving ladder would have its transaction rejected by the ledger.
 */
@RestController
@RequestMapping("/api")
public class ContinuousBookController {

    private static final Logger log = LoggerFactory.getLogger(ContinuousBookController.class);

    /** What a counterparty is called when the ledger declined to name it. */
    private static final String UNDISCLOSED = "(undisclosed)";

    private final LedgerService ledger;

    public ContinuousBookController(LedgerService ledger) {
        this.ledger = ledger;
    }

    // =====================================================================
    // SESSION LIFECYCLE
    // =====================================================================

    /**
     * Open a continuous session for an instrument, or return the one already open.
     * Idempotent, so the UI can call it without first asking whether a book exists.
     */
    @PostMapping("/book/session")
    public ResponseEntity<Dtos.BookStateResponse> openSession(
            @Valid @RequestBody Dtos.BookSessionRequest req) {
        String venue = ledger.resolveParty("Venue");
        String auditor = ledger.resolveParty("Auditor");
        String cash = blankTo(req.cashInstrument(), "USDC");

        Optional<LedgerService.BookView> existing = openBookFor(venue, req.instrumentId(), cash);
        if (existing.isPresent()) {
            return ResponseEntity.ok(stateOf(venue, existing.get()));
        }

        BigDecimal reference = req.referencePrice() != null
                ? req.referencePrice()
                : ledger.referencePriceOf("Issuer", req.instrumentId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                req.instrumentId() + " has no published reference price to anchor"
                                        + " a session on — publish the instrument first, or send"
                                        + " referencePrice explicitly"));
        BigDecimal band = req.bandFraction() != null
                ? req.bandFraction()
                : LedgerCommands.DEFAULT_BAND_FRACTION;

        List<String> participants = ledger.listParties().stream()
                .map(LedgerService.PartyView::party)
                .filter(p -> !LedgerService.labelOf(p).equalsIgnoreCase("sandbox"))
                .toList();

        log.info("BOOK SESSION open instrument={} cash={} reference={} band={} participants={}",
                req.instrumentId(), cash, reference, band, participants.size());
        String cid = ledger.submitForCreated(venue,
                LedgerCommands.createContinuousBook(venue, auditor, participants,
                        req.instrumentId(), cash, reference, band),
                LedgerCommands.continuousBookTemplateId());
        LedgerService.BookView book = bookByCid(venue, cid);
        return ResponseEntity.status(HttpStatus.CREATED).body(stateOf(venue, book));
    }

    /** Halt the session. Cancellation stays open, as it does in a real halt. */
    @PostMapping("/book/close")
    public Dtos.BookStateResponse closeSession(
            @RequestParam String instrumentId,
            @RequestParam(required = false) String cashInstrument) {
        String venue = ledger.resolveParty("Venue");
        String cash = blankTo(cashInstrument, "USDC");
        LedgerService.BookView book = requireOpenBook(venue, instrumentId, cash);
        Transaction tree = ledger.submit(venue, LedgerCommands.closeBookSession(book.contractId()));
        String next = successorBook(tree, book.contractId());
        log.info("BOOK SESSION closed {} -> {}", book.contractId(), next);
        return stateOf(venue, bookByCid(venue, next));
    }

    /** Resume a halted session. */
    @PostMapping("/book/open")
    public Dtos.BookStateResponse reopenSession(
            @RequestParam String instrumentId,
            @RequestParam(required = false) String cashInstrument) {
        String venue = ledger.resolveParty("Venue");
        String cash = blankTo(cashInstrument, "USDC");
        LedgerService.BookView book = anyBookFor(venue, instrumentId, cash)
                .orElseThrow(() -> new IllegalArgumentException(
                        "no continuous session exists for " + instrumentId + "/" + cash));
        if (book.isOpen()) {
            return stateOf(venue, book);
        }
        Transaction tree = ledger.submit(venue, LedgerCommands.openBookSession(book.contractId()));
        String next = successorBook(tree, book.contractId());
        log.info("BOOK SESSION reopened {} -> {}", book.contractId(), next);
        return stateOf(venue, bookByCid(venue, next));
    }

    // =====================================================================
    // THE LADDER — party-aware
    // =====================================================================

    /**
     * The book as the acting party may see it.
     *
     * <p>Acting as the <b>Venue</b> returns the full ladder (the venue signs every
     * order). Acting as a <b>trader</b> returns only that trader's own orders. Acting
     * as the <b>Auditor</b> returns <b>nothing</b> — a RestingOrder has no observers,
     * so even compliance cannot see the book while it rests. That is the dark-pool
     * property, and it is the ledger enforcing it, not this method.
     */
    @GetMapping("/book/state")
    public Dtos.BookStateResponse bookState(
            @RequestParam String instrumentId,
            @RequestParam(required = false) String cashInstrument,
            @RequestParam(required = false, defaultValue = "Venue") String as) {
        String acting = ledger.resolveParty(as);
        String venue = ledger.resolveParty("Venue");
        String cash = blankTo(cashInstrument, "USDC");
        // The SESSION is observable by every participant, so resolve it as the venue
        // (always visible) and then read the LADDER as the acting party.
        LedgerService.BookView book = anyBookFor(venue, instrumentId, cash)
                .orElseThrow(() -> new IllegalArgumentException(
                        "no continuous session exists for " + instrumentId + "/" + cash));
        return stateOf(acting, book);
    }

    /** The public tape. Every print is visible to the whole session and names nobody. */
    @GetMapping("/book/tape")
    public List<Dtos.BookTapeView> tape(
            @RequestParam(required = false) String instrumentId,
            @RequestParam(required = false, defaultValue = "Venue") String as) {
        String acting = ledger.resolveParty(as);
        return ledger.tapePrintsVisibleTo(acting).stream()
                .filter(t -> instrumentId == null || instrumentId.isBlank()
                        || t.instrumentId().equals(instrumentId))
                // Newest first: a tape is read from the top.
                .sorted(Comparator.comparing(LedgerService.TapeView::printedAt).reversed()
                        .thenComparing(Comparator.comparing(LedgerService.TapeView::matchSeq).reversed()))
                .map(t -> new Dtos.BookTapeView(t.contractId(), t.instrumentId(), t.cashInstrument(),
                        t.price(), t.quantity(), String.valueOf(t.printedAt()), t.matchSeq()))
                .toList();
    }

    /** Your confirms. Maker or Taker — the field every fee schedule is built on. */
    @GetMapping("/book/confirms")
    public List<Dtos.BookConfirmView> confirms(
            @RequestParam(required = false, defaultValue = "Venue") String as,
            @RequestParam(required = false) String instrumentId) {
        String acting = ledger.resolveParty(as);
        return ledger.tradeConfirmsVisibleTo(acting).stream()
                .filter(c -> instrumentId == null || instrumentId.isBlank()
                        || c.instrumentId().equals(instrumentId))
                .sorted(Comparator.comparing(LedgerService.ConfirmView::tradedAt).reversed())
                .map(c -> new Dtos.BookConfirmView(c.contractId(), c.traderLabel(), c.instrumentId(),
                        c.cashInstrument(), c.side(), c.quantity(), c.price(), c.cashAmount(),
                        c.liquidity(), String.valueOf(c.tradedAt())))
                .toList();
    }

    // =====================================================================
    // PLACE — and cross, if the order is aggressive
    // =====================================================================

    /**
     * Lodge an order, then immediately try to cross it.
     *
     * <p>This is two ledger transactions, and deliberately so. {@code PlaceOrder}
     * reserves the backing and stamps the arrival sequence; {@code MatchOrder} then
     * crosses the new arrival — which {@code MatchOrder} asserts really is the newest
     * order — against the contra ladder. Doing it in one request is what makes the desk
     * behave like a venue rather than a form: an aggressive order fills on submission,
     * a passive one rests.
     *
     * <p>An order that crosses nothing simply rests (or, if unpriced, is killed by the
     * ledger for want of a contra — an unpriced order may never rest).
     */
    @PostMapping("/book/order")
    public ResponseEntity<Dtos.BookOrderResponse> placeOrder(
            @Valid @RequestBody Dtos.BookOrderRequest req) {
        String trader = ledger.resolveParty(req.trader());
        String venue = ledger.resolveParty("Venue");
        String auditor = ledger.resolveParty("Auditor");
        String cash = blankTo(req.cashInstrument(), "USDC");
        var side = LedgerCommands.bookSide(req.side());
        boolean isBid = side.toString().equalsIgnoreCase("BID");
        Optional<BigDecimal> limit = Optional.ofNullable(req.limitPrice());
        var tif = LedgerCommands.timeInForce(req.timeInForce(), limit);

        // Resolve or open the session.
        Optional<LedgerService.BookView> open = openBookFor(venue, req.instrumentId(), cash);
        boolean opened = false;
        LedgerService.BookView book;
        if (open.isPresent()) {
            book = open.get();
        } else {
            var seed = new Dtos.BookSessionRequest(req.instrumentId(), cash, null, null);
            openSession(seed);
            book = requireOpenBook(venue, req.instrumentId(), cash);
            opened = true;
        }

        log.info("BOOK ORDER start trader={} side={} qty={} limit={} tif={} instrument={} book={}",
                req.trader(), side, req.quantity(), req.limitPrice(), tif, req.instrumentId(),
                book.contractId());

        // RESERVE THE BACKING, sized to the worst price this order can legally execute
        // at — the same discipline as the auction, against a different bound. A BID
        // reserves cash at its own limit, or at the top of the band when it is unpriced
        // (an unpriced bid has no limit of its own, so the band is the only bound). An
        // ASK delivers `quantity` of the asset whatever it prints, so the asset leg is
        // price-independent.
        String holdingCid = isBid
                ? ledger.provisionAtLeastHolding(trader, cash,
                        LedgerCommands.bidReservation(limit, book.referencePrice(),
                                book.bandFraction(), req.quantity()))
                : ledger.provisionAtLeastHolding(trader, req.instrumentId(), req.quantity());

        // PlaceOrder is consuming on the book: read BOTH new contracts out of the one
        // transaction — the order, and the successor book the next placement must target.
        Transaction placed = ledger.submit(trader,
                LedgerCommands.placeOrder(book.contractId(), trader, side, req.quantity(),
                        limit, tif, holdingCid));
        String orderCid = ledger.createdOf(placed, LedgerCommands.restingOrderTemplateId())
                .stream().findFirst().orElseThrow(() ->
                        new IllegalStateException("placement produced no resting order"));
        String bookCid = successorBook(placed, book.contractId());
        log.info("BOOK ORDER placed order={} book {} -> {}", orderCid, book.contractId(), bookCid);

        // CROSS IT, if anything on the other side is reachable.
        List<String> ladder = ladderFor(venue, book.instrumentId(), cash, orderCid);

        // AN ORDER THAT MAY NOT REST MUST NOT BE LEFT RESTING.
        //
        // `PlaceOrder` and `MatchOrder` are two transactions — the taker cannot name its
        // own contras, because it cannot see them — so there is one commit in between
        // where the order exists on the book. For GTC and AON that is simply the order
        // resting, which is what they are for. For IOC and FOK it is a lie: the ledger
        // refuses to let them rest, and if the match then fails or finds nothing, the
        // order is still there holding the trader's collateral and crossing the book.
        // So the venue kills it, which is exactly the duty `ContinuousBook.daml`'s
        // header assigns it ("the venue must fill or KillOrder it in the next
        // transaction, and the window is one ledger round trip, not zero").
        boolean mustNotRest = tif.toString().equalsIgnoreCase("IOC")
                || tif.toString().equalsIgnoreCase("FOK");

        if (ladder.isEmpty()) {
            if (mustNotRest) {
                ledger.submit(venue, LedgerCommands.killBookOrder(bookCid, orderCid));
                log.info("BOOK ORDER killed (no contra, {}) order={}", tif, orderCid);
                throw new IllegalStateException(
                        tif + ": nothing on the other side was reachable, so the order was"
                                + " killed and nothing traded");
            }
            log.info("BOOK ORDER rests (no contra) order={}", orderCid);
            return created(new Dtos.BookOrderResponse(orderCid, bookCid, opened,
                    book.referencePrice(), List.of(), BigDecimal.ZERO, req.quantity()));
        }

        Transaction matched;
        try {
            matched = ledger.submit(venue, LedgerCommands.matchOrder(bookCid, orderCid, ladder));
        } catch (RuntimeException e) {
            // The match aborted — for a FOK or an AON that means the ladder did not cover
            // the full size, which is the instruction working. A FOK is killed; an AON is
            // LEFT RESTING, because "all or none, whenever" is precisely a promise to wait.
            if (mustNotRest) {
                try {
                    ledger.submit(venue, LedgerCommands.killBookOrder(bookCid, orderCid));
                } catch (RuntimeException killFailed) {
                    log.warn("BOOK ORDER {} could not be killed after a failed match: {}",
                            orderCid, killFailed.toString());
                }
                log.info("BOOK ORDER killed after failed match ({}) order={}", tif, orderCid);
                throw new IllegalStateException(
                        tif + ": the reachable ladder did not cover the full size, so the"
                                + " order was killed and nothing traded");
            }
            log.info("BOOK ORDER rests after a refused match ({}) order={}", tif, orderCid);
            throw e;
        }
        String afterCid = successorBook(matched, bookCid);

        // What actually filled: read the confirms this transaction minted for the
        // AGGRESSOR (one per fill, tagged "Taker"). They carry the maker's price — the
        // number that actually moved — so the response never restates a computed figure.
        List<Dtos.BookFillView> fills = fillsFrom(matched, trader, isBid);
        BigDecimal filled = fills.stream()
                .map(Dtos.BookFillView::quantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal resting = req.quantity().subtract(filled).max(BigDecimal.ZERO);

        // Did the aggressor survive? A full fill archives it; a partial fill under GTC
        // leaves the remainder resting with its ORIGINAL queue position.
        boolean stillResting = ledger.restingOrdersVisibleTo(venue).stream()
                .anyMatch(o -> o.contractId().equals(orderCid));
        List<String> residual = ledger.createdOf(matched, LedgerCommands.restingOrderTemplateId());
        String survivingCid = stillResting ? orderCid
                : residual.stream().findFirst().orElse(null);

        log.info("BOOK ORDER done order={} fills={} filled={} resting={} book {} -> {}",
                orderCid, fills.size(), filled, resting, bookCid, afterCid);
        return created(new Dtos.BookOrderResponse(survivingCid, afterCid, opened,
                book.referencePrice(), fills, filled, resting));
    }

    /** Pull an order off the book and return its reserved backing to the trader. */
    @PostMapping("/book/order/{orderCid}/cancel")
    public Dtos.BookStateResponse cancelOrder(
            @PathVariable String orderCid,
            @Valid @RequestBody Dtos.BookCancelRequest req) {
        String trader = ledger.resolveParty(req.trader());
        String venue = ledger.resolveParty("Venue");
        LedgerService.RestingOrderView order = ledger.restingOrdersVisibleTo(venue).stream()
                .filter(o -> o.contractId().equals(orderCid))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "no live resting order " + orderCid));
        LedgerService.BookView book = anyBookFor(venue, order.instrumentId(), order.cashInstrument())
                .orElseThrow(() -> new IllegalStateException(
                        "order " + orderCid + " has no session"));
        // Cancellation works on a CLOSED session too — that is what a real halt does.
        Transaction tree = ledger.submit(trader,
                LedgerCommands.cancelBookOrder(book.contractId(), trader, orderCid));
        String next = successorBook(tree, book.contractId());
        log.info("BOOK CANCEL order={} book {} -> {}", orderCid, book.contractId(), next);
        return stateOf(venue, bookByCid(venue, next));
    }

    // =====================================================================
    // THE LADDER — the one piece of judgement, which the ledger then checks
    // =====================================================================

    /**
     * Build the contra ladder for {@code aggressorCid}, BEST FIRST.
     *
     * <p>Reachable contras only: for a bid, asks at or below the bid's limit; for an
     * ask, bids at or above it. An unpriced aggressor reaches the whole other side.
     * Ordering is <b>price then time</b> — the best price first, and within a price
     * level the earliest arrival ({@code seqNo}) first. That is the queue, and
     * {@code MatchOrder} re-derives and asserts it, so a venue cannot quietly favour a
     * friend by reordering this list.
     *
     * <p>Resting orders are read AS THE VENUE, which signs every order and therefore
     * sees the whole book. No other party could build this ladder.
     */
    private List<String> ladderFor(String venue, String instrumentId, String cash, String aggressorCid) {
        List<LedgerService.RestingOrderView> live = ledger.restingOrdersVisibleTo(venue);
        LedgerService.RestingOrderView aggressor = live.stream()
                .filter(o -> o.contractId().equals(aggressorCid))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "the order just placed is not on the book: " + aggressorCid));
        boolean aggressorIsBid = "Bid".equals(aggressor.side());
        String wanted = aggressorIsBid ? "Ask" : "Bid";
        Optional<BigDecimal> limit = Optional.ofNullable(aggressor.limitPrice());

        Comparator<LedgerService.RestingOrderView> byPriceThenTime = aggressorIsBid
                // Taking asks: cheapest first.
                ? Comparator.comparing(LedgerService.RestingOrderView::limitPrice,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(LedgerService.RestingOrderView::seqNo)
                // Hitting bids: highest first.
                : Comparator.comparing(LedgerService.RestingOrderView::limitPrice,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(LedgerService.RestingOrderView::seqNo);

        return live.stream()
                .filter(o -> !o.contractId().equals(aggressorCid))
                .filter(o -> o.instrumentId().equals(instrumentId))
                .filter(o -> o.cashInstrument().equals(cash))
                .filter(o -> wanted.equals(o.side()))
                // SELF-MATCH PREVENTION, mirrored client-side purely so we do not PROPOSE
                // a ladder the ledger will reject outright. The rule itself is enforced in
                // the choice body — this is politeness, not the control.
                .filter(o -> !o.trader().equals(aggressor.trader()))
                // A resting order always has a limit (an unpriced order may not rest), so
                // a null price here would be a ledger invariant violation, not a market
                // order. Skip defensively rather than NPE mid-ladder.
                .filter(o -> o.limitPrice() != null)
                .filter(o -> crosses(aggressorIsBid, limit, o.limitPrice()))
                .sorted(byPriceThenTime)
                .map(LedgerService.RestingOrderView::contractId)
                .toList();
    }

    /** Would an aggressor with {@code limit} trade against a resting order at {@code restingPrice}? */
    private static boolean crosses(boolean aggressorIsBid, Optional<BigDecimal> limit,
                                   BigDecimal restingPrice) {
        if (limit.isEmpty()) {
            return true;   // unpriced: reaches the whole other side
        }
        return aggressorIsBid
                ? restingPrice.compareTo(limit.get()) <= 0
                : restingPrice.compareTo(limit.get()) >= 0;
    }

    // =====================================================================
    // helpers
    // =====================================================================

    /**
     * The fills of one match, read from the confirms it minted for the aggressor.
     *
     * <p>Confirms are used rather than a recomputed number because the confirm carries
     * the maker's price and the exact cash that moved — the same value the ledger
     * delivered. Only the aggressor's side is read ("Taker"), so each fill appears once.
     */
    private List<Dtos.BookFillView> fillsFrom(Transaction tree, String aggressorParty, boolean isBid) {
        List<String> confirmCids = ledger.createdOf(tree, LedgerCommands.tradeConfirmTemplateId());
        if (confirmCids.isEmpty()) {
            return List.of();
        }
        String venue = ledger.resolveParty("Venue");
        String taker = LedgerService.labelOf(aggressorParty);
        // THE CONTRA IS NOT NAMED, AND THAT IS THE POINT. The venue stood between the
        // two sides as momentary counterparty, and a TradeConfirm deliberately never
        // names the other trader — so the aggressor learns its price, its size and its
        // liquidity flag, and nothing about who supplied it. Reporting the maker here
        // would be this layer inventing a disclosure the ledger refused to make.
        return ledger.tradeConfirmsVisibleTo(venue).stream()
                .filter(c -> confirmCids.contains(c.contractId()))
                .filter(c -> "Taker".equals(c.liquidity()))
                .map(c -> new Dtos.BookFillView(
                        c.price(), c.quantity(), c.cashAmount(),
                        isBid ? taker : UNDISCLOSED,
                        isBid ? UNDISCLOSED : taker,
                        taker,
                        UNDISCLOSED))
                .toList();
    }

    private Dtos.BookStateResponse stateOf(String acting, LedgerService.BookView book) {
        List<LedgerService.RestingOrderView> mine = ledger.restingOrdersVisibleTo(acting).stream()
                .filter(o -> o.instrumentId().equals(book.instrumentId()))
                .filter(o -> o.cashInstrument().equals(book.cashInstrument()))
                .toList();

        List<Dtos.BookOrderView> bids = mine.stream()
                .filter(o -> "Bid".equals(o.side()))
                .sorted(Comparator.comparing(LedgerService.RestingOrderView::limitPrice,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(LedgerService.RestingOrderView::seqNo))
                .map(ContinuousBookController::orderView)
                .toList();
        List<Dtos.BookOrderView> asks = mine.stream()
                .filter(o -> "Ask".equals(o.side()))
                .sorted(Comparator.comparing(LedgerService.RestingOrderView::limitPrice,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(LedgerService.RestingOrderView::seqNo))
                .map(ContinuousBookController::orderView)
                .toList();

        // Best bid / best ask are only meaningful in a view that HAS the other side —
        // i.e. the venue's. A trader's own view reports its own best, which is honest:
        // it is all that party is entitled to know.
        BigDecimal bestBid = bids.stream().map(Dtos.BookOrderView::limitPrice)
                .filter(java.util.Objects::nonNull).max(BigDecimal::compareTo).orElse(null);
        BigDecimal bestAsk = asks.stream().map(Dtos.BookOrderView::limitPrice)
                .filter(java.util.Objects::nonNull).min(BigDecimal::compareTo).orElse(null);

        return new Dtos.BookStateResponse(book.contractId(), book.instrumentId(),
                book.cashInstrument(), book.referencePrice(),
                LedgerCommands.bandLow(book.referencePrice(), book.bandFraction()),
                LedgerCommands.bandHigh(book.referencePrice(), book.bandFraction()),
                book.isOpen(), book.liveCount(), book.nextSeq(), bids, asks, bestBid, bestAsk);
    }

    private static Dtos.BookOrderView orderView(LedgerService.RestingOrderView o) {
        return new Dtos.BookOrderView(o.contractId(), o.traderLabel(), o.side(), o.quantity(),
                o.limitPrice(), o.timeInForce(), o.seqNo());
    }

    private Optional<LedgerService.BookView> openBookFor(String venue, String instrumentId, String cash) {
        return ledger.continuousBooksVisibleTo(venue).stream()
                .filter(b -> b.isOpen()
                        && b.instrumentId().equals(instrumentId)
                        && b.cashInstrument().equals(cash))
                .findFirst();
    }

    private Optional<LedgerService.BookView> anyBookFor(String venue, String instrumentId, String cash) {
        return ledger.continuousBooksVisibleTo(venue).stream()
                .filter(b -> b.instrumentId().equals(instrumentId)
                        && b.cashInstrument().equals(cash))
                .findFirst();
    }

    private LedgerService.BookView requireOpenBook(String venue, String instrumentId, String cash) {
        return openBookFor(venue, instrumentId, cash)
                .orElseThrow(() -> new IllegalArgumentException(
                        "no OPEN continuous session for " + instrumentId + "/" + cash));
    }

    private LedgerService.BookView bookByCid(String party, String cid) {
        return ledger.continuousBooksVisibleTo(party).stream()
                .filter(b -> b.contractId().equals(cid))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("book " + cid + " is not active"));
    }

    /**
     * Every choice on the book is consuming, so the cid just exercised is DEAD. Pull the
     * successor out of the same transaction; falling back to the old cid would hand the
     * caller an id whose next use is a guaranteed CONTRACT_NOT_FOUND.
     */
    private String successorBook(Transaction tree, String previous) {
        return ledger.createdOf(tree, LedgerCommands.continuousBookTemplateId())
                .stream().findFirst().orElse(previous);
    }

    private static <T> ResponseEntity<T> created(T body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    private static String blankTo(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
