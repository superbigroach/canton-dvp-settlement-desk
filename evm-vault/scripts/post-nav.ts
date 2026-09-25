/**
 * Reference NAV relay: Canton fixing -> ETP Foundry API -> EIP-712 -> vault.
 *
 *   npx hardhat run scripts/post-nav.ts --network baseSepolia
 *
 * Reads GET ${ETPFOUNDRY_API_BASE}/benchmarks/${BENCHMARK_ID}, builds the
 * NavFixing struct, signs it with N LOCAL attestor keys and submits postNav.
 *
 * THIS IS A REFERENCE RELAY, NOT PRODUCTION KEY CUSTODY. In production each
 * committee member signs the fixing on their own signer (HSM / KMS / the
 * Canton participant's key service) and the relay only COLLECTS signatures
 * and pays gas. Holding all N keys in one process collapses K-of-N to 1-of-1.
 *
 * API shape observed 2026-09-24 at https://etpfoundry.com/api/benchmarks/LX1:
 * {
 *   "id":"LX1","name":"LX1 NAV","kind":"nav","publishTime":"16:00",
 *   "timezone":"Europe/London","description":"Lucilla Crypto Index (cETH + CBTC)",
 *   "last":   {"price":890.0,"asOf":"2026-09-24T19:32:12.629907679Z","tier":0,
 *              "tierLabel":"seed","displayLabel":"seed value, not attested",
 *              "k":0,"n":3,"signers":[],"ageSeconds":56,"fixingCid":null},
 *   "latest": { ...same shape... },
 *   "referencing":[]
 * }
 * `last.tier` per `SeriesRow.labelFor` in the desk backend: 1 = attested,
 * 2 = alternate-seats, 3 = benchmark-x-factor, 4 = carried-forward, 5 = missed,
 * anything else (including 0) = seed. The scale ASCENDS AS TRUST DESCENDS.
 * `k`/`n`/`signers` describe the Canton committee that produced it;
 * `fixingCid` is the Canton NavFixing contract id (null for a seed value).
 * The relay puts keccak256(utf8(fixingCid)) into `fixingRef` and `last.tier`
 * into `tier`, and REFUSES to post when fixingCid is absent: the on-chain
 * record must always point back to an attested Canton contract. The vault then
 * enforces 1 <= tier <= maxTier (default 1) itself. There is no `session`
 * field, so the session code comes from NAV_SESSION (0 = close).
 */
import { ethers, network } from "hardhat";

// Ethereum, Base, Arbitrum One, Polygon, Optimism, Arc mainnet, Robinhood Chain.
// Same list as scripts/deploy.ts and scripts/e2e-testnet.ts: Ethereum 1, Base
// 8453, Arbitrum One 42161, Polygon 137, Optimism 10, Arc 5042001, Robinhood
// Chain 4663, BNB Smart Chain 56, Avalanche C-Chain 43114, Ink 57073, Mantle
// 5000, Morph 2818. A new network in hardhat.config.ts adds its mainnet id to
// all three sets in the same commit.
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

type ApiFixing = {
  price: number | string;
  asOf: string;
  tier?: number;
  tierLabel?: string;
  k?: number;
  n?: number;
  signers?: string[];
  fixingCid?: string | null;
  displayLabel?: string;
  session?: number | string;
};
type ApiBenchmark = { id: string; name?: string; kind?: string; latest: ApiFixing | null; last?: ApiFixing | null };

function req(name: string): string {
  const v = process.env[name];
  if (!v || v.trim() === "") throw new Error(`Missing env ${name}`);
  return v.trim();
}
function opt(name: string, fallback = ""): string {
  const v = process.env[name];
  return v && v.trim() !== "" ? v.trim() : fallback;
}
function toBytes32(v: string): string {
  return ethers.isHexString(v, 32) ? v : ethers.keccak256(ethers.toUtf8Bytes(v));
}

