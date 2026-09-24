package com.lucilla.settlement.signing;

import com.lucilla.settlement.auth.FileUserStore;
import com.lucilla.settlement.auth.Principal;
import com.lucilla.settlement.auth.Role;
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
import java.time.LocalDate;
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
 * {@code POST /api/proposals/{cid}/confirm} for the v2 seats — custodian and transfer
 * agent — and the venue's two forms of evidence. Companion to
 * {@link ProposalServiceEvidenceTest}, which covers the issuer and lender.
 */
class ProposalServiceSeatsTest {

    LedgerService ledger = mock(LedgerService.class);
    SettlementController desk = mock(SettlementController.class);
    ProposalService service;

    static final Principal CUSTODIAN = new Principal("custodian-uid", "custodian@x", Role.SIGNER, "Custodian",
            "custodian", List.of("TEQ"), "Custodian", "Custodian", "sandbox-header", null);
    static final Principal TRANSFER_AGENT = new Principal("ta-uid", "ta@x", Role.SIGNER, "TA",
            "transfer-agent", List.of("TEQ"), "TA", "Transfer agent", "sandbox-header", null);
    static final Principal VENUE = new Principal("venue-uid", "venue@x", Role.SIGNER, "Venue", "venue",
            List.of("TEQ"), "Venue", "Venue", "sandbox-header", null);

    static LedgerService.FixingProposalView proposal() {
        return new LedgerService.FixingProposalView("p1", "Issuer",
                List.of("Issuer", "Custodian", "TA", "Venue"), 3, "Auditor", "Issuer", "TEQ", "USDC", "Close",
                new BigDecimal("65000"), "scheduled", BigDecimal.ZERO, "ACT/365",
                Instant.parse("2026-09-22T15:00:00Z"), List.of("Issuer"),
                new BigDecimal("65000"), BigDecimal.ONE, List.of(), "1", LocalDate.of(2026, 9, 22));
    }

