# CrossDesk Fixing Methodology

**Version 0.1 — draft, 12 August 2026.** Administrator: CrossDesk (Sebastian Borjas).

This document is the rulebook for a CrossDesk fixing. It exists because a number nobody can
audit is not a benchmark: a benchmark is a **rule, a clock and a record**. Anyone referencing a
CrossDesk fixing in a contract is referencing this document.

> **Status.** §§2–7 and 9 describe behaviour that is implemented and tested in `Governance.daml`,
> `MarketOnClose.daml` and `Basket.daml` — including §3's minimum quality conditions and §6's
> restatement quorum. **§8 (cessation) is a process, not code**, and §4's scheduled strike is not
> built; see §12 for the per-section state. Nothing here should be read as a claim that a fixing is
> currently in production: no fixing has been published, and the participant the desk ran on is
> offline.

---

## 1. Identification

| Field | Value |
|---|---|
| Benchmark family | `CDX` |
| Identifier form | `CDX-<INSTRUMENT>-<FREQ>` — e.g. `CDX-CBTC-D` (daily), `CDX-LX1-D` |
| Administrator | CrossDesk |
| Currency of quotation | The instrument's `cashInstrument` (e.g. USDC) |
| Ledger | Canton. Every fixing is a `NavFixing` contract, signed on-ledger |

An instrument has **at most one** fixing per identifier per strike. A second strike for the same
identifier and date is a **restatement** (§6), never a second fixing.

## 2. What the fixing measures

The price at which the instrument is deemed to transact for settlement purposes at the strike
time. It is **not** an estimate of fair value, a prediction, or an indication of where the
instrument may trade next. It is the number contracts settle against.

Two numbers are published side by side and must never be confused:

| | Meaning | Binding? |
|---|---|---|
| **Official fixing** | Produced by §3. Creation and redemption settle at this number | **Yes** |
| **Indicative value** | Derived continuously from live market data | **No.** Informational only |

The difference between them, in basis points, is published with the fixing. It is the honest
measure of how stale the last strike has become.

## 3. Calculation — the waterfall

Applied in order. The first tier that produces a value **is** the fixing, and the tier used is
published with it.

### Tier 1 — Sealed auction uniform price (preferred)
Where an auction session is held for the instrument, the fixing is the **uniform clearing price**
at which the auction uncrosses, determined from the sealed orders themselves. All executions at
that strike occur at that single price. Unpriced market-on-close orders are allocated first; priced
orders through the print fill in full, in price priority; only the marginal at-the-print level is
rationed, earliest-submitted first — at most one order is ever partially filled.

**Minimum quality conditions.** The auction price is used only if, at uncrossing:
- at least **two** orders rest on the book, and
- they originate from at least **two distinct parties**, and
- the crossed quantity is greater than zero.

If any condition fails, the auction is declared **uncrossed** and the waterfall proceeds to
Tier 2. A single participant cannot set a CrossDesk fixing.

### Tier 2 — Committee attestation
A `K`-of-`N` `OperatorCommittee` attests the value. Critically, the committee signs a **recipe,
not a number**:

```
value(t) = base × (1 + rate × daysBetween(asOf, t) / dayCountBasis)
```

so the ledger *derives* the value continuously after the strike rather than holding a number that
goes stale. `base`, `rate`, `asOf` and the day-count basis are all signed. For a non-accruing
instrument, `rate = 0` and the recipe degenerates to a constant.

For a **basket**, the fixing is `Σ (unitsPerShare_i × fixing_i)` over the components. A basket
fixing therefore requires a current fixing for **every** component; if one is missing the basket
fixing is not published (§5).

### Tier 3 — Carry forward
If neither tier is available, the prior fixing's recipe continues to derive a value and the
fixing is published **flagged as carried forward**, with the age of the underlying strike. Three
consecutive carried-forward strikes trigger a review under §8.

## 4. Strike time and schedule

- **Frequency:** every business day, unless the instrument's terms state otherwise.
- **Strike time:** declared per identifier at launch (e.g. 16:00 Europe/London) and **fixed**. It
  may only change under §9.
- **Business days:** the calendar declared per identifier. Where a component's own market is
  closed, Tier 2 or Tier 3 applies to that component.
- **Publication:** as soon as the strike completes. There is no embargo.
- **Ledger latency is not a delay in the fixing.** The strike time is the moment the auction
  closes or the committee's quorum is reached, not the moment the transaction commits.

## 4a. Strike frequency when the asset trades 24/7 — and when its market is shut

