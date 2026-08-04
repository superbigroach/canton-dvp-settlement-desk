package com.lucilla.settlement.web;

import com.lucilla.settlement.ledger.LedgerService;
import com.lucilla.settlement.ledger.MarketData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer slice test: real controller + JSON (de)serialization + bean
 * validation, with {@link LedgerService} MOCKED. Verifies request routing,
 * actAs derivation, and response shaping WITHOUT a ledger — so it runs in the
 * default build.
 */
@WebMvcTest(SettlementController.class)
class SettlementControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    LedgerService ledger;

    // The controller reads CANDIDATE marks from an outside feed. Mocked here so the
    // slice never touches the network: these tests are about routing and validation,
    // and a unit test that depends on a live price endpoint is a flaky test.
    @MockBean
    MarketData marketData;

    @Test
    void issueHolding_returns201WithContractId() throws Exception {
        // DEVNET: this build resolves BOTH the issuer and the owner label to a full
        // party id before building the command (on a real node an actAs claim only
        // matches the full "label::namespace" id). The mock must therefore answer
        // resolveParty, or the controller would submit as null and match nothing.
        when(ledger.resolveParty("Issuer")).thenReturn("Issuer");
        when(ledger.resolveParty("Alice")).thenReturn("Alice");
        when(ledger.submitForCreated(eq("Issuer"), any(), any())).thenReturn("holding#1");

        mvc.perform(post("/api/holdings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"issuer":"Issuer","instrumentId":"USD","owner":"Alice","amount":2550.0}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contractId").value("holding#1"));
    }

    @Test
    void issueHolding_rejectsNonPositiveAmountWith400() throws Exception {
        mvc.perform(post("/api/holdings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"issuer":"Issuer","instrumentId":"USD","owner":"Alice","amount":-5}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getHoldings_returnsVisibleHoldings() throws Exception {
        when(ledger.resolveParty("Alice")).thenReturn("Alice");
        when(ledger.holdingsVisibleTo("Alice")).thenReturn(List.of(
                new LedgerService.HoldingView(
                        "h#1", "Issuer", "USD", "Alice", new BigDecimal("50.0"), List.of())));

        mvc.perform(get("/api/holdings").param("party", "Alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].owner").value("Alice"))
                .andExpect(jsonPath("$[0].instrumentId").value("USD"));
    }

    @Test
    void accept_usesCounterpartyAsActAs() throws Exception {
        when(ledger.submitForCreated(eq("Alice"), any(), any())).thenReturn("agreement#1");

        mvc.perform(post("/api/dvp/proposal%231/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"counterparty":"Alice"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contractId").value("agreement#1"));
    }

    @Test
    void ledgerRejection_becomes422WithMessage() throws Exception {
        when(ledger.submitForCreated(any(), any(), any()))
                .thenThrow(new LedgerService.LedgerException("asset amount mismatch"));

        mvc.perform(post("/api/holdings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"issuer":"Issuer","instrumentId":"USD","owner":"Alice","amount":2550.0}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("asset amount mismatch"));
    }
    // ---- Continuous accrual ------------------------------------------------

    @Test
    void proposeAccruingFixing_echoesTheAttestedRecipe() throws Exception {
        when(ledger.resolveParty("Issuer")).thenReturn("Issuer");
        when(ledger.submitForCreated(eq("Issuer"), any(), any())).thenReturn("proposal#1");

        mvc.perform(post("/api/committee/committee%231/propose-accruing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"proposer":"Issuer","instrumentId":"cETH","session":"Close",
                                 "price":2400.0,"ratePerAnnum":0.036,"dayCount":"ACT/360",
                                 "accrualFrom":"2026-08-05T14:00:00Z"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contractId").value("proposal#1"))
                .andExpect(jsonPath("$.accruing").value(true))
                .andExpect(jsonPath("$.dayCount").value("ACT/360"))
                // Decimals cross the wire as STRINGS at the ledger's own 10dp scale, so
                // the browser can re-parse them into exact fixed-point rather than float.
                .andExpect(jsonPath("$.basePrice").value("2400.0000000000"))
                .andExpect(jsonPath("$.ratePerAnnum").value("0.0360000000"))
                .andExpect(jsonPath("$.accrualFromEpochMicros").value(1785938400000000L));
    }

    @Test
    void proposeAccruingFixing_rejectsAnUnsupportedDayCountWith400_notALedgerAbort() throws Exception {
        // Governance.daml refuses 30/360 and ACT/ACT at proposal rather than defaulting
        // one. The desk mirrors that check BEFORE submitting, so an operator's typo is a
        // form rejection with the supported list rather than a 422 from the ledger.
        when(ledger.resolveParty("Issuer")).thenReturn("Issuer");

        mvc.perform(post("/api/committee/committee%231/propose-accruing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"proposer":"Issuer","instrumentId":"GILT","session":"Close",
                                 "price":100.0,"ratePerAnnum":0.04,"dayCount":"30/360"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("unsupported day-count convention")));

        mvc.perform(post("/api/committee/committee%231/propose-accruing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"proposer":"Issuer","instrumentId":"UST","session":"Close",
                                 "price":100.0,"ratePerAnnum":0.04,"dayCount":"ACT/ACT"}
                                """))
                .andExpect(status().isBadRequest());

        // Nothing was ever sent to the ledger.
        org.mockito.Mockito.verify(ledger, org.mockito.Mockito.never())
                .submitForCreated(any(), any(), any());
    }

    @Test
    void proposeAccruingFixing_refusesARateAtOrBelowMinus100Percent() throws Exception {
        when(ledger.resolveParty("Issuer")).thenReturn("Issuer");

        mvc.perform(post("/api/committee/committee%231/propose-accruing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"proposer":"Issuer","instrumentId":"cETH","session":"Close",
                                 "price":100.0,"ratePerAnnum":-1.5,"dayCount":"ACT/360"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("-100%")));
    }

    @Test
    void accruedNav_showsItsWorkingAndTheAuctionBinding() throws Exception {
        // A fix struck at 2,400 accruing 3.6% ACT/360 from a known instant, valued a day
        // later: the ledger's own vector is 2,402.40 after ten days, 2,400.24 after one.
        when(ledger.resolveParty("Auditor")).thenReturn("Auditor");
        when(ledger.navFixingById("Auditor", "fix-1")).thenReturn(
                new LedgerService.NavFixingView(
                        "fix-1", List.of("Issuer::ns", "Bank::ns"), 2L,
                        "cETH", "USDC", "Close",
                        new BigDecimal("2400.0"), "sealed-cross VWAP",
                        new BigDecimal("0.036"), "ACT/360",
                        Instant.parse("2026-08-05T14:00:00Z"),
                        List.of(), Instant.parse("2026-08-05T14:07:00Z")));
        when(ledger.auctionsVisibleTo("Auditor")).thenReturn(List.of());

        mvc.perform(get("/api/fixing/fix-1/nav").param("at", "2026-08-06T14:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.navNow").value("2400.2400000000"))
                .andExpect(jsonPath("$.accrued").value("0.2400000000"))
                .andExpect(jsonPath("$.perDay").value("0.2400000000"))
                .andExpect(jsonPath("$.basePrice").value("2400.0000000000"))
                .andExpect(jsonPath("$.dayCount").value("ACT/360"))
                .andExpect(jsonPath("$.accruing").value(true))
                .andExpect(jsonPath("$.elapsedMicros").value(86_400_000_000L))
                .andExpect(jsonPath("$.yearMicros").value(31_104_000_000_000L))
                .andExpect(jsonPath("$.attestors[0]").value("Issuer"))
                // No live auction: the binding is reported as UNJUDGED, never as "fine".
                .andExpect(jsonPath("$.anchorNote").value(
                        org.hamcrest.Matchers.containsString("no live auction")));
    }

    @Test
    void accruedNav_judgesASuppliedAnchorWithTheLedgersOwnBand() throws Exception {
        when(ledger.resolveParty("Auditor")).thenReturn("Auditor");
        when(ledger.navFixingById(eq("Auditor"), eq("fix-1"))).thenReturn(
                new LedgerService.NavFixingView(
                        "fix-1", List.of("Issuer::ns", "Bank::ns"), 2L,
                        "cETH", "USDC", "Close",
                        new BigDecimal("2400.0"), "",
                        new BigDecimal("0.036"), "ACT/360",
                        Instant.parse("2026-08-05T14:00:00Z"),
                        List.of(), Instant.parse("2026-08-05T14:00:00Z")));

        // One day behind at 3.6% ACT/360 is EXACTLY the 1bp budget — RunClose accepts it.
        mvc.perform(get("/api/fixing/fix-1/nav")
                        .param("at", "2026-08-06T14:00:00Z").param("anchor", "2400.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anchorConsistent").value(true))
                .andExpect(jsonPath("$.anchorDrift").value("0.2400000000"))
                .andExpect(jsonPath("$.staleBudget").value("0.2400240000"));

        // TWO days behind is past it — stale, and RunClose refuses.
        mvc.perform(get("/api/fixing/fix-1/nav")
                        .param("at", "2026-08-07T14:00:00Z").param("anchor", "2400.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anchorConsistent").value(false));

        // An anchor AHEAD of the accrual is rejected however small the excess: that is
        // not staleness, it is value the fund has not earned.
        mvc.perform(get("/api/fixing/fix-1/nav")
                        .param("at", "2026-08-06T14:00:00Z").param("anchor", "2400.25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anchorConsistent").value(false));
    }
}
