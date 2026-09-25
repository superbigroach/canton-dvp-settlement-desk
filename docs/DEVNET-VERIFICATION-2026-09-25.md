# DevNet verification — 25 September 2026

Read-only verification of ETP Foundry's desk and web app **against the real Canton DevNet
deployment**, not a local sandbox. This closes as much of the 24 Sep per-seat walkthrough gap as
can be closed without a human signing in — that walkthrough was done on a local sandbox stack.

## Summary

**37 numbered checks: 33 PASS · 2 PARTIAL · 2 FAIL.** All 14 public pages return 200, with
**zero console errors, zero failed requests and zero stuck placeholders**. The API is healthy and
genuinely reading ETP Foundry's own DevNet validator at `10.20.0.10:5001`.

**12 defects (D-1 … D-12) and 10 false or stale site claims (F-1 … F-10).**

The four things worth knowing first:

1. **The site tells visitors it is a sandbox.** Three hardcoded "hosted sandbox" strings in
   `frontend/public/site/site.js` render on `/`, `/benchmarks/` and `/benchmarks/CBTC`,
   contradicting the footer on the same screen (**F-8**). Grepping the HTML misses them.
2. **The landing page claims 4.16 cBTC; the ledger holds 1.0** (**F-7**) — a figure
   `/regulatory` already retracts.
3. **A committee *has* been convened** — "E2E NAV Committee", K=2 of N=3 — yet five pages say
   none has (**F-1 … F-5**). The desk's own API publishes `"n": 3` on those very pages.
4. **The one real attested fixing is dated 2039-02-03** and is therefore permanently
   unpublishable, so every benchmark reads "seed, not attested" (**D-1**). The suppression is
   correct and was already known; the *data* is the problem.

**The sign-in gap is much smaller than the roster implies.** Only `s.borjas@lucilla.ca` has a
working Firebase credential — the nine `@sandbox.crossdesk` seats can never verify an e-mail at a
non-existent domain. But the desk ships an admin **`X-Act-As` "View as" switcher**, so **one
human login is enough** to walk every seat except custodian and transfer-agent (whose ledger
parties do not exist). See §3.

**Verified true:** the desk really does run on our own DevNet validator; **1.0 cBTC** is a real
`cbtc-network` registry asset; and no *third-party* committee has been seated.

**Note:** uncommitted local fixes for F-1 … F-6 and D-7 appeared in the working tree during this
run. **None is deployed**; all findings were re-confirmed live afterwards. See the table below.

**Target under test**

| Thing | Value |
|---|---|
| Site | `https://etpfoundry.com` (Firebase Hosting) |
| API | `/api/**` → Cloud Run `crossdesk-devnet-api`, us-central1, project `crossdesk-devnet-app` |
| Live revision | `crossdesk-devnet-api-00017-7nk` (the `deploy/own-devnet-validator/README.md` note saying "rev 00013" is stale) |
| Ledger | ETP Foundry's own Canton DevNet validator, `10.20.0.10:5001`, plaintext, over Direct VPC egress (`crossdesk-devnet-vpc` / `crossdesk-run-egress`, `private-ranges-only`) |
| Ledger auth | `LEDGER_AUTH_MODE=hmac`, subject/audience from Secret Manager |
| User auth | `AUTH_MODE=firebase`, roster-only |
| Scale | `minScale=1`, `maxScale=1` |
| Feature flags | `DEMO_SEED_FUND=false`, `DEMO_SEED_COMMITTEE=false`, `SCHEDULER_ENABLED=false` |
| Ledger package | `crossdesk` 3.0.0, `9f697598fdc5…`, `PACKAGE_STATUS_REGISTERED` |

**Method.** Live HTTPS calls to the public API; a headless browser pass over every public page;
read-only queries against the validator's JSON Ledger API from the VM itself
(`GET /v2/state/ledger-end` and `POST /v2/state/active-contracts` only — an ACS *read*).
**No ledger command was submitted. Nothing was created, exercised, archived or finalised. No
Cloud Run configuration was changed. No git commit. Nothing deployed.** The two inspection
scripts were written to `/tmp` on the VM and are not part of its persistent state.

Ledger snapshot taken at offset **500172**.

### Important: this report describes the DEPLOYED site, and the working tree has moved

While this verification was running, **uncommitted fixes for several of the findings below
appeared in the local working tree**. They are **not deployed** — every finding in this report was
re-confirmed against the live site *after* those edits existed:

| Finding | Working tree | Deployed at `etpfoundry.com` |
|---|---|---|
| **F-1** `committee.html` | fixed → "no seat is held by a third party. The only committee on the ledger is one whose seats ETP Foundry operates itself…" | **still** "no committee has been convened" |
| **F-2** `governance.html` | fixed → "No third-party committee has yet been seated; the only committee on the ledger is one ETP Foundry operates itself…" | **still** "No committee has yet been convened" |
| **F-3** `methodology.html` | fixed → "no seat is held by a third party" | **still** "no committee has been convened" |
| **F-4** `documents.html` | fixed → "no committee holds a seat, and no value attested by an independent committee has been published" | **still** "no committee has been convened, and no value has been struck" |
| **F-5 / F-6** `benchmarks/benchmark.html` | `SIGNER_PROTOCOL v1` → `v2` | **still** serves `SIGNER_PROTOCOL v1` |
| **D-7** `auth/AuthFilter.java` | fixed — `resolve()` wrapped so a PUBLIC route ignores an unusable credential | **still** returns `401 INVALID_ID_TOKEN` on `/api/health` with a junk token |

**F-7 (4.16 cBTC), F-8 (the three "hosted sandbox" strings in `site/site.js`), F-9, F-10, D-12 and
every other defect are untouched in the working tree as well as live.** Nothing in this report has
been committed or deployed by this verification.

---

