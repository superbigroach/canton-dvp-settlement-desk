# Factsheets — template and first drafts

**Version 0.1 DRAFT · 22 September 2026 · ETP Foundry (formerly CrossDesk)**

> **No benchmark below is published or live.** These are the factsheets as they will read
> on day one of publication. Every "history" field is empty because the history does not
> exist yet — that is the honest state and it is not to be dressed up.

A factsheet is the one page a licensee, a lender's risk team or an issuer's board actually
reads. It states the parameters this specific benchmark uses; anything it does not state
defaults to `6-RULEBOOK.md`.

---

## A. The template

```
NAME                  Full name, and the short code
TYPE                  INDEX / OFFICIAL fixing / INDICATIVE fixing
CLASS                 A–F per rulebook §3
WHAT IT MEASURES      One sentence. The economic reality, not the mechanism.
CURRENCY              Index/quote currency, and the FX source if any leg is not in it
RETURN TYPE           Price-return or total-return (indices only)

CONSTITUENTS          What is in it, and the weighting scheme
CAPPING               Single-asset cap, if any
REBALANCE             Frequency, effective moment, notice period
RECONSTITUTION        Frequency (universe review)

CONSTITUENT VENUES    Named. Each must meet rulebook §4.0
OBSERVATION WINDOW    Length, partitions, effective time
CALCULATION           Reference to rulebook §5.2a, plus anything specific
SCREENING             Potentially-erroneous parameter (default 5%)
MINIMUM INPUTS        Default: 2 independent venues

COMMITTEE             Seats, N and K, mandatory seats, trust levels (L1/L2/L3)
TOLERANCES            Aggregate declared tolerance of the panel

PUBLICATION           Times, channel, precision
BAND                  How the uncertainty band is derived
FAILURE               What happens on no-quorum / insufficient inputs
FALLBACK              What consumers should use if this benchmark ceases

FIRST VALUE           Date of first published value
HISTORY               Values published to date; longest unbroken run
RESTATEMENTS          Count, and links to the notices
VERSION               Rulebook version in force
```

---

## B. ETP Foundry cBTC Close — draft

| Field | Value |
|---|---|
| **Name** | ETP Foundry cBTC Close — `CBTC_CLOSE` |
| **Type** | OFFICIAL fixing (daily), with INDICATIVE fixings off-hours |
| **Class** | A — wrapped single asset |
| **What it measures** | The price at which cBTC, BitSafe's Canton representation of bitcoin, could be exchanged for US dollars at 16:00 UTC, as observed on constituent Canton venues and adjusted for the wrapper's reserve condition |
| **Currency** | USD |
| **Constituents** | Single asset |
| **Constituent venues** | ⬜ **TO BE NAMED** — two required, each meeting §4.0, each with a signed data-sharing agreement |
| **Observation window** | 60 minutes to 16:00 UTC, 12 × 5-minute partitions |
| **Calculation** | Rulebook §5.2a — volume-weighted median per partition, equally-weighted mean of the 12 medians, × wrapper factor (§5.5) |
| **Wrapper factor** | Derived from BitSafe's reserve attestation and observed redemption friction. Published with its derivation, never folded into the price |
| **Screening** | Potentially-erroneous parameter **5%** at venue level |
| **Minimum inputs** | 2 independent venues |
| **Committee** | N=5, K=3 — BitSafe (issuer) · a cBTC node operator running FROST (reserve) · a constituent venue (market-facing) · a lender holding cBTC as collateral (**risk taker, mandatory for credibility**) · administrator (operator, ideally not signing) |
| **Trust levels** | ⬜ pilot expected at **L1**; target **L2+** before third-party settlement use |
| **Publication** | 16:00 UTC daily (OFFICIAL); 00:00 / 08:00 / 16:00 UTC (INDICATIVE). To ledger + public tape |
| **Band** | Per §5.4, widest when fewer than 3 live inputs |
| **Failure** | < 2 inputs or < K signatures → `NO FIXING`, prior value carried forward and aged. 3 consecutive carry-forwards → committee review |
| **Chain events** | `8-CHAIN-EVENT-POLICY.md` — fork test, reserve shortfall, de-peg, registrar failure |
| **Fallback on cessation** | ⬜ **TO BE DECIDED** — candidate: CME CF Bitcoin Reference Rate × last published wrapper factor |
| **First value** | ⬜ none |
| **History** | ⬜ **none. Zero values published.** |
| **Restatements** | 0 |
| **Version** | Rulebook v0.1 |

**Honesty note for this sheet [FIXED]:** cBTC has a liquid underlying with an open market.
This fixing exists so that **contracts have one struck moment agreed in advance**, as the
LBMA Gold Price does — not because bitcoin lacks a price. Never pitch it as price discovery.

---

## C. ETP Foundry cBTC–cETH 50/50 — draft

| Field | Value |
|---|---|
| **Name** | ETP Foundry cBTC–cETH 50/50 — `CBE50` |
| **Type** | INDEX (continuous) + OFFICIAL fixing at 16:00 UTC |
| **Class** | B — multi-asset basket |
| **What it measures** | The cost of assembling equal dollar weights of cBTC and cETH on Canton at the struck moment |
| **Currency** | USD |
| **Return type** | Price-return |
| **Constituents** | cBTC (BitSafe) 50% · cETH (onRails) 50% |
| **Weighting** | **Fixed 50/50**, restored at each rebalance |
| **Capping** | n/a — two constituents |
| **Rebalance** | Monthly, effective at the first OFFICIAL fixing of the first business day; weights published **5 business days** earlier |
| **Reconstitution** | Quarterly |
| **Constituent venues** | ⬜ **TO BE NAMED** — two **dual-listed** venues covering both legs, from: Cantex, Cantor8, Silvana, Rho, Bron, Console, Zoro, OneSwap, Walley, Loop |
| **Observation window** | 60 minutes, 12 × 5-minute partitions, per leg |
| **Calculation** | Each leg per §5.2a with its own wrapper factor; basket = Σ(weight × leg) ÷ divisor |
| **Divisor** | Adjusted so the level is unchanged at every rebalance or constituent change. History published |
| **Committee** | N=5, K=3 — BitSafe · onRails · dual-listed venue · lender · administrator. **K must include at least one of the venue or lender seats** — the two issuers may not reach quorum alone |
| **Trust levels** | ⬜ pilot **L1**, target **L2+** |
| **Failure** | Either leg unavailable → basket `NO FIXING`. No partial basket is ever published |
| **Fallback on cessation** | ⬜ **TO BE DECIDED** |
| **First value** | ⬜ none |
| **History** | ⬜ **none** |
| **Version** | Rulebook v0.1 |

**Why this one matters:** this is the benchmark that does not exist anywhere today. Both
legs trade; the basket has no observable price. It is also the cheapest to stand up — both
issuers are reachable, ten venues carry both legs so one seat covers both, and the index
half needs nobody's permission at all.

---

## D. Open fields to close before first publication

| # | Field | Blocking |
|---|---|---|
| 1 | Constituent venues named, with data-sharing agreements | **Both sheets** |
| 2 | The lender seat filled | Committee credibility on both |
| 3 | Wrapper factor derivation agreed with BitSafe / onRails | cBTC Close, CBE50 |
| 4 | Cessation fallbacks decided | Both |
| 5 | Trust level per signer recorded (§6.7) | Both |
| 6 | onRails' four facts received | CBE50 |

**The index half of CBE50 can publish before items 2, 3 and 5 are resolved**, because an
index needs no committee. That is the fastest route to a first published value — and the
first day of history.
