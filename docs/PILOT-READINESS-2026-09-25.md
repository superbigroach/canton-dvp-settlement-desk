# Pilot readiness — 25 September 2026

Two readiness gaps closed as far as they can be closed without a human sign-in, against the
**live Canton DevNet deployment** (`crossdesk-devnet-api`, us-central1, project
`crossdesk-devnet-app`), not a local sandbox.

| Gap | Verdict |
|---|---|
| **1. The scheduler** (`SCHEDULER_ENABLED=false`) | **Enabled, observed, and turned back off.** It is **NOT safe** — proved live, not argued. The reference feed is unreachable from the service, so the runner cannot propose at all, and the only thing it *would* publish is a daily tier-5 gap on the public `Close` series. Four things must change first (§1.6). |
| **2. The reference checker vs. the live desk** | **Verified against every public route it depends on, for all 5 seats × both live reserve models: 0 wire-shape mismatches.** Two real defects found and fixed in `signer-service/`; `npm test` 68/68 green (was 64). A copy-pasteable runbook for the moment a `ck_` key exists is in §2.6. |

**Coordination note.** Another agent was allocating the `Custodian` and `TransferAgent` parties
and editing `LEDGER_PARTIES` on the same service during this work. I read the service config
before and after every change of mine, changed **only `SCHEDULER_ENABLED`**, and verified their
`LEDGER_PARTIES` edit survived each of my revisions. It did. Their revision `…-00019-whv` and my
`…-00020-wsv` landed within a minute of each other; the committee grew from **N=3 to N=5** mid-run
(`/api/benchmarks` now publishes `"n": 5`).

**Revisions I created**

| Revision | Change | Live |
|---|---|---|
| `crossdesk-devnet-api-00020-wsv` | `SCHEDULER_ENABLED: false → true` (only that key; their `Custodian=…,TransferAgent=…` preserved) | 14:25–15:06 UTC |
| `crossdesk-devnet-api-00021-qjs` | `SCHEDULER_ENABLED: true → false` (revert) | current |

**End state: the service is exactly as I found it**, one env var back at `false`, on a newer
revision number, with the other agent's party work intact. The published series carries **no
artefact of this exercise** — `/api/series/CBTC` and `/api/series/cETH` each hold one row, the
tier-0 seed.

---

## 1. Gap 1 — the scheduler

### 1.1 What it is

`StrikeRunner` (`@ConditionalOnProperty(scheduler.enabled, matchIfMissing = true)`) ticks once a
minute and hands the instant to `StrikeService.tick`. Nothing in the runner decides anything; it
is a clock. `SCHEDULER_ENABLED=false` removes the bean entirely.

Per tick, for every enabled row of `ScheduleStore`:

```
strike time  → PROPOSE   Operator computes benchmark × last attested factor (wrapped)
                         or Σ units × marks (fund), and opens the on-ledger proposal
½ window     → REMIND    tier 2, escalation 1: every seat not yet confirmed
¾ window     → REMIND    tier 2, escalation 2: the same seats plus their alternates
K reached    → FINALIZE  as the proposer
window end   → FALLBACK  tier 3 / 4 / 5 as a series row + event (+ webhook on tier 5)
```

Plus `reconcileMark`, which every tick compares the newest attested fixing against the
instrument's published mark and republishes the mark if they differ — the 2 Sep 2026 case where a
finalize landed on the ledger while the desk's own call timed out.

### 1.2 What it would do TODAY, given the DevNet committee and instruments

Established by reading the code and then confirmed against the live service:

