package com.lucilla.settlement.ledger;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The canonical signer protocol — {@code docs/SIGNER_PROTOCOL.md}, as data.
 *
 * <p>WHY THIS CLASS EXISTS. {@code SignerCheck.checksPassed} is {@code [Text]} on the
 * ledger, deliberately: the protocol is versioned prose that will gain conditions faster
 * than a DAR can be upgraded, and a closed enum on-chain would force a package upgrade to
 * add a check. But "the ledger does not constrain it" is not the same as "nothing should",
 * and without something in between a signer can post {@code ["whatever"]} and the
 * attestation looks identical to a real one on the record forever.
 *
 * <p>So the constraint lives HERE, at the edge, where it can be versioned with the
 * document rather than with the package. Three things follow from that placement:
 *
 * <ul>
 *   <li>The API refuses a condition that does not belong to the declared role, so an
 *       attestation cannot claim a check its seat is not the one that can perform.
 *   <li>The UI renders the conditions from {@link #roles()} rather than hard-coding them,
 *       so what a signer ticks is exactly what the backend will accept — there is one
 *       source of truth and the screen cannot drift from the rule.
 *   <li>Raising the protocol version is a code change with a diff, not an edit to prose
 *       that nothing reads.
 * </ul>
 *
 * <p>EVIDENCE. Since 2 Sep 2026 an issuer or lender condition carries an {@link Evidence}
 * schema: the numbers the signer must supply, and the rule the server checks them
 * against before the confirm is submitted ({@link SignerEvidence}). A tick alone is no
 * longer accepted for those seats. The venue's evidence is its traded range, checked by
 * the ledger itself, and that path is unchanged. What this still does not do is audit
 * the signer's systems: the numbers are the signer's own, and a false number is a false
 * statement made on the record. See {@code docs/SIGNER_PROTOCOL.md} §7.
 */
public final class SignerProtocol {

    private SignerProtocol() {}

    /**
     * The protocol version stamped onto every attestation that omits its own.
     *
     * <p>Bump this whenever a condition is added, removed or redefined. A fixing is always
     * read under the version in force when it was struck, so the stamp is what makes an
     * old attestation interpretable after the document has moved on.
     */
    public static final String VERSION = "SIGNER_PROTOCOL v1";

    /** A seat, and the named conditions that seat is the one able to verify. */
    public record Role(
            String key,
            String title,
            String uniquelyKnows,
            List<Condition> conditions,
            boolean requiresObservedRange) {

        /** Does this seat have to bring per-condition numeric evidence? */
        public boolean requiresEvidence() {
            return conditions.stream().anyMatch(c -> c.evidence() != null && c.evidence().required());
        }

        public Condition condition(String name) {
            String n = name == null ? "" : name.trim();
            return conditions.stream().filter(c -> c.name().equals(n)).findFirst().orElse(null);
        }
    }

    /** One named condition, the plain statement of when it passes, and the evidence it needs. */
    public record Condition(String name, String passesWhen, Evidence evidence) {
        public Condition(String name, String passesWhen) {
            this(name, passesWhen, Evidence.NONE);
        }
    }

    /** One field of evidence: {@code type} is {@code integer} | {@code number} | {@code instant}. */
    public record Field(String name, String type, String description) {
    }

    /**
     * The evidence a condition needs. {@code verifiedBy} says who checks it: {@code server}
     * (this desk, before submitting), {@code ledger} (the Daml choice), or {@code signer}
     * (nobody but the signer — recorded, not checked).
     */
    public record Evidence(boolean required, List<Field> fields, String rule, String verifiedBy) {
        public static final Evidence NONE = new Evidence(false, List.of(), null, "signer");

        static Evidence server(String rule, Field... fields) {
            return new Evidence(true, List.of(fields), rule, "server");
        }
    }

    /** Where the lender's tolerance comes from: {@code PUT /api/signer/settings} {@code tolerances}. */
    public static final String TOLERANCE_MARK_KEY = "markBps";
    public static final String TOLERANCE_LIQUIDATION_KEY = "liquidationBps";
    public static final int DEFAULT_TOLERANCE_BPS = 25;

    private static final Map<String, Role> ROLES = new LinkedHashMap<>();

    static {
        // §2a — the seat that makes the product exist. `wrapperFactor` is the only field
        // in the system no external administrator produces, and the issuer is the only
        // party with the facts to justify it.
        put(new Role("issuer", "Issuer", "Whether the wrapper can actually be redeemed right now",
                List.of(
                        new Condition("attestor-quorum",
                                "At least the issuer's own threshold of attestors are online and signing",
                                Evidence.server("quorumSigners >= quorumThreshold",
                                        new Field("quorumSigners", "integer", "attestors online and signing right now"),
                                        new Field("quorumThreshold", "integer", "the issuer's own quorum threshold"))),
                        new Condition("reserves-current",
                                "The most recent proof-of-reserve attestation is less than 24h old",
                                Evidence.server("reservesAsOf within 24 hours of the confirmation",
                                        new Field("reservesAsOf", "instant", "ISO-8601 time of the latest proof-of-reserve attestation"))),
                        new Condition("reserves-cover-supply",
                                "Attested reserves are at least the circulating supply of the wrapped token",
                                Evidence.server("reserves >= supply",
                                        new Field("reserves", "number", "attested reserves, in units of the underlying"),
                                        new Field("supply", "number", "circulating supply of the wrapped token"))),
                        new Condition("redemption-queue-clear",
                                "No redemption request is unfilled beyond its stated window",
                                Evidence.server("queueDepth <= maxQueueDepth",
                                        new Field("queueDepth", "integer", "redemption requests currently unfilled"),
                                        new Field("maxQueueDepth", "integer", "the depth the issuer's own window allows")))),
                false));

        // §2b — the strongest signature available, because it is the only one asserted
        // against the signer's own money.
        put(new Role("lender", "Lender", "Whether you will carry this number on your own book",
                List.of(
                        new Condition("independent-mark-within-tolerance",
                                "The proposed mark is within your declared tolerance of your own valuation",
                                Evidence.server("|independentMark - proposal| / proposal <= tolerances." + TOLERANCE_MARK_KEY
                                        + " (default " + DEFAULT_TOLERANCE_BPS + " bp)",
                                        new Field("independentMark", "number", "your own valuation of the instrument at the strike"))),
                        new Condition("liquidations-consistent",
                                "No liquidation you ran in the session cleared materially away from the mark",
                                Evidence.server("worstDeviationBps <= tolerances." + TOLERANCE_LIQUIDATION_KEY
                                        + " (default: the mark tolerance)",
                                        new Field("liquidationsToday", "integer", "liquidations you ran in the session (0 is fine)"),
                                        new Field("worstDeviationBps", "number", "the largest deviation of any of them from the proposed mark, in bp"))),
                        new Condition("book-acceptance",
                                "You will mark your own collateral at this level for the period the fixing governs",
                                Evidence.server("acceptedAt is a timestamp not in the future",
                                        new Field("acceptedAt", "instant", "ISO-8601 time your book accepted the mark")))),
                false));

        // §2c — the only seat with observed prints for the WRAPPED asset, and the only
        // one whose claim the ledger itself checks.
        put(new Role("venue", "Venue", "The transaction data — the only observed prints for the wrapped asset",
                List.of(
                        new Condition("traded-range",
                                "The proposed mark lies within the high/low your own book traded in the window",
                                new Evidence(true, List.of(
                                        new Field("low", "number", "the low your book traded in the window"),
                                        new Field("high", "number", "the high your book traded in the window")),
                                        "low <= proposal <= high, enforced by ConfirmWithChecks on-ledger", "ledger")),
                        new Condition("spread-within-tolerance",
                                "Best bid/ask spread at the strike is inside the declared tolerance"),
                        new Condition("sufficient-volume",
                                "Traded volume in the window meets the declared minimum")),
                true));

        // §2d — tolerated at the very start of a pilot and required to be exited. Recorded
        // as its own role so the exception is visible on every fixing it touched rather
        // than quietly indistinguishable from a real seat.
        put(new Role("operator", "Operator (CrossDesk)",
                "The proposal itself — inputs, composition and arithmetic",
                List.of(
                        new Condition("inputs-published",
                                "Every input to the proposal is disclosed with it"),
                        new Condition("composition-reconciled",
                                "Units per share match the ledger at the strike instant")),
                false));
    }

    private static void put(Role r) {
        ROLES.put(r.key(), r);
    }

    /** Every seat, in the order the document presents them. */
    public static List<Role> roles() {
        return List.copyOf(ROLES.values());
    }

    public static Role role(String key) {
        return ROLES.get(key == null ? "" : key.trim().toLowerCase());
    }

    /** The default protocol reference stamped on an attestation for a given seat. */
    public static String refFor(String roleKey) {
        return VERSION + " " + (roleKey == null ? "" : roleKey.trim().toLowerCase());
    }

    /**
     * Validate an attestation against the seat it claims, returning the reason it is
     * refused or {@code null} if it stands.
     *
     * <p>Returns a REASON rather than throwing so the controller can render it as a 400
     * the signer can act on. A signer told "unknown condition: tradedrange" fixes it in
     * seconds; a signer told "invalid request" opens a ticket.
     */
    public static String rejectionReason(
            String roleKey, List<String> checksPassed, boolean hasLow, boolean hasHigh) {
        Role role = role(roleKey);
        if (role == null) {
            return "unknown signer role '" + roleKey + "'; expected one of " + ROLES.keySet();
        }
        if (checksPassed == null || checksPassed.isEmpty()) {
            // Also enforced on-ledger. Checked here too so the caller gets a readable
            // message instead of a Daml assertion failure surfaced through the API.
            return "a signer must name at least one condition it verified";
        }
        Set<String> known = new LinkedHashSet<>();
        for (Condition c : role.conditions()) known.add(c.name());

        Set<String> seen = new LinkedHashSet<>();
        for (String c : checksPassed) {
            String n = c == null ? "" : c.trim();
            if (!known.contains(n)) {
                return "condition '" + n + "' is not one the " + role.key()
                        + " seat verifies; expected any of " + known;
            }
            if (!seen.add(n)) {
                return "condition '" + n + "' was named twice";
            }
        }
        // A venue's range is the one claim checked against reality, on-ledger. Requiring
        // it here as well means the refusal arrives before the submission rather than as
        // a contract-level abort, and it stops a venue quietly attesting as though it
        // were a seat with nothing to observe.
        if (role.requiresObservedRange() && !(hasLow && hasHigh)) {
            return "the venue seat must supply both observedLow and observedHigh — "
                    + "the traded range is the only assertion the ledger checks";
        }
        if (!role.requiresObservedRange() && (hasLow || hasHigh)) {
            return "only the venue seat supplies an observed range";
        }
        return null;
    }
}
