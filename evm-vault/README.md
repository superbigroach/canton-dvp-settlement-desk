# ETP Foundry EVM Basket Vault

`EtpBasketVault` is "basket as a service" for tokenised stocks: an ERC20 share
token backed one-for-one, **in kind**, by a fixed basket of on-chain equities
(xStocks on Base, Robinhood stock tokens, or any allowlisted ERC20). Authorised
participants (APs) create shares by delivering the whole basket in one
transaction and redeem shares by receiving the whole basket in one transaction.
The daily NAV is fixed off-chain by a K-of-N committee on Canton (the ETP
Foundry fixing), signed as EIP-712 typed data, and relayed on-chain by anyone.

This directory is a self-contained Hardhat project. It does not depend on
`backend/`, `frontend/` or the Daml code and nothing there depends on it.

```
evm-vault/
  contracts/
    EtpBasketVault.sol            the vault (ERC20 + ERC20Permit, AccessControl, Pausable, ReentrancyGuard)
    interfaces/IHolderRegistry.sol compliance hook
    mocks/MockERC20.sol            unrestricted constituent for tests
    mocks/MockRestrictedToken.sol  xStocks-style allowlisted constituent
    mocks/MockHolderRegistry.sol   allowlist registry
  scripts/deploy.ts                env-parameterised testnet deploy
  scripts/post-nav.ts              reference relay: ETP Foundry API -> signed fixing -> postNav
  test/EtpBasketVault.test.ts      67 tests
  hardhat.config.ts                solc 0.8.24 / OZ 5 / ethers v6; 10 testnets, mainnet ids refused
```

## Roles and actors

| Actor | On-chain identity | What they can do |
|---|---|---|
| Issuer / sponsor admin | `ADMIN_ROLE` (and `DEFAULT_ADMIN_ROLE`) | fees, fee recipient, registry, attestor committee, rebalance proposals, pause, sweep |
| Authorised participant | `AP_ROLE` | `create` and `redeem` (primary market, in kind) |
| Attestor committee | plain addresses in `setAttestors(list, K)` — **not** a role | sign `NavFixing` off-chain; nothing else |
| Relay | anyone | `postNav` (security is in the signatures, not the caller) |
| Holder registry | `IHolderRegistry` contract, optional | says who may *receive* shares |
| Fee recipient | plain address | receives all fees, in shares |

## Create / redeem flow (in kind)

```
              AP                                   EtpBasketVault                       constituents
               |                                          |                                 |
 create(S, r)  |----------------------------------------->|                                 |
               |                                          | accrue management fee           |
               |   for each constituent i:                |                                 |
               |     amount_i = ceil(units_i * S / 1e18)  |                                 |
               |                                          |-- safeTransferFrom(AP, vault) ->|  (issuer allowlist checked here)
               |                                          |<- balance delta == amount_i ----|
               |                                          | mint fee  -> feeRecipient       |
               |                                          | mint S-fee -> r  (registry gate)|
               |<---------------- Created(...) -----------|                                 |

 redeem(S, r)  |----------------------------------------->|                                 |
               |                                          | accrue management fee           |
               |                                          | transfer fee shares -> feeRecipient
               |                                          | burn S-fee from AP              |
               |   for each constituent i:                |                                 |
               |     amount_i = floor(units_i*(S-fee)/1e18)|-- safeTransfer(r, amount_i) -->|  (issuer allowlist checked here)
               |<---------------- Redeemed(...) ----------|                                 |
```

* **Atomic by construction.** If any single leg reverts — the issuer has not
  whitelisted the vault, an allowance is short, the underlying is paused — the
  entire `create` or `redeem` reverts. The AP is never half-delivered.
* **Rounding.** Inbound amounts round **up**, outbound round **down**. Dust
  stays in the vault and belongs pro rata to remaining holders. Because
  `floor(entitlement) <= balance` always holds, the last holder can always
  exit in full; rounding can never block a redemption.