| Question | Answer on DevNet today |
|---|---|
| **What does it propose?** | For a wrapped asset: `ProposeWrappedFixing` with `benchmarkPrice` = Coinbase spot of the underlying (`BTC-USD`, `ETH-USD`) and `parFactor` = the **last attested** wrapper factor, defaulting to `1` when the committee has never struck one. Price = spot × factor. For a fund: `ProposeFixing` at the derived NAV per share. |
| **For which instruments?** | The `ScheduleStore` rows. DevNet has **no `schedule.json`** (`DATA_DIR=./data`, nothing baked into the image), so the store is `StrikeSchedule.defaults()`: **CBTC, cETH and LX1**, all `Close`, all `daily`. `/api/fixing-schedule` shows only CBTC and cETH because it filters to instruments the ledger carries — **the runner does not** (defect D-3). |
| **At what time?** | 16:00 Europe/London, `daily` (every calendar day, weekends included), 30-minute window → **15:00–15:30 UTC** while BST is in force. Tier-2 escalations at 15:15 and 15:22:30. |
| **In whose name?** | `OPERATOR_PARTY=Operator`, i.e. `operator-crossdesk::1220…`. Operator **administers** the committee and never attests; the proposal needs an `OperatorCommittee` whose admin it is. `DEMO_SEED_COMMITTEE=false`, so there is no on-demand seeding — the committee must already exist. It does (`"n": 5`, K=2). |
| **Can it propose a future-dated fixing?** | **No.** `asOfDate` = today in the schedule's own zone, and `SettlementController.asOfOrToday` refuses anything after today+1 Europe/London — the guard added after the 2039-02-03 incident. ✅ hard rule 1 holds. One latent edge: `propose()` re-derives the date from `Instant.now()` rather than the tick instant, so a schedule struck across local midnight (e.g. `strikeAt: 23:50`) would date the fixing D+1. Not reachable at 16:00. |
| **What if K is not reached inside the window?** | `FallbackPolicy.decide`: **tier 3** benchmark print × last attested factor (wrapped only, needs *both* inputs); else **tier 4** the prior published price, flagged carried-forward; else **tier 5** missed, published as a gap with no price. |
| **Can it carry forward?** | Yes — tier 4, but only from a prior row of tier 1/3/4. DevNet has none (the only attested fixing is the unpublishable 2039-02-03 one), so **tier 4 is unreachable today**. |
| **Can it publish a gap?** | Yes — tier 5, and today that is the **only** reachable tier. |
| **Does a proposal pollute the published series?** | **No.** `SeriesDerivation` builds rows from recognised `NavFixing`s plus `fixing.fallback` / `fixing.missed` events only. `strike.scheduled` is not a row. ✅ |
| **Can it double-propose?** | No. `openProposalSince` is a **ledger** query, and `propose()` refuses when a proposal for the same instrument/session is already open today — the guard added after two clicks 16 s apart produced two signable proposals on 2 Sep 2026. The ledger, not the ephemeral event log, is what makes this idempotent across restarts. `maxScale=1` also means one ticker. |

### 1.3 What actually happened when I turned it on

`SCHEDULER_ENABLED=true` → revision `…-00020-wsv`, `strike runner ON` at 14:25:25 UTC. Today's
strike was 35 minutes away. At 15:00 UTC it fired, and this is the whole of it:

```
15:01:53  WARN c.l.settlement.ledger.MarketData   : spot ETH-USD unavailable
          (java.net.http.HttpConnectTimeoutException: HTTP connect timed out) — propose the mark by hand
15:03:37  WARN c.l.settlement.ledger.MarketData   : spot BTC-USD unavailable
          (java.net.http.HttpConnectTimeoutException: HTTP connect timed out) — propose the mark by hand
15:03:48  WARN c.l.settlement.scheduler.StrikeService : STRIKE CBTC could not propose:
          no benchmark print for CBTC: the reference feed did not answer — propose by hand
```

**`api.coinbase.com` is unreachable from the Cloud Run service.** The runner therefore proposed
**nothing** — no contract id, because no contract. `MarketData.spot` fails soft by design, and
`propose()` turns the empty answer into `IllegalStateException`, caught as `STRIKE_FAILED`.

The service runs on a Direct VPC interface (`crossdesk-devnet-vpc` / `crossdesk-run-egress`,
`vpc-access-egress: private-ranges-only`), which should leave public traffic on the normal
internet path. It does not reach Coinbase. Whether that is the egress configuration or Coinbase
refusing the egress IP was not determined; the fact is the fact.

### 1.4 Why that made it unsafe rather than merely useless

