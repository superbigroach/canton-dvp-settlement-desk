# Market and Pricing — What Fund Valuation Infrastructure Actually Costs

**Prepared:** 2026-08-03 · **For:** HackCanton Season 2 pitch, Wednesday 2026-08-05
**Product in scope:** K-of-N committee-attested NAV / official price fixing on Canton, plus a sealed closing auction for price discovery. Near-term wedge = attested NAV for tokenized money-market and treasury funds.

> **Rule for this document:** every number carries a source and a date. Anything I could not verify from a primary or credible secondary source is tagged **`[UNVERIFIED]`** and must not be spoken on stage as fact. There are **11** such items, listed in §8.
>
> **The single most important number in this document is 0.325 basis points.** Read §1.1 before anything else. It is the number that decides whether this is a business, and it is not the number you want.

---

## 0. Executive summary — the four findings that matter

| # | Finding | Consequence |
|---|---|---|
| 1 | **Calculating a NAV costs 0.325 bps of fund assets.** Brown Brothers Harriman charges exactly that to strike the daily NAV for the BBH funds, disclosed in an SEC filing. | The job you are replacing is already close to free. On the *entire* $16.16B tokenized-treasury market, total NAV-calculation spend is roughly **$525,000 per year**. You cannot build a company on a share of that. |
| 2 | **Producing an *official price* costs 10–15x more than calculating a NAV.** SPY pays S&P Dow Jones **3 bps of AUM plus $600,000/year** for the right to reference the S&P 500. | Your pricing case rests entirely on being classified as a **benchmark/index administrator**, not a fund accountant. This is the whole argument. |
| 3 | **The tokenized-fund market on Canton is small and not publicly measurable.** Total tokenized Treasury/MMF AUM across *all* chains is $16.16B (rwa.xyz, 2026-08-04). Public trackers do not report Canton at all — rwa.xyz lists USYC on BNB Chain, Ethereum and Solana, and does not list Canton, despite Circle/Hashnote having publicly launched USYC on Canton. | Do not quote a Canton tokenized-fund AUM figure. You will be wrong or unfalsifiable, and a judge from Circle, Franklin or DA will know. |
| 4 | **Canton app rewards are a gas rebate, not a revenue line.** Rewards are capped at **$1.50 per transaction**, and at current network volume (~40M tx/month) the pool works out to almost exactly that cap. A NAV venue does tens of transactions per day, not millions. | Realistic app-reward income at design-partner scale is **~$10K/year**. Mention it as cost recovery. Never as a business model. |

---

## 1. What fund administration and valuation cost today

### 1.1 The anchor number: 0.325 basis points to calculate a NAV

This is the closest available public price for *exactly the job the product replaces*, and it is unusually well documented because BBH is both custodian and fund accountant and therefore had to disclose the rate.

> "BBH acts as a custodian and fund accountant and receives custody and fund accounting fees from the Fund calculated daily and incurred monthly. **BBH holds all of the Fund's cash and investments and calculates the Fund's daily net asset value.** The custody fee is an asset and transaction-based fee. **The fund accounting fee is an asset-based fee calculated at 0.325 basis points per annum of the Fund's net asset value**, effective from September 1, 2023 based on the new agreement. **The fund accounting fee was 0.40 basis points per annum until August 31, 2023.**"
>
> — BBH Trust, Form N-CSR for fiscal year ended 2023-10-31, filed 2026-01-05 (accession 0001213900-24-001494). Identical language appears in the notes to every fund in the trust.

Two things to notice. First, the rate is **0.325 bps = 0.00325% = $32.50 per $1 million of NAV per year**. Second, it **fell 19%** (0.40 → 0.325 bps) at a contract renewal in 2023. This is a commodity in structural price decline.

What that rate produces in dollars:

| Fund | NAV | Fund-accounting fee at 0.325 bps |
|---|---|---|
| BBH U.S. Government Money Market Fund (actual, 2023-10-31) | $5,898,966,281 | ~$191,700/yr |
| Circle USYC (largest tokenized MMF) | $3.006B | ~$97,700/yr |
| BlackRock BUIDL | $2.673B | ~$86,900/yr |
| A $1B tokenized fund | $1.000B | **~$32,500/yr** |
| **Every tokenized Treasury/MMF fund on earth, combined** | **$16.16B** | **~$525,200/yr** |

*Fund NAVs from rwa.xyz, data date 2026-08-04. Arithmetic mine.*

Cross-check on the same filing: BBH's U.S. Government Money Market Fund incurred **$499,969 in combined custody *and* fund accounting fees** on $5.899B — about 0.85 bps for the two services together. Same source.

**Say this on stage before a judge says it to you.** It is the strongest possible signal that you understand the industry, and it sets up §3.

### 1.2 Full administration costs more — but only for hard assets

Fund accounting is the cheap slice. Full administration of an *alternative* fund, where the administrator also does investor servicing, regulatory reporting and hard-to-value pricing, is priced an order of magnitude higher:

