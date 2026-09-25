package com.lucilla.settlement.portal;

import com.lucilla.settlement.events.EventStore;
import com.lucilla.settlement.events.FixingEvent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * "Request access" — the one way in for somebody who is not already a seat.
 *
 * <p>WHY IT EXISTS. The desk is invitation-only: a roster maps a named e-mail to a role,
 * and signing in with an address that is not on it shows an empty desk. That is correct —
 * a committee seat is issued to a person, never self-registered — but it left a visitor
 * who wanted to BE a seat with nowhere to go. The site offered "Sign in" and nothing else,
 * so the answer to "how do I use this" was: you cannot, and we will not tell you why.
 *
 * <p>The request is recorded in the same durable append-only log as every fixing event, so
 * it survives a restart and is visible in the audit export rather than living in a mailbox.
 *
 * <p><b>This creates nothing.</b> It does not add a user, issue a seat, or grant a role. It
 * writes a line saying somebody asked. Every account is still created by hand, which is the
 * property that makes a signature worth anything.
 */
@RestController
public class AccessRequestController {

    private static final Logger log = LoggerFactory.getLogger(AccessRequestController.class);

    /** Field caps: this endpoint is public, so nothing unbounded reaches the log. */
    public record AccessRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 160) @Email String email,
            @NotBlank @Size(max = 160) String firm,
            @Size(max = 60) String seat,
            @Size(max = 1200) String message) {
    }

    private final EventStore events;

    /**
     * Crude per-address throttle. A public write endpoint with no throttle is an invitation
     * to fill the audit log with junk, and the audit log is a record we make durability
     * claims about — so the cheapest possible guard is worth more here than elsewhere.
     */
    private final Map<String, Instant> lastSeen = new ConcurrentHashMap<>();
    private static final Duration COOLDOWN = Duration.ofMinutes(5);

    public AccessRequestController(EventStore events) {
        this.events = events;
    }

    @PostMapping("/api/access-request")
    public ResponseEntity<Map<String, Object>> request(HttpServletRequest http,
                                                       @Valid @RequestBody AccessRequest body) {
        String key = body.email().trim().toLowerCase();
        Instant prev = lastSeen.get(key);
        if (prev != null && Duration.between(prev, Instant.now()).compareTo(COOLDOWN) < 0) {
            // Deliberately a 200, not a 429: a caller learns nothing about who has already
            // written in, and an honest double-submit is not an error to the person sending it.
            return ResponseEntity.ok(Map.of(
                    "received", true,
                    "note", "We already have your request and will be in touch."));
        }
        lastSeen.put(key, Instant.now());

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("name", body.name().trim());
        detail.put("email", key);
        detail.put("firm", body.firm().trim());
        detail.put("seat", body.seat() == null ? "" : body.seat().trim());
        detail.put("message", body.message() == null ? "" : body.message().trim());

        events.append(FixingEvent.of("access.requested", null, null, null,
                key, "public", null, "access request from " + body.firm().trim(),
                null, null, null, detail));

        log.info("ACCESS REQUEST from {} <{}> seat={}", body.firm().trim(), key, detail.get("seat"));

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "received", true,
                "note", "Thanks — we read every one of these and will reply to " + key + "."));
    }
}
