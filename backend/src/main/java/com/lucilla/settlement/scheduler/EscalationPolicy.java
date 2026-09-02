package com.lucilla.settlement.scheduler;

import java.time.Duration;
import java.time.Instant;

/**
 * Tier 2 — escalation BEFORE fallback (docs/PRODUCT-PLAN.md §4), as a pure function of
 * the clock so the timing can be tested without a ledger.
 *
 * <pre>
 *   strike ──────────── ½ ──────────── ¾ ──────────── window end
 *                       │              │                   │
 *                escalation 1    escalation 2          tier 3/4/5
 *             remind every seat  remind again, and     (only now)
 *             not yet confirmed  bring in alternates
 * </pre>
 *
 * <p>A reminder is a {@code proposal.reminder} webhook and event carrying
 * {@code escalation: 1 | 2}. Nothing here changes the ledger, and nothing here can
 * produce a price: the point of tier 2 is to get a real signature inside the window so
 * that tiers 3–5 — which publish a number nobody attested — are never reached.
 */
public final class EscalationPolicy {

    private EscalationPolicy() {}

    /** The first escalation fires at this fraction of the window. */
    public static final double FIRST_AT = 0.5;
    /** The second at this fraction — alternates are brought in here. */
    public static final double SECOND_AT = 0.75;

    /**
     * Which escalation level is due at {@code now}: 0 before half the window, 1 from
     * half, 2 from three quarters. A zero-length window never escalates — there is no
     * "half" of nothing, and the fallback runs at the strike instead.
     */
    public static int levelDue(Instant strike, Instant windowEnd, Instant now) {
        long total = Duration.between(strike, windowEnd).toMillis();
        if (total <= 0 || now.isBefore(strike)) return 0;
        long elapsed = Duration.between(strike, now).toMillis();
        if (elapsed >= Math.round(total * SECOND_AT)) return 2;
        if (elapsed >= Math.round(total * FIRST_AT)) return 1;
        return 0;
    }

    /** The instant at which {@code level} (1 or 2) becomes due. */
    public static Instant dueAt(Instant strike, Instant windowEnd, int level) {
        long total = Duration.between(strike, windowEnd).toMillis();
        double fraction = level >= 2 ? SECOND_AT : FIRST_AT;
        return strike.plusMillis(Math.round(total * fraction));
    }
}
