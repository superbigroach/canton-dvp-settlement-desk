# Daml Finance Integration — the documented library swap

This project ships a **self-contained settlement core** (`Instrument` / `Holding`
/ `Settlement` / `SettlementBatch` / `MarketOnClose`) that compiles with only the
Daml SDK's standard library — `daml-prim`, `daml-stdlib`, `daml-script`. **No
external `daml-finance` `.dar` is a build dependency.**

That is a deliberate, defensible engineering choice, and this document explains
both **why** we did it and **exactly how** to swap in the real
[Daml Finance](https://docs.daml.com/daml-finance/) V4 library when you want the
institutional-grade primitives.

> **Honesty note.** The mapping below reflects the Daml Finance V4 package layout
> and the standard instrument/holding/settlement lifecycle. The precise package
> **versions** must be pinned to whatever the Canton **Devnet / Splice** release
> you deploy against ships — confirm them from the cn-quickstart / HackCanton
> onboarding before adding the dependencies. Treat the version strings here as
> placeholders to be replaced, not as verified pins.

---

## Why we ship a self-contained core (and why that's the right call)

1. **Version-pin safety.** Daml Finance packages must match the ledger's SDK /
   Splice release exactly. Pinning the wrong version is the single most common
   way a hackathon build stops compiling the day before submission. A core built
   on only the standard library **always builds** with whatever SDK you install.
2. **It demonstrates that we understand the primitives.** The hand-rolled
   `Instrument` / `Holding` / `Settlement` templates are modelled on the exact
   Daml Finance shapes (see the table). Reviewers — and a JPMorgan Digital Assets
   audience — can read the authority model directly instead of through a library.
3. **The swap is low-risk and additive.** Because our templates mirror the Daml
   Finance interfaces one-for-one, replacing them is a mechanical, well-scoped
   upgrade, not a redesign. This doc is the runbook.

---

## The mapping table

| This project (self-contained) | Real Daml Finance V4 module that replaces it | Notes on the swap |
|---|---|---|
| `Instrument.InstrumentKey` `{issuer, depository, id, version}` | `Daml.Finance.Interface.Types.Common.Types.InstrumentKey` | Same 4-tuple. Ours is a drop-in for the interface type. |
| `Instrument.Instrument` (template, `signatory issuer`) | Instrument created by a **`Daml.Finance.Instrument.Token.V4.Factory`** implementing `Daml.Finance.Interface.Instrument.Token.V4.Instrument` | Factory pattern registers reference data; `kind`/`description`/`referencePrice` map to the token's `TokenMetadata` / observers. |
| `Holding.Holding` (`signatory issuer; observer owner`; `Transfer`/`Split`/`Merge`) | **`Daml.Finance.Holding.V4`** (`Fungible`, `Transferable`, `Base`) implementing `Daml.Finance.Interface.Holding.V4` | `Transfer` → the `Transferable` interface choice; `Split`/`Merge` → `Fungible`. Real holdings reference the instrument by `InstrumentKey` and support `Lock`/`Release`. |
| *(implicit — we mint holdings directly)* | **`Daml.Finance.Account.V4`** implementing `Daml.Finance.Interface.Account.V4` | Real Daml Finance credits holdings into an `Account` (a custody relationship between `custodian` and `owner`). Adopting Accounts is the main structural addition. |
| `Settlement.DvPProposal` / `DvPAgreement` / `Settle` | **`Daml.Finance.Settlement.V4`** `Instruction` + the `allocate` / `approve` / `settle` lifecycle in `Daml.Finance.Interface.Settlement.V4` | Our propose→accept→settle becomes: create `Instruction`s via a `Settlement Factory`, each party `allocate`s (pledges its holding) and `approve`s (names its receiving account), then the `Batch` `settle`s. |
| `Settlement.SettlementBatch` (settles N fills atomically) | **`Daml.Finance.Interface.Settlement.V4.Batch`** produced by a `Settlement Factory`, routed by a **`RouteProvider`** | The `Batch.Settle` choice executes all `Instruction`s atomically once every leg is allocated + approved. This is the exact "N instructions, all-or-nothing" contract our batch stands in for. |
| `Settlement.SettlementReceipt` | An `Effect` / report contract (e.g. modelled around `Daml.Finance.Interface.Claims`/lifecycle) or a bespoke audit template | Daml Finance has no single "receipt"; the audit artifact stays a small bespoke template observing the auditor. |
| `MarketOnClose.SealedOrder` / `ClosingAuction` / `RunClose` | **App-layer** — built ON Daml Finance settlement | The auction is *your* business logic; under the hood `RunClose` would assemble a Daml Finance `Batch` of `Instruction`s and settle it, instead of the per-order `CrossBuy`/`DeliverAsset` choices. |
| `Agent.TradingMandate` | **App-layer** delegation (unchanged) | Delegation is a Daml authority pattern, not a Finance primitive; it composes with either core. |

---

## The `daml.yaml` dependencies you would add

Replace the self-contained modules with the library by adding the Daml Finance
`.dar`s to `data-dependencies` (they are distributed as compiled DARs, not source
packages). Fetch them from the
[digital-asset/daml-finance releases](https://github.com/digital-asset/daml-finance/releases)
and pin every version to the one your Devnet SDK/Splice release supports:

```yaml
# daml.yaml  (ILLUSTRATIVE — pin <VERSION> to the Devnet-supported Daml Finance release)
dependencies:
  - daml-prim
  - daml-stdlib
  - daml-script

data-dependencies:
  # Interfaces (the types/choices you code against)
  - .lib/daml-finance-interface-types-common-<VERSION>.dar
  - .lib/daml-finance-interface-holding-<VERSION>.dar
  - .lib/daml-finance-interface-account-<VERSION>.dar
  - .lib/daml-finance-interface-instrument-base-<VERSION>.dar
  - .lib/daml-finance-interface-instrument-token-<VERSION>.dar
  - .lib/daml-finance-interface-settlement-<VERSION>.dar
  # Implementations (the concrete templates/factories you create)
  - .lib/daml-finance-holding-<VERSION>.dar
  - .lib/daml-finance-account-<VERSION>.dar
  - .lib/daml-finance-instrument-token-<VERSION>.dar
  - .lib/daml-finance-settlement-<VERSION>.dar
```

A convenience fetch script (as used by the Daml Finance getting-started repos):

```bash
# Download the pinned DARs into ./.lib (versions per the Devnet release notes)
mkdir -p .lib
VERSION=<pin-to-devnet-release>
BASE="https://github.com/digital-asset/daml-finance/releases/download"
for pkg in \
  interface-types-common interface-holding interface-account \
  interface-instrument-base interface-instrument-token interface-settlement \
  holding account instrument-token settlement ; do
  curl -sSL -o ".lib/daml-finance-${pkg}-${VERSION}.dar" \
    "${BASE}/<release-tag>/daml-finance-${pkg}-${VERSION}.dar"
done
```

> **Why `data-dependencies` and not `dependencies`.** Daml Finance is shipped as
> pre-compiled DARs. `dependencies` is for the SDK-bundled source packages
> (`daml-prim`/`daml-stdlib`/`daml-script`); third-party compiled packages go in
> `data-dependencies`. Getting this wrong is a common first-swap error.

---

## The migration, step by step (low-risk, incremental)

1. **Add Accounts.** Introduce `Daml.Finance.Account.V4` and credit holdings into
   accounts instead of minting `Holding` directly. This is the biggest structural
   change and can be done first, in isolation.
2. **Swap the instrument.** Replace `Instrument.Instrument` with a Token
   `Factory`; keep our `InstrumentKey` field names (they already match).
3. **Swap the holding.** Replace `Holding.Holding` with `Daml.Finance.Holding.V4`
   and route `Transfer`/`Split`/`Merge` through the `Transferable`/`Fungible`
   interfaces. Our `deliverExact` helper becomes a `Fungible.Split` + `Transfer`.
4. **Swap settlement.** Replace `DvPAgreement.Settle` with a `Settlement Factory`
   producing `Instruction`s; wire `allocate`/`approve`; settle via the `Batch`.
   `MarketOnClose.RunClose` assembles the `Batch` of matched instructions.
5. **Keep** `TradingMandate` and the `SealedOrder`/`ClosingAuction` app logic —
   they sit *above* the settlement layer and compose with either core.

Each step is independently testable against the existing `Test.daml` scenarios,
which describe the *behaviour* (atomic DvP, batch close, privacy) the library must
continue to satisfy.

---

## References

- Daml Finance documentation & tutorials — <https://docs.daml.com/daml-finance/>
- Getting started (instrument → account → holding → settlement) —
  <https://docs.daml.com/daml-finance/tutorials/getting-started/intro.html>
- Settlement concept (Instruction / Batch / RouteProvider) —
  <https://docs.daml.com/daml-finance/concepts/settlement.html>
- `digital-asset/daml-finance` (source, releases, package list) —
  <https://github.com/digital-asset/daml-finance>
- `digital-asset/daml-finance-app` (reference app patterns) —
  <https://github.com/digital-asset/daml-finance-app>
