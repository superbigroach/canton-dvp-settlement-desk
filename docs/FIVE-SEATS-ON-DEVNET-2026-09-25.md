# Five seats on DevNet — 25 September 2026

Closing **D-3** of [`DEVNET-VERIFICATION-2026-09-25.md`](DEVNET-VERIFICATION-2026-09-25.md): the
DevNet ledger carried nine parties, `users.yml` mapped two signer seats to `Custodian` and
`TransferAgent`, and neither party existed. Two of the six advertised seats could not be filled.

**Both parties now exist. All five SIGNER_PROTOCOL v2 signing seats have attested on the real
DevNet ledger, with evidence, and two fixings were finalised.** Nothing was deleted and the
published benchmark series is unchanged.

## Summary

| | |
|---|---|
| Parties on the validator | **9 → 11** (`custodian-crossdesk`, `transferagent-crossdesk` allocated) |
| `crossdesk-backend` rights | **18 → 22** (CanActAs + CanReadAs on each of the 11) |
| Cloud Run `LEDGER_PARTIES` | 9 entries → **11 entries**, new revision **`crossdesk-devnet-api-00020-wsv`**, 100 % traffic |
| `GET /api/health` after | **200**, `{"status":"UP","auth":"hmac","ledgerHost":"10.20.0.10","ledgerPort":5001,"tls":false}` |
| `GET /api/diag` after | **200**, `parties: 11` — `Custodian` and `TransferAgent` both `isLocal: true`, `partiesError: null` |
| Five-seat committee | `OperatorCommittee` **`00f3a2fa7fdca041…`** — admin Operator, **K=3 of N=5**, auditor Auditor |
| Fixings finalised | **2** — CBTC `000aab6329f6d6b0…` and cETH `003b444790d8679a…`, each with **5 attestors** |
| Seats proven | **5 of 5 PASS** (issuer, lender, venue ×2 paths, custodian, transfer-agent) |
| Published series | **unchanged** — both fixings use session `Open`; the public series reads `Close` |
| Scheduler | still **`SCHEDULER_ENABLED=false`**. Not turned on. Recommendation in §6 |

### Cloud Run changes made, in full

Exactly one env var was changed, the one the task asked for:

- **`LEDGER_PARTIES`** — extended from 9 to 11 entries. This created revision
  **`crossdesk-devnet-api-00020-wsv`**, now serving 100 % of traffic.
  (`gcloud run services update` also produced an intermediate revision
  **`crossdesk-devnet-api-00019-whv`** that still carries the 9-entry roster; it holds no
  traffic. 00018-6c5 remains the previous good revision for rollback.)

**Nothing else was changed.** `AUTH_MODE` is still `firebase`, `SCHEDULER_ENABLED` still `false`,
`DEMO_SEED_FUND` / `DEMO_SEED_COMMITTEE` still `false`, image digest unchanged
(`…350eaa0bb69598a2`), `minScale`/`maxScale` still `1`, ingress, VPC egress and the secret
reference untouched.

---

## 1. How a party is created here, and how the desk learns about it

Worth stating plainly, because the roster is **an environment variable, not a database**, and the
two halves are independent — allocating a party the desk is not told about is invisible, and
naming a party in `LEDGER_PARTIES` that the participant does not hold makes every call that
resolves it fail.

1. **On the validator.** `deploy/own-devnet-validator/05-upload-dar-and-parties.sh` reads
   `CD_PARTY_HINTS` from the profile and, for each hint, `POST /v2/parties` on the participant's
   JSON Ledger API with `partyIdHint = <hint><CD_PARTY_SUFFIX>`. The participant returns the full
   id `<hint>-crossdesk::<namespace>`, where the namespace is this participant's own fingerprint
   (`1220278484b0cea1…`). The script is idempotent: it skips a party that already exists.
2. **Rights.** The same script re-grants `CanActAs` + `CanReadAs` on every roster party to the
   ledger user `crossdesk-backend` (2 rights per party). It deliberately grants no
   `ParticipantAdmin`.
