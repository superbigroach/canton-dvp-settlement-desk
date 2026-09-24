# Per-seat signer-portal walkthrough — 24 Sep 2026

Live click-through of the ETP Foundry signer portal on a local stack in sandbox auth mode,
one pass per committee seat, plus the admin / AP / fund-admin / auditor landing pages and the
public API-backed pages. Repo `master` at `1b9470e` plus the fixes listed under "Defects".
Nothing was committed or deployed.

Facts only. A cell says PASS only when the action was performed and its result read back
(from the DOM and, for every confirm, from `GET /api/proposals/{cid}/events` on the server).

## Matrix — seat × action

| Seat (party) | Sign-in | Guide panel | Confirm OK (event `verified:true` + numbers) | Confirm refused — 422 verbatim | Refuse with reason | Empty form blocked | API key (shown once) | 400 px |
|---|---|---|---|---|---|---|---|---|
| issuer (Issuer), **attested** CBTC | PASS | PASS — 4 conditions, fields, examples, rule lines, refuse steps, quickstart, trust ladder; no `undefined`, no empty list | PASS — `attestor-quorum 8/7`, `reserves-current` ageHours 0, `reserves-cover-supply 1250.5/1248` coverage 1.002003, `redemption-queue-clear 0/0` | PASS — `Refused (422): evidence refused for the issuer seat: reserves-cover-supply: reserves 1000 do not cover supply 1248` — identical to the server body. **FIXED** (see D2): the banner was wiped ~1 s later by the list reload | PASS — `redemption-queue-clear`, card turns `refused` | PASS — `Missing: at least one condition`; after one tick `Missing: attestor-quorum.quorumSigners`, then `.quorumThreshold` | PASS — `ck_…` 67 chars, "Copy this now" warning, then "A key starting ck_ee1c0… exists" | PASS — login and open-proposals pages, `scrollWidth == 400` after **D6**; settings page NOT TESTED (headless daemon died on every attempt) |
| issuer, **onchain-verifiable** cETH | (same session) | PASS — guide `cETH` tab shows 3 conditions (no `attestor-quorum`); example body matches | PASS — 3 conditions, coverage 1.002004, queue 1/3 | NOT TESTED on cETH (422 path proven on CBTC, same code path) | PASS — refused cETH Open 2026-09-24 | PASS | (same key) | PASS (same page) |
| lender (Bank) | PASS | PASS — 3 conditions incl. `rule: … tolerances.markBps (default 25 bp)` | PASS — `independentMark 65010` deviationBps 1.54, toleranceBps 25; `liquidations-consistent 0/0`; `book-acceptance` | PASS — `… your mark 65650 is 100.00 bp from the proposal 65000; your declared tolerance is 25 bp`; banner still on screen 5 s later (D2 fix in effect) | PASS — `liquidations-consistent` on CBTC Open | PASS — `Missing: independent-mark-within-tolerance.independentMark` | PASS | NOT TESTED (layout identical to issuer's) |
| venue (Venue) — range path | PASS | PASS — `traded-range` "checked by ledger", `no-prints-attested` "checked by server" | PASS — range 64,900–65,100, event `observedLow/High`, `verified:false` (ledger-checked, by design) | PASS on the **ledger** path: low 2500 / high 2600 on a 2400 proposal → card hint "2,400.00 is OUTSIDE …", submit → server refused. **FIXED** (D4/D5): the desk showed the raw `DAML_FAILURE … AssertionFailed (error category 9)` scaffolding as a 409 with the hint "re-read and retry"; now `422 attested price sits outside the venue's observed range` with a model-rejection hint | PASS — `traded-range` on CBTC Close 2026-09-23 | PASS — `Missing: traded low`, then `traded high` | PASS | PASS — open proposals with the range inputs, `scrollWidth == 400` |
| venue — "no prints in the window" toggle | (same) | (same) | PASS — `bestBid 2390 / bestAsk 2410`, event `quoted: 1`, `verified:true` | PASS — `… no-prints-attested: the proposal 2400 sits outside your quoted 2500 / 2600` verbatim | (same) | toggle hides the range inputs and the other venue conditions; forced tick on `no-prints-attested` | (same) | (same) |
| custodian (Custodian) — **NEW seat** | PASS (**FIXED** D1: seat did not exist) | PASS — 3 conditions, "reference checker ships issuer/lender/venue" warning, quickstart `seat: custodian` | PASS — `holdings 100000/100000`, `statementAsOf` ageHours 0, `encumbered 0` | PASS — `… no-encumbrance: 12 units are pledged, lent or encumbered` (read on screen); `holdings 99000 is below issued …` served by the server (log) but the banner was not read (tool timeout) | PASS — `no-encumbrance` on cETH Close 2026-09-23 | PASS — `Missing: holdings-cover-supply.holdings` | PASS | NOT TESTED |
| transfer-agent (TransferAgent) — **NEW seat** | PASS (**FIXED** D1) | PASS — 2 conditions; instrument tabs CBTC / cETH / LX1 | PASS — `registerShares 1000000 == ledgerShares 1000000`, `accruedFees 412.5` | PASS — `… fees-accrued: accruedFees cannot be negative` (read on screen); `register shows 1000000 but the ledger shows 999990` served (log), banner not read (tool timeout) | PASS — `shares-outstanding-reconciled` on cETH Close 2026-09-22 | PASS — `Missing: shares-outstanding-reconciled.registerShares` | PASS | NOT TESTED |
| admin (s.borjas@lucilla.ca) | PASS → `/desk/admin` Schedule, 3 rows, nav Admin/Committees/Users/Events/Fallback/Operator desk/Audit | n/a | n/a | n/a | n/a | n/a | n/a | NOT TESTED |
| ap (Alice) | PASS → `/desk/ap` Funds: LX1 890.00 seed, Create / Redeem | n/a | n/a | n/a | n/a | n/a | n/a | NOT TESTED |
| fund_admin (fund@) | PASS → `/desk/fund` LX1 dashboard: NAV 890, 2 fixings, basket, licensees | n/a | n/a | n/a | n/a | n/a | n/a | NOT TESTED |
| auditor (Auditor) | PASS → `/desk/audit` Events, 22 rows incl. every action above; nav Events / Series | n/a | n/a | n/a | n/a | n/a | n/a | NOT TESTED |

Full quorum reached on CBTC Close 2026-09-24: Issuer, Bank, Venue, Custodian, TransferAgent
all attested one proposal (`5 signed · 2 needed`). Every refusal is in the auditor's event log.

Public pages against the local backend (Chrome, console errors read after load): `/status.html`
(health, diag `crossdesk 3.0.0 PACKAGE_STATUS_REGISTERED`, signer protocol v2, benchmarks) —
0 console errors; `/benchmarks/index.html` and `/benchmarks/benchmark.html?id=CBTC` — 0;
`/calendar.html` (7 rows from `/api/benchmarks`) — 0. Note: `/benchmarks/` (directory URL) is
served as the desk SPA by the **Vite dev server** only; Firebase Hosting serves the folder's
`index.html`. Not a product defect.

## Defects found and fixed

| # | Where | What was wrong | Fix |
|---|---|---|---|
| D1 | `backend/src/main/resources/users.yml`, `frontend/src/auth/sandboxUsers.ts`, `frontend/src/desk/types.ts` (`Seat`), `config/DemoSeed.java`, `ledger/LedgerService.java`, `auth/Principal.java`, `signing/ProposalService.java` | No custodian or transfer-agent user existed, `Test:initialize` allocates no party for them, and the seeded committee was Issuer/Bank/Venue only, so the §2e/§2f seats could never receive a proposal | Added `custodian@sandbox.crossdesk` (party `Custodian`, CBTC+cETH) and `transferagent@sandbox.crossdesk` (party `TransferAgent`, CBTC+cETH+LX1) to both rosters; `LedgerService.allocateSandboxParty(hint)` allocates a party through the admin API on the local sandbox only (refuses when a `LEDGER_PARTIES` roster is configured); `DemoSeed` now seats all five (K=2 of 5). Seat messages say all five seats |
| D2 | `frontend/src/pages/sign/Proposals.tsx` | `LoadState loading={list.loading}` swapped the whole list for "Loading…" on every `reload`, so the 422 banner a card had just shown was unmounted by its own `onRefresh` within ~1 s and the form was wiped | `loading={list.loading && !list.data}` — cards stay mounted during a background reload; verified: lender's 422 banner still on screen 5 s later |
| D3 | `frontend/src/pages/sign/Settings.tsx`, `desk/types.ts` | Tolerance form saved `maxDeviationBps` / `maxAgeSeconds`, which nothing on the server reads (`SignerEvidence.Tolerances.from` reads `markBps` / `liquidationBps`) — a lender editing the tolerance was still judged at the 25 bp default | Form now edits `markBps` and `liquidationBps`, placeholders show `toleranceDefaults` from the API, blank removes the key. Verified: saved `markBps: 40` comes back from `/api/signer/settings` |
| D4 | `backend/src/main/java/com/lucilla/settlement/ledger/LedgerErrors.java` (+ test) | Canton 3.4 renders an `assertMsg` failure as `… User failure: UNHANDLED_EXCEPTION/DA.Exception.AssertionFailed:AssertionFailed (error category 9): <msg>`; neither existing regex matched, so a venue range that excluded the price came back as **409 DAML_FAILURE** with the full scaffolding | New `DAML_USER_FAILURE` pattern → `damlMessage` extracted, `businessRejection` true → **422** with the model's sentence. Test `damlMessage_handlesTheDaml3UserFailureRendering` |
| D5 | same file | The hint on a model rejection was the `FAILED_PRECONDITION` one: "the ledger's state does not permit this command right now — typically a contract that has moved on. Re-read and retry" — wrong advice for a rule refusal | `DAML_REJECTION_HINT`: "the Daml model refused this command; the message above is its own reason. Resubmitting the same input gives the same answer — change the input, or refuse with a reason." |
| D6 | `frontend/src/desk.css` | At 400 px the `.check` grid children (`min-width:auto`) let an unbreakable mono rule line push the page to 402 px (2 px horizontal scroll) | `.check > *, .checklist .hint.mono, .evidence .hint.mono { min-width: 0; overflow-wrap: anywhere }` in the narrow media query; re-measured `scrollWidth == 400` |
| D7 | `frontend/src/pages/Login.tsx` | Signing out on `/desk/admin` and back in as a signer landed on `/desk/admin` → "NOT YOUR SECTION" (the `from` state was honoured regardless of role) | `from` is honoured only when the signed-in role has that section (`sectionsFor`); otherwise `/` |
| D8 | `backend/.../signing/ProposalService.java`, `frontend/src/desk/types.ts`, `pages/sign/ProposalCard.tsx` | Two open proposals on the same instrument+session (different `asOfDate`, which 3.0.0 attests) were indistinguishable on the card — the API never emitted `asOfDate` | API emits `asOfDate`; card title reads `CBTC · Close · 2026-09-24` and the facts row has `as of 2026-09-24` |

Gates after the fixes: `cd frontend && npx tsc --noEmit -p .` → exit 0.
`cd backend && GRADLE_OPTS=-Xmx2g ./gradlew test --no-daemon --offline -q` → **329 tests, 0 failures,
0 errors** (328 at HEAD + the new LedgerErrors test). No `console.log` or debug code left.

## Not exercised / caveats

- **400 px**: only the login page, the issuer's and the venue's open-proposals page were measured
  (headless Playwright, viewport 400×800). The settings page and the non-signer landing pages were
  not — the gstack `browse` daemon died on each attempt to reach `/desk/sign/settings`. Claude-in-
  Chrome's `resize_window` reported success but the viewport stayed at 2560 px, so it could not be
  used for this.
- **Custodian / transfer-agent first 422**: the server served it (backend log) but the on-screen
  banner was read only for the *second* failing number of each seat (the first read timed out in the
  browser tool, a background-tab timer throttle). The seats' banner path is the same component
  that was read for issuer, lender and venue.
- **Issuer 422 on cETH**: not repeated on the on-chain-verifiable instrument (proven on CBTC).
- **API key use**: the minted key was shown once; confirming *with* the key over
  `Authorization: Bearer ck_…` was not exercised.
- **Interaction method**: the flows were driven through the running React app in Chrome via DOM
  events (native value setter + `input` event, `.click()`), not pixel clicks — the same handlers,
  validation and fetches ran; screenshots were flaky in that tab.
- Window/deadline: every confirm above landed inside the 30-minute window shown on the card; the
  backend does not itself reject after the deadline (not tested).
- The scheduler ran a strike at boot (the 16:00 London slot had passed) and published today's row
  as **tier 5 missed** before any seat had signed; `/api/series/CBTC` shows that row. Unchanged.
- An untracked `evm-vault/` directory appeared in the repo root at 15:35 during this session; it
  was not created by this walkthrough and was left alone.

## The stack recipe that worked

Ledger and backend in WSL Ubuntu (Daml SDK 3.4.11 under `/home/superbigroach/.daml`, Java 17),
Vite on the Windows host. Every WSL background process must be started with `setsid nohup … &
disown` or it dies when the `wsl -e` call returns.

```bash
# 1. sandbox (WSL) — ports 6900 ledger / 6901 admin / 6902 seq public / 6903 seq admin / 6904 mediator / 6905 json-api
cd /mnt/c/CrossDesk/canton-dvp-settlement-desk
setsid nohup ~/.daml/bin/daml sandbox --no-legacy-assistant-warning --port 6900 --admin-api-port 6901 \
  --sequencer-public-port 6902 --sequencer-admin-port 6903 --mediator-admin-port 6904 --json-api-port 6905 \
  --port-file /tmp/ledger6900.portfile --dar .daml/dist/crossdesk-3.0.0.dar > /tmp/sandbox6900.log 2>&1 < /dev/null & disown
# ready when /tmp/sandbox6900.log says "Canton sandbox is ready." (~1 min); --dar uploads the DAR

# 2. seed (WSL, once per sandbox, ~1 min)
~/.daml/bin/daml script --no-legacy-assistant-warning --ledger-host localhost --ledger-port 6900 \
  --dar .daml/dist/crossdesk-3.0.0.dar --script-name Test:initialize

# 3. backend jar — build on the WINDOWS side (the WSL gradle cache lacks two google deps offline)
cd C:\CrossDesk\canton-dvp-settlement-desk\backend && ./gradlew build -x test --offline --no-daemon -q

# 4. backend (WSL, so the gRPC streams do not cross the WSL network boundary)
cd /mnt/c/CrossDesk/canton-dvp-settlement-desk/backend
AUTH_MODE=sandbox LEDGER_HOST=localhost LEDGER_PORT=6900 LEDGER_TLS=false SERVER_PORT=8080 \
  DATA_DIR=/tmp/crossdesk-qa-data OPERATOR_PARTY=Operator \
  setsid nohup java -jar build/libs/canton-dvp-desk-1.0.0.jar > /tmp/backend8080.log 2>&1 < /dev/null & disown
# "Started SettlementDeskApplication" in ~5 s; DemoSeed then allocates Custodian + TransferAgent and
# seeds the 2-of-5 committee. DATA_DIR is outside the repo because backend/data is NOT gitignored.
# To restart: kill and start in SEPARATE shells — `pkill -f "java -jar build/libs/canton-dvp-desk"`
# in the same command line as the `java -jar …` launch matches its own shell.

# 5. frontend (Windows host)
cd C:\CrossDesk\canton-dvp-settlement-desk\frontend
VITE_AUTH_MODE=sandbox VITE_API_MOCK=0 npx vite --port 5173 --strictPort     # proxies /api -> :8080

# 6. checks
curl -H 'X-Sandbox-User: issuer@sandbox.crossdesk' localhost:8080/api/me     # -> seat issuer, party Issuer
curl localhost:8080/api/parties                                             # Custodian + TransferAgent present
CID=$(curl -s -H 'X-Sandbox-User: s.borjas@lucilla.ca' localhost:8080/api/admin/committees | jq -r '.[0].committeeCid')
curl -X POST -H 'Content-Type: application/json' localhost:8080/api/committee/$CID/propose \
  -d '{"proposer":"Operator","instrumentId":"CBTC","session":"Close","price":65000,"rationale":"QA","asOfDate":"2026-09-24"}'
# open http://localhost:5173/desk/login and pick a seat
```

Proposals used: CBTC Close 2026-09-24 and cETH Close 2026-09-24 (all seats confirm), plus one
per seat for the refusals (cETH Open / CBTC Open 2026-09-24, cETH Close / CBTC Close 2026-09-23,
cETH Close 2026-09-22). The raw `/propose` route accepts any `asOfDate`; only the strike runner
enforces one open proposal per instrument, session and day.

## Processes

Sandbox (WSL, pid 457/1129), backend (WSL), Vite (Windows pid 3646512) and the headless browse
daemon were all stopped at the end of the session; `/tmp/crossdesk-qa-data` was removed.