## 1. Public API surface

| # | Check | Result | Evidence |
|---|---|---|---|
| A1 | `GET /api/health` | **PASS** | 200. `{"applicationId":"crossdesk-backend","status":"UP","tls":false,"ledgerPort":5001,"ledgerHost":"10.20.0.10","auth":"hmac"}` |
| A2 | `GET /api/benchmarks` | **PASS** | 200, 1393 B, 2 products: `CBTC`, `cETH` |
| A3 | `GET /api/benchmarks/CBTC` | **PASS** | 200, full product view |
| A4 | `GET /api/benchmarks/cETH` | **PASS** | 200 |
| A5 | `GET /api/benchmarks/LX1` | **PASS** (correct 404) | 404 `{"message":"no benchmark 'LX1'"}` — see **D-2** |
| A6 | `GET /api/benchmarks/NOPE` (unknown id) | **PASS** | 404, same clean JSON shape |
| A7 | `GET /api/series/CBTC` | **PASS** | 200, 1 row, `2026-09-24`, price 65000, `tier 0`, `tierLabel "seed"` |
| A8 | `GET /api/series/cETH` | **PASS** | 200, 1 row, price 2400, `tier 0` |
| A9 | `GET /api/series/CBTC.csv` | **PASS** | 200, `content-type: text/csv`, header `date,asOf,price,referencePrice,wrapperFactor,tier,tierLabel,k,n,signers,fixingCid,restated` |
| A10 | `GET /api/series/cETH.csv` | **PASS** | 200, `text/csv` |
| A11 | `GET /api/series/LX1.csv`, `/NOPE.csv` | **PARTIAL** | 404 correctly, but `content-type: application/json` on a `.csv` route — see **D-5** |
| A12 | `GET /api/methodology` | **PASS** | 200. `signerProtocolVersion: "SIGNER_PROTOCOL v2"`, 2 documents at v0.1, tiers 1–5 |
| A13 | `GET /api/signer-protocol` (no param) | **PASS** | 200, 7232 B, `v2`, 6 roles: issuer, lender, venue, custodian, transfer-agent, operator. Issuer seat carries the strictest set incl. `attestor-quorum` |
| A14 | `GET /api/signer-protocol?instrument=CBTC` | **PASS** | Byte-identical to A13 — **correct**: `CBTC` is configured `attested` (`RESERVE_MODEL_CBTC:attested`), which *is* the default profile |
| A15 | `GET /api/signer-protocol?instrument=cETH` | **PASS** | Differs from A13 as designed: issuer conditions drop `attestor-quorum` → `[reserves-current, reserves-cover-supply, redemption-queue-clear]`, because cETH is `onchain-verifiable`. The param is genuinely wired |
| A16 | `GET /api/signer-protocol?instrument=NOPE` | **PASS** | 200, falls back to the strictest ATTESTED profile (never the easier checklist) |
| A17 | `GET /api/fixing-schedule` | **PARTIAL** | 200, 3 identifiers `CBTC`/`cETH`/`LX1`, all `PENDING`, `overdueCount: 0`. LX1 does not exist on this ledger — see **D-2** |
| A18 | `GET /api/diag` | **PASS** | 200. `ledger.reachable: true`, `ledgerEnd: 498902`, package registered, token present & valid, 9 parties, `partiesError: null` |

### The number the site actually publishes

Both products publish `tier 0` / `tierLabel "seed"` / `displayLabel "seed value, not attested"`,
with `k: 0`, `n: 3`, `signers: []`, `fixingCid: null`. `n: 3` is read from the **real committee on
the ledger** (N=3). `k: 0` because **no publishable attested fixing exists** — see §2 and **D-1**.

---

## 1b. Public pages

All 14 pages return **200**. Both trailing-slash and bare forms work for the benchmark detail
page (`/benchmarks/CBTC` and `/benchmarks/CBTC/` return identical 14364-byte bodies).

| Page | HTTP | Bytes |
|---|---|---|
| `/` | 200 | 15128 |
| `/benchmarks/` | 200 | 7997 |
| `/benchmarks/CBTC` | 200 | 14364 |
| `/methodology` | 200 | 45319 |
| `/governance` | 200 | 18461 |
| `/licensing` | 200 | 17690 |
| `/regulatory` | 200 | 20255 |
| `/documents` | 200 | 9366 |
| `/calendar` | 200 | 11025 |
| `/status` | 200 | 11358 |
| `/committee` | 200 | 34002 |
| `/news` | 200 | 10858 |
| `/about` | 200 | 11338 |

The pages are static HTML hydrated by **`/site/site.js?v=9`** (200, 22825 B), which fetches only
`/api/benchmarks`, `/api/benchmarks/{id}`, `/api/series/{id}`, `/api/methodology` and
`/api/signer-protocol` — all in the public allowlist, so they resolve with no credential.

### Rendering, console and network (headless browser, 1280px)

