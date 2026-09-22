# ETP Foundry Signer Protocol

**Version 2 — 22 September 2026** (v1 30 August 2026). Administrator: ETP Foundry
(formerly CrossDesk). Wire value: `SIGNER_PROTOCOL v2`, served by `GET /api/signer-protocol`. Companion to
`docs/FIXING_METHODOLOGY.md`, which is the rulebook for the fixing itself. This document is the
rulebook for the people who sign it.

> **Status.** The mechanism described in §3 is implemented and tested in `Governance.daml`
> (`ConfirmWithChecks`, `SignerCheck`, `ProposeWrappedFixing`) — see §7 for the per-section state.
> **No fixing has been published and no committee has been convened.** Nothing here is a claim
> that a seat is currently occupied.
>
> **What changed in v2 (22 Sep 2026):** two new seats (`custodian`, `transfer-agent`); the venue
> may attest the *absence* of prints (`no-prints-attested`) instead of halting; each instrument
> declares a **reserve model** (`attested` · `onchain-verifiable` · `custodial`) that selects
> which issuer conditions apply — `GET /api/signer-protocol?instrument=…`; a bare tick is refused
> for every seat except the venue; and the published series recognises a fixing only if a real
> committee could have produced it (§7, last rows). See §8 for what is still not enforced on-ledger.

---

## 1. The one rule this document exists to enforce

> **No signer is ever asked for an opinion about the price.
> Each signer asserts a fact only it can see.**

Everything else follows from that sentence, so it is worth being explicit about why.

A committee whose members are each asked *"do you agree this is the right price?"* is a committee
that rubber-stamps. Not from bad faith — from economics. Under `FIXING_METHODOLOGY.md` §7 the
signers **are not paid**; attestation is a by-product of a position they already hold. An
unpaid member asked for a daily act of judgement will, within a fortnight, click yes. The
signature then means nothing, and a signature that means nothing is worse than no signature,
because it looks like governance.

So the question put to each member is narrowed until it is nearly free to answer:

| | Asking for judgement | Asking for a fact |
|---|---|---|
| Cost to the signer | High — requires forming a view | Near zero — a query against their own systems |
| Can it be automated? | No | **Yes**, and it should be (§4) |
| What refusal means | "I disagree" — unactionable | **"Attestor quorum was 5 of 10"** — actionable |
| Failure mode | Rubber-stamping | The check fails and the fixing stops |

**Two properties follow, and they are the whole design:**

1. **Signing is cheap.** That is what makes an unpaid committee operable at all.
2. **Refusal is specific.** A refusal that names a condition tells the administrator, the fund
   and every other signer exactly what broke. A vote of no confidence tells them nothing.

### What this is not

It is **not** a claim that CrossDesk's committee resembles a regulated oversight function. Under
IOSCO's benchmark principles and the UK/EU Benchmarks Regulation, an administrator's oversight
committee is composed to be **independent of parties with positions**. This design does the
opposite deliberately: it seats parties *because* they have exposure.

That is a real and defensible difference, and it will be challenged. See §6, which states the
challenge in its strongest form and answers it. Do not pitch this design without having read §6.

---

## 2. The seats, and what each one asserts

A fixing needs `K` of `N`. `FIXING_METHODOLOGY.md` §7 requires that no single interest holds `K`.
The composition below satisfies that by construction, because the three roles **want different
answers**:

- the **issuer** wants the wrapper marked at par — it makes its asset look sound;
- the **lender** wants it marked conservatively — it is the one under-collateralised if the mark
  is too high;
- the **venue** wants it marked where the asset actually traded — it quotes there.

A committee of three issuers is not a committee. It is an expensive way for issuers to bless
their own valuation, and any risk officer sees through it in one meeting.

### 2a. Issuer — *redemption integrity*

**Who:** the party that issues the wrapped asset. For cBTC, BitSafe. For cETH, onRails.

**What it uniquely knows:** whether the wrapper can actually be redeemed right now. Nobody
outside the issuer can see the attestor set's health or the redemption queue.

| Condition | Name in `checksPassed` | Pass when |
|---|---|---|
| Attestor quorum | `attestor-quorum` | At least the issuer's own threshold of attestors are online and signing (e.g. 7 of 10) |
| Reserves current | `reserves-current` | The most recent proof-of-reserve attestation is less than 24h old |
| Reserves sufficient | `reserves-cover-supply` | Attested reserves ≥ circulating supply of the wrapped token |
| Redemption queue clear | `redemption-queue-clear` | No redemption request is unfilled beyond its stated window |

