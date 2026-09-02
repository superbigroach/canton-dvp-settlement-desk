package com.lucilla.settlement.portal;

import com.lucilla.settlement.auth.CurrentUser;
import com.lucilla.settlement.auth.Principal;
import com.lucilla.settlement.benchmarks.SeriesRow;
import com.lucilla.settlement.benchmarks.SeriesService;
import com.lucilla.settlement.events.EventStore;
import com.lucilla.settlement.events.FixingEvent;
import com.lucilla.settlement.ledger.LedgerService;
import com.lucilla.settlement.scheduler.ScheduleStore;
import com.lucilla.settlement.web.ApiExceptionHandler;
import com.lucilla.settlement.web.Dtos;
import com.lucilla.settlement.web.SettlementController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@code /api/ap/*} — the authorised participant's portal (docs/PRODUCT-PLAN.md §5).
 * Creation and redemption wrap the existing one-call basket paths AS THE CALLER'S PARTY;
 * the AP never names itself on the wire.
 *
 * <p>Shapes follow {@code frontend/src/desk/types.ts} ({@code ApFund}, {@code Receipt}),
 * with the desk's fuller detail alongside.
 */
@RestController
public class ApController {

    private final LedgerService ledger;
    private final SettlementController desk;
    private final SeriesService series;
    private final ScheduleStore schedules;
    private final EventStore events;

    public ApController(LedgerService ledger, SettlementController desk, SeriesService series,
            ScheduleStore schedules, EventStore events) {
        this.ledger = ledger;
        this.desk = desk;
        this.series = series;
        this.schedules = schedules;
        this.events = events;
    }

    @GetMapping("/api/ap/funds")
    public List<Map<String, Object>> funds(HttpServletRequest req) {
        Principal me = CurrentUser.requireParty(req);
        String party = ledger.resolveParty(me.party());
        List<Map<String, Object>> out = new ArrayList<>();
        for (var b : ledger.basketsVisibleTo(party)) {
            boolean isAp = b.participants().contains(party) || me.isAdmin();
            if (!isAp) continue;
            out.add(fundView(b, party));
        }
        return out;
    }

    public record OrderRequest(@NotBlank String fundId, @NotNull @Positive BigDecimal shares) {
    }

    /** Returns the receipt ({@code types.ts Receipt}). */
    @PostMapping("/api/ap/create")
    public Map<String, Object> create(HttpServletRequest req, @Valid @RequestBody OrderRequest body) {
        Principal me = CurrentUser.requireParty(req);
        String party = ledger.resolveParty(me.party());
        var r = desk.createBasketUnits(new Dtos.BasketCreateRequest(body.fundId(), party, body.shares()));
        record(FixingEvent.Kinds.CREATION, me, party, body, r.receiptCid(), r.navPerShare());
        Map<String, Object> out = receiptOr(party, r.receiptCid(), body.fundId(), "create", body.shares(), r.navPerShare());
        out.put("mintedSharesCid", r.mintedSharesCid());
        return out;
    }

    @PostMapping("/api/ap/redeem")
    public Map<String, Object> redeem(HttpServletRequest req, @Valid @RequestBody OrderRequest body) {
        Principal me = CurrentUser.requireParty(req);
        String party = ledger.resolveParty(me.party());
        var r = desk.redeemBasketUnits(new Dtos.BasketRedeemRequest(body.fundId(), party, body.shares()));
        record(FixingEvent.Kinds.REDEMPTION, me, party, body, r.receiptCid(), null);
        Map<String, Object> out = receiptOr(party, r.receiptCid(), body.fundId(), "redeem", body.shares(), null);
        out.put("returnedHoldingCids", r.returnedHoldingCids());
        return out;
    }

    @GetMapping("/api/ap/receipts")
    public List<Map<String, Object>> receipts(HttpServletRequest req) {
        Principal me = CurrentUser.requireParty(req);
        String party = ledger.resolveParty(me.party());
        List<Map<String, Object>> out = new ArrayList<>();
        Map<String, BigDecimal> marks = new LinkedHashMap<>();
        for (var r : ledger.basketReceiptsVisibleTo(party)) {
            if (!r.ap().equals(party) && !me.isAdmin()) continue;
            out.add(receiptView(r, marks));
        }
        return out;
    }

    // ---- views (shared with the fund dashboard) --------------------------------

