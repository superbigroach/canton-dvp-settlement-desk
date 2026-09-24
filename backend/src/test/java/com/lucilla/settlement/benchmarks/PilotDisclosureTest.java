package com.lucilla.settlement.benchmarks;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The pilot disclosure the rulebook (§6.7) requires on every value: at trust level L1 the
 * administrator holds the key for every seat, so a row that says only "attested" tells a
 * reader something untrue by omission.
 */
class PilotDisclosureTest {

    static SeriesRow row(String tierLabel, int tier, List<String> signers) {
        return new SeriesRow("2026-09-24", "2026-09-24T15:00:00Z", new BigDecimal("65000"), null, null,
                tier, signers.size(), 3, signers, "cid", false, tierLabel, "Close", null, null);
    }

    @Test
    @DisplayName("an attested row signed only by administrator-operated seats says so")
    void attestedByAdministratorSeatsIsDisclosed() {
        var r = row("attested", 1, List.of("issuer-crossdesk", "venue-crossdesk"));
        assertThat(PublicBenchmarkController.displayLabel(r, true))
                .contains("pilot")
                .contains("administrator");
        assertThat(PublicBenchmarkController.displayLabel(r, false)).isEqualTo("attested");
    }

    @Test
    @DisplayName("the disclosure also covers alternate seats, and never overrides a gap row")
    void alternateSeatsAndGaps() {
        assertThat(PublicBenchmarkController.displayLabel(row("alternate-seats", 2, List.of("a")), true))
                .contains("pilot");
        // no committee seated: that message must survive, it is not an attestation at all
        assertThat(PublicBenchmarkController.displayLabel(
                new SeriesRow("2026-09-24", "2026-09-24T15:00:00Z", null, null, null,
                        5, 0, 0, List.of(), null, false, "missed", "Close", null, null), true))
                .isEqualTo("awaiting committee");
    }

    @Test
    @DisplayName("an unattested row is labelled by its tier, disclosure or not")
    void unattestedRowsAreUnchanged() {
        assertThat(PublicBenchmarkController.displayLabel(row("seed", 0, List.of()), true))
                .isEqualTo("seed value, not attested");
        assertThat(PublicBenchmarkController.displayLabel(row("carried-forward", 4, List.of()), true))
                .isEqualTo("carried forward");
    }
}
