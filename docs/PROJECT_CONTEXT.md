# CrossDesk — complete project context

Everything needed to understand the project and write a better pitch. Written 2026-08-05.
Facts marked ✅ were verified by execution against a live ledger on that date.

---

## 1. What CrossDesk is, in one paragraph

CrossDesk is a **trading venue and valuation engine for tokenised funds, built on the Canton
Network**. It does three jobs that today are done by three separate companies: it **discovers a
price** (a sealed closing auction plus a continuous order book), it **produces an official NAV**
(a K-of-N committee that signs a valuation recipe rather than a number), and it **settles
creation and redemption of fund shares in kind** against that NAV, atomically. It also runs
cash-settled perpetuals so the arbitrage that keeps a fund glued to its NAV can be hedged and
levered. It is written in Daml, runs on a real Canton participant, and its defining property is
that the order book is **invisible even to the auditor** while orders rest.

---

## 2. The problem, precisely

A tokenised fund needs three things on day one:

1. **A price for what it holds.** If it holds tokenised assets, somebody must mark them.
2. **An official NAV** that the issuer cannot be accused of setting itself.
3. **A create/redeem mechanism** so shares can be minted and destroyed against that NAV.

Today these are an exchange, a fund administrator and a custodian — three vendors, three
contracts, and a reconciliation between them.

**The second-order problem is the one that actually costs money.** When the fund's shares trade
away from NAV, the trade that closes the gap is: buy the underlying basket → create shares →
sell the shares (or the reverse on a discount). That is three legs on three venues with price
risk between each. The arbitrageur charges for that risk, so he quotes the gap wide and only
shows up when it is large. **The discount persists, and the fund's own investors pay it.**

**And the obvious fix makes it worse.** Put the order book on a public chain and the mempool
*is* the book — every resting order is visible before it executes. Index funds are estimated to
lose **$1–2bn/year** to participants who can see them coming.

---

## 3. How it works — the three mechanisms

### 3.1 Sealed closing auction (price discovery)
Every participant lodges a sealed order. Nobody — not even the auditor — can see another's
order while it rests. At the close the venue uncrosses the entire book in one atomic
transaction at a **single uniform price** discovered from the orders themselves, with the heavy
side rationed pro-rata. Unpriced market-on-close orders are allocated ahead of limit orders.
The price the auction prints is the official close.

### 3.2 K-of-N committee attestation (NAV)
For anything with no market, a committee of K-of-N signers attests a value. Critically it signs
a **recipe, not a number** — base value, rate, day-count convention — so the ledger *derives*
the value continuously at every instant afterwards, rather than holding a number that goes
stale between strikes. Two NAVs are published side by side:

- **Official NAV** — from signed marks. This is what create/redeem settles at.
- **Indicative NAV** — from live market data, refreshed continuously, binding on nobody. The
  on-chain equivalent of the iNAV an exchange disseminates every ~15 seconds.

The drift between them, in basis points, is on screen. It is the honest measure of how stale
the last strike has become and the cue to strike again.

### 3.3 In-kind creation and redemption (settlement)
An authorised participant delivers the exact underlying assets and receives freshly-minted
shares, or the reverse — atomically, all-or-nothing, at the signed NAV. This is the mechanism
that keeps an ETF glued to its NAV.

### 3.4 Cash-settled perpetuals (the arb plumbing)
Leverage exists here for a specific reason, and it is not retail speculation:

- A fund's discount is a **ten-basis-point business**. It is uninvestable unlevered, which is
  why arbitrage is a levered trade everywhere it exists.
- Without a short instrument, **only existing holders can close a premium.** A perp lets a
  market maker take the other side without owning the fund.
- The perp price becomes a **live, tradeable second opinion on the committee's NAV.** If the
  two disagree, everyone can see it.

For a fund, the perp's index falls back to the basket NAV, so shorting the perp hedges NAV
movement while you wait to redeem — a clean, fully-hedged basis trade.

---

## 4. The superpower — Canton's privacy model

On Canton a contract is visible only to its **signatories and observers**. A `RestingOrder` is
signed by the operator and the trader and observed by **nobody**. So a sealed order is not a
clever trick layered on top — it is the default behaviour of the ledger.

