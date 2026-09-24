import * as anchor from "@coral-xyz/anchor";
import { BN, Program } from "@coral-xyz/anchor";
import { Keypair, PublicKey, SystemProgram } from "@solana/web3.js";
import { TOKEN_PROGRAM_ID, TOKEN_2022_PROGRAM_ID, getMint } from "@solana/spl-token";
import { randomBytes } from "crypto";
import { expect } from "chai";
import { EtpBasketVault } from "../target/types/etp_basket_vault";
import {
  Env,
  VaultFixture,
  ONE_DAY,
  SHARE_UNIT,
  airdropTo,
  ata,
  bpsFloor,
  ceilUnits,
  createAta,
  createIx,
  createRemainingAccounts,
  deployVault,
  ed25519Ix,
  expectError,
  expectFailure,
  fixingRefFor,
  floorUnits,
  makeFixing,
  mintSupply,
  mintTo,
  navMessage,
  postNavIx,
  redeemIx,
  redeemRemainingAccounts,
  registryPda,
  shareMintPda,
  signedBy,
  todayDays,
  tokenBalance,
  vaultPda,
} from "./helpers";

describe("etp_basket_vault (validator)", () => {
  const provider = anchor.AnchorProvider.env();
  anchor.setProvider(provider);
  const program = anchor.workspace.EtpBasketVault as Program<EtpBasketVault>;
  const env: Env = {
    provider,
    payer: (provider.wallet as anchor.Wallet).payer,
    program,
    getAccountData: async (pk) => (await provider.connection.getAccountInfo(pk))?.data ?? null,
  };

  /** Raw initialize with overrides, for validation tests. Reuses the fixture's mints. */
  function initRaw(f: VaultFixture, over: Record<string, unknown>) {
    const instrumentId = randomBytes(32);
    const vault = vaultPda(program.programId, instrumentId);
    const shareMint = shareMintPda(program.programId, vault);
    const args = {
      instrumentId: Array.from(instrumentId),
      constituents: f.constituents.map((c) => ({ mint: c.mint, unitsPerShare: new BN(c.unitsPerShare.toString()) })),
      createFeeBps: 25,
      redeemFeeBps: 10,
      managementFeeBpsPerYear: 50,
      feeRecipient: f.feeRecipient.publicKey,
      attestors: f.attestors.map((a) => a.publicKey),
      threshold: 2,
      rebalanceDelaySecs: new BN(ONE_DAY),
      ...over,
    };
    return program.methods
      .initializeVault(args as any)
      .accountsStrict({
        authority: f.admin.publicKey,
        vault,
        shareMint,
        shareTokenProgram: TOKEN_PROGRAM_ID,
        systemProgram: SystemProgram.programId,
      })
      .signers([f.admin])
      .rpc();
  }

  // =================================================================================
  describe("initialize_vault", () => {
    let f: VaultFixture;
    before(async () => {
      f = await deployVault(env);
    });

    it("stores the configuration and makes the vault PDA the share mint authority", async () => {
      const v = await program.account.vault.fetch(f.vault);
      expect(v.authority.equals(f.admin.publicKey)).to.be.true;
      expect(Buffer.from(v.instrumentId).equals(f.instrumentId)).to.be.true;
      expect(v.shareMint.equals(f.shareMint)).to.be.true;
      expect(v.constituents.length).to.eq(3);
      expect(v.constituents[1].mint.equals(f.constituents[1].mint)).to.be.true;
      expect(v.constituents[1].unitsPerShare.toString()).to.eq("333333333");
      expect(v.createFeeBps).to.eq(25);
      expect(v.redeemFeeBps).to.eq(10);
      expect(v.managementFeeBpsPerYear).to.eq(50);
      expect(v.threshold).to.eq(2);
      expect(v.maxTier).to.eq(1);
      expect(v.attestors.length).to.eq(3);
      expect(v.paused).to.be.false;
      expect(v.redeemWhilePaused).to.be.false;
      expect(v.pendingBasket).to.be.null;
      expect(v.holderRegistry).to.be.null;
      expect(v.latestNav.postedAt.toNumber()).to.eq(0);
      expect(v.rebalanceDelaySecs.toNumber()).to.eq(ONE_DAY);
      expect(v.authorisedParticipants.map((k) => k.toBase58())).to.deep.eq([f.ap.publicKey.toBase58()]);

      const mint = await getMint(provider.connection, f.shareMint, undefined, TOKEN_PROGRAM_ID);
      expect(mint.decimals).to.eq(9);
      expect(mint.mintAuthority?.equals(f.vault)).to.be.true;
      expect(mint.freezeAuthority?.equals(f.vault)).to.be.true;
      expect(mint.supply).to.eq(0n);
    });

    it("rejects a threshold below 2", async () => {
      await expectError(initRaw(f, { threshold: 1 }), "ThresholdTooLow");
    });

    it("rejects a threshold above the attestor count", async () => {
      await expectError(initRaw(f, { threshold: 4 }), "ThresholdUnreachable");
    });

    it("rejects duplicate attestors", async () => {
      const a = f.attestors[0].publicKey;
      await expectError(initRaw(f, { attestors: [a, a, f.attestors[1].publicKey] }), "DuplicateAttestor");
    });

    it("rejects a rebalance delay below one day", async () => {
      await expectError(initRaw(f, { rebalanceDelaySecs: new BN(ONE_DAY - 1) }), "RebalanceDelayTooShort");
    });

    it("rejects a duplicated constituent mint", async () => {
      const c = f.constituents[0];
      await expectError(
        initRaw(f, {
          constituents: [
            { mint: c.mint, unitsPerShare: new BN(1) },
            { mint: c.mint, unitsPerShare: new BN(2) },
          ],
        }),
        "DuplicateConstituent",
      );
    });

    it("rejects a zero units-per-share weight", async () => {
      await expectError(
        initRaw(f, { constituents: [{ mint: f.constituents[0].mint, unitsPerShare: new BN(0) }] }),
        "ZeroUnitsPerShare",
      );
    });

    it("rejects an empty basket", async () => {
      await expectError(initRaw(f, { constituents: [] }), "EmptyBasket");
    });

    it("rejects fees above the hard caps", async () => {
      await expectError(initRaw(f, { createFeeBps: 1001 }), "FeeTooHigh");
      await expectError(initRaw(f, { managementFeeBpsPerYear: 501 }), "FeeTooHigh");
    });

    it("rejects an all-zero instrument id", async () => {
      await expectError(initRaw(f, { instrumentId: Array(32).fill(0) }), "ZeroInstrumentId");
    });
  });

  // =================================================================================
  describe("admin access control", () => {
    let f: VaultFixture;
    before(async () => {
      f = await deployVault(env);
    });

    it("refuses set_ap from a non-authority", async () => {
      const stranger = Keypair.generate();
      await airdropTo(env, stranger.publicKey, 100_000_000);
      await expectError(
        program.methods
          .setAp(stranger.publicKey, true)
          .accountsStrict({ authority: stranger.publicKey, vault: f.vault })
          .signers([stranger])
          .rpc(),
        "NotAuthority",
      );
    });

    it("removing an AP blocks its creates; re-adding restores them", async () => {
      await program.methods
        .setAp(f.ap.publicKey, false)
        .accountsStrict({ authority: f.admin.publicKey, vault: f.vault })
        .signers([f.admin])
        .rpc();
      await expectError(createIx(env, f, SHARE_UNIT, f.apShareAccount).rpc(), "NotAuthorisedParticipant");
      await program.methods
        .setAp(f.ap.publicKey, true)
        .accountsStrict({ authority: f.admin.publicKey, vault: f.vault })
        .signers([f.admin])
        .rpc();
      await createIx(env, f, SHARE_UNIT, f.apShareAccount).rpc();
      const v = await program.account.vault.fetch(f.vault);
      expect(v.authorisedParticipants.length).to.eq(1);
    });

    it("set_attestors enforces the same roster rules as initialise", async () => {
      const ks = [Keypair.generate().publicKey, Keypair.generate().publicKey];
      await expectError(
        program.methods
          .setAttestors(ks, 3)
          .accountsStrict({ authority: f.admin.publicKey, vault: f.vault })
          .signers([f.admin])
          .rpc(),
        "ThresholdUnreachable",
      );
      await program.methods
        .setAttestors(ks, 2)
        .accountsStrict({ authority: f.admin.publicKey, vault: f.vault })
        .signers([f.admin])
        .rpc();
      const v = await program.account.vault.fetch(f.vault);
      expect(v.attestors.map((k) => k.toBase58())).to.deep.eq(ks.map((k) => k.toBase58()));
    });

    it("set_authority hands over admin and locks the old key out", async () => {
      const next = Keypair.generate();
      await airdropTo(env, next.publicKey, 100_000_000);
      await program.methods
        .setAuthority(next.publicKey)
        .accountsStrict({ authority: f.admin.publicKey, vault: f.vault })
        .signers([f.admin])
        .rpc();
      await expectError(
        program.methods
          .setPause(true)
          .accountsStrict({ authority: f.admin.publicKey, vault: f.vault })
          .signers([f.admin])
          .rpc(),
        "NotAuthority",
      );
      await program.methods
        .setPause(true)
        .accountsStrict({ authority: next.publicKey, vault: f.vault })
        .signers([next])
        .rpc();
      expect((await program.account.vault.fetch(f.vault)).paused).to.be.true;
    });
  });

  // =================================================================================
  describe("create", () => {
    let f: VaultFixture;
    before(async () => {
      f = await deployVault(env);
    });

    it("deposits ceil-rounded units of every constituent and mints shares minus the fee", async () => {
      const shares = 1_234_567_891n; // 1.234567891 shares: forces rounding on every line
      const apBefore = await Promise.all(f.apAtas.map((a) => tokenBalance(env, a)));
      const vaultBefore = await Promise.all(f.vaultAtas.map((a) => tokenBalance(env, a)));

      await createIx(env, f, shares, f.apShareAccount).rpc();

      const expectedFee = bpsFloor(shares, f.createFeeBps);
      expect(await tokenBalance(env, f.apShareAccount)).to.eq(shares - expectedFee);
      expect(await tokenBalance(env, f.feeShareAccount)).to.eq(expectedFee);
      expect(await mintSupply(env, f.shareMint)).to.eq(shares);

      for (let i = 0; i < f.constituents.length; i++) {
        const units = ceilUnits(shares, f.constituents[i].unitsPerShare);
        expect(units * SHARE_UNIT >= shares * f.constituents[i].unitsPerShare, "ceil never under-collects").to.be.true;
        expect(await tokenBalance(env, f.apAtas[i])).to.eq(apBefore[i] - units, `AP balance line ${i}`);
        expect(await tokenBalance(env, f.vaultAtas[i])).to.eq(vaultBefore[i] + units, `vault balance line ${i}`);
      }
      // Concrete check of the rounding on line A (1.5 units/share, 6 dp):
      // 1234567891 * 1500000 / 1e9 = 1851851.8365 -> 1851852
      expect(ceilUnits(shares, 1_500_000n)).to.eq(1_851_852n);
      // line C (2 units/share, 0 dp): 2.469135782 -> 3
      expect(ceilUnits(shares, 2n)).to.eq(3n);
    });

    it("refuses a non-AP signer", async () => {
      const stranger = Keypair.generate();
      await airdropTo(env, stranger.publicKey, 500_000_000);
      const strangerAtas: PublicKey[] = [];
      for (const c of f.constituents) {
        const a = await createAta(env, c.mint, stranger.publicKey, c.programId);
        await mintTo(env, c.mint, a, c.mintAuthority, 10_000_000_000n, c.programId);
        strangerAtas.push(a);
      }
      await expectError(
        createIx(env, f, SHARE_UNIT, f.apShareAccount, {
          ap: stranger,
          remaining: createRemainingAccounts(f, strangerAtas),
        }).rpc(),
        "NotAuthorisedParticipant",
      );
    });

    it("is atomic: one underfunded constituent fails the whole create and mints nothing", async () => {
      const ap2 = Keypair.generate();
      await airdropTo(env, ap2.publicKey, 500_000_000);
      await program.methods
        .setAp(ap2.publicKey, true)
        .accountsStrict({ authority: f.admin.publicKey, vault: f.vault })
        .signers([f.admin])
        .rpc();
      const ap2Share = await createAta(env, f.shareMint, ap2.publicKey, TOKEN_PROGRAM_ID);
      const ap2Atas: PublicKey[] = [];
      for (let i = 0; i < f.constituents.length; i++) {
        const c = f.constituents[i];
        const a = await createAta(env, c.mint, ap2.publicKey, c.programId);
        // Fund A and B generously, leave C (the last line) EMPTY.
        if (i < 2) await mintTo(env, c.mint, a, c.mintAuthority, 10_000_000_000n, c.programId);
        ap2Atas.push(a);
      }
      const supplyBefore = await mintSupply(env, f.shareMint);
      const aBefore = await tokenBalance(env, ap2Atas[0]);
      const vaultABefore = await tokenBalance(env, f.vaultAtas[0]);

      const text = await expectFailure(
        createIx(env, f, SHARE_UNIT, ap2Share, { ap: ap2, remaining: createRemainingAccounts(f, ap2Atas) }).rpc(),
      );
      expect(text.toLowerCase()).to.include("insufficient funds");

      expect(await mintSupply(env, f.shareMint)).to.eq(supplyBefore);
      expect(await tokenBalance(env, ap2Share)).to.eq(0n);
      expect(await tokenBalance(env, ap2Atas[0]), "line A was not taken").to.eq(aBefore);
      expect(await tokenBalance(env, f.vaultAtas[0])).to.eq(vaultABefore);
    });

    it("refuses remaining accounts out of basket order", async () => {
      const rem = createRemainingAccounts(f);
      const swapped = [...rem.slice(3, 6), ...rem.slice(0, 3), ...rem.slice(6)];
      await expectError(createIx(env, f, SHARE_UNIT, f.apShareAccount, { remaining: swapped }).rpc(), "ConstituentOrderMismatch");
    });

    it("refuses a short remaining-accounts list", async () => {
      const rem = createRemainingAccounts(f).slice(0, 6);
      await expectError(createIx(env, f, SHARE_UNIT, f.apShareAccount, { remaining: rem }).rpc(), "ConstituentAccountsMismatch");
    });

    it("refuses zero shares", async () => {
      await expectError(createIx(env, f, 0n, f.apShareAccount).rpc(), "ZeroAmount");
    });

    it("refuses a source token account not owned by the AP", async () => {
      const other = Keypair.generate();
      const c = f.constituents[0];
      const otherAta = await createAta(env, c.mint, other.publicKey, c.programId);
      const rem = createRemainingAccounts(f);
      rem[1] = { pubkey: otherAta, isSigner: false, isWritable: true };
      await expectError(createIx(env, f, SHARE_UNIT, f.apShareAccount, { remaining: rem }).rpc(), "SourceNotOwnedByAp");
    });

    it("refuses a destination token account not owned by the vault", async () => {
      const rem = createRemainingAccounts(f);
      rem[2] = { pubkey: f.apAtas[0], isSigner: false, isWritable: true };
      await expectError(createIx(env, f, SHARE_UNIT, f.apShareAccount, { remaining: rem }).rpc(), "DestinationNotOwnedByVault");
    });

    it("refuses a fee share account that is not the fee recipient's", async () => {
      await expectError(
        program.methods
          .create(new BN(SHARE_UNIT.toString()))
          .accountsStrict({
            vault: f.vault,
            ap: f.ap.publicKey,
            shareMint: f.shareMint,
            receiverShareAccount: f.apShareAccount,
            feeShareAccount: f.apShareAccount,
            holderRegistry: null,
            shareTokenProgram: TOKEN_PROGRAM_ID,
            tokenProgram: TOKEN_PROGRAM_ID,
            token2022Program: TOKEN_2022_PROGRAM_ID,
          })
          .remainingAccounts(createRemainingAccounts(f))
          .signers([f.ap])
          .rpc(),
        "WrongFeeAccount",
      );
    });
  });

  // =================================================================================
  describe("holder registry", () => {
    let f: VaultFixture;
    let receiver: Keypair;
    let receiverShare: PublicKey;
    before(async () => {
      f = await deployVault(env);
      receiver = Keypair.generate();
      receiverShare = await createAta(env, f.shareMint, receiver.publicKey, TOKEN_PROGRAM_ID);
      await program.methods
        .initHolderRegistry()
        .accountsStrict({
          authority: f.admin.publicKey,
          vault: f.vault,
          holderRegistry: f.registry,
          systemProgram: SystemProgram.programId,
        })
        .signers([f.admin])
        .rpc();
      await program.methods
        .setHolderRegistry(true)
        .accountsStrict({ authority: f.admin.publicKey, vault: f.vault, holderRegistry: f.registry })
        .signers([f.admin])
        .rpc();
    });

    it("is a PDA of the vault and starts empty and enabled", async () => {
      expect(f.registry.equals(registryPda(program.programId, f.vault))).to.be.true;
      const r = await program.account.holderRegistry.fetch(f.registry);
      expect(r.vault.equals(f.vault)).to.be.true;
      expect(r.allowed.length).to.eq(0);
      const v = await program.account.vault.fetch(f.vault);
      expect(v.holderRegistry?.equals(f.registry)).to.be.true;
    });

    it("blocks a create to a receiver that is not allow-listed", async () => {
      await expectError(createIx(env, f, SHARE_UNIT, receiverShare, { registry: f.registry }).rpc(), "ReceiverNotAllowed");
    });

    it("blocks a create that omits the registry account while one is enabled", async () => {
      await expectError(createIx(env, f, SHARE_UNIT, receiverShare, { registry: null }).rpc(), "HolderRegistryMismatch");
    });

    it("allows the create once the receiver is allow-listed", async () => {
      await program.methods
        .setHolderAllowed(receiver.publicKey, true)
        .accountsStrict({ authority: f.admin.publicKey, holderRegistry: f.registry })
        .signers([f.admin])
        .rpc();
      await createIx(env, f, SHARE_UNIT, receiverShare, { registry: f.registry }).rpc();
      expect(await tokenBalance(env, receiverShare)).to.eq(SHARE_UNIT - bpsFloor(SHARE_UNIT, f.createFeeBps));
    });

    it("blocks again after the receiver is removed", async () => {
      await program.methods
        .setHolderAllowed(receiver.publicKey, false)
        .accountsStrict({ authority: f.admin.publicKey, holderRegistry: f.registry })
        .signers([f.admin])
        .rpc();
      await expectError(createIx(env, f, SHARE_UNIT, receiverShare, { registry: f.registry }).rpc(), "ReceiverNotAllowed");
    });

    it("refuses registry edits from a non-authority", async () => {
      const stranger = Keypair.generate();
      await airdropTo(env, stranger.publicKey, 100_000_000);
      await expectError(
        program.methods
          .setHolderAllowed(stranger.publicKey, true)
          .accountsStrict({ authority: stranger.publicKey, holderRegistry: f.registry })
          .signers([stranger])
          .rpc(),
        "NotAuthority",
      );
    });

    it("disabling the registry lifts the restriction", async () => {
      await program.methods
        .setHolderRegistry(false)
        .accountsStrict({ authority: f.admin.publicKey, vault: f.vault, holderRegistry: f.registry })
        .signers([f.admin])
        .rpc();
      await createIx(env, f, SHARE_UNIT, receiverShare, { registry: null }).rpc();
    });
  });

  // =================================================================================
  describe("redeem", () => {
    let f: VaultFixture;
    const created = 5_000_000_000n; // 5 shares in, net of the create fee
    before(async () => {
      f = await deployVault(env);
      await createIx(env, f, created, f.apShareAccount).rpc();
    });

    it("burns net shares, pays the fee in shares and returns floor-rounded units", async () => {
      const shares = 1_234_567_891n;
      const apShareBefore = await tokenBalance(env, f.apShareAccount);
      const feeBefore = await tokenBalance(env, f.feeShareAccount);
      const supplyBefore = await mintSupply(env, f.shareMint);
      const apBefore = await Promise.all(f.apAtas.map((a) => tokenBalance(env, a)));
      const vaultBefore = await Promise.all(f.vaultAtas.map((a) => tokenBalance(env, a)));

      await redeemIx(env, f, shares).rpc();

      const fee = bpsFloor(shares, f.redeemFeeBps);
      const net = shares - fee;
      expect(await tokenBalance(env, f.apShareAccount)).to.eq(apShareBefore - shares);
      expect(await tokenBalance(env, f.feeShareAccount)).to.eq(feeBefore + fee);
      expect(await mintSupply(env, f.shareMint)).to.eq(supplyBefore - net);
      for (let i = 0; i < f.constituents.length; i++) {
        const units = floorUnits(net, f.constituents[i].unitsPerShare);
        expect(units * SHARE_UNIT <= net * f.constituents[i].unitsPerShare, "floor never over-pays").to.be.true;
        expect(await tokenBalance(env, f.apAtas[i])).to.eq(apBefore[i] + units, `AP line ${i}`);
        expect(await tokenBalance(env, f.vaultAtas[i])).to.eq(vaultBefore[i] - units, `vault line ${i}`);
      }
      // net = 1234567891 - 1234567 = 1233333324; line A: 1233333324*1.5e6/1e9 = 1849999.986 -> 1849999
      expect(floorUnits(net, 1_500_000n)).to.eq(1_849_999n);
    });

    it("the vault never ends up short: total units held cover the outstanding supply", async () => {
      const supply = await mintSupply(env, f.shareMint);
      for (let i = 0; i < f.constituents.length; i++) {
        const held = await tokenBalance(env, f.vaultAtas[i]);
        expect(held >= floorUnits(supply, f.constituents[i].unitsPerShare), `line ${i} covers supply`).to.be.true;
      }
    });

    it("refuses to redeem more shares than the AP holds", async () => {
      const bal = await tokenBalance(env, f.apShareAccount);
      const text = await expectFailure(redeemIx(env, f, bal + 1n).rpc());
      expect(text.toLowerCase()).to.include("insufficient funds");
    });

    it("refuses a non-AP", async () => {
      const stranger = Keypair.generate();
      await airdropTo(env, stranger.publicKey, 200_000_000);
      const strangerShare = await createAta(env, f.shareMint, stranger.publicKey, TOKEN_PROGRAM_ID);
      const strangerAtas: PublicKey[] = [];
      for (const c of f.constituents) strangerAtas.push(await createAta(env, c.mint, stranger.publicKey, c.programId));
      await expectError(
        redeemIx(env, f, 1n, { ap: stranger, apShareAccount: strangerShare, remaining: redeemRemainingAccounts(f, strangerAtas) }).rpc(),
        "NotAuthorisedParticipant",
      );
    });

    it("refuses a destination token account not owned by the AP", async () => {
      const other = Keypair.generate();
      const c = f.constituents[0];
      const otherAta = await createAta(env, c.mint, other.publicKey, c.programId);
      const rem = redeemRemainingAccounts(f);
      rem[2] = { pubkey: otherAta, isSigner: false, isWritable: true };
      await expectError(redeemIx(env, f, SHARE_UNIT, { remaining: rem }).rpc(), "DestinationNotOwnedByAp");
    });
  });

  // =================================================================================
  describe("post_nav (K-of-N committee fixing via Ed25519 introspection)", () => {
    let f: VaultFixture;
    let today: number;
    before(async () => {
      f = await deployVault(env, { attestorCount: 3, threshold: 2 });
      today = todayDays();
    });

    it("accepts 2-of-3 and stores the fixing, fixing_ref, tier and attestor count", async () => {
      const cantonCid = "00a1b2c3d4:NavFixing:ETPF-BASKET-1";
      const fx = makeFixing(f, today - 1, 101_250_000_000n, { fixingRef: fixingRefFor(cantonCid) });
      await postNavIx(env, f, fx, signedBy(env, f, fx, [f.attestors[0], f.attestors[2]])).rpc();

      const v = await program.account.vault.fetch(f.vault);
      expect(v.latestNav.asOfDate).to.eq(today - 1);
      expect(v.latestNav.navPerShare.toString()).to.eq("101250000000");
      expect(v.latestNav.session).to.eq(0);
      expect(v.latestNav.attestorCount).to.eq(2);
      expect(v.latestNav.tier).to.eq(1);
      expect(Buffer.from(v.latestNav.fixingRef).equals(fixingRefFor(cantonCid))).to.be.true;
      expect(Buffer.from(v.latestNav.rulebookVersion).equals(fx.rulebookVersion)).to.be.true;
      expect(v.latestNav.postedAt.toNumber()).to.be.greaterThan(0);
    });

    it("refuses a replay of the same as-of date", async () => {
      const fx = makeFixing(f, today - 1);
      await expectError(postNavIx(env, f, fx, signedBy(env, f, fx, [f.attestors[0], f.attestors[1]])).rpc(), "StaleFixing");
    });

    it("refuses an older as-of date", async () => {
      const fx = makeFixing(f, today - 2);
      await expectError(postNavIx(env, f, fx, signedBy(env, f, fx, [f.attestors[0], f.attestors[1]])).rpc(), "StaleFixing");
    });

    it("refuses the same attestor signing twice (2 signatures, 1 key)", async () => {
      const fx = makeFixing(f, today);
      await expectError(
        postNavIx(env, f, fx, signedBy(env, f, fx, [f.attestors[1], f.attestors[1]])).rpc(),
        "DuplicateAttestation",
      );
    });

    it("refuses fewer signatures than the threshold", async () => {
      const fx = makeFixing(f, today);
      await expectError(postNavIx(env, f, fx, signedBy(env, f, fx, [f.attestors[0]])).rpc(), "InsufficientAttestations");
    });

    it("refuses with no Ed25519 instructions at all", async () => {
      const fx = makeFixing(f, today);
      await expectError(postNavIx(env, f, fx, []).rpc(), "InsufficientAttestations");
    });

    it("refuses a signature from a key that is not a registered attestor", async () => {
      const fx = makeFixing(f, today);
      const outsider = Keypair.generate();
      await expectError(postNavIx(env, f, fx, signedBy(env, f, fx, [f.attestors[0], outsider])).rpc(), "UnknownAttestor");
    });

    it("refuses signatures over a different message than the posted fixing (tampering)", async () => {
      const signedFx = makeFixing(f, today, 99_000_000_000n);
      const postedFx = makeFixing(f, today, 150_000_000_000n);
      await expectError(
        postNavIx(env, f, postedFx, signedBy(env, f, signedFx, [f.attestors[0], f.attestors[1]])).rpc(),
        "SignedMessageMismatch",
      );
    });

    it("refuses signatures bound to another vault (message includes the vault key)", async () => {
      const fx = makeFixing(f, today);
      const otherVault = Keypair.generate().publicKey;
      const msg = navMessage(program.programId, otherVault, fx);
      const ixs = [f.attestors[0], f.attestors[1]].map((a) => ed25519Ix(a, msg));
      await expectError(postNavIx(env, f, fx, ixs).rpc(), "SignedMessageMismatch");
    });

    it("refuses a fixing for a different instrument id", async () => {
      const fx = { ...makeFixing(f, today), instrumentId: randomBytes(32) };
      await expectError(postNavIx(env, f, fx, signedBy(env, f, fx, [f.attestors[0], f.attestors[1]])).rpc(), "WrongInstrument");
    });

    it("refuses an as-of date in the future", async () => {
      const fx = makeFixing(f, today + 2);
      await expectError(postNavIx(env, f, fx, signedBy(env, f, fx, [f.attestors[0], f.attestors[1]])).rpc(), "FixingInFuture");
    });

    it("refuses a zero NAV", async () => {
      const fx = makeFixing(f, today, 0n);
      await expectError(postNavIx(env, f, fx, signedBy(env, f, fx, [f.attestors[0], f.attestors[1]])).rpc(), "ZeroNav");
    });

    it("refuses a forged signature (the Ed25519 precompile itself rejects it)", async () => {
      const fx = makeFixing(f, today);
      const msg = navMessage(program.programId, f.vault, fx);
      const good = ed25519Ix(f.attestors[0], msg);
      const forged = anchor.web3.Ed25519Program.createInstructionWithPublicKey({
        publicKey: f.attestors[1].publicKey.toBytes(),
        message: msg,
        signature: randomBytes(64),
      });
      const text = await expectFailure(postNavIx(env, f, fx, [good, forged]).rpc());
      expect(text.toLowerCase()).to.match(/precompile|signature|verification/);
    });

    // Tier scale (SeriesRow.labelFor): 0 seed, 1 attested, 2 alternate-seats,
    // 3 benchmark-x-factor, 4 carried-forward, 5 missed. Gate is tier <= max_tier.
    const setMaxTier = (f: VaultFixture, t: number, signer: Keypair = f.admin) =>
      program.methods.setMaxTier(t).accountsStrict({ authority: signer.publicKey, vault: f.vault }).signers([signer]).rpc();
    const post = (f: VaultFixture, fx: ReturnType<typeof makeFixing>) =>
      postNavIx(env, f, fx, signedBy(env, f, fx, [f.attestors[0], f.attestors[1]])).rpc();

    it("tier 4 (carried-forward) is refused at the default max_tier of 1", async () => {
      const fx = makeFixing(f, today, 102_000_000_000n, { tier: 4 });
      await expectError(post(f, fx), "TierExceedsMaximum");
    });

    it("a relay cannot relabel a carried-forward print as attested: the tier is signed", async () => {
      const signed = makeFixing(f, today, 102_000_000_000n, { tier: 4 });
      const relabelled = { ...signed, tier: 1 };
      await expectError(
        postNavIx(env, f, relabelled, signedBy(env, f, signed, [f.attestors[0], f.attestors[1]])).rpc(),
        "SignedMessageMismatch",
      );
    });

    it("tier 0 (seed) is refused at EVERY max_tier setting", async () => {
      for (const maxTier of [1, 2, 3, 4, 5]) {
        await setMaxTier(f, maxTier);
        expect((await program.account.vault.fetch(f.vault)).maxTier).to.eq(maxTier);
        const seed = makeFixing(f, today, 102_000_000_000n, { tier: 0 });
        await expectError(post(f, seed), "SeedTierRefused");
      }
      await setMaxTier(f, 1);
    });

    it("tier 4 is accepted once max_tier is 4, and tier 5 (missed) is still refused", async () => {
      await setMaxTier(f, 4);
      const missed = makeFixing(f, today, 102_000_000_000n, { tier: 5 });
      await expectError(post(f, missed), "TierExceedsMaximum");
      const carried = makeFixing(f, today, 102_000_000_000n, { tier: 4 });
      await post(f, carried);
      const v = await program.account.vault.fetch(f.vault);
      expect(v.latestNav.tier).to.eq(4);
      expect(v.latestNav.asOfDate).to.eq(today);
      expect(v.latestNav.navPerShare.toString()).to.eq("102000000000");
    });

    it("tier 1 (attested) is accepted at the default setting", async () => {
      const g = await deployVault(env, { attestorCount: 3, threshold: 2 });
      const fx = makeFixing(g, today, 100_000_000_000n, { tier: 1 });
      await postNavIx(env, g, fx, signedBy(env, g, fx, [g.attestors[0], g.attestors[1]])).rpc();
      expect((await program.account.vault.fetch(g.vault)).latestNav.tier).to.eq(1);
    });

    it("set_max_tier refuses 0 and 6, and is admin-only", async () => {
      await expectError(setMaxTier(f, 0), "MaxTierOutOfRange");
      await expectError(setMaxTier(f, 6), "MaxTierOutOfRange");
      const stranger = Keypair.generate();
      await airdropTo(env, stranger.publicKey, 100_000_000);
      await expectError(setMaxTier(f, 2, stranger), "NotAuthority");
      expect((await program.account.vault.fetch(f.vault)).maxTier).to.eq(4);
    });

    it("3-of-3 is accepted by any relay, records all three attestors", async () => {
      const g = await deployVault(env, { attestorCount: 3, threshold: 2 });
      const relay = Keypair.generate();
      await airdropTo(env, relay.publicKey, 200_000_000);
      const fx = makeFixing(g, today);
      await postNavIx(env, g, fx, signedBy(env, g, fx, g.attestors), relay).rpc();
      const v = await program.account.vault.fetch(g.vault);
      expect(v.latestNav.attestorCount).to.eq(3);
    });

    it("rotating the committee invalidates the old members' signatures", async () => {
      const g = await deployVault(env, { attestorCount: 3, threshold: 2 });
      const fresh = [Keypair.generate(), Keypair.generate()];
      await program.methods
        .setAttestors(fresh.map((k) => k.publicKey), 2)
        .accountsStrict({ authority: g.admin.publicKey, vault: g.vault })
        .signers([g.admin])
        .rpc();
      const fx = makeFixing(g, today);
      await expectError(postNavIx(env, g, fx, signedBy(env, g, fx, [g.attestors[0], g.attestors[1]])).rpc(), "UnknownAttestor");
      await postNavIx(env, g, fx, signedBy(env, g, fx, fresh)).rpc();
    });
  });

  // =================================================================================
  describe("rebalance timelock", () => {
    let f: VaultFixture;
    before(async () => {
      f = await deployVault(env);
    });

    const newWeights = (f: VaultFixture) =>
      f.constituents.map((c, i) => ({ mint: c.mint, unitsPerShare: new BN((c.unitsPerShare + BigInt(i + 1)).toString()) }));

    it("refuses a proposal from a non-authority", async () => {
      const stranger = Keypair.generate();
      await airdropTo(env, stranger.publicKey, 100_000_000);
      await expectError(
        program.methods
          .proposeBasket(newWeights(f))
          .accountsStrict({ authority: stranger.publicKey, vault: f.vault })
          .signers([stranger])
          .rpc(),
        "NotAuthority",
      );
    });

    it("stores the pending basket with apply_after = now + delay", async () => {
      const before = Math.floor(Date.now() / 1000);
      await program.methods
        .proposeBasket(newWeights(f))
        .accountsStrict({ authority: f.admin.publicKey, vault: f.vault })
        .signers([f.admin])
        .rpc();
      const v = await program.account.vault.fetch(f.vault);
      expect(v.pendingBasket).to.not.be.null;
      const applyAfter = v.pendingBasket!.applyAfter.toNumber();
      expect(applyAfter).to.be.within(before + ONE_DAY - 30, before + ONE_DAY + 60);
      expect(v.pendingBasket!.constituents[0].unitsPerShare.toString()).to.eq("1500001");
      // live basket untouched
      expect(v.constituents[0].unitsPerShare.toString()).to.eq("1500000");
    });

    it("refuses a second proposal while one is pending", async () => {
      await expectError(
        program.methods
          .proposeBasket(newWeights(f))
          .accountsStrict({ authority: f.admin.publicKey, vault: f.vault })
          .signers([f.admin])
          .rpc(),
        "BasketAlreadyPending",
      );
    });

    it("refuses to apply before the timelock elapses", async () => {
      await expectError(
        program.methods.applyBasket().accountsStrict({ authority: f.admin.publicKey, vault: f.vault }).signers([f.admin]).rpc(),
        "TimelockNotElapsed",
      );
    });

    it("creates keep working at the OLD composition while a change is pending", async () => {
      const before = await tokenBalance(env, f.vaultAtas[0]);
      await createIx(env, f, SHARE_UNIT, f.apShareAccount).rpc();
      expect(await tokenBalance(env, f.vaultAtas[0])).to.eq(before + 1_500_000n);
    });

    it("cancel clears the proposal; apply then reports nothing pending", async () => {
      await program.methods.cancelBasket().accountsStrict({ authority: f.admin.publicKey, vault: f.vault }).signers([f.admin]).rpc();
      expect((await program.account.vault.fetch(f.vault)).pendingBasket).to.be.null;
      await expectError(
        program.methods.applyBasket().accountsStrict({ authority: f.admin.publicKey, vault: f.vault }).signers([f.admin]).rpc(),
        "NoPendingBasket",
      );
      await expectError(
        program.methods.cancelBasket().accountsStrict({ authority: f.admin.publicKey, vault: f.vault }).signers([f.admin]).rpc(),
        "NoPendingBasket",
      );
    });

    it("validates the proposed basket like initialise does", async () => {
      await expectError(
        program.methods
          .proposeBasket([{ mint: f.constituents[0].mint, unitsPerShare: new BN(0) }])
          .accountsStrict({ authority: f.admin.publicKey, vault: f.vault })
          .signers([f.admin])
          .rpc(),
        "ZeroUnitsPerShare",
      );
    });

    it("rebalance_withdraw is refused unless the vault is paused, and moves units when it is", async () => {
      const c = f.constituents[0];
      const dest = await createAta(env, c.mint, f.admin.publicKey, c.programId);
      const call = () =>
        program.methods
          .rebalanceWithdraw(new BN(1000))
          .accountsStrict({
            authority: f.admin.publicKey,
            vault: f.vault,
            mint: c.mint,
            vaultTokenAccount: f.vaultAtas[0],
            destination: dest,
            tokenProgram: TOKEN_PROGRAM_ID,
            token2022Program: TOKEN_2022_PROGRAM_ID,
          })
          .signers([f.admin])
          .rpc();
      await expectError(call(), "VaultNotPaused");
      await program.methods.setPause(true).accountsStrict({ authority: f.admin.publicKey, vault: f.vault }).signers([f.admin]).rpc();
      const before = await tokenBalance(env, f.vaultAtas[0]);
      await call();
      expect(await tokenBalance(env, f.vaultAtas[0])).to.eq(before - 1000n);
      expect(await tokenBalance(env, dest)).to.eq(1000n);
      await program.methods.setPause(false).accountsStrict({ authority: f.admin.publicKey, vault: f.vault }).signers([f.admin]).rpc();
    });
  });

  // =================================================================================
  describe("pause semantics", () => {
    let f: VaultFixture;
    before(async () => {
      f = await deployVault(env);
      await createIx(env, f, 3n * SHARE_UNIT, f.apShareAccount).rpc();
      await program.methods.setPause(true).accountsStrict({ authority: f.admin.publicKey, vault: f.vault }).signers([f.admin]).rpc();
    });

    it("paused: create is refused", async () => {
      await expectError(createIx(env, f, SHARE_UNIT, f.apShareAccount).rpc(), "VaultPaused");
    });

    it("paused: redeem is refused by default", async () => {
      await expectError(redeemIx(env, f, SHARE_UNIT).rpc(), "VaultPaused");
    });

    it("emergency redeem: set_redeem_while_paused lets holders exit while creation stays blocked", async () => {
      await program.methods
        .setRedeemWhilePaused(true)
        .accountsStrict({ authority: f.admin.publicKey, vault: f.vault })
        .signers([f.admin])
        .rpc();
      const before = await tokenBalance(env, f.apShareAccount);
      await redeemIx(env, f, SHARE_UNIT).rpc();
      expect(await tokenBalance(env, f.apShareAccount)).to.eq(before - SHARE_UNIT);
      await expectError(createIx(env, f, SHARE_UNIT, f.apShareAccount).rpc(), "VaultPaused");
    });

    it("unpause restores creation", async () => {
      await program.methods.setPause(false).accountsStrict({ authority: f.admin.publicKey, vault: f.vault }).signers([f.admin]).rpc();
      await createIx(env, f, SHARE_UNIT, f.apShareAccount).rpc();
      const v = await program.account.vault.fetch(f.vault);
      expect(v.paused).to.be.false;
    });
  });

  // =================================================================================
  describe("fees", () => {
    let f: VaultFixture;
    before(async () => {
      f = await deployVault(env);
      await createIx(env, f, 10n * SHARE_UNIT, f.apShareAccount).rpc();
    });

    const accrue = (f: VaultFixture) =>
      program.methods
        .accrueFees()
        .accountsStrict({
          vault: f.vault,
          shareMint: f.shareMint,
          feeShareAccount: f.feeShareAccount,
          shareTokenProgram: TOKEN_PROGRAM_ID,
        })
        .rpc();

    it("accrue_fees right after initialise mints nothing and is permissionless", async () => {
      const feeBefore = await tokenBalance(env, f.feeShareAccount);
      const supplyBefore = await mintSupply(env, f.shareMint);
      await accrue(f);
      // A few seconds at 50 bps p.a. on 10 shares is far below one base unit.
      expect(await tokenBalance(env, f.feeShareAccount)).to.eq(feeBefore);
      expect(await mintSupply(env, f.shareMint)).to.eq(supplyBefore);
    });

    it("set_fees refuses rates above the caps and non-admins", async () => {
      const call = (create: number, redeem: number, mgmt: number, signer: Keypair) =>
        program.methods
          .setFees(create, redeem, mgmt, f.feeRecipient.publicKey)
          .accountsStrict({
            authority: signer.publicKey,
            vault: f.vault,
            shareMint: f.shareMint,
            feeShareAccount: f.feeShareAccount,
            shareTokenProgram: TOKEN_PROGRAM_ID,
          })
          .signers([signer])
          .rpc();
      await expectError(call(1001, 10, 50, f.admin), "FeeTooHigh");
      await expectError(call(25, 1001, 50, f.admin), "FeeTooHigh");
      await expectError(call(25, 10, 501, f.admin), "FeeTooHigh");
      const stranger = Keypair.generate();
      await airdropTo(env, stranger.publicKey, 100_000_000);
      await expectError(call(25, 10, 50, stranger), "NotAuthority");
    });

    it("set_fees changes the create fee applied on the next create", async () => {
      await program.methods
        .setFees(100, 10, 50, f.feeRecipient.publicKey)
        .accountsStrict({
          authority: f.admin.publicKey,
          vault: f.vault,
          shareMint: f.shareMint,
          feeShareAccount: f.feeShareAccount,
          shareTokenProgram: TOKEN_PROGRAM_ID,
        })
        .signers([f.admin])
        .rpc();
      const feeBefore = await tokenBalance(env, f.feeShareAccount);
      await createIx(env, f, SHARE_UNIT, f.apShareAccount).rpc();
      expect(await tokenBalance(env, f.feeShareAccount)).to.eq(feeBefore + bpsFloor(SHARE_UNIT, 100));
    });

    it("a zero create fee mints every share to the receiver", async () => {
      const g = await deployVault(env, { createFeeBps: 0, redeemFeeBps: 0 });
      await createIx(env, g, SHARE_UNIT, g.apShareAccount).rpc();
      expect(await tokenBalance(env, g.apShareAccount)).to.eq(SHARE_UNIT);
      expect(await tokenBalance(env, g.feeShareAccount)).to.eq(0n);
      await redeemIx(env, g, SHARE_UNIT).rpc();
      expect(await tokenBalance(env, g.apShareAccount)).to.eq(0n);
      expect(await mintSupply(env, g.shareMint)).to.eq(0n);
    });
  });
});
