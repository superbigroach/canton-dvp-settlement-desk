# CrossDesk on its own Canton validator

Two tracks, one set of scripts. **A single variable picks the ledger**:

```bash
CD_PROFILE=localnet    # Track A — Splice LocalNet on this laptop. Works TODAY. No allowlist.
CD_PROFILE=own-devnet  # Track B — our Splice validator on GCP, on DevNet. After SV allowlisting.
```

`profiles/<name>.env` holds the only values that differ: addresses, token audience, the
secret *reference*, wallet user, and registry URLs. Scripts 05, 06 and 07 run unchanged on
both.

| | |
|---|---|
| Network version | DevNet runs **Splice 0.8.1**, migration id **1** (`https://docs.dev.global.canton.network.sync.global/info`, 2026-09-15). Canton **3.5.17**, Daml SDK 3.5.2 for Splice's own DARs. |
| CrossDesk DAR | `crossdesk-2.1.0.dar`, SDK **3.4.11 → Daml-LF 2.2**. Rebuilt 2026-09-15: byte-identical to the committed build (sha256 `0a4aa4cc…805f`). A Canton 3.5.17 participant **accepted** it (LocalNet). |
| Reserved egress IP | `crossdesk-devnet-validator-ip` = **34.71.67.227** (us-central1). From any other IP, DevNet Scan answers **HTTP 403**. Checked from the laptop, 2026-09-15. |
| Status | **Track A is up and green: 12/12 real-transaction tests pass** (below). **Track B is prepared, not created.** No VM exists, and nothing was deployed. The live site is untouched. |

---

## Track A — Splice LocalNet (today)

LocalNet is the full Canton Network stack: a super validator (sequencer, mediator, Scan,
the DSO and Canton Coin), plus an **app-provider validator and participant**, which stands
in for our node. Same Canton, same Splice apps, same token standard. The HMAC auth shape is
the same one Track B uses.

```bash
cd canton-dvp-settlement-desk/deploy/own-devnet-validator
LOCALNET_HOME=/e/crossdesk-localnet ./localnet/up.sh        # Docker Desktop must be running
CD_PROFILE=localnet ./05-upload-dar-and-parties.sh          # DAR, 7 *-crossdesk parties, user + rights
(cd ../../backend && ./gradlew bootJar)                     # this commit's backend/
CD_PROFILE=localnet ./06-wire-backend.sh                    # runs the jar on :8080, hmac mode
../../scripts/bootstrap-devnet.sh http://localhost:8080/api # base layer (instruments, balances)
CD_PROFILE=localnet ./07-e2e-real-devnet.sh                 # the real-transaction suite
./localnet/down.sh                                          # stop (WIPE=1 to reset the network)
```

On Windows Git Bash, put a `python3` on PATH before running `bootstrap-devnet.sh`: it calls
`python3`, and only `python` exists there. `lib/common.sh` already detects either one.

### Result, 2026-09-15 (run `e2e1789502865`, LocalNet 0.8.1, backend/ from this commit)

