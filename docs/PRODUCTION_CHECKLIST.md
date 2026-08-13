# Production checklist — every action the desk can perform

**51 operations**, taken from `frontend/src/api.ts` rather than from memory. Every one of them is
reachable from a button in the desk, so this is the list to walk before showing it to anyone.

## How to read the columns

- **Local** — fully verifiable on a local sandbox. No node operator, no network, no credentials.
- **Node** — needs a live participant *and* a foreign registrar. Only real third-party assets do.
- **Scope** — `CORE` is what the business sells. `PARKED` still works and still passes its tests;
  it is simply not what is being sold, and should not be led with.

**47 of 51 are Local.** The four that are not are the CIP-56 transfer legs, and even those work
locally against this project's own registry — a node is needed only for someone else's asset.

---

## 1. Boot the sandbox (this is the whole answer to "can we test it locally")

SDK 3.4.11 lives in WSL at `/home/sborj/.daml/bin`. ⚠️ **Long-running processes die when a shell
call returns** — start each in its own background process, not with `setsid -f`.

```bash
# 1) ledger
daml sandbox --port 6900                       # ~40s to "Canton sandbox is ready"

# 2) code + seed
daml ledger upload-dar --host localhost --port 6900 .daml/dist/crossdesk-2.1.0.dar
daml script --ledger-host localhost --ledger-port 6900 \
  --dar .daml/dist/crossdesk-2.1.0.dar --script-name Test:initialize

# 3) backend
cd backend-devnet && LEDGER_HOST=localhost LEDGER_PORT=6900 LEDGER_TLS=false SERVER_PORT=8080 \
  java -jar build/libs/canton-dvp-desk-1.0.0.jar

# 4) desk
cd frontend && npx vite preview --port 5173     # proxies /api -> :8080
```

Vite runs on the Windows side and still reaches WSL's `:8080` through localhost forwarding.

**Seed balances to know before clicking:** Bob holds the only `DEMO:AAPL` besides Bank (500) and
the Issuer; Alice holds USDC + cETH + CBTC but **no AAPL** — so use Bank for asks. A reserved order
consumes the balance, which is why a second attempt reports "no uncommitted X to commit".

---

## 2. Reference and identity — 5

| # | Operation | Local | Node | Scope |
|---|---|:-:|:-:|---|
| 1 | `parties` | ✅ | | CORE |
| 2 | `instruments` — list and publish | ✅ | | CORE |
| 3 | `holdings` | ✅ | | CORE |
| 4 | `liveMarks` — Coinbase spot, 60s cache, fails soft | ✅ | | CORE |
| 5 | `accruedNav` | ✅ | | CORE |

⚠️ `liveMarks` reaches the public internet. It fails soft by design, so a blocked egress shows a
stale mark rather than an error — check the timestamp, not just the number.

## 3. Bilateral DvP and CIP-56 transfers — 6

| # | Operation | Local | Node | Scope |
|---|---|:-:|:-:|---|
| 6 | `trade` — bilateral atomic DvP | ✅ | | CORE |
| 7 | `receiptsFor` | ✅ | | CORE |
| 8 | `pendingTransfers` | ✅ own registry | ⚠️ foreign asset | CORE |
| 9 | `acceptTransfer` | ✅ own registry | ⚠️ foreign asset | CORE |
| 10 | `rejectTransfer` | ✅ own registry | ⚠️ foreign asset | CORE |
| 11 | `withdrawTransfer` | ✅ own registry | ⚠️ foreign asset | CORE |

**These four are the only node-gated actions.** Against this project's own
`TokenStandardRegistry` they work locally end to end. Against a *foreign* registrar — real cBTC —
they need a live participant and that registrar's `ChoiceContext`, per `docs/ASSET_ONBOARDING.md`.

## 4. Sealed closing auction — 7 · the differentiator

| # | Operation | Local | Node | Scope |
|---|---|:-:|:-:|---|
| 12 | `mocOrder` — lodge a sealed order | ✅ | | CORE |
| 13 | `mocState` | ✅ | | CORE |
| 14 | `mocClose` — uncross at one uniform price | ✅ | | CORE |
| 15 | `imbalance` | ✅ | | CORE |
| 16 | `mandateTerms` | ✅ | | CORE |
| 17 | `myMandate` | ✅ | | CORE |
| 18 | `acceptMandate` | ✅ | | CORE |

🔴 **`mocState` takes `actingAs`, while `bookState` takes `as`.** Passing `as` to the MOC endpoint
is silently ignored and falls back to the Venue — which makes the book look fully transparent when
it is not. This caused a false alarm once already. **When demonstrating the dark book, verify the
parameter name before believing the output.**

**The money shot:** query one auction with two resting orders as four parties — Venue sees 2,
each trader sees only its own, **the auditor sees 0**.

## 5. Committee attestation — 6 · this is the product

