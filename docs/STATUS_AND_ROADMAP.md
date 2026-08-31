# Status, aim, and what happens next

Updated **13 August 2026**. This is the page to read first if you are deciding whether to use,
buy, fund, or work on CrossDesk.

---

## 1. What this repository is for

**CrossDesk is a commercial product, published so it can be read.** It is not a community project
and it is not looking for contributors.

It exists to do three jobs for a tokenised fund, which today take three separate vendors and a
reconciliation between them:

1. **Make a price** — a sealed uniform-price auction that prints one official open/close.
2. **Sign a valuation** — a K-of-N committee that attests a *recipe*, not a static number.
3. **Issue and destroy shares** — atomic in-kind creation and redemption against that number.

The commercial thesis in one line: **calculating a NAV pays about a third of a basis point;
publishing an official price pays three basis points plus a flat annual fee.** The company is a bet
on which side of that line a committee-attested fixing lands.

**Who this is written for**, in the order they turn up:

| Reader | What to read |
|---|---|
| A prospective **customer** or partner | `docs/FIXING_METHODOLOGY.md`, then §3 below |
| An **engineer** evaluating it | `docs/PRODUCTION_CHECKLIST.md`, then `README.md` |
| Someone wanting to **hold a real asset** in it | `docs/ASSET_ONBOARDING.md` |
| An **employer, judge, grant reviewer or immigration authority** | `README.md` and `docs/BUILD_INVENTORY.md` |

## 2. Licence posture — read this before using anything here

**Source-available, not open source.** See `LICENSE`.

- You **may** read it, clone or fork it to evaluate it, and quote it with attribution.
- You **may not** run it in production, operate it as a service, build a derivative product from
  it, or redistribute it, without a written licence. Enquiries: **s.borjas@lucilla.ca**
- The vendored `splice-api-token-*` packages in `deps/` are **Apache-2.0** and are *not* covered by
  the CrossDesk licence — see `NOTICE` and `licenses/Apache-2.0.txt`.

Publishing it rather than hiding it is deliberate. The defensibility of this business is being the
**named administrator with a signed price history**, not the source files — a competitor who reads
the design still has to become somebody's fixing. Meanwhile the repository does real work as a
credential.

## 3. How a customer actually receives it

**Not as this repository.** A licensee gets artifacts, not source:

| Artifact | What they do with it |
|---|---|
| **The DAR** (`crossdesk-2.1.0.dar`) | Upload to *their own* Canton participant |
| **A backend container image** | Run it — Helm chart at `deploy/helm/canton-dvp-desk/`, or `docker compose`, or any container host |
| **The frontend bundle** | Static files behind their own ingress |
| **Configuration** | Ledger host/port/TLS, a JWT or refresh token, party ids, and any registrar entries (`docs/ASSET_ONBOARDING.md`) |

**They run the node, deliberately.** It keeps the infrastructure cost and the uptime obligation with
the party that has them anyway, and it puts the "operating a venue" permissions on their side rather
than ours. The one thing that is **not** handed over is the **fixing** — a benchmark
administrator's product *is* the operation.

**The exception is a pilot.** For a 90-day shadow run, invert it: they supply a party and a JWT on
their participant and we run the container. Zero infrastructure work on their side is what makes a
pilot easy to agree to. At contract time they take the chart in-house.

**Private distribution, when needed:** ship the DAR plus a container image from a private registry
against a signed licence. Source access is not part of the product and should not be offered as a
convenience.

## 4. Verified state — 13 August 2026

| Check | Result |
|---|---|
| `daml test` | **125 scripts, 0 failures** |
| `backend-devnet` tests | **92, 0 failures** |
| `backend` tests | **103, 0 failures** |
| Upgrade check (`--upgrades=crossdesk-2.0.0.dar`) | **0 errors** — 2.1.0 legally upgrades 2.0.0 |
| Frontend | builds clean, TypeScript passes |

```
DAR     .daml/dist/crossdesk-2.1.0.dar
sha256  4510677a30030c25d04d7adbd67dfe58a848945898d8c5aaebd0b7ddab77c7df
```

**Every new field added in 2.1.0 is `Optional` and appended at the end of its record.** That is what
makes it a legal smart-contract upgrade rather than a package rename: `templateIdScope` is
`#crossdesk`, so a version bump preserves every contract already on a participant. A field inserted
mid-record, or a non-`Optional` one, fails `NOT_VALID_UPGRADE_PACKAGE` and orphans the ledger.

### Two things that must not be misread

✅ **It has now been run end to end**, 13 August 2026 — `docs/PRODUCTION_CHECKLIST.md` §12 records
eighteen operations driven over HTTP against a live sandbox, with the numbers. Highlights: a
committee-attested fixing at 1885 feeding a basket NAV of **838.50**; create-then-redeem exact to the
unit; the sealed book returning **2 orders to the venue and 0 to the auditor**; and the operator's fee
landing at exactly 25 then 10. What is still unexercised is the desk **UI** — the walkthrough drove
the API, not buttons.

