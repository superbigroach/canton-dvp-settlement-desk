package com.lucilla.settlement.registry;

import com.daml.ledger.javaapi.data.ActiveContract;
import com.daml.ledger.javaapi.data.CommandsSubmission;
import com.daml.ledger.javaapi.data.ContractEntry;
import com.daml.ledger.javaapi.data.ContractFilter;
import com.daml.ledger.javaapi.data.CreatedEvent;
import com.daml.ledger.javaapi.data.CumulativeFilter;
import com.daml.ledger.javaapi.data.EventFormat;
import com.daml.ledger.javaapi.data.Filter;
import com.daml.ledger.javaapi.data.Identifier;
import com.daml.ledger.javaapi.data.Transaction;
import com.daml.ledger.javaapi.data.TransactionFormat;
import com.daml.ledger.javaapi.data.TransactionShape;
import com.daml.ledger.javaapi.data.codegen.HasCommands;
import com.daml.ledger.rxjava.DamlLedgerClient;
import com.lucilla.settlement.config.LedgerConnection;
import com.lucilla.settlement.ledger.LedgerCommands;
import com.lucilla.settlement.ledger.LedgerErrors;
import com.lucilla.settlement.ledger.LedgerService;
import com.lucilla.settlement.model.splice.api.token.metadatav1.ChoiceContext;
import com.lucilla.settlement.model.splice.api.token.metadatav1.ExtraArgs;
import com.lucilla.settlement.model.splice.api.token.metadatav1.Metadata;
import com.lucilla.settlement.model.splice.api.token.transferinstructionv1.TransferInstruction;
import com.lucilla.settlement.model.splice.api.token.transferinstructionv1.TransferInstructionStatus;
import com.lucilla.settlement.model.splice.api.token.transferinstructionv1.TransferInstructionView;
import com.lucilla.settlement.model.splice.api.token.transferinstructionv1.transferinstructionstatus.TransferPendingInternalWorkflow;
import com.lucilla.settlement.model.splice.api.token.transferinstructionv1.transferinstructionstatus.TransferPendingReceiverAcceptance;
import com.lucilla.settlement.model.tokenstandarddvp.TokenStandardAllocation;
import com.lucilla.settlement.model.tokenstandarddvp.TokenStandardHolding;
import com.lucilla.settlement.model.tokenstandarddvp.TokenStandardRegistry;
import com.lucilla.settlement.model.tokenstandarddvp.TokenStandardTransferOffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * The LEDGER HALF of the off-ledger CIP-56 Registry API.
 *
 * <p><b>What a registry is for.</b> CIP-56 splits an asset in two. The on-ledger half is
 * a set of Daml interfaces — this project has six of them, in
 * {@code daml/TokenStandardDvp.daml}. The off-ledger half is an HTTP service that lets a
 * wallet <i>discover</i> the instruments and <i>assemble</i> a valid command for a
 * standard choice. Without the second half the first is unreachable: the factory
 * contract that every transfer and every allocation is exercised on is signed by the
 * registry admin and observed by NOBODY, so a wallet cannot see it, cannot name its
 * contract id, and cannot make its participant accept a command that touches it. That is
 * not a policy gap, it is a hard visibility one, and the only key that opens it is an
 * explicitly disclosed contract — the {@code createdEventBlob} this class reads off the
 * ledger and hands out.
 *
 * <p>Until now this project obtained that blob from a Daml Script
 * ({@code queryDisclosure} in {@code TokenStandardTest.daml}), which is the same
 * MECHANISM production uses driven from a test rather than a service. This class is that
 * service.
 *
 * <p><b>Everything here is read from live ledger state.</b> There is no fixture, no
 * configuration file of instruments, and no cached view: the factory contract id, its
 * disclosure blob, its synchronizer, the instrument list and every total supply are
 * computed from the active contract set on each call. If the ledger holds no
 * {@code TokenStandardRegistry}, the API says so with a 404 rather than inventing one.
 *
 * <p><b>Reads are addressed to a party, and that is the privacy model.</b> Instrument
 * supply is read AS THE REGISTRY ADMIN, which is sound precisely because the admin
 * co-signs every {@code TokenStandardHolding} of its own instruments — so the admin sees
 * all of them and nobody else's. No holder's balance is ever exposed by these endpoints;
 * only the aggregate is.
 */
@Service
public class RegistryService {

    private static final Logger log = LoggerFactory.getLogger(RegistryService.class);

    /**
     * The Daml JSON encoding of {@code Splice.Api.Token.MetadataV1.ChoiceContext} with no
     * values — {@code ChoiceContext with values : TextMap AnyValue}.
     *
     * <p><b>This is {@code {"values":{}}}, NOT {@code {}}.</b> The caller assigns this
     * object straight into {@code choiceArguments.extraArgs.context} (see the reference
     * CLI: {@code choiceArgs.extraArgs.context = transferFactory.choiceContext
     * .choiceContextData}), and {@code extraArgs.context} is decoded as a Daml record. A
     * bare {@code {}} would fail to decode as a record with a required {@code values}
     * field, so the shape matters more than the emptiness does.
     *
     * <p>It is empty because this registry genuinely needs no off-ledger context: its
     * factory choices validate {@code expectedAdmin} and the transfer/allocation fields
     * from their own arguments and read no fee schedule, price feed or round contract. A
     * production registry (Amulet) puts exactly that reference data here.
     */
    static final Map<String, Object> EMPTY_CHOICE_CONTEXT_DATA = Map.of("values", Map.of());

