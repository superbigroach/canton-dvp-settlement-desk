package com.lucilla.settlement.benchmarks;

import com.lucilla.settlement.events.FixingEvent;
import com.lucilla.settlement.ledger.LedgerService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The published series: newest first, every row carries its tier, and a gap is a gap. */
class SeriesDerivationTest {

    static final ZoneId LONDON = ZoneId.of("Europe/London");

    static LedgerService.NavFixingView fixing(String cid, String instrument, Instant at, String price,
            String ref, String factor, String supersedes, List<String> attestors) {
        return new LedgerService.NavFixingView(cid, attestors, 2, instrument, "USDC", "Close",
                new BigDecimal(price), "r", BigDecimal.ZERO, "NONE", at, List.of(), at.plusSeconds(60),
                "committee", ref == null ? null : new BigDecimal(ref), factor == null ? null : new BigDecimal(factor),
                supersedes, supersedes == null ? null : "typo");
    }

    static FixingEvent event(String kind, String instrument, Instant at, String price, Integer tier, Map<String, Object> d) {
        return new FixingEvent(1, at.toString(), kind, instrument, null, null, "scheduler", "operator", null,
                "note", price == null ? null : new BigDecimal(price), tier, null, d);
    }

    @Test
    void mergesFixingsFallbacksAndSeedNewestFirst() {
        Instant d1 = Instant.parse("2026-09-01T15:00:00Z");
        Instant d2 = Instant.parse("2026-09-02T15:00:00Z");
        Instant d3 = Instant.parse("2026-09-03T15:00:00Z");
        Instant seed = Instant.parse("2026-08-30T09:00:00Z");
        List<SeriesRow> rows = SeriesDerivation.derive("CBTC", "Close",
                List.of(
                        fixing("f1", "CBTC", d1, "65000", "65000", "1.0", null, List.of("Issuer::1", "Bank::1")),
                        fixing("f-other", "cETH", d1, "2400", null, null, null, List.of("Issuer::1", "Bank::1"))),
                List.of(
                        event(FixingEvent.Kinds.FIXING_FALLBACK, "CBTC", d2, "64870", 3,
                                Map.of("referencePrice", "64870", "wrapperFactor", "1.0")),
                        event(FixingEvent.Kinds.FIXING_MISSED, "CBTC", d3, null, 5, Map.of()),
                        event(FixingEvent.Kinds.PROPOSAL_CREATED, "CBTC", d3, "1", null, Map.of())),
                new BigDecimal("65000"), seed, "seed", LONDON, LedgerService::labelOf, 3);

        assertEquals(4, rows.size());
        assertEquals(List.of(5, 3, 1, 0), rows.stream().map(SeriesRow::tier).toList(), "newest first");

        SeriesRow missed = rows.get(0);
        assertNull(missed.price(), "a gap is published as a gap");
        assertEquals("missed", missed.tierLabel());
        assertEquals("2026-09-03", missed.date());

        SeriesRow auto = rows.get(1);
        assertEquals(new BigDecimal("64870"), auto.price());
        assertEquals(new BigDecimal("1.0"), auto.wrapperFactor());
        assertEquals(0, auto.k());
        assertEquals(3, auto.n());
        assertTrue(auto.signers().isEmpty());

        SeriesRow attested = rows.get(2);
        assertEquals("f1", attested.fixingCid());
        assertEquals(2, attested.k(), "k is the real signature count");
        assertEquals(3, attested.n());
        assertEquals(List.of("Issuer", "Bank"), attested.signers());
        assertFalse(attested.restated());
        assertEquals("2026-09-01", attested.date());

        SeriesRow seedRow = rows.get(3);
        assertEquals(0, seedRow.tier());
        assertEquals("seed", seedRow.tierLabel());
        assertNull(seedRow.fixingCid());
    }

    @Test
    void restatementsAreMarkedOnBothSides() {
        Instant d1 = Instant.parse("2026-09-01T15:00:00Z");
        Instant d1b = Instant.parse("2026-09-01T17:00:00Z");
        List<SeriesRow> rows = SeriesDerivation.derive("CBTC", "Close",
                List.of(
                        fixing("orig", "CBTC", d1, "65000", null, null, null, List.of("A::1", "B::1")),
                        fixing("fix", "CBTC", d1b, "65100", null, null, "orig", List.of("A::1", "C::1"))),
                List.of(), null, null, null, LONDON, LedgerService::labelOf, 3);
        assertEquals(2, rows.size());
        assertEquals("fix", rows.get(0).fixingCid());
        assertTrue(rows.get(0).restated());
        assertNull(rows.get(0).superseded());
        assertEquals("orig", rows.get(1).fixingCid());
        assertEquals(Boolean.TRUE, rows.get(1).superseded());
        assertFalse(rows.get(1).restated());
    }

    @Test
    void sessionAndInstrumentFilter() {
        Instant d1 = Instant.parse("2026-09-01T15:00:00Z");
        var open = new LedgerService.NavFixingView("o", List.of("A::1", "B::1"), 2, "CBTC", "USDC", "Open",
                BigDecimal.TEN, "r", BigDecimal.ZERO, "NONE", d1, List.of(), d1, "committee", null, null, null, null);
        List<SeriesRow> rows = SeriesDerivation.derive("CBTC", "Close", List.of(open), List.of(),
                null, null, null, LONDON, LedgerService::labelOf, 0);
        assertTrue(rows.isEmpty(), "an Open fixing is not the Close series");
    }
}
