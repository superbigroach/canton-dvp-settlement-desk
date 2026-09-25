/**
 * ETP Foundry basket vault — end-to-end exercise on SOLANA DEVNET.
 *
 * Mirrors ../evm-vault/scripts/e2e-testnet.ts: deploy two clearly-labelled mock
 * constituents, initialise a vault, set a 2-of-3 attestor committee, grant the AP role,
 * create shares in kind, redeem a portion, post a NAV signed by two of three attestors
 * through the Ed25519 instruction-introspection path, then prove two negatives on-chain
 * (one signature short, and tier 0). Exact token balances are asserted and printed at
 * every step, and everything is written to deployments/devnet.json.
 *
 * DEVNET ONLY. The script refuses to run against any endpoint that is not devnet.
 *
 * THE CONSTITUENTS ARE MOCKS. The NAV is invented. The fixing reference is a hash of a
 * throwaway string, NOT a Canton attestation. Nothing here is a real asset or a real
 * valuation.
 *
 *   anchor build
 *   npx ts-node scripts/e2e-devnet.ts        # ANCHOR_WALLET=~/.config/solana/etp-devnet.json
 */
import * as anchor from "@coral-xyz/anchor";
import { AnchorProvider, BN, Program, Wallet } from "@coral-xyz/anchor";
import {
  Connection,
  Keypair,
  PublicKey,
  SystemProgram,
  Transaction,
  TransactionInstruction,
  Ed25519Program,
  LAMPORTS_PER_SOL,
} from "@solana/web3.js";
import {
  TOKEN_PROGRAM_ID,
  TOKEN_2022_PROGRAM_ID,
  ASSOCIATED_TOKEN_PROGRAM_ID,
  MINT_SIZE,
  AccountLayout,
  MintLayout,
  ACCOUNT_SIZE,
  createInitializeMint2Instruction,
  createAssociatedTokenAccountIdempotentInstruction,
  createMintToInstruction,
  getAssociatedTokenAddressSync,
} from "@solana/spl-token";
import nacl from "tweetnacl";
import { createHash, randomBytes } from "crypto";
import * as fs from "fs";
import * as os from "os";
import * as path from "path";
import type { EtpBasketVault } from "../target/types/etp_basket_vault";

// ---------------------------------------------------------------------------------------
// Constants and small helpers
// ---------------------------------------------------------------------------------------

const SHARE_UNIT = 1_000_000_000n; // share mint has 9 decimals; 1.0 share = 1e9 base units
const ONE_DAY = 86_400;
const NAV_DOMAIN = Buffer.from("ETPFOUNDRY_NAV_FIXING_V1");
const EXPLORER = (kind: "tx" | "address", id: string) =>
  `https://explorer.solana.com/${kind}/${id}?cluster=devnet`;

/** Mock constituents. The names are deliberately unmistakable. */
const MOCKS = [
  { label: "MOCK Apple (test only)", symbol: "mAAPL", decimals: 6, unitsPerShare: 2_000_000n },
  { label: "MOCK T-Bill (test only)", symbol: "mTBILL", decimals: 6, unitsPerShare: 100_000_000n },
];
const INSTRUMENT_LABEL = "MOCK-ETP-SOLANA-DEVNET-DEMO";
const SHARES_TO_CREATE = 1_000n * SHARE_UNIT; // 1000.000000000 shares
const SHARES_TO_REDEEM = 250n * SHARE_UNIT; //  250.000000000 shares
const NAV_PER_SHARE = 1_234_560_000_000n; // 1234.56, scaled 1e9
const AP_FUNDING = 10_000_000_000_000n; // plenty of every mock for the AP

function fail(message: string): never {
  throw new Error(message);
}
function assertEq(actual: bigint, expected: bigint, what: string) {
  if (actual !== expected) fail(`${what}: expected ${expected}, got ${actual}`);
}
/** ceil(shares * unitsPerShare / SHARE_UNIT) — what `create` charges. */
const unitsForCreate = (shares: bigint, ups: bigint) => (shares * ups + SHARE_UNIT - 1n) / SHARE_UNIT;
/** floor(shares * unitsPerShare / SHARE_UNIT) — what `redeem` returns. */
const unitsForRedeem = (shares: bigint, ups: bigint) => (shares * ups) / SHARE_UNIT;

const ata = (mint: PublicKey, owner: PublicKey, programId = TOKEN_PROGRAM_ID) =>
  getAssociatedTokenAddressSync(mint, owner, true, programId, ASSOCIATED_TOKEN_PROGRAM_ID);
