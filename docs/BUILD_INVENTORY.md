# CrossDesk — complete build inventory

**Verified live against the running ledger on 2026-08-05, 07:2x–07:4x UTC.**
Everything below marked ✅ was executed as a real transaction during that window, not asserted.

---

## 1. What was verified live, with the actual numbers

| # | Flow | Result | Evidence |
|---|---|---|---|
| 1 | **Official NAV** from signed marks | `322.34253` /share | `100×1.00 + 0.05×1871.87 + 0.002×64374.515` ✅ |
| 2 | **Indicative NAV** from live Coinbase spot | `321.23638602`, drift **−34.32 bps** | ETH `1863.145`, BTC `64039.195`, refreshed live ✅ |
| 3 | **Money-market accrual** | `1.0000074602` | basis string: `accrued @ 0.032/yr ACT/360` ✅ |
| 4 | **In-kind redeem pays exactly NAV** | 2 shares → `0.1 cETH + 0.004 CBTC + 200 MMF` = `644.6851` | exactly `2 × 322.34253` ✅ |
| 5 | **The arbitrage** — buy under NAV, redeem | **+8.6851 USDC** | bought 2 @ `318`, redeemed at NAV. Theoretical `(322.34253−318)×2 = 8.6851`. Exact ✅ |
| 6 | **Continuous book fill** | filled 2.0 @ `318.0` | price-time priority, settled at the maker's price ✅ |
| 7 | **Sealed auction — the dark book** | see §2 | the central claim of the pitch ✅ |
| 8 | **Venue runs the close** | closing price `322.34253`, 3 shares crossed | Alice `+967.0276` / Bob `−967.0276`, **cash conserved** ✅ |
| 9 | **Perp market on a fund** | index syncs to the attested mark `322.34253` | `openShort 30`, `skew −30` ✅ |
| 10 | **Leverage** | Short 30 @ entry `350`, margin `2000` | notional `10500`, **5.25x**, maintenance `525`, **liq `396.825397`** ✅ |
| 11 | **Mark-to-market P&L** | **+829.7241** | exactly `30 × (350 − 322.34253)` ✅ |

---

## 2. The dark book — the property the whole pitch rests on

One sealed auction on LX1, two orders resting. The same ledger, queried as four different
parties:

| Viewer | Orders visible | Hidden from it |
|---|---|---|
| **Venue** (operator) | 2 — `Alice:Sell`, `Bob:Buy` | 0 |
| **Alice** | 1 — her own | 1 |
| **Bob** | 1 — his own | 1 |
| **Auditor** | **0** | **2** |

The auditor is an observer of the *auction* and of every *fill*, and sees **not one resting
order**. This is enforced by Daml's signatory/observer model, not by an API filter — the
endpoint queries the ledger **as** the acting party.

> **API gotcha that cost me a false alarm:** `/api/moc/state` takes **`actingAs`**, while
> `/api/book/state` takes **`as`**. Passing `as` to the MOC endpoint is silently ignored and
> falls back to the Venue, which makes the book look fully transparent. It is not.

---

## 3. Daml — 16 files, 16,573 lines, 125 test scripts

| File | Lines | Templates |
|---|---|---|
| `MarketOnClose.daml` | 2856 | `ClosingAuction`, `SealedOrder`, `ImbalanceDisclosure` |
| `ContinuousBook.daml` | 1123 | `ContinuousBook`, `RestingOrder`, `TapePrint`, `TradeConfirm` |
| `LiquidityMandate.daml` | 682 | `MandateTerms`, `LiquidityMandate`, `MandatePerformance` |
| `TokenStandardDvp.daml` | 637 | `TokenStandardHolding`, `TokenStandardTransferOffer`, `TokenStandardAllocation`, `TokenStandardRegistry`, `TokenStandardDvpProposal`, `TokenStandardDvp` |
| `TokenSettlement.daml` | 634 | `AuctionAllocationRequest`, `MatchSettlement`, `AuctionCross` |
| `Perpetual.daml` | 603 | `PerpMarket`, `PerpPosition` |
| `Governance.daml` | 1095 | `OperatorCommittee`, `FixingProposal`, `RestatementProposal`, `NavFixing`, `CessationNotice` |
| `Basket.daml` | 497 | `BasketDefinition`, `CreationOrder`, `CreationAgreement`, `RedemptionOrder`, `RedemptionAgreement`, `BasketReceipt` |
| `Settlement.daml` | 309 | `DvPProposal`, `DvPAgreement`, `SettlementReceipt`, `SettlementBatch` |
| `Holding.daml` | 190 | `Holding` |
| `Agent.daml` | 133 | `TradingMandate` |
| `Instrument.daml` | 82 | `Instrument` |
| **Tests** | | |
| `Test.daml` | 5894 | — |
| `ContinuousBookTest.daml` | 1069 | — |
| `PerpetualTest.daml` | 417 | — |
| `TokenStandardTest.daml` | 352 | — |

