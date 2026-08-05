# Recording the demo video — 45 seconds, muted

**Record this URL, not localhost:** `https://crossdesk-devnet-app.web.app`

That is the desk running against the **real shared HackCanton Canton node**
(`participant.hackcanton-01`), package `crossdesk 2.0.0`. **Leave the URL bar visible** — it is
the proof that this is not your laptop, and it costs you nothing.

Save the file to: `docs/deck/video/demo.mp4`
It then plays automatically on slide 9. No other step.

---

## Setup (2 minutes)

- Chrome, **one tab**, no other tabs visible, no bookmarks bar
- Browser zoom **110–125%** so text is legible when the deck scales it down
- **Notifications off** (Windows: Focus assist on)
- Record with **Win+Alt+R** (Xbox Game Bar) or OBS · 1080p · **no microphone, no system audio**
- Move the mouse **slowly**. Rest 2 seconds on every state that matters — the judges need
  time to read a number they've never seen before.

---

## The shot list — 45 seconds

### 0:00–0:12 · THE DARK BOOK (the single most important shot)
1. Continuous Session card, asset **cETH**, *viewing as* **Venue** — 4 resting orders on screen.
2. **Pause 2s.**
3. Change *viewing as* to **Auditor**. **The table empties.**
4. **Pause 3 full seconds on the empty table.** Do not rush this. This is the whole pitch.
5. Change to **Alice** — one order, hers only.

### 0:12–0:22 · THE TWO NAVs
6. Fund / ETF Builder, basket **LX1**.
7. Rest on **Official NAV** and **Indicative NAV** with the **drift in bps** between them.
8. **Pause 2s.** One is signed and settles; the other is live and moves.

### 0:22–0:32 · THE ARB
9. Same card — the **DISCOUNT** strip: last trade **316.00** vs NAV **~321.44**, **−169 bps**,
   edge **5.44/share**.
10. Click **Redeem at NAV**. Let the confirmation land.

### 0:32–0:40 · THE CLOSE
11. Sealed auction card for **cETH** — an auction is already staged and open
    (Alice sell 2 @ 1840, Bob buy 2 @ 1890).
12. Run the close as **Venue**. **Rest on the printed clearing price.**

### 0:40–0:45 · LEVERAGE
13. Leverage panel, **LX1** — Alice's open **Short 25**, **4.73x**, liquidation **370.89**.
14. Rest on the **Est. liquidation** figure and stop recording.

---

## If it runs long

Cut in this order — the first item is the least persuasive, the last is untouchable:

1. Leverage (step 13–14)
2. The close (step 11–12)
3. The arb (step 9–10)
4. The two NAVs (step 6–8)
5. **The dark book — never cut this.**

A clean 30 seconds beats a rushed 50. You are talking over it; the video only has to make your
words true, not tell the story on its own.

---

## If the recording comes out badly

Do not re-record it three times. Take **stills** of the same six states instead and drop them in
`docs/deck/img/`. A screenshot cannot stutter, cannot buffer, and cannot fail on stage. Tell me
and I'll swap slide 9 to a still grid.

---

## What is already staged on the node

| State | Value |
|---|---|
| Package | `crossdesk 2.0.0`, `PACKAGE_STATUS_REGISTERED` |
| cETH continuous book | 4 resting orders · Venue 4 / Alice 1 / Bob 1 / **Auditor 0** |
| cETH sealed auction | **open**, Alice sell 2 @ 1840, Bob buy 2 @ 1890 · Auditor sees 0 |
| LX1 official NAV | `321.43686` |
| LX1 last trade | `316.00` → **DISCOUNT −169.1 bps**, edge `5.4369`/share |
| LX1 perp market | index `321.43686`, 10x max, insured |
| Alice's position | Short 25 · **4.73x** · liquidation `370.892248` · not at risk |
| Live marks | Coinbase spot ETH-USD and BTC-USD, refreshing |
