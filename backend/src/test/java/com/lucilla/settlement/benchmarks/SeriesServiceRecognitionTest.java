package com.lucilla.settlement.benchmarks;

import com.lucilla.settlement.events.JsonlEventStore;
import com.lucilla.settlement.ledger.LedgerService;
import com.lucilla.settlement.scheduler.ScheduleStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link SeriesService#recognised}: a fixing is published only if some committee visible
 * to the auditor has the same admin, the same threshold, every attestor as a member, and
 * enough attestors to meet that threshold. Anything else is a possible forgery and is
 * dropped (Daml audit G1/G2, 22 Sep 2026).
 */
class SeriesServiceRecognitionTest {

    static final Instant AT = Instant.parse("2026-09-22T15:00:00Z");
    static final List<String> COMMITTEE = List.of("Issuer::1", "Bank::1", "Venue::1");

    LedgerService ledger = mock(LedgerService.class);
    // A mock, not a real one: these tests exercise recognition, which never asks for a
    // price, and a test that can reach a trading venue is a test that can fail on a train.
    SeriesService service = new SeriesService(ledger, JsonlEventStore.inMemory(), ScheduleStore.inMemory(),
            mock(com.lucilla.settlement.ledger.MarketData.class));

    static LedgerService.CommitteeView committee(String cid, String admin, List<String> members, long threshold) {
        return new LedgerService.CommitteeView(cid, admin, members, threshold, "Auditor::1", "cBTC committee");
    }

    static LedgerService.NavFixingView fixing(String cid, String admin, long threshold, List<String> attestors) {
        return new LedgerService.NavFixingView(cid, attestors, threshold, "CBTC", "USDC", "Close",
                new BigDecimal("65000"), "r", BigDecimal.ZERO, "NONE", AT, List.of(), AT.plusSeconds(60),
                "committee", null, null, null, null, admin, LocalDate.of(2026, 9, 22), attestors);
    }

    static LedgerService.NavFixingView dated(String cid, LocalDate asOf) {
        List<String> attestors = List.of("Issuer::1", "Bank::1");
        return new LedgerService.NavFixingView(cid, attestors, 2, "CBTC", "USDC", "Close",
                new BigDecimal("65000"), "r", BigDecimal.ZERO, "NONE", AT, List.of(), AT.plusSeconds(60),
                "committee", null, null, null, null, "Issuer::1", asOf, attestors);
    }

    @Test
    @DisplayName("a fixing dated after today is never published, however well it is attested")
    void futureDatedIsDropped() {
        LocalDate today = LocalDate.now(ZoneId.of("Europe/London"));
        // The DevNet case, 24 Sep 2026: a real 2-of-3 attestation dated 2039-02-03.
        assertThat(service.recognised(List.of(dated("far", LocalDate.of(2039, 2, 3))))).isEmpty();
        assertThat(service.recognised(List.of(dated("tomorrow", today.plusDays(1))))).isEmpty();
    }

    @Test
    @DisplayName("today, yesterday and an undated fixing are kept — the boundary is today, not before it")
    void todayAndEarlierAreKept() {
        LocalDate today = LocalDate.now(ZoneId.of("Europe/London"));
        var now = dated("today", today);
        var before = dated("yesterday", today.minusDays(1));
        var undated = dated("undated", null);
        assertThat(service.recognised(List.of(now, before, undated))).containsExactly(now, before, undated);
    }

    @Test
    @DisplayName("a future-dated fixing cannot become the published value even when it is the only one")
    void futureDatedNeverBecomesTheMark() {
        assertThat(service.recognised(List.of(dated("only", LocalDate.of(2039, 2, 3))))).isEmpty();
    }

    @BeforeEach
    void oneCommittee() {
        when(ledger.resolveParty("Auditor")).thenReturn("Auditor::1");
        when(ledger.committeesVisibleTo("Auditor::1"))
                .thenReturn(List.of(committee("c1", "Issuer::1", COMMITTEE, 2)));
    }

    @Test
    @DisplayName("a fixing whose admin, threshold and attestors match a visible committee is kept")
    void matchingFixingIsKept() {
        var f = fixing("f1", "Issuer::1", 2, List.of("Issuer::1", "Bank::1"));
        assertThat(service.recognised(List.of(f))).containsExactly(f);
        // the whole committee signing is also fine — more than the threshold, all members
        var full = fixing("f2", "Issuer::1", 2, COMMITTEE);
        assertThat(service.recognised(List.of(full))).containsExactly(full);
    }

