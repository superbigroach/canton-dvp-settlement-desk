# Build status — 24 September 2026

**The program and its tests are written. They have never compiled.** Recorded plainly,
because an unrun test suite is not a passing one and nobody should read this directory as
verified work.

## What was tried

| Where | Result |
|---|---|
| Windows, `anchor build` | `error: no such command: build-sbf` — Windows has `cargo` and `anchor` but not the Solana platform tools |
| WSL, on `/mnt/c/...` | every crate fails, including trivial ones (`itoa`, `cfg-if`) |
| WSL, copied into the Linux filesystem (`~/sv-build`) | same, with the real diagnostic: **`error[E0463]: can't find crate for 'core'` … `the sbf-solana-solana target may not be installed`** |

## The actual blocker

The toolchain is installed and self-consistent:

```
solana-cli 2.1.22 (Agave)   solana-cargo-build-sbf 2.1.22
platform-tools v1.43        rustc 1.79.0   (the sysroot used for the sbf target)
```

The failure is at the **build scripts of ordinary dependencies**, not in our code, which
means the dependency graph resolved by Anchor 0.31.1 wants a newer Rust than the 1.79 that
platform-tools v1.43 carries. This is a toolchain version problem, not a program problem.

## What to do when this is picked up

1. Upgrade the Solana CLI (Agave 2.2 or 3.x ships newer platform-tools), then
   `cargo-build-sbf --force-tools-install`; or
2. pin the dependency graph to what rustc 1.79 accepts (`cargo update -p <crate> --precise`),
   which is the smaller change but has to be redone on every bump.

Then, from the Linux filesystem, never `/mnt/c`:

```bash
wsl -e bash -lc 'export PATH="$HOME/.local/share/solana/install/active_release/bin:$HOME/.cargo/bin:$PATH" \
  && cd ~/sv-build && anchor build && anchor test'
```

## What to check once it runs

- `post_nav` verifies K-of-N Ed25519 signatures through instruction introspection, and the
  signed message carries `fixing_ref` (the Canton attestation it projects) and `tier`.
- The tier gate is a **maximum** (`tier <= max_tier`, default 1); tier 0, the unattested
  seed, is refused at every setting. The desk's scale ascends as trust falls — 1 attested,
  4 carried forward, 5 missed — so a minimum would have accepted a stale value as attested.
- Devnet only. `Anchor.toml` pins `cluster = "localnet"`; never point it at mainnet-beta.

**Priority note.** This is the second chain, not the first. Tokenised equities live on Base,
Arc and Robinhood Chain today, and `../evm-vault` covers those with 67 passing tests. Solana
is worth finishing when a client asks for it.