The question every tokenised-fund conversation reaches: *if it trades around the clock, do you need
an hourly NAV?* **No.** This section is the reasoning, because it is asked more often than it is
answered well.

### The rule

**Strike frequency follows SETTLEMENT, not trading.**

An ETF trades continuously all day and has exactly **one** official NAV. Its intraday price comes
from the market anchored by arbitrage, not from recomputing NAV; what the exchange disseminates
every ~15 seconds is the **indicative** value, binding on nobody. Creation and redemption settle
once, at one struck price.

The empirical answer for a 24/7 asset already exists: **Bitcoin never stops trading, and CME settles
every future and option on ONE daily fixing.** The most sophisticated venue pricing the most
continuously-traded asset chose once a day. Frequency is a settlement decision.

### Why more strikes are worse, not better

1. **Every strike costs a quorum.** Hourly is 24 K-of-N quorums a day. The `N` are parties with an
   existing commercial interest (§7), not staff — attestation is a by-product of a position they
   already hold, and it does not scale to hourly.
2. **More strikes do not make the settling price more accurate.** They multiply the number of
   published prices someone can dispute.
3. **Manipulation surface grows with every fixing.** Benchmarks use few, well-defined strikes
   deliberately.
4. **A contract needs one referenceable moment.** A margin agreement names *"the 16:00 fixing"*, not
   *"whichever of twenty-four."*

**The one genuine case for more frequency:** creation and redemption that must settle intraday. Some
money-market funds strike several times a day for exactly that reason. Follow settlement — a
customer with three settlement windows gets three strikes, declared per §4 and fixed.

### How the other 23 hours are covered — three mechanisms, not one

| Between strikes | Covered by | Applies to |
|---|---|---|
| **Derivation from the signed recipe** | §3 Tier 2 — base, rate, day-count are attested, so the ledger derives a value at every instant | Accruing assets: T-bills, money market, anything with a yield |
| **Indicative value** | §2 — continuous, from live market data, **binding on nobody** | Volatile assets with an open market: cBTC, cETH |
| **Committee attestation** | §3 Tier 2 again — but as the *primary* source, not a fallback | **Assets whose market is closed** — see below |

This is why the committee signs a **recipe rather than a number**. A recipe extrapolates a T-bill; it
cannot extrapolate Bitcoin. So a fund holding money-market assets has a live value at 03:00 on a
Sunday with no strike at all, while a fund holding volatile assets relies on the official/indicative
split and on arbitrage to close the gap.

### 🟢 The closed-market case — the strongest argument this document contains

**What is the NAV of a tokenised equity fund at 03:00 on a Sunday?**

If tokenised stocks and stock funds trade 24/7, their underlying market does not. Overnight, at
weekends and on holidays there is **no price for the constituents**. Not a stale price — no price.

**No oracle can solve this.** A price feed relays a number that exists; when the underlying market is
shut there is nothing to relay. Chainlink, RedStone and any data standard are transport, and
transport has nothing to carry.

Somebody must **attest** a value instead of reporting one — a K-of-N committee signing a defensible
mark, with the method published in advance and every signature attributable. That is not an
adaptation of this design; it is the case the design was built for, and the one place where
attestation is strictly better than a feed rather than merely different.

It also points where the money is. Per `docs/PROJECT_CONTEXT.md` §8: the market pays **0.325 bps**
for valuing an easy asset and **6–12 bps** where the mark is genuinely argued about. *"The market is
closed and someone still has to settle"* is the definition of a doubted mark.

### Talking points, compressed

- *"Frequency follows settlement, not trading. An ETF trades all day on one NAV."*
- *"Bitcoin trades 24/7 and CME still settles on one daily fixing. That is the answer to whether you
  need hourly."*
- *"We sign a recipe, not a number, so the ledger derives the value between strikes. Hourly strikes
  solve a problem we do not have."*
- *"Hourly would mean twenty-four quorums a day. Nobody staffs that, and every extra fixing is
  another number to dispute and another moment to push."*
- *"If you need to settle intraday, you get intraday strikes — declared and fixed, not ad hoc."*
- **The closer:** *"When a tokenised stock fund trades at 3am Sunday, no oracle can price it, because
  its market is closed and there is nothing to relay. Someone has to attest a value. That is what a
  K-of-N committee is for, and it is the one job a price feed structurally cannot do."*

## 5. Inputs and their treatment

- **Tier 1 inputs** are sealed orders on the ledger. They are visible to no other participant
  while they rest — including the auditor — and no participant sees the book before the
  uncrossing. This is enforced by the ledger's signatory/observer model, not by an API filter.