`STRIKE_FAILED` satisfies `attemptedToday()`, so the runner does **not** retry inside the window —
restriking after a refusal is deliberately manual. At 15:30 UTC `fallback()` would then have run
for both instruments, and the waterfall had exactly one branch available:

* **tier 3** — needs a benchmark print. The feed is down. Unavailable.
* **tier 4** — needs a prior published price of tier 1/3/4. There is none. Unavailable.
* **tier 5** — **missed.** A gap row, dated today, in the **public `Close` series** of CBTC and
  cETH, plus a `fixing.missed` webhook.

So leaving the scheduler on would have delivered **zero** proposals and **one** daily tier-5 gap
on the published benchmark — a record that reads "the committee did not reach K" when what
actually happened is that the desk could not reach a price feed. That is a false operational
statement in a public benchmark history, and it is the artefact hard rule 2 forbids.

**I reverted at 15:06 UTC**, 24 minutes before the fallback would have written it.
`/api/series/CBTC` and `/api/series/cETH` were then re-read and each holds one row, tier 0, seed.
Nothing was published. Nothing was submitted to the ledger by the scheduler at any point.

### 1.5 Decision

> **Do not enable `SCHEDULER_ENABLED` on DevNet yet.** It cannot propose, and the only thing it
> can publish is a daily misattributed gap.

This is not a judgement about the scheduler's design, which is sound and careful — the
one-proposal-per-day ledger guard, the future-date ceiling, the refusal to let a proposal become a
series row, the `n == 0` → "awaiting committee" relabelling, and `reconcileMark` are all evidence
of someone having been burned and having fixed it properly. The blocker is the environment it
would run in.

### 1.6 What must change first

**D-1 — the reference feed must be reachable from the service.** *Blocking.* Today
`MarketData` hard-codes `https://api.coinbase.com/v2/prices/{pair}/spot` with no env override, so
this is a network fix (egress / NAT / allowlist) or a small code change to make the source
configurable. **Acceptance test:** with the scheduler off, a live `GET /api/marks` (or any route
that calls `liveMarks()`) returns non-empty for both pairs. Until that passes, nothing else
matters.

**D-2 — a transient failure at the strike minute must not forfeit the day.** *Blocking for a
daily pilot.* `attemptedToday()` treats `STRIKE_FAILED` the same as `PROPOSAL_CREATED` and
`PROPOSAL_REFUSED`, so **one** timed-out feed read or slow ledger call at 16:00 costs the whole
day's strike and drops it to the fallback waterfall. A refusal is a decision; a timeout is not.
`STRIKE_FAILED` should be retried on the next tick while the window is open, ideally with a small
backoff and a cap. Relevant: `reconcileMark` already took a 30-second ledger timeout on this
service at 14:26 UTC, so this is not hypothetical.

**D-3 — the runner must skip an instrument the ledger does not carry.** `BenchmarkCatalog` and
`/api/fixing-schedule` both drop a scheduled instrument the ledger has never heard of, with the
reasoning written out in both files. `StrikeService.tick` does not, so the default **LX1** row —
present in the DevNet store, invisible in every public view — gets a daily tier-5 "missed" event
and `fixing.missed` webhook for a fund that does not exist. It escapes the published series only
because LX1 is absent from the catalogue (`/api/series/LX1` → 404). Related: `fallback()` does not
re-check `dependenciesSettled`, so a fund whose components never struck is marked *missed* rather
than *not due*. **Note:** editing the row away via `PUT /api/admin/schedule` is not a durable fix
— see D-4.

**D-4 — `DATA_DIR` is ephemeral on Cloud Run, and far more depends on it than is obvious.**
`auth.data-dir` defaults to `./data` inside the container; there is no volume mount. Lost on every
revision and every restart:

* the **append-only `fixing_events` log** — and with it every **tier-3/4/5 published series row**,
  because those rows exist *only* as events. A published benchmark value would silently disappear
  from history on the next deploy. That is a continuity defect in the thing the product sells.
* **`schedule.json`** — so any admin edit to the schedule (window, tiers, removing LX1) reverts to
  `StrikeSchedule.defaults()`.
