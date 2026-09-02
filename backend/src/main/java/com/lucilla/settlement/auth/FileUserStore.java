package com.lucilla.settlement.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * In-memory mapping seeded from {@code users.yml}, with every edit persisted to
 * {@code <data-dir>/users.json} and re-applied on top of the seed at the next boot.
 *
 * <p>The seed is the roster a fresh sandbox needs (docs/PRODUCT-PLAN.md §3); the JSON
 * file is what the admin console and the signer portal have changed since — settings,
 * API-key hashes, added users. Seed rows are matched to persisted rows by uid, so a
 * change to a seeded user survives a restart and a new seed row still appears.
 *
 * <p>Thread-safe by coarse locking: this is a roster of tens of rows, read on every
 * request and written a few times a day.
 */
public class FileUserStore implements UserStore {

    private static final Logger log = LoggerFactory.getLogger(FileUserStore.class);

    private final Map<String, UserRecord> byUid = new LinkedHashMap<>();
    private final Path persistFile;   // null = memory only
    private final ObjectMapper json = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public FileUserStore(String seedLocation, Path dataDir) {
        this.persistFile = dataDir == null ? null : dataDir.resolve("users.json");
        loadSeed(seedLocation);
        loadPersisted();
    }

    /** A store with only the given rows and no file — for tests. */
    public static FileUserStore inMemory(List<UserRecord> seed) {
        FileUserStore s = new FileUserStore(null, null);
        for (UserRecord u : seed) s.save(u);
        return s;
    }