    /** Daml {@code Decimal} carries 10 decimal places; that is the instrument's precision. */
    private static final int DAML_DECIMAL_PLACES = 10;

    /** How long any single ledger read may take before it is treated as a failure. */
    private static final int READ_TIMEOUT_SECONDS = 30;

    private final LedgerConnection connection;
    private final LedgerService ledger;
    private final RemoteRegistryClient remote;

    /**
     * Optional pin for the registry admin party ({@code REGISTRY_ADMIN} /
     * {@code registry.admin}). Left blank the admin is DISCOVERED from the ledger, which
     * is the right default: on a local sandbox the party namespace is re-minted on every
     * run, so a configured id would be stale within one restart. Set it on a participant
     * that hosts a large roster, to narrow the discovery read to one party.
     */
    @Value("${registry.admin:}")
    private String adminPin = "";

    /**
     * Pin the token-standard INTERFACE package to the version this backend was compiled
     * against ({@code registry.pin-interface-package}, default on).
     *
     * <p>An interface exercise names the interface by package NAME, which the participant
     * resolves to whichever vetted version it prefers. The choice argument this backend
     * encodes is v1.0.0's, so preferring v1.0.0 keeps the two in step.
     *
     * <p><b>It is a switch and not a constant deliberately.</b> The pin only helps while
     * the pinned package is vetted on the node; on somebody else's participant that is a
     * fact about their deployment, not ours. If a foreign accept ever fails resolving the
     * package, setting {@code REGISTRY_PIN_INTERFACE_PACKAGE=false} restores the
     * participant's own preference without a rebuild.
     */
    @Value("${registry.pin-interface-package:true}")
    private boolean pinInterfacePackage = true;

    /**
     * The admin party once discovered. Cached because it is a stable fact about the
     * deployment; the factory CONTRACT is never cached, because a contract id is not.
     */
    private volatile String discoveredAdmin;

    public RegistryService(
            LedgerConnection connection, LedgerService ledger, RemoteRegistryClient remote) {
        this.connection = connection;
        this.ledger = ledger;
        this.remote = remote;
    }

    // -----------------------------------------------------------------------
    // The factory contract — the thing a wallet cannot see for itself
    // -----------------------------------------------------------------------

    /**
     * The registry's factory contract as it stands on the ledger right now, together
     * with everything a caller needs to disclose it to its own participant.
     *
     * @param contractId       the factory id a wallet exercises the standard choice on
     * @param admin            the instrument admin — the contract's only signatory
     * @param templateId       the ledger's own (package-id qualified) template identifier
     * @param createdEventBlob the participant's authenticated create-event serialisation,
     *                         base64. This is the disclosure; without it the command is
     *                         rejected with the factory contract "not found".
     * @param synchronizerId   the synchronizer the contract is currently assigned to
     * @param packageName      advisory, for the response's {@code debugPackageName}
     * @param createdAt        advisory, for the response's {@code debugCreatedAt}
     */
    public record FactoryContract(
            String contractId, String admin, Identifier templateId, String createdEventBlob,
            String synchronizerId, String packageName, Instant createdAt) {
    }

