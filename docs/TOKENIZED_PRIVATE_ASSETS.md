# Tokenized Private Assets and Longer-Dated Bonds — Where Value Is an Opinion

**Prepared:** 2026-08-04 · **For:** HackCanton Season 2 pitch, Wednesday 2026-08-05
**Companion to:** `docs/MARKET_AND_PRICING.md`, which covered tokenized treasury/money-market funds ($16.16B, 0.325 bps to strike a NAV). **Read that one first.** This document covers everything that is *not* a treasury fund.

> **Rule for this document, inherited from the companion:** every number carries a source and a date. Anything I could not verify from a primary or credible source is tagged **`[UNVERIFIED]`** and must not be spoken on stage as fact. There are **12** such items, listed in §9.
>
> **A second rule specific to this document.** The RWA space double-counts, restates press releases as market data, and lets interested parties publish "research" that is really a sales brochure. Where a figure comes from someone who sells the solution it implies, **I say so inline.** Two of the most quotable numbers in here are from interested parties, and one of them is contradicted by better data (§5.2). You need to know which.

---

## 0. Executive summary — six findings

| # | Finding | Consequence |
|---|---|---|
| 1 | **The single largest "tokenized private credit" asset on earth is marked at exactly $1.00.** Figure's HELOC token is $20.81B of the $36.27B tokenized-credit total, sits on Figure's own permissioned chain, and carries NAV $1.00 / price $1.0000. | The headline private-credit number is not a market. It is one issuer's book, mirrored on its own ledger, at par. Subtract it and the category is roughly a sixth the size. |
| 2 | **Assets whose value is genuinely an opinion total roughly $1–2B on-chain, not $36B.** Tokenized real estate is **$202.6M**. Tokenized non-US government debt is **$1.31B** and is *mostly money-market funds again*. Tokenized credit ex-Figure, ex-crypto-collateralised lending, ex-CLO funds is low single-digit billions at best. | Your addressable set outside treasuries is **smaller in AUM** than the treasury set, not larger. But willingness-to-pay per dollar is 20–40x higher (companion §1.2: 6–12 bps vs 0.325 bps). |
| 3 | **The mark-blowup event the companion doc said would create this market has already happened — in tokenized private credit.** Goldfinch is in wind-down after $50M+ of defaults with depositors reporting ~70% losses against a dashboard showing 20%. Maple lost $36M to a single counterparty in 2022. Centrifuge pools went delinquent. | This is your strongest non-obvious point. Companion §5.4 item 4 — "somebody's mark blows up" — is not a future condition. It is history, and it happened specifically to the self-reported-mark model. |
| 4 | **The market prices mark uncertainty, and Jefferies measures the price.** LP secondary portfolios traded at **87% of NAV** in 2025; **credit at 91%**, **real estate at 70%**, **venture at 78%**. | This is the product-thesis number you asked for. A 9-to-30-point discount to stated NAV is what the world charges for a mark it cannot independently verify. |
| 5 | **The academic literature on smoothing is deep, top-tier, and unambiguous.** Private equity's true volatility is ~30% against a headline ~10% (Baz & Davis, *JPM* 2022). Serial correlation from illiquidity is formally modelled (Getmansky/Lo/Makarov, NBER 2003) and appraisal lag is documented in individual appraisals (Clayton/Geltner/Hamilton, *Real Estate Economics* 2001). | Cite this, not RWA blog posts. It is the most credible material in the entire pitch and it is free. |
| 6 | **On Canton specifically, nothing non-treasury is confirmed live.** The one substantive private-credit programme — T-RIZE / Kairos, up to $500M — was **announced 2026-03-30** with a first $50M tranche, and I found no confirmation it has drawn. | Do not claim a live non-treasury Canton asset. Same discipline as companion §2.2. |

**The one-line verdict:** *the market for attested marks on non-treasury on-chain assets does not exist yet at meaningful size, but unlike the treasury market it is a market that could exist, because the thing being sold is genuinely contested and the failures are already on the record.*

---

## 1. What is actually on-chain that is not a treasury fund

### 1.1 A methodology warning you must understand before quoting anything

rwa.xyz now reports two different totals for every category, and they differ by **5x** at the top level:

| Measure | Total RWA (ex-stablecoins) | Tokenized credit |
|---|---|---|
| **Distributed value** | **$37.29B** (+1.47% / 30d) | **$7.02B** (−0.96% / 30d) |
| **Represented value** | **$411.35B** (+171.06% / 30d) | **$36.27B** (+3.71% / 30d) |

*rwa.xyz, data date 2026-08-04. Stablecoins separately $295.52B.*

**`[UNVERIFIED-1]`** The precise definitions of "distributed" versus "represented" value. rwa.xyz's methodology page returned 404 on 2026-08-04. From the asset-level data the observable pattern is that **"represented" includes assets recorded on an issuer's own permissioned ledger** (Figure's HELOCs on Provenance, Tradable's notes) while **"distributed" counts tokens circulating on general-purpose networks.** That is an inference, not a quoted definition.

Two reasons this matters more than a footnote:

1. **The represented total rose 171% in thirty days.** A category cannot grow 171% in a month organically. That is a methodology or coverage change, and any figure spanning it is not a time series.
2. **Represented value is where all the big private-credit numbers live.** If you quote $36.27B for tokenized private credit you are quoting a number whose majority is one company's internal loan book.

**Use distributed value on stage.** It is the conservative measure and it is the one a sceptic can check.

### 1.2 The honest size table for non-treasury assets

| Category | Distributed value | Represented value | Assets | Holders | 30-day |
|---|---|---|---|---|---|
| **Tokenized credit** (private credit, on-chain lending, corporate & structured credit) | **$7.02B** | $36.27B | 2,535 | 191,108 | −0.96% |
| **Non-US government debt** | **$1.31B** | $0.00 | 24 | 10,097 | −3.48% |
| **Real estate** | **$202.6M** | $279.8M | 104 | 18,680 | −0.05% |
| *Memo — US Treasury / MMF (companion doc)* | *$16.16B* | — | — | — | *+4.06%* |

*rwa.xyz category pages, data date 2026-08-04.*

Note the direction of travel. **Every non-treasury category shrank over the last thirty days** while treasuries grew 4.06%. That is not a rounding artefact; it is three categories out of three.

---

## 2. Tokenized private credit

### 2.1 The Figure problem — one asset is most of the category

