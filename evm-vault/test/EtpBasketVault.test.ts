import { expect } from "chai";
import { ethers } from "hardhat";
import { loadFixture, time } from "@nomicfoundation/hardhat-toolbox/network-helpers";
import type { HardhatEthersSigner } from "@nomicfoundation/hardhat-ethers/signers";
import type { EtpBasketVault, MockERC20, MockHolderRegistry, MockRestrictedToken } from "../typechain-types";

const ONE = 10n ** 18n;
const DAY = 24n * 60n * 60n;
const INSTRUMENT = ethers.keccak256(ethers.toUtf8Bytes("LX1"));
const RULEBOOK = ethers.keccak256(ethers.toUtf8Bytes("lx1-rulebook-v1"));

// 0.5 of a 6-decimal token and 0.25 of an 18-decimal token per share.
const UNITS_A = 500_000n;
const UNITS_B = ONE / 4n;

function mulDivUp(a: bigint, b: bigint, d: bigint): bigint {
  const p = a * b;
  return p === 0n ? 0n : (p - 1n) / d + 1n;
}
function mulDivDown(a: bigint, b: bigint, d: bigint): bigint {
  return (a * b) / d;
}

type Fixing = {
  instrumentId: string;
  asOfDate: bigint;
  session: number;
  navPerShare: bigint;
  rulebookVersion: string;
  signedAt: bigint;
};

const NAV_TYPES = {
  NavFixing: [
    { name: "instrumentId", type: "bytes32" },
    { name: "asOfDate", type: "uint64" },
    { name: "session", type: "uint8" },
    { name: "navPerShare", type: "uint256" },
    { name: "rulebookVersion", type: "bytes32" },
    { name: "signedAt", type: "uint64" },
  ],
};

async function domainFor(vault: EtpBasketVault, overrides: Partial<{ chainId: bigint; verifyingContract: string }> = {}) {
  const { chainId } = await ethers.provider.getNetwork();
  return {
    name: await vault.name(),
    version: "1",
    chainId: overrides.chainId ?? chainId,
    verifyingContract: overrides.verifyingContract ?? (await vault.getAddress()),
  };
}

async function signFixing(
  vault: EtpBasketVault,
  signers: HardhatEthersSigner[],
  f: Fixing,
  overrides: Partial<{ chainId: bigint; verifyingContract: string }> = {}
): Promise<string[]> {
  const domain = await domainFor(vault, overrides);
  return Promise.all(signers.map((s) => s.signTypedData(domain, NAV_TYPES, f)));
}

async function todayDays(): Promise<bigint> {
  return BigInt(await time.latest()) / DAY;
}

async function makeFixing(nav: bigint, dateOffsetDays = 0n, session = 0): Promise<Fixing> {
  const now = BigInt(await time.latest());
  return {
    instrumentId: INSTRUMENT,
    asOfDate: (await todayDays()) + dateOffsetDays,
    session,
    navPerShare: nav,
    rulebookVersion: RULEBOOK,
    signedAt: now,
  };
}

