# The Rulebook — index construction and fixing methodology, all asset classes

**ETP Foundry (formerly CrossDesk). Version 0.1 DRAFT, 21 September 2026.**

> **Status: nothing in this document is a published rule yet.** No benchmark described
> here has been published for commercial use, and no committee has adopted it. This is
> the document to take *to* the first committee, not the record of one. Every rule below
> is marked **[ADOPT]** (needs committee sign-off before first publication) or
> **[FIXED]** (a design decision that does not require a committee).
>
> Companions: `5-24-7-fixing-methodology.md` (the 24/7 schedule and waterfall, 15 Sep —
> this rulebook extends it and does not replace it), `1-operations-runbook.md` (how to
> run the panel day to day), `3-proposed-methodology-amendment.md` (contributed inputs),
> `2-message-templates.md` (what to send when).

---

## 0. Why this document exists

A benchmark is not a number; it is a **number plus the rules that produced it, published
in advance**. Anyone can compute a price. What a licensee is buying — and what an auditor,
a lender's risk committee and eventually a regulator will read — is this document.

Three things follow, and they shape everything below:

1. **Rules are published before they are used, never after.** A rule invented to explain
   a number already struck is not a rule.
2. **Every number carries its provenance**: which inputs were used, which were stale, who
   signed, and how uncertain it was.
3. **The administrator does not get to be clever in the moment.** Discretion is defined,
   bounded, logged, and rare.

The structure follows the IOSCO *Principles for Financial Benchmarks* (§13 maps each
principle to a section). We are not claiming IOSCO assurance — that requires an
independent auditor. We are claiming the shape is right, which is what a counterparty's
diligence will check.

---

## 1. Definitions

| Term | Meaning here |
|---|---|
| **Administrator** | ETP Foundry. Controls the methodology, computes, publishes. |
| **Committee** | The K-of-N signers who attest a fixing. Not employees, not paid. |
| **Contributor** | A party supplying an input (a venue's traded range, a custodian's reserve statement). May also be a signer. |
| **Input** | An observable datum used in a computation. |
| **Index** | A number computed from published rules and observable inputs. **No committee required.** |
| **Fixing** | A valuation struck at a defined moment and **attested by K of N signers**. |
| **OFFICIAL** | A fixing usable for NAV, create/redeem, reporting, audited records. |
| **INDICATIVE** | A fixing usable for margin, haircuts, health factors. **Never a NAV.** |
| **Band** | The published uncertainty around a fixing. |
| **Carry-forward** | The prior value republished, flagged, when no new value can be struck. |
| **Restatement** | A correction of an already-published value, under §8. |
| **Tolerance** | The distance from its own valuation a signer will accept before refusing. Declared per signer, recommended **25 bp**. |

**The distinction that must never blur [FIXED]:** an **index** is a computation; a
**fixing** is an attestation. When prices exist, compute. When they do not, attest. A
product may reference both — an index while the underlying market is open, a fixing when
it is shut — but each published value is one or the other, labelled, never both.

---

## 2. The two products

| | **Index** | **Attested fixing** |
|---|---|---|
| Needs a committee | No | **Yes, K of N** |
| Needs anyone's permission | No | Yes — signers must agree to sign |
| Cost to launch | Near zero | Committee assembly |
| Fails when | Inputs go stale | Quorum is not reached |
| Right when | Every leg has an observable price | No observable price exists, or a contract needs one struck moment |
| Labelled | `INDEX` | `OFFICIAL` or `INDICATIVE` |

**[FIXED] Publish indices first.** They are free, they require nobody's consent, and they
accumulate the only asset that cannot be copied: unbroken history. The attested fixing is
the product that earns the licence fee, and it is credible in proportion to the history
already printing beside it.

---

## 3. Asset-class taxonomy — which rules apply

| Class | Example | Observable price? | Product | Committee |
|---|---|---|---|---|
| **A. Wrapped single asset** | cBTC, cETH | Yes, on venues | Index (continuous) + **fixing for contract settlement** | 3 of 5 (§6.1) |
| **B. Multi-asset basket** | cBTC + cETH | Legs yes, **basket no** | Index if all legs live; **fixing otherwise** | 3 of 5 (§6.2) |
| **C. Tokenised equity / equity basket** | SPYx, QQQx, a tokenised-equity basket | **Only ~32.5 of 168 h/week** | Index while home market open; **fixing when shut** | 3 of 5 (§6.3) |
| **D. Long/short or spread** | long cBTC / short cETH | **No** — depends on borrow and financing | **Fixing only** | 3 of 5 (§6.4) |
| **E. Illiquid / private** | private credit, real assets | **No** | **Fixing only**, expert judgement bounded by §5.6 | 3 of 5 (§6.5) |
| **F. Fund NAV** | an ETP's own NAV per share | Computed from A–E legs | **Fixing** | 3 of 5 (§6.6) |

