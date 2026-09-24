# ETP Foundry reference signer

The small program an institution runs on its own infrastructure to hold a seat on an
ETP Foundry (CrossDesk) fixing committee without a human clicking every day.

It implements `docs/SIGNER_PROTOCOL.md` §4 — *automating a seat* — for **protocol v2**
(22 Sep 2026). Each seat asserts facts only it can see; this service reads those facts from
**your** systems, and for every open proposal on an instrument you sign:

- if **every** condition of your seat passes, it **confirms** with the named checks and the
  numeric evidence each one was decided from;
- if **any** condition fails on real numbers, it **refuses**, naming the condition and a reason
  built from the numbers (`attestor quorum 5 of 7 required - below threshold`);
- if **any** condition **cannot be evaluated** (your source is down, a field is missing or
  unreadable, the protocol lookup failed), it **halts**: nothing is sent, nothing is recorded,
  and it retries on the next poll. A halt is never a confirm, and never a refusal either — a
  refusal is a statement about numbers you actually have. A halt on one condition stops the
  whole proposal even if another condition failed outright.

It never widens a tolerance to make a check pass (tolerances come from your config and from
nowhere else), never confirms a condition without evidence, and never acts twice on the same
proposal.

## The five seats

| seat | asserts | conditions (`checks`) | evidence fields sent |
|---|---|---|---|
| `issuer` | the wrapper can be redeemed right now | depends on the instrument's **reserve model** — see below | `quorumSigners, quorumThreshold` · `reservesAsOf` · `reserves, supply` · `queueDepth, maxQueueDepth` |
| `lender` | the mark is safe to lend against | `independent-mark-within-tolerance` · `liquidations-consistent` · `book-acceptance` | `independentMark, deviationBps` · `liquidationsToday, worstDeviationBps` · `acceptedAt` |
| `venue` | the mark sits where the asset traded — or nothing traded, and here is where the book stood | `traded-range` + `spread-within-tolerance` + `sufficient-volume`, **or** `no-prints-attested` alone | `low, high` (also top-level; enforced on-ledger) · `bid, ask, spreadBps` · `volume` — or `bestBid, bestAsk` |
| `custodian` | what is actually in the account | `holdings-current` · `holdings-cover-supply` · `no-encumbrance` | `statementAsOf` · `holdings, supply` · `encumbered` |
| `transfer-agent` | the share register the NAV is divided by | `shares-outstanding-reconciled` · `fees-accrued` | `registerShares, ledgerShares` · `accruedFees` |

The field names are the backend's (`SignerEvidence.java`). CrossDesk re-applies each rule
server-side to the numbers you send before it submits anything on-ledger; a bare tick is refused
for every seat except the venue, and the venue's range is checked by the ledger itself.

### The reserve model lookup (issuer)

In v2 each instrument declares how its backing is proven, and that decides which issuer
conditions exist for it:

| reserve model | example | issuer conditions |
|---|---|---|
| `attested` | cBTC (attestor multisig + proof-of-reserve) | `attestor-quorum` · `reserves-current` · `reserves-cover-supply` · `redemption-queue-clear` |
| `onchain-verifiable` | cETH (protocol-controlled lock) | `reserves-current` · `reserves-cover-supply` · `redemption-queue-clear` |
| `custodial` | a tokenised equity | `reserves-current` · `reserves-cover-supply` · `redemption-queue-clear` |

For every proposal the service calls **`GET /api/signer-protocol?instrument=<id>`** (cached
`crossdesk.protocolCacheSeconds`, default 300) and evaluates **exactly** the conditions your
seat has under that instrument's model. The proposal row's own `conditions` list is not used —
it still advertises the strict profile. An issuer config that covers both cBTC and cETH lists
the union of the sources; the `attestor-quorum` source is simply never resolved for a cETH
proposal. The wire carries no model name; the service infers it from the issuer profile it was
served and shows it in the `preflight` log and on `/health` (`reserveModels`).

The lender, venue, custodian and transfer-agent conditions do not vary with the model, but the
lookup is made for every seat so a protocol change on CrossDesk's side is picked up without a
restart.

### The venue's two claims (`no-prints-attested`)

On thin markets an empty window is the normal case. In v1 a venue with no prints could only
halt, so no fixing was struck on exactly the markets the product exists for. In v2 the
**tape decides** which claim the venue makes:

- prints in the window → `checks: [traded-range, spread-within-tolerance, sufficient-volume]`,
  `evidence.low/high` hoisted to the top level (the range the ledger enforces);
- an **explicitly empty** tape (`prints: []`) → `checks: [no-prints-attested]` with
  `{ bestBid, bestAsk }` and **no range** — the backend refuses a range next to that claim,
  and refuses `traded-range` and `no-prints-attested` together. `0` on a side means no quote
  there; when both sides are quoted the proposal must sit inside them, else the venue refuses.

