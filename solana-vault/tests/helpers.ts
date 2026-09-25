import * as anchor from "@coral-xyz/anchor";
import { BN, Program } from "@coral-xyz/anchor";
import {
  Keypair,
  PublicKey,
  SystemProgram,
  Transaction,
  TransactionInstruction,
  Ed25519Program,
} from "@solana/web3.js";
import {
  TOKEN_PROGRAM_ID,
  TOKEN_2022_PROGRAM_ID,
  ASSOCIATED_TOKEN_PROGRAM_ID,
  AccountLayout,
  MintLayout,
  MINT_SIZE,
  ACCOUNT_SIZE,
  createInitializeMint2Instruction,
  createAssociatedTokenAccountIdempotentInstruction,
  createMintToInstruction,
  getAssociatedTokenAddressSync,
} from "@solana/spl-token";
import nacl from "tweetnacl";
import { createHash, randomBytes } from "crypto";
import { expect } from "chai";
import { EtpBasketVault } from "../target/types/etp_basket_vault";

export const SHARE_UNIT = 1_000_000_000n;
export const NAV_DOMAIN = Buffer.from("ETPFOUNDRY_NAV_FIXING_V1");
export const ONE_DAY = 86_400;

// ------------------------------------------------------------------------------------
// Provider abstraction: the same helpers drive AnchorProvider (validator) and
// BankrunProvider (in-process bank with a warpable clock).
// ------------------------------------------------------------------------------------

export interface Env {
  provider: anchor.Provider & {
    sendAndConfirm(tx: Transaction, signers?: Keypair[]): Promise<string>;
  };
  payer: Keypair;
  program: Program<EtpBasketVault>;
  getAccountData(pubkey: PublicKey): Promise<Buffer | null>;
}

export async function send(env: Env, ixs: TransactionInstruction[], signers: Keypair[] = []) {
  const tx = new Transaction().add(...ixs);
  return env.provider.sendAndConfirm(tx, signers);
}

/** Rent-exempt lamports for `size` bytes, per the Rent sysvar defaults (3480 lamports
 *  per byte-year, 2-year exemption threshold, 128 bytes of account overhead). Computed
 *  locally so no RPC call is needed under bankrun. */
export function rentExempt(size: number): number {
  return (128 + size) * 3480 * 2;
}

export async function createMint(
  env: Env,
  decimals: number,
  programId: PublicKey,
  mintAuthority: PublicKey,
): Promise<PublicKey> {
  const mint = Keypair.generate();
  await send(
    env,
    [
      SystemProgram.createAccount({
        fromPubkey: env.payer.publicKey,
        newAccountPubkey: mint.publicKey,
        space: MINT_SIZE,
        lamports: rentExempt(MINT_SIZE),
        programId,
      }),
      createInitializeMint2Instruction(mint.publicKey, decimals, mintAuthority, null, programId),
    ],
    [mint],
  );
  return mint.publicKey;
}

export function ata(mint: PublicKey, owner: PublicKey, programId: PublicKey): PublicKey {
  return getAssociatedTokenAddressSync(mint, owner, true, programId, ASSOCIATED_TOKEN_PROGRAM_ID);
}

export async function createAta(env: Env, mint: PublicKey, owner: PublicKey, programId: PublicKey) {
  const address = ata(mint, owner, programId);
  await send(env, [
    createAssociatedTokenAccountIdempotentInstruction(
      env.payer.publicKey,
      address,
      owner,
      mint,
      programId,
      ASSOCIATED_TOKEN_PROGRAM_ID,
    ),
  ]);
  return address;
}

export async function mintTo(
  env: Env,
  mint: PublicKey,
  dest: PublicKey,
  authority: Keypair,
  amount: bigint,
  programId: PublicKey,
) {
  await send(env, [createMintToInstruction(mint, dest, authority.publicKey, amount, [], programId)], [authority]);
}

export async function tokenBalance(env: Env, account: PublicKey): Promise<bigint> {
  const data = await env.getAccountData(account);
  if (!data) throw new Error(`token account ${account.toBase58()} does not exist`);
  return AccountLayout.decode(data.subarray(0, ACCOUNT_SIZE)).amount;
}

export async function mintSupply(env: Env, mint: PublicKey): Promise<bigint> {
  const data = await env.getAccountData(mint);
  if (!data) throw new Error(`mint ${mint.toBase58()} does not exist`);
  return MintLayout.decode(data.subarray(0, MINT_SIZE)).supply;
}