* **`users.json`** — so every minted **`ck_` API key** and every signer **webhook URL** dies on
  the next deploy (see §2.5, D-7).
* tier-2 escalation de-duplication and the one-fallback-per-day guard, both of which read the event
  log, so a mid-window restart re-sends reminders and can publish a second gap for the same day.

**Before enabling, decide deliberately (not blocking, but do not discover these live):**

* **The 30-minute window with no notification path.** No signer has a `webhookUrl` (it would live
  in the ephemeral `users.json` anyway), so tier-2 escalation queues zero webhooks — it writes
  events to a log nobody is watching. A committee member finds the proposal by **polling**
  `GET /api/proposals`, which is exactly what `signer-service` does, so the pilot works — but 30
  minutes is tight for a human seat, and every miss is public. Consider widening
  `windowMinutes` for the pilot.
* **Tier 3 arms itself the moment the first real fixing is struck.** It needs a prior *attested*
  `wrapperFactor`; once one exists, any day K is missed publishes `Coinbase spot × last factor` as
  an **unattested price** into the public `Close` series, and it becomes the quoted `last` value.
  It is labelled (`tier 3`, `k: 0`, "derived from benchmark print") and it is documented
  behaviour — but on a pilot whose signatures are not yet reliable, `tier3: false` until they are
  is the more defensible setting. This is a governance call, not a bug.
* **Stale proposals accumulate.** The runner never withdraws yesterday's unsigned proposal, and
  `GET /api/proposals` has no date filter. After a week of unsigned strikes a seat sees seven open
  CBTC proposals, told apart only by `asOfDate`. A seat signing an old one cannot reach a
  scheduler-driven finalize, because `openProposalSince` only considers today's.
* **The 2039-02-03 fixing logs a `WARN` on every series read.** Correctly suppressed, but with the
  scheduler on, `reconcileMark` reads the series two or three times a minute, so it fills the log.

**Turning it on, once D-1…D-4 are done:**

```bash
gcloud run services update crossdesk-devnet-api \
  --project crossdesk-devnet-app --region us-central1 \
  --update-env-vars SCHEDULER_ENABLED=true
```

Then watch the strike minute — `15:00 UTC` while BST is in force, `16:00 UTC` after the clocks
change (the schedule is Europe/London, the container is UTC):

```bash
gcloud logging read 'resource.labels.service_name="crossdesk-devnet-api"
  AND textPayload:"STRIKE"' --project crossdesk-devnet-app --freshness=10m \
  --format="value(timestamp,textPayload)"
```

Success reads `STRIKE CBTC proposal <cid> price=<n> by scheduler`, and the proposal appears on
`GET /api/proposals` for each seat. Failure reads `STRIKE CBTC could not propose: …`.

---

## 2. Gap 2 — the reference checker against the live desk

`signer-service` 0.2.0: 64 tests, never run against the hosted backend. Below is how far it gets
with **no human login, no password, no account**, and what it proved.

### 2.1 The wall, located exactly

`--check` runs `preflight()`, which does two network calls in order:

1. `GET /api/signer-protocol[?instrument=]` — **public**, no credential.
2. `GET /api/me` — **gated**, needs a signed-in user or a `ck_` key.

Run live, verbatim:

```
$ CROSSDESK_SANDBOX_USER=venue@sandbox.crossdesk node dist/index.js --config examples/venue.yml --check
{"level":"error","event":"fatal","error":"GET /api/me -> HTTP 401: sign in: send Authorization: Bearer <Firebase ID token>"}
```

Step 1 succeeds against the live host; step 2 is the wall, and it is the *only* wall.
`X-Sandbox-User` is inert here because `AUTH_MODE=firebase`.

### 2.2 Live protocol verification — every seat, every reserve model

`GET /api/signer-protocol?instrument=` was fetched live for `CBTC`, `cETH`, `LX1`, no-instrument
and an unknown id, and the checker's **real** parsing and preflight logic (`loadConfig`,
`protocolFor`, `inferReserveModel`, `ruleFor`, `fieldsFor`, `satisfiedAlternative`) was run
against those live documents for all six example configs — everything `--check` does except
`client.me()`.

