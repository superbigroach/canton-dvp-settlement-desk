//! On-chain state for the ETP Foundry basket vault.
//!
//! One `Vault` per instrument (series). The vault PDA is the mint authority of the share
//! token and the owner of every constituent token account, so nothing can move in or out
//! of the basket except through the instructions in this program.

use anchor_lang::prelude::*;

use crate::errors::VaultError;

/// Hard cap on constituents. 16 keeps `create`/`redeem` (one CPI per constituent) inside
/// a single transaction's compute budget and keeps the account rent-sized.
pub const MAX_CONSTITUENTS: usize = 16;
/// Hard cap on committee members (the Canton fixing committee is small by design).
pub const MAX_ATTESTORS: usize = 10;
/// Hard cap on authorised participants held inline in the vault.
pub const MAX_APS: usize = 32;
/// Hard cap on allow-listed holders in a `HolderRegistry`.
pub const MAX_HOLDERS: usize = 128;
/// Share token decimals. `units_per_share` is expressed per `SHARE_UNIT` share base units.
pub const SHARE_DECIMALS: u8 = 9;
pub const SHARE_UNIT: u128 = 1_000_000_000;
pub const BPS_DENOMINATOR: u128 = 10_000;
/// Minimum rebalance timelock: holders must get at least one day to redeem at the old
/// composition before it changes under them.
pub const MIN_REBALANCE_DELAY_SECS: i64 = 86_400;
pub const SECONDS_PER_DAY: i64 = 86_400;
pub const SECONDS_PER_YEAR: u128 = 31_536_000;
/// A fixing is only a committee fixing if at least two members attested it.
pub const MIN_THRESHOLD: u8 = 2;
/// Caps on fees so an admin cannot confiscate a basket by setting a 100% fee.
pub const MAX_CREATE_REDEEM_FEE_BPS: u16 = 1_000; // 10%
pub const MAX_MANAGEMENT_FEE_BPS: u16 = 500; // 5% p.a.

pub const VAULT_SEED: &[u8] = b"vault";
pub const SHARE_MINT_SEED: &[u8] = b"share_mint";
pub const REGISTRY_SEED: &[u8] = b"registry";

/// One line of the basket: `units_per_share` base units of `mint` back every
/// `SHARE_UNIT` (1.000000000) share base units.
#[derive(AnchorSerialize, AnchorDeserialize, Clone, Copy, PartialEq, Eq, Debug, InitSpace)]
pub struct Constituent {
    pub mint: Pubkey,
    pub units_per_share: u64,
}

/// The most recently accepted committee fixing, kept on-chain so any program or
/// indexer can read "what is one share worth, as of when, under which rulebook".
#[derive(AnchorSerialize, AnchorDeserialize, Clone, Copy, PartialEq, Eq, Debug, Default, InitSpace)]
pub struct NavRecord {
    pub instrument_id: [u8; 32],
    /// Whole days since the Unix epoch. Strictly increasing across accepted fixings.
    pub as_of_date: u32,
    /// Session code the rulebook defines (0 = close).
    pub session: u8,
    /// NAV per share in the quote currency, scaled so 1_000_000_000 == 1.0.
    pub nav_per_share: u64,
    /// Hash/tag of the calculation rulebook the committee applied.
    pub rulebook_version: [u8; 32],
    /// Cluster time the fixing was accepted on-chain. 0 == never posted.
    pub posted_at: i64,
    /// How many distinct committee members signed this fixing.
    pub attestor_count: u8,
    /// sha256 of the Canton `NavFixing` contract id this record projects.
    pub fixing_ref: [u8; 32],
    /// Desk fixing tier of this record, on the `SeriesRow.labelFor` scale (see
    /// `SEED_TIER` / `MAX_TIER_CEILING`).
    pub tier: u8,
}

