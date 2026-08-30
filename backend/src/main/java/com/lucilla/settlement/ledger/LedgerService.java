package com.lucilla.settlement.ledger;

import com.daml.ledger.javaapi.data.CommandsSubmission;
import com.daml.ledger.javaapi.data.ContractFilter;
import com.daml.ledger.javaapi.data.CreatedEvent;
import com.daml.ledger.javaapi.data.CumulativeFilter;
import com.daml.ledger.javaapi.data.Event;
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
import com.lucilla.settlement.model.basket.BasketDefinition;
import com.lucilla.settlement.model.basket.Component;
import com.lucilla.settlement.model.governance.NavFixing;
import com.lucilla.settlement.model.holding.Holding;
import com.lucilla.settlement.model.instrument.Instrument;
import com.lucilla.settlement.model.liquiditymandate.LiquidityMandate;
import com.lucilla.settlement.model.liquiditymandate.MandateTerms;
import com.lucilla.settlement.model.marketonclose.ClosingAuction;
import com.lucilla.settlement.model.marketonclose.ImbalanceDisclosure;
import com.lucilla.settlement.model.marketonclose.SealedOrder;
import com.lucilla.settlement.model.continuousbook.ContinuousBook;
import com.lucilla.settlement.model.continuousbook.RestingOrder;
import com.lucilla.settlement.model.continuousbook.TapePrint;
import com.lucilla.settlement.model.continuousbook.TradeConfirm;
import com.lucilla.settlement.model.perpetual.PerpMarket;
import com.lucilla.settlement.model.perpetual.PerpPosition;
import com.lucilla.settlement.model.settlement.FillRecord;
import com.lucilla.settlement.model.settlement.SettlementBatch;
import com.lucilla.settlement.model.settlement.SettlementReceipt;
import com.lucilla.settlement.model.basket.BasketReceipt;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
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
 * <p>Submissions use {@code submitAndWaitForTransaction} with the LEDGER_EFFECTS
 * shape so that, on success, the freshly-created contract id (the DvPAgreement from
 * an Accept, the successor ClosingAuction from a SubmitOrder, the SettlementBatch
 * from a close, …) can be pulled straight out of the resulting transaction and
 * handed back to the caller as the handle for the next step.
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
        Transaction tree = submit(actAs, command);
        return firstCreatedOf(tree, createdTemplate).orElseThrow(() ->
                new LedgerException("no " + createdTemplate.getEntityName()
                        + " contract was created by the transaction"));
    }

    /**
     * Submit a single command as {@code actAs} and block for the resulting transaction.
     *
     * <p><b>THIS IS THE INSTRUMENTED SEAM.</b> Every submission logs twice — once
     * BEFORE, once after — and the before-line is the important one, because a command
     * that never comes back leaves nothing else behind.
     *
     * <p>The before-line carries exactly what someone else needs to find this
     * submission in the PARTICIPANT'S log: the command id, the application/user id, the
     * acting party as a FULL party id (a label matches nothing on a real node), the
     * template and choice, and the key arguments. Canton refuses to tell the client why
     * an authorization check failed — "the exact reason is logged on the participant,
     * but not given to the user for security reasons" — so these handles are the only
     * bridge between a failure seen here and the explanation held there.
     */
    /*
     * THE DECENTRALIZED-PARTY SEAM. Every write in this application funnels through
     * this one method, which is why making the venue operator a threshold-governed
     * party is a change HERE and nowhere else.
     *
     * Today `actAs` is a single party holding a single key, and this submits directly.
     * Under a Canton Decentralized Party the id is unchanged - it is still an ordinary
     * `hint::fingerprint` - but the key behind that fingerprint is split across
     * independent operators, so a submission must satisfy a threshold before it reaches
     * the ledger. That is what Decentralization Manager's coordinator does: it gathers
     * the required signatures, then submits.
     *
     * So the change is: route through the coordinator instead of calling
     * submitAndWaitForTransaction directly, for the operator-controlled writes only
     * (CloseBidding, RunClose, ProcessCreation, ProcessRedemption - note FinalizeFixing
     * is NOT one: it is `controller proposer`, a committee member, not the operator).
     * The Daml does not
     * change at all - nothing in it inspects how a party's key is hosted.
     *
     * NOT IMPLEMENTED, and it cannot be until a participant exists: there is no party
     * to decentralize and nothing to run the coordinator against. The open question is
     * what threshold coordination costs in write latency when a fixing has a strike
     * time; the `in={}ms` figure logged below is the baseline that experiment measures
     * against. See BUSINESS/5-RUN-THE-COMMITTEE/ for the runbook.
     */
    public Transaction submit(String actAs, HasCommands command) {
        DamlLedgerClient client = connection.get();
        // Generated up-front (rather than inline) precisely so it can be LOGGED: this
        // string is what a node operator greps for.
        String commandId = UUID.randomUUID().toString();
        String applicationId = connection.properties().getApplicationId();
        String what = LedgerErrors.describe(command);

        CommandsSubmission submission = CommandsSubmission
                .create(applicationId,
                        commandId,
                        Optional.empty(),                 // v2: Optional<String> synchronizerId
                        List.of(command))
                .withActAs(actAs);
        if (connection.properties().hasJwt()) {
            // v2 takes the raw token, not an Optional. It goes into gRPC call metadata
            // and is never part of the command, so nothing below can log it.
            submission = submission.withAccessToken(connection.properties().getJwt());
        }

        log.info("LEDGER SUBMIT commandId={} applicationId={} actAs={} {} args={}",
                commandId, applicationId, actAs, what, LedgerErrors.describeArgs(command));
        long startedNanos = System.nanoTime();
        try {
            // Ledger API v2 removed the transaction-TREE command endpoints. We ask for a
            // flat Transaction with the LEDGER_EFFECTS shape, which still carries a
            // CreatedEvent for every contract the update creates — which is exactly what
            // firstCreatedOf / createdHoldingsOf / batchOf read back.
            Transaction tx = client.getCommandClient()
                    .submitAndWaitForTransaction(submission, ledgerEffectsFormat(actAs))
                    .timeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .blockingGet();
            long ms = (System.nanoTime() - startedNanos) / 1_000_000L;
            // The resulting contract ids are the HANDLES for the next step, so they are
            // logged as well as returned: a demo that stalls half way through a
            // propose → accept → settle chain is diagnosed from exactly this line.
            log.info("LEDGER OK     commandId={} updateId={} offset={} in={}ms created=[{}]",
                    commandId, tx.getUpdateId(), tx.getOffset(), ms, createdSummary(tx));
            recordSuccess(what, commandId, tx.getUpdateId());
            return tx;
        } catch (StatusRuntimeException e) {
            // CAUGHT SPECIFICALLY. The gRPC Status code and description are the only
            // machine-readable facts Canton hands back, and PERMISSION_DENIED,
            // NOT_FOUND, INVALID_ARGUMENT, ABORTED, UNAVAILABLE and CONTRACT_NOT_FOUND
            // each mean a different thing and are fixed in a different place.
            throw failedSubmission(e, commandId, actAs, what);
        } catch (RuntimeException e) {
            // rxjava and the gRPC futures both wrap; LedgerErrors looks THROUGH the
            // cause chain, so a wrapped StatusRuntimeException is classified identically
            // and a genuine non-gRPC failure still gets a clean message.
            throw failedSubmission(e, commandId, actAs, what);
        }
    }

    /**
     * Log a failed submission in full, then wrap it for the web layer.
     *
     * <p>Three log lines on purpose: WHAT failed and how, WHAT TO DO about it, and — for
     * a Daml rejection — the model's own sentence on its own line so it is impossible to
     * miss among the gRPC scaffolding. The command id repeats on every line so that one
     * {@code grep} for it returns the whole story.
     */
    private LedgerException failedSubmission(
            RuntimeException e, String commandId, String actAs, String what) {
        LedgerErrors.Failure f = LedgerErrors.of(e);
        log.error("LEDGER FAIL   commandId={} actAs={} {} status={} grpc={} correlationId={} "
                        + "at={} description=\"{}\"",
                commandId, actAs, what, f.codeLabel(),
                f.code() == null ? "-" : f.code().name(), f.correlationId(),
                Instant.now(), LedgerErrors.truncate(f.description()));
        log.error("LEDGER FAIL   commandId={} WHAT THIS MEANS: {}", commandId, f.hint());
        if (f.damlMessage() != null) {
            log.warn("LEDGER FAIL   commandId={} THE MODEL REJECTED IT: {}",
                    commandId, f.damlMessage());
        }
        return new LedgerException(LedgerErrors.userMessage(f, commandId), e, f, commandId);
    }

    /** The contracts a transaction created, as {@code Entity cid} pairs, for the OK line. */
    private static String createdSummary(Transaction tree) {
        List<String> out = new ArrayList<>();
        for (Event ev : tree.getEventsById().values()) {
            if (ev instanceof CreatedEvent created) {
                if (out.size() >= 10) {
                    out.add("…more");
                    break;
                }
                out.add(created.getTemplateId().getEntityName() + " " + created.getContractId());
            }
        }
        return String.join(", ", out);
    }

    // -----------------------------------------------------------------------
    // "Did this ever work?" — the first question worth asking mid-demo
    // -----------------------------------------------------------------------

    /** The most recent ledger interaction that SUCCEEDED. Reported by {@code GET /api/diag}. */
    private volatile LastCall lastCall;

    /**
     * When the ledger last answered successfully, and what the call was. This is what
     * separates "never connected" from "worked until thirty seconds ago" — two failures
     * that look identical from the UI and have nothing else in common.
     */
    public Optional<LastCall> lastSuccessfulCall() {
        return Optional.ofNullable(lastCall);
    }

    private void recordSuccess(String what, String commandId, String updateId) {
        lastCall = new LastCall(what, commandId, updateId, Instant.now());
    }

    /** One successful ledger interaction. {@code commandId}/{@code updateId} are null for reads. */
    public record LastCall(String what, String commandId, String updateId, Instant at) {
    }

    /**
     * A LEDGER_EFFECTS transaction format scoped to {@code party}, with a wildcard
     * (all-templates) filter — the v2 replacement for the old transaction-tree result.
     * LEDGER_EFFECTS gives the full set of created/exercised nodes (not just the ACS
     * delta), which is what the decoders below need to recover freshly-created cids.
     */
    private TransactionFormat ledgerEffectsFormat(String party) {
        Filter wildcard = new CumulativeFilter(
                java.util.Map.of(), java.util.Map.of(),
                Optional.of(Filter.Wildcard.HIDE_CREATED_EVENT_BLOB));
        EventFormat eventFormat = new EventFormat(
                java.util.Map.of(party, wildcard), Optional.empty(), true);
        return new TransactionFormat(eventFormat, TransactionShape.LEDGER_EFFECTS);
    }

    /** Holdings CREATED by a transaction (cid + amount), decoded from the tree. */
    public List<HoldingView> createdHoldingsOf(Transaction tree) {
        List<HoldingView> out = new ArrayList<>();
        for (Event ev : tree.getEventsById().values()) {
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
    public Optional<BatchView> batchOf(Transaction tree) {
        for (Event ev : tree.getEventsById().values()) {
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
        Transaction tree = submit(owner, LedgerCommands.splitHolding(holdingCid, amount));
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
            client.getStateClient()
                    .getActiveContracts(filter, Set.of(party), false, client.getStateClient().getLedgerEnd().blockingGet())
                    .timeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .blockingForEach(active -> {
                        for (ClosingAuction.Contract c : active.activeContracts) {
                            ClosingAuction a = c.data;
                            out.add(new AuctionView(c.id.contractId, a.operator, a.instrumentId,
                                    a.cashInstrument, a.session, a.referencePrice, a.participants,
                                    a.liquidityProvider.orElse(null),
                                    a.submittedCount, a.cancelledCount, a.isOpen));
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
            client.getStateClient()
                    .getActiveContracts(filter, Set.of(party), false, client.getStateClient().getLedgerEnd().blockingGet())
                    .timeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .blockingForEach(active -> {
                        for (SealedOrder.Contract c : active.activeContracts) {
                            SealedOrder o = c.data;
                            out.add(new OrderView(c.id.contractId, o.operator, labelOf(o.trader),
                                    o.instrumentId, o.cashInstrument, o.session,
                                    o.side.toString().equalsIgnoreCase("BUY") ? "Buy" : "Sell",
                                    // UNWRAP THE ORDER TYPE. `None` is an unpriced
                                    // market-on-close order, carried onwards as a null
                                    // limit — never coerced to 0 or to the anchor, both
                                    // of which would read as a real (and wrong) price.
                                    o.quantity, o.limitPrice.orElse(null)));
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
            client.getStateClient()
                    .getActiveContracts(filter, Set.of(party), false, client.getStateClient().getLedgerEnd().blockingGet())
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

    // -----------------------------------------------------------------------
    // The contestable liquidity mandate: the posted offer and the obligation
    // -----------------------------------------------------------------------

    /**
     * Open {@link MandateTerms} — the seat, POSTED where every eligible participant
     * can take it.
     *
     * <p>The venue signs the terms and every party on {@code eligible} observes them
     * (plus the auditor), so this query returns the offer to anyone entitled to accept
     * it and nothing to an outsider. That visibility IS the contestability: an open
     * offer nobody can see is not an open offer, and the ledger — not this class —
     * decides who sees it.
     */
    public List<MandateTermsView> mandateTermsVisibleTo(String party) {
        return withRetry("mandate terms for " + party, () -> {
            DamlLedgerClient client = connection.get();
            ContractFilter<MandateTerms.Contract> filter = ContractFilter.of(MandateTerms.COMPANION);
            List<MandateTermsView> out = new ArrayList<>();
            client.getStateClient()
                    .getActiveContracts(filter, Set.of(party), false, client.getStateClient().getLedgerEnd().blockingGet())
                    .timeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .blockingForEach(active -> {
                        for (MandateTerms.Contract c : active.activeContracts) {
                            MandateTerms t = c.data;
                            out.add(new MandateTermsView(c.id.contractId, t.operator,
                                    t.instrumentId, t.cashInstrument, t.session, t.anchorPrice,
                                    t.commitmentSize, t.maxBandBps, t.expiresAt,
                                    t.eligible, t.accepted, t.barred));
                        }
                    });
            return out;
        });
    }

    /**
     * Live {@link LiquidityMandate} obligations visible to {@code party}.
     *
     * <p>Operator AND provider are both signatories (the auditor observes), which is
     * exactly the difference between this and the old {@code liquidityProvider} field:
     * the obligation could not exist unless the provider's authority was present when
     * it was created. So querying as a PROVIDER returns that provider's own mandates
     * and no others, and querying as the VENUE returns every mandate over its books —
     * which is what lets the desk resolve "who may be shown this residual" server-side
     * instead of trusting a contract id off the wire.
     *
     * <p>Note {@code expiresAt} is returned raw and NOT filtered here. Liveness is the
     * LEDGER's call ({@code mandateIsLive} runs on ledger time inside the choice); this
     * layer reports the fact and lets the caller say something precise about it.
     */
    public List<MandateView> mandatesVisibleTo(String party) {
        return withRetry("mandates for " + party, () -> {
            DamlLedgerClient client = connection.get();
            ContractFilter<LiquidityMandate.Contract> filter =
                    ContractFilter.of(LiquidityMandate.COMPANION);
            List<MandateView> out = new ArrayList<>();
            client.getStateClient()
                    .getActiveContracts(filter, Set.of(party), false, client.getStateClient().getLedgerEnd().blockingGet())
                    .timeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .blockingForEach(active -> {
                        for (LiquidityMandate.Contract c : active.activeContracts) {
                            LiquidityMandate m = c.data;
                            out.add(new MandateView(c.id.contractId, m.operator, m.provider,
                                    m.instrumentId, m.cashInstrument, m.session, m.anchorPrice,
                                    m.commitmentSize, m.maxBandBps, m.expiresAt, m.acceptedAt,
                                    m.shownSide, m.peakShownQty, m.lastShownAt.orElse(null),
                                    m.disclosuresSeen));
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
            client.getStateClient()
                    .getActiveContracts(filter, Set.of(party), false, client.getStateClient().getLedgerEnd().blockingGet())
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
            client.getStateClient()
                    .getActiveContracts(filter, Set.of(party), false, client.getStateClient().getLedgerEnd().blockingGet())
                    .timeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .blockingForEach(active -> {
                        for (BasketDefinition.Contract c : active.activeContracts) {
                            BasketDefinition b = c.data;
                            List<ComponentView> comps = new ArrayList<>();
                            for (Component comp : b.components) {
                                comps.add(new ComponentView(comp.instrumentId, comp.unitsPerShare,
                                        comp.expectedIssuer));
                            }
                            out.add(new BasketView(c.id.contractId, b.administrator, b.basketId,
                                    b.description, b.cashInstrument, comps, b.participants,
                                    b.feeReceiver, b.creationFee, b.redemptionFee));
                        }
                    });
            return out;
        });
    }

    /**
     * Settlement + basket receipts VISIBLE to {@code party} — queried AS that party, so the
     * ledger itself decides who sees each receipt (need-to-know audit). A DvP receipt is seen
     * by its two principals + the auditor; an auction fill by the venue + traders + auditor; a
     * basket create/redeem by the AP + administrator + auditor. An outsider sees NOTHING.
     */
    public List<ReceiptView> receiptsVisibleTo(String party) {
        List<ReceiptView> out = new ArrayList<>();
        out.addAll(withRetry("settlement receipts for " + party, () -> {
            DamlLedgerClient client = connection.get();
            ContractFilter<SettlementReceipt.Contract> filter = ContractFilter.of(SettlementReceipt.COMPANION);
            List<ReceiptView> rs = new ArrayList<>();
            client.getStateClient()
                    .getActiveContracts(filter, Set.of(party), false, client.getStateClient().getLedgerEnd().blockingGet())
                    .timeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .blockingForEach(active -> {
                        for (SettlementReceipt.Contract c : active.activeContracts) {
                            SettlementReceipt r = c.data;
                            boolean auction = r.venue.isPresent();
                            List<String> vis = new ArrayList<>();
                            if (auction) {
                                vis.add(labelOf(r.venue.get()));
                            }
                            vis.add(labelOf(r.seller));
                            vis.add(labelOf(r.buyer));
                            vis.add(labelOf(r.auditor));
                            rs.add(new ReceiptView(
                                    c.id.contractId,
                                    auction ? "Auction fill" : "DvP",
                                    labelOf(r.seller) + " → " + labelOf(r.buyer) + " · "
                                            + strip(r.assetAmount) + " " + r.assetInstrument + " @ "
                                            + strip(r.unitPrice) + " " + r.cashInstrument,
                                    r.settledAt.toString(),
                                    vis.stream().distinct().toList()));
                        }
                    });
            return rs;
        }));
        out.addAll(withRetry("basket receipts for " + party, () -> {
            DamlLedgerClient client = connection.get();
            ContractFilter<BasketReceipt.Contract> filter = ContractFilter.of(BasketReceipt.COMPANION);
            List<ReceiptView> rs = new ArrayList<>();
            client.getStateClient()
                    .getActiveContracts(filter, Set.of(party), false, client.getStateClient().getLedgerEnd().blockingGet())
                    .timeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .blockingForEach(active -> {
                        for (BasketReceipt.Contract c : active.activeContracts) {
                            BasketReceipt b = c.data;
                            rs.add(new ReceiptView(
                                    c.id.contractId,
                                    b.action,
                                    labelOf(b.ap) + " " + ("Creation".equals(b.action) ? "created" : "redeemed")
                                            + " " + strip(b.shares) + " " + b.basketId,
                                    b.settledAt.toString(),
                                    List.of(labelOf(b.administrator), labelOf(b.ap), labelOf(b.auditor))));
                        }
                    });
            return rs;
        }));
        return out;
    }

    /**
     * Official {@code NavFixing}s visible to {@code party} — the committee-attested
     * marks, WITH THE ACCRUAL RECIPE ON THEM.
     *
     * <p>Read as that party, so the ledger decides who sees which fix: a NavFixing is
     * signed by its attestors and observed by the auditor plus exactly the venues it
     * was published to. An outsider gets nothing, which is the point — governance
     * without leakage.
     *
     * <p>Each view carries the four ATTESTED INPUTS ({@code price}, {@code ratePerAnnum},
     * {@code dayCount}, {@code accrualFrom}) rather than a single number, because the
     * value now is DERIVED from them and not stored anywhere. {@link Accrual#navAt} is
     * the derivation, and it is the same arithmetic the ledger runs.
     */
    /**
     * Every cessation notice this party can see — docs/FIXING_METHODOLOGY.md §8.
     *
     * <p>Read as the acting party, so the ledger's own disclosure rules apply: a notice
     * reaches the administrator, the auditor and the desks it named. That is deliberate
     * and is the whole point of the contract — a cessation notice the referencing desks
     * cannot read is not a notice, it is a filing.
     */
    public List<CessationView> cessationNoticesVisibleTo(String party) {
        return withRetry("cessation notices for " + party, () -> {
            DamlLedgerClient client = connection.get();
            ContractFilter<com.lucilla.settlement.model.governance.CessationNotice.Contract> filter =
                    ContractFilter.of(com.lucilla.settlement.model.governance.CessationNotice.COMPANION);
            List<CessationView> out = new ArrayList<>();
            client.getStateClient()
                    .getActiveContracts(filter, Set.of(party), false,
                            client.getStateClient().getLedgerEnd().blockingGet())
                    .timeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .blockingForEach(active -> {
                        for (com.lucilla.settlement.model.governance.CessationNotice.Contract c
                                : active.activeContracts) {
                            var n = c.data;
                            out.add(new CessationView(c.id.contractId, n.instrumentId, n.session,
                                    n.publishedAt, n.finalStrike, n.successor.orElse(null),
                                    n.reason, n.notifyTo));
                        }
                    });
            // Soonest final strike first: the one about to bite is the one a consumer
            // needs to see, not the one furthest away.
            out.sort(java.util.Comparator.comparing(CessationView::finalStrike));
            return out;
        });
    }

    public List<NavFixingView> navFixingsVisibleTo(String party) {
        return withRetry("nav fixings for " + party, () -> {
            DamlLedgerClient client = connection.get();
            ContractFilter<NavFixing.Contract> filter = ContractFilter.of(NavFixing.COMPANION);
            List<NavFixingView> out = new ArrayList<>();
            client.getStateClient()
                    .getActiveContracts(filter, Set.of(party), false, client.getStateClient().getLedgerEnd().blockingGet())
                    .timeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .blockingForEach(active -> {
                        for (NavFixing.Contract c : active.activeContracts) {
                            NavFixing f = c.data;
                            out.add(new NavFixingView(c.id.contractId, f.attestors, f.threshold,
                                    f.instrumentId, f.cashInstrument, f.session, f.price, f.rationale,
                                    f.ratePerAnnum, f.dayCount, f.accrualFrom, f.publishedTo,
                                    f.finalizedAt,
                                    f.tier.orElse(null),
                                    f.referencePrice.orElse(null),
                                    f.wrapperFactor.orElse(null),
                                    f.supersedes.map(cid -> cid.contractId).orElse(null),
                                    f.restatementReason.orElse(null)));
                        }
                    });
            return out;
        });
    }

    /**
     * One official fix by contract id, as {@code party} sees it — or a clear 400 if that
     * party cannot see it (which on this ledger is indistinguishable from, and reported
     * as, "no such fix").
     */
    public NavFixingView navFixingById(String party, String contractId) {
        return navFixingsVisibleTo(party).stream()
                .filter(f -> f.contractId().equals(contractId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "no official NavFixing " + contractId + " is visible to " + labelOf(party)));
    }

    /**
     * The live {@link Instrument} contract for {@code instrumentId}, with the party that
     * may republish its mark.
     *
     * <p>{@code InstrumentView} deliberately carries no contract id (it is the UI's
     * reference-data shape). Exercising {@code SetReferencePrice} needs one, and needs to
     * know WHO signs — only the issuer controls that choice — so this returns both.
     */
    public Optional<InstrumentRef> instrumentRefOf(String party, String instrumentId) {
        return withRetry("instrument ref " + instrumentId + " for " + party, () -> {
            DamlLedgerClient client = connection.get();
            ContractFilter<Instrument.Contract> filter = ContractFilter.of(Instrument.COMPANION);
            List<InstrumentRef> out = new ArrayList<>();
            client.getStateClient()
                    .getActiveContracts(filter, Set.of(party), false,
                            client.getStateClient().getLedgerEnd().blockingGet())
                    .timeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .blockingForEach(active -> {
                        for (Instrument.Contract c : active.activeContracts) {
                            if (c.data.id.equals(instrumentId)) {
                                out.add(new InstrumentRef(c.id.contractId, c.data.issuer, c.data.id));
                            }
                        }
                    });
            return out.stream().findFirst();
        });
    }

    /** An instrument's live contract id plus the issuer who controls its mark. */
    public record InstrumentRef(String contractId, String issuer, String instrumentId) {
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
    public List<String> createdOf(Transaction tree, Identifier template) {
        List<String> ids = new ArrayList<>();
        for (Event ev : tree.getEventsById().values()) {
            if (ev instanceof CreatedEvent created && sameTemplate(created.getTemplateId(), template)) {
                ids.add(created.getContractId());
            }
        }
        return ids;
    }

    /**
     * THE VALUE A CHOICE ACTUALLY RETURNED, pulled out of the transaction it ran in.
     *
     * <p>Some choices compute a number the caller cannot recompute honestly — a
     * settlement's payout, for instance, where the ledger clamps a negative equity to
     * zero because the insurance pool absorbs the shortfall. Recomputing that
     * client-side reports a loss the trader never took. This reads the number the
     * ledger actually produced.
     *
     * <p>Returns the LAST {@code Numeric} found in the exercise result, which is the
     * shape these choices use: a tuple whose final field is the amount. Empty when the
     * transaction shape does not carry a result, so callers keep their fallback.
     */
    public Optional<java.math.BigDecimal> exerciseResultDecimalOf(
            Transaction tree, Identifier template, String choiceName) {
        for (Event ev : tree.getEventsById().values()) {
            if (!(ev instanceof com.daml.ledger.javaapi.data.ExercisedEvent ex)) {
                continue;
            }
            if (!sameTemplate(ex.getTemplateId(), template)
                    || !ex.getChoice().equals(choiceName)) {
                continue;
            }
            Optional<java.math.BigDecimal> found = lastNumericIn(ex.getExerciseResult());
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    /** Depth-first for the last Numeric in a (possibly nested) result value. */
    private static Optional<java.math.BigDecimal> lastNumericIn(
            com.daml.ledger.javaapi.data.Value v) {
        if (v == null) {
            return Optional.empty();
        }
        if (v instanceof com.daml.ledger.javaapi.data.Numeric n) {
            return Optional.of(n.getValue());
        }
        if (v instanceof com.daml.ledger.javaapi.data.DamlRecord rec) {
            java.math.BigDecimal last = null;
            for (var f : rec.getFields()) {
                var inner = lastNumericIn(f.getValue());
                if (inner.isPresent()) {
                    last = inner.get();
                }
            }
            return Optional.ofNullable(last);
        }
        return Optional.empty();
    }

    private Optional<String> firstCreatedOf(Transaction tree, Identifier template) {
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
     * live here for the UI's party picker. This is why the LOCAL build keeps the
     * admin channel that backend-devnet does without: a local sandbox mints a new
     * namespace on every run, so a configured roster would go stale immediately.
     *
     * <p>Ledger API v2 dropped {@code display_name} from {@code PartyDetails}, so the
     * friendly {@code label} is the party hint — the prefix before {@code "::"}.
     */
    public List<PartyView> listParties() {
        return withRetry("list parties", () -> {
            var stub = connection.partyManagement();
            var req = com.daml.ledger.api.v2.admin.PartyManagementServiceOuterClass
                    .ListKnownPartiesRequest.getDefaultInstance();
            var resp = stub
                    .withDeadlineAfter(30, java.util.concurrent.TimeUnit.SECONDS)
                    .listKnownParties(req);
            List<PartyView> out = new ArrayList<>();
            for (var pd : resp.getPartyDetailsList()) {
                String party = pd.getParty();
                String label = party.contains("::")
                        ? party.substring(0, party.indexOf("::")) : party;
                out.add(new PartyView(party, "", label, pd.getIsLocal()));
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
            client.getStateClient()
                    .getActiveContracts(filter, Set.of(party), false, client.getStateClient().getLedgerEnd().blockingGet())
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
        // 6 attempts with growing backoff: the local sandbox's ACS stream drops mid-frame under
        // host load ("end-of-stream mid-frame"); a pure read is safe to retry, so we give it more
        // chances (and more time to recover) before surfacing the error to the UI.
        int attempts = 6;
        RuntimeException last = null;
        for (int i = 1; i <= attempts; i++) {
            try {
                T result = op.call();
                recordSuccess("read " + what, null, null);
                return result;
            } catch (Exception e) {
                RuntimeException re = (e instanceof RuntimeException r) ? r : new RuntimeException(e);
                // Classify BEFORE deciding: a PERMISSION_DENIED on a read is not a
                // transient stream drop and must not be retried six times before
                // surfacing as an unexplained 500.
                LedgerErrors.Failure f = LedgerErrors.of(re);
                String msg = rootMessage(re);
                boolean transientErr = msg != null && (msg.contains("end-of-stream")
                        || msg.contains("RESOURCE_EXHAUSTED") || msg.contains("UNAVAILABLE")
                        || msg.contains("INTERNAL"));
                if (!transientErr || i == attempts) {
                    log.error("LEDGER READ FAIL ({}) after {} attempt(s) status={} grpc={} "
                                    + "correlationId={} description=\"{}\"",
                            what, i, f.codeLabel(), f.code() == null ? "-" : f.code().name(),
                            f.correlationId(), LedgerErrors.truncate(f.description()));
                    log.error("LEDGER READ FAIL ({}) WHAT THIS MEANS: {}", what, f.hint());
                    if (transientErr) {
                        throw new LedgerException("ledger read failed after " + attempts
                                + " attempts (" + what + "): " + msg, re, f, null);
                    }
                    if (f.code() != null) {
                        // A real gRPC rejection on a read: classified, so the web layer
                        // can answer 403/404/503 instead of an opaque 500.
                        throw new LedgerException(
                                LedgerErrors.userMessage(f, null) + " (while reading " + what + ")",
                                re, f, null);
                    }
                    throw re;   // not a ledger failure at all — let it surface as itself
                }
                last = re;
                log.warn("Transient ledger read failure ({}), retry {}/{}: {}", what, i, attempts, msg);
                try {
                    Thread.sleep(300L * i);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new LedgerException("interrupted while retrying " + what, ie);
                }
            }
        }
        throw last != null ? last : new LedgerException("read failed: " + what);
    }

    private static String rootMessage(Throwable t) {
        return LedgerErrors.rootMessage(t);
    }

    // =====================================================================
    // THE CONTINUOUS SESSION — party-aware reads
    // =====================================================================
    // Same discipline as the auction: the ACS is queried AS the acting party, so
    // Daml's own visibility does the filtering and the web layer never decides who
    // may see what.

    /** Active {@link ContinuousBook} sessions visible to {@code party}. */
    public List<BookView> continuousBooksVisibleTo(String party) {
        return withRetry("continuous books for " + party, () -> {
            DamlLedgerClient client = connection.get();
            ContractFilter<ContinuousBook.Contract> filter = ContractFilter.of(ContinuousBook.COMPANION);
            List<BookView> out = new ArrayList<>();
            client.getStateClient()
                    .getActiveContracts(filter, Set.of(party), false,
                            client.getStateClient().getLedgerEnd().blockingGet())
                    .timeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .blockingForEach(active -> {
                        for (ContinuousBook.Contract c : active.activeContracts) {
                            ContinuousBook b = c.data;
                            out.add(new BookView(c.id.contractId, b.operator, b.instrumentId,
                                    b.cashInstrument, b.referencePrice, b.bandFraction,
                                    b.nextSeq, b.liveCount, b.isOpen, b.participants));
                        }
                    });
            return out;
        });
    }

    /**
     * Active {@link RestingOrder}s visible to {@code party} — THE DARK-POOL PROPERTY.
     *
     * <p>A RestingOrder is signed by operator + trader and observed by <b>nobody</b>.
     * So the venue sees the whole book (it signs every order), a trader sees only its
     * own, and the auditor — who sees the session and the tape — sees <b>none</b> of
     * it while it rests. That asymmetry is enforced by the ledger, not by this method.
     */
    public List<RestingOrderView> restingOrdersVisibleTo(String party) {
        return withRetry("resting orders for " + party, () -> {
            DamlLedgerClient client = connection.get();
            ContractFilter<RestingOrder.Contract> filter = ContractFilter.of(RestingOrder.COMPANION);
            List<RestingOrderView> out = new ArrayList<>();
            client.getStateClient()
                    .getActiveContracts(filter, Set.of(party), false,
                            client.getStateClient().getLedgerEnd().blockingGet())
                    .timeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .blockingForEach(active -> {
                        for (RestingOrder.Contract c : active.activeContracts) {
                            RestingOrder o = c.data;
                            out.add(new RestingOrderView(c.id.contractId, o.operator, o.trader,
                                    labelOf(o.trader), o.instrumentId, o.cashInstrument,
                                    o.side.toString().equalsIgnoreCase("BID") ? "Bid" : "Ask",
                                    o.quantity,
                                    // An absent limit is a MARKET order, carried onwards as
                                    // null — never coerced to 0 or to the reference, both of
                                    // which would read as a real (and wrong) price.
                                    o.limitPrice.orElse(null),
                                    o.timeInForce.toString(), o.seqNo));
                        }
                    });
            return out;
        });
    }

    /** The public tape — every print, visible to the whole session, naming nobody. */
    public List<TapeView> tapePrintsVisibleTo(String party) {
        return withRetry("tape for " + party, () -> {
            DamlLedgerClient client = connection.get();
            ContractFilter<TapePrint.Contract> filter = ContractFilter.of(TapePrint.COMPANION);
            List<TapeView> out = new ArrayList<>();
            client.getStateClient()
                    .getActiveContracts(filter, Set.of(party), false,
                            client.getStateClient().getLedgerEnd().blockingGet())
                    .timeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .blockingForEach(active -> {
                        for (TapePrint.Contract c : active.activeContracts) {
                            TapePrint t = c.data;
                            out.add(new TapeView(c.id.contractId, t.instrumentId, t.cashInstrument,
                                    t.price, t.quantity, t.printedAt, t.matchSeq));
                        }
                    });
            return out;
        });
    }

    /**
     * The venue's bilateral confirms to {@code party}. A confirm never names the
     * other trader — the venue stood between them as momentary counterparty.
     */
    public List<ConfirmView> tradeConfirmsVisibleTo(String party) {
        return withRetry("confirms for " + party, () -> {
            DamlLedgerClient client = connection.get();
            ContractFilter<TradeConfirm.Contract> filter = ContractFilter.of(TradeConfirm.COMPANION);
            List<ConfirmView> out = new ArrayList<>();
            client.getStateClient()
                    .getActiveContracts(filter, Set.of(party), false,
                            client.getStateClient().getLedgerEnd().blockingGet())
                    .timeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .blockingForEach(active -> {
                        for (TradeConfirm.Contract c : active.activeContracts) {
                            TradeConfirm t = c.data;
                            out.add(new ConfirmView(c.id.contractId, labelOf(t.trader),
                                    t.instrumentId, t.cashInstrument,
                                    t.side.toString().equalsIgnoreCase("BID") ? "Bid" : "Ask",
                                    t.quantity, t.price, t.cashAmount, t.liquidity, t.tradedAt));
                        }
                    });
            return out;
        });
    }

    // =====================================================================
    // LEVERAGED LONG / SHORT — party-aware reads
    // =====================================================================

    /** Perpetual markets visible to {@code party}. Observed by every participant. */
    public List<PerpMarketView> perpMarketsVisibleTo(String party) {
        return withRetry("perp markets for " + party, () -> {
            DamlLedgerClient client = connection.get();
            ContractFilter<PerpMarket.Contract> filter = ContractFilter.of(PerpMarket.COMPANION);
            List<PerpMarketView> out = new ArrayList<>();
            client.getStateClient()
                    .getActiveContracts(filter, Set.of(party), false,
                            client.getStateClient().getLedgerEnd().blockingGet())
                    .timeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .blockingForEach(active -> {
                        for (PerpMarket.Contract c : active.activeContracts) {
                            PerpMarket m = c.data;
                            out.add(new PerpMarketView(c.id.contractId, m.operator, m.instrumentId,
                                    m.cashInstrument, m.indexPrice, m.fundingRate, m.fundingRateCap,
                                    m.maxLeverage, m.maintenanceMarginBps,
                                    m.openLong, m.openShort, m.insurance.isPresent(), m.isOpen));
                        }
                    });
            return out;
        });
    }

    /**
     * Positions visible to {@code party} — and a position is PRIVATE.
     *
     * <p>A {@code PerpPosition} is signed by operator + trader and observed only by
     * the auditor, so a trader sees its own book and nobody else's. On a leveraged
     * product that matters more than it does on a resting order: a visible
     * liquidation price is an invitation to push the market into it.
     */
    public List<PerpPositionView> perpPositionsVisibleTo(String party) {
        return withRetry("perp positions for " + party, () -> {
            DamlLedgerClient client = connection.get();
            ContractFilter<PerpPosition.Contract> filter = ContractFilter.of(PerpPosition.COMPANION);
            List<PerpPositionView> out = new ArrayList<>();
            client.getStateClient()
                    .getActiveContracts(filter, Set.of(party), false,
                            client.getStateClient().getLedgerEnd().blockingGet())
                    .timeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .blockingForEach(active -> {
                        for (PerpPosition.Contract c : active.activeContracts) {
                            PerpPosition p = c.data;
                            out.add(new PerpPositionView(c.id.contractId, labelOf(p.trader),
                                    p.instrumentId, p.cashInstrument,
                                    p.side.toString().equalsIgnoreCase("LONG") ? "Long" : "Short",
                                    p.size, p.entryPrice, p.collateral,
                                    p.maintenanceMarginBps, p.openedAt, p.lastFundingAt));
                        }
                    });
            return out;
        });
    }

    /** Flat view of a perpetual market. {@code insured} = a pool is funded. */
    public record PerpMarketView(
            String contractId, String operator, String instrumentId, String cashInstrument,
            java.math.BigDecimal indexPrice, java.math.BigDecimal fundingRate,
            java.math.BigDecimal fundingRateCap, java.math.BigDecimal maxLeverage,
            java.math.BigDecimal maintenanceMarginBps,
            java.math.BigDecimal openLong, java.math.BigDecimal openShort,
            boolean insured, boolean isOpen) {
    }

    /** Flat view of one leveraged position. */
    public record PerpPositionView(
            String contractId, String traderLabel, String instrumentId, String cashInstrument,
            String side, java.math.BigDecimal size, java.math.BigDecimal entryPrice,
            java.math.BigDecimal collateral, java.math.BigDecimal maintenanceMarginBps,
            java.time.Instant openedAt, java.time.Instant lastFundingAt) {
    }

    /** Flat view of a continuous session. */
    public record BookView(
            String contractId, String operator, String instrumentId, String cashInstrument,
            java.math.BigDecimal referencePrice, java.math.BigDecimal bandFraction,
            Long nextSeq, Long liveCount, boolean isOpen, List<String> participants) {
    }

    /** Flat view of one resting order. {@code limitPrice} null = an unpriced market order. */
    public record RestingOrderView(
            String contractId, String operator, String trader, String traderLabel,
            String instrumentId, String cashInstrument, String side,
            java.math.BigDecimal quantity, java.math.BigDecimal limitPrice,
            String timeInForce, Long seqNo) {
    }

    /** Flat view of one public tape print — price, size, time, and no identities. */
    public record TapeView(
            String contractId, String instrumentId, String cashInstrument,
            java.math.BigDecimal price, java.math.BigDecimal quantity,
            java.time.Instant printedAt, Long matchSeq) {
    }

    /** Flat view of one bilateral confirm. {@code liquidity} is Maker or Taker. */
    public record ConfirmView(
            String contractId, String traderLabel, String instrumentId, String cashInstrument,
            String side, java.math.BigDecimal quantity, java.math.BigDecimal price,
            java.math.BigDecimal cashAmount, String liquidity, java.time.Instant tradedAt) {
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

    /**
     * One leg of a basket's creation unit. {@code expectedIssuer} is the party whose
     * version of the instrument the fund accepts — empty means any issuer. Carried here
     * because the instrument NAME is not the instrument: a caller needs to see WHOSE
     * asset a fund holds, not just what it is called.
     */
    public record ComponentView(String instrumentId, java.math.BigDecimal unitsPerShare,
                                java.util.Optional<String> expectedIssuer) {

        /** A leg with no issuer pin. */
        public ComponentView(String instrumentId, java.math.BigDecimal unitsPerShare) {
            this(instrumentId, unitsPerShare, java.util.Optional.empty());
        }
    }

    /** Flat, JSON-friendly view of a BasketDefinition (an ETF/fund). */
    /**
     * Flat view of a BasketDefinition. The three fee fields are carried because the
     * create and redeem endpoints have to KNOW the fee before they can fund it: the AP
     * pays it from a cash holding supplied at request time, and a caller should not have
     * to read the basket itself to discover what to bring.
     */
    public record BasketView(
            String contractId, String administrator, String basketId, String description,
            String cashInstrument, List<ComponentView> components, List<String> participants,
            java.util.Optional<String> feeReceiver,
            java.util.Optional<java.math.BigDecimal> creationFee,
            java.util.Optional<java.math.BigDecimal> redemptionFee) {

        /** A fee-free basket. */
        public BasketView(String contractId, String administrator, String basketId,
                String description, String cashInstrument, List<ComponentView> components,
                List<String> participants) {
            this(contractId, administrator, basketId, description, cashInstrument, components,
                    participants, java.util.Optional.empty(), java.util.Optional.empty(),
                    java.util.Optional.empty());
        }
    }

    /**
     * Flat, JSON-friendly view of an official {@code NavFixing}.
     *
     * <p>{@code price} IS THE BASE, NOT THE ANSWER — it is what one share was worth at
     * {@code accrualFrom}. What it is worth NOW is {@code Accrual.navAt(...)} evaluated
     * against the clock, and every consumer should be asking that question instead of
     * reading this field. The name is the Daml field's name and is kept honest rather
     * than helpfully renamed.
     *
     * <p>{@code finalizedAt} is a DIFFERENT FACT from {@code accrualFrom}: the first is
     * when the ledger saw the fix, the second is the instant the mark applies from. A
     * mark struck as of 16:00 and finalised at 16:07 records both, so the committee's
     * own latency stays auditable rather than absorbed.
     */
    public record NavFixingView(
            String contractId, List<String> attestors, long threshold,
            String instrumentId, String cashInstrument, String session,
            java.math.BigDecimal price, String rationale,
            java.math.BigDecimal ratePerAnnum, String dayCount, java.time.Instant accrualFrom,
            List<String> publishedTo, java.time.Instant finalizedAt,
            // ---- docs/FIXING_METHODOLOGY.md §10: what a published fixing must carry ----
            // The tier used, the wrapper mark, and the restatement lineage. All optional
            // because a fixing struck before those fields existed reads exactly as it did.
            String tier,                                  // "auction" | "committee" | null
            java.math.BigDecimal referencePrice,          // the benchmark print
            java.math.BigDecimal wrapperFactor,           // the attested par ratio
            String supersedes,                            // contract id this corrects
            String restatementReason) {

        /** True when this fix's value MOVES — a zero rate is the old snapshot exactly. */
        public boolean accruing() {
            return ratePerAnnum.signum() != 0;
        }

        /** True when this fixing is a correction of an earlier one (§6). */
        public boolean isRestatement() {
            return supersedes != null && !supersedes.isBlank();
        }
    }

    /** A published cessation notice as the acting party sees it (§8). */
    public record CessationView(
            String contractId, String instrumentId, String session,
            java.time.Instant publishedAt, java.time.Instant finalStrike,
            String successor, String reason, List<String> notifyTo) {
    }

    /** A receipt as seen by the acting party, with WHO can see it (the privacy proof). */
    public record ReceiptView(
            String contractId, String kind, String headline, String settledAt,
            List<String> visibleTo) {
    }

    /**
     * Flat, JSON-friendly view of a ClosingAuction.
     *
     * <p>{@code referencePrice} is the venue's published ANCHOR, not the price the
     * cross prints at — RunClose discovers that from the sealed book.
     *
     * <p>{@code submittedCount}/{@code cancelledCount} are the ledger's own book
     * counters (COMPLETE-ORDER COMMITMENT): their difference is exactly how many
     * orders are live, and RunClose refuses to run unless the supplied book is that
     * many. {@link #liveOrderCount()} is the number to compare a candidate book against.
     */
    public record AuctionView(
            String contractId, String operator, String instrumentId, String cashInstrument,
            String session, java.math.BigDecimal referencePrice, List<String> participants,
            String liquidityProvider,   // full party id of the designated DLP, or null
            long submittedCount, long cancelledCount,
            boolean isOpen) {

        /** Orders RunClose expects to be handed: everything lodged, less everything cancelled. */
        public long liveOrderCount() {
            return submittedCount - cancelledCount;
        }
    }

    /**
     * The venue's OPEN OFFER of the liquidity seat, flattened for the web layer.
     *
     * <p>Party fields are FULL party ids (never labels) because callers match on them —
     * {@code eligible} decides who may accept, {@code accepted} who already holds a
     * mandate, {@code barred} who forfeited the seat this session by failing a
     * commitment. The web layer relabels for display; matching must not go through a
     * label, which is only the readable prefix of a namespaced id.
     */
    public record MandateTermsView(
            String contractId, String operator, String instrumentId, String cashInstrument,
            String session, java.math.BigDecimal anchorPrice,
            java.math.BigDecimal commitmentSize, long maxBandBps, java.time.Instant expiresAt,
            List<String> eligible, List<String> accepted, List<String> barred) {

        /** May {@code party} take up this seat right now? (eligible, not already in, not barred) */
        public boolean openTo(String party) {
            return eligible.contains(party) && !accepted.contains(party) && !barred.contains(party);
        }
    }

    /**
     * A signed liquidity OBLIGATION, flattened for the web layer.
     *
     * <p>{@code peakShownQty} / {@code shownSide} are the running disclosure record the
     * ledger stamps on every publish: the duty is sized by what this provider was
     * ACTUALLY SHOWN during the session, not by a figure assembled afterwards.
     * {@code lastShownAt} is null when the provider has been shown nothing yet.
     */
    public record MandateView(
            String contractId, String operator, String provider, String instrumentId,
            String cashInstrument, String session, java.math.BigDecimal anchorPrice,
            java.math.BigDecimal commitmentSize, long maxBandBps, java.time.Instant expiresAt,
            java.time.Instant acceptedAt, String shownSide, java.math.BigDecimal peakShownQty,
            java.time.Instant lastShownAt, long disclosuresSeen) {

        /** Is this obligation still running at {@code now}? (the ledger re-checks on its own clock) */
        public boolean liveAt(java.time.Instant now) {
            return expiresAt.isAfter(now);
        }

        /** Does this mandate cover exactly this book, at this published anchor? */
        public boolean covers(String venue, String instrument, String cash, String sess,
                java.math.BigDecimal anchor) {
            return operator.equals(venue) && instrumentId.equals(instrument)
                    && cashInstrument.equals(cash) && session.equals(sess)
                    && (anchor == null || anchorPrice.compareTo(anchor) == 0);
        }
    }

    /** Flat, JSON-friendly view of an ImbalanceDisclosure (the aggregate only). */
    public record ImbalanceView(
            String contractId, String operator, String liquidityProvider, String instrumentId,
            String cashInstrument, String session, String netSide,
            java.math.BigDecimal netQuantity, java.math.BigDecimal referencePrice) {
    }

    /**
     * Flat, JSON-friendly view of a resting SealedOrder (as seen by the operator).
     *
     * <p>{@code limitPrice} is NULL for an unpriced MARKET-on-close order (the ledger's
     * {@code None}) and non-null for a LIMIT-on-close. Callers must branch on it rather
     * than substitute a number: there is no price at which an MOC is "off-price".
     */
    public record OrderView(
            String contractId, String operator, String trader, String instrumentId,
            String cashInstrument, String session, String side, java.math.BigDecimal quantity,
            java.math.BigDecimal limitPrice) {

        /** {@code true} when this is an unpriced market-on-close order. */
        public boolean isMarketOrder() {
            return limitPrice == null;
        }

        /** The order type as a label for display: {@code "MOC"} or {@code "LOC"}. */
        public String orderType() {
            return isMarketOrder() ? "MOC" : "LOC";
        }
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

    /**
     * Wraps ledger/command failures as a clean runtime error for the web layer.
     *
     * <p>Carries the gRPC classification and the command id when there was one, so
     * {@code ApiExceptionHandler} can answer with the RIGHT status (403 for a refused
     * authorization, 409 for a stale contract id, 503 for a participant that is down,
     * 422 for a Daml rejection) and hand the caller the handle a node operator needs.
     * Both are optional: the desk's own pre-flight rejections ("Alice holds no USDC")
     * are constructed with the message alone and keep the historical 422.
     */
    public static class LedgerException extends RuntimeException {

        /** The gRPC/Canton classification, or null when this never reached the ledger. */
        private final transient LedgerErrors.Failure failure;

        /** The submission's command id — THE string to quote to the node operator. */
        private final String commandId;

        public LedgerException(String message) {
            this(message, null, null, null);
        }

        public LedgerException(String message, Throwable cause) {
            this(message, cause, null, null);
        }

        public LedgerException(String message, Throwable cause,
                LedgerErrors.Failure failure, String commandId) {
            super(message, cause);
            this.failure = failure;
            this.commandId = commandId;
        }

        public LedgerErrors.Failure failure() {
            return failure;
        }

        public String commandId() {
            return commandId;
        }
    }

    // Kept for symmetry / potential callers needing raw durations.
    @SuppressWarnings("unused")
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
}
