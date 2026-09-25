package com.lucilla.settlement.readiness;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The register, and the one behaviour that justifies it being code.
 *
 * <p>A document can say "insurance: in place". Only something that computes can notice
 * that the policy behind that sentence lapsed last month — and an expired policy is the
 * single most likely way this register would otherwise come to state something false.
 */
class ControlsRegisterTest {

    private static Control control(String id, String status, LocalDate expires) {
        Control c = new Control();
        c.setId(id);
        c.setName(id);
        c.setCategory("Insurance");
        c.setStatus(status);
        c.setExpiresAt(expires);
        return c;
    }

    @Test
    @DisplayName("an expired control is NOT in place, whatever the file says")
    void expiredIsNotEffective() {
        Control lapsed = control("insurance-eo", "in_place", LocalDate.now().minusDays(1));

        assertThat(lapsed.statusEnum()).isEqualTo(Control.Status.IN_PLACE);
        assertThat(lapsed.isExpired()).isTrue();
        assertThat(lapsed.isEffective())
                .as("a policy that lapsed yesterday is not a control")
                .isFalse();
    }

    @Test
    @DisplayName("a control with no expiry never expires and never warns")
    void noExpiryNeverLapses() {
        Control c = control("ci-pipeline", "in_place", null);

        assertThat(c.getDaysUntilExpiry()).isNull();
        assertThat(c.isExpired()).isFalse();
        assertThat(c.isExpiringSoon()).isFalse();
        assertThat(c.isEffective()).isTrue();
    }

    @Test
    @DisplayName("renewal warns inside the window and not outside it")
    void warnsBeforeRenewal() {
        assertThat(control("a", "in_place", LocalDate.now().plusDays(30)).isExpiringSoon()).isTrue();
        assertThat(control("b", "in_place", LocalDate.now().plusDays(Control.EXPIRY_WARNING_DAYS)).isExpiringSoon()).isTrue();
        assertThat(control("c", "in_place", LocalDate.now().plusDays(Control.EXPIRY_WARNING_DAYS + 1)).isExpiringSoon()).isFalse();
        // Already gone is not "soon" — it is a different, louder problem.
        assertThat(control("d", "in_place", LocalDate.now().minusDays(1)).isExpiringSoon()).isFalse();
    }

    @Test
    @DisplayName("an unrecognised status reads as not started, never as in place")
    void unknownStatusFailsSafe() {
        assertThat(control("x", "probably fine", null).statusEnum()).isEqualTo(Control.Status.NOT_STARTED);
        assertThat(control("y", null, null).statusEnum()).isEqualTo(Control.Status.NOT_STARTED);
        assertThat(control("z", "", null).isEffective()).isFalse();
    }

    // ---- the shipped file -------------------------------------------------

    private static ControlsRegister shipped() {
        return new ControlsRegister(new DefaultResourceLoader(), "classpath:controls.yml");
    }

    @Test
    @DisplayName("the shipped register parses and carries the controls a client asks about")
    void shippedRegisterParses() {
        List<Control> all = shipped().all();

        assertThat(all).isNotEmpty();
        assertThat(all).allSatisfy(c -> {
            assertThat(c.getId()).isNotBlank();
            assertThat(c.getName()).isNotBlank();
            assertThat(c.getCategory()).isNotBlank();
        });
        assertThat(all).extracting(Control::getId)
                .contains("insurance-eo", "assurance-report", "ci-pipeline", "alerting",
                        "incident-response", "backup-restore-tested");
    }

    @Test
    @DisplayName("gaps sort above controls that hold, because the gaps are the point")
    void worstFirst() {
        List<Control> all = shipped().all();
        int lastEffective = -1, firstEffective = Integer.MAX_VALUE;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).isEffective()) {
                firstEffective = Math.min(firstEffective, i);
            } else {
                lastEffective = Math.max(lastEffective, i);
            }
        }
        assertThat(firstEffective).isGreaterThan(lastEffective - all.size());
        assertThat(all.get(0).isEffective()).isFalse();
    }

    @Test
    @DisplayName("the public subset is a subset, and insurance is not in it")
    void publishedIsNarrower() {
        ControlsRegister r = shipped();

        assertThat(r.published()).isNotEmpty();
        assertThat(r.published().size()).isLessThan(r.all().size());
        assertThat(r.published()).allMatch(Control::isPublish);
        assertThat(r.published()).extracting(Control::getId)
                .as("an itemised list of absent controls is not for the open web")
                .doesNotContain("insurance-eo", "insurance-cyber", "assurance-report");
    }

    @Test
    @DisplayName("summary counts what HOLDS, not what the file claims")
    void summaryCountsEffective() {
        Map<String, Object> s = shipped().summary();

        assertThat(s).containsKeys("total", "effective", "inProgress", "notStarted",
                "expired", "expiringSoon", "blockedOn");
        assertThat((Integer) s.get("total")).isPositive();
        assertThat(s).doesNotContainKey("loadError");

        @SuppressWarnings("unchecked")
        Map<String, Long> blocked = (Map<String, Long>) s.get("blockedOn");
        assertThat(blocked).containsKeys("time", "money", "customer");
        assertThat(blocked.values().stream().mapToLong(Long::longValue).sum()).isPositive();
    }

    @Test
    @DisplayName("a missing file leaves the desk running and says so")
    void missingFileIsNotFatal() {
        ControlsRegister r = new ControlsRegister(new DefaultResourceLoader(), "classpath:no-such-controls.yml");

        assertThat(r.all()).isEmpty();
        assertThat(r.summary()).containsEntry("total", 0);
        assertThat(r.summary()).containsKey("loadError");
    }
}
