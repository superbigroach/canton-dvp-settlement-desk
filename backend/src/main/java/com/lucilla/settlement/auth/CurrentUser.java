package com.lucilla.settlement.auth;

import jakarta.servlet.http.HttpServletRequest;

/** How a controller reads the identity the filter attached. */
public final class CurrentUser {

    private CurrentUser() {}

    /** The principal, or a 401 — the filter guarantees one on every gated route. */
    public static Principal require(HttpServletRequest req) {
        Object p = req.getAttribute(Principal.ATTRIBUTE);
        if (p instanceof Principal principal) {
            return principal;
        }
        throw AuthException.unauthenticated("no identity on this request");
    }

    /**
     * The principal AND its ledger party — the requirement for anything that submits a
     * command as the caller. The anonymous sandbox operator has no party, so it cannot
     * sign or create; that is the honest answer, not a bug.
     */
    public static Principal requireParty(HttpServletRequest req) {
        Principal p = require(req);
        if (!p.hasParty()) {
            throw AuthException.forbidden("your user has no ledger party mapped — an admin must "
                    + "set one at PUT /api/admin/users/" + p.uid());
        }
        return p;
    }
}
