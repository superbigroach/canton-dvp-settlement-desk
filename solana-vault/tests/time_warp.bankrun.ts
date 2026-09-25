/**
 * Time-dependent behaviour (rebalance timelock, management-fee accrual over a year)
 * cannot be exercised against the test validator, whose clock is wall time. These tests
 * run the SAME compiled program (`target/deploy/etp_basket_vault.so`) in solana-bankrun,
 * an in-process bank whose Clock sysvar can be warped forward.
 */
import { BN, Program } from "@coral-xyz/anchor";
import { BankrunProvider } from "anchor-bankrun";
import { Clock, ProgramTestContext, startAnchor } from "solana-bankrun";
import { Keypair, PublicKey, SystemProgram } from "@solana/web3.js";
import { TOKEN_PROGRAM_ID, TOKEN_2022_PROGRAM_ID } from "@solana/spl-token";
import { expect } from "chai";
import { EtpBasketVault } from "../target/types/etp_basket_vault";
import IDL from "../target/idl/etp_basket_vault.json";
import {
  Env,
  VaultFixture,
  ONE_DAY,
  SHARE_UNIT,
  bnToBigint,
  createAta,
  createIx,
  createMint,
  deployVault,
  expectError,
  mintSupply,
  mintTo,
  tokenBalance,
} from "./helpers";

const SECONDS_PER_YEAR = 31_536_000n;

