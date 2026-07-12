package com.lucilla.settlement.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request/response payloads for {@link SettlementController}.
 *
 * <p>Parties and contract ids are plain strings: the caller supplies the actual
 * on-ledger party ids (as allocated on the sandbox / participant) and the
 * contract ids returned by earlier calls. That keeps the desk honest — there is
 * no hidden party-name magic; the REST surface mirrors the Ledger API's own
 * identifiers.
 */
public final class Dtos {

    private Dtos() {
    }

    // ---- Instruments & Holdings ------------------------------------------

    public record IssueInstrumentRequest(
            @NotBlank String issuer,
            String depository,               // defaults to issuer when blank
            @NotBlank String id,
            String version,                  // defaults to "1" when blank
            @NotBlank String kind,           // Equity | Cash | CryptoWrapped
            String description,
            BigDecimal referencePrice) {     // optional (null for cash)
    }

    public record IssueHoldingRequest(
            @NotBlank String issuer,
            @NotBlank String instrumentId,
            @NotBlank String owner,
            @NotNull @Positive BigDecimal amount) {
    }

    public record HoldingResponse(
            String contractId, String issuer, String instrumentId, String owner,
            BigDecimal amount, List<String> disclosedTo) {
    }

    // ---- Bilateral DvP ----------------------------------------------------

    public record ProposeDvpRequest(
            @NotBlank String proposer,       // the SELLER: delivers asset, receives cash
            @NotBlank String counterparty,   // the BUYER: delivers cash, receives asset
            @NotBlank String auditor,
            @NotBlank String assetHoldingCid,
            @NotBlank String cashHoldingCid,
            @NotBlank String assetInstrument,
            @NotNull @Positive BigDecimal assetAmount,
            @NotBlank String cashInstrument,
            @NotNull @Positive BigDecimal cashAmount) {
    }

    public record AcceptDvpRequest(
            @NotBlank String counterparty) { // acts as; must be the proposal's counterparty
    }

    public record SettleDvpRequest(
            @NotBlank String proposer) {     // acts as; must be the agreement's proposer
    }

    // ---- Market-on-Close --------------------------------------------------

    public record OpenAuctionRequest(
            @NotBlank String operator,
            @NotBlank String auditor,
            @NotBlank String instrumentId,
            @NotBlank String cashInstrument,
            @NotNull @Positive BigDecimal referencePrice,
            @NotNull List<@NotBlank String> participants) {
    }

    public record SubmitOrderRequest(
            @NotBlank String trader,
            @NotBlank String side,           // Buy | Sell
            @NotNull @Positive BigDecimal quantity,
            @NotNull @Positive BigDecimal limitPrice,
            @NotBlank String holdingCid) {   // pre-committed cash (Buy) or asset (Sell)
    }

    public record CloseAuctionRequest(
            @NotBlank String operator,
            @NotNull List<@NotBlank String> buyOrderCids,
            @NotNull List<@NotBlank String> sellOrderCids) {
    }

    // ---- Generic responses ------------------------------------------------

    public record CidResponse(String contractId) {
    }

    public record SettleResponse(
            String receiptCid,
            List<String> createdHoldingCids) {
    }

    public record CloseResponse(
            String settlementBatchCid,
            String sealedAuctionCid) {
    }
}
