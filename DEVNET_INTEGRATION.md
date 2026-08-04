# CrossDesk — Devnet Integration: how it works, what was ported, what's proven

This document is the honest, end-to-end account of taking CrossDesk from a
**local Daml 2.9.4 sandbox** to the **shared HackCanton devnet node** (NODERS
`hackcanton-01`, Canton 3.x). It explains the deployment model, the auth model,
the full Ledger-API-v1 → v2 port, every problem hit and the attempt that fixed
it, and exactly what is proven working versus the one thing still pending.

> **TL;DR.** The desk is deployed on the shared node, the backend connects to it
> over TLS + JWT, and — after the `actAs` puzzle in §6.7 — **a full atomic DvP
> settled on-chain on 2026-07-19**. Everything on our side (Daml package, v2
> bindings port, read and write paths) is done and verified.
>
> ⚠️ **What is on the node is the PRE-FEEDBACK build.** Package `72ec9833…` is the
> DAR as it stood at the original submission. The post-finalist-feedback rebuild
> (price discovery, complete-order commitments, the CIP-56 module, the contestable
> mandate, accruing NAV) is **not on devnet** — uploading a DAR is admin-only on
> the node operator's side. See **§9**.

---

## 1. What CrossDesk is (30-second recap)

An institutional settlement desk on Canton:

- **Atomic DvP** — two tokenised legs (e.g. `cETH` ↔ `USDC`) move together or not
  at all (no Herstatt/principal risk).
- **Sealed Market-on-Close** — a uniform-price call auction whose order book is
  invisible until it clears (no front-running; the mempool can't leak it), and
  whose clearing price is **discovered from the book** by a pure, deterministic
  function rather than supplied by the operator.
- **K-of-N NAV committee** — an attested official mark no single venue can strike,
  which the auction anchors on.
- **In-kind ETF/basket builder** — create/redeem fund shares against underlyings
  atomically.

All of it is `daml/` (the on-ledger logic) + `backend/` (Spring Boot over the
Ledger API) + `frontend/` (React). Local build details are in `README.md`; the
system as it stands today is `docs/HOW_IT_WORKS.md`.

---

## 2. The deployment model — who does what

Canton separates *writing code* from *running a node*. We wrote the code; NODERS
runs the node. Concretely:

| Step | Who | What |
|---|---|---|
| Build the Daml model → **DAR** | us | `daml build` packages every template into one `.dar` (a "compiled jar of smart contracts") |
| **Upload the DAR** to the participant | **node operator (Kiryl/NODERS)** | admin-only: makes the CrossDesk templates exist on the node |
| **Allocate parties** (`issuer-crossdesk`, …) | **node operator** | admin-only: creates the on-ledger identities |
| **Grant our user `actAs`/`readAs`** on those parties | **node operator** | admin-only: attaches permissions to our login |
| Connect a backend + drive the desk | us | our JWT + TLS, over the Ledger API |

The three admin steps require **operator rights on the participant**, which only
the node runner has. That's why the DAR was handed to Kiryl to deploy, and why
the final `actAs` grant is his to set (see §7).

---

## 3. The auth / connection model

```
appsfactory login (your email + password)
        │   → NODERS' identity provider (Keycloak)
        ▼
   JWT issued  ── your "keycard": proves you're `sborjas`, scope daml_ledger_api
        │
        ▼  sent inside TLS (encrypted tunnel; the node presents a CA-signed cert)
        ▼
   Participant node — Ledger API (gRPC :443)
        │   node checks the JWT, then your readAs/actAs rights per party
        ▼
   Synchronizer — orders & finalizes the transaction across participants
```

- **JWT** = identity. Minted by `POST` to the Keycloak token endpoint with your
  appsfactory credentials. It is a *bearer* token — no client certificate.
- **TLS** (not mTLS) = the secure pipe. Only the **server** shows a cert; the
  client authenticates with the **JWT**, not a cert.
- **readAs vs actAs** — two rights the node attaches to your user, **per party**:
  - `readAs` → *view* a party's contracts (read-only).
  - `actAs` → *submit transactions as* a party (create, exercise, settle).
  - Reads need only readAs; **every state-changing action needs actAs.**
- **synchronizer** = Canton's sequencing/finality layer (formerly "domain"). Our
  submit passes an empty `synchronizerId`, i.e. "node, pick the synchronizer."
- **appsfactory account** is specific to *this* node — NODERS uses Keycloak as the
  gatekeeper. Running your own node, you'd wire your own auth (own Keycloak/Auth0,
  or no-auth for a local sandbox). The pattern (an IdP issuing JWTs) is universal.

