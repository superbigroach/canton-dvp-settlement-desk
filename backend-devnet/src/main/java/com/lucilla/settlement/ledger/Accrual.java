package com.lucilla.settlement.ledger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Optional;

/**
 * A LINE-FOR-LINE JAVA MIRROR OF {@code Governance.daml}'s accrual arithmetic.
 *
 * <p><b>Why this class exists at all.</b> The ledger is the authority: {@code navAt}
 * is a pure Daml function re-executed by every validator, and the number it produces
 * is the number the fund is worth. This class does not decide anything — it
 * REPRODUCES that computation off-ledger so the desk can answer "what is it worth
 * right now?" without a round trip per tick, and so the web UI can be handed the four
 * attested inputs and derive the same value locally. A desk that showed a number the
 * ledger would not agree with is worse than a desk that shows nothing, so everything
 * below is a deliberate transcription rather than an independent implementation.
 *
 * <p><b>THE TWO THINGS THAT MUST MATCH EXACTLY, AND WHY.</b>
 * <ol>
 *   <li><b>The order of operations.</b> Daml {@code Decimal} is {@code Numeric 10}:
 *       multiplication and division ROUND TO 10 DECIMAL PLACES. So
 *       {@code base * (rate * micros) / yearMicros} and the "obvious"
 *       {@code base * (rate / yearMicros) * micros} are DIFFERENT NUMBERS — the second
 *       computes a per-microsecond rate of order 1e-9, quantises it at 1e-10, and bakes
 *       a ~1.09% RELATIVE error into every tick forever. Governance.daml multiplies
 *       first and divides last for exactly that reason; this class does the same, in
 *       the same sequence, with the same intermediate roundings.</li>
 *   <li><b>The rounding mode.</b> Daml-LF's {@code MUL_NUMERIC}/{@code DIV_NUMERIC}
 *       round HALF-EVEN (banker's rounding) at the target scale, so every intermediate
 *       here is {@code setScale(10, HALF_EVEN)}. Half-even only differs from half-up on
 *       an exact tie at the 11th decimal place, but "only on ties" is not a reason to
 *       guess.</li>
 * </ol>
 *
 * <p><b>Verified against the ledger's own test vectors.</b> {@code Test.daml}'s
 * {@code testAccrualArithmeticUnit} pins concrete values ({@code 100.0} at 3.6% ACT/360
 * is {@code 100.01} after a day, {@code 100.0004166667} after an hour, and ACT/365F is
 * {@code 100.0098630137} after a day). {@code AccrualTest} asserts this class produces
 * those same Decimals, digit for digit — see that file for the full vector list. The
 * TypeScript mirror in {@code frontend/src/accrual.ts} asserts the same vectors again.
 *
 * <p>Everything here is total and pure: no ledger, no Spring, no clock of its own.
 */
public final class Accrual {

    private Accrual() {
    }

    /** Daml {@code Decimal} is {@code Numeric 10} — ten decimal places, always. */
    public static final int SCALE = 10;

    /** Daml-LF rounds multiplication and division HALF-EVEN at the target scale. */
    public static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    /** {@code Governance.microsPerDay}. Daml {@code Time} is microsecond-resolution. */
    public static final long MICROS_PER_DAY = 86_400_000_000L;

    /** {@code Governance.anchorAccrualToleranceBps} — the auction anchor staleness budget. */
    public static final long ANCHOR_TOLERANCE_BPS = 1L;

    /** {@code Governance.anchorRoundingSlack} — ten units in the last place. */
    public static final BigDecimal ANCHOR_ROUNDING_SLACK = new BigDecimal("0.0000000010");

    /** The explicit NON-accruing convention the snapshot path writes. */
    public static final String DAY_COUNT_NONE = "NONE";

    /** The conventions this venue accrues on, in the order a picker should offer them. */
    public static final java.util.List<String> ACCRUING_DAY_COUNTS =
            java.util.List.of("ACT/360", "ACT/365F");

    // -----------------------------------------------------------------------
    // Day count
    // -----------------------------------------------------------------------