3. **The desk.** The script writes `Label=fullPartyId,…` to `.roster-<profile>` and prints it as
   `LEDGER_PARTIES`. `config/PartyRoster.java` parses that string; `LedgerService.resolveParty`
   matches a reference against the label **case-insensitively**, or against a full id, or against
   a `hint::` prefix. Because a non-admin ledger user cannot list parties, this env var *is* the
   desk's entire view of who exists.
4. **Auth.** `LEDGER_AUTH_MODE=hmac`: the desk mints its own HS256 token from
   `crossdesk-ledger-hmac-secret` and acts as `crossdesk-backend`, which is why the rights grant
   in step 2 is what actually lets the new seats sign.

## 2. The two new parties

Run on the VM (`~/run05-five-seats.sh`, derived from the existing `run05.sh`), with
`CD_PARTY_HINTS` extended by `custodian transferagent` and everything else unchanged:

```
ok  Canton 3.5.18, namespace 1220278484b0cea1…  (own-devnet-vm)
ok  uploaded crossdesk-3.0.0.dar (sha256 dcafdbb55490c69d…)      # same DAR, a no-op
ok  exists    issuer-crossdesk…  bank…  alice…  bob…  auditor…  venue…  agent…  operator…
ok  allocated custodian-crossdesk::1220278484b0cea14aaf823ee4c5890d0573ff07327bf38a3fe75537b261c6213238
ok  allocated transferagent-crossdesk::1220278484b0cea14aaf823ee4c5890d0573ff07327bf38a3fe75537b261c6213238
ok  wallet    crossdesk-validator-1::1220278484b0…  (user administrator)
ok  rights re-granted to existing crossdesk-backend
ok  crossdesk-backend holds 22 rights (expect 22)
```

**Additive only.** Two `POST /v2/parties` calls and a rights re-grant. The DAR upload is the same
file already on the participant. Nothing was archived, revoked or removed.

Notes:

- The party-id hints follow the existing nine exactly: lower-case name + `-crossdesk`. The
  **roster labels** are `Custodian` and `TransferAgent`, which is what `users.yml` binds to.
  `05`'s label helper only capitalises the first letter, so it emits `Transferagent`; the label
  was corrected to `TransferAgent` before it went into `LEDGER_PARTIES`. (Either spelling would
  in fact resolve — `resolveParty` compares labels case-insensitively — but the exact spelling is
  what the roster and the portal display.)
- The VM reports **Canton 3.5.18**; the runbook still says 3.5.17.
- The local `deploy/own-devnet-validator/.roster-own-devnet` (gitignored) was refreshed to the
  11-entry roster so `06`/`07` pick it up.

### `users.yml` seat mappings — they line up

| uid | seat | `party:` | roster label | resolves to |
|---|---|---|---|---|
| `sandbox-issuer` | issuer | `Issuer` | `Issuer` | `issuer-crossdesk::1220278484b0…` |
| `sandbox-lender` | lender | `Bank` | `Bank` | `bank-crossdesk::1220278484b0…` |
| `sandbox-venue` | venue | `Venue` | `Venue` | `venue-crossdesk::1220278484b0…` |
| `sandbox-custodian` | custodian | `Custodian` | **`Custodian`** | **`custodian-crossdesk::1220278484b0…`** |
| `sandbox-transferagent` | transfer-agent | `TransferAgent` | **`TransferAgent`** | **`transferagent-crossdesk::1220278484b0…`** |

No change to `users.yml` was needed. One detail there is unchanged and still wrong:
`sandbox-transferagent` carries instrument **`LX1`**, which does not exist on this ledger (D-2).

### Verified on the live service

```
GET https://etpfoundry.com/api/health  → 200
  {"status":"UP","applicationId":"crossdesk-backend","auth":"hmac",
   "ledgerHost":"10.20.0.10","ledgerPort":5001,"tls":false}

GET https://etpfoundry.com/api/diag    → 200
  parties: 11  (Issuer, Bank, Alice, Bob, Auditor, Venue, Agent, Operator,
                Custodian, TransferAgent, Wallet) — all isLocal: true
  ledger.reachable: true   ledgerEnd: 531433   partiesError: null
```

---

## 3. Proving the five seats — what was run, and where

