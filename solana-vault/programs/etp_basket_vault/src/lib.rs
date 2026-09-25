//! ETP Foundry basket vault (Solana / Anchor).
//!
//! A share token that represents a FIXED basket of tokenised stocks. Authorised
//! participants (APs) create shares by depositing exact units of every constituent in
//! one atomic transaction, and redeem by burning shares and taking the units back. The
//! NAV is not computed here: an off-chain K-of-N committee (the Canton fixing) signs it
//! and a relay posts it with Ed25519 proofs. Holder restrictions are enforced through an
//! on-chain allow-list on the create side and, for the tokenised stocks themselves, by
//! the issuer's Token-2022 transfer hooks (see README).
//!
//! Devnet only. Never deploy to mainnet-beta.

#![allow(clippy::too_many_arguments)]
#![allow(unexpected_cfgs)]

use anchor_lang::prelude::*;
use anchor_lang::solana_program::sysvar::instructions::ID as INSTRUCTIONS_SYSVAR_ID;
use anchor_spl::token::Token;
use anchor_spl::token_2022::Token2022;
use anchor_spl::token_interface::{
    self, Burn, Mint, MintTo, TokenAccount, TokenInterface, TransferChecked,
};

pub mod errors;
pub mod events;
pub mod nav;
pub mod state;

use errors::VaultError;
use events::*;
use nav::{canonical_nav_message, verify_attestations, NavFixingArgs};
use state::*;

declare_id!("ERs1iunZ9RWCRCWTaND3B1YNBNcs1bAPZWfCU9YByc5m");

#[program]
pub mod etp_basket_vault {
    use super::*;

    /// Creates the vault PDA and the share mint (vault PDA = mint + freeze authority).
    ///
    /// WHY: the vault must be the only thing that can mint shares, otherwise a share is
    /// not a claim on the basket. Making the share mint a PDA of the vault (rather than a
    /// client-supplied mint) means there is exactly one share mint per instrument and it
    /// cannot be pre-minted before the vault exists.
    pub fn initialize_vault(ctx: Context<InitializeVault>, args: InitializeVaultArgs) -> Result<()> {
        require!(args.instrument_id != [0u8; 32], VaultError::ZeroInstrumentId);
        Vault::validate_constituents(&args.constituents)?;
        Vault::validate_attestors(&args.attestors, args.threshold)?;
        Vault::validate_fees(args.create_fee_bps, args.redeem_fee_bps, args.management_fee_bps_per_year)?;
        require!(
            args.rebalance_delay_secs >= MIN_REBALANCE_DELAY_SECS,
            VaultError::RebalanceDelayTooShort
        );

        let now = Clock::get()?.unix_timestamp;
        let vault = &mut ctx.accounts.vault;
        vault.bump = ctx.bumps.vault;
        vault.share_mint_bump = ctx.bumps.share_mint;
        vault.authority = ctx.accounts.authority.key();
        vault.instrument_id = args.instrument_id;
        vault.share_mint = ctx.accounts.share_mint.key();
        vault.constituents = args.constituents.clone();
        vault.create_fee_bps = args.create_fee_bps;
        vault.redeem_fee_bps = args.redeem_fee_bps;
        vault.management_fee_bps_per_year = args.management_fee_bps_per_year;
        vault.last_accrual_ts = now;
        vault.fee_recipient = args.fee_recipient;
        vault.authorised_participants = Vec::new();
        vault.attestors = args.attestors.clone();
        vault.threshold = args.threshold;
        vault.max_tier = DEFAULT_MAX_TIER;
        vault.latest_nav = NavRecord::default();
        vault.pending_basket = None;
        vault.rebalance_delay_secs = args.rebalance_delay_secs;
        vault.paused = false;
        vault.redeem_while_paused = false;
        vault.holder_registry = None;

        emit!(VaultInitialized {
            vault: vault.key(),
            authority: vault.authority,
            instrument_id: vault.instrument_id,
            share_mint: vault.share_mint,
            constituents: args.constituents,
            attestors: args.attestors,
            threshold: args.threshold,
            rebalance_delay_secs: args.rebalance_delay_secs,
        });
        Ok(())
    }

    /// Hands the admin role to another key.
    ///
    /// WHY: the admin key will move (e.g. to a multisig) after launch; without this the
    /// only route is redeploying the series.
    pub fn set_authority(ctx: Context<AdminOnly>, new_authority: Pubkey) -> Result<()> {
        let vault = &mut ctx.accounts.vault;
        let previous = vault.authority;
        vault.authority = new_authority;
        emit!(AuthorityTransferred { vault: vault.key(), previous, next: new_authority });
        Ok(())
    }

    /// Adds or removes an authorised participant.
    ///
    /// WHY: only vetted APs may create/redeem, mirroring the primary market of a listed
    /// ETP. This is what stops an arbitrary wallet from minting shares against the basket.
    pub fn set_ap(ctx: Context<AdminOnly>, participant: Pubkey, allowed: bool) -> Result<()> {
        let vault = &mut ctx.accounts.vault;
        if allowed {
            if !vault.is_ap(&participant) {
                require!(
                    vault.authorised_participants.len() < MAX_APS,
                    VaultError::TooManyAuthorisedParticipants
                );
                vault.authorised_participants.push(participant);
            }
        } else {
            vault.authorised_participants.retain(|k| *k != participant);
        }
        emit!(AuthorisedParticipantSet { vault: vault.key(), participant, allowed });
        Ok(())
    }

