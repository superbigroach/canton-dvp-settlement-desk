package com.lucilla.settlement.signing;

import com.lucilla.settlement.auth.FileUserStore;
import com.lucilla.settlement.auth.Principal;
import com.lucilla.settlement.auth.Role;
import com.lucilla.settlement.auth.SignerSettings;
import com.lucilla.settlement.auth.UserRecord;
import com.lucilla.settlement.events.JsonlEventStore;
import com.lucilla.settlement.ledger.LedgerService;
import com.lucilla.settlement.ledger.SignerEvidence;
import com.lucilla.settlement.scheduler.ScheduleStore;
import com.lucilla.settlement.web.Dtos;
import com.lucilla.settlement.web.SettlementController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@code POST /api/proposals/{cid}/confirm} for the issuer and lender seats: a bare
 * tick is refused with the schema, numbers that fail the rule are refused with the
 * number, and numbers that pass are verified before the on-ledger confirm is sent. The
 * venue path is unchanged.
 */
class ProposalServiceEvidenceTest {

    LedgerService ledger = mock(LedgerService.class);
    SettlementController desk = mock(SettlementController.class);
    JsonlEventStore events = JsonlEventStore.inMemory();
    FileUserStore users;
    ProposalService service;

    static final Principal LENDER = new Principal("lender-uid", "lender@x", Role.SIGNER, "Bank", "lender",
            List.of("CBTC"), "Bank", "Lender", "sandbox-header", null);
    static final Principal ISSUER = new Principal("issuer-uid", "issuer@x", Role.SIGNER, "Issuer", "issuer",
            List.of("CBTC"), "Issuer", "Issuer", "sandbox-header", null);
    static final Principal VENUE = new Principal("venue-uid", "venue@x", Role.SIGNER, "Venue", "venue",
            List.of("CBTC"), "Venue", "Venue", "sandbox-header", null);

    static LedgerService.FixingProposalView proposal() {
        return new LedgerService.FixingProposalView("p1", "Issuer", List.of("Issuer", "Bank", "Venue"), 2,
                "Auditor", "Issuer", "CBTC", "USDC", "Close", new BigDecimal("65000"), "scheduled",
                BigDecimal.ZERO, "ACT/365", Instant.parse("2026-09-02T15:00:00Z"), List.of("Issuer"),
                new BigDecimal("65000"), BigDecimal.ONE, List.of(), "1");
    }