    /**
     * Find the registry's factory contract, discovering the admin party if it is not pinned.
     *
     * <p><b>Discovery is a single query, not a scan.</b> The {@code TokenStandardRegistry}
     * contract has exactly one stakeholder — its admin — so a template-filtered ACS read
     * addressed to every party this desk knows about returns it to exactly one of them,
     * and the contract's own signatory set names the admin. That is more robust than
     * configuration: it cannot disagree with the ledger.
     *
     * @return empty when the ledger holds no registry contract visible to this desk
     */
    public Optional<FactoryContract> findFactory() {
        Set<String> parties = readAsParties();
        // includeCreatedEventBlob is the whole point of this read: without it the
        // CreatedEvent comes back with an empty blob and the disclosure is worthless.
        EventFormat format = ContractFilter.of(TokenStandardRegistry.COMPANION)
                .withIncludeCreatedEventBlob(true)
                .eventFormat(parties.isEmpty() ? Optional.empty() : Optional.of(parties));

        List<FactoryContract> found = read("token-standard registry factory", () -> {
            DamlLedgerClient client = connection.get();
            Long end = client.getStateClient().getLedgerEnd().blockingGet();
            Map<String, FactoryContract> byCid = new LinkedHashMap<>();
            client.getStateClient().getActiveContracts(format, end)
                    .timeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .blockingForEach(resp -> {
                        Optional<ContractEntry> entry = resp.getContractEntry();
                        if (entry.isEmpty() || !(entry.get() instanceof ActiveContract ac)) {
                            return;
                        }
                        CreatedEvent ce = ac.getCreatedEvent();
                        // Addressed to several parties at once, so the same contract can
                        // arrive more than once. Keyed by contract id, first wins.
                        byCid.putIfAbsent(ce.getContractId(), new FactoryContract(
                                ce.getContractId(),
                                adminOf(ce),
                                ce.getTemplateId(),
                                Base64.getEncoder().encodeToString(
                                        ce.getCreatedEventBlob().toByteArray()),
                                ac.getSynchronizerId(),
                                ce.getPackageName(),
                                ce.getCreatedAt()));
                    });
            return new ArrayList<>(byCid.values());
        });

        if (found.isEmpty()) {
            log.info("REGISTRY no TokenStandardRegistry contract is visible to this desk "
                    + "(readAs={})", parties.isEmpty() ? "(any party)" : parties);
            return Optional.empty();
        }
        if (found.size() > 1) {
            // Several registries on one participant is legitimate (each admin runs its
            // own). This deployment serves ONE, so say which and why, rather than
            // silently picking.
            log.warn("REGISTRY {} TokenStandardRegistry contracts are visible; serving admin={}. "
                            + "Pin one with registry.admin (REGISTRY_ADMIN) to make this explicit.",
                    found.size(), found.get(0).admin());
        }
        FactoryContract factory = found.get(0);
        discoveredAdmin = factory.admin();
        return Optional.of(factory);
    }

    /**
     * The registry admin's full party id, or empty when no registry exists on the ledger.
     * Cheap after the first call; never guesses a party that has no factory behind it.
     */
    public Optional<String> adminParty() {
        String cached = discoveredAdmin;
        if (cached != null) {
            return Optional.of(cached);
        }
        return findFactory().map(FactoryContract::admin);
    }

    // -----------------------------------------------------------------------
    // Instruments — derived from holdings, because that is where the truth is
    // -----------------------------------------------------------------------

    /**
     * One instrument this registry administers, as the ledger describes it.
     *
     * @param id           the instrument id — the {@code id} of {@code InstrumentId
     *                     {admin, id}}, e.g. {@code "cETH"}
     * @param totalSupply  the sum of EVERY holding of this instrument, locked or free.
     *                     Exact, not an estimate: the admin co-signs every
     *                     {@code TokenStandardHolding} of its own instruments, so this
     *                     read cannot miss one.
     * @param lockedSupply how much of that supply is currently committed to an allocation
     *                     or an outstanding transfer offer
     * @param holdingCount how many holding contracts make up the supply
     * @param asOf         when the supply was computed (the ledger read, not a stored field)
     */
    public record InstrumentRow(
            String id, BigDecimal totalSupply, BigDecimal lockedSupply, int holdingCount,
            Instant asOf) {
    }

    /**
     * Every instrument this registry administers, ascending by id.
     *
     * <p><b>Why this is derived and not stored.</b> CIP-56 v1 defines no on-ledger
     * instrument-definition contract — the registry IS the authority on what it
     * administers, and the standard's only on-ledger trace of an instrument is the
     * {@code InstrumentId} stamped on each holding. So the honest source for "which
     * instruments exist and how much of each" is the holdings themselves. The consequence
     * is worth stating plainly: <b>an instrument with no holdings does not appear</b>,
     * because on this ledger there is nothing that says it exists.
     */
    public List<InstrumentRow> instruments(String admin) {
        Instant asOf = Instant.now();
        Map<String, BigDecimal[]> totals = new TreeMap<>();   // id -> [total, locked]
        Map<String, Integer> counts = new TreeMap<>();

        read("token-standard holdings for " + admin, () -> {
            DamlLedgerClient client = connection.get();
            Long end = client.getStateClient().getLedgerEnd().blockingGet();
            client.getStateClient()
                    .getActiveContracts(ContractFilter.of(TokenStandardHolding.COMPANION),
                            Set.of(admin), false, end)
                    .timeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .blockingForEach(active -> {
                        for (TokenStandardHolding.Contract c : active.activeContracts) {
                            TokenStandardHolding h = c.data;
                            // Guard even though the template's `ensure` already does:
                            // a participant hosting two registries would otherwise let
                            // one registry's supply leak into the other's answer.
                            if (!admin.equals(h.instrumentId.admin)) {
                                continue;
                            }
                            String id = h.instrumentId.id;
                            BigDecimal[] acc = totals.computeIfAbsent(id,
                                    k -> new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO});
                            acc[0] = acc[0].add(h.amount);
                            if (h.lock.isPresent()) {
                                acc[1] = acc[1].add(h.amount);
                            }
                            counts.merge(id, 1, Integer::sum);
                        }
                    });
            return null;
        });

