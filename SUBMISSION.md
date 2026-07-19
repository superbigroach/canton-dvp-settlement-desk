# CrossDesk — HackCanton S2 Submission

**The closing/opening auction for tokenised assets: a sealed uniform-price call auction
that prints an official NAV, a K-of-N committee so no single venue sets that price, and
an in-kind ETF builder — all on one atomic, principal-risk-free settlement engine.**

- **Repo:** github.com/superbigroach/canton-dvp-settlement-desk
- **Track:** Financial Applications / RWA & Business Workflows
- **Live demo:** full-stack desk (React + Spring + Daml over the Ledger API) — see *Setup* below
- **Assets used:** **cETH** (OnRails) and **CBTC** (BitSafe) as first-class instruments

*(This is the judges' quick-read. The full technical writeup is in `README.md`.)*

---

## 1 · The user and the problem
Institutions trading **tokenised assets** (wrapped crypto like cETH/CBTC, tokenised
equities, cash tokens) have no on-chain venue that gives them all three things at once:

- **an official, credibly-neutral price** — a "closing NAV" that no single operator can
  set unilaterally (today it's one venue's number, which can be gamed);
- **a sealed order book** — so a large order isn't front-run before it trades;
- **atomic settlement** — both legs move together, or neither (no Herstatt/principal risk).

Transparent chains can't do the sealed book (the mempool leaks every order). CrossDesk
solves all three on Canton, and adds a **tokenised-fund (ETF) primary market** on the
same engine.

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
Not "another exchange" and not a continuous orderbook — it's the **price-formation layer**
(the official open/close) for tokenised assets, with a **decentralised price authority** and
a **tokenised-fund primary market**, all sharing one privacy-preserving atomic-settlement
engine that a transparent chain fundamentally cannot provide.
