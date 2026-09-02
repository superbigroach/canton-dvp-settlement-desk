package com.lucilla.settlement.scheduler;

import com.lucilla.settlement.auth.AuthProperties;
import com.lucilla.settlement.auth.FileUserStore;
import com.lucilla.settlement.auth.SignerSettings;
import com.lucilla.settlement.auth.UserRecord;
import com.lucilla.settlement.benchmarks.SeriesService;
import com.lucilla.settlement.events.FixingEvent;
import com.lucilla.settlement.events.JsonlEventStore;
import com.lucilla.settlement.ledger.LedgerService;
import com.lucilla.settlement.ledger.MarketData;
import com.lucilla.settlement.ledger.StrikeCalendars;
import com.lucilla.settlement.web.SettlementController;
import com.lucilla.settlement.webhooks.WebhookDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tier 2 through the runner: a proposal open on a SATURDAY (the daily calendar strikes
 * it), one of three signatures in, and the clock walked through the window. What must
 * hold: nothing before half, a reminder to every unconfirmed seat at half, no repeat a
 * minute later, alternates at three quarters (only those on the committee), and the
 * fallback only after the window closes.
 */
class StrikeServiceEscalationTest {

    static final Instant STRIKE = Instant.parse("2026-09-05T15:00:00Z");   // Sat 5 Sep 2026, 16:00 London
    static final Instant END = STRIKE.plusSeconds(30 * 60);

    LedgerService ledger = mock(LedgerService.class);
    MarketData marketData = mock(MarketData.class);
    SeriesService series = mock(SeriesService.class);
    SettlementController desk = mock(SettlementController.class);
    WebhookDispatcher webhooks = mock(WebhookDispatcher.class);
    JsonlEventStore events = JsonlEventStore.inMemory();
    ScheduleStore schedules = new ScheduleStore(null, StrikeCalendars.defaults());
    FileUserStore users;
    StrikeService strikes;

    static UserRecord signer(String email, String seat, String party, boolean webhook) {
        UserRecord u = new UserRecord();
        u.setUid(email);
        u.setEmail(email);
        u.setRole("signer");
        u.setSeat(seat);
        u.setParty(party);
        u.setInstruments(List.of("CBTC"));
        if (webhook) {
            SignerSettings s = new SignerSettings();
            s.setWebhookUrl("https://example.invalid/" + email);
            u.setSettings(s);
        }
        return u;
    }

    static LedgerService.FixingProposalView proposal(List<String> approvers) {
        return new LedgerService.FixingProposalView("p#1", "Issuer", List.of("Issuer", "Bank", "Venue"), 2,
                "Auditor", "Issuer", "CBTC", "USDC", "Close", new BigDecimal("65000"), "scheduled",
                BigDecimal.ZERO, "ACT/365", STRIKE, approvers, new BigDecimal("65000"), BigDecimal.ONE,
                List.of(), "1");
    }

