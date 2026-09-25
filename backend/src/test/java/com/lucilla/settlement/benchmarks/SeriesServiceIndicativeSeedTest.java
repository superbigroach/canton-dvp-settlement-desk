package com.lucilla.settlement.benchmarks;

import com.lucilla.settlement.events.JsonlEventStore;
import com.lucilla.settlement.ledger.LedgerService;
import com.lucilla.settlement.ledger.MarketData;
import com.lucilla.settlement.scheduler.ScheduleStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * THE 25 SEPTEMBER 2026 BUG, pinned down.
 *
 * <p>On that day the public API served {@code "price": 65000.00} for CBTC — the figure
 * written onto the instrument when it was created — while bitcoin traded at 83,906. The
 * tier was honest ({@code 0, "seed", "not attested"}); the number was months stale, and it
 * was the first number any prospect would have seen.
 *
 * <p>The rule these tests hold in place: when there is no attested fixing, the bottom row
 * is a LIVE observation if the venues answer, and the stored mark only if they do not.
 * Neither is ever presented as attested.
 */
class SeriesServiceIndicativeSeedTest {

    static final String CBTC = "CBTC";

    private static LedgerService.InstrumentView instrument(String id, String kind, String referencePrice) {
        return new LedgerService.InstrumentView(id, kind, id + " on Canton", new BigDecimal(referencePrice));
    }

    private static MarketData.Composite composite(String median, String spreadBps, String... venues) {
        List<MarketData.VenueQuote> quotes = java.util.Arrays.stream(venues)
                .map(v -> new MarketData.VenueQuote(v, new BigDecimal(median), null))
                .map(q -> (MarketData.VenueQuote) q)
                .toList();
        return new MarketData.Composite(new BigDecimal(median), quotes, new BigDecimal(spreadBps));
    }

    private SeriesService serviceWith(MarketData marketData, String referencePrice) {
        LedgerService ledger = mock(LedgerService.class);
        when(ledger.resolveParty(anyString())).thenAnswer(i -> i.getArgument(0) + "::1");
        when(ledger.navFixingsVisibleTo(anyString())).thenReturn(List.of());
        when(ledger.committeesVisibleTo(anyString())).thenReturn(List.of());
        when(ledger.instrumentsVisibleTo(anyString()))
                .thenReturn(List.of(instrument(CBTC, "wrapped", referencePrice)));
        return new SeriesService(ledger, JsonlEventStore.inMemory(), ScheduleStore.inMemory(), marketData);
    }

    @Test
    @DisplayName("no fixing + venues answering: the row is the LIVE median, not the stored 65,000")
    void liveCompositeReplacesTheStaleStoredMark() {
        MarketData md = mock(MarketData.class);
        when(md.compositeOf(CBTC)).thenReturn(Optional.of(
                composite("83906.30", "0.6", "Coinbase", "Kraken", "Bitstamp")));

        List<SeriesRow> rows = serviceWith(md, "65000").series(CBTC);

        assertThat(rows).hasSize(1);
        SeriesRow r = rows.get(0);
        assertThat(r.price()).isEqualByComparingTo("83906.30");
        assertThat(r.price()).isNotEqualByComparingTo("65000");
        assertThat(r.tier()).isZero();
        assertThat(r.tierLabel()).isEqualTo("indicative");
        assertThat(r.k()).isZero();
        assertThat(r.signers()).isEmpty();
        assertThat(r.note())
                .contains("median of Coinbase, Kraken, Bitstamp")
                .contains("0.6 bps apart")
                .contains("attested by nobody");
    }

    @Test
    @DisplayName("a live mark is still tier 0 — real is not the same as signed")
    void liveIsNotAttested() {
        MarketData md = mock(MarketData.class);
        when(md.compositeOf(CBTC)).thenReturn(Optional.of(composite("83906.30", "0.6", "Coinbase", "Kraken")));

        SeriesRow r = serviceWith(md, "65000").series(CBTC).get(0);

        assertThat(r.tier()).isZero();
        assertThat(r.fixingCid()).isNull();
        assertThat(PublicBenchmarkController.displayLabel(r, false))
                .isEqualTo("indicative — live market observation, not attested");
    }

    @Test
    @DisplayName("every venue down: falls back to the stored mark and says that is what it is")
    void fallsBackToTheStoredMarkWhenNoVenueAnswers() {
        MarketData md = mock(MarketData.class);
        when(md.compositeOf(anyString())).thenReturn(Optional.empty());

        SeriesRow r = serviceWith(md, "65000").series(CBTC).get(0);

        assertThat(r.price()).isEqualByComparingTo("65000");
        assertThat(r.tierLabel()).isEqualTo("seed");
        assertThat(r.note()).contains("issuer's published reference mark at seed");
        assertThat(PublicBenchmarkController.displayLabel(r, false)).isEqualTo("seed value, not attested");
    }

    @Test
    @DisplayName("a single venue still publishes, and the row says only one answered")
    void singleVenueIsFlaggedOnTheRow() {
        MarketData md = mock(MarketData.class);
        var only = new MarketData.Composite(new BigDecimal("83911.03"),
                List.of(new MarketData.VenueQuote("Bitstamp", new BigDecimal("83911.03"), null),
                        new MarketData.VenueQuote("Coinbase", null, "HttpConnectTimeoutException")),
                BigDecimal.ZERO);
        when(md.compositeOf(CBTC)).thenReturn(Optional.of(only));

        SeriesRow r = serviceWith(md, "65000").series(CBTC).get(0);

        assertThat(r.price()).isEqualByComparingTo("83911.03");
        assertThat(r.note()).contains("Bitstamp only — single source").contains("ONLY ONE VENUE ANSWERED");
    }
}
