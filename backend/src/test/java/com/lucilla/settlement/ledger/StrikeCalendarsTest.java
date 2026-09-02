package com.lucilla.settlement.ledger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The strike calendars — docs/FIXING_METHODOLOGY.md §4. The load-bearing assertions are
 * the two the task named: a Saturday strikes for {@code daily} and not for
 * {@code weekdays}, and 3 July 2026 (Independence Day observed) does not strike for
 * {@code nyse} even though it is a Friday.
 */
class StrikeCalendarsTest {

    static final StrikeCalendars CAL = StrikeCalendars.defaults();
    static final LocalDate SATURDAY = LocalDate.of(2026, 9, 5);
    static final LocalDate JULY_3_2026 = LocalDate.of(2026, 7, 3);

    @Test
    @DisplayName("a Saturday strikes for daily, not for weekdays")
    void saturday() {
        assertThat(SATURDAY.getDayOfWeek()).isEqualTo(DayOfWeek.SATURDAY);
        assertThat(CAL.strikes(StrikeCalendars.DAILY, SATURDAY)).isTrue();
        assertThat(CAL.strikes(StrikeCalendars.WEEKDAYS, SATURDAY)).isFalse();
        assertThat(CAL.strikes(StrikeCalendars.NYSE, SATURDAY)).isFalse();
        assertThat(CAL.strikes(StrikeCalendars.LSE, SATURDAY)).isFalse();
    }

    @Test
    @DisplayName("4 July 2026 falls on a Saturday; the NYSE observes it on Friday 3 July, which does not strike")
    void independenceDayObserved() {
        assertThat(JULY_3_2026.getDayOfWeek()).isEqualTo(DayOfWeek.FRIDAY);
        assertThat(CAL.strikes(StrikeCalendars.NYSE, JULY_3_2026)).isFalse();
        // The same Friday is an ordinary day everywhere else.
        assertThat(CAL.strikes(StrikeCalendars.WEEKDAYS, JULY_3_2026)).isTrue();
        assertThat(CAL.strikes(StrikeCalendars.LSE, JULY_3_2026)).isTrue();
        assertThat(CAL.strikes(StrikeCalendars.DAILY, JULY_3_2026)).isTrue();
        // …and the NYSE is back on Monday.
        assertThat(CAL.nextStrikeDay(StrikeCalendars.NYSE, JULY_3_2026)).isEqualTo(LocalDate.of(2026, 7, 6));
    }

    @Test
    @DisplayName("the LSE keeps the UK substitute days; the NYSE does not")
    void boxingDaySubstitute() {
        LocalDate mon = LocalDate.of(2026, 12, 28);
        assertThat(mon.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(CAL.strikes(StrikeCalendars.LSE, mon)).isFalse();
        assertThat(CAL.strikes(StrikeCalendars.NYSE, mon)).isTrue();
        // 2027: Christmas Day observed Friday 24 Dec on the NYSE, Monday 27 Dec on the LSE.
        assertThat(CAL.strikes(StrikeCalendars.NYSE, LocalDate.of(2027, 12, 24))).isFalse();
        assertThat(CAL.strikes(StrikeCalendars.LSE, LocalDate.of(2027, 12, 24))).isTrue();
        assertThat(CAL.strikes(StrikeCalendars.LSE, LocalDate.of(2027, 12, 27))).isFalse();
    }

    @Test
    @DisplayName("both exchange files cover 2026 and 2027")
    void coverage() {
        assertThat(CAL.get(StrikeCalendars.NYSE).coverage()).containsExactly(2026, 2027);
        assertThat(CAL.get(StrikeCalendars.LSE).coverage()).containsExactly(2026, 2027);
        assertThat(CAL.get(StrikeCalendars.NYSE).holidays()).hasSize(20);
        assertThat(CAL.get(StrikeCalendars.LSE).holidays()).hasSize(16);
        assertThat(CAL.names()).containsExactlyInAnyOrder("daily", "weekdays", "nyse", "lse");
    }

    @Test
    @DisplayName("the intersection rule: a fund of a daily and an NYSE component strikes only on NYSE days")
    void intersection() {
        List<String> fund = List.of(StrikeCalendars.DAILY, StrikeCalendars.NYSE);
        assertThat(CAL.allStrike(fund, SATURDAY)).isFalse();
        assertThat(CAL.allStrike(fund, JULY_3_2026)).isFalse();
        assertThat(CAL.allStrike(fund, LocalDate.of(2026, 7, 6))).isTrue();
        assertThat(CAL.allStrike(List.of(StrikeCalendars.DAILY, StrikeCalendars.DAILY), SATURDAY)).isTrue();
    }

    @Test
    @DisplayName("an unknown calendar is treated as weekdays — the safe direction to be wrong in")
    void unknownIsWeekdays() {
        assertThat(CAL.has("tse")).isFalse();
        assertThat(CAL.strikes("tse", SATURDAY)).isFalse();
        assertThat(CAL.strikes("tse", JULY_3_2026)).isTrue();
        assertThat(CAL.strikes(null, JULY_3_2026)).isTrue();
        assertThat(CAL.describe("tse")).contains("unknown");
    }

    @Test
    @DisplayName("an override directory replaces a built-in file and can add a calendar")
    void overrideDirectory(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("nyse.yml"), """
                calendar: nyse
                name: NYSE (override)
                holidays:
                  2026-09-08: Test closure
                """);
        Files.writeString(dir.resolve("tse.yml"), """
                name: Tokyo Stock Exchange
                holidays:
                  - date: 2026-09-09
                    name: List form
                  - 2026-09-10
                """);
        StrikeCalendars c = StrikeCalendars.load(dir);
        assertThat(c.strikes(StrikeCalendars.NYSE, LocalDate.of(2026, 9, 8))).isFalse();
        assertThat(c.strikes(StrikeCalendars.NYSE, JULY_3_2026)).isTrue();   // the override replaced the whole file
        assertThat(c.has("tse")).isTrue();
        assertThat(c.strikes("tse", LocalDate.of(2026, 9, 9))).isFalse();
        assertThat(c.strikes("tse", LocalDate.of(2026, 9, 10))).isFalse();
        assertThat(c.strikes("tse", LocalDate.of(2026, 9, 11))).isTrue();
        assertThat(c.strikes("tse", SATURDAY)).isFalse();
        // The LSE file is untouched by an override that does not mention it.
        assertThat(c.strikes(StrikeCalendars.LSE, LocalDate.of(2026, 12, 28))).isFalse();
    }

    @Test
    @DisplayName("a calendar with weekends: true strikes on Saturday even with holidays listed")
    void weekendsFlag() {
        var c = StrikeCalendars.parse("crypto", new ByteArrayInputStream("""
                weekends: true
                holidays:
                  2026-09-05: Never mind
                """.getBytes(StandardCharsets.UTF_8)));
        assertThat(c.name()).isEqualTo("crypto");
        assertThat(c.strikes(SATURDAY)).isFalse();
        assertThat(c.strikes(SATURDAY.plusDays(1))).isTrue();
    }
}
