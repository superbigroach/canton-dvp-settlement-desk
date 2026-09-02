package com.lucilla.settlement.scheduler;

import com.lucilla.settlement.auth.AuthProperties;
import com.lucilla.settlement.benchmarks.SeriesRow;
import com.lucilla.settlement.benchmarks.SeriesService;
import com.lucilla.settlement.events.EventStore;
import com.lucilla.settlement.events.FixingEvent;
import com.lucilla.settlement.ledger.FixingSchedule;
import com.lucilla.settlement.ledger.LedgerCommands;
import com.lucilla.settlement.ledger.LedgerService;
import com.lucilla.settlement.ledger.MarketData;
import com.lucilla.settlement.web.Dtos;
import com.lucilla.settlement.web.SettlementController;
import com.lucilla.settlement.webhooks.WebhookDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The strike runner's brain — docs/PRODUCT-PLAN.md §4 as a state machine over the
 * clock, the ledger and the event log:
 *
 * <pre>
 *   strike time  → PROPOSE  operator computes benchmark × last factor (wrapped) or
 *                           Σ units × marks (fund) and opens the on-ledger proposal
 *   K reached    → FINALIZE as the proposer; funds re-mark; tier-1 row
 *   window end   → FALLBACK tier 3 / 4 / 5 as a series row + event (+ webhook on 5)
 * </pre>
 *
 * <p>Every ledger step goes through the SAME controller methods the operator desk
 * uses, so a scheduled strike and a hand-struck one are indistinguishable on the
 * ledger. Restriking after a refusal is deliberately manual
 * ({@code POST /api/admin/strike/{id}}): the runner re-proposing the same inputs would
 * only produce the same refusal.
 */
@Service
public class StrikeService {

    private static final Logger log = LoggerFactory.getLogger(StrikeService.class);

    private final LedgerService ledger;
    private final MarketData marketData;
    private final SeriesService series;
    private final ScheduleStore schedules;
    private final EventStore events;
    private final AuthProperties props;
    private final SettlementController desk;
    private final WebhookDispatcher webhooks;

    public StrikeService(LedgerService ledger, MarketData marketData, SeriesService series,
            ScheduleStore schedules, EventStore events, AuthProperties props,
            SettlementController desk, WebhookDispatcher webhooks) {
        this.ledger = ledger;
        this.marketData = marketData;
        this.series = series;
        this.schedules = schedules;
        this.events = events;
        this.props = props;
        this.desk = desk;
        this.webhooks = webhooks;
    }

    // ---- the tick ---------------------------------------------------------------

    public void tick(Instant now) {
        for (StrikeSchedule s : schedules.all()) {
            if (!s.isEnabled()) continue;
            try {
                evaluate(s, now);
            } catch (RuntimeException e) {
                log.warn("strike runner: {} not evaluated: {}", s.getInstrumentId(), e.toString());
            }
            try {
                reconcileMark(s);
            } catch (RuntimeException e) {
                log.warn("strike runner: {} mark not reconciled: {}", s.getInstrumentId(), e.toString());
            }
        }
    }

    /**
     * THE FIX IS FINAL EVEN WHEN THE CLIENT CALL WAS NOT. A finalize can land on the ledger
     * while the desk's own call times out (it did on 2 Sep 2026 at 16:08 UTC: the NavFixing
     * existed, the Instrument still said the seed price, and no event was written). The
     * ledger is the source of truth, so every tick compares the latest attested fixing with
     * the instrument's published mark and republishes the mark when they differ — the same
     * two writes the finalize path performs, done idempotently, and recorded.
     */
    void reconcileMark(StrikeSchedule s) {
        String id = s.getInstrumentId();
        Optional<SeriesRow> last = series.lastPriced(id);
        if (last.isEmpty() || last.get().tier() != 1 || last.get().price() == null) return;
        BigDecimal attested = last.get().price();
        String issuer = ledger.resolveParty("Issuer");
        Optional<BigDecimal> published = ledger.referencePriceOf(issuer, id);
        if (published.isPresent() && published.get().compareTo(attested) == 0) return;
        var ref = ledger.instrumentRefOf(issuer, id);
        if (ref.isEmpty()) return;
        ledger.submit(ref.get().issuer(), LedgerCommands.setReferencePrice(ref.get().contractId(), attested));
        List<String> funds = ledger.remarkFundsHolding(id);
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("previousMark", published.orElse(null));
        d.put("fixingCid", last.get().fixingCid());
        d.put("fundsRemarked", funds);
        events.append(FixingEvent.of(FixingEvent.Kinds.FIXING_FINALIZED, id, null, last.get().fixingCid(),
                "scheduler", "operator", null,
                "mark reconciled to the attested fixing (finalize had landed without the mark update)",
                attested, 1, last.get().fixingCid(), d));
        log.info("RECONCILE {} mark {} -> {} (fixing {}); funds re-marked: {}", id,
                published.orElse(null), attested, last.get().fixingCid(), funds);
    }

