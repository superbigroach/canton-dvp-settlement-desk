package com.lucilla.settlement.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The acting party must mean the same thing whichever of its two names a caller uses.
 *
 * <p>The failure this prevents is not a 400. It is a 200 carrying the VENUE's view: both endpoint
 * families default to the Venue, and Spring ignores an unrecognised parameter silently, so
 * {@code ?as=Auditor} on a {@code /moc/*} endpoint used to return every resting order — making a
 * genuinely private book look transparent. These tests pin that shut.
 */
class ActingPartyAliasFilterTest {

    /** Run the filter and hand back the request the controller would actually see. */
    private static jakarta.servlet.http.HttpServletRequest through(MockHttpServletRequest in)
            throws Exception {
        var captured = new jakarta.servlet.http.HttpServletRequest[1];
        new ActingPartyAliasFilter().doFilter(in, new MockHttpServletResponse(),
                (req, res) -> captured[0] = (jakarta.servlet.http.HttpServletRequest) req);
        return captured[0];
    }

    @Test
    @DisplayName("as= is honoured where the endpoint reads actingAs")
    void aliasFeedsCanonical() throws Exception {
        var req = new MockHttpServletRequest();
        req.setParameter("as", "Auditor");

        var seen = through(req);

        assertEquals("Auditor", seen.getParameter("actingAs"),
                "a /moc/* endpoint reading actingAs must see the auditor, not fall back to Venue");
        assertEquals("Auditor", seen.getParameter("as"));
    }

    @Test
    @DisplayName("actingAs= is honoured where the endpoint reads as")
    void canonicalFeedsAlias() throws Exception {
        var req = new MockHttpServletRequest();
        req.setParameter("actingAs", "Alice");

        var seen = through(req);

        assertEquals("Alice", seen.getParameter("as"),
                "a /book/* endpoint reading as must see Alice");
        assertEquals("Alice", seen.getParameter("actingAs"));
    }

    @Test
    @DisplayName("when both are given the explicit actingAs wins")
    void canonicalWinsOverAlias() throws Exception {
        var req = new MockHttpServletRequest();
        req.setParameter("actingAs", "Auditor");
        req.setParameter("as", "Venue");

        var seen = through(req);

        assertEquals("Auditor", seen.getParameter("as"));
        assertEquals("Auditor", seen.getParameter("actingAs"));
    }

    @Test
    @DisplayName("a blank alias cannot shadow a real value")
    void blankAliasIsTreatedAsAbsent() throws Exception {
        var req = new MockHttpServletRequest();
        req.setParameter("as", "   ");
        req.setParameter("actingAs", "Bob");

        var seen = through(req);

        assertEquals("Bob", seen.getParameter("as"));
        assertEquals("Bob", seen.getParameter("actingAs"));
    }

    @Test
    @DisplayName("naming no acting party invents none — each endpoint keeps its own default")
    void absentStaysAbsent() throws Exception {
        var req = new MockHttpServletRequest();
        req.setParameter("instrumentId", "CBTC");

        var seen = through(req);

        assertNull(seen.getParameter("actingAs"));
        assertNull(seen.getParameter("as"));
        assertEquals("CBTC", seen.getParameter("instrumentId"),
                "unrelated parameters must pass through untouched");
    }

    @Test
    @DisplayName("the parameter map carries both spellings, and other parameters survive")
    void parameterMapIsConsistent() throws Exception {
        var req = new MockHttpServletRequest();
        req.setParameter("as", "Auditor");
        req.setParameter("symbol", "LX1");

        var map = through(req).getParameterMap();

        assertEquals("Auditor", map.get("actingAs")[0]);
        assertEquals("Auditor", map.get("as")[0]);
        assertEquals("LX1", map.get("symbol")[0]);
        assertTrue(map.containsKey("symbol"));
    }
}