**[FIXED] Class A honesty rule.** For a liquid asset with an open market, *we are not
needed and we say so*. A single-asset fixing exists only because **contracts need one
struck moment agreed in advance** — the LBMA Gold Price logic — not because the market
lacks a price. Never pitch a class-A fixing as price discovery.

---

## 4. Index construction rules

Applies to every published index. A specific index's **factsheet** states its choices
under each heading; anything the factsheet does not state defaults to this section.

### 4.0 Venue (constituent) eligibility [ADOPT]

Modelled on CF Benchmarks' *Constituent Exchange Criteria* (v8.5). A venue supplies input
data only if, in the committee's opinion, it meets **all** of:

1. **Volume contribution.** The venue's share of observed volume in the relevant pair
   exceeds a stated threshold over a continuous observation period.
   - CF's values: **>3% over 180 days** to launch a new index, **>3% over 90 days** to join
     an existing one, with continued inclusion reviewed if it falls below 3%.
   - **Ours [ADOPT]: >5% over 30 days**, reviewed quarterly. The window is shorter and the
     bar higher because Canton venues are young and few — a 180-day history does not exist
     yet, and with only a handful of venues a 3% contributor adds noise rather than
     robustness. Move toward CF's parameters as the market matures.
2. **Fair and transparent market conditions**, with processes to identify and impede
   manipulative trading.
3. **No undue barriers to entry**, and using the venue does not expose participants to
   undue credit, operational or legal risk.
4. **Complies with applicable law** — market conduct, custody, KYC, AML.
5. **Cooperates with inquiries** from the administrator and regulators, and has executed a
   **data-sharing agreement** with us.
6. **API with sufficient reliability, detail and timeliness**, including trade size.

**Minimum two venues at launch [FIXED].** Additions and removals are decided by the
committee, announced before they take effect, and recorded in the oversight minutes. A
venue may be **suspended** immediately for (2)–(5); volume failures take effect at the next
review.

### 4.1 Universe and eligibility [ADOPT]
An asset is eligible only if **all** hold:
1. **Identifiable on-ledger**: registrar party id (with `::1220…` fingerprint), instrument
   id, registry base URL, and a confirmed holding interface (e.g. `HoldingV1`).
2. **Two independent venues** quote it, or one venue plus an issuer primary
   create/redeem level.
3. **Minimum liquidity**: traded on ≥ `L` of the last 20 calendar days, with 20-day median
   daily volume ≥ `V`. Factsheet states `L` and `V`.
4. **Redeemability or reserve attestation**: a stated mechanism to obtain the underlying,
   or a published reserve attestation with a stated cadence.
5. **No unresolved halt** (§7.6).

**Removal** is immediate on failing (1), (4) or (5); on failing (2) or (3) it takes effect
at the next scheduled rebalance unless the committee shortens it under §7.

### 4.2 Selection and weighting [ADOPT]
Each index states one selection rule and one weighting scheme:

| Weighting | When it is the right choice | Cap |
|---|---|---|
| **Equal** | Small universes (2–10), avoids one leg dominating | n/a |
| **Market-value** | Reflects real exposure | **single-asset cap 35%**, redistributed pro-rata |
| **Fixed-weight** | A stated strategy (e.g. 50/50 BTC/ETH) | weights restored at rebalance |
| **Liquidity-weighted** | When tradability matters more than size | cap 35% |

**[FIXED] Capping is mandatory for any index with more than two constituents.** An
uncapped market-value index of tokenised assets becomes a single-asset proxy within
months, and then it is not an index, it is a wrapper with extra steps.

### 4.3 Rebalancing and reconstitution [ADOPT]
- **Scheduled rebalance**: monthly, effective at the first OFFICIAL fixing of the first
  business day of the month, using weights determined **5 business days earlier** and
  published then.