Connection endpoints (from the hackathon Materials tab):

- Ledger API (gRPC, TLS): `ledger-api-grpc.participant.hackcanton-01.devnet.naas.noders.services:443`
- Token (OIDC): `https://keycloak.naas.noders.services/realms/noders-appsfactory/protocol/openid-connect/token`
  (client `web-app-ui-hackcanton-01-devnet`, scope `openid daml_ledger_api offline_access`)

---

## 4. Why there are two backend builds

The shared node runs **Canton 3.x**, which rejects the SDK 2.9.4 package format
and speaks **Ledger API v2**. Rather than break the working local demo, the port
originally lived in a copy:

| | `backend/` | `backend-devnet/` |
|---|---|---|
| Target | local sandbox (plaintext, no auth) | HackCanton shared node (TLS + JWT) |
| Party management | admin gRPC service → **config roster** | **config roster** (v2 dropped the admin service) |
| Default port | 8080 | **8090** (so both run side by side) |

**Update — since the SDK 3.4.11 pin (commit `6529754`), the two are on the same
line.** `daml.yaml` now pins `sdk-version: 3.4.11` (LF 2.2) so the repo reproduces
exactly what the devnet node runs, and **both** backends were regenerated and
compile against **`bindings-java` + `bindings-rxjava` 3.4.0 / Ledger API v2**. The
row that used to read *"`backend/`: 2.9.4 / LF 1.14 / v1 bindings"* is no longer
true. What still distinguishes them is the target and the auth model, above.

The Daml source is the same portable subset for both — one small change (removing
a contract **key** from `Instrument`, since **LF 2.x removed contract keys**) was
required and is harmless to the local build.

**And one addition the pin made possible.** The CIP-56 Token Standard packages
(`splice-api-token-*-v1-1.0.0`) are built with SDK **3.5.2** targeting **LF 2.1**.
A higher-LF package may consume a lower-LF `data-dependency`, so pinning at 3.4.11
/ LF 2.2 lets them be vendored into `deps/` and linked with **no SDK upgrade and
no network fetch at build time**. They appear as `.dalf` entries in the built DAR
(`daml damlc inspect-dar`), which is how you verify the dependency is genuine
rather than re-declared locally.

---

## 5. The Ledger API v1 → v2 port (what actually changed in code)

All changes are confined to two files: `config/LedgerConnection.java` and
`ledger/LedgerService.java` (plus a `parties` property and the run script).

| v1 (2.9.4) | v2 (3.x) | Why |
|---|---|---|
| `PartyManagementServiceGrpc` admin channel to list parties | read a configured `ledger.parties` roster | v2 rxjava bindings **dropped the party-management admin service**; on a shared node the parties are pre-allocated + fixed anyway |
| `client.getActiveContractSetClient()` | `client.getStateClient()` | renamed in v2 |
| `getActiveContracts(filter, parties, verbose)` | `getActiveContracts(filter, parties, verbose, ledgerEndOffset)` | v2 ACS queries are **offset-anchored**; we fetch `getStateClient().getLedgerEnd()` per query |
| `CommandsSubmission.create(appId, cmdId, cmds)` | `create(appId, cmdId, Optional<synchronizerId>, cmds)` | v2 added an optional synchronizer selector |
| `withAccessToken(Optional<String>)` | `withAccessToken(String)` | signature change |
| `c.getLedgerId()` handshake | (removed) — a successful `connect()` is the confirmation | v2 has no ledger-id handshake |
| `submitAndWaitForTransactionTree(sub)` → `TransactionTree` | `submitAndWaitForTransaction(sub, TransactionFormat)` → `Transaction` with **`LEDGER_EFFECTS`** shape | Canton 3.3+ **removed the transaction-tree command endpoints**; `LEDGER_EFFECTS` returns the same full node set. `CreatedEvent implements both Event and TreeEvent`, so the decode logic is unchanged |