* **Fees are paid in shares**, never in constituents, so the vault never needs
  a price to settle a fee.
  * `createFeeBps`: deducted from the gross shares; receiver gets `S - fee`, fee
    recipient is minted `fee`. The vault stays fully backed.
  * `redeemFeeBps`: `fee` shares are *transferred* (not burned) to the fee
    recipient; the receiver gets the basket for `S - fee`.
  * `managementFeeBps` per year: standard time-based dilution, accrued on every
    create / redeem / fee change / rebalance / `accrueFees()`. Exact form: after
    an accrual the recipient owns fraction `f = bps * dt / (10000 * 365d)` of
    the supply (`mint = supply * f / (1 - f)`). **Because fee shares are minted
    without any constituents arriving, the per-share basket shrinks by the same
    ratio** (`unitsMultiplier`), keeping `units * totalSupply` — the backing —
    constant. `basket()` reports *effective* units (what an AP moves now);
    `nominalBasket()` reports the epoch's immutable definition. A rebalance
    re-bases the multiplier to 1.

### Backing invariant

For every constituent `i` with effective units `u_i`:

```
balanceOf(vault, i) >= ceil(u_i * totalSupply / 1e18)
```

Create (ceil in), redeem (floor out), share-denominated fees and the
multiplier-adjusted management fee all preserve it; `applyBasket` and
`sweepExcess` check it explicitly.

## How NAV gets from Canton to the vault

```
 Canton fixing committee (K of N)            ETP Foundry API                relay (anyone)                EtpBasketVault
 ------------------------------              ---------------                --------------                --------------
 compute NAV under the rulebook  --commit-->  GET /api/benchmarks/<id>  -->  build NavFixing{           -->  postNav(f, sigs)
 each member signs the EIP-712                last: price, asOf,              instrumentId, asOfDate,          - K distinct registered attestors
 NavFixing on their own signer                tier, k, n, signers,            session, navPerShare,            - EIP-712 domain = this vault + chain
                                              fixingCid                       rulebookVersion, signedAt,       - asOfDate strictly newer (1/day)
                                                                              fixingRef = keccak(fixingCid),   - not in the future
                                                                              tier }                           - fixingRef != 0, tier in 1..maxTier
                                                                              collect K+ signatures            stores latestNav, emits NavPosted
```

`NavFixing` is EIP-712 typed data in the vault's own domain
(`name`, version `"1"`, `chainId`, `verifyingContract`) — the same domain
`permit` uses — so a fixing signed for another vault, another instrument or
another chain is worthless here. Type string:

```
NavFixing(bytes32 instrumentId,uint64 asOfDate,uint8 session,uint256 navPerShare,
          bytes32 rulebookVersion,uint64 signedAt,bytes32 fixingRef,uint8 tier)
```

* `fixingRef` — `keccak256` of the UTF-8 Canton `NavFixing` contract id
  (`last.fixingCid` in the API). Every on-chain posting names the attested
  Canton record it projects; a zero ref is refused. `latestFixingRef()` returns
  the ref and tier behind the current NAV, and both are in `NavPosted`.
* `tier` — the desk's fixing tier from `last.tier`, as defined by
  `SeriesRow.labelFor` in the desk backend: `1` attested, `2` alternate-seats,
  `3` benchmark-x-factor, `4` carried-forward, `5` missed, and anything else
  (including `0`) a seed value. **The scale ascends as trust descends**, so the
  on-chain gate is a MAXIMUM: `postNav` refuses unless `1 <= tier <= maxTier`.
  **`maxTier` defaults to 1** — fully attested only — and only `ADMIN_ROLE` can
  change it, within `1..5` (`setMaxTier`, emits `MaxTierSet`). Raising it is a
  degraded-operations posture. Tier 0 is refused at every setting: a seed value
  is not attested at all, so there is deliberately no way to configure it in.

`postNav` enforces: instrument match; at least `navThreshold` **distinct**
registered attestors (threshold ≥ 2, ≤ N); `asOfDate` strictly greater than
the last accepted fixing (one fixing per day, no replay, no rollback);
`asOfDate` not in the future; `signedAt` within 10 minutes of chain time;
non-zero `fixingRef`; `1 <= tier <= maxTier`. It does **not** judge whether the
number is right — that is the committee's and the rulebook's job.

**NAV is informational for the in-kind flows.** An AP delivers units, not
value, so the vault never prices anything. `navPerShare()` (value, date, age
since posting) and `isNavFresh(maxAge)` exist for consumers that need a price:
a lender taking shares as collateral, a UI, or a future cash-creation
extension. Consumers must check freshness themselves.

`scripts/post-nav.ts` is a **reference relay**: it reads the benchmark from
`https://etpfoundry.com/api/benchmarks/<id>`, takes `fixingRef` from
`last.fixingCid` and `tier` from `last.tier`, **refuses to run when `fixingCid`
is absent** (as it is for seed values) or when the tier falls outside the
vault's accepted `1..maxTier` band, signs with N local keys and submits. Holding
all N keys in one process collapses K-of-N to 1-of-1; in production each
committee member signs on their own key service and the relay only collects.

