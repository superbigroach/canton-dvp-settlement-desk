package com.lucilla.settlement.portal;

import com.lucilla.settlement.auth.AuthProperties;
import com.lucilla.settlement.auth.CurrentUser;
import com.lucilla.settlement.auth.Principal;
import com.lucilla.settlement.auth.Role;
import com.lucilla.settlement.auth.UserRecord;
import com.lucilla.settlement.auth.UserStore;
import com.lucilla.settlement.benchmarks.BenchmarkCatalog;
import com.lucilla.settlement.events.EventStore;
import com.lucilla.settlement.events.EventsCsv;
import com.lucilla.settlement.events.FixingEvent;
import com.lucilla.settlement.ledger.LedgerService;
import com.lucilla.settlement.scheduler.ScheduleStore;
import com.lucilla.settlement.scheduler.StrikeSchedule;
import com.lucilla.settlement.scheduler.StrikeService;
import com.lucilla.settlement.web.ApiExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code /api/admin/*} — the admin console (docs/PRODUCT-PLAN.md §5): schedule,
 * strike-now, committees roster, users, events export. The read-only halves are reused
 * by {@link AuditController}.
 */
@RestController
public class AdminController {

    private final ScheduleStore schedules;
    private final StrikeService strikes;
    private final LedgerService ledger;
    private final UserStore users;
    private final EventStore events;
    private final BenchmarkCatalog catalog;
    private final AuthProperties props;

    public AdminController(ScheduleStore schedules, StrikeService strikes, LedgerService ledger,
            UserStore users, EventStore events, BenchmarkCatalog catalog, AuthProperties props) {
        this.schedules = schedules;
        this.strikes = strikes;
        this.ledger = ledger;
        this.users = users;
        this.events = events;
        this.catalog = catalog;
        this.props = props;
    }

    // ---- schedule ---------------------------------------------------------------

    @GetMapping("/api/admin/schedule")
    public List<StrikeSchedule> schedule() {
        return schedules.all();
    }

    /** Replace the whole schedule. Body: the same list shape {@code GET} returns. */
    @PutMapping("/api/admin/schedule")
    public List<StrikeSchedule> putSchedule(HttpServletRequest req, @RequestBody List<StrikeSchedule> rows) {
        Principal me = CurrentUser.require(req);
        List<StrikeSchedule> saved = schedules.replace(rows);
        events.append(FixingEvent.of("schedule.updated", null, null, null, actor(me), "admin", null,
                "strike schedule replaced (" + saved.size() + " instrument(s))", null, null, null,
                Map.of("instruments", saved.stream().map(StrikeSchedule::getInstrumentId).toList())));
        return saved;
    }

    /** Where every instrument's strike stands right now — the "fallback status per instrument". */
    @GetMapping("/api/admin/schedule/status")
    public List<Map<String, Object>> scheduleStatus() {
        return strikes.statuses(Instant.now());
    }

    /** Run the propose step for one instrument now. */
    @PostMapping("/api/admin/strike/{id}")
    public ResponseEntity<Map<String, Object>> strikeNow(HttpServletRequest req, @PathVariable String id) {
        Principal me = CurrentUser.require(req);
        StrikeSchedule s = schedules.byInstrument(id).orElseGet(() -> {
            // An instrument off the schedule can still be struck by hand if the desk lists it.
            var p = catalog.product(id).orElseThrow(() -> new ApiExceptionHandler.NotFound("no benchmark '" + id + "'"));
            StrikeSchedule ad = new StrikeSchedule();
            ad.setInstrumentId(p.id());
            ad.setKind("nav".equals(p.kind()) ? "fund" : "wrapped");
            return ad;
        });
        Map<String, Object> out = strikes.propose(s, actor(me));
        return ResponseEntity.status(HttpStatus.CREATED).body(out);
    }

    // ---- committees -------------------------------------------------------------

