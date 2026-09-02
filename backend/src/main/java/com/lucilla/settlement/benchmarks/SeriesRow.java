package com.lucilla.settlement.benchmarks;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * One published value — docs/PRODUCT-PLAN.md §5 {@code /api/series/{id}}:
 * {@code { date, asOf, price, referencePrice?, wrapperFactor?, tier, k, n, signers[], fixingCid, restated }}.
 *
 * <p>Additive fields: {@code tierLabel} (what the number is), {@code session},
 * {@code superseded} (a later restatement corrected this row), {@code note}.
 *
 * <p>Tiers follow §4's waterfall: 1 attested at K (the on-ledger {@code NavFixing}),
 * 3 benchmark × last factor (automatic), 4 prior fixing carried forward (flagged),
 * 5 missed (a gap, {@code price} null). Tier 0 is the issuer's seed mark on a fresh
 * sandbox — a reference price nobody attested, shown so the origin of the series is
 * visible rather than hidden.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SeriesRow(
        String date,               // yyyy-MM-dd in the instrument's strike zone
        String asOf,               // ISO-8601 instant
        BigDecimal price,          // null for a missed strike (tier 5)
        BigDecimal referencePrice,
        BigDecimal wrapperFactor,
        int tier,
        int k,                     // real signatures on the record (0 unless tier 1)
        int n,                     // committee size
        List<String> signers,
        String fixingCid,
        boolean restated,          // this row IS a restatement of an earlier one
        String tierLabel,
        String session,
        Boolean superseded,
        String note) {

    public Instant instant() {
        return Instant.parse(asOf);
    }

    public static String labelFor(int tier) {
        return switch (tier) {
            case 1 -> "attested";
            case 2 -> "alternate-seats";
            case 3 -> "benchmark-x-factor";
            case 4 -> "carried-forward";
            case 5 -> "missed";
            default -> "seed";
        };
    }
}
