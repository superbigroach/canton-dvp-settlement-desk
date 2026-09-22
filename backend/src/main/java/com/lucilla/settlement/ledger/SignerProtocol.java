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
    // v2, 22 Sep 2026: reserve models per instrument; venue `no-prints-attested`;
    // custodian and transfer-agent seats. An attestation stamped v1 was made under the
    // three-seat protocol and must be read that way.
    public static final String VERSION = "SIGNER_PROTOCOL v2";

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

    /**
     * How an asset's backing is proven — which decides what the ISSUER seat can honestly assert.
     *
     * <p>WHY THIS EXISTS. The issuer conditions were written for cBTC: BTC held under an
     * attestor multisig with a periodic proof-of-reserve. Applied unchanged to every asset
     * they ask an issuer for numbers its model does not produce. cETH is locked in
     * protocol-controlled contracts verifiable at any block — there is no attestor quorum to
     * report. A tokenised equity is held by a custodian — again no attestors. Demanding
     * {@code quorumSigners} from either leaves the signer two options: fail to sign, or
     * invent a number. Inventing a number to satisfy a check is the exact failure this
     * protocol exists to prevent, so the protocol has to ask each model what it can prove.
     *
     * <p>The machine rules are unchanged: every condition below already exists and is checked
     * by {@link SignerEvidence} by name. What varies per model is WHICH conditions apply and
     * how they are worded to the signer.
     */
    public enum ReserveModel {
        /** Reserve held under an attestor set with a periodic proof-of-reserve. e.g. cBTC. */
        ATTESTED("attested"),
        /** Reserve locked in protocol-controlled contracts, verifiable on-chain at any block. e.g. cETH. */
        ONCHAIN_VERIFIABLE("onchain-verifiable"),
        /** Reserve held by a custodian who reports holdings. e.g. a tokenised equity. */
        CUSTODIAL("custodial");

        private final String wire;

        ReserveModel(String wire) {
            this.wire = wire;
        }

        public String wire() {
            return wire;
        }

        /** Unknown or absent ⇒ {@link #ATTESTED}, which is the strictest of the three. */
        public static ReserveModel fromWire(String s) {
            String w = s == null ? "" : s.trim();
            for (ReserveModel m : values()) {
                if (m.wire.equalsIgnoreCase(w)) return m;
            }
            return ATTESTED;
        }
    }

    private static final Map<String, Role> ROLES = new LinkedHashMap<>();
    private static final Map<ReserveModel, Role> ISSUER_BY_MODEL = new LinkedHashMap<>();

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
                                "Traded volume in the window meets the declared minimum"),
                        // §2c-bis — the thin-market case, which on Canton is the NORMAL case.
                        // Without this a venue with no prints can only refuse, so no fixing is
                        // struck on exactly the markets this product exists to serve. Attesting
                        // an ABSENCE is a truthful statement and a different act from both
                        // silence and refusal: it supports a fixing derived from other inputs,
                        // and the band widens because the venue confirmed nothing traded.
                        new Condition("no-prints-attested",
                                "Your book showed no trades in the window; you state the best bid/ask "
                                        + "at the strike and do not contradict the proposal",
                                Evidence.server(
                                        "bestBid <= proposal <= bestAsk when both are present; "
                                                + "mutually exclusive with traded-range",
                                        new Field("bestBid", "number", "best bid at the strike, or 0 if the book was empty"),
                                        new Field("bestAsk", "number", "best ask at the strike, or 0 if the book was empty")))),
                true));

        // §2e — the custodian. The rulebook's four-seat model has always specified this
        // seat; the code shipped without it because cBTC's reserve sits with an attestor
        // set rather than a custodian. A tokenised equity has no attestor set and no chain
        // to read: the only party who knows what is actually in the account is whoever
        // holds it. Without this seat a class-C committee cannot be assembled at all.
        put(new Role("custodian", "Custodian / reserve holder",
                "What is actually in the account — the only party who can see the holdings",
                List.of(
                        new Condition("holdings-current",
                                "Your most recent holdings statement is less than 24h old",
                                Evidence.server("statementAsOf within 24 hours of the confirmation",
                                        new Field("statementAsOf", "instant",
                                                "ISO-8601 time of your latest holdings statement"))),
                        new Condition("holdings-cover-supply",
                                "Holdings you actually custody are at least the issued supply",
                                Evidence.server("holdings >= supply",
                                        new Field("holdings", "number", "units you hold for the issuer"),
                                        new Field("supply", "number", "issued supply of the token"))),
                        new Condition("no-encumbrance",
                                "The holdings are unencumbered — not pledged, lent or rehypothecated",
                                Evidence.server("encumbered == 0",
                                        new Field("encumbered", "number",
                                                "units of the holdings pledged, lent or otherwise encumbered")))),
                false));

        // §2f — the administrator / transfer agent. It does the fund's accounting today and
        // is already liable for it, which is exactly why its signature carries information:
        // it is attesting to the share count the NAV is divided by.
        put(new Role("transfer-agent", "Administrator / transfer agent",
                "The share register — how many shares the NAV is divided by",
                List.of(
                        new Condition("shares-outstanding-reconciled",
                                "Shares outstanding on your register match the ledger at the strike instant",
                                Evidence.server("registerShares == ledgerShares",
                                        new Field("registerShares", "number", "shares outstanding on your register"),
                                        new Field("ledgerShares", "number", "shares outstanding on the ledger at the strike"))),
                        new Condition("fees-accrued",
                                "Accrued fees and liabilities deducted are those the fund's published schedule requires",
                                Evidence.server("accruedFees >= 0",
                                        new Field("accruedFees", "number", "fees and liabilities accrued to the strike instant")))),
                false));

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

        // ---- issuer variants, one per reserve model -----------------------------------
        // Same machine rules, same condition names, same SignerEvidence cases. What differs
        // is which conditions a model can honestly produce, and how each is worded.
        Condition coversSupply = new Condition("reserves-cover-supply",
                "Attested reserves are at least the circulating supply of the wrapped token",
                Evidence.server("reserves >= supply",
                        new Field("reserves", "number", "reserves backing the token, in units of the underlying"),
                        new Field("supply", "number", "circulating supply of the wrapped token")));
        Condition queueClear = new Condition("redemption-queue-clear",
                "No redemption request is unfilled beyond its stated window",
                Evidence.server("queueDepth <= maxQueueDepth",
                        new Field("queueDepth", "integer", "redemption requests currently unfilled"),
                        new Field("maxQueueDepth", "integer", "the depth the issuer's own window allows")));

        ISSUER_BY_MODEL.put(ReserveModel.ATTESTED, ROLES.get("issuer"));

        // No attestor set exists: the lock is readable on-chain, so what the issuer asserts
        // is the freshness of its own read, not the freshness of somebody's attestation.
        ISSUER_BY_MODEL.put(ReserveModel.ONCHAIN_VERIFIABLE, new Role(
                "issuer", "Issuer", "Whether the wrapper can actually be redeemed right now",
                List.of(
                        new Condition("reserves-current",
                                "Your on-chain verification of the locked reserve is less than 24h old",
                                Evidence.server("reservesAsOf within 24 hours of the confirmation",
                                        new Field("reservesAsOf", "instant",
                                                "ISO-8601 time you last verified the lock on-chain"))),
                        coversSupply,
                        queueClear),
                false));

        // A custodian reports holdings; there are no attestors and no chain to read.
        ISSUER_BY_MODEL.put(ReserveModel.CUSTODIAL, new Role(
                "issuer", "Issuer", "Whether the wrapper can actually be redeemed right now",
                List.of(
                        new Condition("reserves-current",
                                "The custodian's most recent holdings statement is less than 24h old",
                                Evidence.server("reservesAsOf within 24 hours of the confirmation",
                                        new Field("reservesAsOf", "instant",
                                                "ISO-8601 time of the custodian's latest holdings statement"))),
                        coversSupply,
                        queueClear),
                false));
    }

    private static void put(Role r) {
        ROLES.put(r.key(), r);
    }

    /** Every seat, in the order the document presents them. Issuer as for {@link ReserveModel#ATTESTED}. */
    public static List<Role> roles() {
        return List.copyOf(ROLES.values());
    }

    /**
     * Every seat, with the ISSUER seat as the given reserve model can honestly assert it.
     *
     * <p>This is what {@code GET /signer-protocol} should serve for a named instrument, and
     * what the portal renders its checkboxes from — so a signer is never shown a box whose
     * number its model does not produce.
     */
    public static List<Role> rolesFor(ReserveModel model) {
        Role issuer = issuerFor(model);
        List<Role> out = new java.util.ArrayList<>();
        for (Role r : ROLES.values()) {
            out.add("issuer".equals(r.key()) ? issuer : r);
        }
        return List.copyOf(out);
    }

    /** The ISSUER seat for a reserve model. Never null; unknown models fall back to ATTESTED. */
    public static Role issuerFor(ReserveModel model) {
        Role r = ISSUER_BY_MODEL.get(model == null ? ReserveModel.ATTESTED : model);
        return r == null ? ROLES.get("issuer") : r;
    }

    public static Role role(String key) {
        return ROLES.get(key == null ? "" : key.trim().toLowerCase());
    }

    // ---- which reserve model each instrument uses ---------------------------------
    // Seeded from `signer.reserve-models` in application.yml (instrument id -> wire name)
    // so onboarding an asset does not need a code change. Unknown instruments are
    // ATTESTED, the strictest profile: an asset we know nothing about is not quietly
    // granted the easier checklist.
    private static final Map<String, ReserveModel> MODEL_BY_INSTRUMENT = new LinkedHashMap<>();

    /** Replace the instrument→model registry. Called once at startup from configuration. */
    public static synchronized void configureReserveModels(Map<String, String> config) {
        MODEL_BY_INSTRUMENT.clear();
        if (config == null) return;
        config.forEach((instrument, wire) -> {
            if (instrument == null || instrument.isBlank()) return;
            MODEL_BY_INSTRUMENT.put(instrument.trim().toUpperCase(), ReserveModel.fromWire(wire));
        });
    }

    /** The reserve model for an instrument; {@link ReserveModel#ATTESTED} if not configured. */
    public static ReserveModel reserveModelOf(String instrumentId) {
        if (instrumentId == null || instrumentId.isBlank()) return ReserveModel.ATTESTED;
        return MODEL_BY_INSTRUMENT.getOrDefault(instrumentId.trim().toUpperCase(), ReserveModel.ATTESTED);
    }

    /** Every configured instrument and its model, for diagnostics and the API. */
    public static Map<String, ReserveModel> reserveModels() {
        return Map.copyOf(MODEL_BY_INSTRUMENT);
    }

    /**
     * The seat a signer is held to for a specific asset.
     *
     * <p>Use this, not {@link #role(String)}, wherever an instrument is in scope: it is the
     * difference between refusing an onRails confirm for lacking {@code quorumSigners} and
     * accepting it for the three conditions cETH can actually prove.
     */
    public static Role roleFor(String key, ReserveModel model) {
        Role base = role(key);
        if (base == null) return null;
        return "issuer".equals(base.key()) ? issuerFor(model) : base;
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
        return rejectionReason(roleKey, checksPassed, hasLow, hasHigh, null);
    }

    /**
     * As above, but held to the seat as it applies to a named instrument.
     *
     * <p>Pass the instrument wherever it is known. An issuer whose asset is backed by an
     * on-chain lock is refused for {@code attestor-quorum} — a condition its model cannot
     * produce — rather than being asked for a number it would have to invent.
     */
    public static String rejectionReason(
            String roleKey, List<String> checksPassed, boolean hasLow, boolean hasHigh,
            String instrumentId) {
        Role role = roleFor(roleKey, reserveModelOf(instrumentId));
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
        boolean claimsNoPrints = seen.contains("no-prints-attested");
        if (claimsNoPrints && seen.contains("traded-range")) {
            return "a venue cannot claim both 'traded-range' and 'no-prints-attested' — "
                    + "either the book traded in the window or it did not";
        }
        if (claimsNoPrints && (seen.contains("sufficient-volume") || seen.contains("spread-within-tolerance"))) {
            return "'no-prints-attested' is the whole claim when nothing traded; "
                    + "volume and spread conditions cannot also be asserted";
        }
        // A venue's range is the one claim checked against reality, on-ledger — required
        // UNLESS the venue is attesting that there was nothing to range over, which is a
        // statement about an empty window rather than an absent observation.
        if (role.requiresObservedRange() && !claimsNoPrints && !(hasLow && hasHigh)) {
            return "the venue seat must supply both observedLow and observedHigh — "
                    + "the traded range is the only assertion the ledger checks. "
                    + "If your book showed no trades in the window, claim 'no-prints-attested' instead";
        }
        if (claimsNoPrints && (hasLow || hasHigh)) {
            return "'no-prints-attested' means the window had no trades; do not also send an observed range";
        }
        if (!role.requiresObservedRange() && (hasLow || hasHigh)) {
            return "only the venue seat supplies an observed range";
        }
        // A fixing supported only by an attested absence is weaker than one with prints.
        // The caller records that on the fixing so the band can widen; see the rulebook §5.4.
        return null;
    }
}
