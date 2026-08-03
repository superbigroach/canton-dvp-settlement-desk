# CIP-56 Token Standard — what is real, what is not

**Judges' feedback:** *"cBTC/cETH are currently self-issued stand-ins (move to CIP-56) … and show one real token-standard DvP."*

This document answers that literally, and it is deliberately written to be read
against the code. It names what is now genuinely CIP-56, what is still the
original self-issued path, and what it would take to move the rest.

**Short version:** there is now **one complete, tested, end-to-end DvP over the
official Canton Network Token Standard interfaces** — cETH against cBTC, two
legs, one atomic transaction. It lives in `daml/TokenStandardDvp.daml` and is
proved by `daml/TokenStandardTest.daml`. **The rest of the project — the
market-on-close auction, the ETF basket builder, the agent mandate, the
governance committee — still runs on the original self-issued `Holding.daml`
and has NOT been migrated.** Both statements are true at the same time and we
would rather say so than blur them.

---

## 1. The dependency is real

The official interface packages are vendored, unmodified, into `deps/` and
wired into `daml.yaml` as `data-dependencies`:

```yaml
data-dependencies:
  - deps/splice-api-token-metadata-v1-1.0.0.dar
  - deps/splice-api-token-holding-v1-1.0.0.dar
  - deps/splice-api-token-transfer-instruction-v1-1.0.0.dar
  - deps/splice-api-token-allocation-v1-1.0.0.dar
  - deps/splice-api-token-allocation-instruction-v1-1.0.0.dar
  - deps/splice-api-token-allocation-request-v1-1.0.0.dar
```

These are release artifacts copied byte-for-byte from the Splice distribution.
Nothing in this repository re-declares a standard type: the interfaces, their
views, their choices, their controllers and their authorisation rules are the
standard's. You can verify the packages are genuinely linked in:

```
daml damlc inspect-dar .daml/dist/canton-dvp-settlement-desk-1.0.0.dar
```

which lists, among others:

```
splice-api-token-holding-v1-1.0.0-718a0f77e505a8de22f188bd4c87fe74101274e9d4cb1bfac7d09aec7158d35b.dalf
splice-api-token-allocation-v1-1.0.0-93c942ae2b4c2ba674fb152fe38473c507bda4e82b4e4c5da55a552a9d8cce1d.dalf
splice-api-token-allocation-instruction-v1-1.0.0-275064aacfe99cea72ee0c80563936129563776f67415ef9f13e4297eecbc520.dalf
splice-api-token-allocation-request-v1-1.0.0-6fe848530b2404017c4a12874c956ad7d5c8a419ee9b040f96b5c13172d2e193.dalf
splice-api-token-transfer-instruction-v1-1.0.0-55ba4deb0ad4662c4168b39859738a0e91388d252286480c7331b3f71a517281.dalf
splice-api-token-metadata-v1-1.0.0-4ded6b668cb3b64f7a88a30874cd41c75829f5e064b3fbbadf41ec7e8363354f.dalf
```

**A version note worth having ready.** The Token Standard packages are built
with SDK **3.5.2** targeting **Daml-LF 2.1**; this project builds with SDK
**3.4.11** targeting **LF 2.2**. That works, because a higher-LF package may
consume a lower-LF `data-dependency`. No SDK upgrade was needed and none was
forced.

---

## 2. What is genuinely CIP-56 now

Before this change the project defined **zero** Daml interfaces — `daml test`'s
coverage report said so, and that is precisely what "self-issued stand-in"
meant. It now defines **six interface implementations**, all of them against
the official interfaces:

| Standard interface | Implementing template | What it is |
|---|---|---|
| `HoldingV1.Holding` | `TokenStandardHolding` | A cETH / cBTC balance any CIP-56 wallet can read, with real `Lock` semantics |
| `TransferInstructionV1.TransferFactory` | `TokenStandardRegistry` | The registry's rulebook for instructing transfers |
| `TransferInstructionV1.TransferInstruction` | `TokenStandardTransferOffer` | Two-step transfer: accept / reject / withdraw |
| `AllocationInstructionV1.AllocationFactory` | `TokenStandardRegistry` | The registry's rulebook for committing assets to a settlement |
| `AllocationV1.Allocation` | `TokenStandardAllocation` | A sender's binding commitment to one leg — the thing the DvP executes |
| `AllocationRequestV1.AllocationRequest` | `TokenStandardDvp` | The venue asking both sides to allocate, visible to any standard wallet |