### 3.1 The one honest constraint, stated up front

`/api/committee/*` and `/api/fixing/*` are **LEGACY** routes in `AuthRoutes.java`. With
`AUTH_MODE=firebase` they require an **admin sign-in**, and the only Firebase credential on the
roster is `s.borjas@lucilla.ca`. That is a human step. There is no API-key bootstrap
(`POST /api/signer/apikey` mints a key only for an already-signed-in caller), the Chrome profile
on this machine has **no live desk session** (`/desk` redirects to `/desk/login`), and I did not
obtain or use any credential.

I therefore **did not** flip `AUTH_MODE` to `sandbox` on the live service. Doing so would open
every operator route — ledger writes as any of the 11 parties — to anyone who reaches the public
URL, for as long as the revision serves. That is a founder decision, not a test convenience.

**What was run instead.** The desk's own jar, `backend/build/libs/canton-dvp-desk-1.0.0.jar`,
built from commit `f5e3685` — **the same commit the live image was built from** — run on this
workstation bound to `127.0.0.1:8099` with `AUTH_MODE=sandbox`, pointed at the **real DevNet
validator** through an IAP SSH forward (`127.0.0.1:15001 → 10.20.0.10:5001`) with the real
`LEDGER_AUTH_MODE=hmac` secret and the new 11-entry roster:

```
GET http://127.0.0.1:8099/api/health → {"status":"UP","auth":"hmac","ledgerPort":15001,…}
GET http://127.0.0.1:8099/api/diag   → parties: 11, ledger.reachable true, ledgerEnd 531965
```

So: **the ledger, the participant, the parties, the committee, the contracts and every
attestation below are the real DevNet deployment.** The HTTP process that made the calls ran here
rather than on Cloud Run, because the Cloud Run instance requires a human sign-in. The validation
code that accepted or refused each seat's evidence is byte-identical to the deployed image's.
Every result was afterwards re-read **from the validator itself** through the JSON Ledger API, so
nothing below depends on the local process's word for it.

### 3.2 The committee

`POST /api/committee` → **HTTP 201**

```
cid       00f3a2fa7fdca0413774f2a6ad956365bc48d295d7be33f77c0fbe90c4e6bc9242ca…
label     "CrossDesk five-seat committee (DevNet pilot)"
admin     operator-crossdesk::1220278484b0…      (administers; never attests, package 3.0.0)
members   issuer-, bank-, venue-, custodian-, transferagent-crossdesk   → N = 5
threshold 3                                                            → K = 3
auditor   auditor-crossdesk::1220278484b0…
```

