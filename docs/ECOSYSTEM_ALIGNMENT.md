# Ecosystem alignment — two funded proposals that change our plan, and one we should adopt

Read from `github.com/canton-foundation/canton-dev-fund` (public, CC0) on **13 August 2026**. Both
are **Approved**, so this is funded work in flight, not speculation. Each changes something we were
about to do, which is cheaper to learn now than after building.

---

## 1. Bit Dynamics — reference settlement pattern and reference DEX

`2026-03-Srikanth-reference-implementation-of-settlement-pattern-and-reference-dex-implementation.md`
· Author Srikanth, org **Bit Dynamics** · created 2026-03-19, **approved 2026-05-06** · PR #108

**What it funds:** an open-source, production-shaped reference for building trading and liquidity
workflows on Canton — matched OTC/RFQ settlement, prefunded orders, liquidity pools, LP tokens, on a
public testnet others can inspect. Explicitly *not* a hosted exchange business and not a Uniswap
clone; the contribution is the **Daml workflow pattern**.

**Built on Token Standard V2** — `V2.Allocation` for holdings, allocations and settlement, plus
registry-backed `InstrumentConfiguration`.

### What it changes for us — two things, and the second is important

**① It confirms V2 is the direction, and we vendor V1.** We vendor `splice-api-token-*-v1` because v1
is what is vetted on the networks today. A funded reference implementation is being built on **V2**.
Combined with `2026-03-DA-token-standard-v2.md` also being funded, the move is not hypothetical.
**Hence the one question worth asking any issuer: what is your V2 timeline?**

**② 🔴 It is a free, funded reference for the exact thing Gap A needs.** Our own
`docs/TOKEN_STANDARD_DVP.md` §5 already says in-kind creation "maps onto allocations plus the v2
burn/mint interface". Bit Dynamics is being paid to demonstrate precisely that pattern —
prefunded, allocation-backed settlement — and to publish it.

**So the Gap A plan changes.** Building the CIP-56 basket adapter blind against V1 risks writing the
wrong thing twice. The better sequence:

1. get a participant (still the blocker),
2. read the Bit Dynamics reference once published,
3. build the adapter on **V2 allocations**, following a pattern the ecosystem has already blessed.

**It is not a competitor to the fixing.** It is a DEX/AMM/liquidity reference — continuous trading.
Our product is the *auction fixing and the fund layer*. It **is** adjacent to our continuous order
book, which is already parked commercially — this is one more reason that call was right.

---

## 2. Kaiko — the Data Standard. **Adopt this.**

`2026-05-Kaiko-data-standard.md` · Author Desmonty-Kaiko, org **Kaiko** · created 2026-03-20,
**approved 2026-05-27** · PR #113

**Who Kaiko are:** an institutional crypto market-data company — they sell price data to banks.

**What it funds:** publication of the **Kaiko Data Standard** into **Splice**, making it an
ecosystem-wide common good. It is a shared interface for **how a data provider publishes data on
Canton and how applications read it** — the proposal's own analogy is a common plug standard: any
oracle that implements it can be consumed by any Canton application with no bespoke integration, and
any application built on it can switch or add providers without changing code.

Three facts that make this more than a proposal: it is **already deployed in Kaiko's production
oracle and in use by institutional clients**; it was built **in close collaboration with Digital
Asset**; and it has **adoption interest from DRW and Cumberland**.

### Why this matters more to us than anything else in the fund

**Its scope explicitly includes "asset prices, reference rates, market indicators".** A reference
rate is exactly what a CrossDesk fixing is.

- **It is the distribution rail for our number, not a rival.** Kaiko *sells* data; CrossDesk
  *produces* a price from a sealed auction and a K-of-N attestation. Their standard is the transport
  and interface layer — the layer we have said all along not to compete with, because Chainlink and
  RedStone already own it. Now it is being standardised, funded, and put into Splice.
- **It answers "how would anyone consume your fixing?"** Today the answer is "integrate with my
  API". If we implement the Standard, the answer becomes **"we are on the standard rail — read us the
  way you read anyone."** That is the difference between a benchmark someone must adopt bespoke and
  one they can reference by default.
