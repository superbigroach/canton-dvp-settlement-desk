// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import {ERC20} from "@openzeppelin/contracts/token/ERC20/ERC20.sol";
import {ERC20Permit} from "@openzeppelin/contracts/token/ERC20/extensions/ERC20Permit.sol";
import {IERC20} from "@openzeppelin/contracts/token/ERC20/IERC20.sol";
import {SafeERC20} from "@openzeppelin/contracts/token/ERC20/utils/SafeERC20.sol";
import {AccessControl} from "@openzeppelin/contracts/access/AccessControl.sol";
import {Pausable} from "@openzeppelin/contracts/utils/Pausable.sol";
import {ReentrancyGuard} from "@openzeppelin/contracts/utils/ReentrancyGuard.sol";
import {ECDSA} from "@openzeppelin/contracts/utils/cryptography/ECDSA.sol";
import {Math} from "@openzeppelin/contracts/utils/math/Math.sol";
import {IHolderRegistry} from "./interfaces/IHolderRegistry.sol";

/**
 * @title EtpBasketVault
 * @notice "Basket as a service": an ERC20 share token backed 1:1, in kind, by a
 *         fixed basket of tokenised stocks (xStocks on Base, Robinhood stock
 *         tokens, ...). Authorised participants (APs) create shares by
 *         delivering the whole basket atomically and redeem shares by
 *         receiving the whole basket atomically. A K-of-N attestor committee
 *         (the Canton fixing) signs the daily NAV off-chain and anyone may
 *         relay that signed fixing on-chain via {postNav}.
 *
 * @dev    WHAT THIS CONTRACT IS AND IS NOT
 *
 *         It is infrastructure. The share token it mints is, in every
 *         jurisdiction we care about, a security: it must be issued by a
 *         licensed issuer, and holder eligibility must be enforced. The vault
 *         does not decide who may hold it — it delegates that to an optional
 *         {IHolderRegistry} (see {_update}) which the issuer maintains.
 *
 *         Every underlying token has its own issuer allowlist. The VAULT
 *         ADDRESS must be whitelisted by each underlying's issuer before the
 *         first {create}, otherwise the inbound `safeTransferFrom` reverts
 *         and the whole create reverts with it (that is the atomicity you
 *         want, not a bug). Likewise a {redeem} receiver must be allowed by
 *         every underlying's issuer.
 *
 *         BACKING INVARIANT
 *
 *         For every constituent `i`, with effective units
 *         `u_i = nominalUnits[i] * unitsMultiplier / 1e18`:
 *             balanceOf(vault, i) >= mulDivUp(u_i, totalSupply, 1e18)
 *         Create rounds inbound amounts UP, redeem rounds outbound amounts
 *         DOWN, create/redeem fees are paid in shares (never in
 *         constituents), and the management fee mints shares while shrinking
 *         `unitsMultiplier` by the same ratio so `u_i * totalSupply` is
 *         unchanged — none of these can break the invariant. {applyBasket}
 *         and {sweepExcess} check it explicitly.
 *         Rounding dust accumulates in the vault and belongs pro rata to the
 *         remaining holders; it can never block a redemption because the
 *         floor of a holder's entitlement is always <= the vault balance.
 *
 *         NAV
 *
 *         NAV is INFORMATIONAL for the in-kind flows above: an AP delivers
 *         units, not value, so the vault never prices anything. The posted
 *         NAV exists for consumers that need a price — a lender taking
 *         shares as collateral, a cash-creation extension, a UI — and for
 *         them the guarantee is "K of N named attestors signed this exact
 *         number for this exact date". Consumers should also check
 *         {isNavFresh}.
 *
 *         REBALANCE (in kind)
 *
 *         The nominal basket is immutable within an epoch (only the fee
 *         multiplier moves). ADMIN proposes a new basket, a timelock elapses,
 *         then ADMIN applies it; the proposal's units become the new nominal
 *         AND effective units, so a rebalance also "re-bases" the fee drift
 *         (propose the current effective units to keep exposure unchanged;
 *         {basket} reports them). If the
 *         constituent SET changes the vault must be paused during apply so
 *         no create/redeem can straddle two definitions. The admin brings
 *         the delta: before {applyBasket} the vault must already hold enough
 *         of every NEW constituent to back `totalSupply` at the NEW units
 *         (transfer it in directly), and afterwards ADMIN removes what is no
 *         longer required with {sweepExcess}, which refuses to touch backing.
 */
