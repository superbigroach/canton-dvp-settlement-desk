package com.lucilla.settlement.web;

import com.lucilla.settlement.events.LifecycleEvent;
import com.lucilla.settlement.ledger.LedgerService;
import com.lucilla.settlement.ledger.MarketData;
import com.lucilla.settlement.ledger.SignerEvidence;
import com.lucilla.settlement.ledger.SignerProtocol;
import com.lucilla.settlement.ledger.StrikeCalendars;
import com.lucilla.settlement.scheduler.ScheduleStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@code POST /api/fixing/{cid}/confirm-checked} — {@link SettlementController#confirmFixingWithChecks}
 * — per seat: a seat that must bring numbers may not tick; the venue's evidence is its
 * range; the issuer is held to the reserve model of the proposal's instrument.
 *
 * <p>The controller is built directly with a mocked {@link LedgerService} (the same
 * stubbing the web slice uses), so the refusal is observed as the exception the API
 * handler turns into 400 ({@link IllegalArgumentException}) or 422
 * ({@link SignerEvidence.Rejected}), and "accepted" means the confirm command reached
 * {@code submitForCreated}.
 */
class ConfirmWithChecksSeatTest {

    static final Instant STRIKE = Instant.parse("2026-09-22T15:00:00Z");

    LedgerService ledger = mock(LedgerService.class);
    ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    SettlementController desk;
    Map<String, SignerProtocol.ReserveModel> saved;

    static LedgerService.FixingProposalView proposal(String cid, String instrument) {
        return new LedgerService.FixingProposalView(cid, "Issuer",
                List.of("Issuer", "Bank", "Venue", "Custodian", "TA"), 2, "Auditor", "Issuer",
                instrument, "USDC", "Close", new BigDecimal("65000"), "scheduled",
                BigDecimal.ZERO, "ACT/365", STRIKE, List.of("Issuer"),
                new BigDecimal("65000"), BigDecimal.ONE, List.of(), "1", LocalDate.of(2026, 9, 22));
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        saved = SignerProtocol.reserveModels();
        SignerProtocol.configureReserveModels(Map.of(
                "CBTC", "attested", "cETH", "onchain-verifiable", "TEQ", "custodial"));
        ObjectProvider<ScheduleStore> schedules = mock(ObjectProvider.class);
        ObjectProvider<StrikeCalendars> calendars = mock(ObjectProvider.class);
        desk = new SettlementController(ledger, mock(MarketData.class), events, schedules, calendars);
        when(ledger.resolveParty(anyString())).thenAnswer(inv -> inv.getArgument(0));
        when(ledger.fixingProposalsVisibleTo(anyString())).thenReturn(List.of(
                proposal("p-btc", "CBTC"), proposal("p-eth", "cETH"), proposal("p-teq", "TEQ")));
        when(ledger.submitForCreated(anyString(), any(), any())).thenReturn("p-next");
    }

    @AfterEach
    void restore() {
        Map<String, String> back = new LinkedHashMap<>();
        saved.forEach((k, v) -> back.put(k, v.wire()));
        SignerProtocol.configureReserveModels(back);
    }

    static Dtos.ConfirmWithChecksRequest req(String member, String role, List<String> checks,
            BigDecimal low, BigDecimal high, Map<String, Object> evidence) {
        return new Dtos.ConfirmWithChecksRequest(member, role, null, checks, low, high, evidence, null, null);
    }

    static BigDecimal d(String s) {
        return new BigDecimal(s);
    }

    static String party(String seat) {
        return switch (seat) {
            case "lender" -> "Bank";
            case "venue" -> "Venue";
            case "custodian" -> "Custodian";
            case "transfer-agent" -> "TA";
            default -> "Issuer";
        };
    }

    /** What the confirm recorded on the lifecycle event. */
    Map<String, Object> recorded() {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(captor.capture());
        LifecycleEvent e = (LifecycleEvent) captor.getValue();
        return e.extra();
    }

    void neverReachedTheLedger() {
        verify(ledger, never()).submitForCreated(anyString(), any(), any());
        verify(events, never()).publishEvent(any(Object.class));
    }

    static Map<String, Object> issuerGood() {
        return Map.of(
                "attestor-quorum", Map.of("quorumSigners", 7, "quorumThreshold", 7),
                "reserves-current", Map.of("reservesAsOf", Instant.now().minusSeconds(3600).toString()),
                "reserves-cover-supply", Map.of("reserves", 1000, "supply", 1000),
                "redemption-queue-clear", Map.of("queueDepth", 0, "maxQueueDepth", 10));
    }

    static Map<String, Object> custodianGood() {
        return Map.of(
                "holdings-current", Map.of("statementAsOf", Instant.now().minusSeconds(3600).toString()),
                "holdings-cover-supply", Map.of("holdings", 1000, "supply", 1000),
                "no-encumbrance", Map.of("encumbered", 0));
    }

    // ---- bare ticks --------------------------------------------------------------------

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"issuer", "lender", "custodian", "transfer-agent"})
    @DisplayName("a seat that must bring numbers may not tick: no evidence block is a 400 before anything is submitted")
    void aSeatThatMustBringNumbersMayNotTick(String seat) {
        List<String> own = SignerProtocol.role(seat).conditions().stream().map(SignerProtocol.Condition::name).toList();
        assertThatThrownBy(() -> desk.confirmFixingWithChecks("p-btc", req(party(seat), seat, own, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("the " + seat + " seat must supply evidence for every condition it claims; "
                        + "a bare tick is not an attestation");
        neverReachedTheLedger();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"issuer", "lender", "custodian", "transfer-agent"})
    @DisplayName("an empty evidence block is not a tick either: every claimed condition is refused as missing, with the schema")
    void anEmptyEvidenceBlockIsRefusedWithTheSchema(String seat) {
        List<String> own = SignerProtocol.role(seat).conditions().stream().map(SignerProtocol.Condition::name).toList();
        assertThatThrownBy(() -> desk.confirmFixingWithChecks("p-btc", req(party(seat), seat, own, null, null, Map.of())))
                .isInstanceOf(SignerEvidence.Rejected.class)
                .hasMessageStartingWith("evidence refused for the " + seat + " seat: ")
                .satisfies(e -> {
                    SignerEvidence.Rejected r = (SignerEvidence.Rejected) e;
                    assertThat(r.seat()).isEqualTo(seat);
                    assertThat(r.problems()).hasSize(own.size());
                    for (int i = 0; i < own.size(); i++) {
                        assertThat(r.problems().get(i)).startsWith(own.get(i) + ": evidence missing — supply [");
                    }
                    assertThat(r.schema()).containsOnlyKeys(own.toArray(new String[0]));
                });
        neverReachedTheLedger();
    }

    @Test
    @DisplayName("the operator's conditions carry no evidence, so its tick is accepted and recorded as unverified")
    void theOperatorTicks() {
        ResponseEntity<Dtos.CidResponse> resp = desk.confirmFixingWithChecks("p-btc",
                req("Issuer", "operator", List.of("inputs-published", "composition-reconciled"), null, null, null));
        assertThat(resp.getStatusCode().value()).isEqualTo(201);
        assertThat(resp.getBody().contractId()).isEqualTo("p-next");
        Map<String, Object> extra = recorded();
        assertThat(extra).containsEntry("protocolRef", "SIGNER_PROTOCOL v2 operator");
        assertThat(extra).doesNotContainEntry("verified", true);
    }

    // ---- the venue ---------------------------------------------------------------------

    @Test
    @DisplayName("a venue with only a range is accepted: the range is its evidence and the ledger checks it")
    void aVenueWithOnlyARangeIsAccepted() {
        ResponseEntity<Dtos.CidResponse> resp = desk.confirmFixingWithChecks("p-btc",
                req("Venue", "venue", List.of("traded-range"), d("64900"), d("65100"), null));
        assertThat(resp.getStatusCode().value()).isEqualTo(201);
        assertThat(resp.getBody().contractId()).isEqualTo("p-next");
        verify(ledger).submitForCreated(eq("Venue"), any(), any());
        Map<String, Object> extra = recorded();
        assertThat(extra).containsEntry("observedLow", "64900").containsEntry("observedHigh", "65100")
                .containsEntry("protocolRef", "SIGNER_PROTOCOL v2 venue");
        assertThat(extra).doesNotContainEntry("verified", true);
        assertThat(extra).doesNotContainKey("evidence");
    }

    @Test
    @DisplayName("a venue with half a range, or none and no no-prints claim, is refused before the ledger")
    void aVenueWithoutAFullRangeIsRefused() {
        for (BigDecimal[] half : new BigDecimal[][] {{d("64900"), null}, {null, d("65100")}, {null, null}}) {
            assertThatThrownBy(() -> desk.confirmFixingWithChecks("p-btc",
                    req("Venue", "venue", List.of("traded-range"), half[0], half[1], null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageStartingWith("the venue seat must supply both observedLow and observedHigh");
        }
        neverReachedTheLedger();
    }

    @Test
    @DisplayName("a venue attesting no prints, with quotes that bracket the proposal, is verified server-side")
    void aVenueAttestingNoPrintsWithQuotesIsVerified() {
        ResponseEntity<Dtos.CidResponse> resp = desk.confirmFixingWithChecks("p-btc",
                req("Venue", "venue", List.of("no-prints-attested"), null, null,
                        Map.of("no-prints-attested", Map.of("bestBid", 64900, "bestAsk", 65100))));
        assertThat(resp.getStatusCode().value()).isEqualTo(201);
        Map<String, Object> extra = recorded();
        assertThat(extra).containsEntry("verified", true).containsEntry("verifiedBy", "server");
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> ev = (Map<String, Map<String, Object>>) extra.get("evidence");
        assertThat(ev).containsOnlyKeys("no-prints-attested");
        assertThat(ev.get("no-prints-attested").get("quoted")).isEqualTo(BigDecimal.ONE);
    }

    @Test
    @DisplayName("a venue whose quotes contradict the proposal is refused with 422 semantics naming the numbers")
    void aVenueWhoseQuotesContradictTheProposalIsRefused() {
        assertThatThrownBy(() -> desk.confirmFixingWithChecks("p-btc",
                req("Venue", "venue", List.of("no-prints-attested"), null, null,
                        Map.of("no-prints-attested", Map.of("bestBid", 65100, "bestAsk", 65200)))))
                .isInstanceOf(SignerEvidence.Rejected.class)
                .hasMessage("evidence refused for the venue seat: no-prints-attested: the proposal 65000 "
                        + "sits outside your quoted 65100 / 65200")
                .satisfies(e -> {
                    SignerEvidence.Rejected r = (SignerEvidence.Rejected) e;
                    assertThat(r.seat()).isEqualTo("venue");
                    assertThat(r.schema()).containsOnlyKeys("no-prints-attested");
                });
        neverReachedTheLedger();
    }

    @Test
    @DisplayName("a venue claiming both prints and no prints is refused")
    void aVenueCannotClaimBothPrintsAndNoPrints() {
        assertThatThrownBy(() -> desk.confirmFixingWithChecks("p-btc",
                req("Venue", "venue", List.of("traded-range", "no-prints-attested"), d("64900"), d("65100"), null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("a venue cannot claim both 'traded-range' and 'no-prints-attested'");
        neverReachedTheLedger();
    }

    @Test
    void aVenueMayNotTickNoPrintsAttestedWithoutItsQuotes() {
        assertThatThrownBy(() -> desk.confirmFixingWithChecks("p-btc",
                req("Venue", "venue", List.of("no-prints-attested"), null, null, null)))
                .isInstanceOf(RuntimeException.class);
        neverReachedTheLedger();
    }

    // ---- the issuer under each reserve model ----------------------------------------------

    @Test
    @DisplayName("an issuer under onchain-verifiable or custodial asked for attestor-quorum is refused as not its condition under this model")
    void issuerUnderNonAttestedModelCannotClaimQuorum() {
        for (String cid : List.of("p-eth", "p-teq")) {
            assertThatThrownBy(() -> desk.confirmFixingWithChecks(cid,
                    req("Issuer", "issuer", List.of("attestor-quorum"), null, null, issuerGood())))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("condition 'attestor-quorum' is not one the issuer seat verifies; expected any of "
                            + "[reserves-current, reserves-cover-supply, redemption-queue-clear]");
        }
        neverReachedTheLedger();
    }

    @Test
    @DisplayName("an issuer under attested may claim the quorum, and is verified")
    void issuerUnderAttestedClaimsTheQuorum() {
        ResponseEntity<Dtos.CidResponse> resp = desk.confirmFixingWithChecks("p-btc",
                req("Issuer", "issuer", List.of("attestor-quorum", "reserves-current",
                        "reserves-cover-supply", "redemption-queue-clear"), null, null, issuerGood()));
        assertThat(resp.getStatusCode().value()).isEqualTo(201);
        Map<String, Object> extra = recorded();
        assertThat(extra).containsEntry("verified", true).containsEntry("verifiedBy", "server");
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> ev = (Map<String, Map<String, Object>>) extra.get("evidence");
        assertThat(ev).containsOnlyKeys("attestor-quorum", "reserves-current",
                "reserves-cover-supply", "redemption-queue-clear");
    }

    @Test
    @DisplayName("an issuer under onchain-verifiable with the three conditions its model can prove is verified")
    void issuerUnderOnchainVerifiableWithItsThreeConditionsIsVerified() {
        ResponseEntity<Dtos.CidResponse> resp = desk.confirmFixingWithChecks("p-eth",
                req("Issuer", "issuer", List.of("reserves-current", "reserves-cover-supply", "redemption-queue-clear"),
                        null, null, issuerGood()));
        assertThat(resp.getStatusCode().value()).isEqualTo(201);
        verify(ledger).submitForCreated(eq("Issuer"), any(), any());
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> ev = (Map<String, Map<String, Object>>) recorded().get("evidence");
        assertThat(ev).containsOnlyKeys("reserves-current", "reserves-cover-supply", "redemption-queue-clear");
    }

    @Test
    @DisplayName("issuer numbers that fail are refused with every problem and the schema of the claimed conditions")
    void issuerNumbersThatFailAreRefused() {
        Map<String, Object> bad = Map.of(
                "attestor-quorum", Map.of("quorumSigners", 6, "quorumThreshold", 7),
                "reserves-cover-supply", Map.of("reserves", 999, "supply", 1000),
                "redemption-queue-clear", Map.of("queueDepth", 11, "maxQueueDepth", 10));
        assertThatThrownBy(() -> desk.confirmFixingWithChecks("p-btc",
                req("Issuer", "issuer", List.of("attestor-quorum", "reserves-cover-supply", "redemption-queue-clear"),
                        null, null, bad)))
                .isInstanceOf(SignerEvidence.Rejected.class)
                .satisfies(e -> {
                    SignerEvidence.Rejected r = (SignerEvidence.Rejected) e;
                    assertThat(r.problems()).containsExactly(
                            "attestor-quorum: quorumSigners 6 is below quorumThreshold 7",
                            "reserves-cover-supply: reserves 999 do not cover supply 1000",
                            "redemption-queue-clear: queueDepth 11 exceeds maxQueueDepth 10");
                    assertThat(r.schema()).containsOnlyKeys("attestor-quorum", "reserves-cover-supply", "redemption-queue-clear");
                });
        neverReachedTheLedger();
    }

    // ---- custodian and transfer agent ------------------------------------------------------

    @Test
    @DisplayName("a custodian with its numbers is verified and confirmed; with an encumbrance it is refused by the amount")
    void custodianIsVerifiedOrRefusedByItsNumbers() {
        ResponseEntity<Dtos.CidResponse> resp = desk.confirmFixingWithChecks("p-teq",
                req("Custodian", "custodian", List.of("holdings-current", "holdings-cover-supply", "no-encumbrance"),
                        null, null, custodianGood()));
        assertThat(resp.getStatusCode().value()).isEqualTo(201);
        verify(ledger).submitForCreated(eq("Custodian"), any(), any());
        Map<String, Object> extra = recorded();
        assertThat(extra).containsEntry("verified", true).containsEntry("protocolRef", "SIGNER_PROTOCOL v2 custodian");
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> ev = (Map<String, Map<String, Object>>) extra.get("evidence");
        assertThat(ev).containsOnlyKeys("holdings-current", "holdings-cover-supply", "no-encumbrance");
        assertThat(ev.get("holdings-current")).containsKey("ageHours");

        Map<String, Object> encumbered = new LinkedHashMap<>(custodianGood());
        encumbered.put("no-encumbrance", Map.of("encumbered", 5));
        assertThatThrownBy(() -> desk.confirmFixingWithChecks("p-teq",
                req("Custodian", "custodian", List.of("holdings-current", "holdings-cover-supply", "no-encumbrance"),
                        null, null, encumbered)))
                .isInstanceOf(SignerEvidence.Rejected.class)
                .hasMessage("evidence refused for the custodian seat: no-encumbrance: 5 units are pledged, lent or encumbered");
    }

    @Test
    @DisplayName("a transfer agent with a reconciled register is verified; a mismatch is refused naming both counts")
    void transferAgentIsVerifiedOrRefusedByItsNumbers() {
        ResponseEntity<Dtos.CidResponse> resp = desk.confirmFixingWithChecks("p-teq",
                req("TA", "transfer-agent", List.of("shares-outstanding-reconciled", "fees-accrued"), null, null,
                        Map.of("shares-outstanding-reconciled", Map.of("registerShares", 5000, "ledgerShares", 5000),
                                "fees-accrued", Map.of("accruedFees", "12.5"))));
        assertThat(resp.getStatusCode().value()).isEqualTo(201);
        verify(ledger).submitForCreated(eq("TA"), any(), any());
        assertThat(recorded()).containsEntry("verified", true).containsEntry("protocolRef", "SIGNER_PROTOCOL v2 transfer-agent");

        assertThatThrownBy(() -> desk.confirmFixingWithChecks("p-teq",
                req("TA", "transfer-agent", List.of("shares-outstanding-reconciled"), null, null,
                        Map.of("shares-outstanding-reconciled", Map.of("registerShares", 5000, "ledgerShares", 5001)))))
                .isInstanceOf(SignerEvidence.Rejected.class)
                .hasMessage("evidence refused for the transfer-agent seat: shares-outstanding-reconciled: "
                        + "register shows 5000 but the ledger shows 5001");
    }

    @Test
    @DisplayName("a custodian or transfer agent sending an observed range is refused: only the venue supplies one")
    void onlyTheVenueSuppliesARange() {
        assertThatThrownBy(() -> desk.confirmFixingWithChecks("p-teq",
                req("Custodian", "custodian", List.of("no-encumbrance"), d("64900"), d("65100"), custodianGood())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("only the venue seat supplies an observed range");
        assertThatThrownBy(() -> desk.confirmFixingWithChecks("p-teq",
                req("TA", "transfer-agent", List.of("fees-accrued"), null, d("65100"),
                        Map.of("fees-accrued", Map.of("accruedFees", 0)))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("only the venue seat supplies an observed range");
        neverReachedTheLedger();
    }

    @Test
    @DisplayName("a seat claiming another seat's condition, or an unknown seat, is refused by name before the ledger")
    void crossSeatAndUnknownSeat() {
        assertThatThrownBy(() -> desk.confirmFixingWithChecks("p-teq",
                req("Custodian", "custodian", List.of("shares-outstanding-reconciled"), null, null,
                        Map.of("shares-outstanding-reconciled", Map.of("registerShares", 1, "ledgerShares", 1)))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("condition 'shares-outstanding-reconciled' is not one the custodian seat verifies; "
                        + "expected any of [holdings-current, holdings-cover-supply, no-encumbrance]");
        assertThatThrownBy(() -> desk.confirmFixingWithChecks("p-btc",
                req("Auditor", "auditor", List.of("book-acceptance"), null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("unknown signer role 'auditor'; expected one of "
                        + "[issuer, lender, venue, custodian, transfer-agent, operator]");
        neverReachedTheLedger();
    }
}
