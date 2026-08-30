# CrossDesk — 4-minute pitch

Wed 2026-08-05 · 14:00 UTC · slot 18 of 18.

Every `>` line is spoken. `▸ SLIDE n` = advance the deck. Timing: `python docs/wordcount.py`

**Deck:** `docs/deck/CrossDesk_GrandFinal.html` — Chrome, **F** full screen, arrows to advance.
Present the HTML, **not** the PDF — the PDF cannot play your video.
Previous draft kept at `GRAND_FINAL_PITCH_SCRIPT.bak.md`.

### Three corrections I made to this version, and why

1. **"we already hold your real cBTC on our templates"** → the templates are **BitSafe's**, not
   ours, and the `CBTC` inside the demo fund is **self-issued** by `issuer-crossdesk`. The 4.16
   real cBTC is a separate CIP-56 registry claim. The original wording invites the one question
   you cannot answer.
2. **"the only venue"** → unverifiable, and Chainlink NAVLink is live with Fidelity
   International, UBS and Amundi. The relay-vs-asserter framing is stronger *and* defensible.
3. **"validators"** → on Canton a validator is a **node operator**. For signers say
   **counterparties**, or a Digital Asset judge hears something you did not mean.

---

## §1 · PROBLEM — 0:00–1:00

**▸ SLIDE 1 — title, up while they introduce you**
**▸ SLIDE 2 — the three vendors**

> "You've seen seventeen pitches today. I'll close this out with why index funds leak up to two
> billion dollars a year, and how we fix it.
>
> To launch a tokenised fund you hire three companies: an **exchange** for the price, an
> **administrator** to strike the NAV, a **custodian** to settle the shares.

**▸ SLIDE 3 — the three legs**

> When the fund trades at a discount, arbitrageurs close the gap. But that's **three legs on
> three venues**, carrying price risk between each. So they quote wide and only show up when the
> discount is big. **The fund's investors pay that.**

**▸ SLIDE 4 — the $1–2bn leak**

> So put the book on a public chain. That's worse — **the mempool IS the book.** Every resting
> order is visible before it executes. **Index funds leak an estimated one to two billion dollars
> a year to exactly this.**"

---

## §2 · SOLUTION & PRODUCT — 1:00–2:00

**▸ SLIDE 5 — one ledger, the flow**

> "CrossDesk is a trading venue and valuation engine for tokenised funds, built natively on
> Canton. It does the job of all three vendors in one place.

**▸ SLIDE 6 — Venue 4 · Alice 1 · Bob 1 · Auditor 0. Stop talking. Let them read it.**

> Our superpower is Canton's native privacy. CrossDesk gives you **dark pre-trade and lit
> post-trade** execution. The order book is invisible — **even to the auditor** — while orders
> rest. That is unbuildable on a public chain and unprovable in a centralised database.
>
> Three mechanisms. A **sealed closing auction** that discovers one uniform price. A **K-of-N
> committee** that attests a valuation *recipe* — not a static number — so the ledger keeps
> deriving the NAV. And **atomic in-kind creation and redemption** against that exact number.
>
> Plus cash-settled perpetuals, so a market maker hedges the NAV while it waits to redeem —
> turning the arbitrage into a fully-hedged basis trade."

---

## §3 · MARKET & BUSINESS MODEL — 2:00–3:00

**▸ SLIDE 7 — 0.325 bps vs 3 bps + $600k**

> "One hard truth, and it's the whole company.
>
> **Calculating** a NAV pays about **a third of one basis point** — Brown Brothers discloses it.
> **Publishing an official price** pays **three basis points plus six hundred thousand a year** —
> what the SPY trust pays S&P Dow Jones.
>
> **Same computation. Ten to fifteen times the money.**

**▸ SLIDE 8 — pricing and the bet**

> So I price like the benchmark. **The issuer pays me** — fifty to a hundred and fifty thousand
> flat, one to three basis points at scale. **Their administrator loses nothing:** it keeps the
> mandate, the fee, and becomes a signer — and can licence the venue itself.
>
> The market is **sixteen billion**, growing **four percent a month** — sixty a year. That's a
> **hundred and sixty billion in five years.**
>
> Chainlink and RedStone already put NAVs on-chain — but they only **deliver** a number one
> administrator decided. **We do all three in one place: discover the price, sign the NAV, and
> settle against it.**"

---

## §4 · DEMO & TEAM — 3:00–4:00