| Field | Value |
|---|---|
| Asset | **Figure HELOC Token (FIGR_HELOC)** |
| Issuer | Figure |
| Network | **Provenance** (Figure's own blockchain), pDA standard |
| Total value | **$20,812,600,196** (+6.58% / 30d) |
| Classification | **Represented**, not distributed |
| Supply | 20,812,600,195.61 tokens |
| **NAV** | **$1.00** |
| **Price** | **$1.0000** |
| Launched | 2022-01-17 · CIK 0002064124 · US domicile · zero management and performance fees |

*rwa.xyz asset page for FIGR_HELOC, retrieved 2026-08-04.*

**$20.81B of the $36.27B tokenized-credit total — 57% — is this single asset, and it is marked at exactly par.** Home-equity lines of credit, on the issuer's own permissioned chain, one token per dollar of principal, price identical to NAV to four decimal places.

This is worth saying plainly on stage because it demolishes the standard RWA talking point: *"private credit is now the largest tokenized asset class."* It is, only if you count one company's loan book at face value on its own ledger. **There is no mark here to attest. There is a principal balance.** A K-of-N committee has nothing to contribute to a number that is definitionally 1.00.

### 2.2 The rest of the category, by platform

| Platform | Outstanding | What it actually is |
|---|---|---|
| STOKR | $1.3B | Luxembourg tokenized securities |
| **Maple** | $968.9M | Syrup USDC $1.06B + Syrup USDT $295.6M — **crypto-collateralised lending**, not private credit |
| **Centrifuge** | $746.5M | Dominated by **JAAA, Janus Henderson AAA CLO Fund, $690.1M** |
| Realiz | $500.0M | Single asset: **VuMe Bond 2030, $500M** |
| Hastra | $492.2M | — |
| **Securitize** | $489.1M | Securitize AAA CLO Fund (STAC) $354.2M; **Apollo Diversified Credit (ACRED) $115.2M** |
| Chainlink CCIP | $387.2M | Bridged representations — **double-count risk** |
| OnRe | $244.1M | Reinsurance |
| Pareto | $231.8M | — |
| Huma | $218.5M | Payments financing |
| **Tradable** | notes of $57M–$202.5M each | Senior secured term notes, fintech/real-estate/merchant-services, on ZKsync Era, **classified "represented"** |

*rwa.xyz tokenized-credit platform league table and asset list, 2026-08-04.*

**Decompose that honestly and the "value is an opinion" bucket shrinks hard:**

- **Maple's ~$1.36B is over-collateralised crypto lending.** The mark is the collateral price, which is observable on an exchange every second. Not an opinion.
- **JAAA ($690M) and STAC ($354M) are AAA CLO funds.** CLO tranches have dealer quotes and third-party evaluated pricing. Contestable at the margin, but not unobservable.
- **Chainlink CCIP's $387.2M is bridged supply.** Counting it alongside the origin asset is double-counting.
- **What is genuinely opinion-priced** — Tradable's senior secured notes, Centrifuge's non-JAAA pools, ACRED, the specialty-finance long tail — is plausibly **$1–2B**.

**`[UNVERIFIED-2]`** That $1–2B decomposition is **my arithmetic on rwa.xyz's asset list, not a published figure.** No tracker publishes an "unobservably-priced tokenized assets" total. State it as your own estimate or not at all.

### 2.3 Off-chain context — the scale of what has *not* tokenized

| Source | Global private credit AUM |
|---|---|
| Preqin, *Global Alternatives Report* | ~**$1.7 trillion** at start of 2026 |
| Moody's | **>$2 trillion** in 2026, toward **$4 trillion** by 2030 |
| PwC | **>$2 trillion**, to $3.4 trillion by 2030 |

*Retrieved via secondary sources 2026-08-04.* **`[UNVERIFIED-3]`** I did not reach the Preqin, Moody's or PwC primary documents; these came through secondary reporting. The **$1.7–2T band is consistent across three independent houses**, which is far better than the 12x spread the companion doc found for fund-administration TAM (companion `[UNVERIFIED-2]`), so the band is usable — but cite it as a band, not a point.

**The ratio is the point.** Roughly **$1–2B of genuinely opinion-priced credit on-chain against $1.7–2T off-chain — about 0.1%.** That is the honest penetration figure, and giving it yourself is more persuasive than any growth chart.

### 2.4 Who values these loans, how often, and who signs

**Quarterly, by the manager, with a third-party valuation firm providing cover.** The structure is:

- Private credit loans are **Level 3 assets under ASC 820** — fair value from unobservable inputs, by definition no market price.
- Valuation cadence is **quarterly** for the overwhelming majority of private credit portfolios.
- Third-party valuation firms are widely engaged, but the conflict is structural and openly acknowledged: **"third party appraisers are usually appointed and compensated by private creditors themselves, creating the potential for incentives to provide valuations clients seek."**
- Under **SEC Rule 2a-5** (*Good Faith Determinations of Fair Value*, Release **IC-34128**, adopted 2020-12-03, effective per Federal Register 2021-01-06), the board may designate a **"valuation designee"** — in practice the adviser — which performs fair-value determinations subject to board oversight. **The rule formalises that the manager marks its own book, with process requirements around it.**
- The SEC has run a **private credit examination sweep** focused on whether the applied mark matches the stated policy.

*Sources: SEC Rule 2a-5 adopting release IC-34128, sec.gov; secondary reporting on valuation practice and the SEC sweep, retrieved 2026-08-04.*

**So: yes, the valuation is self-reported by the manager.** That is not an accusation, it is the regulatory architecture. Rule 2a-5 does not require independence; it requires *process*. **This is the single most important structural fact for your pitch, and it is verifiable from a primary SEC document.**

### 2.5 Defaults and write-downs — how they were actually handled

This is where the abstract argument becomes concrete, and it is the most useful research in this document.

**Goldfinch — the model failed and the protocol is winding down.**

- Official: **GIP-87** on `gov.goldfinch.finance`, authored by @mikesall and @blakewest, proposes an **"orderly wind-down of Goldfinch Prime"** and transition to maintenance mode. Community voted **June 2026** to halt new development.
- Reported losses: **$50M+** across borrowers, with **6 of 8 borrowers in default or restructuring**; roughly **$100M** of loans issued lifetime against **$37.7M** raised (a16z-led).
- Named credit events: **Stratos** (~$7M written to zero), **Lend East** (58% principal loss on a $10.15M facility), **Tugende Kenya** ($5M loan, $1.9M diverted to a Ugandan parent in breach of terms).
- 🔴 **The detail that matters most:** depositors reported **~70% effective losses against a protocol dashboard showing ~20%.**
- GFI token down ~99.8% from its January 2022 high.

*Primary: GIP-87, gov.goldfinch.finance. Secondary: The Defiant, CoinMarketCap, HTX News, mid-2026.* **`[UNVERIFIED-4]`** Every figure above except the existence of GIP-87 and the wind-down itself came through **secondary crypto media, not the protocol's own accounts.** The 70%-vs-20% gap is the most quotable and the least independently verified item in this document. **Say "reported by depositors" if you say it at all.**

**Maple — $36M to one counterparty.** Orthogonal Trading defaulted on **$36 million across eight facilities** in December 2022 — about **30% of Maple's active loans** at the time — after losing funds on FTX and, per CoinDesk, **misrepresenting its financial position.** Maple severed ties and pursued recovery; damage was contained to two pools. *Secondary sources (CoinDesk, Binance Square), retrieved 2026-08-04.* Maple survived and is now the second-largest platform in the category (§2.2) — the honest reading is that **this one was handled reasonably well.**

**Centrifuge — delinquency, and a lender walked.** The **Harbor Trade Credit Series 2** pool went delinquent (pool reserve to zero, `gov.centrifuge.io/t/issuer-harbor-trade-credit-series-2/442`), and **MakerDAO's community voted to halt lending** to it after accrued defaults. **`[UNVERIFIED-5]`** The specific amounts circulating — **$2.1M** of Harbor Trade defaults, a **$5.8M** overdue-debt figure, **$1.84M** of MakerDAO exposure at risk — come from **low-quality aggregator sites** (heraldsheets, bullrun.news, Binance Square reposts). The delinquency is real and documented on Centrifuge's own governance forum; **the dollar amounts are not reliable. Do not quote them.**

**The synthesis worth saying out loud:** in all three cases, **the reported mark and the realised recovery diverged, and the divergence was discovered only at default.** Goldfinch's dashboard said 20% while depositors experienced 70%. That is not a liquidity problem or a latency problem — the two failure modes the companion doc's bear case (§7, point three) correctly identified as *not* solvable by a committee. **It is precisely the failure mode a multi-party attested mark addresses.** This is the strongest argument in your entire pitch and it is built on public record.

---

## 3. Tokenized real estate

### 3.1 It is a rounding error

| Metric | Value |
|---|---|
| Distributed value | **$202.58M** (−0.05% / 30d) |
| Represented value | $279.84M (unchanged / 30d) |
| Assets | 104 |
| Holders | 18,680 |
| **Monthly active addresses** | **1,012** |
| Countries | 11 |

| Platform | Assets | Value |
|---|---|---|
| Reental | 58 | $80.1M |
| Groma | 1 | $68.6M |
| DigiShare | 1 | $20.0M |
| Securitize | 1 | $16.3M |
| RealtyX | 1 | $332.1K |

*rwa.xyz real-estate page, 2026-08-04. Largest single assets: GromaCoin $68.6M, Sedona Ranch equity tranche $27.3M, Altus Opportunity Fund $25M, across Polygon, Base and Hedera.*

**$202.6M is less than a single mid-size office building in a major city.** Two platforms are 73% of it, and three of the top five are single-asset issuers. **There is no tokenized real-estate market. There are about a hundred individually tokenized buildings.**

### 3.2 Secondary trading — the academic answer

The best available source is peer-reviewed and recent:

> **Mafrur, Rischan. "Tokenized but Illiquid? Evidence from Real-World Asset Markets."** *FinTech* 2026, **5**, 62. Submitted 2026-05-31, revised 2026-07-17.

Findings: across Ethereum-based Treasury-backed, gold-backed and private-credit-related tokens, measured by turnover, active addresses and trading activity, **"outstanding asset value alone does not reliably predict observed liquidity,"** and **"tokenization and liquidity should be analyzed as distinct outcomes."** Gold-backed tokens showed broader holder bases and more persistent activity than Treasury or private-credit tokens.

**`[UNVERIFIED-6]`** Industry claims of a specific illiquidity magnitude — one widely-repeated figure is **"average daily volume of $50k, but the bid-ask spread for a $1M sale exceeded 40%"** — come from vendor blogs (ChainScore Labs and similar) with no methodology. **Directionally consistent with the peer-reviewed paper; not citable as fact.** Most platforms offer quarterly or annual redemption windows rather than continuous secondary liquidity, which is the practical answer to "is there secondary trading": **mostly no, there is a redemption queue.**

The rwa.xyz figure that tells the story without needing a vendor: **18,680 holders but 1,012 monthly active addresses.** About **5% of holders transact in a given month.**

### 3.3 Appraisal cadence

Private real estate is appraised **quarterly at most, commonly annually**, by an external appraiser instructed by the manager. The academic consequence is documented in §5.

---

## 4. Longer-dated tokenized bonds

### 4.1 Almost nothing, and what exists is mostly money-market funds wearing a bond label

Tokenized **non-US government debt: $1.31B across 24 assets, 10,097 holders, −3.48% over 30 days.**

| Asset | Value | What it is |
|---|---|---|
| **Spiko EU T-Bills Money Market Fund** | **$920.1M** | **A money-market fund.** Short-dated. Not a bond. |
| **NRW1** (Cashlink) | **$115.4M** | German state (North Rhine-Westphalia) digital bond — **a genuine longer-dated bond** |
| ChinaAMC HKD Digital Money Market Fund Class A | $91.4M | Money-market fund |
| A&I CAMC RMB TMMF | $72.7M | Money-market fund |
| ChinaAMC RMB Digital Money Market Fund Class B | $31.3M | Money-market fund |

*rwa.xyz non-US-government-debt page, 2026-08-04.*

**70% of the "non-US government debt" category is a money-market fund.** Strip the MMFs and genuine tokenized sovereign/sub-sovereign bonds are on the order of **$150–250M globally.**

On the corporate side, the largest items in rwa.xyz's corporate-credit league table are **VuMe Bond 2030 (Realiz), $500M** and **Tradable's North America Fintech Senior Secured Term Notes, $133.5M** — with **JAAA's $690.1M being a CLO *fund*, not a bond.** **`[UNVERIFIED-7]`** VuMe Bond 2030's $500M — a single asset that is by itself larger than the entire tokenized real-estate category — appears only as a line in rwa.xyz's table. I could not verify the issuer, the placement, or whether the amount is outstanding or programme size. **Do not cite it.**

### 4.2 The landmark digital bonds — issued on-chain, not marked on-chain

| Issuance | Size | Platform | Notes |
|---|---|---|---|
| **EIB "Project Venus"** | **€100M**, 2-year | **Goldman Sachs GS DAP** (the Canton-based platform) | Nov 2022. First euro digital bond on a private blockchain. Settled with experimental wholesale CBDC via Banque de France. Goldman Sachs Bank Europe, Santander, Société Générale. |
| **EIB fixed-rate note** | **€100M**, maturity 2029-11-22 | **HSBC Orion** | 2024. Investors access primary *and secondary* markets via custody accounts at BNP Paribas, HSBC or J.P. Morgan. |
| **EIB sterling note** | **£50M** | HSBC Orion | — |
| Hong Kong corporate digital bond | — | HSBC Orion | "Hong Kong's first corporate digital bond," platform launched in HK early 2024. |

*Retrieved via secondary sources 2026-08-04.* **`[UNVERIFIED-8]`** I did not reach EIB's own press releases; sizes and dates are from secondary reporting and are broadly consistent across sources, but treat the £50M and Hong Kong items as soft.

### 4.3 🔴 The answer to the question you actually asked

**Are any of them *marked* on-chain, or just *issued* on-chain? Just issued.**

Every one of these is a **primary issuance and settlement innovation.** The bond's record of ownership moves to a distributed ledger; the DvP settles atomically; the coupon may pay programmatically. **None of them publishes a price on-chain.** Where secondary access exists at all (the 2029 EIB note), it runs **through custody accounts at BNP Paribas, HSBC or J.P. Morgan** — which is to say, through the conventional dealer market, priced the conventional way.

**This is a genuine, defensible, non-obvious observation and it is the best thing you can say about bonds on stage:** *the tokenized bond market has solved issuance and settlement, and has not touched valuation at all.* It costs you nothing to say — it is not a claim about your product, it is a claim about the state of the field, and it is true.

---

## 5. 🔴 The pricing problem, evidenced

This section is the core of the document. It has three legs: what the academic literature establishes, what the mark data shows, and what the market actually charges.

### 5.1 Leg one — the academic literature on smoothing and stale marks

Four citations, all peer-reviewed or top working-paper series. **This is the most credible material available to you and it costs nothing to cite.**

**(a) The foundational econometrics.**

> **Getmansky, Mila; Lo, Andrew W.; Makarov, Igor.** *An Econometric Model of Serial Correlation and Illiquidity in Hedge Fund Returns.* **NBER Working Paper 9571, March 2003.** Published in *Journal of Financial Economics* 74(3), 2004.

Tested on **908 hedge funds from the TASS database.** The mechanism, in the authors' words: illiquidity exposure — holding infrequently traded securities without readily available market prices — means **"reported returns will tend to be smoother than true economic returns, which will understate volatility and increase risk-adjusted performance."** Smoothing coefficients varied significantly across categories and serve as a measure of illiquidity exposure.

**(b) The magnitude, for private equity.**

> **Baz, Jamil; Davis, Josh, et al.** *The Value of Smoothing.* ***The Journal of Portfolio Management*** **48(9), 2022.** (PIMCO authors; PM Research.)

> **"The true economic volatility of private equity is close to 30%, rather than a headline number of 10%."**

**A 3x understatement of risk.** This is the single most quotable number in this document. Note the authorship: PIMCO is a public-markets manager and therefore an interested party in the argument that private markets understate risk — **say so, and the number survives the objection.** The mechanism it describes is independently established by (a) and (c).

**(c) The real-estate evidence, from individual appraisals.**

> **Clayton, Jim; Geltner, David; Hamilton, Stanley W.** *Smoothing in Commercial Property Valuations: Evidence from Individual Appraisals.* ***Real Estate Economics*** **29(3), 2001, pp. 337–360.**

Documents **temporal lag bias in appraisals** by examining how appraisers use available transaction data, and validates the partial-adjustment model underlying the standard "unsmoothing" of benchmark real-estate return indices. **The important feature is that it works at the level of individual appraisals, not just index returns** — so it is evidence about appraiser behaviour, not a statistical artefact of aggregation. David Geltner (MIT) is the standard reference for de-smoothing.

**(d) The modern treatment, top-three journal.**

> **Couts, Spencer J.; Gonçalves, Andrei S.; Rossi, Andrea.** *Unsmoothing Returns of Illiquid Funds.* ***The Review of Financial Studies*** **37(7), 2024, pp. 2110–2155.**

Finds that **"funds with similar investments share a common source of spurious autocorrelation not fully resolved by traditional unsmoothing methods and thereby leading to underestimation of systematic risk."** Covers private CRE and hedge funds.

**`[UNVERIFIED-9]`** The specific magnitude of beta/volatility understatement in Couts–Gonçalves–Rossi. I have the abstract and the direction, not the point estimates — the paper is paywalled at Oxford Academic. **Cite the finding qualitatively; do not attach a number to it.** For a number, use Baz & Davis.

**Why this leg matters most:** it establishes that stale and smoothed marks are **a documented, quantified, decades-old property of illiquid assets** — not a crypto-native complaint and not something you invented for a pitch. A judge from a bank will know this literature. Citing it correctly is a credential.

### 5.2 Leg two — mark dispersion, and the number that cuts against you

Two sources examine the same question — *do different holders of the identical loan report different values?* — using the same public data, and reach **opposite-feeling conclusions.** You need both.

**The case for dispersion — MELD Valuation.** Drawn from public **BDC Schedules of Investments** filed with the SEC:

| Borrower | Marks on the same date | Spread |
|---|---|---|
| **Khoros** (senior secured) | Goldman Sachs BDC **50** · Hercules Capital **53** · Sixth Street Specialty Lending **77** | **27 points** across three regulated fiduciaries |
| **Pluralsight** | Golub Capital **97** · Blue Owl **83** | 14 points |
| **Auven Therapeutics** | Oaktree **par** · Barings **78** | ~22 points |
| **Isagenix** | Three holders **86–88** · fourth **98** | ~11 points |

🔴 **MELD Valuation is an independent valuation firm (founder Daniel Eyman) that sells ASC 820 debt valuation** and explicitly positions itself as "a third choice" independent of manager-controlled valuation. **They are selling the solution to the problem their research describes.** The underlying data — BDC Schedules of Investments — is public and checkable, which is the saving grace; but the *selection* of these four examples is theirs.

**The counterweight — MarkQuality, Q2 2025.** Same method, whole-population instead of examples:

| Metric | Value |
|---|---|
| BDCs tracked (with 5+ peer-held positions) | **127** |
| Positions with usable peer marks | **15,672** |
| Peer-comparable fair value | **$272.5B** of $405.1B tracked |
| **Marked 5+ points from peer median** | **$1.5B — 0.6%** ($912.8M above, $592.3M below) |
| **BDCs whose average absolute gap stayed within 1 point** | **92%** |

MarkQuality extracts valuations directly from SEC filings and notes that a gap from the median "can reflect legitimate differences in position, timing, lot, structure, or valuation methodology rather than mispricing." **`[UNVERIFIED-10]`** MarkQuality's ownership and commercial model are not disclosed on the site; it sells peer-comparison tooling, so it is **also an interested party**, though its interest points the same way as MELD's.

🔴 **Read these together and the honest conclusion is uncomfortable for your pitch: mark dispersion in private credit is a tail phenomenon, not a systemic one.** Ninety-two percent of BDCs mark within a point of their peers. Six-tenths of one percent of comparable value sits 5+ points from consensus. **The Khoros 50-vs-77 case is real and is genuinely shocking — but it is in the 0.6%, not the 92%.**

**Do not hide this. Use it.** The correct framing is: *"Most private-credit marks agree. The ones that don't are the ones that matter — dispersion clusters exactly where credit is deteriorating, which is why it works as a stress signal. I'm not selling accuracy on the 92%. I'm selling early detection on the 0.6%."* That is a defensible product statement and it survives a judge who has read the MarkQuality data.

### 5.3 🔴 Leg three — what the market charges for an unverifiable mark

**This is your product-thesis number, and it is from a primary source.**

> **Jefferies Private Capital Advisory, *2025 Global Secondary Market Review*, published 2026-02-10.**
>
> **"The global secondary market reached $240 billion in transaction volume in 2025, a 48 percent year-over-year increase."**
>
> **"Average LP portfolio pricing finished the year at 87 percent of net asset value (NAV), a 200-basis-point decline from 2024."**

| Strategy | 2025 secondary pricing, % of NAV | **Discount to stated NAV** |
|---|---|---|
| **Buyout** | 92% | **8 points** |
| **Credit** | **91%** | **9 points** |
| Venture and growth | 78% | 22 points |
| **Real estate** | **70%** | **30 points** |
| **All LP portfolios** | **87%** | **13 points** |

Volume split: **LP-led $125B (52%) · GP-led $115B (48%).**

**This is the answer to "what is a mark worth."** In a **$240 billion market**, sophisticated institutional buyers, with full data-room access and weeks of diligence, systematically refuse to pay the manager's stated NAV. **They pay 87 cents.** For private credit — the closest analogue to what is tokenized — **91 cents.** For real estate, **70 cents.**

Some of that discount is liquidity, some is fee drag, some is adverse selection, and some is disbelief in the mark. **You cannot cleanly attribute it, and you should say so before a judge does.** But the outer bound is unambiguous and it is measured annually by a bulge-bracket intermediary: **the gap between a manager's stated NAV and what an arms-length buyer will pay is 9 points in credit and 30 points in real estate.**

**`[UNVERIFIED-11]`** Attribution of the 87%/91%/70% discounts between liquidity premium, fee drag, adverse selection and mark scepticism. Jefferies publishes the pricing, not the decomposition. **Any claim that "X points of the discount is mark uncertainty" is yours, not theirs.**

One reconciliation note for honesty: some secondary coverage of the same report cites **90%** rather than 87% for 2025 LP pricing, and **$162B** volume for 2024. **The 87% and $240B figures above are quoted directly from the Jefferies page itself** and should be the ones you use.

### 5.4 Putting the three legs together

| Leg | Establishes | Best citation |
|---|---|---|
| Academic | Illiquid-asset marks are systematically smoothed and lag reality; PE risk understated ~3x | Baz & Davis, *JPM* 48(9) 2022; Getmansky/Lo/Makarov NBER 9571 |
| Mark data | Dispersion is a **tail** phenomenon — 0.6% of value, but spreads reach 27 points there | MarkQuality Q2 2025; MELD (interested party) |
| Market price | Arms-length buyers discount stated NAV by **9 points in credit, 30 in real estate** | **Jefferies, 2026-02-10** |

**The chain of reasoning that survives cross-examination:** marks are self-reported under Rule 2a-5 → the literature shows self-reported marks are smoothed and lagged → in the tail they diverge by tens of points → and a $240B secondary market prices that uncertainty at 9–30 points of NAV. **Each link has a source. None of them is a projection.**

---

## 6. Canton specifically

Applying the same discipline the companion doc used in §2.2 — **be precise, do not pad.**

| Item | Status | Source & date |
|---|---|---|
| **T-RIZE / Kairos Litigation Limited private credit programme** — up to **$500M**, first tranche **$50M**. UK bankruptcy-remote SPV; programme manager **Horizon Group**; distributed via **Canton Network**; targeted at eligible US and European investors through compliant broker-dealers. T-RIZE provides token minting, onboarding, eligibility controls, transfer permissions, lifecycle management. Collateral functionality **"scheduled for later activation."** | **ANNOUNCED**, not confirmed live | Press announcement, **2026-03-30** (London) |
| **HIFI** — moved pilot → production on Canton | **LIVE**, but **not a private asset**. HIFI is **payments infrastructure**: stablecoin movement across banks and blockchains, fiat on/off-ramp, and liquidity/settlement into **tokenized US Treasuries** | canton.network blog, *From Pilot to Production: HIFI on Canton*, **2026-06-30** |
| **GS DAP** — Goldman Sachs' Canton-based platform — issues digital bonds, and carried **EIB Project Venus (€100M, Nov 2022)** | **LIVE for issuance** | §4.2 |
| **Broadridge Distributed Ledger Repo** | **LIVE**, ~$8T/month | Companion doc §2.2 — **repo, which needs no NAV** |
| **Ctrl Alt** — **$1.4B tokenized** across real estate, private credit, funds, commodities (April 2026; $850M in February 2026) | **`[UNVERIFIED-12]`** — **I could not confirm this is on Canton.** The Canton linkage appeared only in an ecosystem-directory search result, not in a primary source. **This is the highest-value unconfirmed lead in the document and also the most dangerous to assert.** | Ecosystem directory, unconfirmed |
| **RedStone pipeline** — Hamilton Lane SCOPE (private credit), Fasanara F-ONE (alt credit) | **"In early discussions"** — RedStone's own characterisation | Companion doc §2.2; blog.redstone.finance 2026-06-25 |

**The precise, defensible answer to "is anything other than money-market funds live on Canton?":**

> **Repo and collateral workflows are live at enormous scale, and they don't need a NAV. Bond *issuance* is live via GS DAP. Payments are live via HIFI. The one substantial private-credit programme — half a billion dollars via T-RIZE — was announced in March and I have no evidence the first fifty-million tranche has drawn. So: for the assets my product is actually for, the honest answer is not yet.**

That answer is better than any number. It is checkable, it is precise, and it demonstrates the same discipline the companion doc built around `[UNVERIFIED-4]`.

---

## 7. The assessment

### 7.1 Is there a real, present market for attested marks on non-treasury assets?

**No. Not today. Answer that directly if asked, because the data is one search away and evasion costs more than the admission.**

The arithmetic, stated as plainly as it deserves:

- Genuinely opinion-priced assets on-chain: **~$1–2B** (§2.2, my estimate, `[UNVERIFIED-2]`).
- At the **6–12 bps** the market pays for hard-to-value administration (companion §1.2): **$600K–$2.4M per year worldwide, across every chain.**
- On Canton specifically: **zero confirmed live** (§6).
- And all three non-treasury categories **shrank** over the last thirty days (§1.2).

**But note the shape of the comparison with the companion doc, because it is genuinely favourable and it is the reason to keep going.** The treasury market is **8–16x larger in AUM** ($16.16B vs $1–2B) but pays **0.325 bps**, for a pool of **~$525K**. The non-treasury market is smaller but pays **6–12 bps**, for a pool of **$600K–$2.4M**. **The smaller market is the bigger business — by 1.1x to 4.5x — and it is the one where the product is actually needed.**

Both are sub-$10M. Neither is a company today. **The difference is that the treasury market will never need this** — nobody doubts a T-bill's price, and that is structural, not temporal — **whereas the private-asset market needs it now and simply has not tokenized yet.** You are not betting that a need will appear. You are betting that an existing, documented, $1.7–2 trillion need moves on-chain. **That is a distribution bet, not a demand bet, and it is a much better bet to be making.**

### 7.2 The single most credible near-term use case

🔴 **Collateral haircuts on tokenized private-credit fund shares. Specifically, ACRED on Morpho.**

The facts, all from 2026-08-04:

| | |
|---|---|
| Asset | **ACRED** — Securitize Tokenized Apollo Diversified Credit Fund, Ltd. |
| Value | **$115,214,219** across seven networks (Ethereum, Solana, Aptos, SEI, Ink, Avalanche, Polygon) |
| **NAV** | **$1,104** |
| **Price** | **$1,104** |
| Holders | **66** (−4.35% / 30d) |
| Terms | 0.50% management fee, no performance fee, **$50,000 minimum, quarterly subscription periods** |
| Use | **Supplied as collateral on Morpho** across Ethereum, Polygon and OP Mainnet — "supply sACRED as collateral, borrow USDC, and purchase more sACRED — a structured carry trade" (morpho.org). Gauntlet involved in risk parameters. |

**Read the third and fourth rows together. The price *is* the NAV. Identically. To the dollar.**

There is no independent price for ACRED anywhere on earth. The number a lending protocol uses to decide whether to liquidate a leveraged position **is the manager's own quarterly-subscription fund accounting, relayed on-chain.** Apollo marks it; a borrower levers it; a liquidation engine trusts it.

**That is the product, and it exists today at $115M.** Not a hypothetical. A live, leveraged, cross-chain position collateralised by a self-reported Level 3 mark, with 66 holders.

**Why this is the right wedge to name on stage:**

1. **The buyer is identifiable and is not Apollo.** It is the **lender** — the Morpho vault curator, Gauntlet, the risk manager setting the LTV. They have a live economic exposure to the mark being wrong, and they cannot ask Apollo to mark its own book more conservatively.
2. **It answers the companion doc's fatal objection (§7, point seven: "who are the N?").** The N are **parties who already hold an opinion on this mark because they already have money against it** — the lender, the curator, the risk manager, a secondary buyer. **Attestation is a byproduct of a position they already hold, not a new service they must be hired and indemnified to perform.** ACRED-on-Morpho is the concrete instance of that argument.
3. **It converts to money in an existing budget line.** A better mark means a **tighter haircut**, and a tighter haircut is worth basis points of borrowing capacity to a live position. This is the one framing that beats the companion doc's bear-case point four — *"collateral takers just widen the haircut"* — because **here the collateral taker is a protocol competing on capital efficiency, for whom a wide haircut is a lost customer, not a free option.**
4. **It is Canton-plausible without being a Canton claim.** Canton's entire pitch is institutional collateral. Franklin's Canton launch was framed around the Global Collateral Network (companion §2.4). This use case is the same shape, one asset class over.

**`[UNVERIFIED]` note:** the LTV/haircut applied to ACRED on Morpho, and the exact price-feed mechanism, are **not disclosed** in anything I could reach. RedStone appeared in a URL context suggesting oracle involvement but nothing confirmed it. **Do not state the LTV.** Say "collateralised on Morpho at a haircut the vault sets against a price that is definitionally the manager's NAV" — which is true and sufficient.

### 7.3 The bear case, as a sceptical judge would put it

Written to be delivered by him, before a judge delivers it to him. This is the non-treasury complement to companion §7 — assume that one still stands, and add these.

> **"You've moved the pitch from an asset class that doesn't need you to an asset class that doesn't exist.**
>
> **One. Show me the assets.** Tokenized real estate is two hundred million dollars — a hundred and four buildings, five thousand of them would be one REIT. Your largest private-credit asset is Figure's HELOC book, twenty billion dollars, on Figure's own chain, marked at exactly one dollar. There's nothing to attest. Strip out Figure, strip out Maple's crypto-collateralised lending, strip out the AAA CLO funds that already have dealer quotes, and you're left with maybe one to two billion dollars — **by your own estimate, which no tracker publishes.** At twelve basis points that's a two-million-dollar global market. **And all three of your categories shrank last month.**
>
> **Two. Your own best data says the problem is rare.** MarkQuality looked at a hundred and twenty-seven BDCs and fifteen thousand overlapping positions. Ninety-two percent marked within one point of their peers. Six-tenths of one percent sat five or more points from consensus. **You're selling a governance layer for a problem that occurs in one position in a hundred and fifty** — and the only source that makes it sound endemic is a valuation firm selling valuations.
>
> **Three. Tokenized private credit already tried self-reported marks and the answer wasn't better marks — it was exit.** Goldfinch is winding down. Maple lost thirty-six million to one counterparty. Centrifuge pools went delinquent and MakerDAO walked. **In none of those cases did anyone respond by building a valuation committee.** They responded by leaving the asset class. That is the revealed preference of every participant who has actually experienced your problem.
>
> **Four. Nobody wants an honest mark.** The issuer's fees are on NAV. The LP's reported returns are on NAV. The borrower's covenants are on NAV. Baz and Davis say the true volatility is thirty percent against a headline ten — **and every single party to that fiction is better off with the ten.** You're selling truth into a market whose participants are jointly compensated for the lie. Who signs the cheque?
>
> **Five. If you're right about the discount, you've priced yourself out.** You say secondaries trade at ninety-one cents in credit and seventy in real estate. If an attested mark closes even a tenth of that gap, you've created value equal to one percent of NAV — a hundred basis points — and no buyer will let a startup capture two. **And if it closes none of it, then the discount was liquidity and fee drag all along, and you've sold nothing.** You can't tell me which, because Jefferies publishes the price and not the decomposition, and you flagged that yourself.
>
> **Six. On Canton, for private assets, you have nothing.** One announced programme from March, five hundred million headline, fifty million first tranche, **and you can't tell me it's drawn.** Everything actually live on Canton is repo, payments, or a money-market fund — repo needs no NAV, payments need no NAV, and the money-market fund is the asset you already conceded is worth 0.325 basis points.
>
> **Seven. Every failure you cite was discovered by default, not by disagreement.** Goldfinch's depositors found out at seventy percent. **Would five signatures have found it sooner?** Only if at least one signer had information the manager didn't, and an incentive to publish it. **You haven't shown me either.**"

**Which of those actually lands, and what to do about it:**

- **Point one is fatal to the TAM and cannot be argued with. Concede it immediately and completely.**
- **Point two is the sharpest** and most people won't see it coming. Answer with §5.2: you are not selling accuracy on the 92%, you are selling early detection on the 0.6%, and **dispersion is a leading indicator of deterioration, which is worth more than an average.**
- **Point four is the deepest.** Answer: the payer is not the issuer or the LP, it is **the lender against the collateral** (§7.2), who is the one party in the chain whose economics are hurt by an inflated mark.
- **Point three is the one to steal.** Reframe it as *your* evidence — those failures are why the model is discredited and why the next generation of tokenized private credit will be built differently.
- **Point seven is the real one, exactly as companion §7 point seven was.** It has a genuine answer and he must rehearse it: **the signer with information the manager lacks is the secondary buyer and the collateral lender — the parties Jefferies shows are already pricing these assets 9 to 30 points below stated NAV.** They are not hypothetical independent attestors. They are a $240 billion market that already publishes a different opinion of NAV than the manager does, just not on-chain and not in a form anyone can use.

---

## 8. What to say on stage

### 8.1 🔴 Should he quantify private assets at all? — **Recommendation**

**Yes — but cite exactly three numbers, and make two of them numbers that hurt him.**

The companion doc's core insight was that leading with the number that damages you is the highest-credibility move available. That applies twice as hard here, because **the non-treasury numbers are small and a judge from Digital Asset, Circle or a bank can check them in thirty seconds.** Any attempt to make $203M of real estate or $1–2B of opinion-priced credit sound like a market will be caught, and being caught inflating once destroys every other number in the pitch — including the excellent ones in the companion doc.

**Cite these three, in this order:**

1. **"Tokenized real estate is two hundred and three million dollars. A hundred and four assets. The whole world."** *(rwa.xyz, 2026-08-04.)* Small, precise, checkable, and it establishes instantly that he is not selling a fantasy.
2. **"Private credit secondaries trade at ninety-one percent of stated NAV. Real estate at seventy. Jefferies publishes it — two hundred and forty billion dollars of transactions in 2025."** *(Jefferies, 2026-02-10.)* This is the thesis. It is a large, primary-sourced, arms-length market price for exactly the uncertainty he proposes to reduce.
3. **"The true volatility of private equity is thirty percent, not the ten percent that gets reported."** *(Baz & Davis, *Journal of Portfolio Management*, 2022.)* Peer-reviewed, from PIMCO, and it is the mechanism in one sentence.

**Do NOT say:**

- ❌ **"Tokenized private credit is thirty-six billion dollars."** It is $36.27B *represented*, of which **$20.8B is one company's HELOC book on its own chain marked at $1.00.** Someone will know. Use **$7.02B distributed** if he must use a total, and volunteer the Figure caveat before he's asked.
- ❌ **Any Canton figure for private assets.** Same rule as companion `[UNVERIFIED-4]`, and here there isn't even a contested number to misuse — there is nothing.
- ❌ **The Goldfinch "70% vs 20%" figure as fact.** Say "depositors reported" or don't say it (`[UNVERIFIED-4]`).
- ❌ **The Centrifuge dollar amounts** ($2.1M / $5.8M / $1.84M). Aggregator-sourced (`[UNVERIFIED-5]`). The *delinquency* is documented on Centrifuge's own forum; the amounts are not.
- ❌ **"Mark dispersion is endemic in private credit."** MarkQuality's whole-population data says 92% agree within a point. He will be corrected, and it is the correction that ends the pitch.

### 8.2 🔴 Three sentences for the stage — limitation first

> **"Outside treasury funds, the on-chain market I'm built for barely exists — tokenized real estate is two hundred million dollars worldwide, and the largest tokenized private-credit asset is one lender's HELOC book on its own chain, marked at exactly a dollar.**
>
> **But Jefferies publishes what a mark you can't verify is actually worth: private-credit secondaries traded at ninety-one percent of stated NAV last year and real estate at seventy, across two hundred and forty billion dollars of transactions — and Apollo's tokenized credit fund is being used as collateral on Morpho today at a price that is, to the dollar, whatever Apollo says it is.**
>
> **So I'm not betting that someone will want an independent mark; the secondary market already pays nine to thirty points for doubting the manager's. I'm betting those assets come on-chain — and until they do, this is a treasury-fund product with a private-asset thesis, and I'd rather tell you that than have you find it in the Q&A."**

**Why this construction works.** Sentence one gives away the weakest fact in the entire pitch before anyone can use it — and gives it away with the most vivid detail available ($1.00). Sentence two replaces it with the largest primary-sourced number he has, and grounds it in a live, named, checkable position. Sentence three names the load-bearing assumption as an assumption, and **explicitly subordinates this whole document to the companion doc's wedge** — which is the strategically correct move, because the treasury product is the one he can actually ship.

### 8.3 If asked "so why not just build for private credit directly?"

> **"Because there's one to two billion dollars of it on-chain that's genuinely hard to value, and it shrank last month. The pipeline is real — Hamilton Lane and Fasanara are in RedStone's stated Canton pipeline, T-RIZE announced a five-hundred-million-dollar private-credit programme on Canton in March — but announced isn't drawn, and I'm not going to stand here and tell you it's live when I can't verify it. Treasury funds are where the assets are today. Private credit is where the *problem* is. The machine is the same; I'd rather start where I can get a design partner and be ready when the assets arrive."**

### 8.4 If asked "hasn't tokenized private credit already failed?"

**This is the best question he can get. He should want it.**

> **"Yes — and that's my evidence, not my problem. Goldfinch is in wind-down after fifty million in defaults with depositors reporting far worse losses than the dashboard showed. Maple lost thirty-six million to one counterparty who misrepresented its balance sheet. Centrifuge pools went delinquent and MakerDAO walked. Every one of those was a self-reported mark that was wrong until the moment it was catastrophically wrong. The market's answer so far has been to leave the asset class. Mine is that the marks needed more than one signature."**

---

## 9. `[UNVERIFIED]` register — 12 items

| # | Item | Why unverified | Risk if spoken |
|---|---|---|---|
| 1 | rwa.xyz "distributed" vs "represented" value definitions | Methodology page 404 on 2026-08-04. Definitions inferred from asset-level behaviour. Represented total moved +171% in 30 days — a coverage change, not growth. | **Medium** — use distributed value and the risk disappears |
| 2 | The **$1–2B "genuinely opinion-priced on-chain assets"** estimate | **My decomposition of rwa.xyz's asset list.** No tracker publishes this. | **Medium** — flag as own estimate |
| 3 | Global private credit AUM $1.7–2T (Preqin / Moody's / PwC) | Reached via secondary reporting, not primary documents. Three houses agree on the band, which is reassuring. | **Low** — cite as a band |
| 4 | Goldfinch loss figures: $50M+ defaults, **70% depositor losses vs 20% dashboard**, 6-of-8 borrowers, $100M lifetime | GIP-87 and the wind-down are primary (gov.goldfinch.finance). **All dollar and percentage figures are secondary crypto media.** | **HIGH** — most quotable, least verified. Say "reported by depositors" |
| 5 | Centrifuge default amounts ($2.1M Harbor Trade / $5.8M overdue / $1.84M MakerDAO) | Low-quality aggregators. Delinquency itself documented on gov.centrifuge.io. | **HIGH — do not quote the amounts** |
| 6 | Tokenized real-estate spread/volume claim ("$50k ADV, 40%+ spread on $1M") | Vendor blog, no methodology. Direction supported by peer-reviewed Mafrur (2026). | **Medium — use the paper instead** |
| 7 | **VuMe Bond 2030, $500M** (Realiz) | A single rwa.xyz table line. Issuer, placement and whether $500M is outstanding or programme size all unconfirmed. Larger by itself than all tokenized real estate. | **HIGH — do not cite** |
| 8 | EIB digital bond details (€100M Venus / €100M 2029 / £50M / HK corporate) | Secondary reporting; did not reach EIB primary releases. Broadly consistent across sources. | **Low–Medium** |
| 9 | Point estimates in Couts/Gonçalves/Rossi (*RFS* 37(7), 2024) | Paywalled at Oxford Academic; have abstract and direction only. | **Low — cite qualitatively; use Baz & Davis for a number** |
| 10 | MarkQuality's ownership and commercial model | Not disclosed on site. Sells peer-comparison tooling → interested party. Method (SEC filings) is sound and checkable. | **Low** |
| 11 | **Decomposition of the 87%/91%/70% secondary discounts** between liquidity, fee drag, adverse selection and mark scepticism | Jefferies publishes pricing, not attribution. | **HIGH for the thesis** — the bear case attacks exactly here. Concede it first |
| 12 | **Ctrl Alt's $1.4B and whether any of it is on Canton** | Canton linkage from an ecosystem-directory search result only; no primary source. | **HIGH — highest-value lead, most dangerous to assert** |

**Also explicitly not established, and worth knowing he doesn't know it:** the LTV/haircut applied to ACRED collateral on Morpho, and the identity of the price-feed mechanism serving it. Both undisclosed in everything reachable. The *fact* of ACRED-as-Morpho-collateral is documented on morpho.org; the parameters are not.

**Two things that came back cleaner than expected and can be stated flatly:**
- **The Jefferies figures** (§5.3) are quoted from the Jefferies publication itself, dated 2026-02-10. Primary.
- **The academic citations** (§5.1) are all real, correctly attributed, and in the journals named. NBER WP 9571 was confirmed against nber.org directly.

---

## 10. Relationship to `MARKET_AND_PRICING.md`

**No figure in this document contradicts the companion.** Where they touch:

| Companion doc | This document |
|---|---|
| §1.2 — market pays **6–12 bps** for hard-to-value administration vs **0.325 bps** for liquid | §7.1 applies that spread to the non-treasury asset base and finds the smaller market is the **bigger business** |
| §2.2 — do not state a Canton AUM number | §6 applies identical discipline and finds **nothing non-treasury confirmed live** |
| §5.4 item 4 — *"somebody's mark blows up"* is a precondition for this market | §2.5 — **it already happened**, to Goldfinch, Maple and Centrifuge |
| §6 — hard-to-value assets are where the money is | §5 supplies the evidence: academic, dispersion, and a **$240B market's price** |
| §7 point 7 — *"who are the N?"* is existential | §7.2 answers it concretely: **the collateral lender and the secondary buyer**, who already hold the position and already publish a contrary opinion of NAV |
| §7 point 4 — *"collateral takers just widen the haircut"* | §7.2 answers it: on **Morpho**, a wide haircut is a lost customer, not a free option |

**The strategic conclusion.** This document does **not** argue he should re-pitch around private assets. The assets aren't there, they shrank last month, and nothing is live on Canton. **It argues that private assets are the *answer to the bear case*, not the pitch.** The treasury wedge gets him in the room; ACRED-on-Morpho and the Jefferies discount are what he says when a judge asks why any of it matters.

---

## Sources

**Market data — rwa.xyz, all retrieved 2026-08-04**
- Overview: $37.29B distributed / $411.35B represented, ex-stablecoins; stablecoins $295.52B
- Tokenized credit: $7.02B distributed / $36.27B represented, 2,535 assets, 191,108 holders; platform league table
- Asset page **FIGR_HELOC**: $20,812,600,196 represented, Provenance, NAV $1.00 / price $1.0000, launched 2022-01-17, CIK 0002064124
- Asset page **ACRED**: $115,214,219, NAV $1,104 / price $1,104, 66 holders, seven networks, 0.50% mgmt fee, $50,000 minimum, quarterly subscriptions
- Real estate: $202.58M distributed / $279.84M represented, 104 assets, 18,680 holders, 1,012 monthly active addresses, 11 countries
- Non-US government debt: $1.31B, 24 assets, 10,097 holders; Spiko $920.1M, NRW1 $115.4M

**Primary — secondary market pricing**
- **Jefferies Private Capital Advisory, *2025 Global Secondary Market Review*, 2026-02-10** — $240B volume (+48% YoY); LP-led $125B / GP-led $115B; **LP portfolio pricing 87% of NAV (−200bps YoY)**; buyout 92%, credit 91%, venture/growth 78%, real estate 70%

**Academic**
- **Getmansky, M.; Lo, A. W.; Makarov, I.** *An Econometric Model of Serial Correlation and Illiquidity in Hedge Fund Returns.* NBER WP 9571, March 2003; *JFE* 74(3), 2004. 908 TASS funds.
- **Baz, J.; Davis, J., et al.** *The Value of Smoothing.* *Journal of Portfolio Management* 48(9), 2022. **PE true volatility ~30% vs headline ~10%.**
- **Clayton, J.; Geltner, D.; Hamilton, S. W.** *Smoothing in Commercial Property Valuations: Evidence from Individual Appraisals.* *Real Estate Economics* 29(3), 2001, 337–360.
- **Couts, S. J.; Gonçalves, A. S.; Rossi, A.** *Unsmoothing Returns of Illiquid Funds.* *Review of Financial Studies* 37(7), 2024, 2110–2155.
- **Mafrur, R.** *Tokenized but Illiquid? Evidence from Real-World Asset Markets.* *FinTech* 2026, 5, 62 (submitted 2026-05-31, revised 2026-07-17). arXiv:2606.01131.

**Regulatory**
- **SEC Rule 2a-5**, *Good Faith Determinations of Fair Value*, Release **IC-34128**, adopted 2020-12-03, Federal Register 2021-01-06 — valuation designee framework. `sec.gov/files/rules/final/2020/ic-34128.pdf`
- SEC private credit examination sweep — fair value policy compliance (secondary reporting)
- **`[UNVERIFIED]`** IMF *Global Financial Stability Report*, April 2024, Ch. 2 (corporate private credit) — **could not retrieve; imf.org returned 403.** Do not cite.

**Mark dispersion — both interested parties**
- **MELD Valuation**, *Same Loan, Different Price* — Khoros 50/53/77 (Goldman Sachs BDC / Hercules / Sixth Street); Pluralsight 97/83 (Golub / Blue Owl); Auven par/78 (Oaktree / Barings); Isagenix 86–88/98. From public BDC Schedules of Investments. **Sells ASC 820 debt valuation.**
- **MarkQuality**, *State of BDC Private-Credit Marks*, Q2 2025 — 127 BDCs, 15,672 peer-marked positions, $272.5B of $405.1B comparable; **$1.5B (0.6%) marked 5+ points from peer median; 92% within 1 point.** Extracted from SEC filings. **Sells peer-comparison tooling.**

**Defaults**
- **GIP-87**, gov.goldfinch.finance — orderly wind-down of Goldfinch Prime; maintenance mode June 2026. Loss figures via The Defiant, CoinMarketCap, HTX News (`[UNVERIFIED-4]`)
- Maple / Orthogonal Trading, December 2022 — $36M across 8 facilities, ~30% of active loans; CoinDesk and secondary
- Centrifuge, `gov.centrifuge.io/t/issuer-harbor-trade-credit-series-2/442` — Harbor Trade Series 2 delinquency; **amounts unreliable** (`[UNVERIFIED-5]`)

**Canton**
- T-RIZE / Kairos Litigation Limited / Horizon Group — up to $500M private credit programme, first $50M tranche, **announced 2026-03-30**
- canton.network blog, *From Pilot to Production: HIFI on Canton*, 2026-06-30 — payments and tokenized-Treasury liquidity
- GS DAP — EIB Project Venus, €100M, November 2022
- Ctrl Alt — $1.4B tokenized (April 2026); **Canton linkage unconfirmed** (`[UNVERIFIED-12]`)
- RedStone pipeline (Hamilton Lane SCOPE, Fasanara F-ONE) — via companion doc §2.2

**Collateral use**
- morpho.org — sACRED as collateral to borrow USDC ("a structured carry trade"); ACRED available as collateral on Morpho across Ethereum, Polygon and OP Mainnet; Gauntlet involved in risk parameters. **LTV and price-feed mechanism not disclosed.**

**Off-chain private credit size** — Preqin ~$1.7T (start 2026); Moody's >$2T 2026 → $4T 2030; PwC >$2T → $3.4T 2030. All via secondary reporting (`[UNVERIFIED-3]`).