| # | Test | Result | Ledger update id |
|---|---|---|---|
| T1 | party read: 8 roster parties with full ids; `crossdesk-backend` holds 16 rights; `/api/diag` reachable | PASS | — (read) |
| T2 | Canton Coin: wallet tap; unlocked 240,520 → 260,520 | PASS | `12200756a484b811a7239d4e86d62fb8c12bf2185fad6392108d2837f85c58ccf075` |
| T3 | CIP-56 claim: Amulet TransferInstruction → Alice, accepted through the desk with Scan's choice context. Alice's `HoldingV1` by interface = `DSO:Amulet 75` (25 from each of 3 runs). **Stand-in for BitSafe CBTC**, see below | PASS | accept cid `00842d3f…` |
| T4 | sealed auction: the auditor sees 0 orders (2 sealed); Alice sees only her own; the venue sees 2; the close prints @ 2,400 with 2 fills | PASS | `1220398c64b5cfa5cd39f240f9fc85e05d2308659873fc5d04c1dab9b5321f524669` |
| T5 | committee: finalize at 1-of-2 is refused (HTTP 409); after Issuer confirms, the NavFixing prints @ 65,000 | PASS | `1220cda9113c0a2a70e40b8a45b2b342fbbaf6e71f30bd13fac3a5340dcd7771d57a` |
| T6 | in-kind create: 5 shares; Alice cETH −0.5, CBTC −0.05 | PASS | `1220d11e864fb42d5dbfc5a990d3d488c922de5f7fc105f3f0416131eba52883dbba` |
| T7 | in-kind redeem: 2 shares; cETH +0.2, CBTC +0.02 | PASS | `12204cab70d0fcfe885d94905f2a048efcaa743999687070e4714a33e30fd73451da` |
| T8a | atomic DvP: Bob buys 1 cETH for 2,400 USDC; both legs move | PASS | `12206807adb860bdf3b334a7feca2401b462b8246c835cb9c67a57e25ccb5626f71f` |
| T8b | unfundable trade (1,000,000 cETH): refused by the desk's pre-check (422), balances unchanged | PASS | — (never submitted) |
| T8c | **ledger-level** must-fail: the proposal's cash amount disagrees with its holding. Propose and accept commit; `Settle` aborts on-ledger (`DAML_FAILURE`); no leg moves | PASS | propose `12204a63…5dc3`, accept `1220b7c8…2a0b` |
| T9 | conservation: USDC 2,702,550, cETH 109, CBTC 23, all unchanged | PASS | — |
| T10 | UI smoke: `/` 200, `/api/benchmarks` 200 | PASS | — |

Also green on this commit: backend unit tests **204/204** (`./gradlew test`), and
`daml test` **125/125 scripts ok** (SDK 3.4.11).

**Token renewal was verified live.** The desk minted a token at 16:03:59 and re-minted on
schedule at 16:23:59. A read and a write after the renewal both returned 200, the client
connected exactly once (no reconnect), and zero `UNAUTHENTICATED` responses appeared.

### What differs between LocalNet and DevNet, and what LocalNet cannot prove

| | LocalNet (Track A) | Own DevNet node (Track B) |
|---|---|---|
| Synchronizer | 1 local SV, its own DSO | ~14 SVs (BFT, 2/3 quorum), the real Global Synchronizer |
| CBTC | **Not available.** T3 uses **Amulet** through the same `TransferInstruction_Accept` + registry choice-context path. This is a **stand-in**, and the "real BitSafe CBTC" claim is **not** made from LocalNet | real BitSafe CBTC from `cbtc-faucet.bitsafe.finance`, accepted with context from `api.utilities.digitalasset-dev.com` |
| Canton Coin | wallet `tap`, unlimited | wallet `tap` on DevNet (the faucet); traffic is bought automatically on DevNet |
| Auth secret | LocalNet's published dev value `unsafe` | 64 hex chars in Secret Manager |
| Parties' namespace | `1220 93c2…` (the LocalNet app-provider) | our participant's own namespace, new after every DevNet reset |
| Latency | ~0.4–1.5 s per command | expect several seconds (a shared participant measured ~7.5 s) |
| Not proven by A | cross-participant privacy, the allowlist, traffic limits, SV upgrades, resets | — |

### Findings from running it for real

- **The nginx body limit.** The Splice nginx answers a 3 MB DAR upload with **413**, so
  scripts call the JSON Ledger API port directly (LocalNet `3975`; the VM's
  `127.0.0.1:7575`).
- **Java cannot reach `scan.localhost`.** The desk's HTTP client can neither resolve
  `*.localhost` nor set `Host`, so Scan is published through a socat sidecar on
  `127.0.0.1:5012`. The DevNet equivalent is the `crossdesk-scan-proxy` in the node overlay.
- **The `/api/dvp/*` endpoints take FULL party ids** (by design; see `Dtos.java`). A label
  gets `PERMISSION_DENIED`, and the participant log says why: *"Claims do not authorize to
  act as party 'Bob'"*. On your own node you can read that log. On Noders you could not.
