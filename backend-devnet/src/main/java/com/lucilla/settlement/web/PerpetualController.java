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
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * LEVERAGED LONG AND SHORT — cash-settled perpetuals on a marked instrument.
 *
 * <p>The desk could already price a fund and issue its shares in kind. What it could
 * not do is let anyone take a view on that fund without holding it, or hedge one they
 * do hold. Both need the same thing: a cash-settled perpetual on the fund's mark.
 *
 * <pre>
 *   POST /api/perp/market            open a market on an instrument (idempotent)
 *   POST /api/perp/market/fund       fund the venue's insurance pool
 *   GET  /api/perp/markets           markets, with open interest and funding
 *   POST /api/perp/position          open leveraged exposure
 *   POST /api/perp/position/{cid}/close       realise P&L
 *   POST /api/perp/position/{cid}/collateral  top up, move the liquidation price away
 *   POST /api/perp/position/{cid}/liquidate   venue closes an underwater position
 *   GET  /api/perp/positions?as=     YOUR positions, marked to the index
 *   POST /api/perp/market/index      republish the index from the attested mark
 *   POST /api/perp/market/funding    derive the funding rate from the perp's own price
 * </pre>
 *
 * <p><b>Nothing here decides anything about risk.</b> The leverage ceiling, the
 * maintenance floor, whether a position is liquidatable, and the conservation of cash
 * across every settlement path are all in the choice bodies, which every Canton
 * validator re-executes. This layer resolves parties, threads consuming choices, and
 * computes the read-only marks a screen needs.
 */
@RestController
@RequestMapping("/api")
public class PerpetualController {

    private static final Logger log = LoggerFactory.getLogger(PerpetualController.class);

    private final LedgerService ledger;

    public PerpetualController(LedgerService ledger) {
        this.ledger = ledger;
    }

    // =====================================================================
    // MARKET
    // =====================================================================

    /**
     * Open a perpetual market on an instrument, or return the one already open.
     *
     * <p>The index is seeded from the instrument's attested mark — the committee's
     * fix or the auction's print. It is deliberately NOT an independent feed: a
     * leveraged position is liquidated against this number, so an index one party can
     * move at will is an index one party can use to liquidate people.
     */
    @PostMapping("/perp/market")
    public ResponseEntity<Dtos.PerpMarketResponse> openMarket(
            @Valid @RequestBody Dtos.PerpMarketRequest req) {
        String venue = ledger.resolveParty("Venue");
        String auditor = ledger.resolveParty("Auditor");
        String cash = blankTo(req.cashInstrument(), "USDC");

        Optional<LedgerService.PerpMarketView> open = openMarketFor(venue, req.instrumentId(), cash);
        if (open.isPresent()) {
            return ResponseEntity.ok(viewOf(open.get()));
        }

        BigDecimal index = req.indexPrice() != null
                ? req.indexPrice()
                : ledger.referencePriceOf("Issuer", req.instrumentId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                req.instrumentId() + " has no attested mark to index against —"
                                        + " strike one with the committee first, or send"
                                        + " indexPrice explicitly"));

        List<String> participants = ledger.listParties().stream()
                .map(LedgerService.PartyView::party)
                .filter(p -> !LedgerService.labelOf(p).equalsIgnoreCase("sandbox"))
                .toList();

        BigDecimal maxLev = req.maxLeverage() != null
                ? req.maxLeverage() : LedgerCommands.DEFAULT_MAX_LEVERAGE;
        BigDecimal maint = req.maintenanceMarginBps() != null
                ? req.maintenanceMarginBps() : LedgerCommands.DEFAULT_MAINTENANCE_BPS;

