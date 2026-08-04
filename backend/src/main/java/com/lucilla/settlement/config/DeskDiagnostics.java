package com.lucilla.settlement.config;

import com.daml.ledger.rxjava.DamlLedgerClient;
import com.lucilla.settlement.ledger.LedgerErrors;
import com.lucilla.settlement.ledger.LedgerService;
import com.lucilla.settlement.model.holding.Holding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * The one place that answers "what is this process actually pointed at?".
 *
 * <p>Backs two things: a BANNER logged once the desk is up, and {@code GET /api/diag},
 * which recomputes the same facts on demand. Both report five things, and each of them
 * is a failure this desk has actually had:
 *
 * <ol>
 *   <li><b>the ledger endpoint and TLS</b> — the plaintext-vs-TLS mismatch fails as an
 *       unhelpful transport error;</li>
 *   <li><b>the token's presence and expiry</b> (never its value) — an expired token
 *       fails as an opaque {@code UNAUTHENTICATED};</li>
 *   <li><b>the resolved party ids</b> — a Canton party id carries a per-allocation
 *       namespace suffix, so yesterday's id silently matches nothing today;</li>
 *   <li><b>THE PACKAGE ID THE BACKEND IS BOUND TO</b>, and whether the participant
 *       actually holds it. After a fresh DAR upload a backend built against the old
 *       package fails in thoroughly confusing ways — choices "not found", arguments
 *       "invalid" — and this single line makes it obvious at a glance;</li>
 *   <li><b>the last successful ledger call</b> — which separates "never worked" from
 *       "worked until 30 seconds ago", the first question worth asking mid-demo.</li>
 * </ol>
 *
 * <p><b>Nothing here can fail the application.</b> Every probe is individually
 * try/caught: the ledger connection is deliberately lazy so the desk boots before the
 * participant does, and a diagnostic that refused to answer while the ledger was down
 * would be useless exactly when it is needed.
 *
 * <p><b>Nothing here can leak a secret.</b> The token is passed to
 * {@link TokenInfo#of(String)} and never rendered, stored or returned.
 */
@Component
public class DeskDiagnostics {

    private static final Logger log = LoggerFactory.getLogger(DeskDiagnostics.class);

    private final LedgerProperties props;
    private final LedgerConnection connection;
    private final LedgerService ledger;

    public DeskDiagnostics(LedgerProperties props, LedgerConnection connection,
            LedgerService ledger) {
        this.props = props;
        this.connection = connection;
        this.ledger = ledger;
    }

    // -----------------------------------------------------------------------
    // The startup banner
    // -----------------------------------------------------------------------

    /**
     * Log the whole picture once the desk is serving. Runs AFTER the context is up
     * (not during bean creation) so that a slow or absent participant delays nothing
     * that matters and can never abort the boot.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void logStartupDiagnostics() {
        LedgerProbe probe = probeLedger();
        TokenInfo token = TokenInfo.of(connection.activeToken());

        log.info("======================================================================");
        log.info(" CANTON DvP SETTLEMENT DESK — STARTUP DIAGNOSTIC");
        log.info("   ledger endpoint : {}:{}", props.getHost(), props.getPort());
        log.info("   TLS             : {}", props.isTls()
                ? "ON" : "OFF (plaintext — local sandbox only)");
        log.info("   access token    : {}", token.summary());
        log.info("   application id  : {}  (stamped on every submission)",
                props.getApplicationId());
        log.info("   DAR package     : {} v{}", Holding.PACKAGE_NAME, Holding.PACKAGE_VERSION);
        log.info("   PACKAGE ID      : {}   <-- the package this backend is BOUND to",
                Holding.PACKAGE_ID);
        log.info("   template scope  : {}  (package-NAME reference; the participant "
                + "resolves the version)", Holding.TEMPLATE_ID.getPackageId());
        log.info("   ledger reachable: {}", probe.reachable ? "yes" : "NO — " + probe.error);
        if (probe.reachable) {
            log.info("   ledger end      : {}", probe.ledgerEnd);
            log.info("   package on node : {}", probe.packageStatus);
            if (!probe.packageKnownToNode) {
                log.error("   *** THE PARTICIPANT DOES NOT HOLD PACKAGE {} ***", Holding.PACKAGE_ID);
                log.error("   *** Every command will fail until the matching DAR is uploaded, "
                        + "or this backend is rebuilt from the DAR that IS uploaded. ***");
            }
        }
        List<String> parties = partyLines();
        if (parties.isEmpty()) {
            log.warn("   parties         : none resolved — the party picker will be empty");
        } else {
            log.info("   parties ({})     :", parties.size());
            for (String p : parties) {
                log.info("       {}", p);
            }
        }
        log.info("   diagnostics     : GET /api/diag  (same facts, on demand)");
        log.info("======================================================================");
    }

    // -----------------------------------------------------------------------
    // The on-demand snapshot (GET /api/diag)
    // -----------------------------------------------------------------------

    /** The same facts as the startup banner, recomputed now. Never throws. */
    public Map<String, Object> snapshot() {
        LedgerProbe probe = probeLedger();
        TokenInfo token = TokenInfo.of(connection.activeToken());

        Map<String, Object> ledgerSection = new LinkedHashMap<>();
        ledgerSection.put("host", props.getHost());
        ledgerSection.put("port", props.getPort());
        ledgerSection.put("tls", props.isTls());
        ledgerSection.put("applicationId", props.getApplicationId());
        ledgerSection.put("reachable", probe.reachable);
        ledgerSection.put("ledgerEnd", probe.ledgerEnd);
        ledgerSection.put("error", probe.error);
        ledgerSection.put("errorHint", probe.errorHint);

        // THE LINE THAT MATTERS AFTER A DAR UPLOAD.
        Map<String, Object> packageSection = new LinkedHashMap<>();
        packageSection.put("packageId", Holding.PACKAGE_ID);
        packageSection.put("packageName", Holding.PACKAGE_NAME);
        packageSection.put("packageVersion", String.valueOf(Holding.PACKAGE_VERSION));
        packageSection.put("templateIdScope", Holding.TEMPLATE_ID.getPackageId());
        packageSection.put("statusOnParticipant", probe.packageStatus);
        packageSection.put("knownToParticipant", probe.reachable ? probe.packageKnownToNode : null);

        // PRESENCE AND EXPIRY ONLY. There is no field here that could hold a token.
        Map<String, Object> tokenSection = new LinkedHashMap<>();
        tokenSection.put("present", token.present());
        tokenSection.put("expiryReadable", token.readable());
        tokenSection.put("expiresAt", token.expiresAt() == null
                ? null : token.expiresAt().toString());
        tokenSection.put("secondsRemaining", token.secondsRemaining());
        tokenSection.put("expired", token.present() ? token.expired() : null);
        tokenSection.put("summary", token.summary());

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("status", overallStatus(probe, token));
        out.put("checkedAt", Instant.now().toString());
        out.put("ledger", ledgerSection);
        out.put("package", packageSection);
        out.put("token", tokenSection);
        out.putAll(partiesSection());
        out.put("lastSuccessfulLedgerCall", lastCallSection());
        return out;
    }

    /** UP when the ledger answers and the token is not known-expired; DEGRADED otherwise. */
    private static String overallStatus(LedgerProbe probe, TokenInfo token) {
        if (!probe.reachable) {
            return "DEGRADED";
        }
        if (token.present() && token.expired()) {
            return "DEGRADED";
        }
        return probe.packageKnownToNode ? "UP" : "DEGRADED";
    }

    private Map<String, Object> partiesSection() {
        Map<String, Object> out = new LinkedHashMap<>();
        try {
            List<Map<String, Object>> parties = new ArrayList<>();
            for (LedgerService.PartyView p : ledger.listParties()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("label", p.label());
                row.put("party", p.party());   // the FULL id — what actAs claims must match
                row.put("isLocal", p.isLocal());
                parties.add(row);
            }
            out.put("parties", parties);
            out.put("partiesError", null);
        } catch (RuntimeException e) {
            LedgerErrors.Failure f = LedgerErrors.of(e);
            out.put("parties", List.of());
            out.put("partiesError", f.codeLabel() + ": " + LedgerErrors.truncate(f.description()));
        }
        return out;
    }

    private Map<String, Object> lastCallSection() {
        Map<String, Object> out = new LinkedHashMap<>();
        LedgerService.LastCall last = ledger.lastSuccessfulCall().orElse(null);
        if (last == null) {
            out.put("at", null);
            out.put("what", "none since this process started");
            out.put("commandId", null);
            out.put("updateId", null);
            return out;
        }
        out.put("at", last.at().toString());
        out.put("what", last.what());
        out.put("commandId", last.commandId());
        out.put("updateId", last.updateId());
        return out;
    }

    private List<String> partyLines() {
        try {
            List<String> lines = new ArrayList<>();
            for (LedgerService.PartyView p : ledger.listParties()) {
                lines.add(p.label() + " = " + p.party());
            }
            return lines;
        } catch (RuntimeException e) {
            LedgerErrors.Failure f = LedgerErrors.of(e);
            log.warn("   parties         : could not resolve ({}: {})",
                    f.codeLabel(), LedgerErrors.truncate(f.description()));
            return List.of();
        }
    }

    // -----------------------------------------------------------------------
    // The probe
    // -----------------------------------------------------------------------

    /** Result of one cheap round-trip to the participant. Never carries a token. */
    private static final class LedgerProbe {
        boolean reachable;
        Long ledgerEnd;
        String packageStatus = "unknown";
        boolean packageKnownToNode;
        String error;
        String errorHint;
    }

    /**
     * Ask the participant two questions: where is the ledger end (are we connected at
     * all?) and do you hold {@link Holding#PACKAGE_ID} (are we bound to the DAR you are
     * running?). Both are pure reads, so this is safe to call on every {@code /api/diag}.
     */
    private LedgerProbe probeLedger() {
        LedgerProbe probe = new LedgerProbe();
        long timeout = Math.max(2, props.getConnectTimeoutSeconds());
        try {
            DamlLedgerClient client = connection.get();
            probe.ledgerEnd = client.getStateClient().getLedgerEnd()
                    .timeout(timeout, TimeUnit.SECONDS)
                    .blockingGet();
            probe.reachable = true;
        } catch (RuntimeException e) {
            LedgerErrors.Failure f = LedgerErrors.of(e);
            probe.reachable = false;
            probe.error = f.codeLabel() + ": " + LedgerErrors.truncate(f.description());
            probe.errorHint = f.hint();
            return probe;
        }
        try {
            var status = connection.get().getPackageClient()
                    .getPackageStatus(Holding.PACKAGE_ID)
                    .timeout(timeout, TimeUnit.SECONDS)
                    .blockingGet();
            probe.packageStatus = String.valueOf(status.getPackageStatusValue());
            // Anything other than an explicit "unknown/unspecified" means the node has it.
            probe.packageKnownToNode = !probe.packageStatus.toUpperCase().contains("UNSPECIFIED")
                    && !probe.packageStatus.toUpperCase().contains("UNKNOWN");
        } catch (RuntimeException e) {
            LedgerErrors.Failure f = LedgerErrors.of(e);
            probe.packageStatus = "NOT AVAILABLE (" + f.codeLabel() + ")";
            probe.packageKnownToNode = false;
        }
        return probe;
    }
}
