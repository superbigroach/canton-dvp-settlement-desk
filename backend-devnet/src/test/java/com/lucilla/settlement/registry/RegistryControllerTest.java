package com.lucilla.settlement.registry;

import com.daml.ledger.javaapi.data.Identifier;
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

import static org.hamcrest.Matchers.anEmptyMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer slice test for the off-ledger CIP-56 Registry API, with
 * {@link RegistryService} mocked — so it runs in the default build, with no ledger.
 *
 * <p>What these tests are actually protecting is <b>conformance to somebody else's
 * specification</b>, which is a different risk from the rest of this codebase. The
 * consumer is a third-party wallet generated from the Splice OpenAPI documents; it will
 * not adapt to a path that drifts, a renamed field, or a JSON shape that is nearly right.
 * So the assertions below deliberately pin the things a refactor would silently break:
 *
 * <ul>
 *   <li>the exact paths, including the {@code /registry/...} prefix and the
 *       {@code choice-contexts/<choice>} suffixes;</li>
 *   <li>that {@code choiceContextData} is {@code {"values":{}}} — the Daml JSON encoding
 *       of an empty {@code ChoiceContext} record — and NOT a bare {@code {}}. The caller
 *       assigns it straight into {@code extraArgs.context}, where a bare object fails to
 *       decode. This is the single easiest thing to get subtly wrong;</li>
 *   <li>that the factory response actually carries the DISCLOSURE, since that is the only
 *       reason the endpoint exists;</li>
 *   <li>that errors use the spec's {@code {"error": "..."}} body rather than the desk's
 *       own richer one.</li>
 * </ul>
 */
@WebMvcTest(RegistryController.class)
class RegistryControllerTest {

    private static final String ADMIN = "onRails::1220abcd";

    private static final Identifier REGISTRY_TEMPLATE = new Identifier(
            "b2aa4af53dbf06e12822d2b51bfa82a52c41f27f936b81b8364b62cfe358689c",
            "TokenStandardDvp", "TokenStandardRegistry");

    @Autowired
    MockMvc mvc;

    @MockBean
    RegistryService registry;

    private RegistryService.FactoryContract factory() {
        return new RegistryService.FactoryContract(
                "factory#1", ADMIN, REGISTRY_TEMPLATE, "Y3JlYXRlZEV2ZW50Qmxvbg==",
                "sync::domain1", "canton-dvp-settlement-desk",
                Instant.parse("2026-08-04T09:00:00Z"));
    }

    // ---- metadata ---------------------------------------------------------

    @Test
    void info_reportsTheAdminPartyThatSIGNEDTheFactory_notAConfiguredString() throws Exception {
        when(registry.findFactory()).thenReturn(Optional.of(factory()));

        mvc.perform(get("/registry/metadata/v1/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminId").value(ADMIN))
                // Registry-WIDE apis only, per the metadata spec's own note.
                .andExpect(jsonPath("$.supportedApis['splice-api-token-metadata-v1']").exists())
                .andExpect(jsonPath("$.supportedApis['splice-api-token-holding-v1']")
                        .doesNotExist());
    }

    @Test
    void info_404sWithTheSpecsErrorShapeWhenTheLedgerHoldsNoRegistry() throws Exception {
        when(registry.findFactory()).thenReturn(Optional.empty());

        mvc.perform(get("/registry/metadata/v1/info"))
                .andExpect(status().isNotFound())
                // The spec's ErrorResponse is a single `error` string. The desk's own body
                // puts an HTTP reason phrase there, which would tell a generated client
                // nothing; this endpoint must not use it.
                .andExpect(jsonPath("$.error").isString())
                .andExpect(jsonPath("$.message").doesNotExist());
    }