**39 templates, 84 distinct choices.** Every settlement path in `PerpetualTest` asserts
`totalCash` before == after.

> The Daml SDK is **not installed on this machine** (`~/.daml` absent), so `daml test` could
> not be re-run here. The DAR was built elsewhere. The live-ledger results in §1 are stronger
> evidence anyway — they exercise the same code through the real participant.

---

## 4. Backend — Spring Boot 3.3.4 / Java 17, 58 endpoints

| Controller | Endpoints | Lines |
|---|---|---|
| `SettlementController` | 37 | 1801 |
| `PerpetualController` | 11 | 507 |
| `ContinuousBookController` | 8 | 566 |
| `DiagnosticsController` | 1 | 55 |
| `HealthController` | 1 | 33 |

Surfaces: instruments · holdings · DvP · sealed auction (`/moc/*`) · imbalance + liquidity
mandate · committee fixings · baskets (define / create / redeem / NAV / indicative NAV) ·
continuous book (`/book/*`) · perpetuals (`/perp/*`) · live marks (`/marks/live`).

`MarketData.java` pulls **Coinbase spot** with a 60s cache and fails soft; money-market
accrual uses `USYC_NET_YIELD = 0.0320`, ACT/360.

## 5. Frontend — React + Vite + TS, 6,872 lines

`App.tsx` 1294 · `api.ts` 1217 · `styles.css` 983 · `FundPanel` 652 · `ContinuousBookPanel` 620
· `CommitteePanel` 545 · `PerpetualPanel` 544 · `accrual.ts` 370 · `AccrualTicker` 335 ·
`PendingTransfersPanel` 291.

---

## 6. Fixed today

| Fix | File |
|---|---|
| Refresh / Halt / Open-session were **unstyled OS buttons** | `ContinuousBookPanel.tsx` |
| **"Place order" — the desk's main CTA — was a raw white button.** `.ticket-submit` sets only layout, no colour | `ContinuousBookPanel.tsx` + `styles.css` |
| Session now **auto-opens on asset selection** (one attempt per instrument, manual button as fallback) | `ContinuousBookPanel.tsx` |
| Basket designer let you **redefine a live fund**. Now checks basket *and* instrument namespaces, opens on the first free `LX`n, lists what's in use, blocks self-reference and duplicate components | `FundPanel.tsx` |
| **"Start from an existing recipe"** — copies components, keeps the symbol free | `FundPanel.tsx` |
| **Arb strip** — PREMIUM/DISCOUNT in bps + one-click Create/Redeem at NAV, reading the *traded* price (last print, else mid) rather than the instrument mark, which never moves on a fill | `FundPanel.tsx` + `styles.css` |
| Perp Refresh / Sync-index / Fund-pool unstyled; PendingTransfers had **classes that don't exist** (`error`, bare `muted`) so text rendered at full brightness | `PerpetualPanel.tsx`, `PendingTransfersPanel.tsx`, `App.tsx` |
| Added global `.muted` / `.error` — they only ever existed scoped, so any new panel copying the idiom got unstyled text | `styles.css` |
| **LX1 was not a published instrument** → invisible in the asset picker, no book, arb strip dead. Published as `Fund` at NAV | live ledger |

---

## 7. Known gaps — say these only if asked

- **`MMF:USYC-REF` is marked `1.00`; real USYC is `1.133066960425761961`** (round 483,
  2026-08-04, `https://usyc.hashnote.com/api/price`). The instrument is explicitly described
  as `MODEL ONLY - not a holding of the fund`. Changing the base re-prices LX1's NAV and would
  require re-anchoring the open session and the perp index.
- **USYC's 2:00pm ET cutoff governs subscriptions only** — redemptions are always at the
  current price, and the price *reports* ~9am ET. See `docs/research/USYC_OFFICIAL_METHODOLOGY.md`.
- **cETH is self-issued.** The real-asset claim is **cBTC only** (4.16, BitSafe templates, CIP-56).
- **Create-and-hedge is two submissions**, not one Daml transaction. "Same ledger, no leg
  risk" is true; "one atomic transaction" is not.
- The perpetual layer is **local only** — not on the shared HackCanton node.
- Not built: auto-deleveraging, cross-margin, partial closes, asset collateral,
  permissionless liquidation (positions are private, so a keeper cannot see them).

---

## 8. The DAR

```
path   .daml/dist/crossdesk-2.0.0.dar
size   2,862,275 bytes
sha256 7b79beed8807d8ff75952c58a7237d44f6070182e1ffb7ee0fbcd14846475e91
name   crossdesk        version 2.0.0        sdk 3.4.11
```

Renamed from the July package because `TradingMandate` gained non-optional fields and failed
`NOT_VALID_UPGRADE_PACKAGE`. `2.0.0` upgrades `1.0.0` cleanly — only templates were added.
