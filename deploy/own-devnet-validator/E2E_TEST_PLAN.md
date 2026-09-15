# E2E test plan — real transactions on a live participant

These tests run against a live participant, not a mock or an in-memory sandbox. Every test
goes through the desk's REST API, which is the product path:
REST → `LedgerService` → gRPC Ledger API v2, carrying the desk's self-minted token. The
results are then checked against the participant's own JSON Ledger API.

```bash
CD_PROFILE=localnet   ./07-e2e-real-devnet.sh    # Track A (run 2026-09-15: 12/12 PASS)
CD_PROFILE=own-devnet ./07-e2e-real-devnet.sh    # Track B, same script, tunnel open
```

The script's exit code is the number of FAILs. A SKIP is not a failure.

## Notation

- `B` = `$CD_BACKEND_URL/api`. That is `http://localhost:8080/api` on LocalNet, and the
  Cloud Run URL for `crossdesk-devnet-api` on DevNet.
- `J` = the participant's JSON Ledger API. That is `http://localhost:3975` on LocalNet, and
  `http://127.0.0.1:17575` through the tunnel on DevNet.
- `W` = the validator wallet API: `$CD_NGINX` + `Host: wallet.localhost` + `/api/validator/v0/wallet/...`.
- Labels (`Alice`) resolve through the roster, `LEDGER_PARTIES`. `$ALICE` is Alice's full id.
- **Update id evidence.** Every committed step resolves the update id from the contract id
  it returned:
  - `POST J/v2/events/events-by-contract-id {contractId, eventFormat.filtersByParty}` → `created.createdEvent.offset`
  - `POST J/v2/updates/update-by-offset {offset, updateFormat.includeTransactions}` → `update.Transaction.value.updateId`

## Preconditions

1. `05-upload-dar-and-parties.sh` has run: the DAR is uploaded, the 7 `*-crossdesk` parties
   exist, and `crossdesk-backend` holds CanActAs and CanReadAs on those 7 plus the wallet
   party.
2. `06-wire-backend.sh` has run. `GET B/health` returns `{"auth":"hmac","applicationId":"crossdesk-backend"}`.
3. The base layer is seeded: `scripts/bootstrap-devnet.sh $B`, which creates the
   instruments and opening balances.

---

## T1 — party read

| | |
|---|---|
| Calls | `GET B/parties`; `GET J/v2/users/crossdesk-backend/rights` (admin token); `GET $CD_BACKEND_URL/api/diag` |
| Expect | ≥ 8 parties, each with a full `hint::1220…` id; ≥ 16 rights (CanActAs + CanReadAs × 8); `ledger.reachable = true` |
| Negative (manual) | Call `J` with no token → **401**. Call with a token for a user holding no rights → `PERMISSION_DENIED` |

## T2 — Canton Coin balance

| | |
|---|---|
| Calls | `GET W/balance` → `POST W/tap {"amount":"100.0","command_id":"<run>-tap"}` → `GET W/balance` |
| Expect | `effective_unlocked_qty` increases, and tap returns an Amulet `contract_id`, which resolves to an update id |
| DevNet | The same calls. The wallet tap is the DevNet faucet, and traffic is bought automatically |

## T3 — CIP-56 claim (BitSafe CBTC on DevNet; Amulet stand-in on LocalNet)

