package com.lucilla.settlement.config;

import com.lucilla.settlement.ledger.LedgerCommands;
import com.lucilla.settlement.ledger.LedgerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * THE DEMO FUND, seeded at start-up.
 *
 * <p>{@code Test:initialize} publishes the instruments and the holdings but no basket, so
 * the desk used to boot with a Fund / ETF builder that said "No baskets defined yet" and
 * an Asset picker with nothing to open on — every cold start of the hosted demo began
 * with someone re-typing LX1 by hand, and the sealed cross on the fund could not be shown
 * until they had. This defines it, through exactly the path {@code POST /api/basket}
 * takes, so the seed and a hand-defined basket are the same thing on the ledger: a
 * {@code BasketDefinition} plus its {@code Fund} instrument, in one transaction.
 *
 * <p>LX1 = 0.10 cETH + 0.01 CBTC per share, administered by Bank, with Alice and Bob as
 * the authorised participants — the same basket the Daml tests use, at the same weights,
 * so what the tests prove is what the demo shows.
 *
 * <p>Idempotent and non-fatal. A basket that already exists is left alone; a basket that
 * exists but was never listed (defined before the desk published funds) gets its share
 * record published; and nothing here can fail the boot — the ledger connection is lazy,
 * so a participant that is still coming up gets a handful of retries and then a warning,
 * never an aborted desk. Off with {@code DEMO_SEED_FUND=false}, which is what a real
 * participant wants: nobody seeds a fund into production from a start-up hook.
 */
@Component
public class DemoSeed {

    private static final Logger log = LoggerFactory.getLogger(DemoSeed.class);

    /** The basket the desk opens on. */
    public static final String BASKET_ID = "LX1";
    public static final String DESCRIPTION = "Lucilla Crypto Index (cETH + CBTC)";

    private static final int ATTEMPTS = 40;   // a cold sandbox needs minutes, not seconds
    private static final long PAUSE_MS = 5_000;

    /** The committee the signer portal signs against on a fresh sandbox. */
    public static final String COMMITTEE_LABEL = "CrossDesk NAV Committee";

    private final LedgerService ledger;
    private final boolean enabled;
    private final boolean seedCommittee;

    public DemoSeed(LedgerService ledger, @Value("${demo.seed-fund:true}") boolean enabled,
                    @Value("${demo.seed-committee:true}") boolean seedCommittee) {
        this.ledger = ledger;
        this.enabled = enabled;
        this.seedCommittee = seedCommittee;
    }

    /**
     * Runs once the desk is serving, on its own thread, so a slow ledger delays no
     * request and a failed seed aborts nothing.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        if (!enabled && !seedCommittee) {
            log.info("demo seed is OFF (demo.seed-fund=false, demo.seed-committee=false)");
            return;
        }
        Thread t = new Thread(this::seedWithRetry, "demo-seed");
        t.setDaemon(true);
        t.start();
    }

    void seedWithRetry() {
        for (int attempt = 1; attempt <= ATTEMPTS; attempt++) {
            try {
                seedOnce();
                return;
            } catch (RuntimeException e) {
                if (attempt == ATTEMPTS) {
                    log.warn("demo fund {} NOT seeded after {} attempts: {} — define it from the"
                            + " Fund / ETF builder, or POST /api/basket", BASKET_ID, ATTEMPTS,
                            e.getMessage());
                    return;
                }
                log.info("demo fund seed attempt {}/{} failed ({}); retrying in {}s",
                        attempt, ATTEMPTS, e.getMessage(), PAUSE_MS / 1000);
                try {
                    Thread.sleep(PAUSE_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    /** One pass: list any unlisted basket, define LX1 if it is not there, then the committee. */
    void seedOnce() {
        if (enabled) {
            seedFund();
        }
        if (seedCommittee) {
            seedCommitteeOnce();
        }
    }

    /**
     * A 2-of-3 committee of Issuer, Bank and Venue — the issuer, lender and venue seats
     * of docs/SIGNER_PROTOCOL.md §2 — administered by Issuer, observed by the Auditor.
     * The scheduler proposes into it and the signer portal signs against it. Idempotent
     * on the label, so a committee the operator desk stood up by hand is left alone
     * and a second boot does not mint a second roster.
     */
    public synchronized void seedCommitteeOnce() {
        String issuer = ledger.resolveParty("Issuer");
        boolean exists = ledger.committeesVisibleTo(issuer).stream()
                .anyMatch(c -> COMMITTEE_LABEL.equals(c.label()));
        if (exists) {
            log.info("demo committee '{}' already stands — nothing to seed", COMMITTEE_LABEL);
            return;
        }
        List<String> members = List.of(issuer, ledger.resolveParty("Bank"), ledger.resolveParty("Venue"));
        String cid = ledger.submitForCreated(issuer,
                LedgerCommands.createCommittee(issuer, members, 2, ledger.resolveParty("Auditor"), COMMITTEE_LABEL),
                LedgerCommands.operatorCommitteeTemplateId());
        log.info("demo committee '{}' seeded ({}): Issuer, Bank, Venue; K=2", COMMITTEE_LABEL, cid);
    }

    void seedFund() {
        int listed = ledger.publishMissingFundInstruments();
        if (listed > 0) {
            log.info("published {} previously unlisted basket(s) as {} instrument(s)",
                    listed, LedgerCommands.FUND_KIND);
        }
        if (ledger.basketById(BASKET_ID).isPresent()) {
            log.info("demo fund {} already defined — nothing to seed", BASKET_ID);
            return;
        }
        String bank = ledger.resolveParty("Bank");
        String auditor = ledger.resolveParty("Auditor");
        List<String> aps = List.of(ledger.resolveParty("Alice"), ledger.resolveParty("Bob"));
        String cid = ledger.defineBasket(bank, auditor, BASKET_ID, DESCRIPTION, "USDC",
                List.of(LedgerCommands.basketComponent("cETH", new BigDecimal("0.10")),
                        LedgerCommands.basketComponent("CBTC", new BigDecimal("0.01"))),
                aps, null, null, null);
        log.info("demo fund {} seeded ({}); it is listed as a {} instrument and can trade",
                BASKET_ID, cid, LedgerCommands.FUND_KIND);
    }
}
