# Chain and Wrapper Event Policy

**Version 0.1 DRAFT · 22 September 2026 · ETP Foundry (formerly CrossDesk)**

> Applies to every benchmark in the ETP Foundry Canton Series. Nothing here is in force
> until the first published value.

CF Benchmarks publishes a Hard Fork Policy because chain events break single-asset
benchmarks and the rules must exist *before* the event, not be invented during it. Our
assets are **wrapped and represented** — cBTC, cETH, tokenised equities — so we face the
fork case **plus** four failure modes CF does not: reserve shortfall, registrar failure,
registry migration, and a de-peg of the representation from its underlying.

**[FIXED] The governing principle:** during a chain or wrapper event, **the correct output
is usually `NO FIXING`, not a brave guess.** An administrator that keeps printing through
an event it does not understand is the administrator that gets blamed for it.

---

## 1. Hard fork of an underlying chain

### 1.1 Definition
A hard fork event has occurred if **all three** hold (CF's definition, adopted):
1. two or more diverging blockchains exist post-fork sharing the same pre-fork chain,
2. the tokens on the post-fork chains are **non-fungible across chains**, and
3. the respective chains are actively validated such that transactions process at
   reasonable speed.

### 1.2 Significance test [ADOPT]
A new token is **significant** if it meets **all** of the following on **at least 2 of the
first 7 days** after the fork (CF's test, adopted unchanged — it is proven and the burden
of proof sits in the right place):

1. The new token pair trades on **at least 2 constituent venues**.
2. There are **at least 100 trades** in the new pair across all constituent venues.
3. The new pair trades at **≥10%** of the combined price of the original and new pairs.
4. The new pair's volume is **≥10%** of the combined volume of the original and new pairs.

**If significant:** we initiate calculation of a benchmark for the new token pair, and the
original benchmark continues to track the original token.
**If not significant:** no new benchmark. Starting one anyway is at the administrator's
discretion and requires committee approval.

### 1.3 During the fork window [ADOPT]
- From the fork block until the significance test resolves (max 7 days), fixings for the
  affected asset publish with flag `EXCEPTIONAL` and a **widened band**.
- If venues disagree on which chain carries the original ticker, the asset moves to
  `NO FIXING` until at least two constituent venues agree on the symbol.
- **The administrator does not decide which chain is "real."** The constituent venues'
  symbol assignment decides it. We follow the market; we do not lead it.

---

## 2. Wrapper events (the part CF does not have)

A wrapped asset is a claim. These are the ways a claim breaks.

### 2.1 Reserve shortfall [ADOPT]
The reserve attestation shows less than the issued supply, or an attestation is **missed at
its stated cadence**.

| Condition | Action |
|---|---|
| Attestation missed, < 1 cadence period late | Publish with `EXCEPTIONAL`, band widened |
| Attestation missed, ≥ 1 cadence period late | **`NO FIXING`** until attested |
| Shortfall attested, < 1% | Wrapper factor adjusted, disclosed on the tape and in a notice |
| Shortfall attested, ≥ 1% | **`NO FIXING`**; committee convenes; licensees notified same day |

**[FIXED]** A shortfall is never absorbed silently into the price. The wrapper factor is
published with its derivation, always (`6-RULEBOOK.md` §5.5).

### 2.2 De-peg [ADOPT]
The representation trades persistently away from the value of its underlying claim.

- Deviation > **2%** from the implied claim value, sustained across **3 consecutive
  fixings** → flag `EXCEPTIONAL`, widen band, notify signers.
- Deviation > **10%** sustained → committee convenes within 1 business day to decide
  between continuing with a disclosed wrapper factor, or suspending the benchmark.
- **We publish the de-peg.** A benchmark that hides a discount is worse than no benchmark:
  the lender relying on it is the party who gets hurt.

### 2.3 Registrar or issuer failure [ADOPT]
Insolvency, loss of keys, halt of mint/redeem, or withdrawal of the registrar.
- **Immediate `NO FIXING`** for that asset.
- Committee convenes within 1 business day.
- If redemption is not restored within **30 days**, the asset is removed from every index
  under `6-RULEBOOK.md` §4.5 at its last reliable price, and single-asset benchmarks on it
  enter cessation (§9 of the rulebook, 90 days' notice).

### 2.4 Registry or network migration [ADOPT]
A registrar migrates the instrument to a new registry, instrument id, or network.
- The asset's identifying tuple *(network, registrar, instrument id, redemption path)* is
  updated by **committee approval**, announced **before** it takes effect.
- Continuity is preserved: the benchmark continues without a level break, the divisor is
  adjusted if needed, and the migration is recorded in the version history.
- If continuity cannot be established, the old asset is removed and the new one treated as
  a new constituent subject to full eligibility (`6-RULEBOOK.md` §4.0–4.1).

### 2.5 Canton domain / synchronizer events [ADOPT]
A synchronizer outage, domain migration, or loss of a participant node.
- If the ledger cannot record the fixing, the fixing is **computed and published to the
  tape, flagged `EXCEPTIONAL`**, and written to the ledger when service resumes, with both
  timestamps recorded.
- If signers cannot reach the ledger to sign, §7.2 carry-forward applies.
- **Ledger unavailability is never a reason to alter a value**, only to delay its recording.

---

## 3. Underlying corporate and market events

For tokenised equities, `6-RULEBOOK.md` §4.5 governs corporate actions. In addition:

| Event | Action |
|---|---|
| Underlying market halt, single name | `NO FIXING` for that asset; index constituent held at last reliable price |
| Underlying market holiday | Normal — this is the case the 24/7 fixing exists for |
| Trading suspension > 5 days | Remove from indices at last reliable price |
| Wrapper stops passing dividends through | Methodology change under §8; factsheet updated; **licensees notified** |

---

## 4. Notification

On any event in this policy: signers notified immediately, licensees within 1 business
day, a public notice on the tape, and a record in the quarterly oversight minutes.

## 5. Definitions

**Day** — 00:00:00 to 23:59:59 UTC.
**Original token / new token** — as recognised by the constituent venues through the
trading symbols they operate, not by us.
**Constituent venue** — a venue meeting `6-RULEBOOK.md` §4.0.
**Cadence** — the attestation frequency stated in the asset's factsheet.

## 6. Version history

| Version | Date | Changes |
|---|---|---|
| 0.1 | 22 Sep 2026 | Initial draft. Fork test adopted from CF Benchmarks Hard Fork Policy v7.3; wrapper, registrar, migration and Canton sections original. |
