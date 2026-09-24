package com.lucilla.settlement.benchmarks;

import com.lucilla.settlement.events.EventStore;
import com.lucilla.settlement.ledger.LedgerCommands;
import com.lucilla.settlement.ledger.LedgerService;
import com.lucilla.settlement.scheduler.ScheduleStore;
import com.lucilla.settlement.scheduler.StrikeSchedule;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
        List<LedgerService.NavFixingView> fixings = recognised(ledger.navFixingsVisibleTo(auditor));
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

    /**
     * Only fixings that a REAL committee could have produced.
     *
     * <p>WHY. On-ledger, {@code NavFixing} is {@code signatory attestors} and {@code admin},
     * {@code threshold} and the attestor list are plain fields. Nothing binds a fixing to an
     * {@code OperatorCommittee}, so a single party can create a proposal naming the real
     * administrator with {@code threshold = 1} and itself as sole approver, finalise it, and
     * hold a "committee fixing" that this service would otherwise publish as attested.
     * (Daml audit G1/G2, 22 Sep 2026.) Until the contract itself carries the committee, the
     * consumer must do the binding: a fixing is recognised only if some committee visible
     * to the auditor has the same admin, contains every attestor as a member, declares the
     * same threshold, and that threshold is actually met. Anything else is dropped and
     * logged — never rendered, never used as a mark.
     */
    List<LedgerService.NavFixingView> recognised(List<LedgerService.NavFixingView> fixings) {
        List<LedgerService.CommitteeView> committees;
        try {
            committees = ledger.committeesVisibleTo(ledger.resolveParty("Auditor"));
        } catch (RuntimeException e) {
            committees = List.of();
        }
        List<LedgerService.NavFixingView> out = new ArrayList<>();
        for (var f : fixings) {
            if (futureDated(f)) continue;
            boolean ok = false;
            for (var c : committees) {
                if (f.admin() == null || !f.admin().equals(c.admin())) continue;
                if (f.threshold() != c.threshold()) continue;
                if (f.attestors() == null || f.attestors().size() < c.threshold()) continue;
                if (!c.members().containsAll(f.attestors())) continue;
                ok = true;
                break;
            }
            if (ok) {
                out.add(f);
            } else {
                log.warn("FIXING {} for {} REJECTED: not produced by any recognised committee "
                        + "(admin={}, threshold={}, attestors={}) — possible forgery, not published",
                        f.contractId(), f.instrumentId(), LedgerService.labelOf(f.admin()),
                        f.threshold(), f.attestors());
            }
        }
        return out;
    }

    /**
     * Could the administrator have produced every one of these signatures itself?
     *
     * <p>WHY THIS IS PUBLISHED. At trust level L1 the signer's party lives on the
     * administrator's participant and the administrator exercises the choice on its behalf
     * (rulebook §6.7, SIGNER_PROTOCOL §4a). That is honest for a pilot and dishonest as a
     * destination, and the rulebook requires the pilot configuration to be printed on every
     * value. A page that says "attested, 2 of 3" while all three seats are operated by the
     * administrator tells a reader something untrue by omission, so the label says it.
     *
     * <p>The test is exact rather than rhetorical: a signer is administrator-operated when its
     * party is one this desk is configured to act as. A seat held on the signer's own
     * participant (L3) is not in that list, so the disclosure disappears by itself the day a
     * real counterparty signs — nobody has to remember to remove it.
     */
    public boolean allSeatsAdministratorOperated(List<String> signerLabels) {
        if (signerLabels == null || signerLabels.isEmpty()) return false;
        List<LedgerService.PartyView> ours;
        try {
            ours = ledger.listParties();
        } catch (RuntimeException e) {
            return false;   // cannot prove it; say nothing rather than claim independence
        }
        for (String label : signerLabels) {
            boolean mine = ours.stream().anyMatch(p ->
                    p.label().equalsIgnoreCase(label) || p.party().equals(label)
                            || p.party().startsWith(label + "::"));
            if (!mine) return false;
        }
        return true;
    }

    /**
     * Is this fixing dated after today, in the instrument's own zone?
     *
     * <p>WHY THIS EXISTS. A {@code NavFixing} carries the date it claims to observe, and the
     * ledger enforces one per instrument, session and date — but it cannot know what day it is,
     * so nothing on-chain stops a fixing dated years ahead. On 24 Sep 2026 a test script whose
     * date expression advanced a day per minute struck a REAL attested fixing dated 2039-02-03
     * on DevNet. It is signed, valid and permanent; it simply describes a day that has not
     * happened. Publishing it would put a 2039 row in a public benchmark history and, being the
     * newest row, make it the quoted value.
     *
     * <p>So it is dropped here, at the one gate every published surface passes through, and
     * logged loudly: a fixing about the future is an anomaly somebody must see, not something
     * to hide. {@link com.lucilla.settlement.web.SettlementController#asOfOrToday} stops new
     * ones being created; this stops the ones already on the ledger being quoted. The desk's
     * own authenticated views still show it, because an operator investigating needs to.
     */
    private boolean futureDated(LedgerService.NavFixingView f) {
        if (f.asOfDate() == null) return false;
        ZoneId zone = schedules.byInstrument(f.instrumentId())
                .map(StrikeSchedule::zone).orElse(ZoneId.of("Europe/London"));
        if (!f.asOfDate().isAfter(LocalDate.now(zone))) return false;
        log.warn("FIXING {} for {} NOT PUBLISHED: as-of date {} is in the future (today is {} {}); "
                + "the attestation is valid but describes a day that has not happened",
                f.contractId(), f.instrumentId(), f.asOfDate(), LocalDate.now(zone), zone);
        return true;
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SeriesService.class);
}
