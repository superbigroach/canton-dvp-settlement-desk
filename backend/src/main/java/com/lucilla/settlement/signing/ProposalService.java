package com.lucilla.settlement.signing;

import com.lucilla.settlement.auth.AuthException;
import com.lucilla.settlement.auth.Principal;
import com.lucilla.settlement.auth.Role;
import com.lucilla.settlement.auth.UserRecord;
import com.lucilla.settlement.auth.UserStore;
import com.lucilla.settlement.events.EventStore;
import com.lucilla.settlement.events.FixingEvent;
import com.lucilla.settlement.ledger.LedgerService;
import com.lucilla.settlement.ledger.SignerEvidence;
import com.lucilla.settlement.ledger.SignerProtocol;
import com.lucilla.settlement.scheduler.ScheduleStore;
import com.lucilla.settlement.scheduler.StrikeSchedule;
import com.lucilla.settlement.web.Dtos;
import com.lucilla.settlement.web.SettlementController;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The signer's view of the committee's work — docs/PRODUCT-PLAN.md §5
 * {@code /api/proposals}. Wraps the existing {@code /fixing/{cid}/confirm-checked} path
 * with the caller's own party and seat, so a portal user never types either.
 *
 * <p>REFUSAL IS OFF-LEDGER, and that is what the Daml allows: {@code FixingProposal} has
 * no refuse choice ({@code WithdrawFixing} is the proposer's, and a member declining is
 * not the proposer abandoning). A refusal is therefore recorded as an event naming the
 * condition that failed — which is exactly what docs/SIGNER_PROTOCOL.md §1 says a
 * refusal is for — and the proposal simply never gathers that member's signature.
 */
@Service
public class ProposalService {

    private final LedgerService ledger;
    private final EventStore events;
    private final UserStore users;
    private final ScheduleStore schedules;
    private final SettlementController desk;

    public ProposalService(LedgerService ledger, EventStore events, UserStore users,
            ScheduleStore schedules, SettlementController desk) {
        this.ledger = ledger;
        this.events = events;
        this.users = users;
        this.schedules = schedules;
        this.desk = desk;
    }

    // ---- listing ----------------------------------------------------------------

    public List<Map<String, Object>> list(Principal me, String status, boolean mine) {
        String party = partyOf(me);
        boolean all = "all".equalsIgnoreCase(status);
        List<Map<String, Object>> out = new ArrayList<>();
        for (var p : ledger.fixingProposalsVisibleTo(party)) {
            if (mine && !coversInstrument(me, p.instrumentId())) continue;
            out.add(view(p, me, party));
        }
        if (all) {
            // Closed proposals live only in the event log now (the contract is archived).
            java.util.Set<String> openRoots = new java.util.HashSet<>();
            for (var v : out) openRoots.add(String.valueOf(v.get("rootCid")));
            Map<String, List<FixingEvent>> byRoot = new LinkedHashMap<>();
            for (FixingEvent e : events.all()) {
                if (e.rootCid() == null) continue;
                if (!e.kind().startsWith("proposal.") && !e.kind().startsWith("fixing.")) continue;
                byRoot.computeIfAbsent(e.rootCid(), k -> new ArrayList<>()).add(e);
            }
            for (var entry : byRoot.entrySet()) {
                if (openRoots.contains(entry.getKey())) continue;
                Map<String, Object> h = historyView(entry.getKey(), entry.getValue(), me);
                if (h == null) continue;
                if (mine && !coversInstrument(me, String.valueOf(h.get("instrument")))) continue;
                out.add(h);
            }
        }
        out.sort(Comparator.comparing((Map<String, Object> v) -> String.valueOf(v.get("createdAt"))).reversed());
        return out;
    }

    public Optional<Map<String, Object>> one(Principal me, String cid) {
        String party = partyOf(me);
        String root = events.rootOf(cid);
        return ledger.fixingProposalsVisibleTo(party).stream()
                .filter(p -> p.contractId().equals(cid) || events.rootOf(p.contractId()).equals(root))
                .findFirst()
                .map(p -> view(p, me, party));
    }

    public List<FixingEvent> eventsOf(String cid) {
        List<FixingEvent> list = new ArrayList<>(events.byProposal(cid));
        list.sort(Comparator.comparingLong(FixingEvent::id).reversed());
        return list;
    }

    // ---- actions ----------------------------------------------------------------

