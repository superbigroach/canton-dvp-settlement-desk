package com.lucilla.settlement.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The identity boundary, in both modes — docs/PRODUCT-PLAN.md §3.
 *
 * <p>What must never regress: in sandbox mode the headerless operator desk keeps working
 * on the legacy routes (the live sandbox depends on it); in firebase mode those same
 * routes are admin-only; the new routes always need a real identity; an API key works
 * in both modes as the user it was minted for.
 */
class AuthFilterTest {

    static final String ADMIN_KEY = ApiKeys.generate();

    /** A verifier that accepts exactly two tokens and rejects everything else. */
    static final TokenVerifier VERIFIER = token -> switch (token) {
        case "tok-admin" -> new TokenVerifier.Verified("fb-admin", "s.borjas@lucilla.ca", true);
        case "tok-alice" -> new TokenVerifier.Verified("fb-alice", "alice@sandbox.crossdesk", true);
        case "tok-stranger" -> new TokenVerifier.Verified("fb-x", "nobody@example.com", true);
        default -> throw AuthException.unauthenticated("bad token");
    };

    static UserStore users() {
        UserRecord admin = new UserRecord();
        admin.setUid("admin-sborjas");
        admin.setEmail("s.borjas@lucilla.ca");
        admin.setRole("admin");
        admin.setParty("Issuer");
        admin.setApiKeyHash(ApiKeys.hash(ADMIN_KEY));

        UserRecord issuer = new UserRecord();
        issuer.setUid("sandbox-issuer");
        issuer.setEmail("issuer@sandbox.crossdesk");
        issuer.setRole("signer");
        issuer.setSeat("issuer");
        issuer.setParty("Issuer");
        issuer.setInstruments(List.of("CBTC", "cETH"));

        UserRecord alice = new UserRecord();
        alice.setUid("sandbox-alice");
        alice.setEmail("alice@sandbox.crossdesk");
        alice.setRole("ap");
        alice.setParty("Alice");
        return FileUserStore.inMemory(List.of(admin, issuer, alice));
    }

    static AuthFilter filter(String mode) {
        AuthProperties p = new AuthProperties();
        p.setMode(mode);
        return new AuthFilter(p, users(), VERIFIER, com.lucilla.settlement.events.JsonlEventStore.inMemory());
    }

    static AuthFilter filter(String mode, com.lucilla.settlement.events.EventStore events) {
        AuthProperties p = new AuthProperties();
        p.setMode(mode);
        return new AuthFilter(p, users(), VERIFIER, events);
    }

    record Outcome(int status, Principal principal, boolean reachedChain) {
    }

