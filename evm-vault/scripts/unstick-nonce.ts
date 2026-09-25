/**
 * Replace a STUCK pending transaction with a no-op at the same nonce.
 *
 *   npx hardhat run scripts/unstick-nonce.ts --network morphHoodi
 *
 * Why this exists: Morph's Hoodi sequencer reports eth_gasPrice = 0.001 gwei,
 * but will not include a transaction priced anywhere near that. A first
 * e2e-testnet.ts attempt on 2026-09-25 left a type-2 transaction at nonce 0
 * (maxFeePerGas 0.00127 gwei) pending for ever, and every later transaction
 * queued behind it. This sends a 0-value self-transfer at that same nonce as a
 * LEGACY transaction at the network's configured gasPrice, which replaces the
 * stuck one and lets the queue drain.
 *
 * It only ever sends 0 value to the signer's own address, so the worst case is
 * one wasted gas fee. Testnet only. Never prints a key.
 */
import { ethers, network } from "hardhat";

const MAINNET_CHAIN_IDS = new Set<bigint>([
  1n,
  8453n,
  42161n,
  137n,
  10n,
  5042001n,
  4663n,
  56n,
  43114n,
  57073n,
  5000n,
  2818n,
]);

async function main() {
  const { chainId } = await ethers.provider.getNetwork();
  if (MAINNET_CHAIN_IDS.has(chainId)) {
    throw new Error(`Refusing to touch mainnet chainId ${chainId}. Testnets only.`);
  }
  const [signer] = await ethers.getSigners();
  if (!signer) throw new Error("No signer: set DEPLOYER_PRIVATE_KEY or MNEMONIC");

  const latest = await ethers.provider.getTransactionCount(signer.address, "latest");
  const pending = await ethers.provider.getTransactionCount(signer.address, "pending");
  console.log(`${network.name} (chainId ${chainId}) signer ${signer.address}`);
  console.log(`  nonce latest=${latest} pending=${pending}`);
  if (pending === latest) {
    console.log("  nothing stuck — pending nonce equals latest nonce");
    return;
  }

  const nonce = Number(process.env.UNSTICK_NONCE ?? latest);
  const gasPriceEnv = process.env.UNSTICK_GAS_PRICE_GWEI;
  const gasPrice = gasPriceEnv
    ? ethers.parseUnits(gasPriceEnv, "gwei")
    : ((await ethers.provider.getFeeData()).gasPrice ?? ethers.parseUnits("1", "gwei"));

  console.log(`  replacing nonce ${nonce} with a 0-value self-transfer, legacy, ${ethers.formatUnits(gasPrice, "gwei")} gwei`);
  const tx = await signer.sendTransaction({
    to: signer.address,
    value: 0n,
    nonce,
    gasLimit: 21_000,
    gasPrice, // legacy type-0: no maxFeePerGas, which is what Morph wants
    type: 0,
  });
  console.log(`  tx ${tx.hash}`);
  const rc = await tx.wait();
  console.log(`  mined in block ${rc?.blockNumber} status ${rc?.status}`);
  console.log(`  nonce is now latest=${await ethers.provider.getTransactionCount(signer.address, "latest")}`);
}

main().catch((e) => {
  console.error(e);
  process.exitCode = 1;
});