**▸ SLIDE 9 — the video. Autoplays muted. Let it run 5–10s before you speak.**

> "**Fifteen thousand lines of Daml, live on the HackCanton node.** Not a mockup.
>
> Alice creates ten shares of **LX1** — a **cETH, cBTC and money-market** fund — with the
> indicative price **above** the official NAV. She **shorts the perpetual** and takes the
> arbitrage with a **market-on-close**. The hedge **unwinds itself when the venue crosses.**

**▸ SLIDE 10 — four lanes · the CIP-56 claim · 121 test scripts · package 2.0.0**

> **Four of your six builder lanes**, live — order books, private OTC, leverage, treasury tools.
> On **cETH**, **cBTC** and the fund itself.

**▸ SLIDE 11 — team**

> I built this solo. I'm an ex-equities trader — **European closing auctions, index rebalances,
> merger arbitrage.** This is the exact mechanism I traded. I know where the friction is.

**▸ SLIDE 12 — the ask + the closing line behind you**

> My ask. **One pilot fund. Ninety days. Shadow mode.** I strike the NAV alongside whoever
> strikes it today. Nobody switches, nothing is at risk, and you end up with a **signed price
> history a regulator can read.**
>
>
> Anyone in this room issuing a fund on Canton — **I want the smallest one you have.**
>
> **Atomicity doesn't pay the trader. It pays the fund.** Thank you."

---

# JUDGE Q&A

### "Who are the N attestors, and why take the liability?"
**The one that can kill you.**
> "Nobody is hired — that's the point. The N are parties who already have a commercial reason to
> hold an opinion on that mark: the collateral taker about to lend against it, the issuer, the
> administrator, the market maker who quotes it. Attestation is a **byproduct of a position they
> already have**, not a service somebody buys. I'm not assembling a consortium of disinterested
> referees — that would never happen and I wouldn't fund it either."

### "Doesn't the administrator already do the NAV?"
> "They do the arithmetic, and they keep doing it — they keep the mandate and the fee. I don't
> replace them, I make them **one of the signers**. What I sell isn't the calculation, it's the
> **official price**: multi-party, signed on the ledger, and a *recipe* rather than a number."

### "Isn't this just Chainlink NAVLink / RedStone?"
> "Both are **relays**, and good ones — Chainlink is live with Fidelity International, UBS and
> Amundi; RedStone is the production oracle on Canton. But what they transport is still one
> administrator's assertion. **K-of-N changes who asserts, not how it travels.** I'm not
> competing for the transport layer. It's taken."

### "Is this on Canton or your laptop?"
> "The shared HackCanton node, right now. Package **crossdesk 2.0.0**, registered on the
> participant — the auditor cannot see the resting book on that node, and I can show you. And I
> claimed real BitSafe cBTC through the CIP-56 registry flow. To be precise: that's the registry
> integration. The instruments inside the demo basket I issued myself — I'm not going to tell you
> a hackathon fund is holding twenty real Bitcoin."

### "How big is this really?"
> "Small, today. Sixteen billion across every tokenised Treasury fund on earth — a five-to-seven
> million pool at index pricing. It has to grow ten to a hundred times, and that's the
> load-bearing assumption in everything I've said. I won't give you a Canton-specific number: no
> public tracker reports Canton, and anyone quoting you one is guessing."

### "What else do you sell besides the NAV?"
> "Three lines. **NAV-as-a-service** is the wedge — the issuer pays fifty to a hundred and fifty
> thousand flat, one to three basis points at scale, because it cannot launch without a credible
> price and it's the thing it can least do for itself.
>
> Then **execution** — basis points on notional crossed at the close, paid by the traders, not the
> issuer. That scales with **volume** rather than AUM, so it's a different curve entirely. To be
> straight with you: the fee leg isn't implemented yet. It's the model, not a running feature.
>
> Then a **platform licence** — an institution runs the venue for its own members, their parties,
> their committee, their book. That's how you stop selling fund by fund. It's also the last thing
> you sell, because nobody licenses a platform from a solo founder without a reference customer.
>
> And Canton app rewards are capped at a dollar fifty a transaction — about ten thousand a year at
> this scale. **A gas rebate, not a business model.**"

### "Then why is this a business?"
> "Two paths, and I'd take the second. Either tokenised funds grow ten to a hundred times, or I
> go where the mark is genuinely **doubted**. Hard-to-value fund administration pays **six to
> twelve basis points versus 0.325**. Nobody doubts a T-bill."

