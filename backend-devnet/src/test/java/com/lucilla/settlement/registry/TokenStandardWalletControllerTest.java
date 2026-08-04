package com.lucilla.settlement.registry;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer slice test for the wallet side of the token standard — claiming an asset
 * from a foreign registry.
 *
 * <p>The behaviour worth protecting here is not arithmetic, it is <b>what the operator is
 * told</b>. Two things in particular:
 *
 * <ul>
 *   <li>An empty list must not read as "nothing was sent". A pending instruction is
 *       visible only to its own stakeholders and only while it is pending, and the most
 *       likely cause of an empty list is a party id that does not match the one the
 *       faucet was given — so the response carries that sentence.</li>
 *   <li>Accepting with an EMPTY choice context and accepting with the issuing registry's
 *       context are different claims, and the response says which happened. Reporting the
 *       first as though it were the second is the one dishonesty this surface could
 *       plausibly commit.</li>
 * </ul>
 */
@WebMvcTest(TokenStandardWalletController.class)
class TokenStandardWalletControllerTest {

    private static final String ALICE =
            "alice-crossdesk::122003aa7c491e00a453145c4d2cd3dbf5db8908b4e663c9944baed57fd66effa668";
    private static final String BITSAFE = "bitsafe::1220ffff";

    @Autowired
    MockMvc mvc;

    @MockBean
    RegistryService registry;

    @MockBean
    RemoteRegistryClient remote;

    private RegistryService.PendingTransferRow inboundCbtc() {
        return new RegistryService.PendingTransferRow(
                "00cbtc01", BITSAFE + "-faucet", ALICE, BITSAFE, "CBTC",
                new BigDecimal("0.5"), Instant.parse("2026-08-04T09:00:00Z"),
                Instant.parse("2036-08-04T09:00:00Z"),
                "PendingReceiverAcceptance", "inbound", false);
    }

    @Test
    void pending_listsAForeignInstructionAndNamesTheRegistryThatIssuedIt() throws Exception {
        when(registry.resolvePartyReference(ALICE)).thenReturn(ALICE);
        when(registry.pendingTransfers(ALICE)).thenReturn(List.of(inboundCbtc()));
        when(remote.baseUrlFor(BITSAFE)).thenReturn(Optional.empty());

        mvc.perform(get("/api/token-standard/pending").param("party", ALICE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pending[0].instructionCid").value("00cbtc01"))
                .andExpect(jsonPath("$.pending[0].instrumentId").value("CBTC"))
                .andExpect(jsonPath("$.pending[0].direction").value("inbound"))
                .andExpect(jsonPath("$.pending[0].action").value("accept"))
                // NOT ours — that is the whole point of showing this row.
                .andExpect(jsonPath("$.pending[0].ourRegistry").value(false))
                // No registry URL configured: the thing to fix if the accept needs a context.
                .andExpect(jsonPath("$.pending[0].registryUrl").doesNotExist())
                .andExpect(jsonPath("$.pending[0].expired").value(false));
    }

    @Test
    void pending_emptyListCarriesTheReasonItMightBeEmpty() throws Exception {
        when(registry.resolvePartyReference(ALICE)).thenReturn(ALICE);
        when(registry.pendingTransfers(ALICE)).thenReturn(List.of());

        mvc.perform(get("/api/token-standard/pending").param("party", ALICE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pending.length()").value(0))
                // "nothing here" and "you asked about the wrong party" look identical
                // otherwise, and only one of them is worth acting on.
                .andExpect(jsonPath("$.note").isString());
    }

    @Test
    void accept_reportsWhereTheChoiceContextCameFrom() throws Exception {
        when(registry.exerciseTransferInstruction(eq(ALICE), eq("00cbtc01"), eq("accept")))
                .thenReturn(new RegistryService.ExerciseOutcome(
                        "update#1",
                        List.of("BitSafe.Cbtc:CbtcHolding 00newholding"),
                        "https://registry.bitsafe.example", 3, 2));

        mvc.perform(post("/api/token-standard/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"party\":\"" + ALICE + "\",\"instructionCid\":\"00cbtc01\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.choice").value("TransferInstruction_Accept"))
                .andExpect(jsonPath("$.updateId").value("update#1"))
                // The created contract is on the FOREIGN registry's template. That is the
                // evidence the asset is not self-issued.
                .andExpect(jsonPath("$.created[0]")
                        .value("BitSafe.Cbtc:CbtcHolding 00newholding"))
                .andExpect(jsonPath("$.choiceContext.source")
                        .value("https://registry.bitsafe.example"))
                .andExpect(jsonPath("$.choiceContext.disclosedContracts").value(2));
    }

    @Test
    void accept_withoutAConfiguredRegistrySaysSoRatherThanImplyingAContextWasSupplied()
            throws Exception {
        when(registry.exerciseTransferInstruction(any(), any(), eq("accept")))
                .thenReturn(new RegistryService.ExerciseOutcome(
                        "update#2", List.of(),
                        "EMPTY — no registry URL is configured for instrument admin " + BITSAFE,
                        0, 0));

        mvc.perform(post("/api/token-standard/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"party\":\"" + ALICE + "\",\"instructionCid\":\"00cbtc01\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.choiceContext.values").value(0))
                .andExpect(jsonPath("$.choiceContext.disclosedContracts").value(0))
                .andExpect(jsonPath("$.choiceContext.source").isString());
    }

    @Test
    void aForeignRegistryThatCannotBeReachedIsA502_notALedgerFailure() throws Exception {
        when(registry.exerciseTransferInstruction(any(), any(), any()))
                .thenThrow(new RemoteRegistryClient.RemoteRegistryException(
                        "the registry at https://registry.bitsafe.example could not be reached"));

        mvc.perform(post("/api/token-standard/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"party\":\"" + ALICE + "\",\"instructionCid\":\"00cbtc01\"}"))
                // Not 422 and not 500: nothing reached the ledger, and the fault is
                // neither the participant's nor this desk's.
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.hint").isString());
    }

    @Test
    void actingAsTheWrongPartyIsA400ThatNamesTheControllerTheStandardRequires()
            throws Exception {
        when(registry.exerciseTransferInstruction(any(), any(), eq("accept")))
                .thenThrow(new IllegalArgumentException(
                        "the standard makes the RECEIVER the controller of "
                                + "TransferInstruction_Accept, so this must be submitted as alice"));

        mvc.perform(post("/api/token-standard/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"party\":\"Bob\",\"instructionCid\":\"00cbtc01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isString());
    }
}