    @Test
    void instruments_carryTheAggregateSupplyDerivedFromHoldings() throws Exception {
        when(registry.adminParty()).thenReturn(Optional.of(ADMIN));
        when(registry.decimals()).thenReturn(10);
        when(registry.instruments(ADMIN)).thenReturn(List.of(
                new RegistryService.InstrumentRow("cBTC", new BigDecimal("3.00"),
                        new BigDecimal("0.25"), 4, Instant.parse("2026-08-04T09:00:00Z")),
                new RegistryService.InstrumentRow("cETH", new BigDecimal("9.0"),
                        BigDecimal.ZERO, 2, Instant.parse("2026-08-04T09:00:00Z"))));

        mvc.perform(get("/registry/metadata/v1/instruments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instruments[0].id").value("cBTC"))
                // Decimal-encoded as a STRING, per the metadata spec.
                .andExpect(jsonPath("$.instruments[0].totalSupply").value("3"))
                .andExpect(jsonPath("$.instruments[1].id").value("cETH"))
                .andExpect(jsonPath("$.instruments[1].totalSupply").value("9"))
                .andExpect(jsonPath("$.instruments[1].decimals").value(10))
                // Instrument-level apis DO include the interfaces the ledger implements.
                .andExpect(jsonPath(
                        "$.instruments[1].supportedApis['splice-api-token-holding-v1']").exists())
                // One page: no continuation token.
                .andExpect(jsonPath("$.nextPageToken").doesNotExist());
    }

    @Test
    void instruments_pageAndHandBackAResumeToken() throws Exception {
        when(registry.adminParty()).thenReturn(Optional.of(ADMIN));
        when(registry.decimals()).thenReturn(10);
        when(registry.instruments(ADMIN)).thenReturn(List.of(
                new RegistryService.InstrumentRow("cBTC", BigDecimal.ONE, BigDecimal.ZERO, 1,
                        Instant.EPOCH),
                new RegistryService.InstrumentRow("cETH", BigDecimal.ONE, BigDecimal.ZERO, 1,
                        Instant.EPOCH)));

        mvc.perform(get("/registry/metadata/v1/instruments").param("pageSize", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instruments.length()").value(1))
                .andExpect(jsonPath("$.nextPageToken").value("cBTC"));

        mvc.perform(get("/registry/metadata/v1/instruments")
                        .param("pageSize", "1").param("pageToken", "cBTC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instruments[0].id").value("cETH"));
    }

    @Test
    void getInstrument_404sForAnInstrumentNoOneHolds() throws Exception {
        when(registry.adminParty()).thenReturn(Optional.of(ADMIN));
        when(registry.instruments(ADMIN)).thenReturn(List.of());

        mvc.perform(get("/registry/metadata/v1/instruments/cDOGE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").isString());
    }

    // ---- the factory endpoints: the whole point of the service -------------

    @Test
    void transferFactory_handsBackTheFactoryIdAndTheDISCLOSUREThatMakesItUsable()
            throws Exception {
        when(registry.findFactory()).thenReturn(Optional.of(factory()));
        when(registry.instruments(ADMIN)).thenReturn(List.of(
                new RegistryService.InstrumentRow("cETH", BigDecimal.TEN, BigDecimal.ZERO, 1,
                        Instant.EPOCH)));

        mvc.perform(post("/registry/transfer-instruction/v1/transfer-factory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"choiceArguments":{
                                   "expectedAdmin":"%s",
                                   "transfer":{"sender":"Alice","receiver":"Bob","amount":"1.0",
                                               "instrumentId":{"admin":"%s","id":"cETH"}}}}
                                """.formatted(ADMIN, ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.factoryId").value("factory#1"))
                // This registry has no pre-approval and no self-transfer shortcut, so the
                // only honest kind is "offer".
                .andExpect(jsonPath("$.transferKind").value("offer"))
                // THE DISCLOSURE. Without these four fields a wallet cannot exercise the
                // factory at all, because the contract has no observers.
                .andExpect(jsonPath("$.choiceContext.disclosedContracts[0].contractId")
                        .value("factory#1"))
                .andExpect(jsonPath("$.choiceContext.disclosedContracts[0].createdEventBlob")
                        .value("Y3JlYXRlZEV2ZW50Qmxvbg=="))
                .andExpect(jsonPath("$.choiceContext.disclosedContracts[0].synchronizerId")
                        .value("sync::domain1"))
                .andExpect(jsonPath("$.choiceContext.disclosedContracts[0].templateId")
                        .value(REGISTRY_TEMPLATE.getPackageId()
                                + ":TokenStandardDvp:TokenStandardRegistry"));
    }

    /**
     * The shape that is easiest to get wrong and hardest to debug. The caller assigns
     * {@code choiceContextData} into {@code extraArgs.context}, which is decoded as the
     * Daml record {@code ChoiceContext with values : TextMap AnyValue}. An empty context
     * is therefore {@code {"values":{}}}; a bare {@code {}} would be rejected as a record
     * missing a required field, deep inside the caller's submission.
     */
    @Test
    void choiceContextData_isAnEmptyChoiceContextRECORD_notABareObject() throws Exception {
        when(registry.findFactory()).thenReturn(Optional.of(factory()));

        mvc.perform(post("/registry/allocation-instruction/v1/allocation-factory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"choiceArguments\":{}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.factoryId").value("factory#1"))
                .andExpect(jsonPath("$.choiceContext.choiceContextData.values", anEmptyMap()));
    }

    @Test
    void factory_rejectsAnExpectedAdminForADIFFERENTRegistry_beforeTheLedgerDoes()
            throws Exception {
        when(registry.findFactory()).thenReturn(Optional.of(factory()));

        mvc.perform(post("/registry/transfer-instruction/v1/transfer-factory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"choiceArguments":{"expectedAdmin":"someoneElse::1220ffff"}}
                                """))
                .andExpect(status().isBadRequest())
                // The Daml choice asserts the same thing but names no parties; the value
                // of answering here is that the message can.
                .andExpect(jsonPath("$.error").isString());
    }