contract EtpBasketVault is ERC20, ERC20Permit, AccessControl, Pausable, ReentrancyGuard {
    using SafeERC20 for IERC20;

    // ------------------------------------------------------------------
    // Types
    // ------------------------------------------------------------------

    /// @notice The EIP-712 struct the attestor committee signs.
    /// @param instrumentId    Which basket this fixing is for (must equal {instrumentId}).
    /// @param asOfDate        Valuation date as whole days since the Unix epoch.
    /// @param session         Session code (0 = close). Free for the rulebook to define.
    /// @param navPerShare     NAV per 1e18 shares, 18 decimals, in the quote currency.
    /// @param rulebookVersion Hash/tag of the calculation rulebook the committee applied.
    /// @param signedAt        Unix seconds when the committee produced the fixing.
    struct NavFixing {
        bytes32 instrumentId;
        uint64 asOfDate;
        uint8 session;
        uint256 navPerShare;
        bytes32 rulebookVersion;
        uint64 signedAt;
    }

    /// @notice What the vault remembers about the last accepted fixing.
    struct NavRecord {
        uint256 navPerShare;
        uint64 asOfDate;
        uint8 session;
        uint64 postedAt;
        bytes32 rulebookVersion;
        uint256 attestorCount;
    }

    struct BasketProposal {
        address[] constituents;
        uint256[] unitsPerShare;
        uint64 applyAfter;
        bool exists;
    }

    /// @dev Grouped constructor arguments to keep deploy scripts readable and
    ///      the constructor under the stack limit.
    struct InitParams {
        string name;
        string symbol;
        bytes32 instrumentId;
        address admin;
        address feeRecipient;
        address[] constituents;
        uint256[] unitsPerShare;
    }

    // ------------------------------------------------------------------
    // Constants
    // ------------------------------------------------------------------

    bytes32 public constant ADMIN_ROLE = keccak256("ADMIN_ROLE");
    bytes32 public constant AP_ROLE = keccak256("AP_ROLE");

    /// @notice Shares are 18-decimal; `unitsPerShare` is "per SHARE_UNIT shares".
    uint256 public constant SHARE_UNIT = 1e18;
    uint256 public constant BPS = 10_000;
    uint256 public constant MAX_CREATE_REDEEM_FEE_BPS = 500; // 5 %
    uint256 public constant MAX_MANAGEMENT_FEE_BPS = 1_000; // 10 % p.a.
    uint256 public constant MIN_REBALANCE_DELAY = 1 days;
    uint256 public constant DEFAULT_REBALANCE_DELAY = 2 days;
    /// @dev Longest gap a single accrual will charge for. Keeps the exact
    ///      dilution formula's denominator strictly positive whatever happens.
    uint256 public constant MAX_FEE_ACCRUAL_PERIOD = 365 days;
    uint256 public constant MAX_CONSTITUENTS = 64;
    uint256 public constant MAX_ATTESTORS = 32;
    uint256 public constant MIN_NAV_THRESHOLD = 2;
    /// @dev Tolerated clock skew between the committee's `signedAt` and the chain.
    uint256 public constant SIGNED_AT_MAX_SKEW = 10 minutes;

    bytes32 public constant NAV_FIXING_TYPEHASH =
        keccak256(
            "NavFixing(bytes32 instrumentId,uint64 asOfDate,uint8 session,uint256 navPerShare,bytes32 rulebookVersion,uint64 signedAt)"
        );

    // ------------------------------------------------------------------
    // Storage
    // ------------------------------------------------------------------

    /// @notice Identifier of the basket this vault represents; bound into every fixing.
    bytes32 public immutable instrumentId;

    address[] private _constituents;
    /// @dev NOMINAL units per SHARE_UNIT shares as defined for this epoch. The
    ///      amount an AP actually moves is `nominal * unitsMultiplier / 1e18`.
    uint256[] private _unitsPerShare;
    /// @notice Scales the epoch's nominal units down as the management fee
    ///         streams. 1e18 at every basket apply; multiplied by
    ///         `supplyBefore / supplyAfter` on every fee accrual.
    /// @dev    WHY: fee shares are minted without any constituents arriving,
    ///         so if units per share stayed fixed the vault would become
    ///         under-backed by exactly the fee and the last redeemer could not
    ///         exit. Shrinking the per-share basket instead keeps
    ///         `units * supply` (the backing) constant across an accrual —
    ///         the same mechanism as a streaming-fee position multiplier.
    uint256 public unitsMultiplier;
    /// @notice True for tokens in the CURRENT basket.
    mapping(address => bool) public isConstituent;
    /// @notice Incremented on every applied basket; 1 after construction.
    uint256 public epoch;

    BasketProposal private _proposal;
    /// @notice Seconds between {proposeBasket} and the earliest {applyBasket}.
    uint256 public rebalanceDelay;

    uint256 public createFeeBps;
    uint256 public redeemFeeBps;
    uint256 public managementFeeBps;
    address public feeRecipient;
    /// @notice Timestamp up to which the management fee has been accrued.
    uint256 public lastFeeAccrual;

    /// @notice Optional compliance hook; address(0) disables receiver gating.
    IHolderRegistry public registry;
    /// @notice Emergency-exit switch: when true, {redeem} works even while paused.
    bool public redeemWhilePaused;

    address[] private _attestors;
    mapping(address => bool) public isAttestor;
    /// @notice Minimum number of distinct attestor signatures a fixing needs.
    uint256 public navThreshold;

    NavRecord private _latestNav;

    // ------------------------------------------------------------------
    // Events
    // ------------------------------------------------------------------

    event Created(
        address indexed ap,
        address indexed receiver,
        uint256 grossShares,
        uint256 netShares,
        uint256 feeShares,
        uint256[] amountsIn
    );
    event Redeemed(
        address indexed ap,
        address indexed receiver,
        uint256 grossShares,
        uint256 netShares,
        uint256 feeShares,
        uint256[] amountsOut
    );
    event ManagementFeeAccrued(address indexed recipient, uint256 shares, uint256 fromTimestamp, uint256 toTimestamp);
    event FeesSet(uint256 createFeeBps, uint256 redeemFeeBps, uint256 managementFeeBps);
    event FeeRecipientSet(address indexed previous, address indexed current);
    event RegistrySet(address indexed previous, address indexed current);
    event RedeemWhilePausedSet(bool enabled);
    event RebalanceDelaySet(uint256 previous, uint256 current);
    event BasketProposed(uint256 indexed epoch, address[] constituents, uint256[] unitsPerShare, uint64 applyAfter);
    event BasketProposalCancelled(uint256 indexed epoch);
    event BasketApplied(uint256 indexed epoch, address[] constituents, uint256[] unitsPerShare);
    event ExcessSwept(address indexed token, address indexed to, uint256 amount);
    event AttestorsSet(address[] attestors, uint256 threshold);
    event NavPosted(bytes32 indexed instrumentId, uint64 indexed asOfDate, uint256 navPerShare, uint256 attestorCount);

    // ------------------------------------------------------------------
    // Errors
    // ------------------------------------------------------------------

    error ZeroAddress();
    error ZeroShares();
    error ZeroInstrumentId();
    error LengthMismatch();
    error EmptyBasket();
    error TooManyConstituents();
    error DuplicateConstituent(address token);
    error ZeroUnits(address token);
    error NotAContract(address token);
    error UnderDelivered(address token, uint256 expected, uint256 received);
    error ReceiverNotAllowed(address receiver);
    error FeeTooHigh();
    error RebalanceDelayTooShort();
    error ProposalPending();
    error NoProposal();
    error RebalanceTimelocked(uint64 applyAfter);
    error RebalanceRequiresPause();
    error InsufficientBacking(address token, uint256 required, uint256 actual);
    error BackingWouldBreak(address token, uint256 required, uint256 remaining);
    error AttestorsNotConfigured();
    error InvalidThreshold();
    error TooManyAttestors();
    error DuplicateAttestor(address attestor);
    error NotAnAttestor(address signer);
    error InsufficientSignatures(uint256 provided, uint256 required);
    error WrongInstrument(bytes32 provided, bytes32 expected);
    error ZeroNav();
    error StaleFixing(uint64 provided, uint64 latest);
    error FixingInFuture(uint64 asOfDate);
    error SignedAtInFuture(uint64 signedAt);
    error NoNavPosted();

    // ------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------

    /// @notice Deploys the vault with its first basket (epoch 1).
    /// @dev    `admin` receives DEFAULT_ADMIN_ROLE (role administration) and
    ///         ADMIN_ROLE (operations). APs and attestors are configured
    ///         afterwards so the deployer can hand over admin cleanly. The
    ///         EIP-712 domain used for both `permit` and NAV fixings is
    ///         `(name, "1", chainId, address(this))`, so a fixing signed for
    ///         one vault or one chain is worthless on any other.
    constructor(InitParams memory p) ERC20(p.name, p.symbol) ERC20Permit(p.name) {
        if (p.admin == address(0) || p.feeRecipient == address(0)) revert ZeroAddress();
        if (p.instrumentId == bytes32(0)) revert ZeroInstrumentId();
        _validateBasket(p.constituents, p.unitsPerShare);

        instrumentId = p.instrumentId;
        feeRecipient = p.feeRecipient;
        rebalanceDelay = DEFAULT_REBALANCE_DELAY;
        lastFeeAccrual = block.timestamp;

        _setBasket(p.constituents, p.unitsPerShare);
        epoch = 1;

        _grantRole(DEFAULT_ADMIN_ROLE, p.admin);
        _grantRole(ADMIN_ROLE, p.admin);

        emit BasketApplied(1, p.constituents, p.unitsPerShare);
        emit FeeRecipientSet(address(0), p.feeRecipient);
    }

    // ------------------------------------------------------------------
    // Primary market: create / redeem (AP only)
    // ------------------------------------------------------------------

    /// @notice Create `shares` by delivering the full basket in kind.
    /// @dev    WHY AP-only: primary-market access is a regulated relationship
    ///         (the AP agreement); anyone else must buy on the secondary market.
    ///         WHY round UP: the vault must never be under-backed by a wei, and
    ///         the AP is the professional counterparty who absorbs dust.
    ///         WHY fee in shares: paying the fee in constituents would need a
    ///         price; paying it in shares keeps the vault price-agnostic.
    ///         WHY balance delta check: fee-on-transfer or rebasing
    ///         underlyings would silently under-back the vault; we refuse
    ///         them rather than trust `transferFrom`'s return value.
    ///         Atomicity: any single constituent failing (not whitelisted,
    ///         insufficient allowance/balance, paused issuer) reverts the
    ///         whole call, so the AP never ends up half-delivered.
    /// @param shares   Gross shares to create (18 dp). The creation fee is
    ///                 deducted from this amount, so the receiver gets
    ///                 `shares - fee` and the fee recipient gets `fee`.
    /// @param receiver Recipient of the net shares (registry-gated if set).
    /// @return netShares Shares minted to `receiver`.
    function create(uint256 shares, address receiver)
        external
        nonReentrant
        whenNotPaused
        onlyRole(AP_ROLE)
        returns (uint256 netShares)
    {
        if (shares == 0) revert ZeroShares();
        if (receiver == address(0)) revert ZeroAddress();
        _accrueManagementFee();

        uint256 n = _constituents.length;
        uint256[] memory amounts = new uint256[](n);
        for (uint256 i = 0; i < n; ++i) {
            IERC20 token = IERC20(_constituents[i]);
            uint256 amount = _units(i, shares, Math.Rounding.Ceil);
            uint256 before = token.balanceOf(address(this));
            token.safeTransferFrom(msg.sender, address(this), amount);
            uint256 received = token.balanceOf(address(this)) - before;
            if (received < amount) revert UnderDelivered(address(token), amount, received);
            amounts[i] = amount;
        }

        uint256 fee = (shares * createFeeBps) / BPS;
        netShares = shares - fee;
        if (netShares == 0) revert ZeroShares();
        if (fee > 0) _mint(feeRecipient, fee);
        _mint(receiver, netShares);

        emit Created(msg.sender, receiver, shares, netShares, fee, amounts);
    }

    /// @notice Redeem `shares` for the full basket in kind.
    /// @dev    WHY round DOWN: the vault pays out at most what the shares are
    ///         entitled to, so the backing invariant survives and the last
    ///         holder out can always exit — floor(entitlement) <= balance.
    ///         WHY burn before transfer: checks-effects-interactions; a
    ///         malicious underlying cannot re-enter with un-burned shares
    ///         (nonReentrant guards the vault anyway).
    ///         WHY the fee is transferred, not burned: the fee recipient
    ///         keeps the shares (and their backing) — burning them would gift
    ///         the backing to remaining holders instead of the sponsor.
    ///         Pause: blocked while paused UNLESS {redeemWhilePaused} is on,
    ///         which is the emergency-exit posture (holders can leave, nobody
    ///         can enter).
    /// @param shares   Gross shares to redeem; the redeem fee is deducted.
    /// @param receiver Recipient of the constituents. Must be allowed by
    ///                 every underlying's issuer or the whole call reverts.
    /// @return amounts Constituent amounts delivered, in basket order.
    function redeem(uint256 shares, address receiver)
        external
        nonReentrant
        onlyRole(AP_ROLE)
        returns (uint256[] memory amounts)
    {
        if (paused() && !redeemWhilePaused) revert EnforcedPause();
        if (shares == 0) revert ZeroShares();
        if (receiver == address(0)) revert ZeroAddress();
        _accrueManagementFee();

        uint256 fee = (shares * redeemFeeBps) / BPS;
        uint256 netShares = shares - fee;
        if (netShares == 0) revert ZeroShares();

        if (fee > 0) _transfer(msg.sender, feeRecipient, fee);
        _burn(msg.sender, netShares);

        uint256 n = _constituents.length;
        amounts = new uint256[](n);
        for (uint256 i = 0; i < n; ++i) {
            uint256 amount = _units(i, netShares, Math.Rounding.Floor);
            amounts[i] = amount;
            if (amount > 0) IERC20(_constituents[i]).safeTransfer(receiver, amount);
        }

        emit Redeemed(msg.sender, receiver, shares, netShares, fee, amounts);
    }

    /// @notice Constituent amounts an AP must deliver to create `shares` (rounded up).
    function previewCreate(uint256 shares) external view returns (address[] memory tokens, uint256[] memory amounts) {
        uint256 n = _constituents.length;
        tokens = _constituents;
        amounts = new uint256[](n);
        for (uint256 i = 0; i < n; ++i) {
            amounts[i] = _units(i, shares, Math.Rounding.Ceil);
        }
    }

    /// @notice Constituent amounts a receiver gets for redeeming `shares` (after fee, rounded down).
    function previewRedeem(uint256 shares) external view returns (address[] memory tokens, uint256[] memory amounts) {
        uint256 netShares = shares - (shares * redeemFeeBps) / BPS;
        uint256 n = _constituents.length;
        tokens = _constituents;
        amounts = new uint256[](n);
        for (uint256 i = 0; i < n; ++i) {
            amounts[i] = _units(i, netShares, Math.Rounding.Floor);
        }
    }

    // ------------------------------------------------------------------
    // Management fee
    // ------------------------------------------------------------------

    /// @notice Mint the management fee accrued since the last accrual.
    /// @dev    Callable by anyone: accrual is deterministic and only ever
    ///         moves value to the fee recipient, so there is nothing to gain
    ///         from timing it. It runs automatically on every create, redeem,
    ///         fee change, recipient change and basket apply so that no
    ///         holder is charged for a period they were not in the vault.
    /// @return minted Shares minted to the fee recipient.
    function accrueFees() external returns (uint256 minted) {
        return _accrueManagementFee();
    }

    /// @notice Shares that {accrueFees} would mint right now.
    function pendingManagementFee() public view returns (uint256) {
        return _pendingManagementFee(block.timestamp);
    }

    /// @dev Exact dilution: after accrual the recipient owns fraction `f` of
    ///      the supply where `f = bps * elapsed / (BPS * 365 days)`, achieved
    ///      by minting `supply * f / (1 - f)`. Elapsed time is capped at
    ///      {MAX_FEE_ACCRUAL_PERIOD} per accrual; with fees capped at 10 % p.a.
    ///      that bounds `f` at 0.1 so the denominator is always positive. A
    ///      vault dormant for more than a year simply forfeits the excess.
    function _pendingManagementFee(uint256 nowTs) internal view returns (uint256) {
        uint256 supply = totalSupply();
        if (managementFeeBps == 0 || supply == 0 || nowTs <= lastFeeAccrual) return 0;
        uint256 elapsed = nowTs - lastFeeAccrual;
        if (elapsed > MAX_FEE_ACCRUAL_PERIOD) elapsed = MAX_FEE_ACCRUAL_PERIOD;
        // f scaled by 1e18
        uint256 f = Math.mulDiv(managementFeeBps * elapsed, 1e18, BPS * 365 days);
        return Math.mulDiv(supply, f, 1e18 - f);
    }

    /// @dev Mints the fee and shrinks {unitsMultiplier} by `supplyBefore /
    ///      supplyAfter` (rounded down) so `effectiveUnits * totalSupply` —
    ///      the backing requirement — never increases. Both roundings are in
    ///      the vault's favour.
    function _accrueManagementFee() internal returns (uint256 minted) {
        uint256 from = lastFeeAccrual;
        minted = _pendingManagementFee(block.timestamp);
        lastFeeAccrual = block.timestamp;
        if (minted > 0) {
            uint256 supplyBefore = totalSupply();
            uint256 supplyAfter = supplyBefore + minted;
            unitsMultiplier = Math.mulDiv(unitsMultiplier, supplyBefore, supplyAfter, Math.Rounding.Floor);
            _mint(feeRecipient, minted);
            emit ManagementFeeAccrued(feeRecipient, minted, from, block.timestamp);
        }
    }

    /// @dev Effective constituent amount for `shares` of constituent `i`:
    ///      `nominalUnits * unitsMultiplier * shares / 1e36`, in one mulDiv so
    ///      there is exactly one rounding step, in the direction the caller
    ///      asks for (Ceil inbound, Floor outbound).
    function _units(uint256 i, uint256 shares, Math.Rounding rounding) internal view returns (uint256) {
        return Math.mulDiv(_unitsPerShare[i] * unitsMultiplier, shares, SHARE_UNIT * 1e18, rounding);
    }

    // ------------------------------------------------------------------
    // NAV attestation (K-of-N committee, EIP-712)
    // ------------------------------------------------------------------

    /// @notice Post a committee-signed NAV fixing.
    /// @dev    Permissionless on purpose: the security is in the signatures,
    ///         not the caller, so any relay (the ETP Foundry relay, an AP, a
    ///         keeper) can carry the fixing from Canton to the chain.
    ///         Guarantees enforced here:
    ///           - the fixing is for THIS instrument, THIS vault and THIS
    ///             chain (EIP-712 domain binds chainId + verifyingContract);
    ///           - at least {navThreshold} DISTINCT registered attestors signed
    ///             the exact same struct;
    ///           - `asOfDate` is strictly newer than the last accepted fixing:
    ///             one fixing per day, no replay, no rollback;
    ///           - `asOfDate` is not in the future and `signedAt` is not
    ///             materially ahead of the chain clock.
    ///         NOT enforced: whether the number is right. That is the
    ///         committee's job and the rulebook's job.
    /// @param f    The fixing struct exactly as signed.
    /// @param sigs 65-byte ECDSA signatures over the EIP-712 digest of `f`.
    ///             Every signature provided is verified; `attestorCount`
    ///             reports how many were, which may exceed the threshold.
    function postNav(NavFixing calldata f, bytes[] calldata sigs) external {
        uint256 threshold = navThreshold;
        if (threshold == 0) revert AttestorsNotConfigured();
        if (f.instrumentId != instrumentId) revert WrongInstrument(f.instrumentId, instrumentId);
        if (f.navPerShare == 0) revert ZeroNav();
        uint64 latestDate = _latestNav.asOfDate;
        if (_latestNav.postedAt != 0 && f.asOfDate <= latestDate) revert StaleFixing(f.asOfDate, latestDate);
        if (uint256(f.asOfDate) * 1 days > block.timestamp + 1 days) revert FixingInFuture(f.asOfDate);
        if (f.signedAt > block.timestamp + SIGNED_AT_MAX_SKEW) revert SignedAtInFuture(f.signedAt);
        if (sigs.length < threshold) revert InsufficientSignatures(sigs.length, threshold);

        bytes32 digest = hashNavFixing(f);
        address[] memory seen = new address[](sigs.length);
        for (uint256 i = 0; i < sigs.length; ++i) {
            address signer = ECDSA.recover(digest, sigs[i]);
            if (!isAttestor[signer]) revert NotAnAttestor(signer);
            for (uint256 j = 0; j < i; ++j) {
                if (seen[j] == signer) revert DuplicateAttestor(signer);
            }
            seen[i] = signer;
        }

        _latestNav = NavRecord({
            navPerShare: f.navPerShare,
            asOfDate: f.asOfDate,
            session: f.session,
            postedAt: uint64(block.timestamp),
            rulebookVersion: f.rulebookVersion,
            attestorCount: sigs.length
        });

        emit NavPosted(f.instrumentId, f.asOfDate, f.navPerShare, sigs.length);
    }

    /// @notice EIP-712 digest a committee member signs for `f`.
    /// @dev    Exposed so relays and tests derive the digest from the vault
    ///         itself instead of re-implementing the domain.
    function hashNavFixing(NavFixing calldata f) public view returns (bytes32) {
        return
            _hashTypedDataV4(
                keccak256(
                    abi.encode(
                        NAV_FIXING_TYPEHASH,
                        f.instrumentId,
                        f.asOfDate,
                        f.session,
                        f.navPerShare,
                        f.rulebookVersion,
                        f.signedAt
                    )
                )
            );
    }

    /// @notice Latest NAV per share with its on-chain age.
    /// @dev    `ageSeconds` counts from when the fixing was POSTED, which is
    ///         what a consumer can act on; `asOfDate` tells you which
    ///         valuation day it describes. Reverts if nothing was ever posted
    ///         so a consumer can never mistake 0 for a price.
    function navPerShare() external view returns (uint256 nav, uint64 asOfDate, uint256 ageSeconds) {
        NavRecord storage r = _latestNav;
        if (r.postedAt == 0) revert NoNavPosted();
        return (r.navPerShare, r.asOfDate, block.timestamp - r.postedAt);
    }

    /// @notice Full record of the latest accepted fixing (all zero if none).
    function latestNav() external view returns (NavRecord memory) {
        return _latestNav;
    }

    /// @notice Whether a fixing was posted within the last `maxAgeSeconds`.
    function isNavFresh(uint256 maxAgeSeconds) external view returns (bool) {
        uint64 postedAt = _latestNav.postedAt;
        return postedAt != 0 && block.timestamp - postedAt <= maxAgeSeconds;
    }

    /// @notice Replace the attestor committee and its K-of-N threshold.
    /// @dev    Threshold >= 2 so no single key can post a price, <= n so a
    ///         fixing is always possible. Replacing the set does not reset
    ///         `latestNav`: monotonic dates survive committee rotation.
    function setAttestors(address[] calldata attestors_, uint256 threshold) external onlyRole(ADMIN_ROLE) {
        uint256 n = attestors_.length;
        if (n > MAX_ATTESTORS) revert TooManyAttestors();
        if (threshold < MIN_NAV_THRESHOLD || threshold > n) revert InvalidThreshold();

        address[] storage old = _attestors;
        for (uint256 i = 0; i < old.length; ++i) isAttestor[old[i]] = false;
        delete _attestors;

        for (uint256 i = 0; i < n; ++i) {
            address a = attestors_[i];
            if (a == address(0)) revert ZeroAddress();
            if (isAttestor[a]) revert DuplicateAttestor(a);
            isAttestor[a] = true;
            _attestors.push(a);
        }
        navThreshold = threshold;
        emit AttestorsSet(attestors_, threshold);
    }

    /// @notice Current attestor set.
    function attestors() external view returns (address[] memory) {
        return _attestors;
    }

    // ------------------------------------------------------------------
    // Basket definition and rebalance
    // ------------------------------------------------------------------

    /// @notice Current basket: constituents and EFFECTIVE units per 1e18
    ///         shares (nominal units scaled by {unitsMultiplier}) — what an AP
    ///         delivers or receives per share right now. Drifts down slowly
    ///         while a management fee is set; see {nominalBasket} for the
    ///         epoch definition.
    function basket() external view returns (address[] memory constituents, uint256[] memory unitsPerShare) {
        uint256 n = _constituents.length;
        unitsPerShare = new uint256[](n);
        for (uint256 i = 0; i < n; ++i) unitsPerShare[i] = _units(i, SHARE_UNIT, Math.Rounding.Floor);
        return (_constituents, unitsPerShare);
    }

    /// @notice The epoch's immutable definition: constituents and NOMINAL units.
    function nominalBasket() external view returns (address[] memory constituents, uint256[] memory unitsPerShare) {
        return (_constituents, _unitsPerShare);
    }

    function constituentCount() external view returns (uint256) {
        return _constituents.length;
    }

    /// @notice Constituent `i` with its nominal and effective units per share.
    function constituentAt(uint256 i) external view returns (address token, uint256 nominalUnits, uint256 effectiveUnits) {
        return (_constituents[i], _unitsPerShare[i], _units(i, SHARE_UNIT, Math.Rounding.Floor));
    }

    /// @notice Minimum balance of `token` the vault must hold to back `totalSupply`.
    /// @dev    0 for tokens outside the current basket.
    function requiredBacking(address token) public view returns (uint256) {
        if (!isConstituent[token]) return 0;
        uint256 n = _constituents.length;
        for (uint256 i = 0; i < n; ++i) {
            if (_constituents[i] == token) return _units(i, totalSupply(), Math.Rounding.Ceil);
        }
        return 0;
    }

    /// @notice The pending basket proposal, if any.
    function pendingBasket()
        external
        view
        returns (address[] memory constituents, uint256[] memory unitsPerShare, uint64 applyAfter, bool exists)
    {
        BasketProposal storage p = _proposal;
        return (p.constituents, p.unitsPerShare, p.applyAfter, p.exists);
    }

    /// @notice Propose the next basket; starts the {rebalanceDelay} timelock.
    /// @dev    WHY a timelock: holders and APs must be able to see a basket
    ///         change coming (and redeem in kind before it) — an instant
    ///         change would let an admin swap good collateral for bad. Only
    ///         one proposal may be pending; cancel it to replace it.
    function proposeBasket(address[] calldata newConstituents, uint256[] calldata newUnits)
        external
        onlyRole(ADMIN_ROLE)
    {
        if (_proposal.exists) revert ProposalPending();
        _validateBasket(newConstituents, newUnits);
        uint64 applyAfter = uint64(block.timestamp + rebalanceDelay);
        _proposal = BasketProposal({
            constituents: newConstituents,
            unitsPerShare: newUnits,
            applyAfter: applyAfter,
            exists: true
        });
        emit BasketProposed(epoch + 1, newConstituents, newUnits, applyAfter);
    }

    /// @notice Cancel the pending proposal.
    function cancelBasketProposal() external onlyRole(ADMIN_ROLE) {
        if (!_proposal.exists) revert NoProposal();
        delete _proposal;
        emit BasketProposalCancelled(epoch + 1);
    }

    /// @notice Apply the pending proposal once its timelock has elapsed.
    /// @dev    In-kind rebalance flow:
    ///           1. (if the constituent SET changes) ADMIN pauses the vault;
    ///           2. ADMIN transfers the delta of every NEW/increased
    ///              constituent directly to the vault so it holds
    ///              >= mulDivUp(newUnits, totalSupply) of each;
    ///           3. ADMIN calls applyBasket() — it accrues fees first (so the
    ///              backing check sees the post-fee supply), verifies the
    ///              backing for every new constituent, then swaps the
    ///              definition and bumps the epoch;
    ///           4. ADMIN calls {sweepExcess} to retrieve constituents that
    ///              are no longer required, then unpauses.
    ///         WHY pause when the set changes: an AP create/redeem landing
    ///         between the delta transfer and the apply would be priced
    ///         against a basket that no longer reflects reality. A pure
    ///         weight change with the same tokens is safe without pausing
    ///         because the backing check still holds and no token appears or
    ///         disappears mid-flight.
    function applyBasket() external nonReentrant onlyRole(ADMIN_ROLE) {
        BasketProposal storage p = _proposal;
        if (!p.exists) revert NoProposal();
        if (block.timestamp < p.applyAfter) revert RebalanceTimelocked(p.applyAfter);

        _accrueManagementFee();

        address[] memory newC = p.constituents;
        uint256[] memory newU = p.unitsPerShare;

        if (!_sameConstituentSet(newC) && !paused()) revert RebalanceRequiresPause();

        uint256 supply = totalSupply();
        for (uint256 i = 0; i < newC.length; ++i) {
            uint256 required = Math.mulDiv(newU[i], supply, SHARE_UNIT, Math.Rounding.Ceil);
            uint256 actual = IERC20(newC[i]).balanceOf(address(this));
            if (actual < required) revert InsufficientBacking(newC[i], required, actual);
        }

        _setBasket(newC, newU);
        epoch += 1;
        delete _proposal;
        emit BasketApplied(epoch, newC, newU);
    }

    /// @notice Withdraw tokens the vault holds beyond what backs the supply.
    /// @dev    Used after a rebalance to retrieve retired constituents, and
    ///         to return tokens sent to the vault by mistake. For a current
    ///         constituent the call refuses to dip below {requiredBacking};
    ///         for anything else the full balance is sweepable. `to` must be
    ///         allowed by the token's issuer or the transfer reverts.
    function sweepExcess(address token, address to, uint256 amount) external nonReentrant onlyRole(ADMIN_ROLE) {
        if (to == address(0)) revert ZeroAddress();
        if (isConstituent[token]) {
            uint256 required = requiredBacking(token);
            uint256 balance = IERC20(token).balanceOf(address(this));
            uint256 remaining = balance > amount ? balance - amount : 0;
            if (balance < amount || remaining < required) revert BackingWouldBreak(token, required, remaining);
        }
        IERC20(token).safeTransfer(to, amount);
        emit ExcessSwept(token, to, amount);
    }

    /// @notice Set the rebalance timelock (minimum {MIN_REBALANCE_DELAY}).
    function setRebalanceDelay(uint256 newDelay) external onlyRole(ADMIN_ROLE) {
        if (newDelay < MIN_REBALANCE_DELAY) revert RebalanceDelayTooShort();
        emit RebalanceDelaySet(rebalanceDelay, newDelay);
        rebalanceDelay = newDelay;
    }

    // ------------------------------------------------------------------
    // Fees, compliance and pause administration
    // ------------------------------------------------------------------

    /// @notice Set creation, redemption and management fees (bps).
    /// @dev    Accrues the management fee at the OLD rate first so a rate
    ///         change is never retroactive.
    function setFees(uint256 createFeeBps_, uint256 redeemFeeBps_, uint256 managementFeeBps_)
        external
        onlyRole(ADMIN_ROLE)
    {
        if (createFeeBps_ > MAX_CREATE_REDEEM_FEE_BPS || redeemFeeBps_ > MAX_CREATE_REDEEM_FEE_BPS) revert FeeTooHigh();
        if (managementFeeBps_ > MAX_MANAGEMENT_FEE_BPS) revert FeeTooHigh();
        _accrueManagementFee();
        createFeeBps = createFeeBps_;
        redeemFeeBps = redeemFeeBps_;
        managementFeeBps = managementFeeBps_;
        emit FeesSet(createFeeBps_, redeemFeeBps_, managementFeeBps_);
    }

    /// @notice Change who receives fees.
    /// @dev    Accrues to the OLD recipient first. The new recipient must be
    ///         allowed by the registry (if set) or the next fee mint reverts —
    ///         and with it every create and redeem. Check before switching.
    function setFeeRecipient(address newRecipient) external onlyRole(ADMIN_ROLE) {
        if (newRecipient == address(0)) revert ZeroAddress();
        _accrueManagementFee();
        emit FeeRecipientSet(feeRecipient, newRecipient);
        feeRecipient = newRecipient;
    }

    /// @notice Set (or clear with address(0)) the holder registry.
    /// @dev    Takes effect on the next mint/transfer. Existing balances are
    ///         not retroactively frozen: an address removed from the registry
    ///         can still SEND and still be redeemed via an AP; it just cannot
    ///         RECEIVE. The fee recipient and every AP creation receiver must
    ///         be allowed.
    function setRegistry(IHolderRegistry newRegistry) external onlyRole(ADMIN_ROLE) {
        emit RegistrySet(address(registry), address(newRegistry));
        registry = newRegistry;
    }

    /// @notice Enable/disable redemptions while paused (emergency exit).
    /// @dev    WHY: a pause is usually a response to a problem with an
    ///         underlying or an AP. Letting holders leave in kind while no
    ///         one can enter is the conservative posture; ADMIN opts in
    ///         because in some incidents (e.g. a compromised underlying) an
    ///         exit could itself be harmful.
    function setRedeemWhilePaused(bool enabled) external onlyRole(ADMIN_ROLE) {
        redeemWhilePaused = enabled;
        emit RedeemWhilePausedSet(enabled);
    }

    /// @notice Pause creates (and redeems unless {redeemWhilePaused}).
    /// @dev    Secondary-market transfers are NOT paused: the vault is not
    ///         the venue and freezing holders is the registry's job.
    function pause() external onlyRole(ADMIN_ROLE) {
        _pause();
    }

    function unpause() external onlyRole(ADMIN_ROLE) {
        _unpause();
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    /// @dev Compliance hook. Receivers of mints and transfers must be allowed
    ///      by the registry when one is set; burns (to == 0) never are, so
    ///      redemption is always possible. This is where "holder restrictions
    ///      inherited via the registry" is enforced for the SHARE token; the
    ///      underlyings enforce their own restrictions on the vault.
    function _update(address from, address to, uint256 value) internal override {
        if (to != address(0)) {
            IHolderRegistry r = registry;
            if (address(r) != address(0) && !r.isAllowed(to)) revert ReceiverNotAllowed(to);
        }
        super._update(from, to, value);
    }

    function _validateBasket(address[] memory constituents, uint256[] memory units) internal view {
        uint256 n = constituents.length;
        if (n == 0) revert EmptyBasket();
        if (n != units.length) revert LengthMismatch();
        if (n > MAX_CONSTITUENTS) revert TooManyConstituents();
        for (uint256 i = 0; i < n; ++i) {
            address t = constituents[i];
            if (t == address(0)) revert ZeroAddress();
            if (t.code.length == 0) revert NotAContract(t);
            if (units[i] == 0) revert ZeroUnits(t);
            for (uint256 j = 0; j < i; ++j) {
                if (constituents[j] == t) revert DuplicateConstituent(t);
            }
        }
    }

    /// @dev Installs a basket and resets the fee multiplier: the new epoch's
    ///      nominal units are, by definition, the effective units at apply time.
    function _setBasket(address[] memory constituents, uint256[] memory units) internal {
        address[] storage old = _constituents;
        for (uint256 i = 0; i < old.length; ++i) isConstituent[old[i]] = false;
        delete _constituents;
        delete _unitsPerShare;
        unitsMultiplier = 1e18;
        for (uint256 i = 0; i < constituents.length; ++i) {
            _constituents.push(constituents[i]);
            _unitsPerShare.push(units[i]);
            isConstituent[constituents[i]] = true;
        }
    }

    /// @dev True when `newC` contains exactly the current constituents (any order).
    function _sameConstituentSet(address[] memory newC) internal view returns (bool) {
        if (newC.length != _constituents.length) return false;
        for (uint256 i = 0; i < newC.length; ++i) {
            if (!isConstituent[newC[i]]) return false;
        }
        return true; // same length + no duplicates (validated) + all present => same set
    }
}
