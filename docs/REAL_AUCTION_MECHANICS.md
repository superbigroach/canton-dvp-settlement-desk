# How Real Closing Auctions Work — and Where CrossDesk Differs

**Purpose.** A mechanics specification for the four closing auctions CrossDesk is most often compared to, sourced to exchange rulebooks and official market-model documents, followed by an honest gap analysis against `daml/MarketOnClose.daml` as it stands today.

**Venues covered:** Nasdaq Closing Cross · NYSE Closing Auction · Xetra (Deutsche Börse) closing auction · Euronext closing auction.

**Rules of this document.**
- Every mechanical claim names the venue and the source. Nothing is averaged across venues.
- Where the venues genuinely differ, the difference is stated, not smoothed over. There is no such thing as "the" closing auction algorithm — Nasdaq, Xetra and Euronext each publish a *different* tie-break ladder, and the NYSE closing auction is not a pure algorithm at all.
- Anything not confirmed against a primary source is tagged `[UNVERIFIED]` and collected in §12.
- All US times are ET; Xetra/Euronext times are CET.

---

## 1. Order types accepted into the close

### 1.1 Nasdaq Closing Cross

| Order type | Priced? | Entry window | Cancel / modify | Notes |
|---|---|---|---|---|
| **MOC** (Market-on-Close) | **Unpriced** | Accepted from 4:00 a.m.; **must be received prior to 3:55 p.m.** | Modify/cancel only **prior to 3:50 p.m.** | Entered after 3:55 p.m. → **not accepted** (rejected, not queued). |
| **LOC** (Limit-on-Close) | Priced (limit) | Accepted from 4:00 a.m.; **must be received prior to 3:58 p.m.** | Modify/cancel only **prior to 3:50 p.m.** | An LOC received **after 3:55 p.m.** is accepted at its limit **unless** that limit is more aggressive than the 3:50 p.m. or 3:55 p.m. Reference Price, in which case it **re-prices to the more aggressive of the two**. If there is no crossing interest and therefore no 3:55 p.m. Reference Price, a post-3:55 LOC is **not accepted**. |
| **IO** (Imbalance-Only) | **Must be priced** — there is no market IO | Rule 4702(b)(13): from 4:00 a.m. **until the time of execution of the Closing Cross** | See `[UNVERIFIED-2]` | **Offset-only by construction**: an IO Order "may be executed only in the Nasdaq Closing Cross and only against MOC Orders or LOC Orders" (Rule 4702(b)(13)). IO orders **do not add to an imbalance**. Before the cross they are **re-priced to the best bid/offer (displayed or non-displayed) on the Nasdaq book**; after re-pricing they are treated as limit orders at the re-priced price but keep their **original entry timestamp for time priority**. IO orders **are** counted in Paired Shares. |
| **Continuous-book interest** | Both | — | — | The closing book and the Nasdaq continuous book are **brought together** into a single cross. Pegged and discretionary orders may participate **if they rest on the continuous book**; on-close and IO orders may **not** be pegged or discretionary. |
| **ETC** (Extended Trading Close) | — | — | — | A separate post-close session at the NOCP. An ETC Order executes **only during the ETC and only at the NOCP**; an "ETC Eligible LOC Order" is an LOC that did not fully execute in the cross and participates in the ETC if the NOCP is at or within its limit. Unexecuted ETC interest is cancelled. |

> The Crosses **do not access liquidity from other venues** — only interest on the Nasdaq book participates.
> Odd lots follow regular Nasdaq processing (no special auction handling).