| # | Check | Result | Evidence |
|---|---|---|---|
| C1 | Console errors/warnings, all 13 pages | **PASS** | "(no console messages)" on every page — no errors, no warnings |
| C2 | Failed network requests, all 13 pages | **PASS** | no non-200 request on any page. Landing's only XHR is `GET /api/benchmarks` → 200 (1757 B); `site.css?v=9`, `site.js?v=9` and 4 woff2 all 200 |
| C3 | Live values populate (no stuck placeholders) | **PASS** | a grep for "loading" across every captured page's visible text returned **zero** hits. Landing, `/benchmarks/` and `/benchmarks/CBTC` all render `CBTC Close 65,000.00 USDC` and `cETH Close 2,400.00 USDC`, "as of 24 Sept 2026, 22:20 BST", "awaiting attestation · 0 of 3", "tier 0 · seed value, not attested" |
| C4 | `/calendar` populates | **PASS** | "Next strike 25/09/2026 16:00 Europe/London" for CBTC Close and cETH Close |
| C5 | `/status` populates and is live | **PASS** | all three component rows fill; "Checked at Fri, 25 Sep 2026 05:03:39 UTC", and 05:39:40 UTC on a later render. Reports `ledger end 500460`, `package crossdesk 3.0.0 PACKAGE_STATUS_REGISTERED`, `SIGNER_PROTOCOL v2` |
| C6 | LX1 empty state | **PASS** (renders correctly) | `/benchmarks/` shows `CDX-LX1-D LX1 NAV — as of — no fixing published tier —`, the intended em-dash empty state, not a hang. But see **D-2**: LX1 should not be advertised at all |
| C7 | "No such benchmark." in extracted text | **PASS** (not a bug) | `<section class="section hero" data-notfound hidden>` ships on every detail page and is correctly `hidden`; it surfaces only because text extraction includes hidden nodes |
| C8 | Trailing-slash routing | **PASS** | `/benchmarks` → 301 → `/benchmarks/`; `/about/`, `/status/`, `/committee/`, `/methodology/` all 200 either way. No path 404'd |

### Phone width (400px, `window.innerWidth` confirmed 400)

| # | Page | Result | Evidence |
|---|---|---|---|
| C9 | `/` | **PASS** | `documentElement.scrollWidth` 400, `body.scrollWidth` 400. No overflow except the `.skip` link at `left:-999px` (by design). Values render, layout stacks readably |
| C10 | `/committee` | **PASS** (with a nit) | no page-level horizontal scroll (scrollWidth 400). `table.docs.ladder` is 589px but its parent has `overflow-x: auto`, so it scrolls within its own container — readable, though it needs sideways dragging |
| C11 | `/status` | **FAIL** | `documentElement.scrollWidth` **584** against a 400px viewport — **184px of horizontal scroll on the whole page**. See **D-12** |

---

## 2. What the real ledger actually holds

Queried read-only from the validator VM at offset 500172.

### 2.1 Parties — 9, all local to our participant

Namespace `::1220278484b0cea14aaf823ee4c5890d0573ff07327bf38a3fe75537b261c6213238`.

| Label | Party id prefix |
|---|---|
| Issuer | `issuer-crossdesk::1220278484b0…` |
| Bank | `bank-crossdesk::1220278484b0…` |
| Alice | `alice-crossdesk::1220278484b0…` |
| Bob | `bob-crossdesk::1220278484b0…` |
| Auditor | `auditor-crossdesk::1220278484b0…` |
| Venue | `venue-crossdesk::1220278484b0…` |
| Agent | `agent-crossdesk::1220278484b0…` |
| Operator | `operator-crossdesk::1220278484b0…` |
| Wallet | `crossdesk-validator-1::1220278484b0…` (validator operator) |

**`Custodian` and `TransferAgent` do not exist on this ledger.** `users.yml` maps two seats to
them. See **D-3**.

### 2.2 Instruments — 6

| Id | Kind | Reference price | Issuer |
|---|---|---|---|
| `USDC` | Cash | — | Issuer |
| `DEMO:AAPL` | Equity | 255.0 | Issuer |
| `cETH` | CryptoWrapped | 2400.0 | Issuer |
| `CBTC` | CryptoWrapped | 65000.0 | Issuer |
| `MMF:USYC-REF` | MoneyMarket | 1.0 | Issuer |
| `E2E173047` | Fund | 890.0 | Bank |

**`LX1` does not exist** (`DEMO_SEED_FUND=false` on DevNet, correctly).

### 2.3 OperatorCommittee — exactly 1, and it IS seated

Contract `00d07d1086588f7c…`

| Field | Value |
|---|---|
| `label` | **"E2E NAV Committee"** |
| `admin` | `operator-crossdesk::…` (administers, never attests) |
| `members` | `bank-crossdesk`, `issuer-crossdesk`, `venue-crossdesk` → **N = 3** |
| `threshold` | **K = 2** |
| `auditor` | `auditor-crossdesk::…` |

This satisfies the pilot minimum (N=3, K=2). **Every member is a party ETP Foundry controls** —
so there is no *independent* committee, but a committee unquestionably **has been convened**.
Several pages still say none has. See **F-1**.

### 2.4 NavFixing — exactly 1, attested, and unpublishable

Contract `001f841909842452ed61db390c51aec3cdbf20ce05d9dddbdf443e9435c81d16f6…`

| Field | Value |
|---|---|
| `instrumentId` / `cashInstrument` / `session` | `CBTC` / `USDC` / `Close` |
| `price` | **65000.0000000000** |
| `tier` | **`"committee"`** |
| `threshold` | 2 |
| `attestors` | `issuer-crossdesk`, `venue-crossdesk` (**K=2 met**) |
| `attestations` | issuer → `[redemption-queue-clear]`, `SIGNER_PROTOCOL v2 issuer`; venue → `[traded-range]`, observedLow 64500 / observedHigh 65500, `SIGNER_PROTOCOL v2 venue` |
| `finalizedAt` | `2026-09-24T17:30:38.708983Z` |
| **`asOfDate`** | **`2039-02-03`** |
| `publishedTo` | `venue-crossdesk` |
| `rationale` | "E2E: committee-attested mark" |
| `referencePrice` / `wrapperFactor` / `supersedes` / `restatementReason` | all null |

**This is a real, valid, permanent K-of-2 attestation — dated 13 years in the future.** It was
struck by a test script on 24 Sep whose date expression advanced a day per minute. It is
therefore suppressed from every published surface by `SeriesService.futureDated()`
(`backend/src/main/java/com/lucilla/settlement/benchmarks/SeriesService.java:186-211`), which
logs it loudly at WARN. **That behaviour is correct and was already known** — the code comment
names this exact contract and date. The consequence is the honest headline: **the DevNet
deployment has zero publishable attested fixings**, and every public benchmark reads
"seed value, not attested". See **D-1**.