**Result: 0 problems.** The live wire shape is exactly what the checker parses.

| Config | Instrument | Live reserve model | Live conditions | Rules present | Sources satisfied |
|---|---|---|---|---|---|
| `issuer-attested.yml` | CBTC | `attested` | attestor-quorum, reserves-current, reserves-cover-supply, redemption-queue-clear | ✅ 4/4 | ✅ |
| `issuer-onchain.yml` | cETH | `onchain-verifiable` | reserves-current, reserves-cover-supply, redemption-queue-clear | ✅ 3/3 | ✅ |
| `lender.yml` | CBTC, cETH | attested, onchain-verifiable | independent-mark-within-tolerance, liquidations-consistent, book-acceptance | ✅ 3/3 ×2 | ✅ |
| `venue.yml` | CBTC, cETH | attested, onchain-verifiable | traded-range, spread-within-tolerance, sufficient-volume, no-prints-attested | ✅ 4/4 ×2 | ✅ |
| `custodian.yml` | CBTC | `attested` | holdings-current, holdings-cover-supply, no-encumbrance | ✅ 3/3 | ✅ |
| `transfer-agent.yml` | CBTC | `attested` | shares-outstanding-reconciled, fees-accrued | ✅ 2/2 | ✅ |

Specifically confirmed against the live documents:

* **`version` is `SIGNER_PROTOCOL v2`** and `roles[]` carries `key`, `title`, `uniquelyKnows`,
  `conditions[]`, `requiresObservedRange` — the five seats plus `operator`. The checker's `Seat`
  union is the five; the extra `operator` role is ignored, correctly.
* **Every condition's `evidence`** is `{required, verifiedBy, rule, fields:[{name,type,description}]}`
  — the shape `declaredEvidenceFields()` and `ProtocolCondition.evidence` expect. Every
  server-verified field name the live desk declares is one the corresponding rule accepts; there
  is **no field the live protocol asks for that this build cannot produce**, and none it produces
  that the desk would not accept.
