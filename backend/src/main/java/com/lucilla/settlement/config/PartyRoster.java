package com.lucilla.settlement.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses {@code ledger.parties} ({@code LEDGER_PARTIES}) — comma-separated
 * {@code Label=partyId} pairs — into ordered entries.
 *
 * <p>Same format backend-devnet has always read, so the env set already on the
 * {@code crossdesk-devnet-api} service works unchanged. Two labels MAY point at the same
 * party (the Noders roster maps {@code Venue} onto {@code bank-crossdesk}); a malformed
 * pair is rejected loudly rather than silently dropped, because a missing {@code Venue}
 * or {@code Auditor} fails dozens of call sites with "no known party matches".
 */
public final class PartyRoster {

    public record Entry(String label, String party) {
    }

    private PartyRoster() {
    }

    public static List<Entry> parse(String roster) {
        List<Entry> out = new ArrayList<>();
        if (roster == null || roster.isBlank()) {
            return out;
        }
        for (String pair : roster.split(",")) {
            String p = pair.trim();
            if (p.isEmpty()) {
                continue;
            }
            int eq = p.indexOf('=');
            if (eq <= 0 || eq == p.length() - 1) {
                throw new IllegalStateException("LEDGER_PARTIES entry '" + p
                        + "' is not Label=partyId");
            }
            String label = p.substring(0, eq).trim();
            String party = p.substring(eq + 1).trim();
            if (!party.contains("::")) {
                // actAs claims only ever match the FULL id (hint::fingerprint).
                throw new IllegalStateException("LEDGER_PARTIES entry '" + label
                        + "' must be a full party id (hint::1220...), got '" + party + "'");
            }
            out.add(new Entry(label, party));
        }
        return out;
    }
}
