# CrossDesk — Demo Video Script (v3 — LIVE DEVNET edition)

**Target length:** ~3 minutes (max 5).
**Record:** screen at **https://crossdesk-devnet-app.web.app** + voiceover.
**This is the REAL shared HackCanton devnet node** (`hackcanton-01`, NODERS) — every
click lands an actual on-chain transaction. Say so; it's the strongest line you have.
**Tools:** OBS Studio, or Windows Game Bar (Win + G). Do a silent click-through first.

---

## Current LIVE state on the node (already there — use it, don't recreate)
- Instruments: **cETH** (ref 3,200) · **CBTC** (ref 65,000) · **USDC** (cash)
- **Alice**: 5.0 cETH · 1.0 CBTC · 32,000 USDC ← the USDC came from a REAL settled DvP
- **Bob**: 10 cETH (bought from Alice @ 3,200)
- **Basket LX1** defined: 1 share = 0.10 cETH + 0.01 CBTC → NAV/share = **970.00 USDC**
- A settled **DvP receipt** (alice → bob · 10 cETH @ 3200 USDC) — visible as Alice/Bob/Auditor
- A 2-of-3 committee already exists (from testing) — the panel lets you stand up your own on camera; that's fine.

## Before you record — 60-second checklist
1. Open **https://crossdesk-devnet-app.web.app**, hard-refresh (**Ctrl+Shift+R**).
2. Top-right **"Acting as" = Alice**.
3. Confirmation lives in the **POSITION panel** (top-right) + green toast. Basket
   create/redeem shows in Position, NOT in "Settlement Receipts."
4. Committee panel members default to **Issuer / Bank / Auditor** (threshold 2) — leave as-is.
5. If a call errors mid-take, click Refresh and re-shoot the scene (shared node, occasional latency).

---

## The script

### [0:00–0:20] Hook — the real-node opener
**DO:** Show the desk header + Position panel (Alice's real balances).
**SAY:**
> "This is CrossDesk — a tool for building tokenised funds and ETFs. And this isn't a local sandbox: everything you're about to see executes on a **shared Canton devnet node**, operated by a third party, with real allocated parties. Every click is an actual on-chain transaction."

### [0:20–0:40] The problem
**SAY:**
> "Even tokenised funds today are priced by hand, off-chain, by one administrator — and issued through a back office. CrossDesk puts both directly on the ledger: **in-kind creation and redemption**, atomically, and a **NAV no single party can print**."

### [0:40–1:30] Create in-kind — THE HEADLINE
**DO:** As **Alice**, scroll to **FUND / ETF BUILDER**. Point at **LX1 = 0.10 cETH + 0.01 CBTC**, and **NAV / SHARE = 970.00 USDC** with the breakdown (0.10 × 3,200 + 0.01 × 65,000).
**SAY:**
> "Here's a live fund — LX1. One share is a tenth of a wrapped ETH plus a hundredth of a wrapped Bitcoin — cETH by onRails, CBTC by BitSafe, both real Canton ecosystem assets. Its NAV, 970 dollars a share, is computed on-ledger from committee-attested prices. I'm Alice, an authorised participant — I'll create 10 shares."

**DO:** Set **SHARES = 10** → click **Create 10** → immediately show POSITION: LX1 **+10**, cETH **−1.0**, CBTC **−0.1**.
**SAY:**
> "One atomic transaction on the shared node: my cETH and CBTC moved into the fund's custody and 10 shares were minted to me — watch the position update. Both legs moved together, or neither would have. No middleman, no settlement risk, and the ledger is the fund register."

### [1:30–1:55] Redeem
**DO:** Click **Redeem** (shares = 10 or 4). Position: cETH/CBTC come back, LX1 drops.
**SAY:**
> "Redemption is the mirror — shares in, underlyings out, atomically. This is the in-kind mechanism the **SEC approved for crypto ETFs in July 2025**, running natively on the chain the assets live on."

### [1:55–2:35] The decentralised operator
**DO:** Scroll to **DECENTRALISED OPERATOR**.
- **Stand up committee** (Issuer / Bank / Auditor · 2-of-3)
- **Propose** (cETH, Close, 3200)
- **Bank confirms** — point at attestations going **1 → 2**
- **Finalise**
**SAY:**
> "Now the price itself. A NAV is only trustworthy if no single party can set it. Here a committee of independent parties attests: one proposes, another signs, and **only at two-of-three does an official NAV exist** — provable from the contract's own signatures, on a node I don't operate. No single venue can print the price."

### [2:35–2:55] The receipts — proof of privacy on a shared node
**DO:** Open **Settlement Receipts** (as Alice or Auditor) — show the real DvP receipt: *alice-crossdesk → bob-crossdesk · 10 cETH @ 3200 USDC*.
**SAY:**
> "And here's a receipt from a real delivery-versus-payment that settled on this node — visible to exactly three parties: buyer, seller, auditor. Everyone else on this shared node sees nothing. That's Canton's sub-transaction privacy — a dark pool by construction, not by promise."

### [2:55–3:10] Close
**SAY:**
> "Atomic settlement, in-kind fund issuance, a committee-struck NAV, and privacy — live on a shared Canton node. CrossDesk: the fund-issuance and NAV layer for tokenised assets. Thanks for watching."

---

## Optional extra (~30s) — the sealed auction
**DO:** TRADE → cETH, Buy, qty 2 → **Send to Close Cross**. Acting as → a second party → **Run the Cross**.
**SAY:**
> "The price feeding that NAV comes from a sealed auction — every order private until the cross, one uniform price, no front-running."

## Notes
- Exact balances may drift as the shared node accrues activity — the story is identical; the Position panel is always the truth.
- If "Create" complains about insufficient cETH/CBTC, lower shares (Alice holds 5.0 cETH / 1.0 CBTC → max 50 shares by cETH, 100 by CBTC — 10 is always safe).
- Keep the "shared node / real transaction" line in EVERY section — it's the differentiator vs. every sandbox demo.