    /// Replaces the committee roster and threshold.
    ///
    /// WHY: committee membership rotates. The threshold floor of 2 is enforced again here
    /// so a later admin cannot degrade a committee fixing to a single signer.
    pub fn set_attestors(ctx: Context<AdminOnly>, attestors: Vec<Pubkey>, threshold: u8) -> Result<()> {
        Vault::validate_attestors(&attestors, threshold)?;
        let vault = &mut ctx.accounts.vault;
        vault.attestors = attestors.clone();
        vault.threshold = threshold;
        emit!(AttestorsSet { vault: vault.key(), attestors, threshold });
        Ok(())
    }

    /// Sets the WORST fixing tier the vault will accept (1..=5).
    ///
    /// WHY a maximum: the desk's tier scale is defined by `SeriesRow.labelFor` in
    /// `backend/src/main/java/com/lucilla/settlement/benchmarks/SeriesRow.java` and
    /// ascends as trust decreases: 0 = seed, 1 = attested, 2 = alternate-seats,
    /// 3 = benchmark-x-factor, 4 = carried-forward, 5 = missed. The default (1) admits
    /// only attested committee fixings; an admin may widen it (e.g. to 4 so a
    /// carried-forward print keeps the on-chain NAV alive over a holiday). Tier 0 is the
    /// unattested seed placeholder and is refused at every setting, which is why the
    /// range starts at 1. The tier is inside the signed message, so the committee
    /// vouches for it and a relay cannot relabel a print.
    pub fn set_max_tier(ctx: Context<AdminOnly>, max_tier: u8) -> Result<()> {
        require!(
            max_tier > SEED_TIER && max_tier <= MAX_TIER_CEILING,
            VaultError::MaxTierOutOfRange
        );
        let vault = &mut ctx.accounts.vault;
        vault.max_tier = max_tier;
        emit!(MaxTierSet { vault: vault.key(), max_tier });
        Ok(())
    }

    /// Updates fee rates and the fee recipient.
    ///
    /// WHY: fees are capped (10% create/redeem, 5% p.a. management) so that no admin key,
    /// compromised or not, can confiscate a basket by fee. Management fees are accrued up
    /// to now at the OLD rate first so a rate change is never retroactive.
    pub fn set_fees(
        ctx: Context<SetFees>,
        create_fee_bps: u16,
        redeem_fee_bps: u16,
        management_fee_bps_per_year: u16,
        fee_recipient: Pubkey,
    ) -> Result<()> {
        Vault::validate_fees(create_fee_bps, redeem_fee_bps, management_fee_bps_per_year)?;
        accrue_management_fee(
            &mut ctx.accounts.vault,
            &ctx.accounts.share_mint,
            &ctx.accounts.fee_share_account,
            &ctx.accounts.share_token_program,
        )?;
        let vault = &mut ctx.accounts.vault;
        vault.create_fee_bps = create_fee_bps;
        vault.redeem_fee_bps = redeem_fee_bps;
        vault.management_fee_bps_per_year = management_fee_bps_per_year;
        vault.fee_recipient = fee_recipient;
        emit!(FeesSet {
            vault: vault.key(),
            create_fee_bps,
            redeem_fee_bps,
            management_fee_bps_per_year,
            fee_recipient,
        });
        Ok(())
    }

    /// Creates the vault's holder allow-list account (empty).
    ///
    /// WHY: holder restrictions on the underlying stocks are inherited by the share, so
    /// the vault needs somewhere to record who may receive shares. It is a PDA of the
    /// vault so there is exactly one and nobody can substitute a permissive look-alike.
    pub fn init_holder_registry(ctx: Context<InitHolderRegistry>) -> Result<()> {
        let registry = &mut ctx.accounts.holder_registry;
        registry.bump = ctx.bumps.holder_registry;
        registry.vault = ctx.accounts.vault.key();
        registry.authority = ctx.accounts.authority.key();
        registry.allowed = Vec::new();
        emit!(HolderRegistryInitialized { vault: ctx.accounts.vault.key(), registry: registry.key() });
        Ok(())
    }

    /// Turns the holder allow-list on or off for this vault.
    ///
    /// WHY: a series may launch unrestricted and become restricted later (or the reverse
    /// on delisting). The registry account itself is validated as this vault's PDA.
    pub fn set_holder_registry(ctx: Context<SetHolderRegistry>, enabled: bool) -> Result<()> {
        let vault = &mut ctx.accounts.vault;
        vault.holder_registry = if enabled { Some(ctx.accounts.holder_registry.key()) } else { None };
        emit!(HolderRegistrySet { vault: vault.key(), registry: vault.holder_registry });
        Ok(())
    }

    /// Allows or disallows one wallet as a share holder.
    ///
    /// WHY: the create path refuses to mint to a receiver that is not on this list when
    /// the registry is enabled. Removal is immediate; existing balances are not seized
    /// (that is a freeze-authority decision, not a registry one).
    pub fn set_holder_allowed(ctx: Context<SetHolderAllowed>, holder: Pubkey, allowed: bool) -> Result<()> {
        let registry = &mut ctx.accounts.holder_registry;
        if allowed {
            if !registry.is_allowed(&holder) {
                require!(registry.allowed.len() < MAX_HOLDERS, VaultError::TooManyHolders);
                registry.allowed.push(holder);
            }
        } else {
            registry.allowed.retain(|k| *k != holder);
        }
        emit!(HolderAllowedSet { registry: registry.key(), holder, allowed });
        Ok(())
    }