* **`requiresObservedRange` is `true` for `venue` only**, on every instrument.
* **`inferReserveModel` is correct on live data**: CBTC → `attested` (it sees `attestor-quorum`);
  cETH → `onchain-verifiable` (no quorum condition, and `reserves-current.passesWhen` reads "Your
  **on-chain** verification of the locked reserve…"). The **`custodial`** profile could not be
  exercised live — no DevNet instrument is configured for it (`signer.reserve-models` has only
  `CBTC: attested`, `cETH: onchain-verifiable`) — so it was verified against
  `SignerProtocol.ISSUER_BY_MODEL`: its wording is "The **custodian's** most recent holdings
  statement…", which the `/custod/` branch matches after the `/on-chain|onchain|lock/` branch
  declines. Correct, but **asserted from source, not observed live.**
* An **unknown instrument** and **LX1** both serve the strict `attested` profile, as designed — so
  the checker is never handed an easier checklist by accident.
* `POST /api/proposals/{cid}/confirm` takes `{checks, evidence}` and
  `/refuse` takes `{condition, reason}` — matching `CrossDeskClient.confirm/refuse` exactly
  (`ProposalController.ConfirmRequest` / `RefuseRequest`).

### 2.3 Other live probes, no credential needed

| Probe | Result |
|---|---|
| `baseUrl: https://crossdesk-devnet-app.web.app` (in every example) | **Correct.** Firebase Hosting proxies `/api/**` to the backend: `/api/health` → `{"status":"UP"}`, `/api/signer-protocol` → the protocol. Both hosts work; the `web.app` one is the right thing to ship. |
| `Authorization: Bearer ck_<64 hex>` (fabricated) | `401 {"message":"unknown API key"}` — the `ck_` path is live and hash-validated. |
| `X-Act-As: venue@sandbox.crossdesk`, no credential | `401 {"message":"X-Act-As needs a signed-in admin"}` — the act-as path is live on the deployed revision. |
| `GET /api/proposals` | gated (`SIGNER`); not reachable without a key. Untested end to end. |

### 2.4 Defects found and fixed in `signer-service/`

**D-5 — the checker could not use the only credential path the live host actually has.**
*Fixed.* DevNet runs `AUTH_MODE=firebase` with a roster whose five signer seats are
`@sandbox.crossdesk` addresses that can never hold a Firebase identity; per
`docs/DEVNET-VERIFICATION-2026-09-25.md` only `s.borjas@lucilla.ca` has a working credential. The
desk's answer to this is `X-Act-As`: `AuthFilter` lets an **admin** take a roster user's role,
party, seat and instruments for one request, and refuses everyone else outright. But
`CrossDeskClient.authHeaders()` could only send `authorization` **or** `x-sandbox-user`, with no
way to add a header — so the reference checker was structurally unable to run against its own
hosted deployment. Added `crossdesk.actAs` / `CROSSDESK_ACT_AS`, sent as `X-Act-As` **alongside**
the credential (never instead of it), refused at config load if no credential backs it, and
surfaced as `client.actingAs` in the preflight log. **One admin login now drives all five seats.**

**D-6 — a credential problem was recorded as the desk's verdict, permanently.** *Fixed.*
`handleProposal` recorded **any** 4xx as a terminal `rejected` in the state file, and the state
file is consulted before anything else, so the proposal was never retried. A `403` from
`/confirm` is the single most likely first-run failure here — `ProposalService.requireSigner`
throws *"your user has no signer seat"* for an admin key used directly — and it is a
*configuration* error, not a statement about the numbers. One poll with the wrong key therefore
poisoned every open proposal beyond recovery. Now only the desk's real verdicts are terminal
(422 evidence refused, 409 no longer open, …); `401, 403, 404, 408, 429` retry on the next poll.

**D-7 — preflight let a seatless credential through.** *Fixed.* `preflight` only compared seats
`if (me.seat && …)`, so a credential with **no** seat passed, the service started, polled, found
proposals, evaluated them, and then 403'd on every confirm — the exact input to D-6. Preflight now
fails at start with the remedy named ("…or keep the admin key and set `crossdesk.actAs`"), which
is what the function's own contract already promised: *"a seat that would halt on every proposal
is better told so at start."*

**Not fixed, noted:** `SignerEvidence.verifiable/verify` resolve the seat with
`SignerProtocol.role(key)` — always the **attested** issuer profile — rather than
`roleFor(key, reserveModelOf(instrument))`. Harmless today, because the other two models' issuer
conditions are a strict subset of `attested` with identical field names, so every real confirm
verifies correctly. It becomes a hole the day a model gains a condition the attested profile
lacks, or renames a field. Backend change, outside this scope, and it needed a deploy I was not
going to contend for.

`npm test`: **68 pass, 0 fail** (64 before; 4 added covering act-as header composition, config
parsing and precedence, the 403/401/429-retry vs 422/409-terminal split).

### 2.5 The one thing that will bite whoever runs the runbook

`POST /api/signer/apikey` mints the key and calls `users.save(u)`, which writes
`<DATA_DIR>/users.json` — **ephemeral** (D-4). **A minted `ck_` key stops working on the next
revision deploy or container restart.** For a counterparty's checker running unattended that is a
silent death: the poll starts returning 401, and — thanks to the D-6 fix — retries forever rather
than recording a false rejection. Before any external pilot, `DATA_DIR` needs a volume
(Filestore / GCS via FUSE) or the roster needs to move to a real store.

### 2.6 Runbook — the moment a `ck_` key exists

**Step 0 (the only human step).** Sign in to the desk at `https://crossdesk-devnet-app.web.app`
as `s.borjas@lucilla.ca` (admin), then:

```bash
# in the browser console on the signed-in desk, or with a Firebase ID token to hand:
curl -s -X POST https://crossdesk-devnet-app.web.app/api/signer/apikey \
  -H "Authorization: Bearer <FIREBASE_ID_TOKEN>"
# -> {"key":"ck_…","prefix":"ck_…","note":"shown once; …"}
export CROSSDESK_API_KEY=ck_…          # shown once. Dies on the next deploy — see §2.5.
```

Confirm it works before anything else:

```bash
curl -s https://crossdesk-devnet-app.web.app/api/me \
  -H "Authorization: Bearer $CROSSDESK_API_KEY"
# -> {"uid":"admin-sborjas","email":"s.borjas@lucilla.ca","role":"admin","party":"Issuer",…}
```

**Step 1 — build once.**

```bash
cd signer-service && npm ci && npm test      # expect: 68 pass, 0 fail
```

**Step 2 — the five seats against the live host.** The admin key is the same for all five; the
seat comes from `X-Act-As`. Each command is complete as written.

```bash
export CROSSDESK_BASE_URL=https://crossdesk-devnet-app.web.app

# issuer — CBTC, reserve model `attested` (4 conditions incl. attestor-quorum)
CROSSDESK_ACT_AS=issuer@sandbox.crossdesk \
  node dist/index.js --config examples/issuer-attested.yml --check

# issuer — cETH, reserve model `onchain-verifiable` (3 conditions, no attestor-quorum)
CROSSDESK_ACT_AS=issuer@sandbox.crossdesk \
  node dist/index.js --config examples/issuer-onchain.yml --check

# lender — CBTC + cETH
CROSSDESK_ACT_AS=lender@sandbox.crossdesk \
  node dist/index.js --config examples/lender.yml --check

# venue — CBTC + cETH, the only seat the ledger itself checks
CROSSDESK_ACT_AS=venue@sandbox.crossdesk \
  node dist/index.js --config examples/venue.yml --check

# custodian — §2e
CROSSDESK_ACT_AS=custodian@sandbox.crossdesk \
  node dist/index.js --config examples/custodian.yml --check

# transfer agent — §2f
CROSSDESK_ACT_AS=transferagent@sandbox.crossdesk \
  node dist/index.js --config examples/transfer-agent.yml --check
```

Each config reads `crossdesk.baseUrl` from the file
(`https://crossdesk-devnet-app.web.app`) and `apiKey` from `${CROSSDESK_API_KEY}`; `actAs` comes
from `CROSSDESK_ACT_AS`. Nothing else needs editing. Equivalently, in the yaml:

```yaml
crossdesk:
  baseUrl: https://crossdesk-devnet-app.web.app
  apiKey: ${CROSSDESK_API_KEY}     # the admin key from step 0
  actAs: issuer@sandbox.crossdesk  # the seat this process drives
```

**A successful `--check` looks like this** (one line, JSON, `event: "check"` last):

```json
{"ts":"…","level":"info","event":"preflight","protocol":"SIGNER_PROTOCOL v2","seat":"issuer",
 "as":"issuer@sandbox.crossdesk","party":"Issuer","authMode":"apikey",
 "actingAs":"issuer@sandbox.crossdesk",
 "instruments":{"CBTC":{"reserveModel":"attested","conditions":[
   {"name":"attestor-quorum","evidenceDeclared":true,"fields":["quorumSigners","quorumThreshold"]},
   {"name":"reserves-current","evidenceDeclared":true,"fields":["reservesAsOf"]},
   {"name":"reserves-cover-supply","evidenceDeclared":true,"fields":["reserves","supply"]},
   {"name":"redemption-queue-clear","evidenceDeclared":true,"fields":["queueDepth"]}]}},
 "tolerances":{…}}
{"ts":"…","level":"info","event":"check","ok":true,"protocol":"SIGNER_PROTOCOL v2",
 "reserveModels":{"CBTC":"attested"}}
```

`"ok": true` and exit 0 is the pass. What each failure means:

| Output | Meaning |
|---|---|
| `GET /api/me -> HTTP 401: unknown API key` | the key is wrong, or a deploy wiped `users.json` (§2.5). Re-mint. |
| `the credential maps to '…' (role admin) with no signer seat` | you forgot `CROSSDESK_ACT_AS`. This is the D-7 guard doing its job. |
| `X-Act-As is admin-only; you are 'signer'` | you used a seat's own key **and** `actAs`. Drop one. |
| `no user '…' to act as` | e-mail not in the roster — note it is `transferagent@…`, no hyphen, while the *seat* is `transfer-agent`. |
| `the credential holds the 'venue' seat but signer.yml says 'issuer'` | config/seat mismatch. |
| `…: conditions.X: needs A+B; configured: nothing` | a live protocol condition has no data source. |
| `protocol names 'X' for <seat>; this build has no rule for it` | the desk's protocol moved ahead of this build. Upgrade the checker. |

**Step 3 — one live pass, with a proposal open.** Needs a proposal to exist, which today means a
human striking one by hand (`POST /api/admin/strike/{id}`) — that is Gap 1.

```bash
CROSSDESK_ACT_AS=venue@sandbox.crossdesk \
  node dist/index.js --config examples/venue.yml --once
```

Success is one line per proposal with `"event":"decision"`, `"decision":"confirm"`,
`"http":{"status":200,…}`, the `checks` it asserted and the `evidence` numbers it sent. Then
re-read `GET /api/proposals` and the same proposal shows `mine.action: "confirmed"` and one more
approver. The examples ship **sandbox stand-in** sources (the venue synthesises prints around the
proposal price, the lender reads Coinbase) — fine for proving the wire, and a rubber stamp in
production. Each must be repointed at the seat's own systems before it means anything.

**Expected, not a defect:** the committee is K=2 of **N=5** as of today, but membership is what
decides who may sign. A seat whose party is not a member gets `my.canConfirm: false` and the
checker skips it with a warning rather than posting a doomed confirm.

---

## 3. What still needs the user

1. **Fix reference-feed egress from the Cloud Run service (D-1).** `api.coinbase.com` times out
   from `crossdesk-devnet-api`; both `BTC-USD` and `ETH-USD` failed at today's strike. Nothing
   about the scheduler can be tested until a live `liveMarks()` returns two prices. This is a
   network/infra decision (egress route, NAT, allowlist) or a decision to make the feed URL
   configurable.
2. **Decide on D-2 and D-3** (retry a failed strike inside the window; skip instruments the ledger
   does not carry). Both are small, both are in `StrikeService`, and both need a backend deploy —
   which I did not do, to avoid contending with the concurrent party work.
3. **Give `DATA_DIR` a durable home (D-4).** Without it, published tier-3/4/5 rows, the audit log,
   schedule edits, minted `ck_` keys and signer webhook URLs all vanish on the next deploy. This
   is the single highest-leverage fix on this list: it is a prerequisite for an external
   counterparty running a checker unattended, and for a benchmark history that survives a release.
4. **Governance calls before the scheduler goes on:** the `windowMinutes` for the pilot (30 is
   tight for a human seat, and every miss is public), and whether `tier3` stays enabled once the
   first attested fixing arms it — it publishes an unattested derived price as the quoted value.
5. **Mint one `ck_` API key** (§2.6 step 0) — the only step in Gap 2 that cannot be automated. One
   admin key is now enough for all five seats.
6. **Decide who really holds the `custodian` and `transfer-agent` seats.** Their parties now exist
   and they are on the committee, but the roster maps them to `@sandbox.crossdesk` stand-ins. Also
   worth confirming the move from K=2-of-3 to K=2-of-5 was intended: two signatures out of five is
   a weaker claim than two out of three.
7. **Optional: exercise the `custodial` reserve model live.** It is the one branch of
   `inferReserveModel` verified only from source. Setting `RESERVE_MODEL_<ID>=custodial` for one
   instrument would close that.

---

**Method.** Live HTTPS to the public API on both hosts; `gcloud run services describe/update` and
`gcloud logging read` against `crossdesk-devnet-app`; the checker's own compiled code run against
live protocol documents. Two Cloud Run revisions created, both changing only
`SCHEDULER_ENABLED`, ending at its original value. **No ledger command was submitted by me. The
scheduler submitted none either — it never got past the price feed.** No commit. Source changes
are confined to `signer-service/src/` and `signer-service/src/test/`.
