# CrossDesk Signer Protocol

**Version 1 — 30 August 2026.** Administrator: CrossDesk. Companion to
`docs/FIXING_METHODOLOGY.md`, which is the rulebook for the fixing itself. This document is the
rulebook for the people who sign it.

> **Status.** The mechanism described in §3 is implemented and tested in `Governance.daml`
> (`ConfirmWithChecks`, `SignerCheck`, `ProposeWrappedFixing`) — see §7 for the per-section state.
> **No fixing has been published and no committee has been convened.** Nothing here is a claim
> that a seat is currently occupied.

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

**Escalation.** A checker that halts notifies the administrator with the failed condition named.
The administrator either restrikes with a corrected input, or the fixing does not happen (§5).

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
| §2 role definitions and named conditions | **documented here; not machine-checked.** `checksPassed` is free text by design (the protocol versions faster than the DAR) — the ledger records what was claimed, it does not verify the issuer's or lender's claims |
| §2c venue range enforcement | **implemented, tested** — `ConfirmWithChecks` refuses a price outside the range, an inverted range, or a half-specified one (`testSignerProtocolEvidence`) |
| §2 one attestation per member, signed by the confirming member | implemented, tested |
| §3 wrapper mark as an explicit field | **implemented, tested** — `ProposeWrappedFixing`, `wrapperConsistent` (`testWrapperMarkAttested`) |
| §3 evidence survives finalisation onto `NavFixing` | implemented, tested |
| §4 reference checker implementations | **not built** — each signer writes its own against its own systems |
| §5 fund behaviour when `K` is not reached | **not specified** — belongs to the fund's governing documents |
| §2d operator-exit rule | **policy only** — `role = "operator"` makes it visible; nothing enforces the exit |

**The honest summary:** the ledger enforces the venue's claim and the arithmetic of the wrapper
mark. It records, but cannot verify, the issuer's and the lender's claims — those rest on the
signer's own systems and on the fact that a false attestation is permanent, attributable, and
made against their own money. That asymmetry is deliberate and should be disclosed to anyone
taking a seat, not glossed.
