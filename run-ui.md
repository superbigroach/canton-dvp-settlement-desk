# Run the Canton DvP Settlement Desk UI (local, end-to-end)

A browser UI for the settlement desk, served by the Spring Boot backend, driving a
**live local Canton sandbox** over the Ledger API (gRPC) with the Daml Java bindings.
This replaces the deprecated Daml Navigator.

**Open the UI at:** <http://localhost:8080>

---

## Ports

`daml sandbox` needs **four** ports (ledger API, admin API, domain public, domain admin).
A default `daml start` already occupies **6865, 6866, 6867, 6868** (plus 7500 Navigator,
7575 JSON-API), so this desk's sandbox uses a **clear 6900–6903 block** and the backend
talks to the ledger API on **6900**:

| Purpose                         | Port |
|---------------------------------|------|
| Sandbox — Ledger API (gRPC)     | 6900 |
| Sandbox — Admin API             | 6901 |
| Sandbox — Domain public         | 6902 |
| Sandbox — Domain admin          | 6903 |
| Backend HTTP (UI + REST)        | 8080 |

> If you are NOT running a separate `daml start`, 6865–6868 are free and you may use them
> instead — just pass `LEDGER_PORT=<ledger-api-port>` to the backend to match.

All commands run in **WSL Ubuntu** with the Daml 2.9.4 SDK. Prefix each shell with
`export HOME=/root` so the `daml` assistant finds its SDK. Full Daml path:
`/root/.daml/bin/daml`. Java 17: `/usr/lib/jvm/java-17-openjdk-amd64/bin/java`.

---

## Start it (from scratch)

Run from the repo root: `/mnt/c/Users/sborj/Desktop/hackcanton-ceth-settlement`.

### 1. Build the DAR
```bash
export HOME=/root
cd /mnt/c/Users/sborj/Desktop/hackcanton-ceth-settlement
/root/.daml/bin/daml build
# -> .daml/dist/canton-dvp-settlement-desk-1.0.0.dar
```

### 2. Start the sandbox (leave this running)
```bash
export HOME=/root
cd /mnt/c/Users/sborj/Desktop/hackcanton-ceth-settlement
/root/.daml/bin/daml sandbox \
  --port 6900 --admin-api-port 6901 \
  --domain-public-port 6902 --domain-admin-port 6903 \
  --port-file /tmp/ledger6900.portfile
# Ready when the log prints "Canton sandbox is ready." and the port-file appears.
```

### 3. Upload the DAR + seed parties/contracts (one time, after the sandbox is up)
```bash
export HOME=/root
cd /mnt/c/Users/sborj/Desktop/hackcanton-ceth-settlement
DAR=.daml/dist/canton-dvp-settlement-desk-1.0.0.dar
/root/.daml/bin/daml ledger upload-dar --host localhost --port 6900 "$DAR"
/root/.daml/bin/daml script --ledger-host localhost --ledger-port 6900 \
  --dar "$DAR" --script-name Test:initialize
# Verify the parties exist:
/root/.daml/bin/daml ledger list-parties --host localhost --port 6900
```
This allocates **Issuer, Venue, Alice, Bob, Bank, Auditor, Agent, Eve**, publishes the
instruments (`DEMO:AAPL`, `USD`, `cETH`), mints seed holdings, and posts a seed DvP proposal.

> `Test:initialize` can be run only ONCE per sandbox — re-running fails because the parties
> already exist. To reseed, stop the sandbox and start a fresh one (state is in-memory).

### 4. Build + start the backend (leave this running)
```bash
export HOME=/root
cd /mnt/c/Users/sborj/Desktop/hackcanton-ceth-settlement/backend
./gradlew build -x test          # produces build/libs/canton-dvp-desk-1.0.0.jar
LEDGER_HOST=localhost LEDGER_PORT=6900 LEDGER_TLS=false SERVER_PORT=8080 \
  /usr/lib/jvm/java-17-openjdk-amd64/bin/java -jar build/libs/canton-dvp-desk-1.0.0.jar
# Ready when the log prints "Started SettlementDeskApplication".
```

### 5. Open the UI
<http://localhost:8080>

---

## Verify it's live (host shell)
```bash
curl http://localhost:8080/api/health                  # {"status":"UP", "ledgerPort":6900, ...}
curl http://localhost:8080/api/parties                 # live parties w/ per-run Canton suffixes
curl "http://localhost:8080/api/holdings?party=Alice"  # Alice's holdings (label or full id both work)
curl http://localhost:8080/                            # the HTML page
```

## What the UI does
- **Party picker** populated live from `/api/parties` (ids resolved dynamically — never hardcoded).
- **Holdings table** for the acting party (`/api/holdings`); click a row to reuse its contract id.
- **Propose DvP** (seller/asset + buyer/cash) → **Accept** → **Settle** (both legs move atomically).
- **Market-on-Close** panel: open auction, submit sealed orders, close & cross into a SettlementBatch.
- **Activity Log** shows every REST call and its raw ledger response.

## Which processes must stay up
- The **sandbox** (step 2) — the ledger. If it stops, ledger state is lost (in-memory); re-run steps 2–3.
- The **backend** (step 4) — serves the UI and the REST API.
- The user's separate `daml start` is unrelated and can keep running alongside this.

## Restart the whole thing
Stop the backend (step 4) and sandbox (step 2), then re-run steps 2 → 3 → 4.
The DAR (step 1) only needs rebuilding if the Daml source changed.
```

---

## Backend config knobs (env vars)
| Var            | Default     | Meaning                                   |
|----------------|-------------|-------------------------------------------|
| `LEDGER_HOST`  | `localhost` | Ledger API host                           |
| `LEDGER_PORT`  | `6865`      | Ledger API gRPC port (set **6900** here)  |
| `LEDGER_TLS`   | `false`     | plaintext (local) vs TLS (real Canton)    |
| `LEDGER_JWT`   | *(empty)*   | bearer token for a real participant       |
| `SERVER_PORT`  | `8080`      | backend HTTP port                         |
