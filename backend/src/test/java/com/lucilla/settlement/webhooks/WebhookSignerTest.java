package com.lucilla.settlement.webhooks;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** {@code X-CrossDesk-Signature: sha256=HMAC(secret, body)} — docs/PRODUCT-PLAN.md §5. */
class WebhookSignerTest {

    /** Computed outside Java (Python hmac) so the test is not the code checking itself. */
    static final String VECTOR =
            "sha256=1f4f59fab232730af620923a1a68af8a78e8c5f648e2202c3457c3b718bc2dc9";

    @Test
    void matchesAnIndependentHmacSha256() {
        // python: hmac.new(b"whsec_test", b'{"type":"proposal.created"}', sha256).hexdigest()
        String actual = WebhookSigner.signature("whsec_test", "{\"type\":\"proposal.created\"}");
        assertEquals(VECTOR, actual);
        assertEquals(WebhookSigner.HEADER, "X-CrossDesk-Signature");
    }

    @Test
    void verifyRoundTripsAndRejectsTampering() {
        byte[] body = "{\"instrument\":\"CBTC\",\"price\":65000}".getBytes(StandardCharsets.UTF_8);
        String sig = WebhookSigner.signature("s3cret", body);
        assertTrue(WebhookSigner.verify("s3cret", body, sig));
        assertFalse(WebhookSigner.verify("other", body, sig));
        assertFalse(WebhookSigner.verify("s3cret", "{\"instrument\":\"CBTC\",\"price\":65001}".getBytes(StandardCharsets.UTF_8), sig));
        assertFalse(WebhookSigner.verify("s3cret", body, null));
    }

    @Test
    void payloadHasTheContractShape() throws Exception {
        Map<String, Object> body = WebhookDispatcher.payload("proposal.created", "CBTC", "cid#1",
                new BigDecimal("64870"), new BigDecimal("64870"), BigDecimal.ONE,
                List.of("attestor-quorum", "reserves-current"), Instant.parse("2026-09-02T15:30:00Z"));
        String json = new ObjectMapper().writeValueAsString(body);
        assertEquals("{\"type\":\"proposal.created\",\"instrument\":\"CBTC\",\"proposalCid\":\"cid#1\","
                + "\"price\":64870,\"referencePrice\":64870,\"wrapperFactor\":1,"
                + "\"conditions\":[\"attestor-quorum\",\"reserves-current\"],"
                + "\"deadline\":\"2026-09-02T15:30:00Z\"}", json);
        Map<String, Object> missed = WebhookDispatcher.payload("fixing.missed", "CBTC", null, null, null, null,
                List.of(), null);
        assertFalse(missed.containsKey("referencePrice"), "optional fields are absent, not null");
    }

    @Test
    void conditionsFollowTheSeat() {
        assertEquals(List.of("traded-range", "spread-within-tolerance", "sufficient-volume"),
                WebhookDispatcher.conditionsFor("venue"));
        assertTrue(WebhookDispatcher.conditionsFor("nobody").isEmpty());
    }
}