## Rebalance (in kind, time-locked)

1. `proposeBasket(newConstituents, newUnits)` (ADMIN) — starts `rebalanceDelay`
   (default 2 days, minimum 1 day). One pending proposal at a time;
   `cancelBasketProposal()` clears it.
2. If the **set** of constituents changes, `pause()` the vault. A pure weight
   change with the same tokens may apply unpaused.
3. The admin **brings the delta**: transfer enough of every new / increased
   constituent straight to the vault so it holds
   `>= ceil(newUnits_i * totalSupply / 1e18)` of each.
4. `applyBasket()` — accrues fees, verifies backing for every constituent of
   the new basket (reverts `InsufficientBacking` otherwise), installs it,
   resets `unitsMultiplier` to 1, bumps `epoch`.
5. `sweepExcess(token, to, amount)` retrieves retired constituents (and any
   balance above required backing for current ones — it refuses to dip below).
   Then `unpause()`.

To keep exposure unchanged while re-basing accumulated fee drift, propose the
current *effective* units from `basket()` (freeze the management fee first if
the timelock would otherwise keep shrinking them).

## Compliance: what the vault enforces and what it inherits

* **The vault is infrastructure.** The share token it mints is a security. It
  must be issued by a licensed issuer under its own prospectus / rulebook, and
  the issuer — not this contract — is responsible for who may hold it.
* **Holder restrictions on the shares** are enforced through the optional
  `IHolderRegistry` hook. When set, every mint and transfer requires the
  *receiver* to be allowed; burns are never gated so an exit via an AP is
  always possible. The fee recipient and every creation receiver must be
  allowed, or fee-bearing creates revert (`ReceiverNotAllowed`).
* **Holder restrictions on the underlyings** are inherited automatically: each
  tokenised stock has its own issuer allowlist. The **vault address must be
  whitelisted by every underlying's issuer** before the first create, or
  `create` reverts inside the underlying (`MockRestrictedToken` reproduces
  this as `TransferToNonAllowed(vault)`). A redeem receiver must likewise be
  allowed by every underlying's issuer.
* Secondary-market transfers are **not** paused by `pause()`; freezing holders
  is the registry's job. `pause()` blocks `create` and `redeem`;
  `setRedeemWhilePaused(true)` re-enables `redeem` only (emergency exit:
  holders can leave, nobody can enter).

## Build and test

```bash
cd evm-vault
npm install
npx hardhat compile
npx hardhat test        # 67 passing
```

Toolchain: Solidity 0.8.24 (`evmVersion: cancun` — OZ 5.6's `Bytes.sol` uses
`mcopy`; every configured chain was probed for that opcode on 2026-09-25 via an
`eth_call` state override, with `fe`/INVALID as the control), OpenZeppelin
Contracts 5.x, Hardhat 2.x
with `@nomicfoundation/hardhat-toolbox` 5, ethers v6, TypeScript tests.

## Deploy to Base Sepolia / Arc testnet

Testnets only. `scripts/deploy.ts` refuses mainnet chain ids by design.

```bash
cp .env.example .env      # fill DEPLOYER_PRIVATE_KEY, CONSTITUENTS, UNITS_PER_SHARE, ATTESTORS, ...
npm run deploy:base-sepolia
npm run deploy:arc-testnet
```

