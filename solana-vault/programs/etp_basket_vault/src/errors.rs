use anchor_lang::prelude::*;

#[error_code]
pub enum VaultError {
    // ---- configuration ----
    #[msg("Basket must have at least one constituent")]
    EmptyBasket,
    #[msg("Basket exceeds the maximum number of constituents")]
    TooManyConstituents,
    #[msg("A constituent has zero units per share")]
    ZeroUnitsPerShare,
    #[msg("A mint appears more than once in the basket")]
    DuplicateConstituent,
    #[msg("Attestor set is empty")]
    NoAttestors,
    #[msg("Attestor set exceeds the maximum size")]
    TooManyAttestors,
    #[msg("Threshold must be at least 2")]
    ThresholdTooLow,
    #[msg("Threshold exceeds the number of attestors")]
    ThresholdUnreachable,
    #[msg("An attestor key appears more than once in the roster")]
    DuplicateAttestor,
    #[msg("Fee exceeds the hard cap")]
    FeeTooHigh,
    #[msg("Rebalance delay is below the one-day minimum")]
    RebalanceDelayTooShort,
    #[msg("Instrument id must not be all zero")]
    ZeroInstrumentId,
    #[msg("Too many authorised participants")]
    TooManyAuthorisedParticipants,
    #[msg("Too many allow-listed holders")]
    TooManyHolders,

    // ---- access ----
    #[msg("Signer is not the vault authority")]
    NotAuthority,
    #[msg("Signer is not an authorised participant")]
    NotAuthorisedParticipant,
    #[msg("Receiver is not allow-listed in the holder registry")]
    ReceiverNotAllowed,
    #[msg("Holder registry account is missing or does not match the vault")]
    HolderRegistryMismatch,

    // ---- lifecycle ----
    #[msg("Vault is paused")]
    VaultPaused,
    #[msg("Vault is not paused")]
    VaultNotPaused,
    #[msg("A basket change is already pending; cancel it first")]
    BasketAlreadyPending,
    #[msg("No basket change is pending")]
    NoPendingBasket,
    #[msg("The rebalance timelock has not elapsed")]
    TimelockNotElapsed,
    #[msg("Changing the constituent set requires the vault to be paused")]
    RebalanceRequiresPause,

    // ---- create / redeem ----
    #[msg("Amount must be greater than zero")]
    ZeroAmount,
    #[msg("Net shares after fee would be zero")]
    NetSharesZero,
    #[msg("Remaining accounts do not match the basket (expect mint, source, destination per constituent)")]
    ConstituentAccountsMismatch,
    #[msg("Remaining accounts are not in basket order")]
    ConstituentOrderMismatch,
    #[msg("Token account mint does not match the constituent")]
    ConstituentMintMismatch,
    #[msg("Source token account is not owned by the authorised participant")]
    SourceNotOwnedByAp,
    #[msg("Destination token account is not owned by the vault")]
    DestinationNotOwnedByVault,
    #[msg("Destination token account is not owned by the authorised participant")]
    DestinationNotOwnedByAp,
    #[msg("Source token account is not owned by the vault")]
    SourceNotOwnedByVault,
    #[msg("Mint is owned by neither SPL Token nor Token-2022")]
    UnsupportedTokenProgram,
    #[msg("Fee share account is not owned by the fee recipient")]
    WrongFeeAccount,
    #[msg("Arithmetic overflow")]
    MathOverflow,

    // ---- NAV ----
    #[msg("Fixing is for a different instrument")]
    WrongInstrument,
    #[msg("NAV per share must be greater than zero")]
    ZeroNav,
    #[msg("Fixing is not newer than the latest accepted fixing")]
    StaleFixing,
    #[msg("Fixing as-of date is in the future")]
    FixingInFuture,
    #[msg("Fixing tier 0 (seed) is an unattested placeholder and is never accepted")]
    SeedTierRefused,
    #[msg("Fixing tier is above the vault's maximum tier")]
    TierExceedsMaximum,
    #[msg("Maximum tier must be between 1 (attested) and 5 (missed)")]
    MaxTierOutOfRange,
    #[msg("Fewer distinct attestor signatures than the threshold")]
    InsufficientAttestations,
    #[msg("An attestor signed more than once")]
    DuplicateAttestation,
    #[msg("A signature was produced by a key that is not a registered attestor")]
    UnknownAttestor,
    #[msg("A signature covers bytes other than the canonical fixing message")]
    SignedMessageMismatch,
    #[msg("Ed25519 instruction data is malformed")]
    MalformedEd25519Instruction,
    #[msg("Ed25519 instruction references data outside itself")]
    Ed25519ExternalReference,
}
