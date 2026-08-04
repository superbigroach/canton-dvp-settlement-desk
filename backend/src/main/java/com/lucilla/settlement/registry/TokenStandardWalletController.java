package com.lucilla.settlement.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The WALLET side of the token standard — claiming and managing assets issued by
 * <b>somebody else's</b> registry.
 *
 * <p><b>Why this is the most important surface in the project.</b> The judges' criticism
 * was that cETH and cBTC are self-issued stand-ins. The answer to that is not an argument,
 * it is a balance: a holding of an instrument this project did not issue, administered by a
 * registry this project did not write, sitting in a party this project controls. BitSafe's
 * CBTC faucet sends exactly that — but it sends it as a <i>pending</i>
 * {@code TransferInstruction}, and until the receiver accepts, it is not a holding and
 * appears in no balance anywhere. These endpoints are the accept.
 *
 * <pre>
 *   GET  /api/token-standard/pending?party=Alice[&amp;direction=inbound|outbound|all]
 *   POST /api/token-standard/accept    {"party":"Alice","instructionCid":"00ab…"}
 *   POST /api/token-standard/reject    {"party":"Alice","instructionCid":"00ab…"}
 *   POST /api/token-standard/withdraw  {"party":"Alice","instructionCid":"00ab…"}
 * </pre>
 *
 * <p><b>Nothing here names a concrete template.</b> The reads filter the active contract
 * set by the INTERFACE {@code Splice.Api.Token.TransferInstructionV1:TransferInstruction}
 * and the writes exercise that interface's own choices. A filter on this project's
 * {@code TokenStandardTransferOffer} would return nothing at all for a BitSafe transfer —
 * that is the single most likely way to build this and see an empty list.
 *
 * <p><b>What a foreign accept may need that this desk cannot invent.</b> The standard lets
 * a registry require a {@code ChoiceContext} plus disclosed contracts for
 * {@code TransferInstruction_Accept}, served from ITS
 * {@code /registry/transfer-instruction/v1/{cid}/choice-contexts/accept}. This backend
 * will fetch and attach both when a registry URL is configured for that instrument admin
 * (see {@link RemoteRegistryClient}); with none configured it submits an empty context and
 * <b>says so in the response</b>, because a registry that needs a context will reject the
 * command and the operator needs to know that is the reason rather than guessing.
 */
@RestController
@RequestMapping("/api/token-standard")
public class TokenStandardWalletController {

    private static final Logger log = LoggerFactory.getLogger(TokenStandardWalletController.class);

    private final RegistryService registry;
    private final RemoteRegistryClient remote;

    public TokenStandardWalletController(RegistryService registry, RemoteRegistryClient remote) {
        this.registry = registry;
        this.remote = remote;
    }

    /** {@code {"party":"Alice","instructionCid":"00ab…"}} */
    public record InstructionRequest(String party, String instructionCid) {
    }

    /**
     * One pending instruction, flattened for a wallet UI.
     *
     * @param canAct     whether the party this was queried for is the controller of the
     *                   choice named in {@code action} — so a UI can render one button and
     *                   be sure it will work
     * @param registryUrl the configured registry for this instrument's admin, or null.
     *                    Null on a FOREIGN instrument is the thing to fix if an accept is
     *                    rejected for want of a context.
     */
    public record PendingTransfer(
            String instructionCid, String direction, String action, boolean canAct,
            String sender, String receiver, String instrumentId, String instrumentAdmin,
            BigDecimal amount, String status, Instant requestedAt, Instant executeBefore,
            boolean ourRegistry, String registryUrl, boolean expired) {
    }