*Sources: [Nasdaq Opening and Closing Crosses FAQ](https://nasdaqtrader.com/content/productsservices/trading/crosses/openclose_faqs.pdf) Q10, Q17, Q18, Q22–Q25; Nasdaq Equity 4 Rule 4702(b)(13); [SEC Release 34-98110 / SR-NASDAQ-2023-024](https://www.sec.gov/files/rules/sro/nasdaq/2023/34-97973-ex5.pdf) for Rule 4754(b)(3); [Federal Register, ETC order type](https://www.federalregister.gov/documents/2021/11/01/2021-23670/self-regulatory-organizations-the-nasdaq-stock-market-llc-notice-of-filing-of-amendment-no-1-and).*

### 1.2 NYSE Closing Auction

| Order type | Priced? | Entry window | Cancel / modify | Notes |
|---|---|---|---|---|
| **MOC** | **Unpriced** (a Market Order traded only in a closing auction, Rule 7.31(c)(2)(B)) | Entry until **3:50 p.m.**; after 3:50 p.m. **only on the contra side of a published MOC/LOC Significant Imbalance**, until 4:00 p.m. | **Cannot be cancelled, replaced or reduced in size after 3:50 p.m. — no exception, not even for a Legitimate Error** | The 3:50 p.m. boundary is the **Closing Auction Imbalance Freeze Time**, defined as *10 minutes before the scheduled end of Core Trading Hours* (Rule 7.35(a)(8)); it moves on half days. |
| **LOC** | Priced (Rule 7.31(c)(2)(A)) | Same as MOC | Same as MOC | — |
| **Closing IO Order** (formerly **Closing Offset / CO**) | Priced — **limit order only** | Same as MOC/LOC | Same as MOC/LOC | **Offset-only**: "executes only against the opposite side of an imbalance; never adds to an imbalance," and **yields to all other interest in the closing auction**. Per the fact sheet there is **no validation against any imbalance publication** — any size, price or side is accepted; the order simply does not participate if there is no Unpaired Quantity at the Auction Price (Rule 7.35B(h)(2)(D)(i)). **Naming caution:** NYSE renamed the Closing Offset Order to the **Closing IO Order** in Rule 7.31(c)(2)(D) effective 2022; the 2024 public fact sheet still uses the old "CO" label. |
| **Closing D Order** ("D-Quote") | Priced — **limit order only**, with an instruction to exercise discretion up (down) to a **designated undisplayed price** | **May only be entered by a Floor broker or a DMM.** Entry per the general requirement that all order instructions be entered by the end of Core Trading Hours (Rule 7.34(a)(2)(B)) — see `[UNVERIFIED-1]` | Not subject to the MOC/LOC freeze in the same way — a Closing D Order is processed as a Limit Order on arrival and **may trade or route before the Closing Auction** | **This is the timing asymmetry that matters.** Rule 7.35(b)(1)(C): a Closing D Order is ranked at **its limit price up to five minutes before the end of Core Trading Hours**, and at **its undisplayed discretionary price beginning five minutes before the end** — i.e. D-Order discretionary interest only becomes visible in the imbalance feed at **3:55 p.m.**, five minutes after MOC/LOC interest is frozen and published. |
| **DMM Auction Liquidity** | Non-displayed, auction-only, entered by the DMM manually or in the DMM's electronic auction message | May be entered **after the end of Core Trading Hours**, solely to offset Unpaired Quantity at the Closing Auction Price | — | **Never included in Auction Imbalance Information** (Rule 7.35(a)(4)). Not subject to Limit Order Price Protection when entered after Core Trading Hours. |

*Sources: [NYSE Opening and Closing Auctions Fact Sheet](https://www.nyse.com/publicdocs/nyse/markets/nyse/NYSE_Opening_and_Closing_Auctions_Fact_Sheet.pdf) (2024); [NYSE Regulatory Memo RM-22-08](https://www.nyse.com/publicdocs/nyse/markets/nyse/rule-interpretations/2022/Update_to_Rule_7.35B_-_Final.pdf) (June 17, 2022); [SR-NYSE-2021-45 Exhibit 5](https://www.sec.gov/files/rules/sro/nyse/2021/34-93037-ex5.pdf) (Rules 7.31, 7.35, 7.35B, 7.35C text); [SR-NYSE-2020-104 Exhibit 5](https://www.sec.gov/files/rules/sro/nyse/2021/34-91143-ex5.pdf) (Rule 7.35 definitions, Auction Reference Price table); [SEC Release 34-95086 / SR-NYSE-2021-74](https://www.sec.gov/files/rules/sro/nyse/2022/34-95086.pdf).*

### 1.3 Xetra closing auction

Xetra has **no dedicated MOC/LOC/IO order type**. Instead it has ordinary order types plus **trading restrictions**:

- **Market orders** — unpriced, and they participate in the closing auction.
- **Limit orders** — priced.
- **Trading restrictions** `auction only` and **`closing auction only`** — the functional equivalent of MOC/LOC designation.
- Orders and quotes carried over from continuous trading **also** participate; all available orders are concentrated into one order book at the start of the closing auction.
- **Iceberg orders and volume discovery orders participate with their full volume** in an auction (not just their peak) — a deliberate difference from continuous trading.
- **Resting BOC orders and GTX volume discovery orders are deleted at the start of the closing auction.**
- There is **no imbalance-only order type on Xetra.** Offsetting liquidity comes from Designated Sponsors, who are *obliged to participate in auctions and volatility interruptions*, not from a special order type.

*Source: [T7 Release 10.0 Market Model for the Trading Venue Xetra](https://www.cashmarket.deutsche-boerse.com/resource/blob/2762072/d8e4d932939c6826e4f8391b568e754c/data/T7_Release_10.0_-_Market_Model-_Xetra.pdf) §8.1.4, §6.*

### 1.4 Euronext closing auction

Euronext also has no MOC/LOC/IO type; it uses **validities**:

- **Market orders** — unpriced. "Market orders have priority over all other orders. In particular, **during uncrossing market orders have priority over orders limited at the uncrossing price**."
- **Limit orders** — priced.
- **Market-to-limit orders** — a market order whose unexecuted remainder rests at the uncrossing price.
- **VFU — Valid For Uncrossing**: processed during **any** uncrossing only (opening, closing or instrument uncrossing). Enterable throughout the day.
- **VFCU — Valid For Closing Uncrossing**: enterable throughout the day but **takes part in the closing uncrossing only**. Held inactive until the trading group enters the pre-closing call phase, at which point it becomes visible and contributes to the Indicative Matching Price.
- **No imbalance-only order type.** Euronext's equivalent lever is the collar-driven repeat-uncrossing described in §4.4.

*Source: [Euronext Trading Manual for the cash market](https://www.euronext.com/sites/default/files/2024-03/Trading%20Manual%20for%20cash%20market%20with%20mid-point%20orders%20CLEAN.pdf) §2.2.7, §2.2.8, §3.1.*

### 1.5 The pattern worth naming

| | Unpriced type in the close | Offset-only type | Discretionary / broker-privileged type |
|---|---|---|---|
| Nasdaq | MOC | **IO** (executes only against MOC/LOC) | — |
| NYSE | MOC | **Closing IO** (never adds to an imbalance; yields to all other interest) | **Closing D Order** (Floor broker / DMM only), **DMM Auction Liquidity** |
| Xetra | Market order (`closing auction only`) | none | Designated Sponsor obligation (not an order type) |
| Euronext | Market order (VFCU / VFU) | none | none in the central order book |

**Only the two US venues have a true offset-only order type**, and both define it the same way: it can consume an imbalance but can never create one. That is the exact economic role CrossDesk assigns to its Designated Liquidity Provider — but CrossDesk implements it as *selective information disclosure* rather than as an *order-type constraint*. Those are not the same thing, and §8 treats it as a distinct gap.

---

## 2. Price determination — the tie-break ladder, per venue

This is the section where blending the venues would produce a fiction. They are genuinely different.

### 2.1 Xetra — the fully specified ladder

Xetra publishes the most complete algorithm of the four. From the T7 Market Model §11.1.1, in order:

1. **Most executable volume.** "The auction price is determined according to the principle of most executable volume on the basis of the order book situation stipulated at the end of the call phase."
2. **Lowest surplus.** The price with the most executable volume **and the lowest surplus**.
3. **Surplus side (market pressure).** If more than one limit survives (1) and (2):
   - "The auction price is stipulated according to the **highest limit** if the surplus for **all** limits is on the **buy side** (bid surplus)."
   - "The auction price is stipulated according to the **lowest limit** if the surplus for **all** limits is on the **sell side** (ask surplus)."
4. **Reference price.** If the surplus does not resolve it — which happens when (a) there is a **bid surplus for one part of the limits and an ask surplus for another**, or (b) there is **no surplus for any of the limits** — then:
   - In case (a), first narrow to *"the lowest limit with an ask surplus or the highest limit with a bid surplus."*
   - Then, **in both cases**, clamp the reference price into the surviving range:
     - reference **≥ highest** surviving limit → auction price = **that highest limit**;
     - reference **≤ lowest** surviving limit → auction price = **that lowest limit**;
     - reference **strictly between** them → **auction price = the reference price itself**.
5. **Market orders only.** "If only market orders are executable against one another, they are matched **at the reference price**."
6. **No price.** "An auction price cannot be determined if orders are not executable against one another. In this case, the best bid and ask limits (if available) are displayed."

Two things to notice, because they are load-bearing for §8:

- **Step 4 can print at a price that is not any order's limit.** Xetra's final tie-break is a *clamp of the reference into the surviving interval*, not "the surviving candidate nearest the reference." When the reference sits inside the surviving range, the print **is** the reference.
- **The reference price is an exchange-published number**, not a participant input: it is the last traded price (dynamic reference price 1) or the last scheduled-auction / volatility-interruption price (static reference price 2). Its integrity is an exchange-governance property, not an algorithmic one.

### 2.2 Euronext — a shorter ladder, and a different step 3

From the Trading Manual §3.1, in order:

1. **Maximum Execution Principle** — "the uncrossing price is the price with the highest executable volume for each limit."
2. **Minimum Surplus** — "should the aforementioned process result in more than one limit with the highest executable order volume, the lowest surplus for each limit is taken into account as a further criterion."
3. **Reference Price** — "the uncrossing price will be the one **closest to the reference price**."

Plus the same two terminal rules as Xetra: **market orders only → match at the reference price**; **no executable orders → no price, display best bid/ask.**

**Euronext has no market-pressure step.** Where Xetra says "all-buy-surplus takes the highest limit," Euronext goes straight from minimum surplus to nearest-the-reference. And Euronext's step 3 selects *the nearest surviving limit*, whereas Xetra's step 4 can select the reference price itself. These are different algorithms that agree in most books and disagree in the corner cases — which is precisely where an auction's integrity is tested.

Euronext's reference price for the closing uncrossing is **the last traded price in the central order book** (for uncrossings during the main session); the previous day's closing reference price is used for the opening uncrossing.

### 2.3 Nasdaq — a *third* ladder, anchored on the inside midpoint, inside a hard band

From the Nasdaq FAQ Q19, "the Cross price is based on the following steps":

1. **Maximize the number of shares executed.**
2. **Minimize the imbalance of Cross orders.**
3. **Minimize the distance from the Nasdaq inside bid-ask midpoint.**

And, crucially, **the whole thing is constrained by the threshold band** described in §4.1 — the cross price must fall within a range built around the Nasdaq BBO.

The same three-step definition is used for the **Current Reference Price** disseminated in the NOII: *"A price within the Nasdaq Inside at which paired shares are maximized, the imbalance is minimized and the distance from the bid-ask midpoint is minimized, in that order."*

**Nasdaq's tertiary anchor is the inside bid-ask midpoint at the time of the cross — not the prior close, and not a reference/NAV price.** This is a materially different choice from Xetra and Euronext, and it is the reason Nasdaq needs a *live continuous market* to run its cross at all: two of the three steps depend on the current NBBO/QBBO.

There is a special variant: in the **LULD Closing Cross** (a security in a Limit State at the close), the cross occurs "at the price **within the benchmark prices** that maximizes the number of shares … [then] minimizes any Imbalance," and executes in **strict price/time priority rather than the priority ladder in Rule 4754(b)(3)**.

### 2.4 NYSE — not an algorithm at all

This is the finding most likely to be mis-stated in a pitch, so it is worth being blunt.

**The NYSE Closing Auction price is selected by a human — the Designated Market Maker — subject to two constraints.** NYSE Rule 7.35B(g), "Determining an Auction Price," verbatim:

> "The DMM is responsible for determining the Auction Price for a Closing Auction under this Rule. If there is an Imbalance of any size:
> (1) the DMM must select an Auction Price at which **all better-priced orders on the Side of the Imbalance can be satisfied**; and
> (2) if the Side of the Imbalance is to buy (sell), the Auction Price must be **at or above (below) the last-published Imbalance Reference Price** and **not above (below) the last-published non-zero Continuous Book Clearing Price**."

There is no maximum-volume rule, no minimum-surplus rule, and no reference-price tie-break in the NYSE closing auction. There is a **band** — `[Imbalance Reference Price, Continuous Book Clearing Price]`, oriented by the side of the imbalance — and a **satisfaction constraint**, and a DMM choosing inside it. Only when the DMM *cannot* facilitate does NYSE run an **Exchange-Facilitated Auction** under Rule 7.35C, which applies a mechanical **Auction Collar** (§4.2).

### 2.5 Side-by-side

| Step | Xetra | Euronext | Nasdaq | NYSE (DMM) |
|---|---|---|---|---|
| 1 | Max executable volume | Max executable volume | Maximize shares executed | — (DMM discretion) |
| 2 | Lowest surplus | Lowest surplus | Minimize imbalance | Must satisfy **all better-priced orders on the imbalance side** |
| 3 | Surplus side: all-bid → highest limit; all-ask → lowest limit | **(absent)** | Minimize distance from **Nasdaq inside bid-ask midpoint** | Price must lie between the last-published **Imbalance Reference Price** and the last-published non-zero **Continuous Book Clearing Price** |
| 4 | **Clamp reference price** into surviving range (may print *at* the reference) | Surviving limit **closest to the reference price** | — | — |
| All-unpriced book | Match at **reference price** | Match at **reference price** | MOC-vs-MOC resolved inside the threshold band | DMM discretion inside the band |
| Hard band on the print | No band — instead a **volatility interruption** extends the call (§4.3) | Collars → **reservation** and repeat uncrossing (§4.4) | **Yes**: greater of $0.50 or 10% of the QBBO midpoint (§4.1) | **Yes**: the 7.35B(g)(2) band; Auction Collar in 7.35C |

---

## 3. Imbalance dissemination

### 3.1 Nasdaq NOII (Net Order Imbalance Indicator)

**Start:** 3:50 p.m. for the Closing Cross (9:25 a.m. for the Opening Cross).

**Phase 1 — 3:50 p.m. to 3:55 p.m., every 10 seconds:**

| Field | Definition (verbatim from the Nasdaq FAQ) |
|---|---|
| **Current Reference Price** | "A price within the Nasdaq Inside at which paired shares are maximized, the imbalance is minimized and the distance from the bid-ask midpoint is minimized, in that order." |
| **Number of Paired Shares** | "The number of on-open or on-close shares that Nasdaq is able to pair off at the current reference price." |
| **Imbalance Shares** | "The number of opening or closing shares that would remain unexecuted at the current reference price." |
| **Imbalance Side** | "B = buy-side imbalance; S = sell-side imbalance; N = no imbalance; O = no marketable on-open or on-close orders." |

**Phase 2 — 3:55 p.m. to 4:00 p.m., every 1 second:** all of the above **plus**

| Field | Definition |
|---|---|
| **Near Indicative Clearing Price** | "The crossing price at which orders in the Nasdaq closing book **and continuous book** would clear against each other." |
| **Far Indicative Clearing Price** | "The crossing price at which orders in the Nasdaq closing book would clear against each other." (closing book only) |

**Distribution:** via **Nasdaq TotalView ITCH**, the Nasdaq Workstation, the Nasdaq DataStore, and market-data vendors/service bureaus. It is a **paid, subscribed product**, not a free public broadcast — but it is available to **anyone who subscribes**, i.e. non-discriminatory. This is the key point for §8: NOII is *commercially gated, not selectively disclosed*. Nasdaq does not show it to one favoured firm.

### 3.2 NYSE Closing Auction Imbalance Information

**3:50 p.m. (Closing Auction Imbalance Freeze Time):** systemic publication of the **MOC/LOC Significant Imbalance**. This is the trigger that opens the offset-only entry window.

**3:50 p.m. until the stock closes, every 1 second if changed from the previous publication**, the informational imbalance publication carries:

- **Paired quantity**
- **Unpaired quantity**
- **Total imbalance quantity**
- **Closing only interest price** — defined in Rule 7.35(a)(4)(D) as the **"Closing Interest Only Clearing Price": "the price at which all better-priced MOC and LOC Orders on the Side of the Total Imbalance can trade with both better-priced and at-priced contra-side MOC, LOC, and Closing IO Orders."** If there is no Total Imbalance or no MOC or LOC Orders, it equals the Imbalance Reference Price.
- **Continuous book clearing price**

**Explicitly excluded:** *"DMM Auction Liquidity is never included in Auction Imbalance Information"* (Rule 7.35(a)(4)), and DMM Orders are not eligible for the Closing Auction and are not included in its imbalance information.

**The D-Order asymmetry.** Because a Closing D Order ranks at its **limit price** until five minutes before the close and at its **undisplayed discretionary price** from then on (Rule 7.35(b)(1)(C)), Floor-broker discretionary interest only enters the published imbalance at **3:55 p.m.** MOC/LOC interest is frozen and published at 3:50 p.m.; D-Order discretion appears five minutes later, and is enterable by a privileged class of member only. This is the single largest *deliberate* information asymmetry in any of the four venues, and it is exactly the design question CrossDesk's `ImbalanceDisclosure` raises.

### 3.3 Xetra and Euronext — continuous indicative publication, to everybody

Xetra: throughout the call phase the order book is **partially closed**. Participants are shown:
- the **indicative auction price** when orders are executable; otherwise the **best bid/ask limit**;
- **Market Imbalance Information**: for a crossed book, "the executable volume for the indicative auction price, **the side of the surplus and the volume of the surplus**"; for an uncrossed book, the accumulated volumes at best bid and best ask.

Euronext: throughout the call phase the algorithm **recalculates and republishes the Indicative Matching Price (IMP)** as orders are added, cancelled or modified.

**Neither European venue has an offset-only order type, and neither has a privileged-broker order type in the central order book.** They compensate with *continuous, symmetric* indicative-price publication — everyone sees the same indicative price at the same time, and market makers with obligations (Designated Sponsors / Liquidity Providers) respond to it.

### 3.4 The dissemination design space, stated plainly

| Model | Who sees the imbalance | Venue |
|---|---|---|
| Continuous public indicative price + surplus side/size | Everyone in the market data feed | Xetra, Euronext |
| Staged public feed (10s → 1s), subscription-gated but non-discriminatory | Any subscriber | Nasdaq |
| Staged public feed **plus** a privileged order type whose interest enters the feed late | Everyone sees the feed; **Floor brokers and DMMs** have entry rights others don't | NYSE |
| **Aggregate disclosed to exactly one designated counterparty** | One party | **CrossDesk** |

CrossDesk's model is not on the real-world list. §9 argues about whether that is a defect or the product.

---

## 4. Price collars, bands and limits

This is the largest structural gap in the current implementation, and the four venues solve it in **two different families**.

### 4.1 Nasdaq — a hard band on the print (reject family)

> "Today the Opening and Closing Cross threshold is the greater of **$0.50 or 10%**."
> "10% of the Nasdaq Best Bid and Offer (QBBO) Midpoint (with a minimum of $0.50) is added to the Nasdaq Offer and subtracted from the Nasdaq Bid to establish the threshold price range."

Worked example from the FAQ: bid/offer $10.00 × $11.00 → midpoint $10.50 → threshold 10% × 10.50 = $1.05 → band = **$8.95 to $12.05**. *"$8.95 is the lowest price at which the Cross can occur and $12.05 is the highest price at which it can occur."*

**The band is dynamic** — it moves with the QBBO right up to the cross.

For **Exchange Traded Products**, the Closing Cross thresholds differ: **3% for ETPs > $50.01**, and **greater of 5% or $0.50 for ETPs ≤ $50.00**.

If a Nasdaq-listed ETP has **no closing cross at all**, the closing price falls back to a **time-weighted average of the NBBO midpoint over 15:58:00–15:59:55**, counting only quotes whose spread is no wider than 10% of the midpoint. That fallback is itself a useful pattern for a venue whose book may not cross.

### 4.2 NYSE — two different mechanisms depending on who runs the auction

**DMM-facilitated (the normal case), Rule 7.35B(g)(2):** the Auction Price must be **at or above (below) the last-published Imbalance Reference Price** and **not above (below) the last-published non-zero Continuous Book Clearing Price**, oriented by the side of the imbalance. This is not a percentage collar; it is a band whose endpoints are **two published prices derived from the book itself**. It says, in effect: *the print must be at least as far as the reference in the direction the imbalance is pushing, and no further than where the continuous book would clear.*

**Exchange-facilitated (Rule 7.35C), when the DMM cannot facilitate:** a mechanical **Auction Collar**, defined as "the price collar thresholds for the Indicative Match Price … based on a price that is a specified percentage away from the **Auction Reference Price** for the applicable auction." For the **Closing Auction**, the Auction Reference Price is the **Imbalance Reference Price as determined under Rule 7.35B(e)(3)**. The specific percentage tiers are `[UNVERIFIED-3]`.

**Extreme imbalance escape hatch, Rule 7.35B(j)(2).** To avoid closing-price dislocation from an order entered at or near the end of Core Trading Hours, the Exchange may temporarily suspend the entry deadline and open a **Solicitation Period**, during which — if the imbalance is to buy (sell) — it will accept **only sell (buy) Limit Orders with a limit at or better than the Exchange Last Sale Price**, will reject DMM Orders, and will **reject all other orders and all requests to cancel any order, regardless of when the original order was entered.** Orders entered in the Solicitation Period feed into the Continuous Book Clearing Price. This is a rulebook-level, explicitly offset-only, cancel-frozen liquidity solicitation — the manual version of what CrossDesk's DLP is meant to automate.

### 4.3 Xetra — no collar on the print; the call phase *extends* instead (extend family)

Xetra does not clamp the auction price. It **refuses to print** and re-opens the call:

> "The volatility interruption shall strengthen the price continuity of determined prices. Therefore, trading is interrupted by an additional unscheduled auction price determination according to the principle of most executable volume in case the potential next price would deviate too much from previously determined reference prices."

Two ranges, two reference prices:

- **Dynamic price range** around **reference price 1** — the **last traded price**, however determined (auction, continuous trading, or volatility interruption).
- **Static price range** (wider) around **reference price 2** — generally **the last price determined on the current trading day in a scheduled auction or a volatility interruption**; if unavailable, the last traded price from a previous day. Reference price 2 is only re-adjusted after a scheduled auction or volatility interruption, "so that the position of the static price range remains largely unchanged during trading."

**During an auction (§9.2.2):** "a volatility interruption is initiated if the potential auction price at the end of the call phase lies outside the dynamic and/or static price range." The effect is a **limited extension of the call phase**, during which participants may enter, modify or delete orders. Under the **Single Volatility Interruption Model** the extension has a **pre-defined minimum duration and a random end**, and prints if the potential price is inside a wider pre-defined corridor. Under the **ACE (Automated Corridor Expansion) Model** the extension is a sequence of **subsequently expanding price corridors**, each with a minimum duration and random end, up to a maximum number of corridors per instrument.

If a price still cannot be determined within the final corridor, the interruption becomes an **extended volatility interruption**, terminated **manually according to FWB exchange rules**. A volatility interruption may terminate automatically if the book is no longer executable.

Minimum volatility-interruption duration is a published per-segment parameter — **5 minutes for DAX, 2 minutes for MDAX/SDAX/other German shares** per the Xetra trading-parameters sheet.

### 4.4 Euronext — collars cause **reservation**, and the closing auction gets a special rule

General principle: *"Euronext will temporarily reserve trading or reject an order in a security if the buy or sell orders recorded in the Euronext Trading Platform would inevitably result in a price beyond a certain trading safeguard threshold referred to hereafter as collars."*

- **Dynamic collars** around a dynamic reference price (re-adjusted after each match), and **static collars** around a static reference price (the opening price, or the prior close if not yet traded).
- On breach during continuous trading, the security **reserves**; the reservation lasts **at least 3 minutes** (10 minutes for a second consecutive same-direction reservation in an index constituent — AEX, BEL20, CAC40, ISEQ 20, OBX, PSI). *"During the reservation period, members can enter, modify or cancel orders without matching. The re-opening of the security is done by uncrossing."*
- For securities **traded by uncrossing**: "if outstanding buy and sell orders are likely to result in trades that would breach the collars, Euronext initiates a reservation period on the security **until the next uncrossing**."
- **Specifically for the closing auction (§4.2.3):** *"For equities, the uncrossing process is **repeated with accelerated frequency and necessary threshold adjustment until an uncrossing price can be determined**, to the extent possible."* Euronext will not simply leave an equity unclosed — it widens and retries.
- **Penny stocks** (< €0.20): static collars set with a minimum of **±€0.02**, because a percentage band is meaningless at that price. A pure-percentage collar is wrong at the bottom of the price range — every venue that has one also has an absolute floor (Nasdaq's `$0.50`, Euronext's `€0.02`, NYSE's `$0.15`/`$1.00` variants).
- **ETFs/ETNs/ETVs:** collars are a band of **0.25% to 3%** (in 0.25% steps) around the **iNAV**, not around a traded price — and *"trading shall be halted in case of impossibility of updating the collars,"* e.g. if the iNAV cannot be computed. **A venue whose reference is a computed NAV must halt when the NAV cannot be computed.** That is directly relevant to CrossDesk's `NavFixing`.

### 4.5 The design lesson for CrossDesk

Two families:

| Family | Venues | Behaviour on a collar breach | Requires |
|---|---|---|---|
| **Reject / clamp** | Nasdaq, NYSE | The print simply cannot land outside the band | Nothing beyond the band definition |
| **Extend and re-solicit** | Xetra, Euronext | Call phase extends / security reserves; more orders come in; retry, possibly with a widened band | **A call phase, and time** |

CrossDesk has no call phase and no clock — its close is a single atomic transaction triggered by the operator. **Therefore the reject family is the only one it can implement today**, and it is also the safe one: a Daml `assertMsg` that aborts the whole close when `discoverPrice` returns a price outside the band is a two-line change with all-or-nothing semantics already guaranteed by the ledger.

---

## 5. Allocation when a side is over-subscribed

### 5.1 What each venue actually does

**Xetra:**
> "The price in auctions is determined according to the principle of most executable volume. **At the same time, price/time priority is valid so that the maximum of one order, which is either limited to the auction price or is unlimited, can be partially executed.**"

That sentence is the whole allocation rule. Orders priced *through* the auction price fill **in full**. At the marginal price level, orders fill in **time order**, and **exactly one order** ends up partially filled — the one that straddles the boundary. Everything behind it in time gets nothing.

**Euronext:**
> "Market orders, buy orders with a limit **above** the traded price and sell orders with a limit **below** the traded price are filled **in their entirety**, including the hidden-size quantity if any (**price priority**)."
> "In case of an imbalance between supply and demand, orders with a limit **equal** to the uncrossing price are filled on a **first-come/first-served basis (time priority)**."

Identical structure to Xetra: price priority through the print, then **pure time priority** at the print. Note Euronext's additional wrinkle: **market orders have priority over orders limited at the uncrossing price**, so unpriced interest jumps the at-price queue.

**Nasdaq — Rule 4754(b)(3),** verbatim structure. "If the Nasdaq Closing Cross price is selected and fewer than all MOC, LOC, IO and Close Eligible Interest would be executed, orders will be executed at the Nasdaq Closing Cross price in the following priority:"

- **(A) MOC orders, with time as the secondary priority** — unpriced on-close interest is allocated **first**, ahead of everything;
- **(B) Displayed Orders**, with **price as the primary priority** and, within each price level, **time as the secondary priority** — LOCs, IOs, limit orders, displayed size of Reserve Orders, other displayed interest;
- **(C) Non-Displayed Orders** (LOCs, limit orders, non-displayed size of Reserve Orders), **price primary, time secondary**.

So Nasdaq rations by a strict lexicographic ladder: **order class → price → display status → time.** No pro-rata anywhere.

**NYSE — Rule 7.35B(h):**

> "(1) **Better-priced orders**, including Yielding Orders and the reserve interest of Reserve Orders, entered by the Book Participant or a Floor Broker Participant, and Closing D Orders entered by either a Floor Broker or DMM, are **guaranteed to participate** in the Closing Auction at the Auction Price.
> (2) **At-priced orders and DMM Auction Liquidity are not guaranteed** to participate and will be allocated in the following order:
> (A) Priority 2 – Displayed Orders and Closing D Orders, allocated **on parity by Participant** pursuant to Rule 7.37(b)(2)–(7);
> (B) Priority 3 – Non-Displayed Orders, **on parity by Participant**;
> (C) LOC Orders, allocated **on time**;
> (D) Closing IO Orders opposite the Side of the Unpaired Quantity, **on time** — and they do not participate at all if there is no Unpaired Quantity at the Auction Price;
> (E) DMM Auction Liquidity;
> (F) display quantity of Priority 4 – Yielding Orders and Closing D Orders with a Yielding Modifier, **on time**;
> (G) non-display quantity of Priority 4 – Yielding Orders, **on time**."

NYSE is the only one of the four with a **parity** element — but note carefully what parity means: it is **parity by *Participant*** (each participating member firm gets an equal share of the round-lot allocation), and **a parity allocation to the DMM Participant is then allocated in price-time priority internally** (7.35B(h)(3)(B)). It is **not** pro-rata by order size, and it only applies to the at-price tier.

### 5.2 The universal invariant

**All four venues, without exception:**

> Orders priced **strictly through** the auction price fill **in full**. Rationing applies **only at the marginal (at-the-print) price level.**

They then differ only in how they ration that one level: time (Xetra, Euronext, Nasdaq's within-level rule, NYSE's LOC/IO/Yielding tiers), parity by member firm (NYSE's displayed and non-displayed tiers), or order class (Nasdaq's MOC-first rule).

**Pro-rata-by-size is a legitimate allocation family** — CME uses it for parts of its short-term-interest-rate complex, and several options venues use pro-rata or size-pro-rata hybrids. But **no cash-equity closing auction among these four uses it, and none of them — pro-rata venue or not — rations across price levels.** That distinction is the substance of Gap 1 in §8.

### 5.3 Round lots and residual rounding

- **Nasdaq:** "Odd lots follow regular Nasdaq processing" — no special auction handling. Separately, the NOCP is only propagated to the consolidated tape as the official Consolidated Last Sale Price where **one round lot or more** was executed in the Closing Cross.
- **NYSE:** parity allocation under Rule 7.37 operates in **round lots**, with the residual handled by the parity/priority sequence; the DMM Participant's parity share is then re-allocated internally in price-time order.
- **Xetra:** round lot / odd lot handling is a published per-segment trading parameter; matching quantities are integers.
- **Euronext:** matching quantities are integers; iceberg peaks execute on time priority, then remaining icebergs on total amount by time.

**The common property: every real venue allocates in whole units and has a deterministic, published rule for the residual.** None of them produce fractional fills, and none of them resolve a rounding residual by "whoever happens to be last in the list."

---

## 6. What happens to unfilled interest

**This is a genuine venue split — US cancels, Europe rests.**

| Venue | Unfilled on-close interest |
|---|---|
| **Nasdaq** | **Cancelled.** FAQ Q18: "If a firm sends in a MOO/MOC or LOO/LOC or IO orders that do not get executed, will they receive a cancellation message? **Yes.** A cancellation message will be returned to the firm after the cross occurs." (Pre-2023 this was explicit rule text at 4754(b)(3)(E): "Unexecuted MOC, LOC, and IO orders will be canceled" — `[UNVERIFIED-4]` on where that text now lives.) An unfilled **LOC** entered via RASH or FIX may qualify as an **ETC Eligible LOC Order** and get a second bite at the NOCP in the Extended Trading Close; unexecuted ETC interest is then cancelled. |
| **NYSE** | MOC, LOC, Closing IO and Closing D Orders are **Auction-Only Orders** — "a Limit or Market Order that is to be traded **only in an auction**" (Rule 7.31(c)). They do not rest into a later session. **DMM Orders** are explicitly "cancelled at the end of Core Trading Hours." |
| **Xetra** | **Rests.** "After price determination, non-executed or only partially executed orders are **transferred to the next trading day according to their validity**." Orders that are opted in and priced at or better than the closing auction price **roll over into Trade-at-Close**. Quotes and non-persistent orders are deleted at end of day. |
| **Euronext** | **Split by order type.** VFU and VFCU: "any unfilled remaining quantity … is **cancelled** at the end of the uncrossing, with a cancellation notice." But for securities **traded by uncrossing**, "if pure market orders … are not fully or partially executed during an uncrossing, **the remaining part will participate in the next uncrossing**," and a market-to-limit order not executed at all in an uncrossing **remains in the book for the next uncrossing**. |

CrossDesk's behaviour — everything archived at the close, nothing rests — is the **US model**, and §9 argues it is the correct one for a settlement venue.

---

## 7. Auction phases and timing

### 7.1 Nasdaq closing sequence (ET)

| Time | Event |
|---|---|
| 4:00 a.m. | On-close order acceptance begins |
| **3:50 p.m.** | **Cancel/modify cutoff for closing-cross orders.** NOII dissemination begins, every 10 seconds |
| **3:55 p.m.** | **MOC entry cutoff** (MOC after this is rejected). NOII goes to **1-second** frequency and adds Near/Far Indicative Clearing Price. Post-3:55 LOCs are accepted but **re-priced** against the 3:50/3:55 Reference Prices |
| **3:58 p.m.** | **LOC entry cutoff** (LOC after this is rejected regardless of TIF) |
| 4:00 p.m. | **Closing Cross executes.** Price disseminated to the consolidated tape immediately. NOCP set |
| +15 min | Nasdaq disseminates a trade message setting the NOCP as the official Consolidated Last Sale Price where the cross price differs and ≥1 round lot traded |

**No random end.** The Nasdaq cross fires at 4:00:00 p.m.

### 7.2 NYSE closing sequence (ET)

| Time | Event |
|---|---|
| 6:30 a.m. | Pillar gateways open |
| **3:50 p.m.** (**Closing Auction Imbalance Freeze Time** = 10 min before scheduled end of Core Trading Hours) | MOC/LOC entry cutoff. **All cancel/replace/reduce requests for MOC, LOC and Closing IO Orders are rejected from here to the close — no Legitimate Error exception** (RM-22-08). MOC/LOC Significant Imbalance published. Offsetting MOC/LOC and Closing IO entry permitted |
| **3:50 p.m. → close, every 1 second if changed** | Informational imbalance publication (paired, unpaired, total imbalance, closing-only interest price, continuous book clearing price) |
| **3:55 p.m.** (5 min before scheduled end) | **Closing D Orders switch from ranking at their limit price to ranking at their undisplayed discretionary price** — D-Order discretion enters the imbalance feed here |
| 4:00 p.m. | Closing process begins. DMM may enter DMM Auction Liquidity **after** the end of Core Trading Hours to offset Unpaired Quantity |

**No random end.** The freeze plus the DMM's manual conduct of the auction plays the anti-gaming role instead. The "Pre-Auction Freeze" — the moment the DMM begins the process — is part of the defined **Auction Processing Period** (Rule 7.35(a)(7)).

### 7.3 Xetra closing sequence (CET)

Structure: **continuous trading → closing auction (call phase → price determination) → Trade-at-Close → post-trading.**

Published parameters for German equities:

| Phase | DAX / MDAX / SDAX / other German shares |
|---|---|
| **Closing Auction Call Phase begins** | **17:30** |
| **Closing Auction Price** | **17:35** (earliest) |
| **End Trade-at-Close** | **17:40** |

> **"Auctions have a random end. Times stated are the earliest times of the end of an auction."** — Deutsche Börse Auction Schedule, verbatim.
> "All times are an approximation. Timestamps are meant as the earliest timestamp for the explicit trading phase change."

So the Xetra closing call phase is a **minimum 5 minutes with a randomised end**; the exact randomisation window is a published per-instrument parameter `[UNVERIFIED-5]`. There is **no order-entry freeze** — orders can be entered, modified and deleted throughout the call phase, right up to the (unknown) instant it ends. **The random end substitutes for a freeze**: you cannot time your cancellation to the last moment because you do not know when the last moment is.

**Trade-at-Close** follows automatically if the closing auction printed positive turnover: a continuous session where the **only possible price is the closing auction price**, matching is by **time priority only**, and **market orders do not have priority over limit orders** because a single price applies to everything. Opt-in at trader or order level; stop, trailing stop, OCO, iceberg and volume-discovery orders cannot participate.

### 7.4 Euronext closing sequence (CET, Paris equities)

Structure: **continuous trading → pre-closing call phase (order accumulation) → closing uncrossing → Trading-at-Last.**

> "All uncrossings take place on a **random basis** over a time period specified in the Appendix."
> "During the Uncrossing phase, **no order or quote may be modified, cancelled or entered**."

So Euronext has *both* a randomised uncrossing instant *and* an explicit no-entry/no-cancel window during the uncrossing itself.

Widely published Euronext Paris timings (secondary sources, `[UNVERIFIED-6]`): pre-closing call **17:30–17:35**, uncrossing randomised across **17:35:00–17:35:30**, **Trading-at-Last 17:35–17:40**. The **TAL price** is defined in the manual as "the last traded price of the Trading Day (either the uncrossing price of the last uncrossing or the price of the last transaction executed during continuous trading)."

### 7.5 The three anti-gaming devices

Every venue uses at least one; none uses none:

1. **A hard no-cancel freeze while entry stays open** — NYSE (3:50 → 4:00, offsetting entry only), Nasdaq (cancel closes 3:50, MOC entry until 3:55, LOC until 3:58).
2. **A randomised end to the call phase** — Xetra, Euronext.
3. **A brief total lockout at the moment of the cross** — Euronext's uncrossing phase; NYSE's Auction Processing Period / Pre-Auction Freeze.

The common structural idea in (1) is the one CrossDesk is missing: **cancellation closes *before* entry closes, and the entry that remains open is offset-only.** That asymmetry is what makes published-imbalance liquidity solicitation safe. If cancellation and entry close together, publishing an imbalance just tells people which way to run.

---

## 8. Mapping to `daml/MarketOnClose.daml`

### 8.1 What CrossDesk does today

Read from the current file, for the record:

- `discoverPrice` builds candidates from **every distinct `limitPrice` in the book plus the `referencePrice` anchor** (`dedup (referencePrice :: map limitPrice book)`).
- Scores each candidate with `crossAt`: `buyVol(P) = Σ qty of buys with limit ≥ P`, `sellVol(P) = Σ qty of sells with limit ≤ P`, `exec = min`, `imbalance = buyVol − sellVol` (signed).
- Ladder: **(1)** keep max `executedQty` → **(2)** keep min `abs imbalance` → **(3)** if **all** survivors are buy-heavy take the **highest** price, if **all** are sell-heavy take the **lowest**, else pass through → **(4)** `pickMinOn (abs (price − referencePrice), price)`.
- `RunClose` asserts book completeness (`length buys + length sells == submittedCount − cancelledCount`) and cid distinctness; re-derives `totalBuy`/`totalSell` from eligibility and asserts they equal `cross.buyVolume`/`cross.sellVolume`.
- Rations the heavy side **pro-rata by size across the entire eligible side**: `fill q = (q * matched) / totalHeavy`; the light side fills in full.
- Settles via pledge → pool (`mergeAll`) → `distribute`, where the **last recipient in the list receives the entire remaining pool**.
- Cancels away-from-the-cross orders on close (`VenueCancel`).
- `isOpen` is a single boolean; `CloseBidding` flips it; `WithdrawOrder` requires `isOpen`.

### 8.2 A genuine positive, stated precisely so it can be defended

**CrossDesk's four-step ladder is closer to Xetra's than the code comments claim, and the reason is worth being able to explain on stage.**

Xetra's step 4 is *clamp the reference into the surviving interval*, and CrossDesk's step 4 is *nearest surviving candidate to the reference*. Those look different. They coincide because **CrossDesk puts the reference price into the candidate set**. Sketch:

Let `L1 < L2` be adjacent surviving limits and let the anchor `R` satisfy `L1 < R < L2`. Since no order's limit lies strictly between them, `buyVol(R) = buyVol(L2)` and `sellVol(R) = sellVol(L1)`.
- **Mixed-surplus case** (Xetra's case (a)): `L1` is bid-heavy by `k` and `L2` is ask-heavy by `k`, both executing `V`. Then `buyVol(L2) = V` and `sellVol(L1) = V`, so `exec(R) = V` — tied on volume — and `imbalance(R) = 0`, which is **strictly better** than `k`. CrossDesk's step 2 therefore selects `R` outright. Xetra's step 4 also prints at `R`. **Same answer.**
- **Zero-surplus case** (Xetra's case (b)): every surviving limit has `buyVol = sellVol = V`. Then `exec(R) = V` and `imbalance(R) = 0`, so `R` survives to step 4 at distance 0 and wins. Xetra prints at `R`. **Same answer.**
- **Anchor outside the range:** `R` is not crossable at max volume so it is eliminated at step 1; CrossDesk's step 4 then picks the surviving endpoint nearest `R`, which is exactly Xetra's clamp. **Same answer.**

This is a real property and it is worth saying out loud, because it also exposes the honest caveat: **the anchor is a candidate, so a venue that controls the anchor does influence the print** — specifically, whenever the crossing interval has zero or mixed surplus, which is common. The file's comment that "an operator can no longer move the print by moving its own number" overstates it. The accurate claim is *the operator can only move the print to a price the book already supports at maximum volume*, and the mitigation for the residual influence is exactly the one already built: `fixingRef` binding the anchor to a K-of-N `NavFixing`. Xetra has the identical exposure and mitigates it the identical way — the reference price is exchange-published and audited.

**Correct the file's documentation, though:** the header says *"Every real closing cross (Nasdaq, Xetra, the NYSE close) prints the price that TRADES THE MOST SHARES."* **That is false for the NYSE close.** NYSE Rule 7.35B(g) has no volume-maximisation rule; a DMM selects the price inside a two-endpoint band. And Nasdaq's step 3 is the **inside bid-ask midpoint**, not a reference price. CrossDesk implements the **Xetra** ladder with a **Euronext**-shaped final tie-break. Say that; it is more impressive than the generic claim and it is true.

### 8.3 The gap table

| # | Real-world rule (venue + source) | What CrossDesk does today | Gap | What it takes to close it | Priority |
|---|---|---|---|---|---|
| **1** | **Orders priced through the print fill in full; only the at-the-print level is rationed.** Xetra: "price/time priority … the maximum of **one** order … can be partially executed." Euronext: buys above / sells below the print "are filled in their entirety (price priority)"; only at-price orders are rationed, by time. Nasdaq 4754(b)(3): class → price → display → time. NYSE 7.35B(h)(1): better-priced orders are "**guaranteed to participate**." | `buyFillOf q = (q * matched) / totalBuy` over **all** eligible buys, including orders priced far through the print | **Pro-rata across price levels.** A buyer at 105 and a buyer at 100 are rationed identically when the print is 100. A limit price buys eligibility but **no precedence**, so there is no incentive to price aggressively and none to arrive early — the only way to get filled is to **oversize**, and the stable strategy is to submit 2× what you want. Real venues deliberately refuse to create that equilibrium. | Split the heavy side into **through-the-print** (fill 100%) and **at-the-print** (ration). Then choose an at-price rule and publish it: **time priority** (Xetra/Euronext, matches the existing sealed-order model most naturally — the ledger already has a submission order) or **pro-rata by size** (defensible, but say it is a deliberate choice and say why). ~30 lines in `RunClose`; no template changes. | **P0 — the headline correctness fix** |
| **2** | **A price collar bounds the print.** Nasdaq: greater of $0.50 or 10% of the QBBO midpoint, "$8.95 is the lowest price at which the Cross can occur." NYSE 7.35B(g)(2): between the Imbalance Reference Price and the Continuous Book Clearing Price. Xetra: static + dynamic price ranges around two reference prices. Euronext: static + dynamic collars, with an absolute floor for penny stocks. | No band at all | **A thin book can print anywhere.** Two orders — a 1-unit buy at 10,000 and a 1-unit sell at 10,000 — produce a legitimate maximum-volume cross at 10,000 and a signed `SettlementBatch` calling it the official close. | Add `collarBps : Int` and `collarFloor : Decimal` to `ClosingAuction`; after `discoverPrice`, `assertMsg` that the price is within `referencePrice ± max(collarFloor, referencePrice * collarBps / 10000)`. **Include an absolute floor** — every venue that has a percentage band also has one (Nasdaq $0.50, Euronext €0.02, NYSE $0.15/$1.00). Abort semantics are already atomic. **~8 lines.** | **P0 — cheapest high-value fix in the list** |
| **3** | **An unpriced on-close order type exists** at all four venues (Nasdaq MOC, NYSE MOC, Xetra/Euronext market orders), and both European venues publish the rule for a book of nothing but unpriced orders: **match at the reference price**. | Every order carries a mandatory `limitPrice` (`SubmitOrder` asserts `limitPrice > 0.0`); everything is effectively LOC | **No MOC.** The venue cannot express "I want the close, whatever it is" — the single most-used institutional order type in the world. | **This is the same feature as Gap 2, and that is the point.** The reason CrossDesk cannot have an MOC is collateral: `SubmitOrder` reserves `quantity * limitPrice` for a buy, and an unpriced buy has no bound. **Once a collar exists, an unpriced buy has a bounded worst case — reserve `quantity * collarHigh`** and refund the change in the settlement transaction exactly as the pro-rated path already does. Then make `limitPrice : Optional Decimal`, treat `None` as always-eligible in `crossAt`, and add the all-unpriced → anchor rule. | **P1 — but sequence it immediately after Gap 2** |
| **4** | **A no-cancel freeze that opens *before* entry closes, with offset-only entry in between.** NYSE: cancellation dies at 3:50, offsetting MOC/LOC/Closing IO entry stays open to 4:00. Nasdaq: cancel dies 3:50, MOC entry to 3:55, LOC to 3:58, IO to the cross. Xetra/Euronext substitute a **random end**. | `isOpen : Bool`. `CloseBidding` closes entry and cancellation **in the same instant** | **The DLP feature does not actually work like a real one.** `PublishImbalance` requires `isOpen`, so the only window in which the DLP can offset is the same window in which every other participant can still withdraw. The published imbalance is therefore an exit signal as much as a liquidity signal — the opposite of what NYSE's freeze and Nasdaq's staged cutoffs are designed to achieve. | Replace `isOpen : Bool` with a three-state phase: `Open` (entry + withdrawal) → `Frozen` (**entry allowed, `WithdrawOrder` rejected**) → `Sealed` (neither). Gate `WithdrawOrder` on `Open`, `SubmitOrder` on `Open`/`Frozen`, `RunClose` on `Sealed`. Optionally restrict `Frozen` submissions to the side that offsets the last `ImbalanceDisclosure` — that would make CrossDesk's DLP order a genuine **Imbalance-Only order** rather than an ordinary order placed by a privileged party. Enum + guard changes only. | **P1 — small, and it is what makes the headline DLP feature honest** |
| **5** | **Fills are whole units and the residual has a published rule.** No venue produces fractional fills; NYSE re-allocates the DMM's parity share in price-time order; Nasdaq only tapes the NOCP where ≥1 round lot crossed. | `(q * matched) / totalHeavy` in Daml `Decimal` (fixed 10 dp). Fills generally do **not** sum to `matched`. `distribute` gives the **last** recipient the entire remaining pool | **Two concrete defects.** (a) **Silent residual.** 3 buys of 100 vs 1 sell of 100 → each fill `= 33.3333333333`, summing to `99.9999999999`; the asset pool holds 100, so **the last buyer in list order receives `33.3333333334`** while their `SettlementReceipt` says `33.3333333333`. The receipt and the ledger disagree, and who benefits is decided by list position. (b) **Dust aborts the whole close.** `PledgeToVenue` asserts `fillQty > 0.0`. A heavy-side order small enough that `q * matched / totalHeavy` rounds below `1e-10` gets `fillQty = 0.0`, the assert fails, and — because the close is one atomic transaction — **the entire auction fails to print.** A single dust order is a denial of service on the close. | Allocate in a declared **minimum tradable unit** (add `lotSize : Decimal` to the auction; the venue is already free of the tick-size problem in the *price* dimension, so fix the *quantity* dimension first). Compute integer lot fills, then distribute the residual by a **published rule** (largest fractional remainder, then submission order) rather than by list position. Separately, **allocate zero to dust and skip it** instead of aborting — filter `fillQty <= 0.0` out of the pledge list. | **P1 — (b) is a liveness bug and should be fixed regardless of the allocation redesign** |
| **6** | **A published minimum price increment.** Xetra prices on ESMA liquidity-band tick sizes; every venue has one. | `Decimal` prices with no tick constraint; the candidate grid is whatever limits participants submitted, plus the anchor | The printed close can be an arbitrary-precision number with no venue-defined grid. Not obviously exploitable — `exec(P)` is maximised at book limits, so a sub-tick candidate cannot win step 1 — but it means the venue has no publishable price grid and downstream systems have no representation guarantee. | Add `tickSize : Decimal`; validate submitted limits and the anchor against it. Given the grid is participant-defined today, this is cheap and it removes a class of "what price can actually print" questions from the judges. | **P2** |
| **7** | **Imbalance disclosure is symmetric or subscription-gated, never single-counterparty.** Nasdaq's NOII: any subscriber. Xetra/Euronext: everyone, continuously. NYSE: everyone sees the feed; the asymmetry is in *entry rights* (Floor broker / DMM), and DMM Auction Liquidity is explicitly excluded from the feed. | `ImbalanceDisclosure` — signatory operator, observer = **exactly one** designated LP | **No real venue does this.** NYSE comes closest, and even NYSE grants the privilege to a *class* of members bound by affirmative obligations, and never shows one firm a number it hides from the feed. | **This is a product decision, not a bug** — see §9. What it needs is not a code change but an *obligation*: a real DLP is a Designated Sponsor / DMM, i.e. it receives privileged information **in exchange for a quoting obligation**. Add an obligation the ledger can check (minimum offsetting size, or a penalty template) and the feature stops looking like favouritism and starts looking like NYSE. | **P2 as code; P0 as narrative** |
| **8** | **Every venue publishes a fallback for "the book did not cross."** Xetra: display best bid/ask, transfer orders to the next day; for designated instruments, optionally an "auction price without turnover" at the best bid/ask **midpoint**, if it does not deviate too far from the reference. Euronext: display best bid/ask; for the closing auction, **repeat the uncrossing with widened thresholds until a price can be determined**. Nasdaq: NOCP falls back to the last regular-way trade before 4:00, and for ETPs to a **TWAP of the NBBO midpoint over 15:58:00–15:59:55**. | `abort "there is no crossing volume"` — no close, no price, nothing recorded | The venue produces **no official price** on a no-cross day, and no ledger record that a close was attempted. For a settlement venue whose output is a NAV/official price that downstreams depend on, silence is worse than a documented fallback. | Emit a `NoCrossRecord` (or a `SettlementBatch` with zero fills and a documented fallback price — the anchor, or the mid of best bid/ask). Xetra's "auction price without turnover, if it does not deviate too much from the reference" is the closest published analogue and pairs naturally with the collar from Gap 2. | **P2** |
| **9** | **Unfilled interest handling is a published, venue-specific policy** — US cancels, Xetra rests to the next day, Euronext splits by order type. | Everything archives at the close (pledged orders are consumed; away-from-cross orders get `VenueCancel`) | **Not a gap — this is the US model and it is stated correctly in the file.** Worth naming as a deliberate choice rather than leaving it implicit. | Document it. Nothing to build. | **P3 — documentation only** |
| **10** | **Volatility interruption / extend-and-re-solicit.** Xetra extends the call phase; Euronext reserves and repeats the uncrossing with widened thresholds. | No clock, no call phase, no extension | Cannot be implemented without a time model, and the atomic single-transaction close is what gives CrossDesk its finality story. | **Do not build this.** See §9 — the reject family (Gap 2) is the right choice for this venue, and the extend family is an artefact of venues that must produce a price every day for a listed instrument. | **Do not build** |

---

## 9. Which gaps actually matter for a *settlement* venue

Opinionated, and justified.

### 9.1 Matters a great deal — fix before the final

**Gap 1, allocation across price levels.** This is not a market-structure nicety; it is an economic defect that a trader will identify in about fifteen seconds. Pro-rata across the whole eligible side means a limit price purchases eligibility and nothing else. The rational response is to inflate order size, which inflates the reported imbalance, which corrupts the one number the DLP is being shown. A settlement venue's entire value proposition is that the print is *fair given the book* — and an allocation rule that rewards oversizing corrupts the book itself. **The fix is cheap and it is the difference between "a call auction" and "a call auction that a desk would route to."**

**Gap 2, the collar.** A settlement venue's output is an *official price* — a NAV, a fixing, a number other contracts reference. A venue that will print anywhere when the book is thin cannot be the source of a fixing, because the thin-book case is exactly when someone would attack it. Note that both Euronext (for ETFs) and Xetra tie their collar to the reference price and **halt when the reference cannot be computed** — a rule that maps directly onto `fixingRef` and `NavFixing`. Eight lines of Daml. This should not survive to Wednesday.

**Gap 5(b), the dust abort.** A single sufficiently small order on the heavy side makes `fillQty` round to zero and takes down the entire close. On a venue whose selling point is atomic all-or-nothing settlement, an availability bug in the atomic path is a serious finding. Filter zero fills.

### 9.2 Matters, and is genuinely differentiating — build it because it makes the story true

**Gap 4, the three-state phase.** Not because closing auctions have call phases — because **CrossDesk's own headline feature does not work without it.** The pitch is "selective imbalance disclosure to a designated liquidity provider so the cross can clear without leaking to the market." But today the DLP can only act while everybody else can still withdraw. Every real venue that publishes an imbalance closes cancellation *first* and then permits **offset-only** entry. Implementing `Frozen` with offset-only submission turns CrossDesk's DLP order into a genuine **Imbalance-Only order** — which is precisely what Nasdaq Rule 4702(b)(13) and NYSE Rule 7.31(c)(2)(D) define — and makes the whole selective-disclosure narrative defensible rather than merely novel.

**Gap 3, the MOC.** Worth building not for completeness but because **the collar is what unlocks it**, and being able to say that on stage — *"the price band and the market order are the same feature; a collateralised venue can't accept an unpriced buy until it can bound the worst-case cash"* — demonstrates that the design is understood rather than copied.

### 9.3 Legitimately does not need it — and here is why

**Volatility interruptions and extended call phases (Gap 10).** These exist because a lit venue is the *primary price source* for a listed instrument and must produce a price every day; when the algorithm cannot, the answer is to buy time and re-solicit. CrossDesk's close is a single atomic Daml transaction — that is the finality guarantee it is selling, and inserting a "maybe it prints in five minutes, maybe manually per FWB rules" state destroys it. Xetra's own end-state here is *"terminated manually according to FWB exchange rules"* — a human. **A venue whose thesis is deterministic atomic settlement should abort and re-run, not extend.** Reject family, not extend family.

**Random end (§7.5 device 2).** A random end defeats last-instant cancellation. CrossDesk defeats it more strongly and more cheaply: `WithdrawOrder` is a *ledger* operation gated by phase, so once the venue seals the book, withdrawal is not merely ill-timed, it is **impossible** — enforced by the Daml authorisation model rather than by a rulebook and a millisecond timer. This is a case where the Canton primitive is strictly better than the market-structure workaround, and it should be said that way.

**Round lots (part of Gap 5).** Round lots are a *legacy of physical certificates and quote display*, not a fairness requirement. A settlement venue for tokenised assets has no reason to inherit them. But **the residual rule they exist to make deterministic is a real requirement**, and CrossDesk currently resolves the residual by list position. Adopt a minimum tradable unit and a published residual rule; do not adopt 100-share lots.

**Continuous-book integration.** Nasdaq's cross "brings together" the closing book and the continuous book, and two of Nasdaq's three price-determination steps depend on the live NBBO. CrossDesk has no continuous book, which is why the **Xetra/Euronext** ladder (anchored on a reference price) is the correct model and the **Nasdaq** ladder (anchored on the inside midpoint) is not implementable here. That is a reason the current design is right, and it should be stated as a choice rather than left as an accident.

**Single-counterparty imbalance disclosure (Gap 7).** No real venue does it, and that is the product — Canton's per-contract visibility makes possible something a lit venue and a transparent chain both cannot express. But the honest framing is **NYSE's**: privileged information in exchange for an **obligation**. NYSE grants Floor brokers and DMMs entry rights and late-arriving discretionary interest, and in return the DMM carries an affirmative obligation to facilitate the close. A DLP with information and no obligation is not a market-structure innovation; it is a conflict of interest. **Attach an obligation to the disclosure and the feature is defensible on a market-structure panel.**

---

## 10. Three sentences for the stage

When a judge asks *"how does this compare to a real closing auction?"*:

> **1.** "The real gap first: we ration the whole heavy side pro-rata by size, and no cash-equity closing auction does that — Nasdaq, Xetra and Euronext all fill everything priced *through* the print in full and ration only the marginal price level, by time, and NYSE rations that level on parity by member firm; that is a thirty-line fix and it is next.

> **2.** What we did implement faithfully is the price formation — maximum executable volume, then minimum surplus, then market pressure, then the reference price — which is the **Xetra** ladder, section 11.1.1 of the T7 market model, with Euronext's nearest-the-reference tie-break; we deliberately did not copy Nasdaq, whose tertiary anchor is the live inside midpoint we don't have, or NYSE, where a human DMM picks the price inside a two-endpoint band.

> **3.** The thing we'd build first is the price collar, because it turns out to be the *same* feature as the market-on-close order: every venue bounds the print — Nasdaq at the greater of fifty cents or ten percent of the quote midpoint — and once you have that band, an unpriced buy has a bounded worst-case cash requirement and you can finally collateralise it, which is the actual reason every order in our book carries a limit today."

---

## 11. Sources

**Nasdaq**
- [The Nasdaq Opening and Closing Crosses — Frequently Asked Questions](https://nasdaqtrader.com/content/productsservices/trading/crosses/openclose_faqs.pdf) (Nasdaq, © 2025) — order types, cutoffs, thresholds, NOII fields and frequency, three-step price determination, unexecuted-order cancellation.
- Nasdaq Equity 4, **Rule 4754** (Nasdaq Closing Cross), text as amended by [SR-NASDAQ-2023-024, Exhibit 5](https://www.sec.gov/files/rules/sro/nasdaq/2023/34-97973-ex5.pdf) — Rule 4754(b)(3) allocation ladder.
- Nasdaq Equity 4, **Rule 4702(b)(13)** (Imbalance Only Order) — IO entry window; executes only in the Closing Cross and only against MOC/LOC Orders.
- [Federal Register, SR-NASDAQ-2021-036 — Extended Trading Close and ETC Order Type](https://www.federalregister.gov/documents/2021/11/01/2021-23670/self-regulatory-organizations-the-nasdaq-stock-market-llc-notice-of-filing-of-amendment-no-1-and).
- [Federal Register, SR-NASDAQ-2021-013 — Rule 4754 and the LULD Closing Cross](https://www.federalregister.gov/documents/2021/03/03/2021-04307/self-regulatory-organizations-the-nasdaq-stock-market-llc-notice-of-filing-of-proposed-rule-change); approval [SEC Release 34-94293 (2022)](https://regulations.justia.com/regulations/fedreg/2022/02/24/2022-03876.html).

**NYSE**
- [NYSE Opening and Closing Auctions Fact Sheet](https://www.nyse.com/publicdocs/nyse/markets/nyse/NYSE_Opening_and_Closing_Auctions_Fact_Sheet.pdf) (ICE, 2024) — order types, closing timeline, imbalance publication fields.
- [NYSE Regulatory Memo RM-22-08, "Amendments to NYSE Rule 7.35B"](https://www.nyse.com/publicdocs/nyse/markets/nyse/rule-interpretations/2022/Update_to_Rule_7.35B_-_Final.pdf) (June 17, 2022) — MOC/LOC/Closing IO no longer cancelable or changeable after 3:50 p.m., no Legitimate Error exception.
- [SR-NYSE-2021-45, Exhibit 5](https://www.sec.gov/files/rules/sro/nyse/2021/34-93037-ex5.pdf) — Rule 7.31(c)(2) order types incl. Closing D Order; Rule 7.35(b)(1)(C) D-Order ranking-price switch at five minutes; Rule 7.35B(g) Auction Price determination; Rule 7.35B(h) Auction Allocation; Rule 7.35B(j)(2) Solicitation Period; Rule 7.35C Exchange-Facilitated Auctions.
- [SR-NYSE-2020-104, Exhibit 5](https://www.sec.gov/files/rules/sro/nyse/2021/34-91143-ex5.pdf) — Rule 7.35(a)(4)(D) Closing Interest Only Clearing Price; Rule 7.35(a)(7) Auction Processing Period / Pre-Auction Freeze; Rule 7.35(a)(8) Closing Auction Imbalance Freeze Time; Rule 7.35C(b)(1) Auction Reference Price table.
- [SEC Release 34-95086 / SR-NYSE-2021-74](https://www.sec.gov/files/rules/sro/nyse/2022/34-95086.pdf) — approval of the 2022 Rule 7.35B cancellation amendments.

**Xetra / Deutsche Börse**
- [T7 Release 10.0 — Market Model for the Trading Venue Xetra](https://www.cashmarket.deutsche-boerse.com/resource/blob/2762072/d8e4d932939c6826e4f8391b568e754c/data/T7_Release_10.0_-_Market_Model-_Xetra.pdf) (25/10/2021) — §8.1.4 closing auction, §8.1.5 Trade-at-Close, §9 safeguards / volatility interruptions, §11.1.1 basic auction matching rules and the full tie-break ladder.
- [Auction Schedule — Xetra](https://www.cashmarket.deutsche-boerse.com/resource/blob/4758656/53415ecf604c18ecf5486d49efcde431/data/Auction%20schedule-deutscheboerse-xetra.pdf) — "Auctions have a random end. Times stated are the earliest times of the end of an auction."
- [Trading Parameters — Deutsche Börse Xetra](https://www.cashmarket.deutsche-boerse.com/resource/blob/250890/43c50e657b0ff5fb58132fbf6e063517/data/trading-parameters-xetra.pdf) — closing auction call phase 17:30, closing auction price 17:35, end Trade-at-Close 17:40; minimum volatility-interruption duration by segment; ESMA tick-size bands.

**Euronext**
- [Trading Manual for the cash market (with mid-point orders)](https://www.euronext.com/sites/default/files/2024-03/Trading%20Manual%20for%20cash%20market%20with%20mid-point%20orders%20CLEAN.pdf) (Euronext, 2024) — §1.3.4–1.3.5 pre-closing call phase and closing uncrossing, §2.2.7–2.2.8 VFU/VFCU, §3.1 uncrossing price determination and allocation, §4.1–4.2 reference price, collars and reservations, §4.2.3 management of collars during the closing auction.

**CrossDesk**
- `C:\Users\sborj\Desktop\hackcanton-ceth-settlement\daml\MarketOnClose.daml` (read-only for this document)
- `C:\Users\sborj\Desktop\hackcanton-ceth-settlement\daml\Holding.daml` — `Split` (asserts `0 < splitAmount < amount`) and `deliverExact`, referenced in Gap 5.

---

## 12. `[UNVERIFIED]` register

| Tag | Claim | Status |
|---|---|---|
| `[UNVERIFIED-1]` | **NYSE Closing D Order entry cutoff of 3:59:50 p.m.** This figure is widely repeated in market-structure commentary but does **not** appear in NYSE Rule 7.31(c)(2)(C), 7.35, 7.35B or the NYSE fact sheet. What the rules **do** say is that all order instructions must be entered by the end of Core Trading Hours (Rule 7.34(a)(2)(B), referenced in 7.35B(j)(2)), and that a Closing D Order ranks at its discretionary price beginning five minutes before that end. **Do not assert 3:59:50 on stage without a rulebook citation.** | Not confirmed against a primary source |
| `[UNVERIFIED-2]` | **Nasdaq IO Order cancel/modify cutoff.** Secondary sources state IO Orders may not be cancelled or modified at or after **3:55 p.m.** The Nasdaq FAQ says only (Q25) that orders may be modified or cancelled for the closing cross "prior to 3:50 p.m." Rule 4702(b)(13) confirms the **entry** window runs to the time of execution of the cross but was not retrieved in full. | Conflicting; FAQ 3:50 figure treated as authoritative |
| `[UNVERIFIED-3]` | **NYSE Auction Collar percentage tiers for the Closing Auction.** Secondary sources report 10% for Core Open / Trading Halt Auctions and **5% for Closing Auctions** where the Auction Reference Price is ≤ $25.00, and a "greater of $0.15 or 10%" formulation elsewhere. The definitional text and Auction Reference Price table were confirmed from Rule 7.35C; the **percentage table itself** was not retrieved. | Structure confirmed; percentages not confirmed |
| `[UNVERIFIED-4]` | **Where the text "Unexecuted MOC, LOC, and IO orders will be canceled" now lives.** It was Rule 4754(b)(3)(E) and was struck by SR-NASDAQ-2023-024. The *behaviour* is confirmed by FAQ Q18 (a cancellation message is returned after the cross); the current rule-text location was not traced. | Behaviour confirmed; rule citation stale |
| `[UNVERIFIED-5]` | **The exact randomisation window of the Xetra closing-auction call phase.** Deutsche Börse states the random end exists and that published times are the *earliest* end times; the specific randomisation interval is an instrument-level parameter that was not retrieved. | Existence confirmed; magnitude not |
| `[UNVERIFIED-6]` | **Euronext Paris closing-auction clock times** (pre-closing 17:30–17:35, uncrossing randomised 17:35:00–17:35:30, TAL 17:35–17:40). The Trading Manual confirms the *structure* and that "all uncrossings take place on a random basis over a time period **specified in the Appendix**," but the Appendix is a separate document that was not retrieved. Times above are from secondary sources. | Structure confirmed from primary; clock times secondary |