    // ---- loading ------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private void loadSeed(String location) {
        if (location == null || location.isBlank()) return;
        try {
            Resource r = new DefaultResourceLoader().getResource(location);
            if (!r.exists()) {
                log.warn("users seed {} not found — starting with an empty roster", location);
                return;
            }
            try (InputStream in = r.getInputStream()) {
                Object doc = new Yaml().load(in);
                List<Object> rows = new ArrayList<>();
                if (doc instanceof Map<?, ?> m && m.get("users") instanceof List<?> l) {
                    rows.addAll(l);
                } else if (doc instanceof List<?> l) {
                    rows.addAll(l);
                }
                for (Object o : rows) {
                    if (o instanceof Map<?, ?> row) {
                        UserRecord u = json.convertValue(row, UserRecord.class);
                        put(u);
                    }
                }
            }
            log.info("users seed {}: {} user(s)", location, byUid.size());
        } catch (Exception e) {
            log.warn("could not read users seed {}: {}", location, e.toString());
        }
    }

    private void loadPersisted() {
        if (persistFile == null || !Files.exists(persistFile)) return;
        try {
            List<UserRecord> rows = json.readValue(Files.readString(persistFile, StandardCharsets.UTF_8),
                    json.getTypeFactory().constructCollectionType(List.class, UserRecord.class));
            for (UserRecord u : rows) put(u);
            log.info("users persisted at {}: {} row(s) applied", persistFile, rows.size());
        } catch (IOException e) {
            log.warn("could not read {}: {}", persistFile, e.toString());
        }
    }

    private synchronized void put(UserRecord u) {
        if (u.getUid() == null || u.getUid().isBlank()) {
            u.setUid(uidFor(u));
        }
        if (u.getEmail() != null) u.setEmail(u.getEmail().trim().toLowerCase(Locale.ROOT));
        Role.parse(u.getRole());   // reject an unknown role at load rather than at request time
        byUid.put(u.getUid(), u);
    }

    private static String uidFor(UserRecord u) {
        if (u.getEmail() != null && !u.getEmail().isBlank()) {
            return "u-" + u.getEmail().trim().toLowerCase(Locale.ROOT)
                    .replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        }
        return "u-" + UUID.randomUUID();
    }

    private synchronized void persist() {
        if (persistFile == null) return;
        try {
            Files.createDirectories(persistFile.getParent());
            Files.writeString(persistFile, json.writeValueAsString(new ArrayList<>(byUid.values())),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("could not persist users to {}: {}", persistFile, e.toString());
        }
    }

    // ---- UserStore ----------------------------------------------------------

    @Override
    public synchronized List<UserRecord> all() {
        List<UserRecord> out = new ArrayList<>();
        for (UserRecord u : byUid.values()) out.add(u.copy());
        return out;
    }

    @Override
    public synchronized Optional<UserRecord> byUid(String uid) {
        if (uid == null) return Optional.empty();
        UserRecord u = byUid.get(uid);
        return u == null ? Optional.empty() : Optional.of(u.copy());
    }

    @Override
    public synchronized Optional<UserRecord> byEmail(String email) {
        if (email == null || email.isBlank()) return Optional.empty();
        String e = email.trim().toLowerCase(Locale.ROOT);
        return byUid.values().stream().filter(u -> e.equals(u.getEmail())).findFirst().map(UserRecord::copy);
    }

    @Override
    public synchronized Optional<UserRecord> bySandboxReference(String reference) {
        if (reference == null || reference.isBlank()) return Optional.empty();
        String ref = reference.trim().toLowerCase(Locale.ROOT);
        Optional<UserRecord> exact = byEmail(ref);
        if (exact.isPresent()) return exact;
        String local = ref.contains("@") ? ref.substring(0, ref.indexOf('@')) : ref;
        // Most specific first: a uid or an e-mail local part names ONE row; a party label
        // can be shared (the admin also acts as Issuer) and must not shadow the seat.
        Optional<UserRecord> byName = byUid.values().stream()
                .filter(u -> local.equals(lower(u.getUid())) || local.equals(localPart(u.getEmail())))
                .findFirst();
        if (byName.isPresent()) return byName.map(UserRecord::copy);
        Optional<UserRecord> byDisplay = byUid.values().stream()
                .filter(u -> ref.equals(lower(u.getDisplayName())))
                .findFirst();
        if (byDisplay.isPresent()) return byDisplay.map(UserRecord::copy);
        return byUid.values().stream()
                .filter(u -> local.equals(lower(labelOf(u.getParty()))))
                // A signer seat before an admin who merely acts as that party.
                .sorted(java.util.Comparator.comparingInt(u -> u.roleEnum() == Role.ADMIN ? 1 : 0))
                .findFirst()
                .map(UserRecord::copy);
    }

    @Override
    public synchronized Optional<UserRecord> byApiKeyHash(String hash) {
        if (hash == null) return Optional.empty();
        return byUid.values().stream().filter(u -> hash.equals(u.getApiKeyHash())).findFirst()
                .map(UserRecord::copy);
    }

    @Override
    public synchronized List<UserRecord> byParty(String partyOrLabel) {
        String label = lower(labelOf(partyOrLabel));
        if (label == null) return List.of();
        List<UserRecord> out = new ArrayList<>();
        for (UserRecord u : byUid.values()) {
            if (label.equals(lower(labelOf(u.getParty())))) out.add(u.copy());
        }
        return out;
    }

    @Override
    public synchronized UserRecord save(UserRecord user) {
        UserRecord u = user.copy();
        put(u);
        persist();
        return u.copy();
    }

    @Override
    public synchronized boolean delete(String uid) {
        boolean removed = byUid.remove(uid) != null;
        if (removed) persist();
        return removed;
    }

    // ---- helpers ------------------------------------------------------------

    private static String lower(String s) {
        return s == null ? null : s.trim().toLowerCase(Locale.ROOT);
    }

    private static String localPart(String email) {
        if (email == null) return null;
        String e = email.trim().toLowerCase(Locale.ROOT);
        return e.contains("@") ? e.substring(0, e.indexOf('@')) : e;
    }

    /** {@code Alice::1220…} → {@code Alice}; a bare label is returned as is. */
    static String labelOf(String party) {
        if (party == null) return null;
        int i = party.indexOf("::");
        return i < 0 ? party : party.substring(0, i);
    }
}