- **Reconstitution** (universe review): quarterly, same notice.
- **Announcement before effect, always.** Weights are published before they apply so
  consumers can trade into them. An index that changes weights without notice is
  untradeable and invites the accusation that the administrator front-ran it.
- **No intra-period discretionary rebalancing** except a §7 event.

### 4.4 The divisor [FIXED]
Index level = (Σ weightᵢ × priceᵢ) ÷ **divisor**. The divisor is adjusted so that the
level is **unchanged at the instant of** any rebalance, constituent change, or corporate
action. The divisor's history is published with the index. **A jump in the level caused by
a rule change, not by prices, is the classic benchmark failure** — this is what prevents it.

### 4.5 Corporate actions [ADOPT]
Mandatory for class C. The first dividend breaks a naive index.

| Event | Treatment |
|---|---|
| **Cash dividend** | Price-return index: price drops, no adjustment. **Total-return index**: reinvest at the ex-date fixing. State which the index is in its name. |
| **Stock split / reverse split** | Adjust shares-per-token and divisor. Level unchanged. |
| **Rights issue** | Adjust price by the theoretical ex-rights price; divisor adjusted. |
| **Merger / acquisition** | Constituent removed at the last reliable price; replacement at the next scheduled reconstitution, not immediately. |
| **Spin-off** | Spun entity included at its first reliable price if eligible, else sold out at that price. |
| **Delisting / suspension > 5 days** | Removed at the last reliable price; if none, at zero **with the committee's attestation** and a published note. |
| **Ticker/ISIN change** | No adjustment; identifier updated. |

**[FIXED]** The tokenised wrapper's own treatment governs when it differs from the
underlying: **we price the token, not the share.** If the issuer does not pass a dividend
through, the index does not either — and the factsheet says so plainly.

### 4.6 Currency [ADOPT]
State the index currency. Non-USD legs convert at the same timestamp as the price, using
the FX source named in the factsheet. **Stablecoin FX is never assumed to be 1.00** —
USDC/USD is an input with a max age (§5.2), applied as a multiplier.

### 4.7 Naming and IP [FIXED] ⚠️
- **Never replicate a licensed index's constituents or name.** The Nasdaq-100, S&P 500,
  and their tickers are licensed property; index providers litigate over exactly this.
- Every index published here is **our own methodology, our own name** — e.g.
  "ETP Foundry Tokenised US Large-Cap 10, equal-weighted."
- Never imply endorsement by an asset issuer, venue or exchange.
- The correct sentence to an issuer: *"I'm not offering you the Nasdaq-100. I'm offering
  you a number for the tokenised basket you can actually hold."*

### 4.8 Multi-network assets [ADOPT]

**[FIXED] No rule in this document names a network.** An asset is identified by the tuple
*(network, registrar/issuer, instrument id, redemption path)*. Adding a network is an
adapter, never a methodology change.

**The asymmetry that governs everything here: the number crosses networks freely, the
assets do not.** Pricing an asset that lives on another chain requires observable inputs
only — no bridge, no custody, no counterparty risk. Settlement requires the asset.

| Layer | Network-dependent? | What it needs | Revenue |
|---|---|---|---|
| **Pricing** (index + fixing) | **No** | Readable venues on that network | Licence + bps |
| **Representation** | Yes | A registrar issuing a backed token on the settlement ledger | Enables settlement |
| **Settlement** (atomic create/redeem) | Yes — **one ledger only** | Both legs on the same ledger | Per-order fee |

**Eligibility additions for an asset whose native network is not the settlement ledger:**
1. A named **registrar or custodian** issuing the representation, with the tuple published.
2. A **reserve attestation** with a stated cadence and method, feeding the wrapper factor (§5.5).
3. A **defined redemption path** back to the native network, with expected settlement time.
4. The custodian occupies **committee seat 2** (§6) for any fixing of that asset.
5. The factsheet states, plainly, **which network the underlying actually sits on**.

**[FIXED] Prohibited claim:** no value or product description may assert **atomic
settlement across networks**. Atomicity holds within one ledger. Across ledgers a party
holds both sides, and that party is named. Hash-locked swaps are not used for any
OFFICIAL settlement.

---

## 5. Fixing rules

### 5.1 Schedule [ADOPT]
Per `5-24-7-fixing-methodology.md` §2, restated here as the governing schedule:

| Fixing | Time | Type |
|---|---|---|
| Home-market close (per asset's home market) | e.g. 20:00 UTC for US, DST-aware | **OFFICIAL** |
| Off-hours | 00:00 / 08:00 / 16:00 UTC, **every day including weekends** | INDICATIVE |
| Canton-native assets with no home market (classes A, B, D) | 16:00 UTC daily | **OFFICIAL** |

Times are declared once and **not moved casually**; a schedule change is a methodology
change under §8.

### 5.2 Input waterfall and staleness [ADOPT]
The class-C waterfall in `5-24-7-fixing-methodology.md` §3 stands unchanged. Generalised:

| # | Input | Max age | Role |
|---|---|---|---|
| 1 | Home-market official close / closing auction | to next close | Anchor |
| 2 | Futures on the underlying | 5 min | Off-hours signal |
| 3 | Token secondary trades, volume-weighted, outliers trimmed | 15 min | Primary for classes A/B |
| 4 | Order-book depth at a size threshold | 1 min | **Bound**, not a price |
| 5 | Cross-listed instruments in open regions | 5 min | Secondary |
| 6 | Issuer primary create/redeem level | since publication | Sanity bound |
| 7 | FX / stablecoin peg | 5 min | Multiplier |
| 8 | Corporate actions | as announced | Adjustment |
| 9 | Reserve / wrapper factor (§5.5) | as attested | Multiplier |

**[FIXED] Minimum inputs**: **two independent live inputs** or no fixing is struck (§7.1).
"Independent" means different operators — two venues run by the same operator count once.
This matches CF Benchmarks, whose indices "shall require input data from no less than two
(2) Constituent Exchanges" at launch.

### 5.2a Construction of an OFFICIAL fixing [ADOPT]

**We adopt the construction used by the CME CF Bitcoin Reference Rate (BRR)**, which is a
UK BMR Registered Benchmark and the settlement basis for CME's bitcoin futures. Using a
proven, regulator-accepted construction is worth far more than an original one.

1. Collect all **relevant transactions** in the observation window from all constituent
   venues into one list of (price, size) pairs.
2. **Partition** the window into equal time intervals.
3. For **each partition**, compute the **volume-weighted median** trade price across all
   venues combined — not per venue.
4. The fixing is the **equally-weighted arithmetic mean of the partition medians.**

| Parameter | CF Benchmarks (BRR) | **Ours [ADOPT]** |
|---|---|---|
| Observation window | 60 minutes | **60 minutes** |
| Partition length | 5 minutes | **5 minutes** |
| Number of partitions | 12 | **12** |
| Effective time | 4:00 p.m. London | per §5.1 |

**Why this shape and not a simple VWAP or last-trade:** the volume-weighted *median*
ignores size-weighted outliers, so a single large print cannot move the fixing; averaging
across twelve partitions means a burst of activity in one minute cannot dominate the hour.
CF's own note: it "immunizes ... to a high degree against price anomalies, while being
replicable through spot trading." **Replicable matters** — an AP must be able to trade into
the fixing, or the arbitrage that keeps the product honest does not work.

**Partitions with no trades** are dropped, not interpolated. If too few partitions survive
to meet §7.1, no fixing is struck.

### 5.2b Data screening [ADOPT]

Two stages, both automated, both from the CF pattern:

**Erroneous data** — discarded outright:
1. Non-numeric or non-positive price or size.
2. Unparseable format.
3. Execution timestamp more than **one minute in the future** of our clock.

**Potentially erroneous data** — a venue-level screen:
1. Compute each venue's own volume-weighted median over the window.
2. Compute the median of those venue medians.
3. If a venue's median deviates from it by more than the **Potentially Erroneous Data
   Parameter — 5%** (CF's value for BTC and ETH), **all of that venue's transactions are
   excluded** for that calculation, and the exclusion is flagged on the tape.

This replaces any per-trade outlier rule. It is stricter where it matters (a whole venue
printing off-market is the real risk) and it is precedented.

### 5.3 The sealed auction [FIXED]
Where a class-A or class-B fixing settles contracts, the struck moment is the clearing
price of the **sealed market-on-close auction**: the resting book is dark to every other
trader and to the auditor, and fills print to a public tape naming nobody. The venue
operator can see its own book — **say this; do not claim the book is invisible to everyone.**

### 5.4 The band [ADOPT]
Every fixing publishes `price ± band`, the inputs used, and which were stale.

| Situation | Band |
|---|---|
| Home market open, ≥3 live inputs | Tightest |
| Weeknight, futures live | Moderate |
| Weekend, futures closed | **Widest** |
| < 2 independent live inputs | **No fixing** (§7.1) |

**[FIXED]** A number without a band invites the dispute. The band is what lets a lender
set a haircut and a liquidated borrower see what was known.

### 5.5 Wrapper factor [ADOPT]
For a wrapped asset, the mark is **reference price × wrapper factor**, where the factor
reflects reserve attestation, redemption friction and any observed discount (e.g.
65,000 × 0.998 = 64,870, 20 bp below par). The factor's derivation is published. It is
**never** silently folded into the price.

### 5.6 Expert judgement [ADOPT]
Permitted **only** for class D and E, and only when the waterfall yields fewer than two
live inputs. When used:
1. It is labelled `JUDGEMENT` on the tape.
2. The reasoning is recorded in the fixing record before publication, not after.
3. It requires **K+1** signatures, not K.
4. Three consecutive judgement fixings on the same asset trigger a methodology review.

**[FIXED]** Expert judgement is the mechanism that destroyed LIBOR's credibility. It is
permitted here only where no observable input can exist, never as a convenience.

### 5.7 Class D (long/short) specifics [ADOPT]
A spread or long/short value must state, and sign for, all four:
- the long leg's price and source,
- the short leg's price and source,
- the **borrow cost** used and its source,
- the **financing rate** and accrual convention.

**[FIXED]** No class-D fixing may be struck without a named borrow source. A short leg
nobody can actually borrow is not a position; it is an opinion.

### 5.8 Class F (fund NAV) specifics [ADOPT]
NAV per share = (Σ leg values at the same struck moment − liabilities and accrued fees)
÷ shares outstanding, where:
- **every leg uses the same struck moment** — mixing timestamps across legs is the error
  that creates basis risk for the AP and is prohibited;
- shares outstanding is attested by the issuer or transfer agent;
- accrued fees follow the fund's published schedule.

---

## 6. Committee composition by class

The generic four seats (`1-LEARN/5-who-signs-and-why.md`): **issuer** (biased high),
**custodian/reserve holder** (neutral, liable), **risk taker / lender** (biased low),
**market-facing party** (depends on inventory). Fill as many as exist; **three is a
working quorum**. Signers are parties that **already hold a position against the mark** —
never a paid "neutral attestor."

**[FIXED] The four governing rules, stated in every methodology and out loud in meetings:**
1. **The issuer must never be a majority of the quorum.** If parties the issuer controls
   can reach K alone, the committee has proved nothing and the product is theatre.
2. **No single party may reach K alone, and no single party may block K alone.** That is
   what `N > K` is for. State both halves — counterparties fear capture *and* hostage-taking.
3. **A lender seat is what makes it credible**, not what completes it. It is the only seat
   whose incentive is to sign *below* par.
4. **The administrator should not be a signer** once seats 1–4 are filled. Referee, not player.

| Class | Recommended committee | N/K |
|---|---|---|
| **6.1 Wrapped single asset** (cBTC) | issuer (BitSafe) · reserve/node operator running FROST · venue where it trades · lender holding it as collateral · administrator (ideally not signing) | 5 / 3 |
| **6.2 Multi-asset basket** (cBTC+cETH) | issuer leg A · issuer leg B · **one dual-listed venue** · lender · administrator | 5 / 3 **with at least one of venue/lender required** — the two issuers must not reach K alone |
| **6.3 Tokenised equity fund** | the fund · custodian holding the real shares · administrator/transfer agent · the AP quoting it 24/7 · a lender, if anyone borrows against it | 5 / 3 |
| **6.4 Long/short** | long-leg issuer · **the borrow lender (mandatory)** · venue · risk taker · administrator | 5 / 3, borrow lender's signature **required** |
| **6.5 Illiquid / private** | manager · custodian/trustee · independent valuer if one exists · lender · administrator | 5 / 3, with §5.6 K+1 for judgement |
| **6.6 Fund NAV** | as 6.3 | 5 / 3 |

**Pilot minimum [ADOPT]:** N=3, K=2, only if the three are genuinely opposed —
issuer · risk-taker · market-facing. Disclose the pilot configuration on every value.

**Never seat** [FIXED]: oracles and data vendors (no money riding on the mark — they are a
distribution channel, not a seat); any party with no disclosed licence, no named
principals and no balance sheet at risk. **Every weak signer subtracts.**

**Declared tolerance [ADOPT]:** each signer declares, at onboarding, how far from its own
valuation a mark may sit before it refuses. Recommended **25 bp**. Tolerances are
published in aggregate, not per signer.

### 6.7 Key custody and the attestation trust level [ADOPT]

A signature is only worth the difficulty of forging it. Three arrangements exist, and
**every published value states which level each signature was made at.**

| Level | Where the signing key lives | Could the administrator forge it? | Signer's cost |
|---|---|---|---|
| **L1 — hosted party** | On the administrator's participant; the signer authorises with a scoped API key and the administrator exercises the choice as their party | **Yes, technically** | Zero |
| **L2 — external signing** | With the signer, off-ledger; the administrator submits a transaction the signer has already signed | **No** | Moderate |
| **L3 — own participant** | On the signer's own Canton participant | **No** | High — they run Canton |

**[FIXED] The level is disclosed, never implied.** Each `SignerCheck` records the level, and
the fixing record shows the mix (e.g. `K=3 of N=5 — L1:2, L2:1`). A consumer reading the
record can then judge the signature set for themselves, which is the whole point of putting
it on a ledger.

**[ADOPT] The ladder, and when each is acceptable:**
- **L1 is acceptable for a pilot, a shadow run, or a design partnership**, where nothing
  settles against the value and the signer is evaluating the process.
- **L1 is not acceptable for an OFFICIAL fixing that a regulated product settles against.**
  A lender whose liquidation engine consumes the mark will eventually ask what stops the
  administrator signing on its behalf, and at L1 the honest answer is "a scoped API key I
  issued and control."
- **Target state: every signer at L2 or above before the first OFFICIAL fixing used by a
  third party.** L2 is the realistic institutional default; L3 is for signers who already
  run a participant.

**[FIXED] Scope limits apply at every level.** A signer credential may only confirm or
refuse fixings for the instruments its seat covers. It cannot move assets, cannot propose a
fixing, and cannot act as another seat.

**[FIXED] Administrator impersonation is logged and disclosed.** The administrator can
assume a mapped user's identity for a single request (an operational necessity for support).
Every such act is recorded, and **the capability is disclosed to signers at onboarding.**
It is never used to confirm a fixing on a signer's behalf.

---

## 7. Failure, carry-forward, restatement

### 7.1 Insufficient inputs [ADOPT]
Fewer than two independent live inputs → **`NO FIXING`**, with the reason. **Never guess.**

### 7.2 Quorum failure [ADOPT]
Fewer than K signatures → no new fixing. **Tier 3** applies: the prior recipe keeps
deriving a value, published **flagged `CARRIED FORWARD`** with its age. **Three
consecutive carry-forwards trigger a committee review** and notification of all licensees.

### 7.3 Refusal [FIXED]
Refusal is not a free veto — every signer relies on the number, so refusing is costly to
the refuser. A signer may abstain on any fixing; the fixing stands if K is reached. A
signer who abstains **three consecutive times without cause** is reviewed under
`1-operations-runbook.md` §6.

### 7.4 Source disagreement [ADOPT]
A source disagreeing by more than **X%** (factsheet states X; default 2% for class A/B,
5% for class C off-hours) is dropped and flagged on the tape.

### 7.5 Exceptional moves [ADOPT]
A move greater than **Y%** since the last fixing (default 10%) publishes with flag
`EXCEPTIONAL` and notifies signers. Consumers decide how to treat it. **We do not
suppress a real move** — suppression is how an administrator becomes the story.

### 7.6 Halts, suspensions, delistings [ADOPT]
`NO FIXING` for that asset until the committee agrees a rule. For an index, the
constituent is removed under §4.5.

### 7.7 Restatement [ADOPT]

**Both conditions must be met, or the value stands.** This is CF Benchmarks' two-part test
and it is better than a vaguer "material error" rule because neither we nor a complainant
can argue about it after the fact:

1. **Timeliness** — the correction can be published **before 23:59:59 on the same
   calculation day**.
2. **Materiality** — the corrected value differs from the published value by more than
   **0.10%** in absolute terms.

*Worked example (CF's own):* a value published at 1234.56 is restated only if the corrected
value is **above 1235.79** or **below 1233.33**.

- If both conditions hold: restate, flag `RESTATED`, publish both values and the cause,
  and notify every licensee.
- If either fails: **the value stands on the record.** Publish a correction notice stating
  what it should have been. Consumers have already settled against it, and silently
  rewriting history is worse than a wrong number.
- A value is corrected only for a wrong input or a misapplied rule — **never because the
  result was unpopular.**
- Every restatement and every correction notice is logged in the quarterly oversight minutes.

---

## 8. Change control

**[ADOPT]**
1. Any methodology change is **proposed on-ledger**, requires the same K of N, and takes
   effect only after a **notice period of 5 business days**.
2. **Material** changes (selection, weighting, waterfall order, schedule, committee
   composition rules) require **30 calendar days' notice** and written notice to every
   licensee.
3. The rulebook is **versioned**; every published value carries the rulebook version that
   produced it. Superseded versions stay published.
4. **No retroactive change, ever.** A rule applies from its effective date forward.

---

## 9. Cessation policy [ADOPT]

Every serious administrator publishes one, because consumers write contracts referencing
the number and need to know what happens if it stops.

1. **Notice**: minimum **90 days** before ceasing any OFFICIAL benchmark, to all licensees
   and publicly.
2. **Fallback**: each factsheet names the recommended fallback — a successor index, a named
   alternative administrator, or the stated calculation consumers should adopt.
3. **Transfer**: the methodology and history may be transferred to a successor
   administrator; licensees are notified at least 90 days before.
4. **Records** survive cessation for the §10 retention period.
5. **Trigger review**: if a benchmark has no licensees for two consecutive quarters, the
   committee reviews whether to cease it. Zombie benchmarks are a liability.

---

## 10. Records, conflicts, complaints

**[ADOPT]**
- **Fixing record** (per value, on-ledger where possible): timestamp, inputs with sources
  and ages, discarded inputs and why, computation, band, signers, abstentions, flags,
  rulebook version. **Retention: 7 years.**
- **Conflicts register**: any interest of the administrator or a signer in the asset being
  priced, reviewed quarterly. The administrator holding a position in a priced asset is
  disclosed and, where material, disqualifies it from signing.
- **Complaints procedure**: any licensee or signer may challenge a value in writing;
  acknowledgement within 2 business days, substantive response within 10; unresolved
  complaints escalate to the quarterly oversight meeting and are minuted.
- **Oversight**: quarterly, agenda fixed (`1-operations-runbook.md` §4), minutes published
  to signers and licensees.
- **Audit**: the fixing record is designed to be reproducible by a third party from the
  published inputs. Independent assurance when a licensee requires it (§12).

---

## 11. Publication and labelling [FIXED]

Every published value carries: name, rulebook version, timestamp and timezone, type
(`INDEX` / `OFFICIAL` / `INDICATIVE`), band, flags (`CARRIED FORWARD`, `EXCEPTIONAL`,
`JUDGEMENT`, `RESTATED`), signer count as `K of N`, and the licence class required to use it.

**Licence classes** (`1-LEARN/3-money-who-pays-what.md`): **use/reference** (flat + bps),
**redistribution** (flat per vendor), **display** (free). Reading the number through an
oracle does not substitute for the use licence.

**Prohibited representations [FIXED]:** presenting an `INDICATIVE` fixing as a NAV;
describing any value as "live" or as having a published history before one exists;
describing ETP Foundry as an authorised or registered benchmark administrator.

---

## 12. Regulatory posture [FIXED]

- ETP Foundry is **not an authorised or registered benchmark administrator** in any
  jurisdiction, and says so on every surface.
- **UK**: administering a benchmark requires a Part 4A permission. FCA application fee for
  a **non-significant** benchmark administrator is **£2,820** (FEES 3 Annex 1A, Category 4);
  4 months for authorisation, 45 working days for registration. One fee covers however many
  benchmarks are administered. **The UK third-country transitional runs to 31 December 2030**,
  so a non-UK administrator is not blocked from UK-referencing use before then.
- **EU**: BMR scope and the effect of the recent review are **unverified** — confirm with
  counsel before any EU-domiciled licensee. Do not quote an EU number.
- The real cost is not the fee but the **compliance build** — oversight function, control
  framework, documented methodology, audit: budget **$40–80K** when a licensee requires it.
- The parked code (`ContinuousBook`, `Perpetual`, `LiquidityMandate`) stays parked.
  Publishing a number and having a committee sign a valuation is the unregulated part;
  operating a book is not.

---

## 13. IOSCO mapping

| IOSCO principle | Where |
|---|---|
| 1–5 Governance, oversight, conflicts, control framework | §6, §10 |
| 6 Benchmark design | §4, §5 |
| 7 Data sufficiency | §5.2, §7.1 |
| 8 Hierarchy of data inputs | §5.2 |
| 9 Transparency of determinations | §5.4, §11 |
| 10 Periodic review | §4.3, §8 |
| 11 Content of the methodology | this document |
| 12 Changes to the methodology | §8 |
| 13 Transition / cessation | §9 |
| 14 Submitter code of conduct | §6 tolerances, `1-operations-runbook.md` |
| 15 Internal controls over submitters | §5.2, §7.4 |
| 16 Complaints | §10 |
| 17 Audits | §10 |
| 18 Audit trail | §10 fixing record |
| 19 Cooperation with regulators | §12 |

---

## 14. The eight open items — recommendations

Each is now answered with a precedent. **Committee still has to adopt them**; the point is
that none is an invention.

| # | Item | Recommendation | Precedent |
|---|---|---|---|
| 1 | Thresholds | Venue: **>5% volume over 30 days** (§4.0). Disagreement **X = 5%** per venue (§5.2b). Exceptional **Y = 10%** — flag only, never suppress. | CF: 3%/180d, Potentially Erroneous Data Parameter 5% |
| 2 | Which venue | **Two, both dual-listed** (cBTC *and* cETH) — from Cantex, Cantor8, Silvana, Rho, Bron, Console, Zoro, OneSwap, Walley, Loop. Require a **data-sharing agreement** before first use. | CF requires data-sharing agreements with CME |
| 3 | First basket weighting | **Fixed 50/50, rebalanced monthly.** Explainable in one sentence, deterministic, replicable by an AP. Capped market-value only at 3+ legs. | 21Shares ABBA is a two-leg BTC/ETH product |
| 4 | Price- or total-return | **Price-return first**, named as such. Add a total-return variant later only if the wrapper actually passes dividends through — most tokenised equity wrappers do not. | Standard index-family practice (S&P, MSCI publish both, named) |
| 5 | Class-D borrow source | **Defer class D entirely** until a lender is seated. No named borrow, no fixing (§5.7). | — |
| 6 | Quorum failure | Recommend to the issuer: **suspend creations and redemptions**, keep publishing `CARRIED FORWARD` for margin use only, and **suspend dealing after 3 consecutive carry-forwards** (§7.2). The issuer must write this into its governing documents — it is their decision, not ours. | CF: calculation failure → no value that day |
| 7 | Pilot configuration | **Yes — N=3, K=2, administrator signing**, provided: disclosed on *every* value; the three are genuinely opposed (issuer · risk-taker · market-facing); and a **stated sunset** — the administrator stops signing once a fourth independent seat is filled. | Standard for a new panel; disclosure is what makes it acceptable |
| 8 | EU BMR | **Unresolved — get counsel** before any EU-domiciled licensee. UK position is settled (§12). | — |

---

## 15. The document set we publish

What a real administrator puts in public, taken from CF Benchmarks' own published set. This
is the checklist for "are we professional yet":

| Document | Status here |
|---|---|
| **Methodology Guide** (per series), versioned, with a full change history | **this rulebook** — v0.1 |
| **Constituent venue criteria** | §4.0 |
| **Benchmark Statement** (per series — what it measures, its limitations, who should use it) | `7-BENCHMARK-STATEMENT.md` v0.1 |
| **Restatement policy** | §7.7 |
| **Cessation / transition policy** | §9 |
| **Oversight committee minutes** (published in summary) | `1-operations-runbook.md` §4 — none held yet |
| **Suitability analysis** ("is this fit to underlie a regulated product?") | `9-SUITABILITY-ANALYSIS.md` v0.1 — **verdict: not yet** |
| **Hard-fork / chain-event policy** | `8-CHAIN-EVENT-POLICY.md` v0.1 — fork + wrapper/registrar/migration events |
| **Factsheet per index** (parameters, weights, history) | `10-FACTSHEETS.md` — template + first two drafted |

**[FIXED] Version history is published, not internal.** CF's methodology shows every change
since 2016 with dates and reasons. That visible history is a large part of why anyone
trusts the number — it proves rules were not changed quietly. Ours starts at v0.1 today.
