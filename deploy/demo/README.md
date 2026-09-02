# The public demo — a desk that carries its own ledger

**What this solves.** The hosted desk points at
`hackcanton-01.devnet.naas.noders.services`, which went offline when Season 2
closed. `/api/parties` still answers because it is served from config; everything
ledger-backed returns **503**. A visitor sees a product that loads and then fails.

Waiting for a participant to fix that is the wrong trade. Acquiring one is the
project's standing blocker and has been for weeks, while `.env.example` profile A
has said the whole desk runs on a local sandbox with **no node operator and no
secrets** the entire time. This image carries that sandbox with it.

**Nothing in this configuration can expire.** The JWT that degraded the hosted
desk does not exist here.

---

## What a visitor gets

A real Canton ledger executing real Daml — sealed auctions, the K-of-N committee,
in-kind creation and redemption, atomic DvP. Not a mock, not a recording.

What they do **not** get is a shared world. The sandbox is in-memory, so state
resets whenever the instance recycles. For a demo that is a feature: every
visitor starts from the same seeded book rather than from whatever the last one
left behind.

---

## Build and deploy

The image is assembled from artefacts the repo already produces, so there is no
second toolchain to drift. Produce them first:

```bash
# the package
daml build                       # -> .daml/dist/crossdesk-2.1.0.dar

# the desk
cd backend && ./gradlew bootJar  # -> build/libs/canton-dvp-desk-1.0.0.jar
```

Stage them into this directory, then deploy:

```bash
cd deploy/demo
cp ../../.daml/dist/crossdesk-2.1.0.dar ./crossdesk.dar
cp ../../backend/build/libs/canton-dvp-desk-1.0.0.jar ./app.jar

gcloud run deploy crossdesk-demo \
  --source . \
  --project crossdesk-devnet-app \
  --region us-central1 \
  --allow-unauthenticated \
  --memory 4Gi --cpu 2 \
  --timeout 900 \
  --min-instances 0 --max-instances 1 \
  --port 8080 \
  --set-env-vars AUTH_MODE=sandbox
```

### `AUTH_MODE` — the identity switch (docs/PRODUCT-PLAN.md §3)

The jar's built-in default is `AUTH_MODE=firebase`: every non-public `/api/**` call
must carry `Authorization: Bearer <Firebase ID token>` (project
`crossdesk-devnet-app`, verified with firebase-admin under the revision's own service
account — nothing to configure), and the operator desk's routes become **admin-only**.

The service is deployed with **`AUTH_MODE=sandbox` for now**, because the operator
desk at `/desk` does not send a token yet. In sandbox mode the desk's existing routes
work with no headers (as today) and the new portal routes take
`X-Sandbox-User: <email | local part | party label>` — `Issuer`, `bank`,
`alice@sandbox.crossdesk`, `s.borjas@lucilla.ca` all resolve against the roster in
`backend/src/main/resources/users.yml`.

An admin may act as any other mapped user for one request with `X-Act-As: <email>`
(logged as an `admin.act_as` event on every write; 403 for non-admins).

**Flip it when the app ships its login page:**

```bash
gcloud run services update crossdesk-demo \
  --project crossdesk-devnet-app --region us-central1 \
  --update-env-vars AUTH_MODE=firebase
```

Public routes never need a token in either mode: `/api/benchmarks`,
`/api/benchmarks/{id}`, `/api/series/{id}[.csv]`, `/api/methodology`, `/api/diag`,
`/api/health`, `/api/signer-protocol`, `/api/fixing-schedule`.

Two more knobs worth knowing: `SCHEDULER_ENABLED=false` stops the 16:00 strike runner
(it is on by default and proposes into the seeded committee as `Issuer`), and
`DEMO_SEED_COMMITTEE=false` stops the boot-time seeding of the 2-of-3
Issuer/Bank/Venue committee the signer portal signs against. Both belong OFF on a
real participant. The desk's own state (events, users, schedule) is under
`/app/data` inside the container and is as ephemeral as the sandbox — consistent, and
honest.

### What the strike runner does now (2 Sep 2026)

**Strike calendars.** Each row of `GET/PUT /api/admin/schedule` carries a `calendar`:
`daily` (every calendar day — the default for a wrapped crypto asset, because the CME CF
reference rate prints every day of the year, and for a fund whose components are all
crypto), `weekdays`, `nyse` or `lse` (weekdays minus the exchange's published 2026–2027
closures, from `backend/src/main/resources/calendars/{nyse,lse}.yml`). A fund strikes only
on days *all* its components strike. `CALENDARS_DIR=/path` replaces either file, or adds a
calendar under a new name, without a rebuild. The public `/api/fixing-schedule` shows the
calendar per identifier, and `/api/admin/schedule/status` shows `strikesToday`,
`nextStrikeDate` and the effective calendar list. CBTC and cETH therefore strike on
Saturday and Sunday on this image; that is the calendar, not a bug.