/// The desk's fixing-tier scale is defined in ONE place, the backend's
/// `backend/src/main/java/com/lucilla/settlement/benchmarks/SeriesRow.java`
/// (`labelFor`): 0 = seed (unattested placeholder), 1 = attested, 2 = alternate-seats,
/// 3 = benchmark-x-factor, 4 = carried-forward, 5 = missed. It ASCENDS as trust
/// decreases, so the vault gate is a maximum: `tier <= max_tier`, and tier 0 is never
/// acceptable at any setting.
pub const SEED_TIER: u8 = 0;
/// Default maximum tier: only attested (tier 1) committee fixings are accepted.
pub const DEFAULT_MAX_TIER: u8 = 1;
/// Highest value `max_tier` may be set to (tier 5 = missed).
pub const MAX_TIER_CEILING: u8 = 5;

/// A proposed basket change waiting out its timelock.
#[derive(AnchorSerialize, AnchorDeserialize, Clone, PartialEq, Eq, Debug, InitSpace)]
pub struct PendingBasket {
    #[max_len(MAX_CONSTITUENTS)]
    pub constituents: Vec<Constituent>,
    pub proposed_at: i64,
    pub apply_after: i64,
}

#[account]
#[derive(InitSpace)]
pub struct Vault {
    pub bump: u8,
    pub share_mint_bump: u8,
    /// Administrator. Manages APs, attestors, fees, pause and rebalances. Cannot move
    /// constituents except through `rebalance_withdraw` while paused.
    pub authority: Pubkey,
    /// Series identity; also the PDA seed and part of every signed fixing.
    pub instrument_id: [u8; 32],
    pub share_mint: Pubkey,
    #[max_len(MAX_CONSTITUENTS)]
    pub constituents: Vec<Constituent>,
    pub create_fee_bps: u16,
    pub redeem_fee_bps: u16,
    pub management_fee_bps_per_year: u16,
    pub last_accrual_ts: i64,
    pub fee_recipient: Pubkey,
    #[max_len(MAX_APS)]
    pub authorised_participants: Vec<Pubkey>,
    #[max_len(MAX_ATTESTORS)]
    pub attestors: Vec<Pubkey>,
    pub threshold: u8,
    /// Fixings with `tier > max_tier` (or tier 0) are refused. Default 1: attested only.
    pub max_tier: u8,
    pub latest_nav: NavRecord,
    pub pending_basket: Option<PendingBasket>,
    pub rebalance_delay_secs: i64,
    pub paused: bool,
    pub redeem_while_paused: bool,
    /// When `Some`, receivers of newly created shares must be allow-listed there.
    pub holder_registry: Option<Pubkey>,
}

impl Vault {
    pub fn is_ap(&self, key: &Pubkey) -> bool {
        self.authorised_participants.iter().any(|k| k == key)
    }

    pub fn is_attestor(&self, key: &Pubkey) -> bool {
        self.attestors.iter().any(|k| k == key)
    }

    /// A basket is valid when it is non-empty, within the cap, has no repeated mint and
    /// no zero weight. A zero weight would let `create` mint shares against nothing.
    pub fn validate_constituents(constituents: &[Constituent]) -> Result<()> {
        require!(!constituents.is_empty(), VaultError::EmptyBasket);
        require!(constituents.len() <= MAX_CONSTITUENTS, VaultError::TooManyConstituents);
        for (i, c) in constituents.iter().enumerate() {
            require!(c.units_per_share > 0, VaultError::ZeroUnitsPerShare);
            require!(
                !constituents[..i].iter().any(|p| p.mint == c.mint),
                VaultError::DuplicateConstituent
            );
        }
        Ok(())
    }

    /// Attestor sets must be distinct keys and the threshold must be reachable and >= 2.
    pub fn validate_attestors(attestors: &[Pubkey], threshold: u8) -> Result<()> {
        require!(!attestors.is_empty(), VaultError::NoAttestors);
        require!(attestors.len() <= MAX_ATTESTORS, VaultError::TooManyAttestors);
        require!(threshold >= MIN_THRESHOLD, VaultError::ThresholdTooLow);
        require!((threshold as usize) <= attestors.len(), VaultError::ThresholdUnreachable);
        for (i, a) in attestors.iter().enumerate() {
            require!(!attestors[..i].iter().any(|p| p == a), VaultError::DuplicateAttestor);
        }
        Ok(())
    }

