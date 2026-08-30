# CrossDesk — Devnet Runbook (HackCanton shared node)

Reproduce a **real, on-node atomic settlement** in three copy-paste steps. This
runs the `backend-devnet` build (Daml 3.x / Ledger API v2) against the NODERS
`hackcanton-01` participant. The original `backend/` still targets a local
2.9.4 sandbox unchanged — see [`../DEVNET_INTEGRATION.md`](../DEVNET_INTEGRATION.md)
for why there are two builds and everything that was ported.

---

## Prerequisites (one-time, on the node operator's side)

- The CrossDesk DAR is uploaded to the node (done — package `72ec9833…`, LF 2.2).
- The 5 parties exist: `issuer-crossdesk`, `bank-crossdesk`, `alice-crossdesk`,
  `bob-crossdesk`, `auditor-crossdesk` (all `::122003aa7c49…`).
- **Your user (`sborjas`, sub `8b9dc176-…`) has `CanActAs` on those 5 parties.**

🔴 **THAT LIST IS SHORT BY ONE, AND IT BREAKS THE DESK.**

`resolveParty("Venue")` is called at **26 sites** — every auction and every settlement path —
and it THROWS `no known party matches 'Venue'` when the party is absent. `Test:initialize`
allocates **eight** parties on a sandbox (Issuer, **Venue**, Alice, Bob, Bank, Auditor, Agent,
Eve), which is why this never shows up locally.

`scripts/bootstrap-devnet.sh:93` calls `give Venue USDC 500000` to fund the venue's insurance
pool, and that POSTs `owner=Venue` to this backend — so it has been failing on devnet, silently,
since the 3.x port. Git history confirms `Venue=` was **never** in `run-devnet.sh`.

**Ask the node operator for BOTH:**
1. Allocate **`venue-crossdesk`** on the participant (if it does not already exist), and
2. Grant your user **`CanActAs venue-crossdesk`** — the roster entry alone is not enough.

`run-devnet.sh` and `.env.example` now list Venue. Run `python preflight.py` before the first
submission; it fails on a roster missing Venue, Issuer, Bank or Auditor.
  Reads work with readAs alone; **writes (create/settle) need actAs** — this is
  the only grant the node operator must set. Until then, step 3 returns
  `PERMISSION_DENIED` (everything else still works).

## Step 1 — get your JWT (the keycard), in PowerShell

```powershell
$body = "grant_type=password&client_id=web-app-ui-hackcanton-01-devnet&scope=openid daml_ledger_api offline_access&username=YOUR_EMAIL&password=YOUR_PASSWORD"
$r = Invoke-RestMethod -Method Post -Uri "https://keycloak.naas.noders.services/realms/noders-appsfactory/protocol/openid-connect/token" -ContentType "application/x-www-form-urlencoded" -Body $body
$env:LEDGER_JWT = $r.access_token
$env:LEDGER_JWT   # prints a long eyJ... string
```

## Step 2 — start the backend against the node (WSL)

```bash
cd /mnt/c/Users/sborj/Desktop/hackcanton-ceth-settlement/backend-devnet
export LEDGER_JWT="eyJ...paste from step 1..."
./run-devnet.sh          # connects over TLS+JWT, loads the 5-party roster, serves :8090
```

`run-devnet.sh` sets the devnet host/port/TLS and the party roster for you. It
listens on **8090** (so it can run alongside the local build on 8080).

## Step 3 — fire the settlement (WSL, second terminal)

```bash
cd /mnt/c/Users/sborj/Desktop/hackcanton-ceth-settlement/backend-devnet
./smoke-devnet.sh        # publishes instruments, issues holdings, runs one atomic trade
```

A `SettlementReceipt` in the output = a **real contract settled on the node**.

---

## What each piece is

| Thing | Role |
|---|---|
| **JWT** (step 1) | your identity keycard, minted by the node's login (Keycloak) |
| **TLS** | the encrypted tunnel to the node; the JWT rides inside it |
| **`run-devnet.sh`** | starts CrossDesk and holds the Ledger API connection |
| **`smoke-devnet.sh`** | drives the desk's REST API → real Ledger API commands |
| **readAs / actAs** | see vs. do — reads need readAs, every write needs actAs |

## Troubleshooting

| Symptom | Meaning | Fix |
|---|---|---|
| `PERMISSION_DENIED` on a POST | your user lacks **actAs** for that party | node operator grants `CanActAs` (see prereqs) |
| `Port 8080 was already in use` | the local build owns 8080 | devnet build uses 8090 by default — nothing to do |
| `invalid_grant` in step 1 | wrong login, or an SSO-only account | use the appsfactory email+password; if SSO-only, copy the token from the browser (F12 → Network) |
| `UNIMPLEMENTED` | wrong/older bindings | ensure you're on `backend-devnet` (v2 bindings), not `backend` |
