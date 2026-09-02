package com.lucilla.settlement.webhooks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lucilla.settlement.auth.Role;
import com.lucilla.settlement.auth.UserRecord;
import com.lucilla.settlement.auth.UserStore;
import com.lucilla.settlement.events.EventStore;
import com.lucilla.settlement.events.FixingEvent;
import com.lucilla.settlement.ledger.SignerProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Outbound signer notifications — docs/PRODUCT-PLAN.md §5 "Webhook to signers".
 *
 * <p>For every signer whose seat covers the instrument and who has a webhook URL:
 * {@code POST {webhookUrl}} with
 * {@code { type, instrument, proposalCid, price, referencePrice?, wrapperFactor?, conditions, deadline }}
 * — {@code conditions} being the named conditions for THAT signer's seat — signed with
 * {@link WebhookSigner}. Asynchronous, three attempts with backoff, and every outcome is
 * an event ({@code webhook.sent} / {@code webhook.failed}) so the audit log says who was
 * told and whether it got through.
 */
@Component
public class WebhookDispatcher {

    private static final Logger log = LoggerFactory.getLogger(WebhookDispatcher.class);
    static final int ATTEMPTS = 3;
    static final long[] BACKOFF_MS = {0, 1_000, 3_000};

    public static final String PROPOSAL_CREATED = "proposal.created";
    public static final String PROPOSAL_RESTRUCK = "proposal.restruck";
    public static final String FIXING_FINALIZED = "fixing.finalized";
    public static final String FIXING_MISSED = "fixing.missed";

    private final UserStore users;
    private final EventStore events;
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "webhooks");
        t.setDaemon(true);
        return t;
    });

    public WebhookDispatcher(UserStore users, EventStore events) {
        this.users = users;
        this.events = events;
    }

    /** The payload for one seat. Public so the body shape is testable. */
    public static Map<String, Object> payload(String type, String instrument, String proposalCid,
            BigDecimal price, BigDecimal referencePrice, BigDecimal wrapperFactor,
            List<String> conditions, Instant deadline) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", type);
        body.put("instrument", instrument);
        body.put("proposalCid", proposalCid);
        body.put("price", price);
        if (referencePrice != null) body.put("referencePrice", referencePrice);
        if (wrapperFactor != null) body.put("wrapperFactor", wrapperFactor);
        body.put("conditions", conditions);
        body.put("deadline", deadline == null ? null : deadline.toString());
        return body;
    }

    /** Fan out to every subscribed seat for the instrument. Returns how many were queued. */
    public int dispatch(String type, String instrument, String proposalCid, BigDecimal price,
            BigDecimal referencePrice, BigDecimal wrapperFactor, Instant deadline) {
        int queued = 0;
        for (UserRecord u : users.all()) {
            if (u.roleEnum() != Role.SIGNER) continue;
            if (!covers(u, instrument)) continue;
            String url = u.getSettings() == null ? null : u.getSettings().getWebhookUrl();
            if (url == null || url.isBlank()) continue;
            List<String> conditions = conditionsFor(u.getSeat());
            Map<String, Object> body = payload(type, instrument, proposalCid, price, referencePrice,
                    wrapperFactor, conditions, deadline);
            String secret = u.getSettings().getWebhookSecret();
            final String target = url.trim();
            final String who = u.getEmail() == null ? u.getUid() : u.getEmail();
            final String seat = u.getSeat();
            executor.submit(() -> deliver(type, instrument, proposalCid, target, secret, body, who, seat));
            queued++;
        }
        return queued;
    }

    static boolean covers(UserRecord u, String instrument) {
        List<String> ids = u.getInstruments();
        if (ids == null || ids.isEmpty()) return false;
        return ids.stream().anyMatch(i -> i != null && i.equalsIgnoreCase(instrument));
    }

    static List<String> conditionsFor(String seat) {
        SignerProtocol.Role r = SignerProtocol.role(seat);
        return r == null ? List.of() : r.conditions().stream().map(SignerProtocol.Condition::name).toList();
    }

    private void deliver(String type, String instrument, String proposalCid, String url, String secret,
            Map<String, Object> body, String who, String seat) {
        byte[] bytes;
        try {
            bytes = json.writeValueAsBytes(body);
        } catch (Exception e) {
            record(FixingEvent.Kinds.WEBHOOK_FAILED, type, instrument, proposalCid, who, seat, url, 0,
                    "could not serialise payload: " + e.getMessage());
            return;
        }
        String sig = WebhookSigner.signature(secret, bytes);
        String lastError = null;
        for (int attempt = 1; attempt <= ATTEMPTS; attempt++) {
            try {
                if (BACKOFF_MS[attempt - 1] > 0) Thread.sleep(BACKOFF_MS[attempt - 1]);
                HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                        .timeout(Duration.ofSeconds(10))
                        .header("Content-Type", "application/json")
                        .header("User-Agent", "CrossDesk-Webhooks/1")
                        .header(WebhookSigner.HEADER, sig)
                        .header("X-CrossDesk-Event", type)
                        .POST(HttpRequest.BodyPublishers.ofByteArray(bytes))
                        .build();
                HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                    record(FixingEvent.Kinds.WEBHOOK_SENT, type, instrument, proposalCid, who, seat, url,
                            attempt, "HTTP " + resp.statusCode());
                    return;
                }
                lastError = "HTTP " + resp.statusCode();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                lastError = "interrupted";
                break;
            } catch (Exception e) {
                lastError = e.getClass().getSimpleName() + ": " + e.getMessage();
            }
            log.info("webhook {} to {} attempt {}/{} failed: {}", type, url, attempt, ATTEMPTS, lastError);
        }
        record(FixingEvent.Kinds.WEBHOOK_FAILED, type, instrument, proposalCid, who, seat, url, ATTEMPTS,
                lastError);
    }

    private void record(String kind, String type, String instrument, String proposalCid, String who,
            String seat, String url, int attempts, String outcome) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("webhookType", type);
        d.put("url", url);
        d.put("attempts", attempts);
        d.put("outcome", outcome);
        try {
            events.append(FixingEvent.of(kind, instrument, proposalCid, null, who, seat, null,
                    outcome, null, null, null, d));
        } catch (RuntimeException e) {
            log.warn("could not record {} for {}: {}", kind, who, e.toString());
        }
    }
}
