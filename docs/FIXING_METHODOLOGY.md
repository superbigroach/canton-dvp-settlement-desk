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
that strike occur at that single price; the heavy side is rationed pro-rata. Unpriced
market-on-close orders are allocated ahead of limit orders.

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

## 5. Inputs and their treatment

- **Tier 1 inputs** are sealed orders on the ledger. They are visible to no other participant
  while they rest — including the auditor — and no participant sees the book before the
  uncrossing. This is enforced by the ledger's signatory/observer model, not by an API filter.
- **Tier 2 inputs** are the recipe fields signed by the committee.
- **Indicative value inputs** are external market data (currently Coinbase spot). External data
  is **never** an input to an official fixing. It informs the indicative value only.
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
- **The administrator does not trade the instruments it prices.** A benchmark administrator that
  takes positions in what it prices has no credibility. This is a hard constraint on CrossDesk's
  own business model, not a preference.

## 8. Cessation — *specified, not yet implemented*

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
| §3 Tier 3 carry-forward flagging | derivation works; **the flag and age are not published** |
| §4 Scheduled strike at a fixed time | **not implemented** — strikes are triggered manually |
| §5 Gap-rather-than-estimate | implemented (`navPerShare` returns `None` on a missing mark) |
| §6 Restatement | implemented, tested — `RestatementProposal`, same K-of-N as a fixing. The two-business-day window is policy, not code (see §6) |
| §8 Cessation notice | **not implemented** — process only |
| §10 Lookup by identifier and date | **not implemented** — `GET /fixings` returns contracts, not a dated series |

Closing the four gaps marked in §12 is what turns the existing implementation into a benchmark.