describe("etp_basket_vault (bankrun, warped clock)", () => {
  let context: ProgramTestContext;
  let env: Env;
  let program: Program<EtpBasketVault>;
  let nonce = 0;

  before(async () => {
    context = await startAnchor("", [], []);
    const provider = new BankrunProvider(context);
    program = new Program<EtpBasketVault>(IDL as EtpBasketVault, provider);
    env = {
      provider: provider as Env["provider"],
      payer: context.payer,
      program,
      getAccountData: async (pk: PublicKey) => {
        const a = await context.banksClient.getAccount(pk);
        return a ? Buffer.from(a.data) : null;
      },
    };
  });

  async function warp(seconds: number) {
    const c = await context.banksClient.getClock();
    context.setClock(
      new Clock(c.slot, c.epochStartTimestamp, c.epoch, c.leaderScheduleEpoch, c.unixTimestamp + BigInt(seconds)),
    );
  }
  async function now(): Promise<number> {
    return Number((await context.banksClient.getClock()).unixTimestamp);
  }
  /** Bankrun keeps one blockhash, so two byte-identical transactions collide; a unique
   *  self-transfer makes each argument-less admin call distinct. */
  function uniq() {
    nonce += 1;
    return SystemProgram.transfer({ fromPubkey: env.payer.publicKey, toPubkey: env.payer.publicKey, lamports: nonce });
  }
  const admin = (f: VaultFixture) => ({ authority: f.admin.publicKey, vault: f.vault });

  // =================================================================================
  describe("rebalance timelock", () => {
    it("a weight-only change applies after the delay without pausing, and creates use the new weights", async () => {
      const f = await deployVault(env);
      const next = f.constituents.map((c) => ({ mint: c.mint, unitsPerShare: new BN((c.unitsPerShare * 2n).toString()) }));
      await program.methods.proposeBasket(next).accountsStrict(admin(f)).signers([f.admin]).rpc();
      const v0 = await program.account.vault.fetch(f.vault);
      const applyAfter = v0.pendingBasket!.applyAfter.toNumber();
      expect(applyAfter - v0.pendingBasket!.proposedAt.toNumber()).to.eq(ONE_DAY);

      // one second short of the timelock: still refused
      await warp(applyAfter - (await now()) - 1);
      await expectError(
        program.methods.applyBasket().accountsStrict(admin(f)).preInstructions([uniq()]).signers([f.admin]).rpc(),
        "TimelockNotElapsed",
      );

      await warp(1);
      await program.methods.applyBasket().accountsStrict(admin(f)).preInstructions([uniq()]).signers([f.admin]).rpc();
      const v1 = await program.account.vault.fetch(f.vault);
      expect(v1.pendingBasket).to.be.null;
      expect(v1.paused).to.be.false;
      expect(v1.constituents.map((c) => c.unitsPerShare.toString())).to.deep.eq(["3000000", "666666666", "4"]);

      const before = await tokenBalance(env, f.vaultAtas[0]);
      await createIx(env, f, SHARE_UNIT, f.apShareAccount).rpc();
      expect(await tokenBalance(env, f.vaultAtas[0])).to.eq(before + 3_000_000n);
    });

    it("a constituent-set change needs the vault paused, then works end to end with the new line", async () => {
      const f = await deployVault(env);
      const auth = Keypair.generate();
      const newMint = await createMint(env, 4, TOKEN_PROGRAM_ID, auth.publicKey);
      const next = [
        ...f.constituents.map((c) => ({ mint: c.mint, unitsPerShare: new BN(c.unitsPerShare.toString()) })),
        { mint: newMint, unitsPerShare: new BN(25_000) },
      ];
      await program.methods.proposeBasket(next).accountsStrict(admin(f)).signers([f.admin]).rpc();
      await warp(ONE_DAY + 5);

      await expectError(
        program.methods.applyBasket().accountsStrict(admin(f)).preInstructions([uniq()]).signers([f.admin]).rpc(),
        "RebalanceRequiresPause",
      );
      await program.methods.setPause(true).accountsStrict(admin(f)).signers([f.admin]).rpc();
      await program.methods.applyBasket().accountsStrict(admin(f)).preInstructions([uniq()]).signers([f.admin]).rpc();
      const v = await program.account.vault.fetch(f.vault);
      expect(v.constituents.length).to.eq(4);
      expect(v.constituents[3].mint.equals(newMint)).to.be.true;

      // Custodian brings inventory in line (deposits need no instruction), then unpauses.
      const vaultNew = await createAta(env, newMint, f.vault, TOKEN_PROGRAM_ID);
      const apNew = await createAta(env, newMint, f.ap.publicKey, TOKEN_PROGRAM_ID);
      await mintTo(env, newMint, apNew, auth, 1_000_000_000n, TOKEN_PROGRAM_ID);
      await program.methods.setPause(false).accountsStrict(admin(f)).signers([f.admin]).rpc();

      const g: VaultFixture = {
        ...f,
        constituents: [...f.constituents, { mint: newMint, programId: TOKEN_PROGRAM_ID, decimals: 4, unitsPerShare: 25_000n, mintAuthority: auth }],
        vaultAtas: [...f.vaultAtas, vaultNew],
        apAtas: [...f.apAtas, apNew],
      };
      await createIx(env, g, 3n * SHARE_UNIT, g.apShareAccount).rpc();
      expect(await tokenBalance(env, vaultNew)).to.eq(75_000n);
      // the old 3-line account list is now the wrong shape and is refused
      await expectError(createIx(env, f, SHARE_UNIT, f.apShareAccount).rpc(), "ConstituentAccountsMismatch");
    });

    it("removing a constituent is also a set change (requires pause)", async () => {
      const f = await deployVault(env);
      const next = f.constituents.slice(0, 2).map((c) => ({ mint: c.mint, unitsPerShare: new BN(c.unitsPerShare.toString()) }));
      await program.methods.proposeBasket(next).accountsStrict(admin(f)).signers([f.admin]).rpc();
      await warp(ONE_DAY + 1);
      await expectError(
        program.methods.applyBasket().accountsStrict(admin(f)).preInstructions([uniq()]).signers([f.admin]).rpc(),
        "RebalanceRequiresPause",
      );
    });

    it("a longer configured delay is honoured", async () => {
      const f = await deployVault(env, { rebalanceDelaySecs: 7 * ONE_DAY });
      const next = f.constituents.map((c) => ({ mint: c.mint, unitsPerShare: new BN((c.unitsPerShare + 1n).toString()) }));
      await program.methods.proposeBasket(next).accountsStrict(admin(f)).signers([f.admin]).rpc();
      await warp(6 * ONE_DAY);
      await expectError(
        program.methods.applyBasket().accountsStrict(admin(f)).preInstructions([uniq()]).signers([f.admin]).rpc(),
        "TimelockNotElapsed",
      );
      await warp(ONE_DAY + 1);
      await program.methods.applyBasket().accountsStrict(admin(f)).preInstructions([uniq()]).signers([f.admin]).rpc();
      expect((await program.account.vault.fetch(f.vault)).pendingBasket).to.be.null;
    });
  });

  // =================================================================================
  describe("management fee accrual", () => {
    const accrue = (f: VaultFixture) =>
      program.methods
        .accrueFees()
        .accountsStrict({ vault: f.vault, shareMint: f.shareMint, feeShareAccount: f.feeShareAccount, shareTokenProgram: TOKEN_PROGRAM_ID })
        .preInstructions([uniq()])
        .rpc();

    it("one year at 50 bps mints exactly 0.5% of supply to the fee recipient by dilution", async () => {
      const f = await deployVault(env, { createFeeBps: 0, managementFeeBps: 50 });
      await createIx(env, f, 10n * SHARE_UNIT, f.apShareAccount).rpc();
      const supply = await mintSupply(env, f.shareMint);
      expect(supply).to.eq(10n * SHARE_UNIT);
      const ts0 = bnToBigint((await program.account.vault.fetch(f.vault)).lastAccrualTs);

      await warp(Number(SECONDS_PER_YEAR));
      await accrue(f);

      const expected = (supply * 50n * SECONDS_PER_YEAR) / (10_000n * SECONDS_PER_YEAR); // 0.05 shares
      expect(expected).to.eq(50_000_000n);
      expect(await tokenBalance(env, f.feeShareAccount)).to.eq(expected);
      expect(await mintSupply(env, f.shareMint)).to.eq(supply + expected);
      const v = await program.account.vault.fetch(f.vault);
      expect(bnToBigint(v.lastAccrualTs)).to.eq(ts0 + SECONDS_PER_YEAR);
    });

    it("a partial period is pro-rated with floor rounding and the AP's holding is unchanged", async () => {
      const f = await deployVault(env, { createFeeBps: 0, managementFeeBps: 50 });
      await createIx(env, f, 1_234_567_891n, f.apShareAccount).rpc();
      const supply = await mintSupply(env, f.shareMint);
      const elapsed = 30n * 86_400n;
      await warp(Number(elapsed));
      await accrue(f);
      const expected = (supply * 50n * elapsed) / (10_000n * SECONDS_PER_YEAR);
      expect(expected).to.eq(507_356n); // 1234567891 * 50 * 2592000 / 315360000000 = 507356.7...
      expect(await tokenBalance(env, f.feeShareAccount)).to.eq(expected);
      expect(await tokenBalance(env, f.apShareAccount)).to.eq(1_234_567_891n);
    });

    it("zero management fee never mints and only advances the timestamp", async () => {
      const f = await deployVault(env, { managementFeeBps: 0 });
      await createIx(env, f, 10n * SHARE_UNIT, f.apShareAccount).rpc();
      await warp(Number(SECONDS_PER_YEAR));
      const feeBefore = await tokenBalance(env, f.feeShareAccount);
      await accrue(f);
      expect(await tokenBalance(env, f.feeShareAccount)).to.eq(feeBefore);
      const v = await program.account.vault.fetch(f.vault);
      expect(Number(bnToBigint(v.lastAccrualTs))).to.eq(await now());
    });

    it("set_fees settles the accrued fee at the OLD rate before the new one applies", async () => {
      const f = await deployVault(env, { createFeeBps: 0, managementFeeBps: 50 });
      await createIx(env, f, 10n * SHARE_UNIT, f.apShareAccount).rpc();
      await warp(Number(SECONDS_PER_YEAR));
      await program.methods
        .setFees(0, 0, 200, f.feeRecipient.publicKey)
        .accountsStrict({
          authority: f.admin.publicKey,
          vault: f.vault,
          shareMint: f.shareMint,
          feeShareAccount: f.feeShareAccount,
          shareTokenProgram: TOKEN_PROGRAM_ID,
        })
        .signers([f.admin])
        .rpc();
      // 0.5% of 10 shares, not 2%
      expect(await tokenBalance(env, f.feeShareAccount)).to.eq(50_000_000n);
      const v = await program.account.vault.fetch(f.vault);
      expect(v.managementFeeBpsPerYear).to.eq(200);
      expect(Number(bnToBigint(v.lastAccrualTs))).to.eq(await now());
    });

    it("dust below one base unit is carried, not discarded: the timestamp does not advance", async () => {
      // 1 share = 1e9 base units. At 50 bps p.a. the fee is 1e9*50*t/(1e4*31536000)
      // base units, i.e. ~0.1585 per second, so one WHOLE base unit needs t >= 6.31 s.
      // (An earlier version of this test read the formula as if 1 share were 1 base unit
      // and warped 6000 s, which is already 951 units -- nowhere near dust.)
      const f = await deployVault(env, { createFeeBps: 0, managementFeeBps: 50 });
      await createIx(env, f, SHARE_UNIT, f.apShareAccount).rpc();
      const ts0 = bnToBigint((await program.account.vault.fetch(f.vault)).lastAccrualTs);
      await warp(6);
      await accrue(f);
      // 6 s -> 0.951 base units -> floors to zero, so nothing is minted AND the
      // timestamp must stay put, otherwise the dust would be silently discarded.
      expect(await tokenBalance(env, f.feeShareAccount)).to.eq(0n);
      expect(bnToBigint((await program.account.vault.fetch(f.vault)).lastAccrualTs)).to.eq(ts0);
      await warp(1);
      await accrue(f);
      // 7 s from ts0 -> 1.109 base units -> 1 is minted and the clock finally advances.
      expect(await tokenBalance(env, f.feeShareAccount)).to.eq(1n);
      expect(bnToBigint((await program.account.vault.fetch(f.vault)).lastAccrualTs)).to.eq(ts0 + 7n);
    });
  });

  // =================================================================================
  describe("NAV date window under a warped clock", () => {
    it("a fixing dated tomorrow is allowed (close-of-day tolerance) but two days ahead is not", async () => {
      const { makeFixing, postNavIx, signedBy } = await import("./helpers");
      const f = await deployVault(env, { attestorCount: 3, threshold: 2 });
      const today = Math.floor((await now()) / ONE_DAY);
      const tomorrow = makeFixing(f, today + 1);
      await postNavIx(env, f, tomorrow, signedBy(env, f, tomorrow, [f.attestors[0], f.attestors[1]])).rpc();
      const dayAfter = makeFixing(f, today + 2);
      await expectError(
        postNavIx(env, f, dayAfter, signedBy(env, f, dayAfter, [f.attestors[0], f.attestors[1]])).rpc(),
        "FixingInFuture",
      );
      // once the clock moves on, the same day-after fixing becomes postable
      await warp(ONE_DAY);
      await postNavIx(env, f, dayAfter, signedBy(env, f, dayAfter, [f.attestors[0], f.attestors[1]])).rpc();
      expect((await program.account.vault.fetch(f.vault)).latestNav.asOfDate).to.eq(today + 2);
    });
  });
});
