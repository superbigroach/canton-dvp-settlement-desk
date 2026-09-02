package com.lucilla.settlement.ledger;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link FixingSchedule} — docs/FIXING_METHODOLOGY.md §4.
 *
 * <p>The class exists because strikes were triggered by hand, so the published record
 * depended on somebody remembering. These tests hold it to the one property that makes
 * it worth having: <b>a strike that did not happen must be visible</b>. The load-bearing
 * assertions are therefore the OVERDUE ones, and the one proving that yesterday's fixing
 * does not quietly satisfy today's schedule.
 */
class FixingScheduleTest {

    private static final ZoneId LONDON = ZoneId.of("Europe/London");

    /** 16:00 London, one hour of grace — the CME CF BRR window this desk references. */
    private static final FixingSchedule.Declared CBTC = new FixingSchedule.Declared(
            "CBTC", "Close", LocalTime.of(16, 0), LONDON, 60);

    /** An instant on a known WEDNESDAY, in London terms. */
    private static Instant wed(String hhmm) {
        return LocalDate.of(2026, 8, 26)
                .atTime(LocalTime.parse(hhmm)).atZone(LONDON).toInstant();
    }

    /** The same clock time on the preceding SUNDAY. */
    private static Instant sun(String hhmm) {
        return LocalDate.of(2026, 8, 23)
                .atTime(LocalTime.parse(hhmm)).atZone(LONDON).toInstant();
    }

    @Test
    void beforeTheStrikeTimeItIsSimplyPending() {
        var st = FixingSchedule.statusOf(CBTC, wed("09:00"), null);
        assertThat(st.state()).isEqualTo(FixingSchedule.State.PENDING);
        assertThat(st.minutesLate()).isZero();
        assertThat(st.expectedAt()).isEqualTo(wed("16:00"));
    }

    @Test
    void justAfterTheStrikeItIsDueRatherThanOverdue() {
        // Within grace. A committee signing round takes minutes, and flagging that as a
        // failure would cry wolf on the ordinary case.
        var st = FixingSchedule.statusOf(CBTC, wed("16:30"), null);
        assertThat(st.state()).isEqualTo(FixingSchedule.State.DUE);
        assertThat(st.minutesLate()).isEqualTo(30);
    }

    @Test
    void pastGraceWithNoFixingIsOverdue() {
        // THE POINT OF THE CLASS. This is the state that used to be invisible, and the
        // one §3 Tier 3 turns into a carried-forward flag for consumers.
        var st = FixingSchedule.statusOf(CBTC, wed("18:00"), null);
        assertThat(st.state()).isEqualTo(FixingSchedule.State.OVERDUE);
        assertThat(st.minutesLate()).isEqualTo(120);
        assertThat(st.note()).contains("Tier 3");
    }

    @Test
    void aFixingStruckAtTodaysScheduledInstantSettlesTheObligation() {
        var st = FixingSchedule.statusOf(CBTC, wed("18:00"), wed("16:00"));
        assertThat(st.state()).isEqualTo(FixingSchedule.State.STRUCK);
    }

    @Test
    void aFixingFinalisedLateStillSatisfiesTheStrikeItWasStruckAsOf() {
        // A mark struck as of 16:00 and confirmed at 16:07 meets the 16:00 schedule.
        // The caller passes accrualFrom precisely so the committee's own latency is not
        // recorded as a missed strike.
        var st = FixingSchedule.statusOf(CBTC, wed("18:00"), wed("16:05"));
        assertThat(st.state()).isEqualTo(FixingSchedule.State.STRUCK);
    }

    @Test
    void yesterdaysFixingDoesNotSatisfyTodaysSchedule() {
        // The failure a naive "within 24 hours" check would wave through, and the exact
        // shape of a benchmark quietly going stale while looking current.
        Instant yesterday = LocalDate.of(2026, 8, 25)
                .atTime(LocalTime.of(16, 0)).atZone(LONDON).toInstant();
        var st = FixingSchedule.statusOf(CBTC, wed("18:00"), yesterday);
        assertThat(st.state()).isEqualTo(FixingSchedule.State.OVERDUE);
    }