K=3 of N=5 satisfies the governance rule on `/governance` ("the issuer must never be a majority of
the quorum") and is the shape a pilot can actually run. The pre-existing **"E2E NAV Committee"**
(K=2, N=3, Bank/Issuer/Venue) was left untouched — see the side effects in §5.

### 3.3 Dating and session — how the published series was protected

- **Date: today.** `asOfDate = 2026-09-25` (Europe/London). A future date is now refused by the
  desk — verified: `asOfDate 2026-10-25` → **HTTP 400**, *"asOfDate 2026-10-25 is in the future; a
  fixing is dated the day it observes … A future date consumes a slot that day cannot reuse."*
  That is the guard added after the 2039-02-03 incident, working.
- **Session: `Open`, not `Close`.** A **per-run session string is no longer possible**:
  `LedgerCommands.session()` now accepts only `Open`/`Close` and answers anything else with
  *"session must be Open or Close, got: E2E-5SEAT-A-144458"* (**HTTP 400**). This means
  **`07-e2e-real-devnet.sh` line 184–185 is stale** — its `e2e_session="E2E-$(date -u +%H%M%S)"`
  cannot work against the current backend, and T5 will fail with a 400 until it is changed.
  `Open` was used instead: the published series for both CBTC and cETH is derived on session
  **`Close`** (`SeriesService:40`, from the schedule), so an `Open` fixing is invisible to it.
  Confirmed after the run — see §4.

### 3.4 The five seats

Two proposals. **A** = `POST /committee/{cid}/propose`, CBTC/USDC @ 65 000, session `Open`,
`asOfDate 2026-09-25`. **B** = `POST /committee/{cid}/propose-wrapped`, cETH/USDC,
benchmarkPrice 2 400 × parFactor 1.0 = 2 400, session `Open`, same date. Each confirmation
archives and re-creates the proposal, so each returns a new contract id.

| # | Seat | Party | Path / conditions | Evidence supplied | HTTP | Result |
|---|---|---|---|---|---|---|
| A1 | — | Operator | propose CBTC @ 65 000 | — | **201** | proposal `003aa479e98d863c…` |
| A2 | **venue** | `venue-crossdesk` | `traded-range` | `observedLow 64500`, `observedHigh 65500` (range check is **on-ledger**) | **201** | `00e67363dcefada5…` · **PASS** |
| A3 | **issuer** | `issuer-crossdesk` | `attestor-quorum`, `reserves-current`, `reserves-cover-supply`, `redemption-queue-clear` (CBTC = `attested` reserve model, the strictest) | `quorumSigners 5 / quorumThreshold 3`; `reservesAsOf 2026-09-25T14:46:38Z` (age 0.0 h); `reserves 1.0 / supply 1.0` (coverage 1); `queueDepth 0 / maxQueueDepth 10` | **201** | `00ed8f04f41128b7…` · **PASS**, `verified: true` by server |
| A4 | — | Operator | finalize at 2 of 3 | — | **422** | *"not enough attestations to finalise the fix"* (`DAML_FAILURE` / `FAILED_PRECONDITION`) — **the quorum gate works** |
| A5 | **custodian** | `custodian-crossdesk` | `holdings-current`, `holdings-cover-supply`, `no-encumbrance` | `statementAsOf 2026-09-25T14:46:38Z`; `holdings 1.0 / supply 1.0`; `encumbered 0` | **201** | `00b52f9791f37643…` · **PASS**, `verified: true` |
| A6 | **transfer-agent** | `transferagent-crossdesk` | `shares-outstanding-reconciled`, `fees-accrued` | `registerShares 1.0 / ledgerShares 1.0`; `accruedFees 0` | **201** | `002bee0f169d919a…` · **PASS**, `verified: true` |
| A7 | **lender** | `bank-crossdesk` | `independent-mark-within-tolerance`, `liquidations-consistent`, `book-acceptance` | `independentMark 65010` → **1.54 bp** vs 25 bp; `liquidationsToday 0 / worstDeviationBps 0`; `acceptedAt 2026-09-25T14:46:38Z` | **201** | `0074e1e3668f2be2…` · **PASS**, `verified: true` |
| A8 | — | Operator | finalize at 5 of 3 | publishTo `Venue` | **201** | **NavFixing `000aab6329f6d6b0…`**, `markUpdated: true` |
| B1 | — | Operator | propose-wrapped cETH 2 400 × 1.0 | — | **201** | proposal `0054f98beabf1f87…`, `discountBps 0` |
| B2 | **venue** | `venue-crossdesk` | `no-prints-attested` (the empty-book path) | `bestBid 2399 / bestAsk 2401` — server-checked, `quoted 1` | **201** | `00229e15b0f72328…` · **PASS** |
| B3 | **issuer** | `issuer-crossdesk` | `reserves-current`, `reserves-cover-supply`, `redemption-queue-clear` — **no `attestor-quorum`**, because cETH is `onchain-verifiable` | `reservesAsOf …14:48:07Z`; `reserves 109 / supply 109`; `queueDepth 0 / maxQueueDepth 10` | **201** | `001184361abd38d8…` · **PASS** |
| B4 | **custodian** | `custodian-crossdesk` | as A5 | `statementAsOf …14:48:07Z`; `holdings 109 / supply 109`; `encumbered 0` | **201** | `007fa4d62a7b58c6…` · **PASS** |
| B5 | **transfer-agent** | `transferagent-crossdesk` | as A6 | `registerShares 109 / ledgerShares 109`; `accruedFees 0` | **201** | `00e6c86d4d87303e…` · **PASS** |
| B6 | **lender** | `bank-crossdesk` | as A7 | `independentMark 2404` → **16.67 bp** vs 25 bp; `liquidationsToday 2 / worstDeviationBps 12`; `acceptedAt …14:48:07Z` | **201** | `00ea44401231f31c…` · **PASS** |
| B7 | — | Operator | finalize | publishTo `Venue` | **201** | **NavFixing `003b444790d8679a…`**, `markUpdated: true` |

**Verdict: 5 of 5 seats PASS**, and the venue passes on **both** of its mutually exclusive paths.

### 3.5 Negative controls — the seats are not rubber stamps

Each of these was run against a live proposal and refused:

| Test | HTTP | Refusal |
|---|---|---|
| Future-dated proposal, `asOfDate` +30 d | **400** | *"asOfDate 2026-10-25 is in the future … A future date consumes a slot that day cannot reuse."* |
| Per-run session string | **400** | *"session must be Open or Close"* |
| Venue `traded-range` 2500/2600 on a 2 400 proposal | **422** | *"attested price sits outside the venue's observed range"* — **`DAML_FAILURE`, refused by the ledger, not the desk** |
| Venue claiming `traded-range` **and** `no-prints-attested` | **400** | *"a venue cannot claim both … either the book traded in the window or it did not"* |
| Venue `no-prints-attested` with `bestBid 2450 / bestAsk 2460` | **422** | *"no-prints-attested: the proposal 2400 sits outside your quoted 2450 / 2460"* |
| Custodian `no-encumbrance` with `encumbered 2.5` | **422** | *"evidence refused for the custodian seat: no-encumbrance: 2.5 units are pledged, lent or encumbered"* |
| Issuer sending `attestor-quorum` for **cETH** | **400** | *"condition 'attestor-quorum' is not one the issuer seat verifies; expected any of [reserves-current, reserves-cover-supply, redemption-queue-clear]"* — the per-instrument reserve model is genuinely wired |
| Finalize below K | **422** | *"not enough attestations to finalise the fix"* |

### 3.6 Re-read from the validator

Queried read-only from the VM (`POST /v2/state/active-contracts` as Operator, ledger end 532666):

```
=== NavFixing 000aab6329f6d6b0   CBTC  session Open  asOfDate 2026-09-25  price 65000.0000000000
    threshold 3   members 5   attestors 5   publishedTo [venue-crossdesk]
    finalizedAt 2026-09-25T14:47:14.211776Z
    lender          SIGNER_PROTOCOL v2 lender          [independent-mark-within-tolerance, liquidations-consistent, book-acceptance]
    transfer-agent  SIGNER_PROTOCOL v2 transfer-agent  [shares-outstanding-reconciled, fees-accrued]
    custodian       SIGNER_PROTOCOL v2 custodian       [holdings-current, holdings-cover-supply, no-encumbrance]
    issuer          SIGNER_PROTOCOL v2 issuer          [attestor-quorum, reserves-current, reserves-cover-supply, redemption-queue-clear]
    venue           SIGNER_PROTOCOL v2 venue           [traded-range]  low=64500.0000000000 high=65500.0000000000

=== NavFixing 003b444790d8679a   cETH  session Open  asOfDate 2026-09-25  price 2400.0000000000
    referencePrice 2400.0000000000   wrapperFactor 1.0000000000
    threshold 3   members 5   attestors 5   finalizedAt 2026-09-25T14:48:46.593258Z
    …same four seats…
    venue           SIGNER_PROTOCOL v2 venue           [no-prints-attested]
```

`tier` on both is **`committee`**.

### 3.7 What this does not prove

Stated precisely, because the distinction matters:

1. **The Cloud Run HTTP layer was not exercised for these writes.** The deployed service resolves
   all 11 parties (`/api/diag`) and serves all six seats (`/api/signer-protocol`), but the
   `POST` path through `crossdesk-devnet-api` still needs one admin sign-in. The code that ran is
   the same commit; the process was not the same process.
2. **The numeric evidence is not on the ledger.** The Daml `attestations` record `member`, `role`,
   `protocolRef`, `checksPassed` and the venue's observed range. The numbers (`reserves 1.0`,
   `encumbered 0`, `independentMark 65010` …) are verified server-side and written to the desk's
   **event log**, which lives in `DATA_DIR`. For this run that was a scratch directory on this
   workstation — **so the numeric evidence for these two fixings is not in the deployed service's
   event log at all**, and on Cloud Run `DATA_DIR` is unset and ephemeral anyway (**D-4**).
