# Run the Canton DvP Settlement Desk — React app (full stack)

A simple **Buy / Sell** product UI (React + TypeScript, in `frontend/`) over the
settlement desk. It hides the raw contract-id plumbing: you pick an asset, a side,
a quantity, and either **Settle now (DvP)** with a chosen counterparty, or **Send
to Close (MOC)** as an anonymous sealed order that crosses at the official close.
All ledger orchestration (splitting/merging holdings, propose→accept→settle,
opening/closing the auction) happens **server-side** against a live Canton sandbox.

**Open the app at:** <http://localhost:5173>

---

## The three processes

| Layer                 | Port(s)    | How it runs                                   |
|-----------------------|------------|-----------------------------------------------|
| Canton sandbox        | 6900–6903  | WSL: `daml sandbox` (ledger API on 6900)      |
| Spring Boot backend   | 8080       | WSL: `java -jar` (Daml Java bindings → 6900)   |
| React app (Vite)      | 5173       | Windows host: `npm run preview` (proxies /api → 8080) |

> A separate user `daml start` holds 6865–6868 / 7500 / 7575 — unrelated, leave it.

The demo cash token is **`USDC`**; assets are **`DEMO:AAPL`** (close 255) and
**`cETH`** (close 2500).

---

## Start it from scratch

All WSL commands: prefix `export HOME=/root`; full Daml path `/root/.daml/bin/daml`;
Java 17 at `/usr/lib/jvm/java-17-openjdk-amd64/bin/java`. Repo root in WSL:
`/mnt/c/Users/sborj/Desktop/hackcanton-ceth-settlement`.

### 1. Build the DAR (only if Daml source changed)
```bash
export HOME=/root && cd /mnt/c/Users/sborj/Desktop/hackcanton-ceth-settlement
/root/.daml/bin/daml build          # -> .daml/dist/canton-dvp-settlement-desk-1.0.0.dar
/root/.daml/bin/daml codegen java   # REGENERATE the Java bindings (package-id changes with the source!)
```
> **Important:** editing any `.daml` changes the package hash. You MUST re-run
> `daml codegen java` and rebuild the backend jar, or the backend's `ContractFilter`
> queries fail with `TEMPLATES_OR_INTERFACES_NOT_FOUND`.

### 2. Start a fresh sandbox on 6900 (leave running)
```bash
export HOME=/root && cd /mnt/c/Users/sborj/Desktop/hackcanton-ceth-settlement
rm -f /tmp/ledger6900.portfile
nohup /root/.daml/bin/daml sandbox --port 6900 --admin-api-port 6901 \
  --domain-public-port 6902 --domain-admin-port 6903 \
  --port-file /tmp/ledger6900.portfile > /tmp/sandbox6900.log 2>&1 &
# Ready when /tmp/sandbox6900.log prints "Canton sandbox is ready."
```

### 3. Upload the DAR + seed (once per fresh sandbox)
```bash
export HOME=/root && cd /mnt/c/Users/sborj/Desktop/hackcanton-ceth-settlement
DAR=.daml/dist/canton-dvp-settlement-desk-1.0.0.dar
/root/.daml/bin/daml ledger upload-dar --host localhost --port 6900 "$DAR"
/root/.daml/bin/daml script --ledger-host localhost --ledger-port 6900 \
  --dar "$DAR" --script-name Test:initialize
```
Seeds parties (Issuer, Venue, Alice, Bob, Bank, Auditor, Agent, Eve), the three
instruments, **Bob = 10 DEMO:AAPL**, **Alice = 2,550 USDC**, and a seed DvP proposal.
`Test:initialize` runs only ONCE per sandbox (parties already exist on a re-run) —
to reseed, kill the sandbox (state is in-memory) and redo steps 2–3.

### 4. Build + start the backend (leave running)
```bash
export HOME=/root && cd /mnt/c/Users/sborj/Desktop/hackcanton-ceth-settlement/backend
./gradlew build -x test
LEDGER_HOST=localhost LEDGER_PORT=6900 LEDGER_TLS=false SERVER_PORT=8080 \
  /usr/lib/jvm/java-17-openjdk-amd64/bin/java -jar build/libs/canton-dvp-desk-1.0.0.jar
# Ready when it logs "Started SettlementDeskApplication".
```

### 5. Build + serve the React app (leave running) — on the Windows host
```powershell
cd C:\Users\sborj\Desktop\hackcanton-ceth-settlement\frontend
npm install        # first time only
npm run build      # tsc --noEmit && vite build  -> dist/
npm run preview -- --port 5173 --host   # serves dist/, proxies /api -> :8080
```
(For live-reload development use `npm run dev` instead of build+preview.)

### 6. Open <http://localhost:5173>

---

## What the app does (simple product)

- **Acting-as switcher** (top-right) — who you are, fed by `GET /api/parties`.
- **Position** — your holdings (`GET /api/holdings?party=`), spot only (no shorting).
- **Trade** — pick Asset / Side (Buy·Sell) / Quantity, then a mode:
  - **Settle now (DvP)** — choose a counterparty and price; one click runs
    propose→accept→settle server-side (`POST /api/trade`). Holdings swap atomically.
  - **Send to Close (MOC)** — no counterparty, **no price**: a sealed order that
    crosses at the instrument's published close (`POST /api/moc/order`).
- **The Close** (as **Venue**) — see resting orders (`GET /api/moc/state`) and
  **Run the Close** (`POST /api/moc/{auctionCid}/close`) to cross them at the uniform
  close price. No price is typed — it comes from the instrument.
- **Settlement receipts** — every DvP settle and MOC fill shows who/what/amount/
  price/time and the on-ledger receipt (or batch) contract id.

## Verify live (host shell)
```bash
curl http://localhost:8080/api/health
curl http://localhost:5173/api/instruments            # through the Vite proxy
curl "http://localhost:5173/api/holdings?party=Alice"  # before a trade
curl -X POST http://localhost:5173/api/trade -H 'Content-Type: application/json' \
  -d '{"buyer":"Alice","seller":"Bob","assetInstrument":"DEMO:AAPL","assetAmount":3,"cashInstrument":"USDC","cashAmount":765}'
curl "http://localhost:5173/api/holdings?party=Alice"  # after: +3 AAPL, -765 USDC
```

## Restart
- **React only:** re-run step 5 (`npm run build` if source changed, then preview).
- **Backend only:** stop the jar, `./gradlew build -x test`, re-run step 4.
- **Whole ledger:** kill sandbox + backend, redo steps 2 → 3 → 4 (fresh in-memory state).
- **After editing `.daml`:** step 1 (build + **codegen java**) → rebuild backend jar → reseed.
