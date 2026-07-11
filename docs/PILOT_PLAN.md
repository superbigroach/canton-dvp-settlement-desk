# Pilot Plan — Private cETH Settlement Desk

A short, concrete path from this hackathon prototype to a live pilot with one or
two institutional counterparties on Canton.

---

## Pilot goal

Settle **real cETH against a tokenised cash leg**, bilaterally and privately,
between two participants on Canton Devnet/Testnet — with a compliance observer —
and produce an auditable settlement record for each trade.

---

## Step 1 — Single-participant proof (Week 1)

**Scope:** all parties on one participant node; cETH sourced from onRails on
Devnet.

- Deploy the DAR to a Devnet participant (see `DEPLOY.md`).
- Obtain real cETH to the maker party via the onRails form; model the cash leg
  with a pilot-controlled issuer.
- Run the full lifecycle on-ledger: **propose → settle → receipt**, and capture
  the transaction id + receipt as evidence.

**Required integrations:** DAML SDK, Canton Devnet participant, onRails cETH
registry (issuer party id + instrument identifier).

**Exit criteria:** a confirmed atomic cETH↔cash settlement and a
`SettlementReceipt` visible to the auditor but not to outsiders.

## Step 2 — Two-participant, cross-node settlement (Weeks 2–3)

**Scope:** maker and counterparty on **separate participant nodes**, settling via
the Devnet synchronizer — the real institutional topology.

- Allocate the counterparty on a second participant; connect both to the same
  synchronizer.
- Wire **explicit disclosure** so the settling participant can see the maker's
  cETH leg at `Settle` time (the production disclosure pattern).
- Add the compliance party on a third node as a settlement observer.

**Required integrations:** a second (and third) Canton participant; explicit
disclosure in the settlement submission; JSON API / gRPC client for each desk's
back office.

**Exit criteria:** an atomic settlement where each party's participant holds only
its own leg, and the auditor node receives only the receipt.

## Step 3 — Workflow, limits & agentic access (Weeks 4–6)

**Scope:** productionise the desk workflow and the agentic add-on.

- Add per-desk **mandates** (`TradingMandate`) so an authorised agent/bot can
  initiate settlements under ledger-enforced limits; add revocation + an audit
  trail of agent actions.
- Fee handling: attach a bps settlement fee leg; reporting export of receipts.
- Ops: monitoring, key management, party onboarding runbook, and a compliance
  dashboard that reads only the receipts stream.

**Required integrations:** identity/KYC provider for party onboarding; secrets/KMS
for participant keys; a reporting sink for the receipts stream; (optional) the
agent runtime (bot / AI assistant) that calls `InitiateSettlement`.

**Exit criteria:** a repeatable, limit-governed, audited settlement service two
counterparties can run against daily.

---

## Success metrics

- Time-to-settle (target: sub-second finality on-ledger vs. T+2 today).
- Zero principal-risk incidents (guaranteed by atomicity — verify no half-settled
  states ever occur).
- Zero information-leakage (outsider queries return nothing; auditor sees only
  receipts).
- Settlement volume + notional processed; number of counterparties onboarded.
