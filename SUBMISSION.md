# CrossDesk — HackCanton S2 Submission (Grand Final)

**CrossDesk is a sealed closing auction on Canton that discovers its own official price
— and the fund-issuance layer that consumes it: atomic in-kind creation & redemption,
and a credibly-neutral NAV struck by a K-of-N committee. Traders lodge orders nobody
can see; at the close the venue uncrosses the whole book in one atomic transaction,
prints one price the orders themselves determine, and settles every leg or none.**

- **Repo:** github.com/superbigroach/canton-dvp-settlement-desk
- **Track:** Investment Infrastructure: Funds, DAOs & Governance Tools
- **Live demo:** **https://crossdesk-devnet-app.web.app** — connected to the shared
  HackCanton devnet node (`hackcanton-01`), **settling real on-chain transactions**
  (first atomic DvP: *alice-crossdesk → bob-crossdesk · 10 cETH @ 3,200 USDC*, 2026-07-19).
  ⚠️ It runs the **pre-feedback** package `72ec9833…` — see §0.6.
- **Assets used:** **cETH** (onRails) and **CBTC** (BitSafe) as first-class instruments —
  and as of **2026-08-04 we hold 4.16 of BitSafe's real CBTC** on the devnet node, claimed
  through the CIP-56 registry flow, not self-issued (§0.4)
- **Built entirely during the hackathon** — first commit 2026-07-11 ("… — HackCanton Season 2")

---

## 0 · The finalist feedback, and exactly what was done about it

> *"Strong market-structure thinking and a technically credible Canton build — sealed
> auction, net-imbalance disclosure, a multi-party NAV process and atomic DvP
> demonstrated on DevNet. To sharpen before the final: cBTC/cETH are currently
> self-issued stand-ins (move to CIP-56), and the auction crosses at an
> operator-supplied reference price rather than discovering the official open/close
> from the order book — add deterministic price selection and complete-order
> commitments, and show one real token-standard DvP."*

Four criticisms, four answers. Each is checkable in about thirty seconds.

### 0.1 "crosses at an operator-supplied reference price" → **the venue no longer supplies the price**

`RunClose` calls a **pure** function `discoverPrice` over the sealed book. Candidates
are every distinct limit in the book plus the anchor; the ladder is **max executable
volume → min |imbalance| → market pressure → mixed-surplus boundary → nearest anchor**.
`ClosingAuction.referencePrice` is demoted from *the answer* to the **tie-break of last
resort**.

- **Code:** `daml/MarketOnClose.daml` → `discoverPrice` (and the module header, which is
  the spec).
- **Proof:** `Test:testMarketOnCloseImbalance` — a book anchored at **255** prints at
  **250**, and the test asserts `closingPrice /= 255.0`.
  `Test:testPriceDiscoveryBeatsReference` — a book anchored at **260** prints at **256**,
  and is also the end-to-end proof of price priority (the seller *through* the print
  fills in full; only the at-the-print seller is rationed).
  `Test:testPriceDiscoveryUnit` exercises the algorithm with no ledger, no parties and no
  holdings at all.
- **Consequence for governance, stated honestly:** a committee-bound auction no longer
  *prints* the attested number — it *anchors* on it. `Test:testCommitteeAttestedClose`
  now proves the print can legitimately land **away** from the fix when the book says
  so, and that a venue cannot substitute an anchor of its own.

### 0.2 "add deterministic price selection" → **a total function with no operator input**

Same (anchor, book) in, same price out, on every Canton validator that re-executes the
choice body — which is not a nicety here, it is a requirement, because they must all
agree. Ties are settled by published venue rules, not by list order: the **Xetra ladder**
(T7 Release 10.0 Market Model §11.1.1) with **Euronext's** nearest-the-reference final
tie-break (Trading Manual §3.1). Rule 7 (nearest anchor, then lower) exists purely so
the function is total.

- **Code:** `discoverPrice`'s tie-break block; `allocateSide` for the allocation ladder.
- **Proof:** `Test:testMixedSurplusTieBreak`, `Test:testAllocationUnit`,
  `Test:testFillsMatchReceipts` (delivery equals receipt, exactly, by construction).
- **We do not claim the generic version.** Nasdaq, Xetra and Euronext maximise volume;
  the **NYSE close does not** — under Rule 7.35B(g) a *human* (the DMM) selects the
  price. `docs/REAL_AUCTION_MECHANICS.md` §2 sources all four.

