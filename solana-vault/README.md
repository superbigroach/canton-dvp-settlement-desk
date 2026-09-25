# ETP Foundry basket vault — Solana (Anchor)

**Devnet only. Never deploy this to mainnet-beta.**

A share token that represents a **fixed basket of tokenised stocks** on Solana. It is the
Solana sibling of `../evm-vault/` (`EtpBasketVault.sol`): same product, same roles, same
committee-attested NAV, expressed in Anchor.

| Role | What it may do |
|---|---|
| **Authority** (admin) | add/remove APs, rotate the committee, set fees, pause, propose / apply basket changes, manage the holder allow-list, withdraw units for a rebalance **only while paused** |
| **Authorised participant** (AP) | `create` shares by depositing every constituent atomically; `redeem` shares for the units |
| **Committee** (K-of-N attestors) | sign the daily NAV fixing off-chain (Canton) |
| **Relay** (anyone) | post a signed fixing with `post_nav` |
| **Holders** | hold/transfer shares; may be restricted by the allow-list |

Declared program id: `ERs1iunZ9RWCRCWTaND3B1YNBNcs1bAPZWfCU9YByc5m` (the keypair `anchor build` generated; **not yet deployed to devnet**)

## Layout

```
solana-vault/
├── Anchor.toml                      # localnet by default; devnet section present; npm as package manager
├── Cargo.toml / Cargo.lock          # lockfile is PINNED, see "Toolchain notes"
├── package.json / tsconfig.json / .npmrc
├── programs/etp_basket_vault/src/
│   ├── lib.rs                       # instructions + account contexts (doc comment on each says WHY)
│   ├── state.rs                     # Vault, HolderRegistry, constants, integer math
│   ├── nav.rs                       # canonical fixing message + Ed25519 instruction introspection
│   ├── errors.rs                    # 49 custom errors
│   └── events.rs                    # one event per state change
├── tests/
│   ├── helpers.ts                   # provider-agnostic token helpers, fixtures, fixing signer
│   ├── etp_basket_vault.ts          # validator suite (anchor test)
│   └── time_warp.bankrun.ts         # clock-warped suite (timelock, fee accrual) via solana-bankrun
├── scripts/e2e-devnet.ts            # devnet end-to-end exercise -> deployments/devnet.json
└── migrations/deploy.ts
```

## State

`Vault` — PDA `["vault", instrument_id]`, one per series.

* `authority`, `instrument_id` (32 bytes, also the PDA seed), `share_mint` (PDA
  `["share_mint", vault]`, 9 decimals; the **vault is mint + freeze authority**)
* `constituents: Vec<{mint, units_per_share}>` (max 16). `units_per_share` is in the
  constituent's base units **per 1.000000000 share** (1e9 share base units).
* `create_fee_bps`, `redeem_fee_bps` (cap 10%), `management_fee_bps_per_year` (cap 5%),
  `last_accrual_ts`, `fee_recipient`
* `authorised_participants: Vec<Pubkey>` (max 32)
* `attestors: Vec<Pubkey>` (max 10), `threshold` (>= 2)
* `max_tier` (default 1, see NAV section)
* `latest_nav: {instrument_id, as_of_date (days), session, nav_per_share (1e9 = 1.0
  quote), rulebook_version, posted_at, attestor_count, fixing_ref, tier}`
* `pending_basket: Option<{constituents, proposed_at, apply_after}>`,
  `rebalance_delay_secs` (>= 1 day)
* `paused`, `redeem_while_paused`
* `holder_registry: Option<Pubkey>`

`HolderRegistry` — PDA `["registry", vault]`: `authority`, `allowed: Vec<Pubkey>` (max 128).

## Create / redeem flow

Both are **one instruction** that walks `remaining_accounts` in basket order, three
accounts per constituent, and does one `transfer_checked` CPI per line. SPL Token and
Token-2022 mints may be mixed; both programs are passed and the right one is picked
from the mint's owner.

**create(shares)** — remaining accounts per line: `[mint, ap_token_account, vault_token_account]`

1. vault not paused; signer is an AP; receiver (owner of `receiver_share_account`) is
   allow-listed if a registry is enabled
2. for every constituent: `units = ceil(shares * units_per_share / 1e9)` transferred
   AP → vault (rounding **up**: the vault is never short)
3. `fee = floor(shares * create_fee_bps / 10000)` minted to the fee recipient;
   `shares - fee` minted to the receiver

**redeem(shares)** — remaining accounts per line: `[mint, vault_token_account, ap_token_account]`

1. vault not paused, or `redeem_while_paused` (the emergency exit); signer is an AP
2. `fee = floor(shares * redeem_fee_bps / 10000)` paid **in shares** to the fee recipient;
   `net = shares - fee` burned