    pub fn validate_fees(create_bps: u16, redeem_bps: u16, mgmt_bps: u16) -> Result<()> {
        require!(create_bps <= MAX_CREATE_REDEEM_FEE_BPS, VaultError::FeeTooHigh);
        require!(redeem_bps <= MAX_CREATE_REDEEM_FEE_BPS, VaultError::FeeTooHigh);
        require!(mgmt_bps <= MAX_MANAGEMENT_FEE_BPS, VaultError::FeeTooHigh);
        Ok(())
    }

    /// True when the set of mints differs (a mint added or removed), as opposed to a pure
    /// weight change. Set changes need the vault paused so inventory can be brought in
    /// line before anyone creates or redeems against the new definition.
    pub fn mint_set_changes(current: &[Constituent], next: &[Constituent]) -> bool {
        if current.len() != next.len() {
            return true;
        }
        current.iter().any(|c| !next.iter().any(|n| n.mint == c.mint))
    }
}

/// Allow-list of wallets permitted to receive shares. Lives at a PDA of the vault so a
/// vault has at most one registry and the registry cannot be swapped for a look-alike.
#[account]
#[derive(InitSpace)]
pub struct HolderRegistry {
    pub bump: u8,
    pub vault: Pubkey,
    pub authority: Pubkey,
    #[max_len(MAX_HOLDERS)]
    pub allowed: Vec<Pubkey>,
}

impl HolderRegistry {
    pub fn is_allowed(&self, holder: &Pubkey) -> bool {
        self.allowed.iter().any(|k| k == holder)
    }
}

/// Integer helpers. All basket arithmetic is u128 so a 16-line basket with u64 weights
/// cannot overflow mid-multiplication; results are checked back into u64.
pub fn mul_bps_floor(amount: u64, bps: u16) -> Result<u64> {
    let v = (amount as u128)
        .checked_mul(bps as u128)
        .ok_or(VaultError::MathOverflow)?
        / BPS_DENOMINATOR;
    u64::try_from(v).map_err(|_| error!(VaultError::MathOverflow))
}

/// Units the AP must deposit for `shares`: rounded UP so the vault is never short.
pub fn units_for_create(shares: u64, units_per_share: u64) -> Result<u64> {
    let raw = (shares as u128)
        .checked_mul(units_per_share as u128)
        .ok_or(VaultError::MathOverflow)?;
    let v = raw
        .checked_add(SHARE_UNIT - 1)
        .ok_or(VaultError::MathOverflow)?
        / SHARE_UNIT;
    u64::try_from(v).map_err(|_| error!(VaultError::MathOverflow))
}

/// Units the AP receives for `shares`: rounded DOWN so the vault is never short.
pub fn units_for_redeem(shares: u64, units_per_share: u64) -> Result<u64> {
    let raw = (shares as u128)
        .checked_mul(units_per_share as u128)
        .ok_or(VaultError::MathOverflow)?;
    u64::try_from(raw / SHARE_UNIT).map_err(|_| error!(VaultError::MathOverflow))
}

/// Management fee in shares for `elapsed` seconds on `supply` shares, floor-rounded.
pub fn management_fee_shares(supply: u64, bps_per_year: u16, elapsed_secs: i64) -> Result<u64> {
    if elapsed_secs <= 0 || bps_per_year == 0 || supply == 0 {
        return Ok(0);
    }
    let v = (supply as u128)
        .checked_mul(bps_per_year as u128)
        .ok_or(VaultError::MathOverflow)?
        .checked_mul(elapsed_secs as u128)
        .ok_or(VaultError::MathOverflow)?
        / (BPS_DENOMINATOR * SECONDS_PER_YEAR);
    u64::try_from(v).map_err(|_| error!(VaultError::MathOverflow))
}