    /**
     * Confirm with checks as the caller's seat.
     *
     * <p>Body: {@code { checks: [...], evidence: ... }}. For the VENUE, {@code evidence} is
     * {@code {low, high}} — the traded range the ledger enforces, unchanged. For the ISSUER
     * and the LENDER it is REQUIRED and per condition: {@code { "<condition>": { <field>:
     * <value> } }} in the shape {@code GET /api/signer-protocol} publishes. The desk applies
     * each condition's rule (the lender's mark against its own declared tolerance from
     * {@code /api/signer/settings}, default 25 bp) BEFORE submitting, refuses with the
     * specific failure as a 422 carrying the schema, and records {@code verified: true}
     * with the numbers on the event. A bare tick is never accepted for those two seats.
     */
    public Map<String, Object> confirm(Principal me, String cid, List<String> checks, Map<String, Object> evidence) {
        requireSigner(me);
        String party = partyOf(me);
        String target = currentCidFor(party, cid);
        String seat = me.seat().trim().toLowerCase();
        SignerProtocol.Role role = SignerProtocol.role(seat);
        BigDecimal low = null, high = null;
        Map<String, Object> toVerify = null;
        SignerEvidence.Tolerances tol = null;
        SignerEvidence.Result result = null;
        if (role != null && role.requiresObservedRange()) {
            // The venue path, exactly as before: the range is the evidence, and the ledger checks it.
            low = evidence == null ? null : SignerEvidence.number(evidence.get("low"));
            high = evidence == null ? null : SignerEvidence.number(evidence.get("high"));
        } else if (SignerEvidence.required(seat)) {
            Map<String, Object> schema = SignerEvidence.schemaFor(seat, checks);
            if (evidence == null || evidence.isEmpty()) {
                throw new SignerEvidence.Rejected("the " + seat + " seat confirms with evidence, not a tick: "
                        + "supply the numbers for each checked condition (see `evidence`)", seat,
                        List.of("evidence missing for " + checks), schema);
            }
            tol = SignerEvidence.Tolerances.from(users.byUid(me.uid())
                    .map(u -> u.getSettings() == null ? null : u.getSettings().getTolerances()).orElse(null));
            BigDecimal price = ledger.fixingProposalsVisibleTo(party).stream()
                    .filter(p -> p.contractId().equals(target))
                    .map(LedgerService.FixingProposalView::price).findFirst().orElse(null);
            result = SignerEvidence.verify(seat, checks, evidence, price, tol, Instant.now());
            if (!result.ok()) {
                throw new SignerEvidence.Rejected("evidence refused for the " + seat + " seat: "
                        + String.join("; ", result.problems()), seat, result.problems(), schema);
            }
            toVerify = evidence;
        }
        var req = new Dtos.ConfirmWithChecksRequest(party, seat, null, checks, low, high, toVerify,
                tol == null ? null : tol.markBps(), tol == null ? null : tol.liquidationBps());
        var resp = desk.confirmFixingWithChecks(target, req);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("confirmed", true);
        out.put("proposalCid", target);
        out.put("nextCid", resp.getBody() == null ? null : resp.getBody().contractId());
        out.put("member", LedgerService.labelOf(party));
        out.put("seat", seat);
        out.put("checks", checks);
        if (result != null) {
            out.put("verified", true);
            out.put("evidence", result.verified());
            out.put("tolerances", Map.of("markBps", tol.markBps(), "liquidationBps", tol.liquidationBps()));
        } else if (low != null && high != null) {
            out.put("evidence", Map.of("low", low, "high", high));
        }
        return out;
    }