    /// Proposes a new basket definition, to become applicable after the timelock.
    ///
    /// WHY: a basket change alters what a share is worth in kind. Holders and APs must
    /// be able to see it coming and redeem at the old composition; the timelock (>= 1
    /// day) is that window. Only one proposal may be pending so the queue is unambiguous.
    pub fn propose_basket(ctx: Context<AdminOnly>, constituents: Vec<Constituent>) -> Result<()> {
        Vault::validate_constituents(&constituents)?;
        let vault = &mut ctx.accounts.vault;
        require!(vault.pending_basket.is_none(), VaultError::BasketAlreadyPending);
        let now = Clock::get()?.unix_timestamp;
        let apply_after = now
            .checked_add(vault.rebalance_delay_secs)
            .ok_or(VaultError::MathOverflow)?;
        let set_changes = Vault::mint_set_changes(&vault.constituents, &constituents);
        vault.pending_basket = Some(PendingBasket {
            constituents: constituents.clone(),
            proposed_at: now,
            apply_after,
        });
        emit!(BasketProposed { vault: vault.key(), constituents, apply_after, set_changes });
        Ok(())
    }

    /// Withdraws the pending basket proposal.
    ///
    /// WHY: a proposal can be wrong (fat-finger weight) or overtaken by a corporate
    /// action; cancelling must not require waiting out the timelock.
    pub fn cancel_basket(ctx: Context<AdminOnly>) -> Result<()> {
        let vault = &mut ctx.accounts.vault;
        require!(vault.pending_basket.is_some(), VaultError::NoPendingBasket);
        vault.pending_basket = None;
        emit!(BasketCancelled { vault: vault.key() });
        Ok(())
    }

    /// Makes the pending basket the live one, once the timelock has elapsed.
    ///
    /// WHY paused-if-set-changes: when a mint is added or removed the vault's inventory
    /// no longer matches the definition until the custodian has rebalanced. Creating or
    /// redeeming against a mismatched inventory would either fail (missing ATA) or pay
    /// out the wrong assets, so the vault must be paused for that kind of change. A pure
    /// weight change keeps every mint and every ATA, so it is applied live.
    pub fn apply_basket(ctx: Context<AdminOnly>) -> Result<()> {
        let vault = &mut ctx.accounts.vault;
        let pending = vault.pending_basket.clone().ok_or(VaultError::NoPendingBasket)?;
        let now = Clock::get()?.unix_timestamp;
        require!(now >= pending.apply_after, VaultError::TimelockNotElapsed);
        if Vault::mint_set_changes(&vault.constituents, &pending.constituents) {
            require!(vault.paused, VaultError::RebalanceRequiresPause);
        }
        let previous = std::mem::replace(&mut vault.constituents, pending.constituents.clone());
        vault.pending_basket = None;
        emit!(BasketApplied { vault: vault.key(), previous, next: pending.constituents });
        Ok(())
    }

