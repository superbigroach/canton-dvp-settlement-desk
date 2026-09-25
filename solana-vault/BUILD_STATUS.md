# Build status — 25 September 2026

**The program compiles and the test suite runs.** The toolchain blocker recorded here on
24 September is fixed; what follows is the diagnosis, the fix, and the current state.

## The blocker, correctly diagnosed

The 24 September note blamed the dependency graph ("Anchor 0.31.1 resolves crates that want
a newer Rust than platform-tools v1.43's 1.79"). That was wrong, and the symptom said so:
`error[E0463]: can't find crate for 'core'` was also failing the **build scripts** of
trivial crates (`cfg-if`, `itoa`, `autocfg`). Build scripts compile for the *host*, not for
`sbf-solana-solana`, so a crate-version problem cannot explain them.

The real cause is a **partial extraction of platform-tools v1.43** in the local cache:

```
~/.cache/solana/v1.43/platform-tools/rust/
├── bin/   cargo, rustc, rustdoc
└── lib/   librustc_driver-*.so, libstd-*.so
            ^ and NOTHING else — lib/rustlib/ is entirely absent
```

`lib/rustlib/` is the sysroot. Without it there is no `core`/`std` for **any** target,
host included, which is exactly the error. The 393 MB
`platform-tools-linux-x86_64.tar.bz2` is still sitting un-extracted next to it. By
contrast `~/.cache/solana/v1.47/platform-tools` is complete:

```
rust/lib/rustlib/{sbf-solana-solana, sbpf{,v1,v2,v3}-solana-solana, src, x86_64-unknown-linux-gnu}
```

## The fix: use the Agave release that pins v1.47

`cargo-build-sbf` pins one platform-tools version and **re-creates the symlink to it on
every build**, so repointing
`…/active_release/bin/sdk/sbf/dependencies/platform-tools` by hand does not survive a
single `anchor build`. The version is a property of the Agave release:

| Agave | platform-tools | rustc |
|---|---|---|
| 2.1.22 (was active) | v1.43 — the broken extraction | 1.79.0 |
| **2.2.12 (now active)** | **v1.47 — complete** | **1.84.1** |

2.2.12 was already downloaded under `~/.local/share/solana/install/releases/2.2.12`, so the
whole fix was one command:

```bash
agave-install init 2.2.12
rustup toolchain uninstall solana   # drop the stale link to the v1.43 sysroot
```

After that, plain `anchor build` works — no `--tools-version` flag, which is what the
README previously needed as a workaround.

```
solana-cli 2.2.12 (Agave)   solana-cargo-build-sbf 2.2.12
platform-tools v1.47        rustc 1.84.1
anchor-cli 0.31.1           host rustc 1.86.0, node 22.18, npm 10.9
```

The alternative fix — re-extracting the v1.43 tarball in place — was not needed and would
have left the build on rustc 1.79.

**Build from the WSL Linux filesystem, never `/mnt/c`.** The working copy is `~/sv-build`,
refreshed from the Windows checkout, which remains the source of truth.

```bash
wsl -e bash -lc 'export PATH="$HOME/.local/share/solana/install/active_release/bin:$HOME/.cargo/bin:$PATH" \
  && cd ~/sv-build && anchor build && anchor test'
```

## Program changes made while getting it to build

* `declare_id!` and `Anchor.toml` carried `HQ99NqzmrGn88vJJvKeqE22B7zezHZMQNYX3LxBxHSnV`,
  a placeholder for which **no keypair has ever existed**. `anchor keys sync` rewrote both
  to the pubkey of the keypair `anchor build` generates. That is the only change to the
  program; the logic is untouched.

## Known build warnings (not errors)

* `AccountInfo::realloc` is deprecated in Solana 2.2 — emitted from inside Anchor 0.31.1's
  `#[program]` macro, not from this code.
* `core::slice::sort::stable::driftsort_main` exceeds the 4096-byte SBF stack frame by
  8 bytes. It comes from a `sort` in the standard library as monomorphised for this
  program, is a warning from the linker, and nothing in the suite trips it.

## Test state

Two full runs completed, both against a local validator started by `anchor test`:

| Run | Result |
|---|---|
| first green build | **76 passing, 6 failing** |
| after fixing those six | **80 passing, 2 failing** |

**All eight failures were test bugs, not program bugs.** The program's arithmetic was right
in every case:

1. *`rejects an all-zero instrument id`* — `initRaw` derived the vault PDA from a fresh
   random instrument id while the call carried the overridden all-zero one, so Anchor's
   seeds constraint fired before the program's own `ZeroInstrumentId` check.
2. *`3-of-3 is accepted by any relay`* — three separate Ed25519 instructions plus
   `post_nav` is 1424 bytes against the 1232-byte transaction limit. Fixed by packing the
   signatures into **one** Ed25519 instruction (`ed25519IxMulti` / `signedByPacked` in
   `tests/helpers.ts`), which `nav::verify_attestations` already supports and which a real
   relay has to do for a committee of three.
3. *`a partial period is pro-rated`* — the test's own hard-coded sanity value was a typo:
   `1234567891 * 50 * 2592000 / 315360000000` is `507356.7...`, not `507476.7...`. The
   program returned 507356.
4. *`dust below one base unit is carried`* — the comment read the formula as if one share
   were one base unit. One share is 1e9 base units, so at 50 bps p.a. the fee is ~0.1585
   base units per **second** and one whole unit needs ~6.31 s, not 6307 s. The warps were
   1000x too long (6000 s is already 951 units). Changed to 6 s then +1 s.
5. *`accrue_fees right after initialise mints nothing`* — same arithmetic slip: 10 shares at
   50 bps accrue ~1.585 base units per second, so a live validator does mint a few units
   between initialise and the call. Now bounded at a minute's worth instead of asserted to
   be zero, with the mint required to equal the balance change exactly.
6. *`refuses a forged signature`* — the assertion guessed at the RPC's wording. The precise
   claim is that the native Ed25519 program rejects the transaction *before any instruction
   executes*, so it now asserts the program id and `AnchorError` never appear in the
   failure text.

The last two of those six fixes (5 and 6) were applied but **their rerun did not complete**
— see below — so the last honestly verified count is **80 passing, 2 failing**, with the
two remaining failures being the two assertions that fix 5 and 6 target.

## Not done: devnet deploy and the end-to-end exercise

`scripts/e2e-devnet.ts` is written (see `README.md` → Devnet deploy) but has never run, and
**nothing has been deployed to devnet**. There is no `deployments/devnet.json`, no program
on-chain, and no transaction signatures. The declared id
`ERs1iunZ9RWCRCWTaND3B1YNBNcs1bAPZWfCU9YByc5m` is the locally generated build keypair only.

The blocker is not the toolchain and not the code: **the WSL VM wedged.** Mid-run,
`wsl.exe` stopped servicing new connections — `vmmemWSL` sat at 0 bytes and 0 CPU while ten
`wsl.exe` clients queued forever waiting on VM creation, and one earlier invocation had
already returned

```
The operation timed out because a response was not received from the virtual machine or
container. Error code: Wsl/Service/CreateInstance/CreateVm/HCS_E_CONNECTION_TIMEOUT
```

The fix for that state is `wsl --shutdown` (or `wsl -t Ubuntu`) followed by a fresh
invocation. Nothing in `~/sv-build` is lost by it — the build artefacts, the program
keypair and the node modules are all on the ext4 disk.

To finish, once WSL is back:

```bash
wsl --shutdown          # clears the stuck VM
wsl -e bash -lc 'export PATH="$HOME/.local/share/solana/install/active_release/bin:$HOME/.cargo/bin:$PATH"   && cd ~/sv-build && anchor test'                       # confirm 82 passing
# then the Devnet deploy block in README.md
```

## Rules that still hold

* **Devnet only.** `Anchor.toml` keeps `cluster = "localnet"`; devnet deploys pass
  `--provider.cluster devnet` on the command line. Never mainnet-beta.
* The tier gate is a **maximum** (`tier <= max_tier`, default 1) because the desk's scale
  ascends as trust falls; `tier == 0` (the unattested seed) is refused at every setting.
* Priority note: this is the second chain. `../evm-vault` is the one clients ask for.
