/**
 * End-to-end TESTNET exercise of EtpBasketVault, in one pass.
 *
 *   npx hardhat run scripts/e2e-testnet.ts --network arcTestnet
 *   npx hardhat run scripts/e2e-testnet.ts --network baseSepolia
 *
 * WHAT IT DOES (every step prints a labelled line with its tx hash):
 *   1. deploys two MockERC20 constituents — no real tokenised equity exists on
 *      these testnets, so the basket is deliberately named "MOCK ... (test only)"
 *      and nobody can mistake it for the real thing;
 *   2. deploys EtpBasketVault over those two mocks;
 *   3. setAttestors(3 addresses, threshold 2) + grants AP_ROLE to the deployer,
 *      fees left at 0 for a clean first run;
 *   4. mints + approves + create()s a round number of shares, then ASSERTS the
 *      exact vault holdings and share balance on-chain;
 *   5. redeem()s a portion and asserts the exact in-kind return;
 *   6. proves the negative path first (1-of-2 signatures, and tier 0) by
 *      capturing the real revert reasons from the chain;
 *   7. posts a 2-of-3 attested EIP-712 NavFixing and reads navPerShare() and
 *      latestFixingRef() back;
 *   8. writes deployments/<network>.json (superset of deploy.ts's record shape).
 *
 * Required env (see .env.example): DEPLOYER_PRIVATE_KEY, ATTESTOR_PRIVATE_KEYS
 * (>= NAV_THRESHOLD of them), NAV_THRESHOLD. CONSTITUENTS / UNITS_PER_SHARE are
 * NOT read: this script deploys its own mocks so the run is self-contained.
 *
 * Refuses to run against a mainnet chain id, exactly like deploy.ts. This is a
 * demonstration harness; the share token is a security and goes live only under
 * a licensed issuer's own deployment procedure.
 */
import { ethers, network } from "hardhat";
import type { Interface, Wallet } from "ethers";
import * as fs from "fs";
import * as path from "path";

// Ethereum, Base, Arbitrum One, Polygon, Optimism, Arc mainnet, Robinhood Chain.
const MAINNET_CHAIN_IDS = new Set<bigint>([1n, 8453n, 42161n, 137n, 10n, 5042001n, 4663n]);

/** Explorer bases, keyed by chain id (same sources as hardhat.config.ts). */
const EXPLORERS: Record<string, string> = {
  "5042002": "https://testnet.arcscan.app", // 301-redirects to https://explorer.testnet.arc.io
  "84532": "https://sepolia.basescan.org",
  "46630": "https://explorer.testnet.chain.robinhood.com",
};

// ---------------------------------------------------------------------------
// The mock basket. 18 dp and 6 dp on purpose: a 6-decimal constituent is where
// a units-per-share mistake usually hides.
// ---------------------------------------------------------------------------
const MOCK_A = { name: "MOCK Apple (test only)", symbol: "mAAPL", decimals: 18 };
const MOCK_B = { name: "MOCK T-Bill (test only)", symbol: "mTBILL", decimals: 6 };

const VAULT_NAME = "MOCK ETP Basket (test only)";
const VAULT_SYMBOL = "mBSKT";
/**
 * Instrument id label, per network. A fixing can never cross chains anyway (the
 * EIP-712 domain binds chainId + verifyingContract), but a distinct id per chain
 * keeps each deployment record self-describing. The arcTestnet entry is PINNED to
 * the label already deployed at 0xF4dAf5BEeEEc381A5f0c0264440d31D8d4bb1451 so a
 * re-run reproduces that vault's instrumentId exactly.
 */
const INSTRUMENT_LABELS: Record<string, string> = {
  arcTestnet: "MOCK-ETP-ARC-TESTNET-DEMO",
  baseSepolia: "MOCK-ETP-BASE-SEPOLIA-DEMO",
  robinhoodTestnet: "MOCK-ETP-ROBINHOOD-TESTNET-DEMO",
};

/** One share = 2 mAAPL + 100 mTBILL, each in the token's OWN base units. */
const UNITS_PER_SHARE = [
  ethers.parseUnits("2", MOCK_A.decimals), //   2.000000000000000000 mAAPL per share
  ethers.parseUnits("100", MOCK_B.decimals), // 100.000000 mTBILL per share
];

