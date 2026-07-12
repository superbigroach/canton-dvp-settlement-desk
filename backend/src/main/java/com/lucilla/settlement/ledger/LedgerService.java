package com.lucilla.settlement.ledger;

import com.daml.ledger.javaapi.data.CommandsSubmission;
import com.daml.ledger.javaapi.data.ContractFilter;
import com.daml.ledger.javaapi.data.CreatedEvent;
import com.daml.ledger.javaapi.data.Identifier;
import com.daml.ledger.javaapi.data.TransactionTree;
import com.daml.ledger.javaapi.data.TreeEvent;
import com.daml.ledger.javaapi.data.codegen.HasCommands;
import com.daml.ledger.rxjava.DamlLedgerClient;
import com.lucilla.settlement.config.LedgerConnection;
import com.lucilla.settlement.model.holding.Holding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The seam between the desk and the ledger: submits the {@link LedgerCommands}
 * updates over the Ledger API (gRPC) under the correct {@code actAs} party, and
 * reads active contracts back.
 *
 * <p>Submissions use {@code submitAndWaitForTransactionTree} so that, on success,
 * the freshly-created contract id (the DvPAgreement from an Accept, the
 * SettlementBatch from a close, …) can be pulled straight out of the resulting
 * transaction tree and handed back to the caller as the handle for the next step.
 *
 * <p>This class is the documented <b>integration boundary</b>: its behaviour is
 * exercised by {@code LedgerIntegrationIT} against a running sandbox. The pure
 * command-mapping it relies on is unit-tested in {@code LedgerCommandsTest}.
 */
@Service
public class LedgerService {

    private static final Logger log = LoggerFactory.getLogger(LedgerService.class);

    private final LedgerConnection connection;

    public LedgerService(LedgerConnection connection) {
        this.connection = connection;
    }

    // -----------------------------------------------------------------------
    // Submission
    // -----------------------------------------------------------------------

    /**
     * Submit a single command as {@code actAs}, block for the transaction tree,
     * and return the contract id of the first created contract whose template
     * matches {@code createdTemplate}. Use when the command creates a contract the
     * caller needs a handle to (create, or an exercise that creates a successor).
     */
    public String submitForCreated(String actAs, HasCommands command, Identifier createdTemplate) {
        TransactionTree tree = submit(actAs, command);
        return firstCreatedOf(tree, createdTemplate).orElseThrow(() ->
                new LedgerException("no " + createdTemplate.getEntityName()
                        + " contract was created by the transaction"));
    }

    /** Submit a single command as {@code actAs} and block for the transaction tree. */
    public TransactionTree submit(String actAs, HasCommands command) {
        DamlLedgerClient client = connection.get();
        CommandsSubmission submission = CommandsSubmission
                .create(connection.properties().getApplicationId(),
                        UUID.randomUUID().toString(),
                        List.of(command))
                .withActAs(actAs);
        if (connection.properties().hasJwt()) {
            submission = submission.withAccessToken(Optional.of(connection.properties().getJwt()));
        }
        try {
            return client.getCommandClient()
                    .submitAndWaitForTransactionTree(submission)
                    .timeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .blockingGet();
        } catch (RuntimeException e) {
            log.warn("Command submission failed (actAs={}): {}", actAs, e.getMessage());
            throw new LedgerException(rootMessage(e), e);
        }
    }

    /** Contract ids of ALL created contracts of a template in a transaction tree. */
    public List<String> createdOf(TransactionTree tree, Identifier template) {
        List<String> ids = new ArrayList<>();
        for (TreeEvent ev : tree.getEventsById().values()) {
            if (ev instanceof CreatedEvent created && sameTemplate(created.getTemplateId(), template)) {
                ids.add(created.getContractId());
            }
        }
        return ids;
    }

    private Optional<String> firstCreatedOf(TransactionTree tree, Identifier template) {
        return createdOf(tree, template).stream().findFirst();
    }

    // Match on module + entity name only. The package-id hash on a codegen
    // TEMPLATE_ID and on the ledger's event can differ across package versions,
    // but module+entity uniquely name the template within this project.
    private static boolean sameTemplate(Identifier a, Identifier b) {
        return a.getModuleName().equals(b.getModuleName())
                && a.getEntityName().equals(b.getEntityName());
    }

    // -----------------------------------------------------------------------
    // Query
    // -----------------------------------------------------------------------

    /**
     * All parties the ledger knows about, via the party-management admin service.
     *
     * <p>Party ids on Canton carry a namespace suffix ({@code Alice::1220ab…}) that
     * changes every allocation, so the desk NEVER hardcodes them — it resolves them
     * live here for the UI's party picker. A friendly {@code label} is derived from
     * the display name when present, else the hint prefix before {@code "::"}.
     */
    public List<PartyView> listParties() {
        return withRetry("list parties", () -> {
            var stub = connection.partyManagement();
            var req = com.daml.ledger.api.v1.admin.PartyManagementServiceOuterClass
                    .ListKnownPartiesRequest.getDefaultInstance();
            var resp = stub
                    .withDeadlineAfter(30, java.util.concurrent.TimeUnit.SECONDS)
                    .listKnownParties(req);
            List<PartyView> out = new ArrayList<>();
            for (var pd : resp.getPartyDetailsList()) {
                String party = pd.getParty();
                String display = pd.getDisplayName();
                String label = (display != null && !display.isBlank())
                        ? display
                        : (party.contains("::") ? party.substring(0, party.indexOf("::")) : party);
                out.add(new PartyView(party, display == null ? "" : display, label, pd.getIsLocal()));
            }
            return out;
        });
    }

