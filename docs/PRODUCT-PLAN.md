# CrossDesk — the production product

Written 2 Sep 2026, the afternoon after Canton Builders Office Hours. The showcase went well; the
standing blocker is a DevNet participant. This plan builds the product **as if the node arrives
tomorrow**: everything that does not need the node is built now against the hosted sandbox, and the
node is a config change.

Reference for shape and tone: **cfbenchmarks.com** — a benchmark administrator's site. Nav there is
*About · Products · ETFs · Oracle · Screener · Regulatory · News*. Each index has a product page with
the live value, last-updated time, publish frequency, a plain-English "About", the products that
reference it, "Usage & Licensing — contact", and methodology downloads. Trust signals: regulated
status, audit report, constituent criteria. **We copy the structure, not the claims.** CrossDesk is
not a regulated administrator and says so on its own Regulatory page.

## 1. What CrossDesk is

Two roles. Neither is an exchange.

1. **Benchmark administrator** for tokenised assets on Canton that no administrator covers: proposes,
   schedules, attests (K-of-N committee), publishes. Products: `CBTC Close`, `cETH Close`, and fund
   NAVs such as `LX1`.
2. **Transfer agent** for funds that reference those fixings: creation and redemption in kind, at the
   attested NAV, atomically, for authorised participants (APs).

The sealed closing auction stays as a **module** (for assets with a real market on Canton) and is
not on the pilot surface.

## 2. Surfaces