    @Test
    void excludeDebugFields_omitsThemEntirelyRatherThanNullingThem() throws Exception {
        when(registry.findFactory()).thenReturn(Optional.of(factory()));

        mvc.perform(post("/registry/allocation-instruction/v1/allocation-factory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"choiceArguments\":{},\"excludeDebugFields\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.choiceContext.disclosedContracts[0].createdEventBlob")
                        .exists())
                .andExpect(jsonPath("$.choiceContext.disclosedContracts[0].debugPackageName")
                        .doesNotExist())
                .andExpect(jsonPath("$.choiceContext.disclosedContracts[0].debugPayload")
                        .doesNotExist())
                .andExpect(jsonPath("$.choiceContext.disclosedContracts[0].debugCreatedAt")
                        .doesNotExist());
    }

    // ---- non-factory choice contexts --------------------------------------

    @Test
    void allocationExecuteContext_provesTheAllocationIsLiveAndReturnsAnEmptyContext()
            throws Exception {
        when(registry.adminParty()).thenReturn(Optional.of(ADMIN));
        when(registry.findAllocation(eq(ADMIN), eq("alloc1"))).thenReturn(
                Optional.of(new RegistryService.AllocationRow(
                        "alloc1", "Alice", "Bob", "cETH", new BigDecimal("4.0"),
                        "SettlementDesk", "DVP-CETH-CBTC-001", "holding#7",
                        Instant.parse("2026-08-04T18:00:00Z"))));

        mvc.perform(post("/registry/allocations/v1/alloc1/choice-contexts/execute-transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.choiceContextData.values", anEmptyMap()))
                // Empty ON PURPOSE: executor, sender and receiver are the lock's holders
                // and therefore already observe both the allocation and its locked holding.
                .andExpect(jsonPath("$.disclosedContracts").isArray())
                .andExpect(jsonPath("$.disclosedContracts.length()").value(0));
    }

    @Test
    void allocationContext_404sOnAStaleContractIdRatherThanReturningAContextThatCannotWork()
            throws Exception {
        when(registry.adminParty()).thenReturn(Optional.of(ADMIN));
        when(registry.findAllocation(any(), any())).thenReturn(Optional.empty());

        mvc.perform(post("/registry/allocations/v1/gone1/choice-contexts/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").isString());
    }

    @Test
    void transferInstructionContexts_areServedForAcceptRejectAndWithdraw() throws Exception {
        when(registry.adminParty()).thenReturn(Optional.of(ADMIN));
        when(registry.findTransferOffer(eq(ADMIN), eq("offer1"))).thenReturn(
                Optional.of(new RegistryService.TransferOfferRow(
                        "offer1", "Alice", "Bob", "cETH", new BigDecimal("1.0"), "holding#3",
                        Instant.parse("2026-08-05T09:00:00Z"))));

        for (String choice : List.of("accept", "reject", "withdraw")) {
            mvc.perform(post("/registry/transfer-instruction/v1/offer1/choice-contexts/"
                            + choice)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.choiceContextData.values", anEmptyMap()))
                    .andExpect(jsonPath("$.disclosedContracts.length()").value(0));
        }
    }
}