    @BeforeEach
    void setUp() {
        users = FileUserStore.inMemory(List.of(
                signer("issuer@x", "issuer", "Issuer", true),
                signer("lender@x", "lender", "Bank", true),
                signer("venue@x", "venue", "Venue", false),
                signer("lender2@x", "lender", "Bank", true),        // an alternate ON the committee
                signer("outsider@x", "lender", "Outsider", true))); // an alternate NOT on the committee
        StrikeSchedule cbtc = new StrikeSchedule();
        cbtc.setInstrumentId("CBTC");
        cbtc.setWindowMinutes(30);
        cbtc.setAlternates(Map.of("lender", List.of("lender2@x", "outsider@x", "nobody@x")));
        schedules.replace(List.of(cbtc));
        AuthProperties props = new AuthProperties();
        props.setOperatorParty("Issuer");
        when(ledger.resolveParty("Issuer")).thenReturn("Issuer");
        when(ledger.fixingProposalsVisibleTo("Issuer")).thenReturn(List.of(proposal(List.of("Issuer"))));
        when(webhooks.dispatchTo(any(), anyString(), anyString(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(inv -> ((Collection<?>) inv.getArgument(0)).size());
        strikes = new StrikeService(ledger, marketData, series, schedules, events, props, desk, webhooks, users,
                new org.springframework.beans.factory.support.StaticListableBeanFactory().getBeanProvider(com.lucilla.settlement.config.DemoSeed.class));
    }

    List<FixingEvent> reminders() {
        return events.all().stream().filter(e -> FixingEvent.Kinds.PROPOSAL_REMINDER.equals(e.kind())).toList();
    }

    @Test
    @DisplayName("the daily calendar strikes on a Saturday; the weekday calendar does not")
    void calendarGatesTheDay() {
        strikes.tick(STRIKE.plusSeconds(20 * 60));
        assertThat(reminders()).isNotEmpty();

        events = JsonlEventStore.inMemory();
        StrikeSchedule weekdays = schedules.all().get(0);
        weekdays.setCalendar(StrikeCalendars.WEEKDAYS);
        schedules.replace(List.of(weekdays));
        StrikeService weekdayRunner = new StrikeService(ledger, marketData, series, schedules, events,
                new AuthProperties(), desk, webhooks, users,
                new org.springframework.beans.factory.support.StaticListableBeanFactory().getBeanProvider(com.lucilla.settlement.config.DemoSeed.class));
        weekdayRunner.tick(STRIKE.plusSeconds(20 * 60));
        assertThat(events.all()).isEmpty();
        assertThat(weekdayRunner.status(weekdays, STRIKE.plusSeconds(60)).get("state")).isEqualTo("NOT_DUE_TODAY");
    }

    @Test
    @DisplayName("nothing goes out before half the window")
    void quietBeforeHalf() {
        strikes.tick(STRIKE.plusSeconds(14 * 60));
        assertThat(reminders()).isEmpty();
        verify(webhooks, never()).dispatchTo(any(), anyString(), anyString(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("at half the window every unconfirmed seat is reminded once, with escalation 1")
    void escalationOneAtHalf() {
        strikes.tick(STRIKE.plusSeconds(15 * 60));
        List<FixingEvent> r = reminders();
        assertThat(r).hasSize(2);   // Bank and Venue; Issuer already signed
        assertThat(r).extracting(e -> e.details().get("party")).containsExactlyInAnyOrder("Bank", "Venue");
        assertThat(r).allSatisfy(e -> {
            assertThat(e.details().get("escalation")).isEqualTo(1);
            assertThat(e.details()).doesNotContainKey("alternates");
            assertThat(e.proposalCid()).isEqualTo("p#1");
        });
        FixingEvent bank = r.stream().filter(e -> "Bank".equals(e.details().get("party"))).findFirst().orElseThrow();
        assertThat(bank.seat()).isEqualTo("lender");
        assertThat(bank.details().get("webhooksQueued")).isEqualTo(2);   // lender@x and lender2@x both map to Bank

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> extra = ArgumentCaptor.forClass(Map.class);
        verify(webhooks, times(2)).dispatchTo(any(), eq(WebhookDispatcher.PROPOSAL_REMINDER), eq("CBTC"), eq("p#1"),
                eq(new BigDecimal("65000")), any(), any(), eq(END), extra.capture());
        assertThat(extra.getAllValues()).allSatisfy(m -> assertThat(m.get("escalation")).isEqualTo(1));

        // A minute later: nothing new.
        strikes.tick(STRIKE.plusSeconds(16 * 60));
        assertThat(reminders()).hasSize(2);
        verify(webhooks, times(2)).dispatchTo(any(), anyString(), anyString(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("at three quarters the same seats are reminded again with escalation 2, and alternates are brought in")
    void escalationTwoAtThreeQuarters() {
        strikes.tick(STRIKE.plusSeconds(15 * 60));
        strikes.tick(STRIKE.plusSeconds(23 * 60));
        List<FixingEvent> level2 = reminders().stream().filter(e -> Integer.valueOf(2).equals(e.details().get("escalation"))).toList();
        assertThat(level2).hasSize(2);
        FixingEvent bank = level2.stream().filter(e -> "Bank".equals(e.details().get("party"))).findFirst().orElseThrow();
        assertThat(bank.details().get("alternates")).isEqualTo(List.of("lender2@x"));
        @SuppressWarnings("unchecked")
        List<Map<String, String>> skipped = (List<Map<String, String>>) bank.details().get("alternatesSkipped");
        assertThat(skipped).extracting(m -> m.get("email")).containsExactlyInAnyOrder("outsider@x", "nobody@x");
        assertThat(skipped.stream().filter(m -> "outsider@x".equals(m.get("email"))).findFirst().orElseThrow().get("why"))
                .contains("not a committee member on-ledger");
        assertThat(skipped.stream().filter(m -> "nobody@x".equals(m.get("email"))).findFirst().orElseThrow().get("why"))
                .contains("roster");
        assertThat(bank.reason()).contains("escalation 2");

        // Jumping straight to three quarters without ever having sent level 1 still sends level 2 only once.
        strikes.tick(STRIKE.plusSeconds(24 * 60));
        assertThat(reminders()).hasSize(4);
        // …and the fallback has NOT run inside the window.
        assertThat(events.all()).noneMatch(e -> FixingEvent.Kinds.FIXING_MISSED.equals(e.kind())
                || FixingEvent.Kinds.FIXING_FALLBACK.equals(e.kind()));
    }

    @Test
    @DisplayName("only after the window closes does tier 3+ run — here tier 5, a gap, with the escalations on the record")
    void fallbackOnlyAfterTheWindow() {
        strikes.tick(STRIKE.plusSeconds(23 * 60));
        strikes.tick(STRIKE.plusSeconds(30 * 60));   // the last instant of the window: still nothing
        assertThat(events.all()).noneMatch(e -> FixingEvent.Kinds.FIXING_MISSED.equals(e.kind()));
        strikes.tick(STRIKE.plusSeconds(31 * 60));
        List<FixingEvent> missed = events.all().stream().filter(e -> FixingEvent.Kinds.FIXING_MISSED.equals(e.kind())).toList();
        assertThat(missed).hasSize(1);
        assertThat(missed.get(0).tier()).isEqualTo(5);
        assertThat(missed.get(0).details().get("escalationsSent")).isEqualTo(2);
        assertThat(missed.get(0).details().get("tier2")).isEqualTo(FallbackPolicy.TIER2_STATUS);
        assertThat(missed.get(0).details().get("calendar")).isEqualTo("daily");
        verify(webhooks).dispatch(eq(WebhookDispatcher.FIXING_MISSED), eq("CBTC"), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("with tier 2 switched off nothing is sent inside the window")
    void tier2Off() {
        StrikeSchedule s = schedules.all().get(0);
        s.setTiersEnabled(Map.of("tier2", false, "tier3", true, "tier4", true, "tier5", true));
        schedules.replace(List.of(s));
        strikes.tick(STRIKE.plusSeconds(25 * 60));
        assertThat(reminders()).isEmpty();
        assertThat(strikes.status(s, STRIKE.plusSeconds(60)).get("tier2")).isEqualTo(FallbackPolicy.TIER2_DISABLED);
    }

    @Test
    @DisplayName("the status endpoint says what the calendar decided and when the nudges are due")
    void statusShape() {
        Map<String, Object> st = strikes.status(schedules.all().get(0), STRIKE.plusSeconds(60));
        assertThat(st.get("calendar")).isEqualTo("daily");
        assertThat(st.get("strikesToday")).isEqualTo(true);
        assertThat(st.get("state")).isEqualTo("WAITING_SIGNATURES");
        @SuppressWarnings("unchecked")
        Map<String, Object> esc = (Map<String, Object>) st.get("escalation");
        assertThat(esc.get("firstAt")).isEqualTo("2026-09-05T15:15:00Z");
        assertThat(esc.get("secondAt")).isEqualTo("2026-09-05T15:22:30Z");
        assertThat(esc.get("sent")).isEqualTo(0);
        assertThat(st.get("effectiveCalendars")).isEqualTo(List.of("daily"));
    }

    @Test
    @DisplayName("a fund strikes only on days all its components strike")
    void fundIntersection() {
        StrikeSchedule spy = new StrikeSchedule();
        spy.setInstrumentId("SPY");
        spy.setCalendar(StrikeCalendars.NYSE);
        StrikeSchedule cbtc = schedules.all().get(0);
        StrikeSchedule fund = new StrikeSchedule();
        fund.setInstrumentId("MIX");
        fund.setKind("fund");
        fund.setDependsOn(List.of("CBTC", "SPY"));
        schedules.replace(List.of(cbtc, spy, fund));
        assertThat(strikes.strikesOn(fund, java.time.LocalDate.of(2026, 9, 5))).isFalse();   // Saturday
        assertThat(strikes.strikesOn(fund, java.time.LocalDate.of(2026, 7, 3))).isFalse();   // NYSE holiday
        assertThat(strikes.strikesOn(fund, java.time.LocalDate.of(2026, 7, 6))).isTrue();
        assertThat(strikes.strikesOn(cbtc, java.time.LocalDate.of(2026, 9, 5))).isTrue();
        assertThat(strikes.effectiveCalendars(fund)).containsExactly("daily", "nyse");
        assertThat(strikes.nextStrikeDay(fund, java.time.LocalDate.of(2026, 7, 3))).isEqualTo(java.time.LocalDate.of(2026, 7, 6));
    }
}
