package com.lucilla.settlement.web;

import com.lucilla.settlement.config.DeskDiagnostics;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@code GET /api/diag} — everything you would otherwise have to read logs for.
 *
 * <p>Sibling of {@code /api/health}, which stays a pure config echo that never touches
 * the ledger (a Kubernetes liveness probe must not fail because a participant is
 * restarting). This one DOES touch the ledger, deliberately: it answers "is the desk
 * actually able to work right now, and if not, which of the five usual things is
 * wrong?" — ledger connectivity, the package id the backend is bound to (and whether
 * the participant holds it), the resolved party ids, the token's expiry, and when the
 * last successful ledger call was.
 *
 * <p>Written for the worst moment to be reading logs: something fails live, and the
 * answer needs to be one HTTP call away rather than a grep. It always returns 200 with
 * a body — a diagnostic that can itself fail with a 500 is not a diagnostic — so read
 * {@code status} ({@code UP} / {@code DEGRADED}), not the HTTP code.
 *
 * <p><b>No secret is reachable from here.</b> The token section reports presence and
 * expiry only; {@code TokenInfo} has no accessor that returns the token.
 */
@RestController
public class DiagnosticsController {

    private final DeskDiagnostics diagnostics;

    public DiagnosticsController(DeskDiagnostics diagnostics) {
        this.diagnostics = diagnostics;
    }

    @GetMapping("/api/diag")
    public Map<String, Object> diag() {
        try {
            return diagnostics.snapshot();
        } catch (RuntimeException e) {
            // Belt and braces: snapshot() try/catches every probe individually, so this
            // can only fire on something genuinely unexpected — and even then the caller
            // gets a readable body rather than a stack trace.
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("status", "DEGRADED");
            out.put("checkedAt", Instant.now().toString());
            out.put("error", "the diagnostic itself failed: "
                    + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
            return out;
        }
    }
}
