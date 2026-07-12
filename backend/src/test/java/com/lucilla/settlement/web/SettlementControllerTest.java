package com.lucilla.settlement.web;

import com.lucilla.settlement.ledger.LedgerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
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

    @Test
    void issueHolding_returns201WithContractId() throws Exception {
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
}
