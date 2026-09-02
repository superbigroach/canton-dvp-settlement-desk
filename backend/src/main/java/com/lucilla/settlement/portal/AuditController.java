package com.lucilla.settlement.portal;

import com.lucilla.settlement.benchmarks.SeriesRow;
import com.lucilla.settlement.benchmarks.SeriesService;
import com.lucilla.settlement.events.FixingEvent;
import com.lucilla.settlement.scheduler.StrikeSchedule;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * {@code /api/audit/*} — read-only mirrors of the admin routes for role {@code auditor}
 * (docs/PRODUCT-PLAN.md §5). Same data, same shapes, no writes.
 */
@RestController
public class AuditController {

    private final AdminController admin;
    private final SeriesService series;

    public AuditController(AdminController admin, SeriesService series) {
        this.admin = admin;
        this.series = series;
    }

    @GetMapping("/api/audit/events")
    public List<FixingEvent> events(@RequestParam(required = false) String instrument,
                                    @RequestParam(required = false) String from,
                                    @RequestParam(required = false) String to,
                                    @RequestParam(required = false) String kind) {
        return admin.events(instrument, from, to, kind);
    }

    @GetMapping(value = "/api/audit/events.csv", produces = "text/csv")
    public ResponseEntity<String> eventsCsv(@RequestParam(required = false) String instrument,
                                            @RequestParam(required = false) String from,
                                            @RequestParam(required = false) String to,
                                            @RequestParam(required = false) String kind) {
        return admin.eventsCsv(instrument, from, to, kind);
    }

    @GetMapping("/api/audit/committees")
    public List<Map<String, Object>> committees() {
        return admin.committees();
    }

    @GetMapping("/api/audit/schedule")
    public List<StrikeSchedule> schedule() {
        return admin.schedule();
    }

    @GetMapping("/api/audit/schedule/status")
    public List<Map<String, Object>> scheduleStatus() {
        return admin.scheduleStatus();
    }

    @GetMapping("/api/audit/series/{id}")
    public List<SeriesRow> series(@PathVariable String id) {
        return series.series(id);
    }

    @GetMapping("/api/audit/users")
    public List<Map<String, Object>> users() {
        return admin.listUsers();
    }
}