- **Daml Script against a real participant.** A participant-admin token can allocate
  parties, but cannot act as them (`Claims do not authorize to act as party
  'Issuer::…'`). The 125 scripts run with `daml test`. They do not run unmodified against a
  participant, because they do not grant user rights after `allocatePartyByHint`. The REST
  suite above covers the same scenarios on the live ledger.

---

## Track B — own DevNet validator on GCP (prepared, not created)

Every GCP-mutating script **defaults to a dry run** and prints its commands. `EXECUTE=1`
applies. Every command carries `--project crossdesk-devnet-app`.

### Phase 0 — allowlist (the 2–7 day clock)

1. Send the request in [`ALLOWLIST_REQUEST.md`](ALLOWLIST_REQUEST.md) with egress IP
   **34.71.67.227**. There is one IP per network, and it must be distinct from any
   TestNet/MainNet IP.
2. Wait. **Do not** request an onboarding secret yet: the DevNet self-serve secret lives
   **1 hour**.
3. Verify **from the VM** (a laptop proves nothing). Once Phase 1 exists:
   ```bash
   gcloud compute ssh crossdesk-validator --zone us-central1-a --project crossdesk-devnet-app \
     --tunnel-through-iap -- 'bash ~/own-devnet-validator/03-verify-allowlist.sh'
   ```
   The script runs TRACE 6 (Splice 0.8.1 `validator_onboarding.html`): checkip = 34.71.67.227;
   every SV Scan answers `/api/scan/version`; every sequencer answers
   `grpc.health.v1.Health/Check` = `SERVING`. It needs **≥ 2/3 of the SVs for both**. A Scan
   that is fine while its sequencers time out means *partial* allowlisting; the network is
   not down. **Option:** create the VM (Phase 1) while you wait. It costs about $3.80/day,
   and it is the only place this check means anything.

### Phase 1 — the VM

```bash
./01-create-vm.sh              # dry run
EXECUTE=1 ./01-create-vm.sh    # creates: VPC, 2 subnets, firewall, secret, SA, VM
```

**Size: `e2-standard-4` (4 vCPU, 16 GB), 100 GB pd-balanced, Debian 12.**
Source: Splice 0.8.1 *Validator Hardware Requirements*
(`docs.dev.sync.global/validator_operator/validator_hardware_requirements.html`, fetched
2026-09-15). The figures cover the validator and participant containers:

| Usage | CPU | Memory | DB CPU | DB memory | DB disk |
|---|---|---|---|---|---|
| Experiments / minimal VM | 1 | 6 GB | 1 | 1 GB | 1 GB |
| Production, little activity | 2 | 8 GB | 2 | 4 GB | 10 GB |
| App provider, moderate activity | 2 | 16 GB | 2 | 4 GB | 100 GB |

Compose runs Postgres in the same VM, so the "little activity" row needs 4 CPU and 12 GB.
e2-standard-4 meets that, with ~4 GB for the OS, nginx and UIs. The disk follows the
app-provider row (100 GB), because disk is the variable that surprises people. The observed
LocalNet footprint was canton 2.3 GB + splice 1.5 GB + postgres 1.0 GB. **If memory
pressure appears**, stop the VM and run `set-machine-type e2-highmem-4` (32 GB). That
matches the app-provider row.

Networking (`01-create-vm.sh`):
- A **custom VPC `crossdesk-devnet-vpc`**, not `default`. The project's default network has
  `default-allow-ssh` and `default-allow-rdp` open to 0.0.0.0/0.
- The VM's **external IP is the reserved static 34.71.67.227**, so the SVs see that egress.
- **Ingress:** tcp:22 from the IAP range `35.235.240.0/20` only, and tcp:5001 (gRPC Ledger
  API) plus tcp:5012 (Scan proxy) from `crossdesk-run-egress` 10.20.1.0/26 only. Everything
  else is denied (the implied deny of a custom VPC). **Nothing is exposed publicly.**
  Validators need no ingress (Splice docs, *Networking*).
