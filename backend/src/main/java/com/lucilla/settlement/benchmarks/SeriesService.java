package com.lucilla.settlement.benchmarks;

import com.lucilla.settlement.events.EventStore;
import com.lucilla.settlement.ledger.LedgerCommands;
import com.lucilla.settlement.ledger.LedgerService;
import com.lucilla.settlement.scheduler.ScheduleStore;
import com.lucilla.settlement.scheduler.StrikeSchedule;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The published series for a benchmark, assembled from the ledger (the attested
 * fixings, read as the auditor who observes every one), the event log (fallback rows
 * the scheduler published) and the instrument's seed mark.
 */
@Service
public class SeriesService {

    private final LedgerService ledger;
    private final EventStore events;
    private final ScheduleStore schedules;
    /** When this desk came up — the "as of" of a seed mark, which the ledger does not date. */
    private final Instant bootedAt = Instant.now();

    public SeriesService(LedgerService ledger, EventStore events, ScheduleStore schedules) {
        this.ledger = ledger;
        this.events = events;
        this.schedules = schedules;
    }

    public List<SeriesRow> series(String instrumentId) {
        StrikeSchedule sched = schedules.byInstrument(instrumentId).orElse(null);
        String session = sched == null ? "Close" : sched.getSession();
        ZoneId zone = sched == null ? ZoneId.of("Europe/London") : sched.zone();
        String auditor = ledger.resolveParty("Auditor");
        List<LedgerService.NavFixingView> fixings = ledger.navFixingsVisibleTo(auditor);
        int committeeSize = committeeSize();

        // The seed: the instrument's own published mark (a fund: its NAV from components).
        BigDecimal seed = null;
        String seedNote = null;
        Optional<LedgerService.InstrumentView> inst = ledger.instrumentsVisibleTo(ledger.resolveParty("Issuer"))
                .stream().filter(i -> i.id().equalsIgnoreCase(instrumentId)).findFirst();
        if (inst.isPresent()) {
            boolean fund = LedgerCommands.FUND_KIND.equals(inst.get().kind());
            seed = fund ? ledger.referencePriceOf("Issuer", inst.get().id()).orElse(inst.get().referencePrice())
                    : inst.get().referencePrice();
            seedNote = fund
                    ? "derived: Σ units per share × component marks — not itself an attested fixing"
                    : "issuer's published reference mark at seed — not an attested fixing";
        }
        return SeriesDerivation.derive(instrumentId, session, fixings, events.all(), seed, bootedAt,
                seedNote, zone, LedgerService::labelOf, committeeSize);
    }

    /** The newest row, if any. */
    public Optional<SeriesRow> last(String instrumentId) {
        return series(instrumentId).stream().findFirst();
    }

    /** The newest row that carries a price (a tier-5 gap is not a price). */
    public Optional<SeriesRow> lastPriced(String instrumentId) {
        return series(instrumentId).stream().filter(r -> r.price() != null).findFirst();
    }

    /** The most recent ATTESTED wrapper factor for a wrapped asset, if the committee ever struck one. */
    public Optional<BigDecimal> lastAttestedFactor(String instrumentId) {
        return series(instrumentId).stream()
                .filter(r -> r.tier() == 1 && r.wrapperFactor() != null)
                .map(SeriesRow::wrapperFactor)
                .findFirst();
    }

    /** Funds that hold {@code instrumentId} as a component. */
    public List<BenchmarkCatalog.Product> referencing(String instrumentId) {
        List<BenchmarkCatalog.Product> out = new ArrayList<>();
        try {
            for (var b : ledger.basketsVisibleTo(ledger.resolveParty("Auditor"))) {
                boolean holds = b.components().stream()
                        .anyMatch(c -> c.instrumentId().equalsIgnoreCase(instrumentId));
                if (holds) {
                    out.add(new BenchmarkCatalog.Product(b.basketId(), b.basketId() + " NAV", "nav",
                            null, null, b.description(), "Close"));
                }
            }
        } catch (RuntimeException e) {
            // no baskets readable — nothing references it that we can see
        }
        return out;
    }

    int committeeSize() {
        try {
            return ledger.committeesVisibleTo(ledger.resolveParty("Auditor")).stream()
                    .mapToInt(c -> c.members().size()).max().orElse(0);
        } catch (RuntimeException e) {
            return 0;
        }
    }
}