3. for every constituent: `units = floor(net * units_per_share / 1e9)` transferred
   vault → AP (rounding **down**)

If any line fails (underfunded AP account, missing ATA, wrong order) the whole
transaction reverts: no shares, no partial deposit. The tests prove this.

Compute: three constituents fit comfortably in the default budget. For baskets near
the 16-line cap, add a `ComputeBudgetProgram.setComputeUnitLimit` instruction.

**Management fee** accrues by dilution: `accrue_fees` (permissionless crank) mints
`supply * bps * elapsed / (10000 * 31 536 000)` shares to the fee recipient. Dust below
one base unit is carried (the timestamp only advances when something was minted, or
when there was nothing to charge). `set_fees` settles at the old rate before changing it.

## How the NAV arrives

```
Canton fixing committee            relay (anyone)                    Solana
─────────────────────              ───────────────                   ──────
K of N members confirm a           builds the canonical message,     Ed25519 native program
FixingProposal → NavFixing         collects the K detached           verifies each signature
contract (daml/Governance.daml)    ed25519 signatures, and sends     (tx fails otherwise);
                                   ONE transaction:                  post_nav reads them back
each member signs the              [Ed25519Ix ×K, post_nav]          from the instructions
canonical message with its                                           sysvar, checks message,
Solana ed25519 key                                                   attestor set, distinctness,
                                                                     threshold, date order, tier
```

The **canonical message** (`nav::canonical_nav_message`, 198 bytes) is

```
"ETPFOUNDRY_NAV_FIXING_V1" ‖ program_id ‖ vault ‖ instrument_id ‖ as_of_date u32 LE
‖ session u8 ‖ nav_per_share u64 LE ‖ rulebook_version[32] ‖ fixing_ref[32] ‖ tier u8
```

It binds the fixing to **this program and this vault**, so a signature for one series
or a look-alike deployment cannot be replayed against another.

* `fixing_ref` = sha256 of the Canton `NavFixing` contract-id string (computed by the
  relay, signed by the committee). It lets an auditor walk from the on-chain record back
  to the exact ledger attestation.
* `tier` is the desk's fixing tier on the scale defined in
  `backend/src/main/java/com/lucilla/settlement/benchmarks/SeriesRow.java` (`labelFor`):
  0 = seed, 1 = attested, 2 = alternate-seats, 3 = benchmark-x-factor,
  4 = carried-forward, 5 = missed. It **ascends as trust decreases**, so the gate is a
  maximum: `post_nav` refuses `tier == 0` at every setting and `tier > vault.max_tier`.
  `max_tier` defaults to 1 (attested only) and the admin may set it in 1..=5
  (`set_max_tier`), e.g. 4 to let a carried-forward print keep the on-chain NAV alive
  over a holiday.

`post_nav` rules (all enforced on-chain): instrument matches; NAV > 0; `as_of_date`
strictly greater than the last accepted one (no replay, total order); not more than one
day in the future; every Ed25519 instruction in the transaction must be a well-formed
signature over exactly the canonical message, by a registered attestor, each attestor
at most once; distinct count >= threshold. Any junk or stray signature fails the call.

Client side (see `tests/helpers.ts`): `navMessage()` builds the bytes,
`nacl.sign.detached` signs, `Ed25519Program.createInstructionWithPublicKey` wraps each
signature, and the Anchor call gets them as `preInstructions`.

## Holder restrictions

Two layers:

1. **Share token (this program).** When `holder_registry` is enabled, `create` refuses
   to mint to a receiver that is not on the vault's `HolderRegistry` allow-list. This is
   the one place shares come into existence, so it is the choke point. The vault is also
   the share mint's **freeze authority**, so a delisted holder's account can be frozen.
   Secondary transfers between holders are *not* gated by this program today; see the
   Token-2022 route below.

2. **Tokenised stocks (the constituents).** xStocks and similar Solana stock tokens are
   **Token-2022 mints with a transfer hook** (the issuer's program is invoked on every
   transfer and enforces its own allow-list / permissioned-transfer rules). Consequences:
   * the **vault's constituent ATAs must be allow-listed by the issuer**, exactly as an
     institutional custody wallet would be. Without that, the issuer's hook rejects the
     AP → vault transfer and `create` fails (atomically, no shares).
   * a hooked `transfer_checked` needs the hook program and its extra account metas
     appended to the CPI. The current `create`/`redeem` use the plain interface call,
     which is correct for SPL Token and for Token-2022 mints **without** a hook. Adding
     hooked mints means switching those two CPIs to
     `spl_token_2022::onchain::invoke_transfer_checked` and passing the hook's extra
     accounts after each line's three accounts. Everything else (rounding, atomicity,
     ordering) is unchanged.
   * to gate **secondary transfers of the share itself**, mint the share as a Token-2022
     mint with the `TransferHook` extension pointing at a small hook program that reads
     this vault's `HolderRegistry` (`is_allowed(destination.owner)`). `initialize_vault`
     already accepts Token-2022 as the share token program; the hook program and the
     extension initialisation are not built.

