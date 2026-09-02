package com.lucilla.settlement.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lucilla.settlement.events.EventStore;
import com.lucilla.settlement.events.FixingEvent;
import org.springframework.http.HttpStatus;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The identity boundary on {@code /api/**} — docs/PRODUCT-PLAN.md §3.
 *
 * <p>Order of resolution for every request:
 * <ol>
 *   <li>{@code Authorization: Bearer ck_…} → an API key, in BOTH modes, as the user it
 *       was issued to.</li>
 *   <li>{@code Authorization: Bearer <jwt>} → a Firebase ID token, verified; the e-mail
 *       is looked up in the mapping. A verified user the mapping does not know is a
 *       {@code viewer}: they can call {@code /api/me} and nothing else.</li>
 *   <li>Sandbox mode only: {@code X-Sandbox-User: <email>} → the mapping.</li>
 *   <li>Sandbox mode only, no headers at all: the anonymous operator desk, which acts as
 *       {@code admin} on the LEGACY routes. It is NOT an identity for the new routes —
 *       {@code /api/me} without a user is still a 401, because a portal that showed a
 *       phantom user would be lying to the person testing it.</li>
 * </ol>
 * Then the route's rule (see {@link AuthRoutes}) is applied. The resolved
 * {@link Principal} rides on the request as {@link Principal#ATTRIBUTE}.
 *
 * <p>In firebase mode the legacy routes require {@code admin}; that is what switching
 * {@code AUTH_MODE} on the live service will enforce once the app ships its login page.
 */
public class AuthFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);
    public static final String SANDBOX_HEADER = "X-Sandbox-User";
    /** An admin assumes another mapped user's identity for ONE request. */
    public static final String ACT_AS_HEADER = "X-Act-As";
    public static final String ACT_AS_EVENT = "admin.act_as";

    private final AuthProperties props;
    private final UserStore users;
    private final TokenVerifier verifier;
    private final EventStore events;
    private final ObjectMapper json = new ObjectMapper();

    public AuthFilter(AuthProperties props, UserStore users, TokenVerifier verifier, EventStore events) {
        this.props = props;
        this.users = users;
        this.verifier = verifier;
        this.events = events;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest http) || !(response instanceof HttpServletResponse out)) {
            chain.doFilter(request, response);
            return;
        }
        String path = http.getRequestURI();
        String ctx = http.getContextPath();
        if (ctx != null && !ctx.isEmpty() && path.startsWith(ctx)) {
            path = path.substring(ctx.length());
        }
        AuthRoutes.Rule rule = AuthRoutes.ruleFor(path);
        if (rule.kind() == AuthRoutes.Kind.NOT_API) {
            chain.doFilter(request, response);
            return;
        }
        // CORS preflight carries no credentials by definition.
        if ("OPTIONS".equalsIgnoreCase(http.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        try {
            Optional<Principal> who = resolve(http);
            // X-Act-As: an admin (and, in sandbox mode, the headerless operator desk, which
            // IS the admin) takes another mapped user's role, party, seat and instruments
            // for this request only. Anyone else sending it is refused outright, before
            // the route is even considered.
            String actAs = http.getHeader(ACT_AS_HEADER);
            if (actAs != null && !actAs.isBlank()) {
                Principal by = who.orElseGet(() -> props.isSandbox() && rule.kind() != AuthRoutes.Kind.PUBLIC
                        ? Principal.sandboxOperator() : null);
                if (by == null) {
                    throw AuthException.unauthenticated(ACT_AS_HEADER + " needs a signed-in admin");
                }
                if (!by.isAdmin()) {
                    throw AuthException.forbidden(ACT_AS_HEADER + " is admin-only; you are '"
                            + by.role().wire() + "'");
                }
                UserRecord target = users.byEmail(actAs).or(() -> users.bySandboxReference(actAs))
                        .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND,
                                "no user '" + actAs.trim() + "' to act as"));
                Principal assumed = Principal.actingAs(target, by);
                who = Optional.of(assumed);
                if (isMutating(http.getMethod())) {
                    recordActAs(by, assumed, http.getMethod(), path);
                }
            }
            if (who.isPresent()) {
                http.setAttribute(Principal.ATTRIBUTE, who.get());
            }
            switch (rule.kind()) {
                case PUBLIC -> { /* identity is optional; attached if present */ }
                case GATED -> {
                    Principal p = who.orElseThrow(() -> AuthException.unauthenticated(
                            props.isSandbox()
                                    ? "sign in, or send X-Sandbox-User: <email> (AUTH_MODE=sandbox)"
                                    : "sign in: send Authorization: Bearer <Firebase ID token>"));
                    if (!rule.roles().contains(p.role())) {
                        throw AuthException.forbidden("role '" + p.role().wire()
                                + "' may not call " + path + " (needs one of "
                                + rule.roles().stream().map(Role::wire).sorted().toList() + ")");
                    }
                }
                case LEGACY -> {
                    if (props.isSandbox()) {
                        // The operator desk on the hosted sandbox sends no headers. Let it
                        // through as the anonymous operator so nothing that works today breaks.
                        if (who.isEmpty()) {
                            http.setAttribute(Principal.ATTRIBUTE, Principal.sandboxOperator());
                        }
                    } else {
                        Principal p = who.orElseThrow(() -> AuthException.unauthenticated(
                                "the operator desk requires an admin sign-in (AUTH_MODE=firebase)"));
                        if (!p.isAdmin()) {
                            throw AuthException.forbidden("the operator desk is admin-only; you are '"
                                    + p.role().wire() + "'");
                        }
                    }
                }
                default -> { }
            }
        } catch (AuthException e) {
            reject(out, e, path);
            return;
        }
        chain.doFilter(request, response);
    }

    /** The identity the headers prove, if any. Throws only on a PRESENTED credential that fails. */
    Optional<Principal> resolve(HttpServletRequest http) {
        String authz = http.getHeader("Authorization");
        if (authz != null && authz.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String token = authz.substring(7).trim();
            if (token.isEmpty()) {
                throw AuthException.unauthenticated("empty bearer token");
            }
            if (ApiKeys.looksLikeKey(token)) {
                UserRecord u = users.byApiKeyHash(ApiKeys.hash(token))
                        .orElseThrow(() -> AuthException.unauthenticated("unknown API key"));
                return Optional.of(Principal.of(u, "apikey"));
            }
            TokenVerifier.Verified v = verifier.verify(token);
            Optional<UserRecord> mapped = users.byEmail(v.email());
            if (mapped.isPresent()) {
                return Optional.of(Principal.of(mapped.get(), "firebase"));
            }
            log.info("AUTH verified {} ({}) is not in the user mapping — viewer", v.email(), v.uid());
            return Optional.of(Principal.unmapped(v.uid(), v.email(), "firebase"));
        }
        if (props.isSandbox()) {
            String ref = http.getHeader(SANDBOX_HEADER);
            if (ref != null && !ref.isBlank()) {
                UserRecord u = users.bySandboxReference(ref)
                        .orElseThrow(() -> AuthException.unauthenticated(
                                "no user matches X-Sandbox-User '" + ref.trim() + "'"));
                return Optional.of(Principal.of(u, "sandbox-header"));
            }
        }
        return Optional.empty();
    }

    private static boolean isMutating(String method) {
        String m = method == null ? "" : method.toUpperCase();
        return !(m.equals("GET") || m.equals("HEAD") || m.equals("OPTIONS"));
    }

    private void recordActAs(Principal by, Principal as, String method, String path) {
        try {
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("by", by.email() == null ? by.uid() : by.email());
            d.put("as", as.email());
            d.put("asUid", as.uid());
            d.put("asRole", as.role().wire());
            d.put("method", method);
            d.put("path", path);
            events.append(FixingEvent.of(ACT_AS_EVENT, null, null, null,
                    (by.email() == null ? by.uid() : by.email()) + " as " + as.email(), "admin", null,
                    method + " " + path, null, null, null, d));
        } catch (RuntimeException e) {
            log.warn("act-as event not recorded: {}", e.toString());
        }
    }

    private void reject(HttpServletResponse out, AuthException e, String path) throws IOException {
        log.info("AUTH {} {} -> {}", e.status().value(), path, e.getMessage());
        out.setStatus(e.status().value());
        out.setContentType("application/json");
        if (e.status().value() == 401) {
            out.setHeader("WWW-Authenticate", "Bearer realm=\"crossdesk\"");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", e.status().value());
        body.put("error", e.status().getReasonPhrase());
        body.put("message", e.getMessage());
        body.put("authMode", props.getMode());
        out.getWriter().write(json.writeValueAsString(body));
    }
}
