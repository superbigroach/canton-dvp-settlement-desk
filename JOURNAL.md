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