## Rebalance (timelocked)

`propose_basket` stores the new composition with `apply_after = now + rebalance_delay_secs`
(minimum one day, so holders can redeem at the old composition first). One proposal at a
time; `cancel_basket` withdraws it. `apply_basket` after the delay:

* **weights only** (same mints): applied live.
* **mint set changes** (add/remove a line): the vault **must be paused**, because its
  inventory no longer matches the definition until the custodian has rebalanced. While
  paused, `rebalance_withdraw` (admin, evented) moves units out; deposits need no
  instruction. Then unpause.

## Pause

`set_pause(true)` blocks `create` and `redeem`. `set_redeem_while_paused(true)` re-opens
`redeem` only — the emergency exit when creation must stay halted (e.g. an issuer halt).

## Build & test

Everything runs inside WSL (Ubuntu 24.04) on this machine; the Windows checkout is the
source of truth and is rsynced to a WSL-native directory because `cargo` on `/mnt/c` is
very slow.

```bash
# toolchain used
solana-cli 2.2.12 (Agave), anchor-cli 0.31.1, cargo 1.86 (host), node 22, npm 10
# platform-tools v1.47, rustc 1.84.1 — which is what Agave 2.2.12 pins by default

npm install                                   # .npmrc sets legacy-peer-deps (anchor-bankrun peer range)
anchor build                                  # program + IDL + TS types
anchor test                                   # starts a local validator, runs both suites
```

**If `anchor build` fails with `error[E0463]: can't find crate for 'core'`** — including on
the *build scripts* of trivial crates — the active Agave release is pinning a
platform-tools version whose cache extraction is incomplete (no `rust/lib/rustlib`). That
is a toolchain installation fault, not a dependency-version problem. Fix it by moving to a
release that pins a complete one:

```bash
agave-install init 2.2.12          # pins platform-tools v1.47 (rustc 1.84.1)
rustup toolchain uninstall solana  # drop the stale link to the old sysroot
```

Repointing the `…/sdk/sbf/dependencies/platform-tools` symlink by hand does **not** work:
`cargo-build-sbf` recreates it from its own pinned version on every build. The full
diagnosis is in `BUILD_STATUS.md`.

### Toolchain notes (why Cargo.lock is pinned)

`cargo build-sbf` compiles with platform-tools' rustc (1.79 / 1.84), which cannot parse
crates that declare `edition = "2024"` or `rust-version >= 1.85`. The lockfile pins the
transitive roots that had drifted there: `blake3 1.5.5`, `zeroize 1.8.1`,
`zeroize_derive 1.4.2`, `proc-macro-crate 3.2.0`, `indexmap 2.7.1`, `hashbrown 0.15.5`,
`unicode-segmentation 1.12.0`. Do not `cargo update` blindly. To re-check after any
dependency change:

```bash
cargo metadata --format-version 1 | python3 -c "import json,sys; m=json.load(sys.stdin)
print([p['name']+' '+p['version'] for p in m['packages'] if p.get('edition')=='2024' or int((p.get('rust_version') or '1.0').split('.')[1])>79])"
```

## Devnet deploy

> **Not deployed yet.** The program compiles and the suites run against a local validator,
> but nothing has been published to devnet and there is no `deployments/devnet.json`. When
> there is, this section gets the program id, the vault PDA, the mock mints and the
> transaction signatures — and the warning below stays.

`Anchor.toml` deliberately keeps `cluster = "localnet"`; devnet is selected per command, so
the repo never points at a live cluster by default. **Never mainnet-beta.**

```bash
# inside WSL, from ~/sv-build
solana-keygen new --no-bip39-passphrase --silent -o ~/.config/solana/etp-devnet.json
solana airdrop 2 --url devnet -k ~/.config/solana/etp-devnet.json   # repeat; needs ~2.6 SOL
# rent-exempt minimum for the 499 KB program's data account is 2.53673356 SOL
# (`solana rent 499229`), plus fees. The public devnet faucet is aggressively
# rate-limited per IP: `airdrop request failed ... rate limit` can persist for hours.
# https://faucet.solana.com (GitHub sign-in) is the fallback.
anchor build
# NOTE: pass an explicit --buffer. `solana program deploy` without one generates an
# ephemeral buffer keypair and PRINTS ITS 12-WORD SEED PHRASE if the deploy fails.
solana-keygen new --no-bip39-passphrase --silent -o /tmp/etp-buffer.json
solana program deploy target/deploy/etp_basket_vault.so   --program-id target/deploy/etp_basket_vault-keypair.json   --buffer /tmp/etp-buffer.json -k ~/.config/solana/etp-devnet.json --url devnet
anchor idl init --provider.cluster devnet --provider.wallet ~/.config/solana/etp-devnet.json   -f target/idl/etp_basket_vault.json ERs1iunZ9RWCRCWTaND3B1YNBNcs1bAPZWfCU9YByc5m

# then exercise it end to end and write deployments/devnet.json
ANCHOR_PROVIDER_URL=https://api.devnet.solana.com ANCHOR_WALLET=~/.config/solana/etp-devnet.json   npx ts-node scripts/e2e-devnet.ts
```