const SHARES_TO_CREATE = ethers.parseUnits("1000", 18); // 1,000 shares
const SHARES_TO_REDEEM = ethers.parseUnits("250", 18); //    250 shares

/** A NAV number for the demonstration only — nothing values this mock basket. */
const TEST_NAV_PER_SHARE = ethers.parseUnits("1234.56", 18);

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------
function opt(name: string, fallback = ""): string {
  const v = process.env[name];
  return v && v.trim() !== "" ? v.trim() : fallback;
}
function req(name: string): string {
  const v = opt(name);
  if (!v) throw new Error(`Missing env ${name}`);
  return v;
}
function list(name: string): string[] {
  return opt(name)
    .split(",")
    .map((s) => s.trim())
    .filter((s) => s.length > 0);
}
function toBytes32(v: string): string {
  return ethers.isHexString(v, 32) ? v : ethers.keccak256(ethers.toUtf8Bytes(v));
}
function fmt(x: bigint, decimals: number): string {
  return ethers.formatUnits(x, decimals);
}

const txs: { step: string; hash: string; block: number | null }[] = [];
function note(step: string, hash: string, block: number | null) {
  txs.push({ step, hash, block });
}

/** Read-only retry: testnet RPCs drop requests; a view call is always safe to repeat. */
async function retryRead<T>(label: string, fn: () => Promise<T>, attempts = 4): Promise<T> {
  let last: unknown;
  for (let i = 1; i <= attempts; i++) {
    try {
      return await fn();
    } catch (e) {
      last = e;
      console.log(`  (retry ${i}/${attempts} on ${label}: ${(e as Error).message?.slice(0, 120)})`);
      await new Promise((r) => setTimeout(r, 2_000 * i));
    }
  }
  throw last;
}

/**
 * Wait for a receipt by POLLING, never by re-sending. A dropped RPC response
 * must not turn into a second transaction.
 */
async function waitMined(hash: string, tries = 60): Promise<{ blockNumber: number; status: number }> {
  for (let i = 0; i < tries; i++) {
    try {
      const r = await ethers.provider.getTransactionReceipt(hash);
      if (r) return { blockNumber: r.blockNumber, status: Number(r.status) };
    } catch {
      /* transient RPC error: keep polling */
    }
    await new Promise((r) => setTimeout(r, 2_000));
  }
  throw new Error(`Transaction ${hash} not mined after ${tries * 2}s`);
}

/**
 * Assert an on-chain value, RE-READING until it matches or the deadline passes.
 *
 * WHY re-read rather than read once: a public testnet RPC endpoint is a pool of
 * load-balanced nodes, so a `latest` read issued straight after a receipt can
 * land on a node that has not applied that block yet and return the PREVIOUS
 * value. Observed on https://sepolia.base.org on 2026-09-25: `create()` mined
 * with status 1 and four logs, and the very next `balanceOf(vault)` returned 0.
 * A genuine mismatch still fails — it just takes `timeoutMs` to say so.
 */
async function assertEq(
  label: string,
  get: () => Promise<bigint>,
  expected: bigint,
  decimals: number,
  timeoutMs = 60_000
) {
  const deadline = Date.now() + timeoutMs;
  let actual: bigint | null = null;
  let reads = 0;
  for (;;) {
    reads++;
    try {
      actual = await get();
      if (actual === expected) break;
    } catch (e) {
      console.log(`  (read error on ${label}: ${(e as Error).message?.slice(0, 100)})`);
    }
    if (Date.now() >= deadline) break;
    await new Promise((r) => setTimeout(r, 2_000));
  }
  const ok = actual === expected;
  const shown = actual === null ? "(unreadable)" : `${fmt(actual, decimals)} (raw ${actual})`;
  console.log(
    `  ${ok ? "ASSERT ok " : "ASSERT FAIL"}  ${label}: ${shown}` +
      (ok
        ? reads > 1
          ? ` [settled after ${reads} reads]`
          : ""
        : ` — EXPECTED ${fmt(expected, decimals)} (raw ${expected})`)
  );
  if (!ok) throw new Error(`Assertion failed: ${label} was ${actual}, expected ${expected} after ${reads} reads`);
}