* `CONSTITUENTS` are ERC20 addresses that already exist on the target chain
  (the script checks for code and prints each token's decimals).
* `UNITS_PER_SHARE[i]` is the amount of constituent `i` per `1e18` shares, in
  that token's **own base units** (0.5 of a 6-decimal token is `500000`).
* The deployer configures attestors, fees, registry and APs in the same run
  when it is the admin; otherwise it prints the calls the admin must make.
* Output is written to `deployments/<network>.json`.

Networks (`hardhat.config.ts`), every chain id re-verified by `eth_chainId`
against the endpoint listed, and every one probed for the Cancun `mcopy` opcode
(see the `evmVersion` comment in the config for how):

| Network | chainId | RPC used | Mainnet id the scripts refuse |
|---|---|---|---|
| `baseSepolia` | 84532 | `https://sepolia.base.org` | 8453 |
| `arcTestnet` | 5042002 | `https://rpc.testnet.arc.io` | 5042001 |
| `robinhoodTestnet` | 46630 | `https://rpc.testnet.chain.robinhood.com` | 4663 |
| `arbitrumSepolia` | 421614 | `https://sepolia-rollup.arbitrum.io/rpc` | 42161 |
| `bnbTestnet` | 97 | `https://data-seed-prebsc-1-s1.bnbchain.org:8545` | 56 |
| `avalancheFuji` | 43113 | `https://api.avax-test.network/ext/bc/C/rpc` | 43114 |
| `inkSepolia` | 763373 | `https://rpc-gel-sepolia.inkonchain.com` | 57073 |
| `mantleSepolia` | 5003 | `https://rpc.sepolia.mantle.xyz` | 5000 |
| `ethereumSepolia` | 11155111 | `https://ethereum-sepolia-rpc.publicnode.com` | 1 |
| `morphHoodi` | 2910 | `https://rpc-hoodi-bgw.morph.network` | 2818 |

Each entry carries a comment saying why that chain matters commercially. Two
notes worth lifting out of the config:

* **Morph moved its testnet.** `https://rpc-holesky.morphl2.io` no longer
  answers and Morph Holesky (2810) went away with Holesky itself. Morph's own
  development-setup page now documents **Morph Hoodi Testnet, chainId 2910**
  (`https://rpc-hoodi-bgw.morph.network`, explorer
  `https://explorer-hoodi.morph.network`), which settles to **Hoodi** (560048),
  not Sepolia. Verified 2026-09-25: `eth_chainId` returned `0xb5e`. Morph also
  needs an **explicit legacy `gasPrice`** — a type-2 transaction priced off its
  `eth_gasPrice` of 0.001 gwei sits in the mempool for ever.
* Arc uses USDC as native gas — fund the deployer at https://faucet.circle.com.
  Mantle's gas token is **MNT**, not ETH, and its testnet quotes ~50 gwei, so a
  full end-to-end pass there costs ~0.34 MNT rather than a rounding error.

### Deploying to Robinhood Chain

Robinhood Chain is an Arbitrum Orbit L2 (Nitro), so the vault deploys there
unchanged — no code or compiler changes; Nitro has supported the Cancun opcode
set since ArbOS 20. `robinhoodTestnet` is configured with chainId 46630 and
`https://rpc.testnet.chain.robinhood.com` (verified 2026-09-24 by `eth_chainId`
returning `0xb626` and by the chainid.network registry entry; explorer
`https://explorer.testnet.chain.robinhood.com`, bridge from Sepolia via
`https://portal.arbitrum.io/bridge`, docs `https://docs.robinhood.com/chain/`);
mainnet is chainId 4663 and the scripts refuse it. `npm run
deploy:robinhood-testnet` / `npm run post-nav:robinhood-testnet`. The
constituents on that chain are Robinhood's own stock tokens, which are
**EU-holder-restricted** by their issuer: transfers to an address the issuer
has not approved revert inside the token. The **vault address must therefore
be whitelisted by Robinhood's issuer before `create()` can pull them** (and
every redeem receiver must be whitelisted too) — exactly the failure mode
`MockRestrictedToken` reproduces. Pair the vault with an `IHolderRegistry`
that mirrors the same EU eligibility so the basket shares do not end up in
hands that could not hold the underlyings.

Then post a fixing:

```bash
VAULT_ADDRESS=0x... BENCHMARK_ID=LX1 ATTESTOR_PRIVATE_KEYS=0x..,0x.. npm run post-nav:base-sepolia
```

## Deployed and exercised on five testnets

A full end-to-end run (deploy -> configure -> create -> redeem -> two refused
NAV postings -> one attested NAV) has been executed by `scripts/e2e-testnet.ts`
on five chains. Every address, block number and tx hash is in the matching
`deployments/<network>.json`.

