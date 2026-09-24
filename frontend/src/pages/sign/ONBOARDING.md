# The "Your seat" panel — what it shows and where each sentence comes from

`SeatGuide.tsx` (this folder) is the collapsible guide at the top of the signer's first
screen (`Proposals.tsx`). Its data module is `frontend/src/seatGuide.ts`. The public
companion page is `frontend/public/committee.html` ("Join a committee").

The panel exists so a seat can log in and understand, on one screen: what it asserts every
day, what evidence it supplies, how to automate it, and what trust level it signs at. It
is **not** a second copy of the protocol — anything the API carries is read from the API.

## Two sources, kept apart

| What the panel shows | Where it comes from | Can it drift? |
|---|---|---|
| Seat title, "only your seat knows", condition names, `passesWhen`, evidence field names/types/descriptions, the rule, who verifies it (`server` / `ledger` / `signer`), `requiresObservedRange` | `GET /api/signer-protocol?instrument=<id>` at render time (`fetchSeatProtocol`). The controller serves `SignerProtocol.rolesFor(reserveModelOf(instrument))`, so the issuer seat is shown as its reserve model can honestly assert it — three conditions for `onchain-verifiable` / `custodial`, four for `attested`. | No — same source the confirm route validates against. |
| Protocol version string | Same response, `version` (`SIGNER_PROTOCOL v2`). | No. |
| Which instrument's conditions are shown | `/api/me` → `instruments`; a tab per instrument when there is more than one. | No. |
| The seat key | `/api/me` → `seat`. | No. |
| Everything else (below) | Prose in `seatGuide.ts`, keyed by seat. | Yes — keep it in step with the documents cited. |

