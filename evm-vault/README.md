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
  test/EtpBasketVault.test.ts      58 tests
  hardhat.config.ts                solc 0.8.24 / OZ 5 / ethers v6; baseSepolia + arcTestnet + robinhoodTestnet
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
npx hardhat test        # 58 passing
```

Toolchain: Solidity 0.8.24 (`evmVersion: cancun` — OZ 5.6's `Bytes.sol` uses
`mcopy`; Base and Arc both support it), OpenZeppelin Contracts 5.x, Hardhat 2.x
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

Networks (`hardhat.config.ts`): `baseSepolia` chainId 84532
(`https://sepolia.base.org`); `arcTestnet` chainId 5042002
(`https://rpc.testnet.arc.io`, per https://docs.arc.network; also reachable at
`https://rpc.testnet.arc.network`; both verified to return `0x4cef52` on
2026-09-24). Arc uses USDC as native gas — fund the deployer at
https://faucet.circle.com.

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