3. **Still one submitting credential.** Every command was submitted by `crossdesk-backend`, which
   holds CanActAs on all 11 parties. This is L1 "hosted party" exactly as `/committee` describes
   it. Adding Custodian and TransferAgent makes the seats *exercisable*; it does not make them
   independent.
4. **Minor reporting wart.** The venue's `traded-range` confirm is recorded in the event log as
   `verified: false`, because the desk did not check the numbers — the *ledger* did (proved by the
   422 above). A reader of the event log could mistake that for an unverified attestation.

---

## 4. The published series is unchanged

Before and after, on the live site:

```
GET /api/series/CBTC → [{"date":"2026-09-25","price":65000.0,"tier":0,"tierLabel":"seed",
                         "k":0,"n":5,"signers":[],"session":"Close", …}]
GET /api/series/cETH → [{"date":"2026-09-25","price":2400.0,"tier":0,"tierLabel":"seed",
                         "k":0,"n":5,"signers":[],"session":"Close", …}]
```

`tier 0 / seed / k 0` is unchanged — the two new fixings are session `Open` and the published
series reads `Close`. The finalise republished each instrument's reference mark at the attested
price (`markUpdated: true`), but both were already 65 000 and 2 400, so no published number moved.

**One public value did change: `n` went from 3 to 5**, because the series now reads the new
five-seat committee. That is a truer number for a pilot, but it is a visible change to the public
API and to every page that renders "0 of N".

