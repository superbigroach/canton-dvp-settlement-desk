package com.lucilla.settlement.readiness;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * One control a client's risk team asks about, and where we actually are on it.
 *
 * <p>Mutable with getters and setters because it is bound from {@code controls.yml} by
 * SnakeYAML, which needs a JavaBean. Everything derived — whether it counts as in place,
 * how long until it expires — is computed here rather than stored, so the file can never
 * disagree with itself.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Control {

    /** The states a control can be in. Anything unrecognised in the file reads as NOT_STARTED. */
    public enum Status {
        NOT_STARTED, IN_PROGRESS, IN_PLACE, NOT_APPLICABLE;

        static Status of(String raw) {
            if (raw == null) return NOT_STARTED;
            return switch (raw.trim().toLowerCase().replace('-', '_')) {
                case "in_place", "inplace", "yes" -> IN_PLACE;
                case "in_progress", "inprogress", "partial" -> IN_PROGRESS;
                case "not_applicable", "na", "n_a" -> NOT_APPLICABLE;
                default -> NOT_STARTED;
            };
        }
    }

    /** How near a renewal has to be before the desk starts saying so. */
    public static final int EXPIRY_WARNING_DAYS = 60;

    private String id;
    private String name;
    private String category;
    private List<String> frameworks;
    private String question;
    private String status;
    private String blocked;
    private String owner;
    private String detail;
    private String evidence;
    private String insurer;
    private BigDecimal limit;
    private LocalDate effectiveFrom;
    private LocalDate expiresAt;
    private boolean publish;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public List<String> getFrameworks() { return frameworks; }
    public void setFrameworks(List<String> frameworks) { this.frameworks = frameworks; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getBlocked() { return blocked; }
    public void setBlocked(String blocked) { this.blocked = blocked; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public String getEvidence() { return evidence; }
    public void setEvidence(String evidence) { this.evidence = evidence; }

    public String getInsurer() { return insurer; }
    public void setInsurer(String insurer) { this.insurer = insurer; }

    public BigDecimal getLimit() { return limit; }
    public void setLimit(BigDecimal limit) { this.limit = limit; }

    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }

    public LocalDate getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDate expiresAt) { this.expiresAt = expiresAt; }

    public boolean isPublish() { return publish; }
    public void setPublish(boolean publish) { this.publish = publish; }

    @JsonIgnore
    public Status statusEnum() {
        return Status.of(status);
    }

    /**
     * Days until this control lapses, negative once it has. Null when it does not renew.
     *
     * <p>This is the whole reason the register is code and not a document. An insurance
     * policy and an assurance report are the two things that go stale in silence, and both
     * are exactly what a client rechecks at renewal — so the countdown is a property of the
     * system rather than a note in somebody's calendar.
     */
    public Long getDaysUntilExpiry() {
        return expiresAt == null ? null : ChronoUnit.DAYS.between(LocalDate.now(), expiresAt);
    }

    /** True once the expiry date has passed — an expired control is NOT in place. */
    public boolean isExpired() {
        Long d = getDaysUntilExpiry();
        return d != null && d < 0;
    }

    /** Renews soon enough that somebody should be doing something about it. */
    public boolean isExpiringSoon() {
        Long d = getDaysUntilExpiry();
        return d != null && d >= 0 && d <= EXPIRY_WARNING_DAYS;
    }

    /**
     * Does this control actually hold right now?
     *
     * <p>Deliberately NOT just {@code status == IN_PLACE}: a policy that lapsed last month
     * is still written down as in place by whoever last edited the file, and that is the
     * exact error this register exists to prevent. An expired control is not a control.
     */
    public boolean isEffective() {
        return statusEnum() == Status.IN_PLACE && !isExpired();
    }
}
