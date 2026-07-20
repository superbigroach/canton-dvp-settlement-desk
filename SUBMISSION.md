# CrossDesk — HackCanton S2 Submission

**CrossDesk is the on-chain fund-issuance layer for tokenised assets on Canton: atomic
in-kind creation & redemption, and a credibly-neutral NAV struck by a K-of-N committee —
the exact machinery a tokenised fund needs to exist on-chain. The NAV is priced by a
sealed, uniform-price call auction, and every leg settles atomically on one
principal-risk-free engine.**

- **Repo:** github.com/superbigroach/canton-dvp-settlement-desk
- **Track:** Investment Infrastructure: Funds, DAOs & Governance Tools
- **Live demo:** **https://crossdesk-devnet-app.web.app** — connected to the shared
  HackCanton devnet node (`hackcanton-01`), **settling real on-chain transactions**
  (first atomic DvP: *alice-crossdesk → bob-crossdesk · 10 cETH @ 3,200 USDC*, 2026-07-19)
- **Assets used:** **cETH** (OnRails) and **CBTC** (BitSafe) as first-class instruments
- **Built entirely during the hackathon** — first commit 2026-07-11 ("… — HackCanton Season 2")

### 🎥 Demo video — guided timeline (~3:50, nothing simulated)
| Time | What you see |
|---|---|
| 0:00 | Founder intro & the problem — European closing auctions (up to ⅓ of daily volume) and why public order books leak |
| 0:23 | The solution — a sealed, atomic, multi-party auction desk on Canton |
| 0:48 | **Live in-kind ETF creation** — 10 LX1 shares (0.10 cETH + 0.01 CBTC each) minted atomically on the shared devnet node; redemption mirror |
| 1:40 | **Decentralised NAV committee** — propose → attest (2-of-3) → official NavFixing struck on-chain |
| 2:20 | **Sealed closing cross** — hidden buy/sell orders, venue runs the cross, DvP settles at the committee NAV |
| 2:54 | **Privacy proof** — same ledger viewed as Alice, Bob, Bank, Auditor: non-participants see nothing, the auditor sees everything |
| 3:17 | Close — "CrossDesk: the fund factory for tokenised assets" |

