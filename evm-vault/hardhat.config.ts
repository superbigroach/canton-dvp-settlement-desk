import { HardhatUserConfig } from "hardhat/config";
import "@nomicfoundation/hardhat-toolbox";
import * as dotenv from "dotenv";

dotenv.config();

// Only inject a deployer key when one is actually set. Never hardcode keys.
// MNEMONIC takes precedence so a team can share a derivation path in CI.
type Accounts =
  | string[]
  | { mnemonic: string; path: string; initialIndex: number; count: number };

function getAccounts(): Accounts {
  if (process.env.MNEMONIC) {
    return {
      mnemonic: process.env.MNEMONIC,
      path: "m/44'/60'/0'/0",
      initialIndex: 0,
      count: 10,
    };
  }
  const key = process.env.DEPLOYER_PRIVATE_KEY || process.env.PRIVATE_KEY;
  return key && key.length > 0 ? [key] : [];
}

const config: HardhatUserConfig = {
  solidity: {
    version: "0.8.24",
    settings: {
      optimizer: { enabled: true, runs: 200 },
      viaIR: false,
      // OpenZeppelin 5.6 reaches ERC20Permit -> EIP712 -> MessageHashUtils ->
      // Strings -> Bytes.sol, which emits the Cancun `mcopy` opcode. Base and
      // Arc both run post-Cancun forks (Arc baselines on Osaka). Before adding
      // any NEW target chain, confirm it has Cancun enabled.
      evmVersion: "cancun",
    },
  },
  networks: {
    hardhat: { chainId: 31337 },
    localhost: { url: "http://127.0.0.1:8545", chainId: 31337 },

    // Base Sepolia (testnet). Faucet: https://docs.base.org/docs/tools/network-faucets
    baseSepolia: {
      url: process.env.BASE_SEPOLIA_RPC_URL || "https://sepolia.base.org",
      accounts: getAccounts(),
      chainId: 84532,
    },

    // Circle Arc Testnet. USDC is the native gas token; fund the deployer at
    // https://faucet.circle.com.
    // chainId 5042002 verified 2026-09-24 against https://docs.arc.network
    // ("chain ID 5042002", RPC https://rpc.testnet.arc.io) and by calling
    // eth_chainId on both https://rpc.testnet.arc.io and
    // https://rpc.testnet.arc.network (both returned 0x4cef52 = 5042002).
    arcTestnet: {
      url: process.env.ARC_TESTNET_RPC_URL || "https://rpc.testnet.arc.io",
      accounts: getAccounts(),
      chainId: 5042002,
    },

    // Robinhood Chain Testnet — Arbitrum Orbit L2 settling to Ethereum Sepolia,
    // native ETH gas. The vault deploys here unchanged (Nitro supports the
    // Cancun opcode set incl. mcopy since ArbOS 20).
    // chainId 46630 verified 2026-09-24: eth_chainId on
    // https://rpc.testnet.chain.robinhood.com returned 0xb626 (= 46630), and
    // the chainid.network registry lists "Robinhood Chain Testnet" 46630 with
    // rpc https://rpc.testnet.chain.robinhood.com/rpc, parent eip155-11155111,
    // bridge https://portal.arbitrum.io/bridge, explorer (Blockscout)
    // https://explorer.testnet.chain.robinhood.com, infoURL
    // https://docs.robinhood.com/chain/. Mainnet is chainId 4663 (refused by
    // the scripts).
    robinhoodTestnet: {
      url: process.env.ROBINHOOD_TESTNET_RPC_URL || "https://rpc.testnet.chain.robinhood.com",
      accounts: getAccounts(),
      chainId: 46630,
    },
  },
  etherscan: {
    apiKey: process.env.ETHERSCAN_API_KEY || "",
    customChains: [
      {
        network: "baseSepolia",
        chainId: 84532,
        urls: {
          apiURL: "https://api-sepolia.basescan.org/api",
          browserURL: "https://sepolia.basescan.org",
        },
      },
      {
        network: "arcTestnet",
        chainId: 5042002,
        urls: {
          apiURL: "https://testnet.arcscan.app/api",
          browserURL: "https://testnet.arcscan.app",
        },
      },
      {
        network: "robinhoodTestnet",
        chainId: 46630,
        urls: {
          // Blockscout explorer per the chainid.network registry entry.
          apiURL: "https://explorer.testnet.chain.robinhood.com/api",
          browserURL: "https://explorer.testnet.chain.robinhood.com",
        },
      },
    ],
  },
  gasReporter: { enabled: false },
  mocha: { timeout: 120_000 },
};

export default config;
