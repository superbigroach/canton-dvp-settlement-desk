package com.lucilla.settlement.ledger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The composition rule, tested without a network.
 *
 * <p>These are the cases that matter on a strike day: a venue is down, a venue is lying,
 * every venue is down. The fetching is not tested here — it is three HTTP calls and a
 * JSON pointer — but the decision made from whatever comes back is, because that is the
 * part that can publish a wrong number.
 */
class MarketDataCompositeTest {

    private static MarketData.VenueQuote ok(String venue, String price) {
        return new MarketData.VenueQuote(venue, new BigDecimal(price), null);
    }

    private static MarketData.VenueQuote down(String venue) {
        return new MarketData.VenueQuote(venue, null, "HttpConnectTimeoutException");
    }

    @Test
    @DisplayName("three venues that agree: the median, tiny spread, not single-source")
    void threeAgree() {
        var c = MarketData.compose(List.of(
                ok("Coinbase", "83906.165"), ok("Kraken", "83906.30"), ok("Bitstamp", "83911.03")))
                .orElseThrow();

        assertThat(c.price()).isEqualByComparingTo("83906.30");
        assertThat(c.singleSource()).isFalse();
        assertThat(c.contributors()).containsExactly("Coinbase", "Kraken", "Bitstamp");
        assertThat(c.sourceLabel()).isEqualTo("median of Coinbase, Kraken, Bitstamp");
        // (83911.03 - 83906.165) / 83906.30 * 10000 = 0.58 bps
        assertThat(c.spreadBps()).isEqualByComparingTo("0.6");
    }

    @Test
    @DisplayName("THE 15:01Z CASE — the venue that broke the strike is down, the mark still exists")
    void oneVenueDownIsANonEvent() {
        var c = MarketData.compose(List.of(
                down("Coinbase"), ok("Kraken", "83906.30"), ok("Bitstamp", "83911.03")))
                .orElseThrow();

        assertThat(c.price()).isEqualByComparingTo("83908.665");   // mean of the two
        assertThat(c.singleSource()).isFalse();
        assertThat(c.contributors()).containsExactly("Kraken", "Bitstamp");
    }

    @Test
    @DisplayName("a lying venue moves the mean but not the median — which is why it is a median")
    void outlierDoesNotMoveTheMedian() {
        var honest = MarketData.compose(List.of(
                ok("Coinbase", "83900"), ok("Kraken", "83910"), ok("Bitstamp", "83905")))
                .orElseThrow();
        var withLiar = MarketData.compose(List.of(
                ok("Coinbase", "83900"), ok("Kraken", "83910"), ok("Bitstamp", "1")))
                .orElseThrow();

        assertThat(honest.price()).isEqualByComparingTo("83905");
        // The outlier becomes the low end; the middle value is still a real price.
        assertThat(withLiar.price()).isEqualByComparingTo("83900");
        // ...and the dispersion screams, which is the signal a human needs before signing.
        assertThat(withLiar.spreadBps()).isGreaterThan(new BigDecimal("9000"));
    }

    @Test
    @DisplayName("one venue left: still a mark, but flagged as the thinner claim it is")
    void lastVenueStandingIsFlagged() {
        var c = MarketData.compose(List.of(
                down("Coinbase"), down("Kraken"), ok("Bitstamp", "83911.03")))
                .orElseThrow();

        assertThat(c.price()).isEqualByComparingTo("83911.03");
        assertThat(c.singleSource()).isTrue();
        assertThat(c.sourceLabel()).isEqualTo("Bitstamp only — single source");
        assertThat(c.spreadBps()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("every venue down: empty, never an invented number")
    void allDownIsEmpty() {
        assertThat(MarketData.compose(List.of(down("Coinbase"), down("Kraken"), down("Bitstamp"))))
                .isEqualTo(Optional.empty());
    }

    @Test
    @DisplayName("a non-positive or unparseable quote never reaches the composite")
    void badQuotesAreNotPrices() {
        var c = MarketData.compose(List.of(
                new MarketData.VenueQuote("Coinbase", null, "non-positive price -1"),
                new MarketData.VenueQuote("Kraken", null, "no price field in response"),
                ok("Bitstamp", "2688.74")))
                .orElseThrow();

        assertThat(c.price()).isEqualByComparingTo("2688.74");
        assertThat(c.singleSource()).isTrue();
    }

    @Test
    @DisplayName("an even number of venues takes the mean of the middle two")
    void evenCountTakesTheMiddlePair() {
        var c = MarketData.compose(List.of(
                ok("A", "100"), ok("B", "110"), ok("C", "120"), ok("D", "130")))
                .orElseThrow();

        assertThat(c.price()).isEqualByComparingTo("115");
    }
}