| Chain | chainId | Status | Record |
|---|---|---|---|
| Arc Testnet | 5042002 | **deployed + exercised** | [`arcTestnet.json`](deployments/arcTestnet.json) |
| Base Sepolia | 84532 | **deployed + exercised** | [`baseSepolia.json`](deployments/baseSepolia.json) |
| Robinhood Chain Testnet | 46630 | **deployed + exercised** | [`robinhoodTestnet.json`](deployments/robinhoodTestnet.json) |
| Ink Sepolia | 763373 | **deployed + exercised** | [`inkSepolia.json`](deployments/inkSepolia.json) |
| Morph Hoodi Testnet | 2910 | **deployed + exercised** | [`morphHoodi.json`](deployments/morphHoodi.json) |
| BNB Smart Chain Testnet | 97 | configured, **not funded** | — |
| Avalanche Fuji | 43113 | configured, **not funded** | — |
| Mantle Sepolia | 5003 | configured, **not funded** | — |
| Ethereum Sepolia | 11155111 | configured, funded but **not enough gas** | — |
| Arbitrum Sepolia | 421614 | configured, **not funded** | — |

A chain that could not be funded is **not** a chain this was deployed to. See
"Chains that are configured but not yet exercised" below for exactly what
unblocks each one.

**Arc Testnet** — chainId 5042002, explorer base **https://testnet.arcscan.app**
(301-redirects, path intact, to `https://explorer.testnet.arc.io`):