`fetchSeatProtocol` goes to the public route directly (with the desk's auth headers) because
`desk.signerProtocol()` in `frontend/src/desk/client.ts` takes no instrument parameter and
that file is owned by another workstream. On any failure it falls back to
`desk.signerProtocol()` so the panel still renders (mock mode included).

`api.ts` types `SignerCondition` without the `evidence` block the API sends
(`Dtos.SignerConditionView.evidence` → `SignerEvidence.schemaOf`). `seatGuide.ts` declares
`GuideCondition extends SignerCondition { evidence?: EvidenceSchema }` for that reason
rather than editing `api.ts`.

## Sentence-by-sentence provenance of the prose

Paths are relative to `C:\CrossDesk\canton-dvp-settlement-desk` unless they start with
`BUSINESS/`, which is `C:\CrossDesk\BUSINESS/`.

### "Every day you assert: …" (`SEAT_GUIDE[seat].assertion`)
- issuer / lender / venue: the §2a / §2b / §2c headings of `docs/SIGNER_PROTOCOL.md`
  ("redemption integrity", "the mark is safe to lend against", "the mark sits where the
  asset traded"), expanded with the `uniquelyKnows` phrasing from
  `backend/.../ledger/SignerProtocol.java`.
- custodian / transfer-agent: the `uniquelyKnows` strings and the §2e / §2f comments in
  `SignerProtocol.java` ("what is actually in the account", "the share register — how many
  shares the NAV is divided by"). **Condensed by me** into one sentence each.
- operator: `SignerProtocol.java` §2d comment and `docs/SIGNER_PROTOCOL.md` §2d.

### "TradFi analogue" (`analogue`)
`BUSINESS/1-LEARN/11-tradfi-benchmark-administrators-intel.md` §C, column "Closest
analogue", one row per seat, paraphrased. The closing sentence ("You act for your firm on
the fact you attest, not as an independent individual") is §C's framing paragraph and
`docs/SIGNER_PROTOCOL.md` §1 "What this is not" / §6.

### Conditions block
- "a tick alone is refused for every seat except the venue" — `docs/SIGNER_PROTOCOL.md`
  status box ("a bare tick is refused for every seat except the venue") and §7 row "v2 no
  bare ticks".
- "A missing block or a failing number comes back as a 422 naming the number" — §7 row
  "§2a / §2b issuer and lender evidence"; `SignerEvidence.Rejected`.
- Example values (`examples`) — **invented by me, illustrative only**, except the venue's
  `traded-range` / `spread-within-tolerance` / `sufficient-volume` numbers, which are the
  decision-log sample in `signer-service/README.md` ("The 5-minute run"). The panel says
  so on screen. `maxQueueDepth: 0` follows the README's tolerance default.
- "The point of the seat is that the evidence comes from somewhere ETP Foundry cannot see"
  — `signer-service/README.md`, "Pointing a source at your own system", last paragraph.

### "When a condition fails" (`refuse` + the fixed bullets)
- issuer: `docs/SIGNER_PROTOCOL.md` §2a ("If any fails … either declines, or the proposer
  restrikes with a factor below par and a rationale naming the failed condition").
- lender: §2b ("book-acceptance is the one that carries the weight …").
- venue: §2c range enforcement; `BUSINESS/5-RUN-THE-COMMITTEE/6-RULEBOOK.md` §5.3a
  ("attests the absence", "third act", "the band widens", "never claim both").
- custodian: `11-tradfi…intel.md` §C row (missed statement → `EXCEPTIONAL` then `NO FIXING`,
  chain-event §2.1; notify any lien or rehypothecation immediately).
- transfer-agent: §C row (breaks reported same day; unreconciled register → class-F
  `NO FIXING`, rulebook §5.8).
- "Refuse with reason … a refusal without a reason is not recorded" — the existing
  `ProposalCard.tsx` behaviour.
- "If fewer than K seats confirm, no fixing exists … a gap is published as a gap" — §5.
- "A halt is never a confirm, and never a refusal either" — `signer-service/README.md`
  intro; `BUSINESS/4-ONBOARD-CLIENT/3-ASSET-ISSUER-PACK.md` §4.

### "Automate this"
- Rationale paragraph — `docs/SIGNER_PROTOCOL.md` §4 (all three sentences, including
  "must never widen its own tolerances").
- Step 1 — `Settings.tsx` (key shown once, stored hashed) and §4a table ("API key, `ck_` +
  32 random bytes, SHA-256 hashed at rest, shown once"; scope limits).
- Steps 2–4 — `signer-service/README.md` "The 5-minute run" (`npm ci && npm run build`,
  Node 20+, Docker, `--check`, polls, `POST /webhook`, `GET /health` on :8787; the README
  says every 20 s for the sandbox example and `intervalSeconds: 30` in the config
  reference — the panel quotes the config reference).
- `quickstart` YAML — README "Config reference" skeleton plus the "Pointing a source at
  your own system" examples for issuer, lender and venue. **The custodian and
  transfer-agent skeletons are mine**: the README's `seat:` enum is `issuer | lender |
  venue` and it ships no rules for the two v2 seats, which the panel states in a warning
  banner (`checkerShipped: false`). The field names in those two skeletons are the API's
  (`SignerProtocol.java`); the URLs/commands are placeholders.
- Confirm body — README "The conditions and their evidence, per seat", last paragraph
  (`{ checks, evidence: { "<condition>": {…} } }`; venue `low`/`high` also at the top
  level). Built at render time from the API's condition list plus the example values.

### "Trust level"
- The three rows — `docs/SIGNER_PROTOCOL.md` §4a "Key custody — the trust ladder", the
  same table in `6-RULEBOOK.md` §6.7 and `3-ASSET-ISSUER-PACK.md` §5.
- "Requires" column — **derived by me** from §4a "How a seat is issued" (roster row: uid ·
  email · Canton party · seat · tolerances · API key hash; `AUTH_MODE=firebase`, e-mail-
  verified users map to a roster row) and the README ("It never holds a Canton key … in
  production your party lives on your own participant"). The literal trio in the task
  brief — e-mail on the roster / API key + reference checker / own participant party id —
  is what the column says.
- "You are at L1" — `DEFAULT_TRUST_LEVEL`. `/api/me` (`MeController.java`) returns
  `seat` and `instruments` but no trust level, and every seat issued so far is a hosted
  party (`3-ASSET-ISSUER-PACK.md` §5 "Pilots start at L1"). **The "how to move up"
  sentence is mine**, derived from the ladder rows; there is no self-service upgrade
  route in the API.
- "K=3 of N=5 — L1:2, L2:1", "honest for a pilot and dishonest as a destination", "target
  state: every signer at L2 or above" — §4a verbatim / near-verbatim.
- Administrator impersonation disclosure — §4a last paragraph; rulebook §6.7 last bullet.

## The proposal card collects the evidence (closed 24 Sep 2026)
`ProposalCard.tsx` used to send `{ checks }` with no evidence for every seat except the
venue, so under the v2 "no bare ticks" rule a manual confirm from an issuer, lender,
custodian or transfer-agent seat was a 422. It now:

- fetches the protocol for the proposal's instrument (`fetchSeatProtocol(p.instrument)`)
  and renders one input per evidence field of each ticked condition, typed by the
  field's `type` (`integer` → step 1, `number` → any, `instant` → ISO text with a "now"
  button), labelled with the field's `description`;
- sends exactly `{ checks: [names], evidence: { <condition>: { <field>: <value> } } }`
  (`ProposalController.ConfirmRequest`); numbers as JSON numbers, instants normalised to
  `toISOString()`;
- keeps the venue's range path (`evidence.low` / `evidence.high` at the top level, which
  is what `ProposalService.confirm` reads and the ledger checks; the nested `traded-range`
  block mirrors the reference checker) and adds a "no prints in the window" toggle that
  replaces the range inputs with `bestBid` / `bestAsk` and forces `checks` to
  `["no-prints-attested"]` — the two claims can never be sent together;
- disables Confirm while a field is missing or malformed and names it
  (`Missing: attestor-quorum.quorumSigners`); Refuse is unchanged;
- shows a 422's `message` verbatim (`Refused (422): …`) — `client.ts` already puts the
  server's `message`, which joins `problems`, into `ApiError.message`.

Backend note (read-only, not changed): on the portal route the venue branch reads only
`low`/`high` and never calls `SignerEvidence.verify`, so a portal `no-prints-attested`
confirm reaches the ledger without the server checking `bestBid ≤ proposal ≤ bestAsk`
(the `/fixing/{cid}/confirm-checked` desk route and the reference checker do). The card
still sends the block so the numbers are on the event.
