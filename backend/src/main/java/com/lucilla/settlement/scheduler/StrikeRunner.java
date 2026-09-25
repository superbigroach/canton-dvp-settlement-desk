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
// OFF UNLESS ASKED FOR. This used to be matchIfMissing = true, so the runner started on any
// deployment that had not explicitly disabled it, and a single environment variable was all that
// stood between a fresh environment and a live strike. On 25 Sep 2026 the DevNet service spent 42
// minutes with it on: the price feed was unreachable, and had the window closed it would have
// published a gap row saying the committee failed to reach quorum when the truth was that the desk
// could not reach a price source. For something that publishes a public benchmark — and publishes
// a FAILURE when it cannot price — the safe default is silence. Turn it on deliberately, per
// environment, once the feed, the schedule and the notifications are known good.
@ConditionalOnProperty(prefix = "scheduler", name = "enabled", havingValue = "true", matchIfMissing = false)
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