Never both. A `null` or missing tape is a **source problem and halts**: an absence of trades
and an absence of data are different facts. Set `venue.attestNoPrints: false` to restore the
v1 behaviour (an empty window halts the seat) if your compliance view is that silence is
safer than an attested absence.

### The halt rules

The service halts — sends nothing, records nothing, retries next poll — when:

- `GET /api/signer-protocol?instrument=` fails or returns something that is not a protocol;
- the protocol names a condition this build has no rule for (upgrade the signer);
- a condition has no configured source, or its sources do not cover a field set the rule needs;
- a source fails (HTTP error, command non-zero, non-JSON output, pointer miss);
- a field resolves to something the rule cannot read (`'yesterday'` for a timestamp, a
  negative count, a crossed quote, a zero quorum threshold, an inverted range);
- the lender's book returns a **null** acceptance stamp (`false` is a real "no" and refuses;
  `null` is no answer);
- the venue's tape is empty and `no-prints-attested` is disabled or not in the protocol;
- CrossDesk's protocol declares an evidence field that neither the rule nor a source produces.

A halt is logged at `warn` with every condition's outcome so the operator sees what to fix. A
refusal is only ever sent when every condition was evaluable and at least one failed on its
numbers.

## What it does not do

- **It never holds a Canton key.** The confirm is an HTTPS call to CrossDesk with a scoped API
  key; CrossDesk exercises the choice as your party. This service's key can *only* confirm or
  refuse fixings for the instruments your seat covers. It cannot move assets, cannot propose,
  cannot act as another seat.
- It does not form a view about the price. It checks facts against declared tolerances.
- It does not decide what a fund does when `K` is not reached (§5 of the protocol).

## 5-minute quickstart, per seat

Every seat is the same three steps: **an API key from the portal → a config → run.**

**Step 1 — the credential.** Sign in to the signer portal with the account the administrator
issued for your seat, open *Settings → API key* (`POST /api/signer/apikey`), and copy the
`ck_…` key. It is shown once and stored hashed. Put it in the environment, never in a file
you commit:

```sh
export CROSSDESK_API_KEY=ck_...
export CROSSDESK_WEBHOOK_SECRET=...      # only if you save a webhook URL in the portal
```

To try the protocol first with no account, a backend running in sandbox auth mode
(`AUTH_MODE=sandbox`, e.g. a local `backend/` run) accepts a header instead:
`export CROSSDESK_SANDBOX_USER=<seat>@sandbox.crossdesk` (the sandbox roster seats an issuer,
a lender and a venue; there is no sandbox custodian or transfer agent). The hosted host at
`crossdesk-devnet-app.web.app` runs `AUTH_MODE=firebase` since 22 Sep 2026 and answers the
header with `401` — against it you need the `ck_` key. `GET /api/signer-protocol` is public
on both, so `curl 'https://crossdesk-devnet-app.web.app/api/signer-protocol?instrument=cETH'`
shows you the exact conditions your seat will be held to before you configure anything.

**Step 2 — the config.** Copy the example for your seat and point each source at your own
system. Every example runs as-is against the sandbox with static or synthesised values; those
are placeholders, and a checker whose evidence comes from the proposal is a rubber stamp.

| seat | start from | replace |
|---|---|---|
| issuer, attested asset (cBTC) | `examples/issuer-attested.yml` | attestor status endpoint, proof-of-reserve timestamp, reserves and supply, redemption queue |
| issuer, on-chain asset (cETH) | `examples/issuer-onchain.yml` | your indexer's last read of the lock, locked amount, minted supply, redemption queue |
| lender | `examples/lender.yml` | your risk system's mark, session liquidations, and the command that stamps book acceptance |
| venue | `examples/venue.yml` | your trade tape for the window (`[]` when nothing traded), best bid/ask, volume |
| custodian | `examples/custodian.yml` | your custody statement (as-of, balance, encumbered) and the issued supply |
| transfer-agent | `examples/transfer-agent.yml` | your register's share count, your own ledger read, accrued fees |

**Step 3 — check, then run.**

```sh
npm ci && npm run build                                       # Node 20+
node dist/index.js --config signer.yml --check                # reaches CrossDesk, validates per instrument, exits
node dist/index.js --config signer.yml                        # polls every 20s; POST /webhook and GET /health on :8787
```

`--check` fetches `/api/signer-protocol` (and `?instrument=` for each configured instrument)
and `/api/me`, and refuses to start if the credential's seat differs from the config, a
condition the protocol lists for any of your instruments has no satisfied source, or the config
names a condition none of your instruments has. The `preflight` log line shows the inferred
reserve model and the applied conditions per instrument.