    /// AP deposits every constituent (ceil-rounded units for `shares`) and receives
    /// `shares` minus the create fee; the fee is minted to the fee recipient.
    ///
    /// Remaining accounts, in basket order, per constituent: [mint, ap_token_account,
    /// vault_token_account]. The receiver is the owner of `receiver_share_account`.
    ///
    /// WHY one instruction: the deposit of all constituents and the mint must be atomic,
    /// so a basket that is short one line cannot produce shares. WHY ceil: rounding must
    /// favour the vault so existing holders are never diluted by rounding dust. WHY the
    /// registry check here: this is the only place shares come into existence, so it is
    /// the choke point for "who may hold".
    pub fn create<'info>(ctx: Context<'_, '_, 'info, 'info, Create<'info>>, shares: u64) -> Result<()> {
        let vault_key = ctx.accounts.vault.key();
        let instrument_id = ctx.accounts.vault.instrument_id;
        let bump = ctx.accounts.vault.bump;
        let vault = &ctx.accounts.vault;

        require!(!vault.paused, VaultError::VaultPaused);
        require!(shares > 0, VaultError::ZeroAmount);
        require!(vault.is_ap(&ctx.accounts.ap.key()), VaultError::NotAuthorisedParticipant);

        let receiver = ctx.accounts.receiver_share_account.owner;
        enforce_holder_allowed(vault, ctx.accounts.holder_registry.as_ref(), &receiver)?;

        let fee_shares = mul_bps_floor(shares, vault.create_fee_bps)?;
        let net_shares = shares.checked_sub(fee_shares).ok_or(VaultError::MathOverflow)?;
        require!(net_shares > 0, VaultError::NetSharesZero);

        let n = vault.constituents.len();
        require!(ctx.remaining_accounts.len() == n * 3, VaultError::ConstituentAccountsMismatch);

        let mut units_in: Vec<u64> = Vec::with_capacity(n);
        for (i, c) in vault.constituents.iter().enumerate() {
            let mint_ai = &ctx.remaining_accounts[i * 3];
            let from_ai = &ctx.remaining_accounts[i * 3 + 1];
            let to_ai = &ctx.remaining_accounts[i * 3 + 2];
            require_keys_eq!(mint_ai.key(), c.mint, VaultError::ConstituentOrderMismatch);

            let mint = InterfaceAccount::<Mint>::try_from(mint_ai)?;
            let from = InterfaceAccount::<TokenAccount>::try_from(from_ai)?;
            let to = InterfaceAccount::<TokenAccount>::try_from(to_ai)?;
            require_keys_eq!(from.mint, c.mint, VaultError::ConstituentMintMismatch);
            require_keys_eq!(to.mint, c.mint, VaultError::ConstituentMintMismatch);
            require_keys_eq!(from.owner, ctx.accounts.ap.key(), VaultError::SourceNotOwnedByAp);
            require_keys_eq!(to.owner, vault_key, VaultError::DestinationNotOwnedByVault);

            let amount = units_for_create(shares, c.units_per_share)?;
            let token_program = token_program_for(
                mint_ai.owner,
                &ctx.accounts.token_program,
                &ctx.accounts.token_2022_program,
            )?;
            token_interface::transfer_checked(
                CpiContext::new(
                    token_program,
                    TransferChecked {
                        from: from_ai.clone(),
                        mint: mint_ai.clone(),
                        to: to_ai.clone(),
                        authority: ctx.accounts.ap.to_account_info(),
                    },
                ),
                amount,
                mint.decimals,
            )?;
            units_in.push(amount);
        }

        let seeds: &[&[u8]] = &[VAULT_SEED, instrument_id.as_ref(), &[bump]];
        let signer_seeds = &[seeds];
        let share_program = ctx.accounts.share_token_program.to_account_info();

        token_interface::mint_to(
            CpiContext::new_with_signer(
                share_program.clone(),
                MintTo {
                    mint: ctx.accounts.share_mint.to_account_info(),
                    to: ctx.accounts.receiver_share_account.to_account_info(),
                    authority: ctx.accounts.vault.to_account_info(),
                },
                signer_seeds,
            ),
            net_shares,
        )?;
        if fee_shares > 0 {
            token_interface::mint_to(
                CpiContext::new_with_signer(
                    share_program,
                    MintTo {
                        mint: ctx.accounts.share_mint.to_account_info(),
                        to: ctx.accounts.fee_share_account.to_account_info(),
                        authority: ctx.accounts.vault.to_account_info(),
                    },
                    signer_seeds,
                ),
                fee_shares,
            )?;
        }

        emit!(SharesCreated {
            vault: vault_key,
            ap: ctx.accounts.ap.key(),
            receiver,
            shares_requested: shares,
            shares_to_receiver: net_shares,
            fee_shares,
            units_in,
        });
        Ok(())
    }

    /// AP burns `shares` (net of the redeem fee, which is paid in shares to the fee
    /// recipient) and receives floor-rounded units of every constituent.
    ///
    /// Remaining accounts, in basket order, per constituent: [mint, vault_token_account,
    /// ap_token_account].
    ///
    /// WHY burn-then-transfer: the shares must be gone before assets leave, so a failure
    /// mid-way (the transaction reverts anyway) can never leave both shares and assets
    /// with the AP. WHY floor: rounding favours the vault, never the redeemer. WHY the
    /// pause override: `redeem_while_paused` is the emergency exit; when the admin has
    /// halted creation (e.g. an issuer halts the underlying) holders can still leave.
    pub fn redeem<'info>(ctx: Context<'_, '_, 'info, 'info, Redeem<'info>>, shares: u64) -> Result<()> {
        let vault_key = ctx.accounts.vault.key();
        let instrument_id = ctx.accounts.vault.instrument_id;
        let bump = ctx.accounts.vault.bump;
        let vault = &ctx.accounts.vault;

        require!(!vault.paused || vault.redeem_while_paused, VaultError::VaultPaused);
        require!(shares > 0, VaultError::ZeroAmount);
        require!(vault.is_ap(&ctx.accounts.ap.key()), VaultError::NotAuthorisedParticipant);

        let fee_shares = mul_bps_floor(shares, vault.redeem_fee_bps)?;
        let net_shares = shares.checked_sub(fee_shares).ok_or(VaultError::MathOverflow)?;
        require!(net_shares > 0, VaultError::NetSharesZero);

        let n = vault.constituents.len();
        require!(ctx.remaining_accounts.len() == n * 3, VaultError::ConstituentAccountsMismatch);

        let share_program = ctx.accounts.share_token_program.to_account_info();
        if fee_shares > 0 {
            token_interface::transfer_checked(
                CpiContext::new(
                    share_program.clone(),
                    TransferChecked {
                        from: ctx.accounts.ap_share_account.to_account_info(),
                        mint: ctx.accounts.share_mint.to_account_info(),
                        to: ctx.accounts.fee_share_account.to_account_info(),
                        authority: ctx.accounts.ap.to_account_info(),
                    },
                ),
                fee_shares,
                SHARE_DECIMALS,
            )?;
        }
        token_interface::burn(
            CpiContext::new(
                share_program,
                Burn {
                    mint: ctx.accounts.share_mint.to_account_info(),
                    from: ctx.accounts.ap_share_account.to_account_info(),
                    authority: ctx.accounts.ap.to_account_info(),
                },
            ),
            net_shares,
        )?;

        let seeds: &[&[u8]] = &[VAULT_SEED, instrument_id.as_ref(), &[bump]];
        let signer_seeds = &[seeds];

        let mut units_out: Vec<u64> = Vec::with_capacity(n);
        for (i, c) in vault.constituents.iter().enumerate() {
            let mint_ai = &ctx.remaining_accounts[i * 3];
            let from_ai = &ctx.remaining_accounts[i * 3 + 1];
            let to_ai = &ctx.remaining_accounts[i * 3 + 2];
            require_keys_eq!(mint_ai.key(), c.mint, VaultError::ConstituentOrderMismatch);

            let mint = InterfaceAccount::<Mint>::try_from(mint_ai)?;
            let from = InterfaceAccount::<TokenAccount>::try_from(from_ai)?;
            let to = InterfaceAccount::<TokenAccount>::try_from(to_ai)?;
            require_keys_eq!(from.mint, c.mint, VaultError::ConstituentMintMismatch);
            require_keys_eq!(to.mint, c.mint, VaultError::ConstituentMintMismatch);
            require_keys_eq!(from.owner, vault_key, VaultError::SourceNotOwnedByVault);
            require_keys_eq!(to.owner, ctx.accounts.ap.key(), VaultError::DestinationNotOwnedByAp);

            let amount = units_for_redeem(net_shares, c.units_per_share)?;
            if amount > 0 {
                let token_program = token_program_for(
                    mint_ai.owner,
                    &ctx.accounts.token_program,
                    &ctx.accounts.token_2022_program,
                )?;
                token_interface::transfer_checked(
                    CpiContext::new_with_signer(
                        token_program,
                        TransferChecked {
                            from: from_ai.clone(),
                            mint: mint_ai.clone(),
                            to: to_ai.clone(),
                            authority: ctx.accounts.vault.to_account_info(),
                        },
                        signer_seeds,
                    ),
                    amount,
                    mint.decimals,
                )?;
            }
            units_out.push(amount);
        }

        emit!(SharesRedeemed {
            vault: vault_key,
            ap: ctx.accounts.ap.key(),
            shares_burned: net_shares,
            fee_shares,
            units_out,
        });
        Ok(())
    }

    /// Mints the management fee accrued since the last accrual to the fee recipient.
    ///
    /// WHY permissionless: anyone may crank it; the amount is a pure function of supply,
    /// rate and elapsed time, so there is nothing a caller can influence. WHY in shares:
    /// the vault holds only the basket, so the fee is taken by dilution (as ETP TERs are)
    /// rather than by selling constituents.
    pub fn accrue_fees(ctx: Context<AccrueFees>) -> Result<()> {
        accrue_management_fee(
            &mut ctx.accounts.vault,
            &ctx.accounts.share_mint,
            &ctx.accounts.fee_share_account,
            &ctx.accounts.share_token_program,
        )
    }

    /// Accepts a committee fixing proven by Ed25519 instructions earlier in the same
    /// transaction (see `nav.rs`).
    ///
    /// WHY permissionless relay: the signatures are the authority, not the poster; any
    /// relay (or the committee itself) may post. WHY strictly increasing as_of_date: it
    /// makes replay of an old fixing impossible and gives a total order. WHY not in the
    /// future: a fixing dated tomorrow cannot have been struck under a rulebook that
    /// values a closed session.
    pub fn post_nav(ctx: Context<PostNav>, fixing: NavFixingArgs) -> Result<()> {
        let vault = &mut ctx.accounts.vault;
        require!(fixing.instrument_id == vault.instrument_id, VaultError::WrongInstrument);
        require!(fixing.nav_per_share > 0, VaultError::ZeroNav);
        // Tier scale per SeriesRow.labelFor: 0 = seed is never a fixing; above the
        // vault's maximum is not trusted enough for this series.
        require!(fixing.tier != SEED_TIER, VaultError::SeedTierRefused);
        require!(fixing.tier <= vault.max_tier, VaultError::TierExceedsMaximum);
        if vault.latest_nav.posted_at != 0 {
            require!(fixing.as_of_date > vault.latest_nav.as_of_date, VaultError::StaleFixing);
        }
        let now = Clock::get()?.unix_timestamp;
        let as_of_secs = (fixing.as_of_date as i64)
            .checked_mul(SECONDS_PER_DAY)
            .ok_or(VaultError::MathOverflow)?;
        require!(as_of_secs <= now + SECONDS_PER_DAY, VaultError::FixingInFuture);

        let message = canonical_nav_message(&crate::ID, &vault.key(), &fixing);
        let signers = verify_attestations(&ctx.accounts.instructions_sysvar, &message, &vault.attestors)?;
        require!(signers.len() >= vault.threshold as usize, VaultError::InsufficientAttestations);

        vault.latest_nav = NavRecord {
            instrument_id: fixing.instrument_id,
            as_of_date: fixing.as_of_date,
            session: fixing.session,
            nav_per_share: fixing.nav_per_share,
            rulebook_version: fixing.rulebook_version,
            posted_at: now,
            attestor_count: signers.len() as u8,
            fixing_ref: fixing.fixing_ref,
            tier: fixing.tier,
        };
        emit!(NavPosted {
            vault: vault.key(),
            instrument_id: fixing.instrument_id,
            as_of_date: fixing.as_of_date,
            session: fixing.session,
            nav_per_share: fixing.nav_per_share,
            rulebook_version: fixing.rulebook_version,
            fixing_ref: fixing.fixing_ref,
            tier: fixing.tier,
            attestors: signers,
            relay: ctx.accounts.relay.key(),
        });
        Ok(())
    }

    /// Pauses or unpauses creation (and redemption, unless `redeem_while_paused`).
    ///
    /// WHY: halts of the underlying, a disputed fixing, or a set-changing rebalance all
    /// need primary-market activity stopped without touching balances.
    pub fn set_pause(ctx: Context<AdminOnly>, paused: bool) -> Result<()> {
        let vault = &mut ctx.accounts.vault;
        vault.paused = paused;
        emit!(PauseSet { vault: vault.key(), paused });
        Ok(())
    }

    /// Enables the emergency exit: redemptions continue while creation is paused.
    ///
    /// WHY separate from pause: an issuer halt might block creation forever; holders must
    /// still be able to unwind into the underlying they are entitled to.
    pub fn set_redeem_while_paused(ctx: Context<AdminOnly>, redeem_while_paused: bool) -> Result<()> {
        let vault = &mut ctx.accounts.vault;
        vault.redeem_while_paused = redeem_while_paused;
        emit!(RedeemWhilePausedSet { vault: vault.key(), redeem_while_paused });
        Ok(())
    }

    /// Moves constituent units out of the vault to a destination, only while paused.
    ///
    /// WHY it exists: after a set-changing rebalance the custodian must swap old
    /// constituents for new ones and there is no on-chain venue for tokenised stocks
    /// here; the custodian withdraws, trades, and deposits (deposits need no
    /// instruction). WHY paused-only and evented: this is the one admin path that moves
    /// basket assets, so it is loud and only possible while nobody can create or redeem.
    pub fn rebalance_withdraw(ctx: Context<RebalanceWithdraw>, amount: u64) -> Result<()> {
        let vault = &ctx.accounts.vault;
        require!(vault.paused, VaultError::VaultNotPaused);
        require!(amount > 0, VaultError::ZeroAmount);
        let seeds: &[&[u8]] = &[VAULT_SEED, vault.instrument_id.as_ref(), &[vault.bump]];
        let signer_seeds = &[seeds];
        let token_program = token_program_for(
            ctx.accounts.mint.to_account_info().owner,
            &ctx.accounts.token_program,
            &ctx.accounts.token_2022_program,
        )?;
        token_interface::transfer_checked(
            CpiContext::new_with_signer(
                token_program,
                TransferChecked {
                    from: ctx.accounts.vault_token_account.to_account_info(),
                    mint: ctx.accounts.mint.to_account_info(),
                    to: ctx.accounts.destination.to_account_info(),
                    authority: vault.to_account_info(),
                },
                signer_seeds,
            ),
            amount,
            ctx.accounts.mint.decimals,
        )?;
        emit!(RebalanceWithdrawal {
            vault: vault.key(),
            mint: ctx.accounts.mint.key(),
            destination: ctx.accounts.destination.key(),
            amount,
        });
        Ok(())
    }
}

