//! Committee fixing verification.
//!
//! The Canton fixing committee signs a NAV off-chain (K-of-N). A relay submits it here
//! in one transaction that contains, BEFORE `post_nav`, one Ed25519 native-program
//! instruction per attestor. The native program verifies each signature (the whole
//! transaction fails if any is invalid); `post_nav` then reads those instructions back
//! through the instructions sysvar and checks that
//!   * every signature covers exactly the canonical message for the fixing being posted,
//!   * every signing key is a registered attestor,
//!   * no attestor is counted twice,
//!   * the distinct count reaches the threshold.
//!
//! This is the standard Solana pattern for ed25519 verification (there is no ed25519
//! syscall usable from BPF; the precompile plus sysvar introspection is the route).

use anchor_lang::prelude::*;
use anchor_lang::solana_program::ed25519_program;
use anchor_lang::solana_program::sysvar::instructions::{
    load_current_index_checked, load_instruction_at_checked,
};

use crate::errors::VaultError;

/// Domain separator. Changing the message layout means changing this tag, so a
/// signature over a V1 message can never be replayed against a V2 layout.
pub const NAV_MESSAGE_DOMAIN: &[u8] = b"ETPFOUNDRY_NAV_FIXING_V1";

/// Total canonical message length: domain(24) + program(32) + vault(32) + instrument(32)
/// + as_of_date(4) + session(1) + nav(8) + rulebook(32) + fixing_ref(32) + tier(1).
pub const NAV_MESSAGE_LEN: usize = 24 + 32 + 32 + 32 + 4 + 1 + 8 + 32 + 32 + 1;

/// The fixing as the relay submits it. Field order and widths are the wire format the
/// committee signs, see `canonical_nav_message`.
#[derive(AnchorSerialize, AnchorDeserialize, Clone, Copy, Debug, PartialEq, Eq)]
pub struct NavFixingArgs {
    pub instrument_id: [u8; 32],
    pub as_of_date: u32,
    pub session: u8,
    pub nav_per_share: u64,
    pub rulebook_version: [u8; 32],
    /// sha256 of the Canton `NavFixing` contract id string this posting projects. It is
    /// under the committee's signature, so the on-chain record points at exactly one
    /// ledger attestation and an auditor can walk from Solana back to Canton.
    pub fixing_ref: [u8; 32],
    /// The desk's fixing tier on the `SeriesRow.labelFor` scale (0 = seed, 1 = attested,
    /// 2 = alternate-seats, 3 = benchmark-x-factor, 4 = carried-forward, 5 = missed).
    /// Signed, so a relay cannot relabel a carried-forward print as attested.
    pub tier: u8,
}

/// The exact bytes an attestor signs. It binds the fixing to this program AND this
/// vault, so a signature collected for one series (or a look-alike deployment) cannot
/// be posted to another. Integers are little-endian to match Borsh/Solana convention.
pub fn canonical_nav_message(program_id: &Pubkey, vault: &Pubkey, f: &NavFixingArgs) -> Vec<u8> {
    let mut m = Vec::with_capacity(NAV_MESSAGE_LEN);
    m.extend_from_slice(NAV_MESSAGE_DOMAIN);
    m.extend_from_slice(program_id.as_ref());
    m.extend_from_slice(vault.as_ref());
    m.extend_from_slice(&f.instrument_id);
    m.extend_from_slice(&f.as_of_date.to_le_bytes());
    m.push(f.session);
    m.extend_from_slice(&f.nav_per_share.to_le_bytes());
    m.extend_from_slice(&f.rulebook_version);
    m.extend_from_slice(&f.fixing_ref);
    m.push(f.tier);
    debug_assert_eq!(m.len(), NAV_MESSAGE_LEN);
    m
}

// Layout of an Ed25519 native-program instruction (agave `ed25519_instruction.rs`):
//   [0]      num_signatures: u8
//   [1]      padding: u8
//   [2..]    num_signatures * Ed25519SignatureOffsets (14 bytes each)
//   [...]    pubkeys / signatures / messages at the offsets given
// Every offset comes with an instruction index; 0xFFFF means "this instruction".
const OFFSETS_START: usize = 2;
const OFFSETS_LEN: usize = 14;
const PUBKEY_LEN: usize = 32;
const SIGNATURE_LEN: usize = 64;
const SELF_INDEX: u16 = u16::MAX;

/// Walks every instruction that precedes the current one, collects the (attestor,
/// message) pairs the Ed25519 program verified, and returns the distinct attestor set
/// after enforcing the rules in the module docs. Any Ed25519 instruction that is not a
/// well-formed attestation of `expected_message` by a registered attestor fails the
/// whole call: a relay must not be able to pad a transaction with junk signatures.
pub fn verify_attestations(
    instructions_sysvar: &AccountInfo,
    expected_message: &[u8],
    attestors: &[Pubkey],
) -> Result<Vec<Pubkey>> {
    let current = load_current_index_checked(instructions_sysvar)? as usize;
    let mut signers: Vec<Pubkey> = Vec::new();

    for index in 0..current {
        let ix = load_instruction_at_checked(index, instructions_sysvar)?;
        if ix.program_id != ed25519_program::ID {
            continue;
        }
        let data = ix.data.as_slice();
        require!(data.len() >= OFFSETS_START, VaultError::MalformedEd25519Instruction);
        let count = data[0] as usize;
        for k in 0..count {
            let o = OFFSETS_START + k * OFFSETS_LEN;
            require!(data.len() >= o + OFFSETS_LEN, VaultError::MalformedEd25519Instruction);
            let sig_offset = u16_at(data, o) as usize;
            let sig_ix = u16_at(data, o + 2);
            let pk_offset = u16_at(data, o + 4) as usize;
            let pk_ix = u16_at(data, o + 6);
            let msg_offset = u16_at(data, o + 8) as usize;
            let msg_size = u16_at(data, o + 10) as usize;
            let msg_ix = u16_at(data, o + 12);

            // The bytes we read MUST be the bytes the precompile verified. If an offset
            // pointed into another instruction we would be reading something else.
            let this = index as u16;
            require!(
                is_self(sig_ix, this) && is_self(pk_ix, this) && is_self(msg_ix, this),
                VaultError::Ed25519ExternalReference
            );
            require!(
                data.len() >= sig_offset + SIGNATURE_LEN
                    && data.len() >= pk_offset + PUBKEY_LEN
                    && data.len() >= msg_offset + msg_size,
                VaultError::MalformedEd25519Instruction
            );

            let message = &data[msg_offset..msg_offset + msg_size];
            require!(message == expected_message, VaultError::SignedMessageMismatch);

            let pubkey = Pubkey::try_from(&data[pk_offset..pk_offset + PUBKEY_LEN])
                .map_err(|_| error!(VaultError::MalformedEd25519Instruction))?;
            require!(attestors.iter().any(|a| *a == pubkey), VaultError::UnknownAttestor);
            require!(!signers.contains(&pubkey), VaultError::DuplicateAttestation);
            signers.push(pubkey);
        }
    }
    Ok(signers)
}

#[inline]
fn u16_at(data: &[u8], at: usize) -> u16 {
    u16::from_le_bytes([data[at], data[at + 1]])
}

#[inline]
fn is_self(ix_index: u16, this: u16) -> bool {
    ix_index == SELF_INDEX || ix_index == this
}
