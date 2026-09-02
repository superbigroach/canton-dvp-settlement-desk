package com.lucilla.settlement.signing;

import com.lucilla.settlement.auth.Principal;
import com.lucilla.settlement.events.EventStore;
import com.lucilla.settlement.events.FixingEvent;
import com.lucilla.settlement.events.LifecycleEvent;
import com.lucilla.settlement.ledger.LedgerService;
import com.lucilla.settlement.scheduler.ScheduleStore;
import com.lucilla.settlement.scheduler.StrikeSchedule;
import com.lucilla.settlement.webhooks.WebhookDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Turns the controller's {@link LifecycleEvent}s into {@code fixing_events} rows and
 * signer webhooks — the "every step writes an event" half of docs/PRODUCT-PLAN.md §4.
 *
 * <p>Runs synchronously on the request thread (Spring's default), which is what lets it
 * read the caller's identity off the request and name the ACTOR as a person rather than
 * as a party: "issuer@… (Issuer)" is an audit line, "Issuer" is a guess.
 */
@Component
public class LifecycleRecorder {

    private static final Logger log = LoggerFactory.getLogger(LifecycleRecorder.class);

    private final EventStore events;
    private final WebhookDispatcher webhooks;
    private final ScheduleStore schedules;

    public LifecycleRecorder(EventStore events, WebhookDispatcher webhooks, ScheduleStore schedules) {
        this.events = events;
        this.webhooks = webhooks;
        this.schedules = schedules;
    }

    @EventListener
    public void on(LifecycleEvent e) {
        try {
            handle(e);
        } catch (RuntimeException ex) {
            log.warn("lifecycle event {} not recorded: {}", e.kind(), ex.toString());
        }
    }

    private void handle(LifecycleEvent e) {
        String actor = actorName(e.actorParty());
        Map<String, Object> details = new LinkedHashMap<>(e.extra() == null ? Map.of() : e.extra());
        switch (e.kind()) {
            case FixingEvent.Kinds.PROPOSAL_CREATED -> {
                String session = String.valueOf(details.getOrDefault("session", "Close"));
                boolean restruck = isRestrike(e.instrument(), session);
                String kind = restruck ? FixingEvent.Kinds.PROPOSAL_RESTRUCK : FixingEvent.Kinds.PROPOSAL_CREATED;
                Instant deadline = deadlineFor(e.instrument());
                details.put("deadline", deadline.toString());
                if (e.referencePrice() != null) details.put("referencePrice", e.referencePrice().toPlainString());
                if (e.wrapperFactor() != null) details.put("wrapperFactor", e.wrapperFactor().toPlainString());
                events.append(FixingEvent.of(kind, e.instrument(), e.resultCid(), e.resultCid(), actor,
                        seatOf(e), null, e.rationale(), e.price(), null, e.resultCid(), details));
                webhooks.dispatch(restruck ? WebhookDispatcher.PROPOSAL_RESTRUCK : WebhookDispatcher.PROPOSAL_CREATED,
                        e.instrument(), e.resultCid(), e.price(), e.referencePrice(), e.wrapperFactor(), deadline);
            }
            case FixingEvent.Kinds.PROPOSAL_CONFIRMED -> {
                String root = events.rootOf(e.proposalCid());
                String instrument = e.instrument() != null ? e.instrument() : instrumentOf(root);
                details.put("successorCid", e.resultCid());
                if (e.checks() != null && !e.checks().isEmpty()) details.put("checks", e.checks());
                events.append(FixingEvent.of(FixingEvent.Kinds.PROPOSAL_CONFIRMED, instrument,
                        e.proposalCid(), root, actor, seatOf(e), null,
                        e.checks() == null || e.checks().isEmpty() ? null : String.join(",", e.checks()),
                        null, null, e.resultCid(), details));
            }
            case FixingEvent.Kinds.FIXING_FINALIZED -> {
                String root = events.rootOf(e.proposalCid());
                if (e.referencePrice() != null) details.put("referencePrice", e.referencePrice().toPlainString());
                if (e.wrapperFactor() != null) details.put("wrapperFactor", e.wrapperFactor().toPlainString());
                events.append(FixingEvent.of(FixingEvent.Kinds.FIXING_FINALIZED, e.instrument(),
                        e.proposalCid(), root, actor, seatOf(e), null, e.rationale(), e.price(), 1,
                        e.resultCid(), details));
                webhooks.dispatch(WebhookDispatcher.FIXING_FINALIZED, e.instrument(), root, e.price(),
                        e.referencePrice(), e.wrapperFactor(), null);
            }
            default -> events.append(FixingEvent.of(e.kind(), e.instrument(), e.proposalCid(),
                    events.rootOf(e.proposalCid()), actor, seatOf(e), null, e.rationale(), e.price(),
                    null, e.resultCid(), details));
        }
    }

    /** A second proposal for the same identifier on the same day, before the first finalised. */
    boolean isRestrike(String instrument, String session) {
        StrikeSchedule s = schedules.byInstrument(instrument).orElse(null);
        java.time.ZoneId zone = s == null ? java.time.ZoneId.of("Europe/London") : s.zone();
        String today = Instant.now().atZone(zone).toLocalDate().toString();
        for (FixingEvent ev : events.query(instrument, null, null)) {
            boolean opened = FixingEvent.Kinds.PROPOSAL_CREATED.equals(ev.kind())
                    || FixingEvent.Kinds.PROPOSAL_RESTRUCK.equals(ev.kind());
            if (!opened) continue;
            if (!today.equals(ev.instant().atZone(zone).toLocalDate().toString())) continue;
            Object sess = ev.details() == null ? null : ev.details().get("session");
            if (sess != null && session != null && !session.equalsIgnoreCase(String.valueOf(sess))) continue;
            boolean finalised = events.byProposal(ev.rootCid()).stream()
                    .anyMatch(x -> FixingEvent.Kinds.FIXING_FINALIZED.equals(x.kind()));
            if (!finalised) return true;
        }
        return false;
    }

    Instant deadlineFor(String instrument) {
        int minutes = schedules.byInstrument(instrument).map(StrikeSchedule::getWindowMinutes)
                .orElse(StrikeSchedule.DEFAULT_WINDOW_MINUTES);
        return Instant.now().plus(Duration.ofMinutes(minutes));
    }

    private String instrumentOf(String rootCid) {
        List<FixingEvent> chain = events.byProposal(rootCid);
        return chain.stream().map(FixingEvent::instrument).filter(i -> i != null).findFirst().orElse(null);
    }

    private static String seatOf(LifecycleEvent e) {
        if (e.seat() != null) return e.seat();
        Optional<Principal> p = currentPrincipal();
        return p.map(Principal::seat).orElse(null);
    }

    /** "email (PartyLabel)" when a signed-in user did it; the party label otherwise. */
    static String actorName(String party) {
        String label = LedgerService.labelOf(party);
        Optional<Principal> p = currentPrincipal();
        if (p.isPresent() && p.get().email() != null) {
            return p.get().email() + (label == null ? "" : " (" + label + ")");
        }
        return label;
    }

    private static Optional<Principal> currentPrincipal() {
        try {
            RequestAttributes ra = RequestContextHolder.getRequestAttributes();
            if (ra == null) return Optional.empty();
            Object p = ra.getAttribute(Principal.ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
            return p instanceof Principal pr ? Optional.of(pr) : Optional.empty();
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }
}