    /** {@code types.ts ApFund}, plus the desk's detail. */
    Map<String, Object> fundView(LedgerService.BasketView b, String viewer) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", b.basketId());
        out.put("name", b.description());
        out.put("cash", b.cashInstrument());
        out.put("cashInstrument", b.cashInstrument());
        out.put("administrator", LedgerService.labelOf(b.administrator()));
        out.put("participants", b.participants().stream().map(LedgerService::labelOf).toList());

        List<Map<String, Object>> comps = new ArrayList<>();
        BigDecimal nav = BigDecimal.ZERO;
        boolean complete = true;
        for (var c : b.components()) {
            BigDecimal mark = ledger.referencePriceOf("Issuer", c.instrumentId()).orElse(null);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("instrumentId", c.instrumentId());
            m.put("unitsPerShare", c.unitsPerShare());
            m.put("mark", mark);
            m.put("valuePerShare", mark == null ? null : c.unitsPerShare().multiply(mark));
            comps.add(m);
            if (mark == null) complete = false; else nav = nav.add(c.unitsPerShare().multiply(mark));
        }
        out.put("components", comps);

        // official: the fund's last PUBLISHED value (its own fixing, a fallback row, or the
        // derived seed) — never null while the components are marked.
        Optional<SeriesRow> last = series.lastPriced(b.basketId());
        Map<String, Object> official = null;
        if (last.isPresent()) {
            SeriesRow r = last.get();
            official = new LinkedHashMap<>();
            official.put("nav", r.price());
            official.put("asOf", r.asOf());
            official.put("tier", r.tier());
            official.put("k", r.k());
            official.put("n", r.n());
            official.put("fixingCid", r.fixingCid());
        } else if (complete) {
            official = new LinkedHashMap<>();
            official.put("nav", nav);
            official.put("asOf", Instant.now().toString());
            official.put("tier", 0);
            official.put("k", 0);
            official.put("n", 0);
            official.put("fixingCid", null);
        }
        out.put("official", official);
        out.put("officialNav", complete ? nav : null);
        try {
            var ind = desk.basketIndicativeNav(b.basketId(), viewer);
            out.put("indicative", ind.indicativeNavPerShare());
            out.put("indicativeDriftBps", ind.driftBps());
            out.put("indicativeAsOf", ind.asOf());
        } catch (RuntimeException e) {
            out.put("indicative", null);
        }

        // types.ts ApFund.fee is in basis points. The Basket template charges a FLAT
        // fee per creation/redemption in the fund's cash instrument, so bps is 0 and the
        // flat amount is `minimum` — plus the raw schedule so nothing is lost.
        Map<String, Object> fee = new LinkedHashMap<>();
        fee.put("createBps", 0);
        fee.put("redeemBps", 0);
        BigDecimal flat = b.creationFee().orElse(b.redemptionFee().orElse(null));
        fee.put("minimum", flat);
        fee.put("currency", b.cashInstrument());
        fee.put("createFlat", b.creationFee().orElse(null));
        fee.put("redeemFlat", b.redemptionFee().orElse(null));
        fee.put("feeReceiver", b.feeReceiver().map(LedgerService::labelOf).orElse(null));
        out.put("fee", fee);
        out.put("feeSchedule", fee);

        Map<String, Object> cutoff = new LinkedHashMap<>();
        schedules.byInstrument(b.basketId()).ifPresent(s -> {
            cutoff.put("time", s.getStrikeAt());
            cutoff.put("timezone", s.getTimezone());
            cutoff.put("nextAt", nextStrike(s).toString());
            cutoff.put("windowMinutes", s.getWindowMinutes());
        });
        out.put("cutoff", cutoff);
        out.put("cutoffs", cutoff);