> Administrative Services Fee, tiered on month-end net assets: **12 bps on the first $250 million; 10 bps on the next $250 million; 7 bps on the next $250 million; 6 bps thereafter, less $70,000.**
>
> — Aetos Long/Short Strategies Fund LLC, administration agreement with HedgeServ, filed as Exhibit 99(k)(2) to Form 486BPOS, 2024-05-31 (accession 0001104659-24-067205). Identical schedules in the Aetos Multi-Strategy Arbitrage and Distressed Investment Strategies funds.

The gap between **0.325 bps** (strike a NAV on liquid, priceable assets) and **6–12 bps** (administer a fund-of-hedge-funds with unobservable marks) is the single most useful pricing insight in this document: **the market pays for valuation difficulty, not for valuation itself.** Tokenized Treasury funds are the *easiest* assets to value in the entire industry. That is precisely why they are cheap to price — and it is the core of the bear case in §7.

### 1.3 Small funds pay in absolute dollars, not basis points

A real small-ETF cost stack, from audited financials:

| SmartETFs Dividend Builder ETF, FY2024 | Amount |
|---|---|
| Net assets, 2024-12-31 | $39,095,033 |
| Fund accounting fee and expenses | $22,530 |
| Administration fees | $17,729 |
| Custody fees and expenses | $19,928 |
| **Accounting + administration** | **$40,259 (~10–12 bps)** |

*Guinness Atkinson Funds / SmartETFs, Form N-CSR filed 2025-03-10 (accession 0001398344-25-005256).*

So the bps *rate* is 30x higher for a small fund, but the absolute dollars are only **$40K**. The floor for any provider serving small funds is a minimum annual fee in the tens of thousands, not a rate.

### 1.4 The incumbents' rate cards do not exist publicly — and that is a verifiable fact

You will be asked "what does BNY charge?" The honest and impressive answer is that **nobody outside the contract knows, and the contracts are filed with the SEC with the numbers cut out.** Two direct examples:

> "Fund Administration, Fund Accounting & Portfolio Compliance Services Fee Schedule – Effective January 1, 2026. Annual Fee Based Upon Average Net Assets of the Fund Complex: **___ BPS ON THE FIRST $___ / ___ BPS ON THE NEXT $___ / … / ___ BPS ON THE BALANCE**"
>
> — Sixth Amendment to the Amended and Restated Fund Accounting Servicing Agreement between Baird Funds, Robert W. Baird & Co. and **U.S. Bancorp Fund Services**, filed as Exhibit 99(h)(iii)(g) to Form 485BPOS, 2026-04-29 (accession 0000894189-26-013396). Every rate is redacted.

> "Master Portfolios Annual Fee Based Upon Average Net Assets per Fund: **[ ] basis points on the first $1 billion / [ ] basis points on the balance.** Minimum Annual Fee: **$[ ]**"
>
> — Kinetics Mutual Funds administration agreement with **U.S. Bank**, filed to Form 485BPOS, 2023-04-28 (accession 0000894189-23-003327).

**`[UNVERIFIED-1]`** Published rate cards for BNY, State Street, Northern Trust, Citco, SS&C and Apex: none found. Their fee schedules are redacted even in SEC filings.

**`[UNVERIFIED-2]`** Global fund-administration market size. Vendor market-research estimates for 2024–25 range from **$8.6B** (dataintelo) to **$13.6B** (growthmarketreports) to **$100.2B** (marketintelo, "outsourcing" definition). A 12x spread across three vendors means none of them are reliable. **Do not use a TAM figure from this category.** The one credible industry benchmarking exercise — Barrington Partners' Fund Accounting and Administration Cost Survey, whose 2023 edition covered "15 fund companies with $3.6 trillion in unique AUM" — publishes its cost data only to participants.

### 1.5 The pricing/valuation data vendors

The closer commercial analogue — selling *a price* rather than *a service* — is evaluated pricing. ICE Data Services covers "approximately 2.5 million fixed income instruments globally" via its Continuous Evaluated Pricing product (ICE Developer Portal, accessed 2026-08-03).

**`[UNVERIFIED-3]`** ICE Data Services, Bloomberg BVAL and LSEG/Refinitiv evaluated-pricing fees. No published rates found. All are negotiated enterprise contracts.

The one *published* price point in the price-data category, and it is a good one, is in §3.2.

---

## 2. Tokenized funds — the honest addressable set

### 2.1 The whole market, all chains

**Total tokenized U.S. Treasury / money-market fund value: $16.16 billion**, up 4.06% over 30 days (rwa.xyz, data date **2026-08-04**).

| Rank | Fund | AUM | Platform | Networks listed by rwa.xyz |
|---|---|---|---|---|
| 1 | **Circle USYC** | $3.006B | Circle | Solana, Ethereum, BNB Chain |
| 2 | **BlackRock BUIDL** | $2.673B | Securitize | Tempo, Solana, Polygon, Optimism, Ethereum, BNB Chain, Avalanche, Arbitrum, Aptos |
| 3 | **Ondo USDY** | $2.153B | Ondo | Sui, Stellar, Solana, SEI, Plume, Noble, Mantle, MANTRA, Ethereum, BNB Chain, Arbitrum, Aptos |
| 4 | **iBENJI** (Franklin Templeton) | $1.731B | Benji | Ethereum, BNB Chain |
| 5 | **Janus Henderson JTRSY** | $881.7M | Centrifuge | Stellar, Plume, Monad, Ethereum, Celo, Base, BNB Chain, Avalanche, Arbitrum |