`FixingProposal`: **0**. `RestatementProposal`: **0**.

### 2.5 Token-standard holdings (`HoldingV1` by interface) — 2, and both are real

| Owner | Instrument | Amount | Registry admin |
|---|---|---|---|
| **Alice** | **`CBTC`** | **1.0000000000** | `cbtc-network::12202a83c6f4082217c175e29b…` |
| Wallet (`crossdesk-validator-1`) | `Amulet` (Canton Coin) | 1017.2001958467 | `DSO::1220be58c29e65de40bf273be1dc2b266d4…` |

**"1.0 cBTC" is TRUE** — genuinely issued by the BitSafe `cbtc-network` registrar, not
self-issued. Issuer, Bank, Bob, Auditor, Venue, Agent and Operator hold **no** token-standard
assets.

### 2.6 Legacy `Holding:Holding` — 23 contracts, all self-issued stand-ins

Separate from the token standard, the ledger also carries 23 `Holding:Holding` contracts, **all
issued by `issuer-crossdesk` except `E2E173047` (Bank)** — i.e. assets we minted to ourselves:

| Instrument | Total | Held by |
|---|---|---|
| `USDC` | 2,552,550 | Bank 2,000,000 · Venue 500,000 · Alice 2,550 (incl. 150,100 disclosed to Bob) · Bob 50,000 |
| `CBTC` | 23.00 | Bank 20.03 · Bob 2 · Alice 0.97 |
| `cETH` | 109.00 | Bank 100.3 · Bob 4 · Alice 4.7 |
| `DEMO:AAPL` | 510 | Bank 500 · Bob 10 |
| `MMF:USYC-REF` | 200,000 | Alice 100,000 · Bob 100,000 |
| `E2E173047` | 3 | Alice 3 |

This matters for two reasons, both in **D-6**: it sits awkwardly with the 24 Sep founder rule
("real assets only on DevNet"), and a naive ledger query shows **24 CBTC**, not 1.0.

---

## 3. The sign-in gap — what cannot be tested without a human

### 3.1 What was tested unauthenticated

| # | Check | Result | Evidence |
|---|---|---|---|
| B1 | Public route, no token — `/api/health`, `/api/benchmarks` | **PASS** | 200, full body |
| B2 | Protected route, no token — `/api/me` | **PASS** | **401** `{"error":"Unauthorized","message":"sign in: send Authorization: Bearer <Firebase ID token>","authMode":"firebase"}` |
| B3 | Protected route, no token — `/api/admin/users` | **PASS** | **401**, same message |
| B4 | Legacy route, no token — `/api/token-standard/pending?party=Alice` | **PASS** | **401** `"the operator desk requires an admin sign-in (AUTH_MODE=firebase)"` |
| B5 | Protected route, junk token — `/api/me`, `/api/admin/users` | **PASS** | **401** `"Firebase ID token rejected: INVALID_ID_TOKEN"` |
| B6 | **Public** route, junk token — `/api/health`, `/api/benchmarks` | **FAIL** | **401** `INVALID_ID_TOKEN` — a public route rejects public data because of a bad header. See **D-7** |
| B7 | Sandbox bypass header in firebase mode — `X-Sandbox-User: issuer@sandbox.crossdesk` on `/api/me` | **PASS** | **401**; the header is correctly ignored, no sandbox backdoor |
| B8 | Impersonation header with no admin token — `X-Act-As: issuer@sandbox.crossdesk` on `/api/me` and `/api/admin/users` | **PASS** | **401** `"X-Act-As needs a signed-in admin"` on both. Impersonation cannot be used anonymously |

Route classification confirmed in `backend/src/main/java/com/lucilla/settlement/auth/AuthRoutes.java`:
public = `/api/{benchmarks,methodology,diag,health,signer-protocol,fixing-schedule}` + prefixes
`/api/benchmarks/`, `/api/series/`; `/api/me` gated to any role; `/api/proposals*`,
`/api/signer/*` → SIGNER; `/api/ap/*` → AP; `/api/fund/*` → FUND_ADMIN; `/api/audit/*` → AUDITOR;
`/api/admin/*` → ADMIN; everything else LEGACY (admin-only in firebase mode). **`ADMIN` is added
to every gated rule**, so the admin can reach every route.

### 3.2 The roster does list the seats — and that is the problem

`backend/src/main/resources/users.yml` lists 10 rows, which is the full set needed:

| uid | e-mail | role | seat | party | instruments | Can sign in to DevNet? |
|---|---|---|---|---|---|---|
| `admin-sborjas` | `s.borjas@lucilla.ca` | admin | — | Issuer | — | **YES** |
| `sandbox-issuer` | `issuer@sandbox.crossdesk` | signer | issuer | Issuer | CBTC, cETH | **NO** |
| `sandbox-lender` | `lender@sandbox.crossdesk` | signer | lender | Bank | CBTC, cETH | **NO** |
| `sandbox-venue` | `venue@sandbox.crossdesk` | signer | venue | Venue | CBTC, cETH | **NO** |
| `sandbox-custodian` | `custodian@sandbox.crossdesk` | signer | custodian | **Custodian — party missing** | CBTC, cETH | **NO** |
| `sandbox-transferagent` | `transferagent@sandbox.crossdesk` | signer | transfer-agent | **TransferAgent — party missing** | CBTC, cETH, **LX1 — missing** | **NO** |
| `sandbox-alice` | `alice@sandbox.crossdesk` | ap | — | Alice | — | **NO** |
| `sandbox-bob` | `bob@sandbox.crossdesk` | ap | — | Bob | — | **NO** |
| `sandbox-fund` | `fund@sandbox.crossdesk` | fund_admin | — | Bank | **LX1 — missing** | **NO** |
| `sandbox-auditor` | `auditor@sandbox.crossdesk` | auditor | — | Auditor | — | **NO** |

