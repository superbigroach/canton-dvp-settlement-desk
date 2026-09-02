package com.lucilla.settlement.scheduler;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lucilla.settlement.ledger.FixingSchedule;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One instrument's strike configuration — {@code GET/PUT /api/admin/schedule}
 * (docs/PRODUCT-PLAN.md §5): strike time, restrike window, and which fallback tiers
 * are enabled. Defaults: CBTC and cETH Close at 16:00 Europe/London; LX1 immediately
 * after its components (same time, {@code dependsOn} the two).
 *
 * <p>Mutable bean so it round-trips through the admin API and the JSON file.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class StrikeSchedule {

    public static final int DEFAULT_WINDOW_MINUTES = 30;

    private String instrumentId;
    private String session = "Close";
    private String strikeAt = "16:00";           // HH:mm in `timezone`
    private String timezone = "Europe/London";
    private int windowMinutes = DEFAULT_WINDOW_MINUTES;
    private boolean enabled = true;
    private String kind = "wrapped";             // wrapped | fund
    private List<String> dependsOn = new ArrayList<>();
    private Map<String, Boolean> tiersEnabled = defaultTiers();

    public static Map<String, Boolean> defaultTiers() {
        Map<String, Boolean> t = new LinkedHashMap<>();
        t.put("tier2", false);   // alternate seats — not configured (stub)
        t.put("tier3", true);    // benchmark × last attested factor, automatic
        t.put("tier4", true);    // prior fixing, flagged carried-forward
        t.put("tier5", true);    // missed — a gap is published as a gap
        return t;
    }

    public static List<StrikeSchedule> defaults() {
        List<StrikeSchedule> out = new ArrayList<>();
        for (FixingSchedule.Declared d : FixingSchedule.defaults()) {
            StrikeSchedule s = new StrikeSchedule();
            s.instrumentId = d.instrumentId();
            s.session = d.session();
            s.strikeAt = String.format("%02d:%02d", d.strikeAt().getHour(), d.strikeAt().getMinute());
            s.timezone = d.zone().getId();
            if ("LX1".equals(d.instrumentId())) {
                s.kind = "fund";
                s.dependsOn = new ArrayList<>(List.of("CBTC", "cETH"));
            }
            out.add(s);
        }
        return out;
    }

    public boolean isFund() {
        return "fund".equalsIgnoreCase(kind);
    }

    public boolean tierEnabled(int tier) {
        Boolean b = tiersEnabled == null ? null : tiersEnabled.get("tier" + tier);
        return b != null && b;
    }

    public ZoneId zone() {
        return ZoneId.of(timezone == null || timezone.isBlank() ? "Europe/London" : timezone);
    }

    public LocalTime strikeTime() {
        return LocalTime.parse(strikeAt == null || strikeAt.isBlank() ? "16:00" : strikeAt);
    }

    /** The strike instant on a given local date. */
    public Instant strikeInstantOn(LocalDate date) {
        return ZonedDateTime.of(date, strikeTime(), zone()).toInstant();
    }

    /** The local date, in this schedule's zone, that {@code at} falls on. */
    public LocalDate dateOf(Instant at) {
        return at.atZone(zone()).toLocalDate();
    }

    public StrikeSchedule copy() {
        StrikeSchedule s = new StrikeSchedule();
        s.instrumentId = instrumentId; s.session = session; s.strikeAt = strikeAt;
        s.timezone = timezone; s.windowMinutes = windowMinutes; s.enabled = enabled; s.kind = kind;
        s.dependsOn = dependsOn == null ? new ArrayList<>() : new ArrayList<>(dependsOn);
        s.tiersEnabled = tiersEnabled == null ? defaultTiers() : new LinkedHashMap<>(tiersEnabled);
        return s;
    }

    /** frontend/src/desk/types.ts ScheduleRow.instrument — an alias of instrumentId. */
    public String getInstrument() { return instrumentId; }
    public void setInstrument(String instrument) {
        if (instrument != null && !instrument.isBlank()) this.instrumentId = instrument;
    }

    /** frontend ScheduleRow.tiers — {t2,t3,t4}; the same switches as tiersEnabled. */
    public Map<String, Boolean> getTiers() {
        Map<String, Boolean> t = new LinkedHashMap<>();
        t.put("t2", tierEnabled(2));
        t.put("t3", tierEnabled(3));
        t.put("t4", tierEnabled(4));
        t.put("t5", tierEnabled(5));
        return t;
    }
    public void setTiers(Map<String, Boolean> tiers) {
        if (tiers == null) return;
        Map<String, Boolean> t = tiersEnabled == null ? defaultTiers() : new LinkedHashMap<>(tiersEnabled);
        tiers.forEach((k, v) -> { if (k != null && k.startsWith("t") && v != null) t.put("tier" + k.substring(1), v); });
        this.tiersEnabled = t;
    }

    public String getInstrumentId() { return instrumentId; }
    public void setInstrumentId(String instrumentId) { this.instrumentId = instrumentId; }
    public String getSession() { return session; }
    public void setSession(String session) { this.session = session; }
    public String getStrikeAt() { return strikeAt; }
    public void setStrikeAt(String strikeAt) { this.strikeAt = strikeAt; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public int getWindowMinutes() { return windowMinutes; }
    public void setWindowMinutes(int windowMinutes) { this.windowMinutes = windowMinutes; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public List<String> getDependsOn() { return dependsOn; }
    public void setDependsOn(List<String> dependsOn) {
        this.dependsOn = dependsOn == null ? new ArrayList<>() : dependsOn;
    }
    public Map<String, Boolean> getTiersEnabled() { return tiersEnabled; }
    public void setTiersEnabled(Map<String, Boolean> tiersEnabled) {
        this.tiersEnabled = tiersEnabled == null ? defaultTiers() : tiersEnabled;
    }
}