Net: same behaviour, v2 surface. Both the **read path** and the **write path**
were re-pointed and verified against the live node.

---

## 6. Problems hit, and the attempt that resolved each

A truthful timeline — this is where the real engineering was.

1. **DAR upload rejected: "Disallowed language version … expected 2.1, 2.2, 2.3
   but got 1.14."**
   The node's Canton 3.x only accepts LF 2.x packages; SDK 2.9.4 emits LF 1.14.
   → **Rebuilt the DAR with SDK 3.4.11 (LF 2.2).** That surfaced the next one.

2. **LF 2.x removed contract keys.**
   `Instrument` declared a `key`. → **Removed the key/maintainer** (nothing looks
   an instrument up by key — the desk queries the ACS), keeping the package
   portable across the 2.x and 3.x lines. Kiryl then uploaded it successfully
   (package `72ec9833…`).

3. **Backend wouldn't compile against v2 (182 errors).**
   The v1 bindings API is gone. → Traced the failures to **two files**, and
   ported them (§5): party-management → config roster; ACS client rename +
   offset; `CommandsSubmission` synchronizer arg; token signature; drop
   `getLedgerId`. Down to a clean compile + 41 MB jar.

4. **`javap` showed the *wrong* API at first.**
   It resolved the 2.9.4 jar ahead of the 3.4 one. → Re-ran `javap` against the
   **exact** `3.4.0-snapshot` jars to read the real v2 signatures, then applied
   them.