- **Tier 2 inputs** are the recipe fields signed by the committee.
- **Indicative value inputs** are external market data (currently Coinbase spot). External data
  is **never** an input to an official fixing. It informs the indicative value only.
- **A wrapped asset's mark is two inputs, not one.** Where the instrument is a claim on an asset
  priced elsewhere (cBTC on BTC, cETH on ETH), the fixing carries the **benchmark print**
  (`referencePrice`) and the **par ratio** the committee attested (`wrapperFactor`) as separate
  signed fields, and the struck price is their product. The benchmark is an input nobody argues
  about; the ratio is the judgement, and it is the one number no external administrator produces.
  A pair that does not reconcile cannot exist on-ledger. See `docs/SIGNER_PROTOCOL.md` §2a.
- **Incomplete inputs:** if a required component has no current fixing, the dependent fixing is
  **not published**. A gap is published as a gap. A fixing is never estimated to fill one.

## 6. Errors, corrections and restatement

- A fixing found to be **materially wrong** is restated. Materiality threshold: **1 basis point**
  of the published value, or any error that changes a settlement obligation.
- A restatement is published as a **new record referencing the original**, with the reason. The
  original is never deleted or overwritten — the ledger record is immutable by construction, and
  that is the point.
- **Restatement window: two business days** from publication. After that the fixing stands, and a
  dispute is a matter for the contract that referenced it, not for the administrator.
  ⚠️ **This window is policy, not code.** It is deliberately *not* enforced on-ledger: business-day
  arithmetic needs a holiday calendar the package does not carry, and enforcing two *calendar* days
  would silently shorten the window across a weekend — refusing a Friday correction on Monday, which
  fails closed on exactly the error most worth fixing. Both the original and the restatement carry
  their own `finalizedAt`, so a correction made outside the window is auditable after the fact.
- Anyone may report a suspected error to the administrator. **Corrections require the same
  `K`-of-`N` quorum as a fixing** — enforced by `RestatementProposal`, which accumulates
  attestations exactly as a first-time fixing does. One signer cannot correct the record alone.
- **The corrected fixing must differ from the published one**, and must carry a non-empty reason. A
  restatement with no stated reason is indistinguishable from tampering, and both are refused
  on-ledger.
- **The superseded fixing is not archived.** It stays on the record, provably published, with the
  correction pointing back at it via `supersedes`. This is not laxity: archiving it would require
  the *original* attestors' authority, which would let the members who published a wrong number
  veto its correction.
- **Consumer rule:** the current fixing for an (instrument, session) is the one that no other
  fixing supersedes — equivalently, and more cheaply, the newest `finalizedAt`.
- Disclosure survives a correction: every party disclosed the original is disclosed the
  replacement, so nobody is left holding only the wrong number.

## 7. Oversight and governance

- The `OperatorCommittee` is the oversight function. `N` signers are named on-ledger; `K`
  signatures are required. Both are public per identifier.
- **The `N` are parties with an existing commercial interest in the instrument** — the collateral
  taker, the issuer, the administrator, the market maker who quotes it. They are not paid to
  attest. Attestation is a by-product of a position they already hold. This is deliberate: a panel
  of disinterested referees would never be assembled or funded.
- **Every signature is attributable and permanent.** Who signed which fixing is on the ledger
  forever.
- **What each signer asserts is defined, not left to judgement.** `docs/SIGNER_PROTOCOL.md` sets
  out, per role, the named conditions a member verifies before confirming — redemption integrity
  for the issuer, book acceptance for the lender, the observed traded range for the venue. No
  member is asked for an opinion about the price. This is what keeps an unpaid committee from
  decaying into a rubber stamp, and it is what makes a refusal actionable: a signer declines by
  naming a condition that failed, not by disagreeing.
- **The composition must be opposed.** The `N` are not merely interested, they are interested in
  *different directions* — the issuer favours par, the lender favours prudence, the venue favours
  the observed print. A committee whose members share a direction of interest is the failure mode
  this design is most often accused of; `docs/SIGNER_PROTOCOL.md` §6 states that objection at
  full strength and answers it.
- **The administrator does not trade the instruments it prices.** A benchmark administrator that
  takes positions in what it prices has no credibility. This is a hard constraint on CrossDesk's
  own business model, not a preference.

## 8. Cessation — *implemented and enforced on-ledger; see §12*

If a fixing is to be discontinued, the administrator publishes a cessation notice **no less than
60 calendar days** before the final strike, naming the final strike date and, where one exists, a
recommended successor. Contracts referencing the fixing need that window to amend. Three
consecutive Tier 3 (carried-forward) strikes trigger a review of whether the fixing should cease.