    /** Refuse, naming the condition that failed. Recorded as an event; nothing on-ledger changes. */
    public Map<String, Object> refuse(Principal me, String cid, String condition, String reason) {
        requireSigner(me);
        if (condition == null || condition.isBlank()) {
            throw new IllegalArgumentException("a refusal must name the condition that failed");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("a refusal must give a reason");
        }
        SignerProtocol.Role role = SignerProtocol.role(me.seat());
        if (role == null) {
            throw AuthException.forbidden("your user has no signer seat");
        }
        boolean known = role.conditions().stream().anyMatch(c -> c.name().equals(condition.trim()));
        if (!known) {
            throw new IllegalArgumentException("condition '" + condition + "' is not one the " + role.key()
                    + " seat verifies; expected one of "
                    + role.conditions().stream().map(SignerProtocol.Condition::name).toList());
        }
        String party = partyOf(me);
        String root = events.rootOf(cid);
        String instrument = instrumentOf(party, cid, root);
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("onLedger", false);
        d.put("note", "the Daml FixingProposal has no refuse choice; the refusal is recorded here and "
                + "the member's signature is simply never added");
        FixingEvent ev = events.append(FixingEvent.of(FixingEvent.Kinds.PROPOSAL_REFUSED, instrument, cid,
                root, me.email() == null ? LedgerService.labelOf(party) : me.email() + " (" + LedgerService.labelOf(party) + ")",
                me.seat(), condition.trim(), reason.trim(), null, null, null, d));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("refused", true);
        out.put("onLedger", false);
        out.put("proposalCid", cid);
        out.put("condition", condition.trim());
        out.put("reason", reason.trim());
        out.put("event", ev);
        return out;
    }

    // ---- views ------------------------------------------------------------------

    Map<String, Object> view(LedgerService.FixingProposalView p, Principal me, String party) {
        String root = events.rootOf(p.contractId());
        List<FixingEvent> chain = events.byProposal(p.contractId());
        Instant created = chain.stream().map(FixingEvent::instant).min(Comparator.naturalOrder())
                .orElse(p.accrualFrom());
        Instant deadline = chain.stream()
                .filter(e -> e.details() != null && e.details().get("deadline") instanceof String)
                .map(e -> Instant.parse((String) e.details().get("deadline")))
                .findFirst()
                .orElse(created.plus(Duration.ofMinutes(windowMinutes(p.instrumentId()))));
        List<Map<String, Object>> refusals = new ArrayList<>();
        for (FixingEvent e : chain) {
            if (FixingEvent.Kinds.PROPOSAL_REFUSED.equals(e.kind())) {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("actor", e.actor());
                r.put("seat", e.seat());
                r.put("condition", e.condition());
                r.put("reason", e.reason());
                r.put("ts", e.ts());
                refusals.add(r);
            }
        }
        List<Map<String, Object>> attestations = new ArrayList<>();
        BigDecimal venueLow = null, venueHigh = null;
        for (var a : p.attestations()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("member", LedgerService.labelOf(a.member()));
            m.put("seat", a.role());
            m.put("protocolRef", a.protocolRef());
            m.put("checks", a.checksPassed());
            if (a.observedLow() != null) { m.put("observedLow", a.observedLow()); venueLow = a.observedLow(); }
            if (a.observedHigh() != null) { m.put("observedHigh", a.observedHigh()); venueHigh = a.observedHigh(); }
            attestations.add(m);
        }
        String myLabel = LedgerService.labelOf(party);
        boolean iSigned = p.approvers().stream().map(LedgerService::labelOf).anyMatch(myLabel::equals);
        boolean iProposed = LedgerService.labelOf(p.proposer()).equals(myLabel);
        boolean iRefused = refusals.stream().anyMatch(r -> me.seat() != null && me.seat().equals(r.get("seat")));
        String action = iSigned ? (iProposed ? "proposed" : "confirmed") : iRefused ? "refused" : "pending";

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("cid", p.contractId());
        out.put("rootCid", root);
        out.put("instrument", p.instrumentId());
        out.put("session", p.session());
        out.put("cashInstrument", p.cashInstrument());
        // frontend/src/desk/types.ts Proposal.kind: 'wrapped' | 'nav' | 'snapshot'
        boolean isFund = isFund(p.instrumentId());
        out.put("kind", p.wrapperFactor() != null ? "wrapped" : isFund ? "nav" : "snapshot");
        out.put("accruing", p.ratePerAnnum().signum() != 0);
        if (isFund) out.put("navComponents", navComponents(p.instrumentId()));
        out.put("price", p.price());
        out.put("referencePrice", p.referencePrice());
        out.put("wrapperFactor", p.wrapperFactor());
        out.put("discountBps", p.wrapperFactor() == null ? null
                : BigDecimal.ONE.subtract(p.wrapperFactor()).multiply(BigDecimal.valueOf(10000)).stripTrailingZeros());
        out.put("rationale", p.rationale());
        out.put("proposer", LedgerService.labelOf(p.proposer()));
        out.put("proposedBy", LedgerService.labelOf(p.proposer()));
        out.put("createdAt", created.toString());
        out.put("proposedAt", created.toString());
        out.put("deadline", deadline.toString());
        out.put("threshold", p.threshold());
        out.put("k", p.threshold());
        out.put("n", p.members().size());
        out.put("members", p.members().stream().map(LedgerService::labelOf).toList());
        List<String> approverLabels = p.approvers().stream().map(LedgerService::labelOf).toList();
        out.put("approvers", approverLabels);
        out.put("confirmed", approverLabels);
        out.put("signatures", p.approvers().size());
        out.put("quorumReached", p.quorumReached());
        // types.ts ProposalStatus: 'open' | 'finalized' | 'refused' | 'restruck' | 'missed'.
        // An on-ledger proposal is open; one a seat has refused says so, because that is
        // the fact the other seats and the operator need to act on.
        out.put("status", refusals.isEmpty() ? "open" : "refused");
        out.put("tier", p.tier());
        out.put("attestations", attestations);
        out.put("refusals", refusals);
        if (venueLow != null || venueHigh != null) {
            out.put("venueRange", Map.of("low", venueLow, "high", venueHigh));
        }
        boolean canConfirm = !iSigned && me.role() == Role.SIGNER && me.seat() != null
                && p.members().stream().map(LedgerService::labelOf).anyMatch(myLabel::equals);
        out.put("my", myView(me, party, action, canConfirm));
        SignerProtocol.Role role = SignerProtocol.role(me.seat());
        out.put("conditions", role == null ? List.of()
                : role.conditions().stream().map(SignerProtocol.Condition::name).toList());
        out.put("requiresObservedRange", role != null && role.requiresObservedRange());
        out.put("mine", mineOf(chain, me, myLabel, p.attestations()));
        out.put("seats", seatsOf(p.members()));
        return out;
    }

