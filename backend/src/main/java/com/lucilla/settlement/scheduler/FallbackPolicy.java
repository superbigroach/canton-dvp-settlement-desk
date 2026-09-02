package com.lucilla.settlement.scheduler;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The fallback waterfall when K is not reached by the end of the window —
 * docs/PRODUCT-PLAN.md §4, as a pure function so the tiers can be tested without a
 * ledger or a clock.
 *
 * <pre>
 *   tier 2  escalation inside the window  — reminders at ½, alternates at ¾; see
 *                                           {@link EscalationPolicy}. Runs BEFORE this
 *                                           decision, never produces a price
 *   tier 3  benchmark × last factor       — automatic, wrapped assets only, needs both inputs
 *   tier 4  prior fixing, flagged         — needs a prior published value
 *   tier 5  missed                        — a gap is published as a gap
 * </pre>
 */
public final class FallbackPolicy {

    private FallbackPolicy() {}

    /** What {@code tier2} reads in a status or fallback record when it is switched on. */
    public static final String TIER2_STATUS = "escalation-before-fallback";
    /** …and when it is switched off for the instrument. */
    public static final String TIER2_DISABLED = "disabled";

    /** What the runner should publish. {@code price} is null for tier 5. */
    public record Decision(int tier, BigDecimal price, String note, boolean tier2Requested) {
    }

    /** The inputs available at window end. Any of them may be null. */
    public record Inputs(
            boolean wrapped,
            BigDecimal benchmarkPrint,     // the live benchmark, if the feed answered
            BigDecimal lastFactor,         // the most recent ATTESTED wrapper factor
            BigDecimal priorPrice,         // the most recent published value (any tier ≤ 4)
            String priorRef) {             // where it came from, for the note
    }

    public static Decision decide(StrikeSchedule schedule, Inputs in) {
        boolean tier2 = schedule.tierEnabled(2);
        if (schedule.tierEnabled(3) && in.wrapped() && in.benchmarkPrint() != null
                && in.lastFactor() != null && in.benchmarkPrint().signum() > 0
                && in.lastFactor().signum() > 0) {
            BigDecimal price = in.benchmarkPrint().multiply(in.lastFactor())
                    .setScale(10, RoundingMode.HALF_EVEN).stripTrailingZeros();
            return new Decision(3, price,
                    "tier 3: benchmark print " + plain(in.benchmarkPrint()) + " × last attested factor "
                            + plain(in.lastFactor()) + " (automatic; not attested by the committee)"
                            + tier2Note(tier2), tier2);
        }
        if (schedule.tierEnabled(4) && in.priorPrice() != null && in.priorPrice().signum() > 0) {
            return new Decision(4, in.priorPrice(),
                    "tier 4: prior fixing carried forward, flagged (" + (in.priorRef() == null ? "prior"
                            : in.priorRef()) + ")" + tier2Note(tier2), tier2);
        }
        return new Decision(5, null,
                "tier 5: missed — K not reached and no fallback input; a gap is published as a gap"
                        + tier2Note(tier2), tier2);
    }

    private static String tier2Note(boolean enabled) {
        return enabled ? "; tier 2 escalation ran inside the window (" + TIER2_STATUS + ")"
                : "; tier 2 escalation " + TIER2_DISABLED + " for this instrument";
    }

    private static String plain(BigDecimal d) {
        return d.stripTrailingZeros().toPlainString();
    }
}