**If all four pass**, the issuer may attest `wrapperFactor = 1.0`.
**If any fails**, the issuer does not sign at par. It either declines, or the proposer restrikes
with a factor below par and a rationale naming the failed condition.

> This is the seat that makes the product exist. `wrapperFactor` is the only field in the whole
> system that no benchmark administrator anywhere produces, and the issuer is the only party
> with the facts to justify it.

### 2b. Lender — *the mark is safe to lend against*

**Who:** a platform holding the asset as collateral. On Canton today: ACME Lend, Alpend, Haven
Digital Partners, Verity.

**What it uniquely knows:** whether it will actually carry this number on its own book. This is
the strongest signature in the protocol, because it is the only one where the signer is asserting
something **against its own money**.

| Condition | Name in `checksPassed` | Pass when |
|---|---|---|
| Independent mark agrees | `independent-mark-within-tolerance` | The proposed mark is within the lender's declared tolerance (recommended: 25bp) of its own valuation |
| Liquidations consistent | `liquidations-consistent` | No liquidation the lender ran in the session cleared materially away from the proposed mark |
| Book acceptance | `book-acceptance` | The lender will mark its own collateral at this level for the period the fixing governs |

`book-acceptance` is the one that carries the weight. A lender that signs it and then marks its
own book somewhere else has made a false statement, on-ledger, with its own signature on it.

### 2c. Venue — *the mark sits where the asset traded*

**Who:** a venue where the wrapped asset actually trades. Cantex, Cantor8, or any venue on the
issuer's own ecosystem list.

**What it uniquely knows:** the transaction data. This is the only seat with observed prints for
the wrapped asset — the underlying benchmark does not price the wrapper, so without a venue there
is no market evidence in the room at all.

| Condition | Name in `checksPassed` | Pass when |
|---|---|---|
| Traded range | `traded-range` | The proposed mark lies within the high/low the venue's own book traded in the window |
| Spread | `spread-within-tolerance` | Best bid/ask spread at the strike is inside the declared tolerance |
| Sufficient volume | `sufficient-volume` | Traded volume in the window meets the declared minimum, else the venue has no basis to attest |

A venue supplying `observedLow` and `observedHigh` is **enforced on-ledger**: `ConfirmWithChecks`
refuses an attestation whose range does not contain the price, refuses an inverted range, and
refuses a half-specified one. A venue therefore *cannot* sign a price its own book never printed.

> This is the sharpest guard in the system and the most informative refusal available. It is the
> one place where the protocol is not a policy document but a rule the ledger enforces.

### 2d. Operator (CrossDesk) — *proposes, and should not sign*

CrossDesk computes the proposal: the benchmark print, the units per share off the ledger, the
accrual inputs, and the resulting NAV. It publishes all inputs with the proposal.

**CrossDesk should not be a signer.** `FIXING_METHODOLOGY.md` §7 already states the hard version:
*the administrator does not trade the instruments it prices.* Signing is the adjacent conflict.
Where a pilot has only three available seats, CrossDesk signing is tolerable at the very start
and **must be exited as soon as a fourth party exists** — recorded with `role = "operator"` so
that the exception is visible on every fixing it touched rather than forgotten.

---

## 3. The mechanism

```
CME CF BRR / index print        ← free public input, CrossDesk does not build it
        ↓  (oracle transports it on-ledger — a technical claim, not a seat)
ProposeWrappedFixing            ← benchmarkPrice × parFactor = the struck price
        ↓
ConfirmWithChecks × K           ← each member names the conditions it verified
        ↓
FinalizeFixing → NavFixing      ← signatory set IS the attestor set
        ↓
Create / redeem settles atomically against it
```

**Why the oracle is not a seat.** An oracle asserts *"this is faithfully the benchmark print."*
That is a transport claim. It has no money riding on the answer, so its vote carries no
information, and it dilutes the opposed-interests property in §2. It also structurally cannot
answer the question the committee exists for: when nobody quotes cBTC, an oracle has nothing to
relay. Only a party with exposure can say what the wrapper is worth. An oracle's commercial
relationship to CrossDesk is **redistribution**, not attestation.

**What lands on the ledger.** Each `SignerCheck` carries the member, its role, the protocol
version it applied, the named conditions it verified, and (venue only) the observed range. The
finished `NavFixing` carries the list. That is the difference between a signature count and an
oversight record: the fixing answers *why*, not only *who*.

---

## 4. Automating a seat

**Every check in §2 is a query against the signer's own systems.** None requires a human to form
a view. A signer should run a checker that confirms automatically when all its conditions pass
and **halts and escalates when one does not**.