### 0.3 "complete-order commitments" → **the close is refused unless it runs over the whole book**

`ClosingAuction` carries `submittedCount` and `cancelledCount`; `RunClose` asserts
`length buyOrders + length sellOrders == submittedCount − cancelledCount`, plus a
**distinctness** check so the list cannot be padded with a duplicate. An order can only
leave the book by a real, on-ledger cancellation the trader who placed it can see.

- **Code:** `daml/MarketOnClose.daml` → the two `assertMsg`s at the top of `RunClose`.
- **Proof:** `Test:testCompleteBookRequired`, `Test:testCancellationKeepsTheBookHonest`,
  `Test:testWithdrawAndVenueClear`.
- **The cost, said out loud:** `SubmitOrder` had to become **consuming** to maintain the
  counter, which serialises submissions on one contract. Real contention, accepted
  deliberately: a call auction collects interest over minutes and produces one print, so
  integrity of the print outranks submission throughput.

### 0.4 "cBTC/cETH are self-issued stand-ins … show one real token-standard DvP" → **one real one, and we say what is still not**

`daml/TokenStandardDvp.daml` implements **six** official Canton Network Token Standard
(CIP-56) interfaces — `Holding`, `TransferFactory`, `TransferInstruction`,
`AllocationFactory`, `Allocation`, `AllocationRequest` — and settles a **two-leg atomic
DvP over `AllocationRequest`**: Alice delivers 4.0 cETH, Bob delivers 0.25 cBTC, one
transaction. The official `splice-api-token-*-v1-1.0.0` DARs are vendored **unmodified**
into `deps/` as `data-dependencies`; nothing in this repo re-declares a standard type.

We also refused the Splice reference token's shortcut of making the allocation *be* the
holding: an allocation here **locks a real holding** and names it in `holdingCids`, so a
standard wallet can render the locked position with no knowledge of this app.

- **Code:** `daml/TokenStandardDvp.daml`, `daml.yaml` → `data-dependencies`.
- **Proof:** `daml/TokenStandardTest.daml` — the happy path plus three refusals (missing
  leg, foreign allocation, unauthorised execution) and the plain transfer path.
- ✅ **The stand-ins are no longer only stand-ins — we hold real BitSafe CBTC on devnet.**
  On **2026-08-04** we claimed **4.16 CBTC** through the CIP-56 registry flow and it is
  live on `hackcanton-01` right now:

  | Party | CBTC |
  |---|---|
  | `alice-crossdesk` | 0.84 |
  | `bob-crossdesk` | 0.53 |
  | `bank-crossdesk` | 0.91 |
  | `issuer-crossdesk` | 0.88 |
  | `auditor-crossdesk` | 0.01 |
  | wallet party | 0.99 |
  | **total** | **4.16** |

  These are **BitSafe's contracts, on BitSafe's templates, owned by our parties** — verified
  by `HoldingV1` interface query, not by our own issuance. The reason the desk can consume
  them is that BitSafe's tokens and our CIP-56 code resolve the *same* Token Standard
  interface package `55ba4deb0ad4662c4168b39859738a0e91388d252286480c7331b3f71a517281`,
  which this repo vendors unmodified. The unlock was the registry **choice-context**
  endpoint (`api.utilities.digitalasset-dev.com`) — without it `TransferInstruction_Accept`
  fails with `Missing context entry for: utility.digitalasset.com/transfer-rule`, because
  the registrar's `TransferRule` is not visible to the receiver. Full runbook, including
  every error hit and solved: **[`docs/TOKEN_STANDARD_RUNBOOK.md`](docs/TOKEN_STANDARD_RUNBOOK.md)**.
  The same flow should carry over to onRails cETH with a different registrar.
- 🔴 **What is NOT CIP-56: the auction itself.** `MarketOnClose`, `Holding`,
  `Settlement`, `Basket`, `Governance`, `LiquidityMandate` are all still the legacy
  self-issued layer. The two sets of cETH do not interoperate. Implementing CIP-56 does
  not make our cETH *the* cETH — what it buys is that a real issuer's registry could be
  dropped in **without changing the venue code**. Full accounting, including the
  migration route and the limitations of even the compliant path, in
  `docs/TOKEN_STANDARD_DVP.md`.

### 0.5 What we added on top, because the feedback exposed a shape and not just instances