const vaultPda = (programId: PublicKey, instrumentId: Buffer) =>
  PublicKey.findProgramAddressSync([Buffer.from("vault"), instrumentId], programId)[0];
const shareMintPda = (programId: PublicKey, vault: PublicKey) =>
  PublicKey.findProgramAddressSync([Buffer.from("share_mint"), vault.toBuffer()], programId)[0];

/** Must match `nav::canonical_nav_message` byte for byte. */
function navMessage(
  programId: PublicKey,
  vault: PublicKey,
  f: {
    instrumentId: Buffer;
    asOfDate: number;
    session: number;
    navPerShare: bigint;
    rulebookVersion: Buffer;
    fixingRef: Buffer;
    tier: number;
  },
): Buffer {
  const asOf = Buffer.alloc(4);
  asOf.writeUInt32LE(f.asOfDate);
  const nav = Buffer.alloc(8);
  nav.writeBigUInt64LE(f.navPerShare);
  return Buffer.concat([
    NAV_DOMAIN,
    programId.toBuffer(),
    vault.toBuffer(),
    f.instrumentId,
    asOf,
    Buffer.from([f.session]),
    nav,
    f.rulebookVersion,
    f.fixingRef,
    Buffer.from([f.tier]),
  ]);
}

function ed25519Ix(signer: Keypair, message: Buffer): TransactionInstruction {
  return Ed25519Program.createInstructionWithPublicKey({
    publicKey: signer.publicKey.toBytes(),
    message,
    signature: nacl.sign.detached(message, signer.secretKey),
  });
}

/** ONE Ed25519 instruction carrying SEVERAL signatures over the SAME message, with a
 *  single shared copy of the message body.
 *
 *  A committee of three or more has to use this: `createInstructionWithPublicKey` emits one
 *  instruction per signature, each repeating the 146-byte canonical message, and three of
 *  those plus `post_nav` is 1424 bytes against the 1232-byte transaction limit. Both the
 *  native precompile and `nav::verify_attestations` iterate `num_signatures` offsets inside
 *  one instruction, so this is the supported shape and what a real relay must do.
 *
 *  Layout (agave `ed25519_instruction.rs`): num_signatures u8, padding u8, one 14-byte
 *  offsets record per signature, then pubkeys, signatures, message. Instruction indices are
 *  0xFFFF = "this instruction". */
function ed25519IxMulti(signers: Keypair[], message: Buffer): TransactionInstruction {
  const SELF = 0xffff;
  const n = signers.length;
  const pubkeysAt = 2 + n * 14;
  const sigsAt = pubkeysAt + n * 32;
  const messageAt = sigsAt + n * 64;
  const data = Buffer.alloc(messageAt + message.length);

  data.writeUInt8(n, 0);
  data.writeUInt8(0, 1);
  signers.forEach((signer, i) => {
    const o = 2 + i * 14;
    data.writeUInt16LE(sigsAt + i * 64, o);
    data.writeUInt16LE(SELF, o + 2);
    data.writeUInt16LE(pubkeysAt + i * 32, o + 4);
    data.writeUInt16LE(SELF, o + 6);
    data.writeUInt16LE(messageAt, o + 8);
    data.writeUInt16LE(message.length, o + 10);
    data.writeUInt16LE(SELF, o + 12);
    Buffer.from(signer.publicKey.toBytes()).copy(data, pubkeysAt + i * 32);
    Buffer.from(nacl.sign.detached(message, signer.secretKey)).copy(data, sigsAt + i * 64);
  });
  message.copy(data, messageAt);
  return new TransactionInstruction({ keys: [], programId: Ed25519Program.programId, data });
}

// ---------------------------------------------------------------------------------------