    @BeforeEach
    void setUp() {
        for (String p : List.of("Custodian", "TA", "Venue")) {
            when(ledger.resolveParty(p)).thenReturn(p);
            when(ledger.fixingProposalsVisibleTo(p)).thenReturn(List.of(proposal()));
        }
        when(desk.confirmFixingWithChecks(eq("p1"), any()))
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED).body(new Dtos.CidResponse("p2")));
        service = new ProposalService(ledger, JsonlEventStore.inMemory(), FileUserStore.inMemory(List.of()),
                ScheduleStore.inMemory(), desk);
    }

    static Map<String, Object> custodianGood() {
        return Map.of(
                "holdings-current", Map.of("statementAsOf", Instant.now().minusSeconds(3600).toString()),
                "holdings-cover-supply", Map.of("holdings", 1000, "supply", 1000),
                "no-encumbrance", Map.of("encumbered", 0));
    }

    Dtos.ConfirmWithChecksRequest sentToTheDesk() {
        ArgumentCaptor<Dtos.ConfirmWithChecksRequest> req = ArgumentCaptor.forClass(Dtos.ConfirmWithChecksRequest.class);
        verify(desk).confirmFixingWithChecks(eq("p1"), req.capture());
        return req.getValue();
    }

    @Test
    @DisplayName("a custodian's bare tick is refused with the schema of what it claimed; nothing reaches the desk")
    void custodianBareTickIsRefused() {
        List<String> checks = List.of("holdings-current", "no-encumbrance");
        for (Map<String, Object> nothing : java.util.Arrays.<Map<String, Object>>asList(null, Map.of())) {
            assertThatThrownBy(() -> service.confirm(CUSTODIAN, "p1", checks, nothing))
                    .isInstanceOf(SignerEvidence.Rejected.class)
                    .hasMessage("the custodian seat confirms with evidence, not a tick: "
                            + "supply the numbers for each checked condition (see `evidence`)")
                    .satisfies(e -> {
                        SignerEvidence.Rejected r = (SignerEvidence.Rejected) e;
                        assertThat(r.seat()).isEqualTo("custodian");
                        assertThat(r.problems()).containsExactly("evidence missing for [holdings-current, no-encumbrance]");
                        assertThat(r.schema()).containsOnlyKeys("holdings-current", "no-encumbrance");
                    });
        }
        verify(desk, never()).confirmFixingWithChecks(any(), any());
    }

    @Test
    @DisplayName("a transfer agent's bare tick is refused the same way")
    void transferAgentBareTickIsRefused() {
        assertThatThrownBy(() -> service.confirm(TRANSFER_AGENT, "p1", List.of("shares-outstanding-reconciled"), null))
                .isInstanceOf(SignerEvidence.Rejected.class)
                .hasMessageStartingWith("the transfer-agent seat confirms with evidence, not a tick")
                .satisfies(e -> assertThat(((SignerEvidence.Rejected) e).schema())
                        .containsOnlyKeys("shares-outstanding-reconciled"));
        verify(desk, never()).confirmFixingWithChecks(any(), any());
    }

    @Test
    @DisplayName("a custodian's numbers are verified with the default tolerances and forwarded to the desk as its seat")
    void custodianVerified() {
        List<String> checks = List.of("holdings-current", "holdings-cover-supply", "no-encumbrance");
        Map<String, Object> evidence = custodianGood();
        Map<String, Object> out = service.confirm(CUSTODIAN, "p1", checks, evidence);

        assertThat(out.get("confirmed")).isEqualTo(true);
        assertThat(out.get("nextCid")).isEqualTo("p2");
        assertThat(out.get("seat")).isEqualTo("custodian");
        assertThat(out.get("verified")).isEqualTo(true);
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> v = (Map<String, Map<String, Object>>) out.get("evidence");
        assertThat(v).containsOnlyKeys("holdings-current", "holdings-cover-supply", "no-encumbrance");
        assertThat(v.get("holdings-current")).containsKey("ageHours");
        assertThat(out.get("tolerances")).isEqualTo(Map.of("markBps", 25, "liquidationBps", 25));

        Dtos.ConfirmWithChecksRequest req = sentToTheDesk();
        assertThat(req.member()).isEqualTo("Custodian");
        assertThat(req.role()).isEqualTo("custodian");
        assertThat(req.checksPassed()).isEqualTo(checks);
        assertThat(req.evidence()).isEqualTo(evidence);
        assertThat(req.observedLow()).isNull();
        assertThat(req.observedHigh()).isNull();
    }

    @Test
    @DisplayName("a custodian whose numbers fail is refused with the number, before the desk")
    void custodianFailingNumberIsRefused() {
        Map<String, Object> encumbered = new java.util.LinkedHashMap<>(custodianGood());
        encumbered.put("no-encumbrance", Map.of("encumbered", 5));
        assertThatThrownBy(() -> service.confirm(CUSTODIAN, "p1",
                List.of("holdings-current", "holdings-cover-supply", "no-encumbrance"), encumbered))
                .isInstanceOf(SignerEvidence.Rejected.class)
                .hasMessage("evidence refused for the custodian seat: no-encumbrance: 5 units are pledged, lent or encumbered");
        verify(desk, never()).confirmFixingWithChecks(any(), any());
    }

    @Test
    @DisplayName("a transfer agent's reconciled register is verified and forwarded; a mismatch is refused")
    void transferAgentVerifiedOrRefused() {
        Map<String, Object> out = service.confirm(TRANSFER_AGENT, "p1",
                List.of("shares-outstanding-reconciled", "fees-accrued"),
                Map.of("shares-outstanding-reconciled", Map.of("registerShares", 5000, "ledgerShares", 5000),
                        "fees-accrued", Map.of("accruedFees", "12.5")));
        assertThat(out.get("confirmed")).isEqualTo(true);
        assertThat(out.get("verified")).isEqualTo(true);
        assertThat(sentToTheDesk().role()).isEqualTo("transfer-agent");

        assertThatThrownBy(() -> service.confirm(TRANSFER_AGENT, "p1", List.of("fees-accrued"),
                Map.of("fees-accrued", Map.of("accruedFees", -1))))
                .isInstanceOf(SignerEvidence.Rejected.class)
                .hasMessage("evidence refused for the transfer-agent seat: fees-accrued: accruedFees cannot be negative");
    }

    @Test
    @DisplayName("a venue with only a range is passed through untouched: {low, high} becomes the observed range, no server verification")
    void venueWithOnlyARangeIsAccepted() {
        Map<String, Object> out = service.confirm(VENUE, "p1", List.of("traded-range"), Map.of("low", 64900, "high", 65100));
        assertThat(out.get("confirmed")).isEqualTo(true);
        assertThat(out).doesNotContainKey("verified");
        assertThat(out.get("evidence")).isEqualTo(Map.of("low", new BigDecimal("64900"), "high", new BigDecimal("65100")));
        Dtos.ConfirmWithChecksRequest req = sentToTheDesk();
        assertThat(req.role()).isEqualTo("venue");
        assertThat(req.observedLow()).isEqualByComparingTo("64900");
        assertThat(req.observedHigh()).isEqualByComparingTo("65100");
        assertThat(req.evidence()).isNull();
    }

    @Test
    void venueNoPrintsQuotesAreVerifiedNotDropped() {
        assertThatThrownBy(() -> service.confirm(VENUE, "p1", List.of("no-prints-attested"),
                Map.of("no-prints-attested", Map.of("bestBid", 65100, "bestAsk", 65200))))
                .isInstanceOf(SignerEvidence.Rejected.class)
                .hasMessageContaining("sits outside your quoted 65100 / 65200");
        verify(desk, never()).confirmFixingWithChecks(any(), any());
    }
}
