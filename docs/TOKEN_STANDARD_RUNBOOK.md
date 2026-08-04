# Token Standard on DevNet — the runbook

**Written 2026-08-04**, after claiming real BitSafe **CBTC** on the HackCanton devnet node.
Every error below was actually hit and actually solved. **Read §1 first — it is the whole recipe
in six steps.** The rest is why, and what goes wrong.

**Use this for cETH (onRails).** It will be the same flow with a different registrar.

---

## 0. The facts you need

```
Node (gRPC)   ledger-api-grpc.participant.hackcanton-01.devnet.naas.noders.services:443
Node (JSON)   https://ledger-api-json.participant.hackcanton-01.devnet.naas.noders.services
Registry API  https://api.utilities.digitalasset-dev.com          <-- THE ONE THAT MATTERS
Keycloak      https://keycloak.naas.noders.services/realms/noders-appsfactory/protocol/openid-connect/token
userId (sub)  8b9dc176-f2a4-445b-92ba-a2ca55ed1da9
```

Party suffix, identical for all five: `::122003aa7c491e00a453145c4d2cd3dbf5db8908b4e663c9944baed57fd66effa668`
Parties: `alice-crossdesk` · `bob-crossdesk` · `bank-crossdesk` · `issuer-crossdesk` · `auditor-crossdesk`
Wallet party: `8b9dc176-f2a4-445b-92ba-a2ca55ed1da9::<same suffix>`

CBTC registrar: `cbtc-network::12202a83c6f4082217c175e29bc53da5f2703ba2675778ab99217a5a881a949203ff`
CBTC faucet: `https://cbtc-faucet.bitsafe.finance` (min 0.01, max 1 per request)

🔑 **How the registry URL was found** — it is not published anywhere. The Digital Asset registry
UI at `https://registry.dev.app.digitalasset.com` loads a runtime config at **`/config.js`**, which
contains `utility_backend_url`. Confirmed correct because its `dsoPartyId` is **byte-identical** to
the one the local validator returns at `/api/validator/v0/scan-proxy/dso-party-id`.
**Same trick works for mainnet:** `registry.app.digitalasset.com/config.js` → `api.utilities.digitalasset.com`.

---

## 1. THE RECIPE — accept a Token Standard transfer

**① Get a JWT** (3-hour life). One line in PowerShell — see §2.1 for why one line:

```
$body="grant_type=password&client_id=web-app-ui-hackcanton-01-devnet&scope=openid daml_ledger_api offline_access&username=EMAIL&password=PASSWORD"; $r=Invoke-RestMethod -Method Post -Uri "https://keycloak.naas.noders.services/realms/noders-appsfactory/protocol/openid-connect/token" -ContentType "application/x-www-form-urlencoded" -Body $body; $r.access_token
```

**② Find pending offers — query BY INTERFACE, never by template.** Their contracts are on *their*
templates; a concrete-template query returns nothing.

```
POST /v2/state/active-contracts
{"filter":{"filtersByParty":{"<party>":{"cumulative":[{"identifierFilter":{"InterfaceFilter":{"value":{
  "interfaceId":"#splice-api-token-transfer-instruction-v1:Splice.Api.Token.TransferInstructionV1:TransferInstruction",
  "includeInterfaceView":true,"includeCreatedEventBlob":true}}}}]}}},
 "verbose":false,"activeAtOffset":<ledger-end>}
```

**③ Get the choice context from the registry. No auth required.**

```
POST https://api.utilities.digitalasset-dev.com/api/token-standard/v0/registrars/{registrarParty}/registry/transfer-instruction/v1/{offerCid}/choice-contexts/accept
body: {}
```
`{registrarParty}` is URL-encoded (`::` → `%3A%3A`). It returns `choiceContextData` (containing
`utility.digitalasset.com/transfer-rule`, the instrument configuration, credentials) and
`disclosedContracts` with `createdEventBlob`s.

**④ Exercise the interface choice, passing the context AND the disclosed contracts.**