| Addition | Why | Where |
|---|---|---|
| **Unpriced MOC order type** (`limitPrice : Optional Decimal`) | The overwhelming majority of real closing volume is unpriced — index funds *buying the print*. Modelling only price setters models the smaller half of a market. `None` is eligible at every price, ranks ahead of every limit, and is never away from the cross. | `MarketOnClose.daml`; `Test:testMocClassPriority`, `testMocLocLadderEndToEnd`, `testAllUnpricedBookPrintsAtAnchor`, `testMocIsNeverAwayFromTheCross` |
| **Price-priority allocation** | The old close rationed the *entire* heavy side pro-rata, so a better limit bought eligibility and no precedence — and the rational strategy became oversizing, which corrupts the very imbalance number a provider is shown. Now: MOC first, then priced *through* the print in full, then only the marginal level rationed. | `allocateSide`; `Test:testAllocationUnit`, `testUnpricedInterestRationedProRata` |
| **A price collar that clamps, not aborts** | `max($0.50, 10% of anchor)` (Nasdaq's construction). Cancelling a close on a breach lets one oversized order deny everyone else their print — for an index fund whose whole instruction is "own the close", that is the one outcome that cannot be hedged. So: clamp to the nearer boundary and **re-cross the book there**. It is also the bound that makes an unpriced buy fundable at submission time. | `clampToCollar`; `Test:testPriceCollar`, `testCollarClampsDown`, `testUnpricedBuyFundedAtClampedBoundary`, `testInBandPrintIsNotClamped` |
| **A contestable liquidity mandate** | Imbalance disclosure used to follow a party the venue *named in a field*, owing nothing. Now it requires a live, accepted, obligated `LiquidityMandate` — an open offer any registered participant may take, **at no fee**, many at once. Hu & Murphy (2026, *Management Science* 72(5)) locate the harm of this channel shape precisely where fees suppress competition for the seat. | `daml/LiquidityMandate.daml`; `Test:testMandateSeatIsContestable`, `testNoMandateNoImbalance`, `testTwoLiveMandatesBothSeeTheImbalance`, `testFailedMandateIsRevokedAndBarred` |
| **Accruing NAV** | A money-market fund's value between marks is *earned*, not discovered. The committee attests base / rate / day-count / as-of; the ledger derives value continuously via `navAt`, and the close checks the anchor is consistent with the NAV accrued to *its own* ledger time (never ahead of it, ≤1 bp behind). | `daml/Governance.daml`; `Test:testAccrualArithmeticUnit` and four more |
| **A continuous session** — a price–time-priority limit order book | `HOW_IT_WORKS.md` listed "there is no continuous session here" as a known limitation, and it was a structural one: a closing auction does not manufacture a price, it **inherits** one from the ladder that rests all day. Without that ladder, unpriced MOC flow has nothing to price against — which is exactly why Xetra §11.1.1 step 5 and Euronext §3.1 fall back to the venue's reference. So the other end of the book now exists: limit interest rests, is matched **price then time**, and settles at the **maker's** price in one atomic sweep. Orders are dark pre-trade (a `RestingOrder` has *no* observers — not even the auditor); every fill prints to a **public, anonymous** tape. Dark pre-trade, lit post-trade — the shape of MiFIR's waiver structure, not a shortcut around it. | `daml/ContinuousBook.daml`; **28 scenarios** in `daml/ContinuousBookTest.daml` incl. `testPricePriorityBeatsArrivalOrder`, `testTimePriorityWithinAPriceLevel`, `testSweepTwoLevelsAtEachMakersPrice`, `testSelfMatchingIsRejected`, `testRestingOrderIsDark`, `testTapeIsPublicAndAnonymous`, `testMatchConservesEveryUnit`, `testUnpricedOrderCannotRest` |
| **Leverage — cash-settled perpetuals on a marked instrument** | The desk could already price a fund and issue its shares in kind. What nobody could do was take a view on that fund without holding it, or hedge one they did hold: creation and redemption move real underlyings, so an arbitrageur has to find and fund the whole basket before it can act. A perpetual solves both, and it closes the loop the fund layer opens. Creation/redemption keeps a share honest over hours; a perp keeps it honest over seconds. **A perp on a FUND cannot index on an attested mark, because a fund has none.** Its value is *derived* from what it holds. So the index is the fund's NAV per share, computed from its components' attested marks, which means it inherits the committee's signatures rather than inventing a new authority; an unmarked component yields no index at all, because a fund you cannot value is a fund you cannot lever. That is not fussiness: a leveraged position is *liquidated* against this number, so an index one party can move at will is an index one party can use to liquidate people. **The funding rate is derived, never fetched.** `deriveFunding` computes `clamp(premium + interest, ±cap)` from *this* venue's perp price against *this* venue's index, because another exchange's funding rate is a fact about another exchange's book; the cap turns one printed trade on a thin book from an arbitrary levy on every open position into a bounded cost. **Three trade-offs, stated rather than hidden.** (i) The venue's insurance pool is the counterparty, not a matched book, so lopsided open interest is directional risk the pool carries; `openLong` / `openShort` sit on the market precisely so that imbalance is a ledger fact and the funding rate is the lever against it. (ii) Liquidation is the **operator's** duty rather than permissionless, because a `PerpPosition` is private to its trader and a keeper cannot close what it cannot see; a visible liquidation price on a leveraged product is an invitation to push the market into it. (iii) Collateral is cash only, the market's single `cashInstrument` (USDC), so a holder cannot post the fund's own shares as margin. There is **no** auto-deleveraging, **no** cross-margin, **no** partial close and **no** perp order book. | `daml/Perpetual.daml` (the module header is the argument, including what is deliberately absent); **18 scenarios** in `daml/PerpetualTest.daml`, every one of which counts the cash before and after, incl. `testLongProfitIsConserved`, `testShortProfitIsConserved`, `testLossIsPaidToThePoolAndConserved`, `testLeverageCeilingIsEnforced`, `testCannotOpenBelowMaintenance`, `testLiquidationWhenEquityFallsBelowMaintenance`, `testAddCollateralAvertsLiquidation`, `testPositiveFundingIsPaidByLongs`, `testPoolTooSmallIsRefused`, `testPositionIsPrivateToItsTrader`; `PerpetualController.java` (`indexFor` is the fund-NAV fallback); `POST /api/perp/*` and the *Leverage* panel. **Local only:** see §0.6 |
| **Both NAVs a fund actually has** | A real ETF runs two: an **official** NAV struck from signed marks, which is what creations and redemptions legally settle at, and an **indicative** NAV recomputed continuously and binding on nobody (exchanges disseminate one every ~15s). Marking a volatile asset once a day and settling against it all the next day would be indefensible — and publishing a streamed number as the *official* one would be equally wrong, because it is the signatures, not the freshness, that make a NAV bindable. So the desk shows both, and the **drift in bps** between them is the honest measure of how stale the last strike has become. Each leg is valued by what it is: a wrapped-crypto leg at live spot (BitSafe's own integration guidance for cBTC is a BTC-USD feed), a money-market leg by accrual from the committee's recipe — an MMF has no live price to stream, its NAV is struck and then *earned*. | `GET /api/basket/nav/indicative`; `MarketData.java`; the Fund panel |
| **SDK pinned to 3.4.11** | So the repo reproduces exactly what the devnet node runs (LF 2.2). Both Java backends were regenerated and are on Ledger API v2. | `daml.yaml` |

### 0.6 Where this code is, and what is *not* proven

🔴 Say this before a judge finds it:

- The rebuild is **pushed** — `origin/master` and branch
  **`feat/price-discovery-and-cip56`** point at the same commit. What you clone is
  what is described here.
- **The auction has not been run end-to-end against a live participant.** It is proven by
  **Daml Script scenarios** (115, all green) plus compiling backends and a
  clean `tsc` — not by a
  cross printing on a shared node. Uploading a DAR to `hackcanton-01` is an admin-only
  action on the node operator's side; the request is in with NODERS and the package
  is built and waiting. The *settlement* path did have its live run —
  the atomic DvP of 2026-07-19, receipt `006ef8c599…`.
- **The leverage layer is newer still, and it is local.** `Perpetual.daml` and
  `/api/perp/*` run on a **local sandbox**: the test suite, plus a full open → mark →
  close cycle against a locally running ledger with the cash reconciled to the unit
  before and after. It has never been on `hackcanton-01`, and the hosted demo does not
  carry it. Anything a judge sees of it is a local build.
- The **hosted demo and the recorded demo video therefore show the *pre-feedback*
  build**, in which the close printed at the committee NAV. That is not what the code
  does now, and §0.1 is the correct description.

---

### 🎥 Demo video — guided timeline (~3:50, nothing simulated)

*Recorded on the pre-feedback build — see §0.6. The auction segment shows the close
printing at the supplied reference; §0.1 describes what `RunClose` does today.*

| Time | What you see |
|---|---|
| 0:00 | Founder intro & the problem — European closing auctions (up to ⅓ of daily volume) and why public order books leak |
| 0:23 | The solution — a sealed, atomic, multi-party auction desk on Canton |
| 0:48 | **Live in-kind ETF creation** — 10 LX1 shares (0.10 cETH + 0.01 CBTC each) minted atomically on the shared devnet node; redemption mirror |
| 1:40 | **Decentralised NAV committee** — propose → attest (2-of-3) → official NavFixing struck on-chain |
| 2:20 | **Sealed closing cross** — hidden buy/sell orders, venue runs the cross, DvP settles *(pre-feedback: at the committee NAV)* |
| 2:54 | **Privacy proof** — same ledger viewed as Alice, Bob, Bank, Auditor: non-participants see nothing, the auditor sees everything |
| 3:17 | Close — "CrossDesk: the fund factory for tokenised assets" |

*(This is the judges' quick-read. The full technical writeup is in `README.md`; the
system as it stands today is `docs/HOW_IT_WORKS.md`.)*

---

## 1 · The user and the problem

Thirty-billion-plus dollars of tokenised funds are on-chain — but the machinery that
makes a fund a *fund* isn't. Institutions issuing and trading **tokenised assets**
(wrapped crypto like cETH/CBTC, tokenised equities, cash tokens) have plenty of on-chain
**settlement** and almost no on-chain **price formation**, and no venue for the two
primitives a fund actually runs on:

- **an official price nobody hands in from outside** — today's marks are typed by an
  administrator you have to trust and reconciled for weeks;
- **an in-kind primary market** — create/redeem shares against the underlying basket
  atomically (the mechanism that keeps a fund glued to NAV; the one the SEC approved for
  crypto ETFs in July 2025), instead of a TradFi back office.

Striking a price honestly needs a **sealed order book** (so the largest orders aren't
front-run) and **atomic settlement** (both legs move together, or neither — no
Herstatt/principal risk) — neither of which a transparent chain can provide, because the
mempool leaks every order.

## 2 · Parties and visibility (privacy is enforced at the contract level)

| Party | Role | Sees |
|---|---|---|
| **Venue** | Auction operator | the full sealed book (it signs every order) |
| **Issuer** | Token issuers (onRails cETH, BitSafe CBTC, Circle USDC) | its own instruments/holdings |
| **Alice / Bob** | Traders | only their **own** orders & holdings |
| **Bank** | Liquidity provider **+** ETF fund administrator/custodian | net imbalance — **only while holding a live `LiquidityMandate`** |
| **Auditor** | Compliance | settlement **receipts**, never the underlying holdings |
| **Agent** | Delegated trading bot | acts for a principal within a ledger-enforced mandate |
| **Eve** | Outsider | **nothing** — proves the privacy model in tests |

A `Holding` is visible only to its **issuer + owner**; a `SealedOrder` only to
**operator + that trader**; a `NavFixing` only to **committee members + auditor**; an
`ImbalanceDisclosure` only to **the venue + the mandated provider**. If you're not a
declared party, the data does not exist for you.

## 3 · The core state changes (what actually happens on-ledger)

1. **Sealed closing/opening cross with on-ledger price discovery** — traders lodge
   private orders (MOC or LOC); the venue seals the window and runs one uniform-price
   cross whose **price comes out of the book**. Allocation is class → price through the
   print in full → the marginal level rationed pro-rata by size. Every matched fill
   settles atomically into a signed `SettlementBatch` with a receipt per fill.
2. **Complete-order commitment** — the close is refused unless the supplied lists *are*
   the live book, so the venue cannot move the print by omission.
3. **Decentralised operator (K-of-N)** — a committee attests the official mark; a
   `NavFixing` only exists once **≥ threshold distinct members have signed**
   (accumulating multisignature). An auction can be **bound** to it, so the anchor the
   cross runs against is provably a committee fix. Accruing fixings let the ledger derive
   value continuously between marks.
4. **Contestable liquidity mandate** — an open, free offer; accepting it is what buys
   sight of the net imbalance, and performance is scored on-ledger after the close.
5. **In-kind ETF creation/redemption** — an authorised participant delivers the exact
   basket and receives **freshly-minted shares** (or the reverse), atomically.
   NAV = Σ(unitsPerShare × committee mark).
6. **Atomic bilateral DvP** — propose → accept → settle; both legs in one transaction,
   with an immutable audit receipt. Plus a **CIP-56 Token Standard** DvP over
   `AllocationRequest` in a separate module.

## 4 · How CBTC and cETH are used (bounty-relevant)

**cETH and CBTC are first-class instruments throughout:**

- **Priced** in the sealed opening/closing cross — the cETH open in `testOpeningCross`
  prints at 2,400 because the book brackets it, and in `testCommitteeAttestedClose` a
  cETH book that sits *below* the committee's 2,400 fix prints at **2,350** instead.
- **Settled atomically** via DvP (the agent flow settles a real cETH leg; the CIP-56
  module settles cETH against cBTC over the Token Standard interfaces).
- **The underlying of the LX1 ETF basket** — `LX1 = 0.10 cETH + 0.01 CBTC` per share.
  Creating LX1 **moves real cETH + CBTC holdings** into custody and mints shares;
  redeeming burns shares and returns them. So a cETH/CBTC balance change is the *core
  state transition* of the fund.
- Devnet CBTC obtained from the BitSafe faucet and **claimed for real through the CIP-56
  registry flow on 2026-08-04 — 4.16 CBTC across our six parties, live on the node now**
  (§0.4, and `docs/TOKEN_STANDARD_RUNBOOK.md`). They are BitSafe's contracts on BitSafe's
  templates, not self-issued.

## 5 · Setup — run it locally

**Daml logic + tests (proves the whole model):**
```bash
daml version              # must be 3.4.11 — daml.yaml pins it (the line devnet runs)
daml build && daml test   # 115 scripts, all pass
```
(63 scenarios in `daml/Test.daml`, 28 in `daml/ContinuousBookTest.daml`, 18 in
`daml/PerpetualTest.daml`, 6 in `daml/TokenStandardTest.daml` — the suite is still
growing, so run the command for the live
number. The build also links the six vendored `splice-api-token-*` DARs from `deps/`
— verify with `daml damlc inspect-dar .daml/dist/canton-dvp-settlement-desk-1.0.0.dar`.)

**Full stack (live desk):** see `run-react.md`
```
1) ledger:   daml sandbox --port 6900  + upload DAR + run Test:initialize
2) backend:  cd backend && ./gradlew build -x test && java -jar build/libs/*.jar   # :8080
3) web app:  cd frontend && npm i && npm run build && npm run preview -- --port 5173
→ open http://localhost:5173
```

## 6 · What works today (verified)

- ✅ **`daml test` — 115/115 green** (the suite is still growing —
  run it for the live number), including `testPriceDiscoveryUnit`,
  `testPriceDiscoveryBeatsReference`, `testCompleteBookRequired`, `testMixedSurplusTieBreak`,
  `testCollarClampsDown`, `testMocLocLadderEndToEnd`, `testMandateSeatIsContestable`,
  `testAccruedAnchorBindsTheClose`, `testThresholdAttestation`, `testCommitteeAttestedClose`,
  `testCreateThenRedeem`, `testNavPerShare`, atomic-rollback and dark-pool privacy — plus
  the six CIP-56 scripts in `daml/TokenStandardTest.daml` and the 18 leverage scripts in
  `daml/PerpetualTest.daml`, which check cash conservation on every settlement path.
- ✅ **Backends** (`backend/` and `backend-devnet/`, Spring Boot over the Daml Java
  bindings 3.4.0 / Ledger API v2) — `./gradlew clean build` SUCCESSFUL at the branch
  head; REST surface for trade / auction / mandate / committee / basket.
- ✅ **Frontend** — `tsc --noEmit` clean at the branch head. Full trading-desk UI:
  MOC/LOC order entry, the sealed book, the close, the mandate seat, the K-of-N
  committee (watch signatures accumulate), and the ETF builder.
- ✅ **Live on-chain settlement** on the shared devnet node (2026-07-19) — an atomic DvP
  with sub-transaction privacy, on the **pre-feedback** package (§0.6).
- ⚠️ **Not** verified: the price-discovery build against a live participant. See §0.6 and
  the limitations table in `README.md`.

## Why it's different

Not "another exchange" and not a continuous orderbook. Almost everything on-chain is
settlement without price formation — a mark is handed in and the chain merely moves the
legs. CrossDesk **produces the number**: a sealed, uniform-price call auction whose
clearing price is a deterministic function of the orders, whose book is provably
complete, and whose every fill settles atomically at that one price. On top of it sits
the fund-issuance layer — an in-kind primary market and a committee-attested,
continuously-accruing NAV — that a tokenised fund needs to exist. Privacy-preserving,
principal-risk-free, and impossible on a transparent chain, where the mempool *is* the
book.
