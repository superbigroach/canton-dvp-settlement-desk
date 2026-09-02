package com.lucilla.settlement.scheduler;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lucilla.settlement.ledger.FixingSchedule;
import com.lucilla.settlement.ledger.StrikeCalendars;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * One instrument's strike configuration — {@code GET/PUT /api/admin/schedule}
 * (docs/PRODUCT-PLAN.md §5): strike time, restrike window, calendar, which fallback
 * tiers are enabled, and the alternate signers tier 2 escalates to. Defaults: CBTC and
 * cETH Close at 16:00 Europe/London on the {@code daily} calendar; LX1 immediately
 * after its components (same time, {@code dependsOn} the two).
 *
 * <p>{@code calendar} names a {@link StrikeCalendars} calendar. Left blank it defaults
 * to {@code daily} — for a wrapped crypto asset because its reference rate prints every
 * day, and for a fund because a fund's own calendar is only ever narrowed by its
 * components: it strikes on a day only when every component strikes (the intersection
 * rule, applied in {@code StrikeService}). A fund of NYSE-listed components therefore
 * strikes on NYSE days without anyone having to say so twice.
 *
 * <p>{@code alternates} is {@code { issuer: [emails], lender: [...], venue: [...] }} —
 * users in the roster who are reminded at the second escalation if that seat has not
 * confirmed. They must be committee members on-ledger to be of any use; the runner says
 * so in the event and skips them otherwise.
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
    private String calendar;                     // daily | weekdays | nyse | lse; null = default for kind
    private List<String> dependsOn = new ArrayList<>();
    private Map<String, Boolean> tiersEnabled = defaultTiers();
    private Map<String, List<String>> alternates = new LinkedHashMap<>();

    public static Map<String, Boolean> defaultTiers() {
        Map<String, Boolean> t = new LinkedHashMap<>();
        t.put("tier2", true);    // escalation inside the window: reminders, then alternates
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
            s.calendar = d.calendar();
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

    /** The declared calendar, or the default for the kind: {@code daily} in both cases (see class comment). */
    public String effectiveCalendar() {
        return calendar == null || calendar.isBlank() ? StrikeCalendars.DAILY : calendar.trim().toLowerCase(Locale.ROOT);
    }

    /** The alternates configured for a seat, never null. */
    public List<String> alternatesFor(String seat) {
        if (alternates == null || seat == null) return List.of();
        for (Map.Entry<String, List<String>> e : alternates.entrySet()) {
            if (e.getKey() != null && e.getKey().trim().equalsIgnoreCase(seat.trim()) && e.getValue() != null) {
                return e.getValue().stream().filter(v -> v != null && !v.isBlank()).map(String::trim).toList();
            }
        }
        return List.of();
    }

    public StrikeSchedule copy() {
        StrikeSchedule s = new StrikeSchedule();
        s.instrumentId = instrumentId; s.session = session; s.strikeAt = strikeAt;
        s.timezone = timezone; s.windowMinutes = windowMinutes; s.enabled = enabled; s.kind = kind;
        s.calendar = calendar;
        s.dependsOn = dependsOn == null ? new ArrayList<>() : new ArrayList<>(dependsOn);
        s.tiersEnabled = tiersEnabled == null ? defaultTiers() : new LinkedHashMap<>(tiersEnabled);
        s.alternates = new LinkedHashMap<>();
        if (alternates != null) alternates.forEach((k, v) -> s.alternates.put(k, v == null ? new ArrayList<>() : new ArrayList<>(v)));
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
    /** Serialised as the effective value, so a reader never has to know the default rule. */
    public String getCalendar() { return effectiveCalendar(); }
    public void setCalendar(String calendar) {
        this.calendar = calendar == null || calendar.isBlank() ? null : calendar.trim().toLowerCase(Locale.ROOT);
    }
    public List<String> getDependsOn() { return dependsOn; }
    public void setDependsOn(List<String> dependsOn) {
        this.dependsOn = dependsOn == null ? new ArrayList<>() : dependsOn;
    }
    public Map<String, Boolean> getTiersEnabled() { return tiersEnabled; }
    public void setTiersEnabled(Map<String, Boolean> tiersEnabled) {
        this.tiersEnabled = tiersEnabled == null ? defaultTiers() : tiersEnabled;
    }
    public Map<String, List<String>> getAlternates() { return alternates; }
    public void setAlternates(Map<String, List<String>> alternates) {
        this.alternates = alternates == null ? new LinkedHashMap<>() : alternates;
    }
}
