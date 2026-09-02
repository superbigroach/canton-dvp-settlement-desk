package com.lucilla.settlement.ledger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Server-side verification of an issuer's or lender's evidence — the check that turns
 * "the lender CLAIMED book-acceptance" into "the lender supplied a number and the
 * number passes the rule" before anything is submitted to the ledger.
 *
 * <p>WHAT THIS CLOSES, AND WHAT IT DOES NOT. Until now the issuer's and lender's
 * conditions were ticks: a signer named them and the record carried the names. That is
 * layer 3 of {@code docs/SIGNER_PROTOCOL.md} §7 — residual trust — and it was the whole
 * of those two seats. Now each condition needs the numbers behind it, the desk applies
 * the rule stated in {@link SignerProtocol} and refuses with the specific failure, and the
 * event records {@code verified: true} with the numbers. What remains layer 3 is the
 * provenance of the numbers themselves: the desk cannot query the issuer's attestor set
 * or the lender's book. A signer that types a false number has made a false statement
 * on the record, with its name on it, which is the deterrent the protocol relies on.
 *
 * <p>Pure: takes the evidence, the proposal price, the signer's tolerances and the clock;
 * returns what was verified and what failed. The venue is not handled here — its range is
 * checked by the ledger, and the confirm path for it is unchanged.
 */
public final class SignerEvidence {

    private SignerEvidence() {}

    /** How stale a proof-of-reserve may be. */
    public static final Duration RESERVES_MAX_AGE = Duration.ofHours(24);
    /** Clock skew allowed on {@code acceptedAt} before it counts as "in the future". */
    public static final Duration FUTURE_SKEW = Duration.ofMinutes(5);

    /** The lender's declared tolerances, from its signer settings. */
    public record Tolerances(int markBps, int liquidationBps) {

        public static Tolerances defaults() {
            return new Tolerances(SignerProtocol.DEFAULT_TOLERANCE_BPS, SignerProtocol.DEFAULT_TOLERANCE_BPS);
        }

        /** Read {@code tolerances.markBps} / {@code tolerances.liquidationBps}; missing → 25 bp. */
        public static Tolerances from(Map<String, Object> tolerances) {
            int mark = intOf(tolerances, SignerProtocol.TOLERANCE_MARK_KEY, "toleranceBps", "independentMarkBps")
                    .orElse(SignerProtocol.DEFAULT_TOLERANCE_BPS);
            int liq = intOf(tolerances, SignerProtocol.TOLERANCE_LIQUIDATION_KEY, "liquidationsBps").orElse(mark);
            return new Tolerances(mark, liq);
        }

        private static java.util.Optional<Integer> intOf(Map<String, Object> m, String... keys) {
            if (m == null) return java.util.Optional.empty();
            for (String k : keys) {
                Object v = m.get(k);
                BigDecimal d = number(v);
                if (d != null && d.signum() >= 0) return java.util.Optional.of(d.intValue());
            }
            return java.util.Optional.empty();
        }
    }

    /**
     * The outcome. {@code verified} holds, per condition, the numbers as read plus what
     * the rule derived from them (a deviation in bp, an age in hours); {@code problems}
     * is empty when everything passed.
     */
    public record Result(Map<String, Map<String, Object>> verified, List<String> problems) {
        public boolean ok() {
            return problems.isEmpty();
        }
    }

    /**
     * Refused: thrown by the confirm route as a 422 carrying the schema, so a client that
     * sent a bare tick learns exactly what to send instead.
     */
    public static class Rejected extends RuntimeException {
        private final String seat;
        private final List<String> problems;
        private final Map<String, Object> schema;

        public Rejected(String message, String seat, List<String> problems, Map<String, Object> schema) {
            super(message);
            this.seat = seat;
            this.problems = problems == null ? List.of() : List.copyOf(problems);
            this.schema = schema == null ? Map.of() : schema;
        }

        public String seat() { return seat; }
        public List<String> problems() { return problems; }
        public Map<String, Object> schema() { return schema; }
    }

    /** Does this seat have to bring evidence at all? Issuer and lender do; venue's is the range. */
    public static boolean required(String roleKey) {
        SignerProtocol.Role r = SignerProtocol.role(roleKey);
        return r != null && r.requiresEvidence();
    }

    /** The evidence schema, per condition, for a seat — what the 422 and the protocol JSON carry. */
    public static Map<String, Object> schemaFor(String roleKey, List<String> onlyConditions) {
        SignerProtocol.Role r = SignerProtocol.role(roleKey);
        Map<String, Object> out = new LinkedHashMap<>();
        if (r == null) return out;
        for (SignerProtocol.Condition c : r.conditions()) {
            if (onlyConditions != null && !onlyConditions.isEmpty()
                    && onlyConditions.stream().noneMatch(n -> n != null && n.trim().equals(c.name()))) continue;
            out.put(c.name(), schemaOf(c.evidence()));
        }
        return out;
    }

