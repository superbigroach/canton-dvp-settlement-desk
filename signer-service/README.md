# CrossDesk reference signer

The small program an institution runs on its own infrastructure to hold a seat on a
CrossDesk fixing committee without a human clicking every day.

It implements `docs/SIGNER_PROTOCOL.md` §4 — *automating a seat*. Each seat asserts facts only
it can see (issuer: the wrapper can be redeemed; lender: the mark is safe to lend against;
venue: the mark sits where the asset traded). This service reads those facts from **your**
systems, and for every open proposal on an instrument you sign:

- if **every** condition of your seat passes, it **confirms** with the named checks and the
  numeric evidence each one was decided from;
- if **any** condition fails, it **refuses**, naming the condition and a reason built from the
  numbers (`attestor quorum 5 of 7 required - below threshold`);
- if a condition **cannot be evaluated** (your source is down, a field is missing), it **halts**:
  nothing is sent, nothing is recorded, and it retries on the next poll. A halt is never a
  confirm, and never a refusal either — a refusal is a statement about numbers you actually have.

It never widens a tolerance to make a check pass, never confirms a condition without evidence,
and never acts twice on the same proposal.

## What it does not do

- **It never holds a Canton key.** The confirm is an HTTPS call to CrossDesk with a scoped API
  key; CrossDesk exercises the choice as your party. In production your party lives on your own
  participant and the signing happens there — this service's key can *only* confirm or refuse
  fixings for the instruments your seat covers. It cannot move assets, cannot propose, cannot
  act as another seat.
- It does not form a view about the price. It checks facts against declared tolerances.
- It does not decide what a fund does when `K` is not reached (§5 of the protocol).

## The 5-minute run

You need a config file and one credential. To try it **today** against the hosted sandbox with
no account, use the sandbox header instead of an API key:

```sh
# 1. build (Node 20+)
npm ci && npm run build

# 2. check the config against CrossDesk without acting
CROSSDESK_SANDBOX_USER=venue@sandbox.crossdesk node dist/index.js --config examples/venue.yml --check

# 3. run: polls every 20s, serves POST /webhook and GET /health on :8787
CROSSDESK_SANDBOX_USER=venue@sandbox.crossdesk node dist/index.js --config examples/venue.yml
```

Or with Docker:

```sh
docker build -t crossdesk/signer .
cp examples/venue.yml signer.yml           # edit it
docker run --rm -p 8787:8787 \
  -v ./signer.yml:/app/signer.yml:ro \
  -v signer-data:/app/data \
  -e CROSSDESK_API_KEY=ck_... \
  crossdesk/signer
```

Then open a proposal (on the sandbox, as the administrator:
`curl -X POST -H 'X-Sandbox-User: s.borjas@lucilla.ca' https://crossdesk-devnet-app.web.app/api/admin/strike/CBTC`)
and watch the log — one JSON line per decision:

```json
{"ts":"...","level":"info","event":"decision","proposalCid":"00…","instrument":"CBTC","seat":"venue","price":65000,
 "decision":"confirm","checks":["traded-range","spread-within-tolerance","sufficient-volume"],
 "evidence":{"traded-range":{"low":64935,"high":65078},"low":64935,"high":65078,
             "spread-within-tolerance":{"bid":64967.5,"ask":65032.5,"spreadBps":10},"sufficient-volume":{"volume":12.5}},
 "conditions":{"traded-range":{"pass":true,"values":{"low":64935,"high":65078,"prints":4,"proposed":65000},...}},
 "http":{"status":200,"body":{"confirmed":true,...}}}
```

`GET /api/proposals?status=all&mine=true` as your seat then shows `mine.action = "confirmed"`
with your evidence.

Other invocations: `--once` runs one poll pass and exits (cron); `--check` validates and exits.

## Two ways to hear about a proposal

1. **Webhook** — CrossDesk POSTs `{ type, instrument, proposalCid, price, conditions, deadline }`
   to the URL saved in your signer settings, signed `X-CrossDesk-Signature: sha256=HMAC-SHA256(secret, body)`.
   The service verifies the signature over the exact bytes with a constant-time compare, answers
   `202`, then fetches the proposal from the API and evaluates it. Unsigned or badly signed
   deliveries get `401` and are logged; if no secret is configured the endpoint answers `503`
   rather than trusting anything.
2. **Polling** — `GET /api/proposals?status=open&mine=true` every `intervalSeconds`. This is the
   fallback when your host has no public URL, which is the common case for a pilot. Both can be
   on at once; the state file makes them safe together.