// ---------------------------------------------------------------------------------------
// Shared helpers
// ---------------------------------------------------------------------------------------

/// Picks the token program that owns `mint_owner`. Constituents may live under SPL Token
/// or Token-2022 (tokenised stocks are Token-2022), so both programs are always passed.
fn token_program_for<'info>(
    mint_owner: &Pubkey,
    token_program: &Program<'info, Token>,
    token_2022_program: &Program<'info, Token2022>,
) -> Result<AccountInfo<'info>> {
    if *mint_owner == anchor_spl::token::ID {
        Ok(token_program.to_account_info())
    } else if *mint_owner == anchor_spl::token_2022::ID {
        Ok(token_2022_program.to_account_info())
    } else {
        err!(VaultError::UnsupportedTokenProgram)
    }
}

/// When the vault has a registry enabled, the registry account must be supplied, must
/// be THE registry the vault points at, and must list the receiver.
fn enforce_holder_allowed(
    vault: &Vault,
    registry: Option<&Account<HolderRegistry>>,
    receiver: &Pubkey,
) -> Result<()> {
    match vault.holder_registry {
        None => Ok(()),
        Some(expected) => {
            let registry = registry.ok_or(VaultError::HolderRegistryMismatch)?;
            require_keys_eq!(registry.key(), expected, VaultError::HolderRegistryMismatch);
            require!(registry.is_allowed(receiver), VaultError::ReceiverNotAllowed);
            Ok(())
        }
    }
}