🔴 **The fund does not hold real cBTC.** 4.16 real cBTC was claimed through the CIP-56 registry flow
on BitSafe's templates, and that is a separate holding. The demo fund's own cBTC and cETH legs are
**self-issued test assets**. The basket consumes `ContractId Holding`; real cBTC lives behind
`HoldingV1`. Never state or imply otherwise.

## 5. What is built

- Sealed closing auction with uniform-price uncrossing, pro-rata rationing and a full tie-break
  ladder — **with a minimum-participation gate**: two orders from two distinct traders, or the book
  is uncrossed and nothing prints.
- K-of-N committee attestation of a valuation *recipe*, **and restatement of a published fixing
  under the same quorum**.
- In-kind creation and redemption, atomic, **with a flat operator fee per creation and redemption**
  and **an optional issuer pin per basket component**.
- Official and indicative NAV side by side, with the drift in basis points.
- CIP-56 token-standard holdings, transfers, allocations and DvP against the official Splice
  interfaces.
- Continuous order book, and cash-settled perpetuals. **Both work and neither is sold** — see §7.

## 6. Next — in order

| # | Task | Blocked on | Size |
|---|---|---|---|
| ~~1~~ | ~~Walk the checklist on a local sandbox~~ — ✅ **done**, §12 of the checklist. ⚠️ Remaining: drive the **UI** rather than the API, and note `LEDGER_PARTIES` is mandatory on a sandbox | — | mostly done |
| 2 | **Get a participant.** Ask NODERS (node-as-a-service, hosted the hackathon) or BitSafe (their services arm does node-operator matching) | someone else's yes | a message |
| ~~3~~ | ~~Fee auto-funding at `/basket/create` and `/basket/redeem`~~ — ✅ **done and proven live**: operator USDC `2600 → 2625 → 2635`, exact | — | done |
| 4 | **Fixing lookup by identifier and date.** `GET /fixings` returns contracts; a benchmark needs "the fixing for `CDX-CBTC-D` on 2026-08-12" | nothing | hours |
| 5 | **Scheduled strike.** ⚠️ Decide first: a K-of-N quorum **cannot** be automated. A cron can file the proposal or open the session; it cannot make K members act. Those are different products | a decision | hours |
| 6 | **Correlation id per request**, echoed into every log line. `updateId`/`offset` are already logged on every write, which is the important half | nothing | half a day |
| 7 | **CIP-56 adapter (Gap A)** — teach the basket to accept token-standard holdings so a fund can hold real cBTC, pinning the registrar via `expectedIssuer`. ⚠️ **Target V2 allocations, not V1** — see `docs/ECOSYSTEM_ALIGNMENT.md` §1 | **task 2**, and worth waiting for the funded Bit Dynamics reference | days |
| 7a | **Publish the fixing over the Kaiko Data Standard** once it lands in Splice — the ecosystem's interface for reference rates, so consumers read us by default instead of integrating bespoke. Also our strongest grant angle. See `ECOSYSTEM_ALIGNMENT.md` §2 | the Standard landing in Splice | small, high reach |
| 7b | **Operator as a Decentralized Party** — closes "one party, one key" on the venue itself. No template changes; the open question is what threshold coordination does to write latency. See `ECOSYSTEM_ALIGNMENT.md` §3 | **task 2** | design done |
| 8 | **Minimum-quality flag on the published fixing** — record *which tier* produced it and whether it was carried forward (methodology §3, §10) | nothing | hours |
| 9 | **Cessation process** (methodology §8) — a notice period, not code | nothing | policy |

**Do not start task 7 before task 2.** Building an adapter that has never touched a real registrar
produces code that looks finished and is unverified — the one outcome worse than not having it.

## 7. Deliberately parked

Both still build, both still pass their tests, **neither is deleted**:

- **Continuous order book** (28 scripts) — Temple Trading is a funded institutional CLOB on Canton.
  Weak ground to compete on, and it is the piece that most makes the desk look like an exchange.
- **Perpetuals** (18 scripts) — operating a leveraged derivatives venue for third parties is a
  licensed activity almost everywhere, and the hedge for a cBTC basket is BTC on CME or an offshore
  venue, far deeper than this could run. Licence the module to a regulated operator; never operate
  it. It was also never deployed to the shared node.

Cutting these from the *offering* is also the compliance strategy: what remains — publishing a
number, and a committee signing a valuation — is the unregulated part.

## 8. Known non-goals

Cross-margin · liquidity vaults · AMMs · RFQs · auto-deleveraging · permissionless liquidation
(positions are private, so a keeper cannot see them) · custody of anyone's assets · holding
permissioned fund shares such as USYC or BENJI, which are KYC-gated securities no faucet will issue.