| Surface | Path | Audience | Auth |
|---|---|---|---|
| Marketing site | `/` | public | none |
| Benchmark pages | `/benchmarks/`, `/benchmarks/{id}` | public, licensees | none |
| Methodology, Governance, Regulatory, Licensing, About, Legal | `/methodology` … | public | none |
| Showcase deck, pitch, participate | unchanged | public | none |
| **App** | `/desk/` | signed-in roles | Firebase Auth |
| · Signer portal | `/desk/sign` | issuer / lender / venue seats | role `signer` |
| · AP portal | `/desk/ap` | authorised participants | role `ap` |
| · Fund admin | `/desk/fund` | fund issuer / administrator | role `fund_admin` |
| · Admin console | `/desk/admin` | CrossDesk | role `admin` |
| · Operator desk (today's page) | `/desk/ops` | CrossDesk | role `admin` |
| · Auditor view | `/desk/audit` | auditor | role `auditor` |

## 3. Identity and roles

- **Firebase Authentication** on project `crossdesk-devnet-app` (email/password + Google). The
  frontend gets an ID token; the backend verifies it with `firebase-admin` (Java) in a servlet
  filter on every `/api/**` route except the public ones (§5 "public").
- **User → role + party mapping** in the backend: `users.yml` seed (email → role, party, seat,
  displayName, org) overridable by env, plus an admin endpoint to add/edit. Sandbox mapping:
  Issuer → signer/issuer, Bank → signer/lender, Venue → signer/venue, Alice & Bob → ap,
  Auditor → auditor, admin@crossdesk → admin. **In production the party is on the institution's own
  participant; the mapping is the same.**
- Roles: `admin`, `signer` (with `seat` ∈ issuer|lender|venue and a list of instruments),
  `ap`, `fund_admin`, `auditor`, `viewer`.
- Dev bypass: env `AUTH_MODE=sandbox` accepts header `X-Sandbox-User: <email>` so the operator
  desk and tests work without Firebase. **Default in the deployed image is `firebase`.**
- Automated signers use an **API key** (per user, generated in the signer portal, hashed at rest),
  sent as `Authorization: Bearer ck_…`.

## 4. The fixing lifecycle in production

```
schedule (16:00 London per instrument)
  → PROPOSE   operator computes: benchmark print × last factor (wrapped) / Σ units × marks (fund)
              → on-ledger proposal; every seat is an observer
  → NOTIFY    per seat: webhook POST (signed), email, in-app
  → ATTEST    seat's signer service or portal user: Confirm-with-checks / Refuse-with-reason
              (venue attaches traded range; ledger enforces)
  → RESTRIKE  on refusal, inside the window (default 30 min)
  → FINALIZE  at K; funds re-mark; series row published with tier 1
  → ESCALATE  tier 2, inside the window: at ½ remind every seat not yet confirmed
              (proposal.reminder, escalation 1); at ¾ remind again and bring in the
              seat's configured alternates (escalation 2)
  → FALLBACK  if K not reached by window end: tier 3 benchmark × last factor (auto),
              tier 4 prior fixing flagged, tier 5 missed
```

A strike day is decided by the instrument's **calendar** (`daily` for wrapped crypto — the
reference rate prints every day of the year; `nyse` / `lse` for exchange-listed assets, with the
exchanges' published holidays; `weekdays`), and a fund strikes only on days all its components do.

Every step writes an **event** (`fixing_events`: instrument, proposalCid, kind, actor, reason,
ts, on-ledger cid where applicable). The audit export is those events.

## 5. Backend API contract (new; additive; all JSON)

**Public (no auth)**
- `GET /api/benchmarks` → `[ { id, name, kind, publishTime, timezone, description, last: { price, asOf, tier, k, n, signers[], ageSeconds }, referencing: [ { id, name } ] } ]`
- `GET /api/benchmarks/{id}` → same shape, single
- `GET /api/series/{id}?from&to&limit` → `[ { date, asOf, price, referencePrice?, wrapperFactor?, tier, k, n, signers[], fixingCid, restated: bool } ]` newest first
- `GET /api/series/{id}.csv` → CSV of the above
- `GET /api/methodology` → `{ version, url, signerProtocolVersion }`
- existing: `/api/diag`, `/api/health`, `/api/signer-protocol`, `/api/fixing-schedule`

**Authenticated**
- `GET /api/me` → `{ uid, email, role, party, seat?, instruments?, org, displayName }`
- `GET /api/proposals?status=open|all&mine=true` (signer) → proposals for my instruments with my conditions and what I already did
- `POST /api/proposals/{cid}/confirm` body `{ checks: [condition], evidence }` (wraps `/fixing/{cid}/confirm-checked` with the caller's party/seat). Venue: `evidence: { low, high }`, enforced on-ledger. Issuer and lender: `evidence: { "<condition>": { …numbers } }` is **required**, in the shape `/api/signer-protocol` publishes per condition, checked server-side before submission; a bare tick is a 422 carrying the schema
- `POST /api/proposals/{cid}/refuse` body `{ condition, reason }` → recorded on-ledger where the template supports it, always in events
- `GET /api/proposals/{cid}/events` → the message log for one proposal
- `GET /api/signer/settings` / `PUT` → `{ webhookUrl, webhookSecret?, email, tolerances: {…} }`
- `POST /api/signer/apikey` → `{ key }` (shown once); `DELETE /api/signer/apikey`
- `GET /api/ap/funds` → funds the caller is an AP for, with last official NAV, indicative, units per share, fee schedule, cutoffs
- `POST /api/ap/create` / `POST /api/ap/redeem` body `{ fundId, shares }` → wraps basket create/redeem as the caller's party; returns receipt
- `GET /api/ap/receipts` → caller's receipts
- `GET /api/fund/{id}/dashboard` (fund_admin) → NAV series, shares outstanding, create/redeem log, fee accruals, licensees
- `GET /api/admin/schedule` / `PUT` → per-instrument strike time, window, `calendar` (daily | weekdays | nyse | lse), tiers enabled, `alternates: { issuer: [emails], lender: [...], venue: [...] }` for tier 2
- `POST /api/admin/strike/{id}` → run the propose step now
- `GET /api/admin/committees` → roster per instrument (seats, parties, users, last action)
- `GET /api/admin/users` / `POST` / `PUT /{uid}` → mapping management
- `GET /api/admin/events?instrument&from&to` and `.csv` → audit export
- `GET /api/audit/…` mirrors of the read-only admin routes for role `auditor`

**Webhook to signers** (outbound): `POST {webhookUrl}` body `{ type: "proposal.created"|"proposal.restruck"|"proposal.reminder"|"fixing.finalized"|"fixing.missed", instrument, proposalCid, price, referencePrice?, wrapperFactor?, conditions: [names for your seat], deadline, escalation?: 1|2 }` with header `X-CrossDesk-Signature: sha256=HMAC(secret, body)`. `proposal.reminder` goes only to the seats that have not confirmed (and, at `escalation: 2`, their alternates).

**Scheduler**: Spring `@Scheduled`, reads `/api/admin/schedule`; default CBTC & cETH Close 16:00 Europe/London, LX1 NAV immediately after its components. Safe on the sandbox (in-memory; missed = event).

## 6. Frontend app

- Add `react-router-dom`, `firebase` (auth only). Login page at `/desk/login`. Role-based layout:
  left nav with only the sections the role has. The current one-page desk moves to `/desk/ops`.
- **Signer portal**: Open proposals (card per proposal: instrument, proposed price, benchmark × factor
  or NAV, deadline, my seat's conditions as a checklist with an evidence input where required, the
  venue's low/high; buttons Confirm / Refuse-with-reason), History (my signatures, refusals, with
  on-ledger cids), Settings (webhook URL + secret, email, tolerances, API key). Message log per
  proposal (events, newest first).
- **AP portal**: Funds (table), Fund detail (official NAV, indicative, components, units I deliver /
  receive for N shares, fee, cutoff), Create / Redeem with confirmation, Receipts.
- **Fund admin**: NAV series chart + table, shares outstanding, creation/redemption log, fees.
- **Admin console**: Schedule editor, Strike now, Committees roster, Users & roles, Events with
  CSV export, Fallback status per instrument.
- **Auditor**: read-only events + series.
- Keep the design system (dark, gold for official numbers, Inter/JetBrains Mono). Mobile-usable.

## 7. Marketing site (static, `frontend/public/`, built by the existing arrange-dist step)

Pages: `/` (hero + live benchmark tiles from `/api/benchmarks` + "who it's for" + how it works + trust
strip + CTA), `/benchmarks/` (index), `/benchmarks/{CBTC|cETH|LX1}` (product page: value, as-of, K-of-N,
signers, tier, series table from `/api/series`, methodology link, referencing products, usage &
licensing), `/methodology` (renders FIXING_METHODOLOGY.md + SIGNER_PROTOCOL.md sections), `/governance`
(committee model, seats, fallback waterfall, restatement), `/licensing` (the three licence types, who
pays, contact), `/regulatory` (**plain**: not a regulated administrator; IOSCO-principles alignment;
what recognition would require), `/news` (showcase, hackathon win), `/about`, `/legal/{terms,privacy,
disclaimer}`. Nav: Benchmarks · Methodology · Governance · Licensing · Regulatory · News · About ·
**Sign in** · **Open the desk**. Footer with the sandbox disclosure.

## 8. Claims discipline

Never: "regulated", "live on DevNet", "participant" for the sandbox, named parties as clients. Always:
sandbox disclosure in the footer; "attested by K of N" only when K real signatures exist; tier and age
shown on every published value.

## 9. Order of work

1. Backend: auth filter + roles + `/api/me`; public benchmarks/series; proposals wrapper; scheduler;
   events; webhooks + API keys; AP + fund + admin routes. Tests. Deploy.
2. App: router + auth + layouts; signer portal; AP portal; admin; fund admin; auditor. Ops desk kept.
3. Site: all pages above, consuming the public API. Deploy.
4. QA on the live sandbox end to end as each role; docs updated (`ARCHITECTURE.md`, `PRODUCTION_CHECKLIST.md`).
