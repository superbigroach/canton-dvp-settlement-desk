# Session state — 2026-08-05

Everything done today, where it lives, and what is still open. Written before a machine
restart so nothing has to be reconstructed from memory.

---

## 1. The headline

**CrossDesk won Best Financial Application at the HackCanton Season 2 Grand Final**
(Wed 2026-08-05, slot 18 of 18).

---

## 2. Live URLs

| What | URL |
|---|---|
| Landing page (send THIS to people) | `https://crossdesk-devnet-app.web.app/` |
| Pitch deck, with the video | `https://crossdesk-devnet-app.web.app/pitch/` |
| The trading desk | `https://crossdesk-devnet-app.web.app/desk` |
| Backend (Cloud Run, project `crossdesk-devnet-app`) | `https://crossdesk-devnet-app.web.app/api/*` |
| Personal site | `https://personalweb-f0fb2.web.app` |

Canton participant: `ledger-api-grpc.participant.hackcanton-01.devnet.naas.noders.services`
Package: **crossdesk 2.0.0**, id `d81a41bb2e1aa776f0aa94408776a420c484ef52e52923ccb232d86139f082be`,
`PACKAGE_STATUS_REGISTERED`. DAR sha256 `7b79beed8807d8ff75952c58a7237d44f6070182e1ffb7ee0fbcd14846475e91`.

**The backend token auto-refreshes** (`LEDGER_REFRESH_TOKEN` env var, Cloud Run pinned to
`max-instances=1` — that pin is the fix for the stale-JWT bug, do not remove it).

---

## 3. Key documents

```
docs/GRAND_FINAL_PITCH_SCRIPT.md      the 4-min script (587 words ≈ 3:43), slide cues,
                                      judge Q&A, value-chain table, "do not claim" list
docs/GRAND_FINAL_PITCH_SCRIPT.bak.md  previous draft
docs/deck/CrossDesk_GrandFinal.html   12-slide deck; video at docs/deck/video/demo.mp4
docs/PROJECT_CONTEXT.md               full brief — problem, mechanisms, market, competitors,
                                      the bear case and its answer, USYC methodology
docs/BUILD_INVENTORY.md               every verified number, with the live-ledger evidence
docs/MARKET_AND_PRICING.md            pricing research, every figure sourced, 11 [UNVERIFIED]
docs/APPSFACTORY_ACCELERATOR_APPLICATION.md   the submitted application + sources
docs/research/USYC_OFFICIAL_METHODOLOGY.md    primary-source USYC research
docs/wordcount.py                     `python docs/wordcount.py` → spoken timing
C:\Users\sborj\Desktop\CrossDesk_Pitch_Deck.pdf   12 pages, 0.74 MB
```

---

## 4. What was built/fixed today

**Deployed crossdesk 2.0.0 to the shared node.** Verified `templateIdScope: "#crossdesk"` first —
package-NAME scoping means Daml smart contract upgrades apply, so 1.0.0 → 2.0.0 caused **zero
data loss**. (The "new package = empty ledger" warning in `scripts/bootstrap-devnet.sh` applies
to a package *rename*, not a version bump.) This is what brought **perpetuals** to the shared node.

**Frontend fixes** (all deployed):
- Unstyled controls — including **"Place order"**, the desk's main CTA, whose `.ticket-submit`
  class set layout but no colour
- Continuous session **auto-opens on asset selection**; `viewAs` follows the acting party
- Party ids resolved to labels (`alice-crossdesk` → `Alice`); ladder grid no longer wraps
- **Withdraw button never appeared** — `o.trader === acting` compared a ledger id to a display
  label, so it was always false
- **`Numeric 10` failure on every perp open** — `notional / index` produced 15 decimals; Daml
  rejects anything over 10. Rounded at the point the number is created
- Basket designer blocks redefining a live fund; "start from an existing recipe"
- Committee panel follows the desk asset, derives snapshot-vs-accruing from instrument kind,
  hides the Open session for funds, and survives a refresh
- Positions blotter has a **Trader** column; receipts show `settledAt`
- **Synchronous double-click guard** on every action — `disabled={busy}` is not a guard because
  `setBusy` only schedules a re-render, so two clicks in one frame both submitted
- Arb card: both directions always offered, 15 bp deadband, one-click create+hedge+MOC, and
  **"Close arb" pulls the resting MOC before closing the hedge** (it used to leave a naked order)

