package com.lucilla.settlement.auth;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Which {@code /api/**} paths need what — docs/PRODUCT-PLAN.md §5, as a table.
 *
 * <p>Three classes of route:
 * <ul>
 *   <li><b>public</b> — the benchmark pages and the liveness probes; no token.</li>
 *   <li><b>role-gated</b> — the new product routes; a verified identity with one of the
 *       listed roles ({@code admin} always qualifies).</li>
 *   <li><b>legacy</b> — everything else under {@code /api}, i.e. the operator desk's
 *       existing routes: {@code admin} in firebase mode, anyone in sandbox mode.</li>
 * </ul>
 * Pure and static so it can be unit-tested without a servlet container.
 */
public final class AuthRoutes {

    private AuthRoutes() {}

    /** The verdict for one path. */
    public record Rule(Kind kind, Set<Role> roles) {
        public static final Rule PUBLIC = new Rule(Kind.PUBLIC, Set.of());
        public static final Rule LEGACY = new Rule(Kind.LEGACY, Set.of(Role.ADMIN));

        static Rule gated(Role... roles) {
            EnumSet<Role> s = EnumSet.of(Role.ADMIN);
            s.addAll(List.of(roles));
            return new Rule(Kind.GATED, Set.copyOf(s));
        }
    }

    public enum Kind { PUBLIC, GATED, LEGACY, NOT_API }

    private static final List<String> PUBLIC_EXACT = List.of(
            "/api/benchmarks", "/api/methodology", "/api/diag", "/api/health",
            "/api/signer-protocol", "/api/fixing-schedule");
    private static final List<String> PUBLIC_PREFIX = List.of(
            "/api/benchmarks/", "/api/series/");

    /** Classify a servlet path (context-relative, e.g. {@code /api/me}). */
    public static Rule ruleFor(String path) {
        String p = path == null ? "" : path;
        if (!p.startsWith("/api/") && !p.equals("/api")) {
            return new Rule(Kind.NOT_API, Set.of());
        }
        for (String e : PUBLIC_EXACT) if (p.equals(e)) return Rule.PUBLIC;
        for (String pre : PUBLIC_PREFIX) if (p.startsWith(pre)) return Rule.PUBLIC;

        if (p.equals("/api/me")) {
            return Rule.gated(Role.SIGNER, Role.AP, Role.FUND_ADMIN, Role.AUDITOR, Role.VIEWER);
        }
        if (p.startsWith("/api/proposals")) return Rule.gated(Role.SIGNER);
        if (p.startsWith("/api/signer/")) return Rule.gated(Role.SIGNER);
        if (p.startsWith("/api/ap/")) return Rule.gated(Role.AP);
        if (p.startsWith("/api/fund/")) return Rule.gated(Role.FUND_ADMIN);
        if (p.startsWith("/api/admin/")) return Rule.gated();
        if (p.startsWith("/api/audit/")) return Rule.gated(Role.AUDITOR);
        return Rule.LEGACY;
    }
}