    static Outcome run(AuthFilter f, String path, String... headers) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", path);
        req.setRequestURI(path);
        for (int i = 0; i + 1 < headers.length; i += 2) {
            req.addHeader(headers[i], headers[i + 1]);
        }
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        f.doFilter(req, res, chain);
        boolean reached = chain.getRequest() != null;
        Object p = req.getAttribute(Principal.ATTRIBUTE);
        return new Outcome(res.getStatus(), p instanceof Principal pr ? pr : null, reached);
    }

    // ---- sandbox mode --------------------------------------------------------------

    @Test
    @DisplayName("sandbox: the headerless operator desk still reaches the legacy routes, as admin")
    void sandboxLegacyNoHeaders() throws Exception {
        Outcome o = run(filter("sandbox"), "/api/instruments");
        assertTrue(o.reachedChain());
        assertNotNull(o.principal());
        assertEquals(Role.ADMIN, o.principal().role());
        assertEquals("sandbox-anonymous", o.principal().source());
        assertFalse(o.principal().hasParty(), "the anonymous desk acts as nobody's party");
    }

    @Test
    @DisplayName("sandbox: /api/me with no user is still a 401 — no phantom identity")
    void sandboxMeNeedsHeader() throws Exception {
        Outcome o = run(filter("sandbox"), "/api/me");
        assertEquals(401, o.status());
        assertFalse(o.reachedChain());
    }

    @Test
    @DisplayName("sandbox: X-Sandbox-User resolves by e-mail, and by party label")
    void sandboxHeaderResolves() throws Exception {
        Outcome admin = run(filter("sandbox"), "/api/me", "X-Sandbox-User", "s.borjas@lucilla.ca");
        assertTrue(admin.reachedChain());
        assertEquals(Role.ADMIN, admin.principal().role());
        assertEquals("Issuer", admin.principal().party());

        Outcome issuer = run(filter("sandbox"), "/api/proposals", "X-Sandbox-User", "Issuer");
        assertTrue(issuer.reachedChain());
        assertEquals(Role.SIGNER, issuer.principal().role());
        assertEquals("issuer", issuer.principal().seat());
        assertEquals("sandbox-header", issuer.principal().source());
    }

    @Test
    @DisplayName("sandbox: an unknown X-Sandbox-User is a 401, a wrong role is a 403")
    void sandboxRoleGates() throws Exception {
        assertEquals(401, run(filter("sandbox"), "/api/me", "X-Sandbox-User", "ghost@nowhere").status());
        Outcome alice = run(filter("sandbox"), "/api/admin/users", "X-Sandbox-User", "alice@sandbox.crossdesk");
        assertEquals(403, alice.status());
        assertFalse(alice.reachedChain());
        assertTrue(run(filter("sandbox"), "/api/ap/funds", "X-Sandbox-User", "alice").reachedChain());
    }

    // ---- firebase mode --------------------------------------------------------------

    @Test
    @DisplayName("firebase: the legacy routes need an admin token")
    void firebaseLegacyIsAdminOnly() throws Exception {
        assertEquals(401, run(filter("firebase"), "/api/instruments").status());
        assertEquals(401, run(filter("firebase"), "/api/instruments", "X-Sandbox-User", "s.borjas@lucilla.ca").status(),
                "the sandbox header means nothing in firebase mode");
        Outcome admin = run(filter("firebase"), "/api/instruments", "Authorization", "Bearer tok-admin");
        assertTrue(admin.reachedChain());
        assertEquals("firebase", admin.principal().source());
        assertEquals(403, run(filter("firebase"), "/api/instruments", "Authorization", "Bearer tok-alice").status());
    }

    @Test
    @DisplayName("firebase: a bad token is a 401 with WWW-Authenticate; a verified stranger is a viewer")
    void firebaseTokens() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/me");
        req.setRequestURI("/api/me");
        req.addHeader("Authorization", "Bearer garbage");
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter("firebase").doFilter(req, res, new MockFilterChain());
        assertEquals(401, res.getStatus());
        assertNotNull(res.getHeader("WWW-Authenticate"));
        assertTrue(res.getContentAsString().contains("\"authMode\":\"firebase\""));

        Outcome stranger = run(filter("firebase"), "/api/me", "Authorization", "Bearer tok-stranger");
        assertTrue(stranger.reachedChain(), "/api/me works for any verified identity");
        assertEquals(Role.VIEWER, stranger.principal().role());
        assertNull(stranger.principal().party());
        assertEquals(403, run(filter("firebase"), "/api/proposals", "Authorization", "Bearer tok-stranger").status());
    }

    @Test
    @DisplayName("firebase: the public routes need nothing")
    void firebasePublic() throws Exception {
        for (String p : List.of("/api/benchmarks", "/api/benchmarks/CBTC", "/api/series/CBTC",
                "/api/series/CBTC.csv", "/api/methodology", "/api/diag", "/api/health",
                "/api/signer-protocol", "/api/fixing-schedule")) {
            Outcome o = run(filter("firebase"), p);
            assertTrue(o.reachedChain(), p + " must be public");
            assertEquals(200, o.status(), p);
        }
    }

    // ---- API keys, both modes ---------------------------------------------------------

    @Test
    @DisplayName("an API key is the user it was minted for, in both modes")
    void apiKeys() throws Exception {
        for (String mode : List.of("sandbox", "firebase")) {
            Outcome o = run(filter(mode), "/api/admin/events", "Authorization", "Bearer " + ADMIN_KEY);
            assertTrue(o.reachedChain(), mode);
            assertEquals("apikey", o.principal().source());
            assertEquals("s.borjas@lucilla.ca", o.principal().email());
            assertEquals(401, run(filter(mode), "/api/admin/events", "Authorization", "Bearer ck_nope").status());
        }
    }

    // ---- X-Act-As ------------------------------------------------------------------------

    @Test
    @DisplayName("X-Act-As: an admin becomes the named user for this request only; a mutation is logged")
    void actAsAdmin() throws Exception {
        var events = com.lucilla.settlement.events.JsonlEventStore.inMemory();
        AuthFilter f = filter("firebase", events);
        Outcome o = run(f, "/api/proposals", "Authorization", "Bearer tok-admin",
                "X-Act-As", "issuer@sandbox.crossdesk");
        assertTrue(o.reachedChain());
        assertEquals(Role.SIGNER, o.principal().role());
        assertEquals("issuer", o.principal().seat());
        assertEquals("Issuer", o.principal().party());
        assertEquals(List.of("CBTC", "cETH"), o.principal().instruments());
        assertEquals("issuer@sandbox.crossdesk", o.principal().email());
        assertEquals("s.borjas@lucilla.ca", o.principal().actedBy());
        assertEquals("act-as", o.principal().source());
        assertTrue(events.all().isEmpty(), "a GET is not logged");

        // The assumed role is what the route sees: an admin acting as an AP cannot reach /api/admin.
        assertEquals(403, run(f, "/api/admin/users", "Authorization", "Bearer tok-admin",
                "X-Act-As", "alice@sandbox.crossdesk").status());

        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/proposals/x/refuse");
        req.setRequestURI("/api/proposals/x/refuse");
        req.addHeader("Authorization", "Bearer tok-admin");
        req.addHeader("X-Act-As", "issuer@sandbox.crossdesk");
        MockFilterChain chain = new MockFilterChain();
        f.doFilter(req, new MockHttpServletResponse(), chain);
        assertNotNull(chain.getRequest());
        assertEquals(1, events.all().size());
        var ev = events.all().get(0);
        assertEquals(AuthFilter.ACT_AS_EVENT, ev.kind());
        assertEquals("s.borjas@lucilla.ca", ev.details().get("by"));
        assertEquals("issuer@sandbox.crossdesk", ev.details().get("as"));
        assertEquals("POST", ev.details().get("method"));

        // Sandbox mode: the headerless operator desk is the admin, so it may act as anyone.
        Outcome sandbox = run(filter("sandbox"), "/api/me", "X-Act-As", "alice");
        assertTrue(sandbox.reachedChain());
        assertEquals(Role.AP, sandbox.principal().role());
        assertEquals("sandbox-operator", sandbox.principal().actedBy());
    }

    @Test
    @DisplayName("X-Act-As: a non-admin is refused with 403, an unknown target with 404")
    void actAsNonAdmin() throws Exception {
        assertEquals(403, run(filter("firebase"), "/api/me", "Authorization", "Bearer tok-alice",
                "X-Act-As", "issuer@sandbox.crossdesk").status());
        assertEquals(403, run(filter("sandbox"), "/api/me", "X-Sandbox-User", "issuer",
                "X-Act-As", "alice@sandbox.crossdesk").status());
        assertEquals(403, run(filter("firebase"), "/api/benchmarks", "Authorization", "Bearer tok-alice",
                "X-Act-As", "issuer@sandbox.crossdesk").status(), "refused even on a public route");
        assertEquals(401, run(filter("firebase"), "/api/me", "X-Act-As", "issuer@sandbox.crossdesk").status());
        assertEquals(404, run(filter("firebase"), "/api/me", "Authorization", "Bearer tok-admin",
                "X-Act-As", "ghost@nowhere").status());
    }

    @Test
    @DisplayName("routes outside /api are not touched")
    void nonApiUntouched() throws Exception {
        Outcome o = run(filter("firebase"), "/desk/index.html");
        assertTrue(o.reachedChain());
        assertNull(o.principal());
    }

    @Test
    @DisplayName("the route table classifies as §5 says")
    void routeTable() {
        assertEquals(AuthRoutes.Kind.PUBLIC, AuthRoutes.ruleFor("/api/series/LX1.csv").kind());
        assertEquals(AuthRoutes.Kind.GATED, AuthRoutes.ruleFor("/api/proposals/abc/confirm").kind());
        assertTrue(AuthRoutes.ruleFor("/api/proposals").roles().contains(Role.SIGNER));
        assertTrue(AuthRoutes.ruleFor("/api/audit/events").roles().contains(Role.AUDITOR));
        assertFalse(AuthRoutes.ruleFor("/api/audit/events").roles().contains(Role.AP));
        assertEquals(AuthRoutes.Kind.LEGACY, AuthRoutes.ruleFor("/api/fixing/x/confirm-checked").kind());
        assertEquals(AuthRoutes.Kind.LEGACY, AuthRoutes.ruleFor("/api/committee/x/propose").kind());
        assertEquals(AuthRoutes.Kind.NOT_API, AuthRoutes.ruleFor("/registry/v1").kind());
    }
}
