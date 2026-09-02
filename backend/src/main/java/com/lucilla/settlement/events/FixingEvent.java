package com.lucilla.settlement.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One row of {@code fixing_events} — docs/PRODUCT-PLAN.md §4: "Every step writes an
 * event (instrument, proposalCid, kind, actor, reason, ts, on-ledger cid where
 * applicable). The audit export is those events."
 *
 * <p>{@code rootCid} is the FIRST contract id the proposal had. Every confirmation
 * archives and re-creates a {@code FixingProposal}, so one proposal wears several cids
 * over its life; the root is what lets {@code /api/proposals/{cid}/events} return the
 * whole conversation whichever cid the caller holds.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record FixingEvent(
        long id,
        String ts,
        String kind,            // proposal.created | proposal.confirmed | proposal.refused | …
        String instrument,
        String proposalCid,     // the cid at the time of the event
        String rootCid,         // the proposal's first cid
        String actor,           // e-mail / uid / party label of whoever did it
        String seat,            // issuer | lender | venue | operator | null
        String condition,       // the protocol condition a refusal names
        String reason,
        BigDecimal price,
        Integer tier,
        String ledgerCid,       // the on-ledger contract this step produced, if any
        Map<String, Object> details) {

    public static final class Kinds {
        public static final String PROPOSAL_CREATED = "proposal.created";
        public static final String PROPOSAL_RESTRUCK = "proposal.restruck";
        public static final String PROPOSAL_CONFIRMED = "proposal.confirmed";
        public static final String PROPOSAL_REFUSED = "proposal.refused";
        public static final String PROPOSAL_WITHDRAWN = "proposal.withdrawn";
        public static final String FIXING_FINALIZED = "fixing.finalized";
        public static final String FIXING_FALLBACK = "fixing.fallback";   // tier 3 / 4, a series row
        public static final String FIXING_MISSED = "fixing.missed";       // tier 5, a gap
        public static final String STRIKE_SCHEDULED = "strike.scheduled";
        public static final String STRIKE_FAILED = "strike.failed";
        public static final String WEBHOOK_SENT = "webhook.sent";
        public static final String WEBHOOK_FAILED = "webhook.failed";
        public static final String CREATION = "fund.creation";
        public static final String REDEMPTION = "fund.redemption";

        private Kinds() {}
    }

    /** A builder-ish factory so call sites stay one line. */
    public static FixingEvent of(String kind, String instrument, String proposalCid, String rootCid,
            String actor, String seat, String condition, String reason, BigDecimal price, Integer tier,
            String ledgerCid, Map<String, Object> details) {
        return new FixingEvent(0, Instant.now().toString(), kind, instrument, proposalCid,
                rootCid == null ? proposalCid : rootCid, actor, seat, condition, reason, price, tier,
                ledgerCid, details == null ? new LinkedHashMap<>() : details);
    }

    public FixingEvent withId(long newId) {
        return new FixingEvent(newId, ts, kind, instrument, proposalCid, rootCid, actor, seat,
                condition, reason, price, tier, ledgerCid, details);
    }

    public Instant instant() {
        return Instant.parse(ts);
    }

    /** frontend/src/desk/types.ts: the contract this step produced, else the one it acted on. */
    @JsonProperty("cid")
    public String cid() {
        return ledgerCid != null ? ledgerCid : proposalCid;
    }

    /** A one-line human summary for a message log. */
    @JsonProperty("detail")
    public String detail() {
        StringBuilder sb = new StringBuilder();
        if (seat != null) sb.append(seat).append(": ");
        if (condition != null) sb.append(condition).append(" — ");
        if (reason != null) sb.append(reason);
        if (price != null) sb.append(sb.length() > 0 ? " @ " : "@ ").append(price.stripTrailingZeros().toPlainString());
        if (tier != null) sb.append(" (tier ").append(tier).append(')');
        return sb.toString();
    }
}