export async function airdropTo(env: Env, to: PublicKey, lamports: number) {
  await send(env, [SystemProgram.transfer({ fromPubkey: env.payer.publicKey, toPubkey: to, lamports })]);
}

// ------------------------------------------------------------------------------------
// Vault fixtures
// ------------------------------------------------------------------------------------

export interface ConstituentSpec {
  mint: PublicKey;
  programId: PublicKey;
  decimals: number;
  unitsPerShare: bigint;
  mintAuthority: Keypair;
}

export interface VaultFixture {
  instrumentId: Buffer;
  vault: PublicKey;
  shareMint: PublicKey;
  registry: PublicKey;
  admin: Keypair;
  ap: Keypair;
  feeRecipient: Keypair;
  feeShareAccount: PublicKey;
  apShareAccount: PublicKey;
  attestors: Keypair[];
  threshold: number;
  constituents: ConstituentSpec[];
  vaultAtas: PublicKey[];
  apAtas: PublicKey[];
  createFeeBps: number;
  redeemFeeBps: number;
  managementFeeBps: number;
  rebalanceDelaySecs: number;
}

export function vaultPda(programId: PublicKey, instrumentId: Buffer): PublicKey {
  return PublicKey.findProgramAddressSync([Buffer.from("vault"), instrumentId], programId)[0];
}
export function shareMintPda(programId: PublicKey, vault: PublicKey): PublicKey {
  return PublicKey.findProgramAddressSync([Buffer.from("share_mint"), vault.toBuffer()], programId)[0];
}
export function registryPda(programId: PublicKey, vault: PublicKey): PublicKey {
  return PublicKey.findProgramAddressSync([Buffer.from("registry"), vault.toBuffer()], programId)[0];
}

export interface FixtureOptions {
  createFeeBps?: number;
  redeemFeeBps?: number;
  managementFeeBps?: number;
  rebalanceDelaySecs?: number;
  threshold?: number;
  attestorCount?: number;
  /** AP funding per constituent, in base units. */
  apFunding?: bigint;
}

/** Deploys a fresh vault with three constituents: A (SPL Token, 6 dp), B (SPL Token,
 *  9 dp) and C (Token-2022, 0 dp). Weights are chosen so rounding is exercised. */
export async function deployVault(env: Env, opts: FixtureOptions = {}): Promise<VaultFixture> {
  const programId = env.program.programId;
  const admin = Keypair.generate();
  const ap = Keypair.generate();
  const feeRecipient = Keypair.generate();
  await airdropTo(env, admin.publicKey, 2_000_000_000);
  await airdropTo(env, ap.publicKey, 2_000_000_000);
  await airdropTo(env, feeRecipient.publicKey, 100_000_000);

  const attestorCount = opts.attestorCount ?? 3;
  const threshold = opts.threshold ?? 2;
  const attestors = Array.from({ length: attestorCount }, () => Keypair.generate());

  const specs: Omit<ConstituentSpec, "mint">[] = [
    { programId: TOKEN_PROGRAM_ID, decimals: 6, unitsPerShare: 1_500_000n, mintAuthority: Keypair.generate() },
    { programId: TOKEN_PROGRAM_ID, decimals: 9, unitsPerShare: 333_333_333n, mintAuthority: Keypair.generate() },
    { programId: TOKEN_2022_PROGRAM_ID, decimals: 0, unitsPerShare: 2n, mintAuthority: Keypair.generate() },
  ];
  const constituents: ConstituentSpec[] = [];
  for (const s of specs) {
    const mint = await createMint(env, s.decimals, s.programId, s.mintAuthority.publicKey);
    constituents.push({ ...s, mint });
  }

  const instrumentId = randomBytes(32);
  const vault = vaultPda(programId, instrumentId);
  const shareMint = shareMintPda(programId, vault);
  const registry = registryPda(programId, vault);

  const createFeeBps = opts.createFeeBps ?? 25;
  const redeemFeeBps = opts.redeemFeeBps ?? 10;
  const managementFeeBps = opts.managementFeeBps ?? 50;
  const rebalanceDelaySecs = opts.rebalanceDelaySecs ?? ONE_DAY;

  await env.program.methods
    .initializeVault({
      instrumentId: Array.from(instrumentId),
      constituents: constituents.map((c) => ({ mint: c.mint, unitsPerShare: new BN(c.unitsPerShare.toString()) })),
      createFeeBps,
      redeemFeeBps,
      managementFeeBpsPerYear: managementFeeBps,
      feeRecipient: feeRecipient.publicKey,
      attestors: attestors.map((a) => a.publicKey),
      threshold,
      rebalanceDelaySecs: new BN(rebalanceDelaySecs),
    })
    .accountsStrict({
      authority: admin.publicKey,
      vault,
      shareMint,
      shareTokenProgram: TOKEN_PROGRAM_ID,
      systemProgram: SystemProgram.programId,
    })
    .signers([admin])
    .rpc();

  await env.program.methods
    .setAp(ap.publicKey, true)
    .accountsStrict({ authority: admin.publicKey, vault })
    .signers([admin])
    .rpc();

  // Share accounts for the AP and the fee recipient (classic SPL Token share mint).
  const apShareAccount = await createAta(env, shareMint, ap.publicKey, TOKEN_PROGRAM_ID);
  const feeShareAccount = await createAta(env, shareMint, feeRecipient.publicKey, TOKEN_PROGRAM_ID);

  // Constituent accounts: vault ATAs (owner off-curve) and AP ATAs, funded.
  const vaultAtas: PublicKey[] = [];
  const apAtas: PublicKey[] = [];
  const funding = opts.apFunding ?? 1_000_000_000_000n;
  for (const c of constituents) {
    vaultAtas.push(await createAta(env, c.mint, vault, c.programId));
    const apAta = await createAta(env, c.mint, ap.publicKey, c.programId);
    await mintTo(env, c.mint, apAta, c.mintAuthority, funding, c.programId);
    apAtas.push(apAta);
  }

  return {
    instrumentId,
    vault,
    shareMint,
    registry,
    admin,
    ap,
    feeRecipient,
    feeShareAccount,
    apShareAccount,
    attestors,
    threshold,
    constituents,
    vaultAtas,
    apAtas,
    createFeeBps,
    redeemFeeBps,
    managementFeeBps,
    rebalanceDelaySecs,
  };
}