| | |
|---|---|
| LocalNet setup | `POST W/token-standard/transfers {"receiver_party_id":$ALICE,"amount":"25.0","description":"…","expires_at":<µs>,"tracking_id":"…"}`. This creates an Amulet `TransferInstruction` pending for Alice. **The asset is a stand-in: it is not CBTC** |
| DevNet setup | A human requests CBTC for `$ALICE` at `https://cbtc-faucet.bitsafe.finance`, 0.01–1 per request (manual; no API found) |
| Calls | `GET B/token-standard/pending?party=Alice` → `POST B/token-standard/accept {"party":"Alice","instructionCid":…}` |
| What the desk does | It reads `instrumentId.admin` and looks up the registry base in `REGISTRY_REMOTE_URLS`. It then calls `POST {base}/registry/transfer-instruction/v1/{cid}/choice-contexts/accept` and exercises `TransferInstruction_Accept` by interface, with `choiceContextData` and `disclosedContracts` |
| Verify | `POST J/v2/state/active-contracts`, filtered by `InterfaceFilter #splice-api-token-holding-v1:Splice.Api.Token.HoldingV1:Holding` for `$ALICE` |
| Expect | HTTP 200 from the accept call. Alice's HoldingV1 grows: LocalNet `DSO:Amulet`, DevNet `cbtc-network:CBTC` |
| Known traps | `docs/TOKEN_STANDARD_RUNBOOK.md` §2: filter by `#package-name` rather than the package id; an empty context gives `Missing context entry …transfer-rule`; `TextMap` must be `{}`; offers expire after 7 days |
| DevNet with no offer | **SKIP** (not FAIL). The script prints the faucet step |

## T4 — sealed auction round

| | |
|---|---|
| Calls | `POST B/moc/order {"trader":"Bob","side":"Sell","quantity":1,"instrumentId":"cETH","orderType":"Limit","limitPrice":2350}`; `POST B/moc/order {"trader":"Alice","side":"Buy",…,"limitPrice":2450}`; `GET B/moc/state?instrumentId=cETH&actingAs=Auditor|Alice|Venue`; `POST B/moc/{auctionCid}/close {}` |
| Expect (privacy) | **Auditor:** `orders=[]`, `othersResting ≥ 2`. **Alice:** only `alice-crossdesk` orders. **Venue:** ≥ 2 orders. The ledger enforces this, and the endpoint only reports it |
| Expect (print) | The close returns `closingPrice` (2400, the uniform price), ≥ 2 `fills`, and a `settlementBatchCid` with an update id. `GET B/receipts?party=Auditor` then shows the fill receipts (visibleTo: venue, trader, auditor) |

## T5 — K-of-N committee NAV

| | |
|---|---|
| Calls | `POST B/committee {"admin":"Bank","members":["Bank","Issuer","Venue"],"threshold":2}` → `POST B/committee/{cc}/propose {"proposer":"Bank","instrumentId":"CBTC","price":65000,…}` → **`POST B/fixing/{p}/finalize` (must fail)** → `POST B/fixing/{p}/confirm {"member":"Issuer"}` → `POST B/fixing/{p2}/finalize {"proposer":"Bank","publishTo":["Venue"]}` |
| Expect | The early finalize (1 of 2 signatures) returns **4xx** (409 observed). The final finalize returns a NavFixing `contractId`, `attestedPrice = 65000`, and `markUpdated`. Confirm is consuming, so `p2` ≠ `p` |

## T6 — in-kind create

| | |
|---|---|
| Calls | `POST B/basket {"administrator":"Bank","basketId":"E2E<hhmmss>","cashInstrument":"USDC","components":[{"instrumentId":"cETH","unitsPerShare":0.1},{"instrumentId":"CBTC","unitsPerShare":0.01}],"participants":["Alice","Bob"]}` → `POST B/basket/create {"basketId":…,"ap":"Alice","shares":5}` |
| Expect | `receiptCid` (with its update id) is returned. Alice's cETH drops by exactly 0.5, CBTC by 0.05, and she holds 5 shares. It is **one** transaction: if either leg is short, nothing moves (`daml test`: `testCreationAtomicRollback`) |

## T7 — in-kind redeem

| | |
|---|---|
| Call | `POST B/basket/redeem {"basketId":…,"ap":"Alice","shares":2}` |
| Expect | `receiptCid` is returned. cETH rises by 0.2, CBTC by 0.02, and shares go 5 → 3 |

## T8 — atomic DvP, including the must-fail cases

