# CrossDesk — Demo Video Script (v2, ETF-led)

**Target length:** ~3 minutes (hackathon max is 5).
**Record:** screen at `http://localhost:5173` + voiceover. Do a silent click-through first, then record.
**Tools:** OBS Studio, or Windows Game Bar (Win + G).

---

## Before you record — 60-second checklist
1. Stack is running: backend `:8080`, sandbox `:6900`, frontend `:5173`. (If the desk won't load, ask to restart the stack.)
2. Open `http://localhost:5173`, hard-refresh (**Ctrl+Shift+R**).
3. Top-right **"Acting as" = Alice**.
4. **Where to look for confirmation:** the **POSITION** panel (top-right) — it updates on every action. A brief green toast also appears at the very top. **Basket create/redeem does NOT appear in "Settlement Receipts"** (that panel is only for auction/DvP trades) — the Position change is the proof.
5. Optional: for pristine round numbers (Alice at exactly cETH 5 / CBTC 1 / no LX1), reseed the ledger first.

---

## The script

### [0:00–0:15] Hook
**DO:** Show the desk header ("CANTON DvP DESK") and the Position panel.
**SAY:**
> "This is CrossDesk — a tool to build tokenised funds and ETFs on Canton. It does two hard things directly on the ledger: it **creates and redeems fund shares in-kind, atomically**, and it prices them with a **NAV that no single party controls**. Let me show you."

### [0:15–0:35] The problem
**SAY:**
> "Even tokenised funds today are priced by hand, off-chain, and issued through a trusted administrator — days of reconciliation, and you have to trust the middleman. CrossDesk puts both directly on-chain."

### [0:35–1:25] Create in-kind — THE HEADLINE
**DO:** Acting as **Alice**. Scroll to **FUND / ETF BUILDER**. Point at the basket **LX1 = 0.10 cETH + 0.01 CBTC**, and the **NAV / SHARE = 890.00 USDC** with the breakdown (0.1 × 2,400 + 0.01 × 65,000).
**SAY:**
> "Here's a fund — LX1. One share is 0.10 cETH plus 0.01 CBTC. Its NAV — 890 USDC — is computed on-ledger from the committee-attested prices, right here. I'm Alice, an authorised participant. I'll create 10 shares."

**DO:** Make sure **SHARES = 10**. Click **Create 10**. **Immediately look at the POSITION panel** — LX1 goes **up by 10**, cETH drops by **1.0**, CBTC drops by **0.1**.
**SAY:**
> "In **one atomic transaction**, my cETH and CBTC moved into the fund's custody and I received 10 LX1 shares — watch the position update. No middleman, no settlement risk: both legs move together, or neither does."

### [1:25–1:55] Redeem
**DO:** Click **Redeem** (e.g. Redeem 4 — set shares to 4 first if you want a different number). Watch Position: cETH + CBTC come back, LX1 drops.
**SAY:**
> "Redemption is the reverse — I hand back shares and instantly get the cETH and CBTC back. This is the in-kind mechanism the **SEC approved for crypto ETFs in July 2025** — running natively on the chain the assets live on."

### [1:55–2:40] The decentralised operator — the trust story
**DO:** Scroll to **DECENTRALISED OPERATOR** panel.
- Click **Stand up committee** (members Venue, Bank, Agent · threshold 2-of-3).
- Click **Propose** (instrument cETH, session Close, price 2400).
- Click **Bank confirms** — point at the attestation row going **1 → 2**.
- Click **Finalise**.
**SAY:**
> "Now the price. A NAV is only trustworthy if no single party can set it. Here's a committee — two of three must sign. Venue proposes the price, Bank attests, and **only once two of three have signed does an official NAV exist** — provable from the contract's own signatures. No single venue can print the price."

### [2:40–3:05] Why Canton + close
**SAY:**
> "All of this needs two things a public chain can't give you: **atomic settlement with zero principal risk**, and **privacy** — so competing participants never see each other's baskets. That's Canton. CrossDesk — the **fund-issuance and NAV layer for tokenised assets**, settling cETH and CBTC. Thanks for watching."

---

## Optional extra scene — the sealed auction (the price engine)
If you have time (adds ~30s), show where the NAV comes from:
**DO:** In **TRADE**, pick cETH, Buy, qty 3, **Send to Close Cross**. Switch **Acting as → Venue**, open **THE CROSS**, **Run the Cross**.
**SAY:**
> "And the price itself is produced by a sealed auction — every order is private until the cross, then everyone settles at one uniform price. That's the number the committee attests as the official NAV."

---

## Notes
- Your **exact numbers may differ** from the script (the sandbox has prior activity) — the story is identical. Reseed for clean round numbers if you want.
- If a panel shows an error mid-take, click **Refresh** and re-take that scene (the local sandbox occasionally hiccups under load).
- Keep it tight: judges reward a clear 3-minute demo over a rambling 5-minute one.
