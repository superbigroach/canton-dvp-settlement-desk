package com.lucilla.settlement.auth;

/**
 * Verifies a bearer ID token and returns who it belongs to. Behind an interface so the
 * filter can be tested without Firebase and so a different IdP is a one-class change.
 */
public interface TokenVerifier {

    /** A verified token: the IdP's uid and the e-mail it asserts (may be null). */
    record Verified(String uid, String email, boolean emailVerified) {
    }

    /**
     * @return the verified identity
     * @throws AuthException (401) when the token is invalid, expired, or the verifier
     *         cannot be initialised (no credentials) — the message says which
     */
    Verified verify(String idToken);
}
