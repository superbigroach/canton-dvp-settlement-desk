# Appsfactory Accelerator — application (submitted)

Canton ecosystem accelerator. Submitted after CrossDesk won **Best Financial Application**
at the HackCanton Season 2 Grand Final.

---

## Form fields as submitted

**Full Name** — Sebastian Borjas

**Social Handles** — https://www.linkedin.com/in/sborjasto/

**Stage** — MVP / Prototype

**Pitch Deck** — `CrossDesk_Pitch_Deck.pdf`
(rendered from `docs/deck/CrossDesk_GrandFinal.html`; 12 pages, 0.74 MB, landscape 16:9.
Regenerate with headless Chrome — see §Regenerating below.)

**Website Link** — `https://crossdesk-devnet-app.web.app/`
⚠️ *Submitted as `.../#problem`, which drops the reader mid-page past the hero. Use the bare
root if this is ever resubmitted.*

**What do you need most help with** — Go-to-market strategy
*(Not fundraising: that follows a reference customer. Not technical architecture: it is built
and running on a real node. Not team building: there is no team to build until someone pays.)*

---

## Who is involved in your team

Solo founder — Sebastian Borjas. I build all of it: the Daml smart contracts, the Java/Spring
backend against the Canton Ledger API, the React trading desk, and the market-structure design.

Before this I traded equities at a small shop — European closing auctions, index adds and
deletes, closing imbalances, and merger arbitrage. CrossDesk is the exact mechanism I traded,
which is why I know where the friction is rather than guessing.

Also: Google Cloud partner, Google's first AI innovator cohort, Launch (Jason Calacanis)
incubator alumnus, house architect on Circle's Arc, Daml trained.

---

## Why should it be you

CrossDesk won Best Financial Application at the HackCanton Season 2 Grand Final.

I traded the closing auction for a living — European closes, index rebalances, imbalances,
merger arb. I have been the person on the other side of a fund everyone could see coming. That
is why CrossDesk is a market-structure product rather than a blockchain demo.

It runs on the shared HackCanton participant today, package crossdesk 2.0.0. A sealed closing
auction, a K-of-N committee that signs a valuation recipe rather than a static number, atomic
in-kind create and redeem, and cash-settled perpetuals. Four of the six builder lanes, 30 Daml
templates, 213 test scenarios where every settlement path proves cash is conserved. Real
BitSafe cBTC claimed through the CIP-56 registry flow.

The property only Canton gives you: the resting order book is invisible even to the auditor,
while every fill prints to a public tape that names nobody. Unbuildable on a public chain,
unprovable in a private database.

I also know the honest version of my market. Calculating a NAV pays a third of a basis point.
Publishing an official price pays three basis points plus six hundred thousand a year. The
company is a bet on which side of that line a committee-attested fixing lands.

Three targets, in order. BitSafe first — they issue cBTC and cETH on Canton and there is no
index product on top of them; I have already built it and it is running. Then the money-market
issuers on Canton — Circle, Franklin Templeton, the BNY–Goldman platform — where I sell the
attestation half only: a shadow NAV running in parallel with their administrator, so nobody
switches and nothing is at risk. Then private credit, where the mark is genuinely doubted —
Hamilton Lane's SCOPE and Fasanara's F-ONE are both in RedStone's Canton pipeline, and
administering funds like those pays 12 basis points tapering to 6, against 0.325 for a T-bill
fund. The market pays for valuation difficulty, not valuation.

What I need is one pilot fund for ninety days in shadow mode, and the introduction that gets me
in the room. That is the one thing I cannot build myself.

---

## Where every claim comes from

| Claim | Source |
|---|---|
| 0.325 bps to calculate a NAV | BBH Trust, Form N-CSR FY2023-10-31, accession 0001213900-24-001494 |
| 3 bps + $600,000/yr for an official price | An, Benetton & Song, *Index Providers*, Journal of Financial Economics — State Street → S&P Dow Jones for SPY |
| 12 → 6 bps for hard-to-value administration | Aetos Long/Short Strategies Fund / HedgeServ, Form 486BPOS Ex-99(k)(2), 2024-05-31 |
| Hamilton Lane SCOPE, Fasanara F-ONE in Canton pipeline | blog.redstone.finance, 2026-06-25 |
| USYC live on Canton | canton.network press release; Circle/Hashnote acquisition 2025-01-21 |
| Franklin Templeton BENJI on Canton | canton.network + PR Newswire, 2025-11-12 |
| BNY + Goldman tokenised MMF platform | goldmansachs.com press release, 2025-07-23 |
| package crossdesk 2.0.0 registered | `GET /api/diag` → `PACKAGE_STATUS_REGISTERED` on participant hackcanton-01 |
| Auditor sees 0 of 4 resting orders | verified per-party on the shared node, 2026-08-05 |

Full detail: `docs/MARKET_AND_PRICING.md`, `docs/PROJECT_CONTEXT.md`, `docs/BUILD_INVENTORY.md`.

---

## Regenerating the PDF

The deck is HTML; the PDF is rendered from it, so the two never drift.

```powershell
& "C:\Program Files\Google\Chrome\Application\chrome.exe" --headless=new --disable-gpu `
  --no-pdf-header-footer --run-all-compositor-stages-before-draw --virtual-time-budget=12000 `
  "--print-to-pdf=C:\Users\sborj\Desktop\CrossDesk_Pitch_Deck.pdf" `
  "file:///C:/Users/sborj/Desktop/hackcanton-ceth-settlement/docs/deck/CrossDesk_GrandFinal.html"
```

Slide 9 renders the verified-numbers panel instead of the video, because a PDF cannot play one.
