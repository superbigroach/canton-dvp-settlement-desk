package com.lucilla.settlement.scheduler;

import com.lucilla.settlement.ledger.StrikeCalendars;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The waterfall of docs/PRODUCT-PLAN.md §4, tier by tier. */
class FallbackPolicyTest {

    static StrikeSchedule wrapped() {
        StrikeSchedule s = new StrikeSchedule();
        s.setInstrumentId("CBTC");
        return s;
    }

    static StrikeSchedule fund() {
        StrikeSchedule s = new StrikeSchedule();
        s.setInstrumentId("LX1");
        s.setKind("fund");
        return s;
    }

    @Test
    @DisplayName("tier 3: benchmark × last attested factor, when both exist")
    void tier3() {
        var d = FallbackPolicy.decide(wrapped(), new FallbackPolicy.Inputs(true,
                new BigDecimal("64870.5"), new BigDecimal("0.998"), new BigDecimal("65000"), "yesterday"));
        assertEquals(3, d.tier());
        assertEquals(new BigDecimal("64740.759"), d.price());
        assertTrue(d.note().contains("not attested"));
    }

    @Test
    @DisplayName("tier 4: no attested factor yet → the prior fixing, flagged")
    void tier4WhenNoFactor() {
        var d = FallbackPolicy.decide(wrapped(), new FallbackPolicy.Inputs(true,
                new BigDecimal("64870.5"), null, new BigDecimal("65000"), "tier 1 value of 2026-09-01"));
        assertEquals(4, d.tier());
        assertEquals(new BigDecimal("65000"), d.price());
        assertTrue(d.note().contains("carried forward"));
    }

    @Test
    @DisplayName("tier 4: the feed is down → the prior fixing, flagged")
    void tier4WhenNoPrint() {
        var d = FallbackPolicy.decide(wrapped(), new FallbackPolicy.Inputs(true,
                null, BigDecimal.ONE, new BigDecimal("65000"), "prior"));
        assertEquals(4, d.tier());
    }

    @Test
    @DisplayName("tier 5: nothing to fall back on — a gap, no price")
    void tier5() {
        var d = FallbackPolicy.decide(wrapped(), new FallbackPolicy.Inputs(true, null, null, null, null));
        assertEquals(5, d.tier());
        assertNull(d.price());
    }

    @Test
    @DisplayName("a fund never gets tier 3 — there is no benchmark × factor for a NAV")
    void fundSkipsTier3() {
        var d = FallbackPolicy.decide(fund(), new FallbackPolicy.Inputs(false,
                new BigDecimal("1"), BigDecimal.ONE, new BigDecimal("890"), "prior"));
        assertEquals(4, d.tier());
    }

    @Test
    @DisplayName("disabling a tier skips it")
    void disabledTiers() {
        StrikeSchedule s = wrapped();
        s.setTiersEnabled(Map.of("tier3", false, "tier4", false, "tier5", true));
        var d = FallbackPolicy.decide(s, new FallbackPolicy.Inputs(true,
                new BigDecimal("64870.5"), BigDecimal.ONE, new BigDecimal("65000"), "prior"));
        assertEquals(5, d.tier());
    }

    @Test
    @DisplayName("tier 2 never produces a price: it is recorded as having run before the fallback")
    void tier2IsEscalationNotAPrice() {
        StrikeSchedule s = wrapped();
        s.setTiersEnabled(Map.of("tier2", true, "tier3", true, "tier4", true, "tier5", true));
        var d = FallbackPolicy.decide(s, new FallbackPolicy.Inputs(true,
                new BigDecimal("100"), BigDecimal.ONE, null, null));
        assertEquals(3, d.tier());
        assertTrue(d.tier2Requested());
        assertTrue(d.note().contains(FallbackPolicy.TIER2_STATUS));

        StrikeSchedule off = wrapped();
        off.setTiersEnabled(Map.of("tier2", false, "tier3", true, "tier4", true, "tier5", true));
        var d2 = FallbackPolicy.decide(off, new FallbackPolicy.Inputs(true,
                new BigDecimal("100"), BigDecimal.ONE, null, null));
        assertFalse(d2.tier2Requested());
        assertTrue(d2.note().contains(FallbackPolicy.TIER2_DISABLED));
    }

    @Test
    @DisplayName("defaults: 16:00 Europe/London, 30-minute window, daily calendar, tier 2 on, LX1 after its components")
    void defaults() {
        var all = StrikeSchedule.defaults();
        assertEquals(3, all.size());
        var cbtc = all.get(0);
        assertEquals("CBTC", cbtc.getInstrumentId());
        assertEquals("16:00", cbtc.getStrikeAt());
        assertEquals("Europe/London", cbtc.getTimezone());
        assertEquals(30, cbtc.getWindowMinutes());
        assertEquals(StrikeCalendars.DAILY, cbtc.getCalendar());
        assertTrue(cbtc.tierEnabled(2) && cbtc.tierEnabled(3) && cbtc.tierEnabled(4) && cbtc.tierEnabled(5));
        // BST on 1 Sep 2026: 16:00 London = 15:00Z
        assertEquals(Instant.parse("2026-09-01T15:00:00Z"), cbtc.strikeInstantOn(LocalDate.of(2026, 9, 1)));
        var lx1 = all.get(2);
        assertTrue(lx1.isFund());
        assertEquals(StrikeCalendars.DAILY, lx1.getCalendar());   // all-crypto fund
        assertEquals(java.util.List.of("CBTC", "cETH"), lx1.getDependsOn());
    }

    @Test
    @DisplayName("a blank calendar reads as daily; alternates are looked up by seat, case-insensitively")
    void calendarAndAlternates() {
        StrikeSchedule s = wrapped();
        s.setCalendar(" ");
        assertEquals(StrikeCalendars.DAILY, s.getCalendar());
        s.setCalendar("NYSE");
        assertEquals("nyse", s.getCalendar());
        s.setAlternates(Map.of("Lender", java.util.List.of("alt@lender.example", " ", "second@lender.example")));
        assertEquals(java.util.List.of("alt@lender.example", "second@lender.example"), s.alternatesFor("lender"));
        assertTrue(s.alternatesFor("venue").isEmpty());
        assertEquals(s.alternatesFor("lender"), s.copy().alternatesFor("lender"));
    }

    @Test
    @DisplayName("the store refuses an unknown calendar or an unknown alternates seat")
    void storeValidation() {
        ScheduleStore store = ScheduleStore.inMemory();
        StrikeSchedule bad = wrapped();
        bad.setCalendar("tse");
        var ex = org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> store.replace(java.util.List.of(bad)));
        assertTrue(ex.getMessage().contains("tse"));
        StrikeSchedule badSeat = wrapped();
        badSeat.setAlternates(Map.of("auditor", java.util.List.of("x@y")));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> store.replace(java.util.List.of(badSeat)));
        StrikeSchedule ok = wrapped();
        ok.setCalendar("lse");
        ok.setAlternates(Map.of("lender", java.util.List.of("x@y")));
        assertEquals("lse", store.replace(java.util.List.of(ok)).get(0).getCalendar());
    }
}