5. **`Port 8080 was already in use.**"
   The local build owns 8080; the run script also hard-set 8080. → Made the
   devnet script default to **8090** (`SERVER_PORT="${SERVER_PORT:-8090}"`) so
   both run side by side.

6. **First real submit: `UNIMPLEMENTED: CommandService/SubmitAndWaitForTransactionTree`.**
   The node doesn't serve the removed tree endpoint. → Switched to
   **`submitAndWaitForTransaction(..., LEDGER_EFFECTS)`** and adapted the three
   result-decoders from `TransactionTree`/`TreeEvent` to `Transaction`/`Event`
   (trivial, since `CreatedEvent` implements both). Re-tested: the
   `UNIMPLEMENTED` error is **gone** — the command now reaches the node's
   authorization layer.

7. **`PERMISSION_DENIED` on every submit — SOLVED via the participant logs.**
   Reads returned `200`; writes were denied even after the operator granted actAs.
   Querying the node's **Grafana/Loki logs** for the denied `tid` revealed the real
   error: `Claims are only valid for userId '8b9dc176-…', actual userId is
   'canton-dvp-desk'`. In **Ledger API v2 the submission's `applicationId` IS the
   userId** — it must equal the token's `sub`, not an arbitrary app name.
   → Set `LEDGER_APPLICATION_ID=8b9dc176-…`. Next error: `Claims do not authorize
   to act as party 'Issuer'` — **actAs claims match FULL party ids** (`…::1220…`),
   not labels. → Resolve labels via `resolveParty()` before submitting.
   **Result: HTTP 201 — real contracts created, and a full atomic DvP settled on
   the shared node** (receipt `006ef8c599…`, 2026-07-19).

---

## 7. Current status — proven vs. pending

**Proven working against the live node:**

- ✅ Daml package deployed (LF 2.2, package `72ec9833…`).
- ✅ Backend **connects** over TLS + JWT and boots clean.
- ✅ **Read path** (v2 `getStateClient` + ledger-end offset): `GET /api/instruments`
  and `/api/parties` return **HTTP 200**.
- ✅ **Write path** (v2 `submitAndWaitForTransaction` + `LEDGER_EFFECTS`): the
  command travels all the way to the node and is evaluated (no more
  `UNIMPLEMENTED`).
- ✅ Token identity verified (`sborjas`, scope `daml_ledger_api`).

**RESOLVED — writes are live (2026-07-19):**

- ✅ The operator granted **`CanActAs`** on all CrossDesk parties (+ our own).
- ✅ With `applicationId = token userId` and full party ids in actAs, **real
  contracts create with HTTP 201**, and a **full atomic DvP settled on the shared
  node**: *alice-crossdesk → bob-crossdesk · 10 cETH @ 3,200 USDC*, receipt
  `006ef8c599…`, visible only to Alice/Bob/Auditor (sub-transaction privacy).
- ✅ The hosted demo (https://crossdesk-devnet-app.web.app) now reads this live
  on-chain state and can settle further transactions.

---

## 8. Reproduce it

See [`backend-devnet/RUNBOOK.md`](backend-devnet/RUNBOOK.md): three copy-paste
steps — mint JWT → `run-devnet.sh` → `smoke-devnet.sh` — for a real on-node
atomic settlement (once actAs is granted).

---

## 9. After the finalist feedback — what is on the node, and what is not

The Season 2 finalist feedback landed after everything above, and the rebuild it
prompted (2026-08-03, branch `feat/price-discovery-and-cip56`) changed the Daml
substantially: price discovery in `RunClose`, complete-order commitments,
price-priority allocation, the unpriced MOC order type, a clamping price collar,
the contestable `LiquidityMandate`, accruing NAV, and the CIP-56 `TokenStandardDvp`
module. **None of it is on the shared node.**

**The honest status table.**

| | State |
|---|---|
| Package on `hackcanton-01` | `72ec9833…` — the **pre-feedback** DAR |
| Hosted demo (`crossdesk-devnet-app.web.app`) | reads that package; the close it runs prints at the supplied reference |
| Recorded demo video | same build, same caveat |
| The rebuild | committed on `feat/price-discovery-and-cip56`, **not pushed** — `origin/master` is still the pre-feedback submission |
| Verification of the rebuild | **Daml Script scenarios** (53 at commit `73aca95`, all green), plus `./gradlew clean build` on both backends and a clean `tsc`. **No live-participant run.** |

**Why it isn't deployed, precisely.** Uploading a DAR to a participant is
admin-only — the same constraint documented in §2 that put the original DAR upload,
party allocation and `actAs` grant in the node operator's hands. It is not a code
problem and there is no client-side workaround; it is one request to Kiryl/NODERS.

**What the upload would then require, in order.** Worth stating so nobody assumes
it is a single button:

1. **Upload the new DAR.** It is a different package id, so the old contracts stay
   readable under the old package and the new templates arrive alongside them.
2. **Re-seed.** `ClosingAuction` gained `submittedCount` / `cancelledCount`, so
   auctions created under the old package cannot be closed by the new `RunClose` —
   the close would be scoring counters that do not exist. A demo on the new package
   starts from a fresh auction.
3. **`SubmitOrder` now returns a pair** `(ContractId ClosingAuction, ContractId
   SealedOrder)` and is **consuming**, so any caller must thread the new auction cid
   forward. Both backends already do; anything else driving the ledger directly
   would need the same change.
4. **`PublishImbalance` takes a `mandateCid`.** There is no bypass, so a devnet demo
   of imbalance disclosure needs `MandateTerms` posted and accepted first.

**What a live run would actually add over the tests.** Not correctness — the Daml
Script scenarios execute the identical choice bodies the node would. What it adds
is the *multi-participant* dimension a single-participant script cannot exercise:
sub-transaction privacy across separate participant nodes, and the synchronizer
confirming a transaction whose views are distributed. That is exactly what the
2026-07-19 DvP demonstrated for the settlement path (receipt `006ef8c599…`, visible
to Alice/Bob/Auditor only) — the auction path has not had the same treatment.

**The one thing that changed on our side and would need re-verifying on the node:**
`SubmitOrder` becoming consuming serialises submissions on the auction contract. On
a single-participant sandbox that is invisible; on a shared node with real latency,
two traders submitting at the same instant will contend. The desk re-resolves the
live open auction on **every** `POST /api/moc/order`, so it always submits against
the current cid and a *sequential* stream of orders is unaffected — but a genuine
concurrent race would surface as a failed submit the caller has to reissue. That is
the known, deliberately-accepted cost of the completeness guarantee, documented in
`MarketOnClose.daml`'s header, not a surprise.
