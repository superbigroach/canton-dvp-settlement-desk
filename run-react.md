# Run the Canton DvP Settlement Desk — React app (full stack)

A premium **institutional trading-desk** product UI (React + TypeScript, in
`frontend/`) over the settlement desk. It hides the raw contract-id plumbing: you
pick an asset, a side, a quantity, and either **Settle now (DvP)** with a chosen
counterparty, or **Send to Auction** as an anonymous sealed order that crosses at
the venue's official price. The auction runs in two sessions — **Opening (MOO)** and
**Closing (MOC)** — and the price it prints is shown as the **Official Open** or the
**Official Close / NAV** in a gold quote card. All ledger orchestration
(splitting/merging holdings, propose→accept→settle, opening/closing the auction)
happens **server-side** against a live Canton sandbox. UI/number fonts (Inter +
JetBrains Mono) are bundled via npm (`@fontsource/*`) — no external CDN.

**Open the app at:** <http://localhost:5173>

---

## The three processes

| Layer                 | Port(s)    | How it runs                                   |
|-----------------------|------------|-----------------------------------------------|
| Canton sandbox        | 6900–6903  | WSL: `daml sandbox` (ledger API on 6900)      |
| Spring Boot backend   | 8080       | WSL: `java -jar` (Daml Java bindings → 6900)   |
| React app (Vite)      | 5173       | Windows host: `npm run preview` (proxies /api → 8080) |

> A separate user `daml start` holds 6865–6868 / 7500 / 7575 — unrelated, leave it.

The demo cash token is **`USDC`**; assets are **`DEMO:AAPL`** (ref 255),
**`cETH`** (ref 2,400) and **`CBTC`** (ref 65,000). cETH + CBTC are `CryptoWrapped`
instruments (HackCanton cETH/CBTC bounties). `Test:initialize` seeds Alice and Bob
with cETH/CBTC/USDC books so the crypto desks trade out of the box.

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

- **Acting-as switcher** (top-right) — who you are, fed by `GET /api/parties`, next
  to a pulsing `● live · ledger localhost:6900` status.
- **Official Open / Close · NAV quote card** — the selected instrument's reference
  price in a big gold ticker, labelled **Official Open** or **Official Close / NAV**
  by the chosen auction session.
- **Position** — your holdings (`GET /api/holdings?party=`), spot only (no shorting),
  with a mark-to-reference value column.
- **Trade** — pick Asset / Side (Buy·Sell) / Quantity, then a mode:
  - **Settle now · DvP** — choose a counterparty and price (pre-filled to the
    instrument reference, editable); one click runs propose→accept→settle server-side
    (`POST /api/trade`). Holdings swap atomically.
  - **Send to Auction** — pick a **Session (Opening / Closing)**; no counterparty and
    **no price**: a sealed order that crosses at the instrument's published reference
    (`POST /api/moc/order`, with `session: "Open"|"Close"`). Opening and closing
    sessions rest in **separate books**.
- **The Cross** (as **Venue**) — see the selected session's resting orders
  (`GET /api/moc/state?…&session=`) and **Run the Cross**
  (`POST /api/moc/{auctionCid}/close`) to cross them at the uniform official price.
  No price is typed — it comes from the instrument.
- **Settlement receipts** — every DvP settle and cross fill shows who/what/amount/
  price/time, an OPEN/CLOSE/DvP badge, and the on-ledger receipt (or batch)
  contract id in mono.

## Verify live (host shell)
```bash
curl http://localhost:8080/api/health
curl http://localhost:5173/api/instruments            # through the Vite proxy
curl "http://localhost:5173/api/holdings?party=Alice"  # before a trade
curl -X POST http://localhost:5173/api/trade -H 'Content-Type: application/json' \
  -d '{"buyer":"Alice","seller":"Bob","assetInstrument":"DEMO:AAPL","assetAmount":3,"cashInstrument":"USDC","cashAmount":765}'
curl "http://localhost:5173/api/holdings?party=Alice"  # after: +3 AAPL, -765 USDC

# Opening cross (MOO) on cETH — one sealed BUY + one sealed SELL, then run it.
curl -X POST :8080/api/moc/order -H 'Content-Type: application/json' \
  -d '{"trader":"Bob","side":"Sell","quantity":2,"instrumentId":"cETH","session":"Open"}'
curl -X POST :8080/api/moc/order -H 'Content-Type: application/json' \
  -d '{"trader":"Alice","side":"Buy","quantity":2,"instrumentId":"cETH","session":"Open"}'
AUC=$(curl -s ":8080/api/moc/state?instrumentId=cETH&session=Open" | jq -r .auctionCid)
curl -X POST ":8080/api/moc/$AUC/close"    # -> session "Open", closingPrice 2400 (Official Open)

# Closing cross (MOC) on CBTC — same mechanism, prints the Official Close / NAV.
curl -X POST :8080/api/moc/order -H 'Content-Type: application/json' \
  -d '{"trader":"Bob","side":"Sell","quantity":1,"instrumentId":"CBTC","session":"Close"}'
curl -X POST :8080/api/moc/order -H 'Content-Type: application/json' \
  -d '{"trader":"Alice","side":"Buy","quantity":1,"instrumentId":"CBTC","session":"Close"}'
AUC=$(curl -s ":8080/api/moc/state?instrumentId=CBTC&session=Close" | jq -r .auctionCid)
curl -X POST ":8080/api/moc/$AUC/close"    # -> session "Close", closingPrice 65000 (Official Close/NAV)

# Designated Liquidity Provider — SELECTIVE net-imbalance disclosure (Bank = DLP).
# A closing-session AAPL order auto-opens an auction with Bank as its DLP.
curl -X POST :8080/api/moc/order -H 'Content-Type: application/json' \
  -d '{"trader":"Alice","side":"Buy","quantity":2,"instrumentId":"DEMO:AAPL","session":"Close"}'
# The imbalance is disclosed BY THE LEDGER to the DLP + venue ONLY:
curl ":8080/api/moc/imbalance?instrumentId=DEMO:AAPL&session=Close&actingAs=Alice"  # -> HTTP 403 (a normal trader)
curl ":8080/api/moc/imbalance?instrumentId=DEMO:AAPL&session=Close&actingAs=Bank"   # -> netSide "Buy", netQuantity 2 @ 255
curl ":8080/api/moc/imbalance?instrumentId=DEMO:AAPL&session=Close&actingAs=Venue"  # -> the venue sees it too
# The DLP offsets on the opposite side for the net quantity, then the venue crosses cleanly:
curl -X POST :8080/api/moc/order -H 'Content-Type: application/json' \
  -d '{"trader":"Bank","side":"Sell","quantity":2,"instrumentId":"DEMO:AAPL","session":"Close"}'
AUC=$(curl -s ":8080/api/moc/state?instrumentId=DEMO:AAPL&session=Close&actingAs=Venue" | jq -r .auctionCid)
curl -X POST ":8080/api/moc/$AUC/close"    # -> Alice +2 AAPL, Bank +510 USDC, no principal risk
```

In the React app, acting **as Bank** shows an **"Imbalance · LP View"** panel (net
side + magnitude, one-click **Offset**); no other party sees that panel, and a
normal trader still cannot see the book.

## Restart
- **React only:** re-run step 5 (`npm run build` if source changed, then preview).
- **Backend only:** stop the jar, `./gradlew build -x test`, re-run step 4.
- **Whole ledger:** kill sandbox + backend, redo steps 2 → 3 → 4 (fresh in-memory state).
- **After editing `.daml`:** step 1 (build + **codegen java**) → rebuild backend jar → reseed.