For scale: the *entire* tokenized RWA market ex-stablecoins "exceeded $36B as of late 2025" (Canton Network, *State of RWA Tokenization 2026*, published 2025-12-18).

### 2.2 How many are on Canton — the number you must get right

**Publicly documented tokenized funds / fund infrastructure live or announced on Canton: three, plus a named pipeline.**

| Asset / initiative | Status | Source & date |
|---|---|---|
| **USYC** (Circle/Hashnote tokenized MMF) | Live on Canton | "Hashnote brings USYC to the Canton Network to offer the first tokenized money market fund with built-in privacy" (canton.network press release); Circle acquisition of Hashnote announced 2025-01-21, USYC then "$1.52B deployed as of January 15, 2025" |
| **BENJI / Franklin Templeton** | Platform expanded to Canton | canton.network press release + PR Newswire, **2025-11-12**. Franklin Templeton "over $1.69 trillion in AUM as of October 31, 2025". Benji platform reported at "more than $730 million in tokenized assets" (blockchainjournal.news, Nov 2025) |
| **BNY + Goldman Sachs tokenized MMF solution** on GS DAP (Canton-based) | Announced **2025-07-23**; participants at launch: **BlackRock, BNY Investments Dreyfus, Federated Hermes, Fidelity Investments, Goldman Sachs Asset Management** | goldmansachs.com press release; canton.network news |
| RedStone pipeline: **Hamilton Lane SCOPE** (private credit), **Fasanara F-ONE** (alt credit), **Spiko** (tokenized T-bills), **Re** (reUSD) | "In early discussions" per RedStone | blog.redstone.finance, **2026-06-25** |

**`[UNVERIFIED-4]`** **Tokenized-fund AUM native to Canton.** No public tracker reports it. rwa.xyz's USYC page (2026-08-04) lists only BNB Chain, Ethereum and Solana and **does not list Canton at all**, even though USYC's Canton launch is a matter of public record. Canton is permissioned and privacy-preserving, so the standard on-chain trackers cannot see it. **This means any Canton tokenized-fund AUM number you give on stage is unfalsifiable — which is worse than not giving one.**

Note the trap in Canton's *headline* numbers: the network is variously reported at "$6 trillion in tokenized real-world assets" (blockeden.xyz, 2026-01-27; blockzeit.com) and "over $9 trillion" (RedStone, 2026-06-25), with Broadridge's Distributed Ledger Repo alone at **"$8 trillion per month in repo transactions"** (Interstice Digital, *Who Is Building on Canton*, 2026). **That is repo and collateral flow, not fund AUM.** Repo does not need a NAV. If you cite $6–9T as your market you will be correctly accused of counting the wrong thing.

### 2.3 Who prices them today, and is anyone attesting?

The honest answer: **the fund administrator strikes the NAV, and an oracle relays it.** Nobody is publishing a multi-party attested NAV. But two well-funded incumbents already occupy the relay position, and both are on Canton or adjacent:

- **Chainlink NAVLink / SmartData** — "NAVLink Feeds provide real-time, tamper-proof data on the Net Asset Value (NAV) of tokenized assets, funds, or portfolios" (docs.chain.link, accessed 2026-08-03). Named institutional deployments: **Fidelity International's FILQ** tokenized fund (with J.P. Morgan and Sygnum), **UBS**, and **Amundi**. This is not a roadmap item; it is shipped with tier-one logos.
- **RedStone** — "the first decentralized production oracle infrastructure on Canton," Daml-native, running as a Canton participant node, explicitly offering "**custom NAV feeds for tokenized RWA funds**" and configuring "bespoke price-discovery methodologies in collaboration with the asset issuer" for exotic/illiquid instruments. Canton Foundation member since 2025-07-24. (blog.redstone.finance, 2026-03-24 and 2026-06-25.)

**Your genuine differentiation is narrow but real: both of these are *relays*.** Chainlink's and RedStone's NAV is still one administrator's assertion, cryptographically transported. A K-of-N committee changes *who asserts*, not *how it travels*. Make that the sentence. Do not claim the transport layer — it is taken.

**`[UNVERIFIED-5]`** Whether any tokenized fund currently publishes a multi-party attested or committee-signed NAV. I found none. This is absence of evidence, not evidence of absence.

### 2.4 Is tokenized-fund collateral use real?

**Directionally yes, quantitatively unproven.**

The intent is documented at the highest level. Circle's acquisition announcement stated USYC "will play a critical role as **collateral** in digital asset markets" (2025-01-21). Franklin Templeton's Canton integration was framed around "Canton's **Global Collateral Network**," providing "market makers and institutions with a trusted new source of liquidity and collateral in regulated digital markets" (2025-11-12). The BNY/Goldman MMF initiative was explicitly "a significant step towards enhancing the **utility and transferability** of existing MMF shares" (2025-07-23).

