# Closing-Auction Imbalance Publication: The Empirical and Historical Record

**Purpose.** Settle, on evidence, whether disclosing a net closing imbalance **selectively to one
designated and obligated liquidity provider** is defensible against the two alternatives —
**broadcast to everyone**, and **publish nothing**.

**Compiled:** 2026-08-03. **Audience:** a judge who will push back, and a former MOC trader who
will notice if a claim is dressed up.

**Rules used in compiling this.** Every factual claim carries a source and a date. Where venues'
own documents disagree with each other, the disagreement is shown rather than resolved by
preference. Where the literature conflicts, the conflict is the finding. Anything not verified to a
primary or peer-reviewed source is tagged `[UNVERIFIED]` and collected in §7.

---

## 1. Who actually publishes, and what exactly

### 1.1 NYSE — closing auction

NYSE publishes closing imbalance information, but only in the **last ten minutes**, and only on a
**paid proprietary feed**.

Per NYSE's own *Opening and Closing Auctions* fact sheet (© 2024, document 6642):

| Item | NYSE |
|---|---|
| MOC / LOC entry, modify, cancel cutoff | **3:50 p.m. ET** |
| After 3:50 p.m. | New MOC/LOC accepted **only on the contra side of a published Significant Imbalance**, until 4:00 p.m. Existing MOC/LOC **cannot be modified or cancelled** |
| Imbalance dissemination begins | **3:50 p.m. ET** |
| Frequency | **every 1 second, if changed from the previous publication**, until the stock closes |
| Fields | **Paired quantity, Unpaired quantity, Total imbalance quantity, Closing Only Interest Price, Continuous Book Clearing Price** |
| Opening auction, for contrast | dissemination begins **8:00 a.m. ET**, every 1 second if changed — i.e. 90 minutes of pre-open transparency vs 10 minutes pre-close |

