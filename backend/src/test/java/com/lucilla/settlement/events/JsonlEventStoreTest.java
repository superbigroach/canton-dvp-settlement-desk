package com.lucilla.settlement.events;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Append-only, lineage-aware, and survives a restart. */
class JsonlEventStoreTest {

    @Test
    void lineageFollowsSuccessorCids(@TempDir Path dir) {
        JsonlEventStore s = new JsonlEventStore(dir);
        s.append(FixingEvent.of(FixingEvent.Kinds.PROPOSAL_CREATED, "CBTC", "p1", "p1", "Issuer", "issuer",
                null, "opened", new BigDecimal("65000"), null, "p1", Map.of("deadline", "2026-09-02T15:30:00Z")));
        s.append(FixingEvent.of(FixingEvent.Kinds.PROPOSAL_CONFIRMED, "CBTC", "p1", null, "Bank", "lender",
                null, null, null, null, "p2", Map.of("successorCid", "p2")));
        s.append(FixingEvent.of(FixingEvent.Kinds.PROPOSAL_CONFIRMED, "CBTC", "p2", null, "Venue", "venue",
                null, null, null, null, "p3", Map.of("successorCid", "p3")));
        s.append(FixingEvent.of(FixingEvent.Kinds.FIXING_FINALIZED, "CBTC", "p3", null, "Issuer", null,
                null, null, new BigDecimal("65000"), 1, "fix1", Map.of()));

        assertEquals("p1", s.rootOf("p3"));
        assertEquals("p1", s.rootOf("p2"));
        assertEquals("p3", s.latestCidOf("p1").orElseThrow());
        assertEquals(4, s.byProposal("p2").size(), "any cid in the chain returns the whole conversation");
        assertEquals(List.of(1L, 2L, 3L, 4L), s.all().stream().map(FixingEvent::id).toList());
        assertEquals("unknown", s.rootOf("unknown"));

        // A second store over the same directory replays the log.
        JsonlEventStore again = new JsonlEventStore(dir);
        assertEquals(4, again.all().size());
        assertEquals("p1", again.rootOf("p3"));
        FixingEvent next = again.append(FixingEvent.of("x", "CBTC", null, null, "a", null, null, null, null,
                null, null, Map.of()));
        assertEquals(5, next.id(), "ids continue after a replay");
    }

    @Test
    void queryFiltersByInstrumentAndTime() {
        JsonlEventStore s = JsonlEventStore.inMemory();
        s.append(FixingEvent.of("a", "CBTC", null, null, "x", null, null, null, null, null, null, Map.of()));
        s.append(FixingEvent.of("b", "cETH", null, null, "x", null, null, null, null, null, null, Map.of()));
        assertEquals(1, s.query("cbtc", null, null).size());
        assertEquals(2, s.query(null, Instant.now().minusSeconds(60), Instant.now().plusSeconds(60)).size());
        assertTrue(s.query(null, Instant.now().plusSeconds(60), null).isEmpty());
        String csv = EventsCsv.render(s.all());
        assertTrue(csv.startsWith(EventsCsv.HEADER + "\n"));
        assertEquals(3, csv.split("\n").length);
        assertEquals("\"a,b\"", EventsCsv.q("a,b"));
    }
}