The program keypair lives at `target/deploy/etp_basket_vault-keypair.json` (git-ignored,
never printed). Build on another machine and you get a different one: run
`anchor keys sync` to rewrite `declare_id!` and `Anchor.toml`, then rebuild.

### `scripts/e2e-devnet.ts`

The Solana counterpart of `../evm-vault/scripts/e2e-testnet.ts`. It refuses to run against
anything whose **genesis hash** is not devnet's, then: mints two mock constituents
(`MOCK Apple (test only)` / `mAAPL`, 6 dp, 2.0 per share; `MOCK T-Bill (test only)` /
`mTBILL`, 6 dp, 100.0 per share), initialises a vault with all fees at zero so the in-kind
arithmetic is exact, sets a 2-of-3 attestor committee, grants the AP role, creates 1000
shares in kind, redeems 250, and posts a NAV signed by two of the three through the Ed25519
instruction-introspection path. Every constituent, share and supply balance is asserted
exactly before and after create and redeem. It then proves two negatives on-chain — a
posting one signature short (`InsufficientAttestations`) and one with `tier = 0`
(`SeedTierRefused`) — checks that neither refused posting altered `latest_nav`, and writes
everything, with `https://explorer.solana.com/...?cluster=devnet` links, to
`deployments/devnet.json`.

**What it is not:** the constituents are throwaway SPL mints with no issuer and no on-chain
name (the labels live in `deployments/devnet.json`, not in token metadata), the
`navPerShare` is an invented number, and `fixingRef` is the sha256 of a test string that is
**not a Canton attestation**.

To stand up a series by hand instead, script the same calls the tests make:
`initialize_vault` → `set_ap` → create the vault's constituent ATAs
(`getAssociatedTokenAddressSync(mint, vault, true, programId)`) → optionally
`init_holder_registry` + `set_holder_registry(true)` + `set_holder_allowed`.


## Tests

`tests/etp_basket_vault.ts` (validator): initialise + validation, admin access,
create (exact ceil math, atomicity with an underfunded line, ordering, ownership, fee
account), holder registry, redeem (exact floor math, solvency invariant), `post_nav`
(K-of-N happy path, duplicate signer, below threshold, no signatures, non-attestor,
tampered message, other-vault binding, wrong instrument, future date, replay same day,
older day, forged signature, tier gate incl. seed refused at every setting, committee
rotation), rebalance timelock (propose/cancel/apply-before-delay/withdraw gating), pause
semantics, fees.

`tests/time_warp.bankrun.ts` (bankrun, warped clock): apply after the delay (weights
live, set change needs pause, then a 4-line create), longer delay honoured, management
fee over one year / partial period / zero rate / rate change settlement / dust carry,
NAV date window as the clock advances.

Both suites run under `anchor test`.

A committee of three or more must pack its signatures into **one** Ed25519 instruction
(`ed25519IxMulti` / `signedByPacked` in `tests/helpers.ts`): three separate
`Ed25519Program.createInstructionWithPublicKey` instructions each repeat the 146-byte
canonical message, and together with `post_nav` that is 1424 bytes against the 1232-byte
transaction limit. `nav::verify_attestations` iterates `num_signatures` within an
instruction, so packing is the supported shape, and it is what a real relay has to do.

## What is not built

* **Transfer-hook-aware constituent transfers** (`invoke_transfer_checked` with extra
  account metas) — needed the moment a constituent is a hooked Token-2022 mint such as an
  xStock. Plain SPL Token and hook-less Token-2022 constituents work now (the tests use
  one of each).
* **A transfer hook for the share token** to gate secondary transfers against the
  `HolderRegistry`; only the create side is gated today (plus freeze authority).
* **On-chain NAV use**: the NAV is recorded and evented but nothing prices off it
  on-chain (creation and redemption are in-kind).
* **Multisig authority**: `authority` is a single key; point it at a Squads multisig with
  `set_authority` for anything beyond devnet.
* **Rebalance execution**: the custodian swaps old for new constituents off-chain while
  paused; there is no on-chain venue for tokenised stocks here.
* Mainnet anything.
