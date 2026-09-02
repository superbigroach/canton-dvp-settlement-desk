package com.lucilla.settlement.auth;

import java.util.List;

/**
 * Who is calling, once the filter has decided. Immutable; attached to the request.
 *
 * <p>{@code source} says how they proved it — {@code firebase}, {@code apikey},
 * {@code sandbox-header}, or {@code sandbox-anonymous} (the headerless operator desk in
 * sandbox mode, which acts with admin rights and no party of its own).
 */
public record Principal(
        String uid,
        String email,
        Role role,
        String party,          // a label ("Issuer") or a full party id; resolved by LedgerService
        String seat,           // issuer | lender | venue — signers only
        List<String> instruments,
        String org,
        String displayName,
        String source,
        String actedBy) {      // the admin's e-mail when this identity was assumed via X-Act-As

    public boolean isActedAs() {
        return actedBy != null;
    }

    public static final String ATTRIBUTE = "crossdesk.principal";

    public boolean is(Role r) {
        return role == r;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean hasParty() {
        return party != null && !party.isBlank();
    }

    public static Principal of(UserRecord u, String source) {
        return new Principal(u.getUid(), u.getEmail(), u.roleEnum(), u.getParty(), u.getSeat(),
                u.getInstruments() == null ? List.of() : List.copyOf(u.getInstruments()),
                u.getOrg(), u.getDisplayName(), source, null);
    }

    /** {@code target}'s identity, assumed by the admin {@code by} for one request. */
    public static Principal actingAs(UserRecord target, Principal by) {
        Principal t = of(target, "act-as");
        return new Principal(t.uid(), t.email(), t.role(), t.party(), t.seat(), t.instruments(),
                t.org(), t.displayName(), "act-as", by.email() == null ? by.uid() : by.email());
    }

    /** The headerless operator desk in sandbox mode: admin rights, no party of its own. */
    public static Principal sandboxOperator() {
        return new Principal("sandbox-operator", null, Role.ADMIN, null, null, List.of(),
                "CrossDesk", "Sandbox operator", "sandbox-anonymous", null);
    }

    /** A verified identity the mapping does not know — a viewer with no party. */
    public static Principal unmapped(String uid, String email, String source) {
        return new Principal(uid, email, Role.VIEWER, null, null, List.of(), null, email, source, null);
    }
}
