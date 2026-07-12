# Build Journal — Private cETH Settlement Desk

*HackCanton Season 2. Daily incremental entries — the hackathon rewards steady,
visible progress ("mana") and gives judges a trust trail. Keep entries short,
honest, and dated. Newest at the bottom.*

**How to use:** add one entry per working session. Note what you built, what you
learned, what's next, and (once deploying) paste on-ledger evidence
(transaction/update ids, contract ids). Commit after each entry.

---

## Template (copy for each new entry)

```
## YYYY-MM-DD — <short title>

**Did:**
-

**Learned / decided:**
-

**On-ledger evidence (once on Devnet):**
- update id:
- SettlementReceipt cid:

**Next:**
-
```

---

## 2026-07-11 — Project bootstrap: atomic private DvP core

**Did:**
- Scoped the project: a **Private cETH Settlement Desk** — confidential, atomic
  DvP swapping **cETH** (onRails) against tokenised cash on Canton. Targets the
  **RWA & Business Workflows** track + the **cETH bounty**.
- Built the DAML core (heavily commented, `daml test`-ready):
  - `Token.daml` — generic Canton content-token template (issuer-signatory /
    owner-observer; `Transfer` / `Split` / `Merge` / `Redeem`), instantiated as
    cETH and cash.
  - `Settlement.daml` — `SettlementProposal` with an atomic two-leg `Settle`
    (both legs re-issue in ONE transaction via delegated issuer authority) plus
    `Withdraw` / `Reject`, and a `SettlementReceipt` audit artifact written
    inside `Settle`.
  - `Agent.daml` — `TradingMandate` for agent-initiated settlement under a
    ledger-enforced limit (the agentic angle).
  - `Test.daml` — six scripts: token transfer, split/merge, the atomic DvP,
    the **privacy proof** (Eve sees nothing / can't settle), the **atomic
    rollback** proof, and the **agent-initiated** flow. Plus `initialize` for
    `daml start`.
- Wrote `README.md` (problem → why-Canton → architecture → Mermaid DvP diagram →
  build/test/deploy → demo flow), `DEPLOY.md` (Canton Devnet, human-only steps
  flagged), `docs/BUSINESS_BRIEF.md`, `docs/PILOT_PLAN.md`, `daml.yaml`,
  `.gitignore`.

**Learned / decided:**
- The load-bearing DAML decision: tokens are signed **only by the issuer** (the
  holder is an observer). That is exactly what lets a two-leg swap settle in ONE
  atomic transaction — each leg re-issues to the new owner using the issuer's
  *delegated* authority, so the incoming owner never has to co-sign. Making the
  holder a signatory would break single-transaction atomic DvP.
- The propose/accept split is how we gather **both** principals' authority:
  the maker signs the proposal; the counterparty's authority arrives as the
  `Settle` controller.
- Privacy is *demonstrated*, not asserted: the outsider test queries every
  contract as Eve and gets `None`; the auditor sees the receipt but not the
  holdings.

**Next:**
- Install the DAML SDK, set `daml.yaml → sdk-version` to `daml version`, run
  `daml test` (all six must pass), `daml build`.
- Request Canton Devnet access + test cETH via the onRails form.
- Deploy the DAR to Devnet and settle a real cETH trade; paste the update id +
  receipt cid into the next entry.

---

## 2026-07-11 — Instrument/Holding split, Market-on-Close, batch settlement

**Did:**
- Restructured the core into the Daml-Finance-shaped **three-layer** model and
  added the showcase auction module (all in the portable subset, SDK-only deps):
  - `Instrument.daml` — NEW. `InstrumentKey {issuer, depository, id, version}` +
    an `Instrument` template (kind / description / optional referencePrice, with a
    contract key). Instantiated as `DEMO:AAPL` (Equity, ref 255.0), `USD` (Cash),
    `cETH` (CryptoWrapped).
  - `Holding.daml` — NEW (replaces `Token.daml`). Issuer-signatory / owner-observer
    balance with `Transfer` / `Split` / `Merge` / `Redeem`, plus a reusable
    `deliverExact` primitive (split-off-change) that powers **partial fills**.
  - `Settlement.daml` — reworked to **propose → accept → settle**: `DvPProposal`
    (proposer signs) → `Accept` → `DvPAgreement` (BOTH sign) → a single consuming
    `Settle` moves both legs atomically. Added `SettlementBatch` (N-fill atomic
    record) + a flexible `SettlementReceipt` (bilateral-signed for DvP,
    venue-signed for auction fills) and a `FillRecord`.
  - `MarketOnClose.daml` — NEW showcase. `ClosingAuction` + **sealed** `SealedOrder`
    (signed by operator + one trader, observed by NOBODY else = dark pool) +
    `RunClose` that matches crossing pairs and batch-settles them at ONE closing
    price in a single transaction. Documented the authority mechanics: each leg
    moves inside a choice on the order the relevant trader signs
    (`CrossBuy` → buyer's cash; `DeliverAsset` → seller's asset).
  - `Agent.daml` — adapted `TradingMandate.InitiateDvP` to the new Holding/DvP
    shape (ledger-enforced per-trade cap).
  - `Test.daml` — rewrote into six scenarios: instrument+holding lifecycle,
    bilateral atomic DvP, the MOC sealed auction with a 2-cross batch (balances
    verified), dark-pool privacy (outsider AND rival participant see nothing),
    atomic rollback, and agent-initiated cETH DvP. Kept `initialize` for
    `daml start`.
- Docs: added `docs/DAML_FINANCE_INTEGRATION.md` (template-by-template mapping to
  Daml Finance V4 + the `data-dependencies` you'd add + the low-risk migration
  runbook — with an honest "pin versions to the Devnet release" caveat). Rewrote
  `README.md` (problem → solution → owner's MOC angle → three-layer architecture →
  two Mermaid diagrams → JPM/Kinexys mapping → quickstart → cETH bounty). Updated
  `DEPLOY.md` (curl SDK install, cn-quickstart, Canton Coin gas is free, cETH from
  onRails at ceth.network/contact) and `daml.yaml` (module inventory).

**Learned / decided:**
- Daml authority does NOT flow down into a nested exercise automatically — a
  choice body has exactly `signatories(contract) ∪ controllers(choice)`. So in the
  auction, the buyer's cash MUST move inside a choice on the buy order and the
  seller's asset inside a choice on the sell order; both land in one `RunClose`
  transaction, which is what makes the batch atomic.
- Kept the core **library-free on purpose**: SDK-only deps always build, and
  `DAML_FINANCE_INTEGRATION.md` makes the swap a documented, mechanical upgrade
  rather than a version-pin gamble the night before submission.

**Honest status:**
- Written but **NOT compiled in this environment** (no Daml toolchain here). The
  first `daml build` / `daml test` on an SDK machine is what confirms it; expect to
  fix at most minor syntax nits. The design and authority model are the substance.

**Next:**
- `daml test` on an SDK machine; then Devnet deploy per DEPLOY.md and paste the
  `SettlementBatch` / `SettlementReceipt` update ids as on-ledger evidence.
