package com.lucilla.settlement.scheduler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Tier 2 timing: reminders at half the window, alternates at three quarters. */
class EscalationPolicyTest {

    static final Instant STRIKE = Instant.parse("2026-09-05T15:00:00Z");     // 16:00 London
    static final Instant END = Instant.parse("2026-09-05T15:30:00Z");        // 30-minute window

    @Test
    @DisplayName("nothing before half the window; escalation 1 from half; escalation 2 from three quarters")
    void levels() {
        assertEquals(0, EscalationPolicy.levelDue(STRIKE, END, STRIKE));
        assertEquals(0, EscalationPolicy.levelDue(STRIKE, END, Instant.parse("2026-09-05T15:14:59Z")));
        assertEquals(1, EscalationPolicy.levelDue(STRIKE, END, Instant.parse("2026-09-05T15:15:00Z")));
        assertEquals(1, EscalationPolicy.levelDue(STRIKE, END, Instant.parse("2026-09-05T15:22:29Z")));
        assertEquals(2, EscalationPolicy.levelDue(STRIKE, END, Instant.parse("2026-09-05T15:22:30Z")));
        assertEquals(2, EscalationPolicy.levelDue(STRIKE, END, Instant.parse("2026-09-05T15:29:59Z")));
        // Past the window the fallback runs; the level is still 2, not something new.
        assertEquals(2, EscalationPolicy.levelDue(STRIKE, END, Instant.parse("2026-09-05T15:31:00Z")));
    }

    @Test
    @DisplayName("before the strike, or with no window at all, nothing escalates")
    void edges() {
        assertEquals(0, EscalationPolicy.levelDue(STRIKE, END, STRIKE.minusSeconds(1)));
        assertEquals(0, EscalationPolicy.levelDue(STRIKE, STRIKE, STRIKE.plusSeconds(60)));
    }

    @Test
    @DisplayName("the due instants are published so the console can show when the next nudge goes out")
    void dueAt() {
        assertEquals(Instant.parse("2026-09-05T15:15:00Z"), EscalationPolicy.dueAt(STRIKE, END, 1));
        assertEquals(Instant.parse("2026-09-05T15:22:30Z"), EscalationPolicy.dueAt(STRIKE, END, 2));
    }
}