    /**
     * {@code Governance.dayCountYearMicros} — the length of the convention's YEAR in
     * microseconds, or empty for a convention this venue does not accrue on.
     *
     * <p>ACT/360 is the USD money-market convention (SOFR, repo, T-bills); ACT/365F is
     * the GBP/AUD/NZD/HKD/SGD one. 30/360 and ACT/ACT are deliberately absent — the
     * ledger REJECTS them at proposal rather than defaulting, and so does this desk.
     */
    public static Optional<Long> dayCountYearMicros(String dayCount) {
        if (dayCount == null) {
            return Optional.empty();
        }
        return switch (dayCount) {
            case "ACT/360" -> Optional.of(360L * MICROS_PER_DAY);
            case "ACT/365F" -> Optional.of(365L * MICROS_PER_DAY);
            case DAY_COUNT_NONE -> Optional.of(360L * MICROS_PER_DAY); // rate is 0; length cannot matter
            default -> Optional.empty();
        };
    }

    /** {@code Governance.supportedDayCount}. */
    public static boolean supportedDayCount(String dayCount) {
        return dayCountYearMicros(dayCount).isPresent();
    }

    /**
     * Validate a proposed accrual recipe THE WAY THE LEDGER VALIDATES IT, before any
     * command is submitted.
     *
     * <p>This is a mirror of the {@code ProposeAccruingFixing} asserts plus the
     * {@code FixingProposal} {@code ensure}, and it exists so an operator typing
     * "ACT/ACT" gets a 400 with the reason rather than a ledger abort surfaced as a
     * 422 — the ledger would refuse it either way, but a rejected COMMAND is a worse
     * error message than a rejected FORM.
     *
     * @throws IllegalArgumentException (rendered as HTTP 400) with the ledger's reason
     */
    public static void validateRecipe(BigDecimal price, BigDecimal ratePerAnnum, String dayCount) {
        if (price == null || price.signum() <= 0) {
            throw new IllegalArgumentException("base NAV must be positive");
        }
        if (ratePerAnnum == null) {
            throw new IllegalArgumentException("ratePerAnnum is required (0 for a non-accruing snapshot)");
        }
        if (!supportedDayCount(dayCount)) {
            throw new IllegalArgumentException(
                    "unsupported day-count convention '" + dayCount
                            + "' — this venue accrues on ACT/360 (USD money market) or ACT/365F"
                            + " (GBP/AUD/NZD/HKD/SGD). 30/360 assumes 30-day months and is meaningless"
                            + " for a per-second accrual; ACT/ACT needs leap-year period boundaries."
                            + " The ledger rejects an unknown convention rather than defaulting one,"
                            + " because silently falling back to ACT/360 would mis-accrue by 1.389%"
                            + " of the yield, forever, under a signed attestation saying otherwise.");
        }
        // "A rate at or below -100%/yr is not a money market, it is a typo."
        if (ratePerAnnum.compareTo(BigDecimal.ONE.negate()) <= 0) {
            throw new IllegalArgumentException("accrual rate must be greater than -100% per annum");
        }
        // FixingProposal.ensure: "NONE" can never sit next to a live rate.
        if (DAY_COUNT_NONE.equals(dayCount) && ratePerAnnum.signum() != 0) {
            throw new IllegalArgumentException(
                    "day count 'NONE' is the non-accruing convention and cannot carry a rate — "
                            + "attest ACT/360 or ACT/365F to accrue");
        }
    }

    // -----------------------------------------------------------------------
    // The arithmetic
    // -----------------------------------------------------------------------

    /** Epoch microseconds of an {@link Instant} — Daml {@code Time}'s own resolution. */
    public static long epochMicros(Instant t) {
        return Math.multiplyExact(t.getEpochSecond(), 1_000_000L) + (t.getNano() / 1_000L);
    }

    /**
     * {@code Governance.elapsedMicrosFrom} — how long a fixing has been accruing,
     * CLAMPED AT ZERO. Asking about an instant before the mark applies from is a legal
     * question (a backfill, a replay); the honest answer is "nothing had accrued by
     * then", never a negative accrual that would mark the fund below its attested base.
     */
    public static long elapsedMicrosFrom(Instant from, Instant at) {
        long micros = epochMicros(at) - epochMicros(from);
        return micros <= 0 ? 0L : micros;
    }