**`[UNVERIFIED-6]`** **Volume of tokenized fund shares actually posted as collateral.** Every source asserts the capability; none publishes a number. If a judge asks "how much collateral?", the correct answer is "publicly, nobody has disclosed it — which is itself a problem for anyone sizing this market, including me."

---

## 3. Comparable business models — what people actually charge

### 3.1 Index and benchmark administrators — your real comparable

This is the strongest pricing evidence in the document, and it comes from a peer-reviewed source that hand-collected the data from SEC filings.

> **"State Street charges SPY investors 9 basis points (bps) per year, and in turn, pays 3 bps of the ETF assets plus a flat fee of $600,000 per year to S&P Dow Jones."**
>
> — An, Yu; Benetton, Matteo; Song, Yang. *Index Providers: Whales Behind the Scenes of ETFs.* Journal of Financial Economics (working paper dated 2022-01-12). Fee structure as of December 2020.

Everything else from the same paper:

| Finding | Figure |
|---|---|
| SPDR S&P 500 (SPY) licence to S&P Dow Jones | **3 bps of AUM + $600,000/yr flat** |
| SPDR Dow Jones Industrial Average (DIA) | **4 bps of AUM** |
| Invesco QQQ licence to Nasdaq | **9 bps of the 20 bps management fee** |
| Typical contract form | **"x bps of AUM + $y per year"**, x with AUM breakpoints |
| Share of licensing fee that is AUM-linked | **>95%** — flat fees are "just a tiny fraction" |
| Average implied licensing fee (2019) | **4.4 bps of ETF AUM** |
| Estimated marginal cost of index provision | **1.6 bps** → markup 2.8 bps, Lerner index ~63% |
| Share of all ETF management fees paid to index providers | **~one-third**, rising from 31.4% (2010) to 35.7% (2019) |
| Fee dispersion | FT: index providers "charge some asset managers **13 times as much** as other clients for similar bundles" (ft.com) |

Cross-check against a public company. MSCI's Index segment, FY2025 (Form 10-K filed 2026-02-06):

- Asset-based fees: **$770,670 thousand**
- Recurring subscriptions: **$957,897 thousand**
- Index segment Adjusted EBITDA margin: **76.4%**
- Year-to-date average AUM in ETFs linked to MSCI equity indexes, Dec 2025: **$2,011.3 billion**

$770.67M ÷ $2,011.3B = **3.83 bps** — but this is a strict **upper bound**, because the numerator also includes non-ETF indexed funds and futures/options revenue while the denominator is ETF AUM only. MSCI's 10-K notes ETF and non-ETF revenue grew 21.5% and 12.4% "primarily driven by increases in average AUM, **partially offset by a decrease in average basis points**" — i.e. even the benchmark oligopoly is in price decline.

**The takeaway you should internalise:** an official price is worth **3–4.4 bps + a flat fee**. Calculating a NAV is worth **0.325 bps**. **The same computation is worth 10–15x more when it is an official price than when it is an accounting output.** Your entire pricing case is the argument that a K-of-N attested fixing sits on the index side of that line.

### 3.2 Oracle and price-feed providers — the one published price

Pyth is the only serious player publishing a rate card, which makes it the most citable comparable you have:

| Tier | Price | Content |
|---|---|---|
| Pyth Crypto | **Free** | Crypto, 1-second updates |
| Pyth Crypto+ | **$5,000/month** ($60K/yr) | Crypto, 1-millisecond updates |
| **Pyth Pro** | **$10,000/month** ($120K/yr) | Global cross-asset, 1ms, enterprise support, redistribution rights |

— pyth.network blog, *Introducing Pyth Pro*, **2025-09-24**. Same source: institutions collectively spend "**more than $50 billion annually**" on market data, with costs "rising more than 50% in just the last three years," and legacy vendors charging "**upwards of $250,000 per month** for incomplete coverage."

**`[UNVERIFIED-7]`** Chainlink and RedStone commercial pricing. Neither discloses rates. Chainlink's on-chain protocol revenue (CCIP fees, etc.) is tracked by DefiLlama/Token Terminal but is not comparable to an enterprise data contract.

### 3.3 Enterprise infrastructure software sold to financial institutions

**`[UNVERIFIED-8]`** I could not find a credible, citable benchmark for seed-stage single-product ACVs sold into banks and asset managers. The search results were entirely content-marketing pages with no methodology. **Do not quote an ACV benchmark.** The Pyth Pro rate card in §3.2 — $60K–$120K/yr for an institutional data subscription — is a far better anchor and is a real published price.

---

## 4. The Canton-specific revenue leg — how big is it really?

### 4.1 How featuring works