/// Accrues the management fee. The timestamp only advances when a fee was actually
/// minted or there was nothing to charge (zero supply/rate); otherwise sub-unit dust
/// keeps accumulating instead of being thrown away every call.
fn accrue_management_fee<'info>(
    vault: &mut Account<'info, Vault>,
    share_mint: &InterfaceAccount<'info, Mint>,
    fee_share_account: &InterfaceAccount<'info, TokenAccount>,
    share_token_program: &Interface<'info, TokenInterface>,
) -> Result<()> {
    let now = Clock::get()?.unix_timestamp;
    let from_ts = vault.last_accrual_ts;
    let elapsed = now.saturating_sub(from_ts);
    let supply_before = share_mint.supply;
    let fee_shares = management_fee_shares(supply_before, vault.management_fee_bps_per_year, elapsed)?;

    if fee_shares > 0 {
        let seeds: &[&[u8]] = &[VAULT_SEED, vault.instrument_id.as_ref(), &[vault.bump]];
        token_interface::mint_to(
            CpiContext::new_with_signer(
                share_token_program.to_account_info(),
                MintTo {
                    mint: share_mint.to_account_info(),
                    to: fee_share_account.to_account_info(),
                    authority: vault.to_account_info(),
                },
                &[seeds],
            ),
            fee_shares,
        )?;
        vault.last_accrual_ts = now;
    } else if supply_before == 0 || vault.management_fee_bps_per_year == 0 {
        vault.last_accrual_ts = now;
    }

    emit!(FeesAccrued { vault: vault.key(), from_ts, to_ts: now, supply_before, fee_shares });
    Ok(())
}