/** Pull a custom-error name + args out of whatever shape the RPC hands back. */
function decodeRevert(iface: Interface, err: unknown): string {
  const e = err as {
    message?: string;
    shortMessage?: string;
    reason?: string;
    data?: unknown;
    info?: { error?: { data?: unknown } };
    error?: { data?: unknown };
    revert?: { name: string; args: readonly unknown[] } | null;
  };
  if (e?.revert?.name) {
    return `${e.revert.name}(${e.revert.args.map((a) => String(a)).join(", ")})`;
  }
  const candidates: unknown[] = [e?.data, e?.info?.error?.data, e?.error?.data];
  for (const c of candidates) {
    const hex =
      typeof c === "string" ? c : typeof (c as { data?: string })?.data === "string" ? (c as { data: string }).data : null;
    if (hex && ethers.isHexString(hex) && hex.length >= 10) {
      try {
        const parsed = iface.parseError(hex);
        if (parsed) return `${parsed.name}(${parsed.args.map((a: unknown) => String(a)).join(", ")})`;
      } catch {
        /* fall through to the raw message */
      }
    }
  }
  return e?.shortMessage || e?.reason || e?.message || String(err);
}

// ---------------------------------------------------------------------------
// Main
// ---------------------------------------------------------------------------
async function main() {
  const { chainId } = await retryRead("getNetwork", () => ethers.provider.getNetwork());
  if (MAINNET_CHAIN_IDS.has(chainId)) {
    throw new Error(`Refusing to run against mainnet chainId ${chainId}. Testnets only.`);
  }
  const [deployer] = await ethers.getSigners();
  if (!deployer) throw new Error("No signer: set DEPLOYER_PRIVATE_KEY or MNEMONIC");

  const threshold = BigInt(opt("NAV_THRESHOLD", "2"));
  const attestorKeys = req("ATTESTOR_PRIVATE_KEYS")
    .split(",")
    .map((k) => k.trim())
    .filter(Boolean);
  // Derive the addresses from the keys so the committee we register is provably
  // the committee we can sign with. Never print or log a key.
  const attestorWallets = attestorKeys.map((k) => new ethers.Wallet(k));
  const attestors = attestorWallets.map((w) => w.address);
  if (BigInt(attestors.length) < threshold) {
    throw new Error(`Only ${attestors.length} attestor keys for threshold ${threshold}`);
  }
  const declared = list("ATTESTORS").map((a) => ethers.getAddress(a));
  if (declared.length) {
    const missing = attestors.filter((a) => !declared.some((d) => d.toLowerCase() === a.toLowerCase()));
    if (missing.length) {
      throw new Error(`ATTESTOR_PRIVATE_KEYS derive ${missing.join(",")}, which are absent from ATTESTORS`);
    }
  }

  const bal = await retryRead("getBalance", () => ethers.provider.getBalance(deployer.address));
  const startBlock = await retryRead("getBlockNumber", () => ethers.provider.getBlockNumber());

  console.log("=".repeat(78));
  console.log(`ETP Foundry basket vault — END-TO-END on ${network.name} (chainId ${chainId})`);
  console.log(`  deployer     ${deployer.address}`);
  console.log(`  gas balance  ${ethers.formatEther(bal)} (native gas token)`);
  console.log(`  start block  ${startBlock}`);
  console.log(`  explorer     ${EXPLORERS[chainId.toString()] ?? "(unknown for this chain id)"}`);
  console.log(`  committee    ${attestors.join(", ")}  threshold ${threshold}`);
  console.log("  NOTE: the constituents below are MOCKS minted by this script. No real");
  console.log("        tokenised equity exists on this testnet. This is a demonstration.");
  console.log("=".repeat(78));

  // ---------------------------------------------------------------- step 1
  console.log("\n[1] Deploy the two MOCK constituents");
  const Mock = await ethers.getContractFactory("MockERC20");

  const tokenA = await Mock.deploy(MOCK_A.name, MOCK_A.symbol, MOCK_A.decimals);
  const aDeployTx = tokenA.deploymentTransaction()!;
  console.log(`  tx ${aDeployTx.hash}  deploying ${MOCK_A.symbol}`);
  await tokenA.waitForDeployment();
  const tokenAAddr = await tokenA.getAddress();
  const aRc = await waitMined(aDeployTx.hash);
  note("deploy MockERC20 mAAPL", aDeployTx.hash, aRc.blockNumber);
  console.log(`  ${MOCK_A.symbol} "${MOCK_A.name}" ${MOCK_A.decimals} dp at ${tokenAAddr} (block ${aRc.blockNumber})`);

  const tokenB = await Mock.deploy(MOCK_B.name, MOCK_B.symbol, MOCK_B.decimals);
  const bDeployTx = tokenB.deploymentTransaction()!;
  console.log(`  tx ${bDeployTx.hash}  deploying ${MOCK_B.symbol}`);
  await tokenB.waitForDeployment();
  const tokenBAddr = await tokenB.getAddress();
  const bRc = await waitMined(bDeployTx.hash);
  note("deploy MockERC20 mTBILL", bDeployTx.hash, bRc.blockNumber);
  console.log(`  ${MOCK_B.symbol} "${MOCK_B.name}" ${MOCK_B.decimals} dp at ${tokenBAddr} (block ${bRc.blockNumber})`);

  // ---------------------------------------------------------------- step 2
  console.log("\n[2] Deploy EtpBasketVault over that basket");
  const instrumentLabel = INSTRUMENT_LABELS[network.name] ?? `MOCK-ETP-${network.name}-DEMO`;
  const instrumentId = toBytes32(instrumentLabel);
  console.log(`  instrumentId keccak256("${instrumentLabel}") = ${instrumentId}`);
  console.log(
    `  ONE SHARE (1e18 units of ${VAULT_SYMBOL}) REPRESENTS: ` +
      `${fmt(UNITS_PER_SHARE[0], MOCK_A.decimals)} ${MOCK_A.symbol} + ${fmt(UNITS_PER_SHARE[1], MOCK_B.decimals)} ${MOCK_B.symbol}`
  );
  console.log(
    `  unitsPerShare (per SHARE_UNIT = 1e18 shares, in each token's own base units) = ` +
      `[${UNITS_PER_SHARE[0]} (18 dp), ${UNITS_PER_SHARE[1]} (6 dp)]`
  );
  const Vault = await ethers.getContractFactory("EtpBasketVault");
  const vault = await Vault.deploy({
    name: VAULT_NAME,
    symbol: VAULT_SYMBOL,
    instrumentId,
    admin: deployer.address,
    feeRecipient: deployer.address,
    constituents: [tokenAAddr, tokenBAddr],
    unitsPerShare: UNITS_PER_SHARE,
  });
  const vDeployTx = vault.deploymentTransaction()!;
  console.log(`  tx ${vDeployTx.hash}  deploying ${VAULT_SYMBOL}`);
  await vault.waitForDeployment();
  const vaultAddr = await vault.getAddress();
  const vRc = await waitMined(vDeployTx.hash);
  note("deploy EtpBasketVault", vDeployTx.hash, vRc.blockNumber);
  console.log(`  EtpBasketVault "${VAULT_NAME}" (${VAULT_SYMBOL}) at ${vaultAddr} (block ${vRc.blockNumber})`);

  // ---------------------------------------------------------------- step 3
  console.log("\n[3] Configure: attestors, AP_ROLE, fees left at 0");
  const setAttTx = await vault.setAttestors(attestors, threshold);
  console.log(`  tx ${setAttTx.hash}  setAttestors([${attestors.join(", ")}], ${threshold})`);
  const setAttRc = await waitMined(setAttTx.hash);
  note(`setAttestors(3, threshold ${threshold})`, setAttTx.hash, setAttRc.blockNumber);

  const AP_ROLE = await retryRead("AP_ROLE", () => vault.AP_ROLE());
  const grantTx = await vault.grantRole(AP_ROLE, deployer.address);
  console.log(`  tx ${grantTx.hash}  grantRole(AP_ROLE, ${deployer.address})`);
  const grantRc = await waitMined(grantTx.hash);
  note("grantRole(AP_ROLE, deployer)", grantTx.hash, grantRc.blockNumber);

  console.log(
    `  navThreshold=${await retryRead("navThreshold", () => vault.navThreshold())}` +
      ` maxTier=${await retryRead("maxTier", () => vault.maxTier())}` +
      ` createFeeBps=${await retryRead("createFeeBps", () => vault.createFeeBps())}` +
      ` redeemFeeBps=${await retryRead("redeemFeeBps", () => vault.redeemFeeBps())}` +
      ` managementFeeBps=${await retryRead("managementFeeBps", () => vault.managementFeeBps())}`
  );

  // ---------------------------------------------------------------- step 4
  console.log("\n[4] Mint the constituents, approve, and create shares");
  const needA = (UNITS_PER_SHARE[0] * SHARES_TO_CREATE) / 10n ** 18n;
  const needB = (UNITS_PER_SHARE[1] * SHARES_TO_CREATE) / 10n ** 18n;
  // Mint double what the create needs, so the post-redeem balance maths is
  // visible rather than exactly zero.
  const mintA = needA * 2n;
  const mintB = needB * 2n;

  const mintATx = await tokenA.mint(deployer.address, mintA);
  console.log(`  tx ${mintATx.hash}  mint ${fmt(mintA, MOCK_A.decimals)} ${MOCK_A.symbol} to deployer`);
  note(`mint ${MOCK_A.symbol}`, mintATx.hash, (await waitMined(mintATx.hash)).blockNumber);

  const mintBTx = await tokenB.mint(deployer.address, mintB);
  console.log(`  tx ${mintBTx.hash}  mint ${fmt(mintB, MOCK_B.decimals)} ${MOCK_B.symbol} to deployer`);
  note(`mint ${MOCK_B.symbol}`, mintBTx.hash, (await waitMined(mintBTx.hash)).blockNumber);

  const apprATx = await tokenA.approve(vaultAddr, mintA);
  console.log(`  tx ${apprATx.hash}  approve vault for ${MOCK_A.symbol}`);
  note(`approve ${MOCK_A.symbol}`, apprATx.hash, (await waitMined(apprATx.hash)).blockNumber);

  const apprBTx = await tokenB.approve(vaultAddr, mintB);
  console.log(`  tx ${apprBTx.hash}  approve vault for ${MOCK_B.symbol}`);
  note(`approve ${MOCK_B.symbol}`, apprBTx.hash, (await waitMined(apprBTx.hash)).blockNumber);

  console.log("  BEFORE create:");
  await assertEq(`deployer ${MOCK_A.symbol}`, () => tokenA.balanceOf(deployer.address), mintA, MOCK_A.decimals);
  await assertEq(`deployer ${MOCK_B.symbol}`, () => tokenB.balanceOf(deployer.address), mintB, MOCK_B.decimals);
  await assertEq(`vault ${MOCK_A.symbol}`, () => tokenA.balanceOf(vaultAddr), 0n, MOCK_A.decimals);
  await assertEq(`vault ${MOCK_B.symbol}`, () => tokenB.balanceOf(vaultAddr), 0n, MOCK_B.decimals);
  await assertEq(`deployer ${VAULT_SYMBOL}`, () => vault.balanceOf(deployer.address), 0n, 18);
  await assertEq(`${VAULT_SYMBOL} totalSupply`, () => vault.totalSupply(), 0n, 18);

  const [, previewAmts] = await retryRead("previewCreate", () => vault.previewCreate(SHARES_TO_CREATE));
  console.log(
    `  previewCreate(${fmt(SHARES_TO_CREATE, 18)} shares) = ` +
      `${fmt(previewAmts[0], MOCK_A.decimals)} ${MOCK_A.symbol} + ${fmt(previewAmts[1], MOCK_B.decimals)} ${MOCK_B.symbol}`
  );

  const createTx = await vault.create(SHARES_TO_CREATE, deployer.address);
  console.log(`  tx ${createTx.hash}  create(${fmt(SHARES_TO_CREATE, 18)} shares, deployer)`);
  const createRc = await waitMined(createTx.hash);
  note(`create ${fmt(SHARES_TO_CREATE, 18)} shares`, createTx.hash, createRc.blockNumber);

  console.log("  AFTER create:");
  await assertEq(`vault ${MOCK_A.symbol}`, () => tokenA.balanceOf(vaultAddr), needA, MOCK_A.decimals);
  await assertEq(`vault ${MOCK_B.symbol}`, () => tokenB.balanceOf(vaultAddr), needB, MOCK_B.decimals);
  await assertEq(`deployer ${MOCK_A.symbol}`, () => tokenA.balanceOf(deployer.address), mintA - needA, MOCK_A.decimals);
  await assertEq(`deployer ${MOCK_B.symbol}`, () => tokenB.balanceOf(deployer.address), mintB - needB, MOCK_B.decimals);
  await assertEq(`deployer ${VAULT_SYMBOL}`, () => vault.balanceOf(deployer.address), SHARES_TO_CREATE, 18);
  await assertEq(`${VAULT_SYMBOL} totalSupply`, () => vault.totalSupply(), SHARES_TO_CREATE, 18);

  // ---------------------------------------------------------------- step 5
  console.log("\n[5] Redeem a portion, in kind");
  const backA = (UNITS_PER_SHARE[0] * SHARES_TO_REDEEM) / 10n ** 18n;
  const backB = (UNITS_PER_SHARE[1] * SHARES_TO_REDEEM) / 10n ** 18n;
  const [, previewRed] = await retryRead("previewRedeem", () => vault.previewRedeem(SHARES_TO_REDEEM));
  console.log(
    `  previewRedeem(${fmt(SHARES_TO_REDEEM, 18)} shares) = ` +
      `${fmt(previewRed[0], MOCK_A.decimals)} ${MOCK_A.symbol} + ${fmt(previewRed[1], MOCK_B.decimals)} ${MOCK_B.symbol}`
  );
  const redeemTx = await vault.redeem(SHARES_TO_REDEEM, deployer.address);
  console.log(`  tx ${redeemTx.hash}  redeem(${fmt(SHARES_TO_REDEEM, 18)} shares, deployer)`);
  const redeemRc = await waitMined(redeemTx.hash);
  note(`redeem ${fmt(SHARES_TO_REDEEM, 18)} shares`, redeemTx.hash, redeemRc.blockNumber);

  console.log("  AFTER redeem:");
  await assertEq(`vault ${MOCK_A.symbol}`, () => tokenA.balanceOf(vaultAddr), needA - backA, MOCK_A.decimals);
  await assertEq(`vault ${MOCK_B.symbol}`, () => tokenB.balanceOf(vaultAddr), needB - backB, MOCK_B.decimals);
  await assertEq(
    `deployer ${MOCK_A.symbol}`,
    () => tokenA.balanceOf(deployer.address),
    mintA - needA + backA,
    MOCK_A.decimals
  );
  await assertEq(
    `deployer ${MOCK_B.symbol}`,
    () => tokenB.balanceOf(deployer.address),
    mintB - needB + backB,
    MOCK_B.decimals
  );
  await assertEq(
    `deployer ${VAULT_SYMBOL}`,
    () => vault.balanceOf(deployer.address),
    SHARES_TO_CREATE - SHARES_TO_REDEEM,
    18
  );
  await assertEq(
    `${VAULT_SYMBOL} totalSupply`,
    () => vault.totalSupply(),
    SHARES_TO_CREATE - SHARES_TO_REDEEM,
    18
  );

  // -------------------------------------------------- the fixing to be signed
  // Domain, types and field construction reused from scripts/post-nav.ts. The
  // difference: the live desk currently publishes only TIER 0 seed values with a
  // null fixingCid, which post-nav.ts refuses to relay (correctly). So this run
  // synthesises a clearly-labelled TEST reference instead of pretending a
  // Canton attestation exists.
  const nowIso = new Date().toISOString();
  const asOfDate = BigInt(Math.floor(Date.parse(nowIso) / 86_400_000)); // UTC day index
  const session = Number(opt("NAV_SESSION", "0"));
  const rulebookVersion = toBytes32(opt("RULEBOOK_VERSION", "rulebook-v1"));
  const fixingCidPreimage = `TEST-FIXING-NOT-A-CANTON-ATTESTATION:${network.name}:${instrumentLabel}:${nowIso}`;
  const fixingRef = ethers.keccak256(ethers.toUtf8Bytes(fixingCidPreimage));
  const domain = {
    name: await retryRead("name", () => vault.name()),
    version: "1",
    chainId,
    verifyingContract: vaultAddr,
  };
  const types = {
    NavFixing: [
      { name: "instrumentId", type: "bytes32" },
      { name: "asOfDate", type: "uint64" },
      { name: "session", type: "uint8" },
      { name: "navPerShare", type: "uint256" },
      { name: "rulebookVersion", type: "bytes32" },
      { name: "signedAt", type: "uint64" },
      { name: "fixingRef", type: "bytes32" },
      { name: "tier", type: "uint8" },
    ],
  };
  const base = {
    instrumentId,
    asOfDate,
    session,
    navPerShare: TEST_NAV_PER_SHARE,
    rulebookVersion,
    signedAt: BigInt(Math.floor(Date.now() / 1000)),
    fixingRef,
    tier: 1, // TIER_ATTESTED — the only tier the vault accepts by default
  };
  const signWith = async (wallets: Wallet[], f: typeof base) =>
    Promise.all(wallets.map((w) => w.signTypedData(domain, types, f)));

  // ---------------------------------------------------------------- step 6/7
  // Negatives FIRST: once a fixing is accepted for `asOfDate`, any later
  // attempt at the same date reverts StaleFixing and would mask the gate we
  // are trying to demonstrate.
  console.log("\n[6] NEGATIVE PATHS — prove the gates, not just the happy path");
  const negatives: { label: string; reason: string; txHash: string | null }[] = [];

  const runNegative = async (label: string, f: typeof base, sigs: string[]) => {
    let reason = "(no revert — THIS IS A PROBLEM)";
    try {
      await vault.postNav.staticCall(f, sigs);
    } catch (e) {
      reason = decodeRevert(vault.interface, e);
    }
    console.log(`  ${label}\n    reverted with: ${reason}`);
    // Also land it on-chain as a reverted transaction, so the gate is provable
    // from the explorer and not only from an eth_call.
    let txHash: string | null = null;
    try {
      const tx = await vault.postNav(f, sigs, { gasLimit: 400_000 });
      txHash = tx.hash;
      const rc = await waitMined(tx.hash);
      console.log(`    on-chain tx ${tx.hash} status=${rc.status} (0 = reverted) block ${rc.blockNumber}`);
      note(`NEGATIVE ${label} (reverted on-chain)`, tx.hash, rc.blockNumber);
    } catch (e) {
      console.log(`    (could not land a reverted tx: ${(e as Error).message?.slice(0, 160)})`);
    }
    negatives.push({ label, reason, txHash });
  };

  const oneSig = await signWith([attestorWallets[0]], base);
  await runNegative(`postNav with 1 of ${threshold} required signatures`, base, oneSig);

  const tier0 = { ...base, tier: 0 };
  const tier0Sigs = await signWith(attestorWallets.slice(0, Number(threshold)), tier0);
  await runNegative(`postNav with tier = 0 (seed value) and ${threshold} valid signatures`, tier0, tier0Sigs);

  console.log("\n[7] postNav — 2-of-3 attested EIP-712 NavFixing");
  console.log(`  fixingRef preimage: "${fixingCidPreimage}"`);
  console.log(`  fixingRef = keccak256(utf8(above)) = ${fixingRef}`);
  console.log(
    "  THIS IS A TEST REFERENCE, NOT A REAL CANTON ATTESTATION. The live desk currently"
  );
  console.log(
    "  publishes only tier-0 seed values with a null fixingCid, which the vault refuses;"
  );
  console.log("  the string above is synthesised for this demonstration run.");
  console.log(
    `  fixing: asOfDate=${asOfDate} session=${session} navPerShare=${fmt(TEST_NAV_PER_SHARE, 18)} tier=${base.tier} rulebookVersion=${rulebookVersion}`
  );

  const signers2 = attestorWallets.slice(0, 2);
  const goodSigs = await signWith(signers2, base);
  signers2.forEach((w) => console.log(`  signed by attestor ${w.address}`));

  const onchainDigest = await retryRead("hashNavFixing", () => vault.hashNavFixing(base));
  const localDigest = ethers.TypedDataEncoder.hash(domain, types, base);
  if (onchainDigest !== localDigest) throw new Error("Digest mismatch between local encoder and vault; aborting");
  console.log(`  EIP-712 digest agrees local == on-chain: ${onchainDigest}`);

  const navTx = await vault.postNav(base, goodSigs);
  console.log(`  tx ${navTx.hash}  postNav(...)`);
  const navRc = await waitMined(navTx.hash);
  if (navRc.status !== 1) throw new Error(`postNav reverted on-chain in block ${navRc.blockNumber}`);
  note("postNav (2-of-3)", navTx.hash, navRc.blockNumber);

  // Same staleness hazard as the balances: wait for a node that has the fixing.
  await assertEq("navPerShare after postNav", async () => (await vault.navPerShare())[0], TEST_NAV_PER_SHARE, 18);
  const [nav, navAsOf, navAge] = await retryRead("navPerShare", () => vault.navPerShare());
  const [readRef, readTier] = await retryRead("latestFixingRef", () => vault.latestFixingRef());
  console.log(`  vault.navPerShare()     = ${fmt(nav, 18)} (raw ${nav}) asOfDate=${navAsOf} age=${navAge}s`);
  console.log(`  vault.latestFixingRef() = ${readRef} tier=${readTier}`);
  if (readRef.toLowerCase() !== fixingRef.toLowerCase()) throw new Error("fixingRef read back does not match");
  if (nav !== TEST_NAV_PER_SHARE) throw new Error("navPerShare read back does not match");

  // ---------------------------------------------------------------- step 8
  const endBlock = await retryRead("endBlock", () => ethers.provider.getBlockNumber());
  const record = {
    network: network.name,
    chainId: chainId.toString(),
    explorer: EXPLORERS[chainId.toString()] ?? null,
    vault: vaultAddr,
    deployer: deployer.address,
    admin: deployer.address,
    feeRecipient: deployer.address,
    instrumentId,
    instrumentLabel,
    vaultName: VAULT_NAME,
    vaultSymbol: VAULT_SYMBOL,
    constituents: [tokenAAddr, tokenBAddr],
    unitsPerShare: UNITS_PER_SHARE.map(String),
    mocks: [
      { address: tokenAAddr, name: MOCK_A.name, symbol: MOCK_A.symbol, decimals: MOCK_A.decimals, unitsPerShare: UNITS_PER_SHARE[0].toString() },
      { address: tokenBAddr, name: MOCK_B.name, symbol: MOCK_B.symbol, decimals: MOCK_B.decimals, unitsPerShare: UNITS_PER_SHARE[1].toString() },
    ],
    oneShareRepresents: `${fmt(UNITS_PER_SHARE[0], MOCK_A.decimals)} ${MOCK_A.symbol} + ${fmt(UNITS_PER_SHARE[1], MOCK_B.decimals)} ${MOCK_B.symbol}`,
    attestors,
    threshold: threshold.toString(),
    fees: { createFeeBps: "0", redeemFeeBps: "0", managementFeeBps: "0" },
    registry: null,
    blocks: {
      startBlock,
      mockA: aRc.blockNumber,
      mockB: bRc.blockNumber,
      vault: vRc.blockNumber,
      create: createRc.blockNumber,
      redeem: redeemRc.blockNumber,
      postNav: navRc.blockNumber,
      endBlock,
    },
    exercise: {
      sharesCreated: SHARES_TO_CREATE.toString(),
      sharesRedeemed: SHARES_TO_REDEEM.toString(),
      vaultHoldingsAfterCreate: { [MOCK_A.symbol]: needA.toString(), [MOCK_B.symbol]: needB.toString() },
      vaultHoldingsAfterRedeem: { [MOCK_A.symbol]: (needA - backA).toString(), [MOCK_B.symbol]: (needB - backB).toString() },
      navPerShare: nav.toString(),
      navAsOfDate: navAsOf.toString(),
      navTier: Number(readTier),
      fixingRef,
      fixingRefPreimage: fixingCidPreimage,
      fixingRefIsATestReference: true,
      negatives,
    },
    transactions: txs,
    mocksWarning:
      "The constituents are MockERC20 tokens minted by scripts/e2e-testnet.ts. No real tokenised equity exists on Arc Testnet. This is a testnet demonstration, never a real basket.",
    deployedAt: new Date().toISOString(),
  };
  const outDir = path.join(__dirname, "..", "deployments");
  fs.mkdirSync(outDir, { recursive: true });
  const outFile = path.join(outDir, `${network.name}.json`);
  fs.writeFileSync(outFile, JSON.stringify(record, null, 2));

  console.log("\n[8] Wrote " + outFile);
  console.log("\n" + "=".repeat(78));
  console.log("SUMMARY");
  console.log(`  ${MOCK_A.symbol}  ${tokenAAddr}`);
  console.log(`  ${MOCK_B.symbol} ${tokenBAddr}`);
  console.log(`  vault  ${vaultAddr}`);
  console.log("  transactions:");
  txs.forEach((t) => console.log(`    ${t.hash}  block ${t.block}  ${t.step}`));
  console.log("=".repeat(78));
}

main().catch((e) => {
  console.error(e);
  process.exitCode = 1;
});