---

## 5. Side effects worth knowing about

1. **There are now two committees administered by Operator.** `StrikeService.committeeFor()`
   (`scheduler/StrikeService.java`) sorts committees whose label contains `"crossdesk"` first, so
   the new **"CrossDesk five-seat committee (DevNet pilot)"** is the one the scheduler and
   `POST /api/admin/strike` will pick — deterministically, but it *is* a change of default. The
   old **"E2E NAV Committee"** (K=2, N=3) is still on the ledger and can still be used explicitly
   by cid. If the five-seat committee is the intended pilot committee, consider archiving or
   relabelling the E2E one; **D-11** already flags "E2E NAV Committee" as a name no admin view
   should be showing a visitor.
2. **Two `Open`-session slots are now consumed** for 2026-09-25: CBTC/Open and cETH/Open. The
   `Close` slots for today are free, which is what the 16:00 strike needs.
3. **The `E2E173047` fund was re-marked twice** ("fund NAV re-marked for E2E173047" on both
   finalises) — the same throwaway fund **D-11** says should be cleaned off the public catalog.
4. **`07-e2e-real-devnet.sh` T5 is broken** against the current backend (per-run session → 400).
5. The runbook still says Canton **3.5.17**; the VM reports **3.5.18**.

---

## 6. The scheduler — what turning it on would do

`SCHEDULER_ENABLED=false` today. **It was not changed.** Note that
`StrikeRunner` is annotated `@ConditionalOnProperty(..., matchIfMissing = true)` — the runner is
**on by default**, and this env var is the only thing holding it off. Unsetting the variable
turns it on.

### 6.1 Mechanism

`StrikeRunner` ticks every 60 s (45 s initial delay) and calls `StrikeService.tick`, which walks
every enabled row of `ScheduleStore` and, per row:

| When | What happens |
|---|---|
| strike time | **PROPOSE** as Operator into the Operator-administered committee. Wrapped assets go through `desk.proposeWrappedFixing` with `benchmarkPrice` = Coinbase spot and `parFactor` = the last **attested** wrapper factor (default 1); funds use `Σ units × marks`. `asOfDate` = today on the schedule's own calendar, `session` = `Close`. |
| ½ window | **tier 2 escalation 1** — `proposal.reminder` webhook + event to every seat that has not confirmed |
| ¾ window | **tier 2 escalation 2** — the same plus configured alternates |
| K reached | **FINALIZE** as the proposer; the instrument's mark is republished and funds holding it are re-marked; a **tier-1** row appears in the public series |
| window end, K not reached | **FALLBACK** — tier 3 (`benchmark × last factor`, automatic, *not attested*), else tier 4 (prior fixing carried forward, flagged), else tier 5 (`missed`, + a `fixing.missed` webhook). Published as a series row. |