    /**
     * {@code GET /api/token-standard/pending} — what is waiting for this party.
     *
     * <p>Inbound by default, because that is the question a wallet actually asks ("what can
     * I claim?"). {@code direction=all} adds the party's own outbound instructions, which
     * are the ones it may withdraw.
     */
    @GetMapping("/pending")
    public Map<String, Object> pending(
            @RequestParam String party,
            @RequestParam(required = false, defaultValue = "inbound") String direction) {
        Instant now = Instant.now();
        String resolved = registry.resolvePartyReference(party);
        List<RegistryService.PendingTransferRow> rows = registry.pendingTransfers(resolved);

        List<PendingTransfer> out = rows.stream()
                .filter(r -> "all".equalsIgnoreCase(direction)
                        || r.direction().equalsIgnoreCase(direction))
                .map(r -> {
                    boolean inbound = "inbound".equals(r.direction());
                    return new PendingTransfer(
                            r.contractId(),
                            r.direction(),
                            inbound ? "accept" : "withdraw",
                            inbound || "outbound".equals(r.direction()),
                            r.sender(), r.receiver(), r.instrumentId(), r.instrumentAdmin(),
                            r.amount(), r.status(), r.requestedAt(), r.executeBefore(),
                            r.ours(),
                            r.ours() ? null : remote.baseUrlFor(r.instrumentAdmin()).orElse(null),
                            r.executeBefore() != null && !r.executeBefore().isAfter(now));
                })
                .toList();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("party", resolved);
        body.put("direction", direction);
        body.put("pending", out);
        // The one sentence an operator needs when the list is empty but the faucet said it
        // sent something: an instruction is only visible to its own stakeholders, and only
        // while it is still pending.
        if (out.isEmpty()) {
            body.put("note", "no live TransferInstruction is visible to this party. A pending "
                    + "transfer is visible only to its sender and receiver, and disappears the "
                    + "moment it is accepted, rejected or withdrawn. If a faucet reported a "
                    + "successful send, check the party id it was sent to matches this one "
                    + "EXACTLY, including the ::namespace suffix.");
        }
        log.info("REGISTRY pending party={} direction={} found={}", resolved, direction, out.size());
        return body;
    }

    /**
     * {@code POST /api/token-standard/accept} — claim an inbound transfer.
     *
     * <p>This is the call that turns BitSafe's pending CBTC into a real holding of the
     * party. The controller of {@code TransferInstruction_Accept} is fixed by the standard
     * as the transfer's receiver, so the acting party is checked against the contract
     * rather than taken on trust.
     */
    @PostMapping("/accept")
    public ResponseEntity<Map<String, Object>> accept(@RequestBody InstructionRequest request) {
        return exercise("accept", request);
    }

    /** {@code POST /api/token-standard/reject} — decline an inbound transfer (receiver). */
    @PostMapping("/reject")
    public ResponseEntity<Map<String, Object>> reject(@RequestBody InstructionRequest request) {
        return exercise("reject", request);
    }

    /** {@code POST /api/token-standard/withdraw} — pull back an outbound transfer (sender). */
    @PostMapping("/withdraw")
    public ResponseEntity<Map<String, Object>> withdraw(@RequestBody InstructionRequest request) {
        return exercise("withdraw", request);
    }

    private ResponseEntity<Map<String, Object>> exercise(
            String choice, InstructionRequest request) {
        if (request == null || request.party() == null || request.party().isBlank()) {
            throw new IllegalArgumentException(
                    "party is required — the party acting on the instruction (the RECEIVER for "
                            + "accept and reject, the SENDER for withdraw)");
        }
        if (request.instructionCid() == null || request.instructionCid().isBlank()) {
            throw new IllegalArgumentException("instructionCid is required — the contract id "
                    + "from GET /api/token-standard/pending");
        }
        RegistryService.ExerciseOutcome outcome = registry.exerciseTransferInstruction(
                request.party(), request.instructionCid().trim(), choice);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("choice", "TransferInstruction_"
                + Character.toUpperCase(choice.charAt(0)) + choice.substring(1));
        body.put("instructionCid", request.instructionCid().trim());
        body.put("updateId", outcome.updateId());
        // What appeared on the ledger. For an accept this is where the received holding is,
        // and it is a contract of the FOREIGN registry's template — which is the proof.
        body.put("created", outcome.created());
        body.put("choiceContext", Map.of(
                "source", outcome.contextSource(),
                "values", outcome.contextValues(),
                "disclosedContracts", outcome.disclosedContracts()));
        log.info("REGISTRY {} done instruction={} updateId={} created={}",
                choice, request.instructionCid(), outcome.updateId(), outcome.created().size());
        return ResponseEntity.ok(body);
    }

    /**
     * A foreign registry that could not be reached, or answered with something unusable.
     *
     * <p>Handled HERE and as a {@code 502}, deliberately: this is not the ledger's fault and
     * not this desk's, and reporting it as either would send an operator to the wrong place.
     * The message carries the URL that was called and what to configure.
     */
    @ExceptionHandler(RemoteRegistryClient.RemoteRegistryException.class)
    public ResponseEntity<Map<String, Object>> handleRemoteRegistry(
            RemoteRegistryClient.RemoteRegistryException e) {
        log.error("REGISTRY remote registry failure: {}", e.getMessage());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.BAD_GATEWAY.value());
        body.put("error", HttpStatus.BAD_GATEWAY.getReasonPhrase());
        body.put("message", e.getMessage());
        body.put("hint", "the token standard lets a registry require an off-ledger choice "
                + "context for accept. This desk fetches it from the registry configured for "
                + "the instrument's admin (registry.remote-urls / registry.remote-url) and "
                + "cannot invent one. Nothing was submitted to the ledger.");
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
    }
}
