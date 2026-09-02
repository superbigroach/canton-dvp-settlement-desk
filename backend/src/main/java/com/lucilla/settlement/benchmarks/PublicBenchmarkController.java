package com.lucilla.settlement.benchmarks;

import com.lucilla.settlement.ledger.SignerProtocol;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The public benchmark API — docs/PRODUCT-PLAN.md §5 "Public (no auth)". Read by the
 * marketing site's benchmark pages and by anyone who wants the number; referencing it in
 * a contract is a licence matter, reading it is not (FIXING_METHODOLOGY.md §10).
 */
@RestController
public class PublicBenchmarkController {

    public static final String METHODOLOGY_VERSION = "0.1";

    private final BenchmarkCatalog catalog;
    private final SeriesService series;

    public PublicBenchmarkController(BenchmarkCatalog catalog, SeriesService series) {
        this.catalog = catalog;
        this.series = series;
    }

    @GetMapping("/api/benchmarks")
    public List<Map<String, Object>> benchmarks() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (var p : catalog.products()) {
            out.add(view(p));
        }
        return out;
    }

    @GetMapping("/api/benchmarks/{id}")
    public ResponseEntity<Map<String, Object>> benchmark(@PathVariable String id) {
        return catalog.product(id)
                .map(p -> ResponseEntity.ok(view(p)))
                .orElseThrow(() -> new NoSuchBenchmark(id));
    }

    /** Newest first; {@code from}/{@code to} are ISO-8601 instants or yyyy-MM-dd dates. */
    @GetMapping("/api/series/{id}")
    public List<SeriesRow> series(@PathVariable String id,
                                  @RequestParam(required = false) String from,
                                  @RequestParam(required = false) String to,
                                  @RequestParam(required = false) Integer limit) {
        catalog.product(id).orElseThrow(() -> new NoSuchBenchmark(id));
        return filter(series.series(id), from, to, limit);
    }

    @GetMapping(value = "/api/series/{id}.csv", produces = "text/csv")
    public ResponseEntity<String> seriesCsv(@PathVariable String id,
                                            @RequestParam(required = false) String from,
                                            @RequestParam(required = false) String to,
                                            @RequestParam(required = false) Integer limit) {
        catalog.product(id).orElseThrow(() -> new NoSuchBenchmark(id));
        List<SeriesRow> rows = filter(series.series(id), from, to, limit);
        StringBuilder sb = new StringBuilder(
                "date,asOf,price,referencePrice,wrapperFactor,tier,tierLabel,k,n,signers,fixingCid,restated\n");
        for (SeriesRow r : rows) {
            sb.append(r.date()).append(',').append(r.asOf()).append(',')
                    .append(plain(r.price())).append(',').append(plain(r.referencePrice())).append(',')
                    .append(plain(r.wrapperFactor())).append(',').append(r.tier()).append(',')
                    .append(r.tierLabel()).append(',').append(r.k()).append(',').append(r.n()).append(',')
                    .append('"').append(String.join(";", r.signers())).append('"').append(',')
                    .append(r.fixingCid() == null ? "" : r.fixingCid()).append(',')
                    .append(r.restated()).append('\n');
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header("Content-Disposition", "attachment; filename=\"" + id + "-series.csv\"")
                .body(sb.toString());
    }

    @GetMapping("/api/methodology")
    public Map<String, Object> methodology() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("version", METHODOLOGY_VERSION);
        out.put("url", "/methodology");
        out.put("signerProtocolVersion", SignerProtocol.VERSION);
        out.put("documents", List.of(
                Map.of("id", "fixing-methodology", "title", "CrossDesk Fixing Methodology",
                        "version", METHODOLOGY_VERSION, "path", "docs/FIXING_METHODOLOGY.md"),
                Map.of("id", "signer-protocol", "title", "CrossDesk Signer Protocol",
                        "version", SignerProtocol.VERSION, "path", "docs/SIGNER_PROTOCOL.md")));
        out.put("tiers", Map.of(
                "1", "attested by K of N on-ledger (NavFixing)",
                "2", "alternate seats (not configured)",
                "3", "benchmark print × last attested factor, automatic",
                "4", "prior fixing carried forward, flagged",
                "5", "missed — published as a gap"));
        return out;
    }

    // ---- helpers --------------------------------------------------------------

    private Map<String, Object> view(BenchmarkCatalog.Product p) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", p.id());
        out.put("name", p.name());
        out.put("kind", p.kind());
        out.put("publishTime", p.publishTime());
        out.put("timezone", p.timezone());
        out.put("description", p.description());
        Map<String, Object> last = null;
        Map<String, Object> latest = null;
        try {
            List<SeriesRow> rows = series.series(p.id());
            // `last` is the newest PUBLISHED VALUE (a tier-5 gap has no price to show);
            // `latest` is the newest row of any kind, so a missed strike is still visible.
            last = rows.stream().filter(r -> r.price() != null).findFirst().map(PublicBenchmarkController::lastView).orElse(null);
            latest = rows.stream().findFirst().map(PublicBenchmarkController::lastView).orElse(null);
        } catch (RuntimeException e) {
            out.put("error", "the ledger did not answer: " + e.getMessage());
        }
        out.put("last", last);
        out.put("latest", latest);
        List<Map<String, String>> refs = new ArrayList<>();
        for (var f : series.referencing(p.id())) {
            refs.add(Map.of("id", f.id(), "name", f.name()));
        }
        out.put("referencing", refs);
        return out;
    }

    static Map<String, Object> lastView(SeriesRow r) {
        Map<String, Object> last = new LinkedHashMap<>();
        last.put("price", r.price());
        last.put("asOf", r.asOf());
        last.put("tier", r.tier());
        last.put("tierLabel", r.tierLabel());
        last.put("k", r.k());
        last.put("n", r.n());
        last.put("signers", r.signers());
        last.put("ageSeconds", Math.max(0, Duration.between(r.instant(), Instant.now()).getSeconds()));
        last.put("fixingCid", r.fixingCid());
        if (r.referencePrice() != null) last.put("referencePrice", r.referencePrice());
        if (r.wrapperFactor() != null) last.put("wrapperFactor", r.wrapperFactor());
        return last;
    }

    static List<SeriesRow> filter(List<SeriesRow> rows, String from, String to, Integer limit) {
        Instant f = parse(from, false);
        Instant t = parse(to, true);
        List<SeriesRow> out = rows.stream()
                .filter(r -> f == null || !r.instant().isBefore(f))
                .filter(r -> t == null || !r.instant().isAfter(t))
                .toList();
        if (limit != null && limit > 0 && out.size() > limit) {
            out = out.subList(0, limit);
        }
        return out;
    }

    private static Instant parse(String raw, boolean endOfDay) {
        if (raw == null || raw.isBlank()) return null;
        String s = raw.trim();
        try {
            return Instant.parse(s);
        } catch (RuntimeException notInstant) {
            try {
                var d = java.time.LocalDate.parse(s);
                return endOfDay ? d.plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().minusMillis(1)
                        : d.atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
            } catch (RuntimeException notDate) {
                throw new IllegalArgumentException("not an ISO-8601 instant or yyyy-MM-dd date: " + raw);
            }
        }
    }

    private static String plain(java.math.BigDecimal d) {
        return d == null ? "" : d.stripTrailingZeros().toPlainString();
    }

    /** 404 with the id, rather than the generic conflict a NoSuchElementException maps to. */
    public static class NoSuchBenchmark extends com.lucilla.settlement.web.ApiExceptionHandler.NotFound {
        public NoSuchBenchmark(String id) {
            super("no benchmark '" + id + "'");
        }
    }
}