Two things follow from that table that are worth saying out loud in the pitch:

**The venue never touches the assets.** `TokenStandardDvp` has no choice on any
holding, holds no custody, and cannot invent a transfer. It can only execute an
allocation a sender has already made, and only if the entire
`AllocationSpecification` — amounts, instruments, counterparties, *and the
settlement reference* — matches what it requested.

**The venue is registry-agnostic.** `TokenStandardDvp_Settle` talks only to
`AllocationV1.Allocation`. If cETH were issued by onRails' own registry and the
cash leg by a completely different registry with completely different Daml code,
the same choice would settle them atomically, unchanged. That portability is
the entire point of the standard, and it is the thing a self-issued holding can
never give you.

### One design decision we did not copy from the reference

The Splice reference token (`TestTokenV1`) takes a shortcut: the allocation
contract *is* the holding. Its own comments flag this as unsuitable for
production, and it leaves `AllocationView.holdingCids` empty, forcing wallets to
understand a bespoke construction.

We did it properly instead. An allocation **locks a real
`TokenStandardHolding`** — `lock = Some Lock { holders, expiresAt, context }`,
exactly as `HoldingV1.Lock` defines it — and names that holding in
`holdingCids`. A standard wallet can therefore render *"4.0 cETH, locked until
T, for settlement DVP-CETH-CBTC-001"* with no knowledge of this app. The lock's
`holders` field is also what drives disclosure: a free holding is private to its
owner and the registry, and a locked one is additionally visible to exactly the
parties who can release it.

---

## 3. What the DvP test actually proves

`daml/TokenStandardTest.daml`, five scenarios plus the fixture, all passing:

| Script | What it demonstrates |
|---|---|
| `testTokenStandardDvp` | Alice delivers 4.0 cETH, Bob delivers 0.25 cBTC, **both legs in a single transaction**. Both senders discover what they owe by reading the `AllocationRequest` interface — no bespoke API. Balances are asserted before allocation, while locked, and after settlement. |
| `testDvpRejectsMissingLeg` | **No delivery without payment.** Alice allocates, Bob never does, the venue tries to settle anyway → `submitMustFail`. Alice's cETH stays locked in her own name and never reaches Bob. |
| `testDvpRejectsForeignAllocation` | **An allocation is bound to exactly one settlement.** Bob allocates the correct amount of the correct instrument to a *different* DvP; the venue tries to recycle it → `submitMustFail`. |
| `testDvpRejectsUnauthorisedExecution` | **No one can pull a leg alone.** The receiver alone cannot execute; even sender + receiver together cannot settle behind the executor's back. The sender *can* take her own commitment back via `Allocation_Withdraw`. |
| `testTokenStandardTransfer` | The standard's plain transfer path end to end: `TransferFactory_Transfer` → funds locked, change returned → `TransferInstruction_Accept`. |

Run it:

```bash
daml build && daml test
```

---

## 4. What is still the legacy self-issued path

Everything else. Specifically, these are **not** CIP-56 and we are not claiming
they are:

| Still legacy | Why it matters |
|---|---|
| `daml/Holding.daml` | Issuer-only-signatory holding, `instrumentId : Text`. Implements no interface. |
| `daml/Instrument.daml` | Local `InstrumentKey` reference data, unrelated to `HoldingV1.InstrumentId`. |
| `daml/Settlement.daml` | The original bilateral DvP, moves `Holding` directly. |
| `daml/MarketOnClose.daml` | The sealed-order auction — the project's centrepiece — clears against legacy holdings. |
| `daml/Basket.daml` | ETF creation/redemption, legacy holdings. |
| `daml/Agent.daml`, `daml/Governance.daml` | Mandates and the NAV committee, legacy holdings. |

