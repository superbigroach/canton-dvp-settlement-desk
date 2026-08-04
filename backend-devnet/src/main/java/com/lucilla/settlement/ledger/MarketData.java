package com.lucilla.settlement.ledger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LIVE REFERENCE PRICES — a proposal input, never an authority.
 *
 * <p>This is the one place the desk looks at the outside world, and it is important to
 * be precise about its status. CrossDesk deliberately has <b>no oracle</b>: an official
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
 * <p><b>Source.</b> Coinbase's public spot endpoint — no key, no account, and a
 * defensible name to say out loud. Failures are soft: an empty result means "propose it
 * by hand", never a broken desk.
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

    private static final Duration TTL = Duration.ofSeconds(60);

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    private record Cached(BigDecimal price, Instant at) {
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
                spot(pair).ifPresent(p -> out.put(instrumentId, new LiveMark(
                        instrumentId, pair, p, "Coinbase spot", Instant.now(),
                        "wrapped-token mark = underlying spot; any basis is a committee judgement"))));
        return List.copyOf(out.values());
    }

    /** A candidate mark for one desk instrument, if we know how to price it. */
    public Optional<LiveMark> liveMarkOf(String instrumentId) {
        return liveMarks().stream()
                .filter(m -> m.instrumentId().equals(instrumentId))
                .findFirst();
    }

    /**
     * Spot for a Coinbase pair, cached for a minute so a panel that polls does not
     * hammer a public endpoint. Returns empty on any failure — never throws.
     */
    public Optional<BigDecimal> spot(String pair) {
        Cached hit = cache.get(pair);
        if (hit != null && Duration.between(hit.at(), Instant.now()).compareTo(TTL) < 0) {
            return Optional.of(hit.price());
        }
        try {
            HttpRequest req = HttpRequest.newBuilder(
                            URI.create("https://api.coinbase.com/v2/prices/" + pair + "/spot"))
                    .timeout(Duration.ofSeconds(6))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() / 100 != 2) {
                log.warn("spot {} returned HTTP {} — propose the mark by hand", pair, res.statusCode());
                return Optional.empty();
            }
            JsonNode amount = mapper.readTree(res.body()).path("data").path("amount");
            if (amount.isMissingNode() || amount.asText().isBlank()) {
                log.warn("spot {} response had no data.amount", pair);
                return Optional.empty();
            }
            BigDecimal price = new BigDecimal(amount.asText());
            if (price.signum() <= 0) {
                log.warn("spot {} returned a non-positive price: {}", pair, price);
                return Optional.empty();
            }
            cache.put(pair, new Cached(price, Instant.now()));
            return Optional.of(price);
        } catch (Exception e) {
            // Soft failure by design: no network at a demo must not break the desk, it
            // must only mean the committee types the number itself.
            log.warn("spot {} unavailable ({}) — propose the mark by hand", pair, e.toString());
            return Optional.empty();
        }
    }
}