### "Who's the leverage for? Looks retail."
> "It's arbitrage plumbing. A fund's discount is a **ten-basis-point business** — uninvestable
> unlevered. Without a short instrument only existing holders can close a premium; a perp lets a
> market maker take the other side without owning the fund. And the perp price becomes a **live
> second opinion on the committee's NAV**."

### "Who absorbs the imbalance?"
> "No venue takes principal in a call auction — the heavy side is rationed pro-rata. The party
> with an obligation to offset is a designated provider, and we made that a **mandate**: you must
> commit to offset before you're allowed to see the imbalance. Today that number is a paywall, or
> one designated firm. **We made it a duty instead of a price.**"

---

## Do NOT claim

- ❌ **"The fund holds real cBTC."** The `CBTC` and `cETH` inside LX1 are **self-issued** by
  `issuer-crossdesk`. The **4.16 real BitSafe cBTC is separate** — their templates, CIP-56
  registry flow. Say *"I claimed real BitSafe cBTC through the CIP-56 registry"*, never *"the
  fund holds it"*.
- ❌ **"The only venue…"** — unverifiable.
- ❌ **"Validators"** for signers — on Canton that means node operators. Say **counterparties**.
- ❌ Holding **USYC** — KYC-gated, Reg S. It is *modelled* on its published parameters.
- ❌ Create-and-hedge as **one Daml transaction** — two submissions, one ledger.
- ❌ **Trading fees working** — the fee leg is not implemented. 
- ❌ Any **Canton AUM** figure. ❌ "$6 trillion on Canton" — that's repo flow; repo needs no NAV.
- ❌ Cross-margin, liquidity vaults, AMMs, RFQs, auto-deleveraging, permissionless liquidation.

---

# WHO'S IN THE VALUE CHAIN — and are we with them or against them

## Read this first — the whole thing in one paragraph

> **Three companies do this today.** An exchange discovers a price. An administrator strikes the
> NAV. A custodian settles the shares. Each hands a number to the next, and **the gaps between
> them are where the money leaks** — an arbitrageur closing a discount carries price risk across
> three venues, so he quotes wide and only shows up when the gap is big. The fund's investors
> pay that spread.
>
> **CrossDesk makes those three one ledger event.** The auction that discovers the price, the
> committee that signs the NAV, and the create/redeem that settles against it are the same
> system — so there is no gap to carry risk across.
>
> **Nobody else does all three.** Exchanges don't strike NAVs. Administrators don't run auctions.
> Oracles — Chainlink, RedStone — only *relay* a number one administrator already decided.
> **We change who decides it.**

## How to read the table

The value chain has one fee pool: the issuer's management fee. Everyone in rows 2–13 is paid
out of it, or out of the spread traders pay. So the only question that matters for each row is
**"does this firm lose money if CrossDesk exists?"**

- **Rows 1, 12, 13 — they pay us.** Issuers, collateral takers, and the hard-to-value end.
- **Row 2 — the administrator keeps everything.** We do not compete for the 0.325 bps. They
  keep the mandate and the fee, and become a signer. This is the single most important row:
  it is why the pilot is sellable at all.
- **Row 3 — this is the price we're arguing for.** Not the 0.325 an accountant gets, the 3 bps
  plus a flat fee a benchmark gets. Same computation, ten to fifteen times the money.
- **Rows 7–8 — they use us and pay in bps.** Cheaper arbitrage means tighter quotes, which is
  the fund's discount narrowing. That's the thing the issuer is actually buying.
- **Row 9 — the only real overlap**, and we deliberately concede the transport layer.
- **Rows 4, 5, 6, 10, 11 — untouched or absorbed.** Nobody in these rows is a threat or a target.

**So the one-liner is: we are the venue, the official-price provider and the settlement rail at
once — and that combination is what makes the arbitrage atomic and the NAV live.** Everything
else in the table is context for who reacts how.


If a judge is *from* one of these, this is the row you need. Every fee has a source.