**Why the nine cannot sign in.** `AuthFilter.java:179-198` requires, in order: a Firebase ID
token that verifies → an e-mail present in the roster → **`email_verified == true`**, else
`403 "e-mail address is not verified with the identity provider"`; an e-mail absent from the
roster gets `403 "this e-mail is not on the roster; the administrator must add it with a role
before it can sign in"`.

`sandbox.crossdesk` is **not a resolvable public domain**. No verification message can ever be
delivered to any of those nine addresses, so `email_verified` can never become true for them.

**So only one Firebase credential works on this deployment: `s.borjas@lucilla.ca`, role `admin`,
party `Issuer`.**

**There is no per-seat API-key workaround.** `POST /api/signer/apikey`
(`signing/SignerSettingsController.java:80-95`) mints a key for **the calling user only**, so a
seat would have to sign in before it could hold a key. An admin cannot mint keys for others.

### 3.3 But the seats ARE reachable — via the built-in `X-Act-As` switcher

**This is the important correction to make about the sign-in gap: it is much smaller than the
roster suggests.** The desk ships first-class admin impersonation.

- **Backend** (`auth/AuthFilter.java:103-127`): a signed-in **admin** may send
  `X-Act-As: <e-mail>`; the request then runs as `Principal.actingAs(target, by)`, which takes
  the target's **role, party, seat and instruments** (`auth/Principal.java:49-53`). Non-admins
  sending it are refused outright; anonymous callers get 401 (**B8**, verified live). Mutating
  act-as calls are written to the audit log (`recordActAs`).
- **Frontend**: `src/auth/token.ts:42-74` stores the choice per tab in `sessionStorage` and
  attaches `X-Act-As` to every call; `src/auth/AuthContext.tsx:196-206` exposes
  `startActAs`/`stopActAs`; **`src/shell/ViewAs.tsx` is the UI switcher**; and
  `src/desk/client.ts:143` deliberately suppresses the header for the admin's own user list.
- Because the assumed principal carries the **target's** role, `AuthRoutes` gates apply to the
  seat, not to the admin — so **role isolation is genuinely testable** this way (a signer
  acting-as should be refused from `/api/admin/*`).

**Therefore: one human, one Firebase sign-in, and the per-seat walkthrough can proceed through
the desk's own "View as" control.** Ten credentials are not needed. This was not verified here
only because it requires that sign-in, which is a human step.

### 3.4 What a human login WOULD unlock, and what it still would not

Signing in as `s.borjas@lucilla.ca` (admin) — a human step, out of scope for this read-only pass —
would let someone verify:

1. `GET /api/me` returns the admin principal, role `admin`, party `Issuer`.
2. `GET /api/admin/users` — the live roster, including any edits lost/kept across restarts.
3. `GET /api/admin/committees` — whether the desk surfaces the real "E2E NAV Committee".
4. `GET /api/admin/schedule`, `/api/admin/schedule/status`, `/api/admin/events`, `/api/admin/events.csv`.
5. The **authenticated** view of the 2039-dated NavFixing (the code says operator views still show it) — the single most valuable check, because it is the only place that attestation is visible.
6. `GET /api/token-standard/pending?party=Alice` and the real cBTC holding through the desk UI.
7. Every desk page behind sign-in, as admin.
8. **Each seat's portal via "View as"** (§3.3) — issuer, lender, venue, AP (Alice/Bob), fund_admin and auditor: their own instruments, their own signer checklists, and role isolation (a signer acting-as being refused from `/api/admin/*`).

**It would still NOT prove**, and these remain genuinely unreproduced on DevNet:

- Anything for **custodian** and **transfer-agent**. `X-Act-As` will assume the roster row, but every ledger call resolving party `Custodian` or `TransferAgent` must fail — **those parties do not exist** (**D-3**).
- Any flow touching **LX1**, which does not exist (**D-2**).
- That each seat's **own credential** works — untestable while the roster uses `@sandbox.crossdesk`, and unnecessary for a pilot driven through "View as".
- **Genuine multi-party K-of-N signing by distinct operators holding distinct keys.** Every submission is made by `crossdesk-backend`, which holds CanActAs on all 9 parties; act-as changes the *principal*, not the submitting credential. This is architectural, not an act-as limitation, and it is exactly what "no third-party committee has been seated" means.
- A **correctly-dated** attested fixing (**D-1**) — the 2039 record cannot be superseded for that instrument/session/date.

**To close the remaining gaps a human must** (all writes, deliberately not done here): allocate
`Custodian` and `TransferAgent` via `05-upload-dar-and-parties.sh`; decide LX1's fate; and strike
a correctly-dated fixing. Re-pointing roster e-mails is **not** required given `X-Act-As` — and
if it is done anyway, note **D-4**: those edits do not survive a Cloud Run restart.

---

## 4. Cross-checking the site's claims against the ledger

### Claims that are TRUE