    void evaluate(StrikeSchedule s, Instant now) {
        LocalDate today = s.dateOf(now);
        if (!FixingSchedule.isBusinessDay(today)) return;
        Instant strike = s.strikeInstantOn(today);
        if (now.isBefore(strike)) return;
        Instant windowEnd = strike.plus(Duration.ofMinutes(s.getWindowMinutes()));
        String id = s.getInstrumentId();

        // An ATTESTED row settles the day. A fallback row (tier 3/4/5) does not close it: an
        // operator may still restrike by hand, and a proposal that then reaches K is
        // finalised and supersedes the gap. What a fallback row does stop is a SECOND
        // fallback for the same day.
        Optional<SeriesRow> row = todayRow(id, today);
        if (row.isPresent() && row.get().tier() == 1) return;
        boolean fallbackPublished = row.isPresent();

        Optional<LedgerService.FixingProposalView> open = openProposalSince(s, strike);
        if (open.isPresent()) {
            var p = open.get();
            if (p.quorumReached()) {
                finalize(p, "scheduler");
            } else if (now.isAfter(windowEnd) && !fallbackPublished) {
                fallback(s, now, "window closed with " + p.approvers().size() + " of "
                        + p.threshold() + " signatures on " + p.contractId());
            }
            return;
        }
        if (fallbackPublished) return;

        if (!now.isAfter(windowEnd)) {
            if (attemptedToday(s, strike)) return;              // struck, refused, or failed — manual from here
            if (!dependenciesSettled(s, today)) return;         // LX1 waits for its components
            try {
                propose(s, "scheduler");
            } catch (RuntimeException e) {
                recordStrikeFailure(s, e.getMessage());
            }
            return;
        }
        fallback(s, now, "no proposal reached the ledger inside the window");
    }

    // ---- propose / finalize / fallback -------------------------------------------

    /** Open today's proposal now. {@code actor} is who asked (an admin's e-mail, or "scheduler"). */
    public Map<String, Object> propose(StrikeSchedule s, String actor) {
        String operator = ledger.resolveParty(props.getOperatorParty());
        LedgerService.CommitteeView committee = committeeFor(operator).orElseThrow(() ->
                new IllegalStateException("no OperatorCommittee has " + LedgerService.labelOf(operator)
                        + " as a member — stand one up (POST /api/committee) before striking"));
        String id = s.getInstrumentId();
        Map<String, Object> inputs = new LinkedHashMap<>();
        String proposalCid;
        BigDecimal price;
        if (s.isFund()) {
            price = ledger.referencePriceOf("Issuer", id).orElseThrow(() -> new IllegalStateException(
                    id + " has no complete NAV: a component is missing a mark, and a gap is not estimated"));
            String rationale = "fund NAV per share = Σ units per share × component marks, at the "
                    + s.getStrikeAt() + " " + s.getTimezone() + " strike (" + actor + ")";
            inputs.put("navPerShare", price);
            var resp = desk.proposeFixing(committee.contractId(), new Dtos.ProposeFixingRequest(
                    operator, id, "USDC", s.getSession(), price, rationale));
            proposalCid = resp.getBody() == null ? null : resp.getBody().contractId();
        } else {
            MarketData.LiveMark mark = marketData.liveMarkOf(id).orElseThrow(() -> new IllegalStateException(
                    "no benchmark print for " + id + ": the reference feed did not answer — propose by hand"));
            BigDecimal factor = series.lastAttestedFactor(id).orElse(BigDecimal.ONE);
            String rationale = mark.source() + " " + mark.symbol() + " " + mark.price().stripTrailingZeros()
                    .toPlainString() + " @ " + mark.asOf() + " × last attested factor "
                    + factor.stripTrailingZeros().toPlainString()
                    + " (stand-in for the CME CF reference rate); scheduled strike " + s.getStrikeAt()
                    + " " + s.getTimezone() + " (" + actor + ")";
            inputs.put("benchmarkPrice", mark.price());
            inputs.put("benchmarkSource", mark.source() + " " + mark.symbol());
            inputs.put("benchmarkAsOf", String.valueOf(mark.asOf()));
            inputs.put("parFactor", factor);
            price = mark.price().multiply(factor);
            var resp = desk.proposeWrappedFixing(committee.contractId(), new Dtos.ProposeWrappedFixingRequest(
                    operator, id, "USDC", s.getSession(), mark.price(), factor, rationale));
            proposalCid = resp.getBody() == null ? null : resp.getBody().contractId();
        }
        Map<String, Object> d = new LinkedHashMap<>(inputs);
        d.put("committeeCid", committee.contractId());
        d.put("trigger", actor);
        events.append(FixingEvent.of(FixingEvent.Kinds.STRIKE_SCHEDULED, id, proposalCid, proposalCid, actor,
                "operator", null, "strike: proposal opened", price, null, proposalCid, d));
        log.info("STRIKE {} proposal {} price={} by {}", id, proposalCid, price, actor);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("instrument", id);
        out.put("proposalCid", proposalCid);
        out.put("committeeCid", committee.contractId());
        out.put("price", price);
        out.put("inputs", inputs);
        out.put("proposer", LedgerService.labelOf(operator));
        out.put("note", "proposal opened on committee " + committee.label() + " (" + committee.threshold()
                + " of " + committee.members().size() + " needed); signers notified");
        return out;
    }

