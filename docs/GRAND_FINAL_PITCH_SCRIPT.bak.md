# CrossDesk — 4-minute pitch

Wed 2026-08-05 · 14:00 UTC · slot 18 of 18.

Every `>` line is spoken. `[beat]` = one full second, mouth closed. **Bold** = hit it.
`▸ SLIDE n` = advance the deck. Timing: `python docs/wordcount.py`

**Deck:** `docs/deck/CrossDesk_GrandFinal.html` — open in Chrome, **F** full screen, arrows or
click to advance, **P** for a PDF backup. 12 slides. Present the HTML, not the PDF — the PDF
cannot play the video.

---

## §1 · PROBLEM — 0:00–1:00

**▸ SLIDE 1 — title. Already up while they introduce you. Advance as you open your mouth.**

**▸ SLIDE 2 — "two vendors before you've done any business"**

> "You've just tokenised a fund on Canton. It holds cETH and cBTC. Monday morning — it goes live.
>
> Someone asks what a share is worth.
>
> You don't know. Nothing has traded yet. [beat]
>
> So you hire an administrator to strike a NAV once a day. And an oracle to carry it on-chain.
> **Two vendors — before you've done any business.**

**▸ SLIDE 3 — the three legs**

> Tuesday. The shares trade two percent under what the fund holds. Your investors want to know
> why.
>
> The fix is supposed to be automatic. An arbitrageur buys the cheap shares, redeems them for
> the cETH and cBTC inside, sells those. **Three trades. Three venues. Exposed the whole way.**
>
> So he charges for that risk. And he only shows up when the gap is already wide. [beat]
>
> **The discount stays. Your investors pay it.**

**▸ SLIDE 4 — the $1–2bn leak**

> And on a public chain it's worse — the mempool **is** the book. Index funds leak an estimated
> **one to two billion a year** to it."

---

## §2 · SOLUTION & PRODUCT — 1:00–2:00

**▸ SLIDE 5 — the flow: mint → close → one price → no leg risk**

> "CrossDesk is that whole Monday on one ledger. [beat]
>
> The price isn't struck by a vendor. It's **discovered**. A sealed closing auction — everyone
> lodges orders nobody can see, and at the close the venue uncrosses the entire book at one
> price. **The orders decide it. Not me.**
>
> What has no market, a **K-of-N committee** attests — signing the *recipe*, not the number. So
> the ledger derives the value every second after.
>
> And those three trades? **One desk.** He mints shares in-kind at the signed NAV and sends them
> into the close as a market-on-close order. Same ledger. **No leg risk. Nothing to charge for.**
>
> So he quotes it tight — **and the discount closes.** [beat]

**▸ SLIDE 6 — Venue 4 · Alice 1 · Bob 1 · Auditor 0. Stop talking for a second. Let them read it.**

> The superpower is Canton itself: a contract is visible only to its signatories. **A sealed
> order isn't a trick here. It's the default.**"

---

## §3 · MARKET & BUSINESS MODEL — 2:00–3:00

**▸ SLIDE 7 — 0.325 bps vs 3 bps + $600k, side by side**

> "Here's TradFi today. [beat]
>
> You pay your fund manager **nine basis points**. He pays an index provider **three of them,
> plus six hundred thousand a year** — just for the right to reference a number. And he pays a
> fund accountant **nought point three two five** to actually calculate the NAV.
>
> Three companies. Three fees. **One number.** Index providers alone take **a third of every ETF
> management fee on earth.** [beat]

**▸ SLIDE 8 — the pricing and the bet**

> With CrossDesk, one ledger produces all three. And I price like **the index provider — not the
> accountant.**
>
> That's the whole bet. Same computation, **ten to fifteen times the money**, depending which
> side of that line I land on. **I'll know which when somebody pays me.**"

---

## §4 · DEMO & TEAM — 3:00–4:00

**▸ SLIDE 9 — the video. It autoplays, muted, when you land. Talk over it. Never narrate clicking.**

> "This is running right now — on the shared Canton node. [beat]
>
> That's the order book, viewed as the **auditor**. **Empty.** He sees every trade the second it
> prints — and **not one order before**.
>
> That's shares minted at the signed NAV, hedged straight into the close.

**▸ SLIDE 10 — four lanes, the CIP-56 claim, 213 tests, package 2.0.0**

> And I claimed **real BitSafe cBTC** through the CIP-56 registry flow — four point one six,
> on their templates. That integration is done.
>
> **Four of your six builder lanes** — one fund needs all four, and one ledger is the only place
> they compose. [beat]

**▸ SLIDE 11 — team**

> It's me. Solo. I traded this for a living — closing auctions, index adds and deletes, the
> imbalance. **I've been the guy on the other side of a fund everybody could see coming.**
> That's the judgement. The code I can hire.

**▸ SLIDE 12 — the ask, and the closing line is on the slide behind you**

> Give me **one pilot fund.** Ninety days, shadow mode.
>
> **Atomicity doesn't pay the trader. It pays the fund.** [beat] Thank you."

---

# JUDGE Q&A — prep

Answers only. `>` = say it roughly like this.

### "Who are the N attestors, and why take the liability?"
**The one that can kill you.** Answer:
> "Nobody is hired — that's the point. The N are parties who already have a commercial reason
> to hold an opinion on that mark: the collateral taker about to lend against it, the issuer,
> the administrator, the market maker who quotes it. Attestation is a **byproduct of a position
> they already have**, not a service somebody buys. I'm not assembling a consortium of
> disinterested referees — that would never happen and I wouldn't fund it either."