- **Egress:** default allow (outbound 443 to the SVs).

**Cost (us-central1 list prices, NOT fetched live).** The Cloud Billing Catalog API is
disabled on the project, so these figures are unverified. Check them in the console
calculator before relying on them.

| Item | $/month |
|---|---|
| e2-standard-4, 730 h (≈ $0.134/h) | ≈ 98 |
| 100 GB pd-balanced (≈ $0.10/GB) | ≈ 10 |
| static IPv4 attached to a running VM (≈ $0.005/h) | ≈ 3.65 |
| egress to SVs (small, ≈ $0.12/GB) | ≈ 1–5 |
| **running 24/7** | **≈ $115** |
| VM stopped: disk + static IP no longer attached to a running VM (≈ $0.01/h) | **≈ $17** |

Today, **the reserved but unattached IP already costs ≈ $7/month.**

### Phase 2 — install and stage the node

```bash
gcloud compute scp --recurse --tunnel-through-iap --zone us-central1-a --project crossdesk-devnet-app \
  deploy/own-devnet-validator crossdesk-validator:~/own-devnet-validator
gcloud compute ssh crossdesk-validator --zone us-central1-a --project crossdesk-devnet-app \
  --tunnel-through-iap -- 'sudo bash ~/own-devnet-validator/02-bootstrap-node.sh'
```

`02-bootstrap-node.sh` works as follows:
- It refuses to run if DevNet's version ≠ the pinned 0.8.1.
- It installs Docker CE with the compose plugin (≥ 2.26), plus jq and grpcurl, and
  configures log rotation.
- It downloads `0.8.1_splice-node.tar.gz` (`github.com/digital-asset/decentralized-canton-sync`
  releases) and extracts `docker-compose/validator`.
- It installs the **CrossDesk overlay** `node/compose-crossdesk.yaml` and patches
  `start.sh`/`stop.sh` to pass the overlay last.
- It pre-pulls the images.

The overlay's merge was verified with `docker compose config`:

- The participant's `auth-services` goes from `[]` (**no auth**: anyone reaching 5001 acts
  as anyone) to **`unsafe-jwt-hmac-256`** with our secret and audience. The validator app
  authenticates to its participant with `self-signed` tokens under the same secret. The
  config keys are the ones Splice's own LocalNet uses (`conf/canton/app-provider/app-auth.conf`).
- The gRPC Ledger API is on `<internal-ip>:5001`, and the JSON Ledger API on `127.0.0.1:7575`.
- nginx and the wallet UI stay on `127.0.0.1:80`, as shipped.
- The `crossdesk-scan-proxy` runs on `<internal-ip>:5012` → sponsor Scan. It lets the Cloud
  Run desk reach the IP-allowlisted DevNet registry for Canton Coin.

### Phase 2b — onboard (the moment 03 says READY)

```bash
gcloud compute ssh crossdesk-validator ... -- 'sudo bash ~/own-devnet-validator/04-onboard.sh'
```

The script re-runs 03 as a gate, then takes these steps:
- It pulls the HMAC secret from Secret Manager (through the VM's SA).
- It takes a **self-serve secret**:
  `POST https://sv.sv-1.dev.global.canton.network.sync.global/api/sv/v0/devnet/onboard/validator/prepare`
  (the SV app `sv.`, **not** `scan.`; valid 1 h, one use).
- It runs `./start.sh -s <sponsor> -o <secret> -p crossdesk-validator-1 -w`.

Two deviations from the brief, both forced by the 0.8.1 sources:
- **Party hint `crossdesk-validator-1`, not `crossdesk`.** Since Splice 0.2.x a new
  validator's hint *must* be `<organization>-<function>-<enumerator>` (release notes), and
  the hint becomes the permanent operator party id.
- **`-m MIGRATION_ID` is omitted.** The 0.8.1 `start.sh` says the migration id is "no longer
  required … recommended for new deployments" to omit it. DevNet's value is 1 (recorded
  above). Set `MIGRATION_ID=1` only to match an older DB name.