    @GetMapping("/api/admin/committees")
    public List<Map<String, Object>> committees() {
        String operator = ledger.resolveParty(props.getOperatorParty());
        List<LedgerService.CommitteeView> all;
        try {
            all = ledger.committeesVisibleTo(ledger.resolveParty("Auditor"));
        } catch (RuntimeException e) {
            all = ledger.committeesVisibleTo(operator);
        }
        List<Map<String, Object>> committees = new ArrayList<>();
        for (var c : all) {
            committees.add(committeeView(c));
        }
        // The committee the operator proposes into is the one an instrument is struck
        // under (committees carry no instrument field on-ledger); others are listed too.
        String opLabel = LedgerService.labelOf(operator);
        Map<String, Object> primary = committees.stream()
                .filter(c -> ((List<?>) c.get("seats")).stream()
                        .anyMatch(s -> opLabel.equals(((Map<?, ?>) s).get("party"))))
                .findFirst().orElse(committees.isEmpty() ? null : committees.get(0));
        List<Map<String, Object>> out = new ArrayList<>();
        for (var p : catalog.products()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("instrument", p.id());
            row.put("name", p.name());
            // frontend/src/desk/types.ts Committee: { instrument, k, n, seats[] }
            row.put("k", primary == null ? 0 : primary.get("threshold"));
            row.put("n", primary == null ? 0 : primary.get("size"));
            row.put("seats", primary == null ? List.of() : primary.get("seats"));
            row.put("committeeCid", primary == null ? null : primary.get("contractId"));
            row.put("committees", committees);
            out.add(row);
        }
        return out;
    }

    /**
     * The configured roster label for a full party id — "Bank" for
     * {@code bank-crossdesk::1220…} — matching on the full id first and on the hint
     * second, so it works whether the committee stored an id or a label.
     *
     * <p>Empty when the party is not one this desk is configured with, which is the case
     * a real counterparty seat will eventually be: then the caller keeps the hint, which
     * is still the most informative thing available.
     */
    private java.util.Optional<String> rosterLabelOf(String party) {
        List<LedgerService.PartyView> roster;
        try {
            roster = ledger.listParties();
        } catch (RuntimeException e) {
            return java.util.Optional.empty();
        }
        String hint = LedgerService.labelOf(party);
        return roster.stream()
                .filter(p -> p.party().equals(party) || LedgerService.labelOf(p.party()).equals(hint))
                .map(LedgerService.PartyView::label)
                .findFirst();
    }

