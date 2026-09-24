# Build status — 24 September 2026

The program and its tests are written; **the build and the test run had not completed when
this was committed.** Recorded here rather than implied, because an unrun test suite is not
a passing one.

## Toolchain
- Windows has `cargo` and `anchor` but **not** `cargo-build-sbf`, so `anchor build` fails
  there with `no such command: build-sbf`.
- WSL Ubuntu has the full set: `cargo`, `anchor`, `solana` and `cargo-build-sbf` under
  `~/.local/share/solana/install/active_release/bin`. Build and test from there:

```bash
wsl -e bash -lc 'cd /mnt/c/CrossDesk/canton-dvp-settlement-desk/solana-vault \
  && export PATH="$HOME/.local/share/solana/install/active_release/bin:$HOME/.cargo/bin:$PATH" \
  && anchor build && anchor test'
```

## What to check when it runs
- `post_nav` verifies K-of-N Ed25519 signatures through instruction introspection, and the
  signed message carries `fixing_ref` (the Canton attestation this projects) and `tier`.
- The tier gate is a **maximum** (`tier <= max_tier`, default 1) and tier 0 is refused at
  every setting. The desk's scale ascends as trust falls — 1 attested, 4 carried forward,
  5 missed — so a minimum would have accepted a stale value as attested.
- Devnet only. `Anchor.toml` pins `cluster = "localnet"`; never point it at mainnet-beta.