// ------------------------------------------------------------------------------------
// Create / redeem
// ------------------------------------------------------------------------------------

export function ceilUnits(shares: bigint, unitsPerShare: bigint): bigint {
  return (shares * unitsPerShare + SHARE_UNIT - 1n) / SHARE_UNIT;
}
export function floorUnits(shares: bigint, unitsPerShare: bigint): bigint {
  return (shares * unitsPerShare) / SHARE_UNIT;
}
export function bpsFloor(amount: bigint, bps: number): bigint {
  return (amount * BigInt(bps)) / 10_000n;
}

export function createRemainingAccounts(f: VaultFixture, apAtas: PublicKey[] = f.apAtas) {
  return f.constituents.flatMap((c, i) => [
    { pubkey: c.mint, isSigner: false, isWritable: false },
    { pubkey: apAtas[i], isSigner: false, isWritable: true },
    { pubkey: f.vaultAtas[i], isSigner: false, isWritable: true },
  ]);
}
export function redeemRemainingAccounts(f: VaultFixture, apAtas: PublicKey[] = f.apAtas) {
  return f.constituents.flatMap((c, i) => [
    { pubkey: c.mint, isSigner: false, isWritable: false },
    { pubkey: f.vaultAtas[i], isSigner: false, isWritable: true },
    { pubkey: apAtas[i], isSigner: false, isWritable: true },
  ]);
}

export function createIx(
  env: Env,
  f: VaultFixture,
  shares: bigint,
  receiverShareAccount: PublicKey,
  opts: { ap?: Keypair; registry?: PublicKey | null; remaining?: ReturnType<typeof createRemainingAccounts> } = {},
) {
  const ap = opts.ap ?? f.ap;
  return env.program.methods
    .create(new BN(shares.toString()))
    .accountsStrict({
      vault: f.vault,
      ap: ap.publicKey,
      shareMint: f.shareMint,
      receiverShareAccount,
      feeShareAccount: f.feeShareAccount,
      holderRegistry: opts.registry ?? null,
      shareTokenProgram: TOKEN_PROGRAM_ID,
      tokenProgram: TOKEN_PROGRAM_ID,
      token2022Program: TOKEN_2022_PROGRAM_ID,
    })
    .remainingAccounts(opts.remaining ?? createRemainingAccounts(f))
    .signers([ap]);
}

