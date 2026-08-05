# CrossDesk — Grand Final pitch script

**Wed 2026-08-05 · 14:00 UTC · slot 18 · 4 min hard cutoff + 2 min Q&A**
Join Zoom from **13:30 UTC** to test screen-share and audio.

**Spoken words: 511.** At ~140 effective words per minute — a calm 145 wpm with the
clicking and page-loads folded in — that lands at **≈3:39**, leaving 20 seconds against
a hard 4:00. Per section: §0 56 · §1 124 · §2 66 · §3 84 · §4 94 · §5 87.

**A correction to the old header.** It claimed ~510 and the § sections actually held
**550** — ≈3:56 at the same rate, before the demo failed you in any way. It was already
over. Adding leverage to that would have run you off the end. In this file **every `>`
blockquote is a word you say out loud, and nothing else is**, so the number is
checkable rather than asserted. Re-run this after any edit:

```
awk '/^## Q&A/{exit} /^>/{gsub(/[*_`>]/,""); gsub(/[—–·]/," "); n+=NF} END{print n}' \
  docs/GRAND_FINAL_PITCH_SCRIPT.md
```

**If you are behind at the 1-minute warning:** in §4, do not open a fresh position.
Point at the one already on screen from your pre-flight and go straight to the viewer
switch. That is ~15 seconds back and costs you no argument at all.

---

## What changed, and what paid for it

The leverage layer shipped overnight and it earns a beat. Four minutes did not get
longer, so something had to pay for it. What went, and why:

- **§4 "Real cBTC" is no longer its own section.** The proof is still shown — it moved
  *into* criticism four in §1, which is the criticism it answers. Walking back to a
  point you already made costs ~20 seconds of re-framing and buys nothing.
- **The money-market accrual beat is gone.** "The committee attests the rate and the
  ledger accrues it — you can watch it tick" is the prettiest thing on the screen and
  no judge asked for it. It is now a Q&A answer, where it costs nothing.
- **"Every leg settles or none of them do" is gone from §3.** §5 now makes the
  atomicity argument properly, and making it twice is how you get cut off at 4:00.
- **Euronext's tie-break rule moved to Q&A.** The Xetra ladder stays — one concrete
  rule proves you know the mechanism; two is a lecture.
- **The close is the same length and now says something.** "Atomically, and privately"
  was a claim. It is now the arbitrage argument, which is the strongest economic thing
  this project can say to a room of finance people.
- **§0, §1, §2 are compressed, not amputated.** All four criticisms still get a full
  answer, because answering them is the assignment.

**The demo is four clicks: run the close, switch the viewer on the book, open the
position, switch the viewer on the position.** It is not a tour.

---

## Before you start (13:30 UTC)

- [ ] Local stack up: sandbox `:6900` → backend `:8080` → UI `:5173`
- [ ] `./scripts/seed-fund-demo.sh` run, so marks are today's real prices
- [ ] **The perp market opens itself.** Selecting an asset opens its market and funds
      the insurance pool in the same step — no button, no curl.
- [ ] **But the auto-fund fails silently if the Venue has no cash**, and on the local
      sandbox `Test:initialize` gives the Venue none. If the red **"no insurance pool"**
      box is showing, that is what happened. One curl fixes it, then switch the asset
      away and back to retry:

      curl -sS -X POST localhost:8080/api/holdings -H 'Content-Type: application/json' \
        -d '{"issuer":"Issuer","instrumentId":"USDC","owner":"Venue","amount":500000}'

- [ ] **One asset picker drives the whole desk.** Changing the market in Leverage
      changes the quote at the top, the Cross, and the Continuous Session with it. Set
      it to **cETH** once and everything follows. Do NOT demo on `DEMO:AAPL` — no live
      feed, hardcoded at 255.
- [ ] **Do not plan on LX1 being in that picker.** As of this morning it is built from
      `/api/instruments`, and the fund is a basket, not an instrument. If it is there at
      13:30, fine — the perp will index on NAV per share. If it is not, that is expected
      and the Q&A answer covers it.
- [ ] **Open one position as Alice now and leave it open** — Long, margin 500, 5x. That
      is the safety net: if the live open fails on stage you still have a row with a
      liquidation price to point at, and the privacy switch still works.
- [ ] **Check Bob has no open position.** If he does, the privacy switch in §4 shows a
      row instead of an empty table and the whole beat dies.
- [ ] Click once through the Leverage ticket — direction toggle, Margin box with
      25/50/75/100% buttons, leverage slider, and the **Est. liquidation** figure that
      updates before you trade. Nothing should surprise you live.
- [ ] Set the scroll so the **Leverage** panel is one flick from the fund panel. The
      last 40 seconds should be a scroll, not a hunt.
- [ ] Second tab: the devnet cBTC proof (used in §1), pre-loaded
- [ ] Backup screenshot of that proof, in case the network dies
- [ ] **Audio down** — the S1 feedback said the demo was too loud
- [ ] Notifications off, one screen shared, nothing else on it

---

## §0 · Who I am — 0:00 to 0:22

> "I'm Sebastian. CrossDesk is a solo project — I built all of it.
>
> I traded equities before this: the open and the close, which is the mechanism this
> project is about.
>
> CrossDesk is the price-formation layer for tokenised assets on Canton — a sealed
> closing auction that discovers its own price, and the fund machinery that consumes it."

**Say the solo part with confidence, not apology.** "I built it alone and I'm the
domain expert" is a strong answer to the question they actually asked.

---

## §1 · Your four criticisms, answered — 0:22 to 1:14

> "You gave me four things to fix.
>
> **One — the auction crossed at an operator-supplied price.** Now `RunClose` discovers
> it *from the book*: maximum executable volume, then minimum imbalance, then market
> pressure. The venue's reference is only the last tie-break.
>
> **Two — deterministic selection.** It's a total function: same book in, same price
> out, on every validator, which they all have to agree on. Ties break on the Xetra
> ladder.
>
> **Three — complete-order commitments.** It counts what was submitted and what was
> cancelled, and refuses to close unless the book adds up. An order leaves only by a
> real, on-ledger cancellation.
>
> **Four — self-issued stand-ins.** Gone."

**Second tab. Five seconds on it, then come straight back.**

> "That's the live HackCanton node: 4.16 cBTC across my parties, on BitSafe's own
> templates, claimed this week through the CIP-56 registry flow."

---

## §2 · The dark book — 1:14 to 1:42

*Continuous Session panel. Two orders already resting.*

> "Why Canton. On a public chain the mempool *is* the book — every resting order is
> visible before it executes. The opposite of a sealed auction.
>
> Same ledger, three viewers."

**Click the "viewing as" selector: Venue → Alice → Auditor.**

> "The venue signs every order, so it sees the whole ladder. A trader sees only its
> own. The auditor sees *nothing* — a resting order has no observers at all. That's the
> ledger refusing, not a filtered screen."

---

## §3 · The cross and the two NAVs — 1:42 to 2:24

**Run the close.**

> "The venue's anchor is [X]. The book prints at [Y] — discovered from the orders
> themselves."

*Fund panel.*

> "That price is what a fund needs. This one holds a money-market leg plus wrapped
> crypto.
>
> Two NAVs, and a real ETF runs both. **Official** — struck from committee-signed
> marks, and what creations and redemptions settle at. **Indicative** — what it's worth
> right now, binding on nobody. The gap is how stale the last strike is.
>
> And there's no oracle anywhere: a price is official only when two of three sign."

**The oracle line is not optional.** It is what makes the next 40 seconds safe — a
leveraged position gets liquidated against that number.

---

## §4 · Leverage — 2:24 to 3:07

*Scroll to the Leverage panel. The asset is already cETH.*

> "Built overnight. Creation and redemption keep a fund honest over hours. Leverage
> keeps it honest over seconds."

**Long · margin 500 · slide to 5x. Do not narrate the clicking.**

> "A cash-settled perpetual — post USDC, take the view, never hold the asset. A share
> that drifts from NAV only gets corrected if somebody takes the other side cheaply,
> and synthetic is cheaper than funding the whole basket.
>
> Before I trade, it tells me the liquidation price."

**Open the position. It lands in the table with a Liq. column.**

> "Now as another trader."

**"Viewing as" → Bob. The table goes empty.**

> "Gone. Same proof as the order book, on the product where it matters more: a visible
> liquidation price is an invitation to push the market into it."

**If you are behind, skip the ticket.** Point at the pre-flight position, say the
liquidation-price line, and do the viewer switch. The switch is the beat; the ticket is
the theatre.

---

## §5 · Close — 3:07 to 3:40

> "Price formation and fund administration are normally two companies. CrossDesk is
> both, on one ledger — which only Canton allows.
>
> Closing a NAV gap means buy the underlyings, create, sell the shares: three steps
> with price risk between them, so the arbitrageur charges a spread for it. Here it's
> one atomic transaction. The arb stops being risky, so it stops being expensive, so
> the gap the fund trades at gets tighter.
>
> Atomicity doesn't pay the trader. It pays the fund. That's what tokenised funds are
> missing. Thank you."

---

## Q&A — the ones you will actually get

**"Isn't the price feed just an oracle?"**
> No. The feed can't write anything — it fills in a box. Only signatures move the
> official number. That's why the indicative NAV and the official NAV are two different
> numbers on that screen.

**"Is this running on Canton or on your laptop?"**
> Be straight: *"What you just saw is a local Canton node running the identical DAR.
> The shared node still has my pre-feedback package — uploading a DAR is admin-only and
> my request is still in the queue. The settlement path did run live on the shared node
> in July, and the real cBTC I showed you is on it right now."*
>
> **Say this unprompted the moment leverage comes up:** *"The leverage layer is local
> only. It was written overnight, it is newer than any package the shared node holds,
> and I'm not claiming otherwise."*

**"Does the closing auction use the resting order book?"**
> Not yet — they're two sessions on the same desk. The continuous book is what a real
> close *inherits*, and wiring the ladder into `RunClose` is the next step.
> **Do not claim it already does this.**

**"Why would anyone use this instead of an exchange?"**
> Because an exchange won't strike your NAV and a fund administrator won't print your
> price. On Canton both can be the same atomic transaction, and the book stays private
> while it happens.

**"Is that arbitrage free money?"**
> No. You're exposed the whole time you're assembling the basket, and you pay fees and
> spreads to do it. The gap *is* the payment for bearing that risk — which is exactly
> why a fund trades ten basis points from NAV and not three percent.

**"Who's the counterparty to the perps?"**
> The venue's pool. It is not a matched book — a trader's profit is paid out of the
> pool and a loss is paid into it, which is the broker model, and I'd rather say that
> than imply every long has a short behind it. If open interest goes lopsided, that is
> the pool's directional risk. Open interest is a ledger fact, so the imbalance is
> provable rather than asserted, and the funding rate is the lever that closes it.
> There is no auto-deleveraging. A production venue would need one.

**"Why isn't liquidation permissionless?"**
> Because positions are private, and a keeper can't liquidate what it can't see. That's
> a real trade-off and I chose it deliberately: permissionless liquidation means public
> positions, and a public liquidation price on a leveraged product is a target. The way
> to close it is a disclosed commitment to the liquidation price — the trader publishes
> the number, not the position — and that isn't built.

**"Can you lever the fund itself, not just cETH?"**
> A perp on a basket indexes on NAV per share, computed from the components' attested
> marks — and if any component is unmarked it returns nothing, because a fund you can't
> value is a fund you can't lever. What's on screen is cETH because the asset picker is
> built from the instruments carrying an attested mark, and the fund is a basket. That
> market opens from the API. **Don't demo it live.**

**"How do you mark the money-market leg?"** *(the §3 cut, if they ask)*
> A money fund has no price to stream — its NAV is struck and then *earned*. The
> committee attests the rate and the day-count, and the ledger accrues it continuously
> between fixings. A websocket would be the wrong architecture for it.

**"What are the tie-break rules?"** *(the §1 cut, if they ask)*
> Maximum executable volume, then minimum imbalance, then market pressure, then the
> Xetra reference-price ladder with Euronext's final rule. The venue's own reference is
> last, and that is the only place it appears.

---

## Things not to say

- Don't claim you **hold** USYC — it's KYC-gated. You model it with its published rate.
- Don't claim the close uncrosses the day book — see above.
- Don't call the live feed an oracle, and don't call the attested mark a feed.
- **Don't say "a perp on the fund" while cETH is on the screen.** The capability is
  real; the thing you are pointing at is cETH.
- **Don't imply the leverage layer is on the shared devnet node.** It runs locally.
- **Don't claim auto-deleveraging, cross-margin, or partial closes.** None exist. A
  position closes whole — reducing means close and reopen.
- **Don't say you can post the asset as collateral.** Margin is USDC.
- **Don't say the perp trades against an order book.** Positions open at the index.
  Matching perps through the continuous book is the next step, not a feature.
- **Don't say liquidation is permissionless.** The venue does it — the Q&A says why.
- Don't apologise for being solo.
