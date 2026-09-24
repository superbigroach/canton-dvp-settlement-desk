// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

/// @title IHolderRegistry
/// @notice Minimal compliance hook consumed by {EtpBasketVault}.
/// @dev The vault asks the registry once per mint/transfer whether the RECEIVER
///      may hold basket shares. The registry is where the licensed issuer keeps
///      the outcome of its KYC / jurisdiction / accreditation checks; the vault
///      deliberately knows nothing about why an address is or is not allowed.
///      Burns are never gated, so a holder can always exit via an AP.
interface IHolderRegistry {
    /// @notice Whether `account` may receive basket shares.
    /// @dev MUST be a pure read: the vault calls it from inside `_update` and
    ///      any state change or reentrancy here would be a bug in the registry.
    function isAllowed(address account) external view returns (bool);
}