        // Shares outstanding: the fund's share record is issued by its administrator, so
        // every share holding is visible from there.
        try {
            BigDecimal outstanding = ledger.holdingsVisibleTo(b.administrator()).stream()
                    .filter(h -> h.instrumentId().equals(b.basketId()))
                    .map(LedgerService.HoldingView::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
            out.put("sharesOutstanding", outstanding);
        } catch (RuntimeException e) {
            out.put("sharesOutstanding", null);
        }
        out.put("lastFixing", series.last(b.basketId()).orElse(null));
        BigDecimal mine = ledger.holdingsVisibleTo(viewer).stream()
                .filter(h -> h.instrumentId().equals(b.basketId()) && h.owner().equals(viewer))
                .map(LedgerService.HoldingView::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        out.put("mySharesHeld", mine);
        return out;
    }

    /** {@code types.ts Receipt}. NAV is the receipt's basket marked at CURRENT component marks. */
    Map<String, Object> receiptView(LedgerService.BasketReceiptView r, Map<String, BigDecimal> markCache) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", r.contractId());
        out.put("cid", r.contractId());
        out.put("receiptCid", r.contractId());
        out.put("fundId", r.basketId());
        boolean creation = "Creation".equalsIgnoreCase(r.action());
        out.put("kind", creation ? "create" : "redeem");
        out.put("action", r.action());
        out.put("shares", r.shares());
        BigDecimal nav = BigDecimal.ZERO;
        boolean complete = true;
        List<Map<String, Object>> units = new ArrayList<>();
        for (var c : r.components()) {
            BigDecimal mark = markCache.computeIfAbsent(c.instrumentId(),
                    id -> ledger.referencePriceOf("Issuer", id).orElse(null));
            if (mark == null) complete = false; else nav = nav.add(c.unitsPerShare().multiply(mark));
            Map<String, Object> u = new LinkedHashMap<>();
            u.put("instrumentId", c.instrumentId());
            u.put("amount", c.unitsPerShare().multiply(r.shares()));
            u.put("unitsPerShare", c.unitsPerShare());
            units.add(u);
        }
        out.put("nav", complete ? nav : null);
        out.put("units", units);
        out.put("components", units);
        out.put("fee", r.fee() == null ? BigDecimal.ZERO : r.fee());
        out.put("feeCurrency", r.cashInstrument());
        out.put("cashInstrument", r.cashInstrument());
        out.put("ts", r.settledAt().toString());
        out.put("settledAt", r.settledAt().toString());
        out.put("status", "settled");
        out.put("party", LedgerService.labelOf(r.ap()));
        out.put("ap", LedgerService.labelOf(r.ap()));
        out.put("administrator", LedgerService.labelOf(r.administrator()));
        out.put("note", "nav is the delivered basket marked at current component marks");
        return out;
    }

    /** The receipt just settled, read back from the ledger; a minimal row if it is not visible yet. */
    private Map<String, Object> receiptOr(String party, String receiptCid, String fundId, String kind,
            BigDecimal shares, BigDecimal nav) {
        if (receiptCid != null) {
            try {
                Optional<LedgerService.BasketReceiptView> r = ledger.basketReceiptsVisibleTo(party).stream()
                        .filter(x -> x.contractId().equals(receiptCid)).findFirst();
                if (r.isPresent()) {
                    Map<String, Object> v = receiptView(r.get(), new LinkedHashMap<>());
                    if (nav != null) v.put("nav", nav);
                    return v;
                }
            } catch (RuntimeException e) {
                // fall through to the minimal row
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", receiptCid);
        out.put("cid", receiptCid);
        out.put("receiptCid", receiptCid);
        out.put("fundId", fundId);
        out.put("kind", kind);
        out.put("shares", shares);
        out.put("nav", nav);
        out.put("units", List.of());
        out.put("fee", BigDecimal.ZERO);
        out.put("ts", Instant.now().toString());
        out.put("status", "settled");
        out.put("party", LedgerService.labelOf(party));
        return out;
    }

    static Instant nextStrike(com.lucilla.settlement.scheduler.StrikeSchedule s) {
        Instant now = Instant.now();
        java.time.LocalDate d = s.dateOf(now);
        Instant at = s.strikeInstantOn(d);
        while (at.isBefore(now) || !com.lucilla.settlement.ledger.FixingSchedule.isBusinessDay(d)) {
            d = d.plusDays(1);
            at = s.strikeInstantOn(d);
        }
        return at;
    }

    private void record(String kind, Principal me, String party, OrderRequest body, String receiptCid,
            BigDecimal nav) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("shares", body.shares().toPlainString());
        d.put("ap", LedgerService.labelOf(party));
        events.append(FixingEvent.of(kind, body.fundId(), null, null,
                me.email() == null ? LedgerService.labelOf(party) : me.email() + " (" + LedgerService.labelOf(party) + ")",
                "ap", null, kind.endsWith("creation") ? "in-kind creation" : "in-kind redemption", nav, null,
                receiptCid, d));
    }

    /** The basket named {@code id}, or a 404. */
    LedgerService.BasketView basketOr404(String id) {
        return ledger.basketById(id).orElseThrow(() -> new ApiExceptionHandler.NotFound("no fund '" + id + "'"));
    }
}