Source: [NYSE Opening and Closing Auctions Fact Sheet (2024)](https://www.nyse.com/publicdocs/nyse/markets/nyse/NYSE_Opening_and_Closing_Auctions_Fact_Sheet.pdf); an
earlier NYSE *Closing Process* fact sheet (© 2021) lists the same fields and adds the 3:58 p.m. hard
stop on cancellations and the 3:55 p.m. inclusion of Closing D Orders in the published imbalance:
[NYSE Auctions Closing Process Fact Sheet](https://www.nyse.com/publicdocs/nyse/NYSE_Auctions_Closing_Process_Fact_Sheet.pdf).

Two structural points a judge should be told, because they change the meaning of "NYSE publishes
the imbalance":

1. **The published imbalance is not the true residual, because the DMM's own auction liquidity is
   not in it.** "DMM Auction Liquidity" is a distinct rulebook category (Rule 7.35(a)(9)(A)):
   non-displayed interest entered by the DMM manually on the Floor or via the DMM unit's electronic
   message to conduct the auction, designated for the auction only, and *not* an order under Rule
   7.31. Under Rule 7.35B it may be entered **after the end of Core Trading Hours** specifically "to
   offset any Unpaired Quantity at the Closing Auction Price" — i.e. after imbalance dissemination
   has stopped. So the market's last published imbalance is systematically an *overstatement* of
   what will actually go unfilled, by an amount only the DMM knows.
   **Be precise about the boundary:** ordinary *DMM Orders* entered in advance of the Closing
   Auction **are** included in Closing Auction Imbalance Information — NYSE says so expressly
   ([Rel. 34-99719](https://www.sec.gov/files/rules/sro/nyse/2024/34-99719.pdf), n.4). The exclusion
   is of the DMM's auction-only backstop liquidity, not of the DMM's ordinary orders. Do not
   overclaim this on stage.
2. The `Continuous Book Clearing Price` disseminated to everyone is **computed from per-price-point
   order data that is not itself disseminated** — but which *is* shown to the DMM. This is the real,
   uncontested informational asymmetry. See §4.

**Significant Imbalance (current) replaced Regulatory Imbalance (legacy).** On **28 October 2024**
NYSE replaced its static round-lot Regulatory Imbalance flag with a dynamic **Significant
Imbalance** flag in the 3:50 p.m. imbalance feed. It fires when *both*: (a) the closing imbalance is
≥30% of 20-day average closing size for S&P 500 names, ≥50% for S&P 400/600, ≥70% for all others;
and (b) notional (imbalance × reference price) ≥ $200,000. Source: Castaneda-Dawkins & Bazinas,
[*The NYSE Significant Imbalance*](https://www.nyse.com/data-insights/the-nyse-significant-imbalance-enhanced-trading-opportunities-at-the-nyse-closing-auction),
NYSE Data Insights, 4 November 2024. The flag matters mechanically, not just informationally: it is
the flag that *unlocks* contra-side MOC/LOC entry for the final ten minutes.

### 1.2 Nasdaq — Closing Cross

Nasdaq runs a **two-stage** disclosure, and Nasdaq's own published documents are inconsistent about
it, so here is the reconciliation.

| Stage | Time | Frequency | Contents |
|---|---|---|---|
| **EOII** (Early Order Imbalance Indicator) | from **3:50 p.m. ET** | every **10 seconds** | Current Reference Price, Paired Shares, Imbalance Shares, Imbalance Side — **but not** the Near or Far Indicative Clearing Prices |
| **NOII** (Net Order Imbalance Indicator) | from **3:55 p.m. ET** to the close | every **1 second** | all of the above **plus** Near Indicative Clearing Price and Far Indicative Clearing Price |

Field definitions, verbatim from Nasdaq's FAQ:

- **Current Reference Price** — "A price within the Nasdaq Inside at which paired shares are
  maximized, the imbalance is minimized and the distance from the bid-ask midpoint is minimized, in
  that order."
- **Near Indicative Clearing Price** — "The crossing price at which orders in the Nasdaq
  opening / closing book **and continuous book** would clear against each other."
- **Far Indicative Clearing Price** — "The crossing price at which orders in the Nasdaq
  opening / closing book would clear against each other." (auction book only)
- **Number of Paired Shares** — on-close shares Nasdaq can pair at the current reference price.
- **Imbalance Shares** — "The number of opening or closing shares that would remain unexecuted at
  the current reference price."
- **Imbalance Side** — "B = buy-side imbalance; S = sell-side imbalance; N = no imbalance; O = no
  marketable on-open or on-close orders."

Order cutoffs: **MOC must be received before 3:55 p.m.**; **LOC after 3:55 p.m. is accepted but
re-priced to the 3:55 p.m. Reference Price if more aggressive, and is not accepted after 3:58 p.m.**
Sources: [Nasdaq *The Nasdaq Opening and Closing Crosses — FAQ*](https://www.nasdaq.com/docs/2020/04/03/openclose_faqs.pdf)
(2018 edition, which documents only the 3:55 p.m. NOII and predates or omits the EOII); Nasdaq
Equity 4, Rule 4754(a)(7) and (a)(10), which define NOII and EOII respectively and are described in
[SR-NASDAQ-2024-065 / Rel. 34-101620](https://www.sec.gov/files/rules/sro/nasdaq/2024/34-101620.pdf):
"the Exchange disseminates an early order imbalance indicator ('EOII') every 10 seconds, beginning
at 3:50 p.m. until the order imbalance indicator ('NOII') begins to disseminate. The Exchange
disseminates the NOII every second, beginning at 3:55 p.m. until market close."

**Documented inconsistency, flagged rather than hidden:** the Nasdaq trader-facing FAQ PDF describes
dissemination as beginning at 3:55 p.m. and says nothing about an EOII; the nasdaqtrader.com
Open/Close landing page says "Closing Cross Net Order Imbalance information between 3:50 and 4:00
p.m."; the rulebook is the two-stage version in the table above. **Treat the rulebook as
authoritative and the FAQ as stale.**

**Nasdaq guarantees nothing to the MOC sender and says so.** FAQ item 14: "Does Nasdaq guarantee
market-on-close (MOC) orders in the Closing Cross? **No**, the Opening and Closing Crosses provide
unparalleled transparency which encourages market participants to provide necessary liquidity to
offset any MOC imbalance." That single sentence is the entire official theory of the case:
**transparency in place of an obligation.** It is an assertion, not a finding. §3 is about whether
it holds.

### 1.3 Toronto (TSX) — the cleanest modern natural experiment

TSX modernised its MOC in **October 2021** and published a detailed before/after guide, which makes
it the most legible disclosure-regime change on record.

| | Before (pre-Oct 2021) | After |
|---|---|---|
| Imbalance messages | **one** message, at **3:40 p.m.** | every **10 seconds** from **3:50 p.m.** through the close |
| Fields | **4**: Symbol, Reference Price, Imbalance Volume, Imbalance Side | **10**: + Paired Volume, Market Order Imbalance Volume, Market Order Imbalance Side, Near Indicative Closing Price, Far Indicative Closing Price, Price Variation |
| Freeze | none | **randomised start between 3:56 and 3:57 p.m.**, simultaneous across all symbols |

TSX's stated reasons: "Traders need more information"; "TSX MOC is a global outlier … limits
participation from international investors"; and consistency of execution. Source:
[TSX MOC Modernization — Detailed Guide, September 2021](https://www.tsx.com/en/resource/2357).

**The most valuable sentence in that document, for this argument, is TSX admitting the cost of its
own transparency:**

> "With the increased frequency of imbalance messages comes the potential for participants to hold
> volume to the last moment before the freeze period in order to garner the most information before
> committing volume. In an effort to provide participants time to react to late commitments of
> volume without restriction, TSX will impose a randomized start to the freeze period."

That is a venue conceding, in a primary document, that **broadcast transparency creates a strategic
waiting game in which liquidity providers withhold in order to extract information**, and that the
fix required is randomisation. It is the strongest single primary-source support for the claim that
broadcast is not costless.

Note also the lock-in: during the TSX imbalance period **MOC orders cannot be cancelled or
modified**, and LOC orders cannot be cancelled — only re-priced *more aggressively*, explicitly
"while preserving the significance of the imbalance messages."

### 1.4 Europe — indicative price throughout the call phase

**Confirmed, and the difference is real.** All three major European venues publish, continuously
through the call phase, the **indicative price, the indicative matched volume, AND the imbalance
quantity and its side** — to every paying subscriber simultaneously. That is *more* than the US
publishes and, critically, **more than European law requires.**

#### The regulatory floor — imbalance disclosure in Europe is voluntary

**Commission Delegated Regulation (EU) 2017/587 (RTS 1)**, 14 July 2016, OJ L 87, 31.3.2017,
pp. 387–410, Annex I, Table 1, row "Periodic auction trading system," column "Information to be made
public" — verbatim:

> "The price at which the auction trading system would best satisfy its trading algorithm in respect
> of shares, depositary receipts, ETFs, certificates and other similar financial instruments traded
> on the trading system and **the volume that would potentially be executable at that price** by
> participants in that system."

**The mandate is indicative price plus executable volume. Imbalance is not required.** Every European
venue below publishes side and surplus **voluntarily, above the regulatory minimum**. That is a
load-bearing fact for this argument: the EU legislator, writing a pre-trade transparency regime from
scratch, did **not** conclude that imbalance must be broadcast.
([EUR-Lex, consolidated](https://eur-lex.europa.eu/eli/reg_del/2017/587/oj/eng);
[Commission RTS 1 Annex](https://ec.europa.eu/finance/securities/docs/isd/mifid/rts/160714-rts-1-annex_en.pdf))

**MiFIR Article 13(2)**, as amended by Regulation (EU) 2024/791 (consolidated 02014R0600-20251123):
venues "shall make available to the public the information referred to in paragraph 1 **free of
charge 15 minutes after publication** in a format that is machine-readable and usable for all users,
including retail investors." **In Europe the asymmetry is temporal and priced, not structural** —
but in an auction whose outcome is decided in the final 30 seconds, a 15-minute delay is
functionally no access at all.

⚠️ Do **not** cite ESMA's *Guidelines on the MiFID II/MiFIR obligations on market data*
(ESMA70-156-4263, 2021) — **withdrawn**, superseded by Commission Delegated Regulation (EU)
2025/1156 of 12 June 2025.

#### Xetra (Deutsche Börse)

Source: **T7 Release 14.0, *Market Model for the Trading Venue Xetra*, v1, 27 October 2025**, §8.1.4
— verbatim:

> "The order book is partially closed during the call phase … During the call phase of the auction,
> additional market imbalance information is disseminated. In case of an uncrossed order book, the
> accumulated volumes at the best bid and best ask are displayed in addition to the best bid and ask
> limits. **In case of a crossed order book the executable volume for the indicative auction price,
> the side of the surplus and the volume of the surplus are displayed.**"

§8.1.1: "The call phase has a **random end** after a minimum period **in order to avoid price
manipulation**."

| Checklist item | Verdict |
|---|---|
| Indicative auction price published continuously | **Confirmed** — "Information on the current order situation is provided continuously during the call phase." Technically **event-driven**, not a fixed tick |
| Surplus/imbalance volume | **Confirmed** |
| Imbalance **side** | **Confirmed** on the Xetra CLOB |
| When orders don't cross | Best bid/ask limits + accumulated volumes at those limits ⚠️ (conflict below) |
| Random end | **Confirmed** — but **duration is not published anywhere by Deutsche Börse** |

**Clock:** Closing Auction call **17:30**, earliest closing price **17:35**, plus an undisclosed
random tail. The Auction Schedule footnote: "Auctions have a random end. Times stated are the
**earliest** times of the end of an auction." The Trading Parameters file confirms
`MktImbInd Market Imbalance Indicator — Yes for all instruments`.

**Wire fields (T7 R14.0 Market and Reference Data Interfaces Manual, v1, 29 July 2025):** indicative
price is `MDEntryType (269) = Q` (Auction Clearing Price); surplus is `MDEntryType = 0/1` (Bid/Offer)
with `MDPriceLevel` unset and `QuoteCondition (276) = Z` (Order imbalance), quantity in
`MDEntrySize (271)`. The instrument-level switch is `MarketImbalanceIndicator (28875)`.

⚠️ **Documented conflict, both sources Deutsche Börse primary:** for the *uncrossed* case the T7 R14.0
Market Model says "the accumulated volumes at the **best bid and best ask**"; the Xetra website page
*Continuous Trading with Auctions* says "only the accumulated volumes at (at least) **ten price
levels** are displayed." Likely explained by the per-instrument `ClosedBookIndicator` setting.
Reported, not resolved.

⚠️ **Do not quote a random-end duration for Xetra.** Deutsche Börse deliberately does not publish it.
The commonly repeated "30 seconds" is `[UNVERIFIED]` for Xetra specifically (it is documented for
Euronext, LSE and Borsa Italiana).

🔴 **MATERIAL AND RECENT — Xetra opened the auction book on 1 June 2026.** Deutsche Börse **Xetra
Circular 021/2026**, T7 Release 14.1, feature name "**Auction Transparency**", **effective 1 June
2026**: during auctions Xetra now publishes **the full order book depth via EOBI** and **the first
ten levels via GUI / EMDI / MDI**, and **iceberg orders display their full tradeable quantity.**

This matters for the pitch in three ways, and the third is uncomfortable:

1. The "partially closed order book" description above (T7 R14.0, Oct 2025) is **the immediately
   prior regime**. Cite R14.0 for what Xetra *did*; cite Circular 021/2026 for what it *does now*.
2. It **reverses the exact regime** that van Bommel & Hoffmann (LSF WP 11-9, 2011) found inferior —
   they compared Euronext (five book levels disclosed, fixed end) against Xetra (indicative price and
   volume only, random end) across 126 matched stocks and found **Euronext auctions more liquid,
   contributing more to price discovery, followed by lower spreads**. ⚠️ Working paper, and
   transparency was confounded with end-time randomisation.
3. **It cuts against the "direction of travel favours less disclosure" framing.** Europe's largest
   auction venue has just moved decisively toward *more*. Do not claim a one-way trend. The honest
   statement is that venues are moving in **both** directions at once — Xetra opening the book while
   Euronext launches AVD, which publishes nothing, **in the same T7 release cycle**. What is
   converging is not a level of transparency but a **separation of concerns**: publish the price
   formation, shield the residual.

🔴 This is also a live, certainly unstudied natural experiment with a precise date. Nobody has
measured it yet.

#### Euronext

Source: **Optiq MDG Messages Interface Specification, Euronext Cash and Derivatives, v5.354.0,
5 March 2025**, p.120. Everything travels in **Price Update (1003)** with
**Market Data Price Type = 14 (Indicative Matching Price)**:

> "◼ The **Indicative Matching Price (IMP)** … ◼ The **Indicative Matching Volume (IMV)** … ◼ The
> **indicative imbalance volume**: remaining unmatched quantity at the IMP ◼ The **indicative
> imbalance volume side** … The real-time messages are sent **if at least one of the instrument's
> theoretical opening conditions changes**."

Field-level (p.195):

| Field | Type | Values |
|---|---|---|
| `Imbalance Quantity` | uint64 | "Imbalance volume quantity if Uncrossing occurs at this moment. **This volume includes hidden quantity.**" |
| `Imbalance Quantity Side` | uint8 | **0 = No imbalance, 1 = Buy, 2 = Sell** |

Note the sharpness of that: **Euronext's published imbalance includes iceberg/hidden quantity.**

**Random uncrossing — exactly 30 seconds**, from the *Appendix to Euronext Instructions 4-01/4-03
Trading Manuals*: "**17:35 Random: The closing uncrossing will randomly occur between CET 17:35:00
and 17:35:30**." Optiq exposes `Instrument State = 15 (Random Uncrossing Period)` and a
`Phase Qualifier` bit for it.

| Venue | Continuous ends | Pre-closing call | Closing uncrossing | Trading-At-Last |
|---|---|---|---|---|
| Amsterdam / Brussels / Paris / Lisbon | 17:30 | 17:30–17:35 | random **17:35:00–17:35:30** | 17:35–17:40 |
| Dublin | 17:28 | 17:28–17:30 | random **17:30:00–17:30:30** | 17:30–17:40 |
| Oslo | 16:20 | 16:20–16:25 | random **16:25:00–16:25:30** | 16:25–16:30 |

Borsa Italiana / Euronext Milan matches: 17:30:00–17:35:00 "plus a variable interval of up to 30
seconds, determined automatically on a random basis by the trading system."

#### London Stock Exchange (SETS)

Source: **MIT201 — Guide to the Trading System, Issue 15.8, effective 19 January 2026**, §7.2, p.50
— the single cleanest statement of the European model anywhere:

> "Orders may be entered, modified and cancelled during an auction call, (along with any extensions
> and random periods) but no automated execution occurs. **Throughout the entire period London Stock
> Exchange disseminates the most up-to-date indicative auction price and uncrossing volume, along
> with the auction imbalance direction and quantity. This will be updated whenever orders are added,
> deleted, modified and result in a new auction price/volume.** Please note the auction imbalance
> direction and quantity is not disseminated during SETSqx intraday auction calls."

Wire fields (*GTP 003 Statistics Guide*, Issue 13.0, 4 April 2025, §3.11): **Indicative Auction
Price, Paired Quantity, Auction Type, Imbalance Direction, Imbalance Size**. `Imbalance Direction`
enumerates **B / N / O / S** (Buy, No imbalance, Insufficient orders, Sell).

**Extensions and random end** (MIT201 §7.2.1–7.2.3): a **Market Order extension** when market orders
would remain unexecuted; a **Price Monitoring Extension** when the indicative price is beyond a
configured tolerance from the dynamic reference price; and — verbatim — "**To avoid participants
knowing the exact time of uncrossing, a configured random period precedes invocation of each
extension and the final uncrossing.**" Closing auction call **16:30–16:35**, uncrossing at a random
point in **16:35:00–16:35:30**.

⚠️ Durations (PME 5 min, MOE 2 min, 2/5/10/20/50% tolerance ladder, 4–5 PMEs) come from an
**ETP-scoped** LSE factsheet, and MIT201 (dynamic reference price) conflicts with that factsheet
(previous close) on the PME trigger. The authoritative *Millennium Exchange Business Parameters*
spreadsheet is behind the LSE Member Portal and was not retrievable. Treat the percentages as
illustrative.

#### Free or paid — Europe

**Every venue puts real-time auction indicative data behind a commercial licence. None is free in
real time.**

| Venue | Product carrying auction indicative data | Real-time cost |
|---|---|---|
| Xetra | Xetra Core / Xetra–Pre-Trade (MiFIR products); Ultra / Order by Order (premium) | **€3,642 / €2,230 / €4,553 / €4,955 per month** (distribution licence, ex-VAT, Price List v16_1, valid 1 Aug 2026) |
| Euronext | **Level 1** — definition explicitly names "the indicative matching price and volume" | ≈ **€4,966/mo** Continental Cash L2 `[UNVERIFIED figure; definitions are solid]` |
| LSE | GTP **Level 1** — definition explicitly names "indicative uncrossing volume" | **£37,083 (L1) / £67,611 (L2) per year**, UK market data, professional redistribution |

Note Xetra's structure: **Order by Order costs the same real-time as delayed (€4,955)** — no delayed
relief on the premium tier.

⚠️ **Conflict at LSE worth naming:** Schedule A §4.2 describes UK market data content as including
"indicative uncrossing volume … uncrossing price and volume" and does **not** itemise imbalance —
only *Private Securities Market Data* is described as including "size and direction of order
imbalance." That contradicts MIT201 §7.2 and GTP 003 §3.11, which both plainly state imbalance
direction and quantity are disseminated for ordinary order-book auctions. Probably drafting
looseness in the commercial schedule; do not build a claim on it.

**Scale.** Euronext's quantitative research team reports **closing auctions represent over 25% of
multilateral addressable volume in Europe** as of early 2026 — roughly 2.5× the US share.
(Besson & Quily, [*Better market impact at the close with residual imbalance*](https://www.euronext.com/en/news/better-market-impact-close-residual-imbalance),
Euronext, 2 April 2026.)

**Note the direction of the European design conversation.** Euronext's 2026 research frames the
residual imbalance not as something to publish but as "untapped liquidity" that participants should
be able to "interact with and manage more effectively" — via AVD, which publishes nothing.
Continuous indicative-price transparency during the call and non-disclosure of the residual are
being run *simultaneously*, by the same venue. Those are separable design choices, and Europe is
currently separating them.

### 1.4.1 Hong Kong — the closing auction that was abolished and rebuilt

The strongest cautionary natural experiment on closing-auction design, verified against HKEX primary
documents.

| Date | Event |
|---|---|
| **26 May 2008** | HKEx introduces the original Closing Auction Session (CAS) |
| Nov 2008 – Feb 2009 | Consultation on adding a price limit **or** suspending the CAS |
| **February 2009** | Conclusions: **keep** the CAS, add a **2% price limit**, target Q2 2009 |
| **Monday 9 March 2009** | The HSBC close |
| **12 March 2009** | Suspension announced |
| **20 March 2009** | Suspension confirmed (last CAS = Fri 20 Mar) |
| **Monday 23 March 2009** | Suspension effective |
| **25 July 2016** | **CAS Phase 1** — HSCI Large/MidCap, H-shares with A-share pairs, all ETFs |
| **24 July 2017** | **Phase 2** — + SmallCap; short selling enabled |
| 8 Oct 2019 / 19 Oct 2020 | All equities and funds / Leveraged & Inverse Products |

**The trigger, in HKEX's own numbers.** HKEx published a same-day release,
*Trading in HSBC shares during Today's Closing Auction Session* (9 March 2009): previous close
**HK$43.50** → end of continuous trading **HK$37.70**, "a drop of **13.3 per cent**"; CAS closing
price **HK$33.00**, "representing a drop of another **12.5 per cent**"; total **24.1 per cent** from
the prior close. HSBC's CAS turnover was HK$391.54m — **8.3%** of the day's turnover in the stock.

**Two things about this story are more useful than the story itself:**

1. **The 9 March close reversed a published decision.** One month earlier HKEx had decided to *keep*
   the CAS, writing that a percentage price limit "**is in our view preferable to suspending the
   CAS** given the extensive use of the CAS by market participants and that a closing auction is a
   feature of nearly all mature equity markets globally," and backtesting that a 2% limit would have
   left "on average **95 per cent of the CAS turnover unaffected**." A single print overturned it.
2. **HKEx never attributed the abolition to manipulation, and no regulator ever found any.** The
   12 March 2009 release cites "concerns about **any appearance of abuse** during the CAS and the
   need to maintain public confidence in the orderliness, fairness and transparency of the market in
   light of **recent price volatility**." The SFC did investigate whether the close resulted from
   "any manipulation contrary to the Securities and Futures Ordinance" — **no published finding or
   enforcement action was located.** `[UNVERIFIED — "the SFC investigated" is verified; "the SFC
   found manipulation" is not supported. Do not say it.]`

**Nor was HSBC the sole cause.** The February 2009 Conclusions record "**a number of instances** of
volatile price movements of certain stocks at the close," that HKEx "has notified the SFC of some of
these cases," and name an earlier episode: "as evident from previous events (eg on major index
rebalancing dates such as **30 May 2008**), aggressively priced orders in the first one to two
minutes of the CAS **and order imbalances of particular securities** were the key reasons."

**And the constituency that killed it did not trade it.** Of 102 consultation respondents, 62 wanted
outright suspension — but 46 of those were individual investors and 13 were retail Exchange
Participants representing **4.7% of market share**. Institutional EPs were **13–0 in favour of
keeping** the CAS and represented **43.8% of market share**.

#### The 2016 redesign — the fix was price bounds and randomised timing, not disclosure

Source: HKEX, *Trading Mechanism of Closing Auction Session (CAS) in the Securities Market*.

| Period | Full day | Duration | Input | Amend | Cancel |
|---|---|---|---|---|---|
| Reference Price Fixing | 16:00–16:01 | 1 min | **No** — all order messaging rejected | No | No |
| Order Input | 16:01–16:06 | 5 min | Yes (**±5%**) | Yes | Yes |
| **No-Cancellation** | 16:06–16:08 | 2 min | Yes (Stage 2 band) | **No** | **No** |
| **Random Closing** | 16:08–16:10 | 2 min | Yes (Stage 2 band) | **No** | **No** |

- **Reference price** = median of **five nominal-price snapshots at 15-second intervals** starting
  15:59:00.
- **Stage 1 limit (16:01–16:06): ±5% from the Reference Price.** HKEX notes this is "**the narrowest
  among those with a price limit**" internationally.
- **Stage 2 limit (16:06 → random close): not a percentage.** New orders must be priced **within the
  lowest ask and highest bid of the order book as recorded at 16:06** — i.e. **inside the spread that
  existed before cancellation was frozen**. Fallback to ±5% if only one side exists or the touch is
  already outside ±5%; never looser than ±5%.
- **Random close within a 2-minute window** (16:08–16:10) — four times wider than Europe's 30 seconds.
- **Only at-auction and at-auction limit orders** are accepted; all other order types are rejected.
  Unpriced at-auction orders have **higher matching priority**.
- **Short selling** was banned outright in Phase 1; from Phase 2 permitted only as at-auction limit
  orders subject to a **tick rule** (price not below the Reference Price).

#### What the CAS publishes

**Published:** Indicative Equilibrium Price (**IEP**), Indicative Equilibrium Volume (**IEV**),
**order imbalance quantity and side**, Reference Price with Stage 1 and Stage 2 upper/lower limits,
best **10** bids and offers, broker queue, tickers, nominal price. **Not published:** individual
at-auction orders.

Wire-level (*HKEX OMD-C Binary Interface Specifications v1.23*): **MsgType 41** carries IEP/IEV;
**MsgType 56** is Order Imbalance with `OrderImbalanceDirection` (**N** = buy = sell, **B** = buy
surplus, **S** = sell surplus) and `OrderImbalanceQuantity` ("the absolute difference between the
matchable buy quantity and the sell quantity at IEP"); **MsgType 43** carries `ReferencePrice`,
`LowerPrice`, `UpperPrice` — the whole price band in one message. Carried on **OMD-C** Standard /
Premium / FullTick, all **paid**; delayed/basic data is free on the HKEX website.

⚠️ **Refute the "every 2 seconds" claim if anyone makes it.** Dissemination is **event-driven** —
the IEP message is generated "whenever there is change of the IEP or IEV," imbalance likewise on
change. The 2-second figure belongs to the **heartbeat** and the Market Turnover message. Say "on
every change."

**A detail that cuts toward disclosure, not against it:** HKEX **withholds order imbalance during the
pre-opening session but publishes it in the close.** A venue that had been burned once chose *more*
transparency for the close specifically.

#### Did the redesign work?

- **Chan, K. & Yao, C. (CUHK), *The Effect of a Closing Auction on Market Quality and Market
  Efficiency in the Stock Exchange of Hong Kong*, HKIMR / Hong Kong Academy of Finance, January
  2021.** The CAS grew to **more than 8% of trading volume by June 2018** (from ~5% of full-day
  turnover pre-2009). Volume migrated "mostly from the end of the continuous trading session, with
  the **last 15-minute interval (15:45–16:00)** contributing the largest volume migration"; the rest
  of the session was barely affected. Continuous-session liquidity showed "a **slight decline** …
  wider bid-ask spread and lower depth" in the last 15 minutes — **but price impact in that window
  "decreases significantly."** On efficiency: "There is a general pattern of return reversal, but the
  **magnitude of the reversal is smaller after the introduction of CAS. This indicates that price is
  more efficient after the implementation of CAS.**"
  ([HKIMR/AoF](https://www.aof.org.hk/docs/default-source/hkimr/applied-research-papers/yao_summary.pdf))
- **Lei, A. C. H., Ma, X. & Yick, M. H. Y. (2020), "Callable bull/bear contracts, call auction
  sessions, and price manipulations: Evidence from Hong Kong," *Journal of Futures Markets* 40(11),
  1731–1750.** Elegant identification: CBBCs knock out via a Mandatory Call Event when the underlying
  touches a call price, giving issuers a direct incentive to push the close — so **MCE probability
  proxies for closing-price manipulation**. Using the 2009 suspension and 2016 reintroduction as
  regime shifts, they find the **2016 enhanced CAS is more effective at reducing price manipulation**,
  with a spillover reduction in the pre-opening session. `[UNVERIFIED — paywalled; abstract-level
  findings only, no coefficients or sample windows read.]`
- No standalone HKEX post-implementation review was located, despite a 2015 commitment to one.
  `[UNVERIFIED — not found.]`

**The lesson, stated narrowly.** What failed in Hong Kong in 2009 was not disclosure — the old CAS
published imbalance too. What failed was **unbounded price discretion in a thin, concentrated
window**, with cancellation permitted right up to a known close. What fixed it was **price bounds
plus a frozen, randomly-timed endgame** — while *keeping* full imbalance disclosure. That maps
directly onto the constraint that matters here: whoever holds the informational advantage must be
fenced by a price bound and a randomised, non-cancellable endgame (cf. NYSE Rule 7.35B(g)(2), §4.3).

**A 2025 European counter-example that matters more than the general rule.** On **8 December 2025**
Euronext launched **Auction Volume Discovery (AVD)** — an order type whose whole purpose is to
interact with the *unmatched* auction imbalance, and whose defining property is:

> "AVD orders and their imbalance are **not published**, preventing any information leakage."

AVD orders sit non-displayed, match first against the auction imbalance at the uncrossing, then
against each other, and expire unexecuted at the auction's conclusion; they trade at the official
auction price **without contributing to its determination**. Euronext's stated motivation: in 2024
its average daily auction imbalance exceeded **€200 million** of unmatched liquidity, about **6% of
auction volume**. Sources:
[Euronext, Auction Volume Discovery (AVD)](https://www.euronext.com/en/trading/trading-services/auction-volume-discovery-avd);
[Euronext press release, 8 December 2025](https://www.euronext.com/en/about/media/euronext-press-releases/euronext-unlocks-new-liquidity-opportunities-for-equities).

This is important and should be used: **a top-tier European primary exchange, in December 2025,
built a new liquidity mechanism around deliberately *not* publishing an imbalance, and said the
reason was information leakage.** The direction of travel is not uniformly toward broadcast.

### 1.5 Cboe Market Close — the US venue that publishes matches, never the imbalance

**Cboe Market Close (CMC)** is an SEC-approved, live, on-exchange MOC matching facility for
securities listed elsewhere. It is the closest US analogue to "do not broadcast the residual," and it
is worth more to this argument than any European example because it operates inside the same
regulatory regime as NYSE and Nasdaq.

How it works (Cboe *Market Close FAQ*, Q1 2025):

- Members submit **Market-On-Close orders only** — no limit orders. Cboe's stated principle:
  "**Limit orders are the basis from which price formation occurs. Market orders are recipients of
  that price formation, but do not contribute to the price level.**"
- Orders are matched buy-against-sell at scheduled **matching sessions** — currently **3:15, 3:30,
  3:49** (non-Nasdaq symbols cut off here) and **3:54 p.m. ET** (Nasdaq symbols) — and any match
  settles at the **official closing price** from the primary listing market.
- **"Any remaining shares would be cancelled back to Members for Members to route to the primary
  listing market auctions if so desired."** The unmatched residual is returned to the sender, not
  exposed.
- What is published: **"Total size of all buy and sell orders matched via CMC"** in each matching
  session, via the Cboe Auction feed and Multicast PITCH. **Matched size only. No imbalance, no
  side, no residual.** Cboe's framing: "the number of matched market order shares will be published
  in advance of the primary market's cutoff time for MOC orders. In this way, Cboe provides a
  transparent tally to reflect the added auction depth."

Regulatory history: initially approved January 2018, sent back after NYSE and Nasdaq objected,
**finally approved 21 January 2020** — the Commission finding CMC "should introduce and promote
competitive forces among national securities exchanges for the execution of [MOC] orders" without
disrupting price discovery or increasing manipulation risk — and **launched on BZX 6 March 2020**.
The MOC cut-off was later extended by SEC order in February 2023. Sources:
[Cboe Market Close FAQ](https://cdn.cboe.com/resources/membership/Cboe_Market_Close_FAQ.pdf);
[Cboe launch announcement, 30 Jan 2020](https://ir.cboe.com/news/news-details/2020/Cboe-Announces-Launch-Date-for-Cboe-Market-Close-01-30-2020/default.aspx);
[Rel. 34-88008](https://www.sec.gov/files/rules/sro/batsbzx/2020/34-88008.pdf);
[SR-CboeBZX-2022-064 approval, 15 Feb 2023](https://www.federalregister.gov/documents/2023/02/15/2023-03162/self-regulatory-organizations-cboe-bzx-exchange-inc-order-granting-approval-of-proposed-rule-change).

**The point to carry forward:** the SEC has already approved a design in which MOC interest is
matched at the closing price and the **residual imbalance is never disclosed to anyone** — it is
handed back to the sender. Non-publication of an imbalance is not a novel or exotic regulatory
posture. It is a live US market structure, and Nasdaq's 5.5 bps / 1.7 bps research (§3.2) was written
specifically to argue against it.

### 1.6 Free/public, or subscriber-only? — mostly subscriber-only, and the nuance is decisive

**In the United States, the rich continuous imbalance stream is paid; a single coarse flag is
public.** This distinction matters and is easy to get wrong — get it right on stage.

- **The detailed feed is proprietary.** The continuous, once-per-second Closing Auction Imbalance
  Information — paired quantity, unpaired quantity, total imbalance, Continuous Book Clearing
  Price, Imbalance Reference Price — is disseminated by NYSE **"through its proprietary data
  feed."** ([Rel. 34-99719](https://www.sec.gov/files/rules/sro/nyse/2024/34-99719.pdf), p.3)
  Depth-of-book and auction products generally are not carried by the CTA/UTP SIPs. (See
  [Databento, *Direct proprietary feeds vs SIPs*](https://databento.com/blog/proprietary-feeds-vs-sip-data);
  [Databento, *What is a SIP?*](https://databento.com/microstructure/sip).)
- **But the regulatory flag does reach the tape.** NYSE states that where the Closing Imbalance at
  the freeze time is 500 round lots or more, "the Exchange will disseminate a Regulatory Closing
  Imbalance **to both the securities information processor and proprietary data feeds**."
  ([Rel. 34-99719](https://www.sec.gov/files/rules/sro/nyse/2024/34-99719.pdf), p.3, citing Rule
  7.35B(d)(1)) So a coarse "there is a large imbalance, this side, this size" signal is genuinely
  public; the actionable detail — clearing prices, paired quantity, second-by-second evolution — is
  not.
- **Nasdaq NOII** is distributed via **Nasdaq TotalView-ITCH**, the Nasdaq DataStore, the Nasdaq
  Workstation, and market data distributors — i.e. **by subscription**. Nasdaq's own Open/Close page
  lists no free channel. ([Nasdaq Trader, Opening and Closing Crosses](https://nasdaqtrader.com/Trader.aspx?id=OpenClose))
- **NYSE Order Imbalances** is a paid proprietary product: **$500/month access fee**, non-display
  fees of **$2,000/month** (Categories 1 and 2), with the full Integrated Feed running to
  **~$7,500/month** for non-display use. ([NYSE Proprietary Market Data Pricing](https://www.nyse.com/publicdocs/nyse/data/NYSE_Market_Data_Pricing.pdf);
  [Databento, *Introducing real-time NYSE imbalance data*, 14 August 2025](https://databento.com/blog/NYSE-imbalance-feeds))
- **TSX** is the more open case: imbalance messages are carried "on the same market data feeds as
  they are today, which are **all broadcast, level 1 and level 2 real-time market data feeds**"
  (TSX MOC Modernization Guide, §2.3) — still commercial data, but level 1, so far more widely held.

**Consequence for the argument.** The honest characterisation of the US status quo is **tiered
disclosure**, not broadcast:

| Tier | Who | When | What |
|---|---|---|---|
| 1 | **NYSE DMM** (one firm per security) | continuously, incl. per-price-point book | aggregate order info at **each price point**, incl. full Reserve and MOC/LOC quantities (§4.1) |
| 2 | **NYSE Floor brokers** (registered class) | **2:00 p.m. – 3:50 p.m.** | Total Imbalance, Side, Paired Quantity, Unpaired Quantity, Side of Unpaired (§4.4) |
| 3 | **Paying subscribers** | from **3:50 p.m.** (NYSE) / 3:50 & 3:55 p.m. (Nasdaq) | full imbalance stream incl. clearing prices, ~1/sec — $500–$7,500/mo |
| 4 | **The public tape** | at the freeze time only | a coarse Regulatory/Significant Closing Imbalance flag, ≥500 round lots |

Nasdaq's own Chief Economist words tier 3 exactly this way — "an NOII released **to all
professionals** at the same time" (see §3.2). The live policy question in US equities has never been
*public vs private*; it has been **which restricted set, on what terms**. That is the ground the
venue's design is actually standing on — and note that the design proposed here is a change of
*degree* within tier 1, not the invention of tier 1.

---

## 2. History — when this started, and what was claimed for it

| Date | Event | Source |
|---|---|---|
| pre-2005 | NYSE publishes MOC imbalances at **3:40 p.m.**; NYSE later describes the 10-minute window as "a legacy time frame related to the Exchange's prior publication of imbalance at 3:40 p.m." | NYSE rule filings |
| **14 Sep 2005** | SEC approves SR-NYSE-2005-54: NYSE **eliminates** pre-opening *market-order-only* imbalance publication on expiration Fridays. Reason given: publishing market orders without limit orders "does not provide useful information"; the Commission agreed NYSE "will no longer disseminate information that may have been **misleading** to investors." | [Rel. 34-52421](https://www.sec.gov/files/rules/sro/nyse/34-52421.pdf) |
| **29 Mar 2004** | **Nasdaq Closing Cross goes live**, with the NOII. Filed as **SR-NASD-2003-173** (submitted 24 Nov 2003). Stated benefits: "increased transparency, predictability, and reliability of closing prices." | [SEC comment file SR-NASD-2003-173](https://www.sec.gov/rules/sro/nasd/nasd2003173.shtml) |
| **May 2008** | NYSE systems begin **electronically** providing Floor brokers the size of, and imbalance between, MOC and marketable LOC interest — a channel narrower than the public one | NYSE Rule 123C filings |
| **May 2008 → Mar 2009** | **HKEX launches, then scraps, its Closing Auction Session** after HSBC falls ~10% inside the 10-minute auction on 9 March 2009 | SCMP / CNBC / Traders Magazine |
| **2008** | NYSE **New Market Model**: specialists phased out, replaced by DMMs; the specialist's order-by-order advance **"look" at incoming orders is eliminated** | [SR-NYSE-2008-46 notice](https://www.federalregister.gov/documents/2008/10/29/E8-25797/self-regulatory-organizations-new-york-stock-exchange-llc-notice-of-filing-of-amendment-nos-2-and-3) |
| **24 Jun 2010** | NYSE files to extend Floor-broker imbalance access to **two hours before the close until 15 minutes before it**. Justification: Floor brokers act "as agent for sophisticated customers" and a key part of that role "is to provide market 'color'." Access restricted to Floor broker **hand-held devices**, which "are unable to automatically forward or re-transmit the electronic datafeed," though brokers may relay specific data points to their customers | [SR-NYSE-2010-38, 75 FR (24 Jun 2010)](https://www.federalregister.gov/documents/2010/06/24/2010-15246/self-regulatory-organizations-new-york-stock-exchange-llc-notice-of-filing-of-proposed-rule-change) |
| **2010** | NYSE begins disseminating Closing Auction Imbalance Information **ten minutes** before the close | NYSE Rule 7.35 history |
| **19 Oct 2018** | SEC approves **SR-Nasdaq-2018-68**: Nasdaq MOC/LOC cutoff moves **3:50 → 3:55 p.m.** | Rel. 34-84454, 83 FR 53923 |
| **31 Jan 2019** | SEC approves **SR-NYSE-2018-58**: NYSE cutoff and imbalance publication move **3:45 → 3:50 p.m.**; Floor-broker window correspondingly extended from 2:00–3:45 to **2:00–3:50 p.m.** NYSE's stated reason: more control over end-of-day trading, and additional publication time "should help investors to better understand imbalance and manage their orders." **The Commission received no comment letters.** | [Rel. 34-85021](https://www.sec.gov/files/rules/sro/nyse/2019/34-85021.pdf) |
| **4 Nov 2019** | Nasdaq introduces the **EOII** at 3:50 p.m. (10-second intervals, no near/far price), while keeping MOC entry open to 3:55 p.m. but **removing the ability to cancel after 3:50 p.m.** | [RBC Capital Markets, *Nasdaq — Early Look MOC*, October 2019](https://www.rbccm.com/assets/rbccm/docs/housing-market/Nasdaq_Close.pdf) |
| **21 Jan 2020** | SEC grants final approval to **Cboe Market Close (CMC)** — MOC-only matching at the official closing price, **publishing matched size but never the residual imbalance** — over strenuous objection from both primary exchanges. Launched on BZX **6 March 2020** | [Rel. 34-88008](https://www.sec.gov/files/rules/sro/batsbzx/2020/34-88008.pdf); [Cboe, 30 Jan 2020](https://ir.cboe.com/news/news-details/2020/Cboe-Announces-Launch-Date-for-Cboe-Market-Close-01-30-2020/default.aspx) |
| **25 Jul 2016** | **HKEX reintroduces the CAS** with a ±5% price cap and a two-minute random close — the fix was **price bounds and randomised timing**, not a change in disclosure | SCMP / CNBC |
| **Oct 2021** | **TSX MOC Modernization** goes live (see §1.3) | TSX |
| **Jul 2021** | NYSE Floor brokers **may no longer represent verbal interest** for the Closing Auction; must enter orders electronically during Core Trading Hours | SR-NYSE-2020-95, Rel. 34-92480 |
| **Oct 2023 – Jul 2024** | NYSE proposes (SR-NYSE-2023-36) to **eliminate DMM access to non-public intraday aggregate order information** … and then **withdraws the filing** in July 2024. See §4 — this is the centrepiece | [Rel. 34-98869](https://www.sec.gov/files/rules/sro/nyse/2023/34-98869.pdf); [withdrawal notice, 9 Jul 2024](https://www.federalregister.gov/documents/2024/07/09/2024-15036/self-regulatory-organizations-new-york-stock-exchange-llc-notice-of-withdrawal-of-proposed-rule) |
| **28 Oct 2024** | NYSE **Significant Imbalance** replaces Regulatory Imbalance | NYSE Data Insights, 4 Nov 2024 |
| **8 Dec 2025** | Euronext launches **AVD** — an imbalance-interaction order type that **publishes nothing** | Euronext |

**What venues claimed it would achieve.** Uniformly: attract offsetting liquidity, improve price
discovery, make the closing price more reliable as a benchmark. Nasdaq (2003–04): "increased
transparency, predictability, and reliability of closing prices." NYSE (2018): help investors
"better understand imbalance and manage their orders." TSX (2021): "advertise and solicit liquidity
into the MOC book." **In none of the filings reviewed here did a venue present pre-change empirical
evidence that publication reduces cost for the imbalance side.** The claims are stated as design
rationale and accepted by the Commission on that basis. That is a genuine gap in the regulatory
record and is worth saying out loud.

---

## 3. The core question — what the research actually shows

**Short answer.** The canonical theory (Admati & Pfleiderer 1991) says pre-announcement **reduces the
announcer's own trading costs** — i.e. it points *against* this design. The empirical record supports
that theory **where the imbalance holder retains price discretion**, and contradicts it **where the
holder is contractually forced to print at a benchmark**. That distinction — not "transparency
good/bad" — is what actually separates the findings, and it is the distinction the pitch should be
built on.

On the narrow question of what a published closing imbalance does to the MOC sender's execution, the
literature is **thin to absent**: no peer-reviewed paper measures pre-close drift as a function of
published imbalance magnitude in US closing auctions. On the broader question of pre-trade
transparency, it is **openly contested — by the authors themselves.**

### 3.1 Growth of the closing auction (well established)

- **Bogousslavsky & Muravyev (2023)**, "Who trades at the close? Implications for price discovery
  and liquidity," *Journal of Financial Markets* 66, 100852. Closing auctions were **7.5% of daily US
  volume in 2018, up from 3.1% in 2010**; "closing prices typically match pre-close bid or ask
  prices, and **price impact is lower than during continuous trading**. Auction price deviations
  **revert quickly and almost completely, on average**." Growth is attributed to indexing and ETFs —
  auction-to-intraday volume spikes on S&P 500 additions and stays permanently higher.
  ([SSRN](https://papers.ssrn.com/sol3/papers.cfm?abstract_id=3485840) ·
  [ScienceDirect](https://www.sciencedirect.com/science/article/abs/pii/S1386418123000502))
  ⚠️ **Two warnings.** (i) The widely-circulated claim that "**over 75% of auctions settle within the
  bid–ask spread**" **could not be traced to this paper — do not use it.** (ii) The SSRN title
  *"Should We Use Closing Prices? Institutional Price Pressure at the Close"* is an **earlier title
  of this same paper**, not a second one. No evidence this paper analyses NOII feeds at all.
- **Jegadeesh, N. & Wu, Y. (2022)**, "Closing auctions: Nasdaq versus NYSE," *Journal of Financial
  Economics* 143(3), 1120–1139. 2010–2020. Closing-auction volume peaked at **~10% of total volume in
  2019**. **Price impact is 58% larger on Nasdaq than NYSE**; the temporary component is ~**85% on
  Nasdaq / 62% on NYSE** and **dissipates over 3–5 days**. Cost of trading in the auction is generally
  lower than during the day; auctions attract mainly uninformed/passive flow.
  ⚠️ **This is a direct, unreconciled conflict with Bogousslavsky & Muravyev**, who say deviations
  "revert quickly and almost completely." Three-to-five days is not "quickly." **Flag the conflict
  yourself before a judge does.**
- **Goyal, Jegadeesh & Wu**, "Price Impact in Closing Auctions, Opening Auctions, and Continuous
  Markets: A Benchmark for Cost of Trading on Anomalies," *Journal of Financial and Quantitative
  Analysis* (First View, 2026), DOI 10.1017/S0022109026102592. Closing auctions ≈ **10% of daily
  volume**. **Price impact is lower in closing auctions than in the continuous market for all stocks
  except Nasdaq microcaps.** Large non-microcaps: **3.2–10.1 bps in the auction vs 6.4–20.2 bps
  continuous.** Opening auctions are illiquid.
  ([Cambridge Core](https://www.cambridge.org/core/journals/journal-of-financial-and-quantitative-analysis/article/price-impact-in-closing-auctions-opening-auctions-and-continuous-markets-a-benchmark-for-cost-of-trading-on-anomalies/0F72910A79C5B42CF6E85F55164CE846))
- **Europe runs about 2.5× the US share.** Euronext's quantitative research team: closing auctions
  are **over 25% of multilateral addressable volume in Europe** as of early 2026 (Besson & Quily,
  2 April 2026).
- **Hong Kong:** CAS reached **more than 8% of trading volume by June 2018**, up from ~5% of full-day
  turnover in the pre-2009 regime (Chan & Yao, HKIMR, January 2021).
- Venue-reported current levels: NYSE closing auction ≈ **10.5%** of NYSE-listed volume (Q2 2024),
  with six-month median closing volumes of **12.43% NYSE / 10.12% Nasdaq** in 2025, and a record
  3.57bn shares / $230.5bn matched at the NYSE close on 20 March 2026. `[UNVERIFIED — venue and
  vendor marketing material; directionally consistent with the peer-reviewed ~10%, but the specific
  percentages have not been traced to a primary statistical release.]`

**Is transparency credited or blamed for the growth?** Neither, cleanly. Bogousslavsky & Muravyev
attribute growth to **indexing and ETFs**, not to disclosure design. No paper reviewed here
identifies imbalance publication as a causal driver of closing-auction growth. Venues assert the
link; the academic work does not test it.

### 3.1a The canonical theory says publication HELPS the sender — start by conceding this

**Admati, A. & Pfleiderer, P. (1991), "Sunshine Trading and Financial Market Equilibrium," *Review
of Financial Studies* 4(3), 443–481.** This is the load-bearing theory paper on exactly this
question, and **it runs against the design.** Abstract, verbatim:

> "…preannouncement changes the nature of any informational asymmetries in the market. Second,
> preannouncement can **coordinate the supply and demand of liquidity** in the market. We show that
> **preannouncement typically reduces the trading costs of those who preannounce**, but its effects
> on the trading costs and welfare of **other traders are ambiguous**."

Two channels, both of which map exactly onto an MOC imbalance feed: publication **certifies the flow
as uninformed**, and it **coordinates offsetting liquidity to a point in time**. The cost reduction
accrues to *the announcer*. The ambiguity is about everyone else.

**Concede this on stage.** The theory does not say "transparency is good in general" — it says
specifically that the party with the imbalance benefits from announcing it. Arguing the opposite
without engaging Admati–Pfleiderer will not survive a finance judge.

**The distinction that actually rescues the design.** The index-fund cost literature (§3.3a) shows
real, persistent, billions-of-dollars harm from predictable trades — but in **every one of those
cases the harmed party is contractually forced to print at a specific benchmark and has no price
discretion.** That, not transparency per se, is the operative variable. Sunshine trading helps a
discretionary announcer who can choose not to trade; it harms a mandated one who cannot.

### 3.2 Does publication attract offsetting liquidity, and what does it cost the imbalance side?

The single most quantitative public analysis is **Nasdaq's own**, and it should be used *because* it
is the best evidence and *labelled* because it is self-interested.

**Phil Mackintosh (Chief Economist, Nasdaq), "How Much Does the MOC Imbalance Matter?", 27 September
2019.** All Nasdaq-100 imbalances, Q2 2019:

- "the imbalance information does in fact have a **material effect on price**"
- "about **80% of the ultimate price move into the close was priced in within 300ms**"
- "close prices move an average of **5.5bps** immediately after the information in the NOII
  announcement. That's a fraction of the average **272bps** range during the whole day, and **just
  over 1-times the average spread**"
- "the **liquidity premium, or profit earned by those facilitating the close imbalance news, is just
  1.7bps on average**, or less than half the spread"
- larger imbalances move the market more, consistent with standard impact research

([Nasdaq.com](https://www.nasdaq.com/articles/how-much-does-the-moc-imbalance-matter-2019-09-27))

**Read this carefully, because it cuts both ways and the trader in the room will see both edges.**

- *Against publication:* the price moves **5.5 bps against the imbalance side within 300 milliseconds
  of publication**, before the print. That is a real, measured, immediate cost imposed on the MOC
  sender by the act of disclosure. It is not a theory of front-running; it is a measured
  anticipatory repricing.
- *For publication:* only **1.7 bps of that 5.5 bps is captured as profit** by the liquidity
  providers. The remaining ~3.8 bps is efficient repricing to the true clearing level — information
  that would have shown up in the print anyway. The toll for the service is small, and it is small
  **because many providers compete for it**.

That second point is the crux of the case against selective disclosure, and Mackintosh states it
explicitly, in the context of opposing Cboe Market Close:

> "That makes having a single auction **without separate information leakage to specific traders**
> really important to index funds and investors. If imbalance information was **leaked to specific
> participants ahead of the close**, it's possible that any fee savings would be more than offset by
> market impact, making the investors trading in the close **worse off in total**. What we have now,
> a single MOC auction with an NOII released **to all professionals at the same time**, and time to
> pull liquidity from continuous markets into the close, creates a **competitive dynamic and a level
> playing field**."

**This is the objection to beat, and it is not a weak one.** Note also that it is an argument made by
an incumbent monopolist defending its closing auction against a competitor — Nasdaq and NYSE both
fought Cboe Market Close hard, and the SEC approved CMC anyway in January 2020, finding it "should
introduce and promote competitive forces among national securities exchanges for the execution of
[MOC] orders" without disrupting price discovery or increasing manipulation risk. The argument is
sound on its logic; its provenance is interested.

#### 3.2a Peer-reviewed evidence that publication DOES pull in offsetting liquidity

Four findings, in descending order of how much weight they will bear:

1. **Chung, K.H., Chuwonganant, C. & Kim, Y. (2022), "Preopening price indications and market
   quality: Evidence from NYSE Rule 48," *Journal of Financial Research* 45(2), 205–228.** The
   strongest citation available *because it is a removal design*. Rule 48 suspends DMMs' duty to
   disseminate preopening price indications during extreme volatility. Verbatim:

   > "**The absence of preopening price indications results in reduced liquidity** during the first
   > 30 min of the trading day. We interpret this finding as evidence that **liquidity suppliers are
   > less willing to provide liquidity in the absence of a reference point or benchmark regarding
   > stock value**."

   ⚠️ Treatment is **endogenous** — Rule 48 is invoked precisely on stressed days — and no magnitudes
   were retrievable. Still: take the indication away, liquidity leaves.

2. **Derksen, M., Kleijn, B. & de Vilder, R. (2022), "Heavy tailed distributions in closing
   auctions," *Physica A* 593, 126959.** Liquid European stocks. Most market orders arrive in the
   first seconds, **revealing the imbalance early**; thereafter "**limit orders are placed against
   the direction of the market order imbalance, reflecting strategic behaviour**," with a
   proportionality coefficient of **c ≈ 0.2–0.4**. And the punchline: "large closing price
   fluctuations are typically **not** caused by large market orders … **tails become heavier when
   market orders are removed**." The offsetting response is large enough to **cancel the tail-risk
   contribution of large imbalances.** This is the best peer-reviewed measurement of the
   liquidity-attraction channel that exists.

3. **Barclay, M., Hendershott, T. & Jones, C. (2008), "Order Consolidation, Price Efficiency, and
   Extreme Liquidity Shocks," *JFQA* 43(1), 93–122.** ⚠️ **Correction to the common misattribution:
   this paper studies Nasdaq's OPENING cross, not the closing cross.** Design: S&P 500 futures settle
   on *opening* prices, so quad-witching days create large, predictable, informationless opening
   imbalances. Excess close-to-open volatility on witching days: **28bp NYSE vs 64bp Nasdaq**. After
   Nasdaq launched its opening cross (Nov 8 – Dec 13, 2004), the NYSE–Nasdaq gap in opening price
   efficiency was **completely eliminated** on non-witching days (`Cross×Nasdaq` = +0.25, t≈5).

   **The finding to actually use is revealed preference by the imbalance holder**, verbatim:

   > "the size of the NYSE opening trade increased by a factor of three on witching days — from about
   > $9 million per stock per day in 2003 and 2004 to almost $27 million per stock per day
   > thereafter … **more than half of this increase is due to trades identified as index arbitrage**."

   When a mechanism absorbed a large predictable informationless print better, the parties *with* the
   imbalance **tripled the size they were willing to send it in**. That is the imbalance side voting
   with its own flow.

4. **NYSE quantitative research, Choey Li (22 Aug and 17 Oct 2023), two parts.** Buy imbalances drive
   reference prices up, sell imbalances down. Russell 1000 orders ≈2.4% of CADV: immediate impact
   **0.34× the daily average spread** at 15:55, **0.40×** by 15:59; drift from arrival reference to
   auction price **0.6–1.7× daily average spread**. Part 2: **imbalance-OFFSETTING orders earn
   +$0.0075 to +$0.014 VWAC; imbalance-JOINING orders lose.** ⚠️ Exchange-published, not peer
   reviewed, NYSE has a commercial interest, and impact is reported in **multiples of spread, not
   bps** — do not convert.

**Also relevant, and it is a FOR-the-design citation if it survives checking:**
**Cordi, Félez-Viñas, Foley & Putniņš, "Closing Time: Effects of the closing mechanism and design on
market quality"** — a multi-exchange panel concluding closing auctions work better with randomised
closing times, volatility extensions, no order alteration during the pre-close, **and that DO NOT
display indicative closing prices.** Stated mechanism: transparency reduces manipulation but costs
efficiency, because it "can create a disincentive to trade for those who in doing so may reveal an
information advantage." ⚠️ `[PARTIALLY VERIFIED]` — sources disagree on sample size (20 vs 43
exchanges), the author list differs between versions, and **no journal publication was found for
either version despite the paper circulating since 2015. Verify before citing.** Putniņš is a
serious researcher and this line will be known to a market-microstructure judge either way.

### 3.3 Pre-trade transparency generally — the literature is directly contradictory

This is where an honest answer is more useful than a confident one. Two well-identified natural
experiments on opening the book reach **opposite conclusions**, and the authors of the second say so
in print and cannot fully explain the difference.

**Madhavan, A., Porter, D. & Weaver, D. (2005), "Should securities markets be transparent?",
*Journal of Financial Markets* 8(3), 265–287** (working paper 2000). On **12 April 1990** the Toronto
Stock Exchange began disseminating real-time detailed limit-order-book information **to the public**,
covering both floor-traded and CATS stocks; previously only the registered trader (the TSE's
specialist equivalent) saw the book. `[UNVERIFIED: sources differ on whether four or five levels of
depth were disclosed — say "several levels beyond the best quote".]`

Mechanism, in the authors' own words:

> "Theory suggests that greater transparency of this form will result in more efficient order
> placement by market-order traders. Since trading is a zero-sum game, this gain in expected profits
> is associated with larger losses to liquidity providers if the limit-order book remains as deep as
> before. It follows that **liquidity providers will be less willing to provide free options to the
> market in the form of limit orders and, hence, that spreads will widen**."

Result, verbatim:

> "**We find that higher transparency does not improve market quality.** In particular, our analysis
> shows that **transactions costs increase after the introduction of the rule change**, even when
> controlling for other factors… **Cross-sectional evidence shows that the reduction in liquidity and
> increase in transactions costs are associated with reductions in asset values**… There is no
> evidence, however, that spreads of cross-listed stocks widen in other markets, nor is there any
> significant order-flow migration."

⚠️ **No magnitudes are given in the accessible version. Do not quote a number for this paper.**

Crucially, the authors themselves list order-imbalance disclosure as a target of their result: their
findings bear on "the desirability of pre-announcements of intentions to trade (sunshine trading),
**the nature and extent of disclosure of order imbalances at openings or trading halts**…"
([Bank of Canada conference version](https://www.bankofcanada.ca/wp-content/uploads/2010/09/madhaven-porter-weaver.pdf))

**Boehmer, Saar & Yu (2005), "Lifting the Veil: An Analysis of Pre-trade Transparency at the NYSE,"
*Journal of Finance* vol. LX no. 2, April 2005.** Studies **NYSE OpenBook**. Findings, verbatim from
the abstract: "traders attempt to manage limit-order exposure: They submit smaller orders and cancel
orders faster. **Specialists' participation rate and the depth they add to the quote decline.**
Liquidity increases in that the price impact of orders declines, and we find some improvement in the
informational efficiency of prices."

The authors confront the contradiction head-on:

> "While we find that effective spreads of trades decrease, Madhavan, Porter, and Weaver (2000)
> document an **increase** in spreads following the change in pre-trade transparency implemented by
> the Toronto Stock Exchange in 1990. **What can explain the conflicting results? The answer probably
> does not lie in differences in market structure** between the two exchanges … The conflicting
> results **may** be due to developments over the past decade in information processing, order
> handling, and trading technologies."

And they are candid about the limit of their own welfare claim:

> "the effective spread of a trade does not constitute a perfect measure of transaction costs …
> If indeed the cost of marketable orders (effective spreads) decreases, it is reasonable to assume
> that the **gain to limit orders that supply liquidity decreases as well** … the impact on his
> overall transaction costs is **ambiguous**."

> "We are **unable to judge whether the trading costs of investors** who utilize both market and
> limit orders in the new regime **are lower** than the trading costs when traders did not have
> information about the book."

**The reconciliation that matters — and it is the sharpest analytical point in this document.**
MPW and Boehmer–Saar–Yu both disclose **the liquidity *supplier's* standing book** — the resting limit
orders that can be picked off. MPW's whole mechanism is the "free option" that resting limit orders
hand to the market. A closing-auction imbalance feed discloses something categorically different:
**the liquidity *demander's* unmet need.** Nobody can pick off an MOC order the way they can pick off
a stale limit order; the MOC sender is not writing an option, they are advertising a want.

Admati–Pfleiderer's sunshine-trading result applies to the second object. MPW's free-option result
applies to the first. **They are not in conflict about the same thing.** Anyone — on either side —
who cites MPW as though it settles imbalance disclosure is over-reading it, and so is anyone who
cites Boehmer–Saar–Yu the same way. Say this before it is said to you.

**Three further things follow, and all three are directly on point:**

1. **The empirical literature on pre-trade transparency is not settled.** Two clean natural
   experiments, opposite signs, and the later authors explicitly decline to explain why.
2. **Even the pro-transparency result finds that the designated liquidity provider withdraws.**
   Boehmer, Saar & Yu document that the specialist's participation rate fell and the specialist's
   contribution to quoted depth "declines **monotonically** over the four post-event periods." They
   name the transfer directly: "welfare redistributions … from **liquidity suppliers to demanders**.
   The decrease in the price impact of trades and marketable orders **reduces the compensation for
   liquidity provision, hurting limit-order suppliers and specialists**."
3. **Nobody has measured total cost for the party with the imbalance.** The best paper on the topic
   says so in as many words. That is the exact quantity the venue's design claims to improve, and
   the honest position is that the academic record neither supports nor refutes it.

### 3.3a Predictable trades: predation or liquidity? — also contested

The nearest large literature is on **predictable, benchmark-mandated flow**, and it splits.

**Theory of harm.** **Brunnermeier, M. & Pedersen, L. (2005), "Predatory Trading," *Journal of
Finance* 60(4), 1825–1863.** If one trader must sell, others sell first then buy back; price
overshoots and liquidation value falls. ⚠️ **Note the scope:** the model is about a *distressed or
forced liquidation*, not a routine informationless print. Do not let it be stretched.

**Evidence AGAINST predation.** **Bessembinder, H., Carrion, A., Tuttle, L. & Venkataraman, K.
(2016), "Liquidity, resiliency and market quality around predictable trades: Theory and evidence,"
*Journal of Financial Economics* 121(1), 142–166.** Their result inverts the naive story: even a
*monopolist* strategic trader **improves market quality and increases the liquidator's proceeds** if
temporary impact reverses quickly; competition among strategic traders strictly improves it.
Empirically, around a large ETF's predictable roll: **narrower spreads, greater book depth, improved
resiliency, more individual accounts providing liquidity, and no evidence of systematic predatory
strategies.** ⚠️ Abstract-level only; magnitudes not extracted.

**Evidence FOR harm — and it is substantial.**

| Study | Finding |
|---|---|
| **Mou (2010)**, "Limits to Arbitrage and Commodity Index Investment: Front-Running the Goldman Roll," SSRN 1716841 | Front-running strategies earned **Sharpe ratios up to 4.39** (2000–2010); investors **forwent 3.6% annual return**. ⚠️ **Working paper, not peer-reviewed**; submitted to a CFTC docket. Whether Bessembinder et al. reconcile with it is **unknown** |
| **Petajisto (2011)**, "The index premium and its hidden cost for index funds," *Journal of Empirical Finance* | Price impact **+8.8% (S&P 500 adds), +4.7% (R2000 adds), −15.1% / −4.6% (deletes)**, 1990–2005. Index turnover cost **21–28bp/yr (S&P 500), 38–77bp/yr (Russell 2000)** |
| **Chen, Noronha & Singal (2006)**, "Index Changes and Losses to Index Fund Investors," *Financial Analysts Journal* 62(4), 31–47 | **$1.0–2.1bn/year** across S&P 500 + Russell 2000. Mechanism: **tracking-error minimisation creates predictability, which arbitrageurs exploit at index-fund investors' expense** — an explicit wealth transfer. *The cleanest "predictability costs the constrained party real money" citation available* |
| **Madhavan (2003)**, "The Russell Reconstitution Effect," *FAJ* 59(4), 51–64 | Index funds incurred significant costs; immediacy suppliers captured substantial profits — but those strategies are undiversified with real unwind risk, **which is why the effect persists rather than being arbitraged away** |
| **Sammon & Shim (2026)**, "Index rebalancing and stock market composition: Do indexes time the market?", *JFE* 177 | Index funds' rebalancing portfolios return **4.61% annualised against them**; a **46–69bp/yr index-level drag**; indexes that rebalance less save ~50bp/yr |

**The common factor, and it is the whole ballgame:** in every case where predictability demonstrably
costs the flow-sender real money, **the sender is contractually obliged to print at a specific
benchmark and has no price discretion.** They cannot walk away, cannot wait, cannot re-price. That is
the condition under which sunshine trading turns from an advantage into a levy — and it is precisely
the condition a closing auction imposes on the MOC sender by freezing cancellation at the moment of
publication (§3.5).

### 3.3b The single most relevant peer-reviewed paper — and it is about *post-disclosure access*

**Hu, E. & Murphy, D. (2026), "Vestigial Tails? Floor Brokers at the Close in Modern Electronic
Markets," *Management Science* 72(5), 3974–3996** (online 2 September 2025). ⚠️ Note the correct
title — it is frequently miscited as "…Do Market Closing Auctions Still Serve Their Original
Purpose?"

NYSE accepts late auction orders via floor-broker D-Orders **after** the imbalance is published;
Nasdaq does not. Result: **larger last-minute abnormal imbalances on NYSE and stronger price
reversals** — worse price efficiency — concentrated in stocks where **high floor-broker fees inhibit
auction competition**. Causal identification comes from the **COVID-era NYSE trading floor closure**.
The authors frame it as a tradeoff between auction flexibility and price efficiency.

**Why this is the most important paper in the file.** The measured harm is not caused by the
*disclosure*. It is caused by **who is permitted to act after the disclosure, and how contestable
that channel is.** The problem localises to a privileged late-entry channel with fees high enough to
suppress competition in it. That is simultaneously:

- **the sharpest warning to this design** — a privileged post-disclosure actor with weak competitive
  discipline measurably degraded price efficiency at the largest exchange in the world; and
- **the clearest statement of what fixes it** — contestability of the privileged seat, and low enough
  cost of entry that the privilege is competed for.

Corroborated independently by a timing result: forecastability of the pre-close return **rises into
each order-entry deadline and collapses immediately after it** — peaking approaching Nasdaq's 15:55
cutoff, vanishing after, then spiking again on NYSE before the 15:59:50 D-Quote cutoff.
`[Morand (2020), MSc thesis, Imperial College — student work, colour only, not literature.]`

### 3.4 What is genuinely missing from the record

**The single biggest hole: there is no peer-reviewed paper that measures pre-close price drift as a
function of published imbalance magnitude in US closing auctions.** The direct evidence on that exact
question is (a) exchange-published research by NYSE and Nasdaq — both interested parties, (b) one
MSc thesis, and (c) one European physics-journal paper. **Say this plainly. The honest gap is more
credible than a stretched citation.**

Also **not found**, after multiple search strategies:

| Unexploited natural experiment | Date | Status |
|---|---|---|
| NYSE MOC cutoff + imbalance publication 3:45 → 3:50 | approved 31 Jan 2019, effective ~1 Apr 2019 | **no academic study** |
| Nasdaq **closing-cross** EOII introduced | Q4 2019 (SR-NASDAQ-2019-010) | **no academic study** |
| Nasdaq **opening-cross** EOII introduced | approved 2 Apr 2021, live **26 Apr 2021** | **no academic study — and this is the cleanest identification available**: a precise date, a pure *addition* of earlier disclosure, indicative prices deliberately withheld (isolating the price-signal channel), with NYSE as a control |
| TSX MOC Modernization | Oct 2021 | **no academic study** |
| Xetra "Auction Transparency" (book opened) | **1 June 2026** | **no academic study** |
| Euronext AVD | 8 Dec 2025 | **no academic study** |

⚠️ **Correction to a common framing of the NYSE 2019 change.** It is usually described as "NYSE gave
investors more time." What it actually did was **shorten the pre-auction disclosure window from 15
minutes to 10** while **lengthening order entry by 5 minutes**. Those are opposite-signed treatments
applied simultaneously — **bundled, and not separable.** Anyone using it as evidence about disclosure
is over-reading it.

**No study isolates whether it is the SIZE signal or the PRICE signal that does the work** — despite
Nasdaq having built the EOII around exactly that distinction (§3.4 below).

Two further gaps: no study of TSE Itayose pre-auction disclosure; no LSE or Borsa Italiana
indicative-price regime change located. And **Barardehi has no paper on pre-auction disclosure** —
his imbalance work is on internalised retail order flow; that citation is a misattribution if you
meet it.

**A cautionary note on a much-cited "natural experiment" that does not say what people claim.**
Taiwan switched from 5-second call auctions to continuous trading on **23 March 2020** (open/close
auctions retained). **Lee, Riccò & Wang, "Frequent batch auctions vs. continuous trading: Evidence
from Taiwan," *Journal of Financial Markets*, 2026 (in press)** find the switch associated with
**greater spreads** and greater volume, with efficiency improving in small/mid caps.
**Indriawan, Pascual & Shkilko, "On the Effects of Continuous Trading" (SSRN 3707154, ⚠️ still a
working paper)** find volume rose but from **faster traders picking off slower ones**, with
**overall trading costs rising**, concentrated in individual investors.
⚠️ **Do not use the Citadel Securities / WFE figures** on Taiwan (spreads 8% narrower, etc.) — the
peer-reviewed work finds spreads **widened**, and Citadel has a direct commercial interest.

**Verified but supporting, worth knowing:**

- **Pagano, M. & Schwartz, R. (2003), "A closing call's impact on market quality at Euronext Paris,"
  *Journal of Financial Economics* 68(3), 439–484.** Paris added closing call auctions in 1996
  (illiquid) and 1998 (liquid), 50 stocks each. Introduction **lowered execution costs** for
  individual participants and **sharpened price discovery**.
- **Pagano, M. & Schwartz, R. (2005), "Nasdaq's Closing Cross," *Journal of Portfolio Management*
  31(4), 100–111.** The actual closing-cross study. Launched 29 March 2004, phased in for S&P 500
  names 12 April – 10 May 2004; stress-tested by the 25 June 2004 Russell 2000 rebalance across
  ~1,700 Nasdaq stocks. Effective, **especially for smaller-cap Russell 2000 names**.
- **Comerton-Forde, C. & Rydge, J. (2006), "The influence of call auction algorithm rules on market
  efficiency," *Journal of Financial Markets* 9(2), 199–222.** ASX, **18 March 2002**: began
  disseminating **indicative auction price and indicative surplus volume**. Significantly enhanced
  call-auction price efficiency. ⚠️ **Bundled treatment** — the matching algorithm changed the same
  day. Not a clean transparency experiment.
- **Comerton-Forde, C., Lau, S.T. & McInish, T. (2007), "Opening and closing behavior following the
  introduction of call auctions in Singapore," *Pacific-Basin Finance Journal* 15(1), 18–35.** SGX
  auctions (Aug 2000) enhanced market quality and **reduced closing-price manipulation**.
- **Comerton-Forde, C. & Putniņš, T. (2014), "Stock Price Manipulation: Prevalence and Determinants,"
  *Review of Finance* 18(1), 23–66:** "approximately **one percent of closing prices are
  manipulated**," estimated by detection-controlled estimation (which models that only a non-random
  subset is prosecuted — lead with the methodology, it is why the number holds). Companion:
  **Comerton-Forde & Putniņš (2011), "Measuring closing price manipulation," *Journal of Financial
  Intermediation* 20(2), 135–158** — manipulation produces **~1.4% abnormal day-end return reverting
  ~1.5% next morning**, trading frequency more than triples, spreads +0.5%.
- **Smith, J. (2005), "Nasdaq's Electronic Closing Cross: An Empirical Analysis,"** Nasdaq Economic
  Research WP / *Journal of Trading* 1(3), 47. Participants have "a very good idea of what the
  clearing price will be at the time of the cross" and imbalances are "**effectively buffered by
  participants**." ⚠️ **Smith was Nasdaq's own economist. Interested party — do not present as
  independent.**
- **Eom, K.S., Ok, J. & Park, J.-H. (2007), "Pre-trade transparency and market quality," *Journal of
  Financial Markets* 10(4), 319–341.** Korea Exchange, two transparency changes. **Market quality is
  increasing and CONCAVE in pre-trade transparency, with significantly diminishing returns above a
  point.** They also show prior transparency event studies were econometrically flawed in ways that
  can **reverse** the result. Expect this cited at you — and note it is the most defensible synthesis
  position: *more disclosure helps up to a point, then stops helping.*
- **Han, Q., Zhao, C., Chen, J. & Guo, Q. (2022), "Reexamining the impact of closing call auction on
  market quality: A natural experiment from the Shanghai stock exchange," *Pacific-Basin Finance
  Journal* 74.** SSE introduced a closing call auction **20 August 2018** (Shenzhen 2006 as control):
  **no significant liquidity impact**, volume shifted from closing to pre-closing, **volatility rose
  at pre-closing**, closing-price continuity improved, **no prominent improvement in price
  effectiveness.** A genuine null result — cite it if asked whether closing auctions always help.
- **Gerace, D., Liu, Q. et al. (2015), "Call auction transparency and market liquidity: Evidence from
  China," *International Review of Finance* 15(2), 223–255.** SSE opening call auction: disseminating
  indicative trade information **improves liquidity in the subsequent continuous session** and
  narrows spreads **because adverse selection risk fell significantly**. ⚠️ Exact date of the change
  not retrievable.

**Nasdaq's own design choice isolates the mechanism, and it is quotable.** The **EOII deliberately
excludes the near and far indicative clearing prices**, carrying only reference price, paired shares,
imbalance size and side. Nasdaq's stated reason is to "reduce the possibility of large indicative
price movements" during early price formation. **The exchange itself concluded that publishing an
indicative *price* too early is destabilising, while publishing the imbalance *size* is not.** That
is the size-signal / price-signal distinction, made by the venue, in a rule filing.

The one quasi-experimental result available is **exchange-published**: NYSE reports that after the
28 Oct 2024 switch to the Significant Imbalance flag, daily flags fell from ~313 symbols to ≤151,
small-cap flagged symbols fell 46%, and **slippage compressed by 2.4 bps (−25%) for flagged
symbols**. If that holds, it says something sharp and useful: **narrowing disclosure to where it
matters improved execution.** But it is one week of data, published by the exchange that made the
change, with no control group disclosed. Treat it as suggestive, not as proof.

### 3.5 The mechanism nobody disputes: publication requires lock-in

Across every venue reviewed, **imbalance publication is always paired with removing the imbalance
side's ability to retreat**:

- **NYSE:** after 3:50 p.m., MOC/LOC "cannot be modified or cancelled"; after 3:58 p.m.
  cancellations are rejected outright.
- **Nasdaq:** from 4 Nov 2019, MOC/LOC entry stays open to 3:55 p.m. but **cancellation ends at
  3:50 p.m.** — precisely when the EOII starts.
- **TSX:** during the imbalance period MOC orders are "locked in with no cancels or modifications
  permitted"; LOC orders may only be amended to a **more aggressive** price, "while preserving the
  significance of the imbalance messages."

RBC's contemporaneous note on the Nasdaq change states the trade plainly: "Clients that require the
flexibility to cancel their orders in the last 10 minutes should avoid the Nasdaq close."

**This is the part of the status quo least often said out loud.** A published imbalance is only
informative if it is committed, so venues make it committed. The MOC sender pays for the
market's information with the loss of optionality *and* with the 5.5 bps of anticipatory
repricing. The sender is not a beneficiary of the disclosure regime; the sender is its raw material.

---

## 4. Selective disclosure precedent — privilege bundled with duty

This is the strongest part of the evidentiary case, and it is not analogy. **US equity markets run,
today, a live, SEC-sanctioned regime of selective closing-auction disclosure to a single obligated
liquidity provider per security.**

### 4.1 What the NYSE DMM sees that nobody else does

From NYSE's own filing SR-NYSE-2023-36 (Rel. 34-98869, 6 Nov 2023), footnote 7 — verbatim:

> "in order to facilitate the close, the Exchange makes available to DMMs **at the point of sale
> aggregate order information about all orders eligible to participate in the Closing Auction,
> including the full quantity of Reserve Orders and MOC and LOC Order quantities, at each price
> point**. In addition, the Exchange makes such aggregate order information available to DMM unit
> algorithms in connection with the electronic message sent to a DMM unit algorithm to close an
> assigned security electronically, which is sent shortly after the end of Core Trading Hours. **The
> information available at each price point is not available in the Auction Imbalance Information.**
> However, such information is used to calculate the Continuous Book Clearing Price, which is
> disseminated via Auction Imbalance Information."

Read that again. **The public gets a scalar derived from a curve. The DMM gets the curve.** Plus:
`DMM Auction Liquidity is never included in Auction Imbalance Information` — the DMM alone knows the
true residual, because the DMM alone knows its own intended participation.

### 4.2 What the DMM owes in exchange — NYSE Rule 104, exact terms

| Obligation | Rule | Precise requirement |
|---|---|---|
| **Quoting** | 104(a)(1) / (a)(1)(A) | Maintain a bid or offer **at the NBBO** for at least: **15%** of the trading day (non-ETP, consolidated ADV **< 1 million** shares/month); **10%** (non-ETP, ADV **≥ 1 million**); **25%** (ETPs). **Reserve and other hidden orders do not count** toward the inside-quote calculation |
| **Two-sided quote** | 104(a)(1) | Continuous two-sided quote, displayed size at least one round lot |
| **Facilitate the auctions** | 104(a)(2)–(3) | Facilitate openings, re-openings, **and the close of trading** in assigned securities, "**all of which may include supplying liquidity as needed**" |
| **Odd-lot backstop** | 104(e) | Must **provide contra-side liquidity** for odd-lot quantities eligible for the opening/reopening/**closing transactions** that remain unpaired after all other eligible round-lot interest is paired |
| **Affirmative obligation** | 104(c) | Maintain a fair and orderly market "insofar as reasonably practicable," **including maintaining price continuity with reasonable depth and trading for the DMM's own account when lack of price continuity, lack of depth, or disparity between supply and demand exists or is reasonably to be anticipated**" |
| **Orderly dealing** | 104(d) | DMM proprietary transactions must be effected "in a reasonable and orderly manner in relation to the condition of the general market" |
| **Re-entry after aggressing** | 104(d)(2) | After an Aggressing Transaction the DMM must **re-enter on the opposite side** at or before the applicable Price Participation Point; immediate re-entry if the transaction is block size or greater |
| **Information barriers** | Rule 98 | Restrictions on DMM unit information flow; prohibition on capitalising on material non-public information |
| **Floor communications** | Rule 36 | Restrictions on DMM communications from the Trading Floor |

Source: [Rel. 34-98869 (SR-NYSE-2023-36)](https://www.sec.gov/files/rules/sro/nyse/2023/34-98869.pdf),
which restates Rule 104 in full at pp. 6–7 and 16.

**And NYSE states the bargain in one sentence:**

> "**The Exchange provides access to aggregate order information in order for DMMs and DMM units to
> comply with the requirement to facilitate openings, reopenings, and the close of trading.**"

That is the doctrine, from the venue's own filing: *the information exists to make the obligation
performable.* It is not a perk; it is the input to a duty.

### 4.3 The privilege is bounded by the public number

NYSE Rule 7.35B(g)(2): the Closing Auction Price the DMM is responsible for determining **"must be
at or between the last-published Imbalance Reference Price and the last-published non-zero Continuous
Book Clearing Price."**

This is the design detail worth stealing. **The privileged party's discretion is fenced by the range
implied by what was published to everyone else.** The DMM sees more, but cannot price outside the
envelope the public was shown. Information asymmetry without pricing discretion asymmetry.

### 4.4 The Floor broker tier — a second, wider ring of selective disclosure

NYSE runs selective disclosure at **two** levels of privilege. From NYSE's own
**Regulatory Memo, Q1 2026 Quarterly Expiration, 20 March 2026**, §II.B.3 — verbatim:

> "**From 2:00 p.m. until 3:50 p.m., the Exchange will make available Total Imbalance, Side of Total
> Imbalance, Paired Quantity, Unpaired Quantity, Side of Unpaired Quantity, and if published, Manual
> Closing Imbalance, to Floor Brokers for any security upon request.** In addition, beginning at
> 3:50 p.m., all Closing Auction Imbalance Information will be made available to Floor Brokers."

And the same memo classifies that channel as **non-public**: "The Exchange's Auction Imbalance
Information (i.e., **imbalance information provided to Floor Brokers** for any security in which a
Floor Broker has entered an order or as specifically requested by a Floor Broker) also does not
constitute a Significant Closing Imbalance."

There is a **third** wrinkle: under Rule 7.35B(d)(2) the DMM may use its privileged aggregate order
access to publish a **discretionary Manual Closing Imbalance**, "beginning **one hour before** the
scheduled end of Core Trading Hours up to 3:50 p.m., with prior Trading Official approval." So the
designated provider not only sees more — it holds **discretion over whether and when to reveal part
of what it sees**, subject to exchange sign-off.

So the public's ten-minute window is preceded by a **one-hour-fifty-minute window** in which a
defined, registered class sees the imbalance and the market does not. Delivery is confined to
registered hand-held devices that "are unable to automatically forward or re-transmit the electronic
datafeed," though brokers may relay specific data points to their customers. This was approved in
2010 on the express rationale that providing "market colour" is core to the Floor broker's agency
role, and extended by five minutes without a single comment letter in 2019.
([SR-NYSE-2010-38](https://www.federalregister.gov/documents/2010/06/24/2010-15246/self-regulatory-organizations-new-york-stock-exchange-llc-notice-of-filing-of-proposed-rule-change);
[Rel. 34-85021](https://www.sec.gov/files/rules/sro/nyse/2019/34-85021.pdf))

### 4.5 The natural experiment on selective disclosure itself — NYSE tried to end it and backed out

**October 2023:** NYSE files SR-NYSE-2023-36 to **eliminate DMM access to intraday aggregate order
information**, framing it as follows:

> "Elimination of the availability of aggregate order information to DMMs marks the culmination of
> the Exchange's efforts to **remove any suggestion of informational asymmetry** going into the
> Closing Auction. As a result of the proposal, there would be **no question that DMMs would be on
> the same informational footing as all other market participants** at this crucial point in the
> trading day."

Critically, NYSE proposed to trade the privilege **away against the obligations**: in the same
filing it sought to delete the prohibition on DMM trading in the final ten minutes, remove Rule 36
Floor-communication restrictions, and relax Rule 98 barriers — while *keeping* and *strengthening*
the re-entry requirement.

**The market's two largest electronic liquidity providers both endorsed the swap, and both framed it
as a bargain, not a right:**

- **Hudson River Trading** (Adam Nunes, 28 March 2024): "it is important to draw a distinction
  between features that seek to **balance obligations or restrictions with a DMM's access to
  non-public information** and those that are simply commercial features of NYSE's market model …
  we do not understand the rationale for requiring such features to balance access to non-public
  information as **DMMs do not have access to non-public information during regular trading hours**"
  under the proposal. HRT also argued the legacy obligations "act as a **barrier to entry**,
  resulting in relatively few market makers seeking to become a DMM."
  ([comment letter](https://www.sec.gov/comments/sr-nyse-2023-36/srnyse202336-451239-1158622.pdf))
- **Citadel Securities** (Stephen Berger, 20 March 2024): the proposal "maintain[s] an **appropriate
  balance between the benefits and obligations of being a DMM**." Citadel recites the history
  precisely: pre-DMM specialists enjoyed "(a) trade-through protection for manual quotations and
  (b) an **order-by-order advance 'look' at incoming orders**. **DMMs do not have these benefits.**"
  ([comment letter](https://www.sec.gov/comments/sr-nyse-2023-36/srnyse202336-448299-1147782.pdf))

**Then, in July 2024, NYSE withdrew the filing.** The DMM's privileged, per-price-point view of the
closing book **remains in force today**.

**What this proves and what it does not.** It proves that selective disclosure to one obligated
liquidity provider is not a historical curiosity — it is the operating state of the largest listing
venue in the world, it was examined by the SEC in 2024, the biggest market makers called it a fair
bargain, and the attempt to end it was abandoned. It does **not** prove the arrangement is optimal;
the withdrawal was not accompanied by a public rationale, and the reasons are not on the record.
`[UNVERIFIED — NYSE's reason for withdrawing SR-NYSE-2023-36 was not located.]`

---

### 4.6 A second, closer precedent — Börse Frankfurt's Continuous Auction models

There is a European venue that does **exactly** what this design proposes: shows the indicative
price, executable volume and **imbalance side** to a designated, obligated market maker, and not to
the market.

Source: **T7 Release 14.0, *Market Model Continuous Auction*, v1, 27 October 2025** — the market
model for **Börse Frankfurt (MIC: XFRA)**, a venue distinct from Xetra (MIC: XETR), covering
equities, ETFs/ETPs, mutual funds, bonds, warrants, certificates and subscription rights.

**§6.1.2, Call Phase, "Continuous Auction with Market Maker" — verbatim:**

> "During the auction's call phase, the order book is open with a depth of 1, thus displaying all
> limit and market orders as well as the quote with the accumulated volumes of the best bid and best
> ask limit and the number of orders in the book at these limits. **In case of an executable order
> book the potential executable volume, indicative price and side imbalance for the auction is
> displayed to the respective Market Maker.**"

The **Specialist** variant goes considerably further:

- §6.2.1 (Pre-Call Phase): "the order book is **fully open for the Specialist only**, i.e. the
  Specialist is able to see **each order and quote request as well as its originator**. For trading
  participants the order book is partially closed."
- §2 (Fundamental Principle 2): "Trading is anonymous … **Only the Specialist is able to identify the
  originator of an order.**"
- §5.1: "For trading participants (incl. Market Makers) the order book is partially closed. **Only
  the Specialist receives an overview of the order book situation.**"
- §6.2.2 (Freeze Phase): order book transparency for participants remains **partially closed**.

Corroborated at the wire level: in the T7 market data manual, for the **Continuous Auction Issuer**
trading model the **imbalance side is withheld from the public feed** — `MDEntryType (269) = A`
(Imbalance) is used *instead of* the side-bearing `0`/`1` encoding, and `QuoteCondition` is left
empty. The mechanism for suppressing the side is built into the protocol.

**State this precisely or it will backfire.** This is **not** a scheduled, market-wide closing
auction. These are event-triggered continuous auctions for less-liquid instruments on a separate
venue, and the Market Maker / Specialist carries real quoting obligations (§7.1–7.2). It is a
genuine precedent for the **principle** — indicative price and imbalance side disclosed to one
obligated provider, withheld from the market — and it is *not* a precedent for doing that in a
DAX-style close. Claim the principle; concede the scope.

### 4.7 Where the precedent runs out — the clean negative

Searched and **not found**: any major venue that restricts *scheduled closing-auction* imbalance to a
designated liquidity provider. Specifically checked and negative:

- **Xetra Designated Sponsors** — obliged "to participate in auctions and volatility interruptions,"
  but granted **no** privileged auction data. The controlling field `MarketImbalanceIndicator (28875)`
  is an **instrument-level** switch governing whether the surplus "is displayed to the market"; it has
  no participant dimension. Their only informational privilege is **RFQ targeting** (a requester may
  route an RFQ to Designated Sponsors only) — quote solicitation, not auction imbalance. **Do not
  conflate the two.**
- **Euronext Liquidity Providers** — obligations plus a **fee waiver** ("will not incur any trading
  fees"), never informational access. The only LP-specific data flag runs the *other* way: "LP
  Indications of interest are marked with a specific flag."
- **LSE market makers** — Rules 4000–4334 are pure obligation, e.g. Rule 4101 requires an executable
  quote "until the conclusion of the closing auction including any extensions." Executable Quotes are
  "fully visible … **named**" — *less* anonymity, not more information.
- **Borsa Italiana / Euronext Milan, Japan Exchange (TSE), SIX Swiss** — no evidence found.

**So the honest formulation for the stage is:** every major non-US scheduled closing auction
broadcasts imbalance to all paying subscribers. The only live precedents for *restricted* auction
information are **NYSE** (Floor brokers' 110-minute head start; the DMM's per-price-point view and
discretionary Manual Closing Imbalance) and **Börse Frankfurt's Continuous Auction models**. That is
two precedents, not zero — and not ten. Say the number.

---

## 5. Verdict

### 5.1 Where the evidence supports the design

1. **Precedent is not merely available — it is the incumbent arrangement.** NYSE gives one
   designated liquidity provider per security a strictly richer view of the closing book than
   anyone else gets (per-price-point, including full Reserve quantities), plus discretion to publish
   a Manual Closing Imbalance an hour before anyone else sees anything — and does so on the express
   ground that the information is what makes the obligation performable. In 2024 the SEC examined
   ending it; it was not ended. And **Börse Frankfurt's Continuous Auction models do the narrow thing
   exactly**: indicative price, executable volume and **imbalance side** shown to the designated
   Market Maker / Specialist and withheld from the market, with the side-suppression built into the
   market data protocol (§4.6).
2. **Broadcasting imbalance is a choice, not a legal requirement — and Europe's legislator did not
   make it.** RTS 1 mandates only the indicative price and the executable volume at that price.
   Every European venue that publishes surplus and side does so **voluntarily, above the regulatory
   floor**. A design that discloses imbalance narrowly is not fighting a transparency mandate; there
   isn't one.
2. **The obligations are the justification, and they are concrete.** Not "best efforts" — a
   percentage-of-day NBBO quoting floor (10/15/25%), a duty to supply liquidity as needed at the
   close, a contra-side odd-lot backstop, and an affirmative duty to trade own account against
   supply/demand disparity. A design that pairs privilege with duty is squarely in the tradition.
3. **Broadcast has a measured cost to the imbalance side.** 5.5 bps of adverse repricing within
   300ms of publication, on Nasdaq's own numbers. Whatever else is true, publishing is not free for
   the sender.
4. **Broadcast induces strategic withholding, on the record.** TSX states in a primary document that
   more frequent imbalance messaging created an incentive for providers "to hold volume to the last
   moment … in order to garner the most information before committing volume," requiring a randomised
   freeze as a countermeasure.
5. **Full transparency demonstrably reduces the designated provider's commitment.** Boehmer, Saar &
   Yu find the NYSE specialist's participation rate fell and contribution to quoted depth declined
   monotonically after OpenBook, and name the transfer: compensation for liquidity provision fell,
   "hurting limit-order suppliers and specialists." If you want a provider to stand up size on
   demand, stripping its informational rent is the wrong lever.
6. **The pre-trade transparency literature does not support a confident pro-broadcast prior.**
   Toronto 1990 (costs up) versus NYSE OpenBook (impact down) — opposite signs, and the second set of
   authors cannot explain the first. Eom, Ok & Park (2007) find market quality **concave** in
   transparency with sharply diminishing returns, and show that the econometrics of this event-study
   literature can **reverse** results. Han et al. (2022) on Shanghai is a clean **null**. Anyone
   asserting "transparency is obviously better" is ahead of the evidence.
   ⚠️ But apply this honestly: MPW and Boehmer–Saar–Yu disclose the *supplier's* book, not the
   *demander's* need (§3.3). They do not settle imbalance disclosure in either direction.
7. **Predictability demonstrably costs a benchmark-mandated sender real money.** Chen, Noronha &
   Singal put it at **$1.0–2.1bn/year** for index funds; Petajisto at **21–77bp/yr**; Sammon & Shim at
   **46–69bp/yr**. The mechanism they name is exactly the MOC sender's condition:
   tracking-error minimisation creates predictability, and arbitrageurs monetise it at the
   constrained party's expense. The MOC sender who cannot cancel after 3:50 p.m. is that party.
8. **Two venues, and possibly the literature, agree the *price* signal is the dangerous one.**
   Nasdaq's EOII deliberately omits the near/far indicative clearing prices to "reduce the
   possibility of large indicative price movements"; Cordi, Félez-Viñas, Foley & Putniņš conclude
   closing auctions work better **without** displaying indicative closing prices `[partially
   verified — check before citing]`. A design that withholds a *price* while an obligated provider
   works the *size* is aimed at the right target.
7. **Two major venues have chosen non-disclosure — one of them inside US regulation.** Cboe Market
   Close, SEC-approved January 2020 and live since March 2020, matches MOC interest at the official
   closing price, publishes **matched size only**, and hands the unmatched residual back to the
   sender rather than exposing it. Euronext AVD, live 8 December 2025, matches against the auction
   imbalance and publishes nothing, explicitly "preventing any information leakage." Withholding a
   residual imbalance is an approved market structure, not a deviation.
8. **"Public" was never public — it is a four-tier hierarchy.** DMM → Floor brokers → paying
   subscribers ($500–$7,500/month) → a coarse flag on the tape. The actionable imbalance stream is
   proprietary; only a ≥500-round-lot flag reaches the SIP. The status quo is *already* selective
   disclosure with a designated top tier; this design changes *how* the tiers are drawn, not
   *whether* they exist.

### 5.2 Where the evidence does not support it

1. **No study shows selective disclosure beats broadcast for the imbalance side.** None was found —
   in either direction. The venue is asserting an untested proposition. So is everyone else, but the
   burden sits with the party departing from convention.
2. **Every major *scheduled closing auction* on earth broadcasts.** Xetra, Euronext, LSE, HKEX,
   Nasdaq and NYSE all publish imbalance quantity and side to all paying subscribers simultaneously —
   and LSE does it "throughout the entire period" of the call. The two selective-disclosure
   precedents are real but narrow: NYSE's Floor-broker/DMM tiering *inside* a broadcast auction, and
   Börse Frankfurt's Continuous Auction models, which are event-triggered auctions in illiquid
   instruments on a different venue. Nobody runs a scheduled close on single-provider disclosure. If
   the judge says "name one," the honest answer is that there isn't one at that scale.
2. **The 1.7 bps figure is a competition result, not a disclosure result.** The reason the toll on
   the MOC sender is small on Nasdaq is that many capitalised firms see the same number at the same
   instant and compete the premium down to less than half the spread. A single recipient faces no
   such discipline. This is the load-bearing weakness.
3. **The specialist precedent is the *discredited* end of the DMM lineage, not the live one.** The
   order-by-order advance "look" was abolished in 2008 precisely because it "could permit them to
   adjust their trading interest to the disadvantage of orders residing on the book." NYSE itself
   described eliminating the remaining asymmetry as removing "any suggestion of informational
   asymmetry." The trend line runs *away* from privileged looks, even if the last step was not taken.
4. **NYSE's privilege is bounded in ways a naive design would not be.** The DMM's view exists inside
   Rule 98 information barriers, Rule 36 communication restrictions, a re-entry requirement after
   aggressing, and a hard price constraint (7.35B(g)(2)) tying the auction price to the range implied
   by the *public* data. Cite the precedent, and you inherit the fence.
5. **The venue's exchange-published evidence is thin.** The best directly supportive datum — NYSE's
   2.4 bps (−25%) slippage compression after narrowing the Significant Imbalance flag — is one week,
   self-reported, uncontrolled.
6. 🔴 **The canonical theory is against the design.** Admati & Pfleiderer (1991) — that
   pre-announcement "typically **reduces the trading costs of those who preannounce**" — is the
   single most-cited result on this exact question, and it says the MOC sender is *helped* by
   publication, not harmed. The escape route (§3.1a) is real but narrow: it holds for a discretionary
   announcer, and the empirical harm cases all involve senders **contractually forced to print at a
   benchmark**. That is a defensible position, not a comfortable one.
7. 🔴 **Removing the reference point demonstrably destroys liquidity.** Chung, Chuwonganant & Kim
   (2022) on NYSE Rule 48: when preopening price indications are suspended, "**liquidity suppliers
   are less willing to provide liquidity in the absence of a reference point or benchmark regarding
   stock value**." A design that shows the imbalance to one party and nothing to the market must
   explain what serves as the reference point for everyone else.
8. 🔴 **The measured harm in the closest peer-reviewed paper comes from a privileged post-disclosure
   channel with weak competition.** Hu & Murphy (2026) locate worse price efficiency at NYSE
   precisely where **high floor-broker fees inhibit auction competition** in the late-entry channel.
   That is a description of the failure mode of a single privileged provider, published in
   *Management Science*, using NYSE data.
9. **The "direction of travel" argument does not hold.** Xetra opened its auction order book on
   **1 June 2026** (full EOBI depth, ten levels on GUI/EMDI/MDI, icebergs showing full quantity).
   Europe's largest auction venue moved decisively toward *more* disclosure in the same release cycle
   in which Euronext launched AVD. Claim a separation of concerns, not a trend.

### 5.3 The strongest argument against, stated as a judge would state it

> "You have designed a monopoly. On Nasdaq the MOC sender pays about 1.7 basis points to be
> facilitated, and pays only that because every professional sees the same imbalance in the same
> millisecond and competes the premium down to under half a spread. You are handing that number to
> one counterparty and asking me to believe it stays 1.7. There is no bid against it. Your provider
> knows the size, knows the side, knows nobody else does, and knows the sender cannot cancel —
> because your design, like every other closing auction, locks the imbalance in the moment it is
> disclosed. That is not a market maker with an obligation; that is a counterparty with a hostage.
> And when NYSE was asked in 2024 whether its own designated market maker should keep a privileged
> view of the closing book, NYSE's own filing said the goal was to remove *any suggestion* of
> informational asymmetry. You are reintroducing what the incumbent was trying to retire. Show me
> the number, not the precedent."

**The honest answer to that** is that the quoting and facilitation obligations, a re-entry
requirement, a price bound tied to publicly-derived reference levels, and periodic contestability of
the designation are what stand in for the missing competition. The evidence available today does not
settle it either way, and claiming otherwise in front of this judge is the fastest way to lose.

### 5.4 The four fences the precedent comes with — and the falsifiable test

If the pitch cites NYSE's DMM as precedent, it should also show it has copied the fences. Every one
of these is in the live NYSE rulebook, and a judge who knows the DMM model will look for them:

| Fence | NYSE analogue | Purpose |
|---|---|---|
| **Price bound** | Rule 7.35B(g)(2) — auction price must sit between the last-published Imbalance Reference Price and the last-published non-zero Continuous Book Clearing Price | The privileged party sees more but cannot price outside the envelope the public was shown |
| **Affirmative duty with teeth** | Rule 104(a)(1) NBBO quoting floors (10/15/25%), 104(a)(2)–(3) close facilitation, 104(e) odd-lot backstop | Makes the privilege the input to a service, not a rent |
| **Re-entry after taking** | Rule 104(d)(2) — must re-enter the opposite side at or before the Price Participation Point, same size, immediately if block-size | Prevents the informed party from simply running the price |
| **Information barrier + contestable designation** | Rules 98 and 36; DMM allocation under Rule 103B | Contains the information and keeps the seat winnable |

**The falsifiable claim to put on the record.** State it as a testable proposition rather than a
belief: *realised slippage for the imbalance side, measured against the pre-disclosure reference
price, will be no worse under single-provider disclosure than under broadcast.* Nasdaq has published
the broadcast benchmark — **5.5 bps of adverse move, of which 1.7 bps is provider profit** — so
there is a public number to be measured against. Offering to be judged on it is stronger than
arguing the precedent, because the precedent is contested and the measurement is not.

---

### 5.5 Things not to say — each of these will get caught

1. **Don't say Hong Kong abolished its auction "because of manipulation."** No regulator ever found
   that. HKEx's own wording was "**appearance of abuse**" and "recent price volatility." The SFC
   investigated and published nothing.
2. **Don't say HKEX's IEP updates every 2 seconds.** It is event-driven, on every change. Two seconds
   is the heartbeat and the Market Turnover message.
3. **Don't quote a random-end duration for Xetra.** Deutsche Börse deliberately does not publish it.
   Euronext (30s), LSE (30s), Borsa Italiana (30s) and HKEX (2 min) are documented; Xetra is not.
4. **Don't say Xetra Designated Sponsors get privileged imbalance.** They do not. **Börse Frankfurt's**
   Specialist/Market Maker does — different venue, different trading model.
5. **Don't say "US imbalance data isn't public."** A coarse Significant/Regulatory Closing Imbalance
   flag does go to the SIP. It is the *actionable detail* that is paywalled.
6. **Don't say "every closing auction publishes the imbalance because regulators require it."** RTS 1
   requires only indicative price and executable volume. European venues publish side and surplus
   voluntarily.
7. **Don't cite ESMA70-156-4263** (market data guidelines) — withdrawn, superseded by Delegated
   Regulation (EU) 2025/1156.
8. **Don't overclaim the DMM exclusion.** Ordinary DMM Orders *are* in the published imbalance; it is
   *DMM Auction Liquidity* — the post-close backstop — that is not.
9. **Don't say "over 75% of closing auctions settle within the bid-ask spread."** That widely-repeated
   figure **could not be traced to Bogousslavsky & Muravyev or to any source.** Drop it.
10. **Don't say Barclay, Hendershott & Jones studied the closing cross.** It studies the **opening**
   cross. Cite it for the opening-cross result and the tripling of index-arbitrage volume.
11. **Don't say the NYSE 2019 change "gave investors more time."** It **shortened disclosure from 15
   to 10 minutes** while lengthening order entry by 5. Bundled, opposite-signed, not separable.
12. **Don't claim a one-way trend toward less disclosure.** Xetra **opened** its auction book on
   1 June 2026. The trend is a separation of concerns, not a direction.
13. **Don't cite Hu & Murphy by the wrong title.** It is "**Floor Brokers at the Close in Modern
   Electronic Markets**," *Management Science* 72(5) — not "Do Market Closing Auctions Still Serve
   Their Original Purpose?"
14. **Don't attribute the AMF's US volume figures to the AMF.** They are Credit Suisse and Goldman
   broker research cited via the FT in a footnote, and the AMF itself warns they are "not necessarily
   comparable" to its European numbers.
15. **Don't use the Citadel/WFE Taiwan numbers.** The peer-reviewed work finds spreads **widened**.
16. **Don't cite "An(other) Look at Closing Auctions."** It does not exist.

---

## 6. Three sentences for the stage

> "Every major primary closing auction makes the same trade: it publishes your imbalance and, in the
> same breath, takes away your ability to cancel — and on Nasdaq's own numbers the price moves an
> average of 5.5 basis points against you the moment it is published, with most of the move into the
> close priced in inside 300 milliseconds.
>
> We concentrate that disclosure in one liquidity provider who carries real obligations, because
> that is exactly the bargain the NYSE still runs today — its designated market maker sees the
> closing book at every price point, information the public feed never carries, and pays for it with
> a quoting floor and a duty to supply liquidity at the close.
>
> What the evidence does not establish — and I won't pretend otherwise — is that one obligated
> provider prices as tightly as many competing ones: the canonical theory, Admati and Pfleiderer on
> sunshine trading, says announcing your imbalance normally *helps* you, and it does, right up until
> you're the one who can't cancel; so the obligations, the price bounds and a contestable seat have
> to do the work competition does elsewhere, and we should be judged on realised slippage against
> Nasdaq's published 5.5-and-1.7 benchmark, not on whose precedent sounds better."

---

## 7. Register of unverified and open items

| # | Item | Status |
|---|---|---|
| 1 | Euronext and LSE call-phase specifics | **VERIFIED** to primary specs — Euronext Optiq MDG v5.354.0 (`Imbalance Quantity`, `Imbalance Quantity Side` 0/1/2, includes hidden quantity) and LSE MIT201 Issue 15.8 §7.2 + GTP 003 §3.11. Random uncrossing **exactly 30s** at Euronext/LSE/Borsa Italiana |
| 1b | LSE PME tolerance ladder (2/5/10/20/50%), PME 5-min / MOE 2-min durations, per-sector random period | `[UNVERIFIED]` — from an **ETP-scoped** LSE factsheet; MIT201 and that factsheet **conflict** on the PME trigger (dynamic reference price vs previous close). The authoritative Millennium Business Parameters file sits behind the LSE Member Portal |
| 1c | Xetra random-end duration | **Deutsche Börse does not publish it.** The commonly repeated "30 seconds" is `[UNVERIFIED]` for Xetra. Do not quote a number |
| 1d | Xetra uncrossed-book display: "best bid and best ask" (T7 R14.0 Market Model) vs "at least ten price levels" (Xetra website) | **Unresolved conflict between two Deutsche Börse primary sources.** Likely the per-instrument `ClosedBookIndicator` |
| 2 | HKEX CAS — dates, trigger, safeguards, published fields | **VERIFIED** to HKEX primary documents: 26 May 2008 launch; HKEx's own 9 Mar 2009 release (−13.3% then −12.5%, total −24.1%); suspension effective 23 Mar 2009; Phase 1 25 Jul 2016, Phase 2 24 Jul 2017; ±5% Stage 1 / touch-at-16:06 Stage 2; 2-min random close; IEP/IEV/imbalance published via OMD-C MsgTypes 41/56/43 |
| 2b | Whether the SFC **found** manipulation in the 9 Mar 2009 HSBC close | **No published finding or enforcement action located.** "The SFC investigated" is verified; "the SFC found manipulation" is **not**. Do not say it |
| 2c | HKEX post-implementation review of the 2016 CAS | **Not located**, despite a 2015 commitment to one. The evidence base is Chan & Yao (HKIMR 2021) and Lei/Ma/Yick (JFM 2020) |
| 2d | Lei, Ma & Yick (JFM 2020) coefficients and sample windows | `[UNVERIFIED]` — paywalled; abstract-level findings only |
| 2e | HKEX date conflict: Phase 2 = 24 Jul 2017 (HKEX) vs 26 Jul 2017 (HKIMR paper) | Use **HKEX's** date |
| 3 | Whether European auction call-phase data is free or subscriber-only | **VERIFIED — paid.** Xetra €2,230–€4,955/mo; LSE £37,083 (L1) / £67,611 (L2) per year. **Counterweight: MiFIR Art. 13(2) mandates the same data free of charge 15 minutes delayed** — temporal and priced asymmetry, not structural |
| 3b | Euronext monthly figures (≈€4,966 Continental Cash L2) | `[UNVERIFIED]` — from a track-changes "compare" document; the **definitions** (Level 1 includes IMP + IMV) are solid, the numbers are directional |
| 3c | HKEX OMD-C current-quarter fee figures | `[UNVERIFIED]` |
| 4 | Any venue outside NYSE that discloses auction imbalance **only** to a designated liquidity provider | **FOUND — one.** Börse Frankfurt's Continuous Auction with Market Maker / with Specialist models (T7 R14.0 *Market Model Continuous Auction*, 27 Oct 2025). **Caveat that matters:** event-triggered auctions in illiquid instruments on a venue separate from Xetra — **not** a scheduled market-wide close. For *scheduled closing auctions*, the negative is clean: Xetra Designated Sponsors, Euronext LPs and LSE market makers get obligations and fee waivers, never privileged imbalance |
| 5 | Peer-reviewed evaluation of any closing-imbalance disclosure regime change (NYSE 2019, Nasdaq EOII 2019 & 2021, TSX 2021, Euronext AVD 2025, Xetra 2026) | **Not found** after multiple search strategies. The Nasdaq **opening-cross EOII (live 26 Apr 2021)** is the cleanest unexploited identification available |
| 5b | Any peer-reviewed measurement of pre-close drift as a function of **published imbalance magnitude** in US closing auctions | **Does not exist.** Direct evidence is NYSE research, Nasdaq research, one MSc thesis, one physics-journal paper. This is the biggest hole in the file |
| 5c | Cordi, Félez-Viñas, Foley & Putniņš, "Closing Time" (concludes: do **not** display indicative closing prices) | `[PARTIALLY VERIFIED]` — sample size (20 vs 43 exchanges) and author list **differ between versions**; **no journal publication found** despite circulating since 2015. **Verify before citing** |
| 5d | Bogousslavsky & Muravyev ("reverts quickly and almost completely") vs Jegadeesh & Wu ("temporary component dissipates over 3–5 days") | **Unreconciled conflict between two top-journal papers.** Flag it yourself |
| 5e | Bessembinder et al. (2016, *JFE*, no predation found) vs Mou (2010, WP, 3.6%/yr cost) | **Unresolved.** Whether Bessembinder et al. cite or reconcile with Mou is **unknown** |
| 5f | Madhavan/Porter/Weaver: four or five levels of depth disclosed on TSX in April 1990 | `[UNVERIFIED]` — sources differ; say "several levels beyond the best quote." **No magnitudes** are given in the accessible version — do not quote a number |
| 5g | Bessembinder et al. (2016) magnitudes and the identity of the ETF studied | `[UNVERIFIED]` — abstract-level only |
| 5h | Chung, Chuwonganant & Kim (2022) magnitudes | `[UNVERIFIED]` — publisher paywall. Treatment is also **endogenous** (Rule 48 fires on stressed days) |
| 5i | Chinco & Sammon, "The Passive-Ownership Share Is Double What You Think It Is" | Exists (SSRN 4188052); **publication venue unverified** — treat as a working paper |
| 5j | Xetra Circular 021/2026 / T7 R14.1 "Auction Transparency", effective 1 Jun 2026 | Reported by the research pass; **not independently re-verified against the circular by me.** Check the circular before putting the date in a deck |
| 6 | Closing-auction volume percentages from venue/vendor marketing (9.44% Q2 2024; 12.43%/10.12% 2025; NYSE Q1 2026 records) | `[UNVERIFIED]` — use the peer-reviewed ~7.5% (2018) and ~10% figures instead when precision matters |
| 7 | NYSE's stated reason for withdrawing SR-NYSE-2023-36 in July 2024 | `[UNVERIFIED]` — not located |
| 8 | Nasdaq FAQ vs rulebook conflict on NOII start time (3:55 vs 3:50) | **Resolved** in favour of the rulebook two-stage EOII/NOII structure; the conflict itself is documented in §1.2 |

---

## 8. Primary sources

**Exchange documents**
- NYSE, *Opening and Closing Auctions Fact Sheet* (2024) — https://www.nyse.com/publicdocs/nyse/markets/nyse/NYSE_Opening_and_Closing_Auctions_Fact_Sheet.pdf
- NYSE, *Closing Process Fact Sheet* (2021) — https://www.nyse.com/publicdocs/nyse/NYSE_Auctions_Closing_Process_Fact_Sheet.pdf
- NYSE, *NYSE Proprietary Market Data Pricing* — https://www.nyse.com/publicdocs/nyse/data/NYSE_Market_Data_Pricing.pdf
- Nasdaq, *The Nasdaq Opening and Closing Crosses — FAQ* — https://www.nasdaq.com/docs/2020/04/03/openclose_faqs.pdf
- Nasdaq Trader, *Opening and Closing Crosses* — https://nasdaqtrader.com/Trader.aspx?id=OpenClose
- TSX, *TSX MOC Modernization — Detailed Guide*, September 2021 — https://www.tsx.com/en/resource/2357
- Deutsche Börse, *T7 Release 14.0 — Market Model for the Trading Venue Xetra*, v1, 27 Oct 2025 — https://www.cashmarket.deutsche-boerse.com/resource/blob/4805606/2b26aa5a6fd8cf9dca6743b674e7317f/data/T7_Release_14.0_-_Market_Model%20_Xetra.pdf
- Deutsche Börse, *T7 Release 14.0 — Market Model Continuous Auction* (Börse Frankfurt), v1, 27 Oct 2025 — https://www.xetra.com/resource/blob/4805608/61b3f9d39889402dfca793bb8ab18ca0/data/T7_Release_14.0_-_Market_Model_Continious_Auction.pdf
- Deutsche Börse, *Trading Parameter Xetra (MIC: XETR)*, 1 Dec 2025 — https://www.cashmarket.deutsche-boerse.com/resource/blob/250890/43c50e657b0ff5fb58132fbf6e063517/data/trading-parameters-xetra.pdf
- Deutsche Börse, *MDDA Price List v16_1*, valid 1 Aug 2026 — https://www.mds.deutsche-boerse.com/resource/blob/5353328/480628e5e12c9126a6a67030377d2a6e/data/MDDA_Price_List_16_1.pdf
- Euronext, *Optiq MDG Messages — Interface Specification, Cash and Derivatives*, v5.354.0, 5 Mar 2025 — https://connect.euronext.com/sites/default/files/it-documentation/Optiq%20MDG%20Messages%20-%20Interface%20Specification%20-%20Euronext%20Cash%20and%20Derivatives%20Markets%20-%20External%20-%20v5.354.0.pdf
- Euronext, *Appendix to Instructions 4-01 / 4-03 Trading Manuals* (auction clock times) — https://www.euronext.com/en/media/1927
- Euronext, *Auction Volume Discovery (AVD)* — https://www.euronext.com/en/trading/trading-services/auction-volume-discovery-avd
- London Stock Exchange, *MIT201 — Guide to the Trading System*, Issue 15.8, eff. 19 Jan 2026 — https://docs.londonstockexchange.com/sites/default/files/documents/mit201-guide-to-the-trading-system-15-8-20260119_0.pdf
- HKEX, *Trading Mechanism of Closing Auction Session (CAS) in the Securities Market*
- HKEx, *Trading in HSBC shares during Today's Closing Auction Session*, 9 Mar 2009 — https://www.hkex.com.hk/News/News-Release/2009/090309news?sc_lang=en
- Borsa Italiana, *Asta di chiusura* — https://www.borsaitaliana.it/etf/perintermediari/astadichiusura/astadichiusura.en.htm
- NYSE, *Regulatory Memo — Q1 2026 Quarterly Expiration*, 20 Mar 2026 — https://www.nyse.com/publicdocs/nyse/markets/nyse/rule-interpretations/2026/Q1_2026_Quarterly_Expiration_RM_3.20.2026.pdf

**EU regulation**
- Commission Delegated Regulation (EU) 2017/587 (RTS 1), Annex I Table 1 — https://eur-lex.europa.eu/eli/reg_del/2017/587/oj/eng · Annex text: https://ec.europa.eu/finance/securities/docs/isd/mifid/rts/160714-rts-1-annex_en.pdf
- MiFIR Art. 13(2), as amended by Regulation (EU) 2024/791, consolidated 02014R0600-20251123
- Commission Delegated Regulation (EU) 2025/1156 of 12 June 2025 (supersedes withdrawn ESMA70-156-4263)

**SEC rule filings and orders**
- Rel. 34-52421 (SR-NYSE-2005-54), 14 Sep 2005 — https://www.sec.gov/files/rules/sro/nyse/34-52421.pdf
- SR-NASD-2003-173 (Nasdaq Closing Cross) — https://www.sec.gov/rules/sro/nasd/nasd2003173.shtml
- SR-NYSE-2010-38 (Floor broker imbalance access), 24 Jun 2010 — https://www.federalregister.gov/documents/2010/06/24/2010-15246/self-regulatory-organizations-new-york-stock-exchange-llc-notice-of-filing-of-proposed-rule-change
- Rel. 34-85021 (SR-NYSE-2018-58), 31 Jan 2019 — https://www.sec.gov/files/rules/sro/nyse/2019/34-85021.pdf
- Rel. 34-98869 (SR-NYSE-2023-36), 6 Nov 2023 — https://www.sec.gov/files/rules/sro/nyse/2023/34-98869.pdf
- Withdrawal of SR-NYSE-2023-36, 9 Jul 2024 — https://www.federalregister.gov/documents/2024/07/09/2024-15036/self-regulatory-organizations-new-york-stock-exchange-llc-notice-of-withdrawal-of-proposed-rule
- Rel. 34-99719 (SR-NYSE-2024-13, Significant Imbalance) — https://www.sec.gov/files/rules/sro/nyse/2024/34-99719.pdf
- Rel. 34-101620 (SR-NASDAQ-2024-065, EOII/NOII definitions) — https://www.sec.gov/files/rules/sro/nasdaq/2024/34-101620.pdf

**Comment letters**
- Hudson River Trading LLC, 28 Mar 2024 — https://www.sec.gov/comments/sr-nyse-2023-36/srnyse202336-451239-1158622.pdf
- Citadel Securities, 20 Mar 2024 — https://www.sec.gov/comments/sr-nyse-2023-36/srnyse202336-448299-1147782.pdf

**Academic**
- Boehmer, Saar & Yu (2005), "Lifting the Veil: An Analysis of Pre-trade Transparency at the NYSE," *Journal of Finance* LX(2)
- Madhavan, Porter & Weaver (2005), "Should Securities Markets Be Transparent?", *Journal of Financial Markets* — https://www.bankofcanada.ca/wp-content/uploads/2010/09/madhaven-porter-weaver.pdf
- Bogousslavsky & Muravyev (2023), "Who trades at the close? Implications for price discovery and liquidity," *Journal of Financial Markets* 66 — https://papers.ssrn.com/sol3/papers.cfm?abstract_id=3485840
- Goyal, Jegadeesh & Wu (2026), "Price Impact in Closing Auctions, Opening Auctions, and Continuous Markets," *JFQA* First View, DOI 10.1017/S0022109026102592
- **Admati, A. & Pfleiderer, P. (1991), "Sunshine Trading and Financial Market Equilibrium," *Review of Financial Studies* 4(3), 443–481** — the load-bearing theory on this exact question
- Madhavan, A. (1992), "Trading Mechanisms in Securities Markets," *Journal of Finance* 47(2), 607–641
- Madhavan, A. (1995), "Consolidation, Fragmentation, and the Disclosure of Trading Information," *RFS* 8(3), 579–603
- de Frutos, M.Á. & Manzano, C. (2014), "Market transparency, market quality, and sunshine trading," *JFM* 17, 174–198
- Barclay, M., Hendershott, T. & Jones, C. (2008), "Order Consolidation, Price Efficiency, and Extreme Liquidity Shocks," *JFQA* 43(1), 93–122 — **opening** cross
- Pagano, M. & Schwartz, R. (2003), "A closing call's impact on market quality at Euronext Paris," *JFE* 68(3), 439–484
- Pagano, M. & Schwartz, R. (2005), "Nasdaq's Closing Cross," *Journal of Portfolio Management* 31(4), 100–111
- **Hu, E. & Murphy, D. (2026), "Vestigial Tails? Floor Brokers at the Close in Modern Electronic Markets," *Management Science* 72(5), 3974–3996**
- Jegadeesh, N. & Wu, Y. (2022), "Closing auctions: Nasdaq versus NYSE," *JFE* 143(3), 1120–1139
- Chung, K.H., Chuwonganant, C. & Kim, Y. (2022), "Preopening price indications and market quality: Evidence from NYSE Rule 48," *Journal of Financial Research* 45(2), 205–228
- Derksen, M., Kleijn, B. & de Vilder, R. (2022), "Heavy tailed distributions in closing auctions," *Physica A* 593, 126959
- Eom, K.S., Ok, J. & Park, J.-H. (2007), "Pre-trade transparency and market quality," *JFM* 10(4), 319–341
- Comerton-Forde, C. & Rydge, J. (2006), *JFM* 9(2), 199–222 · Comerton-Forde, Lau & McInish (2007), *PBFJ* 15(1), 18–35
- Comerton-Forde, C. & Putniņš, T. (2014), "Stock Price Manipulation: Prevalence and Determinants," *Review of Finance* 18(1), 23–66 · (2011), *JFI* 20(2), 135–158
- Han, Q., Zhao, C., Chen, J. & Guo, Q. (2022), *Pacific-Basin Finance Journal* 74 (Shanghai closing call, null result)
- Gerace, D., Liu, Q. et al. (2015), *International Review of Finance* 15(2), 223–255
- Brunnermeier, M. & Pedersen, L. (2005), "Predatory Trading," *JF* 60(4), 1825–1863
- Bessembinder, H., Carrion, A., Tuttle, L. & Venkataraman, K. (2016), *JFE* 121(1), 142–166
- Petajisto, A. (2011), *Journal of Empirical Finance* · Chen, Noronha & Singal (2006), *FAJ* 62(4), 31–47 · Madhavan (2003), *FAJ* 59(4), 51–64 · Sammon & Shim (2026), *JFE* 177
- Lee, Riccò & Wang (2026), "Frequent batch auctions vs. continuous trading: Evidence from Taiwan," *JFM* (in press)
- Chan, K. & Yao, C. (2021), *The Effect of a Closing Auction on Market Quality and Market Efficiency in the Stock Exchange of Hong Kong*, HKIMR / Hong Kong Academy of Finance — https://www.aof.org.hk/docs/default-source/hkimr/applied-research-papers/yao_summary.pdf
- Lei, A. C. H., Ma, X. & Yick, M. H. Y. (2020), "Callable bull/bear contracts, call auction sessions, and price manipulations: Evidence from Hong Kong," *Journal of Futures Markets* 40(11), 1731–1750 `[abstract-level only]`
- Suen & Wan, *Sniping to Manipulate Closing Prices in Call Auctions* (SSRN) — on the **pre-2009** HKEX auction and the sniping the 2016 design was built to defeat

**Practitioner / exchange research**
- Mackintosh, P. (Nasdaq), "How Much Does the MOC Imbalance Matter?", 27 Sep 2019 — https://www.nasdaq.com/articles/how-much-does-the-moc-imbalance-matter-2019-09-27
- Castaneda-Dawkins & Bazinas (NYSE), "The NYSE Significant Imbalance", 4 Nov 2024 — https://www.nyse.com/data-insights/the-nyse-significant-imbalance-enhanced-trading-opportunities-at-the-nyse-closing-auction
- RBC Capital Markets, "Nasdaq — Early Look MOC", Oct 2019 — https://www.rbccm.com/assets/rbccm/docs/housing-market/Nasdaq_Close.pdf
- Dick, D. (CFA), "Imbalancing Acts", *CFA Magazine*, 1 Mar 2015 — https://rpc.cfainstitute.org/research/cfa-magazine/2015/imbalancing-acts
- Databento, "Introducing real-time NYSE imbalance data", 14 Aug 2025 — https://databento.com/blog/NYSE-imbalance-feeds
- Besson, P. & Quily, L. (Euronext), "Better market impact at the close with residual imbalance", 2 Apr 2026 — https://www.euronext.com/en/news/better-market-impact-close-residual-imbalance
- Aquis Exchange, "Introduction to trading mechanisms in the European equities market: Closing Auctions" — https://www.aquis.eu/news/closing-auctions
- Cboe, *Cboe Market Close FAQ* (Q1 2025) — https://cdn.cboe.com/resources/membership/Cboe_Market_Close_FAQ.pdf

**Press (HKEX natural experiment — secondary sources only)**
- South China Morning Post, 25 Jul 2016 — https://www.scmp.com/business/companies/article/1997712/hong-kongs-relaunched-closing-auction-session-avoids-chaos-will
- CNBC, 25 Jul 2016 — https://www.cnbc.com/2016/07/25/what-you-need-to-know-about-the-hong-kong-stock-exchange-restoring-closing-auctions.html
- Traders Magazine, "HKEx to Resume Closing Auction, Impose Temporary Price Limits" — https://www.tradersmagazine.com/news/hkex-to-resume-closing-auction-impose-temporary-price-limits/