Restarts: `RESTART=1 04-onboard.sh` (passes `-o ""`).

### Phase 3 — auth: how the desk gets a token that never goes stale

**The Noders-era bug.** The desk held a Keycloak access token (~3 h) plus an offline
refresh token, both as plain env values on `crossdesk-devnet-api` (they are still there;
06 removes them). When the refresh chain broke, every call answered `UNAUTHENTICATED`.

| Option | How the desk gets tokens | For | Against |
|---|---|---|---|
| **A. HMAC (chosen for DevNet)** | The participant runs `unsafe-jwt-hmac-256`. The desk **mints its own** HS256 `{sub, aud, iat, exp}` from a Secret Manager secret, re-mints every 20 min (1 h TTL) | Nothing to expire or revoke out from under us, no IdP, no network call. The exact shape Splice LocalNet ships, so Track A tests it. Zero cost | Symmetric: **whoever holds the secret can mint ANY user, including the participant admin**. Canton labels it "unsafe": development only. No per-client revocation (rotate = new secret + restart both) |
| B. OIDC (`start.sh -a`) + client-credentials | IdP (Auth0 / Keycloak / Entra) issues RS256 tokens; the participant verifies via JWKS; the desk runs `LEDGER_AUTH_MODE=client-credentials` | Asymmetric, revocable, per-client scopes. **What TestNet/MainNet need** | An IdP to run or buy. The validator app, wallet UI and CNS UI all need OAuth clients. More moving parts than a 90-day-reset DevNet warrants. An IdP outage takes the desk dark |
| C. No auth + firewall only | none | simplest | any host in the VPC acts as any party; one firewall mistake is total compromise |

**Decision: A on DevNet, with network isolation as the second layer.**
- The Ledger API is reachable only from the Cloud Run egress subnet.
- The secret lives only in Secret Manager. Its accessors are the VM SA (to start the node)
  and the Cloud Run SA (to mint), and the value reaches Cloud Run as a secret reference.
- The desk's ledger user `crossdesk-backend` holds **only CanActAs and CanReadAs on the
  CrossDesk parties and the wallet party. It has no ParticipantAdmin.**
- **Move to B before TestNet/MainNet.** The desk side is already built:
  `LEDGER_AUTH_MODE=client-credentials` with `LEDGER_TOKEN_ENDPOINT`, `LEDGER_CLIENT_ID`,
  `LEDGER_CLIENT_SECRET`, `LEDGER_TOKEN_AUDIENCE`.
- **Rotation:** `openssl rand -hex 32 | gcloud secrets versions add crossdesk-ledger-hmac-secret --data-file=- --project crossdesk-devnet-app`,
  then `RESTART=1 04-onboard.sh` on the VM, then roll a new Cloud Run revision (a secret
  version is resolved per revision).
- **TLS:** plaintext gRPC inside the VPC. Google encrypts VM↔VM traffic in transit at the
  network layer, but this is not end-to-end TLS. The desk supports `LEDGER_TLS=true` with a
  public-CA cert. A private CA would need a small change (`LedgerConnection.clientTls`).

### Desk code changes (this commit, `backend/`, all behind env vars)

The same image now runs every shape:

| Env | Effect |
|---|---|
| `LEDGER_AUTH_MODE` = `auto` (default) · `none` · `static` · `refresh` · `client-credentials` · `hmac` | `auto` keeps every existing config working: a refresh token means Noders-style refresh; a JWT means static; nothing means sandbox |
| `LEDGER_HMAC_SECRET`, `LEDGER_TOKEN_SUBJECT`, `LEDGER_TOKEN_AUDIENCE`, `LEDGER_TOKEN_TTL_SECONDS`, `LEDGER_TOKEN_SCOPE` | hmac minting; **in hmac mode `applicationId` is forced to `LEDGER_TOKEN_SUBJECT`** (Ledger API v2: userId must equal `sub`) |
| `LEDGER_TOKEN_ENDPOINT`, `LEDGER_CLIENT_ID`, `LEDGER_REFRESH_TOKEN`, `LEDGER_CLIENT_SECRET`, `LEDGER_REFRESH_SECONDS` | refresh / client-credentials, the same names backend-devnet used |
| `LEDGER_PARTIES` | `Label=fullPartyId,...` roster. It replaces the party-management listing (a non-admin user cannot call it; on a validator it would also return DSO/operator parties). The format is shared with backend-devnet |
| `LEDGER_AUTHORITY` | gRPC `:authority` override, for a name-routed proxy (e.g. `grpc-ledger-api.localhost`) |