        log.info("PERP MARKET open instrument={} index={} maxLeverage={} maintenanceBps={}",
                req.instrumentId(), index, maxLev, maint);
        String cid = ledger.submitForCreated(venue,
                LedgerCommands.createPerpMarket(venue, auditor, participants,
                        req.instrumentId(), cash, index, maxLev, maint,
                        LedgerCommands.DEFAULT_FUNDING_CAP),
                LedgerCommands.perpMarketTemplateId());
        return ResponseEntity.status(HttpStatus.CREATED).body(viewOf(marketByCid(venue, cid)));
    }

    /**
     * Fund the venue's insurance pool.
     *
     * <p>Without it no trader can ever be paid a profit, and the ledger says so rather
     * than settling at zero. The pool is disclosed to every participant — mechanically
     * because a trader closing a winner must be able to read the contract that pays
     * it, and substantively because the size of the fund standing behind every open
     * position is what a trader is entitled to know before taking leverage here.
     */
    @PostMapping("/perp/market/fund")
    public Dtos.PerpMarketResponse fundInsurance(@Valid @RequestBody Dtos.PerpFundRequest req) {
        String venue = ledger.resolveParty("Venue");
        String cash = blankTo(req.cashInstrument(), "USDC");
        LedgerService.PerpMarketView market = requireOpenMarket(venue, req.instrumentId(), cash);

        String poolCid = ledger.provisionAtLeastHolding(venue, cash, req.amount());
        Transaction tree = ledger.submit(venue,
                LedgerCommands.fundPerpInsurance(market.contractId(), poolCid));
        String next = successorMarket(tree, market.contractId());
        log.info("PERP INSURANCE funded {} {} market {} -> {}", req.amount(), cash,
                market.contractId(), next);
        return viewOf(marketByCid(venue, next));
    }

    /** Markets, with open interest and the current funding rate. */
    @GetMapping("/perp/markets")
    public List<Dtos.PerpMarketResponse> markets(
            @RequestParam(required = false, defaultValue = "Venue") String as) {
        return ledger.perpMarketsVisibleTo(ledger.resolveParty(as)).stream()
                .map(this::viewOf)
                .toList();
    }

    /**
     * Republish the index from the instrument's attested mark.
     *
     * <p>This is the sharpest authority in the module — the number that decides who
     * gets liquidated — so it reads the mark off the ledger rather than accepting one
     * from the caller. Passing a price explicitly is allowed for a market whose
     * instrument has no mark yet, and is logged as such.
     */
    @PostMapping("/perp/market/index")
    public Dtos.PerpMarketResponse setIndex(@Valid @RequestBody Dtos.PerpIndexRequest req) {
        String venue = ledger.resolveParty("Venue");
        String cash = blankTo(req.cashInstrument(), "USDC");
        LedgerService.PerpMarketView market = requireOpenMarket(venue, req.instrumentId(), cash);

        BigDecimal index = req.indexPrice() != null
                ? req.indexPrice()
                : ledger.referencePriceOf("Issuer", req.instrumentId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "no attested mark for " + req.instrumentId()));
        if (req.indexPrice() != null) {
            log.warn("PERP INDEX set EXPLICITLY to {} for {} — not read from an attested mark",
                    index, req.instrumentId());
        }
        Transaction tree = ledger.submit(venue,
                LedgerCommands.setPerpIndex(market.contractId(), index));
        return viewOf(marketByCid(venue, successorMarket(tree, market.contractId())));
    }

    /**
     * Derive the funding rate from what the perpetual itself last traded at.
     *
     * <p>A perpetual has no expiry, so nothing forces its price onto the thing it
     * tracks — the funding rate is that force. It is computed from THIS venue's perp
     * price against THIS venue's index, never fetched from another exchange, because
     * another exchange's funding rate is a fact about another exchange's book.
     */
    @PostMapping("/perp/market/funding")
    public Dtos.PerpMarketResponse deriveFunding(@Valid @RequestBody Dtos.PerpFundingRequest req) {
        String venue = ledger.resolveParty("Venue");
        String cash = blankTo(req.cashInstrument(), "USDC");
        LedgerService.PerpMarketView market = requireOpenMarket(venue, req.instrumentId(), cash);
        Transaction tree = ledger.submit(venue,
                LedgerCommands.derivePerpFunding(market.contractId(), req.perpMark()));
        var after = marketByCid(venue, successorMarket(tree, market.contractId()));
        log.info("PERP FUNDING derived from perpMark={} index={} -> {}",
                req.perpMark(), after.indexPrice(), after.fundingRate());
        return viewOf(after);
    }

    // =====================================================================
    // POSITIONS
    // =====================================================================

    /** Open leveraged exposure. Collateral is the market's cash instrument. */
    @PostMapping("/perp/position")
    public ResponseEntity<Dtos.PerpPositionResponse> openPosition(
            @Valid @RequestBody Dtos.PerpOpenRequest req) {
        String trader = ledger.resolveParty(req.trader());
        String venue = ledger.resolveParty("Venue");
        String cash = blankTo(req.cashInstrument(), "USDC");
        var side = LedgerCommands.positionSide(req.side());
        LedgerService.PerpMarketView market = requireOpenMarket(venue, req.instrumentId(), cash);

        // Reserve the collateral the trader is committing. It stays THEIRS — posting
        // margin splits the holding and discloses the slice to the venue so settlement
        // can move it, but ownership does not change until a close or a liquidation.
        String collateralCid = ledger.provisionAtLeastHolding(trader, cash, req.collateral());

        log.info("PERP OPEN trader={} side={} size={} collateral={} instrument={} index={}",
                req.trader(), side, req.size(), req.collateral(), req.instrumentId(),
                market.indexPrice());
        Transaction tree = ledger.submit(trader,
                LedgerCommands.openPerpPosition(market.contractId(), trader, side,
                        req.size(), req.collateral(), collateralCid));
        String posCid = ledger.createdOf(tree, LedgerCommands.perpPositionTemplateId())
                .stream().findFirst().orElseThrow(() ->
                        new IllegalStateException("open produced no position"));
        var pos = ledger.perpPositionsVisibleTo(trader).stream()
                .filter(p -> p.contractId().equals(posCid))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("the position just opened is not visible"));
        return created(viewOf(pos, marketByCid(venue, successorMarket(tree, market.contractId()))));
    }

    /** Realise P&L against the market's CURRENT index and return what is left. */
    @PostMapping("/perp/position/{cid}/close")
    public Dtos.PerpCloseResponse closePosition(
            @PathVariable String cid, @Valid @RequestBody Dtos.PerpCloseRequest req) {
        String trader = ledger.resolveParty(req.trader());
        String venue = ledger.resolveParty("Venue");
        var pos = positionByCid(trader, cid);
        var market = requireOpenMarket(venue, pos.instrumentId(), pos.cashInstrument());

        BigDecimal equity = equityOf(pos, market.indexPrice());
        Transaction tree = ledger.submit(trader,
                LedgerCommands.closePerpPosition(cid, market.contractId()));
        log.info("PERP CLOSE position={} side={} size={} entry={} index={} equity~{}",
                cid, pos.side(), pos.size(), pos.entryPrice(), market.indexPrice(), equity);
        var after = marketByCid(venue, successorMarket(tree, market.contractId()));
        return new Dtos.PerpCloseResponse(cid, pos.side(), pos.size(), pos.entryPrice(),
                market.indexPrice(), pnlOf(pos, market.indexPrice()), equity, viewOf(after));
    }

    /** Top up collateral to move the liquidation price away. */
    @PostMapping("/perp/position/{cid}/collateral")
    public Dtos.PerpPositionResponse addCollateral(
            @PathVariable String cid, @Valid @RequestBody Dtos.PerpCollateralRequest req) {
        String trader = ledger.resolveParty(req.trader());
        String venue = ledger.resolveParty("Venue");
        var pos = positionByCid(trader, cid);
        String extraCid = ledger.provisionAtLeastHolding(trader, pos.cashInstrument(), req.extra());
        Transaction tree = ledger.submit(trader,
                LedgerCommands.addPerpCollateral(cid, req.extra(), extraCid));
        String next = ledger.createdOf(tree, LedgerCommands.perpPositionTemplateId())
                .stream().findFirst().orElse(cid);
        var market = requireOpenMarket(venue, pos.instrumentId(), pos.cashInstrument());
        return viewOf(positionByCid(trader, next), market);
    }

    /**
     * The venue closes a position whose equity has fallen below the maintenance floor.
     *
     * <p>Not permissionless, and deliberately: a keeper cannot liquidate what it
     * cannot see, and a {@code PerpPosition} is private to its trader. Permissionless
     * liquidation would require public positions, and a visible liquidation price on a
     * leveraged product is an invitation to push the market into it.
     */
    @PostMapping("/perp/position/{cid}/liquidate")
    public Dtos.PerpCloseResponse liquidate(@PathVariable String cid) {
        String venue = ledger.resolveParty("Venue");
        var pos = positionByCid(venue, cid);
        var market = requireOpenMarket(venue, pos.instrumentId(), pos.cashInstrument());
        BigDecimal equity = equityOf(pos, market.indexPrice());

        log.info("PERP LIQUIDATE position={} equity~{} maintenance~{}",
                cid, equity, maintenanceOf(pos, market.indexPrice()));
        Transaction tree = ledger.submit(venue,
                LedgerCommands.liquidatePerpPosition(cid, market.contractId()));
        var after = marketByCid(venue, successorMarket(tree, market.contractId()));
        return new Dtos.PerpCloseResponse(cid, pos.side(), pos.size(), pos.entryPrice(),
                market.indexPrice(), pnlOf(pos, market.indexPrice()), equity, viewOf(after));
    }

    /** Move a position's collateral by the market's funding rate. */
    @PostMapping("/perp/position/{cid}/funding")
    public Dtos.PerpPositionResponse applyFunding(@PathVariable String cid) {
        String venue = ledger.resolveParty("Venue");
        var pos = positionByCid(venue, cid);
        var market = requireOpenMarket(venue, pos.instrumentId(), pos.cashInstrument());
        Transaction tree = ledger.submit(venue,
                LedgerCommands.applyPerpFunding(cid, market.contractId()));
        String next = ledger.createdOf(tree, LedgerCommands.perpPositionTemplateId())
                .stream().findFirst().orElse(cid);
        return viewOf(positionByCid(venue, next), market);
    }

    /**
     * Positions visible to the acting party — and a position is PRIVATE.
     *
     * <p>Acting as a trader returns that trader's own book and nothing else. The venue
     * signs every position so it sees them all; the auditor observes them. No other
     * participant sees any of it, which is the same property the sealed book has and
     * matters more here, not less.
     */
    @GetMapping("/perp/positions")
    public List<Dtos.PerpPositionResponse> positions(
            @RequestParam(required = false, defaultValue = "Venue") String as) {
        String acting = ledger.resolveParty(as);
        String venue = ledger.resolveParty("Venue");
        List<LedgerService.PerpMarketView> markets = ledger.perpMarketsVisibleTo(venue);
        return ledger.perpPositionsVisibleTo(acting).stream()
                .map(p -> viewOf(p, markets.stream()
                        .filter(m -> m.instrumentId().equals(p.instrumentId())
                                && m.cashInstrument().equals(p.cashInstrument()))
                        .findFirst().orElse(null)))
                .toList();
    }

    // =====================================================================
    // read-only marks — the same arithmetic the ledger uses
    // =====================================================================
    // Mirrored from Perpetual.daml so a screen can show a position's health without
    // a write. If the model's formulas move, these move with them; the LEDGER's copy
    // is the one that decides anything.

    private static BigDecimal pnlOf(LedgerService.PerpPositionView p, BigDecimal mark) {
        BigDecimal diff = "Long".equals(p.side())
                ? mark.subtract(p.entryPrice())
                : p.entryPrice().subtract(mark);
        return p.size().multiply(diff);
    }

    private static BigDecimal equityOf(LedgerService.PerpPositionView p, BigDecimal mark) {
        return p.collateral().add(pnlOf(p, mark));
    }

    private static BigDecimal maintenanceOf(LedgerService.PerpPositionView p, BigDecimal mark) {
        return p.size().multiply(mark)
                .multiply(p.maintenanceMarginBps())
                .divide(new BigDecimal("10000"), 10, RoundingMode.HALF_EVEN);
    }

    /**
     * The index at which this position becomes liquidatable — the number a trader
     * most wants and no venue shows prominently enough.
     *
     * <p>Solving equity = maintenance for the mark:
     * <pre>
     *   LONG :  liq = (size·entry − collateral) / (size · (1 − mmr))
     *   SHORT:  liq = (size·entry + collateral) / (size · (1 + mmr))
     * </pre>
     */
    private static BigDecimal liquidationPriceOf(LedgerService.PerpPositionView p) {
        BigDecimal mmr = p.maintenanceMarginBps().divide(new BigDecimal("10000"), 10, RoundingMode.HALF_EVEN);
        BigDecimal notionalAtEntry = p.size().multiply(p.entryPrice());
        if ("Long".equals(p.side())) {
            BigDecimal denom = p.size().multiply(BigDecimal.ONE.subtract(mmr));
            if (denom.signum() <= 0) return null;
            return notionalAtEntry.subtract(p.collateral()).divide(denom, 6, RoundingMode.HALF_EVEN);
        }
        BigDecimal denom = p.size().multiply(BigDecimal.ONE.add(mmr));
        if (denom.signum() <= 0) return null;
        return notionalAtEntry.add(p.collateral()).divide(denom, 6, RoundingMode.HALF_EVEN);
    }

    // =====================================================================
    // helpers
    // =====================================================================

    private Dtos.PerpMarketResponse viewOf(LedgerService.PerpMarketView m) {
        BigDecimal skew = m.openLong().subtract(m.openShort());
        return new Dtos.PerpMarketResponse(m.contractId(), m.instrumentId(), m.cashInstrument(),
                m.indexPrice(), m.fundingRate(), m.fundingRateCap(), m.maxLeverage(),
                m.maintenanceMarginBps(), m.openLong(), m.openShort(), skew,
                m.insured(), m.isOpen());
    }

    private Dtos.PerpPositionResponse viewOf(
            LedgerService.PerpPositionView p, LedgerService.PerpMarketView m) {
        BigDecimal mark = m == null ? p.entryPrice() : m.indexPrice();
        BigDecimal pnl = pnlOf(p, mark);
        BigDecimal equity = equityOf(p, mark);
        BigDecimal maint = maintenanceOf(p, mark);
        BigDecimal notional = p.size().multiply(mark);
        BigDecimal leverage = p.collateral().signum() == 0
                ? null : notional.divide(p.collateral(), 2, RoundingMode.HALF_EVEN);
        return new Dtos.PerpPositionResponse(
                p.contractId(), p.traderLabel(), p.instrumentId(), p.cashInstrument(),
                p.side(), p.size(), p.entryPrice(), mark, p.collateral(),
                notional, leverage, pnl, equity, maint,
                liquidationPriceOf(p), equity.compareTo(maint) < 0,
                String.valueOf(p.openedAt()), String.valueOf(p.lastFundingAt()));
    }

    private Optional<LedgerService.PerpMarketView> openMarketFor(
            String venue, String instrumentId, String cash) {
        return ledger.perpMarketsVisibleTo(venue).stream()
                .filter(m -> m.isOpen() && m.instrumentId().equals(instrumentId)
                        && m.cashInstrument().equals(cash))
                .findFirst();
    }

    private LedgerService.PerpMarketView requireOpenMarket(
            String venue, String instrumentId, String cash) {
        return openMarketFor(venue, instrumentId, cash)
                .orElseThrow(() -> new IllegalArgumentException(
                        "no open perpetual market for " + instrumentId + "/" + cash));
    }

    private LedgerService.PerpMarketView marketByCid(String party, String cid) {
        return ledger.perpMarketsVisibleTo(party).stream()
                .filter(m -> m.contractId().equals(cid))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("perp market " + cid + " is not active"));
    }

    private LedgerService.PerpPositionView positionByCid(String party, String cid) {
        return ledger.perpPositionsVisibleTo(party).stream()
                .filter(p -> p.contractId().equals(cid))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "no position " + cid + " is visible to " + LedgerService.labelOf(party)));
    }

    /** Every choice on the market is consuming, so the cid just exercised is dead. */
    private String successorMarket(Transaction tree, String previous) {
        return ledger.createdOf(tree, LedgerCommands.perpMarketTemplateId())
                .stream().findFirst().orElse(previous);
    }

    private static <T> ResponseEntity<T> created(T body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    private static String blankTo(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