    private Map<String, Object> committeeView(LedgerService.CommitteeView c) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("contractId", c.contractId());
        out.put("label", c.label());
        out.put("threshold", c.threshold());
        out.put("size", c.members().size());
        out.put("admin", LedgerService.labelOf(c.admin()));
        out.put("auditor", LedgerService.labelOf(c.auditor()));
        List<Map<String, Object>> seats = new ArrayList<>();
        for (String m : c.members()) {
            String label = LedgerService.labelOf(m);
            // THE NAME THE LEDGER USES IS NOT THE NAME THE ROSTER USES, and this page
            // silently compared them. `labelOf` returns the party-id hint —
            // `bank-crossdesk::1220…` -> "bank-crossdesk" — while users.yml stores the
            // configured label, "Bank". `users.byParty("bank-crossdesk")` therefore never
            // matched anything, so every seat on the Committees page read "none —
            // automated only or unassigned" and the SEAT column was blank, on a desk whose
            // roster had all five seats filled. Resolve through the configured roster
            // first; fall back to the hint when a party is not one of ours.
            String rosterLabel = rosterLabelOf(m).orElse(label);
            Map<String, Object> seat = new LinkedHashMap<>();
            seat.put("party", label);
            List<UserRecord> us = users.byParty(rosterLabel);
            seat.put("seat", us.stream().map(UserRecord::getSeat).filter(s -> s != null && !s.isBlank())
                    .findFirst().orElse(null));
            // types.ts CommitteeSeat.users: string[] (e-mails); the richer rows ride alongside.
            seat.put("users", us.stream().filter(u -> u.roleEnum() == Role.SIGNER)
                    .map(UserRecord::getEmail).filter(e -> e != null).toList());
            seat.put("userDetails", us.stream().filter(u -> u.roleEnum() == Role.SIGNER).map(u -> Map.of(
                    "email", u.getEmail() == null ? "" : u.getEmail(),
                    "displayName", u.getDisplayName() == null ? "" : u.getDisplayName(),
                    "webhook", u.getSettings() != null && u.getSettings().getWebhookUrl() != null,
                    "apiKey", u.getApiKeyHash() != null)).toList());
            seat.put("lastAction", events.all().stream()
                    .filter(e -> e.actor() != null && (e.actor().endsWith("(" + label + ")") || e.actor().equals(label)))
                    .max(Comparator.comparingLong(FixingEvent::id))
                    .map(e -> Map.of("kind", e.kind(), "at", e.ts(), "ts", e.ts(), "instrument",
                            e.instrument() == null ? "" : e.instrument()))
                    .orElse(null));
            seats.add(seat);
        }
        out.put("seats", seats);
        return out;
    }

    // ---- users ------------------------------------------------------------------

    @GetMapping("/api/admin/users")
    public List<Map<String, Object>> listUsers() {
        return users.all().stream().map(AdminController::userView).toList();
    }

    @GetMapping("/api/admin/users/{uid}")
    public Map<String, Object> getUser(@PathVariable String uid) {
        return users.byUid(uid).map(AdminController::userView)
                .orElseThrow(() -> new ApiExceptionHandler.NotFound("no user " + uid));
    }

    @PostMapping("/api/admin/users")
    public ResponseEntity<Map<String, Object>> createUser(HttpServletRequest req, @RequestBody UserRecord body) {
        Principal me = CurrentUser.require(req);
        validate(body);
        if (body.getEmail() != null && users.byEmail(body.getEmail()).isPresent()) {
            throw new IllegalStateException("a user with e-mail " + body.getEmail() + " already exists");
        }
        body.setApiKeyHash(null);
        body.setApiKeyCreatedAt(null);
        UserRecord saved = users.save(body);
        audit(me, "user.created", saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(userView(saved));
    }

    @PutMapping("/api/admin/users/{uid}")
    public Map<String, Object> updateUser(HttpServletRequest req, @PathVariable String uid, @RequestBody UserRecord body) {
        Principal me = CurrentUser.require(req);
        UserRecord existing = users.byUid(uid).orElseThrow(() -> new ApiExceptionHandler.NotFound("no user " + uid));
        if (body.getEmail() != null) existing.setEmail(body.getEmail());
        if (body.getRole() != null) existing.setRole(body.getRole());
        if (body.getParty() != null) existing.setParty(body.getParty().isBlank() ? null : body.getParty());
        if (body.getSeat() != null) existing.setSeat(body.getSeat().isBlank() ? null : body.getSeat());
        if (body.getInstruments() != null && !body.getInstruments().isEmpty()) existing.setInstruments(body.getInstruments());
        if (body.getOrg() != null) existing.setOrg(body.getOrg());
        if (body.getDisplayName() != null) existing.setDisplayName(body.getDisplayName());
        validate(existing);
        UserRecord saved = users.save(existing);
        audit(me, "user.updated", saved);
        return userView(saved);
    }

    @DeleteMapping("/api/admin/users/{uid}")
    public Map<String, Object> deleteUser(HttpServletRequest req, @PathVariable String uid) {
        Principal me = CurrentUser.require(req);
        if (uid.equals(me.uid())) {
            throw new IllegalArgumentException("you cannot delete your own user");
        }
        boolean removed = users.delete(uid);
        if (!removed) throw new ApiExceptionHandler.NotFound("no user " + uid);
        events.append(FixingEvent.of("user.deleted", null, null, null, actor(me), "admin", null, uid,
                null, null, null, Map.of()));
        return Map.of("deleted", true, "uid", uid);
    }

    private static void validate(UserRecord u) {
        if (u.getEmail() == null || !u.getEmail().contains("@")) {
            throw new IllegalArgumentException("email is required");
        }
        Role r = Role.parse(u.getRole());
        if (r == Role.SIGNER) {
            if (u.getSeat() == null || com.lucilla.settlement.ledger.SignerProtocol.role(u.getSeat()) == null) {
                throw new IllegalArgumentException("a signer needs a seat: issuer | lender | venue | operator");
            }
            if (u.getParty() == null || u.getParty().isBlank()) {
                throw new IllegalArgumentException("a signer needs a ledger party");
            }
        }
        if ((r == Role.AP || r == Role.FUND_ADMIN) && (u.getParty() == null || u.getParty().isBlank())) {
            throw new IllegalArgumentException("an " + r.wire() + " needs a ledger party");
        }
    }

    /** Never the key hash or the webhook secret. */
    static Map<String, Object> userView(UserRecord u) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("uid", u.getUid());
        out.put("email", u.getEmail());
        out.put("role", u.roleEnum().wire());
        out.put("party", u.getParty());
        out.put("seat", u.getSeat());
        out.put("instruments", u.getInstruments());
        out.put("org", u.getOrg());
        out.put("displayName", u.getDisplayName());
        out.put("apiKeySet", u.getApiKeyHash() != null);
        out.put("webhookUrl", u.getSettings() == null ? null : u.getSettings().getWebhookUrl());
        return out;
    }

    private void audit(Principal me, String kind, UserRecord u) {
        events.append(FixingEvent.of(kind, null, null, null, actor(me), "admin", null,
                u.getEmail() + " → " + u.roleEnum().wire() + (u.getParty() == null ? "" : " / " + u.getParty()),
                null, null, null, Map.of("uid", u.getUid())));
    }

    // ---- events -----------------------------------------------------------------

    @GetMapping("/api/admin/events")
    public List<FixingEvent> events(@RequestParam(required = false) String instrument,
                                    @RequestParam(required = false) String from,
                                    @RequestParam(required = false) String to,
                                    @RequestParam(required = false) String kind) {
        return filtered(instrument, from, to, kind);
    }

    @GetMapping(value = "/api/admin/events.csv", produces = "text/csv")
    public ResponseEntity<String> eventsCsv(@RequestParam(required = false) String instrument,
                                            @RequestParam(required = false) String from,
                                            @RequestParam(required = false) String to,
                                            @RequestParam(required = false) String kind) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header("Content-Disposition", "attachment; filename=\"fixing_events.csv\"")
                .body(EventsCsv.render(filtered(instrument, from, to, kind)));
    }

    List<FixingEvent> filtered(String instrument, String from, String to, String kind) {
        List<FixingEvent> rows = new ArrayList<>(events.query(instrument, parse(from, false), parse(to, true)));
        if (kind != null && !kind.isBlank()) {
            rows = rows.stream().filter(e -> e.kind().startsWith(kind.trim())).toList();
        }
        List<FixingEvent> out = new ArrayList<>(rows);
        out.sort(Comparator.comparingLong(FixingEvent::id).reversed());
        return out;
    }

    private static Instant parse(String raw, boolean endOfDay) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Instant.parse(raw.trim());
        } catch (RuntimeException notInstant) {
            try {
                var d = java.time.LocalDate.parse(raw.trim());
                return endOfDay ? d.plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().minusMillis(1)
                        : d.atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
            } catch (RuntimeException notDate) {
                throw new IllegalArgumentException("not an ISO-8601 instant or yyyy-MM-dd date: " + raw);
            }
        }
    }

    private static String actor(Principal me) {
        return me.email() == null ? me.uid() : me.email();
    }
}