    void finalize(LedgerService.FixingProposalView p, String actor) {
        List<String> publishTo = new ArrayList<>();
        try {
            publishTo.add(ledger.resolveParty("Venue"));
        } catch (RuntimeException e) {
            // no venue party on this ledger — the auditor still observes the fixing
        }
        var resp = desk.finalizeFixing(p.contractId(), new Dtos.FinalizeFixingRequest(p.proposer(), publishTo));
        log.info("STRIKE {} finalised {} -> {} by {}", p.instrumentId(), p.contractId(),
                resp.getBody() == null ? "?" : resp.getBody().contractId(), actor);
    }

    void fallback(StrikeSchedule s, Instant now, String why) {
        String id = s.getInstrumentId();
        BigDecimal print = s.isFund() ? null : marketData.liveMarkOf(id).map(MarketData.LiveMark::price).orElse(null);
        BigDecimal factor = s.isFund() ? null : series.lastAttestedFactor(id).orElse(null);
        Optional<SeriesRow> prior = series.series(id).stream()
                .filter(r -> r.price() != null && (r.tier() == 1 || r.tier() == 3 || r.tier() == 4))
                .findFirst();
        FallbackPolicy.Decision d = FallbackPolicy.decide(s, new FallbackPolicy.Inputs(!s.isFund(), print, factor,
                prior.map(SeriesRow::price).orElse(null),
                prior.map(r -> "tier " + r.tier() + " value of " + r.date()).orElse(null)));
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("why", why);
        details.put("session", s.getSession());
        details.put("tier2", FallbackPolicy.TIER2_STATUS);
        if (d.tier() == 3) {
            details.put("referencePrice", print.toPlainString());
            details.put("wrapperFactor", factor.toPlainString());
        }
        if (d.tier() == 4) {
            details.put("carriedFrom", prior.map(SeriesRow::asOf).orElse(null));
            details.put("carriedFromCid", prior.map(SeriesRow::fixingCid).orElse(null));
        }
        String kind = d.tier() == 5 ? FixingEvent.Kinds.FIXING_MISSED : FixingEvent.Kinds.FIXING_FALLBACK;
        events.append(FixingEvent.of(kind, id, null, null, "scheduler", "operator", null, d.note(), d.price(),
                d.tier(), null, details));
        log.info("STRIKE {} fallback tier {}: {}", id, d.tier(), d.note());
        if (d.tier() == 5) {
            webhooks.dispatch(WebhookDispatcher.FIXING_MISSED, id, null, null, null, null, now);
        }
    }

    // ---- status (admin console "fallback status per instrument") ------------------

