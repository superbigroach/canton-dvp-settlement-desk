package com.lucilla.settlement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Canton DvP Settlement Desk — Spring Boot entry point.
 *
 * <p>A REST desk in front of a Daml/Canton ledger. It issues instruments and
 * holdings, runs atomic Delivery-versus-Payment (DvP), and operates a sealed
 * Market-on-Close auction — every action a real Ledger API command built from
 * the Daml Java bindings and submitted over gRPC. See {@code README.md} and
 * {@code deploy/GKE_RUNBOOK.md}.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class SettlementDeskApplication {
    public static void main(String[] args) {
        SpringApplication.run(SettlementDeskApplication.class, args);
    }
}