Or with Docker:

```sh
docker build -t crossdesk/signer .
cp examples/custodian.yml signer.yml           # edit it
docker run --rm -p 8787:8787 \
  -v ./signer.yml:/app/signer.yml:ro \
  -v signer-data:/app/data \
  -e CROSSDESK_API_KEY=ck_... \
  crossdesk/signer
```

Then wait for a proposal (on the sandbox, as the administrator:
`curl -X POST -H 'X-Sandbox-User: s.borjas@lucilla.ca' https://crossdesk-devnet-app.web.app/api/admin/strike/CBTC`)
and watch the log — one JSON line per decision:

```json
{"ts":"...","level":"info","event":"decision","proposalCid":"00…","instrument":"CBTC","seat":"venue","price":65000,
 "model":"attested","venueMode":"traded-range",
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
  protocolCacheSeconds: 300                 # reuse of GET /api/signer-protocol?instrument= per instrument

server: { port: 8787, host: 0.0.0.0, webhookPath: /webhook }
state:  { file: ./signer-state.json }       # idempotency record; mount a volume in Docker

seat: venue                                  # issuer | lender | venue | custodian | transfer-agent
instruments: [CBTC, cETH]

venue:                                       # venue only
  attestNoPrints: true                       # false = an empty window halts instead of attesting

tolerances:                                  # the declared tolerances - config only, never from a source
  # issuer
  reservesMaxAgeHours: 24                    # the portal calls this freshnessHours
  maxQueueDepth: 0
  # lender
  markToleranceBps: 25                       # keep equal to tolerances.markBps in your portal settings
  liquidationToleranceBps: 100
  bookAcceptanceMaxAgeMinutes: 60
  # venue
  maxSpreadBps: 50
  minVolume: 0
  # custodian
  holdingsMaxAgeHours: 24

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
`bid` and `ask` from one book snapshot and a custodian can read `statementAsOf`, `holdings` and
`encumbered` from one statement.

A source may **not** set a tolerance. `maxQueueDepth` under `conditions:` is refused at load;
it lives under `tolerances:`.

### The conditions and their evidence, per seat

The condition names are the protocol's (`GET /api/signer-protocol`). The fields are what this
service needs to decide each one, and what it sends as evidence on a confirm.

**issuer** — asserts redemption integrity (which rows apply depends on the reserve model, above)

| condition | fields | passes when |
|---|---|---|
| `attestor-quorum` | `quorumSigners`, `quorumThreshold` | signers ≥ threshold (threshold must be > 0, else halt) |
| `reserves-current` | `reservesAsOf` (ISO 8601 or epoch) | age ≤ `reservesMaxAgeHours` |
| `reserves-cover-supply` | `reserves`, `supply` | reserves ≥ supply |
| `redemption-queue-clear` | `queueDepth` | depth ≤ `tolerances.maxQueueDepth`; both are sent |

**lender** — asserts the mark is safe to lend against

| condition | fields | passes when |
|---|---|---|
| `independent-mark-within-tolerance` | `independentMark` | \|price − mark\| / mark ≤ `markToleranceBps`; sends `independentMark`, `deviationBps` |
| `liquidations-consistent` | `liquidationsToday`, `worstDeviationBps` | no liquidations, or worst deviation ≤ `liquidationToleranceBps` |
| `book-acceptance` | `acceptedAt` (timestamp, or `false`) | your book stamped acceptance within `bookAcceptanceMaxAgeMinutes`. `false` refuses; `null` halts. Point this at the system that actually marks your collateral — it is the signature that carries the weight |

**venue** — asserts the mark sits where the asset traded, or that nothing traded

| condition | fields | passes when |
|---|---|---|
| `traded-range` | `prints` (list of numbers or `{price}` objects; `[]` = nothing traded) **or** `low` + `high` | low ≤ price ≤ high. The service computes low/high from the prints; the ledger refuses a range that does not contain the price |
| `spread-within-tolerance` | `spreadBps` **or** `bid` + `ask` | spread (bp of mid) ≤ `maxSpreadBps` |
| `sufficient-volume` | `volume` | volume ≥ `minVolume` and > 0 |
| `no-prints-attested` | `bestBid`, `bestAsk` (0 = no quote on that side) | only evaluated when the tape is empty; when both sides are quoted, bestBid ≤ price ≤ bestAsk |

**custodian** — asserts what is actually in the account

| condition | fields | passes when |
|---|---|---|
| `holdings-current` | `statementAsOf` | age ≤ `holdingsMaxAgeHours` |
| `holdings-cover-supply` | `holdings`, `supply` | holdings ≥ supply |
| `no-encumbrance` | `encumbered` | encumbered = 0 |

**transfer-agent** — asserts the share register

| condition | fields | passes when |
|---|---|---|
| `shares-outstanding-reconciled` | `registerShares`, `ledgerShares` | exactly equal |
| `fees-accrued` | `accruedFees` | ≥ 0 |

The confirm body is `{ checks: [names], evidence: { "<condition>": { field: value }, ... } }` —
one block per checked condition, which CrossDesk verifies server-side against the tolerances in
your signer settings. For the venue's `traded-range`, `low`/`high` are also present at the top
level of `evidence`: that is the range the ledger enforces. If CrossDesk's protocol endpoint
declares evidence fields for a condition and no source provides them, the service halts on
that condition rather than confirm without them.

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
  no-prints-attested:
    bestBid: { kind: http, url: "https://book.internal/book?symbol={instrument}", bearer: ${BOOK_TOKEN}, pointer: /bids/0/price }
    bestAsk: { kind: http, url: "https://book.internal/book?symbol={instrument}", bearer: ${BOOK_TOKEN}, pointer: /asks/0/price }
  book-acceptance:
    acceptedAt: { kind: command, command: "riskctl accept-mark {instrument} {price} --json", pointer: /acceptedAt }
  holdings-current:
    statementAsOf: { kind: http, url: "https://custody.internal/accounts/{instrument}/statement", bearer: ${CUSTODY_TOKEN}, pointer: /asOf }
```