    /** One condition's schema as plain JSON. */
    public static Map<String, Object> schemaOf(SignerProtocol.Evidence e) {
        Map<String, Object> m = new LinkedHashMap<>();
        SignerProtocol.Evidence ev = e == null ? SignerProtocol.Evidence.NONE : e;
        m.put("required", ev.required());
        m.put("verifiedBy", ev.verifiedBy());
        if (ev.rule() != null) m.put("rule", ev.rule());
        List<Map<String, String>> fields = new ArrayList<>();
        for (SignerProtocol.Field f : ev.fields()) {
            Map<String, String> fm = new LinkedHashMap<>();
            fm.put("name", f.name());
            fm.put("type", f.type());
            fm.put("description", f.description());
            fields.add(fm);
        }
        m.put("fields", fields);
        return m;
    }

    /**
     * Verify the evidence for every checked condition of an issuer or lender seat.
     *
     * @param evidence the request's {@code evidence}: condition name → {field → value}
     * @param proposal the proposed price (needed for the lender's mark test)
     */
    public static Result verify(String roleKey, List<String> checks, Map<String, Object> evidence,
            BigDecimal proposal, Tolerances tol, Instant now) {
        SignerProtocol.Role role = SignerProtocol.role(roleKey);
        Map<String, Map<String, Object>> verified = new LinkedHashMap<>();
        List<String> problems = new ArrayList<>();
        if (role == null) {
            problems.add("unknown signer role '" + roleKey + "'");
            return new Result(verified, problems);
        }
        Tolerances t = tol == null ? Tolerances.defaults() : tol;
        for (String raw : checks == null ? List.<String>of() : checks) {
            String name = raw == null ? "" : raw.trim();
            SignerProtocol.Condition c = role.condition(name);
            if (c == null) {
                problems.add("condition '" + name + "' is not one the " + role.key() + " seat verifies");
                continue;
            }
            if (c.evidence() == null || !c.evidence().required() || !"server".equals(c.evidence().verifiedBy())) {
                continue;   // nothing for the server to check (venue range is the ledger's; operator has none)
            }
            Map<String, Object> block = block(evidence, name);
            if (block == null) {
                problems.add(name + ": evidence missing — supply " + fieldNames(c));
                continue;
            }
            Map<String, Object> out = new LinkedHashMap<>();
            String problem = switch (name) {
                case "attestor-quorum" -> {
                    BigDecimal signers = num(block, "quorumSigners", out);
                    BigDecimal threshold = num(block, "quorumThreshold", out);
                    if (signers == null || threshold == null) yield missing(c, block);
                    if (threshold.signum() <= 0) yield "attestor-quorum: quorumThreshold must be positive";
                    yield signers.compareTo(threshold) >= 0 ? null
                            : "attestor-quorum: quorumSigners " + plain(signers) + " is below quorumThreshold " + plain(threshold);
                }
                case "reserves-current" -> {
                    Instant asOf = instant(block, "reservesAsOf", out);
                    if (asOf == null) yield missing(c, block);
                    Duration age = Duration.between(asOf, now);
                    out.put("ageHours", BigDecimal.valueOf(age.toMinutes()).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP));
                    if (age.isNegative() && age.abs().compareTo(FUTURE_SKEW) > 0) {
                        yield "reserves-current: reservesAsOf " + asOf + " is in the future";
                    }
                    yield age.compareTo(RESERVES_MAX_AGE) <= 0 ? null
                            : "reserves-current: the proof-of-reserve is " + age.toHours() + "h old; it must be under "
                            + RESERVES_MAX_AGE.toHours() + "h";
                }
                case "reserves-cover-supply" -> {
                    BigDecimal reserves = num(block, "reserves", out);
                    BigDecimal supply = num(block, "supply", out);
                    if (reserves == null || supply == null) yield missing(c, block);
                    if (supply.signum() < 0 || reserves.signum() < 0) yield "reserves-cover-supply: reserves and supply must be non-negative";
                    if (supply.signum() > 0) {
                        out.put("coverage", reserves.divide(supply, 6, RoundingMode.HALF_EVEN).stripTrailingZeros());
                    }
                    yield reserves.compareTo(supply) >= 0 ? null
                            : "reserves-cover-supply: reserves " + plain(reserves) + " do not cover supply " + plain(supply);
                }
                case "redemption-queue-clear" -> {
                    BigDecimal depth = num(block, "queueDepth", out);
                    BigDecimal max = num(block, "maxQueueDepth", out);
                    if (depth == null || max == null) yield missing(c, block);
                    if (depth.signum() < 0 || max.signum() < 0) yield "redemption-queue-clear: depths must be non-negative";
                    yield depth.compareTo(max) <= 0 ? null
                            : "redemption-queue-clear: queueDepth " + plain(depth) + " exceeds maxQueueDepth " + plain(max);
                }
                case "independent-mark-within-tolerance" -> {
                    BigDecimal mark = num(block, "independentMark", out);
                    if (mark == null) yield missing(c, block);
                    if (proposal == null || proposal.signum() <= 0) yield "independent-mark-within-tolerance: the proposal has no positive price to compare against";
                    if (mark.signum() <= 0) yield "independent-mark-within-tolerance: independentMark must be positive";
                    BigDecimal bps = mark.subtract(proposal).abs()
                            .divide(proposal, 12, RoundingMode.HALF_EVEN)
                            .multiply(BigDecimal.valueOf(10_000)).setScale(2, RoundingMode.HALF_UP);
                    out.put("proposal", proposal);
                    out.put("deviationBps", bps);
                    out.put("toleranceBps", t.markBps());
                    yield bps.compareTo(BigDecimal.valueOf(t.markBps())) <= 0 ? null
                            : "independent-mark-within-tolerance: your mark " + plain(mark) + " is " + bps
                            + " bp from the proposal " + plain(proposal) + "; your declared tolerance is " + t.markBps() + " bp";
                }
                case "liquidations-consistent" -> {
                    BigDecimal n = num(block, "liquidationsToday", out);
                    BigDecimal worst = num(block, "worstDeviationBps", out);
                    if (n == null || worst == null) yield missing(c, block);
                    if (n.signum() < 0) yield "liquidations-consistent: liquidationsToday must be non-negative";
                    out.put("toleranceBps", t.liquidationBps());
                    if (n.signum() == 0) yield null;   // nothing ran, nothing to be inconsistent
                    yield worst.abs().compareTo(BigDecimal.valueOf(t.liquidationBps())) <= 0 ? null
                            : "liquidations-consistent: the worst liquidation deviated " + plain(worst.abs())
                            + " bp from the mark; your declared tolerance is " + t.liquidationBps() + " bp";
                }
                case "book-acceptance" -> {
                    Instant at = instant(block, "acceptedAt", out);
                    if (at == null) yield missing(c, block);
                    yield at.isAfter(now.plus(FUTURE_SKEW)) ? "book-acceptance: acceptedAt " + at + " is in the future" : null;
                }
                default -> "no server-side rule for '" + name + "'";
            };
            if (problem != null) {
                problems.add(problem);
            } else {
                verified.put(name, out);
            }
        }
        return new Result(verified, problems);
    }

    // ---- helpers ------------------------------------------------------------------

    private static String fieldNames(SignerProtocol.Condition c) {
        return c.evidence().fields().stream().map(f -> f.name() + " (" + f.type() + ")").toList().toString();
    }

    private static String missing(SignerProtocol.Condition c, Map<String, Object> block) {
        return c.name() + ": evidence incomplete — needs " + fieldNames(c) + ", got " + block.keySet();
    }

    /** The evidence block for a condition — a nested object keyed by the condition name. */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> block(Map<String, Object> evidence, String condition) {
        if (evidence == null) return null;
        Object o = evidence.get(condition);
        if (o instanceof Map<?, ?> m) return (Map<String, Object>) m;
        return null;
    }

    private static BigDecimal num(Map<String, Object> block, String key, Map<String, Object> out) {
        BigDecimal d = number(block.get(key));
        if (d != null) out.put(key, d);
        return d;
    }

    /** A number from JSON — a {@link Number} or a numeric string — else {@code null}. */
    public static BigDecimal number(Object v) {
        if (v == null) return null;
        if (v instanceof BigDecimal b) return b;
        if (v instanceof Number n) return new BigDecimal(n.toString());
        try {
            return new BigDecimal(v.toString().trim());
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static Instant instant(Map<String, Object> block, String key, Map<String, Object> out) {
        Object v = block.get(key);
        if (v == null) return null;
        Instant i;
        try {
            if (v instanceof Number n) {
                i = Instant.ofEpochMilli(n.longValue());
            } else {
                i = Instant.parse(v.toString().trim());
            }
        } catch (RuntimeException e) {
            return null;
        }
        out.put(key, i.toString());
        return i;
    }

    private static String plain(BigDecimal d) {
        return d.stripTrailingZeros().toPlainString();
    }
}
