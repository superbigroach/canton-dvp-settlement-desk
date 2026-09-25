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
      // HOW THE CHAINS BELOW WERE CONFIRMED, 2026-09-25: an `eth_call` with a
      // state override installing the runtime `6020600060005e60206000f3`
      // (MCOPY 32 bytes, then RETURN) returned a word on bnbTestnet,
      // avalancheFuji, inkSepolia, mantleSepolia, ethereumSepolia, morphHoodi
      // and arbitrumSepolia, while the control override `fe` (INVALID) errored
      // on every one of them - so the probe really does tell a supported opcode
      // apart from an unsupported one, and all seven have MCOPY.
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
    // Arbitrum One holds the largest COUNT of tokenised equities of any chain (4,925
    // instruments, $167M), almost all of it Bitget's "Reality Tokenized" range. Today none
    // of it is withdrawable, so nothing can enter a vault there — but that restriction sits
    // behind a 3-of-5 Safe and can be lifted, so the config is here and ready rather than
    // being written the week a counterparty says yes. chainId verified by eth_chainId.
    arbitrumSepolia: {
      url: process.env.ARBITRUM_SEPOLIA_RPC_URL || "https://sepolia-rollup.arbitrum.io/rpc",
      accounts: getAccounts(),
      chainId: 421614,
    },

    // BNB Smart Chain carries more of the top tokenised-fund AUM than any chain
    // except Ethereum: Circle USYC, BlackRock BUIDL, Ondo USDY, Franklin
    // Templeton iBENJI and Janus Henderson JTRSY are ALL issued there
    // (docs/MARKET_AND_PRICING.md, sourced from rwa.xyz 2026-08-04). Every one of
    // those is a fund share an issuer might want wrapped into a basket, and the
    // distribution sits on the same chain as the largest retail venue in Asia.
    // chainId 97 verified 2026-09-25: eth_chainId on the RPC below returned 0x61.
    // MCOPY (Cancun) confirmed present by the state-override probe described
    // above, so evmVersion "cancun" is safe here. Mainnet is 56 (refused).
    bnbTestnet: {
      url: process.env.BNB_TESTNET_RPC_URL || "https://data-seed-prebsc-1-s1.bnbchain.org:8545",
      accounts: getAccounts(),
      chainId: 97,
    },

    // Avalanche is the institutional-subnet story, and it already carries two of
    // the five largest tokenised funds - BlackRock BUIDL and Janus Henderson
    // JTRSY (docs/MARKET_AND_PRICING.md). It matters to us because a sponsor who
    // wants a PERMISSIONED venue for a restricted basket can have one on
    // Avalanche without leaving the EVM, which answers the single most common
    // objection a regulated issuer raises against a public-chain wrapper.
    // chainId 43113 verified 2026-09-25 (eth_chainId returned 0xa869); MCOPY
    // confirmed by the state-override probe. Mainnet is 43114 (refused).
    avalancheFuji: {
      url: process.env.AVALANCHE_FUJI_RPC_URL || "https://api.avax-test.network/ext/bc/C/rpc",
      accounts: getAccounts(),
      chainId: 43113,
    },

    // Ink is KRAKEN own OP Stack L2 (inkonchain.com front page, 2026-09-25:
    // "DeFi unleashed by Kraken, built on the Superchain"). Kraken is one of the
    // few venues that actually sells tokenised US equities to retail at scale, so
    // Ink is where a counterparty owns both the customer and the chain - the
    // shortest path on this list from "who distributes this" to "whose chain does
    // it settle on". Being OP Stack it also funds from Ethereum Sepolia through
    // the L1 standard bridge, which is exactly how this deployment got its gas.
    // chainId 763373 verified 2026-09-25 (eth_chainId returned 0xba5ed) and
    // against https://docs.inkonchain.com/general/network-information; MCOPY
    // confirmed by the state-override probe. Mainnet is 57073 (refused).
    inkSepolia: {
      url: process.env.INK_SEPOLIA_RPC_URL || "https://rpc-gel-sepolia.inkonchain.com",
      accounts: getAccounts(),
      chainId: 763373,
    },

    // Mantle is the one chain here that has already shipped a BASKET: Mantle
    // Index Four (MI4), an index fund built with Securitize, plus Ondo USDY
    // (docs/MARKET_AND_PRICING.md). A treasury that has itself bought the "one
    // token, several assets, a published NAV" shape is the easiest room to walk
    // into with this vault. NOTE: gas is MNT, not ETH, and the testnet quotes
    // ~50 gwei - budget in MNT and do not carry ether intuitions across.
    // chainId 5003 verified 2026-09-25 (eth_chainId returned 0x138b); MCOPY
    // confirmed by the state-override probe. Mainnet is 5000 (refused).
    mantleSepolia: {
      url: process.env.MANTLE_SEPOLIA_RPC_URL || "https://rpc.sepolia.mantle.xyz",
      accounts: getAccounts(),
      chainId: 5003,
    },

    // Ethereum is where the tokenised-equity market actually clears: xstocks.com
    // (2026-09-25) lists exactly two chains for its 1:1-backed US equity tokens,
    // Ethereum and Solana, and all five of the largest tokenised funds are issued
    // on Ethereum. It is also the L1 that Base, Ink, Arbitrum and Robinhood Chain
    // settle to, which makes Sepolia the FUNDING SOURCE for their testnets as
    // well as a venue in its own right.
    // chainId 11155111 verified 2026-09-25 (eth_chainId returned 0xaa36a7);
    // MCOPY confirmed by the state-override probe. Mainnet is 1 (already refused).
    ethereumSepolia: {
      url: process.env.ETHEREUM_SEPOLIA_RPC_URL || "https://ethereum-sepolia-rpc.publicnode.com",
      accounts: getAccounts(),
      chainId: 11155111,
    },

    // Morph is the weakest commercial case on this list and is here for one
    // reason: it is the chain the BITGET group controls end to end (morph.network
    // fronts a "BGB Ecosystem" and bills itself "the settlement layer for a
    // global payment network"), and Bitget is the venue behind the largest COUNT
    // of tokenised equity instruments anywhere - today issued on Arbitrum, not
    // here. No tokenised equity lives on Morph as of 2026-09-25; the config
    // exists so that if Bitget moves that range onto its own chain we are not
    // writing this the week they ask.
    // IMPORTANT - MORPH MOVED ITS TESTNET. https://rpc-holesky.morphl2.io does
    // not answer any more and Morph Holesky (2810) went away with Holesky itself.
    // Per Morph own docs
    // (https://docs.morph.network/docs/build-on-morph/build-on-morph/development-setup,
    // read 2026-09-25) the current testnet is "Morph Hoodi Testnet", chainId
    // 2910, settling to Hoodi (560048) and NOT to Sepolia - so it cannot be
    // funded from the Sepolia balance the other L2s draw on.
    // chainId 2910 verified 2026-09-25: eth_chainId on both
    // https://rpc-hoodi-bgw.morph.network and https://rpc-hoodi.morph.network
    // returned 0xb5e; MCOPY confirmed by the state-override probe. Mainnet is
    // 2818 (refused by the scripts).
    morphHoodi: {
      url: process.env.MORPH_HOODI_RPC_URL || "https://rpc-hoodi-bgw.morph.network",
      accounts: getAccounts(),
      chainId: 2910,
      // Morph needs an EXPLICIT gas price so that Hardhat sends a LEGACY
      // (type-0) transaction. Observed 2026-09-25: with no gasPrice set, ethers
      // priced a type-2 transaction off eth_gasPrice (0.001 gwei) as
      // maxFeePerGas 0.00127 gwei, and the mAAPL deploy at nonce 0 sat in the
      // pool for ever with no receipt while every later transaction queued
      // behind it (scripts/unstick-nonce.ts exists to clear exactly that).
      // Morph own Hardhat snippet sets gasprice and its Foundry snippet passes
      // --legacy, which is the same instruction. The value below is 20x the
      // observed Hoodi base fee of 0.001 gwei; Morph most-published value is
      // 1 gwei, but that makes the ~6.85M-gas e2e pass cost ~0.0069 ETH, and
      // the faucet only gives 0.01 ETH per address per hour. The L1 data fee is
      // ~7e-6 ETH per transaction, so execution price is what actually bites.
      gasPrice: 20_000_000,
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
      {
        network: "bnbTestnet",
        chainId: 97,
        urls: {
          apiURL: "https://api-testnet.bscscan.com/api",
          browserURL: "https://testnet.bscscan.com",
        },
      },
      {
        network: "avalancheFuji",
        chainId: 43113,
        urls: {
          apiURL: "https://api-testnet.snowtrace.io/api",
          browserURL: "https://testnet.snowtrace.io",
        },
      },
      {
        network: "inkSepolia",
        chainId: 763373,
        urls: {
          // Blockscout, per https://docs.inkonchain.com/general/network-information
          apiURL: "https://explorer-sepolia.inkonchain.com/api",
          browserURL: "https://explorer-sepolia.inkonchain.com",
        },
      },
      {
        network: "mantleSepolia",
        chainId: 5003,
        urls: {
          // https://explorer.sepolia.mantle.xyz 301s to sepolia.mantlescan.xyz
          apiURL: "https://api-sepolia.mantlescan.xyz/api",
          browserURL: "https://sepolia.mantlescan.xyz",
        },
      },
      {
        network: "ethereumSepolia",
        chainId: 11155111,
        urls: {
          apiURL: "https://api-sepolia.etherscan.io/api",
          browserURL: "https://sepolia.etherscan.io",
        },
      },
      {
        network: "morphHoodi",
        chainId: 2910,
        urls: {
          // Blockscout, per Morph development-setup page.
          apiURL: "https://explorer-hoodi.morph.network/api",
          browserURL: "https://explorer-hoodi.morph.network",
        },
      },
    ],
  },
  gasReporter: { enabled: false },
  mocha: { timeout: 120_000 },
};

export default config;