Mechanism: one gRPC channel interceptor attaches the **current** token to every call
(`LedgerConnection`). A renewal therefore needs no reconnect and drops no streams, and there
is no more per-submission `withAccessToken`, which pinned the static JWT.
Files: `config/LedgerProperties.java`, `config/LedgerConnection.java`,
`config/TokenRefresher.java` (new), `config/HmacTokenMinter.java` (new),
`config/PartyRoster.java` (new), `ledger/LedgerService.java`, `registry/RegistryService.java`,
`web/HealthController.java`, `application.yml`; tests in `config/LedgerAuthConfigTest.java`.

### Phase 4 — DAR, parties, rights

```bash
gcloud compute ssh crossdesk-validator --zone us-central1-a --project crossdesk-devnet-app \
  --tunnel-through-iap -- -N -L 18080:127.0.0.1:80 -L 17575:127.0.0.1:7575 &
CD_PROFILE=own-devnet ./05-upload-dar-and-parties.sh
```

The script does four things:
- It uploads the DAR (SDK 3.4.11 / LF 2.2; accepted by Canton 3.5.17 on LocalNet).
- It allocates `issuer`, `bank`, `alice`, `bob`, `auditor`, `venue` and `agent` with the
  `-crossdesk` suffix. **Venue and Agent are added** because the desk resolves `Venue` at 26
  call sites; on Noders they were doubled onto bank/bob.
- It reads the wallet party (the primary party of user `administrator`, i.e. the validator
  operator).
- It creates `crossdesk-backend` with CanActAs + CanReadAs on all 8 parties, and writes
  `.roster-own-devnet`.

### Phase 5 — fund

- **Canton Coin:** the wallet tap (DevNet faucet). T2 does it, or use the wallet UI through
  the tunnel (`http://wallet.localhost:18080`, user `administrator`). Traffic top-ups are
  automatic on DevNet.
- **Real BitSafe CBTC:** request from `https://cbtc-faucet.bitsafe.finance` (0.01–1 per
  request) to each `*-crossdesk` party id. Then take either path:
  - T3 accepts it through the desk, `/api/token-standard/pending` → `/accept`.
  - The flow in `docs/TOKEN_STANDARD_RUNBOOK.md` §1 (query **by interface** with
    `#package-name` filters; choice context from the registry; exercise with the resolved
    interface package id; accept within `executeBefore`).

  **Re-verify the CBTC registrar party id after any DevNet reset.** The one in
  `profiles/own-devnet.env` dates from 2026-08-04.

### Phase 6 — wire the desk, then (only then) the site

`crossdesk-devnet-api` currently runs a **backend-devnet** image. It needs a backend/ image
from this commit first. **Not done; this was preparation only**:

```bash
cd backend && ./gradlew bootJar && cp build/libs/canton-dvp-desk-1.0.0.jar ../backend-devnet/cloudrun/app.jar
gcloud run deploy crossdesk-devnet-api --source ../backend-devnet/cloudrun \
  --project crossdesk-devnet-app --region us-central1 --no-traffic --tag own-node
CD_PROFILE=own-devnet ./06-wire-backend.sh            # dry run: prints the env-only update
CD_PROFILE=own-devnet EXECUTE=1 ./06-wire-backend.sh  # Direct VPC egress + env + secret ref
CD_BACKEND_URL=<the service URL> CD_PROFILE=own-devnet ./07-e2e-real-devnet.sh
# all PASS (T3 may SKIP until CBTC is requested) →
CD_PROFILE=own-devnet EXECUTE=1 SWITCH_SITE=1 ./06-wire-backend.sh   # Firebase /api/** → crossdesk-devnet-api
```

