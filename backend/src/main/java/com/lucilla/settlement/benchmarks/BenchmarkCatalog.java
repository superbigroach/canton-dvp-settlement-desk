package com.lucilla.settlement.benchmarks;

import com.lucilla.settlement.ledger.LedgerCommands;
import com.lucilla.settlement.ledger.LedgerService;
import com.lucilla.settlement.scheduler.ScheduleStore;
import com.lucilla.settlement.scheduler.StrikeSchedule;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Which benchmarks CrossDesk publishes: every instrument on the strike schedule, plus
 * any fund the desk lists. Names and kinds are derived, not typed — the schedule says
 * whether it is a wrapped close or a fund NAV, the ledger says what it is called.
 */
@Component
public class BenchmarkCatalog {

    /** The static half of a benchmark's product page. */
    public record Product(String id, String name, String kind, String publishTime, String timezone,
                          String description, String session) {
    }

    private final LedgerService ledger;
    private final ScheduleStore schedules;

    public BenchmarkCatalog(LedgerService ledger, ScheduleStore schedules) {
        this.ledger = ledger;
        this.schedules = schedules;
    }

    public List<Product> products() {
        Map<String, Product> out = new LinkedHashMap<>();
        Map<String, LedgerService.InstrumentView> instruments = new LinkedHashMap<>();
        try {
            for (var i : ledger.instrumentsVisibleTo(ledger.resolveParty("Issuer"))) {
                instruments.putIfAbsent(i.id(), i);
            }
        } catch (RuntimeException e) {
            // A ledger that is still coming up must not empty the catalogue: the schedule
            // alone names the products; descriptions fill in once the ledger answers.
        }
        for (StrikeSchedule s : schedules.all()) {
            var inst = instruments.get(s.getInstrumentId());
            String desc = inst == null ? "" : inst.description();
            out.put(s.getInstrumentId(), new Product(s.getInstrumentId(),
                    nameFor(s.getInstrumentId(), s.isFund(), s.getSession()),
                    s.isFund() ? "nav" : "wrapped",
                    s.getStrikeAt(), s.getTimezone(), desc, s.getSession()));
        }
        for (var i : instruments.values()) {
            if (LedgerCommands.FUND_KIND.equals(i.kind()) && !out.containsKey(i.id())) {
                out.put(i.id(), new Product(i.id(), nameFor(i.id(), true, "Close"), "nav",
                        "16:00", "Europe/London", i.description(), "Close"));
            }
        }
        return new ArrayList<>(out.values());
    }

    public Optional<Product> product(String id) {
        return products().stream().filter(p -> p.id().equalsIgnoreCase(id)).findFirst();
    }

    static String nameFor(String id, boolean fund, String session) {
        return fund ? id + " NAV" : id + " " + (session == null ? "Close" : session);
    }
}