| # | Role | Real names | What they charge | Us |
|---|---|---|---|---|
| 1 | **Sponsor / issuer** — creates the fund, owns the brand | BlackRock (BUIDL), Circle (USYC), Franklin Templeton (BENJI), Janus Henderson, Ondo | management fee — **9 bps** (SPY) to **50+ bps** (active/alt). Out of that they pay everyone below. | **CUSTOMER.** They pay us. |
| 2 | **Fund administrator / accountant** — strikes the NAV | BNY, State Street, Northern Trust, Citco, SS&C, Apex, BBH, U.S. Bancorp | **0.325 bps** — BBH Trust, Form N-CSR FY2023. *Fell 19% at renewal.* | **PARTNER, NOT TARGET.** They keep the mandate and the fee and become **one of the signers**. |
| 3 | **Index / benchmark administrator** — the official price | S&P Dow Jones, MSCI, FTSE Russell, Nasdaq | **3 bps + $600,000/yr** (State Street→S&P DJI for SPY). Take **~⅓ of every ETF management fee**. MSCI index segment: **76.4% adj. EBITDA margin** | **WHO WE PRICE LIKE.** The whole bet. |
| 4 | **Custodian** — holds the assets | BNY, State Street, Copper, Fireblocks, Anchorage | asset + transaction based; BBH custody **+** accounting together ≈ **0.85 bps** | Neutral. Untouched. |
| 5 | **Prime broker** — values and reconciles positions | Goldman, Morgan Stanley, Marex | spread + financing | Neutral. **This is who actually prices USYC's T-bills.** |
| 6 | **Transfer agent** — the share register | Computershare, Securitize | flat / per-holder | Partly absorbed — the token *is* the register. |
| 7 | **Authorised Participant** — creates/redeems | Jane Street, Flow Traders, Optiver, IMC | **the arb spread** they close | **USER.** We make their job cheaper, so they quote tighter. |
| 8 | **Market maker** — quotes the secondary | often the same firms as 7 | **bid-ask spread** | **USER.** The perp exists for them. |
| 9 | **Oracle / NAV relay** | **Chainlink NAVLink** (live: Fidelity Intl FILQ, UBS, Amundi) · **RedStone** (production oracle *on Canton*) · Pyth | Pyth Pro **$120K/yr** published. Chainlink & RedStone undisclosed. | **THE ONE WE OVERLAP.** Both **relay** one administrator's number. We change **who asserts**. Don't claim the transport layer — it's taken. |
| 10 | **Exchange / venue** — matching + the cross | NYSE, Nasdaq, Euronext, LSE; on Canton: Broadridge DLR, GS DAP | **bps on notional** | **WE'RE THE MECHANISM, NOT THE OPERATOR.** The licensed institution runs it. Nasdaq sells its matching engine without being the exchange. |
| 11 | **CCP / clearer** — novates every matched trade | LCH, Eurex Clearing, EuroCCP, DTCC | clearing fee per trade | **We model this** — the venue stands between both sides, so a confirm never names the contra. |
| 12 | **Collateral taker** — lends against fund shares | banks, repo desks, Canton's Global Collateral Network | interest; protects itself with a **haircut** | **THE REAL BUYER.** A better-attested mark argues for a smaller haircut. |
| 13 | **Hard-to-value administrator** — where marks are doubted | HedgeServ, Citco (alt funds) | **12 bps** first $250m → **6 bps** (Aetos/HedgeServ, SEC-filed) | **THE EXPANSION.** Same machinery, ~20x the price. Hamilton Lane, Fasanara are already in RedStone's Canton pipeline. |

### Where the money actually comes to us

| Line | Who pays | Price | Status |
|---|---|---|---|
| **NAV-as-a-service** — a private, auditable daily strike | the **issuer** (row 1) | **$50–150K flat** first partner → **1–3 bps + flat** steady state | **The wedge.** An issuer can't launch without a credible NAV. |
| **Execution** — bps on notional crossed at the close | traders (rows 7–8) | bps on notional | **NOT BUILT.** No fee leg in the Daml. Say it's the model, not that it runs. |
| **Platform licence** — an institution runs its own venue | market infrastructure (row 10) | annual licence | Last thing you sell — nobody licenses a platform from a solo founder without a reference customer. |
| Canton app rewards | the network | capped **$1.50/tx** → **~$10K/yr** at design-partner scale | **A gas rebate, not a business model.** Say so before they do. |

### The three sentences that survive any of these questions

1. **"I don't replace the administrator — I make them one of the signers."** Nobody loses a mandate, which is what makes the pilot sellable.
2. **"Chainlink and RedStone are relays. K-of-N changes who asserts, not how it travels."**
3. **"The buyer is whoever takes the shares as collateral, because a better mark is a smaller haircut."**
