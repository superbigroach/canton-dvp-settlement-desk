package com.lucilla.settlement.web;

import com.lucilla.settlement.ledger.LedgerCommands;
import com.lucilla.settlement.ledger.LedgerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.closeTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer slice test for the perpetuals desk: real controller + JSON
 * (de)serialization + bean validation, with {@link LedgerService} MOCKED.
 *
 * <p>The RISK rules — the leverage ceiling, the maintenance floor, the conservation of
 * cash on every settlement path — live in Perpetual.daml and are not this layer's to
 * decide. What IS this layer's is pinned here: routing, validation, the idempotent
 * market open, the read-only marks a screen shows, and the fact that a position is
 * private to its trader.
 */
@WebMvcTest(PerpetualController.class)
class PerpetualControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    LedgerService ledger;

    private static final String VENUE = "Venue::ns";
    private static final String ALICE = "Alice::ns";
    private static final String BOB = "Bob::ns";

    /**
     * Decimals reach the wire as JSON numbers whose SCALE follows the arithmetic
     * (1000/200 is written {@code 5.00}), so the marks are compared by value rather
     * than by the literal digits Jackson happened to emit.
     */
    private static ResultMatcher num(String path, double expected) {
        return jsonPath(path).value(closeTo(expected, 1e-6), Double.class);
    }

    private static LedgerService.PerpMarketView market(String cid, BigDecimal index) {
        return market(cid, index, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    private static LedgerService.PerpMarketView market(
            String cid, BigDecimal index, BigDecimal openLong, BigDecimal openShort) {
        return new LedgerService.PerpMarketView(cid, VENUE, "cETH", "USDC",
                index, new BigDecimal("0.0010"), new BigDecimal("0.0075"),
                new BigDecimal("10"), new BigDecimal("500"),
                openLong, openShort, true, true);
    }

    private static LedgerService.PerpPositionView position(
            String cid, String traderLabel, String side, String size, String entry, String collateral) {
        return new LedgerService.PerpPositionView(cid, traderLabel, "cETH", "USDC", side,
                new BigDecimal(size), new BigDecimal(entry), new BigDecimal(collateral),
                new BigDecimal("500"),
                Instant.parse("2026-08-05T14:00:00Z"), Instant.parse("2026-08-05T14:00:00Z"));
    }

    // ---- Market ------------------------------------------------------------

    @Test
    void openMarket_returns201WithTheMarketJustCreated() throws Exception {
        // A market opened where none was open is a CREATE, and the response is the
        // ledger's own view of it, not an echo of the request.
        when(ledger.resolveParty("Venue")).thenReturn(VENUE);
        when(ledger.resolveParty("Auditor")).thenReturn("Auditor::ns");
        when(ledger.listParties()).thenReturn(List.of(
                new LedgerService.PartyView(VENUE, "Venue", "Venue", true),
                new LedgerService.PartyView(ALICE, "Alice", "Alice", true)));
        when(ledger.perpMarketsVisibleTo(VENUE))
                .thenReturn(List.of())
                .thenReturn(List.of(market("market#1", new BigDecimal("2400"))));
        when(ledger.submitForCreated(eq(VENUE), any(), any())).thenReturn("market#1");

        mvc.perform(post("/api/perp/market")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"instrumentId":"cETH","cashInstrument":"USDC","indexPrice":2400.0}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contractId").value("market#1"))
                .andExpect(jsonPath("$.instrumentId").value("cETH"))
                .andExpect(jsonPath("$.isOpen").value(true))
                .andExpect(num("$.indexPrice", 2400))
                .andExpect(num("$.maxLeverage", 10));
    }

    @Test
    void openMarket_returnsTheMarketAlreadyOpenWith200AndSubmitsNothing() throws Exception {
        // Opening is IDEMPOTENT: a second call on the same instrument/cash pair hands
        // back the live market rather than creating a rival one that would split open
        // interest across two indexes.
        when(ledger.resolveParty("Venue")).thenReturn(VENUE);
        when(ledger.resolveParty("Auditor")).thenReturn("Auditor::ns");
        when(ledger.perpMarketsVisibleTo(VENUE))
                .thenReturn(List.of(market("market#1", new BigDecimal("2400"))));

        mvc.perform(post("/api/perp/market")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"instrumentId":"cETH","cashInstrument":"USDC","indexPrice":9999.0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contractId").value("market#1"))
                // The live index stands; the price sent with the duplicate open is ignored.
                .andExpect(num("$.indexPrice", 2400));

        verify(ledger, never()).submitForCreated(any(), any(), any());
    }

    @Test
    void markets_reportSkewAsOpenLongMinusOpenShort() throws Exception {
        // `skew` is not on the ledger — it is derived here, and it is the directional
        // exposure the venue's insurance pool is carrying.
        when(ledger.resolveParty("Venue")).thenReturn(VENUE);
        when(ledger.perpMarketsVisibleTo(VENUE)).thenReturn(List.of(
                market("market#1", new BigDecimal("2400"),
                        new BigDecimal("30"), new BigDecimal("12"))));

        mvc.perform(get("/api/perp/markets"))
                .andExpect(status().isOk())
                .andExpect(num("$[0].openLong", 30))
                .andExpect(num("$[0].openShort", 12))
                .andExpect(num("$[0].skew", 18));
    }

    // ---- Opening a position ------------------------------------------------

    @Test
    void openPosition_returns201WithNotionalLeverageAndLiquidationPrice() throws Exception {
        // 10 at an index of 100 on 200 of collateral is 5x, and the trader is told the
        // index at which it dies: (size·entry − collateral) / (size · (1 − mmr)).
        var open = market("market#1", new BigDecimal("100"));
        when(ledger.resolveParty("Alice")).thenReturn(ALICE);
        when(ledger.resolveParty("Venue")).thenReturn(VENUE);
        when(ledger.perpMarketsVisibleTo(VENUE)).thenReturn(List.of(open));
        when(ledger.provisionAtLeastHolding(eq(ALICE), eq("USDC"), any())).thenReturn("holding#1");
        when(ledger.createdOf(any(), eq(LedgerCommands.perpPositionTemplateId())))
                .thenReturn(List.of("pos#1"));
        when(ledger.createdOf(any(), eq(LedgerCommands.perpMarketTemplateId())))
                .thenReturn(List.of("market#1"));
        when(ledger.perpPositionsVisibleTo(ALICE)).thenReturn(List.of(
                position("pos#1", "Alice", "Long", "10", "100", "200")));

        mvc.perform(post("/api/perp/position")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"trader":"Alice","side":"Long","size":10,
                                 "instrumentId":"cETH","cashInstrument":"USDC","collateral":200}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contractId").value("pos#1"))
                .andExpect(jsonPath("$.trader").value("Alice"))
                .andExpect(jsonPath("$.side").value("Long"))
                .andExpect(num("$.entryPrice", 100))
                .andExpect(num("$.notional", 1000))
                .andExpect(num("$.leverage", 5))
                .andExpect(num("$.liquidationPrice", 84.210526))
                .andExpect(jsonPath("$.liquidatable").value(false));
    }

    @Test
    void openPosition_rejectsNonPositiveSizeWith400() throws Exception {
        // Size and collateral are @Positive, so a zero or negative order is a FORM
        // rejection here and never becomes a submission the ledger has to refuse.
        mvc.perform(post("/api/perp/position")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"trader":"Alice","side":"Long","size":0,
                                 "instrumentId":"cETH","collateral":200}
                                """))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/perp/position")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"trader":"Alice","side":"Long","size":-10,
                                 "instrumentId":"cETH","collateral":200}
                                """))
                .andExpect(status().isBadRequest());

        verify(ledger, never()).submit(any(), any());
    }

    @Test
    void openPosition_rejectsNonPositiveCollateralWith400() throws Exception {
        // Unlimited leverage would be collateral of zero. It is refused at the form.
        mvc.perform(post("/api/perp/position")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"trader":"Alice","side":"Long","size":10,
                                 "instrumentId":"cETH","collateral":0}
                                """))
                .andExpect(status().isBadRequest());

        verify(ledger, never()).submit(any(), any());
    }

    @Test
    void openPosition_rejectsABlankTraderOrInstrumentWith400() throws Exception {
        // A blank party or instrument would resolve to nothing at all; @NotBlank stops
        // it before the desk goes looking for a market that cannot exist.
        mvc.perform(post("/api/perp/position")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"trader":"","side":"Long","size":10,
                                 "instrumentId":"cETH","collateral":200}
                                """))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/perp/position")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"trader":"Alice","side":"Long","size":10,
                                 "instrumentId":"  ","collateral":200}
                                """))
                .andExpect(status().isBadRequest());

        verify(ledger, never()).submit(any(), any());
    }

    // ---- Reading positions -------------------------------------------------

    @Test
    void positions_showOnlyTheActingPartysOwnBook() throws Exception {
        // THE PRIVACY PROPERTY at the web layer: the endpoint returns exactly what the
        // acting party can see, so Bob gets [] on a ledger where Alice is open.
        when(ledger.resolveParty("Venue")).thenReturn(VENUE);
        when(ledger.resolveParty("Alice")).thenReturn(ALICE);
        when(ledger.resolveParty("Bob")).thenReturn(BOB);
        when(ledger.perpMarketsVisibleTo(VENUE))
                .thenReturn(List.of(market("market#1", new BigDecimal("100"))));
        when(ledger.perpPositionsVisibleTo(ALICE)).thenReturn(List.of(
                position("pos#1", "Alice", "Long", "10", "100", "200")));
        when(ledger.perpPositionsVisibleTo(BOB)).thenReturn(List.of());

        mvc.perform(get("/api/perp/positions").param("as", "Alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].trader").value("Alice"));

        mvc.perform(get("/api/perp/positions").param("as", "Bob"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void positions_areMarkedToTheIndexAndNotToTheEntryPrice() throws Exception {
        // A Long of 10 struck at 100 with the index at 110 on 200 of collateral:
        // pnl = 10·(110−100) = 100, equity = 300, notional = 1100,
        // maintenance = 10·110·5% = 55 — so it is nowhere near the floor.
        when(ledger.resolveParty("Venue")).thenReturn(VENUE);
        when(ledger.resolveParty("Alice")).thenReturn(ALICE);
        when(ledger.perpMarketsVisibleTo(VENUE))
                .thenReturn(List.of(market("market#1", new BigDecimal("110"))));
        when(ledger.perpPositionsVisibleTo(ALICE)).thenReturn(List.of(
                position("pos#1", "Alice", "Long", "10", "100", "200")));

        mvc.perform(get("/api/perp/positions").param("as", "Alice"))
                .andExpect(status().isOk())
                .andExpect(num("$[0].markPrice", 110))
                .andExpect(num("$[0].unrealisedPnl", 100))
                .andExpect(num("$[0].equity", 300))
                .andExpect(num("$[0].notional", 1100))
                .andExpect(num("$[0].maintenance", 55))
                .andExpect(jsonPath("$[0].liquidatable").value(false));
    }

    @Test
    void positions_areFlaggedLiquidatableOnceEquityIsUnderMaintenance() throws Exception {
        // Same Long on 30 of collateral with the index at 95: pnl = −50, equity = −20,
        // maintenance = 10·95·5% = 47.5. Equity is below the floor, so the screen says
        // so — the ledger still decides whether the venue may act on it.
        when(ledger.resolveParty("Venue")).thenReturn(VENUE);
        when(ledger.resolveParty("Alice")).thenReturn(ALICE);
        when(ledger.perpMarketsVisibleTo(VENUE))
                .thenReturn(List.of(market("market#1", new BigDecimal("95"))));
        when(ledger.perpPositionsVisibleTo(ALICE)).thenReturn(List.of(
                position("pos#1", "Alice", "Long", "10", "100", "30")));

        mvc.perform(get("/api/perp/positions").param("as", "Alice"))
                .andExpect(status().isOk())
                .andExpect(num("$[0].unrealisedPnl", -50))
                .andExpect(num("$[0].equity", -20))
                .andExpect(num("$[0].maintenance", 47.5))
                .andExpect(jsonPath("$[0].liquidatable").value(true));
    }

    // ---- Failure -----------------------------------------------------------

    @Test
    void ledgerRejection_becomes422CarryingTheModelsOwnSentence() throws Exception {
        // A Daml refusal is the model working, not a server fault: 422 with the
        // ledger's message intact.
        when(ledger.resolveParty("Venue")).thenReturn(VENUE);
        when(ledger.resolveParty("Auditor")).thenReturn("Auditor::ns");
        when(ledger.listParties()).thenReturn(List.of(
                new LedgerService.PartyView(VENUE, "Venue", "Venue", true)));
        when(ledger.perpMarketsVisibleTo(VENUE)).thenReturn(List.of());
        when(ledger.submitForCreated(any(), any(), any()))
                .thenThrow(new LedgerService.LedgerException("maintenance margin exceeds the initial margin"));

        mvc.perform(post("/api/perp/market")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"instrumentId":"cETH","cashInstrument":"USDC","indexPrice":2400.0}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("maintenance margin exceeds the initial margin"));
    }
}