// ---------------------------------------------------------------------------------------
// Instruction arguments
// ---------------------------------------------------------------------------------------

#[derive(AnchorSerialize, AnchorDeserialize, Clone, Debug)]
pub struct InitializeVaultArgs {
    pub instrument_id: [u8; 32],
    pub constituents: Vec<Constituent>,
    pub create_fee_bps: u16,
    pub redeem_fee_bps: u16,
    pub management_fee_bps_per_year: u16,
    pub fee_recipient: Pubkey,
    pub attestors: Vec<Pubkey>,
    pub threshold: u8,
    pub rebalance_delay_secs: i64,
}

// ---------------------------------------------------------------------------------------
// Accounts
// ---------------------------------------------------------------------------------------

#[derive(Accounts)]
#[instruction(args: InitializeVaultArgs)]
pub struct InitializeVault<'info> {
    #[account(mut)]
    pub authority: Signer<'info>,
    #[account(
        init,
        payer = authority,
        space = 8 + Vault::INIT_SPACE,
        seeds = [VAULT_SEED, args.instrument_id.as_ref()],
        bump,
    )]
    pub vault: Account<'info, Vault>,
    #[account(
        init,
        payer = authority,
        seeds = [SHARE_MINT_SEED, vault.key().as_ref()],
        bump,
        mint::decimals = SHARE_DECIMALS,
        mint::authority = vault,
        mint::freeze_authority = vault,
        mint::token_program = share_token_program,
    )]
    pub share_mint: InterfaceAccount<'info, Mint>,
    pub share_token_program: Interface<'info, TokenInterface>,
    pub system_program: Program<'info, System>,
}

#[derive(Accounts)]
pub struct AdminOnly<'info> {
    pub authority: Signer<'info>,
    #[account(
        mut,
        seeds = [VAULT_SEED, vault.instrument_id.as_ref()],
        bump = vault.bump,
        has_one = authority @ VaultError::NotAuthority,
    )]
    pub vault: Account<'info, Vault>,
}

#[derive(Accounts)]
pub struct SetFees<'info> {
    pub authority: Signer<'info>,
    #[account(
        mut,
        seeds = [VAULT_SEED, vault.instrument_id.as_ref()],
        bump = vault.bump,
        has_one = authority @ VaultError::NotAuthority,
        has_one = share_mint,
    )]
    pub vault: Account<'info, Vault>,
    #[account(mut)]
    pub share_mint: InterfaceAccount<'info, Mint>,
    /// Share account of the CURRENT fee recipient (fees up to now are settled first).
    #[account(
        mut,
        constraint = fee_share_account.mint == vault.share_mint @ VaultError::ConstituentMintMismatch,
        constraint = fee_share_account.owner == vault.fee_recipient @ VaultError::WrongFeeAccount,
    )]
    pub fee_share_account: InterfaceAccount<'info, TokenAccount>,
    #[account(constraint = share_token_program.key() == *share_mint.to_account_info().owner @ VaultError::UnsupportedTokenProgram)]
    pub share_token_program: Interface<'info, TokenInterface>,
}

#[derive(Accounts)]
pub struct InitHolderRegistry<'info> {
    #[account(mut)]
    pub authority: Signer<'info>,
    #[account(
        seeds = [VAULT_SEED, vault.instrument_id.as_ref()],
        bump = vault.bump,
        has_one = authority @ VaultError::NotAuthority,
    )]
    pub vault: Account<'info, Vault>,
    #[account(
        init,
        payer = authority,
        space = 8 + HolderRegistry::INIT_SPACE,
        seeds = [REGISTRY_SEED, vault.key().as_ref()],
        bump,
    )]
    pub holder_registry: Account<'info, HolderRegistry>,
    pub system_program: Program<'info, System>,
}

#[derive(Accounts)]
pub struct SetHolderRegistry<'info> {
    pub authority: Signer<'info>,
    #[account(
        mut,
        seeds = [VAULT_SEED, vault.instrument_id.as_ref()],
        bump = vault.bump,
        has_one = authority @ VaultError::NotAuthority,
    )]
    pub vault: Account<'info, Vault>,
    #[account(
        seeds = [REGISTRY_SEED, vault.key().as_ref()],
        bump = holder_registry.bump,
        constraint = holder_registry.vault == vault.key() @ VaultError::HolderRegistryMismatch,
    )]
    pub holder_registry: Account<'info, HolderRegistry>,
}

#[derive(Accounts)]
pub struct SetHolderAllowed<'info> {
    pub authority: Signer<'info>,
    #[account(
        mut,
        seeds = [REGISTRY_SEED, holder_registry.vault.as_ref()],
        bump = holder_registry.bump,
        has_one = authority @ VaultError::NotAuthority,
    )]
    pub holder_registry: Account<'info, HolderRegistry>,
}

