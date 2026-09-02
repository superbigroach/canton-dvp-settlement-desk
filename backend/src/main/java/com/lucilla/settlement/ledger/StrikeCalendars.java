package com.lucilla.settlement.ledger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * The strike calendars — docs/FIXING_METHODOLOGY.md §4: "Business days: the calendar
 * declared per identifier."
 *
 * <p>WHY THIS REPLACES THE WEEKDAY APPROXIMATION. A weekday rule is wrong in both
 * directions. For a wrapped crypto asset it is wrong on Saturday and Sunday: the CME CF
 * Bitcoin Reference Rate is calculated every day of the year, so a cBTC fixing that
 * skips the weekend publishes a gap where the benchmark it references printed a value.
 * For an equity it is wrong on every exchange holiday: 3 July 2026 is a Friday and the
 * NYSE is shut, so a fixing due that day is not a missed strike, it is a day with no
 * close to strike against.
 *
 * <p>Four calendars ship, and the names are the values of {@code StrikeSchedule.calendar}:
 * <ul>
 *   <li>{@code daily} — every calendar day. The default for kind {@code CryptoWrapped}
 *       and for funds whose components are all crypto.
 *   <li>{@code weekdays} — Monday to Friday, no holidays (the old approximation, kept
 *       for an instrument whose market has no published calendar).
 *   <li>{@code nyse} — weekdays minus the NYSE's published full-day closures.
 *   <li>{@code lse} — weekdays minus the London Stock Exchange's closures.
 * </ul>
 * Holiday dates live in {@code src/main/resources/calendars/{nyse,lse}.yml} and are
 * overridable, per file, from a directory named by {@code CALENDARS_DIR}
 * ({@code scheduler.calendars-dir}). A file in that directory with a new name adds a
 * calendar under that name. A calendar only knows the years its file lists; a date
 * outside them falls back to the weekday rule, and {@link #coverage} says which years
 * are covered so an operator can see the file needs extending before it silently does.
 *
 * <p>A FUND STRIKES ONLY ON DAYS ALL ITS COMPONENTS STRIKE (the intersection rule).
 * That is not a property of this class — it needs the schedule — and lives in
 * {@code StrikeService}; this class answers for one calendar at a time.
 */
public final class StrikeCalendars {

    private static final Logger log = LoggerFactory.getLogger(StrikeCalendars.class);

    public static final String DAILY = "daily";
    public static final String WEEKDAYS = "weekdays";
    public static final String NYSE = "nyse";
    public static final String LSE = "lse";

    /** The calendars every build carries, whatever the override directory holds. */
    static final List<String> BUILT_IN_FILES = List.of(NYSE, LSE);

    /** One calendar: whether weekends strike, and the dated closures. */
    public record Calendar(String name, String title, String source, boolean weekends,
                           Map<LocalDate, String> holidays) {

        public boolean strikes(LocalDate date) {
            Objects.requireNonNull(date, "date");
            if (!weekends) {
                DayOfWeek d = date.getDayOfWeek();
                if (d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY) return false;
            }
            return !holidays.containsKey(date);
        }

        /** The next strike day on or after {@code date}. */
        public LocalDate nextStrikeDay(LocalDate date) {
            LocalDate x = date;
            int guard = 0;
            while (!strikes(x)) {
                x = x.plusDays(1);
                if (++guard > 400) throw new IllegalStateException("calendar " + name + " has no strike day in a year after " + date);
            }
            return x;
        }

        /** The years the holiday file covers — empty for the rule-only calendars. */
        public Set<Integer> coverage() {
            Set<Integer> years = new TreeSet<>();
            for (LocalDate d : holidays.keySet()) years.add(d.getYear());
            return years;
        }
    }

    private final Map<String, Calendar> byName = new LinkedHashMap<>();

    private StrikeCalendars() {
        byName.put(DAILY, new Calendar(DAILY, "Every calendar day",
                "the reference rate is calculated every day of the year (CME CF BRR)", true, Map.of()));
        byName.put(WEEKDAYS, new Calendar(WEEKDAYS, "Monday to Friday, no holiday calendar",
                "the weekday approximation", false, Map.of()));
    }

    /** The built-in calendars from the classpath. */
    public static StrikeCalendars defaults() {
        return load(null);
    }

    /**
     * The built-in calendars, each replaced by {@code <overrideDir>/<name>.yml} when that
     * file exists, plus any other {@code *.yml} in the directory as a calendar of that name.
     */
    public static StrikeCalendars load(Path overrideDir) {
        StrikeCalendars c = new StrikeCalendars();
        for (String name : BUILT_IN_FILES) {
            String resource = "calendars/" + name + ".yml";
            try (InputStream in = StrikeCalendars.class.getClassLoader().getResourceAsStream(resource)) {
                if (in == null) {
                    log.warn("calendar resource {} is missing from the build", resource);
                    continue;
                }
                c.put(parse(name, in));
            } catch (IOException | RuntimeException e) {
                log.warn("calendar {} could not be read: {}", resource, e.toString());
            }
        }
        if (overrideDir != null && Files.isDirectory(overrideDir)) {
            try (DirectoryStream<Path> files = Files.newDirectoryStream(overrideDir, "*.yml")) {
                for (Path f : files) {
                    String fn = f.getFileName().toString();
                    String name = fn.substring(0, fn.length() - ".yml".length()).toLowerCase(Locale.ROOT);
                    try (InputStream in = Files.newInputStream(f)) {
                        c.put(parse(name, in));
                        log.info("calendar {} loaded from {}", name, f);
                    } catch (IOException | RuntimeException e) {
                        log.warn("calendar override {} could not be read: {}", f, e.toString());
                    }
                }
            } catch (IOException e) {
                log.warn("calendar override directory {} could not be listed: {}", overrideDir, e.toString());
            }
        }
        return c;
    }

    /** Parse one calendar file. Public so a test can hand it a string. */
    @SuppressWarnings("unchecked")
    public static Calendar parse(String fallbackName, InputStream in) {
        Object doc = new Yaml().load(in);
        if (!(doc instanceof Map<?, ?> m)) {
            throw new IllegalArgumentException("a calendar file is a YAML mapping");
        }
        Map<String, Object> map = (Map<String, Object>) m;
        String name = str(map.get("calendar"), fallbackName).toLowerCase(Locale.ROOT);
        String title = str(map.get("name"), name);
        String source = str(map.get("source"), null);
        boolean weekends = Boolean.TRUE.equals(map.get("weekends"));
        Map<LocalDate, String> holidays = new TreeMap<>();
        Object h = map.get("holidays");
        if (h instanceof Map<?, ?> hm) {
            for (Map.Entry<?, ?> e : hm.entrySet()) {
                holidays.put(toDate(e.getKey()), str(e.getValue(), ""));
            }
        } else if (h instanceof Collection<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> row) {
                    holidays.put(toDate(row.get("date")), str(row.get("name"), ""));
                } else {
                    holidays.put(toDate(o), "");
                }
            }
        } else if (h != null) {
            throw new IllegalArgumentException("holidays must be a mapping of date → name or a list");
        }
        return new Calendar(name, title, source, weekends, Map.copyOf(holidays));
    }

    private static LocalDate toDate(Object o) {
        if (o instanceof java.util.Date d) {
            // SnakeYAML parses an unquoted yyyy-MM-dd as a java.util.Date at UTC midnight.
            return d.toInstant().atZone(java.time.ZoneOffset.UTC).toLocalDate();
        }
        if (o instanceof LocalDate d) return d;
        return LocalDate.parse(String.valueOf(o).trim());
    }

    private static String str(Object o, String dflt) {
        return o == null ? dflt : String.valueOf(o);
    }

    private void put(Calendar c) {
        byName.put(c.name(), c);
    }

    public Set<String> names() {
        return Set.copyOf(byName.keySet());
    }

    public List<Calendar> all() {
        return List.copyOf(byName.values());
    }

    public boolean has(String name) {
        return name != null && byName.containsKey(name.trim().toLowerCase(Locale.ROOT));
    }

    /** The calendar of that name, or {@code null}. Names are case-insensitive. */
    public Calendar get(String name) {
        return name == null ? null : byName.get(name.trim().toLowerCase(Locale.ROOT));
    }

    /**
     * Does {@code calendar} strike on {@code date}? An unknown or blank calendar name is
     * treated as {@code weekdays} — the safe direction: a strike that is expected and did
     * not happen is visible, a strike silently excused is not.
     */
    public boolean strikes(String calendar, LocalDate date) {
        Calendar c = get(calendar);
        return (c == null ? byName.get(WEEKDAYS) : c).strikes(date);
    }

    /** The next date on or after {@code date} on which {@code calendar} strikes. */
    public LocalDate nextStrikeDay(String calendar, LocalDate date) {
        Calendar c = get(calendar);
        return (c == null ? byName.get(WEEKDAYS) : c).nextStrikeDay(date);
    }

    /** Do ALL of the named calendars strike on {@code date} — the fund intersection rule. */
    public boolean allStrike(Collection<String> calendars, LocalDate date) {
        for (String c : calendars) {
            if (!strikes(c, date)) return false;
        }
        return true;
    }

    /** A one-line description for a status note. */
    public String describe(String calendar) {
        Calendar c = get(calendar);
        if (c == null) return "unknown calendar '" + calendar + "' (treated as weekdays)";
        Set<Integer> years = c.coverage();
        return c.name() + " — " + c.title() + (years.isEmpty() ? "" : "; holidays listed for " + years);
    }
}
