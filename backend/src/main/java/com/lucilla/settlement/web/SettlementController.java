package com.lucilla.settlement.web;

import com.daml.ledger.javaapi.data.TransactionTree;
import com.lucilla.settlement.ledger.LedgerCommands;
import com.lucilla.settlement.ledger.LedgerService;
import jakarta.validation.Valid;
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
import java.util.List;
import java.util.Optional;

/**
 * REST surface of the settlement desk. Each endpoint maps a request to a Daml
 * Ledger API command (built by {@link LedgerCommands}) submitted under the right
 * {@code actAs} party by {@link LedgerService}, and returns the resulting
 * contract id(s).
 *
 * <p>Endpoints:
 * <pre>
 *   POST /api/instruments                 issue an Instrument (actAs issuer)
 *   POST /api/holdings                    issue a Holding      (actAs issuer)
 *   GET  /api/holdings?party=             holdings visible to a party
 *   POST /api/dvp/propose                 create a DvPProposal (actAs proposer)
 *   POST /api/dvp/{cid}/accept            accept -> DvPAgreement (actAs counterparty)
 *   POST /api/dvp/{cid}/settle            settle both legs atomically (actAs proposer)
 *   POST /api/auction                     open a ClosingAuction (actAs operator)
 *   POST /api/auction/{cid}/order         submit a sealed order (actAs trader)
 *   POST /api/auction/{cid}/close         seal + run the uniform-price cross (actAs operator)
 * </pre>
 */
@RestController
@RequestMapping("/api")
public class SettlementController {

    private final LedgerService ledger;

    public SettlementController(LedgerService ledger) {
        this.ledger = ledger;
    }

    // ---- Parties ----------------------------------------------------------

    /**
     * The parties this ledger knows about, for the UI's party picker. Party ids
     * carry a per-run Canton namespace suffix, so they are resolved live — never
     * hardcoded.
     */
    @GetMapping("/parties")
    public List<Dtos.PartyResponse> parties() {
        return ledger.listParties().stream()
                .map(p -> new Dtos.PartyResponse(p.party(), p.displayName(), p.label(), p.isLocal()))
                .toList();
    }

    // ---- Instruments ------------------------------------------------------

    @PostMapping("/instruments")
    public ResponseEntity<Dtos.CidResponse> issueInstrument(
            @Valid @RequestBody Dtos.IssueInstrumentRequest req) {
        String depository = blankTo(req.depository(), req.issuer());
        String version = blankTo(req.version(), "1");
        String description = req.description() == null ? "" : req.description();
        var cmd = LedgerCommands.createInstrument(
                req.issuer(), depository, req.id(), version, req.kind(), description,
                Optional.ofNullable(req.referencePrice()));
        String cid = ledger.submitForCreated(req.issuer(), cmd, LedgerCommands.instrumentTemplateId());
        return created(new Dtos.CidResponse(cid));
    }

    // ---- Holdings ---------------------------------------------------------

    @PostMapping("/holdings")
    public ResponseEntity<Dtos.CidResponse> issueHolding(
            @Valid @RequestBody Dtos.IssueHoldingRequest req) {
        var cmd = LedgerCommands.createHolding(
                req.issuer(), req.instrumentId(), req.owner(), req.amount());
        String cid = ledger.submitForCreated(req.issuer(), cmd, LedgerCommands.holdingTemplateId());
        return created(new Dtos.CidResponse(cid));
    }

    @GetMapping("/holdings")
    public List<Dtos.HoldingResponse> holdings(@RequestParam String party) {
        // Accept either a full party id (from the picker) or a friendly label.
        String resolved = ledger.resolveParty(party);
        return ledger.holdingsVisibleTo(resolved).stream()
                .map(h -> new Dtos.HoldingResponse(
                        h.contractId(), h.issuer(), h.instrumentId(), h.owner(),
                        h.amount(), h.disclosedTo()))
                .toList();
    }

    // ---- Bilateral DvP ----------------------------------------------------