export function redeemIx(
  env: Env,
  f: VaultFixture,
  shares: bigint,
  opts: { ap?: Keypair; apShareAccount?: PublicKey; remaining?: ReturnType<typeof redeemRemainingAccounts> } = {},
) {
  const ap = opts.ap ?? f.ap;
  return env.program.methods
    .redeem(new BN(shares.toString()))
    .accountsStrict({
      vault: f.vault,
      ap: ap.publicKey,
      shareMint: f.shareMint,
      apShareAccount: opts.apShareAccount ?? f.apShareAccount,
      feeShareAccount: f.feeShareAccount,
      shareTokenProgram: TOKEN_PROGRAM_ID,
      tokenProgram: TOKEN_PROGRAM_ID,
      token2022Program: TOKEN_2022_PROGRAM_ID,
    })
    .remainingAccounts(opts.remaining ?? redeemRemainingAccounts(f))
    .signers([ap]);
}

// ------------------------------------------------------------------------------------
// NAV fixings
// ------------------------------------------------------------------------------------

export interface Fixing {
  instrumentId: Buffer;
  asOfDate: number;
  session: number;
  navPerShare: bigint;
  rulebookVersion: Buffer;
  /** sha256 of the Canton NavFixing contract id string. */
  fixingRef: Buffer;
  /** Desk fixing tier; 1 = official committee fixing. */
  tier: number;
}

/** What the relay does off-chain: hash the Canton contract id into the 32-byte ref. */
export function fixingRefFor(cantonContractId: string): Buffer {
  return createHash("sha256").update(cantonContractId, "utf8").digest();
}