| What | Address |
|---|---|
| `EtpBasketVault` "MOCK ETP Basket (test only)" / `mBSKT` | [`0xF4dAf5BEeEEc381A5f0c0264440d31D8d4bb1451`](https://testnet.arcscan.app/address/0xF4dAf5BEeEEc381A5f0c0264440d31D8d4bb1451) |
| `MockERC20` "MOCK Apple (test only)" / `mAAPL`, 18 dp | [`0xe0B324A4a18065CA7cd775ebd3e7aa35c3842542`](https://testnet.arcscan.app/address/0xe0B324A4a18065CA7cd775ebd3e7aa35c3842542) |
| `MockERC20` "MOCK T-Bill (test only)" / `mTBILL`, 6 dp | [`0xA1E23ACCD39B0884Ec1E54d94e336773F7E237C9`](https://testnet.arcscan.app/address/0xA1E23ACCD39B0884Ec1E54d94e336773F7E237C9) |

**Base Sepolia** — chainId 84532, explorer base **https://sepolia.basescan.org**:

| What | Address |
|---|---|
| `EtpBasketVault` "MOCK ETP Basket (test only)" / `mBSKT` | [`0xcdd1B8e6b2406F45c7177E6916f3f66CA6503e63`](https://sepolia.basescan.org/address/0xcdd1B8e6b2406F45c7177E6916f3f66CA6503e63) |
| `MockERC20` "MOCK Apple (test only)" / `mAAPL`, 18 dp | [`0xe8CF891AcA30dAb742AC3F38c9C076ea1f8F8B93`](https://sepolia.basescan.org/address/0xe8CF891AcA30dAb742AC3F38c9C076ea1f8F8B93) |
| `MockERC20` "MOCK T-Bill (test only)" / `mTBILL`, 6 dp | [`0x3FF4adFEb818Da7c5E6550b6c3307cE60c183B99`](https://sepolia.basescan.org/address/0x3FF4adFEb818Da7c5E6550b6c3307cE60c183B99) |

**Robinhood Chain Testnet** — chainId 46630, explorer base
**https://explorer.testnet.chain.robinhood.com**:

| What | Address |
|---|---|
| `EtpBasketVault` "MOCK ETP Basket (test only)" / `mBSKT` | [`0xF4dAf5BEeEEc381A5f0c0264440d31D8d4bb1451`](https://explorer.testnet.chain.robinhood.com/address/0xF4dAf5BEeEEc381A5f0c0264440d31D8d4bb1451) |
| `MockERC20` "MOCK Apple (test only)" / `mAAPL`, 18 dp | [`0xe0B324A4a18065CA7cd775ebd3e7aa35c3842542`](https://explorer.testnet.chain.robinhood.com/address/0xe0B324A4a18065CA7cd775ebd3e7aa35c3842542) |
| `MockERC20` "MOCK T-Bill (test only)" / `mTBILL`, 6 dp | [`0xA1E23ACCD39B0884Ec1E54d94e336773F7E237C9`](https://explorer.testnet.chain.robinhood.com/address/0xA1E23ACCD39B0884Ec1E54d94e336773F7E237C9) |

**Ink Sepolia** — chainId 763373, explorer base
**https://explorer-sepolia.inkonchain.com**. Kraken's OP Stack L2; funded by
`scripts/fund-ink-sepolia.ts`, which deposits 0.0003 ETH through **Ink's own
`L1StandardBridge` on Ethereum Sepolia**, `0x33f60714bbd74d62b66d79213c348614de51901c` (read off
https://docs.inkonchain.com/useful-information/contracts and then checked on
chain: `version()` = `2.8.2`, `otherBridge()` = `0x42..0010`, `messenger()` =
the `L1CrossDomainMessenger` the same page lists). L1 deposit tx
[`0x8d87c909…e70fa6`](https://sepolia.etherscan.io/tx/0x8d87c90950acb30e4be337eb3dfe7b9a1aadd31376d29414f6d83e9aa6e70fa6),
credited on L2 in about 70 seconds. The whole run then cost **0.00024 ETH**:

| What | Address |
|---|---|
| `EtpBasketVault` "MOCK ETP Basket (test only)" / `mBSKT` | [`0xF4dAf5BEeEEc381A5f0c0264440d31D8d4bb1451`](https://explorer-sepolia.inkonchain.com/address/0xF4dAf5BEeEEc381A5f0c0264440d31D8d4bb1451) |
| `MockERC20` "MOCK Apple (test only)" / `mAAPL`, 18 dp | [`0xe0B324A4a18065CA7cd775ebd3e7aa35c3842542`](https://explorer-sepolia.inkonchain.com/address/0xe0B324A4a18065CA7cd775ebd3e7aa35c3842542) |
| `MockERC20` "MOCK T-Bill (test only)" / `mTBILL`, 6 dp | [`0xA1E23ACCD39B0884Ec1E54d94e336773F7E237C9`](https://explorer-sepolia.inkonchain.com/address/0xA1E23ACCD39B0884Ec1E54d94e336773F7E237C9) |

**Morph Hoodi Testnet** — chainId 2910, explorer base
**https://explorer-hoodi.morph.network**. Funded from Morph's own faucet,
https://morph-rails-hoodi.morph.network/faucet, which takes an **address only**
— no account, no captcha — and drips 0.01 ETH per address per hour. The run cost
**0.00025 ETH** at the pinned 0.02 gwei:

| What | Address |
|---|---|
| `EtpBasketVault` "MOCK ETP Basket (test only)" / `mBSKT` | [`0x8C1066c4BDC00Bb70aBD1b32Fa64Ee0E79099A4b`](https://explorer-hoodi.morph.network/address/0x8C1066c4BDC00Bb70aBD1b32Fa64Ee0E79099A4b) |
| `MockERC20` "MOCK Apple (test only)" / `mAAPL`, 18 dp | [`0x424B5BeD028F733761a235f8b7482575a7296C4e`](https://explorer-hoodi.morph.network/address/0x424B5BeD028F733761a235f8b7482575a7296C4e) |
| `MockERC20` "MOCK T-Bill (test only)" / `mTBILL`, 6 dp | [`0x76e61eDF3EB114212672Be16335CFbdA27793bF9`](https://explorer-hoodi.morph.network/address/0x76e61eDF3EB114212672Be16335CFbdA27793bF9) |

On Morph the first two attempts left transactions stranded in the mempool
because ethers priced them as type-2 off an `eth_gasPrice` of 0.001 gwei;
`scripts/unstick-nonce.ts` replaces a stuck nonce with a legacy no-op and exists
for exactly that. One orphaned `MockERC20` from that first attempt sits at
[`0x3FF4adFEb818Da7c5E6550b6c3307cE60c183B99`](https://explorer-hoodi.morph.network/address/0x3FF4adFEb818Da7c5E6550b6c3307cE60c183B99)
and is **not** part of the recorded basket.

Identical configuration on every chain: one share (`1e18` units of `mBSKT`)
represents **2 mAAPL + 100 mTBILL**, i.e. `unitsPerShare =
[2000000000000000000, 100000000]` - 18 dp for the first, **6 dp for the second**,
each in that token's own base units. Committee: the three attestors from `.env`,
threshold 2. All fees 0. `maxTier` left at the default 1 (`TIER_ATTESTED`). The
`instrumentId` differs per chain (`keccak256("MOCK-ETP-ARC-TESTNET-DEMO")`,
`keccak256("MOCK-ETP-BASE-SEPOLIA-DEMO")`, `keccak256("MOCK-ETP-inkSepolia-DEMO")`,
`keccak256("MOCK-ETP-morphHoodi-DEMO")` and so on) so each record is
self-describing. A fixing could not cross chains anyway — the EIP-712 domain
binds `chainId` and `verifyingContract`.

What every run proved, asserted on-chain rather than assumed: `create(1000)`
moved exactly 2,000 mAAPL + 100,000 mTBILL into the vault and minted exactly
1,000 `mBSKT`; `redeem(250)` returned exactly 500 mAAPL + 25,000 mTBILL in kind
and burned exactly 250 shares, leaving the vault with 1,500 mAAPL + 75,000
mTBILL against 750 shares; a `postNav` carrying 1 of 2 required signatures
reverted `InsufficientSignatures(1, 2)` and one carrying `tier = 0` reverted
`TierNotAccepted(0, 1)` (each landed on-chain as a `status: 0` transaction, so
the gate is visible in the explorer and not only in an `eth_call`); a 2-of-3
signed fixing then posted and reads back as `navPerShare() = 1234.56` with
`latestFixingRef() = (<the test ref>, tier 1)`.

Re-run it (it deploys a **fresh** set each time - it does not reuse the above):

```bash
cd evm-vault
npx hardhat run scripts/e2e-testnet.ts --network arcTestnet
npx hardhat run scripts/e2e-testnet.ts --network baseSepolia
npx hardhat run scripts/e2e-testnet.ts --network robinhoodTestnet
npx hardhat run scripts/e2e-testnet.ts --network inkSepolia
npx hardhat run scripts/e2e-testnet.ts --network morphHoodi
```

Needs `DEPLOYER_PRIVATE_KEY`, `ATTESTOR_PRIVATE_KEYS` (at least `NAV_THRESHOLD`
of them) and `NAV_THRESHOLD` in `.env`, plus native gas in the deployer. Arc's
gas token is USDC (faucet https://faucet.circle.com) and the run above cost
about 0.20 of it; Base Sepolia's is ETH and the run cost about 0.00008 ETH.
`CONSTITUENTS` / `UNITS_PER_SHARE` are **not** read: the script deploys its own
mocks so the run is self-contained. It refuses mainnet chain ids exactly like
`deploy.ts` and `post-nav.ts`.

Two things the runs taught us about the endpoints. `https://sepolia.base.org` is
a **pool of load-balanced nodes**: a `latest` read issued immediately after a
receipt can hit a node that has not applied that block and return the previous
value (a `create()` that mined with status 1 and four logs was followed by
`balanceOf(vault) == 0`, and `AP_ROLE()` once returned `0x`). Every assertion in
the script therefore re-reads until it settles or a 60s deadline passes, and
receipts are awaited by polling, never by re-sending. A first Base Sepolia
attempt aborted on exactly that stale read, so the deployer also owns an earlier,
**abandoned** Base Sepolia set (vault `0xF4dAf5BEeEEc381A5f0c0264440d31D8d4bb1451`,
created 1,000 shares, no NAV ever posted) - `deployments/baseSepolia.json` records
the complete second run, not that one.

### Chains that are configured but not yet exercised

One end-to-end pass is **6,853,599 gas** (measured by summing the receipts of
the Base Sepolia run), so the gas budget per chain is that number times its gas
price. The deployer is `0x10e3868214DAe0479276fa8d2082FB7805aeb647` on every
chain. As of 2026-09-25:

| Chain | Balance | A run needs | Why it is not funded | What unblocks it |
|---|---|---|---|---|
| BNB Smart Chain Testnet (97) | 0 BNB | ~0.00069 BNB @ 0.1 gwei | The faucet is the stock go-ethereum faucet service (WebSocket `wss://testnet.bnbchain.org/faucet-smart/api`, address only, no captcha token in its payload) and it answered `insufficient BNB on BSC mainnet (require >=0.002BNB)` | Send at least **0.002 real BNB to the deployer on BSC mainnet** (chainId 56), then claim at https://www.bnbchain.org/en/testnet-faucet. No account or captcha is needed once that balance exists |
| Avalanche Fuji (43113) | 0 AVAX | ~7e-11 AVAX (base fee is 10 wei) | The only official faucet is the Avalanche Builder Hub console, which requires the Core Wallet extension, a connected wallet **and** a logged-in Builder Hub account; `core.app`'s faucet requires Connect Wallet | Claim to the deployer at https://build.avax.network/console/primary-network/faucet after logging in — or send it **any dust AVAX at all** from another wallet; the run itself is essentially free |
| Mantle Sepolia (5003) | 0 MNT | ~0.34 MNT @ 50 gwei | https://faucet.mantle.xyz shows only "Connect your wallet to claim testnet MNT" — there is no address field to POST to | Connect a wallet at https://faucet.mantle.xyz and claim **at least ~0.4 MNT** to the deployer (claim repeatedly if the drip is smaller) |
| Ethereum Sepolia (11155111) | 0.000441 ETH | ~0.0073 ETH @ 1.07 gwei | Funded, but two orders of magnitude short. Every reachable faucet is gated: pk910's PoW faucet refused with `[INVALID_CAPTCHA] captcha check failed: invalid token`, Stakely wants reCAPTCHA **and** a public tweet, Google Cloud / Alchemy / QuickNode want accounts | Top the deployer up to **~0.01 ETH on Sepolia** from any faucet you can log in to. That also unblocks Arbitrum Sepolia and any future OP Stack L2, which bridge from here |
| Arbitrum Sepolia (421614) | 0 ETH | ~0.0007 ETH @ 0.1 gwei | Nothing to bridge with — the Sepolia balance went into the Ink deposit | Fund Sepolia first (row above), then bridge via https://portal.arbitrum.io/bridge, or use any Arbitrum Sepolia faucet |

The Sepolia balance started at 0.001399 ETH: 0.0003 went across the Ink bridge
and 0.000658 paid the L1 deposit's gas (`depositETH` burns ~646k L1 gas, because
the `OptimismPortal` charges up-front for the L2 gas the message buys — it is
not the ~50k a naive estimate suggests).

> **WARNING - this is a testnet demonstration, never a real basket.** The two
> constituents are `MockERC20` tokens with open minting, deployed and minted by
> the script itself; no real tokenised equity exists on Arc Testnet, Base
> Sepolia, Robinhood Chain Testnet, Ink Sepolia or Morph Hoodi, and nothing here
> is Apple stock or a Treasury bill. The NAV posted is
> an invented number and its `fixingRef` is a synthesised string
> (`TEST-FIXING-NOT-A-CANTON-ATTESTATION:...`), **not** a Canton attestation -
> the live desk currently publishes only tier-0 seed values with a null
> `fixingCid`, which `scripts/post-nav.ts` correctly refuses to relay. All three
> attestor keys live in one `.env`, which collapses K-of-N to 1-of-1 and is fine
> only because this is a demonstration. The share token is a security: a real
> basket goes live only under a licensed issuer's own deployment procedure, with
> real constituents, a committee that holds its own keys, and an
> `IHolderRegistry`.

## Not built (deliberately)

* **Cash creation / redemption** — would need the NAV to price a USDC leg,
  plus an execution venue for the underlying. The `postNav` path is the input
  it would consume.
* **Solana program** — xStocks also exist on Solana; this is EVM only.
* **Cross-chain messaging** — no bridge, no CCIP/LayerZero; one vault per
  chain, each fed by the same committee.
* **Mainnet deployment path / timelocked admin / upgradeability** — the
  contract is non-upgradeable and the admin is a plain role. A production
  deployment puts `ADMIN_ROLE` behind a multisig + timelock; that wiring is
  the issuer's, not this repo's.
* **Production key custody for attestors** — `post-nav.ts` holds keys locally
  for demonstration only.

## Design decisions the brief left open

* Management fee dilution vs. fixed units are incompatible; resolved with the
  `unitsMultiplier` (streaming-fee position multiplier) so the backing
  invariant holds and the nominal epoch definition stays immutable.
* Fee caps: create/redeem ≤ 5 %, management ≤ 10 % p.a.; a single accrual
  charges at most 365 days of elapsed time.
* Redeem fee is transferred to the fee recipient rather than burned, so the
  sponsor (not remaining holders) receives its backing.
* `create` verifies the balance delta of each leg, refusing fee-on-transfer or
  rebasing underlyings (`UnderDelivered`).
* `applyBasket` enforces backing for the new basket on-chain instead of
  trusting the admin; `sweepExcess` is the only way constituents leave the
  vault outside `redeem` and it refuses to breach backing.
* Signatures may be supplied in any order; duplicates are detected on-chain
  (O(n²) over ≤ 32 attestors) so relays need not sort.
* `navPerShare()` reverts when nothing has been posted so a consumer can never
  read `0` as a price; `isNavFresh` returns `false` in that case.
* Additional `postNav` guards beyond the brief: `asOfDate` not in the future,
  `signedAt` ≤ chain time + 10 min, `navPerShare != 0`, instrument match.