    @PostMapping("/dvp/propose")
    public ResponseEntity<Dtos.CidResponse> propose(@Valid @RequestBody Dtos.ProposeDvpRequest req) {
        var cmd = LedgerCommands.createDvPProposal(
                req.proposer(), req.counterparty(), req.auditor(),
                req.assetHoldingCid(), req.cashHoldingCid(),
                req.assetInstrument(), req.assetAmount(),
                req.cashInstrument(), req.cashAmount());
        String cid = ledger.submitForCreated(req.proposer(), cmd, LedgerCommands.dvpProposalTemplateId());
        return created(new Dtos.CidResponse(cid));
    }

    @PostMapping("/dvp/{cid}/accept")
    public ResponseEntity<Dtos.CidResponse> accept(
            @PathVariable String cid, @Valid @RequestBody Dtos.AcceptDvpRequest req) {
        var cmd = LedgerCommands.acceptProposal(cid);
        String agreementCid = ledger.submitForCreated(
                req.counterparty(), cmd, LedgerCommands.dvpAgreementTemplateId());
        return created(new Dtos.CidResponse(agreementCid));
    }

    @PostMapping("/dvp/{cid}/settle")
    public Dtos.SettleResponse settle(
            @PathVariable String cid, @Valid @RequestBody Dtos.SettleDvpRequest req) {
        var cmd = LedgerCommands.settleAgreement(cid);
        TransactionTree tree = ledger.submit(req.proposer(), cmd);
        List<String> receipts = ledger.createdOf(tree, LedgerCommands.settlementReceiptTemplateId());
        List<String> holdings = ledger.createdOf(tree, LedgerCommands.holdingTemplateId());
        return new Dtos.SettleResponse(receipts.isEmpty() ? null : receipts.get(0), holdings);
    }

    // ---- Market-on-Close --------------------------------------------------

    @PostMapping("/auction")
    public ResponseEntity<Dtos.CidResponse> openAuction(
            @Valid @RequestBody Dtos.OpenAuctionRequest req) {
        var cmd = LedgerCommands.createAuction(
                req.operator(), req.auditor(), req.instrumentId(), req.cashInstrument(),
                req.referencePrice(), req.participants());
        String cid = ledger.submitForCreated(req.operator(), cmd, LedgerCommands.closingAuctionTemplateId());
        return created(new Dtos.CidResponse(cid));
    }

    @PostMapping("/auction/{cid}/order")
    public ResponseEntity<Dtos.CidResponse> submitOrder(
            @PathVariable String cid, @Valid @RequestBody Dtos.SubmitOrderRequest req) {
        var side = LedgerCommands.side(req.side());
        var cmd = LedgerCommands.submitOrder(
                cid, req.trader(), side, req.quantity(), req.limitPrice(), req.holdingCid());
        String orderCid = ledger.submitForCreated(req.trader(), cmd, LedgerCommands.sealedOrderTemplateId());
        return created(new Dtos.CidResponse(orderCid));
    }

    /**
     * Seal the window (CloseBidding) then run the uniform-price cross (RunClose)
     * over the supplied sealed orders. Two exercises submitted by the operator;
     * returns the sealed auction handle and the resulting SettlementBatch.
     */
    @PostMapping("/auction/{cid}/close")
    public Dtos.CloseResponse close(
            @PathVariable String cid, @Valid @RequestBody Dtos.CloseAuctionRequest req) {
        // 1) Seal the order window; the sealed auction is a fresh contract id.
        String sealedCid = ledger.submitForCreated(
                req.operator(), LedgerCommands.closeBidding(cid),
                LedgerCommands.closingAuctionTemplateId());
        // 2) Cross the sealed book at the published close -> SettlementBatch.
        String batchCid = ledger.submitForCreated(
                req.operator(),
                LedgerCommands.runClose(sealedCid, req.buyOrderCids(), req.sellOrderCids()),
                LedgerCommands.settlementBatchTemplateId());
        return new Dtos.CloseResponse(batchCid, sealedCid);
    }

    // ---- helpers ----------------------------------------------------------

    private static <T> ResponseEntity<T> created(T body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    private static String blankTo(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    @SuppressWarnings("unused")
    private static BigDecimal req(BigDecimal v) {
        return v;
    }
}