describe("EtpBasketVault", () => {
  async function deployFixture() {
    const [deployer, admin, ap, feeRecipient, holder, outsider, att1, att2, att3, att4] =
      await ethers.getSigners();

    const Mock = await ethers.getContractFactory("MockERC20");
    const tokA = (await Mock.deploy("Tokenised AAPL", "AAPLx", 6)) as unknown as MockERC20;
    const tokB = (await Mock.deploy("Tokenised MSFT", "MSFTx", 18)) as unknown as MockERC20;

    const Vault = await ethers.getContractFactory("EtpBasketVault");
    const vault = (await Vault.deploy({
      name: "ETP Foundry LX1 Basket",
      symbol: "LX1",
      instrumentId: INSTRUMENT,
      admin: admin.address,
      feeRecipient: feeRecipient.address,
      constituents: [await tokA.getAddress(), await tokB.getAddress()],
      unitsPerShare: [UNITS_A, UNITS_B],
    })) as unknown as EtpBasketVault;

    const AP_ROLE = await vault.AP_ROLE();
    await vault.connect(admin).grantRole(AP_ROLE, ap.address);
    await vault.connect(admin).setAttestors([att1.address, att2.address, att3.address], 2);

    // Fund the AP generously and pre-approve the vault.
    await tokA.mint(ap.address, 1_000_000n * 10n ** 6n);
    await tokB.mint(ap.address, 1_000_000n * ONE);
    await tokA.connect(ap).approve(await vault.getAddress(), ethers.MaxUint256);
    await tokB.connect(ap).approve(await vault.getAddress(), ethers.MaxUint256);

    const Registry = await ethers.getContractFactory("MockHolderRegistry");
    const registry = (await Registry.deploy()) as unknown as MockHolderRegistry;

    return {
      deployer, admin, ap, feeRecipient, holder, outsider, att1, att2, att3, att4,
      tokA, tokB, vault, registry, AP_ROLE,
    };
  }

  // ------------------------------------------------------------------
  describe("deployment", () => {
    it("stores the basket, sets epoch 1 and grants admin roles", async () => {
      const { vault, admin, tokA, tokB } = await loadFixture(deployFixture);
      const [c, u] = await vault.basket();
      expect(c).to.deep.equal([await tokA.getAddress(), await tokB.getAddress()]);
      expect(u).to.deep.equal([UNITS_A, UNITS_B]);
      expect(await vault.epoch()).to.equal(1n);
      expect(await vault.hasRole(await vault.ADMIN_ROLE(), admin.address)).to.equal(true);
      expect(await vault.hasRole(await vault.DEFAULT_ADMIN_ROLE(), admin.address)).to.equal(true);
      expect(await vault.rebalanceDelay()).to.equal(2n * DAY);
      expect(await vault.decimals()).to.equal(18);
    });

    it("rejects a length-mismatched basket", async () => {
      const { admin, feeRecipient, tokA, tokB } = await loadFixture(deployFixture);
      const Vault = await ethers.getContractFactory("EtpBasketVault");
      await expect(
        Vault.deploy({
          name: "x", symbol: "x", instrumentId: INSTRUMENT, admin: admin.address, feeRecipient: feeRecipient.address,
          constituents: [await tokA.getAddress(), await tokB.getAddress()], unitsPerShare: [UNITS_A],
        })
      ).to.be.revertedWithCustomError(Vault, "LengthMismatch");
    });

    it("rejects duplicate constituents, zero units, and non-contract constituents", async () => {
      const { admin, feeRecipient, tokA, holder } = await loadFixture(deployFixture);
      const Vault = await ethers.getContractFactory("EtpBasketVault");
      const a = await tokA.getAddress();
      const base = { name: "x", symbol: "x", instrumentId: INSTRUMENT, admin: admin.address, feeRecipient: feeRecipient.address };
      await expect(Vault.deploy({ ...base, constituents: [a, a], unitsPerShare: [1n, 1n] }))
        .to.be.revertedWithCustomError(Vault, "DuplicateConstituent").withArgs(a);
      await expect(Vault.deploy({ ...base, constituents: [a], unitsPerShare: [0n] }))
        .to.be.revertedWithCustomError(Vault, "ZeroUnits").withArgs(a);
      await expect(Vault.deploy({ ...base, constituents: [holder.address], unitsPerShare: [1n] }))
        .to.be.revertedWithCustomError(Vault, "NotAContract").withArgs(holder.address);
      await expect(Vault.deploy({ ...base, constituents: [], unitsPerShare: [] }))
        .to.be.revertedWithCustomError(Vault, "EmptyBasket");
    });
  });

  // ------------------------------------------------------------------
  describe("create / redeem (in kind)", () => {
    it("create pulls exact units (rounded up) and mints shares to the receiver", async () => {
      const { vault, ap, holder, tokA, tokB } = await loadFixture(deployFixture);
      const shares = 1_000n * ONE;
      const expectA = mulDivUp(UNITS_A, shares, ONE); // 500 AAPLx
      const expectB = mulDivUp(UNITS_B, shares, ONE); // 250 MSFTx
      await expect(vault.connect(ap).create(shares, holder.address))
        .to.emit(vault, "Created").withArgs(ap.address, holder.address, shares, shares, 0n, [expectA, expectB]);
      expect(await tokA.balanceOf(await vault.getAddress())).to.equal(expectA);
      expect(await tokB.balanceOf(await vault.getAddress())).to.equal(expectB);
      expect(await vault.balanceOf(holder.address)).to.equal(shares);
      expect(await vault.totalSupply()).to.equal(shares);
    });

    it("rounds inbound amounts UP on create when units do not divide evenly", async () => {
      const { vault, ap, holder, tokA } = await loadFixture(deployFixture);
      // 1 wei of share: 500000 * 1 / 1e18 = 5e-13 -> ceil = 1 base unit of tokA.
      await vault.connect(ap).create(1n, holder.address);
      expect(await tokA.balanceOf(await vault.getAddress())).to.equal(1n);
      const [, amts] = await vault.previewCreate(1n);
      expect(amts[0]).to.equal(1n);
      expect(amts[1]).to.equal(1n); // ceil(0.25e18 * 1 / 1e18) = 1
    });

    it("rounds outbound amounts DOWN on redeem", async () => {
      const { vault, ap, holder, tokA, tokB } = await loadFixture(deployFixture);
      await vault.connect(ap).create(1_000n * ONE, ap.address);
      const odd = 3n; // 3 wei of share -> 500000*3/1e18 floors to 0 of tokA
      const [, amts] = await vault.previewRedeem(odd);
      expect(amts[0]).to.equal(0n);
      expect(amts[1]).to.equal(0n);
      const balA = await tokA.balanceOf(holder.address);
      const balB = await tokB.balanceOf(holder.address);
      await vault.connect(ap).redeem(odd, holder.address);
      expect(await tokA.balanceOf(holder.address)).to.equal(balA);
      expect(await tokB.balanceOf(holder.address)).to.equal(balB);
      expect(await vault.balanceOf(ap.address)).to.equal(1_000n * ONE - odd);
    });

    it("round-trips exactly for evenly divisible sizes", async () => {
      const { vault, ap, holder, tokA, tokB } = await loadFixture(deployFixture);
      const shares = 10n * ONE;
      const a0 = await tokA.balanceOf(ap.address);
      const b0 = await tokB.balanceOf(ap.address);
      await vault.connect(ap).create(shares, ap.address);
      await vault.connect(ap).redeem(shares, ap.address);
      expect(await tokA.balanceOf(ap.address)).to.equal(a0);
      expect(await tokB.balanceOf(ap.address)).to.equal(b0);
      expect(await vault.totalSupply()).to.equal(0n);
      void holder;
    });

    it("rounding dust stays in the vault and never blocks the last holder's full redemption", async () => {
      const { vault, ap, tokA, tokB } = await loadFixture(deployFixture);
      const oddShares = 123_456_789_012_345_678n; // ~0.123 shares, not divisible
      await vault.connect(ap).create(oddShares, ap.address);
      const inA = mulDivUp(UNITS_A, oddShares, ONE);
      const inB = mulDivUp(UNITS_B, oddShares, ONE);
      const outA = mulDivDown(UNITS_A, oddShares, ONE);
      const outB = mulDivDown(UNITS_B, oddShares, ONE);
      expect(inA).to.be.gte(outA);
      await vault.connect(ap).redeem(oddShares, ap.address);
      expect(await vault.totalSupply()).to.equal(0n);
      expect(await tokA.balanceOf(await vault.getAddress())).to.equal(inA - outA);
      expect(await tokB.balanceOf(await vault.getAddress())).to.equal(inB - outB);
    });

    it("refuses zero shares and zero receiver", async () => {
      const { vault, ap, holder } = await loadFixture(deployFixture);
      await expect(vault.connect(ap).create(0n, holder.address)).to.be.revertedWithCustomError(vault, "ZeroShares");
      await expect(vault.connect(ap).create(1n, ethers.ZeroAddress)).to.be.revertedWithCustomError(vault, "ZeroAddress");
      await expect(vault.connect(ap).redeem(0n, holder.address)).to.be.revertedWithCustomError(vault, "ZeroShares");
    });

    it("refuses a non-AP on create and redeem", async () => {
      const { vault, outsider, holder, AP_ROLE } = await loadFixture(deployFixture);
      await expect(vault.connect(outsider).create(ONE, holder.address))
        .to.be.revertedWithCustomError(vault, "AccessControlUnauthorizedAccount").withArgs(outsider.address, AP_ROLE);
      await expect(vault.connect(outsider).redeem(ONE, holder.address))
        .to.be.revertedWithCustomError(vault, "AccessControlUnauthorizedAccount").withArgs(outsider.address, AP_ROLE);
    });

    it("a failing constituent transfer reverts the WHOLE create (atomicity)", async () => {
      const { vault, ap, holder, tokA, tokB } = await loadFixture(deployFixture);
      // First leg (tokA) is fine; second leg (tokB) has no allowance.
      await tokB.connect(ap).approve(await vault.getAddress(), 0n);
      const a0 = await tokA.balanceOf(ap.address);
      await expect(vault.connect(ap).create(ONE, holder.address))
        .to.be.revertedWithCustomError(tokB, "ERC20InsufficientAllowance");
      expect(await tokA.balanceOf(ap.address)).to.equal(a0); // leg 1 rolled back
      expect(await tokA.balanceOf(await vault.getAddress())).to.equal(0n);
      expect(await vault.totalSupply()).to.equal(0n);
    });

    it("reverts when the AP lacks balance for any leg", async () => {
      const { vault, ap, holder, tokB } = await loadFixture(deployFixture);
      const huge = 10_000_000n * ONE; // needs 2.5M MSFTx, AP has 1M
      await expect(vault.connect(ap).create(huge, holder.address))
        .to.be.revertedWithCustomError(tokB, "ERC20InsufficientBalance");
    });

    it("redeem burns the AP's shares and delivers constituents to the receiver", async () => {
      const { vault, ap, holder, tokA, tokB } = await loadFixture(deployFixture);
      await vault.connect(ap).create(100n * ONE, ap.address);
      await expect(vault.connect(ap).redeem(40n * ONE, holder.address))
        .to.emit(vault, "Redeemed").withArgs(ap.address, holder.address, 40n * ONE, 40n * ONE, 0n, [20n * 10n ** 6n, 10n * ONE]);
      expect(await vault.balanceOf(ap.address)).to.equal(60n * ONE);
      expect(await tokA.balanceOf(holder.address)).to.equal(20n * 10n ** 6n);
      expect(await tokB.balanceOf(holder.address)).to.equal(10n * ONE);
    });

    it("redeem reverts if the AP holds fewer shares than requested", async () => {
      const { vault, ap } = await loadFixture(deployFixture);
      await vault.connect(ap).create(ONE, ap.address);
      await expect(vault.connect(ap).redeem(2n * ONE, ap.address))
        .to.be.revertedWithCustomError(vault, "ERC20InsufficientBalance");
    });
  });

  // ------------------------------------------------------------------
  describe("fees", () => {
    it("creation fee is minted to the fee recipient out of the gross shares", async () => {
      const { vault, admin, ap, holder, feeRecipient } = await loadFixture(deployFixture);
      await vault.connect(admin).setFees(50, 0, 0); // 0.5 %
      const shares = 1_000n * ONE;
      await vault.connect(ap).create(shares, holder.address);
      expect(await vault.balanceOf(feeRecipient.address)).to.equal(5n * ONE);
      expect(await vault.balanceOf(holder.address)).to.equal(995n * ONE);
      expect(await vault.totalSupply()).to.equal(shares); // fully backed
    });

    it("redeem fee is transferred (not burned) to the fee recipient and reduces the payout", async () => {
      const { vault, admin, ap, holder, feeRecipient, tokA } = await loadFixture(deployFixture);
      await vault.connect(admin).setFees(0, 100, 0); // 1 %
      await vault.connect(ap).create(1_000n * ONE, ap.address);
      await vault.connect(ap).redeem(100n * ONE, holder.address);
      expect(await vault.balanceOf(feeRecipient.address)).to.equal(ONE);
      expect(await vault.totalSupply()).to.equal(901n * ONE);
      expect(await tokA.balanceOf(holder.address)).to.equal(mulDivDown(UNITS_A, 99n * ONE, ONE));
    });

    it("fee caps are enforced and only ADMIN may set fees", async () => {
      const { vault, admin, outsider } = await loadFixture(deployFixture);
      await expect(vault.connect(admin).setFees(501, 0, 0)).to.be.revertedWithCustomError(vault, "FeeTooHigh");
      await expect(vault.connect(admin).setFees(0, 501, 0)).to.be.revertedWithCustomError(vault, "FeeTooHigh");
      await expect(vault.connect(admin).setFees(0, 0, 1001)).to.be.revertedWithCustomError(vault, "FeeTooHigh");
      await expect(vault.connect(outsider).setFees(0, 0, 0)).to.be.revertedWithCustomError(vault, "AccessControlUnauthorizedAccount");
      await expect(vault.connect(admin).setFees(500, 500, 1000)).to.emit(vault, "FeesSet").withArgs(500, 500, 1000);
    });

    it("management fee accrues by exact dilution over time (1 % p.a. -> recipient owns 1 % after a year)", async () => {
      const { vault, admin, ap, feeRecipient } = await loadFixture(deployFixture);
      await vault.connect(admin).setFees(0, 0, 100);
      await vault.connect(ap).create(1_000n * ONE, ap.address);
      const t0 = await vault.lastFeeAccrual();
      await time.increaseTo(t0 + 365n * DAY);
      expect(await vault.pendingManagementFee()).to.be.gt(0n);
      await expect(vault.accrueFees()).to.emit(vault, "ManagementFeeAccrued");
      const supply = await vault.totalSupply();
      const feeBal = await vault.balanceOf(feeRecipient.address);
      // recipient share of supply should be 1 % (exact form: minted = S * 0.01 / 0.99)
      const expected = (1_000n * ONE * ONE) / (99n * ONE);
      expect(feeBal).to.be.closeTo(expected, 10n ** 6n);
      expect((feeBal * 10_000n + supply - 1n) / supply).to.equal(100n);
      expect(await vault.pendingManagementFee()).to.equal(0n);
    });

    it("management fee shrinks the effective per-share basket, never the backing (invariant)", async () => {
      const { vault, admin, ap, feeRecipient, tokA, tokB, AP_ROLE } = await loadFixture(deployFixture);
      await vault.connect(admin).setFees(0, 0, 100);
      await vault.connect(ap).create(1_000n * ONE, ap.address);
      const vaultAddr = await vault.getAddress();
      const backingA = await tokA.balanceOf(vaultAddr);
      const backingB = await tokB.balanceOf(vaultAddr);
      const t0 = await vault.lastFeeAccrual();
      await time.increaseTo(t0 + 365n * DAY);
      await vault.accrueFees();
      // Effective units are ~1 % smaller; nominal units unchanged; multiplier < 1e18.
      const [, eff] = await vault.basket();
      const [, nom] = await vault.nominalBasket();
      expect(nom).to.deep.equal([UNITS_A, UNITS_B]);
      expect(eff[0]).to.be.lt(UNITS_A);
      expect(eff[0]).to.be.closeTo((UNITS_A * 99n) / 100n, 5n);
      expect(await vault.unitsMultiplier()).to.be.lt(ONE);
      // Required backing never exceeds what the vault holds.
      expect(await vault.requiredBacking(await tokA.getAddress())).to.be.lte(backingA);
      expect(await vault.requiredBacking(await tokB.getAddress())).to.be.lte(backingB);
      // Everyone — the AP and the fee recipient — can redeem in full. Stop the
      // clock on fees first so the recipient's own redeem does not accrue a
      // few more wei of shares inside the same transaction.
      await vault.connect(admin).setFees(0, 0, 0);
      await vault.connect(admin).grantRole(AP_ROLE, feeRecipient.address);
      await vault.connect(ap).redeem(await vault.balanceOf(ap.address), ap.address);
      await vault.connect(feeRecipient).redeem(await vault.balanceOf(feeRecipient.address), feeRecipient.address);
      expect(await vault.totalSupply()).to.equal(0n);
      // The fee recipient walked away with ~1 % of the original basket.
      expect(await tokA.balanceOf(feeRecipient.address)).to.be.closeTo(backingA / 100n, 10n);
      // (a few seconds of extra accrual elapsed before the rate was zeroed)
      expect(await tokB.balanceOf(feeRecipient.address)).to.be.closeTo(backingB / 100n, 10n ** 12n);
    });

    it("applying a basket re-bases the multiplier to 1e18", async () => {
      const { vault, admin, ap, tokA, tokB } = await loadFixture(deployFixture);
      await vault.connect(admin).setFees(0, 0, 500);
      await vault.connect(ap).create(100n * ONE, ap.address);
      await time.increase(100n * DAY);
      await vault.accrueFees();
      expect(await vault.unitsMultiplier()).to.be.lt(ONE);
      // Freeze the fee so the effective units we snapshot are still exact at apply time
      // (a live fee keeps shrinking them during the timelock, which would need a delta).
      await vault.connect(admin).setFees(0, 0, 0);
      const [c, eff] = await vault.basket();
      // Proposing the current effective units keeps exposure unchanged and needs no delta.
      await vault.connect(admin).proposeBasket([...c], [...eff]);
      await time.increase(2n * DAY);
      await vault.connect(admin).applyBasket();
      expect(await vault.unitsMultiplier()).to.equal(ONE);
      const [, nom] = await vault.nominalBasket();
      expect(nom[0]).to.equal(eff[0]);
      void tokA; void tokB;
    });

    it("management fee is accrued automatically on create and at the OLD rate on a fee change", async () => {
      const { vault, admin, ap, feeRecipient } = await loadFixture(deployFixture);
      await vault.connect(admin).setFees(0, 0, 200);
      await vault.connect(ap).create(1_000n * ONE, ap.address);
      const t0 = await vault.lastFeeAccrual();
      await time.increaseTo(t0 + 182n * DAY);
      // Changing the rate first accrues at 2 % for the elapsed half-year.
      await vault.connect(admin).setFees(0, 0, 0);
      const bal = await vault.balanceOf(feeRecipient.address);
      expect(bal).to.be.gt(0n);
      await time.increase(365n * DAY);
      await vault.connect(ap).create(ONE, ap.address);
      expect(await vault.balanceOf(feeRecipient.address)).to.equal(bal); // rate is 0 now
    });

    it("no fee accrues while supply is zero or the rate is zero", async () => {
      const { vault, admin, ap, feeRecipient } = await loadFixture(deployFixture);
      await vault.connect(admin).setFees(0, 0, 100);
      await time.increase(400n * DAY);
      expect(await vault.pendingManagementFee()).to.equal(0n);
      await vault.connect(ap).create(ONE, ap.address);
      expect(await vault.balanceOf(feeRecipient.address)).to.equal(0n);
    });

    it("setFeeRecipient accrues to the old recipient before switching", async () => {
      const { vault, admin, ap, feeRecipient, holder } = await loadFixture(deployFixture);
      await vault.connect(admin).setFees(0, 0, 100);
      await vault.connect(ap).create(1_000n * ONE, ap.address);
      await time.increase(30n * DAY);
      await expect(vault.connect(admin).setFeeRecipient(holder.address))
        .to.emit(vault, "FeeRecipientSet").withArgs(feeRecipient.address, holder.address);
      expect(await vault.balanceOf(feeRecipient.address)).to.be.gt(0n);
      expect(await vault.balanceOf(holder.address)).to.equal(0n);
      await expect(vault.connect(admin).setFeeRecipient(ethers.ZeroAddress)).to.be.revertedWithCustomError(vault, "ZeroAddress");
    });
  });

  // ------------------------------------------------------------------
  describe("holder registry (compliance hook)", () => {
    it("blocks a create to a non-allowed receiver and allows an allowed one", async () => {
      const { vault, admin, ap, holder, registry, feeRecipient } = await loadFixture(deployFixture);
      await vault.connect(admin).setRegistry(await registry.getAddress());
      await expect(vault.connect(ap).create(ONE, holder.address))
        .to.be.revertedWithCustomError(vault, "ReceiverNotAllowed").withArgs(holder.address);
      await registry.setAllowed(holder.address, true);
      await vault.connect(ap).create(ONE, holder.address);
      expect(await vault.balanceOf(holder.address)).to.equal(ONE);
      void feeRecipient;
    });

    it("blocks secondary transfers to non-allowed receivers", async () => {
      const { vault, admin, ap, holder, outsider, registry } = await loadFixture(deployFixture);
      await vault.connect(admin).setRegistry(await registry.getAddress());
      await registry.setAllowed(holder.address, true);
      await vault.connect(ap).create(ONE, holder.address);
      await expect(vault.connect(holder).transfer(outsider.address, 1n))
        .to.be.revertedWithCustomError(vault, "ReceiverNotAllowed").withArgs(outsider.address);
      await registry.setAllowed(outsider.address, true);
      await vault.connect(holder).transfer(outsider.address, 1n);
      expect(await vault.balanceOf(outsider.address)).to.equal(1n);
    });

    it("never gates burns: an AP that is no longer allowed can still redeem", async () => {
      const { vault, admin, ap, registry } = await loadFixture(deployFixture);
      await registry.setAllowed(ap.address, true);
      await vault.connect(admin).setRegistry(await registry.getAddress());
      await vault.connect(ap).create(ONE, ap.address);
      await registry.setAllowed(ap.address, false);
      await vault.connect(ap).redeem(ONE, ap.address);
      expect(await vault.totalSupply()).to.equal(0n);
    });

    it("a non-allowed fee recipient makes fee-bearing creates revert (documented foot-gun)", async () => {
      const { vault, admin, ap, holder, registry, feeRecipient } = await loadFixture(deployFixture);
      await vault.connect(admin).setFees(10, 0, 0);
      await vault.connect(admin).setRegistry(await registry.getAddress());
      await registry.setAllowed(holder.address, true);
      await expect(vault.connect(ap).create(1_000n * ONE, holder.address))
        .to.be.revertedWithCustomError(vault, "ReceiverNotAllowed").withArgs(feeRecipient.address);
      await registry.setAllowed(feeRecipient.address, true);
      await vault.connect(ap).create(1_000n * ONE, holder.address);
    });

    it("clearing the registry removes gating; only ADMIN may set it", async () => {
      const { vault, admin, ap, holder, registry, outsider } = await loadFixture(deployFixture);
      await vault.connect(admin).setRegistry(await registry.getAddress());
      await expect(vault.connect(admin).setRegistry(ethers.ZeroAddress))
        .to.emit(vault, "RegistrySet").withArgs(await registry.getAddress(), ethers.ZeroAddress);
      await vault.connect(ap).create(ONE, holder.address);
      await expect(vault.connect(outsider).setRegistry(await registry.getAddress()))
        .to.be.revertedWithCustomError(vault, "AccessControlUnauthorizedAccount");
    });
  });

  // ------------------------------------------------------------------
  describe("issuer-restricted underlying (xStocks-style allowlist)", () => {
    async function restrictedFixture() {
      const base = await deployFixture();
      const { admin, ap, feeRecipient } = base;
      const R = await ethers.getContractFactory("MockRestrictedToken");
      const rtok = (await R.deploy("Tokenised NVDA", "NVDAx", 18)) as unknown as MockRestrictedToken;
      await rtok.setAllowed(ap.address, true);
      await rtok.mint(ap.address, 1_000n * ONE);
      const Vault = await ethers.getContractFactory("EtpBasketVault");
      const rvault = (await Vault.deploy({
        name: "Single NVDA Basket", symbol: "NV1", instrumentId: INSTRUMENT,
        admin: admin.address, feeRecipient: feeRecipient.address,
        constituents: [await rtok.getAddress()], unitsPerShare: [ONE],
      })) as unknown as EtpBasketVault;
      await rvault.connect(admin).grantRole(await rvault.AP_ROLE(), ap.address);
      await rtok.connect(ap).approve(await rvault.getAddress(), ethers.MaxUint256);
      return { ...base, rtok, rvault };
    }

    it("create reverts inside the underlying when the VAULT is not whitelisted by the issuer", async () => {
      const { rvault, rtok, ap } = await loadFixture(restrictedFixture);
      await expect(rvault.connect(ap).create(ONE, ap.address))
        .to.be.revertedWithCustomError(rtok, "TransferToNonAllowed").withArgs(await rvault.getAddress());
      expect(await rvault.totalSupply()).to.equal(0n);
    });

    it("works once the issuer whitelists the vault; redeem to a non-whitelisted receiver still reverts", async () => {
      const { rvault, rtok, ap, holder } = await loadFixture(restrictedFixture);
      await rtok.setAllowed(await rvault.getAddress(), true);
      await rvault.connect(ap).create(ONE, ap.address);
      expect(await rtok.balanceOf(await rvault.getAddress())).to.equal(ONE);
      await expect(rvault.connect(ap).redeem(ONE, holder.address))
        .to.be.revertedWithCustomError(rtok, "TransferToNonAllowed").withArgs(holder.address);
      // Shares were NOT burned because the whole redeem reverted.
      expect(await rvault.balanceOf(ap.address)).to.equal(ONE);
      await rtok.setAllowed(holder.address, true);
      await rvault.connect(ap).redeem(ONE, holder.address);
      expect(await rtok.balanceOf(holder.address)).to.equal(ONE);
    });
  });

  // ------------------------------------------------------------------
  describe("NAV attestation (K-of-N, EIP-712)", () => {
    it("accepts a fixing signed by threshold distinct attestors and exposes it", async () => {
      const { vault, att1, att2 } = await loadFixture(deployFixture);
      const f = await makeFixing(890n * ONE);
      const sigs = await signFixing(vault, [att1, att2], f);
      await expect(vault.postNav(f, sigs))
        .to.emit(vault, "NavPosted").withArgs(INSTRUMENT, f.asOfDate, 890n * ONE, 2n);
      const [nav, asOf, age] = await vault.navPerShare();
      expect(nav).to.equal(890n * ONE);
      expect(asOf).to.equal(f.asOfDate);
      expect(age).to.be.lte(2n);
      expect(await vault.isNavFresh(60n)).to.equal(true);
      const rec = await vault.latestNav();
      expect(rec.attestorCount).to.equal(2n);
      expect(rec.rulebookVersion).to.equal(RULEBOOK);
    });

    it("accepts more than threshold signatures (3 of 3) and records the count", async () => {
      const { vault, att1, att2, att3 } = await loadFixture(deployFixture);
      const f = await makeFixing(1n * ONE);
      const sigs = await signFixing(vault, [att3, att1, att2], f);
      await expect(vault.postNav(f, sigs)).to.emit(vault, "NavPosted").withArgs(INSTRUMENT, f.asOfDate, ONE, 3n);
    });

    it("refuses a duplicate signer even if the count meets the threshold", async () => {
      const { vault, att1 } = await loadFixture(deployFixture);
      const f = await makeFixing(ONE);
      const [s] = await signFixing(vault, [att1], f);
      await expect(vault.postNav(f, [s, s]))
        .to.be.revertedWithCustomError(vault, "DuplicateAttestor").withArgs(att1.address);
    });

    it("refuses below-threshold signatures", async () => {
      const { vault, att1 } = await loadFixture(deployFixture);
      const f = await makeFixing(ONE);
      const sigs = await signFixing(vault, [att1], f);
      await expect(vault.postNav(f, sigs))
        .to.be.revertedWithCustomError(vault, "InsufficientSignatures").withArgs(1n, 2n);
    });

    it("refuses a non-attestor signer", async () => {
      const { vault, att1, outsider } = await loadFixture(deployFixture);
      const f = await makeFixing(ONE);
      const sigs = await signFixing(vault, [att1, outsider], f);
      await expect(vault.postNav(f, sigs))
        .to.be.revertedWithCustomError(vault, "NotAnAttestor").withArgs(outsider.address);
    });

    it("refuses a replay of the same asOfDate", async () => {
      const { vault, att1, att2 } = await loadFixture(deployFixture);
      const f = await makeFixing(ONE);
      const sigs = await signFixing(vault, [att1, att2], f);
      await vault.postNav(f, sigs);
      await expect(vault.postNav(f, sigs))
        .to.be.revertedWithCustomError(vault, "StaleFixing").withArgs(f.asOfDate, f.asOfDate);
      // A different value for the same day is also refused: one fixing per day.
      const f2 = { ...f, navPerShare: 2n * ONE };
      await expect(vault.postNav(f2, await signFixing(vault, [att1, att2], f2)))
        .to.be.revertedWithCustomError(vault, "StaleFixing");
    });

    it("refuses an older asOfDate than the latest posted", async () => {
      const { vault, att1, att2 } = await loadFixture(deployFixture);
      const today = await makeFixing(ONE);
      await vault.postNav(today, await signFixing(vault, [att1, att2], today));
      const yesterday = await makeFixing(ONE, -1n);
      await expect(vault.postNav(yesterday, await signFixing(vault, [att1, att2], yesterday)))
        .to.be.revertedWithCustomError(vault, "StaleFixing").withArgs(yesterday.asOfDate, today.asOfDate);
    });

    it("accepts the next day's fixing after the previous one", async () => {
      const { vault, att1, att2 } = await loadFixture(deployFixture);
      const d0 = await makeFixing(ONE, -1n);
      await vault.postNav(d0, await signFixing(vault, [att1, att2], d0));
      const d1 = await makeFixing(2n * ONE);
      await vault.postNav(d1, await signFixing(vault, [att1, att2], d1));
      const [nav] = await vault.navPerShare();
      expect(nav).to.equal(2n * ONE);
    });

    it("refuses signatures made for another chainId (domain mismatch)", async () => {
      const { vault, att1, att2 } = await loadFixture(deployFixture);
      const f = await makeFixing(ONE);
      const sigs = await signFixing(vault, [att1, att2], f, { chainId: 8453n });
      // Recovers to some random address that is not an attestor.
      await expect(vault.postNav(f, sigs)).to.be.revertedWithCustomError(vault, "NotAnAttestor");
    });

    it("refuses signatures made for another verifying contract (domain mismatch)", async () => {
      const { vault, att1, att2, tokA } = await loadFixture(deployFixture);
      const f = await makeFixing(ONE);
      const sigs = await signFixing(vault, [att1, att2], f, { verifyingContract: await tokA.getAddress() });
      await expect(vault.postNav(f, sigs)).to.be.revertedWithCustomError(vault, "NotAnAttestor");
    });

    it("refuses a fixing for another instrument, a zero NAV, a future date and a future signedAt", async () => {
      const { vault, att1, att2 } = await loadFixture(deployFixture);
      const other = { ...(await makeFixing(ONE)), instrumentId: ethers.keccak256(ethers.toUtf8Bytes("OTHER")) };
      await expect(vault.postNav(other, await signFixing(vault, [att1, att2], other)))
        .to.be.revertedWithCustomError(vault, "WrongInstrument");
      const zero = await makeFixing(0n);
      await expect(vault.postNav(zero, await signFixing(vault, [att1, att2], zero)))
        .to.be.revertedWithCustomError(vault, "ZeroNav");
      const future = await makeFixing(ONE, 3n);
      await expect(vault.postNav(future, await signFixing(vault, [att1, att2], future)))
        .to.be.revertedWithCustomError(vault, "FixingInFuture");
      const skew = { ...(await makeFixing(ONE)), signedAt: BigInt(await time.latest()) + 3600n };
      await expect(vault.postNav(skew, await signFixing(vault, [att1, att2], skew)))
        .to.be.revertedWithCustomError(vault, "SignedAtInFuture");
    });

    it("hashNavFixing matches the ethers EIP-712 digest", async () => {
      const { vault } = await loadFixture(deployFixture);
      const f = await makeFixing(ONE);
      const domain = await domainFor(vault);
      expect(await vault.hashNavFixing(f)).to.equal(ethers.TypedDataEncoder.hash(domain, NAV_TYPES, f));
    });

    it("navPerShare reverts and isNavFresh is false before any fixing; freshness expires", async () => {
      const { vault, att1, att2 } = await loadFixture(deployFixture);
      await expect(vault.navPerShare()).to.be.revertedWithCustomError(vault, "NoNavPosted");
      expect(await vault.isNavFresh(10n ** 9n)).to.equal(false);
      const f = await makeFixing(ONE);
      await vault.postNav(f, await signFixing(vault, [att1, att2], f));
      await time.increase(3600n);
      expect(await vault.isNavFresh(1800n)).to.equal(false);
      expect(await vault.isNavFresh(7200n)).to.equal(true);
    });

    it("setAttestors validates threshold, duplicates, zero address, and only ADMIN may call it", async () => {
      const { vault, admin, outsider, att1, att2, att3, att4 } = await loadFixture(deployFixture);
      await expect(vault.connect(admin).setAttestors([att1.address, att2.address], 1))
        .to.be.revertedWithCustomError(vault, "InvalidThreshold");
      await expect(vault.connect(admin).setAttestors([att1.address, att2.address], 3))
        .to.be.revertedWithCustomError(vault, "InvalidThreshold");
      await expect(vault.connect(admin).setAttestors([att1.address, att1.address], 2))
        .to.be.revertedWithCustomError(vault, "DuplicateAttestor").withArgs(att1.address);
      await expect(vault.connect(admin).setAttestors([att1.address, ethers.ZeroAddress], 2))
        .to.be.revertedWithCustomError(vault, "ZeroAddress");
      await expect(vault.connect(outsider).setAttestors([att1.address, att2.address], 2))
        .to.be.revertedWithCustomError(vault, "AccessControlUnauthorizedAccount");
      // Rotation replaces the old set entirely.
      await expect(vault.connect(admin).setAttestors([att3.address, att4.address], 2))
        .to.emit(vault, "AttestorsSet").withArgs([att3.address, att4.address], 2n);
      expect(await vault.isAttestor(att1.address)).to.equal(false);
      expect(await vault.isAttestor(att4.address)).to.equal(true);
      expect(await vault.attestors()).to.deep.equal([att3.address, att4.address]);
    });

    it("postNav reverts before any committee is configured", async () => {
      const { admin, feeRecipient, tokA, att1, att2 } = await loadFixture(deployFixture);
      const Vault = await ethers.getContractFactory("EtpBasketVault");
      const fresh = (await Vault.deploy({
        name: "x", symbol: "x", instrumentId: INSTRUMENT, admin: admin.address, feeRecipient: feeRecipient.address,
        constituents: [await tokA.getAddress()], unitsPerShare: [ONE],
      })) as unknown as EtpBasketVault;
      const f = await makeFixing(ONE);
      await expect(fresh.postNav(f, await signFixing(fresh, [att1, att2], f)))
        .to.be.revertedWithCustomError(fresh, "AttestorsNotConfigured");
    });
  });

  // ------------------------------------------------------------------
  describe("rebalance timelock", () => {
    it("only ADMIN may propose; proposal is recorded with applyAfter = now + delay", async () => {
      const { vault, admin, outsider, tokA, tokB } = await loadFixture(deployFixture);
      const c = [await tokA.getAddress(), await tokB.getAddress()];
      await expect(vault.connect(outsider).proposeBasket(c, [1n, 1n]))
        .to.be.revertedWithCustomError(vault, "AccessControlUnauthorizedAccount");
      const tx = await vault.connect(admin).proposeBasket(c, [UNITS_A * 2n, UNITS_B]);
      const ts = BigInt((await tx.getBlock())!.timestamp);
      await expect(tx).to.emit(vault, "BasketProposed").withArgs(2n, c, [UNITS_A * 2n, UNITS_B], ts + 2n * DAY);
      const p = await vault.pendingBasket();
      expect(p.exists).to.equal(true);
      expect(p.applyAfter).to.equal(ts + 2n * DAY);
    });

    it("refuses an early apply", async () => {
      const { vault, admin, tokA, tokB } = await loadFixture(deployFixture);
      await vault.connect(admin).proposeBasket([await tokA.getAddress(), await tokB.getAddress()], [UNITS_A * 2n, UNITS_B]);
      await time.increase(DAY);
      await expect(vault.connect(admin).applyBasket()).to.be.revertedWithCustomError(vault, "RebalanceTimelocked");
    });

    it("cancel works and clears the proposal; a second proposal while one is pending is refused", async () => {
      const { vault, admin, tokA, tokB } = await loadFixture(deployFixture);
      const c = [await tokA.getAddress(), await tokB.getAddress()];
      await vault.connect(admin).proposeBasket(c, [UNITS_A * 2n, UNITS_B]);
      await expect(vault.connect(admin).proposeBasket(c, [UNITS_A, UNITS_B])).to.be.revertedWithCustomError(vault, "ProposalPending");
      await expect(vault.connect(admin).cancelBasketProposal()).to.emit(vault, "BasketProposalCancelled").withArgs(2n);
      expect((await vault.pendingBasket()).exists).to.equal(false);
      await expect(vault.connect(admin).cancelBasketProposal()).to.be.revertedWithCustomError(vault, "NoProposal");
      await expect(vault.connect(admin).applyBasket()).to.be.revertedWithCustomError(vault, "NoProposal");
    });

    it("a weight-only change applies after the delay without pausing, provided the delta was brought", async () => {
      const { vault, admin, ap, tokA, tokB } = await loadFixture(deployFixture);
      await vault.connect(ap).create(100n * ONE, ap.address);
      const c = [await tokA.getAddress(), await tokB.getAddress()];
      await vault.connect(admin).proposeBasket(c, [UNITS_A * 2n, UNITS_B]);
      await time.increase(2n * DAY);
      // Not enough tokA yet: vault has 50, needs 100.
      await expect(vault.connect(admin).applyBasket())
        .to.be.revertedWithCustomError(vault, "InsufficientBacking").withArgs(c[0], 100n * 10n ** 6n, 50n * 10n ** 6n);
      await tokA.mint(await vault.getAddress(), 50n * 10n ** 6n); // admin brings the delta
      await expect(vault.connect(admin).applyBasket()).to.emit(vault, "BasketApplied").withArgs(2n, c, [UNITS_A * 2n, UNITS_B]);
      expect(await vault.epoch()).to.equal(2n);
      expect((await vault.pendingBasket()).exists).to.equal(false);
      // Redeem now pays the new weight.
      const [, amts] = await vault.previewRedeem(ONE);
      expect(amts[0]).to.equal(UNITS_A * 2n);
    });

    it("a constituent-set change requires the vault to be paused; full in-kind rebalance flow", async () => {
      const { vault, admin, ap, tokA, tokB, holder } = await loadFixture(deployFixture);
      await vault.connect(ap).create(100n * ONE, ap.address);
      const Mock = await ethers.getContractFactory("MockERC20");
      const tokC = (await Mock.deploy("Tokenised TSLA", "TSLAx", 8)) as unknown as MockERC20;
      const newC = [await tokA.getAddress(), await tokC.getAddress()];
      const newU = [UNITS_A, 10n ** 8n]; // swap B for 1.0 C
      await vault.connect(admin).proposeBasket(newC, newU);
      await time.increase(2n * DAY);
      await expect(vault.connect(admin).applyBasket()).to.be.revertedWithCustomError(vault, "RebalanceRequiresPause");
      await vault.connect(admin).pause();
      await tokC.mint(await vault.getAddress(), 100n * 10n ** 8n); // delta for 100 shares
      await vault.connect(admin).applyBasket();
      expect(await vault.isConstituent(await tokB.getAddress())).to.equal(false);
      expect(await vault.isConstituent(await tokC.getAddress())).to.equal(true);
      // Retired constituent is now sweepable in full; current ones are protected.
      const bBal = await tokB.balanceOf(await vault.getAddress());
      await expect(vault.connect(admin).sweepExcess(await tokB.getAddress(), holder.address, bBal))
        .to.emit(vault, "ExcessSwept").withArgs(await tokB.getAddress(), holder.address, bBal);
      await expect(vault.connect(admin).sweepExcess(await tokC.getAddress(), holder.address, 1n))
        .to.be.revertedWithCustomError(vault, "BackingWouldBreak");
      await vault.connect(admin).unpause();
      // Redeem delivers the new basket.
      await vault.connect(ap).redeem(10n * ONE, holder.address);
      expect(await tokC.balanceOf(holder.address)).to.equal(10n * 10n ** 8n);
    });

    it("sweepExcess allows dust above required backing and refuses more than the balance", async () => {
      const { vault, admin, ap, tokA, holder } = await loadFixture(deployFixture);
      await vault.connect(ap).create(100n * ONE, ap.address);
      await tokA.mint(await vault.getAddress(), 7n); // stray dust
      await vault.connect(admin).sweepExcess(await tokA.getAddress(), holder.address, 7n);
      expect(await tokA.balanceOf(holder.address)).to.equal(7n);
      await expect(vault.connect(admin).sweepExcess(await tokA.getAddress(), holder.address, 10n ** 12n))
        .to.be.revertedWithCustomError(vault, "BackingWouldBreak");
    });

    it("setRebalanceDelay enforces the 1-day minimum", async () => {
      const { vault, admin } = await loadFixture(deployFixture);
      await expect(vault.connect(admin).setRebalanceDelay(DAY - 1n)).to.be.revertedWithCustomError(vault, "RebalanceDelayTooShort");
      await expect(vault.connect(admin).setRebalanceDelay(DAY)).to.emit(vault, "RebalanceDelaySet").withArgs(2n * DAY, DAY);
    });

    it("applyBasket accrues the management fee first, so re-proposing NOMINAL units needs a delta", async () => {
      const { vault, admin, ap, tokA, tokB, feeRecipient } = await loadFixture(deployFixture);
      await vault.connect(admin).setFees(0, 0, 1000);
      await vault.connect(ap).create(100n * ONE, ap.address);
      const c = [await tokA.getAddress(), await tokB.getAddress()];
      await vault.connect(admin).proposeBasket(c, [UNITS_A, UNITS_B]); // original nominal weights
      await time.increase(2n * DAY);
      // Supply has grown by the fee, so 0.5 tokA per share now needs more than the vault holds.
      await expect(vault.connect(admin).applyBasket()).to.be.revertedWithCustomError(vault, "InsufficientBacking");
      await tokA.mint(await vault.getAddress(), 10n ** 6n);
      await tokB.mint(await vault.getAddress(), ONE);
      await vault.connect(admin).applyBasket();
      expect(await vault.balanceOf(feeRecipient.address)).to.be.gt(0n);
      expect(await vault.epoch()).to.equal(2n);
    });
  });

  // ------------------------------------------------------------------
  describe("pause / emergency exit", () => {
    it("pause blocks create and redeem; unpause restores both", async () => {
      const { vault, admin, ap } = await loadFixture(deployFixture);
      await vault.connect(ap).create(10n * ONE, ap.address);
      await vault.connect(admin).pause();
      await expect(vault.connect(ap).create(ONE, ap.address)).to.be.revertedWithCustomError(vault, "EnforcedPause");
      await expect(vault.connect(ap).redeem(ONE, ap.address)).to.be.revertedWithCustomError(vault, "EnforcedPause");
      await vault.connect(admin).unpause();
      await vault.connect(ap).create(ONE, ap.address);
      await vault.connect(ap).redeem(ONE, ap.address);
    });

    it("redeemWhilePaused lets holders exit in kind while creates stay blocked", async () => {
      const { vault, admin, ap, holder, tokA } = await loadFixture(deployFixture);
      await vault.connect(ap).create(10n * ONE, ap.address);
      await vault.connect(admin).pause();
      await expect(vault.connect(admin).setRedeemWhilePaused(true)).to.emit(vault, "RedeemWhilePausedSet").withArgs(true);
      await expect(vault.connect(ap).create(ONE, ap.address)).to.be.revertedWithCustomError(vault, "EnforcedPause");
      await vault.connect(ap).redeem(10n * ONE, holder.address);
      expect(await tokA.balanceOf(holder.address)).to.equal(5n * 10n ** 6n);
      await vault.connect(admin).setRedeemWhilePaused(false);
      await expect(vault.connect(ap).redeem(1n, holder.address)).to.be.revertedWithCustomError(vault, "EnforcedPause");
    });

    it("secondary transfers are not paused", async () => {
      const { vault, admin, ap, holder } = await loadFixture(deployFixture);
      await vault.connect(ap).create(ONE, ap.address);
      await vault.connect(admin).pause();
      await vault.connect(ap).transfer(holder.address, ONE);
      expect(await vault.balanceOf(holder.address)).to.equal(ONE);
    });

    it("only ADMIN may pause/unpause or toggle redeemWhilePaused", async () => {
      const { vault, outsider, ap } = await loadFixture(deployFixture);
      await expect(vault.connect(outsider).pause()).to.be.revertedWithCustomError(vault, "AccessControlUnauthorizedAccount");
      await expect(vault.connect(ap).unpause()).to.be.revertedWithCustomError(vault, "AccessControlUnauthorizedAccount");
      await expect(vault.connect(ap).setRedeemWhilePaused(true)).to.be.revertedWithCustomError(vault, "AccessControlUnauthorizedAccount");
    });
  });

  // ------------------------------------------------------------------
  describe("ERC20Permit", () => {
    it("permit sets an allowance via signature and shares the EIP-712 domain with NAV fixings", async () => {
      const { vault, ap, holder, outsider } = await loadFixture(deployFixture);
      await vault.connect(ap).create(ONE, holder.address);
      const domain = await domainFor(vault);
      const deadline = BigInt(await time.latest()) + 3600n;
      const value = ONE / 2n;
      const nonce = await vault.nonces(holder.address);
      const sig = await holder.signTypedData(
        domain,
        { Permit: [
          { name: "owner", type: "address" }, { name: "spender", type: "address" },
          { name: "value", type: "uint256" }, { name: "nonce", type: "uint256" }, { name: "deadline", type: "uint256" },
        ] },
        { owner: holder.address, spender: outsider.address, value, nonce, deadline }
      );
      const { v, r, s } = ethers.Signature.from(sig);
      await vault.permit(holder.address, outsider.address, value, deadline, v, r, s);
      expect(await vault.allowance(holder.address, outsider.address)).to.equal(value);
      expect(await vault.DOMAIN_SEPARATOR()).to.equal(ethers.TypedDataEncoder.hashDomain(domain));
    });
  });
});