## 9. Changes to this methodology

- **Material changes** — the waterfall, the strike time, the minimum quality conditions, the
  materiality threshold, `K` or `N` — require **30 days' notice** before taking effect.
- Non-material changes (clarifications, typographical corrections) take effect on publication.
- Every version of this document is retained. A fixing is always interpreted under the version in
  force at its strike.

## 10. Publication and access

Each published fixing carries: identifier · strike date and time · value · **tier used** ·
signers · drift versus indicative value in basis points · and, if carried forward, the age of the
underlying strike.

Read access is via the CrossDesk API. Referencing a CrossDesk fixing in a contract requires a
licence from the administrator; reading a published fixing does not.

## 11. What this document does not do

It does not make CrossDesk a regulated benchmark administrator. Benchmark administration is a
licensed activity in the EU and UK; CrossDesk is not authorised or registered in those
jurisdictions and this methodology is not a claim otherwise. Where an EU- or UK-supervised entity
wishes to reference a CrossDesk fixing in a regulated product, the recognition or endorsement
route must be resolved first.

## 12. Implementation gaps, stated plainly

| Section | Status |
|---|---|
| §3 Tier 1 auction, uniform price, pro-rata rationing | implemented, tested |
| §3 Tier 1 minimum quality conditions (≥2 orders, ≥2 parties) | implemented, tested — enforced in `uncrossSealedBook`, shared by both closes, as module constants an operator cannot lower |
| §3 Tier 2 committee attestation of a recipe | implemented, tested |
| §3 Tier 2 basket summation | implemented, tested |
| §3 Tier 3 carry-forward flagging | implemented — a fixing whose strike is more than one interval old is returned with `carriedForward` and `ageOfStrikeHours`. A stale number that looks freshly struck is worse than a gap, because a gap is visible and staleness is not |
| §4 Scheduled strike at a fixed time | **detection implemented; striking is still manual.** `FixingSchedule` declares the strike time and zone per identifier and `GET /fixing-schedule` reports PENDING / DUE / OVERDUE / STRUCK / NOT_DUE_TODAY, judged against the ATTESTED strike instant rather than when the ledger saw it. The desk deliberately does **not** auto-strike: a fixing nobody attested is not a cheaper fixing, it is a lie with a timestamp. Business days are approximated as weekdays — no holiday calendar is carried, so a public holiday reads as a missed strike |
| §5 Gap-rather-than-estimate | implemented (`navPerShare` returns `None` on a missing mark) |
| §6 Restatement | implemented, tested — `RestatementProposal`, same K-of-N as a fixing. The two-business-day window is policy, not code (see §6) |
| §5 Wrapper mark as two signed fields (`referencePrice`, `wrapperFactor`) | implemented, tested — reconciliation enforced on-ledger (`testWrapperMarkAttested`) |
| §7 Per-signer protocol evidence on the published fixing | implemented, tested — `ConfirmWithChecks` (`testSignerProtocolEvidence`). The venue's observed range is enforced; the issuer's and lender's claims are recorded but not machine-checked (`docs/SIGNER_PROTOCOL.md` §7) |
| §7 Fund behaviour when `K` is not reached | **not specified** — belongs to the fund's governing documents, not the administrator's |
| §8 Cessation notice | implemented, tested — `CessationNotice`, served through `POST /committee/{cid}/cessation` and readable at `GET /cessations`. The sixty days are enforced **on-ledger** (unlike §6's window, which needs a holiday calendar this package does not carry; sixty *calendar* days needs none). Extensions may only move the date later |
| §10 Lookup by identifier and date | implemented — `GET /fixings/{instrumentId}` returns the published series with §6's consumer rule applied (`current` is the print nothing supersedes), an optional `asOf` for "which number was in force when my liquidation fired", and any cessation notice covering the identifier. Every fixing now carries its tier, wrapper mark and restatement lineage |

**Where this now stands.** Every gap in the original §12 list is closed or explicitly reassigned.
What remains is not a missing feature but two honest limits: **striking is still a human act** (by
design — the schedule reports a missed strike rather than inventing a number nobody attested), and
**no holiday calendar is carried**, so both the §6 restatement window and §4's business days are
approximations that are stated rather than hidden.

The two decisions that are deliberately *not* the administrator's: what a fund does when the
committee cannot reach quorum (its governing documents), and whether an EU- or UK-supervised entity
may reference these fixings at all (§11).