This is not a convenience. An unpaid committee that requires daily human attention will decay
into rubber-stamping within weeks, and the protocol's whole value is destroyed at that point. A
seat that is automated is a seat that is still honest in month six.

**A checker must never auto-confirm on a failed condition, and must never widen its own
tolerances to make a check pass.** Both are silent conversions of this protocol back into a
rubber stamp, and both are invisible on the ledger because the signature looks identical.

**Reference implementation.** [`signer-service/README.md`](../signer-service/README.md) is a
checker that does exactly this: it reads each condition from a data source the signer declares
(a value, an HTTP endpoint, or a command against its own systems), confirms with evidence when
all pass, refuses naming the condition when one fails, and halts when one cannot be evaluated.

**Escalation.** A checker that halts notifies the administrator with the failed condition named.
The administrator either restrikes with a corrected input, or the fixing does not happen (§5).

---

## 4a. Holding a seat: accounts, credentials, and key custody

### How a seat is issued

**A seat is issued, never self-registered.** The roster is seeded from `users.yml` and
persisted to `users.json`; the administrator adds a signer, which mints the account and binds
it to a Canton party. Open registration would let anyone self-declare as a lender, and the
value of `K`-of-`N` is entirely in *who* the `N` are — §2 is meaningless if the seats are
self-asserted.

Each row carries: uid · email · Canton party · seat role · declared tolerances · API key hash.

### Three ways to act on a seat

| Mode | Credential | Use |
|---|---|---|
| **Portal** | Firebase identity token | A human reviewing and confirming in the signer portal. Checkboxes render from `GET /signer-protocol`, so a box on screen is always a box the API accepts |
| **Programmatic** | API key, `ck_` + 32 random bytes, **SHA-256 hashed at rest, shown once** | The signer's own checker (`signer-service`) on their infrastructure |
| **Sandbox** | `X-Sandbox-User:` header | Evaluating the protocol with no account and nothing at stake |

A signer credential may **only** confirm or refuse fixings for instruments its seat covers.
It cannot move assets, propose a fixing, or act as another seat.

### Key custody — the trust ladder

A signature is worth exactly what it costs to forge. **Every value states the level each
signature was made at**; the fixing record shows the mix, e.g. `K=3 of N=5 — L1:2, L2:1`.

| Level | Signing key | Administrator could forge? | Signer cost |
|---|---|---|---|
| **L1 — hosted party** | On the administrator's participant; the API key authorises and the administrator exercises the choice as the signer's party | **Yes, technically** | Zero |
| **L2 — external signing** | Held by the signer off-ledger; the administrator submits an already-signed transaction | No | Moderate |
| **L3 — own participant** | On the signer's own Canton participant | No | High |

**L1 is honest for a pilot and dishonest as a destination.** It is the right level for a free
shadow run or a design partnership, where nothing settles against the value. It is **not**
acceptable for an OFFICIAL fixing a third party settles against: a lender whose liquidation
engine consumes the mark will ask what stops the administrator signing on its behalf, and at
L1 the true answer is "a scoped API key the administrator issued and controls."

**Target state: every signer at L2 or above before the first OFFICIAL fixing is used by a
third party.**

This is the concession that belongs beside §6. The composition argument answers *"you seated
interested parties."* It does not answer *"you hold their keys."* Only the ladder does.

### Administrator impersonation

The administrator can assume a mapped user's identity for a single request — an operational
necessity for support. Every such act is recorded. **The capability is disclosed to every
signer at onboarding, and is never used to confirm a fixing on a signer's behalf.**

---

## 5. When `K` is not reached

No `NavFixing` exists. `RunClose` has nothing to assert against, so no auction prints, and
`navPerShare` returns `None` rather than a guess — `FIXING_METHODOLOGY.md` §5: *a gap is
published as a gap.*

**What the fund does then is not yet specified, and it must be before any fund relies on this.**
The options are the ordinary ones — suspend creations and redemptions for the session, or invoke
a declared fair-value procedure — and the choice belongs to the fund's own governing documents,
not to the administrator. It is listed as an open item in §7 because a fund issuer will ask, and
"we haven't decided" is a worse answer than a documented suspension.

---

## 6. The strongest objection, stated fairly

> *"You have seated people with positions and called it oversight. That is LIBOR."*

**Take it seriously; it is the best argument against this design.** LIBOR's panel banks submitted
rates on instruments they held positions in, and some of them moved submissions to suit those
positions.

Three differences, and they are structural rather than rhetorical:

1. **The panel is composed to disagree.** LIBOR's submitters shared a direction of interest.
   Here the issuer wants par, the lender wants conservative, the venue wants observed — and
   `FIXING_METHODOLOGY.md` §7 requires that no single interest holds `K`.
2. **Submissions are verifiable rather than asserted.** A LIBOR submission was an unfalsifiable
   estimate of where a bank *could* borrow. Every check in §2 is a fact with a record behind it,
   and the venue's is checked by the ledger itself.
3. **Every signature is permanent and attributable.** LIBOR ran on phone calls and unlogged
   discretion. Here who signed which fixing, under which protocol version, having verified which
   conditions, is on the ledger forever.

**What honesty requires us to concede:** this is a *mitigated* conflict, not an absent one. A
regulated administrator would seat independent members and manage conflicts by exclusion. This
design cannot — a panel of disinterested referees would never be assembled or funded for an
asset this size — so it manages conflict by **opposition and evidence** instead.

An EU- or UK-supervised entity referencing a CrossDesk fixing must resolve
`FIXING_METHODOLOGY.md` §11 first. This section does not substitute for that.

---

## 7. Implementation state, stated plainly

| Section | Status |
|---|---|
| §2 role definitions and named conditions | **implemented, tested at the edge** — `SignerProtocol.java` is this document as data. `POST /fixing/{cid}/confirm-checked` refuses a condition that does not belong to the declared seat, an unknown role, an empty or repeated checklist, a venue without a range, and a non-venue with one. `checksPassed` stays free text *on-ledger* by design (the protocol versions faster than the DAR), so the constraint lives where it can be versioned with this document |
| §2 the conditions a signer is shown | **implemented** — `GET /signer-protocol` serves the same list the validator uses, and the desk UI renders its checkboxes from it rather than a local copy, so a box on screen is a box the API accepts |
| §2a / §2b issuer and lender evidence, checked before submission | **implemented, tested (2 Sep 2026)** — `POST /api/proposals/{cid}/confirm` no longer accepts a tick from these seats. Each checked condition needs its numbers, in the shape `GET /api/signer-protocol` publishes under `evidence` per condition, and `SignerEvidence` applies the rule server-side before the on-ledger confirm: `attestor-quorum` `{quorumSigners, quorumThreshold}` with signers ≥ threshold; `reserves-current` `{reservesAsOf}` within 24h; `reserves-cover-supply` `{reserves, supply}` with reserves ≥ supply; `redemption-queue-clear` `{queueDepth, maxQueueDepth}`; `independent-mark-within-tolerance` `{independentMark}` with \|mark − proposal\| / proposal ≤ the lender's declared `tolerances.markBps` (default 25 bp); `liquidations-consistent` `{liquidationsToday, worstDeviationBps}` ≤ `tolerances.liquidationBps`; `book-acceptance` `{acceptedAt}`. A missing block or a failing number is a **422** naming the number and carrying the schema; a pass is recorded on the `proposal.confirmed` event as `verified: true` with the numbers and what was derived (deviation in bp, age in hours). The operator desk's legacy `/fixing/{cid}/confirm-checked` still takes a tick and records `verified: false` |
| §2c venue range enforcement | **implemented, tested** — `ConfirmWithChecks` refuses a price outside the range, an inverted range, or a half-specified one (`testSignerProtocolEvidence`). The portal path for the venue is unchanged: `evidence: {low, high}` |
| §2 one attestation per member, signed by the confirming member | implemented, tested |
| §3 wrapper mark as an explicit field | **implemented, tested** — `ProposeWrappedFixing`, `wrapperConsistent` (`testWrapperMarkAttested`) |
| §3 evidence survives finalisation onto `NavFixing` | implemented, tested |
| §4 reference checker implementations | **not built** — each signer writes its own against its own systems. What a checker must send is now machine-readable (`evidence` per condition in `GET /api/signer-protocol`), so a checker is a query against the signer's systems plus one POST |
| §4 escalation when a seat is silent | **implemented, tested (2 Sep 2026)** — tier 2 of the fallback waterfall runs *inside* the window: at half the window every seat that has not confirmed gets a `proposal.reminder` webhook and event with `escalation: 1`; at three quarters the same seats get `escalation: 2` and the `alternates` configured per seat on `/api/admin/schedule` are brought in (an alternate who is not a committee member on-ledger is named in the event and skipped). Only after the window closes do tiers 3–5 run. On by default |
| §5 fund behaviour when `K` is not reached | **not specified** — belongs to the fund's governing documents |
| §2d operator-exit rule | **policy only** — `role = "operator"` makes it visible; nothing enforces the exit |
| **v2** custodian and transfer-agent seats | **implemented, deployed 22 Sep 2026** — `custodian`: `holdings-current` (statement age ≤ `freshnessHours`), `holdings-cover-supply`, `no-encumbrance`; `transfer-agent`: `shares-outstanding-reconciled`, `fees-accrued`. Same evidence-or-422 path as issuer/lender |
| **v2** venue `no-prints-attested` | **implemented, deployed** — a venue with no prints in the window attests that fact with its best bid/ask instead of leaving the seat silent; mutually exclusive with `traded-range` on the same proposal |
| **v2** reserve model per instrument | **implemented, deployed** — `signer.reserve-models` (`RESERVE_MODEL_CBTC`, `RESERVE_MODEL_CETH` env) → `SignerProtocol.reserveModelOf(instrument)`; the confirm path refuses a condition that does not belong to the seat *under that model*. ⚠️ Portal schemas and the 422 body still advertise the strict profile — the model is applied at confirm time only (audit medium #5) |
| **v2** no bare ticks | **implemented, deployed** — `confirm-checked` refuses any non-venue seat that omits the evidence block; `verified: false` attestations can no longer be created from the desk |
| **v2** committee recognition on publish | **implemented, deployed** — `SeriesService.recognised()` drops any `NavFixing` whose admin/threshold/attestors are not matched by a real `OperatorCommittee` visible to the auditor, and logs it as a possible forgery. This is a *consumer-side* defence; see §8 |
| **v2** identity | **deployed** — hosted host runs `AUTH_MODE=firebase`; only e-mail-verified Firebase users map to a roster row; request paths are normalised before the filter classifies them |

**The honest summary, in three layers:**

1. **The ledger enforces** the venue's traded range and the arithmetic of the wrapper mark. A
   price outside the range, or a factor that does not reconcile, cannot exist on-chain.
2. **The API enforces** that a claimed condition belongs to the seat claiming it, and — for the
   issuer and the lender — that the numbers behind it were supplied and pass the rule stated in
   §2 before anything is submitted. A lender cannot file the issuer's evidence, and neither can
   confirm with a tick.
3. **Nothing enforces** the provenance of those numbers: the desk cannot query the issuer's
   attestor set or the lender's book, so it checks what the signer reports, not what is true.
   That rests on the signer's own systems and on the fact that a false number is permanent,
   attributable, and made against their own money.

Layer 3 is the residual trust in this design. It is deliberate — the alternative is auditing every
signer's internal systems daily, which is the disinterested-referee model that cannot be funded —
and it should be disclosed to anyone taking a seat rather than glossed. What the evidence rule
changes is the *shape* of a false attestation: it is no longer a box that was ticked, it is a
number that was typed, and a number can be checked after the fact.

---

## 8. What the ledger does not yet enforce — read before quoting §3

The sentence in §3, *"the signatory set IS the attestor set, so the quorum is provable from the
ledger rather than asserted in a PDF"*, is true **and incomplete**. The 22 September 2026 audit
found that on the 2.x package:

- `FixingProposal` is `signatory approvers` and `NavFixing` is `signatory attestors`, but `admin`,
  `threshold` and the member list are **plain fields**. Nothing binds them to an
  `OperatorCommittee`. A party can therefore create a proposal naming the real administrator with
  `threshold = 1` and itself as the only approver, finalise it, and hold a contract that reads as
  "committee-attested" — K-of-K for a K the forger chose.
- `ConfirmWithChecks` enforces the venue range **only when both bounds are supplied**; a venue that
  sends none is silently skipped and its signature still counts. Plain `Confirm` bypasses evidence.
- Two fixings for the same instrument, session and day can coexist; "newest" is a consumer
  convention, not a ledger fact.

**Today's mitigation** is off-ledger: `SeriesService.recognised()` publishes only fixings a real
committee could have produced. That protects the public series and every consumer that reads it
through this API. It does **not** protect a consumer that reads the ledger directly.

**The fix** is package **3.0.0** — `signatory admin :: approvers` on the proposal and the fixing,
`ensure threshold >= 2 && threshold < length members && admin notElem members` on the committee,
venue range mandatory, plain `Confirm` retired, an `asOfDate` slot contract for uniqueness, and
members as observers of `NavFixing` so a non-attesting member can open a restatement. A signatory
change is not a compatible upgrade, so this is a new package; there is nothing to migrate because
no fixing exists. **Until 3.0.0 is on the participant, §3 is quoted with this section attached.**
