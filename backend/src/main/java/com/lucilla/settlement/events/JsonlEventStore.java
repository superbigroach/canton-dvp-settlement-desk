package com.lucilla.settlement.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory list plus one JSON line per event appended to
 * {@code <data-dir>/fixing_events.jsonl}; replayed at boot so the log outlives the
 * process. On the hosted sandbox the directory is as ephemeral as the ledger, which is
 * consistent: there is no world in which the events survive and the contracts do not.
 */
public class JsonlEventStore implements EventStore {

    private static final Logger log = LoggerFactory.getLogger(JsonlEventStore.class);

    private final List<FixingEvent> events = new ArrayList<>();
    private final Map<String, String> rootByCid = new HashMap<>();
    private final Map<String, String> latestByRoot = new HashMap<>();
    private final Path file;   // null = memory only
    private final ObjectMapper json = new ObjectMapper();
    private long nextId = 1;

    public JsonlEventStore(Path dataDir) {
        this.file = dataDir == null ? null : dataDir.resolve("fixing_events.jsonl");
        replay();
    }

    public static JsonlEventStore inMemory() {
        return new JsonlEventStore(null);
    }

    private void replay() {
        if (file == null || !Files.exists(file)) return;
        int n = 0;
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (line.isBlank()) continue;
                try {
                    FixingEvent e = json.readValue(line, FixingEvent.class);
                    index(e);
                    n++;
                } catch (IOException bad) {
                    log.warn("skipping unreadable event line: {}", bad.getMessage());
                }
            }
        } catch (IOException e) {
            log.warn("could not replay {}: {}", file, e.toString());
        }
        log.info("fixing_events: {} event(s) replayed from {}", n, file);
    }

    private void index(FixingEvent e) {
        events.add(e);
        nextId = Math.max(nextId, e.id() + 1);
        if (e.proposalCid() != null) {
            String root = e.rootCid() == null ? e.proposalCid() : e.rootCid();
            rootByCid.putIfAbsent(e.proposalCid(), root);
            rootByCid.putIfAbsent(root, root);
            latestByRoot.put(root, e.proposalCid());
            Object successor = e.details() == null ? null : e.details().get("successorCid");
            if (successor instanceof String s && !s.isBlank()) {
                rootByCid.putIfAbsent(s, root);
                latestByRoot.put(root, s);
            }
        }
    }

    @Override
    public synchronized FixingEvent append(FixingEvent event) {
        // The lineage the store already knows wins over whatever the caller guessed: a
        // confirmation names the cid it exercised, and that cid's root is on record.
        String known = event.proposalCid() == null ? null : rootByCid.get(event.proposalCid());
        String root = known != null ? known
                : event.rootCid() != null ? event.rootCid() : event.proposalCid();
        FixingEvent stored = new FixingEvent(nextId, event.ts(), event.kind(), event.instrument(),
                event.proposalCid(), root, event.actor(), event.seat(), event.condition(),
                event.reason(), event.price(), event.tier(), event.ledgerCid(), event.details());
        index(stored);
        if (file != null) {
            try {
                Files.createDirectories(file.getParent());
                try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                    w.write(json.writeValueAsString(stored));
                    w.newLine();
                }
            } catch (IOException e) {
                log.warn("could not append event {} to {}: {}", stored.id(), file, e.toString());
            }
        }
        return stored;
    }

    @Override
    public synchronized List<FixingEvent> all() {
        return List.copyOf(events);
    }

    @Override
    public synchronized List<FixingEvent> byProposal(String cid) {
        if (cid == null) return List.of();
        String root = rootOf(cid);
        return events.stream()
                .filter(e -> root.equals(e.rootCid()) || cid.equals(e.proposalCid()))
                .toList();
    }

    @Override
    public synchronized List<FixingEvent> query(String instrument, Instant from, Instant to) {
        return events.stream()
                .filter(e -> instrument == null || instrument.isBlank()
                        || instrument.equalsIgnoreCase(e.instrument()))
                .filter(e -> from == null || !e.instant().isBefore(from))
                .filter(e -> to == null || !e.instant().isAfter(to))
                .toList();
    }

    @Override
    public synchronized String rootOf(String cid) {
        if (cid == null) return null;
        return rootByCid.getOrDefault(cid, cid);
    }

    @Override
    public synchronized Optional<String> latestCidOf(String rootCid) {
        return Optional.ofNullable(latestByRoot.get(rootCid));
    }
}
