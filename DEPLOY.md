# Deploying to Canton Devnet

Deploying the DAR to **Canton Devnet** and executing a real settlement is the
HackCanton Season 2 **qualifying requirement**. This guide takes you from a clean
machine to a confirmed cETH settlement transaction running on-ledger.

Steps marked **[HUMAN ONLY]** require credentials or account access that must be
requested from the hackathon organisers / onRails — an automated agent cannot do
them for you. Everything else is copy-pasteable.

---

## 0. Prerequisites

| Need | How |
|---|---|
| DAML SDK | <https://docs.daml.com/getting-started/installation.html> |
| JDK 17+ | required by the DAML/Canton runtime |
| **[HUMAN ONLY]** Canton Devnet access | Register through the HackCanton Season 2 onboarding. You receive: a **participant node** endpoint (host:port for the Ledger API / JSON API), an **auth token** (JWT) or admin credentials, and a **synchronizer/domain** connection. |
| **[HUMAN ONLY]** cETH on Devnet | Request test cETH via the **onRails cETH form** provided in the bounty brief. Note the cETH **issuer party id** and the **instrument identifier** onRails uses — you may need to align the `instrument`/issuer in `daml/Token.daml` and the init script to match the real cETH registry on Devnet. |

> **Version note.** Confirm the exact **Splice / Daml SDK version** Devnet expects
> from the onboarding docs and set it in `daml.yaml → sdk-version` before building
> the DAR you upload. Build locally with the same version to avoid a DAR/ledger
> mismatch.

---

## 1. Confirm the code builds and passes locally

Never upload a DAR you haven't tested. From the repo root:

```bash
daml version          # copy this into daml.yaml -> sdk-version
daml test             # all six scripts must pass
daml build            # produces .daml/dist/private-ceth-settlement-1.0.0.dar
```

You now have the artifact to deploy:

```
.daml/dist/private-ceth-settlement-1.0.0.dar
```

---

## 2. [HUMAN ONLY] Get your Devnet connection details

From the onboarding you should have:

```
PARTICIPANT_HOST   e.g. participant.devnet.example
LEDGER_API_PORT    e.g. 5001            (gRPC Ledger API)
JSON_API_URL       e.g. https://participant.devnet.example/json  (optional convenience)
AUTH_TOKEN         a JWT bearer token, or admin-console credentials
DOMAIN / SYNCHRONIZER  the Devnet synchronizer your participant is connected to
```

Put secrets in a local `.env` (already git-ignored). **Never commit them.**

```bash
# .env  (DO NOT COMMIT)
PARTICIPANT_HOST=participant.devnet.example
LEDGER_API_PORT=5001
AUTH_TOKEN=eyJhbGciOi...
```

---

## 3. Upload the DAR to your Devnet participant

Use the DAML assistant against the remote Ledger API. With a JWT:

```bash
daml ledger upload-dar \
  --host "$PARTICIPANT_HOST" \
  --port "$LEDGER_API_PORT" \
  --access-token-file <(printf '%s' "$AUTH_TOKEN") \
  .daml/dist/private-ceth-settlement-1.0.0.dar
```

> If your Devnet uses the **Canton admin console** instead, upload with:
> `participant.dars.upload(".daml/dist/private-ceth-settlement-1.0.0.dar")`
> from the Canton console connected to your node.

Confirm the package is registered:

```bash
daml ledger list-parties --host "$PARTICIPANT_HOST" --port "$LEDGER_API_PORT" \
  --access-token-file <(printf '%s' "$AUTH_TOKEN")
```

---

## 4. Allocate parties on Devnet

Locally, `Test:initialize` allocates parties for you. On Devnet you allocate the
parties you control on your participant (the counterparty / issuer may live on a
different participant — that is the whole point of Canton). Two options:

**A. Run the init script against the remote ledger** (simplest for a solo demo
where you host all parties):