The cETH and cBTC used by the auction are **still self-issued stand-ins**. The
cETH and cBTC used by `TokenStandardDvp` are standard-compliant instruments.
They are different instruments in different modules; they do not interoperate.
That is the honest state of the migration as of this commit.

### Honest limitations of even the CIP-56 path

These are real gaps, not oversights, and we would rather name them first:

- **There is no registry *app*, only its on-ledger half.** The standard expects
  a registry backend serving the off-ledger Registry API (`getTransferFactory`,
  `getAllocationFactory`, choice contexts, disclosed contracts over HTTP). We
  implement the Daml side; the tests obtain the factory as an explicitly
  disclosed contract via `queryDisclosure`, which is the same *mechanism*
  production uses, driven from a script rather than from a service.
- **`ExtraArgs` / `ChoiceContext` are always empty.** Our registry needs no
  off-ledger context. A real registry (Amulet, say) passes fee schedules and
  round information through here.
- **We implement v1, not v2.** `splice-api-token-*-v2` exists in the same
  release and adds burn/mint, transfer events and multi-executor settlement.
  v1 is what is currently vetted on DevNet/MainNet, so v1 is what we targeted.
- **Issuance is not standardised and ours is not standard.** CIP-56 v1 says
  nothing about minting; each registry does it its own way. Ours is a direct
  create co-signed by admin and owner.
- **`TransferInstruction_Update` is not implemented** — it fails loudly. We
  complete transfers in one registry step, so there is no interim state for the
  registry to advance. Similarly we never return a pending
  `AllocationInstruction`; allocation always completes in one step. The standard
  permits both.
- **Implementing CIP-56 does not make our cETH *the* cETH.** The instrument
  admin is a party we allocate in the test. What the standard buys is that a
  real issuer's registry could be dropped in *without changing the venue code* —
  the compliance is in the interfaces, not in the issuer's identity.
- **This has not been deployed or vetted on DevNet** as part of this change. It
  builds and its tests pass locally.

---

## 5. The migration route for the rest

In rough order of value per unit of risk:

1. **A read-only veneer on the legacy holding.** `daml/Holding.daml` could be
   given an `interface instance HoldingV1.Holding` almost for free — the view is
   just `owner`, `InstrumentId { admin = issuer, id = instrumentId }`, `amount`,
   `lock = None`. Every existing holding would immediately be *visible* to
   standard wallets. **We deliberately did not do this**, because read-only
   visibility without the transfer and allocation lifecycle would let us claim
   more compliance than we have. It is the right first step *after* the pitch,
   with the caveat stated.

2. **Move the auction onto allocations.** `MarketOnClose` is already
   structurally an `AllocationRequest`: a venue collecting commitments from N
   senders and executing matched legs atomically. The change is to make
   `ClosingAuction` implement `AllocationRequest`, have traders commit via
   `AllocationFactory_Allocate` instead of pledging a `Holding`, and have the
   close exercise `Allocation_ExecuteTransfer` per matched leg — which is
   exactly the loop `TokenStandardDvp_Settle` already runs, just wider. This is
   the single highest-value remaining step: it makes the project's centrepiece
   standard-native.

3. **Basket creation/redemption.** In-kind creation is a multi-leg settlement
   with a mint on one side; it maps onto allocations plus the v2 burn/mint
   interface.

4. **Retire the local instrument layer.** Keep `Instrument.daml` as reference
   data (kind, description, reference price — the standard does not model these)
   but key it by `HoldingV1.InstrumentId` rather than the local `InstrumentKey`.

5. **Swap our registry for the real one.** Once the issuers exist, delete
   `TokenStandardRegistry` and point at the issuer's DARs. By construction, no
   venue code changes.

Steps 2–5 are days of work, not hours, and none of them will be done by the
pitch. Step 1 is an hour. We are showing one real path rather than a broken
half-migration, on purpose.