    @Test
    void weekendsExpectNothingAndPointAtTheNextBusinessDay() {
        var st = FixingSchedule.statusOf(CBTC, sun("18:00"), null);
        assertThat(st.state()).isEqualTo(FixingSchedule.State.NOT_DUE_TODAY);
        // Monday 24 August, not Sunday.
        assertThat(st.expectedAt()).isEqualTo(
                LocalDate.of(2026, 8, 24).atTime(LocalTime.of(16, 0)).atZone(LONDON).toInstant());
        // The weekday approximation is disclosed rather than hidden: a public holiday
        // will read as a missed strike, and that is the safe direction to be wrong in.
        assertThat(st.note()).contains("holiday calendar");
    }

    @Test
    void aDailyIdentifierIsDueOnSunday() {
        // The CME CF BRR prints every day of the year; a cBTC fixing that skipped the
        // weekend would publish a gap where the benchmark it references has a value.
        var daily = new FixingSchedule.Declared("CBTC", "Close", LocalTime.of(16, 0), LONDON, 60,
                StrikeCalendars.DAILY);
        var st = FixingSchedule.statusOf(daily, sun("18:00"), null);
        assertThat(st.state()).isEqualTo(FixingSchedule.State.OVERDUE);
        assertThat(st.expectedAt()).isEqualTo(sun("16:00"));
        assertThat(FixingSchedule.statusOf(daily, sun("09:00"), null).state())
                .isEqualTo(FixingSchedule.State.PENDING);
    }

    @Test
    void anNyseIdentifierIsNotDueOnAnExchangeHoliday() {
        // Friday 3 July 2026 — Independence Day observed. Under the weekday rule this read
        // as a missed strike; under the exchange calendar there is no close to strike.
        var nyse = new FixingSchedule.Declared("SPY", "Close", LocalTime.of(16, 0),
                ZoneId.of("America/New_York"), 60, StrikeCalendars.NYSE);
        Instant fri = LocalDate.of(2026, 7, 3).atTime(LocalTime.of(18, 0))
                .atZone(ZoneId.of("America/New_York")).toInstant();
        var st = FixingSchedule.statusOf(nyse, fri, null);
        assertThat(st.state()).isEqualTo(FixingSchedule.State.NOT_DUE_TODAY);
        assertThat(st.note()).contains("Independence Day").contains("nyse");
        // Monday 6 July, not Saturday.
        assertThat(st.expectedAt()).isEqualTo(LocalDate.of(2026, 7, 6).atTime(LocalTime.of(16, 0))
                .atZone(ZoneId.of("America/New_York")).toInstant());
    }

    @Test
    void theDefaultsAreDaily() {
        for (FixingSchedule.Declared d : FixingSchedule.defaults()) {
            assertThat(d.calendar()).as("%s calendar", d.instrumentId()).isEqualTo(StrikeCalendars.DAILY);
        }
    }

    @Test
    void theDeclaredRosterIsWellFormed() {
        // §9 makes a strike time a material term, so these live in a reviewed artefact.
        assertThat(FixingSchedule.defaults()).isNotEmpty();
        for (FixingSchedule.Declared d : FixingSchedule.defaults()) {
            assertThat(d.instrumentId()).isNotBlank();
            assertThat(d.session()).isIn("Open", "Close");
            assertThat(d.zone()).isNotNull();
            assertThat(d.graceMinutes()).isPositive();
        }
    }

    @Test
    void identifierKeysAreCaseAndWhitespaceInsensitive() {
        assertThat(FixingSchedule.key(" CBTC ", "Close"))
                .isEqualTo(FixingSchedule.key("cbtc", "close"));
    }
}