#[derive(Accounts)]
pub struct Create<'info> {
    #[account(
        mut,
        seeds = [VAULT_SEED, vault.instrument_id.as_ref()],
        bump = vault.bump,
        has_one = share_mint,
    )]
    pub vault: Account<'info, Vault>,
    pub ap: Signer<'info>,
    #[account(mut)]
    pub share_mint: InterfaceAccount<'info, Mint>,
    /// Shares net of fee go here; its owner is the receiver the registry is checked for.
    #[account(
        mut,
        constraint = receiver_share_account.mint == vault.share_mint @ VaultError::ConstituentMintMismatch,
    )]
    pub receiver_share_account: InterfaceAccount<'info, TokenAccount>,
    #[account(
        mut,
        constraint = fee_share_account.mint == vault.share_mint @ VaultError::ConstituentMintMismatch,
        constraint = fee_share_account.owner == vault.fee_recipient @ VaultError::WrongFeeAccount,
    )]
    pub fee_share_account: InterfaceAccount<'info, TokenAccount>,
    /// Required (and validated against `vault.holder_registry`) only when a registry is
    /// enabled; otherwise may be omitted.
    pub holder_registry: Option<Account<'info, HolderRegistry>>,
    #[account(constraint = share_token_program.key() == *share_mint.to_account_info().owner @ VaultError::UnsupportedTokenProgram)]
    pub share_token_program: Interface<'info, TokenInterface>,
    pub token_program: Program<'info, Token>,
    pub token_2022_program: Program<'info, Token2022>,
    // remaining_accounts: [mint, ap_token_account, vault_token_account] per constituent
}

#[derive(Accounts)]
pub struct Redeem<'info> {
    #[account(
        mut,
        seeds = [VAULT_SEED, vault.instrument_id.as_ref()],
        bump = vault.bump,
        has_one = share_mint,
    )]
    pub vault: Account<'info, Vault>,
    pub ap: Signer<'info>,
    #[account(mut)]
    pub share_mint: InterfaceAccount<'info, Mint>,
    #[account(
        mut,
        constraint = ap_share_account.mint == vault.share_mint @ VaultError::ConstituentMintMismatch,
        constraint = ap_share_account.owner == ap.key() @ VaultError::SourceNotOwnedByAp,
    )]
    pub ap_share_account: InterfaceAccount<'info, TokenAccount>,
    #[account(
        mut,
        constraint = fee_share_account.mint == vault.share_mint @ VaultError::ConstituentMintMismatch,
        constraint = fee_share_account.owner == vault.fee_recipient @ VaultError::WrongFeeAccount,
    )]
    pub fee_share_account: InterfaceAccount<'info, TokenAccount>,
    #[account(constraint = share_token_program.key() == *share_mint.to_account_info().owner @ VaultError::UnsupportedTokenProgram)]
    pub share_token_program: Interface<'info, TokenInterface>,
    pub token_program: Program<'info, Token>,
    pub token_2022_program: Program<'info, Token2022>,
    // remaining_accounts: [mint, vault_token_account, ap_token_account] per constituent
}

#[derive(Accounts)]
pub struct AccrueFees<'info> {
    #[account(
        mut,
        seeds = [VAULT_SEED, vault.instrument_id.as_ref()],
        bump = vault.bump,
        has_one = share_mint,
    )]
    pub vault: Account<'info, Vault>,
    #[account(mut)]
    pub share_mint: InterfaceAccount<'info, Mint>,
    #[account(
        mut,
        constraint = fee_share_account.mint == vault.share_mint @ VaultError::ConstituentMintMismatch,
        constraint = fee_share_account.owner == vault.fee_recipient @ VaultError::WrongFeeAccount,
    )]
    pub fee_share_account: InterfaceAccount<'info, TokenAccount>,
    #[account(constraint = share_token_program.key() == *share_mint.to_account_info().owner @ VaultError::UnsupportedTokenProgram)]
    pub share_token_program: Interface<'info, TokenInterface>,
}

#[derive(Accounts)]
pub struct PostNav<'info> {
    /// Whoever pays for the transaction. Not an authority: the signatures are.
    pub relay: Signer<'info>,
    #[account(
        mut,
        seeds = [VAULT_SEED, vault.instrument_id.as_ref()],
        bump = vault.bump,
    )]
    pub vault: Account<'info, Vault>,
    /// CHECK: address-checked against the instructions sysvar id; read via the sysvar API.
    #[account(address = INSTRUCTIONS_SYSVAR_ID)]
    pub instructions_sysvar: UncheckedAccount<'info>,
}

#[derive(Accounts)]
pub struct RebalanceWithdraw<'info> {
    pub authority: Signer<'info>,
    #[account(
        seeds = [VAULT_SEED, vault.instrument_id.as_ref()],
        bump = vault.bump,
        has_one = authority @ VaultError::NotAuthority,
    )]
    pub vault: Account<'info, Vault>,
    pub mint: InterfaceAccount<'info, Mint>,
    #[account(
        mut,
        constraint = vault_token_account.mint == mint.key() @ VaultError::ConstituentMintMismatch,
        constraint = vault_token_account.owner == vault.key() @ VaultError::SourceNotOwnedByVault,
    )]
    pub vault_token_account: InterfaceAccount<'info, TokenAccount>,
    #[account(
        mut,
        constraint = destination.mint == mint.key() @ VaultError::ConstituentMintMismatch,
    )]
    pub destination: InterfaceAccount<'info, TokenAccount>,
    pub token_program: Program<'info, Token>,
    pub token_2022_program: Program<'info, Token2022>,
}