| Claim | Where | Verdict | Evidence |
|---|---|---|---|
| "Values come from ETP Foundry's own Canton **DevNet validator**" | `landing.html:86`, `about.html:164`, `benchmarks/index.html:115`, `benchmarks/benchmark.html:240`, `calendar.html:165` | **TRUE** | `/api/health` + `/api/diag`: `ledgerHost 10.20.0.10`, port 5001, `reachable: true`, `ledgerEnd 498902`; Cloud Run Direct VPC egress into `crossdesk-devnet-vpc` |
| "**1.0 cBTC**" held "on our own participant and verified on 24 September 2026" | `regulatory.html:160` | **TRUE** | `HoldingV1`: Alice, `CBTC`, `1.0000000000`, admin `cbtc-network::12202a83…` |
| "1.0 cBTC issued by the cBTC registry, claimed through the CIP-56 transfer flow" | `news.html:75` | **TRUE** | same contract; registrar is the external `cbtc-network`, not us |
| "**No third-party committee has been seated yet**, so they are not attested fixings" | `landing.html:88` | **TRUE, and precisely worded** | all 3 committee members are our own parties; published tier is 0 "seed" |
| "No committee of **third parties** has been seated" | the shared footer on `about`, `calendar`, `committee`, `documents`, `governance`, `landing`, `licensing`, `methodology`, `news`, `regulatory`, `status`, `benchmarks/*` | **TRUE** | as above |
| "Live on our own participant, not yet on MainNet … holding real registry assets … What is still missing … is a committee of third parties" | `about.html:101` | **TRUE** | as above |
| "ledger package crossdesk-3.0.0" | `committee.html:61` | **TRUE** | `/api/diag`: `packageName crossdesk`, `packageVersion 3.0.0`, registered |
| "Signer protocol SIGNER_PROTOCOL v2 (22 September 2026)" | `committee.html:61` | **TRUE** | `/api/signer-protocol` → `"version":"SIGNER_PROTOCOL v2"` |
| "no fixing has been published for commercial use" | several | **TRUE** | nothing attested is served publicly; the one fixing is suppressed as future-dated |
| "The desk runs on our own Canton DevNet validator. Sign in is for committee signers, authorised participants and fund administrators, **by invitation to a named e-mail**." | landing page | **TRUE** | roster-only `AUTH_MODE=firebase`; an e-mail absent from `users.yml` gets 403 (§3.2) |
| Footer: "Since 24 September 2026 the values and the public API come from ETP Foundry's own Canton DevNet validator, running the published Daml packages — **not from a demonstration sandbox** … It is DevNet: it is not TestNet or MainNet, and nothing here settles a real obligation." | every page | **TRUE** | verified against `/api/health`, `/api/diag` and the Cloud Run config. **But `site.js` contradicts it on three of those same pages — see F-8** |
| Footer: "Where a value is attested by seats ETP Foundry itself operates, it is labelled as a pilot on the value." | every page | **TRUE, and the honest framing** | this is precisely the situation: the real committee's 3 members are all our own parties |
| `/status` Environments: "DevNet — ETP Foundry's own Canton validator, approved 23 Sep 2026, live 24 Sep 2026 — live — real-transaction suite green; real registry assets (cBTC) held" | `/status` | **TRUE** | `ledgerEnd 500460` live; 1.0 cBTC held (§2.5) |
| `/regulatory`: "Before that date this site ran on an in-process sandbox, and said so." | `/regulatory` | **TRUE** | `crossdesk-demo` is the retained in-memory service, kept only for rollback |
| `/committee`: "L1 · hosted party — … Could the administrator forge it? **Yes, technically**" and "L1 is honest for a pilot and dishonest as a destination." | `/committee` | **TRUE, and commendably candid** | `crossdesk-backend` holds CanActAs on all 9 parties, so every signature is administrator-submitted |
| `/governance`: "This is a mitigated conflict, not an absent one." / "The issuer must never be a majority of the quorum." | `/governance` | **TRUE** | the real committee is Bank + Issuer + Venue with K=2 — the issuer alone cannot reach K, but ETP Foundry controls all three |
| `/regulatory`: "17 Audits — not addressed"; "19 Cooperation with regulatory authorities — not addressed"; "This is a self-assessment, not an audit." | `/regulatory` | **TRUE** | no audit or regulator engagement was found anywhere in the repo or the deployment |

### Claims that are now FALSE or STALE

| # | Claim (verbatim) | Where | Why it is wrong |
|---|---|---|---|
| **F-1** | "No fixing has been published for commercial use and **no committee has been convened**. Nothing on this page is a claim that a seat is occupied." | `committee.html:60` | **FALSE.** `OperatorCommittee` `00d07d1086588f7c…` "E2E NAV Committee" exists on DevNet: admin Operator, members Bank/Issuer/Venue, **K=2, N=3**. Seats *are* occupied — by our own parties |
| **F-2** | "**No committee has yet been convened.** This page describes the model as specified in the methodology §7 and the signer protocol." | `governance.html:60` | **FALSE**, same contract |
| **F-3** | "no fixing has been published for commercial use and **no committee has been convened**" | `methodology.html:74` | **FALSE** on the second clause |
| **F-4** | "No benchmark has been published for commercial use, **no committee has been convened, and no value has been struck**." | `documents.html:58` | **FALSE TWICE.** A committee exists **and** a value *was* struck: NavFixing `001f8419…`, CBTC Close @ **65,000**, `tier "committee"`, finalised `2026-09-24T17:30:38Z`, attested by Issuer + Venue. It was never *published* (future-dated), but "no value has been struck" is not true |
| **F-5** | "**No committee has yet been convened for this identifier**; the seats above describe the protocol, not occupants." | `benchmarks/benchmark.html:107` | **FALSE for CBTC** — the committee struck a CBTC Close fixing. (True for cETH; the page is one template serving every identifier, so it cannot currently tell the truth for both) |
| **F-6** | "Conditions are served by `/api/signer-protocol` (**SIGNER_PROTOCOL v1**)" | `benchmarks/benchmark.html:105` | **STALE.** The live API serves **v2**; `committee.html:61` and `/api/methodology` both say v2. Only this page still says v1 |
| **F-7** | "**4.16 cBTC** — Real cBTC claimed through a CIP-56 registry flow during the season, on the issuer's own templates" | `landing.html:179` — the **"Facts" block on the home page** | **FALSE as a present-tense claim, and the site retracts it elsewhere.** The ledger holds **1.0 cBTC**, once (§2.5). `regulatory.html:158` explicitly says the 4.16 figure "was made on a shared hackathon participant that is no longer online, and does not describe anything held today." The landing tile carries no such date-scoping, so the most-visited page of the site overstates real holdings by **4.16×**. (`news.html:133` also says 4.16 but is correctly scoped to the 5 Aug item.) |
| **F-8** | "Values from the **hosted sandbox** via `/api/benchmarks`. Refreshed on load." · "Live from the **hosted sandbox** via `/api/benchmarks/{id}`." · "LX1 is a demonstration instrument on the **hosted sandbox**." | **`frontend/public/site/site.js:207`, `:329`, `:52`** — rendered live on `/`, `/benchmarks/` and `/benchmarks/CBTC` | **FALSE, and self-contradicting on the same screen.** These strings are hardcoded in the JS that hydrates the pages, so they survived the switch to the real validator. Each sits a few hundred pixels above the footer's "not from a demonstration sandbox" and the site's own "instead of the in-process sandbox it used before". **This is the most damaging stale text on the site**: it tells a visitor the numbers are sandbox values when they now come from the DevNet validator. Grepping the HTML alone misses it — only `site.js` and a rendered page show it. |
| **F-9** | Restatement rule stated two incompatible ways | `governance.html:193` vs `calendar.html:96` | **INCONSISTENT.** `/governance`: materiality "**One basis point** of the published value", window "**Two business days** from publication". `/calendar`: "only if the error is found **the same day** *and* exceeds **0.10 %**" (= 10 bp). Different thresholds *and* different windows for the same rule. At most one can match the rulebook |
| **F-10** | "cBTC Close" factsheet naming | `documents.html` | **Cosmetic inconsistency.** Every other page and the API use `CBTC Close` |