| # | Operation | Local | Node | Scope |
|---|---|:-:|:-:|---|
| 19 | `createCommittee` — K-of-N | ✅ | | CORE |
| 20 | `proposeFixing` | ✅ | | CORE |
| 21 | `proposeAccruingFixing` — signs a recipe, not a number | ✅ | | CORE |
| 22 | `confirmFixing` — a second signer | ✅ | | CORE |
| 23 | `finalizeFixing` — republishes the mark | ✅ | | CORE |
| 24 | `fixings` | ✅ | | CORE |

**Walk 19 → 23 in order. That sequence is the pilot.** If it runs clean, there is a sellable
product. Everything else is upside.

## 6. Fund: create, redeem, NAV — 6 · the second half of the product

| # | Operation | Local | Node | Scope |
|---|---|:-:|:-:|---|
| 25 | `defineBasket` — now takes `feeReceiver`, `creationFee`, `redemptionFee`, and `expectedIssuer` per leg | ✅ | | CORE |
| 26 | `baskets` — echoes the issuer pin back as a label | ✅ | | CORE |
| 27 | `basketCreate` — in-kind, atomic | ✅ | | CORE |
| 28 | `basketRedeem` — in-kind, atomic | ✅ | | CORE |
| 29 | `basketNav` — official, from signed marks | ✅ | | CORE |
| 30 | `basketIndicativeNav` — live, binding on nobody | ✅ | | CORE |

**Test the fee explicitly**, since it is new: define a basket with `feeReceiver` set and a fee, run
a creation, and confirm the receiver's cash increased and the receipt records the amount. Then
define one with a fee and *no* funding cash and confirm the creation is refused with nothing moved.

⚠️ **Not yet wired:** `basketCreate` / `basketRedeem` do not auto-provision the fee cash. Defining
a fee-bearing basket works over REST; funding the fee from those two endpoints is the remaining
piece. Until then, exercise the fee from Daml or the command layer.

## 7. Continuous order book — 10 · works, not sold

| # | Operation | Local | Node | Scope |
|---|---|:-:|:-:|---|
| 31–40 | `openBookSession`, `closeBookSession`, `reopenBookSession`, `placeBookOrder`, `cancelBookOrder`, `withdrawOrder`, `clearBook`, `bookState`, `bookTape`, `bookConfirms` | ✅ | | PARKED |

Fully implemented, 28 passing scripts. **Parked commercially:** Temple Trading is a funded
institutional CLOB on Canton, so this is the weakest ground to compete on and the piece that most
makes the desk look like an exchange. Demonstrate it; do not sell it.

## 8. Perpetuals — 11 · works, deliberately not sold

| # | Operation | Local | Node | Scope |
|---|---|:-:|:-:|---|
| 41–51 | `openPerpMarket`, `openPerpPosition`, `closePerpPosition`, `addPerpCollateral`, `setPerpIndex`, `derivePerpFunding`, `applyPerpFunding`, `fundPerpInsurance`, `liquidatePerpPosition`, `perpMarkets`, `perpPositions` | ✅ | | PARKED |

**Nothing was deleted.** 603 lines of Daml and 18 passing scripts, still building. Parked because
operating a leveraged derivatives venue for third parties is a licensed activity almost everywhere,
and because the hedge for a cBTC basket is BTC on CME or an offshore venue — far deeper than
anything this could run. Licence the module to a regulated operator; never operate it.

⚠️ The perp layer was **never deployed to the shared node** — it is local-only. Do not claim it runs
there.

---

## 9. The walkthrough order

Click it in this sequence and every CORE path is exercised once:

1. `parties` → `instruments` → `holdings` — does the desk see the world
2. `liveMarks` — check the **timestamp**, not just the number
3. `createCommittee` → `proposeAccruingFixing` → `confirmFixing` → `finalizeFixing` → `fixings`
4. `defineBasket` (with a fee and an `expectedIssuer`) → `basketNav` → `basketIndicativeNav`
5. `basketCreate` → `basketRedeem` — confirm cash is conserved and the receipt carries the fee
6. `mocOrder` ×2 from different parties → `mocState` **as four parties** → `mocClose`
7. `trade` → `receiptsFor`
8. `pendingTransfers` → `acceptTransfer`

Anything that errors in 1–8 is a real defect. Anything that errors in the book or perp panels is
noted and skipped — those are not in the offering.

## 10. Known traps that look like bugs

| Symptom | Actual cause |
|---|---|
| Every write fails, reads return 200 | Backend bound to a package id the participant does not have |
| The dark book looks transparent | `as` passed to a `/moc/*` endpoint instead of `actingAs` |
| "No uncommitted X to commit" | A reserved order already consumed that balance |
| A button does nothing, no error | A dead selector or a stale hot-restart — rebuild rather than debug |
| Two clicks, two submissions | `disabled={busy}` is not a guard; `setBusy` only schedules a re-render |
| Stale mark, no error | `liveMarks` fails soft on purpose |

## 11. What this checklist cannot cover

That **real cBTC settles into a fund**. The basket consumes `ContractId Holding`; real cBTC lives
behind `HoldingV1` on BitSafe's templates. No local run proves that, because a sandbox has no
BitSafe registrar. It needs a live participant holding the real asset — and that is the only item
on this page that money and a Telegram message can fix but code cannot.
