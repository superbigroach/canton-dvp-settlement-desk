# Private cETH Settlement Desk

**A confidential, atomic Delivery-versus-Payment (DvP) settlement app on the
Canton Network.** Institutions swap **cETH** (wrapped Ethereum on Canton, by
[onRails](https://onrails.io)) against a **tokenised cash / deposit** —
**privately** (each party sees only its own leg) and **atomically**
(all-or-nothing, zero principal risk).

> Built for **HackCanton Season 2** — targeting the **RWA & Business Workflows**
> track and the **cETH bounty**. It is also a working reference for the exact
> problem institutional digital-assets desks (e.g. JPMorgan Kinexys / the Canton
> Network) are built to solve: privacy-preserving, atomic settlement of
> tokenised assets between known counterparties.

---

## The problem

Two institutions want to swap tokenised assets — one holds **cETH**, the other
holds **tokenised cash**. On today's rails this is hard to do well:

- **Principal (Herstatt) risk.** Someone has to move first. If you deliver and
  your counterparty defaults before paying, you are unsecured. Escrow agents and
  T+2 settlement exist precisely to paper over this.
- **No confidentiality on public chains.** On a public EVM chain the whole
  market sees your trade, your counterparty, and your position. A dealer's book
  leaking to competitors is a non-starter.
- **Bridges add risk.** Wrapping ETH onto another chain to settle usually means
  a bridge — the single most exploited component in crypto.

## The solution

A **DvP settlement desk** where the delivery leg is **cETH** and the payment leg
is **tokenised cash**, both modelled as Canton content tokens. A maker proposes a
swap to a named counterparty; the counterparty accepts; **both legs move in one
atomic transaction**, and an **immutable, bilateral audit receipt** is written in
the same transaction. Only the two principals — and a named compliance auditor —
ever see the trade.

### Why Canton specifically

| Requirement | Canton / DAML gives you… |
|---|---|
| **Atomicity (no principal risk)** | A DAML transaction commits all-or-nothing. Both legs move in ONE transaction — a half-settled state is impossible *by construction*. |
| **Privacy by default** | Sub-transaction privacy: a contract is visible only to its signatories/observers. Outsiders don't see the payload, the parties, or even that it exists. |
| **No bridge** | cETH is a first-class Canton content token. Settlement is a native ledger transfer — no cross-chain bridge in the settlement path. |
| **Known, accountable identities** | Parties are KYC'd institutions, not pseudonymous addresses — the basis for legal finality, netting, and reporting. |
| **Composability with isolation** | The cETH app and the cash app settle against each other atomically, yet each keeps its own data private (the "network of networks"). |

---

## Architecture & roles

Everything is modelled with three DAML templates plus one agentic extension:

| File | Template | What it is |
|---|---|---|
| `daml/Token.daml` | `Token` | A generic, transferable **content token** (issuer-signatory / owner-observer, `Transfer` / `Split` / `Merge` / `Redeem`). Instantiated as **cETH** (`instrument = "cETH"`) and **cash** (`instrument = "USDC"`) — the same Canton token-standard shape USDC / CBDCs use. |
| `daml/Settlement.daml` | `SettlementProposal` | The core DvP. The maker proposes *give X cETH, receive Y cash*. `Settle` moves **both legs atomically in one transaction**; `Withdraw` / `Reject` cancel. |
| `daml/Settlement.daml` | `SettlementReceipt` | The **audit / event-trail** artifact — an immutable, timestamped, bilaterally-signed record written *inside* `Settle`, observed by a compliance auditor. |
| `daml/Agent.daml` | `TradingMandate` | The **agentic** angle: a principal authorises an agent (a bot / AI / delegate) to *initiate* settlements on its behalf, capped by a ledger-enforced limit — privately. |

### The three authorisation roles

- **Issuer (signatory)** — the obligor/registrar that stands behind a token
  (onRails for cETH; a bank for cash). Its authority is required to mint and is
  delegated automatically into transfers — which is what makes atomic settlement
  possible in a single transaction.
- **Holder / owner (observer + controller)** — the current holder. Can see and
  move its own token; is not the obligor.
- **Auditor (observer)** — a named compliance party disclosed *by choice* to the
  proposal and the receipt. Sees the trades on a need-to-know basis — **not**
  either party's underlying holdings or the rest of their book.

> **The load-bearing design decision**: tokens are signed **only by their
> issuer** (the holder is an observer). That is what lets a two-leg swap settle
> in ONE atomic transaction — each leg re-issues to the new owner using the
> issuer's *delegated* authority, with no need for the incoming owner to co-sign.
> Making the holder a signatory would break single-transaction atomic DvP. This
> is the canonical Digital Asset holding model; every DAML comment in the repo
> explains the *why*, not just the *what*.

### How it uses cETH (the bounty)

cETH is the **delivery leg** of every settlement. Each `Settle` performs a real
cETH `Transfer` (a genuine on-ledger state change of a cETH holding), plus a
`Split`/`Merge` path for exact-amount delivery. Running the demo on Devnet drives
real cETH state-change activity — mint, transfer, settle — which is exactly what
the cETH bounty rewards.

---

## The atomic DvP (sequence diagram)

Alice (maker) delivers 5 cETH and receives 12,500 USDC from Bob (counterparty),
in a single atomic transaction. A Regulator audits; Eve (an outsider) sees
nothing.

```mermaid
sequenceDiagram
    autonumber
    actor Alice as Alice — Maker (holds cETH)
    actor Bob as Bob — Counterparty (holds cash)
    participant Reg as Regulator (auditor)
    participant Eve as Eve (outsider)
    participant Ledger as Canton Ledger + Synchronizer

    Note over Alice,Bob: Setup — holdings exist privately on each holder's participant
    Ledger-->>Alice: Token cETH x5   [signatory: Issuer, observer: Alice]
    Ledger-->>Bob: Token USDC x12,500 [signatory: Issuer, observer: Bob]

    Note over Alice,Reg: Proposal — Alice offers the swap; Bob + Regulator are observers
    Alice->>Ledger: create SettlementProposal(maker=Alice, counterparty=Bob, auditor=Reg, 5 cETH ⇄ 12,500 USDC)
    Ledger-->>Bob: (can see the proposal)
    Ledger-->>Reg: (can see the proposal — need-to-know)
    Note over Eve: Eve sees NOTHING — not the payload, not that it exists

    Note over Bob,Ledger: Acceptance — ONE atomic transaction: both legs or neither
    Bob->>Ledger: exercise Settle
    activate Ledger
    Ledger->>Ledger: leg 1 — cETH Transfer → Bob
    Ledger->>Ledger: leg 2 — cash Transfer → Alice
    Ledger->>Ledger: write SettlementReceipt (signed Alice+Bob, observed by Reg)
    Ledger-->>Bob: SettleResult(deliveredCeth, deliveredCash, receipt)
    deactivate Ledger

    Note over Alice,Bob: Result — Bob owns the cETH, Alice owns the cash.<br/>If EITHER leg failed, BOTH roll back (no principal risk).
    Ledger-->>Reg: sees the receipt (the trade) — but NOT the underlying holdings
```

---

## Build & test

### 1. Install the DAML SDK

Follow <https://docs.daml.com/getting-started/installation.html>, then:

```bash
daml version
```

Copy the printed version into **`daml.yaml` → `sdk-version:`** (it ships as a
placeholder).

### 2. Run the test suite

```bash
cd hackcanton-ceth-settlement
daml test
```

`daml test` compiles the project and runs every `Script` in `daml/Test.daml`:

- `testTokenIssueAndTransfer` — mint cETH, transfer it, verify archival.
- `testTokenSplitAndMerge` — split/merge holdings (standard token ops).
- `testAtomicDvP` — the headline: 5 cETH ⇄ 12,500 USDC settles atomically + the audit receipt is written; the auditor sees the receipt but **not** the holdings.
- `testOutsiderCannotSeeOrSettle` — **privacy proof**: Eve sees nothing and cannot settle.
- `testAtomicRollback` — a price mismatch rolls the **entire** settlement back; the maker keeps her cETH.
- `testAgentInitiatedSettlement` — an agent initiates a settlement within a ledger-enforced mandate limit; over-limit fails.

### 3. Explore interactively

```bash
daml start
```

This builds the DAR, starts a local Canton sandbox, runs `Test:initialize`
(allocates Issuer / Alice / Bob / Regulator / Agent / Eve and seeds a live DvP
proposal), and opens **Navigator** at <http://localhost:7500>. Log in as each
party to *see for yourself* which contracts each can and cannot see — then log in
as Bob and exercise `Settle` to watch the atomic swap happen.

Useful individual commands:

```bash
daml build      # compile to a .dar (in .daml/dist/)
daml studio     # open VS Code with the DAML IDE + inline script results
```

---

## Deploy to Canton Devnet

Deploying the DAR to **Canton Devnet** and executing a real settlement is the
hackathon's qualifying requirement. Full step-by-step (including the steps only
you can do — getting Devnet credentials and cETH via the onRails form) is in
**[DEPLOY.md](./DEPLOY.md)**.

---

## Demo flow (for judges / an institutional audience)

1. **Privacy** — open Navigator; show Eve's blank view next to Alice's and Bob's.
2. **Propose** — as Alice, create the 5 cETH ⇄ 12,500 USDC proposal. Show that
   Bob and the Regulator can see it; Eve still cannot.
3. **Settle atomically** — as Bob, exercise `Settle`. Both legs flip in one
   transaction; the receipt appears.
4. **Audit** — as the Regulator, show you can see the *receipt* (the trade) but
   **not** the underlying cETH/cash holdings — need-to-know compliance.
5. **No principal risk** — run `testAtomicRollback` live: a bad leg rolls the
   whole thing back; Alice keeps her cETH.
6. **Agentic** — run `testAgentInitiatedSettlement`: an AI/bot agent initiates a
   trade within its mandate, privately, and it settles.

---

## Further reading

- **[DEPLOY.md](./DEPLOY.md)** — Canton Devnet deployment.
- **[docs/BUSINESS_BRIEF.md](./docs/BUSINESS_BRIEF.md)** — the 1-page RWA brief (ICP, use case, who pays, value).
- **[docs/PILOT_PLAN.md](./docs/PILOT_PLAN.md)** — a short pilot plan.
- **[JOURNAL.md](./JOURNAL.md)** — the daily build journal.

## Glossary

- **cETH** — wrapped Ethereum represented as a native Canton content token (by onRails).
- **DvP** — Delivery-versus-Payment: the asset leg and the cash leg settle atomically.
- **Party** — an on-ledger identity (a KYC'd institution or desk).
- **Signatory / Observer / Controller** — DAML's authorisation model: *on the hook + can see* / *can see only* / *may pull this lever*.
- **Active Contract Set (ACS)** — the set of currently-live (unarchived) contracts.
- **Synchronizer** — Canton's ordering + delivery layer; routes encrypted per-party views, never sees contract data.

---

*Licensed for hackathon and evaluation use. cETH is a product of onRails; Canton
and DAML are products of Digital Asset. This project is an independent submission.*
