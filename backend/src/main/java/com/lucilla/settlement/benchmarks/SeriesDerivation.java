package com.lucilla.settlement.benchmarks;

import com.lucilla.settlement.events.FixingEvent;
import com.lucilla.settlement.ledger.LedgerService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * The pure half of the series: given the on-ledger fixings, the fallback events and the
 * seed mark, produce the published rows newest first. No ledger, no clock — testable.
 *
 * <p>Rules:
 * <ul>
 *   <li>a {@code NavFixing} is tier 1; {@code k} is the number of real signatures on it,
 *       {@code n} the committee size; {@code restated} when it corrects another;
 *       {@code superseded} when another corrects it;</li>
 *   <li>a {@code fixing.fallback} event is a tier 3/4 row; a {@code fixing.missed} event
 *       is a tier 5 row with no price;</li>
 *   <li>the seed mark, when given, is a tier 0 row at the bottom.</li>
 * </ul>
 */
public final class SeriesDerivation {

    private SeriesDerivation() {}

    public static List<SeriesRow> derive(
            String instrumentId, String session,
            List<LedgerService.NavFixingView> fixings,
            List<FixingEvent> events,
            BigDecimal seedMark, Instant seedAsOf, String seedNote,
            ZoneId zone, Function<String, String> label, int committeeSize) {

        List<SeriesRow> rows = new ArrayList<>();
        Set<String> supersededCids = new HashSet<>();
        for (var f : fixings) {
            if (f.supersedes() != null && !f.supersedes().isBlank()) supersededCids.add(f.supersedes());
        }
        for (var f : fixings) {
            if (!f.instrumentId().equalsIgnoreCase(instrumentId)) continue;
            if (session != null && !f.session().equalsIgnoreCase(session)) continue;
            List<String> signers = f.attestors().stream().map(label).toList();
            int n = Math.max(committeeSize, Math.max(signers.size(), (int) f.threshold()));
            rows.add(new SeriesRow(
                    dateOf(f.accrualFrom(), zone), f.accrualFrom().toString(),
                    f.price(), f.referencePrice(), f.wrapperFactor(),
                    1, signers.size(), n, signers, f.contractId(), f.isRestatement(),
                    SeriesRow.labelFor(1), f.session(),
                    supersededCids.contains(f.contractId()) ? Boolean.TRUE : null,
                    f.isRestatement() ? "restatement: " + f.restatementReason() : null));
        }
        for (FixingEvent e : events) {
            if (e.instrument() == null || !e.instrument().equalsIgnoreCase(instrumentId)) continue;
            boolean fallback = FixingEvent.Kinds.FIXING_FALLBACK.equals(e.kind());
            boolean missed = FixingEvent.Kinds.FIXING_MISSED.equals(e.kind());
            if (!fallback && !missed) continue;
            int tier = e.tier() == null ? (missed ? 5 : 4) : e.tier();
            Object ref = e.details() == null ? null : e.details().get("referencePrice");
            Object fac = e.details() == null ? null : e.details().get("wrapperFactor");
            rows.add(new SeriesRow(
                    dateOf(e.instant(), zone), e.ts(),
                    missed ? null : e.price(),
                    ref == null ? null : new BigDecimal(ref.toString()),
                    fac == null ? null : new BigDecimal(fac.toString()),
                    tier, 0, committeeSize, List.of(), e.ledgerCid(), false,
                    SeriesRow.labelFor(tier), session, null, e.reason()));
        }
        if (seedMark != null && seedAsOf != null) {
            rows.add(new SeriesRow(dateOf(seedAsOf, zone), seedAsOf.toString(), seedMark, null, null,
                    0, 0, committeeSize, List.of(), null, false, SeriesRow.labelFor(0), session, null,
                    seedNote));
        }
        rows.sort(Comparator.comparing(SeriesRow::instant).reversed());
        return rows;
    }

    static String dateOf(Instant at, ZoneId zone) {
        return at.atZone(zone).toLocalDate().toString();
    }
}