Only `proposal.created` and `proposal.restruck` trigger an evaluation. `fixing.finalized` and
`fixing.missed` are logged.

## Config reference (`signer.yml`)

Any string may contain `${ENV_VAR}` or `${ENV_VAR:-default}`.

```yaml
crossdesk:
  baseUrl: https://crossdesk-devnet-app.web.app
  apiKey: ${CROSSDESK_API_KEY}              # production: from POST /api/signer/apikey (shown once)
  sandboxUser: ${CROSSDESK_SANDBOX_USER}    # sandbox only: X-Sandbox-User header (venue@sandbox.crossdesk ...)
  webhookSecret: ${CROSSDESK_WEBHOOK_SECRET}
  poll: { enabled: true, intervalSeconds: 30 }
  timeoutMs: 15000

server: { port: 8787, host: 0.0.0.0, webhookPath: /webhook }
state:  { file: ./signer-state.json }       # idempotency record; mount a volume in Docker

seat: venue                                  # issuer | lender | venue
instruments: [CBTC, cETH]

tolerances:                                  # the declared tolerances - never widened at runtime
  # issuer
  reservesMaxAgeHours: 24
  maxQueueDepth: 0
  # lender
  markToleranceBps: 25
  liquidationToleranceBps: 100
  bookAcceptanceMaxAgeMinutes: 60
  # venue
  maxSpreadBps: 50
  minVolume: 0

conditions:
  <condition-name>:
    <evidence-field>: <source>
```

If the API key and the sandbox header are both set, the key is used. Inside a YAML flow map
(`{ ... }`) quote substitutions: `state: { file: "${SIGNER_STATE}" }`.

Every number sent as evidence is rounded to 10 decimal places first: the ledger stores it as
Daml `Numeric 10` and rejects a double's trailing noise (`77385.70654600002`) outright.

### Sources

A source is where one evidence field comes from. Three kinds:

| kind | fields | notes |
|---|---|---|
| `static` | `value` | A bare scalar or list is shorthand: `quorumThreshold: 7` |
| `http` | `url`, `method` (GET), `bearer`, `headers`, `pointer`, `parse`, `timeoutMs` | GETs JSON; `pointer` is an RFC 6901 JSON pointer into it (`/data/quorum/online`) |
| `command` | `command`, `pointer`, `parse`, `timeoutMs` | Runs a shell command; stdout is parsed as JSON (`parse: text` for a bare number/string) |

`url`, `headers` and `command` may reference `{instrument}`, `{price}`, `{cid}` and `{seat}`.
The values are validated before substitution (instrument and cid must be plain identifiers,
price must be a finite number) so a proposal cannot inject into your command line.

Two fields that name the same URL or command share one fetch per evaluation, so a venue can read
`bid` and `ask` from one book snapshot.

### The conditions and their evidence, per seat

The condition names are the protocol's (`GET /api/signer-protocol`). The fields are what this
service needs to decide each one, and what it sends as evidence on a confirm.

**issuer** — asserts redemption integrity

| condition | fields | passes when |
|---|---|---|
| `attestor-quorum` | `quorumSigners`, `quorumThreshold` | signers ≥ threshold |
| `reserves-current` | `reservesAsOf` (ISO 8601 or epoch) | age ≤ `reservesMaxAgeHours` |
| `reserves-cover-supply` | `reserves`, `supply` | reserves ≥ supply |
| `redemption-queue-clear` | `queueDepth`, `maxQueueDepth` (optional; else tolerance) | depth ≤ max |

**lender** — asserts the mark is safe to lend against

| condition | fields | passes when |
|---|---|---|
| `independent-mark-within-tolerance` | `independentMark` | \|price − mark\| / mark ≤ `markToleranceBps`; sends `independentMark`, `deviationBps` |
| `liquidations-consistent` | `liquidationsToday`, `worstDeviationBps` | no liquidations, or worst deviation ≤ `liquidationToleranceBps` |
| `book-acceptance` | `acceptedAt` (timestamp, or `false`) | your book stamped acceptance within `bookAcceptanceMaxAgeMinutes`. Point this at the system that actually marks your collateral — it is the signature that carries the weight |

**venue** — asserts the mark sits where the asset traded

| condition | fields | passes when |
|---|---|---|
| `traded-range` | `prints` (list of numbers or `{price}` objects) **or** `low` + `high` | low ≤ price ≤ high. The service computes low/high from the prints; the ledger refuses a range that does not contain the price |
| `spread-within-tolerance` | `spreadBps` **or** `bid` + `ask` | spread (bp of mid) ≤ `maxSpreadBps` |
| `sufficient-volume` | `volume` | volume ≥ `minVolume` and > 0 |

