package com.lucilla.settlement.ledger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LIVE REFERENCE PRICES — a proposal input, never an authority.
 *
 * <p>This is the one place the desk looks at the outside world, and it is important to
 * be precise about its status. The desk deliberately has <b>no oracle</b>: an official
 * mark is a {@code NavFixing} carrying K-of-N attestor signatures, and nothing values
 * against a number that has not been signed. What this service does is <em>fetch a
 * candidate</em> so a committee member does not have to type today's ETH price from
 * memory. The feed proposes; the committee disposes.
 *
 * <p>That distinction is not pedantry. A price scraped from an HTTP endpoint is one
 * party's unattributable claim. A price on a {@code NavFixing} is provable — the
 * signatory set <em>is</em> the attestor set, so `RunClose` can check the threshold was
 * met and a judge can check who stood behind the number. Wiring this feed straight into
 * the ledger would throw that away, so it is not wired in: it only ever pre-fills a
 * proposal a human then attests.
 *
 * <h2>Why several venues and not one</h2>
 *
 * <p>Until 25 Sep 2026 this class asked Coinbase and nobody else. At 15:01Z that day the
 * request timed out, the 16:00 Europe/London strike could not price, and the scheduler was
 * on course to publish a tier-5 <em>missed</em> row — a public statement that the committee
 * had failed to reach quorum, when the truth was that one HTTP endpoint had not answered.
 * A benchmark whose availability is one vendor's uptime is not a benchmark.
 *
 * <p>So a mark is now the <b>median of several independent venues</b>, queried in
 * parallel, each with its own short timeout. One venue down changes nothing. Two down and
 * the last one still answers, flagged as a single source so the committee can see it is
 * being asked to sign a thinner claim. This is also the honest methodology: the median
 * across constituent exchanges is what a published crypto reference rate actually is, and
 * {@link #SOURCES} is a list that can be said out loud — all three are free, keyless, and
 * are constituents of the established reference rates.
 *
 * <p><b>Dispersion is published, not smoothed away.</b> {@link Composite#spreadBps()} is
 * the gap between the highest and lowest venue. A tight spread means the venues agree; a
 * wide one is exactly the circumstance in which a human should look before signing, so it
 * travels with the mark into the proposal rationale instead of being averaged out of sight.
 *
 * <p><b>The wrapped-token assumption, stated plainly.</b> cETH is wrapped ETH and CBTC
 * is wrapped BTC, so each is marked at its underlying's spot. That is the right first
 * approximation and the one a real desk starts from, but it is an assumption: a wrapped
 * asset can trade at a basis to its underlying (bridge risk, redemption friction,
 * liquidity). A production desk would mark the basis separately. Here, the committee is
 * exactly the place that judgement belongs — which is another reason the feed does not
 * get to write to the ledger by itself.
 */
@Service
public class MarketData {

    private static final Logger log = LoggerFactory.getLogger(MarketData.class);

    /** Desk instrument id → the spot pair that underlies it. */
    private static final Map<String, String> UNDERLYING = Map.of(
            "cETH", "ETH-USD",
            "CBTC", "BTC-USD");

    /**
     * One public spot endpoint, and how to read a price out of it.
     *
     * <p>Each source takes the canonical {@code BASE-QUOTE} pair and renders it in that
     * venue's own spelling, because no two of them agree: Coinbase wants {@code BTC-USD},
     * Kraken wants {@code XBTUSD} for bitcoin specifically, Bitstamp wants {@code btcusd}.
     * The extraction is a JSON pointer walk rather than a mapper binding so that a venue
     * changing an unrelated field cannot break parsing.
     */
    private record Source(String name, java.util.function.Function<String, String> url,
                          java.util.function.Function<JsonNode, String> read) {
    }

    /** The venues, in the order a tie is broken. All free, no key, no account. */
    static final List<Source> SOURCES = List.of(
            new Source("Coinbase",
                    pair -> "https://api.coinbase.com/v2/prices/" + pair + "/spot",
                    root -> root.path("data").path("amount").asText(null)),
            new Source("Kraken",
                    pair -> "https://api.kraken.com/0/public/Ticker?pair=" + krakenPair(pair),
                    root -> {
                        JsonNode result = root.path("result");
                        if (!result.isObject()) return null;
                        // Kraken keys the result by its own internal pair name (XXBTZUSD),
                        // which is not derivable from the request — take the only entry.
                        var it = result.fields();
                        if (!it.hasNext()) return null;
                        // `c` is [last trade price, lot volume]; the last trade is the spot.
                        return it.next().getValue().path("c").path(0).asText(null);
                    }),
            new Source("Bitstamp",
                    pair -> "https://www.bitstamp.net/api/v2/ticker/" + bitstampPair(pair) + "/",
                    root -> root.path("last").asText(null)));

    /** Kraken spells bitcoin XBT and wants no separator. */
    private static String krakenPair(String pair) {
        return pair.replace("BTC", "XBT").replace("-", "");
    }

    /** Bitstamp wants lower case and no separator. */
    private static String bitstampPair(String pair) {
        return pair.replace("-", "").toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * USYC's published net yield, used as the default accrual rate for the modelled
     * money-market instrument.
     *
     * <p>USYC (Hashnote International Short Duration Fund, now Circle) holds US Treasury
     * reverse repo and is <b>the</b> tokenised money-market fund on Canton Network. There
     * is no free public endpoint for its net yield, so this is the published figure and
     * it is a DEFAULT, not a fact the ledger trusts — the committee attests whatever rate
     * it is willing to sign. Update it from circle.com/usyc before a demo.
     */
    public static final BigDecimal USYC_NET_YIELD = new BigDecimal("0.0320");

    /** ACT/360 — the convention every USD money-market instrument is quoted on. */
    public static final String MONEY_MARKET_DAY_COUNT = "ACT/360";

    private static final Duration TTL = Duration.ofSeconds(15);

    /**
     * Per-venue budget. Deliberately short: three venues are queried at once, so the
     * whole composite costs one timeout, not three, and a slow venue is simply left out
     * rather than holding up a strike that has a window to hit.
     */
    private static final Duration PER_SOURCE_TIMEOUT = Duration.ofSeconds(6);

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    private record Cached(Composite composite, Instant at) {
    }

    /** What one venue said, or why it did not say anything. */
    public record VenueQuote(String venue, BigDecimal price, String error) {
        public boolean ok() {
            return price != null;
        }
    }

    /**
     * The median across the venues that answered, and the full working.
     *
     * @param price     the median — the number a committee member is offered
     * @param quotes    every venue that was asked, answered or not
     * @param spreadBps high-to-low dispersion in basis points, 0 when only one answered
     */
    public record Composite(BigDecimal price, List<VenueQuote> quotes, BigDecimal spreadBps) {

        public List<String> contributors() {
            return quotes.stream().filter(VenueQuote::ok).map(VenueQuote::venue).toList();
        }

        /** A single venue is a materially weaker claim and says so. */
        public boolean singleSource() {
            return contributors().size() < 2;
        }

        /** Human-readable provenance, e.g. {@code median of Coinbase, Kraken, Bitstamp}. */
        public String sourceLabel() {
            List<String> c = contributors();
            if (c.isEmpty()) return "no venue answered";
            if (c.size() == 1) return c.get(0) + " only — single source";
            return "median of " + String.join(", ", c);
        }
    }

    /** A candidate mark for one instrument, with where it came from. */
    public record LiveMark(
            String instrumentId, String symbol, BigDecimal price,
            String source, Instant asOf, String note) {
    }

    /** Candidate marks for every instrument this desk knows how to price. */
    public List<LiveMark> liveMarks() {
        Map<String, LiveMark> out = new LinkedHashMap<>();
        UNDERLYING.forEach((instrumentId, pair) ->
                composite(pair).ifPresent(c -> out.put(instrumentId, new LiveMark(
                        instrumentId, pair, c.price(), c.sourceLabel(), Instant.now(),
                        noteFor(c)))));
        return List.copyOf(out.values());
    }

    private static String noteFor(Composite c) {
        StringBuilder b = new StringBuilder("wrapped-token mark = underlying spot; any basis is a committee judgement");
        b.append("; venues ").append(c.spreadBps().toPlainString()).append(" bps apart");
        if (c.singleSource()) {
            b.append("; ONLY ONE VENUE ANSWERED — a thinner claim than a composite, check before signing");
        }
        return b.toString();
    }

    /** A candidate mark for one desk instrument, if we know how to price it. */
    public Optional<LiveMark> liveMarkOf(String instrumentId) {
        return liveMarks().stream()
                .filter(m -> m.instrumentId().equals(instrumentId))
                .findFirst();
    }

    /**
     * The full composite for a pair — every venue's answer and the median — so an
     * operator can see the working rather than a number. Cached with the median.
     */
    public Optional<Composite> compositeOf(String instrumentId) {
        String pair = UNDERLYING.get(instrumentId);
        return pair == null ? Optional.empty() : composite(pair);
    }

    /**
     * Backwards-compatible single number: the composite median for a pair.
     *
     * <p>Kept because callers that only want a price should not have to know how many
     * venues produced it, and because it is the shape every existing test asserts on.
     */
    public Optional<BigDecimal> spot(String pair) {
        return composite(pair).map(Composite::price);
    }

    /**
     * Ask every venue at once, take the median of those that answered.
     *
     * <p>Empty only when NOBODY answered — which stays a soft failure, because no
     * network at a demo must not break the desk, it must only mean the committee types
     * the number itself.
     */
    public Optional<Composite> composite(String pair) {
        Cached hit = cache.get(pair);
        if (hit != null && Duration.between(hit.at(), Instant.now()).compareTo(TTL) < 0) {
            return Optional.of(hit.composite());
        }

        List<CompletableFuture<VenueQuote>> calls = SOURCES.stream()
                .map(s -> CompletableFuture.supplyAsync(() -> ask(s, pair)))
                .toList();

        List<VenueQuote> quotes = new ArrayList<>();
        for (var f : calls) {
            try {
                // Each call already carries PER_SOURCE_TIMEOUT; this is only a backstop so a
                // hung client thread cannot outlive the strike window.
                quotes.add(f.get(PER_SOURCE_TIMEOUT.toMillis() + 2_000, java.util.concurrent.TimeUnit.MILLISECONDS));
            } catch (Exception e) {
                quotes.add(new VenueQuote("?", null, e.toString()));
            }
        }

        Optional<Composite> composed = compose(quotes);
        if (composed.isEmpty()) {
            log.warn("spot {} unavailable: no venue answered ({}) — propose the mark by hand",
                    pair, quotes.stream().map(q -> q.venue() + "=" + q.error()).toList());
            return Optional.empty();
        }

        Composite c = composed.get();
        if (c.singleSource()) {
            log.warn("spot {} composed from ONE venue only ({}) — a thinner claim; others: {}",
                    pair, c.contributors(),
                    quotes.stream().filter(q -> !q.ok()).map(q -> q.venue() + "=" + q.error()).toList());
        } else {
            log.info("spot {} = {} ({}, {} bps apart)", pair, c.price(), c.sourceLabel(), c.spreadBps());
        }
        cache.put(pair, new Cached(c, Instant.now()));
        return Optional.of(c);
    }

    /**
     * The median of the venues that answered, with the dispersion — the whole of the
     * decision, separated from the fetching so it can be tested without a network.
     *
     * <p>Median rather than mean on purpose: one venue printing a stale or manipulated
     * tick moves a mean and does not move a median of three. That property is the reason
     * published reference rates are medians, and it is worth more here than precision.
     *
     * @return empty when no venue answered — never a made-up number
     */
    static Optional<Composite> compose(List<VenueQuote> quotes) {
        List<BigDecimal> prices = quotes.stream()
                .filter(VenueQuote::ok)
                .map(VenueQuote::price)
                .sorted(Comparator.naturalOrder())
                .toList();
        if (prices.isEmpty()) return Optional.empty();

        BigDecimal median = median(prices);
        BigDecimal spreadBps = prices.size() < 2 ? BigDecimal.ZERO
                : prices.get(prices.size() - 1).subtract(prices.get(0))
                .multiply(new BigDecimal("10000"))
                .divide(median, 1, RoundingMode.HALF_UP);
        return Optional.of(new Composite(median, quotes, spreadBps));
    }

    /** Middle value, or the mean of the two middle values. Input must be sorted. */
    private static BigDecimal median(List<BigDecimal> sorted) {
        int n = sorted.size();
        if (n % 2 == 1) return sorted.get(n / 2);
        return sorted.get(n / 2 - 1).add(sorted.get(n / 2))
                .divide(new BigDecimal("2"), MathContext.DECIMAL64);
    }

    /** One venue, one request. Never throws — a failure is a quote with a reason. */
    private VenueQuote ask(Source s, String pair) {
        String url = s.url().apply(pair);
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(PER_SOURCE_TIMEOUT)
                    .header("Accept", "application/json")
                    // Some venues reject a request with no user agent as a bot.
                    .header("User-Agent", "etp-foundry-desk/1.0 (+https://etpfoundry.com)")
                    .GET()
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() / 100 != 2) {
                return new VenueQuote(s.name(), null, "HTTP " + res.statusCode());
            }
            String amount = s.read().apply(mapper.readTree(res.body()));
            if (amount == null || amount.isBlank()) {
                return new VenueQuote(s.name(), null, "no price field in response");
            }
            BigDecimal price = new BigDecimal(amount);
            if (price.signum() <= 0) {
                return new VenueQuote(s.name(), null, "non-positive price " + price);
            }
            return new VenueQuote(s.name(), price, null);
        } catch (Exception e) {
            return new VenueQuote(s.name(), null, e.getClass().getSimpleName());
        }
    }
}
