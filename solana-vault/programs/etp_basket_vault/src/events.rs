//! One event per state change so an indexer can rebuild the vault's history without
//! diffing account snapshots.

use anchor_lang::prelude::*;

use crate::state::Constituent;

#[event]
pub struct VaultInitialized {
    pub vault: Pubkey,
    pub authority: Pubkey,
    pub instrument_id: [u8; 32],
    pub share_mint: Pubkey,
    pub constituents: Vec<Constituent>,
    pub attestors: Vec<Pubkey>,
    pub threshold: u8,
    pub rebalance_delay_secs: i64,
}

#[event]
pub struct AuthorityTransferred {
    pub vault: Pubkey,
    pub previous: Pubkey,
    pub next: Pubkey,
}

#[event]
pub struct AuthorisedParticipantSet {
    pub vault: Pubkey,
    pub participant: Pubkey,
    pub allowed: bool,
}

#[event]
pub struct AttestorsSet {
    pub vault: Pubkey,
    pub attestors: Vec<Pubkey>,
    pub threshold: u8,
}

#[event]
pub struct FeesSet {
    pub vault: Pubkey,
    pub create_fee_bps: u16,
    pub redeem_fee_bps: u16,
    pub management_fee_bps_per_year: u16,
    pub fee_recipient: Pubkey,
}

#[event]
pub struct HolderRegistryInitialized {
    pub vault: Pubkey,
    pub registry: Pubkey,
}

#[event]
pub struct HolderRegistrySet {
    pub vault: Pubkey,
    pub registry: Option<Pubkey>,
}

#[event]
pub struct HolderAllowedSet {
    pub registry: Pubkey,
    pub holder: Pubkey,
    pub allowed: bool,
}

#[event]
pub struct BasketProposed {
    pub vault: Pubkey,
    pub constituents: Vec<Constituent>,
    pub apply_after: i64,
    pub set_changes: bool,
}

#[event]
pub struct BasketCancelled {
    pub vault: Pubkey,
}

#[event]
pub struct BasketApplied {
    pub vault: Pubkey,
    pub previous: Vec<Constituent>,
    pub next: Vec<Constituent>,
}

#[event]
pub struct SharesCreated {
    pub vault: Pubkey,
    pub ap: Pubkey,
    pub receiver: Pubkey,
    pub shares_requested: u64,
    pub shares_to_receiver: u64,
    pub fee_shares: u64,
    /// Units deposited, in basket order.
    pub units_in: Vec<u64>,
}

#[event]
pub struct SharesRedeemed {
    pub vault: Pubkey,
    pub ap: Pubkey,
    pub shares_burned: u64,
    pub fee_shares: u64,
    /// Units returned, in basket order.
    pub units_out: Vec<u64>,
}

#[event]
pub struct FeesAccrued {
    pub vault: Pubkey,
    pub from_ts: i64,
    pub to_ts: i64,
    pub supply_before: u64,
    pub fee_shares: u64,
}

#[event]
pub struct NavPosted {
    pub vault: Pubkey,
    pub instrument_id: [u8; 32],
    pub as_of_date: u32,
    pub session: u8,
    pub nav_per_share: u64,
    pub rulebook_version: [u8; 32],
    pub fixing_ref: [u8; 32],
    pub tier: u8,
    pub attestors: Vec<Pubkey>,
    pub relay: Pubkey,
}

#[event]
pub struct MaxTierSet {
    pub vault: Pubkey,
    pub max_tier: u8,
}

#[event]
pub struct PauseSet {
    pub vault: Pubkey,
    pub paused: bool,
}

#[event]
pub struct RedeemWhilePausedSet {
    pub vault: Pubkey,
    pub redeem_while_paused: bool,
}

#[event]
pub struct RebalanceWithdrawal {
    pub vault: Pubkey,
    pub mint: Pubkey,
    pub destination: Pubkey,
    pub amount: u64,
}