    @BeforeEach
    void setUp() {
        UserRecord lender = new UserRecord();
        lender.setUid("lender-uid");
        lender.setEmail("lender@x");
        lender.setRole("signer");
        lender.setSeat("lender");
        lender.setParty("Bank");
        users = FileUserStore.inMemory(List.of(lender));
        for (String p : List.of("Bank", "Issuer", "Venue")) when(ledger.resolveParty(p)).thenReturn(p);
        for (String p : List.of("Bank", "Issuer", "Venue")) when(ledger.fixingProposalsVisibleTo(p)).thenReturn(List.of(proposal()));
        when(desk.confirmFixingWithChecks(eq("p1"), any()))
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED).body(new Dtos.CidResponse("p2")));
        service = new ProposalService(ledger, events, users, ScheduleStore.inMemory(), desk);
    }

    @Test
    @DisplayName("a lender's bare tick is refused with 422 semantics and the schema; nothing reaches the ledger")
    void lenderBareTickIsRefused() {
        assertThatThrownBy(() -> service.confirm(LENDER, "p1", List.of("book-acceptance"), null))
                .isInstanceOf(SignerEvidence.Rejected.class)
                .hasMessageContaining("evidence, not a tick")
                .satisfies(e -> {
                    SignerEvidence.Rejected r = (SignerEvidence.Rejected) e;
                    assertThat(r.seat()).isEqualTo("lender");
                    assertThat(r.schema()).containsOnlyKeys("book-acceptance");
                });
        // The old venue-shaped body from a lender is evidence of the wrong shape, not a tick — still refused.
        assertThatThrownBy(() -> service.confirm(LENDER, "p1", List.of("book-acceptance"), Map.of("low", 1, "high", 2)))
                .isInstanceOf(SignerEvidence.Rejected.class)
                .hasMessageContaining("acceptedAt");
        verify(desk, never()).confirmFixingWithChecks(any(), any());
    }

    @Test
    @DisplayName("a lender's mark outside its declared tolerance is refused with the deviation")
    void lenderMarkOutsideTolerance() {
        assertThatThrownBy(() -> service.confirm(LENDER, "p1", List.of("independent-mark-within-tolerance"),
                Map.of("independent-mark-within-tolerance", Map.of("independentMark", 64000))))
                .isInstanceOf(SignerEvidence.Rejected.class)
                .hasMessageContaining("153.85 bp")
                .hasMessageContaining("25 bp");
        verify(desk, never()).confirmFixingWithChecks(any(), any());
    }

    @Test
    @DisplayName("valid lender evidence is verified against the signer's own tolerance and passed through to the confirm")
    void lenderValidEvidence() {
        UserRecord u = users.byUid("lender-uid").orElseThrow();
        SignerSettings s = new SignerSettings();
        s.setTolerances(Map.of("markBps", 200));
        u.setSettings(s);
        users.save(u);

        Map<String, Object> evidence = Map.of(
                "independent-mark-within-tolerance", Map.of("independentMark", 64000),
                "liquidations-consistent", Map.of("liquidationsToday", 0, "worstDeviationBps", 0),
                "book-acceptance", Map.of("acceptedAt", Instant.now().minusSeconds(60).toString()));
        Map<String, Object> out = service.confirm(LENDER, "p1",
                List.of("independent-mark-within-tolerance", "liquidations-consistent", "book-acceptance"), evidence);

        assertThat(out.get("confirmed")).isEqualTo(true);
        assertThat(out.get("nextCid")).isEqualTo("p2");
        assertThat(out.get("verified")).isEqualTo(true);
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> v = (Map<String, Map<String, Object>>) out.get("evidence");
        assertThat(v.get("independent-mark-within-tolerance").get("deviationBps")).isEqualTo(new BigDecimal("153.85"));
        assertThat(v.get("independent-mark-within-tolerance").get("toleranceBps")).isEqualTo(200);

        ArgumentCaptor<Dtos.ConfirmWithChecksRequest> req = ArgumentCaptor.forClass(Dtos.ConfirmWithChecksRequest.class);
        verify(desk).confirmFixingWithChecks(eq("p1"), req.capture());
        assertThat(req.getValue().member()).isEqualTo("Bank");
        assertThat(req.getValue().role()).isEqualTo("lender");
        assertThat(req.getValue().evidence()).isEqualTo(evidence);
        assertThat(req.getValue().toleranceBps()).isEqualTo(200);
        assertThat(req.getValue().observedLow()).isNull();
    }

    @Test
    @DisplayName("an issuer needs its numbers too")
    void issuerNeedsEvidence() {
        assertThatThrownBy(() -> service.confirm(ISSUER, "p1", List.of("attestor-quorum"), Map.of()))
                .isInstanceOf(SignerEvidence.Rejected.class);
        Map<String, Object> out = service.confirm(ISSUER, "p1", List.of("attestor-quorum"),
                Map.of("attestor-quorum", Map.of("quorumSigners", 7, "quorumThreshold", 7)));
        assertThat(out.get("verified")).isEqualTo(true);
    }

    @Test
    @DisplayName("the venue path is exactly as before: {low, high} becomes the observed range the ledger checks")
    void venueUnchanged() {
        Map<String, Object> out = service.confirm(VENUE, "p1", List.of("traded-range"), Map.of("low", 64900, "high", "65100"));
        assertThat(out.get("confirmed")).isEqualTo(true);
        assertThat(out).doesNotContainKey("verified");
        ArgumentCaptor<Dtos.ConfirmWithChecksRequest> req = ArgumentCaptor.forClass(Dtos.ConfirmWithChecksRequest.class);
        verify(desk).confirmFixingWithChecks(eq("p1"), req.capture());
        assertThat(req.getValue().observedLow()).isEqualByComparingTo("64900");
        assertThat(req.getValue().observedHigh()).isEqualByComparingTo("65100");
        assertThat(req.getValue().evidence()).isNull();
    }
}