**Verified on the real HackCanton shared node, cETH continuous session, 4 orders resting** ✅

| Viewing as | Bids visible | Asks visible |
|---|---|---|
| Venue (operator) | 2 | 2 |
| Alice | 1 (her own) | 0 |
| Bob | 0 | 1 (his own) |
| **Auditor** | **0** | **0** |

Post-trade is the opposite: **every fill prints to a public tape that names nobody.** Dark
pre-trade, lit post-trade — which is the whole of MiFID II's waiver structure, not a shortcut.

This is **unbuildable on a public chain** (the mempool leaks it) and **unprovable in a private
database** (you'd have to trust the operator). That combination is the entire technical case.

---

## 5. What is actually built

### Daml — 16 files, 15,984 lines, 38 templates, 79 choices, 121 test scripts

| File | Lines | Templates |
|---|---|---|
| `MarketOnClose.daml` | 2821 | ClosingAuction, SealedOrder, ImbalanceDisclosure |
| `ContinuousBook.daml` | 1123 | ContinuousBook, RestingOrder, TapePrint, TradeConfirm |
| `LiquidityMandate.daml` | 682 | MandateTerms, LiquidityMandate, MandatePerformance |
| `TokenStandardDvp.daml` | 637 | CIP-56 token-standard holding/transfer/allocation/registry/DvP |
| `TokenSettlement.daml` | 634 | AuctionAllocationRequest, MatchSettlement, AuctionCross |
| `Perpetual.daml` | 603 | PerpMarket, PerpPosition |
| `Governance.daml` | 592 | OperatorCommittee, FixingProposal, NavFixing |
| `Basket.daml` | 357 | BasketDefinition, Creation/Redemption Order+Agreement, BasketReceipt |
| `Settlement.daml` | 309 | DvPProposal, DvPAgreement, SettlementReceipt, SettlementBatch |
| `Holding.daml` | 190 | Holding |
| `Agent.daml` | 133 | TradingMandate |
| `Instrument.daml` | 82 | Instrument |
| Tests | 7073 | Test.daml 5235 · ContinuousBookTest 1069 · PerpetualTest 417 · TokenStandardTest 352 |

Every settlement path in `PerpetualTest` asserts total cash before == after.

### Backend — Spring Boot 3.3.4 / Java 17, 58 endpoints
`SettlementController` 37 · `PerpetualController` 11 · `ContinuousBookController` 8 ·
`Diagnostics` 1 · `Health` 1.
`MarketData.java` pulls **Coinbase spot** (60s cache, fails soft). Money-market accrual uses
`USYC_NET_YIELD = 0.0320`, ACT/360.

### Frontend — React + Vite + TypeScript, 6,872 lines
`App.tsx` · `api.ts` · `FundPanel` · `ContinuousBookPanel` · `CommitteePanel` ·
`PerpetualPanel` · `AccrualTicker` · `PendingTransfersPanel`.

### The demo instrument set
- `USDC` — cash
- `cETH` — wrapped ETH (**self-issued**)
- `CBTC` — wrapped BTC (**real BitSafe cBTC, 4.16, claimed through the CIP-56 registry flow**)
- `MMF:USYC-REF` — a money-market instrument **modelled on** USYC. Explicitly labelled
  `MODEL ONLY - not a holding of the fund`.
- `LX1` — an index fund: **100 × MMF:USYC-REF + 0.05 cETH + 0.002 CBTC** per share

---

## 6. Verified live, with the actual numbers ✅

Local ledger, 2026-08-05:

| Flow | Result |
|---|---|
| Official NAV from signed marks | `322.34253` |
| Indicative NAV from live Coinbase spot | `321.23638602`, drift **−34.32 bps** |
| Live marks | ETH `1863.145`, BTC `64039.195` (Coinbase spot) |
| Money-market accrual | `1.0000074602`, basis `accrued @ 0.032/yr ACT/360` |
| In-kind redeem | 2 shares → `0.1 cETH + 0.004 CBTC + 200 MMF` = `644.6851` = exactly `2 × NAV` |
| **The arbitrage** | bought 2 @ `318`, redeemed at NAV → **+8.6851 USDC**, exactly `(322.34253−318)×2` |
| Continuous book fill | 2.0 @ `318.0`, settled at the maker's price |
| **Venue runs the close** | discovered price `322.34253`, 3 crossed, Alice `+967.0276` / Bob `−967.0276`, **cash conserved** |
| Perp market on a fund | index syncs to attested mark, `openShort 30`, `skew −30` |
| **Leverage** | Short 30 @ `350`, margin `2000` → notional `10500`, **5.25x**, maintenance `525`, **liq `396.825397`** |
| Mark-to-market P&L | **+829.7241** = exactly `30 × (350 − 322.34253)` |

Shared HackCanton devnet node, same date:
- Dark book verified per-party (§4) ✅
- Live cETH continuous session, 4 resting orders, bestBid `1870` / bestAsk `1880` ✅
- LX1 published as a tradeable instrument at NAV `321.43686` ✅
- LX1 book seeded: print at `316` → **DISCOUNT −169.1 bps**, edge `5.4369`/share ✅

---

## 7. Deployment topology

| Piece | Where |
|---|---|
| Shared node | `ledger-api-grpc.participant.hackcanton-01.devnet.naas.noders.services`, TLS + Keycloak JWT |
| Devnet API | Cloud Run `crossdesk-devnet-api`, us-central1 |
| Devnet UI | Firebase Hosting `crossdesk-devnet-app`, `/api/**` rewritten to Cloud Run |
| Local | `backend` on :8080 (2.9.4 sandbox), `backend-devnet` on :8090 (Daml 3.x / Ledger API v2) |
| DAR | `crossdesk` **2.0.0**, SDK 3.4.11, 2,862,275 bytes, sha256 `7b79beed8807d8ff75952c58a7237d44f6070182e1ffb7ee0fbcd14846475e91` |

**Important deployment fact:** a new package id starts with an **empty ledger** from the app's
point of view — contracts created under the old package are invisible to a backend bound to the
new one. Switching the hosted desk to 2.0.0 therefore requires a full re-bootstrap and re-seed.

---

## 8. Market and business model

### The two numbers that define the business

| Job | Price | Source |
|---|---|---|
| **Calculating** a NAV | **0.325 bps** of AUM | BBH Trust, Form N-CSR FY2023 — disclosed because BBH is both custodian and fund accountant. It *fell* from 0.40 bps in 2023. |
| Producing an **official price** | **3 bps + $600,000/yr** | What the SPY trust pays S&P Dow Jones for the right to reference the S&P 500. State Street is Trustee; the Sponsor is PDR Services LLC |
| Administering **hard-to-value** assets | **6–12 bps** | Aetos / HedgeServ administration agreement, SEC-filed |

**The same computation is worth 10–15x more as an official price than as an accounting output.**
The entire company is a bet on which side of that line a K-of-N attested fixing lands.

Supporting facts: index providers take **~one-third of all ETF management fees** (31.4% in 2010
→ 35.7% in 2019). MSCI's index segment runs a **76.4% adjusted EBITDA margin**. Average implied
index licensing fee is **4.4 bps** of ETF AUM.

### Pricing
- First design partner: **$50–150K/yr flat** — anchored to **Pyth Pro at $120K/yr**, the only
  published rate card in institutional market data. Flat, not AUM-linked: 0.325 bps on a $200M
  fund is $6,500, which is not a contract.
- Steady state: **1–3 bps + a flat fee** — the standard index-licensing contract form,
  deliberately below the 3–4.4 bps S&P and MSCI command.

### Market size — the honest version
Every tokenised US Treasury / money-market fund on earth is **$16.16bn** (rwa.xyz, 2026-08-04),
growing ~4% per 30 days. At index-administrator pricing that is a **$5–7M annual pool**. It is
small. It must grow 10–100x for this to be a real market. **That is the load-bearing assumption.**

Largest funds: Circle USYC $3.006B · BlackRock BUIDL $2.673B · Ondo USDY $2.153B · Franklin
iBENJI $1.731B · Janus Henderson JTRSY $881.7M.

### On Canton specifically
Publicly documented: **USYC** (Circle/Hashnote, live on Canton, built with Digital Asset),
**Franklin Templeton BENJI** (platform expanded to Canton, Nov 2025), and a **BNY + Goldman
Sachs tokenised MMF platform** on Canton-based GS DAP with BlackRock, BNY Dreyfus, Federated
Hermes, Fidelity and GSAM at launch. RedStone's stated Canton pipeline includes Hamilton Lane
SCOPE (private credit), Fasanara F-ONE, and Spiko.

**Never state a Canton AUM figure.** No public tracker reports Canton — rwa.xyz does not even
list Canton among USYC's networks despite a documented launch. Any number is unfalsifiable.
Also never cite "$6 trillion on Canton": that is repo and collateral flow, and repo does not
need a NAV.

### Canton app rewards
Capped at **$1.50 per transaction**; at current CC price and network volume the pool works out
to almost exactly the cap. A NAV venue does tens of transactions a day → **~$10K/yr** at design-
partner scale. It is a gas rebate, not a business model. Note the auction leg earns materially
more than the NAV leg, because CIP-0104 ties rewards to traffic burned and a settlement
transaction is heavier than an attestation.

---

## 9. Competition — be precise about this

**Chainlink NAVLink** is live delivering NAV on-chain for **Fidelity International (FILQ), UBS
and Amundi**. **RedStone** is the production oracle **on Canton**, Daml-native, running as a
Canton participant, a Canton Foundation member since July 2025, and already advertises custom
NAV feeds for tokenised funds.

**Both are relays.** What they transport is still **one administrator's assertion**,
cryptographically delivered. K-of-N changes *who asserts*, not how it travels. Do not claim the
transport layer — it is taken, by two funded, staffed, shipping teams.

---

## 10. The strongest case against — know it cold

1. Calculating a NAV costs 0.325 bps and is in structural price decline. Total addressable spend
   across every tokenised Treasury fund on earth is ~$525K/yr.
2. T-bills and overnight repo are the **easiest instruments in finance to price**. The market
   pays 6–12 bps for hard marks precisely because those are uncertain. You picked the asset
   class where nobody doubts the number.
3. The real complaint about tokenised MMF NAV is **staleness, not dishonesty**. K-of-N makes it
   slower, not fresher, because now you wait for a quorum.
4. Collateral takers already have a cheap answer to an uncertain mark: **widen the haircut.**
5. The position is occupied (§9).
6. Your customers are BlackRock, Franklin, Circle, Federated and GSAM. They do not buy valuation
   governance from a solo founder.
7. **The one that actually bites: K-of-N needs N credible, independent attestors willing to take
   liability on someone else's valuation.** Who are they, and what are they paid out of a
   $100K contract?

**The answer to 7** — and it is a good one — is that **nobody is hired**. The N are parties who
already have a commercial reason to hold an opinion on that mark: the collateral taker about to
lend against it, the issuer, the administrator, the market maker who quotes it. Attestation is a
**byproduct of a position they already have**, not a service somebody buys. You are not
assembling a consortium of disinterested referees; that would never happen.

---

## 11. Reference — how USYC actually works

Researched from primary sources 2026-08-05. Full detail in
`docs/research/USYC_OFFICIAL_METHODOLOGY.md`.

- **It is an accruing-NAV token, not a $1.00 rebasing coin.** Price = fund NAV ÷ total USYC
  supply, so yield shows up as a rising token price.
- **Current price `1.133066960425761961`**, round 483, reported 2026-08-04. Cross-checked three
  ways (public API, direct `eth_call` to the oracle, rwa.xyz) — all agree.
- **Price updates once per business day, ~9am ET**, after the **prime broker reconciles** the
  prior day. Mon–Fri, excluding US federal holidays.
- **The 2:00pm ET cutoff governs SUBSCRIPTIONS ONLY.** After 2pm your subscription prices off a
  *forecast* of the next report. **Redemptions are always at the current price**, 24/7.
- **How it reaches chain:** a **Circle/Hashnote-operated oracle** implementing Chainlink's
  `AggregatorV3` interface at `0x74f2199AEb743f68f05943e5715A33EaF2b61f53` — *not* a Chainlink
  DON, not NAVLink, not RedStone. Public API: `https://usyc.hashnote.com/api/price`.
- AUM ~$3.0B. **Do not quote circle.com/usyc's AUM figure** — that page renders a broken value
  roughly 10x too low.

**Where the underlying Treasury valuation comes from** — this matters for the pitch. The fund
holds short-duration US Treasuries and overnight reverse repo. Circle does **not** compute those
prices itself: the **prime broker reconciles and values the positions**, the administrator
strikes the NAV, and Circle/Hashnote then **publish** it through their own oracle. That is
exactly the "one administrator's assertion, cryptographically transported" structure — a single
party's number, relayed. It is the precise gap a K-of-N attestation addresses, and it is worth
saying out loud because it is verifiable rather than rhetorical.

`[UNVERIFIED]` — the day-count convention is not published anywhere. ACT numerator is confirmed
(a Friday repo accrues 3 days, observable in the data), but 360 vs 365 is inference: ACT/360 fits
at 3.49% gross and is the repo-market convention. Say "consistent with ACT/360, I'd confirm
against the offering memorandum." Also unverified: how USYC prices **on Canton** specifically —
Circle has published nothing. Do not improvise if a Digital Asset judge asks.

---

## 12. The hackathon context

**HackCanton Season 2 Grand Final**, Wed 2026-08-05, 14:00 UTC. **Slot 18 of 18 — last.**
**4 minutes**, then Q&A. Zoom open from 13:30 UTC to test screen share.

The organiser's two binding instructions:
1. **"Don't burn your time on a live demo. Use screenshots and a short video instead."**
2. A fixed structure, 60 seconds each: **Problem · Solution & product · Market & business model
   · Demo & team.**

**Builder lanes** (their categories): AMMs / order books / RFQs · private OTC · lending +
leverage · cross-margin · liquidity vaults · treasury tools.

**CrossDesk covers four of the six:**
- **Order books** — the continuous book (price-time priority) and the sealed cross
- **Private OTC** — bilateral atomic delivery-versus-payment, dark pre-trade
- **Lending + leverage** — cash-settled perps with margin, funding rate, liquidation
- **Treasury tools** — the accruing money-market NAV and in-kind create/redeem

Not covered: cross-margin, liquidity vaults, AMMs, RFQs. Do not claim them.

Being last matters. Eighteen pitches in, the judges are tired and everything has blurred. Open
with a story, not a feature.

---

## 13. The founder

Solo. Ex-equities trader — **European closing auctions, index adds and deletes, imbalances,
merger arb**. That is the load-bearing credential: this project is a closing auction, and he
traded closing auctions for a living. Also: Google Cloud partner, Google's first AI innovator
cohort, Launch (Jason Calacanis) incubator alumnus, house architect on Circle's Arc.

The trading background belongs in the spoken pitch. The rest belongs on a slide — read, not
recited, because next to "I traded this mechanism for a living" the others dilute rather than add.

---

## 14. The ask

**One pilot fund. Ninety days. Shadow mode.** CrossDesk strikes the NAV in parallel with
whoever strikes it today, and both are published. Nobody switches, nothing is at risk, and at
the end the issuer owns a signed, auditable price history a regulator can read.

This is deliberately the smallest possible ask: parallel running is a normal pattern in fund
administration, it requires no migration, and it creates the reference customer.

The natural first partner is **BitSafe** — they issue cBTC and cETH on Canton, CrossDesk already
holds real cBTC on their templates, and an index fund of their assets drives demand for those
assets. The natural expansion is **where marks are genuinely doubted**: private credit, which
pays 6–12 bps rather than 0.325.

---

## 15. What must never be claimed

- ❌ Any Canton AUM figure. ❌ "$6 trillion on Canton" (repo flow, not fund AUM).
- ❌ Any fund-administration TAM from a research vendor (three vendors give $8.6B / $13.6B /
  $100.2B for the same category).
- ❌ **Holding real cETH.** Real asset = **cBTC only**. cETH is self-issued.
- ❌ **Holding USYC.** It is KYC-gated, Reg S, non-US persons only. It is *modelled*.
- ❌ Create-and-hedge as **one Daml transaction**. It is two submissions on one ledger. "Same
  ledger, no leg risk" is true and sufficient.
- ❌ The close uncrossing the resting book — they are two separate sessions.
- ❌ Auto-deleveraging, cross-margin, partial closes, asset collateral, permissionless
  liquidation (positions are private, so a keeper cannot see them to liquidate them).
- ❌ The perpetual layer being on the shared node. It is local — the hosted devnet backend
  predates it.