**Tier 2 escalation, on by default.** With a proposal open and K not reached, the runner
sends `proposal.reminder` webhooks (and writes events) to every seat that has not confirmed
at half the window (`escalation: 1`) and again at three quarters (`escalation: 2`), the
second time also to any `alternates: { issuer: [...], lender: [...], venue: [...] }` set on
the schedule row — e-mails from `users.yml` whose party is on the committee on-ledger
(anyone else is named in the event and skipped). Tiers 3–5 run only after the window
closes. Switch it off per instrument with `tiers.t2: false`.

**Evidence, not ticks.** `POST /api/proposals/{cid}/confirm` for the issuer and lender
seats requires `evidence: { "<condition>": { ...numbers } }` in the shape
`GET /api/signer-protocol` publishes per condition, checks it server-side (the lender's
mark against its own `tolerances.markBps`, default 25 bp) and refuses with a **422** that
carries the schema. The venue path is unchanged (`evidence: {low, high}`, enforced by the
ledger). Try it against the running demo:

```bash
BASE=https://crossdesk-demo-<hash>-uc.a.run.app
CID=$(curl -s -H 'X-Sandbox-User: lender' "$BASE/api/proposals" | python -c 'import json,sys;print(json.load(sys.stdin)[0]["cid"])')
# a bare tick → 422 with the schema
curl -s -X POST -H 'X-Sandbox-User: lender' -H 'Content-Type: application/json' \
  "$BASE/api/proposals/$CID/confirm" -d '{"checks":["book-acceptance"]}'
# the numbers → 200, verified: true
curl -s -X POST -H 'X-Sandbox-User: lender' -H 'Content-Type: application/json' \
  "$BASE/api/proposals/$CID/confirm" \
  -d '{"checks":["book-acceptance"],"evidence":{"book-acceptance":{"acceptedAt":"2026-09-02T16:00:00Z"}}}'
```

`app.jar` and `crossdesk.dar` are gitignored. They are build output, and a 42 MB
jar in git history is not worth the convenience of skipping two `cp` commands.

### `--max-instances 1` is not a cost control. It is a correctness requirement.

Each container carries **its own in-memory sandbox**, so each instance is a
separate ledger. Left at the Cloud Run default the service scales out under
traffic and two visitors land on two different worlds: one strikes a fixing, the
other cannot see it, and a third arrives to a book neither of them recognises.
Nothing errors. It simply stops being one venue, which is the single claim the
demo exists to support - and it would only ever show up with more than one
person on the site, which is precisely when it matters.

Pinning to one instance makes every visitor share one ledger. It behaves like a
venue because there is only one of it. Concurrency is 160 requests on that
instance, far more than a demo will need.

**Do not raise this ceiling to "handle load".** Load is not the constraint; a
shared world is.

---

## Before you present

**Set `--min-instances 1` about an hour before, and back to 0 afterwards.**

```bash
gcloud run services update crossdesk-demo \
  --project crossdesk-devnet-app --region us-central1 --min-instances 1
```

Cold start is roughly a minute — the sandbox has to come up, take the DAR and run
`Test:initialize` before the desk binds its port. At `--min-instances 0` that
cost lands on whoever arrives first after an idle period, and a demo that makes
you wait is a demo nobody finishes. At `1` the container stays warm and the cost
is a 4Gi instance running continuously, which is why it is not the default.

---

## Pointing the site at it

`frontend/firebase.json` rewrites `/api/**` to the Cloud Run service
`crossdesk-devnet-api`. To send the public desk at this one instead, change that
`serviceId` to `crossdesk-demo` and redeploy hosting.

Do that deliberately. It is the difference between the site demonstrating a
sandbox and the site demonstrating a participant, and the two claims are not
interchangeable in front of anyone who knows the difference — a sandbox proves
the software, a participant proves the integration. Say which one is on screen.

---

## Why one container rather than three

The sandbox is in-memory, so the ledger and the desk share a lifetime whether or
not the topology admits it. Splitting them across services would produce a desk
that outlives its own data and reports 503 in exactly the way the hosted
deployment does today — the failure this image exists to remove.

The entrypoint `exec`s the JVM so it becomes PID 1 and takes Cloud Run's SIGTERM
directly. A wrapper that swallows the signal turns every revision swap into a
hard kill of the Canton JVM, and `LOCAL-RUNBOOK.md` records that as bad enough to
wedge a host until it is rebooted.
