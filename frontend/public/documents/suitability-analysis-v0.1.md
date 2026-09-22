# Suitability Analysis — is this fit to underlie a regulated financial product?

**Version 0.1 DRAFT · 22 September 2026 · ETP Foundry (formerly CrossDesk)**

CF Benchmarks publishes a *Suitability Analysis for the Creation of Regulated Financial
Products* for the BRR, because an issuer's board and its regulator will both ask the same
question before a product launches: **is this number good enough to build a fund on?**

This document answers that question about our own benchmarks. **Today the answer is no,
and the useful part of the document is exactly why, and what changes it.** An analysis that
concluded "yes" on day one would tell a reader only that we are not serious.

---

## 1. The test

A benchmark is suitable to underlie a regulated product when it is:

| # | Criterion | What it means |
|---|---|---|
| 1 | **Representative** | It measures the economic reality it claims to measure |
| 2 | **Resistant to manipulation** | No participant can move it at acceptable cost |
| 3 | **Replicable** | A market maker can trade into it, so arbitrage keeps the product honest |
| 4 | **Data-sufficient** | Enough real transactions, from enough independent sources |
| 5 | **Governed** | Documented methodology, oversight, conflicts, complaints, audit trail |
| 6 | **Continuous** | An unbroken published history, with known behaviour in stress |
| 7 | **Robust to cessation** | Consumers know what happens if it stops |

## 2. Assessment, honestly

| # | Criterion | Status | Detail |
|---|---|---|---|
| 1 | Representative | 🟡 **Design yes, evidence no** | The construction observes actual transactions on venues where the token trades. Untested against live data. |
| 2 | Manipulation-resistant | 🟢 **Structurally strong** | Volume-weighted median per partition, 12 partitions, venue-level 5% screen, and a **K-of-N committee of opposed interests** — the last is stronger than any purely computational benchmark, because a party wanting a high mark faces a lender wanting a low one. |
| 3 | Replicable | 🟢 **Yes by design** | A 60-minute window with 5-minute partitions can be traded into. The sealed auction prints a clearing price an AP can hit. |
| 4 | Data-sufficient | 🔴 **Not yet** | Canton venues are young and thin. Two independent venues is the floor and we are near it. **This is the binding constraint.** |
| 5 | Governed | 🟡 **Documented, not exercised** | Rulebook, benchmark statement, chain-event policy, restatement and cessation policies all exist. **No committee has met. No oversight minutes exist.** |
| 6 | Continuous | 🔴 **No** | **Zero published values.** No history, therefore no stress behaviour. |
| 7 | Cessation-robust | 🟢 **Policy exists** | 90 days' notice, named fallback, transferable methodology. Untested. |

**Overall: NOT SUITABLE to underlie a regulated financial product as of this version.**

The two blockers are **(4) data sufficiency** and **(6) continuity**, and only one of them
is in our control.

## 3. What changes the answer

| Blocker | What resolves it | Controlled by us? |
|---|---|---|
| **Continuity** | 12 months of unbroken daily publication, including at least one stress episode, published with the full fixing record | **Yes — start today, cost is uptime** |
| **Data sufficiency** | 3+ independent constituent venues each clearing the volume threshold, sustained | No — depends on Canton market growth |
| **Governance exercised** | 4 quarterly oversight meetings held and minuted; at least one methodology change made properly through the notice process | **Yes** |
| **Independent assurance** | An external audit against IOSCO principles | Yes, when a licensee funds it (~$40–80K) |
| **Regulatory status** | UK authorisation if a licensee's domicile requires it (£2,820, Cat. 4, 4 months) | Yes, when needed |

**The honest timeline: roughly 12 months of publishing from the first value**, assuming
venue growth cooperates. Nothing shortens it, because the missing ingredient is time under
observation, and that cannot be bought.

## 4. What the benchmarks *are* suitable for today

Suitability is not binary. With no history and a documented methodology, these values are
already appropriate for:

- **Internal risk and collateral monitoring** — a lender comparing our mark to its own
- **Shadow NAV** run in parallel with an issuer's existing process, proving nothing to
  regulators but proving a great deal to the issuer
- **Margin and haircut inputs** where the consumer accepts an `INDICATIVE` label
- **Product design and analysis**

They are **not** appropriate for: a fund's published NAV, creation/redemption settlement in
a regulated wrapper, audited financial reporting, or any use where "this is a benchmark for
regulatory purposes" would be asserted.

**This is precisely why the first commercial engagement should be a design partner or a
free lender shadow run, not a live fund.** The product is sold on the strength of the
methodology and the trajectory, and it becomes suitable while the partner uses it in
parallel.

## 5. What to say to an issuer who asks

> "Not yet, and here's the file that says why. The construction is the same one the CME CF
> Bitcoin Reference Rate uses, the committee design is stronger than a pure calculation
> because the signers' interests oppose each other, and the governance documents are
> written. What's missing is history — which is a matter of time, not of effort, and it
> starts the day you say yes to a shadow run."

**Never** claim suitability we do not have. The one asset a benchmark administrator cannot
rebuild after losing it is the assumption that it tells the truth about itself.

## 6. Review

Reassessed quarterly at the oversight meeting, and on any material methodology change.
Superseded versions remain published so the trajectory is visible.

| Version | Date | Overall conclusion |
|---|---|---|
| 0.1 | 22 Sep 2026 | Not suitable — no published history, data sufficiency at the floor |