        List<InstrumentRow> out = new ArrayList<>();
        totals.forEach((id, acc) -> out.add(new InstrumentRow(
                id, acc[0], acc[1], counts.getOrDefault(id, 0), asOf)));
        out.sort(Comparator.comparing(InstrumentRow::id));
        return out;
    }

    // -----------------------------------------------------------------------
    // The contracts a choice context is ABOUT
    // -----------------------------------------------------------------------

    /**
     * A live {@code TokenStandardAllocation}, flattened to the facts a choice context
     * needs to be validated against — never trusted from the request.
     */
    public record AllocationRow(
            String contractId, String sender, String receiver, String instrumentId,
            BigDecimal amount, String executor, String settlementRef, String lockedHoldingCid,
            Instant settleBefore) {
    }

    /** A live {@code TokenStandardTransferOffer} (the standard's {@code TransferInstruction}). */
    public record TransferOfferRow(
            String contractId, String sender, String receiver, String instrumentId,
            BigDecimal amount, String lockedHoldingCid, Instant executeBefore) {
    }

    /**
     * The allocation with this contract id, as the REGISTRY sees it — or empty.
     *
     * <p>Read as the admin, which is a signatory of every allocation of its own
     * instruments. An id that is absent here is either archived (already executed,
     * withdrawn or cancelled) or belongs to another registry; either way this registry
     * cannot serve a context for it and says 404 rather than returning an empty context
     * that would fail later, on the caller's participant, for reasons the caller cannot see.
     */
    public Optional<AllocationRow> findAllocation(String admin, String contractId) {
        return read("token-standard allocation " + contractId, () -> {
            DamlLedgerClient client = connection.get();
            Long end = client.getStateClient().getLedgerEnd().blockingGet();
            List<AllocationRow> hits = new ArrayList<>();
            client.getStateClient()
                    .getActiveContracts(ContractFilter.of(TokenStandardAllocation.COMPANION),
                            Set.of(admin), false, end)
                    .timeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .blockingForEach(active -> {
                        for (TokenStandardAllocation.Contract c : active.activeContracts) {
                            if (!c.id.contractId.equals(contractId)) {
                                continue;
                            }
                            var spec = c.data.allocation;
                            hits.add(new AllocationRow(
                                    c.id.contractId,
                                    spec.transferLeg.sender,
                                    spec.transferLeg.receiver,
                                    spec.transferLeg.instrumentId.id,
                                    spec.transferLeg.amount,
                                    spec.settlement.executor,
                                    spec.settlement.settlementRef.id,
                                    c.data.lockedHoldingCid.contractId,
                                    spec.settlement.settleBefore));
                        }
                    });
            return hits.stream().findFirst();
        });
    }

    /** The transfer instruction with this contract id, as the registry sees it — or empty. */
    public Optional<TransferOfferRow> findTransferOffer(String admin, String contractId) {
        return read("token-standard transfer instruction " + contractId, () -> {
            DamlLedgerClient client = connection.get();
            Long end = client.getStateClient().getLedgerEnd().blockingGet();
            List<TransferOfferRow> hits = new ArrayList<>();
            client.getStateClient()
                    .getActiveContracts(ContractFilter.of(TokenStandardTransferOffer.COMPANION),
                            Set.of(admin), false, end)
                    .timeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .blockingForEach(active -> {
                        for (TokenStandardTransferOffer.Contract c : active.activeContracts) {
                            if (!c.id.contractId.equals(contractId)) {
                                continue;
                            }
                            var t = c.data.transfer;
                            hits.add(new TransferOfferRow(
                                    c.id.contractId, t.sender, t.receiver, t.instrumentId.id,
                                    t.amount, c.data.lockedHoldingCid.contractId,
                                    t.executeBefore));
                        }
                    });
            return hits.stream().findFirst();
        });
    }

    // -----------------------------------------------------------------------
    // FOREIGN instruments — the wallet side of the standard
    // -----------------------------------------------------------------------
    // Everything above serves OUR registry. Everything below consumes SOMEBODY
    // ELSE'S. That is the direction that proves the standard is real: a
    // TransferInstruction created by BitSafe's registry, on BitSafe's own
    // template, in a package this backend has never seen, is nonetheless
    // readable and acceptable here — because both sides speak the same
    // interface. None of the code below mentions a concrete template.

    /**
     * One inbound or outbound {@code TransferInstruction}, whoever issued it.
     *
     * @param instrumentAdmin the registry that administers the instrument. THIS is what
     *                        decides where the accept choice context has to come from —
     *                        our own registry needs none, a foreign one may.
     * @param direction       {@code "inbound"} when the acting party is the receiver (it
     *                        can accept or reject), {@code "outbound"} when it is the
     *                        sender (it can withdraw)
     * @param status          the standard's own status, flattened to a label:
     *                        {@code PendingReceiverAcceptance} means the receiver's accept
     *                        is what the transfer is waiting for
     * @param ours            true when this desk administers the instrument, in which case
     *                        no foreign registry call is needed
     */
    public record PendingTransferRow(
            String contractId, String sender, String receiver, String instrumentAdmin,
            String instrumentId, BigDecimal amount, Instant requestedAt, Instant executeBefore,
            String status, String direction, boolean ours) {
    }

    /**
     * Every live {@code TransferInstruction} this party is a stakeholder of — <b>queried by
     * INTERFACE, never by template</b>.
     *
     * <p>That distinction is the whole point and it is easy to get wrong. A query filtered
     * on {@code TokenStandardDvp:TokenStandardTransferOffer} returns exactly nothing for a
     * transfer from another registry: BitSafe's instruction is a different template, in a
     * different package, written by someone else. Filtering on the INTERFACE
     * {@code Splice.Api.Token.TransferInstructionV1:TransferInstruction} matches any
     * template that implements it, and the interface VIEW gives back the same
     * {@code sender / receiver / amount / instrumentId / status} fields regardless of who
     * wrote the implementation. That is the portability CIP-56 is for, exercised rather
     * than described.
     */
    public List<PendingTransferRow> pendingTransfers(String party) {
        Optional<String> ourAdmin = quietAdminParty();
        return read("token-standard transfer instructions for " + party, () -> {
            DamlLedgerClient client = connection.get();
            Long end = client.getStateClient().getLedgerEnd().blockingGet();
            List<PendingTransferRow> out = new ArrayList<>();
            client.getStateClient()
                    .getActiveContracts(TransferInstruction.contractFilter(),
                            Set.of(party), false, end)
                    .timeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .blockingForEach(active -> {
                        for (var c : active.activeContracts) {
                            TransferInstructionView v = c.data;
                            String admin = v.transfer.instrumentId.admin;
                            String direction = party.equals(v.transfer.receiver) ? "inbound"
                                    : party.equals(v.transfer.sender) ? "outbound" : "observed";
                            out.add(new PendingTransferRow(
                                    c.id.contractId,
                                    v.transfer.sender,
                                    v.transfer.receiver,
                                    admin,
                                    v.transfer.instrumentId.id,
                                    v.transfer.amount,
                                    v.transfer.requestedAt,
                                    v.transfer.executeBefore,
                                    statusLabel(v.status),
                                    direction,
                                    ourAdmin.map(a -> a.equals(admin)).orElse(false)));
                        }
                    });
            out.sort(Comparator.comparing(PendingTransferRow::executeBefore));
            return out;
        });
    }

    /** One instruction by contract id, as {@code party} sees it. */
    public Optional<PendingTransferRow> findPendingTransfer(String party, String contractId) {
        return pendingTransfers(party).stream()
                .filter(r -> r.contractId().equals(contractId))
                .findFirst();
    }

    /**
     * The outcome of exercising a standard choice on a foreign contract.
     *
     * @param contextSource  where the {@code ChoiceContext} came from — a registry URL, or
     *                       the sentence explaining why it is empty. Reported to the caller
     *                       ON PURPOSE: "it worked with an empty context" and "it worked
     *                       with BitSafe's context" are different claims, and only one of
     *                       them can be repeated for a registry that needs one.
     * @param created        {@code Module:Entity cid} for every contract the update created
     *                       — for an accept, this is where the received holding appears
     */
    public record ExerciseOutcome(
            String updateId, List<String> created, String contextSource, int contextValues,
            int disclosedContracts) {
    }

    /**
     * Accept / reject / withdraw a {@code TransferInstruction}, whoever issued it.
     *
     * <p><b>The acting party is derived from the contract, not from the request.</b> The
     * standard fixes the controller of each choice — receiver for accept and reject, sender
     * for withdraw — so a caller who names the wrong party is told exactly that here,
     * rather than receiving an authorisation failure from the participant that (by Canton's
     * design) explains nothing.
     *
     * <p><b>The choice context is fetched from the instrument's OWN registry.</b> If this
     * desk administers the instrument, no call is made and the context is genuinely empty
     * ({@code TokenStandardTransferOffer} needs none). Otherwise, if a registry URL is
     * configured for that admin, its {@code choice-contexts/{choice}} endpoint is called
     * and both the context and its disclosed contracts are attached to the submission. If
     * no URL is configured, the choice is submitted with an EMPTY context and the fact is
     * recorded in the outcome — many registries need nothing, and trying is strictly better
     * than refusing, provided nobody is told a context was supplied when it was not.
     */
    public ExerciseOutcome exerciseTransferInstruction(
            String party, String contractId, String choice) {
        String actor = resolvePartyReference(party);
        PendingTransferRow row = findPendingTransfer(actor, contractId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "no live TransferInstruction " + contractId + " is visible to "
                                + LedgerService.labelOf(actor) + ". It may already have been "
                                + "accepted, rejected or withdrawn — every one of those choices "
                                + "archives it — or this party is not a stakeholder of it."));

        String required = "withdraw".equals(choice) ? row.sender() : row.receiver();
        if (!actor.equals(required)) {
            throw new IllegalArgumentException(
                    "the standard makes " + ("withdraw".equals(choice) ? "the SENDER" : "the RECEIVER")
                            + " the controller of TransferInstruction_"
                            + Character.toUpperCase(choice.charAt(0)) + choice.substring(1)
                            + ", so this must be submitted as " + LedgerService.labelOf(required)
                            + " (" + required + "), not " + LedgerService.labelOf(actor) + ".");
        }

        // ---- assemble the context -----------------------------------------
        ChoiceContext context = new ChoiceContext(Map.of());
        List<com.daml.ledger.javaapi.data.DisclosedContract> disclosed = List.of();
        String contextSource;
        if (row.ours()) {
            contextSource = "none needed — this desk administers " + row.instrumentId()
                    + " and its TransferInstruction implementation takes no registry context";
        } else {
            Optional<String> baseUrl = remote.baseUrlFor(row.instrumentAdmin());
            if (baseUrl.isPresent()) {
                RemoteRegistryClient.FetchedContext fetched =
                        remote.transferInstructionContext(baseUrl.get(), contractId, choice);
                context = fetched.context();
                disclosed = fetched.disclosed();
                contextSource = fetched.sourceUrl();
            } else {
                contextSource = "EMPTY — no registry URL is configured for instrument admin "
                        + row.instrumentAdmin() + ", so no choice context could be fetched. If "
                        + "this registry requires one the submission will be rejected by its "
                        + "own Daml code; set registry.remote-urls=\""
                        + LedgerService.labelOf(row.instrumentAdmin()) + "=https://<registry>\" "
                        + "(or registry.remote-url) and retry.";
                log.warn("REGISTRY accepting a FOREIGN instruction with an empty choice context: "
                        + "{}", contextSource);
            }
        }

        ExtraArgs extraArgs = new ExtraArgs(context, new Metadata(Map.of()));
        TransferInstruction.ContractId cid = new TransferInstruction.ContractId(contractId);
        HasCommands command = switch (choice) {
            case "accept" -> cid.exerciseTransferInstruction_Accept(extraArgs);
            case "reject" -> cid.exerciseTransferInstruction_Reject(extraArgs);
            case "withdraw" -> cid.exerciseTransferInstruction_Withdraw(extraArgs);
            default -> throw new IllegalArgumentException(
                    "choice must be accept, reject or withdraw — got '" + choice + "'");
        };

        log.info("REGISTRY {} TransferInstruction {} actAs={} instrument={}/{} amount={} "
                        + "context={} disclosed={}",
                choice, contractId, actor, row.instrumentAdmin(), row.instrumentId(),
                row.amount(), context.values.size(), disclosed.size());

        Transaction tx = submitAs(List.of(actor), command, disclosed);
        return new ExerciseOutcome(tx.getUpdateId(), createdSummary(tx), contextSource,
                context.values.size(), disclosed.size());
    }

    /** {@code Module:Entity cid} for every contract an update created. */
    private static List<String> createdSummary(Transaction tx) {
        List<String> out = new ArrayList<>();
        for (com.daml.ledger.javaapi.data.Event ev : tx.getEventsById().values()) {
            if (ev instanceof CreatedEvent created) {
                out.add(created.getTemplateId().getModuleName() + ":"
                        + created.getTemplateId().getEntityName() + " " + created.getContractId());
            }
        }
        return out;
    }

    /** The standard's {@code TransferInstructionStatus} variant, flattened for display. */
    private static String statusLabel(TransferInstructionStatus status) {
        if (status instanceof TransferPendingReceiverAcceptance) {
            return "PendingReceiverAcceptance";
        }
        if (status instanceof TransferPendingInternalWorkflow w) {
            // pendingActions tells a wallet user whether THEY are the one holding it up.
            return w.pendingActions.isEmpty()
                    ? "PendingInternalWorkflow"
                    : "PendingInternalWorkflow " + w.pendingActions.values();
        }
        return String.valueOf(status);
    }

    /** The registry admin if there is one, without letting its absence fail an unrelated read. */
    private Optional<String> quietAdminParty() {
        try {
            return adminParty();
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    // -----------------------------------------------------------------------
    // Registry operator actions (NOT part of the standard's API surface)
    // -----------------------------------------------------------------------

    /**
     * Create this registry's factory contract, or return the existing one.
     *
     * <p>Idempotent on purpose: two factory contracts for one admin would make
     * {@code factoryId} ambiguous, and a wallet that disclosed the wrong one would get a
     * bare authorisation failure with nothing to go on.
     *
     * @return the factory contract id
     */
    public String ensureRegistry(String adminReference) {
        String admin = ledger.resolveParty(adminReference);
        Optional<FactoryContract> existing = findFactory()
                .filter(f -> f.admin().equals(admin));
        if (existing.isPresent()) {
            log.info("REGISTRY factory already on ledger admin={} factoryId={}",
                    admin, existing.get().contractId());
            return existing.get().contractId();
        }
        String cid = ledger.submitForCreated(admin,
                LedgerCommands.createTokenStandardRegistry(admin),
                LedgerCommands.tokenStandardRegistryTemplateId());
        discoveredAdmin = admin;
        log.info("REGISTRY factory created admin={} factoryId={}", admin, cid);
        return cid;
    }

    /**
     * Mint a free {@code TokenStandardHolding} of one of this registry's instruments.
     *
     * <p>Needs TWO acting parties. The standard's holding is co-signed by the instrument
     * admin and the owner — that is the difference from the legacy issuer-only
     * {@code Holding}, and it means nobody can put an asset in your name without your
     * authority. The desk's usual single-party submit cannot express that, so this method
     * builds the submission itself; every other submission on this path still goes
     * through {@link LedgerService}.
     */
    public String mintHolding(String ownerReference, String instrumentId, BigDecimal amount) {
        String admin = adminParty().orElseThrow(() -> new LedgerService.LedgerException(
                "this desk administers no token-standard registry yet — create the factory "
                        + "first (POST /api/token-standard/registry)"));
        String owner = ledger.resolveParty(ownerReference);
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        Transaction tx = submitAs(List.of(admin, owner),
                LedgerCommands.createTokenStandardHolding(admin, owner, instrumentId, amount),
                List.of());
        return ledger.createdOf(tx, LedgerCommands.tokenStandardHoldingTemplateId())
                .stream().findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "the mint produced no TokenStandardHolding"));
    }

    /**
     * Submit one command under SEVERAL acting parties and with explicitly DISCLOSED
     * contracts, with the same instrumentation the rest of the desk uses: the command id is
     * generated up front so it can be logged BEFORE the call (a submission that never
     * returns leaves nothing else behind), and failures are classified by
     * {@link LedgerErrors} so the web layer answers 403/409/503 rather than an opaque 500.
     *
     * <p>Two things here that {@link LedgerService#submit} does not do, and both are
     * required by the token standard rather than by this desk:
     * <ul>
     *   <li><b>Multiple {@code actAs} parties</b> — a standard holding is co-signed by the
     *       instrument admin and the owner.</li>
     *   <li><b>Disclosed contracts</b> — a registry's choice context is delivered as
     *       contracts the acting party cannot otherwise see; attaching them is the whole
     *       mechanism.</li>
     * </ul>
     *
     * <p>The package-id selection preference pins the token-standard INTERFACE package to
     * the exact version this backend was compiled against. Interface exercise commands name
     * the interface by package NAME ({@code #splice-api-token-transfer-instruction-v1}),
     * which a participant resolves to whichever version it prefers; if a node ever vets a
     * newer minor, unpinned resolution could aim a v1.1 choice at a contract implementing
     * v1.0 and fail for a reason nothing in the response would explain.
     */
    private Transaction submitAs(List<String> actAs, HasCommands command,
            List<com.daml.ledger.javaapi.data.DisclosedContract> disclosed) {
        DamlLedgerClient client = connection.get();
        String commandId = UUID.randomUUID().toString();
        String applicationId = connection.properties().getApplicationId();
        String what = LedgerErrors.describe(command);

        CommandsSubmission submission = CommandsSubmission
                .create(applicationId, commandId, Optional.empty(), List.of(command))
                .withActAs(actAs);
        if (pinInterfacePackage) {
            submission = submission.withPackageIdSelectionPreference(
                    List.of(TransferInstruction.PACKAGE_ID));
        }
        if (disclosed != null && !disclosed.isEmpty()) {
            submission = submission.withDisclosedContracts(disclosed);
        }
        if (connection.hasActiveToken()) {
            // Travels in gRPC call metadata, never in the command — nothing logged here
            // can contain it.
            submission = submission.withAccessToken(connection.activeToken());
        }

        log.info("REGISTRY SUBMIT commandId={} applicationId={} actAs={} disclosed={} {} args={}",
                commandId, applicationId, actAs,
                disclosed == null ? 0 : disclosed.size(), what,
                LedgerErrors.describeArgs(command));
        try {
            Transaction tx = client.getCommandClient()
                    .submitAndWaitForTransaction(submission, ledgerEffectsFormat(actAs))
                    .timeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .blockingGet();
            log.info("REGISTRY OK     commandId={} updateId={} offset={}",
                    commandId, tx.getUpdateId(), tx.getOffset());
            return tx;
        } catch (RuntimeException e) {
            LedgerErrors.Failure f = LedgerErrors.of(e);
            log.error("REGISTRY FAIL   commandId={} actAs={} {} status={} correlationId={} "
                            + "description=\"{}\"",
                    commandId, actAs, what, f.codeLabel(), f.correlationId(),
                    LedgerErrors.truncate(f.description()));
            log.error("REGISTRY FAIL   commandId={} WHAT THIS MEANS: {}", commandId, f.hint());
            if (f.damlMessage() != null) {
                log.warn("REGISTRY FAIL   commandId={} THE MODEL REJECTED IT: {}",
                        commandId, f.damlMessage());
            }
            throw new LedgerService.LedgerException(
                    LedgerErrors.userMessage(f, commandId), e, f, commandId);
        }
    }

    /** LEDGER_EFFECTS transaction format scoped to every acting party. */
    private TransactionFormat ledgerEffectsFormat(List<String> parties) {
        Filter wildcard = new CumulativeFilter(
                Map.of(), Map.of(), Optional.of(Filter.Wildcard.HIDE_CREATED_EVENT_BLOB));
        Map<String, Filter> byParty = new LinkedHashMap<>();
        for (String p : parties) {
            byParty.put(p, wildcard);
        }
        return new TransactionFormat(
                new EventFormat(byParty, Optional.empty(), true), TransactionShape.LEDGER_EFFECTS);
    }

    // -----------------------------------------------------------------------
    // Small shared helpers
    // -----------------------------------------------------------------------

    /** Precision to report for an instrument. Fixed by Daml's {@code Decimal}, not by us. */
    public int decimals() {
        return DAML_DECIMAL_PLACES;
    }

    /**
     * Resolve a party reference the desk's usual way, but <b>accept a fully-qualified party
     * id the roster has never heard of</b>.
     *
     * <p>This matters for exactly the case the token standard is for: an asset arriving from
     * outside. {@link LedgerService#resolveParty} answers from the desk's own roster — the
     * admin service locally, a configured list on devnet — and a party that is real on the
     * participant but absent from that list would otherwise be unusable here. A string
     * carrying a {@code ::namespace} suffix is already the ledger's own identifier and needs
     * no translation; if it is wrong, the participant says so, which is the right authority
     * to hear it from. Labels still MUST resolve, because a label is only meaningful
     * against the roster.
     */
    public String resolvePartyReference(String reference) {
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("party is required");
        }
        String ref = reference.trim();
        try {
            return ledger.resolveParty(ref);
        } catch (RuntimeException e) {
            if (ref.contains("::")) {
                log.info("REGISTRY party {} is not in this desk's roster; using it verbatim "
                        + "(it is already a fully-qualified party id)", ref);
                return ref;
            }
            throw e;
        }
    }

    /**
     * The parties the factory-discovery read is addressed to: the pinned admin if one is
     * configured, else every party this desk knows about. An empty set means "let the
     * participant use every party it can read as", which is the right fallback on a node
     * whose roster this desk was not given.
     */
    private Set<String> readAsParties() {
        if (adminPin != null && !adminPin.isBlank()) {
            return Set.of(ledger.resolveParty(adminPin));
        }
        String cached = discoveredAdmin;
        if (cached != null) {
            return Set.of(cached);
        }
        try {
            Set<String> roster = new java.util.LinkedHashSet<>();
            for (LedgerService.PartyView p : ledger.listParties()) {
                roster.add(p.party());
            }
            return roster;
        } catch (RuntimeException e) {
            // A roster this desk cannot read is not fatal here — the participant can
            // still answer for every party it hosts.
            log.warn("REGISTRY could not list parties for factory discovery ({}); "
                    + "falling back to an any-party read", LedgerErrors.rootMessage(e));
            return Set.of();
        }
    }

    /**
     * The admin party of a factory create-event. Decoded from the payload, with the
     * contract's signatory set as the fallback — {@code TokenStandardRegistry} has
     * exactly one signatory and it is the admin, so the two can only agree.
     */
    private static String adminOf(CreatedEvent ce) {
        try {
            return TokenStandardRegistry.Contract.fromCreatedEvent(ce).data.admin;
        } catch (RuntimeException e) {
            return ce.getSignatories().stream().findFirst().orElse("");
        }
    }

    /**
     * Run an idempotent ledger read, retrying the transient stream failures a loaded
     * participant produces ({@code end-of-stream mid-frame}, {@code RESOURCE_EXHAUSTED})
     * and classifying anything else so it surfaces as the right HTTP status.
     */
    private <T> T read(String what, java.util.concurrent.Callable<T> op) {
        int attempts = 3;
        for (int i = 1; i <= attempts; i++) {
            try {
                return op.call();
            } catch (Exception e) {
                RuntimeException re = (e instanceof RuntimeException r) ? r : new RuntimeException(e);
                String msg = LedgerErrors.rootMessage(re);
                boolean transientErr = msg != null && (msg.contains("end-of-stream")
                        || msg.contains("RESOURCE_EXHAUSTED") || msg.contains("UNAVAILABLE"));
                if (!transientErr || i == attempts) {
                    LedgerErrors.Failure f = LedgerErrors.of(re);
                    log.error("REGISTRY READ FAIL ({}) after {} attempt(s) status={} "
                                    + "correlationId={} description=\"{}\"",
                            what, i, f.codeLabel(), f.correlationId(),
                            LedgerErrors.truncate(f.description()));
                    log.error("REGISTRY READ FAIL ({}) WHAT THIS MEANS: {}", what, f.hint());
                    if (LedgerErrors.grpcCause(re).isPresent()) {
                        throw new LedgerService.LedgerException(
                                LedgerErrors.userMessage(f, null) + " (while reading " + what + ")",
                                re, f, null);
                    }
                    throw re;
                }
                log.warn("REGISTRY transient read failure ({}), retry {}/{}: {}",
                        what, i, attempts, msg);
                try {
                    Thread.sleep(300L * i);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new LedgerService.LedgerException("interrupted while retrying " + what, ie);
                }
            }
        }
        throw new LedgerService.LedgerException("ledger read failed: " + what);
    }
}