The confirm body is `{ checks: [names], evidence: { "<condition>": { field: value }, ... } }` —
one block per checked condition, which CrossDesk verifies server-side for the issuer and lender
seats against the tolerances in your signer settings. For the venue, `low`/`high` are also
present at the top level of `evidence`: that is the range the ledger enforces. If CrossDesk's
protocol endpoint declares evidence fields for a condition and no source provides them, the
service halts on that condition rather than confirm without them.

### Pointing a source at your own system

The examples use static values and small `node -e` commands so they run against the sandbox
with nothing else installed. Replace them:

```yaml
conditions:
  attestor-quorum:
    quorumSigners:   { kind: http, url: https://attest.internal/status, bearer: ${ATTEST_TOKEN}, pointer: /online }
    quorumThreshold: { kind: http, url: https://attest.internal/status, bearer: ${ATTEST_TOKEN}, pointer: /threshold }
  reserves-current:
    reservesAsOf:    { kind: command, command: "psql -At -c \"select max(attested_at) from por\"", parse: text }
```

```yaml
  traded-range:
    prints: { kind: http, url: "https://book.internal/trades?symbol={instrument}&window=strike", bearer: ${BOOK_TOKEN}, pointer: /trades }
  book-acceptance:
    acceptedAt: { kind: command, command: "riskctl accept-mark {instrument} {price} --json", pointer: /acceptedAt }
```

A checker that derives its evidence *from the proposal* (as the sandbox venue example does, to
have something to run) is a rubber stamp. The point of the seat is that the evidence comes from
somewhere CrossDesk cannot see.

## Operations

- `GET /health` → `200 {status, seat, instruments, authMode, webhook:{received, rejected}, poll:{lastAt, lastOk, lastError}, acted}`; `503` when the last poll failed.
- Logs: one JSON object per line on stdout. Events: `preflight`, `listening`, `poll`,
  `webhook.received` / `webhook.rejected`, `decision` (confirm | refuse | halt | error), `proposal.skip`.
- State: `state.file` — `{ acted: { <rootCid>: { cid, instrument, decision, at, httpStatus } } }`.
  Keyed by the proposal's root cid, so a proposal whose cid changed because another member
  signed is still recognised. A `5xx` or network failure is **not** recorded (retry next poll);
  a `4xx` is (the request was wrong; fix and clear the entry by hand if you want a retry).
- On start it fetches `/api/signer-protocol` and `/api/me`, and refuses to run if the credential's
  seat differs from the config, a protocol condition has no configured source, or the config names
  a condition that is not this seat's.

## Security notes

- **Secrets** come from the environment (`CROSSDESK_API_KEY`, `CROSSDESK_WEBHOOK_SECRET`, your own
  `${...}` tokens). Never write them into `signer.yml` you commit. The state file is written `0600`.
  Query-string parameters that look like keys are redacted from error messages.
- **Least privilege.** The CrossDesk API key is scoped to one user with one seat and its
  instruments; the only mutations it can make are `confirm` and `refuse`. Rotate it with
  `DELETE /api/signer/apikey` + `POST /api/signer/apikey`. Run the container as the unprivileged
  `node` user it ships with; give it outbound HTTPS to CrossDesk and your sources, nothing else.
- **The key only signs.** It cannot create, redeem, propose, or touch any other seat. On the
  sandbox, `X-Sandbox-User` is a stand-in with the same scope and no secret at all.
- **Webhook.** Signature verified over raw bytes with `crypto.timingSafeEqual`; the body limit is
  256 KB; an unconfigured secret makes the endpoint refuse rather than accept.
- **Commands.** `command` sources run through the shell with only validated substitutions. Prefer
  a small wrapper script you control over an inline command.
- **Tolerances** are configuration, read once at start. Changing them is a deliberate, versioned
  act — the protocol forbids a checker from widening them on its own.

## Development

```sh
npm ci
npm test         # tsc + node:test (rules per seat, handler decisions, idempotency, HMAC, config)
npm run build && npm start -- --config examples/lender.yml --once
```

Layout: `src/config.ts` (YAML + env), `src/sources.ts` (static/http/command), `src/evaluate.ts`
(the seat rules — §2 of the protocol as code), `src/handler.ts` (decide, act, record),
`src/client.ts` (the CrossDesk API), `src/webhook.ts` (HMAC), `src/server.ts` (express),
`src/state.ts` (idempotency), `src/index.ts` (entry).
