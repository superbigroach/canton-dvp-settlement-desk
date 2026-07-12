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

    // ---- Parties ----------------------------------------------------------

    public record PartyResponse(
            String party, String displayName, String label, boolean isLocal) {
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
            String session,                  // Open | Close (defaults to Close when blank)
            @NotNull @Positive BigDecimal referencePrice,
            @NotNull List<@NotBlank String> participants,
            String liquidityProvider) {      // optional: the designated DLP (null/blank = none)
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

    // ---- One-click Buy/Sell (server-orchestrated DvP) ---------------------

    /**
     * A whole bilateral trade in one call: the desk resolves each side's holding
     * (splitting/merging to the exact leg amount) then runs propose → accept →
     * settle server-side. {@code auditor} defaults to "Auditor" when blank.
     */
    public record TradeRequest(
            @NotBlank String buyer,          // delivers cash, receives the asset
            @NotBlank String seller,         // delivers the asset, receives cash
            String auditor,                  // defaults to "Auditor"
            @NotBlank String assetInstrument,
            @NotNull @Positive BigDecimal assetAmount,
            @NotBlank String cashInstrument,
            @NotNull @Positive BigDecimal cashAmount) {
    }

    public record TradeResponse(
            String receiptCid,
            String buyer,
            String seller,
            String assetInstrument,
            BigDecimal assetAmount,
            String cashInstrument,
            BigDecimal cashAmount,
            BigDecimal unitPrice) {
    }

    // ---- Simple Market-on-Close (auto-resolve auction + committed holding) --

    /**
     * Send one sealed order to the close — DEAD SIMPLE: just the asset, the side,
     * and the amount. No price and no counterparty: a MOC order takes the official
     * close price (the instrument's published reference price, resolved server-side)
     * and the desk auto-commits the backing holding (cash for a Buy, the asset for a
     * Sell). The acting party is {@code trader} (the logged-in party from the switcher).
     */
    public record MocOrderRequest(
            @NotBlank String trader,
            @NotBlank String side,           // Buy | Sell
            @NotNull @Positive BigDecimal quantity,
            @NotBlank String instrumentId,
            String cashInstrument,            // defaults to "USDC" when blank
            String session) {                 // Open | Close (defaults to Close when blank)
    }

    public record InstrumentResponse(
            String id, String kind, String description, BigDecimal referencePrice) {
    }

    public record MocOrderResponse(
            String orderCid,
            String auctionCid,
            boolean openedAuction,
            BigDecimal closingPrice) {
    }

    public record MocOrderView(
            String contractId, String trader, String side,
            BigDecimal quantity, BigDecimal limitPrice) {
    }

    public record MocStateResponse(
            String auctionCid, String instrumentId, String cashInstrument, String session,
            BigDecimal referencePrice, boolean isOpen, List<MocOrderView> orders,
            // The dark-pool hint: how many OTHER traders' orders rest HIDDEN from the
            // acting party (0 for the venue, which sees the full book). Never reveals
            // their side/size/price — only that sealed interest exists.
            int othersResting) {
    }

    /** Trader withdraws their own resting order (actAs the trader). */
    public record WithdrawOrderRequest(
            @NotBlank String trader) {
    }

    /** Venue clears the resting book for an instrument/session (actAs the venue). */
    public record ClearBookRequest(
            @NotBlank String instrumentId,
            String cashInstrument,            // defaults to "USDC" when blank
            String session) {                 // Open | Close (defaults to Close when blank)
    }

    public record MocFillView(
            String trader, String side, BigDecimal quantity, BigDecimal price) {
    }

    public record ClearBookResponse(int cleared) {
    }

    public record MocCloseResponse(
            String settlementBatchCid, String session, BigDecimal closingPrice,
            List<MocFillView> fills) {
    }

    /**
     * The NET imbalance of a sealed book — the Designated Liquidity Provider view.
     *
     * <p>Returned by {@code GET /moc/imbalance} ONLY to the acting party the ledger
     * lets see the {@link com.lucilla.settlement.model.marketonclose.ImbalanceDisclosure}
     * (the DLP or the venue). {@code disclosed=false} means the acting party is not
     * entitled to it (a normal trader), or no DLP auction exists. It reveals the
     * AGGREGATE only — never any individual order or trader identity.
     */
    public record MocImbalanceResponse(
            boolean disclosed,               // did the acting party get the aggregate?
            String instrumentId,
            String cashInstrument,
            String session,                  // "Open" | "Close"
            String netSide,                  // "Buy" | "Sell" | "Flat" (heavy side)
            BigDecimal netQuantity,          // magnitude of the imbalance (>= 0)
            BigDecimal referencePrice,       // the uniform price the cross will print at
            String liquidityProvider,        // the DLP's friendly label (who may offset)
            String note) {                   // human-readable context for the UI
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
