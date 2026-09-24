package com.lucilla.settlement.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link SettlementController#asOfOrToday}: the date a fixing claims to observe.
 *
 * <p>The ledger allows one fixing per instrument, session and as-of date, so a date in the
 * future is not a harmless label — it consumes a slot the real day cannot reuse, and the
 * value is published as that day's mark. On 24 Sep 2026 a script with a bad date expression
 * struck a real attested fixing dated 2039-02-03 on DevNet; it is permanent.
 */
class AsOfDateTest {

    static final ZoneId LONDON = ZoneId.of("Europe/London");

    @Test
    @DisplayName("absent means today; a past or present date is taken as given")
    void absentIsToday() {
        LocalDate today = LocalDate.now(LONDON);
        assertThat(SettlementController.asOfOrToday(null)).isEqualTo(today);
        assertThat(SettlementController.asOfOrToday("  ")).isEqualTo(today);
        assertThat(SettlementController.asOfOrToday(today.toString())).isEqualTo(today);
        assertThat(SettlementController.asOfOrToday("2026-09-22")).isEqualTo(LocalDate.of(2026, 9, 22));
    }

    @Test
    @DisplayName("one day of tolerance, for a strike either side of midnight")
    void tomorrowIsAllowed() {
        assertThat(SettlementController.asOfOrToday(LocalDate.now(LONDON).plusDays(1).toString()))
                .isEqualTo(LocalDate.now(LONDON).plusDays(1));
    }

    @Test
    @DisplayName("a date further ahead is refused, naming today")
    void futureIsRefused() {
        assertThatThrownBy(() -> SettlementController.asOfOrToday("2039-02-03"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2039-02-03")
                .hasMessageContaining("in the future");
        assertThatThrownBy(() -> SettlementController.asOfOrToday(LocalDate.now(LONDON).plusDays(2).toString()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("a malformed date is still refused as a parse error, not as a future date")
    void malformedIsRefused() {
        assertThatThrownBy(() -> SettlementController.asOfOrToday("24-09-2026"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ISO date");
    }
}