**Committee attestation run for real on the shared node** — 2-of-3, Bank proposed, Alice
confirmed, finalize republished the mark. USYC modelled at its **real published price
1.1330669604**, accruing **3.486% ACT/360** (rate *derived* from Circle's own `nextPrice`).

**Landing page + deck page** added to the site with a post-build step
(`frontend/scripts/arrange-dist.mjs`) that puts the marketing page at `/` and the SPA at
`/desk/` — necessary because Firebase serves a static `index.html` at `/` *before* consulting
any rewrite.

**Resumes** — 8 files updated with the track win and the **Technical Solutions Architect —
Digital Asset (Daml/Canton, 2026)** certification; stale "18/18 tests" corrected to **213**
everywhere including 3 cover letters; all PDFs regenerated; personal site redeployed.

---

## 5. Facts that must not be got wrong

- **The fund does NOT hold real cBTC.** `CBTC` and `cETH` inside LX1 are **self-issued** by
  `issuer-crossdesk` — that is why the Venue shows ~21 CBTC. The **4.16 real BitSafe cBTC is
  separate**: BitSafe's templates, CIP-56 registry flow. Say *"I claimed real BitSafe cBTC
  through the CIP-56 registry"*, never *"the fund holds it"*.
- **USYC is modelled, not held.** KYC-gated, Reg S.
- **Create-and-hedge is three ledger writes**, not one atomic transaction. "Same ledger, no leg
  risk" is true and sufficient.
- **Trading fees are NOT implemented** — no fee logic anywhere in the Daml or backend.
- Never state a **Canton AUM** figure, and never cite "$6 trillion on Canton" (repo flow).
- On Canton, **"validator" means node operator**. For signers say **counterparties**.
- Not built: cross-margin, liquidity vaults, AMMs, RFQs, auto-deleveraging, permissionless
  liquidation.

## 6. Numbers, with sources

| Figure | Source |
|---|---|
| 0.325 bps to calculate a NAV | BBH Trust, Form N-CSR FY2023, accession 0001213900-24-001494 |
| 3 bps + $600,000/yr for an official price | An, Benetton & Song, *JFE* — State Street → S&P DJI for SPY |
| Index providers take ~⅓ of ETF management fees | same paper (35.7% in 2019) |
| 12 → 6 bps hard-to-value administration | Aetos / HedgeServ, Form 486BPOS Ex-99(k)(2) |
| $16.16bn tokenised Treasuries, +4.06%/30d | rwa.xyz, 2026-08-04 → 61%/yr → 10x in 4.8 years |
| USYC $1.133066960425761961, round 483 | `usyc.hashnote.com/api/price` |
| USYC 2pm ET cutoff is **subscriptions only** | usyc.docs.hashnote.com — redemptions always at current price; price reports ~9am ET |
| Canton app rewards capped $1.50/tx | Canton *Earn with every transaction* |
| Ledger latency: read 0.45s, **write 7.5s** | measured 2026-08-05 against the shared node |

---

## 7. Open items

1. **Ask 2–3 HackCanton judges for recommendation letters — this week.** For an O-1 these are
   worth far more than the prize itself, and they get harder to ask for every week. Also save
   the official announcement, judge names/affiliations, and team/country counts.
2. **Confirm the exact title of the Technical Solutions Architect credential.** I wrote
   "Technical Solutions Architect — Digital Asset (Daml / Canton, 2026)" across 8 resumes. If
   the official wording differs, fix and regenerate.
3. **Verify the GitHub link on the resumes** — they point at
   `github.com/superbigroach/canton-dvp-settlement-desk`. Local folder is
   `hackcanton-ceth-settlement`. If the public repo was renamed that link is dead on 8 documents.
4. **Duplicate LX1 Instrument contract** on the shared node (two contracts, both now at the same
   price). Frontend dedupes for display. Harmless but untidy.
5. **`MarketData` TTL was changed 60s → 15s in source but NOT deployed** — the live backend is
   still on 60s. Needs a backend rebuild + Cloud Run deploy if wanted.
6. Not regenerated: `BJO Job Search 2026\4 - Resumes\*.pdf` (no HTML source) and
   `rideco-resume\resume.html`.
7. Uncommitted work in the repo — nothing has been committed today.

## 8. How to redeploy

```bash
# frontend (landing + deck + desk)
cd frontend && npm run build && npx firebase deploy --only hosting --project crossdesk-devnet-app

# backend  — env vars are preserved by `gcloud run deploy` on an existing service
cd backend-devnet && ./gradlew bootJar && cp build/libs/canton-dvp-desk-1.0.0.jar cloudrun/app.jar
cd cloudrun && gcloud run deploy crossdesk-devnet-api --source . \
  --project crossdesk-devnet-app --region us-central1 --max-instances 1 --allow-unauthenticated

# personal site
cd /c/Users/sborj/Desktop/StellarStudio.SB && npx firebase deploy --only hosting --project personalweb-f0fb2

# any PDF from any HTML
"/c/Program Files/Google/Chrome/Application/chrome.exe" --headless=new --disable-gpu \
  --no-pdf-header-footer --run-all-compositor-stages-before-draw --virtual-time-budget=10000 \
  "--print-to-pdf=C:/path/out.pdf" "file:///C:/path/in.html"
```
