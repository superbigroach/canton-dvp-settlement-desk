# Business Brief — Canton DvP Settlement Desk

*One-page brief on the RWA & business-workflow settlement problem this desk models.*

---

## What it is

A confidential, atomic settlement desk for swapping **tokenised ETH (cETH)**
against **tokenised cash / deposits** between institutions on the Canton Network.
Trades settle **all-or-nothing in one transaction** (no principal risk) and are
visible **only** to the two counterparties plus a named compliance auditor.

## Ideal Customer Profile (ICP)

The parties who already do bilateral, privacy-sensitive, high-value settlement:

1. **Digital-asset desks at banks & broker-dealers** (e.g. JPMorgan Kinexys,
   Goldman GS DAP participants) settling tokenised collateral and cash.
2. **Crypto-native prime brokers & OTC desks** moving ETH vs. stablecoin/deposit
   for institutional clients who cannot leak positions to a public mempool.
3. **Tokenised-fund & treasury operators** rebalancing between ETH exposure and
   cash without wire/T+2 lag or bridge risk.
4. **Market infrastructure / custodians** (DTCC-style) offering a neutral,
   audited DvP venue to members.

## The use case

A desk holds cETH and wants cash (or vice versa). Today that means: pick a
counterparty, agree a price off-venue, then bear **principal risk** while one leg
settles before the other — or pay an escrow/CCP to intermediate, and still risk
leaking the trade. This desk collapses that into a single private, atomic swap
with a built-in audit receipt: **propose → settle → audited**, done.

## Who pays, and for what

- **The desks / counterparties** pay a **per-settlement fee** (bps on notional)
  for atomic, private, principal-risk-free settlement — cheaper than the capital
  and operational cost of the risk it removes.
- **Venue / infrastructure operators** pay a **platform / SaaS** fee to run a
  branded, multi-tenant desk with compliance observers wired in.
- **Optional**: premium **agentic** access — API/AI-agent-initiated settlement
  under mandate limits — as a metered add-on.

## Why Canton specifically (not a public chain, not a private DB)

- **Atomicity across parties/assets** with legal finality → **zero principal
  risk** by construction. A public EVM chain needs escrow contracts and still
  leaks metadata; a database gives you no shared, verifiable settlement.
- **Sub-transaction privacy** → a competitor, and even the cash issuer, sees
  nothing. Confidentiality is the default, disclosure is by choice.
- **No bridge** → cETH is a native Canton content token; settlement never routes
  through the most-exploited component in crypto.
- **Known identities + need-to-know audit** → KYC'd parties and a compliance
  observer that sees the *trades* but not the *book* — exactly the regulatory
  posture institutions require.

## The value

- **Removes principal/Herstatt risk** — capital and credit-line savings.
- **Removes settlement latency** — atomic, not T+2.
- **Protects the book** — no information leakage to the market.
- **Audit-ready** — an immutable, bilateral, timestamped receipt per trade,
  disclosed to a compliance party by design.
- **Composable** — the same pattern extends to any tokenised-asset DvP (bonds,
  MMFs, other RWAs) — cETH is the first, high-liquidity instrument.
