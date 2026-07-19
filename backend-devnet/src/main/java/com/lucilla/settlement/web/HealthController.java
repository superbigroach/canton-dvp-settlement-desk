package com.lucilla.settlement.web;

import com.lucilla.settlement.config.LedgerProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Liveness + configuration echo. Does NOT touch the ledger, so it answers even
 * when the participant is down — handy as a Kubernetes liveness probe and to see
 * which ledger endpoint the running jar is pointed at.
 */
@RestController
public class HealthController {

    private final LedgerProperties props;

    public HealthController(LedgerProperties props) {
        this.props = props;
    }

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "ledgerHost", props.getHost(),
                "ledgerPort", props.getPort(),
                "tls", props.isTls(),
                "auth", props.hasJwt() ? "jwt" : "none",
                "applicationId", props.getApplicationId());
    }
}
