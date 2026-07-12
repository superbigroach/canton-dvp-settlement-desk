# Business Brief — The Closing Auction for Tokenised Assets

*One-page brief. A sealed, uniform-price **Market-on-Close auction** — plus atomic
DvP settlement — for tokenised assets on the Canton Network.*

---

## What it is

A **sealed call auction** for tokenised assets, run as **both the opening cross
(Market-on-Open) and the closing cross (Market-on-Close)**. Traders lodge **sealed**
buy/sell interest; at the open or the close the venue crosses everyone at **one
uniform price**, rations any imbalance pro-rata, and settles every fill **atomically**
(delivery-versus-payment, all-or-nothing). The order book is **private by
construction** — no participant sees another's order until after the cross. The
single price the auction produces **is the official open** (at the opening cross) or
the **official close / NAV** (at the closing cross) — the same mechanism prints the
authoritative mark at both ends of the session.

Two things fall out of one mechanism:
1. **Fair large-order execution** — size trades at one price, with no market impact
   and no front-running.
2. **An authoritative reference price** — the auction *produces* the official daily
   close that funds use for NAV and derivatives settle against.

Atomic **DvP** is the settlement rail underneath; the **sealed call auction** is the
product on top.

## The problem

- **Continuous venues leak and get front-run.** A large resting order on a public
  order book — or in a blockchain mempool — is a searcher's paradise: the price
  gaps against it before it fills (MEV / sandwiching). You cannot run an honest
  large-in-scale trade where the book is visible.
- **Tokenised funds have no trustworthy on-chain close.** A tokenised MMF or fund
  needs a **daily official price** for NAV, redemptions, and collateral marks —
  but there is no neutral, private, auditable mechanism on-chain that *produces*
  one from real supply and demand.

## The solution — one sealed call auction

- **Sealed orders → uniform-price cross.** Interest is hidden until the close;
  everyone prints at the same price; the heavy side is rationed pro-rata. No
  impact, no front-running, no MEV — a batch auction, the known on-chain answer to
  MEV (cf. CoW / Gnosis), made *private* by Canton.
- **The price is the product.** The clearing price is the **official close / NAV** —
  discovered from real orders, no external oracle required.
- **Atomic DvP settlement.** Every matched fill settles all-or-nothing in one
  transaction — zero principal (Herstatt) risk, instant finality — with an
  immutable, need-to-know audit receipt.

## Ideal Customer Profile (ICP)

1. **Tokenised-fund & asset operators** (tokenised MMFs, RWA funds — e.g. the JPMorgan
   Kinexys / tokenised-MMF world) that need a **private, auditable daily close/NAV**.
2. **Institutional desks & large holders** executing **block-size** tokenised trades
   without moving the market or leaking positions.
3. **Market infrastructure / venues & custodians** running a neutral, compliant
   **closing-cross** for their members.

## Who pays, and for what

- **Fund operators** pay for **close-price / NAV-as-a-service** — a private,
  auditable daily auction that establishes the official mark (per-auction or SaaS).
- **Institutional desks** pay a **per-auction execution fee** (bps on notional) for
  fair, impact-free, MEV-proof block execution at the close.
- **Venue operators** pay a **platform / SaaS** fee to run a branded, multi-tenant
  closing cross with compliance observers wired in.
- **Optional:** metered **agentic** access — AI-agent-initiated orders/settlement
  under ledger-enforced mandate limits.

## Why Canton specifically (not a public chain, not a private DB)

- **A sealed order book is only possible with sub-transaction privacy.** On a
  transparent chain the whole point — hidden interest until the cross — is
  impossible; a private DB gives you no shared, verifiable, atomic settlement.
- **Atomic cross-party, cross-asset settlement** with legal finality → **zero
  principal risk** on every fill, no escrow contract, no bridge.
- **No MEV.** Sealed batch cross → no front-running or sandwiching by construction.
- **Known identities + need-to-know audit** → KYC'd parties and a compliance
  observer that sees the *trades* and the *official price*, but not the *book* —
  the regulatory posture institutions require.

## The value

- **Fair large-order execution** — one price, no impact, no front-running.
- **An official, auditable close / NAV** — produced from real orders, no oracle.
- **Zero principal risk & instant finality** — atomic DvP, not T+2, no bridge.
- **Book stays private** — no information leakage to the market or competitors.
- **Audit-ready** — an immutable, timestamped receipt per fill, disclosed to a
  compliance party by design.
- **Composable** — the same sealed-cross + atomic-DvP pattern extends to any
  tokenised asset (equities, bonds, MMFs, cETH, cBTC); cETH is the first,
  high-liquidity instrument.
