# Run the desk locally against a Daml sandbox

The backend is one jar that talks to a Canton Ledger API over gRPC. Locally the
simplest ledger is the Daml **sandbox** (Ledger API on `localhost:6865`,
plaintext, no auth). This is a **two-terminal** flow: the sandbox in one, the
backend in the other.

Everything here is offline and needs no credentials or coins.

---

## Terminal 1 — start the sandbox + upload the DAR + allocate parties

From the repo root:

```bash
daml build                       # produces .daml/dist/canton-dvp-settlement-desk-1.0.0.dar
daml sandbox --port 6865         # Ledger API on localhost:6865  (leave running)
```

In a scratch terminal, upload the DAR and run the init script (allocates
Issuer / Venue / Alice / Bob / Bank / Auditor / Agent / Eve, publishes the three
instruments, and seeds a live DvP proposal):

```bash
daml ledger upload-dar .daml/dist/canton-dvp-settlement-desk-1.0.0.dar \
  --host localhost --port 6865

daml script \
  --dar .daml/dist/canton-dvp-settlement-desk-1.0.0.dar \
  --script-name Test:initialize \
  --ledger-host localhost --ledger-port 6865
```

> Tip: `daml start` does the build + sandbox + `Test:initialize` + Navigator in
> one command if you prefer the UI at <http://localhost:7500>.

**Note the allocated party ids.** On a Canton sandbox a party id carries a
namespace suffix (e.g. `Alice::1220ab…`). List them with:

```bash
daml ledger list-parties --host localhost --port 6865
```

---

## Terminal 2 — run the backend

### Option A — the jar directly (fastest)

```bash
cd backend
./gradlew bootRun
# or: ./gradlew bootJar && java -jar build/libs/canton-dvp-desk-1.0.0.jar
```

Defaults already point at `localhost:6865`, plaintext, no auth. Override via env:

```bash
LEDGER_HOST=localhost LEDGER_PORT=6865 LEDGER_TLS=false ./gradlew bootRun
```

### Option B — the container (from repo root)

```bash
docker compose up --build          # points at host.docker.internal:6865
```

---

## Smoke-test the REST API

```bash
# Liveness + which ledger the jar is pointed at (does NOT touch the ledger):
curl -s localhost:8080/api/health | jq

# Issue an instrument + a holding, then query it. Use the ACTUAL allocated party
# ids from `daml ledger list-parties` (shown here as plain hints for brevity):
ISSUER='Issuer'; ALICE='Alice'

curl -s -XPOST localhost:8080/api/instruments -H 'content-type: application/json' -d "{
  \"issuer\":\"$ISSUER\",\"id\":\"USD\",\"kind\":\"Cash\",\"description\":\"Tokenised USD\"
}" | jq

curl -s -XPOST localhost:8080/api/holdings -H 'content-type: application/json' -d "{
  \"issuer\":\"$ISSUER\",\"instrumentId\":\"USD\",\"owner\":\"$ALICE\",\"amount\":2550.0
}" | jq

curl -s "localhost:8080/api/holdings?party=$ALICE" | jq
```

A full **bilateral DvP** (Bob sells 10 AAPL to Alice for 2,550 USD):

```bash
ISSUER='Issuer'; ALICE='Alice'; BOB='Bob'; AUD='Auditor'

# Bob holds AAPL, Alice holds USD:
BOB_AAPL=$(curl -s -XPOST localhost:8080/api/holdings -H 'content-type: application/json' \
  -d "{\"issuer\":\"$ISSUER\",\"instrumentId\":\"DEMO:AAPL\",\"owner\":\"$BOB\",\"amount\":10.0}" | jq -r .contractId)
ALICE_USD=$(curl -s -XPOST localhost:8080/api/holdings -H 'content-type: application/json' \
  -d "{\"issuer\":\"$ISSUER\",\"instrumentId\":\"USD\",\"owner\":\"$ALICE\",\"amount\":2550.0}" | jq -r .contractId)

# Bob proposes; Alice accepts; Bob settles:
PROP=$(curl -s -XPOST localhost:8080/api/dvp/propose -H 'content-type: application/json' -d "{
  \"proposer\":\"$BOB\",\"counterparty\":\"$ALICE\",\"auditor\":\"$AUD\",
  \"assetHoldingCid\":\"$BOB_AAPL\",\"cashHoldingCid\":\"$ALICE_USD\",
  \"assetInstrument\":\"DEMO:AAPL\",\"assetAmount\":10.0,\"cashInstrument\":\"USD\",\"cashAmount\":2550.0
}" | jq -r .contractId)

AGREE=$(curl -s -XPOST "localhost:8080/api/dvp/$PROP/accept" -H 'content-type: application/json' \
  -d "{\"counterparty\":\"$ALICE\"}" | jq -r .contractId)

curl -s -XPOST "localhost:8080/api/dvp/$AGREE/settle" -H 'content-type: application/json' \
  -d "{\"proposer\":\"$BOB\"}" | jq

# Alice now holds the 10 AAPL:
curl -s "localhost:8080/api/holdings?party=$ALICE" | jq
```

---

## Run the integration test against this ledger

With the sandbox up and the DAR uploaded, the `@Tag("integration")` end-to-end
test runs a full issue -> propose -> accept -> settle -> query flow through the
bindings. Pass the real party ids:

```bash
cd backend
LEDGER_IT=1 \
  IT_ISSUER='Issuer' IT_ALICE='Alice' IT_BOB='Bob' IT_AUDITOR='Auditor' \
  ./gradlew integrationTest
```

It is excluded from the normal `./gradlew build`, which never needs a ledger.