    @Test
    @DisplayName("a fixing naming an admin no visible committee has is dropped")
    void adminMismatchIsDropped() {
        assertThat(service.recognised(List.of(fixing("f1", "Rogue::1", 2, List.of("Issuer::1", "Bank::1"))))).isEmpty();
        assertThat(service.recognised(List.of(fixing("f1", null, 2, List.of("Issuer::1", "Bank::1"))))).isEmpty();
    }

    @Test
    @DisplayName("a fixing whose threshold differs from the committee's is dropped — the single-party threshold=1 forgery")
    void thresholdMismatchIsDropped() {
        assertThat(service.recognised(List.of(fixing("f1", "Issuer::1", 1, List.of("Issuer::1"))))).isEmpty();
        assertThat(service.recognised(List.of(fixing("f1", "Issuer::1", 1, List.of("Issuer::1", "Bank::1"))))).isEmpty();
        assertThat(service.recognised(List.of(fixing("f1", "Issuer::1", 3, COMMITTEE)))).isEmpty();
    }

    @Test
    @DisplayName("a fixing with an attestor who is not a committee member is dropped, even if the rest are")
    void outsideAttestorIsDropped() {
        assertThat(service.recognised(List.of(fixing("f1", "Issuer::1", 2, List.of("Issuer::1", "Eve::1"))))).isEmpty();
        assertThat(service.recognised(List.of(fixing("f1", "Issuer::1", 2, List.of("Issuer::1", "Bank::1", "Eve::1"))))).isEmpty();
        assertThat(service.recognised(List.of(fixing("f1", "Issuer::1", 2, null)))).isEmpty();
    }

    @Test
    @DisplayName("a fixing with fewer attestors than the threshold it claims is dropped")
    void underThresholdIsDropped() {
        assertThat(service.recognised(List.of(fixing("f1", "Issuer::1", 2, List.of("Issuer::1"))))).isEmpty();
        assertThat(service.recognised(List.of(fixing("f1", "Issuer::1", 2, List.of())))).isEmpty();
    }

    @Test
    @DisplayName("a mixed list keeps only the recognised fixings, in order")
    void mixedListIsFiltered() {
        var good1 = fixing("good1", "Issuer::1", 2, List.of("Issuer::1", "Bank::1"));
        var bad = fixing("bad", "Issuer::1", 1, List.of("Issuer::1"));
        var good2 = fixing("good2", "Issuer::1", 2, List.of("Bank::1", "Venue::1"));
        var rogue = fixing("rogue", "Rogue::1", 2, List.of("Issuer::1", "Bank::1"));
        assertThat(service.recognised(List.of(good1, bad, good2, rogue))).containsExactly(good1, good2);
    }

    @Test
    @DisplayName("any visible committee may recognise the fixing — the search does not stop at the first mismatch")
    void anyVisibleCommitteeMayMatch() {
        when(ledger.committeesVisibleTo("Auditor::1")).thenReturn(List.of(
                committee("c1", "Issuer::1", COMMITTEE, 2),
                committee("c2", "Admin2::1", List.of("Admin2::1", "TA::1", "Custodian::1"), 3)));
        var second = fixing("f1", "Admin2::1", 3, List.of("Admin2::1", "TA::1", "Custodian::1"));
        assertThat(service.recognised(List.of(second))).containsExactly(second);
        // same admin, wrong threshold for THAT committee, and no other committee with that admin
        assertThat(service.recognised(List.of(fixing("f2", "Admin2::1", 2, List.of("Admin2::1", "TA::1"))))).isEmpty();
    }

    @Test
    @DisplayName("with no committee visible — none, or the ledger read fails — every fixing is dropped, never published")
    void noCommitteeMeansNothingIsRecognised() {
        var f = fixing("f1", "Issuer::1", 2, List.of("Issuer::1", "Bank::1"));
        when(ledger.committeesVisibleTo("Auditor::1")).thenReturn(List.of());
        assertThat(service.recognised(List.of(f))).isEmpty();
        when(ledger.committeesVisibleTo("Auditor::1")).thenThrow(new LedgerService.LedgerException("participant down"));
        assertThat(service.recognised(List.of(f))).isEmpty();
        assertThat(service.recognised(List.of())).isEmpty();
    }
}
