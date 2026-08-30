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
  --min-instances 0 \
  --port 8080
```

`app.jar` and `crossdesk.dar` are gitignored. They are build output, and a 42 MB
jar in git history is not worth the convenience of skipping two `cp` commands.

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
