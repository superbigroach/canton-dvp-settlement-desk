package com.lucilla.settlement.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The strike schedule, in memory with a JSON file behind it. Defaults come from
 * {@link StrikeSchedule#defaults()}; a {@code PUT} replaces the whole list.
 */
public class ScheduleStore {

    private static final Logger log = LoggerFactory.getLogger(ScheduleStore.class);

    private final Path file;
    private final ObjectMapper json = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private List<StrikeSchedule> entries = StrikeSchedule.defaults();

    public ScheduleStore(Path dataDir) {
        this.file = dataDir == null ? null : dataDir.resolve("schedule.json");
        load();
    }

    public static ScheduleStore inMemory() {
        return new ScheduleStore(null);
    }

    private void load() {
        if (file == null || !Files.exists(file)) return;
        try {
            List<StrikeSchedule> rows = json.readValue(Files.readString(file, StandardCharsets.UTF_8),
                    json.getTypeFactory().constructCollectionType(List.class, StrikeSchedule.class));
            if (!rows.isEmpty()) {
                entries = rows;
                log.info("strike schedule loaded from {}: {} instrument(s)", file, rows.size());
            }
        } catch (IOException e) {
            log.warn("could not read {}: {} — using defaults", file, e.toString());
        }
    }

    public synchronized List<StrikeSchedule> all() {
        List<StrikeSchedule> out = new ArrayList<>();
        for (StrikeSchedule s : entries) out.add(s.copy());
        return out;
    }

    public synchronized Optional<StrikeSchedule> byInstrument(String instrumentId) {
        if (instrumentId == null) return Optional.empty();
        return entries.stream().filter(s -> instrumentId.equalsIgnoreCase(s.getInstrumentId()))
                .findFirst().map(StrikeSchedule::copy);
    }

    /** Validate and replace. A bad row is a 400 with the reason, and nothing changes. */
    public synchronized List<StrikeSchedule> replace(List<StrikeSchedule> rows) {
        if (rows == null || rows.isEmpty()) {
            throw new IllegalArgumentException("the schedule needs at least one instrument");
        }
        List<StrikeSchedule> clean = new ArrayList<>();
        for (StrikeSchedule s : rows) {
            if (s.getInstrumentId() == null || s.getInstrumentId().isBlank()) {
                throw new IllegalArgumentException("every schedule row needs an instrumentId");
            }
            try {
                LocalTime.parse(s.getStrikeAt());
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("strikeAt must be HH:mm, got '" + s.getStrikeAt() + "'");
            }
            try {
                ZoneId.of(s.getTimezone());
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("unknown timezone '" + s.getTimezone() + "'");
            }
            if (s.getWindowMinutes() < 0 || s.getWindowMinutes() > 24 * 60) {
                throw new IllegalArgumentException("windowMinutes must be between 0 and 1440");
            }
            if (!"Close".equalsIgnoreCase(s.getSession()) && !"Open".equalsIgnoreCase(s.getSession())) {
                throw new IllegalArgumentException("session must be Open or Close");
            }
            clean.add(s.copy());
        }
        entries = clean;
        persist();
        return all();
    }

    private void persist() {
        if (file == null) return;
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, json.writeValueAsString(entries), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("could not persist schedule to {}: {}", file, e.toString());
        }
    }
}
