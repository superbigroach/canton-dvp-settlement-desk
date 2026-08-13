package com.lucilla.settlement.web;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Makes {@code as} and {@code actingAs} interchangeable on every endpoint.
 *
 * <p><b>Why this exists.</b> The read endpoints disagreed about what the acting party is called:
 * {@code /moc/*} took {@code actingAs} while {@code /book/*} and {@code /perp/*} took {@code as}.
 * Spring silently ignores an unrecognised query parameter, and both families default to the Venue
 * — so passing the wrong name did not fail. It returned the VENUE's view of the world with a 200.
 *
 * <p>On a venue-operated sealed auction that is the worst possible failure mode: query the book
 * with {@code ?as=Auditor} instead of {@code ?actingAs=Auditor} and every resting order comes
 * back, making a book that is genuinely private look completely transparent. That already caused
 * one false alarm about the central privacy claim of this project. A parameter name should never be
 * able to misrepresent whether a market is dark.
 *
 * <p><b>Why a filter and not fourteen signatures.</b> There are fourteen such parameters across
 * three controllers. Fixing each one leaves the trap in place for the fifteenth. Aliasing them once
 * at the request boundary fixes every existing endpoint and every future one, and cannot be
 * forgotten.
 *
 * <p><b>Precedence.</b> If both names are present the explicit {@code actingAs} wins, because it is
 * the more specific spelling and the one the sealed-auction endpoints have always used. Neither
 * name is invented when absent: a request with no acting party still falls through to each
 * endpoint's own default, unchanged.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ActingPartyAliasFilter implements Filter {

    static final String CANONICAL = "actingAs";
    static final String ALIAS = "as";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest http) {
            chain.doFilter(new AliasedRequest(http), response);
        } else {
            chain.doFilter(request, response);
        }
    }

    /**
     * A request in which the two spellings of the acting party resolve to the same value.
     * Read-only: nothing is added unless one of the two was actually supplied.
     */
    static final class AliasedRequest extends HttpServletRequestWrapper {

        private final String[] resolved;   // null when the caller named no acting party

        AliasedRequest(HttpServletRequest delegate) {
            super(delegate);
            String[] canonical = delegate.getParameterValues(CANONICAL);
            String[] alias = delegate.getParameterValues(ALIAS);
            // actingAs wins when both are given; blanks are treated as absent so that
            // `?as=` cannot shadow a real value.
            this.resolved = firstNonBlank(canonical, alias);
        }

        private static String[] firstNonBlank(String[] a, String[] b) {
            if (a != null && a.length > 0 && a[0] != null && !a[0].isBlank()) return a;
            if (b != null && b.length > 0 && b[0] != null && !b[0].isBlank()) return b;
            return null;
        }

        private boolean isActingParty(String name) {
            return CANONICAL.equals(name) || ALIAS.equals(name);
        }

        @Override
        public String getParameter(String name) {
            if (resolved != null && isActingParty(name)) return resolved[0];
            return super.getParameter(name);
        }

        @Override
        public String[] getParameterValues(String name) {
            if (resolved != null && isActingParty(name)) return resolved.clone();
            return super.getParameterValues(name);
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            Map<String, String[]> merged = new LinkedHashMap<>(super.getParameterMap());
            if (resolved != null) {
                merged.put(CANONICAL, resolved.clone());
                merged.put(ALIAS, resolved.clone());
            }
            return java.util.Collections.unmodifiableMap(merged);
        }
    }
}