    /** types.ts Proposal.mine — what this caller already did, from the event log and the ledger. */
    private Map<String, Object> mineOf(List<FixingEvent> chain, Principal me, String myLabel,
            List<LedgerService.SignerCheckView> attestations) {
        String email = me.email();
        for (int i = chain.size() - 1; i >= 0; i--) {
            FixingEvent e = chain.get(i);
            boolean byMe = e.actor() != null && ((email != null && e.actor().startsWith(email))
                    || e.actor().equals(myLabel) || e.actor().endsWith("(" + myLabel + ")"));
            if (!byMe) continue;
            if (FixingEvent.Kinds.PROPOSAL_CONFIRMED.equals(e.kind())) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("action", "confirmed");
                m.put("at", e.ts());
                Object checks = e.details() == null ? null : e.details().get("checks");
                m.put("checks", checks);
                Object lo = e.details() == null ? null : e.details().get("observedLow");
                Object hi = e.details() == null ? null : e.details().get("observedHigh");
                if (lo != null && hi != null) m.put("evidence", Map.of("low", new BigDecimal(lo.toString()), "high", new BigDecimal(hi.toString())));
                m.put("cid", e.ledgerCid());
                return m;
            }
            if (FixingEvent.Kinds.PROPOSAL_REFUSED.equals(e.kind())) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("action", "refused");
                m.put("at", e.ts());
                m.put("condition", e.condition());
                m.put("reason", e.reason());
                return m;
            }
        }
        // Signed on-ledger but not through this desk's routes (the operator desk): still mine.
        for (var a : attestations) {
            if (LedgerService.labelOf(a.member()).equals(myLabel)) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("action", "confirmed");
                m.put("at", null);
                m.put("checks", a.checksPassed());
                if (a.observedLow() != null && a.observedHigh() != null) {
                    m.put("evidence", Map.of("low", a.observedLow(), "high", a.observedHigh()));
                }
                return m;
            }
        }
        return null;
    }

    private boolean isFund(String instrumentId) {
        try {
            return ledger.basketById(instrumentId).isPresent();
        } catch (RuntimeException e) {
            return false;
        }
    }

    private List<Map<String, Object>> navComponents(String fundId) {
        List<Map<String, Object>> out = new ArrayList<>();
        try {
            var b = ledger.basketById(fundId).orElse(null);
            if (b == null) return out;
            for (var c : b.components()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("instrumentId", c.instrumentId());
                m.put("unitsPerShare", c.unitsPerShare());
                m.put("mark", ledger.referencePriceOf("Issuer", c.instrumentId()).orElse(null));
                out.add(m);
            }
        } catch (RuntimeException e) {
            // no marks readable — the recipe still shows
        }
        return out;
    }

    private Map<String, Object> historyView(String root, List<FixingEvent> chain, Principal me) {
        FixingEvent opened = chain.stream()
                .filter(e -> FixingEvent.Kinds.PROPOSAL_CREATED.equals(e.kind())
                        || FixingEvent.Kinds.PROPOSAL_RESTRUCK.equals(e.kind()))
                .findFirst().orElse(null);
        if (opened == null) return null;
        FixingEvent finalised = chain.stream()
                .filter(e -> FixingEvent.Kinds.FIXING_FINALIZED.equals(e.kind())).findFirst().orElse(null);
        // types.ts ProposalStatus — a proposal that never finalised was either restruck
        // (a later one for the same identifier that day), missed (a gap was published), or
        // refused; "closed" only when the log says nothing more.
        String status = finalised != null ? "finalized"
                : chain.stream().anyMatch(e -> FixingEvent.Kinds.PROPOSAL_WITHDRAWN.equals(e.kind())) ? "restruck"
                : chain.stream().anyMatch(e -> FixingEvent.Kinds.FIXING_MISSED.equals(e.kind())
                        || FixingEvent.Kinds.FIXING_FALLBACK.equals(e.kind())) ? "missed"
                : laterProposalExists(opened) ? "restruck"
                : chain.stream().anyMatch(e -> FixingEvent.Kinds.PROPOSAL_REFUSED.equals(e.kind())) ? "refused"
                : "closed";
        List<String> approvers = new ArrayList<>();
        approvers.add(partyLabelOf(opened.actor()));
        for (FixingEvent e : chain) {
            if (FixingEvent.Kinds.PROPOSAL_CONFIRMED.equals(e.kind())) approvers.add(partyLabelOf(e.actor()));
        }
        String myLabel = LedgerService.labelOf(me.party());
        boolean iSigned = approvers.contains(myLabel);
        boolean iRefused = chain.stream().anyMatch(e -> FixingEvent.Kinds.PROPOSAL_REFUSED.equals(e.kind())
                && me.seat() != null && me.seat().equals(e.seat()));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("cid", events.latestCidOf(root).orElse(root));
        out.put("rootCid", root);
        out.put("instrument", opened.instrument());
        out.put("session", opened.details() == null ? "Close" : opened.details().getOrDefault("session", "Close"));
        out.put("price", finalised != null ? finalised.price() : opened.price());
        out.put("referencePrice", detail(opened, "referencePrice"));
        out.put("wrapperFactor", detail(opened, "wrapperFactor"));
        out.put("rationale", opened.reason());
        out.put("proposer", partyLabelOf(opened.actor()));
        out.put("proposedBy", partyLabelOf(opened.actor()));
        out.put("createdAt", opened.ts());
        out.put("proposedAt", opened.ts());
        out.put("deadline", opened.details() == null ? null : opened.details().get("deadline"));
        out.put("kind", detail(opened, "wrapperFactor") != null ? "wrapped" : isFund(opened.instrument()) ? "nav" : "snapshot");
        out.put("approvers", approvers);
        out.put("confirmed", approvers);
        out.put("signatures", approvers.size());
        Object threshold = finalised == null || finalised.details() == null ? null : finalised.details().get("threshold");
        out.put("k", threshold);
        out.put("n", null);
        out.put("status", status);
        out.put("fixingCid", finalised == null ? null : finalised.ledgerCid());
        out.put("finalizedAt", finalised == null ? null : finalised.ts());
        out.put("my", myView(me, me.party(), iSigned ? "confirmed" : iRefused ? "refused" : "none", false));
        SignerProtocol.Role role = SignerProtocol.role(me.seat());
        out.put("conditions", role == null ? List.of()
                : role.conditions().stream().map(SignerProtocol.Condition::name).toList());
        out.put("requiresObservedRange", role != null && role.requiresObservedRange());
        out.put("mine", mineOf(chain, me, myLabel, List.of()));
        return out;
    }

    private boolean laterProposalExists(FixingEvent opened) {
        String day = opened.ts().substring(0, 10);
        return events.query(opened.instrument(), opened.instant(), null).stream()
                .anyMatch(e -> !e.rootCid().equals(opened.rootCid())
                        && (FixingEvent.Kinds.PROPOSAL_CREATED.equals(e.kind())
                            || FixingEvent.Kinds.PROPOSAL_RESTRUCK.equals(e.kind()))
                        && e.ts().startsWith(day));
    }

    private Map<String, Object> myView(Principal me, String party, String action, boolean canConfirm) {
        SignerProtocol.Role role = SignerProtocol.role(me.seat());
        Map<String, Object> my = new LinkedHashMap<>();
        my.put("party", LedgerService.labelOf(party));
        my.put("seat", me.seat());
        my.put("conditions", role == null ? List.of()
                : role.conditions().stream().map(c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", c.name());
                    m.put("passesWhen", c.passesWhen());
                    m.put("evidence", SignerEvidence.schemaOf(c.evidence()));
                    return m;
                }).toList());
        my.put("requiresObservedRange", role != null && role.requiresObservedRange());
        my.put("requiresEvidence", role != null && role.requiresEvidence());
        my.put("action", action);
        my.put("canConfirm", canConfirm);
        return my;
    }

    /** Which seat each member party holds, from the user mapping. */
    private List<Map<String, Object>> seatsOf(List<String> members) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (String m : members) {
            String label = LedgerService.labelOf(m);
            List<UserRecord> us = users.byParty(label);
            String seat = us.stream().map(UserRecord::getSeat).filter(s -> s != null && !s.isBlank())
                    .findFirst().orElse(null);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("party", label);
            row.put("seat", seat);
            row.put("users", us.stream().map(UserRecord::getEmail).filter(e -> e != null).toList());
            out.add(row);
        }
        return out;
    }

    // ---- helpers ----------------------------------------------------------------

    private static Object detail(FixingEvent e, String key) {
        return e.details() == null ? null : e.details().get(key);
    }

    /** "email (Label)" → "Label"; a bare label stays. */
    static String partyLabelOf(String actor) {
        if (actor == null) return null;
        int i = actor.lastIndexOf('(');
        if (i >= 0 && actor.endsWith(")")) return actor.substring(i + 1, actor.length() - 1);
        return LedgerService.labelOf(actor);
    }

    private String partyOf(Principal me) {
        if (!me.hasParty()) {
            throw AuthException.forbidden("your user has no ledger party mapped");
        }
        return ledger.resolveParty(me.party());
    }

    private static void requireSigner(Principal me) {
        if (me.role() != Role.SIGNER && me.role() != Role.ADMIN) {
            throw AuthException.forbidden("only a signer may act on a proposal");
        }
        if (me.seat() == null || me.seat().isBlank()) {
            throw AuthException.forbidden("your user has no signer seat (issuer | lender | venue)");
        }
    }

    private boolean coversInstrument(Principal me, String instrument) {
        if (me.role() == Role.ADMIN) return true;
        return me.instruments() != null && me.instruments().stream().anyMatch(i -> i.equalsIgnoreCase(instrument));
    }

    private int windowMinutes(String instrument) {
        return schedules.byInstrument(instrument).map(StrikeSchedule::getWindowMinutes)
                .orElse(StrikeSchedule.DEFAULT_WINDOW_MINUTES);
    }

    /**
     * The cid to exercise: the one given if it is still live, else the newest cid of the
     * same proposal (another member confirmed since the caller loaded the page). A cid
     * that is neither is a 409 — the proposal is gone.
     */
    private String currentCidFor(String party, String cid) {
        var open = ledger.fixingProposalsVisibleTo(party);
        if (open.stream().anyMatch(p -> p.contractId().equals(cid))) return cid;
        String root = events.rootOf(cid);
        return open.stream()
                .filter(p -> events.rootOf(p.contractId()).equals(root))
                .map(LedgerService.FixingProposalView::contractId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("proposal " + cid
                        + " is no longer open (finalised, withdrawn, or not visible to "
                        + LedgerService.labelOf(party) + ")"));
    }

    private String instrumentOf(String party, String cid, String root) {
        return ledger.fixingProposalsVisibleTo(party).stream()
                .filter(p -> p.contractId().equals(cid) || events.rootOf(p.contractId()).equals(root))
                .map(LedgerService.FixingProposalView::instrumentId)
                .findFirst()
                .orElseGet(() -> events.byProposal(cid).stream().map(FixingEvent::instrument)
                        .filter(i -> i != null).findFirst().orElse(null));
    }
}
