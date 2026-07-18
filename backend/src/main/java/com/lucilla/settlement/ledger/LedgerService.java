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
import com.lucilla.settlement.ledger.LedgerCommands;
import com.lucilla.settlement.model.basket.BasketDefinition;
import com.lucilla.settlement.model.basket.Component;
import com.lucilla.settlement.model.holding.Holding;
import com.lucilla.settlement.model.instrument.Instrument;
import com.lucilla.settlement.model.marketonclose.ClosingAuction;
import com.lucilla.settlement.model.marketonclose.ImbalanceDisclosure;
import com.lucilla.settlement.model.marketonclose.SealedOrder;
import com.lucilla.settlement.model.settlement.FillRecord;
import com.lucilla.settlement.model.settlement.SettlementBatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
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

    /** Holdings CREATED by a transaction (cid + amount), decoded from the tree. */
    public List<HoldingView> createdHoldingsOf(TransactionTree tree) {
        List<HoldingView> out = new ArrayList<>();
        for (TreeEvent ev : tree.getEventsById().values()) {
            if (ev instanceof CreatedEvent created
                    && sameTemplate(created.getTemplateId(), Holding.TEMPLATE_ID)) {
                Holding.Contract c = Holding.Contract.fromCreatedEvent(created);
                out.add(new HoldingView(c.id.contractId, c.data.issuer, c.data.instrumentId,
                        c.data.owner, c.data.amount, c.data.disclosedTo));
            }
        }
        return out;
    }

    /** The SettlementBatch created by a close transaction, decoded to a flat view. */
    public Optional<BatchView> batchOf(TransactionTree tree) {
        for (TreeEvent ev : tree.getEventsById().values()) {
            if (ev instanceof CreatedEvent created
                    && sameTemplate(created.getTemplateId(), SettlementBatch.TEMPLATE_ID)) {
                SettlementBatch b = SettlementBatch.Contract.fromCreatedEvent(created).data;
                List<FillView> fills = new ArrayList<>();
                for (FillRecord f : b.fills) {
                    // The operator is the momentary CCP: whichever side ISN'T the operator
                    // is the real trader, and that determines Buy vs Sell for display.
                    boolean isBuy = b.operator.equals(f.seller);
                    String trader = isBuy ? f.buyer : f.seller;
                    fills.add(new FillView(labelOf(trader), isBuy ? "Buy" : "Sell",
                            f.quantity, f.price));
                }
                return Optional.of(new BatchView(created.getContractId(), b.instrumentId,
                        b.closingPrice, fills));
            }
        }
        return Optional.empty();
    }

    // -----------------------------------------------------------------------
    // Holding provisioning (auto-resolve the exact/sufficient cid for a leg)
    // -----------------------------------------------------------------------

    /**
     * Return the contract id of a holding owned by {@code owner} of {@code instrumentId}
     * with EXACTLY {@code amount} units — splitting a larger holding (or merging
     * several smaller ones) as needed. This is what lets the desk offer a simple
     * "Buy 5 AAPL" without the caller hand-picking holding contract ids: a DvP leg
     * must match its holding amount exactly (Settlement.daml re-checks it).
     */
    public String provisionExactHolding(String owner, String instrumentId, BigDecimal amount) {
        List<HoldingView> mine = ownedHoldings(owner, instrumentId);
        if (mine.isEmpty()) {
            throw new LedgerException(labelOf(owner) + " holds no " + instrumentId);
        }
        for (HoldingView h : mine) {
            if (h.amount().compareTo(amount) == 0) {
                return h.contractId();
            }
        }
        BigDecimal total = mine.stream().map(HoldingView::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(amount) < 0) {
            throw new LedgerException(labelOf(owner) + " has only " + strip(total) + " "
                    + instrumentId + " but this trade needs " + strip(amount));
        }
        // Ascending: the smallest single holding that covers -> one split, minimal change.
        for (HoldingView h : mine) {
            if (h.amount().compareTo(amount) > 0) {
                return splitForExact(owner, h.contractId(), amount);
            }
        }
        // No single holding covers it: merge (descending) until it does, then split.
        String baseCid = mergeUntil(owner, mine, amount);
        HoldingView merged = ownedHoldings(owner, instrumentId).stream()
                .filter(h -> h.contractId().equals(baseCid)).findFirst().orElseThrow();
        if (merged.amount().compareTo(amount) == 0) {
            return baseCid;
        }
        return splitForExact(owner, baseCid, amount);
    }

    /**
     * Return the contract id of a SINGLE holding owned by {@code owner} of
     * {@code instrumentId} with AT LEAST {@code minAmount} units — merging smaller
     * holdings if necessary, but never splitting (the surplus rides along and is
     * returned as change when the leg settles). Used to pre-commit a MOC order.
     */
    public String provisionAtLeastHolding(String owner, String instrumentId, BigDecimal minAmount) {
        // Only FREE holdings back a new order: skip any slice already committed to
        // an order (disclosed to the venue). A sealed order reserves its exact
        // backing into a dedicated, venue-disclosed holding; re-using that slice for
        // a second order would consume the first order's pinned holding and make its
        // cid stale at the cross (the CONTRACT_NOT_FOUND bug). Picking only
        // uncommitted holdings keeps every resting order's backing valid.
        List<HoldingView> mine = ownedHoldings(owner, instrumentId).stream()
                .filter(h -> h.disclosedTo() == null || h.disclosedTo().isEmpty())
                .toList();
        if (mine.isEmpty()) {
            throw new LedgerException(labelOf(owner) + " has no uncommitted " + instrumentId
                    + " to commit (any balance is already reserved in a resting order)");
        }
        for (HoldingView h : mine) {                       // any single one already big enough
            if (h.amount().compareTo(minAmount) >= 0) {
                return h.contractId();
            }
        }
        BigDecimal total = mine.stream().map(HoldingView::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(minAmount) < 0) {
            throw new LedgerException(labelOf(owner) + " has only " + strip(total) + " "
                    + instrumentId + " but must commit " + strip(minAmount));
        }
        return mergeUntil(owner, mine, minAmount);
    }

    /** Owned (owner == party) holdings of one instrument, ascending by amount. */
    private List<HoldingView> ownedHoldings(String owner, String instrumentId) {
        List<HoldingView> mine = new ArrayList<>(holdingsVisibleTo(owner).stream()
                .filter(h -> h.owner().equals(owner) && h.instrumentId().equals(instrumentId))
                .toList());
        mine.sort(Comparator.comparing(HoldingView::amount));
        return mine;
    }

    /** Merge holdings (largest-first) into one cid until it holds >= target. */
    private String mergeUntil(String owner, List<HoldingView> mine, BigDecimal target) {
        List<HoldingView> desc = new ArrayList<>(mine);
        desc.sort(Comparator.comparing(HoldingView::amount).reversed());
        String baseCid = desc.get(0).contractId();
        BigDecimal running = desc.get(0).amount();
        for (int i = 1; i < desc.size() && running.compareTo(target) < 0; i++) {
            baseCid = submitForCreated(owner,
                    LedgerCommands.mergeHolding(baseCid, desc.get(i).contractId()),
                    LedgerCommands.holdingTemplateId());
            running = running.add(desc.get(i).amount());
        }
        return baseCid;
    }

    /** Split {@code holdingCid} and return the cid of the piece worth exactly {@code amount}. */
    private String splitForExact(String owner, String holdingCid, BigDecimal amount) {
        TransactionTree tree = submit(owner, LedgerCommands.splitHolding(holdingCid, amount));
        return createdHoldingsOf(tree).stream()
                .filter(h -> h.amount().compareTo(amount) == 0)
                .map(HoldingView::contractId).findFirst()
                .orElseThrow(() -> new LedgerException(
                        "split did not yield an exact " + strip(amount) + " holding"));
    }

    // -----------------------------------------------------------------------
    // Market-on-Close reads (as the venue/operator)
    // -----------------------------------------------------------------------

    /** Active ClosingAuctions visible to {@code party} (the operator sees its own). */
    public List<AuctionView> auctionsVisibleTo(String party) {
        return withRetry("auctions for " + party, () -> {
            DamlLedgerClient client = connection.get();
            ContractFilter<ClosingAuction.Contract> filter = ContractFilter.of(ClosingAuction.COMPANION);
            List<AuctionView> out = new ArrayList<>();
            client.getActiveContractSetClient()
                    .getActiveContracts(filter, Set.of(party), false)
                    .timeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .blockingForEach(active -> {
                        for (ClosingAuction.Contract c : active.activeContracts) {
                            ClosingAuction a = c.data;
                            out.add(new AuctionView(c.id.contractId, a.operator, a.instrumentId,
                                    a.cashInstrument, a.session, a.referencePrice, a.participants,
                                    a.liquidityProvider.orElse(null), a.isOpen));
                        }
                    });
            return out;
        });
    }

    /** Active SealedOrders visible to {@code party} (the operator signs every order). */
    public List<OrderView> sealedOrdersVisibleTo(String party) {
        return withRetry("orders for " + party, () -> {
            DamlLedgerClient client = connection.get();
            ContractFilter<SealedOrder.Contract> filter = ContractFilter.of(SealedOrder.COMPANION);
            List<OrderView> out = new ArrayList<>();
            client.getActiveContractSetClient()
                    .getActiveContracts(filter, Set.of(party), false)
                    .timeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .blockingForEach(active -> {
                        for (SealedOrder.Contract c : active.activeContracts) {
                            SealedOrder o = c.data;
                            out.add(new OrderView(c.id.contractId, o.operator, labelOf(o.trader),
                                    o.instrumentId, o.cashInstrument, o.session,
                                    o.side.toString().equalsIgnoreCase("BUY") ? "Buy" : "Sell",
                                    o.quantity, o.limitPrice));
                        }
                    });
            return out;
        });
    }

    /**
     * Active {@link ImbalanceDisclosure} contracts visible to {@code party}.
     *
     * <p>THIS IS THE ENFORCEMENT POINT of the selective-disclosure feature. An
     * ImbalanceDisclosure is signed by the venue and observed ONLY by the auction's
     * designated liquidity provider, so this query returns the net imbalance to the
     * DLP (and the venue) and NOTHING to any other trader — the ledger, not the web
     * layer, decides who is entitled to see the aggregate.
     */
    public List<ImbalanceView> imbalancesVisibleTo(String party) {
        return withRetry("imbalances for " + party, () -> {
            DamlLedgerClient client = connection.get();
            ContractFilter<ImbalanceDisclosure.Contract> filter =
                    ContractFilter.of(ImbalanceDisclosure.COMPANION);
            List<ImbalanceView> out = new ArrayList<>();
            client.getActiveContractSetClient()
                    .getActiveContracts(filter, Set.of(party), false)
                    .timeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .blockingForEach(active -> {
                        for (ImbalanceDisclosure.Contract c : active.activeContracts) {
                            ImbalanceDisclosure d = c.data;
                            out.add(new ImbalanceView(c.id.contractId, d.operator,
                                    labelOf(d.liquidityProvider), d.instrumentId, d.cashInstrument,
                                    d.session, d.netSide, d.netQuantity, d.referencePrice));
                        }
                    });
            return out;
        });
    }

    /** Published instruments (id, kind, reference price) visible to {@code party}. */
    public List<InstrumentView> instrumentsVisibleTo(String party) {
        return withRetry("instruments for " + party, () -> {
            DamlLedgerClient client = connection.get();
            ContractFilter<Instrument.Contract> filter = ContractFilter.of(Instrument.COMPANION);
            List<InstrumentView> out = new ArrayList<>();
            client.getActiveContractSetClient()
                    .getActiveContracts(filter, Set.of(party), false)
                    .timeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .blockingForEach(active -> {
                        for (Instrument.Contract c : active.activeContracts) {
                            Instrument in = c.data;
                            out.add(new InstrumentView(in.id, in.kind, in.description,
                                    in.referencePrice.orElse(null)));
                        }
                    });
            return out;
        });
    }

    /** Active BasketDefinitions visible to {@code party} (administrator + participants + auditor). */
    public List<BasketView> basketsVisibleTo(String party) {
        return withRetry("baskets for " + party, () -> {
            DamlLedgerClient client = connection.get();
            ContractFilter<BasketDefinition.Contract> filter = ContractFilter.of(BasketDefinition.COMPANION);
            List<BasketView> out = new ArrayList<>();
            client.getActiveContractSetClient()
                    .getActiveContracts(filter, Set.of(party), false)
                    .timeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .blockingForEach(active -> {
                        for (BasketDefinition.Contract c : active.activeContracts) {
                            BasketDefinition b = c.data;
                            List<ComponentView> comps = new ArrayList<>();
                            for (Component comp : b.components) {
                                comps.add(new ComponentView(comp.instrumentId, comp.unitsPerShare));
                            }
                            out.add(new BasketView(c.id.contractId, b.administrator, b.basketId,
                                    b.description, b.cashInstrument, comps, b.participants));
                        }
                    });
            return out;
        });
    }

    /** The published reference (close) price for an instrument id, or empty. */
    public Optional<BigDecimal> referencePriceOf(String issuerRef, String instrumentId) {
        return instrumentsVisibleTo(resolveParty(issuerRef)).stream()
                .filter(i -> i.id().equals(instrumentId))
                .map(InstrumentView::referencePrice)
                .filter(java.util.Objects::nonNull)
                .findFirst();
    }

    /** Friendly label (hint prefix before "::") for a full party id. */
    public static String labelOf(String party) {
        return (party != null && party.contains("::"))
                ? party.substring(0, party.indexOf("::")) : party;
    }

    private static String strip(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
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

    /** Flat, JSON-friendly view of a published Instrument. */
    public record InstrumentView(
            String id, String kind, String description, java.math.BigDecimal referencePrice) {
    }

    /** One component leg of a basket's creation unit. */
    public record ComponentView(String instrumentId, java.math.BigDecimal unitsPerShare) {
    }

    /** Flat, JSON-friendly view of a BasketDefinition (an ETF/fund). */
    public record BasketView(
            String contractId, String administrator, String basketId, String description,
            String cashInstrument, List<ComponentView> components, List<String> participants) {
    }

    /** Flat, JSON-friendly view of a ClosingAuction. */
    public record AuctionView(
            String contractId, String operator, String instrumentId, String cashInstrument,
            String session, java.math.BigDecimal referencePrice, List<String> participants,
            String liquidityProvider,   // full party id of the designated DLP, or null
            boolean isOpen) {
    }

    /** Flat, JSON-friendly view of an ImbalanceDisclosure (the aggregate only). */
    public record ImbalanceView(
            String contractId, String operator, String liquidityProvider, String instrumentId,
            String cashInstrument, String session, String netSide,
            java.math.BigDecimal netQuantity, java.math.BigDecimal referencePrice) {
    }

    /** Flat, JSON-friendly view of a resting SealedOrder (as seen by the operator). */
    public record OrderView(
            String contractId, String operator, String trader, String instrumentId,
            String cashInstrument, String session, String side, java.math.BigDecimal quantity,
            java.math.BigDecimal limitPrice) {
    }

    /** One fill from a settled batch, resolved to the trader's side. */
    public record FillView(
            String trader, String side, java.math.BigDecimal quantity, java.math.BigDecimal price) {
    }

    /** Flat view of a SettlementBatch and its fills. */
    public record BatchView(
            String contractId, String instrumentId, java.math.BigDecimal closingPrice,
            List<FillView> fills) {
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