### "Doesn't the administrator already do the NAV?"
> "They do the arithmetic, and they keep doing it — they keep the mandate and the fee. I don't
> replace them, I make them **one of the signers**. What I sell isn't the calculation, it's the
> **official price**: multi-party, signed on the ledger, and a *recipe* rather than a number, so
> value keeps deriving between strikes instead of going stale for 24 hours."

### "Isn't this just Chainlink NAVLink / RedStone?"
> "Both are **relays**, and good ones — Chainlink is live with Fidelity International, UBS and
> Amundi; RedStone is the production oracle on Canton. But what they transport is still one
> administrator's assertion, delivered safely. **K-of-N changes who asserts, not how it
> travels.** I'm not competing for the transport layer. It's taken."

### "What's this worth — 0.325 bps or 3 bps?"
> "That's the whole company in one question. Calculating a NAV pays **0.325 basis points** —
> Brown Brothers discloses it, and it fell 19% at the last renewal. An **official price** pays
> **3 bps plus six hundred thousand a year** — what State Street pays S&P Dow Jones for SPY.
> Same computation, ten to fifteen times the money, depending purely on which box a buyer files
> me under. **I think it's the second. I'll know when somebody pays me.**"

### "How big is this really?"
> "Small, today. Every tokenised Treasury fund on earth is **sixteen billion**. At index pricing
> that's a **five-to-seven-million-dollar** annual pool worldwide. It has to grow ten to a
> hundred times for this to matter — it's growing about 4% a month — and **that is the
> load-bearing assumption in everything I've told you.** I won't give you a Canton-specific
> number: no public tracker reports Canton, rwa.xyz doesn't even list it among USYC's networks
> and USYC launched there. Anyone quoting you a Canton AUM figure is guessing."

### "Then why is this a business?"
> "Two paths, and I'd take the second. Either tokenised funds grow ten to a hundred times, or I
> go where the mark is genuinely **doubted**. Administration of hard-to-value funds pays **six
> to twelve basis points versus 0.325** — the market pays for valuation *difficulty*, not
> valuation. Private credit is the same machinery worth twenty times more. Nobody doubts a
> T-bill."

### "What would you charge?"
> "Fifty to a hundred and fifty thousand flat for a first design partner — anchored to **Pyth
> Pro at $120K/yr**, the only published rate card in institutional market data. Steady state,
> **one to three bps plus a flat fee**: the standard index-licensing form, deliberately under
> the three-to-four-point-four S&P and MSCI command, because I don't have their brand."

### "Who's the leverage for? Looks retail."
> "It's arbitrage plumbing. A fund's discount is a **ten-basis-point business** — uninvestable
> unlevered, which is why arb is a levered trade everywhere it exists. Without a short
> instrument only existing holders can close a premium; a perp lets a market maker take the
> other side without owning the fund. And the perp price becomes a **live second opinion on the
> committee's NAV** — if they disagree, everyone sees it."

### "Is this on Canton or your laptop?"
> "The shared HackCanton node, right now. Package **crossdesk 2.0.0**, registered on the
> participant — the auditor cannot see the resting book on that node, and I can show you. And
> I claimed **real BitSafe cBTC** through the CIP-56 registry flow, on their templates.
> To be precise: that's the registry integration. The instruments inside the demo basket I
> issued myself — I'm not going to tell you a hackathon fund is holding twenty real Bitcoin."

### "Who's your first customer?"
> "**BitSafe** — they're here, they issue cBTC and cETH on Canton, and I already hold their real
> cBTC on their own templates. An index fund of their assets drives demand for their assets.
> After that the leverage is the **BNY–Goldman platform**: BlackRock, Federated, Fidelity and
> GSAM all sit on it, so one integration reaches many funds."

### "Canton app rewards?"
> "Capped at **$1.50 a transaction**. A NAV venue does tens of transactions a day — about **ten
> thousand a year** at design-partner scale. It's a **gas rebate, not a business model.**"

---

## Do NOT claim
- ❌ **"The fund holds real cBTC."** It does NOT. The `CBTC` and `cETH` inside LX1 are
  **self-issued** by `issuer-crossdesk` and modelled on BitSafe's — that is why the Venue
  shows 21 CBTC. The **4.16 real BitSafe cBTC is separate**: BitSafe's own templates,
  claimed through the CIP-56 registry flow, different contracts entirely.
  Safe wording: *"I claimed real BitSafe cBTC through the CIP-56 registry flow — that
  integration works. The basket in the demo uses instruments I issued myself."*
- ❌ Holding **USYC** — KYC-gated, Reg S. It is *modelled*.
- ❌ Create-and-hedge as **one Daml transaction** — two submissions, one ledger. "No leg risk" is true.
- ❌ **Trading fees working** — the fee leg is NOT implemented.
- ❌ Any **Canton AUM** figure. ❌ "$6 trillion on Canton" (that's repo flow; repo needs no NAV).
- ❌ "The **only** venue…" — unverifiable.
- ❌ Cross-margin, liquidity vaults, AMMs, RFQs, auto-deleveraging, partial closes, permissionless liquidation.
- Say "validators" only for Canton node operators — for signers say **counterparties**.