**The contradiction is machine-visible.** `/api/benchmarks` publishes `"n": 3` — read from the
real committee — on the very pages that say no committee has been convened. The honest fix is
the wording already used in the footer and on `landing.html:88`: *no committee of **third
parties***. That is accurate; the flat "no committee has been convened" is not.

---

## 5. Defects and gaps

| # | Severity | Defect |
|---|---|---|
| **D-1** | **High (data)** | The only committee-attested NavFixing on DevNet is dated **`2039-02-03`** and is therefore permanently unpublishable. It cannot be archived (that needs the authority of its own signatories) and the ledger allows one fixing per instrument/session/**date**, so 2039-02-03 CBTC Close is burned. Net effect: **the real deployment has no publishable attested fixing at all**, and every public benchmark reads "seed value, not attested". The suppression itself is correct and deliberate (`SeriesService.futureDated()`), and `SettlementController.asOfOrToday` stops new ones — this is a **data** defect, not a code one. Fix: strike a correctly-dated fixing on DevNet (needs §3's sign-in unblock). |
| **D-2** | **Medium** | Public `/api/fixing-schedule` advertises a daily 16:00 Europe/London strike for **`LX1`**, but `LX1` exists on neither the ledger nor the catalog: `/api/benchmarks/LX1` → 404 `"no benchmark 'LX1'"`. The identifier is hardcoded in `ledger/FixingSchedule.java:99` while the instrument is created only by `DEMO_SEED_FUND`, which is correctly `false` on DevNet. The public schedule therefore promises a strike that can never happen, and `overdueCount` will mislead once 16:00 passes. |
| **D-3** | **Medium** | `users.yml` maps `sandbox-custodian` → party **`Custodian`** and `sandbox-transferagent` → party **`TransferAgent`**; **neither party exists** on the DevNet participant (only 9 parties, listed in §2.1 — they were seeded by `DemoSeed` on the local sandbox only). `/api/signer-protocol` nonetheless advertises `custodian` and `transfer-agent` as two of its six seats. **Two of the six advertised seats cannot be filled on this deployment**, and `sandbox-transferagent` also carries the non-existent `LX1`. |
| **D-4** | **Medium (ops)** | No `DATA_DIR` is set on `crossdesk-devnet-api`, so it defaults to **`./data`** inside the container (`application.yml:114`), which is ephemeral on Cloud Run with no volume mounted. Roster edits made through `/api/admin/users` — **exactly the workaround §3.3 needs** — plus minted API keys, signer settings and the event log are lost on any revision deploy or instance restart, silently reverting to `users.yml`. `maxScale=1` at least avoids split-brain, but it does not make the store durable. |
| **D-5** | **Low** | `/api/series/{unknown}.csv` returns `content-type: application/json` with a JSON error body on a `.csv` route. A CSV consumer gets a content-type it did not ask for instead of a CSV-shaped or clearly-typed error. |
| **D-6** | **Low–Medium (disclosure)** | The DevNet ledger holds **23 self-issued `Holding:Holding` stand-ins** (§2.6: 2,552,550 USDC, 23 CBTC, 109 cETH, 510 DEMO:AAPL, 200,000 MMF:USYC-REF), all minted by `issuer-crossdesk`. This sits badly with the 24 Sep founder rule "real assets only on DevNet", and more concretely: `regulatory.html` says "1.0 cBTC" while a naive query of the same ledger returns **24 CBTC**. Only the single token-standard contract is real. No page currently draws that distinction. |
| **D-7** | **Low–Medium** | A **malformed or expired** `Authorization: Bearer` header causes **public** routes to fail: `/api/health` and `/api/benchmarks` return `401 INVALID_ID_TOKEN` instead of serving public data. `AuthRoutes` classifies them PUBLIC, but the filter verifies the token before consulting the rule. Any signed-out-with-stale-token browser, or a client that attaches a token indiscriminately, breaks the public benchmark pages. A public route should ignore an unusable credential, not reject on it. |
| **D-12** | **Medium (mobile)** | **`/status` scrolls sideways on a phone.** At a 400px viewport `documentElement.scrollWidth` is **584** — 184px of horizontal scroll on the whole page. Three `table.docs` elements (Components; Benchmarks — last published value; Environments) are each 560px wide and their parent computes `overflow-x: visible`, so they are **not** wrapped in the scroll container `/committee`'s wider table already uses. Every non-table block is then squeezed into a 400px column with a dead band of background to its right. Fix: give those three tables the same `overflow-x:auto` wrapper as `/committee`. `/` and `/committee` are clean (C9, C10). |
| **D-8** | **Low–Medium (infosec)** | Internal topology is disclosed publicly, and not only in the API: unauthenticated `/api/health` and `/api/diag` return `ledgerHost 10.20.0.10`, `ledgerPort 5001`, `tls false`, `ledgerEnd`, the package id, token expiry and **all 9 fully-qualified party ids** — and **`/status` prints `auth hmac · ledger 10.20.0.10` as visible page copy**, so the validator's private VPC address is rendered in the public site's own HTML, not merely reachable via an API call. |
| **D-9** | **Low (docs)** | `deploy/own-devnet-validator/README.md` is stale: it says `crossdesk-devnet-api` "currently runs a **backend-devnet** image … **Not done; this was preparation only**" and "rev 00013", and records T10 as SKIP "(site not switched)". The site **is** switched and the live revision is **00017**. |
| **D-11** | **Medium (presentation)** | **Test scaffolding is leaking onto the public benchmark pages.** `GET /api/benchmarks` returns, for **both** CBTC and cETH, `"referencing":[{"name":"E2E173047 NAV","id":"E2E173047"}]` — the throwaway fund the 24 Sep E2E run created (`Instrument` `001f3570…`, kind `Fund`, issuer Bank). `/benchmarks/CBTC` and `/benchmarks/cETH` each carry a **"Referencing products"** section fed by that field, so a public visitor is shown a product literally named `E2E173047 NAV`. Related: the sole committee is labelled **"E2E NAV Committee"**, which any admin/committee view will display. Fix: clean the E2E artifacts off DevNet, or exclude them from the public catalog. |
| **D-10** | **Low (tooling)** | `gcloud compute ssh` is unusable on this workstation: `plink.exe` in the Cloud SDK is renamed **`plink.exe.bak`**, so gcloud fails with "Your platform does not support SSH". Worked around read-only via `gcloud compute start-iap-tunnel` + OpenSSH as OS Login user `s_borjas_lucilla_ca`. The runbook's `--command` recipe will not work as written until plink is restored. |

---

## 6. What still needs a human

Ordered by what unblocks the most.

1. **Sign in once as `s.borjas@lucilla.ca`** (admin) — the only Firebase credential that works. That single login is enough: it unlocks `/api/me`, `/api/admin/*`, the authenticated view of the 2039 NavFixing, **and, through the desk's "View as" switcher, every seat except custodian and transfer-agent** (§3.3). This is the one genuinely human step.
2. **Fix F-8 and F-7 first — these are the two that actively mislead a visitor.** F-8: three hardcoded "hosted sandbox" strings in `frontend/public/site/site.js` (lines 52, 207, 329) tell everyone the landing page and both benchmark pages are showing sandbox values, contradicting the footer on the same screen. F-7: the landing page's "Facts" tile claims **4.16 cBTC** when **1.0** is held — a 4.16× overstatement of real assets, already retracted on `/regulatory`.
3. **Correct the remaining false/stale claims** F-1 … F-6, F-9, F-10 — the flat "no committee has been convened" is contradicted by the desk's own `"n": 3`, and F-9 states the restatement rule two incompatible ways.
4. **Fix the `/status` phone layout** (D-12) — 184px of horizontal scroll at 400px.
5. **Strike a correctly-dated fixing on DevNet** so at least one published benchmark is a real attested value instead of a seed (D-1).
6. **Allocate `Custodian` and `TransferAgent`** or stop advertising those two seats (D-3).
7. **Decide LX1's fate** (D-2): seed the instrument on DevNet, or drop it from `FixingSchedule.defaults()` and from the two roster rows.
8. **Fix D-4** — mount durable storage or point `DATA_DIR` at one — before relying on any roster, API-key, signer-setting or event-log state in `crossdesk-devnet-api`.
9. **Rule on the self-issued stand-ins** (D-6) — leave them and disclose, or clear them out.
10. **Decide on D-7** — whether public routes should ignore an unusable `Authorization` header.
11. **Restore `plink.exe`** plus D-11 (clean the `E2E173047` / "E2E NAV Committee" test artifacts off the public catalog) and D-9 (the stale runbook) (D-10) so the runbook's `gcloud compute ssh --command` recipe works.

---

## 7. Reproducing this

```bash
# Public API (no credential needed)
curl -s https://etpfoundry.com/api/health
curl -s https://etpfoundry.com/api/diag
curl -s 'https://etpfoundry.com/api/signer-protocol?instrument=cETH'

# Read-only ledger inspection. gcloud compute ssh is broken here (D-10), so:
gcloud compute start-iap-tunnel crossdesk-validator 22 --local-host-port=localhost:2222 \
  --zone us-central1-a --project crossdesk-devnet-app &
ssh -i ~/.ssh/google_compute_engine -p 2222 s_borjas_lucilla_ca@localhost
# then on the VM, the read-only pattern from ~/verify-holdingv1.sh:
#   . ./lib/common.sh; CD_PROFILE=own-devnet-vm; cd_load_profile
#   tok=$(cd_mint "$CD_BACKEND_USER")
#   GET  $CD_JSON_API/v2/state/ledger-end
#   POST $CD_JSON_API/v2/state/active-contracts   # TemplateFilter or InterfaceFilter
```

Nothing above submits a ledger command.
