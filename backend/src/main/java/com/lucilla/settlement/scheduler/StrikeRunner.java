package com.lucilla.settlement.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * The clock. One tick a minute (configurable) hands the instant to
 * {@link StrikeService#tick}; nothing here decides anything. Off with
 * {@code SCHEDULER_ENABLED=false}. Never throws — a runner that dies at 16:00 is the
 * one failure the schedule exists to prevent.
 */
@Component
@ConditionalOnProperty(prefix = "scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
public class StrikeRunner {

    private static final Logger log = LoggerFactory.getLogger(StrikeRunner.class);

    private final StrikeService strikes;

    public StrikeRunner(StrikeService strikes) {
        this.strikes = strikes;
        log.info("strike runner ON");
    }

    @Scheduled(fixedDelayString = "${scheduler.tick-ms:60000}", initialDelayString = "45000")
    public void tick() {
        try {
            strikes.tick(Instant.now());
        } catch (RuntimeException e) {
            log.warn("strike runner tick failed: {}", e.toString());
        }
    }
}
