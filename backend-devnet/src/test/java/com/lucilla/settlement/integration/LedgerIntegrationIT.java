package com.lucilla.settlement.integration;

import com.daml.ledger.javaapi.data.TransactionTree;
import com.lucilla.settlement.ledger.LedgerCommands;
import com.lucilla.settlement.ledger.LedgerService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end DvP against a REAL running ledger, exercising the full binding path:
 * connect -> issue holdings -> propose -> accept -> settle -> query.
 *
 * <p>This is the documented integration boundary of {@link LedgerService}. It is
 * <b>excluded from {@code ./gradlew build}</b> (tag {@code integration}) and only
 * runs when {@code LEDGER_IT=1}, so the normal build never needs a ledger. Start
 * one first — see {@code run-local.md} — then:
 * <pre>
 *   # terminal 1: daml sandbox with the DAR + parties allocated
 *   # then, with the allocated party ids exported:
 *   LEDGER_IT=1 IT_ISSUER=... IT_ALICE=... IT_BOB=... IT_AUDITOR=... \
 *     ./gradlew integrationTest
 * </pre>
 * Party ids come from env because on Canton an allocated party carries a
 * namespace suffix (e.g. {@code Alice::1220ab...}); defaults match a plain
 * sandbox where the hint is the id.
 */
@Tag("integration")
@EnabledIfEnvironmentVariable(named = "LEDGER_IT", matches = "1")
@SpringBootTest
class LedgerIntegrationIT {

    @Autowired
    LedgerService ledger;

    private static String party(String env, String fallback) {
        String v = System.getenv(env);
        return (v == null || v.isBlank()) ? fallback : v;
    }

    @Test
    void bilateralDvP_movesBothLegsAtomically() {
        String issuer = party("IT_ISSUER", "Issuer");
        String alice = party("IT_ALICE", "Alice");
        String bob = party("IT_BOB", "Bob");
        String auditor = party("IT_AUDITOR", "Auditor");

        // Bob (seller) gets 10 AAPL; Alice (buyer) gets 2,550 USD.
        String bobAapl = ledger.submitForCreated(issuer,
                LedgerCommands.createHolding(issuer, "DEMO:AAPL", bob, new BigDecimal("10.0")),
                LedgerCommands.holdingTemplateId());
        String aliceCash = ledger.submitForCreated(issuer,
                LedgerCommands.createHolding(issuer, "USD", alice, new BigDecimal("2550.0")),
                LedgerCommands.holdingTemplateId());

        // Bob proposes to sell 10 AAPL for 2,550 USD; Alice accepts; Bob settles.
        String proposal = ledger.submitForCreated(bob,
                LedgerCommands.createDvPProposal(bob, alice, auditor, bobAapl, aliceCash,
                        "DEMO:AAPL", new BigDecimal("10.0"), "USD", new BigDecimal("2550.0")),
                LedgerCommands.dvpProposalTemplateId());
        String agreement = ledger.submitForCreated(alice,
                LedgerCommands.acceptProposal(proposal), LedgerCommands.dvpAgreementTemplateId());
        TransactionTree settleTree = ledger.submit(bob, LedgerCommands.settleAgreement(agreement));

        // A settlement receipt was written in the settle transaction.
        List<String> receipts = ledger.createdOf(settleTree, LedgerCommands.settlementReceiptTemplateId());
        assertThat(receipts).hasSize(1);

        // Atomic swap: Alice now owns 10 AAPL, Bob now owns 2,550 USD.
        BigDecimal aliceAapl = owned(alice, "DEMO:AAPL");
        BigDecimal bobUsd = owned(bob, "USD");
        assertThat(aliceAapl).isEqualByComparingTo("10.0");
        assertThat(bobUsd).isEqualByComparingTo("2550.0");
    }

    private BigDecimal owned(String party, String instrument) {
        return ledger.holdingsVisibleTo(party).stream()
                .filter(h -> h.owner().equals(party) && h.instrumentId().equals(instrument))
                .map(LedgerService.HoldingView::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