async function main() {
  // ---- wallet + connection, with a hard devnet guard -------------------------------
  const walletPath =
    process.env.ANCHOR_WALLET ?? path.join(os.homedir(), ".config", "solana", "etp-devnet.json");
  if (!fs.existsSync(walletPath)) fail(`no keypair at ${walletPath}`);
  const payer = Keypair.fromSecretKey(
    Uint8Array.from(JSON.parse(fs.readFileSync(walletPath, "utf8"))),
  );

  const rpc = process.env.ANCHOR_PROVIDER_URL ?? "https://api.devnet.solana.com";
  if (/mainnet/i.test(rpc)) fail(`refusing to run against ${rpc} — devnet only`);
  const connection = new Connection(rpc, "confirmed");
  // Genesis hash is the authoritative cluster identity; the URL is only a hint.
  const DEVNET_GENESIS = "EtWTRABZaYq6iMfeYKouRu166VU2xqa1wcaWoxPkrZBG";
  const genesis = await connection.getGenesisHash();
  if (genesis !== DEVNET_GENESIS) fail(`genesis ${genesis} is not devnet (${DEVNET_GENESIS})`);
  console.log(`cluster   devnet (genesis ${genesis})`);
  console.log(`payer     ${payer.publicKey.toBase58()}`);

  const provider = new AnchorProvider(connection, new Wallet(payer), {
    commitment: "confirmed",
    preflightCommitment: "confirmed",
  });
  anchor.setProvider(provider);

  const idl = JSON.parse(
    fs.readFileSync(path.join(__dirname, "..", "target", "idl", "etp_basket_vault.json"), "utf8"),
  );
  const program = new Program<EtpBasketVault>(idl as EtpBasketVault, provider);
  const programId = program.programId;
  console.log(`program   ${programId.toBase58()}`);
  const programAccount = await connection.getAccountInfo(programId);
  if (!programAccount) fail(`program ${programId.toBase58()} is not deployed on devnet`);

  const balance = await connection.getBalance(payer.publicKey);
  console.log(`balance   ${(balance / LAMPORTS_PER_SOL).toFixed(4)} SOL`);
  if (balance < 0.3 * LAMPORTS_PER_SOL) fail("fund the payer with `solana airdrop 2` first");

  const txs: Record<string, string> = {};
  const send = async (name: string, ixs: TransactionInstruction[], signers: Keypair[] = []) => {
    const sig = await provider.sendAndConfirm(new Transaction().add(...ixs), signers);
    txs[name] = sig;
    return sig;
  };
  const tokenBalance = async (account: PublicKey): Promise<bigint> => {
    const info = await connection.getAccountInfo(account);
    if (!info) fail(`token account ${account.toBase58()} does not exist`);
    return AccountLayout.decode(info.data.subarray(0, ACCOUNT_SIZE)).amount;
  };
  const supplyOf = async (mint: PublicKey): Promise<bigint> => {
    const info = await connection.getAccountInfo(mint);
    if (!info) fail(`mint ${mint.toBase58()} does not exist`);
    return MintLayout.decode(info.data.subarray(0, MINT_SIZE)).supply;
  };

  // ---- 1. the two mock constituents -----------------------------------------------
  console.log("\n-- mock constituents (SPL Token, mint authority = payer) ------------");
  const mints: PublicKey[] = [];
  for (const m of MOCKS) {
    const mint = Keypair.generate();
    const rent = await connection.getMinimumBalanceForRentExemption(MINT_SIZE);
    await send(
      `mint_${m.symbol}`,
      [
        SystemProgram.createAccount({
          fromPubkey: payer.publicKey,
          newAccountPubkey: mint.publicKey,
          space: MINT_SIZE,
          lamports: rent,
          programId: TOKEN_PROGRAM_ID,
        }),
        createInitializeMint2Instruction(
          mint.publicKey,
          m.decimals,
          payer.publicKey,
          null,
          TOKEN_PROGRAM_ID,
        ),
      ],
      [mint],
    );
    mints.push(mint.publicKey);
    console.log(`  ${m.symbol.padEnd(7)} ${mint.publicKey.toBase58()}  "${m.label}"  ${m.decimals}dp`);
  }

  // ---- 2. the vault ----------------------------------------------------------------
  // A random suffix keeps reruns from colliding on the vault PDA.
  const instrumentSuffix = randomBytes(4).toString("hex");
  const instrumentPreimage = `${INSTRUMENT_LABEL}:${instrumentSuffix}`;
  const instrumentId = createHash("sha256").update(instrumentPreimage, "utf8").digest();
  const vault = vaultPda(programId, instrumentId);
  const shareMint = shareMintPda(programId, vault);

  const ap = Keypair.generate(); // signs create/redeem; the payer pays the fees
  const attestors = [Keypair.generate(), Keypair.generate(), Keypair.generate()];
  const THRESHOLD = 2;

  console.log("\n-- vault ------------------------------------------------------------");
  console.log(`  instrument  ${instrumentId.toString("hex")}  ("${instrumentPreimage}")`);
  console.log(`  vault PDA   ${vault.toBase58()}`);
  console.log(`  share mint  ${shareMint.toBase58()}`);
  console.log(`  AP          ${ap.publicKey.toBase58()}`);
  attestors.forEach((a, i) => console.log(`  attestor ${i}  ${a.publicKey.toBase58()}`));
  console.log(`  threshold   ${THRESHOLD} of ${attestors.length}`);

  // Fees are all zero so the in-kind balance assertions are exact, as on the EVM side.
  await send("initialize_vault", [
    await program.methods
      .initializeVault({
        instrumentId: Array.from(instrumentId),
        constituents: MOCKS.map((m, i) => ({
          mint: mints[i],
          unitsPerShare: new BN(m.unitsPerShare.toString()),
        })),
        createFeeBps: 0,
        redeemFeeBps: 0,
        managementFeeBpsPerYear: 0,
        feeRecipient: payer.publicKey,
        attestors: attestors.map((a) => a.publicKey),
        threshold: THRESHOLD,
        rebalanceDelaySecs: new BN(ONE_DAY),
      })
      .accountsStrict({
        authority: payer.publicKey,
        vault,
        shareMint,
        shareTokenProgram: TOKEN_PROGRAM_ID,
        systemProgram: SystemProgram.programId,
      })
      .instruction(),
  ]);
  console.log(`  initialised  ${EXPLORER("tx", txs["initialize_vault"])}`);

  // ---- 3. grant the AP role --------------------------------------------------------
  await send("set_ap", [
    await program.methods
      .setAp(ap.publicKey, true)
      .accountsStrict({ authority: payer.publicKey, vault })
      .instruction(),
  ]);
  const afterSetAp = await program.account.vault.fetch(vault);
  if (!afterSetAp.authorisedParticipants.some((k) => k.equals(ap.publicKey)))
    fail("AP role was not granted");
  console.log(`  AP granted   ${EXPLORER("tx", txs["set_ap"])}`);

  // ---- 4. token accounts, and fund the AP -----------------------------------------
  const apShareAccount = ata(shareMint, ap.publicKey);
  const feeShareAccount = ata(shareMint, payer.publicKey);
  const vaultAtas = mints.map((m) => ata(m, vault));
  const apAtas = mints.map((m) => ata(m, ap.publicKey));

  await send("token_accounts", [
    createAssociatedTokenAccountIdempotentInstruction(
      payer.publicKey, apShareAccount, ap.publicKey, shareMint, TOKEN_PROGRAM_ID, ASSOCIATED_TOKEN_PROGRAM_ID),
    createAssociatedTokenAccountIdempotentInstruction(
      payer.publicKey, feeShareAccount, payer.publicKey, shareMint, TOKEN_PROGRAM_ID, ASSOCIATED_TOKEN_PROGRAM_ID),
    ...mints.flatMap((m, i) => [
      createAssociatedTokenAccountIdempotentInstruction(
        payer.publicKey, vaultAtas[i], vault, m, TOKEN_PROGRAM_ID, ASSOCIATED_TOKEN_PROGRAM_ID),
      createAssociatedTokenAccountIdempotentInstruction(
        payer.publicKey, apAtas[i], ap.publicKey, m, TOKEN_PROGRAM_ID, ASSOCIATED_TOKEN_PROGRAM_ID),
    ]),
  ]);
  await send(
    "fund_ap",
    mints.map((m, i) =>
      createMintToInstruction(m, apAtas[i], payer.publicKey, AP_FUNDING, [], TOKEN_PROGRAM_ID),
    ),
  );

  // ---- 5. create in kind ------------------------------------------------------------
  console.log("\n-- create ------------------------------------------------------------");
  const apUnitsBefore = await Promise.all(apAtas.map(tokenBalance));
  const vaultUnitsBefore = await Promise.all(vaultAtas.map(tokenBalance));
  const apSharesBefore = await tokenBalance(apShareAccount);
  const supplyBefore = await supplyOf(shareMint);
  MOCKS.forEach((m, i) =>
    console.log(`  before  vault ${m.symbol.padEnd(7)} ${vaultUnitsBefore[i]}   AP ${apUnitsBefore[i]}`),
  );
  console.log(`  before  AP shares ${apSharesBefore}   supply ${supplyBefore}`);

  const createRemaining = MOCKS.map((_, i) => [
    { pubkey: mints[i], isSigner: false, isWritable: false },
    { pubkey: apAtas[i], isSigner: false, isWritable: true },
    { pubkey: vaultAtas[i], isSigner: false, isWritable: true },
  ]).flat();

  await send(
    "create",
    [
      await program.methods
        .create(new BN(SHARES_TO_CREATE.toString()))
        .accountsStrict({
          vault,
          ap: ap.publicKey,
          shareMint,
          receiverShareAccount: apShareAccount,
          feeShareAccount,
          holderRegistry: null,
          shareTokenProgram: TOKEN_PROGRAM_ID,
          tokenProgram: TOKEN_PROGRAM_ID,
          token2022Program: TOKEN_2022_PROGRAM_ID,
        })
        .remainingAccounts(createRemaining)
        .instruction(),
    ],
    [ap],
  );

  const unitsIn = MOCKS.map((m) => unitsForCreate(SHARES_TO_CREATE, m.unitsPerShare));
  const apUnitsAfterCreate = await Promise.all(apAtas.map(tokenBalance));
  const vaultUnitsAfterCreate = await Promise.all(vaultAtas.map(tokenBalance));
  const apSharesAfterCreate = await tokenBalance(apShareAccount);
  const supplyAfterCreate = await supplyOf(shareMint);
  MOCKS.forEach((m, i) => {
    assertEq(vaultUnitsAfterCreate[i], vaultUnitsBefore[i] + unitsIn[i], `vault ${m.symbol} after create`);
    assertEq(apUnitsAfterCreate[i], apUnitsBefore[i] - unitsIn[i], `AP ${m.symbol} after create`);
    console.log(`  after   vault ${m.symbol.padEnd(7)} ${vaultUnitsAfterCreate[i]}   AP ${apUnitsAfterCreate[i]}   (in ${unitsIn[i]})`);
  });
  assertEq(apSharesAfterCreate, apSharesBefore + SHARES_TO_CREATE, "AP shares after create");
  assertEq(supplyAfterCreate, supplyBefore + SHARES_TO_CREATE, "share supply after create");
  console.log(`  after   AP shares ${apSharesAfterCreate}   supply ${supplyAfterCreate}`);
  console.log(`  ${EXPLORER("tx", txs["create"])}`);

  // ---- 6. redeem a portion ---------------------------------------------------------
  console.log("\n-- redeem ------------------------------------------------------------");
  const redeemRemaining = MOCKS.map((_, i) => [
    { pubkey: mints[i], isSigner: false, isWritable: false },
    { pubkey: vaultAtas[i], isSigner: false, isWritable: true },
    { pubkey: apAtas[i], isSigner: false, isWritable: true },
  ]).flat();

  await send(
    "redeem",
    [
      await program.methods
        .redeem(new BN(SHARES_TO_REDEEM.toString()))
        .accountsStrict({
          vault,
          ap: ap.publicKey,
          shareMint,
          apShareAccount,
          feeShareAccount,
          shareTokenProgram: TOKEN_PROGRAM_ID,
          tokenProgram: TOKEN_PROGRAM_ID,
          token2022Program: TOKEN_2022_PROGRAM_ID,
        })
        .remainingAccounts(redeemRemaining)
        .instruction(),
    ],
    [ap],
  );

  const unitsOut = MOCKS.map((m) => unitsForRedeem(SHARES_TO_REDEEM, m.unitsPerShare));
  const apUnitsAfterRedeem = await Promise.all(apAtas.map(tokenBalance));
  const vaultUnitsAfterRedeem = await Promise.all(vaultAtas.map(tokenBalance));
  const apSharesAfterRedeem = await tokenBalance(apShareAccount);
  const supplyAfterRedeem = await supplyOf(shareMint);
  MOCKS.forEach((m, i) => {
    assertEq(vaultUnitsAfterRedeem[i], vaultUnitsAfterCreate[i] - unitsOut[i], `vault ${m.symbol} after redeem`);
    assertEq(apUnitsAfterRedeem[i], apUnitsAfterCreate[i] + unitsOut[i], `AP ${m.symbol} after redeem`);
    console.log(`  after   vault ${m.symbol.padEnd(7)} ${vaultUnitsAfterRedeem[i]}   AP ${apUnitsAfterRedeem[i]}   (out ${unitsOut[i]})`);
  });
  assertEq(apSharesAfterRedeem, apSharesAfterCreate - SHARES_TO_REDEEM, "AP shares after redeem");
  assertEq(supplyAfterRedeem, supplyAfterCreate - SHARES_TO_REDEEM, "share supply after redeem");
  console.log(`  after   AP shares ${apSharesAfterRedeem}   supply ${supplyAfterRedeem}`);
  console.log(`  ${EXPLORER("tx", txs["redeem"])}`);

  // ---- 7. post a NAV signed by two of three ---------------------------------------
  console.log("\n-- post_nav (2 of 3 via Ed25519 introspection) -----------------------");
  const asOfDate = Math.floor(Date.now() / 86_400_000);
  const rulebookVersion = Buffer.from("rulebook-v3.0.0".padEnd(32, "\0"));
  const fixingRefPreimage = `TEST-FIXING-NOT-A-CANTON-ATTESTATION:devnet:${instrumentPreimage}:${new Date().toISOString()}`;
  const fixingRef = createHash("sha256").update(fixingRefPreimage, "utf8").digest();
  const fixing = { instrumentId, asOfDate, session: 0, navPerShare: NAV_PER_SHARE, rulebookVersion, fixingRef, tier: 1 };
  const fixingArgs = (f: typeof fixing) => ({
    instrumentId: Array.from(f.instrumentId),
    asOfDate: f.asOfDate,
    session: f.session,
    navPerShare: new BN(f.navPerShare.toString()),
    rulebookVersion: Array.from(f.rulebookVersion),
    fixingRef: Array.from(f.fixingRef),
    tier: f.tier,
  });
  const postNavIxFor = async (f: typeof fixing) =>
    program.methods
      .postNav(fixingArgs(f))
      .accountsStrict({
        relay: payer.publicKey,
        vault,
        instructionsSysvar: anchor.web3.SYSVAR_INSTRUCTIONS_PUBKEY,
      })
      .instruction();

  const msg = navMessage(programId, vault, fixing);
  // PACKED: both signatures in one Ed25519 instruction. This is the shape a committee of
  // three or more is forced into by the 1232-byte transaction limit, so exercise it here.
  const packedIx = ed25519IxMulti([attestors[0], attestors[1]], msg);
  console.log(`  packed Ed25519 ix: 2 signatures in 1 instruction, ${packedIx.data.length} bytes of data`);
  await send("post_nav", [packedIx, await postNavIxFor(fixing)]);
  const v = await program.account.vault.fetch(vault);
  assertEq(BigInt(v.latestNav.navPerShare.toString()), NAV_PER_SHARE, "latestNav.navPerShare");
  if (v.latestNav.asOfDate !== asOfDate) fail("latestNav.asOfDate mismatch");
  if (v.latestNav.attestorCount !== 2) fail(`expected 2 attestors recorded, got ${v.latestNav.attestorCount}`);
  if (v.latestNav.tier !== 1) fail("latestNav.tier mismatch");
  if (Buffer.from(v.latestNav.fixingRef).toString("hex") !== fixingRef.toString("hex"))
    fail("latestNav.fixingRef mismatch");
  console.log(`  navPerShare ${v.latestNav.navPerShare.toString()} (1e9 = 1.0)  asOfDate ${asOfDate}  tier ${v.latestNav.tier}  attestors ${v.latestNav.attestorCount}`);
  console.log(`  fixingRef   ${fixingRef.toString("hex")}`);
  console.log(`  preimage    ${fixingRefPreimage}`);
  console.log(`  ${EXPLORER("tx", txs["post_nav"])}`);

  // ---- 8. two negatives, on-chain -------------------------------------------------
  console.log("\n-- negatives ---------------------------------------------------------");
  /** Runs a call that must fail, and reports the program error it produced. */
  const mustFail = async (label: string, ixs: TransactionInstruction[], expected: string) => {
    try {
      await provider.sendAndConfirm(new Transaction().add(...ixs), []);
    } catch (e: any) {
      const text = [e?.message, ...(e?.logs ?? []), ...(e?.transactionLogs ?? [])]
        .filter(Boolean)
        .join("\n");
      const m = text.match(/Error Code: (\w+)\. Error Number: (\d+)/);
      const code = m?.[1] ?? "(unparsed)";
      const number = m?.[2] ?? "?";
      if (code !== expected) fail(`${label}: expected ${expected}, got ${code}\n${text.slice(0, 900)}`);
      console.log(`  ${label}\n      -> ${code} (${number}) — ${errorMessageFor(idl, code)}`);
      return { label, error: code, errorNumber: Number(number), message: errorMessageFor(idl, code) };
    }
    fail(`${label}: the call SUCCEEDED, which it must not`);
  };

  // (a) one signature short of the 2-of-3 threshold. as_of_date is bumped so the call
  //     reaches the signature check instead of stopping at StaleFixing.
  const oneSigFixing = { ...fixing, asOfDate: asOfDate + 1 };
  const oneSigMsg = navMessage(programId, vault, oneSigFixing);
  const neg1 = await mustFail(
    "post_nav with 1 of the 2 required signatures",
    [ed25519Ix(attestors[0], oneSigMsg), await postNavIxFor(oneSigFixing)],
    "InsufficientAttestations",
  );

  // (b) tier 0 — the unattested seed, refused at every max_tier setting.
  const seedFixing = { ...fixing, asOfDate: asOfDate + 1, tier: 0 };
  const seedMsg = navMessage(programId, vault, seedFixing);
  const neg2 = await mustFail(
    "post_nav with tier = 0 (unattested seed), properly signed 2 of 3",
    [ed25519Ix(attestors[0], seedMsg), ed25519Ix(attestors[1], seedMsg), await postNavIxFor(seedFixing)],
    "SeedTierRefused",
  );

  // The refused postings must not have touched the accepted record.
  const afterNegatives = await program.account.vault.fetch(vault);
  if (afterNegatives.latestNav.asOfDate !== asOfDate) fail("a refused posting changed latestNav");
  if (afterNegatives.latestNav.tier !== 1) fail("a refused posting changed latestNav.tier");
  assertEq(BigInt(afterNegatives.latestNav.navPerShare.toString()), NAV_PER_SHARE, "latestNav.navPerShare after refusals");
  console.log(`  latestNav unchanged after both refusals: asOfDate ${afterNegatives.latestNav.asOfDate}, tier ${afterNegatives.latestNav.tier}, nav ${afterNegatives.latestNav.navPerShare.toString()}`);

  // ---- 8b. the UNPACKED path, and that a refusal did not poison the date ------------
  // Same as_of_date the two refusals used. It must now be accepted, which shows the
  // refusals left no state behind, and it covers the one-instruction-per-signature shape
  // that a committee of two can still use.
  console.log("
-- second posting, separate Ed25519 instructions ----------------------");
  const nextFixing = { ...fixing, asOfDate: asOfDate + 1, navPerShare: NAV_PER_SHARE + 10_000_000n };
  const nextMsg = navMessage(programId, vault, nextFixing);
  await send("post_nav_2", [
    ed25519Ix(attestors[0], nextMsg),
    ed25519Ix(attestors[2], nextMsg), // a different pair, to show any K of N works
    await postNavIxFor(nextFixing),
  ]);
  const after = await program.account.vault.fetch(vault);
  assertEq(BigInt(after.latestNav.navPerShare.toString()), nextFixing.navPerShare, "second latestNav.navPerShare");
  if (after.latestNav.asOfDate !== asOfDate + 1) fail("second posting asOfDate mismatch");
  if (after.latestNav.attestorCount !== 2) fail(`second posting recorded ${after.latestNav.attestorCount} attestors`);
  console.log(`  accepted: navPerShare ${after.latestNav.navPerShare.toString()}  asOfDate ${after.latestNav.asOfDate}  attestors ${after.latestNav.attestorCount}`);
  console.log(`  ${EXPLORER("tx", txs["post_nav_2"])}`);

  // and the first fixing is now stale
  const neg3 = await mustFail(
    "re-posting the FIRST fixing after a newer one was accepted",
    [ed25519IxMulti([attestors[0], attestors[1]], msg), await postNavIxFor(fixing)],
    "StaleFixing",
  );

  // ---- 9. record it ----------------------------------------------------------------
  const out = {
    network: "solana-devnet",
    genesisHash: genesis,
    rpc,
    explorer: "https://explorer.solana.com",
    explorerClusterQuery: "?cluster=devnet",
    warning:
      "MOCKS AND AN INVENTED NAV. The constituents are throwaway SPL mints with no issuer; " +
      "navPerShare is a made-up number; fixingRef is the sha256 of the test string in " +
      "fixingRefPreimage and is NOT a Canton attestation. Devnet only.",
    programId: programId.toBase58(),
    payer: payer.publicKey.toBase58(),
    authority: payer.publicKey.toBase58(),
    feeRecipient: payer.publicKey.toBase58(),
    ap: ap.publicKey.toBase58(),
    vault: vault.toBase58(),
    shareMint: shareMint.toBase58(),
    shareDecimals: 9,
    instrumentId: `0x${instrumentId.toString("hex")}`,
    instrumentLabel: INSTRUMENT_LABEL,
    instrumentIdPreimage: instrumentPreimage,
    mocks: MOCKS.map((m, i) => ({
      mint: mints[i].toBase58(),
      label: m.label,
      symbol: m.symbol,
      decimals: m.decimals,
      unitsPerShare: m.unitsPerShare.toString(),
      tokenProgram: TOKEN_PROGRAM_ID.toBase58(),
      labelIsOffChain: true,
      vaultTokenAccount: vaultAtas[i].toBase58(),
      apTokenAccount: apAtas[i].toBase58(),
    })),
    oneShareRepresents: MOCKS.map((m) => `${Number(m.unitsPerShare) / 10 ** m.decimals} ${m.symbol}`).join(" + "),
    attestors: attestors.map((a) => a.publicKey.toBase58()),
    threshold: THRESHOLD,
    maxTier: after.maxTier,
    fees: { createFeeBps: 0, redeemFeeBps: 0, managementFeeBpsPerYear: 0 },
    holderRegistry: null,
    exercise: {
      sharesCreated: SHARES_TO_CREATE.toString(),
      sharesRedeemed: SHARES_TO_REDEEM.toString(),
      unitsInPerConstituent: Object.fromEntries(MOCKS.map((m, i) => [m.symbol, unitsIn[i].toString()])),
      unitsOutPerConstituent: Object.fromEntries(MOCKS.map((m, i) => [m.symbol, unitsOut[i].toString()])),
      balances: {
        beforeCreate: {
          vault: Object.fromEntries(MOCKS.map((m, i) => [m.symbol, vaultUnitsBefore[i].toString()])),
          ap: Object.fromEntries(MOCKS.map((m, i) => [m.symbol, apUnitsBefore[i].toString()])),
          apShares: apSharesBefore.toString(),
          shareSupply: supplyBefore.toString(),
        },
        afterCreate: {
          vault: Object.fromEntries(MOCKS.map((m, i) => [m.symbol, vaultUnitsAfterCreate[i].toString()])),
          ap: Object.fromEntries(MOCKS.map((m, i) => [m.symbol, apUnitsAfterCreate[i].toString()])),
          apShares: apSharesAfterCreate.toString(),
          shareSupply: supplyAfterCreate.toString(),
        },
        afterRedeem: {
          vault: Object.fromEntries(MOCKS.map((m, i) => [m.symbol, vaultUnitsAfterRedeem[i].toString()])),
          ap: Object.fromEntries(MOCKS.map((m, i) => [m.symbol, apUnitsAfterRedeem[i].toString()])),
          apShares: apSharesAfterRedeem.toString(),
          shareSupply: supplyAfterRedeem.toString(),
        },
      },
      nav: {
        navPerShare: NAV_PER_SHARE.toString(),
        navPerShareScale: "1000000000 == 1.0",
        asOfDate,
        session: 0,
        tier: 1,
        rulebookVersion: "rulebook-v3.0.0",
        attestorsRecorded: v.latestNav.attestorCount,
        fixingRef: `0x${fixingRef.toString("hex")}`,
        fixingRefPreimage,
        fixingRefIsATestReference: true,
      },
      navSecondPosting: {
        navPerShare: (NAV_PER_SHARE + 10_000_000n).toString(),
        asOfDate: asOfDate + 1,
        tier: 1,
        attestorsRecorded: after.latestNav.attestorCount,
        signedBy: "attestors[0] + attestors[2], one Ed25519 instruction each (unpacked)",
      },
      ed25519Shapes: {
        firstPosting: "PACKED - 2 signatures in a single Ed25519 instruction",
        secondPosting: "UNPACKED - one Ed25519 instruction per signature",
        note:
          "A committee of 3+ MUST pack: 3 separate instructions plus post_nav is 1424 bytes " +
          "against the 1232-byte transaction limit.",
      },
      negatives: [neg1, neg2, neg3],
    },
    transactions: Object.fromEntries(
      Object.entries(txs).map(([k, sig]) => [k, { signature: sig, explorer: EXPLORER("tx", sig) }]),
    ),
    addressExplorerLinks: {
      program: EXPLORER("address", programId.toBase58()),
      vault: EXPLORER("address", vault.toBase58()),
      shareMint: EXPLORER("address", shareMint.toBase58()),
      ...Object.fromEntries(MOCKS.map((m, i) => [m.symbol, EXPLORER("address", mints[i].toBase58())])),
    },
    ranAt: new Date().toISOString(),
  };

  const dir = path.join(__dirname, "..", "deployments");
  fs.mkdirSync(dir, { recursive: true });
  const file = path.join(dir, "devnet.json");
  fs.writeFileSync(file, JSON.stringify(out, null, 2) + "\n");
  console.log(`\nwrote ${file}`);
  console.log("\nALL ASSERTIONS PASSED. Mocks, invented NAV, devnet only.");
}

function errorMessageFor(idl: any, code: string): string {
  return idl.errors?.find((e: { name: string; msg: string }) => e.name === code)?.msg ?? "";
}

main().catch((e) => {
  console.error("\nFAILED:", e?.message ?? e);
  if (e?.logs) console.error(e.logs.join("\n"));
  process.exit(1);
});