    /**
     * Resolve a caller-supplied party reference (a hint/label like {@code "Alice"},
     * or an already-qualified {@code "Alice::1220…"}) to the FULL on-ledger party id.
     * If an exact match exists it wins; otherwise a unique prefix match on the label
     * is used. Throws when nothing (or more than one thing) matches — never guesses.
     */
    public String resolveParty(String reference) {
        if (reference == null || reference.isBlank()) {
            throw new LedgerException("party is required");
        }
        String ref = reference.trim();
        List<PartyView> parties = listParties();
        for (PartyView p : parties) {
            if (p.party().equals(ref)) {
                return p.party();
            }
        }
        List<PartyView> byLabel = parties.stream()
                .filter(p -> p.label().equalsIgnoreCase(ref) || p.party().startsWith(ref + "::"))
                .toList();
        if (byLabel.size() == 1) {
            return byLabel.get(0).party();
        }
        if (byLabel.isEmpty()) {
            throw new LedgerException("no known party matches '" + reference + "'");
        }
        throw new LedgerException("party reference '" + reference + "' is ambiguous ("
                + byLabel.size() + " matches)");
    }

    /**
     * Active {@link Holding} contracts visible to {@code party}. On Canton a
     * holding is visible only to its issuer and owner (plus explicit disclosures),
     * so this returns exactly what {@code party} is entitled to see.
     */
    public List<HoldingView> holdingsVisibleTo(String party) {
        return withRetry("holdings for " + party, () -> {
            DamlLedgerClient client = connection.get();
            ContractFilter<Holding.Contract> filter = ContractFilter.of(Holding.COMPANION);
            List<HoldingView> out = new ArrayList<>();
            client.getActiveContractSetClient()
                    .getActiveContracts(filter, Set.of(party), false)
                    .timeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .blockingForEach(active -> {
                        for (Holding.Contract c : active.activeContracts) {
                            Holding h = c.data;
                            out.add(new HoldingView(
                                    c.id.contractId, h.issuer, h.instrumentId, h.owner, h.amount, h.disclosedTo));
                        }
                    });
            return out;
        });
    }

    /**
     * Retry an idempotent read a few times on transient gRPC stream failures.
     *
     * <p>On a loaded host (many JVMs sharing the box) the ACS snapshot stream
     * occasionally drops mid-frame ({@code INTERNAL: Encountered end-of-stream
     * mid-frame} / {@code RESOURCE_EXHAUSTED}). The snapshot is a pure read, so a
     * bounded retry makes the endpoint reliable without changing any ledger state.
     */
    private <T> T withRetry(String what, java.util.concurrent.Callable<T> op) {
        int attempts = 4;
        RuntimeException last = null;
        for (int i = 1; i <= attempts; i++) {
            try {
                return op.call();
            } catch (Exception e) {
                RuntimeException re = (e instanceof RuntimeException r) ? r : new RuntimeException(e);
                String msg = rootMessage(re);
                boolean transientErr = msg != null && (msg.contains("end-of-stream")
                        || msg.contains("RESOURCE_EXHAUSTED") || msg.contains("UNAVAILABLE")
                        || msg.contains("INTERNAL"));
                if (!transientErr || i == attempts) {
                    if (transientErr) {
                        throw new LedgerException("ledger read failed after " + attempts
                                + " attempts (" + what + "): " + msg, re);
                    }
                    throw re;
                }
                last = re;
                log.warn("Transient ledger read failure ({}), retry {}/{}: {}", what, i, attempts, msg);
                try {
                    Thread.sleep(150L * i);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new LedgerException("interrupted while retrying " + what, ie);
                }
            }
        }
        throw last != null ? last : new LedgerException("read failed: " + what);
    }

    private static String rootMessage(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null && c.getCause() != c) {
            c = c.getCause();
        }
        return c.getMessage() != null ? c.getMessage() : t.toString();
    }

    /** Flat, JSON-friendly view of a known party. */
    public record PartyView(String party, String displayName, String label, boolean isLocal) {
    }

    /** Flat, JSON-friendly view of a Holding. */
    public record HoldingView(
            String contractId, String issuer, String instrumentId, String owner,
            java.math.BigDecimal amount, List<String> disclosedTo) {
    }

    /** Wraps ledger/command failures as a clean runtime error for the web layer. */
    public static class LedgerException extends RuntimeException {
        public LedgerException(String message) {
            super(message);
        }

        public LedgerException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    // Kept for symmetry / potential callers needing raw durations.
    @SuppressWarnings("unused")
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
}