    /**
     * {@code Governance.accruedAmount} — {@code base * (rate * elapsed) / yearMicros}.
     *
     * <p>THE ORDER IS THE POINT. {@code rate * elapsedMicros} is exact (a 10dp value
     * times a whole number of microseconds has at most 10dp); multiplying by the base
     * is the only intermediate rounding, bounded by half a unit in the last place; and
     * dividing by the year length (3.11e13) shrinks that inherited error by thirteen
     * orders of magnitude before the single final rounding. One evaluation is within
     * ~5e-11 of the exact real answer — a bound, not an estimate.
     */
    public static BigDecimal accruedAmount(
            BigDecimal basePrice, BigDecimal ratePerAnnum, String dayCount, long elapsedMicros) {
        Optional<Long> yearMicros = dayCountYearMicros(dayCount);
        if (yearMicros.isEmpty() || elapsedMicros <= 0) {
            // An unrecognised convention accrues NOTHING rather than guessing a year
            // length — same total-function stance as the Daml.
            return dec(0);
        }
        BigDecimal rateTimesMicros = mul(ratePerAnnum, BigDecimal.valueOf(elapsedMicros));
        BigDecimal numerator = mul(basePrice, rateTimesMicros);
        return div(numerator, BigDecimal.valueOf(yearMicros.get()));
    }

    /**
     * {@code Governance.navAt} — what ONE SHARE is worth at an instant.
     *
     * <p>Floored at zero for the same real reason the Daml floors it: EUR/CHF/JPY money
     * markets printed NEGATIVE rates from 2015 to 2022, so a downward accrual exists,
     * and a fixing whose accrual would drive the NAV through zero must report zero
     * rather than hand a negative "price" to a template that demands a positive one.
     */
    public static BigDecimal navAt(
            BigDecimal basePrice, BigDecimal ratePerAnnum, String dayCount,
            Instant accrualFrom, Instant at) {
        BigDecimal accrued = accruedAmount(
                basePrice, ratePerAnnum, dayCount, elapsedMicrosFrom(accrualFrom, at));
        BigDecimal nav = scaled(basePrice).add(accrued);   // addition is EXACT in Numeric 10
        return nav.signum() < 0 ? dec(0) : nav;
    }

    /**
     * {@code Governance.anchorConsistentWithNav} — may an auction anchored here run
     * against a fixing that says the NAV is {@code accruedNav} right now?
     *
     * <p>ASYMMETRIC ON PURPOSE. Below the accrual is STALENESS, forgivable up to one
     * basis point (at a 3.6%/yr ACT/360 rate a fund accrues exactly 1bp per day, so the
     * band says "the anchor may be up to a day old"). Above it is not staleness at all —
     * it is a venue pricing value the fund has not earned yet, which no elapsed time
     * explains — so that side is a hard edge with only rounding slack behind it.
     */
    public static boolean anchorConsistentWithNav(BigDecimal accruedNav, BigDecimal anchor) {
        BigDecimal budget = staleBudget(accruedNav);
        return anchor.compareTo(accruedNav.add(ANCHOR_ROUNDING_SLACK)) <= 0
                && accruedNav.subtract(anchor).compareTo(budget) <= 0;
    }

    /** The 1bp staleness budget, in price units, for a given accrued NAV. */
    public static BigDecimal staleBudget(BigDecimal accruedNav) {
        return div(mul(accruedNav, BigDecimal.valueOf(ANCHOR_TOLERANCE_BPS)), dec(10000));
    }

    // -----------------------------------------------------------------------
    // Numeric 10 primitives — every op rounds exactly where Daml-LF rounds.
    // -----------------------------------------------------------------------

    /** {@code MUL_NUMERIC}: multiply, then round to 10dp half-even. */
    public static BigDecimal mul(BigDecimal a, BigDecimal b) {
        return a.multiply(b).setScale(SCALE, ROUNDING);
    }

    /** {@code DIV_NUMERIC}: divide directly INTO 10dp half-even (never a two-step round). */
    public static BigDecimal div(BigDecimal a, BigDecimal b) {
        return a.divide(b, SCALE, ROUNDING);
    }

    /** Widen a value to the ledger's fixed scale without changing it. */
    public static BigDecimal scaled(BigDecimal v) {
        return v.setScale(SCALE, ROUNDING);
    }

    private static BigDecimal dec(long v) {
        return BigDecimal.valueOf(v).setScale(SCALE, ROUNDING);
    }

    /**
     * The wire form of a Decimal: a PLAIN STRING at the ledger's own 10dp scale.
     *
     * <p>Not a JSON number, and that is deliberate. A 10dp fixed-point value handed to
     * JavaScript as a number becomes a float64 and stops being the ledger's value; the
     * browser mirror re-parses these strings into exact fixed-point integers instead,
     * which is the only way the ticking figure on screen can be the SAME number the
     * ledger would compute rather than a close one.
     */
    public static String wire(BigDecimal v) {
        return scaled(v).toPlainString();
    }
}