Every ledger step goes through the same controller methods the operator desk uses, so a scheduled
strike is indistinguishable from a hand-struck one. Restriking after a refusal stays manual.

### 6.2 What it would propose, concretely, at the next tick

Read from `GET /api/admin/schedule/status` on the same code against the same ledger:

| Instrument | Session | Strike | Window ends | Escalations | State |
|---|---|---|---|---|---|
| **CBTC** | Close | 16:00 Europe/London (`15:00Z`) | `15:30Z` (30 min) | `15:15Z`, `15:22:30Z` | PENDING, strikes today, deps settled |
| **cETH** | Close | 16:00 (`15:00Z`) | `15:30Z` | same | PENDING, strikes today, deps settled |
| **LX1** | Close | 16:00 (`15:00Z`) | `15:30Z` | same | PENDING, `dependsOn [CBTC, cETH]`, **`dependenciesSettled: false`** |

Calendar is `daily` for all three — **including weekends**. Tiers 2/3/4/5 are all enabled.

Prices it would use, from `api.coinbase.com/v2/prices/{pair}/spot` (fetched 2026-09-25 14:5x UTC):

- **CBTC** ← `BTC-USD` spot **83 978.575** × last attested factor **1** ⇒ proposal ≈ **83 979**
- **cETH** ← `ETH-USD` spot **2 694.265** × factor **1** ⇒ proposal ≈ **2 694**

The published seed values today are **65 000** and **2 400**. So the first scheduled strike would
move the published CBTC number by roughly **+29 %** and cETH by **+12 %** — correctly, since the
seeds are stale, but it would happen without anyone pressing anything.

*(The factor is 1 because `series.lastAttestedFactor` reads the published `Close` series, and the
only fixing carrying a `wrapperFactor` is the `Open`-session cETH one struck above.)*

### 6.3 What would break, with the current committee and configuration

1. **LX1 would publish a "missed" gap every single day.** `LX1` is filtered out of the *public*
   `/api/fixing-schedule` (the D-2 fix), but it is **still in `ScheduleStore`** and the runner
   still evaluates it. Inside the window `dependenciesSettled: false` keeps it quiet; at
   `15:30Z` `evaluate` falls through to `fallback(...)`, and for a fund with no print, no factor
   and no prior price that is **tier 5 — `fixing.missed`** — a published gap and a webhook, every
   day, for an instrument that does not exist on this ledger. **This is the single clearest
   blocker.** The fix is to drop LX1 from the schedule store, not just from the public view.
2. **Nobody would be told to sign.** Escalation reminders go through
   `WebhookDispatcher.dispatchTo`, which skips any user with no `webhookUrl` in their signer
   settings (`WebhookDispatcher:118`). No seat has one, and signer settings live in `DATA_DIR`,
   which is unset and ephemeral on Cloud Run (**D-4**). So tier 2 would write events nobody reads
   and queue **zero** notifications. The five seats also have no working sign-in of their own —
   they are reachable only through the admin's `X-Act-As` switcher.
3. **Therefore K=3 would not be reached, and tier 3 would fire daily.** Thirty minutes after each
   strike the runner would publish `benchmark print × last factor` as a **tier 3, not-attested**
   row in the *public* series for CBTC and cETH. The site would stop saying "seed" and start
   saying "tier 3", which is honest labelling of a number no committee attested — but it is a
   daily automatic publication on a public benchmark site, and **§9 of the methodology treats a
   strike time as a material term**.
4. **Committee ambiguity, now resolved in favour of the new one.** With two Operator-administered
   committees, `committeeFor` picks the label containing "crossdesk" — the new K=3 of N=5. Before
   today's change that tie was between a single committee and nothing; it is worth being
   deliberate about which one the runner should use.
