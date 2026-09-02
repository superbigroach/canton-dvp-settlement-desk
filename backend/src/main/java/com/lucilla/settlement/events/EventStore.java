package com.lucilla.settlement.events;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Append-only. There is no update and no delete, and that is the point of an audit log. */
public interface EventStore {

    /** Append; returns the stored row with its id assigned. */
    FixingEvent append(FixingEvent event);

    /** Every event, oldest first. */
    List<FixingEvent> all();

    /** Events for one proposal — any cid in its lineage — oldest first. */
    List<FixingEvent> byProposal(String cid);

    /** Filtered export, oldest first. Nulls mean "no bound". */
    List<FixingEvent> query(String instrument, Instant from, Instant to);

    /** The proposal's first cid, given any cid it has worn; the cid itself if unknown. */
    String rootOf(String cid);

    /** The newest cid known for a proposal root (what to exercise next). */
    Optional<String> latestCidOf(String rootCid);
}
