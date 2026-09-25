/**
 * Fund the deployer on Ink Sepolia by depositing ETH through INK'S OWN
 * L1StandardBridge on Ethereum Sepolia.
 *
 *   npx hardhat run <this file> --network ethereumSepolia
 *
 * The bridge address is read off Ink's own docs
 * (https://docs.inkonchain.com/useful-information/contracts -> "L1 Contract
 * Addresses" -> Sepolia -> L1StandardBridge) and then CHECKED on chain before
 * any value moves: version() must be a Bridge version string, otherBridge()
 * must be the OP predeploy 0x42..10, and messenger() must equal the
 * L1CrossDomainMessenger the same docs page lists for Ink Sepolia.
 *
 * Never prints a key. Testnet only.
 */
import { ethers, network } from "hardhat";

const L1_STANDARD_BRIDGE = "0x33f60714bbd74d62b66d79213c348614de51901c";
const EXPECTED_MESSENGER = "0x9fe1d3523f5342535e6e7770ed09ed85dbc1acc2";
const EXPECTED_OTHER_BRIDGE = "0x4200000000000000000000000000000000000010";

const AMOUNT = ethers.parseEther(process.env.BRIDGE_AMOUNT_ETH || "0.0005");
// The deposit burns L1 gas in proportion to the L2 gas it buys (OptimismPortal
// charges for the L2 gas limit up front), so depositETH costs ~625k L1 gas, not
// the ~50k a naive guess would give. With a single-digit-milli-ether Sepolia
// balance that fee, not the deposit, is the binding constraint - so pin the fee
// explicitly instead of letting ethers pick maxFeePerGas = 2x base + tip.
const MAX_FEE = ethers.parseUnits(process.env.BRIDGE_MAX_FEE_GWEI || "1.6", "gwei");
const PRIORITY_FEE = ethers.parseUnits(process.env.BRIDGE_PRIORITY_FEE_GWEI || "0.05", "gwei");
const MIN_GAS_LIMIT = 200_000;

const ABI = [
  "function depositETH(uint32 _minGasLimit, bytes _extraData) payable",
  "function otherBridge() view returns (address)",
  "function messenger() view returns (address)",
  "function version() view returns (string)",
];

async function main() {
  const { chainId } = await ethers.provider.getNetwork();
  if (chainId !== 11155111n) throw new Error(`Expected Ethereum Sepolia (11155111), got ${chainId}`);
  const [deployer] = await ethers.getSigners();

  const bridge = new ethers.Contract(L1_STANDARD_BRIDGE, ABI, deployer);
  const [version, otherBridge, messenger] = await Promise.all([
    bridge.version(),
    bridge.otherBridge(),
    bridge.messenger(),
  ]);
  console.log(`L1StandardBridge ${L1_STANDARD_BRIDGE}`);
  console.log(`  version()      ${version}`);
  console.log(`  otherBridge()  ${otherBridge}`);
  console.log(`  messenger()    ${messenger}`);
  if (otherBridge.toLowerCase() !== EXPECTED_OTHER_BRIDGE.toLowerCase()) {
    throw new Error("otherBridge() is not the OP L2StandardBridge predeploy — refusing to send");
  }
  if (messenger.toLowerCase() !== EXPECTED_MESSENGER.toLowerCase()) {
    throw new Error("messenger() is not Ink Sepolia's documented L1CrossDomainMessenger — refusing to send");
  }

  const bal = await ethers.provider.getBalance(deployer.address);
  const fee = await ethers.provider.getFeeData();
  console.log(`\ndeployer ${deployer.address}`);
  console.log(`  Sepolia balance   ${ethers.formatEther(bal)} ETH`);
  console.log(`  gasPrice          ${ethers.formatUnits(fee.gasPrice ?? 0n, "gwei")} gwei`);
  console.log(`  depositing        ${ethers.formatEther(AMOUNT)} ETH to the same address on Ink Sepolia`);

  const gas = await bridge.depositETH.estimateGas(MIN_GAS_LIMIT, "0x", { value: AMOUNT });
  const gasLimit = (gas * 110n) / 100n;
  const cost = gasLimit * MAX_FEE;
  console.log(
    `  estimated L1 gas  ${gas} (limit ${gasLimit}, worst case at ${ethers.formatUnits(MAX_FEE, "gwei")} gwei = ` +
      `${ethers.formatEther(cost)} ETH)`
  );
  if (AMOUNT + cost > bal) throw new Error("Amount + worst-case L1 fee exceeds the balance; lower BRIDGE_AMOUNT_ETH");

  const tx = await bridge.depositETH(MIN_GAS_LIMIT, "0x", {
    value: AMOUNT,
    gasLimit,
    maxFeePerGas: MAX_FEE,
    maxPriorityFeePerGas: PRIORITY_FEE,
  });
  console.log(`\n  L1 deposit tx ${tx.hash}`);
  console.log(`  https://sepolia.etherscan.io/tx/${tx.hash}`);
  const rc = await tx.wait();
  console.log(`  mined in block ${rc?.blockNumber} status ${rc?.status} gasUsed ${rc?.gasUsed}`);
  console.log(`  Sepolia balance now ${ethers.formatEther(await ethers.provider.getBalance(deployer.address))} ETH`);

  // Poll Ink Sepolia until the deposit lands (OP Stack deposits are ~1-3 min).
  const l2 = new ethers.JsonRpcProvider(process.env.INK_SEPOLIA_RPC_URL || "https://rpc-gel-sepolia.inkonchain.com");
  console.log("\n  waiting for the deposit to appear on Ink Sepolia...");
  for (let i = 0; i < 90; i++) {
    let b = 0n;
    try {
      b = await l2.getBalance(deployer.address);
    } catch {
      /* transient */
    }
    if (b > 0n) {
      console.log(`  Ink Sepolia balance ${ethers.formatEther(b)} ETH after ${i * 5}s — FUNDED`);
      return;
    }
    await new Promise((r) => setTimeout(r, 5_000));
  }
  console.log("  still 0 on Ink Sepolia after 450s — the deposit may still be in flight; re-check the balance.");
}

main().catch((e) => {
  console.error(e);
  process.exitCode = 1;
});
