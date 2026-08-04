package com.lucilla.settlement.ledger;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * THE AGREEMENT TEST: does the Java mirror produce the LEDGER'S digits?
 *
 * <p>Every headline vector below is lifted from {@code daml/Test.daml}, which is green
 * (53 scripts pass) and pins these exact Decimals with {@code ===}:
 * {@code testAccrualArithmeticUnit} fixes the ACT/360 and ACT/365F values for 100.0 at
 * 3.6%, {@code testCommitteeAttestedClose} fixes 2,400 → 2,402.40 over ten days, and
 * {@code testAccrualBackwardsTimeIsSafe} fixes the clamp. Asserting them here is what
 * makes "the desk agrees with the ledger" a checked fact rather than a claim — if
 * Governance.daml's arithmetic ever changes, this file goes red before a demo does.
 *
 * <p>The same vectors are asserted a third time in {@code frontend/src/accrual.ts}'s
 * self-check, so ledger → backend → browser is one chain of pinned numbers.
 */
class AccrualTest {

    private static final Instant T0 = Instant.parse("2026-08-05T14:00:00Z");
    private static final BigDecimal BASE = new BigDecimal("100.0");
    private static final BigDecimal RATE = new BigDecimal("0.036");   // 3.6%/yr

    private static BigDecimal nav360(Duration elapsed) {
        return Accrual.navAt(BASE, RATE, "ACT/360", T0, T0.plus(elapsed));
    }

    private static void isExactly(BigDecimal actual, String expected) {
        // compareTo, not equals: 100.0100000000 and 100.01 are the SAME Decimal at
        // different trailing-zero widths, and the ledger's value is the number.
        assertThat(actual).isEqualByComparingTo(new BigDecimal(expected));
    }

    // ---- Test.daml : testAccrualArithmeticUnit -----------------------------

    @Test
    void act360_matchesTheLedgersPinnedVectors() {
        // "3.6% ACT/360 accrues EXACTLY 1bp per day" — the whole reason 1bp is the
        // auction's staleness budget.
        isExactly(nav360(Duration.ofDays(1)), "100.01");
        isExactly(nav360(Duration.ofDays(10)), "100.10");
        isExactly(nav360(Duration.ofHours(12)), "100.005");        // half a day, half a bp
        isExactly(nav360(Duration.ofHours(1)), "100.0004166667");  // 0.01/24, rounded at 10dp
    }

    @Test
    void act365F_isADifferentNumberOnTheSameRate() {
        BigDecimal v = Accrual.navAt(BASE, RATE, "ACT/365F", T0, T0.plus(Duration.ofDays(1)));
        isExactly(v, "100.0098630137");
        // The 1.389% convention gap, made visible: same rate, same day, different value.
        assertThat(v).isLessThan(nav360(Duration.ofDays(1)));
    }

    @Test
    void snapshotAccruesNothingEver() {
        // The ProposeFixing path: rate 0.0, day count "NONE". Ten years later it is
        // still exactly the struck price — byte-for-byte the old behaviour.
        BigDecimal v = Accrual.navAt(BASE, BigDecimal.ZERO, Accrual.DAY_COUNT_NONE,
                T0, T0.plus(Duration.ofDays(3650)));
        isExactly(v, "100.0");
    }

    @Test
    void atTheStrikeInstantNothingHasAccrued() {
        isExactly(Accrual.navAt(BASE, RATE, "ACT/360", T0, T0), "100.0");
    }

    @Test
    void accrualIsMonotoneInElapsedTime() {
        BigDecimal s = nav360(Duration.ofSeconds(1));
        BigDecimal h = nav360(Duration.ofHours(1));
        BigDecimal d = nav360(Duration.ofDays(1));
        assertThat(s).isLessThan(h);
        assertThat(h).isLessThan(d);
    }

    // ---- Test.daml : testAccrualBackwardsTimeIsSafe ------------------------

    @Test
    void askingBeforeTheStrikeClampsToTheBaseRatherThanMarkingDown() {
        // A backfill or a replay may legitimately ask about an instant before the mark
        // applies from. The honest answer is "nothing had accrued by then" — never a
        // negative accrual that silently marks the fund below its attested base.
        isExactly(nav360(Duration.ofDays(-5)), "100.0");
        assertThat(Accrual.elapsedMicrosFrom(T0, T0.minus(Duration.ofDays(5)))).isZero();
    }

    // ---- Test.daml : testCommitteeAttestedClose ----------------------------

    @Test
    void theAuctionVectorAgrees() {
        BigDecimal base = new BigDecimal("2400.0");
        BigDecimal navAtClose = Accrual.navAt(base, RATE, "ACT/360", T0, T0.plus(Duration.ofDays(10)));
        isExactly(navAtClose, "2402.40");   // 2,400 + 10 x 0.24 — derived, not attested
    }

    @Test
    void anchorConsistency_isAsymmetricAndOneBasisPointWide() {
        BigDecimal base = new BigDecimal("2400.0");
        BigDecimal anchor = new BigDecimal("2400.0");

        // One day behind at 3.6% ACT/360 is EXACTLY the 1bp budget — accepted.
        BigDecimal oneDay = Accrual.navAt(base, RATE, "ACT/360", T0, T0.plus(Duration.ofDays(1)));
        assertThat(Accrual.anchorConsistentWithNav(oneDay, anchor)).isTrue();

        // Two days behind is beyond it — rejected.
        BigDecimal twoDays = Accrual.navAt(base, RATE, "ACT/360", T0, T0.plus(Duration.ofDays(2)));
        assertThat(Accrual.anchorConsistentWithNav(twoDays, anchor)).isFalse();

        // An anchor AHEAD of the accrual is rejected however small the excess: that is
        // not staleness, it is a venue pricing value the fund has not earned.
        assertThat(Accrual.anchorConsistentWithNav(new BigDecimal("2400.0"), new BigDecimal("2400.01")))
                .isFalse();
    }

