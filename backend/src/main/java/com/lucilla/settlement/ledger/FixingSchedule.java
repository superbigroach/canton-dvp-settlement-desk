package com.lucilla.settlement.ledger;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The declared strike schedule — docs/FIXING_METHODOLOGY.md §4.
 *
 * <p>WHAT THIS DOES AND, MORE IMPORTANTLY, WHAT IT DOES NOT. §4 requires a strike time
 * declared per identifier at launch and thereafter FIXED, changeable only under §9's
 * thirty days' notice. Until now strikes were triggered by hand, which meant the
 * published record depended on somebody remembering — and a benchmark whose continuity
 * rests on an operator's diary is not one a contract can safely reference.
 *
 * <p>This class computes, for a declared schedule and an instant, <em>whether today's
 * strike is due, and how overdue it is</em>. It deliberately DOES NOT propose a price.
 * Auto-striking would mean the desk inventing a number when the committee had not met,
 * which is the one thing the whole design exists to prevent: a fixing that nobody
 * attested is not a cheaper fixing, it is a lie with a timestamp. Detection is the
 * honest half of §4 — the half that turns "we forgot" into a visible, actionable fact
 * and feeds §3 Tier 3's carry-forward flag.
 *
 * <p>BUSINESS DAYS COME FROM THE IDENTIFIER'S DECLARED CALENDAR ({@link StrikeCalendars}):
 * {@code daily} for a wrapped crypto asset whose reference rate prints every day of the
 * year, {@code nyse} / {@code lse} for an asset whose market keeps exchange holidays,
 * {@code weekdays} for the old approximation. The bare {@link #isBusinessDay(LocalDate)}
 * is that approximation and is kept for callers that have no calendar to hand.
 */
public final class FixingSchedule {

    private FixingSchedule() {
    }

    /**
     * One identifier's declared schedule. Immutable by intent: §9 makes the strike time
     * a material term, so changing one is a governance event and not a config reload.
     */
    public record Declared(
            String instrumentId,
            String session,      // "Open" | "Close"
            LocalTime strikeAt,  // e.g. 16:00
            ZoneId zone,         // e.g. Europe/London — a fixing without a zone is not a time
            long graceMinutes,   // how long after the strike before it counts as overdue
            String calendar) {   // a StrikeCalendars name: daily | weekdays | nyse | lse

        /** The pre-calendar constructor: the weekday approximation. */
        public Declared(String instrumentId, String session, LocalTime strikeAt, ZoneId zone, long graceMinutes) {
            this(instrumentId, session, strikeAt, zone, graceMinutes, StrikeCalendars.WEEKDAYS);
        }
    }

    /** How a declared strike stands at a given instant. */
    public enum State {
        /** Not a strike day for this identifier's calendar, so nothing is expected. */
        NOT_DUE_TODAY,
        /** Today's strike time has not arrived yet. */
        PENDING,
        /** The strike time has passed, within grace, and no fixing exists yet. */
        DUE,
        /** Past grace with no fixing. §3 Tier 3 applies and the record must say so. */
        OVERDUE,
        /** A fixing exists whose strike falls on today's scheduled instant. */
        STRUCK
    }

    /** The schedule, plus where it stands right now. */
    public record Status(
            Declared declared,
            State state,
            Instant expectedAt,     // today's strike instant, or the next one if not due today
            long minutesLate,       // 0 unless DUE/OVERDUE
            String note) {
    }

    /**
     * The default roster. Deliberately a constant rather than a database: §9 makes a
     * strike time a material term requiring thirty days' notice, so it belongs in a
     * reviewed, diffable artefact and not in a row somebody can quietly update.
     *
     * <p>16:00 Europe/London matches the CME CF Bitcoin Reference Rate window this
     * desk's wrapped-asset fixings reference, so a cBTC mark and the benchmark it is
     * derived from describe the same moment rather than two moments an hour apart. The
     * BRR is calculated every day of the year, so the calendar is {@code daily}; LX1
     * holds only crypto, so it is daily too.
     */
    public static List<Declared> defaults() {
        ZoneId london = ZoneId.of("Europe/London");
        return List.of(
                new Declared("CBTC", "Close", LocalTime.of(16, 0), london, 60, StrikeCalendars.DAILY),
                new Declared("cETH", "Close", LocalTime.of(16, 0), london, 60, StrikeCalendars.DAILY),
                new Declared("LX1", "Close", LocalTime.of(16, 0), london, 60, StrikeCalendars.DAILY));
    }

    /** True when {@code date} is a weekday — the approximation for callers with no calendar. */
    public static boolean isBusinessDay(LocalDate date) {
        DayOfWeek d = date.getDayOfWeek();
        return d != DayOfWeek.SATURDAY && d != DayOfWeek.SUNDAY;
    }

    /** True when {@code calendar} strikes on {@code date}. */
    public static boolean isBusinessDay(StrikeCalendars calendars, String calendar, LocalDate date) {
        return calendars.strikes(calendar, date);
    }

    /** Today's scheduled strike instant for a declaration, in its own zone. */
    public static Instant strikeInstantOn(Declared d, LocalDate date) {
        return ZonedDateTime.of(date, d.strikeAt(), d.zone()).toInstant();
    }

    /** The next weekday on or after {@code date} (the approximation). */
    public static LocalDate nextBusinessDay(LocalDate date) {
        LocalDate x = date;
        while (!isBusinessDay(x)) {
            x = x.plusDays(1);
        }
        return x;
    }

    /** Where a declared strike stands at {@code now}, under the built-in calendars. */
    public static Status statusOf(Declared d, Instant now, Instant lastStruck) {
        return statusOf(Holder.DEFAULTS, d, now, lastStruck);
    }

    /** Lazily loaded so a class that only wants {@link #key} never parses a YAML file. */
    private static final class Holder {
        static final StrikeCalendars DEFAULTS = StrikeCalendars.defaults();
    }

    /**
     * Where a declared strike stands at {@code now}.
     *
     * @param lastStruck the most recent fixing's strike instant for this identifier, or
     *                   {@code null} if there has never been one. Compared against
     *                   today's scheduled instant rather than "within 24 hours", because
     *                   a fixing struck at yesterday's 16:00 is emphatically not today's.
     */
    public static Status statusOf(StrikeCalendars calendars, Declared d, Instant now, Instant lastStruck) {
        LocalDate today = now.atZone(d.zone()).toLocalDate();
        String cal = d.calendar() == null || d.calendar().isBlank() ? StrikeCalendars.WEEKDAYS : d.calendar();

        if (!calendars.strikes(cal, today)) {
            LocalDate next = calendars.nextStrikeDay(cal, today);
            StrikeCalendars.Calendar c = calendars.get(cal);
            String why = c != null && c.holidays().containsKey(today)
                    ? "closed: " + c.holidays().get(today) + " (" + cal + " calendar)"
                    : "not a strike day on the " + cal + " calendar";
            return new Status(d, State.NOT_DUE_TODAY, strikeInstantOn(d, next), 0,
                    why + "; next strike " + next
                            + (StrikeCalendars.WEEKDAYS.equals(cal) ? "; no holiday calendar is carried for this identifier" : ""));
        }

        Instant expected = strikeInstantOn(d, today);

        // STRUCK is judged against today's scheduled instant, not against a rolling
        // window. A fixing whose strike is at or after today's expected instant settles
        // today's obligation; one from yesterday does not, however recent it looks.
        if (lastStruck != null && !lastStruck.isBefore(expected)) {
            return new Status(d, State.STRUCK, expected, 0, "today's strike is on the record");
        }

        if (now.isBefore(expected)) {
            return new Status(d, State.PENDING, expected, 0, "today's strike has not come round yet");
        }

        long late = Duration.between(expected, now).toMinutes();
        if (late <= d.graceMinutes()) {
            return new Status(d, State.DUE, expected, late,
                    "the strike time has passed and no fixing has been finalised");
        }
        return new Status(d, State.OVERDUE, expected, late,
                "past grace with no fixing — §3 Tier 3 carry-forward applies and consumers "
                        + "must be shown the age of the underlying strike");
    }

    /** Status for every declared identifier, given the last strike known for each. */
    public static List<Status> statuses(
            List<Declared> declared, Instant now, Map<String, Instant> lastStruckByKey) {
        return statuses(Holder.DEFAULTS, declared, now, lastStruckByKey);
    }

    public static List<Status> statuses(StrikeCalendars calendars,
            List<Declared> declared, Instant now, Map<String, Instant> lastStruckByKey) {
        Map<String, Instant> last = lastStruckByKey == null ? new LinkedHashMap<>() : lastStruckByKey;
        return declared.stream()
                .map(d -> statusOf(calendars, d, now, last.get(key(d.instrumentId(), d.session()))))
                .toList();
    }

    /** The lookup key for a declaration: an identifier is (instrument, session). */
    public static String key(String instrumentId, String session) {
        return (instrumentId == null ? "" : instrumentId.trim().toLowerCase())
                + "|" + (session == null ? "" : session.trim().toLowerCase());
    }
}