```
POST /v2/commands/submit-and-wait
{"commands":[{"ExerciseCommand":{
   "templateId":"55ba4deb0ad4662c4168b39859738a0e91388d252286480c7331b3f71a517281:Splice.Api.Token.TransferInstructionV1:TransferInstruction",
   "contractId":"<offerCid>","choice":"TransferInstruction_Accept",
   "choiceArgument":{"extraArgs":{"context":<choiceContextData>,"meta":{"values":{}}}}}}],
 "commandId":"<unique>","userId":"8b9dc176-f2a4-445b-92ba-a2ca55ed1da9",
 "actAs":["<receiver party>"],"readAs":[],
 "disclosedContracts":[{"templateId":…,"contractId":…,"createdEventBlob":…,"synchronizerId":…}]}
```

**⑤ Verify by interface** — query `#splice-api-token-holding-v1:Splice.Api.Token.HoldingV1:Holding`
and sum `viewValue.amount` where `owner` is the party.

**⑥ Wire it into the desk** so it is a button, not a script:
```
REGISTRY_REMOTE_URLS=cbtc-network::1220…=https://api.utilities.digitalasset-dev.com
```

**The working script is `/tmp/acc.py`** — it loops every party, fetches each context and accepts.
Reuse it for cETH by changing nothing except which registrar appears in the offers (it reads the
registrar from each offer's `instrumentId.admin`, so it already handles any issuer).

---

## 2. EVERY ERROR, AND WHAT IT MEANT

### 2.1 `Unexpected token '$r' in expression or statement`
**PowerShell.** A multi-line paste ran together into one statement.
**Fix:** paste as **one line**, semicolon-separated. Not a Canton problem at all.

### 2.2 `INVALID_ARGUMENT: Invalid template:…:TransferOffer or choice:Accept`
The choice is **not exposed on the template** — only through the interface. The error says
"template **or** choice" and is deliberately unhelpful about which.
**Fix:** exercise `TransferInstruction_Accept` with the **interface id** in `templateId`.
The same error appeared for `TransferOffer_Accept`, which *does* exist in the package binary — it
is simply not reachable as a template choice from the Ledger API.

### 2.3 `LEDGER_API_INTERNAL_ERROR: Expected ujson.Obj (data: [])`
A Daml `TextMap` must be a JSON **object**, not an array.
**Fix:** `{"values":{}}` — never `{"values":[]}`.

### 2.4 Interface query returns zero contracts with an explicit package id
Filtering with `55ba4deb…:Splice.Api.Token…:TransferInstruction` matched nothing, even though the
response *reports* that exact interface id.
**Fix:** filter with the **package-name form** `#splice-api-token-transfer-instruction-v1:…`.
🔴 Use `#name` for **filters**, the resolved package id for **exercises**.

### 2.5 `MISSING_FIELD: contract_id`
Cascade — the contract-id extraction returned empty because of 2.4, and the command went out with
a blank cid. **Always echo the cid before submitting.**

### 2.6 🔴 `Missing context entry for: utility.digitalasset.com/transfer-rule`
**The real one.** `TransferInstruction_Accept` looks up the registrar's `TransferRule` contract in
the `ChoiceContext`. An empty context always fails. The rule is **not visible to the receiver** —
it is not in any of our parties' ACS — so it must arrive as a **disclosed contract** from the
registry API.
**Fix:** step ③ + ④ above.

### 2.7 The faucet's warning is a red herring
> *"The receiving party must have the DA Utility Registry installed on their node."*

We chased this for a long time. **The packages were already vetted** — the parties could see
`utility-registry-app-v0` contracts, which is only possible if the packages are on the node.
The actual missing thing was the **choice context**, not an installation.
**Lesson:** if a party can *see* a contract of a template, that template's package is vetted.
Do not diagnose an install problem when the contracts are visible.

### 2.8 The Splice wallet cannot help
`/api/validator/v0/wallet/token-standard/transfers` returns `{"transfers":[]}` even with offers
sitting in that party's ACS. **The Splice wallet handles Amulet/Canton Coin, not Utility Registry
assets.** Do not expect the wallet UI to show CBTC.
Also: the **wallet party is not your app parties** — it is `8b9dc176-…::1220…`. Sending to
`alice-crossdesk` will never appear in the wallet.

### 2.9 Dead ends when hunting the registry URL (do not repeat these)
- `registry.bitsafe.finance`, `cbtc-registry.bitsafe.finance`, `cbtc.devnet.bitsafe.finance` — DNS fail
- `api.bitsafe.finance` — 404
- `utility.digitalasset.com`, `registry.utility.digitalasset.com` — DNS fail
- `api.devnet.bitsafe.finance` — **real**, "Omnibus cBTC API", Swagger at `/swagger-ui/`, spec at
  `/api-docs/openapi.json`. Serves `/cbtc/v1/token-standard-contracts` with blobs for
  `burn_mint_factory`, `instrument_configuration`, `issuer_credential` — **but NOT the transfer rule.**
  Useful, not sufficient.
- ✅ **The answer was `/config.js` on the DA registry UI.** Frontend runtime config is where hosted
  SaaS puts its backend URL. **Try `/config.js` first next time.**

### 2.10 `deadline-exceeded`
One offer failed with `stdlib.daml.com/deadline-exceeded`. Offers carry `executeBefore`
(faucet default: **+7 days**). That one had passed.
**Fix:** re-request from the faucet. Accept promptly.

### 2.11 `HolderService` is deprecated — do not build against it
The package binary contains `HolderService_AcceptTransferOffer`, which looks like an onboarding
path. **It is deprecated since Utility 0.8.0** and is planned to be disabled via `ensure false`.
Ignore it.

### 2.12 Git Bash `/tmp` ≠ Windows Python `/tmp`
A script written to `/tmp` from Git Bash lands in `%LOCALAPPDATA%\Temp`; Windows Python then
cannot open `/tmp/x`. **Fix:** `cd /tmp` and use a **relative** filename.

---

## 3. FOR cETH (onRails) — what to expect

onRails' integration page states cETH is **"a CIP-0056 asset"**, so the same flow should apply.

1. Watch for pending offers with `instrumentId.admin` = onRails' registrar party.
2. `/tmp/acc.py` already reads the registrar from each offer, so **it should just work**.
3. If their registry is also DA-hosted, `api.utilities.digitalasset-dev.com` serves it too — the
   registrar party is in the path, so one host serves many registries.
4. If not, find their config: fetch their registry UI and read **`/config.js`** (§2.9).
5. Add their registrar to `REGISTRY_REMOTE_URLS`.

**Ask onRails for:** registrar party id (full, with `::1220…`), the instrument id, their registry
API base URL, and confirmation their holdings implement `HoldingV1`.

---

## 4. RESULT — 2026-08-04

| Party | CBTC |
|---|---|
| alice-crossdesk | 0.84 |
| bob-crossdesk | 0.53 |
| bank-crossdesk | 0.91 |
| issuer-crossdesk | 0.88 |
| auditor-crossdesk | 0.01 |
| wallet party | 0.99 |
| **Accepted total** | **4.16** |

Verified by `HoldingV1` interface query. These are **BitSafe's contracts, on BitSafe's templates,
owned by our parties** — not self-issued.

🔑 **The interface package is the same one this project vendors:**
`55ba4deb0ad4662c4168b39859738a0e91388d252286480c7331b3f71a517281`. Our CIP-56 code and BitSafe's
tokens speak the identical Token Standard package, which is why the desk can consume them.

---

## 5. BUILD-SIDE ERRORS worth remembering

- **`Disallowed language version … expected 2.1, 2.2, 2.3 but got 1.14`** — the node is Canton 3.x
  and rejects LF 1.x. Build with SDK 3.4.11 (LF 2.2). This is also why **Daml Finance cannot be
  used**: it targets `--target=1.17`.
- **Node runs Canton 3.5.10** (`GET /v2/version`) while we build on SDK 3.4.11. Fine — 3.5 accepts
  LF 2.2 — but do not assume the node matches the SDK.
- **`SealedOrder` gained `seqNo`** → **always re-run `daml codegen java` and rebuild both backends**
  after any template field change, or the backend decodes against a stale schema.
- **`git add -A` swept an agent's in-flight files into the wrong commit.** Use path-scoped
  `git add <path>` while background work is running.
- **Warning: templates + `daml-script` in one package** — uploading the DAR uploads daml-script to
  the participant. Pre-existing; split tests into their own package after the pitch.