    // ---- The precision trap this design exists to avoid --------------------

    @Test
    void dividingBeforeMultiplyingWouldBakeInMoreThanOnePercentOfError() {
        // THE WRONG WAY, measured rather than asserted: compute a per-microsecond rate
        // first (0.036 / 31,104,000,000,000 at 10dp is 0.0000000000 — it VANISHES), then
        // multiply. Governance.daml multiplies first and divides last precisely so this
        // cannot happen, and this test is here so nobody "simplifies" it back.
        long yearMicros = 360L * Accrual.MICROS_PER_DAY;
        long elapsed = Duration.ofDays(1).toNanos() / 1_000L;

        BigDecimal right = Accrual.accruedAmount(BASE, RATE, "ACT/360", elapsed);
        BigDecimal perMicro = Accrual.div(RATE, BigDecimal.valueOf(yearMicros));
        BigDecimal wrong = Accrual.mul(Accrual.mul(BASE, perMicro), BigDecimal.valueOf(elapsed));

        isExactly(right, "0.01");
        // At 3.6% the per-microsecond rate underflows 10dp entirely: the fund earns
        // NOTHING under the wrong order. (At 4% it survives but carries ~1.09% error.)
        assertThat(wrong).isNotEqualByComparingTo(right);

        // And the 4% case the Daml header calls out by name: > 1% relative error.
        BigDecimal r4 = new BigDecimal("0.04");
        BigDecimal right4 = Accrual.accruedAmount(BASE, r4, "ACT/360", elapsed);
        BigDecimal perSec = Accrual.div(r4, BigDecimal.valueOf(yearMicros / 1_000_000L));
        BigDecimal wrong4 = Accrual.mul(Accrual.mul(BASE, perSec),
                BigDecimal.valueOf(elapsed / 1_000_000L));
        BigDecimal relErr = right4.subtract(wrong4).abs()
                .divide(right4, 12, RoundingMode.HALF_EVEN);
        assertThat(relErr).isGreaterThan(new BigDecimal("0.01"));
    }

    // ---- Day-count validation: reject, never default -----------------------

    @Test
    void supportedConventionsAreExactlyTheTwoTheLedgerAccrualsOn() {
        assertThat(Accrual.supportedDayCount("ACT/360")).isTrue();
        assertThat(Accrual.supportedDayCount("ACT/365F")).isTrue();
        assertThat(Accrual.supportedDayCount("NONE")).isTrue();       // the non-accruing marker
        assertThat(Accrual.supportedDayCount("30/360")).isFalse();
        assertThat(Accrual.supportedDayCount("ACT/ACT")).isFalse();
        assertThat(Accrual.supportedDayCount("act/360")).isFalse();   // normalised by the web layer
        assertThat(Accrual.supportedDayCount(null)).isFalse();
    }

    @Test
    void anUnsupportedConventionIsRefused_notQuietlyDefaultedToAct360() {
        assertThatThrownBy(() -> Accrual.validateRecipe(BASE, RATE, "30/360"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported day-count convention");
        assertThatThrownBy(() -> Accrual.validateRecipe(BASE, RATE, "ACT/ACT"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ACT/360");
        // ...and it really does not accrue on one, either.
        isExactly(Accrual.accruedAmount(BASE, RATE, "ACT/ACT", Accrual.MICROS_PER_DAY), "0");
    }

    @Test
    void aRateAtOrBelowMinus100PercentIsATypo() {
        assertThatThrownBy(() -> Accrual.validateRecipe(BASE, new BigDecimal("-1.0"), "ACT/360"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("-100%");
        // Merely negative is FINE — EUR/CHF/JPY money markets printed negative rates
        // from 2015 to 2022, so a fund that accrues downward has actually existed.
        Accrual.validateRecipe(BASE, new BigDecimal("-0.005"), "ACT/360");
    }

    @Test
    void noneCannotSitNextToALiveRate() {
        // FixingProposal's own `ensure`, mirrored: a non-accruing mark must be
        // non-accruing on BOTH fields, so "NONE" can never quietly accrue on an
        // assumed convention.
        assertThatThrownBy(() -> Accrual.validateRecipe(BASE, RATE, "NONE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-accruing");
        Accrual.validateRecipe(BASE, BigDecimal.ZERO, "NONE");
    }

    @Test
    void negativeRateFloorsAtZeroRatherThanHandingBackANegativePrice() {
        // A fixing whose accrual would drive the NAV through zero reports zero; an
        // auction cannot be anchored on it at all, because ClosingAuction's `ensure`
        // demands a positive reference price. Loud at the ledger, not quiet here.
        BigDecimal v = Accrual.navAt(new BigDecimal("1.0"), new BigDecimal("-0.5"), "ACT/360",
                T0, T0.plus(Duration.ofDays(3650)));
        isExactly(v, "0");
    }
}
