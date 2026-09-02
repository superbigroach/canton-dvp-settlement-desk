package com.lucilla.settlement.events;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * What the existing operator-desk controller PUBLISHES when a lifecycle step lands on
 * the ledger, so the event log and the webhooks hear about it without the controller
 * depending on either.
 *
 * <p>A Spring application event rather than a direct call, deliberately: the
 * {@code @WebMvcTest} slices that already cover {@code SettlementController} construct it
 * with only the ledger mocked, and a new constructor dependency would break every one of
 * them. {@code ApplicationEventPublisher} is always in the context.
 */
public record LifecycleEvent(
        String kind,               // a FixingEvent.Kinds constant
        String instrument,
        String proposalCid,        // the cid acted on (null for a fresh proposal)
        String resultCid,          // the cid produced (a successor proposal, or the NavFixing)
        String actorParty,         // the party that submitted
        String seat,
        BigDecimal price,
        BigDecimal referencePrice,
        BigDecimal wrapperFactor,
        String rationale,
        List<String> checks,
        Map<String, Object> extra) {
}
