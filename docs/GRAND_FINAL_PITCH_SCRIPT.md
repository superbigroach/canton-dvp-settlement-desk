# CrossDesk — Grand Final pitch script

**Wed 2026-08-05 · 14:00 UTC · slot 18 · 4 min hard cutoff + 2 min Q&A**
Join Zoom from **13:30 UTC** to test screen-share and audio.

> Word count is ~510. At a calm 140–150 wpm that lands **≈3:40**, leaving ~20s of slack.
> The cutoff is hard — it is better to finish at 3:45 than be cut mid-sentence at 4:00.
> **If you are behind at the 1-minute warning, skip §4 and go straight to the close.**

---

## Before you start (13:30 UTC)

- [ ] Local stack up: sandbox `:6900` → backend `:8080` → UI `:5173`
- [ ] `./scripts/seed-fund-demo.sh` run, so marks are today's real prices
- [ ] Browser at **localhost:5173**, zoomed to ~125% so text is readable on stream
- [ ] Second tab: the devnet cBTC proof (see §4), pre-loaded
- [ ] Backup screenshot of that proof, in case the network dies
- [ ] **Audio down** — the S1 feedback said the demo was too loud
- [ ] Notifications off, one screen shared, nothing else on it

---

## §0 · Who I am — 0:00 to 0:25

> "I'm Sebastian. I built all of CrossDesk myself — it's a solo project.
>
> Before this I traded equities, and specifically I traded the open and the close.
> That's the mechanism this whole project is about, so this is the one thing I
> actually know from the inside.
>
> CrossDesk is the price-formation layer for tokenised assets on Canton: a sealed
> closing auction that discovers its own official price, and the fund machinery that
> consumes it."

**Say the solo part with confidence, not apology.** They asked teams to introduce
themselves; "I built it alone and I'm the domain expert" is a strong answer.

---

## §1 · Your four criticisms, answered — 0:25 to 1:15

> "You gave me four things to fix. I'll take them in order.
>
> **One — the auction crossed at an operator-supplied price.** It doesn't any more.
> `RunClose` now calls a pure function that discovers the price *from the book*:
> maximum executable volume, then minimum imbalance, then market pressure. The
> venue's reference is demoted to the last tie-break.
>
> **Two — deterministic selection.** It's a total function. Same book in, same price
> out, on every validator — which isn't a nicety, it's a requirement, because they
> all have to agree. Ties break on the Xetra ladder with Euronext's final rule.
>
> **Three — complete-order commitments.** The auction counts what was submitted and
> what was cancelled, and refuses to close unless the book it's given adds up. An
> order can only leave by a real, on-ledger cancellation.
>
> **Four — self-issued stand-ins.** I hold real BitSafe cBTC now. I'll show you."

---

## §2 · The dark book — 1:15 to 1:45

*Continuous Session panel. Two orders already resting.*

> "First, why this has to be Canton. On a public chain the mempool *is* the book —
> every resting order is visible before it executes, which is the opposite of a
> sealed auction.
>
> Watch. Same ledger, three viewers."

**Click the "viewing as" selector: Venue → Alice → Auditor.**

> "The venue signs every order, so it sees the whole ladder. A trader sees only its
> own. And the auditor — compliance — sees *nothing*, because a resting order has no
> observers at all. That's the ledger refusing, not a filtered screen.
>
> Post-trade, the tape below is public and names nobody. Dark before, lit after."

---

## §3 · The cross and the two NAVs — 1:45 to 3:15

**Run the close.**

> "Now the cross. The venue's anchor here is [X]. The book prints at [Y] — a
> different number, discovered from the orders themselves. Every leg settles or none
> of them do."

*Fund panel.*

> "That price is what a fund needs. Here's a tokenised fund holding a money-market
> leg plus wrapped crypto.
>
> Two NAVs, and a real ETF runs both. **Official** — struck from marks the committee
> signed, and what creations and redemptions legally settle at. **Indicative** —
> what it's worth right now, live, binding on nobody. The gap between them, in basis
> points, is how stale the last strike has become.
>
> The crypto legs are at today's real spot. The money-market leg has no live price to
> stream — a money fund's NAV is struck and then *earned* — so the committee attests
> the rate and the ledger accrues it continuously. You can watch it tick.
>
> And there's no oracle anywhere. A price is official only when two of three sign it."

---

## §4 · Real cBTC — 3:15 to 3:35 · **CUT THIS IF BEHIND**

**Switch to the devnet tab.**

> "Last one. This is the live HackCanton node. 4.16 cBTC across my parties —
> BitSafe's contracts, on BitSafe's templates, claimed this week through the CIP-56
> registry flow. Not self-issued."

---

## §5 · Close — 3:35 to 4:00

> "Normally price formation and fund administration are two different companies — an
> exchange and a fund administrator. CrossDesk puts them on one ledger: the venue
> that prints the official price is the same venue that strikes the NAV and settles
> creation and redemption. Atomically, and privately.
>
> That's the piece tokenised funds are missing, and Canton is the only place it can
> be built. Thank you."

---

## Q&A — the four you will actually get

**"Isn't the price feed just an oracle?"**
> No. The feed can't write anything — it fills in a box. Only signatures move the
> official number. That's why the indicative NAV and the official NAV are two
> different numbers on that screen.

**"Is this running on Canton or on your laptop?"**
> Be straight: *"What you just saw is a local Canton node running the identical DAR.
> The shared node still has my pre-feedback package — uploading a DAR is admin-only
> and my request is still in the queue. The settlement path did run live on the
> shared node in July, and the real cBTC I just showed you is on it right now."*

**"Does the closing auction use the resting order book?"**
> Not yet — they're two sessions on the same desk. The continuous book is what a
> real close *inherits*, and wiring the ladder into `RunClose` is the next step.
> **Do not claim it already does this.**

**"Why would anyone use this instead of an exchange?"**
> Because an exchange won't strike your NAV and a fund administrator won't print
> your price. On Canton both can be the same atomic transaction, and the order book
> stays private while it happens.

---

## Things not to say

- Don't claim you **hold** USYC — it's KYC-gated. You model it with its published rate.
- Don't claim the close uncrosses the day book — see above.
- Don't call the live feed an oracle, and don't call the attested mark a feed.
- Don't apologise for being solo.