### Phase 7 — the end-to-end test plan

See [`E2E_TEST_PLAN.md`](E2E_TEST_PLAN.md) for each test's exact call and expected result.
`07-e2e-real-devnet.sh` runs them.

---

## Rollback

| What | How |
|---|---|
| Site | set `frontend/firebase.json` `/api/**` `serviceId` back to `crossdesk-demo` → `firebase deploy --only hosting --project crossdesk-devnet-app`. `crossdesk-demo` is never modified by these scripts |
| Cloud Run | `gcloud run services update-traffic crossdesk-devnet-api --to-revisions <previous>=100 --project crossdesk-devnet-app --region us-central1` |
| Desk config | `LEDGER_AUTH_MODE=static` (or unset the HMAC vars): the same image behaves as before |
| Node | `sudo /opt/splice/splice-node/docker-compose/validator/stop.sh`; the originals are kept as `start.sh.orig`/`stop.sh.orig` |
| Everything GCP | stop the VM. Deleting it needs `--deletion-protection` removed first, and the boot disk is kept (`auto-delete=no`) |

## Cost controls

- **Stop the VM when idle:** `gcloud compute instances stop crossdesk-validator --zone us-central1-a --project crossdesk-devnet-app`
  (≈ $115 → ≈ $17/month). The static IP stays reserved, so the allowlist entry survives.
  After a long stop, the participant catches up on start. Expect minutes, and do not demo
  straight after a start.
- **Once devnet is live behind the site:** `gcloud run services update crossdesk-demo --min-instances 0 --project crossdesk-devnet-app --region us-central1`
  (it is 2 CPU/4 GiB at minScale 1 today).
- `crossdesk-devnet-api` minScale 1 is needed only while demoing. Set it to 0 otherwise.
- Budget alert (free): `gcloud billing budgets create` on the billing account, for example
  at $150/month.

## DevNet resets (every ~3 months) and upgrades

- A reset wipes the network. Our parties, the DAR, CC and CBTC are gone, and the
  **participant namespace changes**. The Splice docs cover the procedure
  (`validator_operator/validator_network_resets.html`). Summary:
  1. `stop.sh`.
  2. Wipe the postgres volume (`docker volume rm splice-validator_postgres-splice`).
  3. Re-check `/info` for the new version.
  4. `SPLICE_VERSION=<new> 02`, then `04` (a new secret), then `05`; `06 EXECUTE=1` pushes
     the new roster, then `07`.
- **The static IP stays allowlisted across resets.** Reset times are announced in the GSF
  Slack channel `#validator-operations` (per the docs). The mailing list is
  `validator-announce` on lists.sync.global.
- The 0.8.1 reset page still says "redeploy with migration id 0". With compose 0.8.1,
  **omit `-m`**: the validator resolves the migration id itself.
- Upgrades arrive on DevNet first. Bump `SPLICE_VERSION`, re-run 02, then `RESTART=1 04`.
  Never change the migration id by hand.

## Backups

Right after onboarding, and after every upgrade, save the participant identities (they are
what recovers the validator's CC). Use the wallet UI's identities download, or the
validator admin API `GET /api/validator/v0/admin/participant/identities`. Store the dump in
Secret Manager or a locked bucket, **never in this repo**. See `validator_backups.html`.

## What still blocks full automation

1. **The allowlist is human, and takes 2–7 days.** The request goes to the Canton Foundation
   through a form or a sponsor ([`ALLOWLIST_REQUEST.md`](ALLOWLIST_REQUEST.md)).
2. **The CBTC faucet is a web form** (no API was found); T3 SKIPs on DevNet until it is used.
3. **The 1-hour onboarding secret** means 04 must run the moment 03 is green.
4. **Deploying the backend/ image** to `crossdesk-devnet-api` (Phase 6) is a deliberate
   manual step.
5. **GCP prices are unverified** (the Billing API is disabled on the project).
