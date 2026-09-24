/**
 * Deploy EtpBasketVault to a TESTNET, parameterised entirely by environment.
 *
 *   npx hardhat run scripts/deploy.ts --network baseSepolia
 *   npx hardhat run scripts/deploy.ts --network arcTestnet
 *
 * Required env (see .env.example):
 *   DEPLOYER_PRIVATE_KEY | MNEMONIC   signer for the network
 *   VAULT_NAME, VAULT_SYMBOL
 *   INSTRUMENT_ID                     free text (keccak256'd) or 0x..32 bytes
 *   CONSTITUENTS                      comma-separated ERC20 addresses
 *   UNITS_PER_SHARE                   comma-separated units per 1e18 shares,
 *                                     in each token's OWN base units
 *   ATTESTORS, NAV_THRESHOLD          K-of-N committee
 * Optional:
 *   ADMIN, FEE_RECIPIENT (default: deployer), AP_ADDRESSES,
 *   CREATE_FEE_BPS, REDEEM_FEE_BPS, MANAGEMENT_FEE_BPS, HOLDER_REGISTRY
 *
 * Refuses to run against a mainnet chain id. This repo ships no mainnet
 * deployment path on purpose: the share token is a security and goes live
 * only under a licensed issuer's own deployment procedure.
 */
import { ethers, network } from "hardhat";
import * as fs from "fs";
import * as path from "path";

// Ethereum, Base, Arbitrum One, Polygon, Optimism, Arc mainnet, Robinhood Chain.
const MAINNET_CHAIN_IDS = new Set<bigint>([1n, 8453n, 42161n, 137n, 10n, 5042001n, 4663n]);

function req(name: string): string {
  const v = process.env[name];
  if (!v || v.trim() === "") throw new Error(`Missing env ${name}`);
  return v.trim();
}
function opt(name: string, fallback = ""): string {
  const v = process.env[name];
  return v && v.trim() !== "" ? v.trim() : fallback;
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

async function main() {
  const { chainId } = await ethers.provider.getNetwork();
  if (MAINNET_CHAIN_IDS.has(chainId)) {
    throw new Error(`Refusing to deploy to mainnet chainId ${chainId}. Testnets only.`);
  }
  const [deployer] = await ethers.getSigners();
  if (!deployer) throw new Error("No signer: set DEPLOYER_PRIVATE_KEY or MNEMONIC");

  const name = req("VAULT_NAME");
  const symbol = req("VAULT_SYMBOL");
  const instrumentId = toBytes32(req("INSTRUMENT_ID"));
  const constituents = list("CONSTITUENTS").map((a) => ethers.getAddress(a));
  const unitsPerShare = list("UNITS_PER_SHARE").map((u) => BigInt(u));
  if (constituents.length === 0 || constituents.length !== unitsPerShare.length) {
    throw new Error("CONSTITUENTS and UNITS_PER_SHARE must be non-empty and the same length");
  }
  const attestors = list("ATTESTORS").map((a) => ethers.getAddress(a));
  const threshold = BigInt(opt("NAV_THRESHOLD", "2"));
  const admin = ethers.getAddress(opt("ADMIN", deployer.address));
  const feeRecipient = ethers.getAddress(opt("FEE_RECIPIENT", deployer.address));
  const aps = list("AP_ADDRESSES").map((a) => ethers.getAddress(a));
  const createFeeBps = BigInt(opt("CREATE_FEE_BPS", "0"));
  const redeemFeeBps = BigInt(opt("REDEEM_FEE_BPS", "0"));
  const managementFeeBps = BigInt(opt("MANAGEMENT_FEE_BPS", "0"));
  const registry = opt("HOLDER_REGISTRY");

  // Sanity: every constituent must be a contract on this chain, and we print
  // its decimals so a units mistake is visible before money moves.
  for (let i = 0; i < constituents.length; i++) {
    const code = await ethers.provider.getCode(constituents[i]);
    if (code === "0x") throw new Error(`Constituent ${constituents[i]} has no code on chain ${chainId}`);
    const erc20 = await ethers.getContractAt("MockERC20", constituents[i]);
    let dec = "?";
    let sym = "?";
    try { dec = String(await erc20.decimals()); sym = await erc20.symbol(); } catch { /* not all tokens expose these */ }
    console.log(`  constituent[${i}] ${constituents[i]} ${sym} (${dec} dp) units/share = ${unitsPerShare[i]}`);
  }

  console.log(`\nDeploying ${name} (${symbol}) to ${network.name} (chainId ${chainId}) from ${deployer.address}`);
  const Vault = await ethers.getContractFactory("EtpBasketVault");
  const vault = await Vault.deploy({ name, symbol, instrumentId, admin, feeRecipient, constituents, unitsPerShare });
  await vault.waitForDeployment();
  const address = await vault.getAddress();
  console.log(`EtpBasketVault deployed at ${address}`);

  // Post-deploy configuration only works if the deployer IS the admin; if a
  // separate admin was given, print the calls for them to make instead.
  const deployerIsAdmin = admin.toLowerCase() === deployer.address.toLowerCase();
  const todo: string[] = [];
  const run = async (label: string, fn: () => Promise<unknown>) => {
    if (deployerIsAdmin) {
      const tx = (await fn()) as { wait: () => Promise<unknown> };
      await tx.wait();
      console.log(`  ok  ${label}`);
    } else {
      todo.push(label);
    }
  };

  if (attestors.length > 0) {
    await run(`setAttestors([${attestors.join(",")}], ${threshold})`, () => vault.setAttestors(attestors, threshold));
  }
  if (createFeeBps || redeemFeeBps || managementFeeBps) {
    await run(`setFees(${createFeeBps}, ${redeemFeeBps}, ${managementFeeBps})`, () =>
      vault.setFees(createFeeBps, redeemFeeBps, managementFeeBps)
    );
  }
  if (registry) {
    await run(`setRegistry(${registry})`, () => vault.setRegistry(ethers.getAddress(registry)));
  }
  const AP_ROLE = await vault.AP_ROLE();
  for (const ap of aps) {
    await run(`grantRole(AP_ROLE, ${ap})`, () => vault.grantRole(AP_ROLE, ap));
  }
  if (todo.length) {
    console.log(`\nADMIN (${admin}) must now call:`);
    todo.forEach((t) => console.log(`  - ${t}`));
  }

  const record = {
    network: network.name,
    chainId: chainId.toString(),
    vault: address,
    deployer: deployer.address,
    admin,
    feeRecipient,
    instrumentId,
    constituents,
    unitsPerShare: unitsPerShare.map(String),
    attestors,
    threshold: threshold.toString(),
    fees: { createFeeBps: String(createFeeBps), redeemFeeBps: String(redeemFeeBps), managementFeeBps: String(managementFeeBps) },
    registry: registry || null,
    deployedAt: new Date().toISOString(),
  };
  const outDir = path.join(__dirname, "..", "deployments");
  fs.mkdirSync(outDir, { recursive: true });
  const outFile = path.join(outDir, `${network.name}.json`);
  fs.writeFileSync(outFile, JSON.stringify(record, null, 2));
  console.log(`\nWrote ${outFile}`);
  console.log(`\nREMINDER: each underlying's issuer must whitelist the vault address ${address} before the first create.`);
}

main().catch((e) => {
  console.error(e);
  process.exitCode = 1;
});