- **DRW and Cumberland watching it are the buyers of reference rates.** That is our customer set
  looking at the pipe our number would travel down.

### What to do

1. **Watch for the Standard landing in Splice** (it is a funded milestone), then read the interface.
2. **Publish the fixing over it.** Implementing a published interface for output we already produce is
   small work with disproportionate reach.
3. **It is also the strongest grant angle we have** — "a reference implementation of a K-of-N attested
   fixing, published over the Kaiko Data Standard" is a common good, in a funded category
   (reference implementations), complementary to work the Foundation has already backed rather than
   duplicative of it.

⚠️ Do not claim to implement it before reading the published interface. It is not in Splice yet.

---

## 3. Decentralization Manager — the design, so we are ready

BitSafe's Apache-2.0 framework for managing Canton **Decentralized Parties**: a party whose namespace
key is split across independent operators, so no single one can act as it. Grant 1 shipped party
setup, a **Generalized Governance Core**, and Token Management + Custody **Daml** templates, with cBTC
governance on MainNet as the proof point. Phase 2 is in flight and its **milestone 5 is "ecosystem
adoption by teams other than BitSafe."**

### The problem it solves for us, which exists regardless of BitSafe

`ClosingAuction.operator` and `BasketDefinition.administrator` are **single parties holding single
keys**. Our K-of-N committee proves *K parties agreed the number*; it says nothing about the venue
itself. So today:

- a customer must trust the operator personally;
- losing that key loses the venue;
- and the honest answer to *"what stops you re-running the close in your favour?"* is **nothing
  technical**.

Making the operator a Decentralized Party makes "the venue cannot act alone" arithmetic rather than a
promise — and the customer can be one of its signers.

| | What is protected |
|---|---|
| Our `OperatorCommittee` | **the number** — K of N sign the valuation |
| A Decentralized Party operator | **the venue's identity** — no single operator can act as it |

### 🟢 The good news: no template changes

**A party is a party.** Nothing in our Daml inspects how a party's key is hosted, so
`operator = <a decentralized party>` type-checks and behaves identically. Every read path — the
per-party queries that make the dark book dark — is unchanged, because they filter by party id and a
Decentralized Party has an ordinary party id.

### ⚠️ The part that genuinely needs a node

**Writes as the operator are where it gets real.** The backend submits commands *as* the operator. A
single-key party submits alone; a Decentralized Party's submission must satisfy its threshold, which
is exactly what Decentralization Manager's coordinator exists to do — collect the required signatures
before ledger submission. So:

- **`CloseBidding`, `RunClose`, `ProcessCreation` and `ProcessRedemption`** are operator- or
  administrator-controlled writes. Under a decentralized operator each becomes a coordinated
  submission rather than a direct one.
- 🔴 **`FinalizeFixing` is deliberately NOT in that list.** It is `controller proposer`
  (`Governance.daml:542`) — a committee member, not the operator — and the backend submits it
  as that member. An earlier version of this document had it wrong.
- ⚠️ **A close is TWO operator submissions** (`CloseBidding` then `RunClose`), and creation
  and redemption are two administrator submissions each (approve, then process). Any latency
  budget assuming one submission per operation has half the headroom it thought.
- That changes **how the backend submits**, not what the Daml says.
- 🔴 **This cannot be designed on paper alone and must not be claimed as working.** It needs a
  participant, a Decentralized Party, and DM running, and it needs the latency measured — a
  threshold-coordinated write will be slower than the ~7.5s already observed on a shared node, and a
  fixing has a strike time.

### The honest position for a conversation

We have **not** used Decentralization Manager. Our committee is our own Daml — same threshold
*pattern*, different code and a different layer. The credible offer is:

> The venue operator is the weakest point in my design: one party, one key. Making it a Decentralized
> Party on your Governance Core fixes it, and the customer can be one of the signers. Your framework
> governs custody; my committee governs valuation — the same threshold pattern one layer apart. I
> haven't built it: I need a participant first, and the open question is what threshold coordination
> does to write latency when a fixing has a strike time.

"Here is exactly how I would use it, and the one thing I do not yet know" is a stronger position than
a claim an engineer can puncture in one follow-up.
