package com.lucilla.settlement.registry;

import com.daml.ledger.javaapi.data.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The <b>off-ledger CIP-56 Registry API</b> — the half of the token standard this project
 * was missing.
 *
 * <p><b>The gap this closes, stated exactly.</b> {@code daml/TokenStandardDvp.daml}
 * implements six official interfaces, so cETH and cBTC are genuine token-standard
 * instruments on the ledger. But a wallet cannot reach them. The standard's transfer and
 * allocation flows are both exercised on a FACTORY contract
 * ({@code TokenStandardRegistry}), and that contract is signed by the registry admin and
 * observed by nobody — invisible on every other party's stream. A wallet therefore cannot
 * learn the factory's contract id, and even if it were told the id, its participant would
 * reject the command because the contract is not visible to the submitting party. The
 * only way through is an <i>explicitly disclosed contract</i>: the registry hands the
 * caller the participant-authenticated {@code createdEventBlob}, the caller passes it
 * with the command, and the command becomes submittable. Serving that blob over HTTP is
 * what a registry is <b>for</b>, and it is what these endpoints do.
 *
 * <p>Until now the project's Daml tests obtained the same blob with {@code queryDisclosure}
 * from a script. Same mechanism, no service. Now there is a service.
 *
 * <p><b>Paths and payloads are the specification's, verbatim</b> — transcribed from the
 * OpenAPI documents shipped in the Splice release, not invented:
 * <pre>
 *   GET  /registry/metadata/v1/info
 *   GET  /registry/metadata/v1/instruments
 *   GET  /registry/metadata/v1/instruments/{instrumentId}
 *   POST /registry/transfer-instruction/v1/transfer-factory
 *   POST /registry/transfer-instruction/v1/{transferInstructionId}/choice-contexts/accept
 *   POST /registry/transfer-instruction/v1/{transferInstructionId}/choice-contexts/reject
 *   POST /registry/transfer-instruction/v1/{transferInstructionId}/choice-contexts/withdraw
 *   POST /registry/allocation-instruction/v1/allocation-factory
 *   POST /registry/allocations/v1/{allocationId}/choice-contexts/execute-transfer
 *   POST /registry/allocations/v1/{allocationId}/choice-contexts/withdraw
 *   POST /registry/allocations/v1/{allocationId}/choice-contexts/cancel
 * </pre>
 *
 * <p><b>What this is NOT.</b> It is not full CIP-56 registry compliance and is not
 * claimed to be. Precisely:
 * <ul>
 *   <li>The <b>v1</b> APIs only. The v2 line (burn/mint, transfer events, multi-executor
 *       settlement) is not served, because the on-ledger half implements v1.</li>
 *   <li>No <b>transfer pre-approval</b>, so {@code transferKind} is always {@code "offer"}
 *       — this registry has no direct-transfer path to advertise.</li>
 *   <li>No <b>pause</b>, no <b>account provider/id</b> model, no <b>display metadata</b>
 *       beyond the instrument id: nothing on-ledger backs those fields, so they are
 *       omitted rather than filled with plausible-looking defaults.</li>
 *   <li>Instrument <b>existence is inferred from holdings</b> (see
 *       {@link RegistryService#instruments}), because CIP-56 v1 has no on-ledger
 *       instrument-definition contract. An instrument nobody holds does not appear.</li>
 *   <li>The choice contexts are <b>empty of data</b> — correctly so; see
 *       {@link RegistryService#EMPTY_CHOICE_CONTEXT_DATA}. The factory contexts do carry
 *       the disclosure, which is the part that was actually missing.</li>
 *   <li><b>No authentication.</b> The Registry API is a read-mostly public discovery
 *       surface (Canton Coin's is served unauthenticated by Scan) and everything returned
 *       here is either public registry metadata, an aggregate supply, or a disclosure
 *       blob whose contract the participant re-validates. No holder balance and no
 *       counterparty is exposed. A production deployment would still put rate limiting
 *       in front of it.</li>
 * </ul>
 *
 * <p><b>Error bodies here are the spec's {@code {"error": "..."}}</b>, not the desk's
 * richer body — see {@link RegistryDtos.ErrorResponse} for why. Ledger and gRPC failures
 * are NOT caught here: they propagate to {@code ApiExceptionHandler}, which already turns
 * a {@code PERMISSION_DENIED} into a 403 and a participant outage into a 503, with the
 * command id a node operator needs.
 */
@RestController
@RequestMapping("/registry")
public class RegistryController {

    private static final Logger log = LoggerFactory.getLogger(RegistryController.class);

    /**
     * The REGISTRY-WIDE apis, per the metadata spec's own note that this map "only
     * includes the registry-wide APIs". Canton Coin's Scan answers exactly
     * {@code {"splice-api-token-metadata-v1":1}} here.
     *
     * <p>The value is the MINOR version supported. Ours is {@code 0}: the vendored DARs
     * are the {@code *-v1-1.0.0} release artifacts and this service implements the 1.0
     * response shape — it does not serve the later-minor fields ({@code pauseInfo},
     * {@code accountInputFieldsToShow}), so claiming a higher minor would be a lie a
     * generated client could actually trip over.
     */
    private static final Map<String, Integer> REGISTRY_WIDE_APIS =
            Map.of("splice-api-token-metadata-v1", 0);

    /**
     * The apis supported FOR AN INSTRUMENT of this registry. One entry per interface the
     * on-ledger half actually implements: holdings are readable, transfers are
     * instructable, allocations are creatable and executable.
     *
     * <p>{@code splice-api-token-allocation-request-v1} is deliberately absent — it is
     * implemented on-ledger (by {@code TokenStandardDvp}, the venue) but it is an APP
     * interface with no registry-served off-ledger API, so listing it here would
     * advertise endpoints that do not and should not exist.
     */
    private static final Map<String, Integer> INSTRUMENT_APIS = new java.util.TreeMap<>(Map.of(
            "splice-api-token-holding-v1", 0,
            "splice-api-token-transfer-instruction-v1", 0,
            "splice-api-token-allocation-v1", 0,
            "splice-api-token-allocation-instruction-v1", 0));

    /** The default page size the metadata spec declares for the instrument list. */
    private static final int DEFAULT_PAGE_SIZE = 25;

    private final RegistryService registry;

    public RegistryController(RegistryService registry) {
        this.registry = registry;
    }

    // -----------------------------------------------------------------------
    // token-metadata-v1
    // -----------------------------------------------------------------------

    /**
     * {@code GET /registry/metadata/v1/info} — who this registry is.
     *
     * <p>{@code adminId} is read off the ledger: it is the signatory of the live
     * {@code TokenStandardRegistry} contract, not a configured string. That matters
     * because a wallet uses {@code adminId} to build {@code InstrumentId {admin, id}} and
     * to fill {@code expectedAdmin}; an admin id that did not match the factory's actual
     * signatory would fail inside the Daml choice with nothing to explain it.
     */
    @GetMapping("/metadata/v1/info")
    public ResponseEntity<Object> info() {
        Optional<RegistryService.FactoryContract> factory = registry.findFactory();
        if (factory.isEmpty()) {
            return noRegistry();
        }
        return ResponseEntity.ok(
                new RegistryDtos.RegistryInfo(factory.get().admin(), REGISTRY_WIDE_APIS));
    }

    /**
     * {@code GET /registry/metadata/v1/instruments} — everything this registry administers.
     *
     * <p>Paged as the spec defines it: {@code pageToken} is the {@code nextPageToken} from
     * the previous page and the list is ordered by instrument id, so paging is stable even
     * while supply changes underneath it.
     */
    @GetMapping("/metadata/v1/instruments")
    public ResponseEntity<Object> listInstruments(
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String pageToken) {
        Optional<String> admin = registry.adminParty();
        if (admin.isEmpty()) {
            return noRegistry();
        }
        int size = (pageSize == null || pageSize <= 0) ? DEFAULT_PAGE_SIZE : pageSize;

        List<RegistryService.InstrumentRow> all = registry.instruments(admin.get());
        List<RegistryDtos.Instrument> page = new ArrayList<>();
        String next = null;
        boolean started = (pageToken == null || pageToken.isBlank());
        for (RegistryService.InstrumentRow row : all) {
            if (!started) {
                // The token is the LAST id of the previous page: resume strictly after it.
                started = row.id().equals(pageToken);
                continue;
            }
            if (page.size() == size) {
                next = page.get(page.size() - 1).id();
                break;
            }
            page.add(instrumentOf(row));
        }
        log.info("REGISTRY instruments admin={} total={} returned={} pageSize={} pageToken={}",
                admin.get(), all.size(), page.size(), size, pageToken);
        return ResponseEntity.ok(new RegistryDtos.ListInstrumentsResponse(page, next));
    }

    /** {@code GET /registry/metadata/v1/instruments/{instrumentId}} — one instrument, or 404. */
    @GetMapping("/metadata/v1/instruments/{instrumentId}")
    public ResponseEntity<Object> getInstrument(@PathVariable String instrumentId) {
        Optional<String> admin = registry.adminParty();
        if (admin.isEmpty()) {
            return noRegistry();
        }
        return registry.instruments(admin.get()).stream()
                .filter(r -> r.id().equals(instrumentId))
                .findFirst()
                .<ResponseEntity<Object>>map(r -> ResponseEntity.ok(instrumentOf(r)))
                .orElseGet(() -> error(HttpStatus.NOT_FOUND,
                        "this registry administers no instrument '" + instrumentId + "'. An "
                                + "instrument exists here only while at least one holding of it "
                                + "does — CIP-56 v1 defines no on-ledger instrument-definition "
                                + "contract, so holdings are the only on-ledger evidence an "
                                + "instrument is real."));
    }

    // -----------------------------------------------------------------------
    // transfer-instruction-v1
    // -----------------------------------------------------------------------

    /**
     * {@code POST /registry/transfer-instruction/v1/transfer-factory} — the factory and
     * the disclosure needed to exercise {@code TransferFactory_Transfer} on it.
     *
     * <p><b>{@code transferKind} is always {@code "offer"}, and that is the truth rather
     * than a default.</b> The spec's other two values describe registry behaviours this
     * one does not have: {@code "direct"} requires the receiver to have pre-approved
     * incoming transfers (there is no pre-approval contract in
     * {@code TokenStandardDvp.daml}), and {@code "self"} promises an immediate,
     * approval-free transfer, whereas this registry would still create a
     * {@code TokenStandardTransferOffer} that the same party then has to accept. Reporting
     * either would tell a wallet to expect a settlement that never happens.
     *
     * <p>The request's {@code choiceArguments} are validated against exactly what the Daml
     * choice itself enforces — {@code expectedAdmin} and the instrument's admin — so a
     * request this endpoint accepts cannot be rejected by the factory for a reason the
     * registry already knew. Anything the choice does not check is not checked here
     * either; a registry that is stricter than its own ledger code is a registry that
     * refuses valid commands.
     */
    @PostMapping("/transfer-instruction/v1/transfer-factory")
    public ResponseEntity<Object> transferFactory(
            @RequestBody(required = false) RegistryDtos.GetFactoryRequest request) {
        RegistryDtos.GetFactoryRequest req = request == null
                ? new RegistryDtos.GetFactoryRequest(Map.of(), false) : request;
        Optional<RegistryService.FactoryContract> found = registry.findFactory();
        if (found.isEmpty()) {
            return noRegistry();
        }
        RegistryService.FactoryContract factory = found.get();
        Map<String, Object> args = req.choiceArguments() == null ? Map.of() : req.choiceArguments();

        Optional<ResponseEntity<Object>> bad = checkAdmin(factory.admin(), args,
                path(args, "transfer", "instrumentId", "admin"));
        if (bad.isPresent()) {
            return bad.get();
        }
        warnIfUnknownInstrument(factory.admin(), str(path(args, "transfer", "instrumentId", "id")));

        log.info("REGISTRY transfer-factory admin={} factoryId={} sender={} receiver={} "
                        + "amount={} instrument={}",
                factory.admin(), factory.contractId(), str(path(args, "transfer", "sender")),
                str(path(args, "transfer", "receiver")), str(path(args, "transfer", "amount")),
                str(path(args, "transfer", "instrumentId", "id")));
        return ResponseEntity.ok(new RegistryDtos.TransferFactoryWithChoiceContext(
                factory.contractId(), "offer", factoryContext(factory, req.hideDebug())));
    }

    /**
     * {@code POST /registry/transfer-instruction/v1/{id}/choice-contexts/accept} — and the
     * {@code reject} / {@code withdraw} siblings below.
     *
     * <p><b>These contexts are legitimately empty, and here is why.</b> A choice context
     * exists to hand the caller contracts it cannot otherwise see. For
     * {@code TransferInstruction_Accept}/{@code _Reject}/{@code _Withdraw} there are none:
     * the instruction is signed by sender and admin and observed by the receiver, and its
     * locked holding lists sender and receiver as the lock's {@code holders}, which makes
     * them observers of it. Every party who can control one of these choices can already
     * see every contract the choice touches. Returning a fabricated disclosure would be
     * noise, so the response carries an empty {@code disclosedContracts} — deliberately,
     * not because it was not implemented.
     *
     * <p>The endpoint still does real work: it confirms the instruction is live on the
     * ledger and administered by this registry, so a caller holding a stale contract id
     * learns that here rather than from an unexplained rejection on its own participant.
     */
    @PostMapping("/transfer-instruction/v1/{transferInstructionId}/choice-contexts/accept")
    public ResponseEntity<Object> transferAcceptContext(
            @PathVariable String transferInstructionId,
            @RequestBody(required = false) RegistryDtos.GetChoiceContextRequest request) {
        return transferContext("accept", transferInstructionId);
    }

    @PostMapping("/transfer-instruction/v1/{transferInstructionId}/choice-contexts/reject")
    public ResponseEntity<Object> transferRejectContext(
            @PathVariable String transferInstructionId,
            @RequestBody(required = false) RegistryDtos.GetChoiceContextRequest request) {
        return transferContext("reject", transferInstructionId);
    }

    @PostMapping("/transfer-instruction/v1/{transferInstructionId}/choice-contexts/withdraw")
    public ResponseEntity<Object> transferWithdrawContext(
            @PathVariable String transferInstructionId,
            @RequestBody(required = false) RegistryDtos.GetChoiceContextRequest request) {
        return transferContext("withdraw", transferInstructionId);
    }

    // -----------------------------------------------------------------------
    // allocation-instruction-v1 / allocation-v1
    // -----------------------------------------------------------------------

    /**
     * {@code POST /registry/allocation-instruction/v1/allocation-factory} — the factory and
     * disclosure for {@code AllocationFactory_Allocate}.
     *
     * <p>This is the endpoint that makes the project's DvP reachable by a third party. A
     * sender reads the venue's {@code AllocationRequest} off its own stream, calls this to
     * learn the factory id and receive the disclosure, and can then lock its holding into
     * the settlement — without ever having seen this repository.
     */
    @PostMapping("/allocation-instruction/v1/allocation-factory")
    public ResponseEntity<Object> allocationFactory(
            @RequestBody(required = false) RegistryDtos.GetFactoryRequest request) {
        RegistryDtos.GetFactoryRequest req = request == null
                ? new RegistryDtos.GetFactoryRequest(Map.of(), false) : request;
        Optional<RegistryService.FactoryContract> found = registry.findFactory();
        if (found.isEmpty()) {
            return noRegistry();
        }
        RegistryService.FactoryContract factory = found.get();
        Map<String, Object> args = req.choiceArguments() == null ? Map.of() : req.choiceArguments();

        Optional<ResponseEntity<Object>> bad = checkAdmin(factory.admin(), args,
                path(args, "allocation", "transferLeg", "instrumentId", "admin"));
        if (bad.isPresent()) {
            return bad.get();
        }
        warnIfUnknownInstrument(factory.admin(),
                str(path(args, "allocation", "transferLeg", "instrumentId", "id")));

        log.info("REGISTRY allocation-factory admin={} factoryId={} settlementRef={} "
                        + "legSender={} amount={} instrument={}",
                factory.admin(), factory.contractId(),
                str(path(args, "allocation", "settlement", "settlementRef", "id")),
                str(path(args, "allocation", "transferLeg", "sender")),
                str(path(args, "allocation", "transferLeg", "amount")),
                str(path(args, "allocation", "transferLeg", "instrumentId", "id")));
        return ResponseEntity.ok(new RegistryDtos.FactoryWithChoiceContext(
                factory.contractId(), factoryContext(factory, req.hideDebug())));
    }

    /**
     * {@code POST /registry/allocations/v1/{id}/choice-contexts/execute-transfer} — and the
     * {@code withdraw} / {@code cancel} siblings.
     *
     * <p>Empty for the same structural reason as the transfer contexts, and it is worth
     * being precise about it because this is the DvP's settlement step. The allocation is
     * signed by sender and admin and observed by the executor and receiver; its locked
     * holding names the standard's own {@code allocationControllers} — executor, sender,
     * receiver — as the lock's {@code holders}, and therefore as observers. So the venue
     * exercising {@code Allocation_ExecuteTransfer} can already see both the allocation
     * and the holding it consumes. Nothing needs disclosing, and nothing is withheld.
     */
    @PostMapping("/allocations/v1/{allocationId}/choice-contexts/execute-transfer")
    public ResponseEntity<Object> allocationExecuteContext(
            @PathVariable String allocationId,
            @RequestBody(required = false) RegistryDtos.GetChoiceContextRequest request) {
        return allocationContext("execute-transfer", allocationId);
    }

    @PostMapping("/allocations/v1/{allocationId}/choice-contexts/withdraw")
    public ResponseEntity<Object> allocationWithdrawContext(
            @PathVariable String allocationId,
            @RequestBody(required = false) RegistryDtos.GetChoiceContextRequest request) {
        return allocationContext("withdraw", allocationId);
    }

    @PostMapping("/allocations/v1/{allocationId}/choice-contexts/cancel")
    public ResponseEntity<Object> allocationCancelContext(
            @PathVariable String allocationId,
            @RequestBody(required = false) RegistryDtos.GetChoiceContextRequest request) {
        return allocationContext("cancel", allocationId);
    }

    // -----------------------------------------------------------------------
    // Internals
    // -----------------------------------------------------------------------

    private ResponseEntity<Object> transferContext(String choice, String instructionId) {
        Optional<String> admin = registry.adminParty();
        if (admin.isEmpty()) {
            return noRegistry();
        }
        Optional<RegistryService.TransferOfferRow> offer =
                registry.findTransferOffer(admin.get(), instructionId);
        if (offer.isEmpty()) {
            return error(HttpStatus.NOT_FOUND,
                    "no live transfer instruction " + instructionId + " is administered by this "
                            + "registry. Either it has already been accepted, rejected or "
                            + "withdrawn (every one of those choices archives it), or it belongs "
                            + "to a different instrument admin.");
        }
        log.info("REGISTRY transfer choice-context choice={} instruction={} sender={} receiver={} "
                        + "disclosed=0 (all stakeholders already see every contract this choice "
                        + "touches)",
                choice, instructionId, offer.get().sender(), offer.get().receiver());
        return ResponseEntity.ok(emptyContext());
    }

    private ResponseEntity<Object> allocationContext(String choice, String allocationId) {
        Optional<String> admin = registry.adminParty();
        if (admin.isEmpty()) {
            return noRegistry();
        }
        Optional<RegistryService.AllocationRow> allocation =
                registry.findAllocation(admin.get(), allocationId);
        if (allocation.isEmpty()) {
            return error(HttpStatus.NOT_FOUND,
                    "no live allocation " + allocationId + " is administered by this registry. "
                            + "Either it has already been executed, withdrawn or cancelled (each "
                            + "of those archives it), or it belongs to a different instrument "
                            + "admin.");
        }
        RegistryService.AllocationRow a = allocation.get();
        log.info("REGISTRY allocation choice-context choice={} allocation={} settlementRef={} "
                        + "executor={} amount={} {} settleBefore={} disclosed=0 (executor, sender "
                        + "and receiver are the lock holders and already observe the holding)",
                choice, allocationId, a.settlementRef(), a.executor(), a.amount(),
                a.instrumentId(), a.settleBefore());
        return ResponseEntity.ok(emptyContext());
    }

    /**
     * The factory choice context: empty data, and the ONE disclosure that matters.
     *
     * <p>The disclosed contract is the factory itself. It is not decoration — without it
     * the caller's participant cannot resolve {@code factoryId} at all, because the
     * contract has no observers.
     */
    private RegistryDtos.ChoiceContext factoryContext(
            RegistryService.FactoryContract factory, boolean hideDebug) {
        Map<String, Object> debugPayload = hideDebug
                ? null : Map.of("admin", factory.admin());
        RegistryDtos.DisclosedContract disclosed = new RegistryDtos.DisclosedContract(
                templateIdOf(factory.templateId()),
                factory.contractId(),
                factory.createdEventBlob(),
                factory.synchronizerId(),
                hideDebug ? null : factory.packageName(),
                debugPayload,
                hideDebug || factory.createdAt() == null ? null : factory.createdAt().toString());
        return new RegistryDtos.ChoiceContext(
                RegistryService.EMPTY_CHOICE_CONTEXT_DATA, List.of(disclosed));
    }

    private static RegistryDtos.ChoiceContext emptyContext() {
        return new RegistryDtos.ChoiceContext(
                RegistryService.EMPTY_CHOICE_CONTEXT_DATA, List.of());
    }

    private RegistryDtos.Instrument instrumentOf(RegistryService.InstrumentRow row) {
        // name and symbol are both the instrument id. The standard requires them; the
        // ledger holds no separate display name for a token-standard instrument, and
        // inventing one here would be a string this registry cannot be held to.
        return new RegistryDtos.Instrument(
                row.id(),
                row.id(),
                row.id(),
                plain(row.totalSupply()),
                row.asOf().toString(),
                registry.decimals(),
                INSTRUMENT_APIS);
    }

    /**
     * Reject a request the Daml choice would reject anyway, with the reason the choice
     * would NOT have given. {@code expectedAdmin} is the standard's own guard against a
     * caller being pointed at the wrong registry; getting it wrong produces an
     * interpretation failure that names no parties, so saying both party ids here is the
     * difference between a fixable error and a mystery.
     */
    private Optional<ResponseEntity<Object>> checkAdmin(
            String admin, Map<String, Object> args, Object instrumentAdmin) {
        String expected = str(args.get("expectedAdmin"));
        if (expected != null && !expected.equals(admin)) {
            return Optional.of(error(HttpStatus.BAD_REQUEST,
                    "expectedAdmin '" + expected + "' is not this registry. This registry "
                            + "administers instruments for admin '" + admin + "' — the factory "
                            + "choice asserts the two match, so the command would be rejected "
                            + "on-ledger. GET /registry/metadata/v1/info returns the admin id "
                            + "to use."));
        }
        String legAdmin = str(instrumentAdmin);
        if (legAdmin != null && !legAdmin.equals(admin)) {
            return Optional.of(error(HttpStatus.BAD_REQUEST,
                    "instrumentId.admin '" + legAdmin + "' is not administered by this registry "
                            + "('" + admin + "'). Each instrument is administered by exactly one "
                            + "registry; fetch the factory from the admin named in the "
                            + "instrument id."));
        }
        return Optional.empty();
    }

    /**
     * An instrument this registry has never seen a holding of is suspicious but NOT
     * rejected: the Daml factory choices do not check instrument existence either (the
     * input holdings do that), and a registry that refuses what its own ledger code would
     * accept is worse than one that warns.
     */
    private void warnIfUnknownInstrument(String admin, String instrumentId) {
        if (instrumentId == null || instrumentId.isBlank()) {
            return;
        }
        boolean known = registry.instruments(admin).stream()
                .anyMatch(r -> r.id().equals(instrumentId));
        if (!known) {
            log.warn("REGISTRY factory request names instrument '{}', which has no holdings on "
                    + "this ledger. Not rejected — the Daml choice does not check this either, "
                    + "and the caller's input holdings will settle the question.", instrumentId);
        }
    }

    /** {@code packageId:Module:Entity} — the form the ledger API expects for a disclosure. */
    private static String templateIdOf(Identifier id) {
        return id == null ? "" : id.getPackageId() + ":" + id.getModuleName() + ":"
                + id.getEntityName();
    }

    private static String plain(BigDecimal v) {
        return v == null ? "0" : v.stripTrailingZeros().toPlainString();
    }

    /** Walk a nested JSON object safely; any missing or wrongly-typed level yields null. */
    private static Object path(Map<String, Object> root, String... keys) {
        Object node = root;
        for (String key : keys) {
            if (!(node instanceof Map<?, ?> m)) {
                return null;
            }
            node = m.get(key);
        }
        return node;
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private ResponseEntity<Object> noRegistry() {
        // A 404 with an instruction, not a bare "not found": the usual cause is a ledger
        // that has never had the factory created on it, and that is one call away.
        return error(HttpStatus.NOT_FOUND,
                "this participant hosts no TokenStandardRegistry contract, so there is no "
                        + "registry to describe. The registry's on-ledger half must exist before "
                        + "its off-ledger API can serve anything — create it with "
                        + "POST /api/token-standard/registry {\"admin\":\"<party>\"}, or point "
                        + "registry.admin (REGISTRY_ADMIN) at the party that already holds one.");
    }

    private static ResponseEntity<Object> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new RegistryDtos.ErrorResponse(message));
    }
}