/** Must match `nav::canonical_nav_message` byte for byte. */
export function navMessage(programId: PublicKey, vault: PublicKey, f: Fixing): Buffer {
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

export function ed25519Ix(signer: Keypair, message: Buffer): TransactionInstruction {
  const signature = nacl.sign.detached(message, signer.secretKey);
  return Ed25519Program.createInstructionWithPublicKey({
    publicKey: signer.publicKey.toBytes(),
    message,
    signature,
  });
}

/** One Ed25519 native-program instruction carrying SEVERAL signatures over the SAME
 *  message, with a single shared copy of the message body.
 *
 *  WHY this exists: `Ed25519Program.createInstructionWithPublicKey` emits one instruction
 *  per signature, each repeating the 146-byte NAV message, and three of those plus
 *  `post_nav` overflows the 1232-byte legacy transaction limit (measured: 1424 bytes).
 *  The precompile and `nav::verify_attestations` both iterate `num_signatures` offsets
 *  within one instruction, so packing is the supported shape — and it is what a real
 *  relay must do for a committee larger than two.
 *
 *  Layout (agave `ed25519_instruction.rs`): num_signatures u8, padding u8, then one
 *  14-byte offsets record per signature, then the pubkeys, the signatures, and the
 *  message. Every instruction index is 0xFFFF, i.e. "this instruction". */
export function ed25519IxMulti(signers: Keypair[], message: Buffer): TransactionInstruction {
  const SELF = 0xffff;
  const n = signers.length;
  const headerLen = 2 + n * 14;
  const pubkeysAt = headerLen;
  const sigsAt = pubkeysAt + n * 32;
  const messageAt = sigsAt + n * 64;
  const data = Buffer.alloc(messageAt + message.length);

  data.writeUInt8(n, 0);
  data.writeUInt8(0, 1);
  signers.forEach((signer, i) => {
    const o = 2 + i * 14;
    data.writeUInt16LE(sigsAt + i * 64, o); // signature_offset
    data.writeUInt16LE(SELF, o + 2); // signature_instruction_index
    data.writeUInt16LE(pubkeysAt + i * 32, o + 4); // public_key_offset
    data.writeUInt16LE(SELF, o + 6); // public_key_instruction_index
    data.writeUInt16LE(messageAt, o + 8); // message_data_offset (shared)
    data.writeUInt16LE(message.length, o + 10); // message_data_size
    data.writeUInt16LE(SELF, o + 12); // message_instruction_index

    Buffer.from(signer.publicKey.toBytes()).copy(data, pubkeysAt + i * 32);
    Buffer.from(nacl.sign.detached(message, signer.secretKey)).copy(data, sigsAt + i * 64);
  });
  message.copy(data, messageAt);

  return new TransactionInstruction({ keys: [], programId: Ed25519Program.programId, data });
}

export function fixingArgs(f: Fixing) {
  return {
    instrumentId: Array.from(f.instrumentId),
    asOfDate: f.asOfDate,
    session: f.session,
    navPerShare: new BN(f.navPerShare.toString()),
    rulebookVersion: Array.from(f.rulebookVersion),
    fixingRef: Array.from(f.fixingRef),
    tier: f.tier,
  };
}

export function todayDays(): number {
  return Math.floor(Date.now() / 86_400_000);
}

export function makeFixing(
  f: VaultFixture,
  asOfDate: number,
  navPerShare = 101_250_000_000n,
  overrides: Partial<Pick<Fixing, "tier" | "fixingRef" | "session">> = {},
): Fixing {
  return {
    instrumentId: f.instrumentId,
    asOfDate,
    session: overrides.session ?? 0,
    navPerShare,
    rulebookVersion: Buffer.from("rulebook-v3.0.0".padEnd(32, "\0")),
    fixingRef: overrides.fixingRef ?? fixingRefFor(`00c0ffee:navfixing:${f.instrumentId.toString("hex")}:${asOfDate}`),
    tier: overrides.tier ?? 1,
  };
}

/** Builds a post_nav call with the given Ed25519 pre-instructions. `edIxs` may sign a
 *  different message than `fixing` describes, to test tampering. */
export function postNavIx(env: Env, f: VaultFixture, fixing: Fixing, edIxs: TransactionInstruction[], relay?: Keypair) {
  const relayKey = relay ?? env.payer;
  return env.program.methods
    .postNav(fixingArgs(fixing))
    .accountsStrict({
      relay: relayKey.publicKey,
      vault: f.vault,
      instructionsSysvar: anchor.web3.SYSVAR_INSTRUCTIONS_PUBKEY,
    })
    .preInstructions(edIxs)
    .signers(relay ? [relay] : []);
}

export function signedBy(env: Env, f: VaultFixture, fixing: Fixing, signers: Keypair[]) {
  const msg = navMessage(env.program.programId, f.vault, fixing);
  return signers.map((s) => ed25519Ix(s, msg));
}

/** Same as `signedBy`, but packs every signature into ONE Ed25519 instruction. Required
 *  for three or more attestors: see `ed25519IxMulti`. */
export function signedByPacked(env: Env, f: VaultFixture, fixing: Fixing, signers: Keypair[]) {
  const msg = navMessage(env.program.programId, f.vault, fixing);
  return [ed25519IxMulti(signers, msg)];
}

// ------------------------------------------------------------------------------------
// Assertions
// ------------------------------------------------------------------------------------

/** Asserts a promise rejects with the given Anchor error code (by name), searching the
 *  parsed AnchorError first and the raw logs/message second (the latter covers errors
 *  surfaced through bankrun or preflight simulation). */
export async function expectError(p: Promise<unknown>, code: string) {
  try {
    await p;
  } catch (e: any) {
    const parsed = e?.error?.errorCode?.code;
    if (parsed === code) return;
    const text = [e?.message, e?.toString?.(), ...(e?.logs ?? []), ...(e?.transactionLogs ?? [])]
      .filter(Boolean)
      .join("\n");
    if (text.includes(code) || text.includes(`Error Code: ${code}`)) return;
    // Bankrun surfaces "custom program error: 0x...." without the name: map via the IDL.
    const num = errorNumber(code);
    if (num !== undefined && (text.includes(`0x${num.toString(16)}`) || text.includes(`Error Number: ${num}`))) return;
    throw new Error(`expected error ${code}, got: ${text.slice(0, 1200)}`);
  }
  expect.fail(`expected error ${code}, but the call succeeded`);
}

function errorNumber(code: string): number | undefined {
  // eslint-disable-next-line @typescript-eslint/no-var-requires
  const idl = require("../target/idl/etp_basket_vault.json");
  return idl.errors?.find((e: { name: string; code: number }) => e.name === code)?.code;
}

/** Asserts a promise rejects for any reason and returns the error text. */
export async function expectFailure(p: Promise<unknown>): Promise<string> {
  try {
    await p;
  } catch (e: any) {
    return [e?.message, ...(e?.logs ?? [])].filter(Boolean).join("\n");
  }
  expect.fail("expected the call to fail, but it succeeded");
}

export function bnToBigint(v: BN): bigint {
  return BigInt(v.toString());
}