| | |
|---|---|
| T8a | `POST B/trade {"buyer":"Bob","seller":"Alice","assetInstrument":"cETH","assetAmount":1,"cashInstrument":"USDC","cashAmount":2400}` → `receiptCid`. Alice: cETH −1, USDC +2400. Bob: the mirror image |
| T8b | The same call with `assetAmount: 1000000` → **422** from the desk's pre-check. All four balances are unchanged. (Nothing reaches the ledger.) |
| T8c (ledger-level) | The REST version of `Test.daml` `testAtomicRollback`. `POST B/dvp/propose` uses **full party ids**, Bob's AAPL holding cid, Alice's USDC holding cid, and `cashAmount` = holding + 50, so the amount disagrees. Then `POST B/dvp/{p}/accept {"counterparty":$ALICE}` → `POST B/dvp/{a}/settle {"proposer":$BOB}` |
| T8c expect | Propose and accept commit (update ids printed). Settle returns **4xx** with `DAML_FAILURE` (the Daml `Settle` assertion). Alice and Bob keep identical USDC and AAPL balances: no half-settled state |
| Label trap | Sending `"proposer":"Bob"` to `/dvp/*` fails with `PERMISSION_DENIED`, and the participant log shows *"Claims do not authorize to act as party 'Bob'"*. These endpoints take full ids by design |

## T9 — cash conservation

| | |
|---|---|
| Calls | Before T1 and after T8: `GET B/holdings?party=<L>` for Issuer, Bank, Alice, Bob, Auditor, Venue and Agent. Dedupe by `contractId` and sum by `instrumentId` |
| Expect | USDC, cETH and CBTC totals are identical. Only new fund-share instruments (`E2E…`) appear. LocalNet run: USDC 2,702,550 / cETH 109 / CBTC 23 both before and after |

## T10 — UI smoke

| | |
|---|---|
| LocalNet (scripted) | `GET $CD_BACKEND_URL/` 200 and `GET B/benchmarks` 200 |
| DevNet (scripted) | `GET https://crossdesk-devnet-app.web.app/desk` 200, and `GET …/api/health` shows `"auth":"hmac"`. This is a SKIP until `SWITCH_SITE=1` |
| Manual, in a browser (both tracks) | Open the desk (LocalNet: `frontend` with `VITE_API_TARGET=http://localhost:8080 npx vite`; DevNet: `https://crossdesk-devnet-app.web.app/desk`), then work through the checklist below |

The manual browser checklist:

1. The party picker lists the 8 roster parties.
2. **Position** shows Alice's balances matching T6/T7/T8.
3. **Trade (Market-on-Close)** as Alice shows only her own order and a "N sealed" hint.
4. **Decentralised Operator** shows the E2E committee and the 65,000 CBTC fixing.
5. **Fund / ETF Builder** lists `E2E…`, and create/redeem of 1 share works.
6. **Pending transfers CIP-56** as Alice lists nothing after T3.
7. **Settlement Receipts** as Auditor shows the auction and trade receipts.
8. There are no 5xx responses in the network tab.

---

## Existing suites reused

| Suite | Command | What it covers | Result, 2026-09-15 |
|---|---|---|---|
| Daml Script (in-memory ledger) | `daml test` from the project root (SDK 3.4.11; run in WSL `~/crossdesk-build`) | 125 scripts: DvP, `testAtomicRollback`, MOC incl. `testDarkPoolPrivacy`, committee/threshold attestation, create/redeem incl. `testCreationAtomicRollback`, fees, token standard | **125 ok** |
| Backend unit tests | `cd backend && GRADLE_OPTS=-Xmx2g ./gradlew test --no-daemon` | 204 tests, including the new `LedgerAuthConfigTest` (mode resolution, HS256 signature, applicationId = sub, roster) | **204 passed** |
| Base-layer seed | `scripts/bootstrap-devnet.sh <B>` | instruments + opening balances over REST | ok on LocalNet |
| Fund demo seed | `scripts/seed-fund-demo.sh <B>` | committee-attested marks, MMF accruing fix, LX1 basket, continuous book | not run (the book panel is gated off) |
| Legacy smoke | `backend-devnet/smoke-devnet.sh` | one atomic DvP | superseded by T8 |
| Daml Script on a participant | `daml script --dar … --script-name Test:<t> --ledger-host … --access-token-file …` | — | **does not run unmodified.** The scripts allocate parties but never grant user rights, so the participant answers `PERMISSION_DENIED` (observed on LocalNet) |