A checker that derives its evidence *from the proposal* (as the sandbox venue example does, to
have something to run) is a rubber stamp. The point of the seat is that the evidence comes from
somewhere CrossDesk cannot see.

## Operations

- `GET /health` → `200 {status, seat, instruments, authMode, protocolVersion, reserveModels,
  webhook:{received, rejected}, poll:{lastAt, lastOk, lastError}, acted}`; `503` when the last
  poll did not yield a proposal list — a 5xx, a timeout, **or a 2xx whose body was not JSON**
  (a login page, a proxy error). Such a reply is never iterated as data.
- Logs: one JSON object per line on stdout. Events: `preflight`, `listening`, `poll`,
  `webhook.received` / `webhook.rejected`, `decision` (confirm | refuse | halt | error),
  `proposal.skip`. Every line is redacted before it is written (see Security).
- State: `state.file` — `{ acted: { <rootCid>: { cid, instrument, decision, at, httpStatus } } }`.
  Keyed by the proposal's root cid, so a proposal whose cid changed because another member
  signed is still recognised. A `5xx`, a network failure or a non-JSON `2xx` is **not** recorded
  (retry next poll; `mine.action` tells the service if the confirm actually landed);
  a `4xx` is (the request was wrong; fix and clear the entry by hand if you want a retry).
- On start it fetches `/api/signer-protocol`, `?instrument=` for each configured instrument,
  and `/api/me`, and refuses to run if the credential's seat differs from the config, a
  protocol condition for any instrument has no configured source, or the config names a
  condition none of your instruments has.

## Security notes

- **Secrets** come from the environment (`CROSSDESK_API_KEY`, `CROSSDESK_WEBHOOK_SECRET`, your own
  `${...}` tokens). Never write them into `signer.yml` you commit. The state file is written `0600`.
- **Nothing that could carry a credential is logged.** A failed `command` source is reported by
  its config path (`conditions.attestor-quorum.quorumSigners`) plus exit code and a redacted
  slice of stderr — never the substituted command line. Every log line is additionally passed
  through a redactor that masks the configured API key, webhook secret and every source
  `bearer` / header value, plus anything shaped like `token=…`, `Authorization: Bearer …`,
  `password: …` or `scheme://user:pass@host`. Query-string parameters that look like keys are
  redacted from error messages.
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
- **Tolerances** are configuration, read once at start, and a data source cannot supply one.
  Changing them is a deliberate, versioned act — the protocol forbids a checker from widening
  them on its own.

## Development

```sh
npm ci
npm test         # tsc + node:test (rules per seat, handler decisions per seat, no-prints, halts, non-JSON, redaction, HMAC, config)
npm run build && npm start -- --config examples/lender.yml --once
```

Layout: `src/config.ts` (YAML + env, seats, tolerance-only fields), `src/sources.ts`
(static/http/command), `src/evaluate.ts` (the seat rules — §2 of the protocol as code),
`src/protocol.ts` (per-instrument protocol lookup and reserve-model inference),
`src/handler.ts` (decide, act, record; the venue's two claims), `src/client.ts` (the CrossDesk
API; non-JSON 2xx is a failure), `src/log.ts` (JSON lines with redaction), `src/webhook.ts`
(HMAC), `src/server.ts` (express), `src/state.ts` (idempotency), `src/index.ts` (entry,
preflight per instrument).