/** Decimal string/number -> 18-dp fixed point without going through a float. */
function toWad(price: number | string): bigint {
  // The API serialises as a JSON number with up to 20 decimals; JSON.parse
  // gives us a double. toFixed(18) is exact enough for a NAV (15-16
  // significant digits) — a production relay should read the raw body with a
  // decimal parser instead of JSON.parse to avoid the double entirely.
  const s = typeof price === "number" ? price.toFixed(18) : String(price);
  const m = /^(\d+)(?:\.(\d+))?$/.exec(s);
  if (!m) throw new Error(`Unparseable price ${price}`);
  const frac = (m[2] ?? "").slice(0, 18); // truncate beyond 18 dp
  return ethers.parseUnits(frac.length ? `${m[1]}.${frac}` : m[1], 18);
}

/** ISO timestamp -> whole days since the Unix epoch (UTC calendar day). */
function toDaysSinceEpoch(iso: string): bigint {
  const ms = Date.parse(iso);
  if (Number.isNaN(ms)) throw new Error(`Unparseable asOf ${iso}`);
  return BigInt(Math.floor(ms / 86_400_000));
}

async function main() {
  const { chainId } = await ethers.provider.getNetwork();
  if (MAINNET_CHAIN_IDS.has(chainId)) throw new Error(`Refusing to post to mainnet chainId ${chainId}`);

  const vaultAddress = ethers.getAddress(req("VAULT_ADDRESS"));
  const benchmarkId = req("BENCHMARK_ID");
  const apiBase = opt("ETPFOUNDRY_API_BASE", "https://etpfoundry.com/api").replace(/\/$/, "");
  const attestorKeys = req("ATTESTOR_PRIVATE_KEYS").split(",").map((k) => k.trim()).filter(Boolean);
  const relayerKey = opt("RELAYER_PRIVATE_KEY", opt("DEPLOYER_PRIVATE_KEY"));
  const session = Number(opt("NAV_SESSION", "0"));
  const rulebookVersion = toBytes32(opt("RULEBOOK_VERSION", "rulebook-v1"));
  if (!relayerKey) throw new Error("Set RELAYER_PRIVATE_KEY (or DEPLOYER_PRIVATE_KEY) to pay gas");

  // 1. Fetch the latest fixing from ETP Foundry.
  const url = `${apiBase}/benchmarks/${encodeURIComponent(benchmarkId)}`;
  console.log(`GET ${url}`);
  const res = await fetch(url, { headers: { accept: "application/json" } });
  if (!res.ok) throw new Error(`API ${res.status} ${res.statusText}`);
  const bench = (await res.json()) as ApiBenchmark;
  const fixing = bench.last ?? bench.latest;
  if (!fixing) throw new Error(`Benchmark ${benchmarkId} has no fixing yet`);
  console.log(
    `  ${bench.name ?? bench.id}: price=${fixing.price} asOf=${fixing.asOf} tier=${fixing.tier} (${fixing.tierLabel}) ` +
      `k=${fixing.k}/n=${fixing.n} signers=${JSON.stringify(fixing.signers ?? [])} fixingCid=${fixing.fixingCid ?? "null"}`
  );
  if (typeof fixing.fixingCid !== "string" || fixing.fixingCid.trim() === "") {
    throw new Error(
      `Refusing to relay: benchmark ${benchmarkId} has no fixingCid (tier ${fixing.tier ?? "?"}: ${fixing.displayLabel ?? fixing.tierLabel ?? "unlabelled"}). ` +
        "Every on-chain posting must reference the Canton NavFixing contract it projects."
    );
  }
  if (fixing.tier === undefined || !Number.isInteger(fixing.tier) || fixing.tier < 0 || fixing.tier > 255) {
    throw new Error(`Refusing to relay: benchmark ${benchmarkId} has no usable tier (${fixing.tier})`);
  }
  const fixingRef = ethers.keccak256(ethers.toUtf8Bytes(fixing.fixingCid));
  const tier = fixing.tier;

  // 2. Build the on-chain struct.
  const relayer = new ethers.Wallet(relayerKey, ethers.provider);
  const vault = await ethers.getContractAt("EtpBasketVault", vaultAddress, relayer);
  const instrumentId = await vault.instrumentId();
  // Never post benchmark X's value into vault Y. The vault's instrumentId must
  // equal keccak256(BENCHMARK_ID) (how deploy.ts derives it) or the explicit
  // INSTRUMENT_ID override for vaults that were deployed with a raw bytes32.
  const expectedInstrument = toBytes32(opt("INSTRUMENT_ID", benchmarkId));
  if (expectedInstrument.toLowerCase() !== instrumentId.toLowerCase()) {
    throw new Error(
      `Instrument mismatch: vault ${vaultAddress} has instrumentId ${instrumentId} but benchmark "${benchmarkId}" ` +
        `maps to ${expectedInstrument}. Set INSTRUMENT_ID if the vault was deployed with a raw bytes32 id.`
    );
  }
  const navFixing = {
    instrumentId,
    asOfDate: toDaysSinceEpoch(fixing.asOf),
    session: fixing.session !== undefined ? Number(fixing.session) : session,
    navPerShare: toWad(fixing.price),
    rulebookVersion,
    signedAt: BigInt(Math.floor(Date.now() / 1000)),
    fixingRef,
    tier,
  };
  const latest = await vault.latestNav();
  if (latest.postedAt !== 0n && navFixing.asOfDate <= latest.asOfDate) {
    console.log(`Vault already has asOfDate ${latest.asOfDate}; API fixing is ${navFixing.asOfDate}. Nothing to post.`);
    return;
  }
  const maxTier = await vault.maxTier();
  if (tier < 1 || BigInt(tier) > maxTier) {
    throw new Error(
      `Refusing to relay: fixing tier ${tier} (${fixing.tierLabel ?? "?"}) is outside the vault's accepted band 1..${maxTier}. ` +
        "Tier 0 is a seed value and can never be posted; for a weaker but genuine fixing the admin may raise maxTier with setMaxTier()."
    );
  }

  // 3. Sign with each local attestor key against the vault's own EIP-712 domain.
  const domain = {
    name: await vault.name(),
    version: "1",
    chainId,
    verifyingContract: vaultAddress,
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
  const threshold = await vault.navThreshold();
  const sigs: string[] = [];
  for (const key of attestorKeys) {
    const w = new ethers.Wallet(key);
    if (!(await vault.isAttestor(w.address))) {
      console.warn(`  skip ${w.address}: not a registered attestor`);
      continue;
    }
    sigs.push(await w.signTypedData(domain, types, navFixing));
    console.log(`  signed by ${w.address}`);
  }
  if (BigInt(sigs.length) < threshold) {
    throw new Error(`Only ${sigs.length} valid attestor signatures; vault threshold is ${threshold}`);
  }
  const onchainDigest = await vault.hashNavFixing(navFixing);
  const localDigest = ethers.TypedDataEncoder.hash(domain, types, navFixing);
  if (onchainDigest !== localDigest) throw new Error("Digest mismatch between local encoder and vault; aborting");

  // 4. Post.
  console.log(
    `postNav on ${network.name}: asOfDate=${navFixing.asOfDate} nav=${ethers.formatUnits(navFixing.navPerShare, 18)} ` +
      `tier=${tier} fixingCid=${fixing.fixingCid} fixingRef=${fixingRef} sigs=${sigs.length}`
  );
  const tx = await vault.postNav(navFixing, sigs);
  console.log(`  tx ${tx.hash}`);
  const receipt = await tx.wait();
  console.log(`  mined in block ${receipt?.blockNumber}`);
  const [nav, asOf, age] = await vault.navPerShare();
  const [ref, postedTier] = await vault.latestFixingRef();
  console.log(`  vault.navPerShare() = ${ethers.formatUnits(nav, 18)} asOfDate=${asOf} age=${age}s`);
  console.log(`  vault.latestFixingRef() = ${ref} tier=${postedTier}`);
}

main().catch((e) => {
  console.error(e);
  process.exitCode = 1;
});
