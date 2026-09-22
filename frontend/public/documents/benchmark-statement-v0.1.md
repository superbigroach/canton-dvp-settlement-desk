# Benchmark Statement — ETP Foundry Canton Series

**Version 0.1 DRAFT · 22 September 2026 · ETP Foundry (formerly CrossDesk)**

> **No benchmark described here is published, live, or in use.** This statement is written
> in advance of first publication so that the rules exist before the numbers do. It becomes
> effective on the date of the first published value and not before.

A benchmark statement is what a licensee's risk and compliance team reads instead of the
methodology: what the number measures, what it does *not* measure, who should use it, when
it publishes, and what happens if it stops. It follows the structure UK/EU BMR requires of
authorised administrators (Art. 27) and the shape used by CF Benchmarks.

---

## 1. Version history

| Version | Date | Changes |
|---|---|---|
| 0.1 | 22 Sep 2026 | Initial draft. Not effective. |

## 2. Introduction

**Administrator:** ETP Foundry (formerly CrossDesk). Operating from Ontario, Canada.
**Contact:** s.borjas@lucilla.ca · etpfoundry.com

**Regulatory status — stated plainly:** ETP Foundry is **not an authorised or registered
benchmark administrator in any jurisdiction**. It is not supervised by the FCA, ESMA or any
other authority. Nothing published is a benchmark for regulatory purposes. See §9 and
`6-RULEBOOK.md` §12.

**Series covered:** the ETP Foundry Canton Series — fixings and indices for tokenised
assets recorded on the Canton Network, and for baskets of them.

## 3. Benchmark description and aims

**What these benchmarks seek to measure:** the price at which a tokenised asset, or a
defined basket of tokenised assets, could be exchanged at a defined moment — observed from
transactions on constituent venues, and, where no observable price exists, attested by a
committee of parties holding opposing economic positions against the mark.

**The underlying economic reality:** for a single wrapped asset (e.g. cBTC), it is the
exchange of that token for its quote asset on venues where it actually trades. For a basket,
it is the cost of assembling the constituent tokens in their stated weights. **We price the
token, not the asset it wraps or represents** — the wrapper's redemption friction and
reserve condition are reflected through the wrapper factor (`6-RULEBOOK.md` §5.5), never
silently.

**The gap these exist to fill:** a basket of tokenised assets has no observable price even
when every constituent does, and a tokenised asset whose home market is closed has no
reference price at all for roughly 80% of each week. Both are states in which an oracle has
nothing to relay.

## 4. Methodology summary

Full rules: `6-RULEBOOK.md`. In brief:

- **Indices** are computed from published rules and observable inputs. No committee.
- **Fixings** are attested by **K of N** signers drawn from four seats — issuer, custodian,
  risk taker, market-facing party — who already hold positions against the mark. Recommended
  N=5, K=3. **The issuer may never reach quorum using only parties it controls.**
- **OFFICIAL fixings** use a 60-minute observation window, partitioned into 12 × 5-minute
  intervals; the volume-weighted median of each partition is computed across all constituent
  venues, and the fixing is the equally-weighted mean of those 12 medians. This is the
  construction used by the CME CF Bitcoin Reference Rate.
- **Minimum two independent constituent venues.** Below that, no value is published.
- Every value publishes an **uncertainty band**, the inputs used, and which were stale.

## 5. General disclosure — limitations

**This section is the honest one and is not to be softened.**

1. **No track record.** As of this version, no value has been published. A benchmark's
   reliability is demonstrated by an unbroken history, and ours does not yet exist.
2. **Thin markets.** Canton-native tokenised assets trade on young venues with modest
   volume. A benchmark can be no more robust than the market it observes.
3. **Wrapper risk.** For wrapped assets, the value depends on a reserve we do not hold and
   an attestation we do not produce. A reserve shortfall is a price event we can report but
   not prevent.
4. **Committee dependence.** A fixing requires K signatures. If signers are unavailable, no
   value is struck and the prior value is carried forward, flagged and aged.
5. **Judgement, where permitted** (illiquid and spread classes only), is a stated opinion,
   not an observation, and is labelled as such.
6. **Concentration.** With few venues, a single venue may represent a large share of
   observed volume. The venue-level screen (`6-RULEBOOK.md` §5.2b) limits, but does not
   eliminate, the effect.
7. **Not a valuation of the underlying.** A cBTC fixing is not a bitcoin price. A tokenised
   equity fixing is not the share price. Users who need the underlying should use a
   benchmark for the underlying.
8. **Not suitable, yet, to underlie a regulated financial product.** See
   `9-SUITABILITY-ANALYSIS.md`.

## 6. Usage of the benchmark

| Type | May be used for | May **not** be used for |
|---|---|---|
| `INDEX` | information, analysis, product design | settlement, NAV |
| `INDICATIVE` fixing | margin, haircuts, health factors, collateral monitoring | **NAV, creation/redemption, audited records** |
| `OFFICIAL` fixing | NAV, creation/redemption, reporting, contract settlement | — |

**Prohibited representation [FIXED]:** no licensee may present an `INDICATIVE` value as a
NAV. This is a term of the licence.

**Licence classes:** use/reference, redistribution, display. Reading the value through an
oracle or data vendor does not substitute for a use licence.

## 7. Publication timings

Per `6-RULEBOOK.md` §5.1: OFFICIAL at the asset's home-market close (Canton-native assets:
16:00 UTC daily); INDICATIVE at 00:00 / 08:00 / 16:00 UTC every day including weekends.
Times are declared once and moved only as a methodology change under §8.

Values are published to the ledger with their full fixing record, and to the public tape.

## 8. Changes to, and cessation of, the benchmarks

**Changes:** proposed on-ledger, require the same K of N, and take effect after 5 business
days' notice; material changes require 30 calendar days and written notice to every
licensee. The rulebook is versioned and every value carries the version that produced it.
**No change is ever retroactive.**

**Cessation:** minimum 90 days' notice before ceasing any OFFICIAL benchmark, to licensees
and publicly. Each factsheet names a recommended fallback. The methodology and history may
be transferred to a successor administrator. Records survive for 7 years.
Full policy: `6-RULEBOOK.md` §9.

## 9. Oversight

The committee is the oversight body until an independent one exists. It meets quarterly on
a fixed agenda (`1-operations-runbook.md` §4), reviews every restatement, carry-forward,
judgement use and venue change, and publishes summary minutes to signers and licensees.

**Conflicts:** any interest of the administrator or a signer in a priced asset is disclosed
in the conflicts register and, where material, disqualifies that party from signing.

**Complaints:** any licensee or signer may challenge a value in writing. Acknowledgement
within 2 business days, substantive response within 10, escalation to the quarterly meeting
if unresolved, minuted either way.

## 10. Updates to this statement

Reviewed at least annually and on any material methodology change. Superseded versions
remain published.
