package com.lucilla.settlement.auth;

import java.util.List;
import java.util.Optional;

/**
 * The user → role + party mapping (docs/PRODUCT-PLAN.md §3), behind an interface so the
 * sandbox's in-memory-plus-file store can be swapped for a database without touching
 * the filter or the admin endpoints.
 */
public interface UserStore {

    List<UserRecord> all();

    Optional<UserRecord> byUid(String uid);

    Optional<UserRecord> byEmail(String email);

    /**
     * The tolerant lookup the sandbox header uses: an exact email, else the part before
     * {@code @} or the whole value matched against uid, email local part, party label or
     * display name (case-insensitive). {@code X-Sandbox-User: Issuer} and
     * {@code X-Sandbox-User: admin@crossdesk} both resolve.
     */
    Optional<UserRecord> bySandboxReference(String reference);

    Optional<UserRecord> byApiKeyHash(String hash);

    /** Users mapped to a party (label or full id compared case-insensitively on the label). */
    List<UserRecord> byParty(String partyOrLabel);

    /** Create or replace. Assigns a uid when the record has none. Returns the stored copy. */
    UserRecord save(UserRecord user);

    boolean delete(String uid);
}
