# Build Journal — Canton DvP Settlement Desk

*A personal learning/demo project on Canton. Daily incremental entries — a short,
honest, dated trust trail of what was built and why. Keep entries short and dated.
Newest at the bottom.*

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
- Scoped the project: a **Canton DvP Settlement Desk** — confidential, atomic
  DvP swapping **cETH** (onRails) against tokenised cash on Canton. A hands-on
  model of the RWA / business-workflow settlement problem.
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
  two Mermaid diagrams → JPM/Kinexys mapping → quickstart → cETH via onRails). Updated
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
  rather than a version-pin gamble.

**Honest status:**
- Written but **NOT compiled in this environment** (no Daml toolchain here). The
  first `daml build` / `daml test` on an SDK machine is what confirms it; expect to
  fix at most minor syntax nits. The design and authority model are the substance.

**Next:**
- `daml test` on an SDK machine; then Devnet deploy per DEPLOY.md and paste the
  `SettlementBatch` / `SettlementReceipt` update ids as on-ledger evidence.

---

## 2026-07-11 — Compiled green on SDK 2.9.4; fixed DvP visibility, reworked MOC economics

**Did:**
- Installed the Daml SDK (2.9.4) and actually **built + tested**. First run: 6/8
  scripts passed; the two DvP scripts failed and MOC printed divulgence warnings.
- **Fixed atomic DvP visibility (the "allocate" step).** `DvPAgreement.Settle` is
  triggered by the proposer, who could not *see* the counterparty's cash holding
  (a `Holding` is visible to issuer + owner only), so `fetch` aborted with "not
  visible to the reading parties." Added a `Disclose` choice on `Holding` (owner
  adds an observer, pruning-safe) and made `DvPProposal.Accept` **allocate** the
  cash leg by disclosing it to the proposer — captured as the agreement's cash cid.
  Both DvP scripts now settle. Same mechanism fixed `testAgentInitiatedDvP` (the
  principal discloses the asset to the agent so the agent can validate it).
- **Reworked Market-on-Close economics to be correct.** MOC is now a genuine
  uniform-price call auction: the operator can no longer pick a price per fill —
  every fill prints at the auction's published `referencePrice` (the official
  close). `RunClose` crosses `min(totalBuy, totalSell)`, **rations the heavy side
  pro-rata** (`quantity * matched / heavyTotal`), fills the light side fully, and
  leaves the residual unsettled. It clears through the venue as a momentary CCP in
  one atomic tx: **pledge** each pro-rated leg (inside the trader's own order, so
  authority is present) → **pool** (merge) → **deliver** to the other side.
- **Removed all divulgence.** `SubmitOrder` now discloses each committed holding to
  the operator, so the close reads/moves holdings through real visibility, not the
  deprecated divulged-contract path. `daml test` is now warning-free.
- **Tests:** added `testMarketOnCloseImbalance` (sell-heavy 30 vs 20 → seller
  filled 20, keeps 10; buyers full; all @ 255) asserting pro-rata + uniform price;
  updated `testMarketOnClose` (balanced) + `testAtomicRollback` / `testDarkPoolPrivacy`
  to the allocation model. **All scripts pass, no warnings.**
- **Renamed off hackathon framing.** Package `private-ceth-settlement` →
  `canton-dvp-settlement-desk` (DAR output too); retitled README / JOURNAL / doc
  headers to "Canton DvP Settlement Desk" and reframed as a personal learning/demo
  build (kept cETH + onRails/Devnet notes). Added README **Run it locally** and
  **Share it / deploy to Devnet** sections.

**Learned / decided:**
- AUTHORITY ≠ VISIBILITY. Both DvP principals sign the agreement (authority to move
  both legs), but the single submitter still needs *visibility* of each contract it
  touches — hence the explicit `Disclose`/allocate step, which is also the honest,
  pruning-safe replacement for divulgence.
- Pooling through the venue-as-CCP (all within one transaction) is what makes a
  many-to-many pro-rata cross expressible while keeping each leg's move inside the
  choice where that trader's authority lives — and keeps the whole close atomic.

**Status:** `daml build` ✅ · `daml test` ✅ (all scripts `ok`, zero divulgence
warnings) on SDK 2.9.4.

**Next:**
- Devnet deploy per DEPLOY.md; paste the `SettlementBatch` / `SettlementReceipt`
  update ids as on-ledger evidence.