```bash
daml script \
  --dar .daml/dist/private-ceth-settlement-1.0.0.dar \
  --script-name Test:initialize \
  --ledger-host "$PARTICIPANT_HOST" \
  --ledger-port "$LEDGER_API_PORT" \
  --access-token-file <(printf '%s' "$AUTH_TOKEN")
```

**B. Allocate/adopt parties manually** (for a multi-participant demo): allocate
`Alice`, `Bob`, `Regulator` on your participant via the console/JSON API, and use
the **real onRails cETH issuer party** as the `issuer` for the cETH leg. In that
case seed cETH by requesting it from onRails to your `Alice` party rather than
minting it yourself.

> **cETH reality check.** In the local sandbox `Test:initialize` mints cETH by
> `submit issuer (createCmd Token ...)`. On Devnet you generally cannot mint the
> *real* cETH instrument yourself — onRails is its issuer. For the bounty, obtain
> real cETH to your maker party via the onRails form, then have your app model
> the cash leg with an issuer you control. The `Token` template is the shape;
> point its cETH `issuer`/`instrument` at the onRails registry values.

---

## 5. Execute a settlement on-ledger

You can drive the flow three ways — pick one:

- **Navigator** (if enabled on Devnet): log in as Alice, create a
  `SettlementProposal`; log in as Bob, exercise `Settle`.
- **`daml script`**: write a thin script (or reuse `testAtomicDvP`'s body adapted
  to the allocated party ids) and run it against the remote ledger as in step 4.
- **JSON API / gRPC**: `POST` a `create` for the proposal, then an `exercise` of
  `Settle`, using your `AUTH_TOKEN`.

---

## 6. Verify the settlement ran on-ledger

Confirm the state actually changed on Devnet (this is the evidence judges want):

- Query the ACS for the **`SettlementReceipt`** — its existence proves the swap
  committed atomically. Note its `contractId` and `settledAt`.
- Query as Bob: he now owns the **cETH** `Token`. Query as Alice: she now owns
  the **cash** `Token`.
- Query as the Regulator: it sees the **receipt** but **not** the holdings.
- Capture the **transaction id / update id** from the participant's transaction
  stream (Ledger API `GetTransactions`, or the console) — that is your on-ledger
  proof of a cETH state change for the bounty.

```bash
# Example: fetch the active contracts you can see (JSON API)
curl -s "$JSON_API_URL/v1/query" \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"templateIds":["Settlement:SettlementReceipt"]}' | jq .
```

---

## 7. Record the evidence for submission

For the HackCanton submission + the cETH bounty, save:

- the **DAR package id** you uploaded,
- the **update/transaction id** of the `Settle` transaction,
- the **`SettlementReceipt` contract id** and its payload,
- a note of the **cETH holding** moving owner (before/after).

Add these to `JOURNAL.md` as your deployment entry.

---

## Troubleshooting

| Symptom | Likely cause / fix |
|---|---|
| `sdk-version ... is not installed` | Run `daml install <version>` or set `daml.yaml` to an installed version (`daml version`). |
| DAR upload rejected / package mismatch | Build with the **Devnet's** Splice/Daml version; rebuild and re-upload. |
| `PERMISSION_DENIED` on submit | Your `AUTH_TOKEN` lacks `actAs`/`readAs` for that party. Re-issue a token scoped to the acting party. |
| `Settle` fails: "cETH must be owned by the maker" | The cETH `issuer`/`instrument`/owner in the proposal doesn't match the real onRails cETH holding. Align them (step 4). |
| Counterparty on another participant can't see the proposal | On real Canton the counterparty's participant must have the contract disclosed (explicit disclosure or shared observer). Ensure both parties' participants are connected to the same synchronizer and the observer is set. |
| Fetch of the other party's leg fails on-ledger | The settling participant needs the maker's cETH **disclosed** to it. Use explicit disclosure (attach the disclosed contract to the `Settle` command) or host both parties on one participant for the demo. |
