/**
 * Fund the deployer on Arbitrum Sepolia by depositing from Ethereum Sepolia.
 *
 * WHY THIS EXISTS. Every faucet that hands out Arbitrum Sepolia ETH now wants either an
 * account login or a token holding on Ethereum mainnet, neither of which is something an
 * automated run should be doing. But Arbitrum Sepolia is a rollup whose parent IS Sepolia,
 * and its Delayed Inbox has a payable `depositEth()` that credits the SAME address on L2 when
 * an EOA calls it. So a plain deposit is the whole funding route, with no faucet and no wallet.
 *
 * THE ADDRESS, and how it was checked. Arbitrum's own contract-address reference lists the
 * Arbitrum Sepolia Delayed Inbox; the page renders it truncated as 0xaAe2…ae21. Rather than
 * trust a prefix, this script asserts on chain before sending: the contract must have code,
 * and its `bridge()` must return the Bridge address the same page lists. Both were confirmed
 * on 25 Sep 2026 (2,147 bytes of code; bridge() -> 0x38f9…33a9).
 *
 * Run:  npx hardhat run scripts/fund-arbitrum-sepolia.ts --network ethereumSepolia
 */
import { ethers } from "hardhat";

const DELAYED_INBOX = "0xaAe29B0366299461418F5324a79Afc425BE5ae21";
const EXPECTED_BRIDGE = "0x38f918D0E9F1b721EDaA41302E399fa1B79333a9";
const ARBITRUM_SEPOLIA_RPC =
  process.env.ARBITRUM_SEPOLIA_RPC_URL || "https://sepolia-rollup.arbitrum.io/rpc";

/** Leave enough behind to pay for this transaction and one more afterwards. */
const KEEP_ON_L1 = ethers.parseEther("0.004");

async function main() {
  const [deployer] = await ethers.getSigners();
  const provider = ethers.provider;
  const net = await provider.getNetwork();
  if (net.chainId !== 11155111n) {
    throw new Error(`run this against ethereumSepolia (11155111), not chainId ${net.chainId}`);
  }
  console.log(`deployer            ${deployer.address}`);

  const balance = await provider.getBalance(deployer.address);
  console.log(`sepolia balance     ${ethers.formatEther(balance)} ETH`);

  // --- prove the inbox is what the docs say, before sending anything to it ------------
  const code = await provider.getCode(DELAYED_INBOX);
  if (code === "0x") throw new Error(`no contract at ${DELAYED_INBOX} on this network`);
  const inbox = new ethers.Contract(
    DELAYED_INBOX,
    ["function depositEth() payable returns (uint256)", "function bridge() view returns (address)"],
    deployer,
  );
  const bridge: string = await inbox.bridge();
  if (bridge.toLowerCase() !== EXPECTED_BRIDGE.toLowerCase()) {
    throw new Error(`inbox.bridge() = ${bridge}, expected ${EXPECTED_BRIDGE} — wrong contract, refusing`);
  }
  console.log(`delayed inbox       ${DELAYED_INBOX} (${code.length / 2 - 1} bytes, bridge ok)`);

  const amount = balance > KEEP_ON_L1 ? balance - KEEP_ON_L1 : 0n;
  if (amount <= 0n) {
    throw new Error(
      `not enough Sepolia ETH to bridge: have ${ethers.formatEther(balance)}, ` +
        `keeping ${ethers.formatEther(KEEP_ON_L1)} for gas`,
    );
  }
  console.log(`depositing          ${ethers.formatEther(amount)} ETH to the same address on L2`);

  // A revert here costs nothing and tells us the route is wrong before we spend anything.
  const gas = await inbox.depositEth.estimateGas({ value: amount });
  const tx = await inbox.depositEth({ value: amount, gasLimit: (gas * 3n) / 2n });
  console.log(`L1 tx               ${tx.hash}`);
  const receipt = await tx.wait();
  console.log(`L1 confirmed        block ${receipt?.blockNumber}, status ${receipt?.status}`);

  const l2 = new ethers.JsonRpcProvider(ARBITRUM_SEPOLIA_RPC);
  console.log(`watching L2 …       (deposits are usually credited within a few minutes)`);
  for (let i = 0; i < 40; i++) {
    const b = await l2.getBalance(deployer.address);
    if (b > 0n) {
      console.log(`ARBITRUM SEPOLIA    ${ethers.formatEther(b)} ETH — ready to deploy`);
      return;
    }
    await new Promise((r) => setTimeout(r, 15_000));
  }
  console.log(`not credited after 10 minutes; check https://sepolia.arbiscan.io/address/${deployer.address}`);
}

main().catch((e) => {
  console.error(`FAILED: ${e instanceof Error ? e.message : String(e)}`);
  process.exitCode = 1;
});
