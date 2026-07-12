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
     * Active {@link Holding} contracts visible to {@code party}. On Canton a
     * holding is visible only to its issuer and owner (plus explicit disclosures),
     * so this returns exactly what {@code party} is entitled to see.
     */
    public List<HoldingView> holdingsVisibleTo(String party) {
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
    }

    private static String rootMessage(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null && c.getCause() != c) {
            c = c.getCause();
        }
        return c.getMessage() != null ? c.getMessage() : t.toString();
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