*(This is the judges' quick-read. The full technical writeup is in `README.md`.)*

---

## 1 · The user and the problem
Thirty-billion-plus dollars of tokenised funds are on-chain — but the machinery that
makes a fund a *fund* isn't. Institutions issuing and trading **tokenised assets**
(wrapped crypto like cETH/CBTC, tokenised equities, cash tokens) have no on-chain venue
for the two primitives a fund actually runs on:

- **an in-kind primary market** — create/redeem shares against the underlying basket
  atomically (the mechanism that keeps a fund glued to NAV; the one the SEC approved for
  crypto ETFs in July 2025), instead of a TradFi back office;
- **a credibly-neutral NAV** — an official price no single administrator can set
  unilaterally (today it's one venue's number, struck off-chain and reconciled for weeks).

Striking that NAV honestly also needs a **sealed order book** (so the largest orders
aren't front-run) and **atomic settlement** (both legs move together, or neither — no
Herstatt/principal risk) — neither of which a transparent chain can provide, because the
mempool leaks every order. CrossDesk delivers all of it on Canton: an in-kind fund
primary market and a committee NAV, on one privacy-preserving atomic-settlement engine.

## 2 · Parties and visibility (privacy is enforced at the contract level)
| Party | Role | Sees |
|---|---|---|
| **Venue** | Auction operator | the full sealed book (it signs every order) |
| **Issuer** | Token issuers (OnRails cETH, BitSafe CBTC, Circle USDC) | its own instruments/holdings |
| **Alice / Bob** | Traders | only their **own** orders & holdings |
| **Bank** | Liquidity provider **+** ETF fund administrator/custodian | net imbalance (as designated LP) |
| **Auditor** | Compliance | settlement **receipts**, never the underlying holdings |
| **Agent** | Delegated trading bot | acts for a principal within a ledger-enforced mandate |
| **Eve** | Outsider | **nothing** — proves the privacy model in tests |

A `Holding` is visible only to its **issuer + owner**; a `SealedOrder` only to
**operator + that trader**; a `NavFixing` only to **committee members + auditor**. If
you're not a declared party, the data does not exist for you.

## 3 · The core state changes (what actually happens on-ledger)
1. **Sealed closing/opening cross** — traders lodge private orders; the venue runs one
   uniform-price cross that settles **every matched fill atomically** at the official
   price → a signed `SettlementBatch`. Over-subscribed side is pro-rata rationed.
2. **Decentralised operator (K-of-N)** — a committee of members attests the official
   price; a `NavFixing` only exists once **≥ threshold distinct members have signed**
   (accumulating multisignature). The auction can be **bound** to it, so the printed
   close is provably a committee fix, not one venue's number.
3. **In-kind ETF creation/redemption** — an authorised participant delivers the exact
   basket of underlyings and receives **freshly-minted shares** (or the reverse: burn
   shares → get underlyings back), atomically. NAV = Σ(unitsPerShare × committee mark).
4. **Atomic bilateral DvP** — propose → accept → settle; both legs in one transaction,
   with an immutable audit receipt.

## 4 · How CBTC and cETH are used (bounty-relevant)
**cETH and CBTC are first-class instruments throughout:**
- **Traded** in the sealed opening/closing cross (e.g. the cETH open prints at 2,400).
- **Settled atomically** via DvP (the agent flow settles a real cETH leg).
- **The underlying of the LX1 ETF basket** — `LX1 = 0.10 cETH + 0.01 CBTC` per share.
  Creating LX1 **moves real cETH + CBTC holdings** into custody and mints shares; redeeming
  burns shares and returns the cETH + CBTC. So a cETH/CBTC balance change is the *core
  state transition* of the fund.
- Devnet CBTC obtained from the BitSafe faucet; the same flows run on real devnet tokens
  once bridged/received to the acting party.

## 5 · Setup — run it locally
**Daml logic + tests (proves the whole model):**
```bash
daml build && daml test          # 18/18 scripts pass
```
**Full stack (live desk):** see `run-react.md`
```
1) ledger:   daml sandbox --port 6900  + upload DAR + run Test:initialize
2) backend:  cd backend && ./gradlew build -x test && java -jar build/libs/*.jar   # :8080
3) web app:  cd frontend && npm i && npm run build && npm run preview -- --port 5173
→ open http://localhost:5173
```
LocalNet deployment (DPM / Canton Builder) packages the same DAR onto a local Canton net.

## 6 · What works today (verified)
- ✅ **`daml test` — 18/18 green**, incl. `testThresholdAttestation`,
  `testCommitteeAttestedClose`, `testCreateThenRedeem`, `testCreationAtomicRollback`,
  `testNavPerShare`, atomic-rollback, dark-pool privacy, DLP selective disclosure.
- ✅ **Backend** (Spring Boot over the Daml Java bindings) — unit tests green; REST surface
  for trade / auction / committee / basket.
- ✅ **Frontend** — full trading-desk UI: sealed order → run the cross, the K-of-N committee
  (watch signatures accumulate), and the ETF builder (create/redeem in-kind with live NAV).
- ✅ **Live end-to-end** against a Canton sandbox: 2-of-3 committee strikes a NavFixing
  (a single member is rejected below threshold); create 10 LX1 (−1.0 cETH, −0.1 CBTC,
  +10 LX1); NAV 890 USDC; redeem 4 (+0.4 cETH, +0.04 CBTC).

## Why it's different
Not "another exchange" and not a continuous orderbook — CrossDesk is the **fund-issuance
layer**: an in-kind primary market plus a **credibly-neutral, committee-struck NAV**, the
exact machinery a tokenised fund needs to exist on-chain. The sealed uniform-price auction
that prices the NAV and the atomic DvP that settles every leg are the engine underneath —
privacy-preserving, principal-risk-free, and impossible on a transparent chain.