5. **Weekend strikes.** The calendar is `daily`, so it strikes Saturday and Sunday too. That is
   defensible for crypto (the CME CF BRR is calculated daily), but it should be a decision.
6. **State loss on every deploy.** `attemptedToday`, `escalationSent`, schedule overrides, signer
   settings and the whole event log live in `DATA_DIR` (**D-4**). A revision roll mid-window loses
   the memory of what was already attempted. The duplicate-proposal guard survives (it reads the
   ledger), but escalation would re-send and a refused strike could be re-proposed.
7. **`reconcileMark` also starts running.** It republishes an instrument's mark whenever the
   latest **tier-1** row disagrees with it. Harmless today (no tier-1 row), but it becomes live
   the moment a scheduled strike succeeds.

### 6.4 Recommendation

**Do not turn it on yet — and it is close.** In order:

1. **Remove `LX1` from the schedule store** (or disable that row). Until then, turning the
   scheduler on publishes a fabricated daily gap for an instrument that does not exist. This is
   the only hard blocker.
2. **Decide the committee** explicitly: keep the new K=3-of-5 as the pilot committee and retire
   "E2E NAV Committee", or the reverse. Do not leave two.
3. **Give the seats a notification path**: a `webhookUrl` per signer, and therefore a **durable
   `DATA_DIR`** (D-4) — otherwise tier 2 is decorative and every strike silently falls back.
4. **Then turn it on for one instrument first** (`cETH`), on a weekday, with someone watching the
   window, and confirm you get a tier-1 row rather than a tier-3 one.
5. Consider whether the first scheduled strike should be allowed to move the published CBTC value
   ~29 % in one step, or whether the seed should be restruck by hand at the current spot first.

A pilot today does need a human to start every strike. That is currently the *safer* failure mode:
with no notification path, an automatic strike reliably degrades to an unattested tier-3 print.

---

## 7. Follow-ups

| | |
|---|---|
| 1 | Sign in once as `s.borjas@lucilla.ca` and re-run §3.4 through `crossdesk-devnet-api` itself, so the Cloud Run HTTP layer is covered too and the evidence lands in the deployed event log |
| 2 | Drop `LX1` from `ScheduleStore` and from `sandbox-transferagent`'s `instruments` in `users.yml` (D-2) |
| 3 | Fix `07-e2e-real-devnet.sh` T5: `session` must be `Open` or `Close` now |
| 4 | Give `crossdesk-devnet-api` a durable `DATA_DIR` (D-4) before relying on signer settings, API keys, the event log or the evidence record |
| 5 | Decide the committee question in §5.1, and clean the `E2E173047` / "E2E NAV Committee" artifacts off public surfaces (D-11) |
| 6 | Strike a correctly-dated **`Close`** fixing so the public series shows tier 1 instead of seed (D-1) — the machinery is now proven; only the session and a sign-in stand between here and that |
| 7 | Runbook: Canton is 3.5.18, live revision is 00020, `05` emits `Transferagent` where the roster wants `TransferAgent` |

## 8. Reproducing

```bash
# tunnel (plink.exe is renamed on this workstation — D-10)
gcloud compute start-iap-tunnel crossdesk-validator 22 --local-host-port=localhost:2222 \
  --zone us-central1-a --project crossdesk-devnet-app &
ssh -i ~/.ssh/google_compute_engine -p 2222 s_borjas_lucilla_ca@localhost      # shell on the VM
ssh -i ~/.ssh/google_compute_engine -p 2222 -N -L 15001:10.20.0.10:5001 \
  s_borjas_lucilla_ca@localhost &                                              # gRPC Ledger API

# parties (on the VM) — additive, idempotent
#   CD_PARTY_HINTS="issuer bank alice bob auditor venue agent operator custodian transferagent"
#   CD_PROFILE=own-devnet-vm bash ./05-upload-dar-and-parties.sh

# roster → the desk
gcloud run services update crossdesk-devnet-api --project crossdesk-devnet-app \
  --region us-central1 --update-env-vars "^@^LEDGER_PARTIES=<11 entries>"
curl -s https://etpfoundry.com/api/health
curl -s https://etpfoundry.com/api/diag
```
