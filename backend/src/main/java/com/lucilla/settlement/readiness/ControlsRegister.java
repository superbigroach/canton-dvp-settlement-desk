package com.lucilla.settlement.readiness;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The controls register — one answer per question a client's risk team asks.
 *
 * <p>WHY IT IS A SERVICE AND NOT A DOCUMENT. The answers were already written down, in
 * {@code BUSINESS/10-ENTERPRISE-READINESS/}. Documents go stale in a particular way: the
 * control arrives six months later, somebody updates one file, the questionnaire answer
 * that was e-mailed in March is now wrong, and the certificate that was obtained in June
 * expires in December with nobody watching. Holding the state in one machine-readable
 * place means the answer is generated rather than remembered, and the expiry is computed
 * rather than diarised.
 *
 * <p>Loaded once at boot from {@code classpath:controls.yml}, overridable with
 * {@code CONTROLS_FILE} so the register can be edited without a rebuild — the same shape
 * as the user roster. A missing or broken file is not fatal: the desk runs, the register
 * is empty, and the endpoint says it is empty, because a readiness page that silently
 * shows nothing is worse than one that says it could not load.
 */
@Service
public class ControlsRegister {

    private static final Logger log = LoggerFactory.getLogger(ControlsRegister.class);

    /** JavaTimeModule so `expiresAt: 2027-03-01` binds to a LocalDate rather than failing. */
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private final List<Control> controls;
    private final String source;
    private final String loadError;

    public ControlsRegister(ResourceLoader loader,
                            @Value("${readiness.controls-file:classpath:controls.yml}") String path) {
        List<Control> loaded = new ArrayList<>();
        String err = null;
        try {
            Resource r = loader.getResource(path);
            if (!r.exists()) throw new IllegalStateException("not found");
            try (InputStream in = r.getInputStream()) {
                // Plain YAML to Maps, then Jackson to the bean — the same two-step the user
                // roster uses (FileUserStore). SnakeYAML's own bean constructor cannot build
                // the List<Control> property here, and pointing it at an arbitrary class is
                // a thing worth not doing in a file that may later be loaded from disk.
                Object doc = new Yaml().load(in);
                List<Object> rows = new ArrayList<>();
                if (doc instanceof Map<?, ?> m && m.get("controls") instanceof List<?> l) {
                    rows.addAll(l);
                } else if (doc instanceof List<?> l) {
                    rows.addAll(l);
                }
                for (Object o : rows) {
                    if (o instanceof Map<?, ?> row) loaded.add(MAPPER.convertValue(normalise(row), Control.class));
                }
            }
            if (loaded.isEmpty()) throw new IllegalStateException("no controls in file");
            log.info("controls register loaded: {} controls from {}", loaded.size(), path);
        } catch (Exception e) {
            err = e.getClass().getSimpleName() + ": " + e.getMessage();
            log.warn("controls register NOT loaded from {} ({}) — readiness will report empty", path, err);
            loaded = new ArrayList<>();
        }
        this.controls = List.copyOf(loaded);
        this.source = path;
        this.loadError = err;
    }

    /**
     * Turn any value SnakeYAML resolved into a {@code java.util.Date} back into an ISO date
     * string before Jackson sees it.
     *
     * <p>YAML 1.1 resolves an unquoted {@code 2026-09-25} to a timestamp, so SnakeYAML hands
     * over a {@code Date} and Jackson then reads its epoch MILLIS as an epoch DAY and throws.
     * Quoting every date in the file would also work, right up until somebody adds one
     * without quotes — and the one thing this register must not do is fail to load because a
     * renewal date was typed the obvious way.
     */
    private static Map<String, Object> normalise(Map<?, ?> row) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (var e : row.entrySet()) {
            Object v = e.getValue();
            if (v instanceof java.util.Date d) {
                v = d.toInstant().atZone(java.time.ZoneOffset.UTC).toLocalDate().toString();
            }
            out.put(String.valueOf(e.getKey()), v);
        }
        return out;
    }

    /** Every control, worst first — what is missing is the point of the page. */
    public List<Control> all() {
        List<Control> out = new ArrayList<>(controls);
        out.sort(Comparator
                .comparingInt((Control c) -> switch (c.statusEnum()) {
                    case NOT_STARTED -> 0;
                    case IN_PROGRESS -> 1;
                    case IN_PLACE -> c.isExpired() ? 0 : 2;   // an expired control is a gap
                    case NOT_APPLICABLE -> 3;
                })
                .thenComparing(c -> c.getCategory() == null ? "" : c.getCategory())
                .thenComparing(c -> c.getId() == null ? "" : c.getId()));
        return out;
    }

    /** Only the controls marked for the open web. */
    public List<Control> published() {
        return all().stream().filter(Control::isPublish).toList();
    }

    /**
     * Counts and the things that need somebody's attention.
     *
     * <p>{@code effective} counts controls that hold TODAY, which is not the same as the
     * number whose status says in_place — an expired policy is neither.
     */
    public Map<String, Object> summary() {
        List<Control> all = all();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", all.size());
        out.put("effective", all.stream().filter(Control::isEffective).count());
        out.put("inProgress", all.stream().filter(c -> c.statusEnum() == Control.Status.IN_PROGRESS).count());
        out.put("notStarted", all.stream().filter(c -> c.statusEnum() == Control.Status.NOT_STARTED).count());
        out.put("expired", all.stream().filter(Control::isExpired).map(Control::getId).toList());
        out.put("expiringSoon", all.stream().filter(Control::isExpiringSoon).map(Control::getId).toList());
        out.put("expiryWarningDays", Control.EXPIRY_WARNING_DAYS);
        // What is blocked on what — the line that actually drives a plan, because the
        // gaps blocked only on time are the ones a risk team fails us on first.
        Map<String, Long> blocked = new LinkedHashMap<>();
        for (String k : List.of("time", "money", "customer")) {
            blocked.put(k, all.stream()
                    .filter(c -> !c.isEffective())
                    .filter(c -> k.equalsIgnoreCase(c.getBlocked()))
                    .count());
        }
        out.put("blockedOn", blocked);
        out.put("source", source);
        if (loadError != null) out.put("loadError", loadError);
        return out;
    }
}