- Only **featured** applications earn app rewards. CIP-0078 removed rewards for unfeatured apps entirely. (Canton Network docs, *Featured App Activity Markers*.)
- To become featured you submit your application provider party ID via the form at **sync.global/featured-app-request/**. The request goes to the **Tokenomics Committee of the Canton Foundation**, which reviews and responds; topics are tracked publicly at lists.sync.global/g/tokenomics. **You can self-feature on DevNet for testing.** (Canton Network docs, *Canton Coin Tokenomics*.)
- Under the pre-CIP-0104 marker model, "a featured application receives a minting weight with a total equivalent value of about **$1 US**" per activity marker.
- **CIP-0104 (Traffic-Based App Rewards)** replaces activity markers with rewards proportional to **actual traffic burned**. Approved **2026-02-12** (draft 2026-01-29), rolling out in five increments with "at least 30 days" between making traffic costs observable and switching over. Rewards below the `appRewardCouponThreshold` (default **$0.50** per round) are burned, not paid.

### 4.2 The actual arithmetic

From Canton's own blog posts:

- "From January 2026, **62% of the total rewards pool** will go to featured applications, representing around **516 million CC**, shared among app providers every month." (*Cantonomics for App Builders*)
- Reward per transaction is capped: "**up to a max cap of $1.50 per transaction**." Traffic costs about **$1.00** per transaction, and an app provider that pays the traffic itself receives **$0.20** back. The "up to 170% of application-generated traffic fees" headline = ($1.50 + $0.20) ÷ $1.00. (*Earn with every transaction*)
- Canton's own worked example: "If your app transactions in the month of Jan was 100K … (100K ÷ 40M) × (516M × 0.15) = **$193.5K**" — i.e. **40 million network transactions per month** and an assumed CC price of **$0.15**.

**Current CC price: $0.116395** (CoinGecko API, retrieved 2026-08-03; market cap $4.57B). That is **22% below** the $0.15 in Canton's own example.

Recomputing at the live price: 516M CC × $0.116395 = **$60.06M/month** total featured-app pool. Divided by 40M network transactions = **$1.50 per transaction** — which is *exactly the cap*. In other words, at today's price and volume the cap binds, and **$1.50/transaction is a hard ceiling, not a floor.**

### 4.3 What that means for a NAV venue

A K-of-N attested NAV is a **low-transaction-count** application. Assume 5 attestors + 1 finalisation = ~6 Canton transactions per NAV strike per fund.

| Scenario | Transactions/month | Max app rewards/yr at $1.50/tx |
|---|---|---|
| 3 design-partner funds, 1 strike/day | ~540 | **~$9,700** |
| 20 funds, 1 strike/day | ~3,600 | ~$65,000 |
| 50 funds, 1 strike/day | ~9,000 | ~$162,000 |
| 500 funds, 4 strikes/day | ~360,000 | ~$6.5M |

The last row requires roughly 100x the number of tokenized funds that exist on Canton today. **Verdict: app rewards are material only at a scale that is many years away. At design-partner scale they cover your traffic costs and not much else.**

**`[UNVERIFIED-9]`** Actual Canton Coin app rewards earned by any *named* application. No app has published its earnings, and Canton's Scan data is not aggregated per-app publicly. The $1.50 cap and the 516M CC pool are documented; realised per-app earnings are not.

**One structural point worth making on stage, because it is a genuine insight and not a number:** under CIP-0104 rewards track *traffic burned*, and a settlement/auction transaction that moves value is far heavier than a NAV attestation. **The auction leg of your product earns app rewards; the NAV leg essentially does not.** That is an argument for why both halves belong in one venue.

---

## 5. Defensible revenue ranges

Reasoning shown, so it survives cross-examination.

### 5.1 First design partner: **$50,000 – $150,000 per year**

**Why not lower:** the small-fund cost stack in §1.3 shows funds already absorb ~$40K/yr just for accounting + administration on a $39M fund. A $50K floor is credible because it is inside a budget line that already exists.

**Why not higher:** you are pre-revenue and solo. A design partner is buying an experiment. The published institutional-data comparable — Pyth Pro at **$10,000/month = $120,000/yr** for global cross-asset coverage with enterprise support — is the realistic ceiling for a first contract from a single-product vendor. Charging more than Pyth charges for all asset classes, before you have a production track record, is not defensible.

**How to structure it:** flat annual platform fee, **not** AUM-linked. AUM-linked pricing at design-partner stage caps you at the §1.1 arithmetic — 0.325 bps on a $2B fund is $65K and on a $200M fund is $6,500, which is not a contract. A flat fee also survives the fund shrinking.

### 5.2 Steady state: **$150,000 – $500,000 per year per issuer**

Two independent routes converge on the same band, which is why it is defensible:

**Route A — index-administrator pricing.** The industry-standard contract form is **"x bps of AUM + $y flat"** (>95% of index licensing fees are AUM-linked; An/Benetton/Song). At **1–3 bps** — deliberately *below* the 3–4.4 bps that S&P DJI and MSCI command, because you have no brand:
- $2B tokenized fund at 2 bps = **$400,000/yr**
- $1B fund at 2 bps = **$200,000/yr**
- $500M fund at 3 bps + $100K flat = **$250,000/yr**

**Route B — enterprise data subscription.** Pyth Pro at $120K/yr for one subscriber; an issuer with several funds plus committee operations plus the auction venue plausibly supports **2–4x** that = **$240K–$480K/yr**.

**The honest hedge:** these ranges assume you are priced as a **benchmark administrator**. If a buyer classifies you as **fund accounting**, the same customer is worth **$32,500 per $1B** and the business does not clear. **The whole company is a bet on which side of that line you land, and you should say so out loud.** A judge who hears you name your own key risk will trust the rest of your numbers.

### 5.3 Total addressable market today — the honest version

| Basis | Applied to all $16.16B of tokenized Treasury/MMF AUM | Annual revenue pool |
|---|---|---|
| Fund-accounting rate (0.325 bps) | $16.16B | **~$525K** |
| Your steady-state rate (2 bps) | $16.16B | **~$3.2M** |
| Index-administrator rate (3–4.4 bps) | $16.16B | **$4.8M – $7.1M** |

And **on Canton specifically the number is smaller still and cannot be stated** (see `[UNVERIFIED-4]`).

**This is a sub-$10M market today, worldwide, across every chain.** That is not a reason to stop; it is a reason to be precise about what has to be true for it to matter.

### 5.4 What would have to be true for this to become a real market

1. **Tokenized fund AUM grows 10–100x.** $16.16B → $160B–$1.6T. At $500B and 2 bps the pool is $100M/yr. Growth of 4.06% over 30 days (rwa.xyz, Aug 2026) annualises to roughly 60%/yr — at that rate $16B reaches $160B in about five years. That is the single load-bearing assumption.
2. **Canton captures a meaningful share of tokenized funds.** Today the public evidence is three funds/initiatives. Franklin, Circle, BlackRock, Federated Hermes, Fidelity and GSAM are all *present*; whether their AUM follows is unknown.
3. **Fund shares actually get used as collateral at scale, and haircuts start pricing mark uncertainty.** This is the mechanism that turns "nice governance" into "cheaper collateral" — the only version of this product with real pricing power. Currently asserted everywhere and quantified nowhere (`[UNVERIFIED-6]`).
4. **Somebody's mark blows up.** Honest but true: the SEC's Calvert Investment Management matter (Administrative Proceeding IA-4554, 2016) saw the adviser pay **$27 million to the funds and shareholders** plus a **$3.9 million settlement** after mispricing led to shareholder transactions "at the wrong NAV" and "inflated asset-based fees." An equivalent event in a tokenized fund used as collateral creates this market overnight.

---

## 6. Where the money actually is — an alternative framing worth considering

The tokenized-MMF NAV wedge is a **$525K–$7M worldwide** pool today. But the same K-of-N attestation machinery points at markets that are structurally 10–100x richer, and you should know they exist even if you do not pitch them:

- **Hard-to-value assets.** §1.2 proved the market pays **6–12 bps** for administration where marks are unobservable, versus 0.325 bps where they are not. RedStone's stated Canton pipeline is already the illiquid set: **Hamilton Lane SCOPE** (private credit), **Fasanara F-ONE** (alternative credit). A committee-attested mark on private credit is worth vastly more than one on T-bills — because on T-bills nobody doubts the number.
- **The closing auction.** An official closing price is the one thing in this space that is unambiguously priced like a benchmark (§3.1) rather than like accounting, and it is the leg that generates enough Canton traffic to earn app rewards (§4.3).

**If the founder is a former equities trader, the closing auction is the credible half of this pitch, and the attested NAV is the wedge that gets him in the room.** That ordering is worth considering.

---

## 7. The strongest argument that this is not a business

Stated as a hostile, well-informed judge would state it. He should be able to deliver this himself, verbatim, before answering it.

> **"You have built a more expensive way to produce a number that is already almost free, for a problem nobody has demonstrated they have.**
>
> **One.** Calculating a NAV costs 0.325 basis points. BBH disclosed it. It fell 19% at the last renewal. You are entering a commodity in structural price decline, and your entire addressable spend across every tokenized Treasury fund on earth is about half a million dollars a year.
>
> **Two.** The assets are Treasury bills and overnight repo. They are the easiest instruments in finance to price. The market pays 6 to 12 basis points to value fund-of-hedge-funds because those marks are genuinely uncertain, and 0.325 basis points to value T-bills because they are not. **You have chosen the one asset class where nobody doubts the administrator's number.**
>
> **Three.** The actual complaint about tokenized MMF NAV is *staleness*, not *dishonesty*. It is struck once a day. A K-of-N committee does not make it fresher — it makes it slower, because now you wait for a quorum. You have solved trust in a market that has a latency problem.
>
> **Four.** The buyer you need is the collateral taker, and collateral takers already have a cheap, well-understood, zero-integration answer to an uncertain mark: **widen the haircut.** You are selling basis-point precision to people who round to the nearest whole percent.
>
> **Five.** The position is occupied. Chainlink is delivering NAV on-chain today for **Fidelity International, UBS and Amundi**. RedStone is the production oracle **on Canton**, is a Canton Foundation member, already advertises custom NAV feeds for tokenized funds, and is in discussions with Hamilton Lane, Fasanara and Spiko. Both are funded, staffed and shipping. You are one person with a hackathon build.
>
> **Six.** Your customers are BlackRock, Franklin Templeton, Circle, Federated Hermes and Goldman Sachs Asset Management. These firms do not buy valuation governance from a solo founder. They build it, or they demand it from the administrator they already pay — who will supply it for free rather than lose the mandate.
>
> **Seven — and this is the one that actually kills it.** K-of-N requires **N credible, independent attestors** who are willing to put their name on a valuation. Valuation attestation carries liability; the Calvert matter cost $27 million. Who are your N? What are they paid out of a $100,000 contract? And why would a regulated institution accept legal exposure on someone else's fund for a share of that? **You have not built a product. You have built the software layer of a consortium that does not exist, and consortia are the hardest thing in capital markets to assemble — harder than the code by an order of magnitude.**
>
> **And you cannot even size it.** No public tracker reports Canton. rwa.xyz does not list Canton among USYC's networks even though USYC launched there. You cannot tell me your market's size on the one chain you have chosen."

**Point seven is the real one.** He needs an answer to "who are the N and what do they get" before Wednesday. Everything else is arguable; that one is existential. The best available answer is probably that **the N are parties who already have a commercial reason to hold an opinion on the mark** — the collateral taker, the issuer, the administrator, a market maker — so attestation is a byproduct of a position they already hold, not a new service they are hired to perform. That reframing is worth rehearsing.

---

## 8. What to say on stage about money

### Give a number. Give exactly three, and give them in this order.

Refusing to give numbers reads as not having done the work — fatal in front of judges from Digital Asset, Circle and the banks. But the numbers must be **incumbent prices**, not projections. Cited facts cannot be argued with; forecasts from a pre-revenue solo founder always can.

**The three numbers, in order:**

1. **"Calculating a NAV costs 0.325 basis points. BBH discloses it in their N-CSR."**
   Leading with the number that *hurts* you is the single highest-credibility move available. It tells every finance person in the room that you know the industry and are not selling them a fantasy.

2. **"An official price costs 3 basis points plus $600,000 a year. That is what State Street pays S&P Dow Jones for SPY."**
   This is the whole business in one sentence. The same computation is worth 10–15x more as an official price than as an accounting output.

3. **"So the question isn't whether this is worth building — it's which of those two things a K-of-N attested fixing is. I think it's the second. I'll know when someone pays me."**
   Naming your own key uncertainty is what separates a founder who has done the work from one who has done a spreadsheet.

### If asked "what would you charge?"

> "A flat **$50,000 to $150,000** for a first design partner — priced against Pyth Pro at $120,000 a year, which is the only published rate card in institutional data. Steady state, **1 to 3 basis points plus a flat fee**, which is the standard index-licensing contract form and deliberately below the 3 to 4.4 that S&P and MSCI command, because I don't have their brand yet."

### If asked "how big is the market?"

> "Every tokenized Treasury and money-market fund on earth is **$16.16 billion** as of yesterday, per rwa.xyz. At index-administrator pricing that's a **$5 to $7 million** annual pool. It's small. It grew 4% in the last thirty days. **I'm not going to tell you what's on Canton specifically, because no public tracker reports Canton — rwa.xyz doesn't even list Canton among USYC's networks, and USYC launched there. Anyone who gives you a Canton AUM number is guessing.**"

That last sentence will win more credibility than any TAM slide.

### If asked about Canton app rewards

> "Capped at **$1.50 per transaction**. At today's CC price and network volume the pool works out to almost exactly that cap, so it's a ceiling, not a floor. A NAV venue does tens of transactions a day. It's a **gas rebate, roughly $10,000 a year at design-partner scale** — it covers traffic, it isn't a business model. The auction leg earns more than the NAV leg, because under CIP-0104 rewards track traffic burned and a settlement transaction is heavier than an attestation."

### Three things NOT to say

- ❌ **Any dollar figure for tokenized-fund AUM on Canton.** Unfalsifiable and a judge from Circle or DA will know it (`[UNVERIFIED-4]`).
- ❌ **"$6 trillion / $9 trillion of assets on Canton."** That is repo and collateral flow. Repo does not need a NAV. Citing it invites the correct accusation that you are counting the wrong thing.
- ❌ **Any fund-administration TAM from a market-research vendor.** Three vendors give $8.6B, $13.6B and $100.2B for roughly the same category (`[UNVERIFIED-2]`).

---

## 9. `[UNVERIFIED]` register — 11 items

| # | Item | Why unverified |
|---|---|---|
| 1 | BNY / State Street / Northern Trust / Citco / SS&C / Apex rate cards | No published rates. Fee schedules **redacted even in SEC filings** (Baird/USBFS 2026-04-29; Kinetics/U.S. Bank 2023-04-28). |
| 2 | Global fund-administration market size | Vendor estimates span $8.6B / $13.6B / $100.2B — 12x spread, no methodology. Barrington Partners' survey data is participant-only. |
| 3 | ICE Data Services / Bloomberg BVAL / LSEG evaluated-pricing fees | Negotiated enterprise contracts, no public rates. |
| 4 | **Tokenized-fund AUM native to Canton** | No public tracker covers Canton. rwa.xyz omits Canton from USYC's network list despite a documented Canton launch. **Highest-risk item — do not state a number.** |
| 5 | Whether any tokenized fund publishes a multi-party attested NAV today | Searched; none found. Absence of evidence only. |
| 6 | Volume of tokenized fund shares actually posted as collateral | Capability asserted by Circle, Franklin, BNY/GS; no figure disclosed by anyone. |
| 7 | Chainlink and RedStone commercial pricing | Neither discloses rates. |
| 8 | Seed-stage enterprise-infra ACV benchmarks for fintech | No credible source; all results were content marketing. Use the Pyth Pro rate card instead. |
| 9 | Actual Canton Coin app rewards earned by any named app | Pool size and $1.50 cap documented; realised per-app earnings never published. |
| 10 | Current S&P DJI / FTSE Russell rate cards | Only the SPY/DIA/QQQ disclosures via An/Benetton/Song (Dec 2020 vintage) exist publicly. |
| 11 | Total Canton network transactions/month, current | Canton's own worked example uses 40M/month ("~15 TPS average") but an adjacent post says "~5 TPS average, Sept '25". The two are inconsistent; the app-reward arithmetic in §4.2 uses 40M as published. |

**One regulatory point that came back clean and is worth knowing:** publishing a fund NAV does **not** make you an EU benchmark administrator. Per ESMA's Benchmarks Regulation Q&A, "**the NAVs of investment funds do not qualify as indices or benchmarks within the meaning of BMR, but can be used as input data of benchmarks**" (via Elvinger Hoss, *Benchmark Regulation: Update of ESMA's Q&A*). A fund contributing its NAV into an index may be subject to BMR **contributor** requirements — a materially lighter designation than administrator.

Note the double edge: **no licensing barrier to entry for you, but also none for anyone else** — and it weakens the "price me like an index administrator" argument, since the regulator explicitly declines to treat a NAV as a benchmark. **A closing-auction official price used to value instruments is a different question and may sit closer to BMR scope; that one is worth a lawyer's hour before you sell it.**

---

## Sources

**Primary — SEC EDGAR**
- BBH Trust, Form N-CSR FY2023-10-31, filed 2026-01-05 (0001213900-24-001494) — fund accounting fee 0.325 bps
- Aetos Long/Short Strategies Fund LLC, Form 486BPOS Ex-99(k)(2), 2024-05-31 (0001104659-24-067205) — HedgeServ tiered administration fee
- Guinness Atkinson Funds / SmartETFs, Form N-CSR, 2025-03-10 (0001398344-25-005256) — small-fund cost stack
- Baird Funds / U.S. Bancorp Fund Services, Form 485BPOS Ex-99(h)(iii)(g), 2026-04-29 (0000894189-26-013396) — redacted fee schedule
- Kinetics Mutual Funds / U.S. Bank, Form 485BPOS, 2023-04-28 (0000894189-23-003327) — redacted fee schedule
- Morgan Stanley Institutional Liquidity Funds, Form 485BPOS, 2026-02-03 (0001133228-26-001288) — MMF expense structure
- MSCI Inc., Form 10-K FY2025, filed 2026-02-06 (0001408198-26-000011) — Index segment revenue and ETF AUM
- SEC Administrative Proceeding IA-4554, Calvert Investment Management (2016) — NAV error remediation

**Academic**
- An, Yu; Benetton, Matteo; Song, Yang. *Index Providers: Whales Behind the Scenes of ETFs.* Journal of Financial Economics (working paper 2022-01-12)

**Market data**
- rwa.xyz — Tokenized U.S. Treasury Funds, data date 2026-08-04; USYC asset page 2026-08-04
- CoinGecko API — Canton Coin (CC) price $0.116395, retrieved 2026-08-03

**Canton Network**
- *Cantonomics for App Builders* — 62% / 516M CC
- *Earn with every transaction* — $1.50 cap, $1.00 traffic, $0.20 rebate, 40M tx/month
- *State of RWA Tokenization 2026*, published 2025-12-18 — $36B RWA ex-stablecoins
- Canton Network docs, *Canton Coin Tokenomics* / *Featured App Activity Markers* / *Traffic-Based App Rewards*
- CIP-0104 (Traffic-Based App Rewards), approved 2026-02-12; CIP-0078; CIP-0042
- Press releases: Hashnote/USYC on Canton; Franklin Templeton Benji on Canton (2025-11-12); BNY + Goldman Sachs tokenized MMF (2025-07-23)

**Industry**
- pyth.network, *Introducing Pyth Pro*, 2025-09-24 — $5K/$10K per month tiers, $50B market data industry
- docs.chain.link — SmartData / NAVLink; Sygnum + Fidelity International FILQ
- blog.redstone.finance, 2026-03-24 and 2026-06-25 — Canton oracle deployment and fund pipeline
- Interstice Digital, *Who Is Building on Canton* (2026) — ecosystem census
- Elvinger Hoss, *Benchmark Regulation: Update of ESMA's Q&A* — fund NAVs are not BMR benchmarks
- Barrington Partners, Fund Accounting and Administration Cost Survey (2023 edition: 15 fund companies, $3.6T AUM)