    public List<Map<String, Object>> statuses(Instant now) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (StrikeSchedule s : schedules.all()) {
            out.add(status(s, now));
        }
        return out;
    }

    public Map<String, Object> status(StrikeSchedule s, Instant now) {
        LocalDate today = s.dateOf(now);
        Instant strike = s.strikeInstantOn(today);
        Instant windowEnd = strike.plus(Duration.ofMinutes(s.getWindowMinutes()));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("instrument", s.getInstrumentId());
        out.put("session", s.getSession());
        out.put("kind", s.getKind());
        out.put("enabled", s.isEnabled());
        out.put("strikeAt", s.getStrikeAt());
        out.put("timezone", s.getTimezone());
        out.put("windowMinutes", s.getWindowMinutes());
        out.put("todayStrikeAt", strike.toString());
        out.put("windowEndsAt", windowEnd.toString());
        out.put("tiersEnabled", s.getTiersEnabled());
        out.put("tier2", FallbackPolicy.TIER2_STATUS);
        out.put("dependsOn", s.getDependsOn());
        String state;
        Optional<SeriesRow> row = Optional.empty();
        Optional<LedgerService.FixingProposalView> open = Optional.empty();
        boolean deps = true;
        try {
            row = todayRow(s.getInstrumentId(), today);
            open = openProposalSince(s, strike);
            deps = dependenciesSettled(s, today);
        } catch (RuntimeException e) {
            out.put("error", e.getMessage());
        }
        if (!FixingSchedule.isBusinessDay(today)) state = "NOT_DUE_TODAY";
        else if (row.isPresent() && row.get().tier() == 1) state = "STRUCK";
        else if (open.isPresent()) state = open.get().quorumReached() ? "QUORUM"
                : now.isAfter(windowEnd) && !row.isPresent() ? "FALLBACK_DUE" : "WAITING_SIGNATURES";
        else if (row.isPresent()) state = row.get().tier() == 5 ? "MISSED" : "FALLBACK_PUBLISHED";
        else if (now.isBefore(strike)) state = "PENDING";
        else if (now.isAfter(windowEnd)) state = "FALLBACK_DUE";
        else if (!deps) state = "WAITING_COMPONENTS";
        else state = "DUE";
        out.put("state", state);
        out.put("dependenciesSettled", deps);
        out.put("todayRow", row.orElse(null));
        open.ifPresent(p -> {
            Map<String, Object> op = new LinkedHashMap<>();
            op.put("cid", p.contractId());
            op.put("signatures", p.approvers().size());
            op.put("threshold", p.threshold());
            op.put("approvers", p.approvers().stream().map(LedgerService::labelOf).toList());
            op.put("price", p.price());
            out.put("openProposal", op);
        });
        out.put("checkedAt", now.toString());
        return out;
    }

    // ---- helpers ----------------------------------------------------------------

    Optional<LedgerService.CommitteeView> committeeFor(String operator) {
        String label = LedgerService.labelOf(operator);
        return ledger.committeesVisibleTo(operator).stream()
                .filter(c -> c.members().stream().map(LedgerService::labelOf).anyMatch(label::equals))
                .sorted(Comparator.comparing((LedgerService.CommitteeView c) ->
                        c.label() != null && c.label().toLowerCase().contains("crossdesk") ? 0 : 1))
                .findFirst();
    }

    Optional<SeriesRow> todayRow(String id, LocalDate today) {
        return series.series(id).stream()
                .filter(r -> r.tier() >= 1 && today.toString().equals(r.date()))
                .findFirst();
    }

    Optional<LedgerService.FixingProposalView> openProposalSince(StrikeSchedule s, Instant since) {
        String operator = ledger.resolveParty(props.getOperatorParty());
        return ledger.fixingProposalsVisibleTo(operator).stream()
                .filter(p -> p.instrumentId().equalsIgnoreCase(s.getInstrumentId()))
                .filter(p -> p.session().equalsIgnoreCase(s.getSession()))
                .filter(p -> !p.accrualFrom().isBefore(since))
                .max(Comparator.comparing(LedgerService.FixingProposalView::accrualFrom));
    }

    boolean attemptedToday(StrikeSchedule s, Instant strike) {
        return events.query(s.getInstrumentId(), strike, null).stream().anyMatch(e ->
                FixingEvent.Kinds.STRIKE_SCHEDULED.equals(e.kind())
                        || FixingEvent.Kinds.STRIKE_FAILED.equals(e.kind())
                        || FixingEvent.Kinds.PROPOSAL_CREATED.equals(e.kind())
                        || FixingEvent.Kinds.PROPOSAL_RESTRUCK.equals(e.kind()));
    }

    boolean dependenciesSettled(StrikeSchedule s, LocalDate today) {
        for (String dep : s.getDependsOn()) {
            Optional<SeriesRow> r = todayRow(dep, today);
            if (r.isEmpty() || r.get().price() == null) return false;
        }
        return true;
    }

    private void recordStrikeFailure(StrikeSchedule s, String why) {
        events.append(FixingEvent.of(FixingEvent.Kinds.STRIKE_FAILED, s.getInstrumentId(), null, null,
                "scheduler", "operator", null, why, null, null, null, Map.of("session", s.getSession())));
        log.warn("STRIKE {} could not propose: {}", s.getInstrumentId(), why);
    }
}
