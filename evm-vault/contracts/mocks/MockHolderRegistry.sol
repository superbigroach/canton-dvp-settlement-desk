// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import {IHolderRegistry} from "../interfaces/IHolderRegistry.sol";

/// @title MockHolderRegistry
/// @notice Test double for {IHolderRegistry}: a plain allowlist with an
///         "allow everyone" switch. Not for production — a real registry is
///         maintained by the licensed issuer's compliance function.
contract MockHolderRegistry is IHolderRegistry {
    mapping(address => bool) public allowed;
    bool public allowAll;

    event AllowedSet(address indexed account, bool allowed);
    event AllowAllSet(bool allowAll);

    function setAllowed(address account, bool isAllowed_) external {
        allowed[account] = isAllowed_;
        emit AllowedSet(account, isAllowed_);
    }

    function setAllowAll(bool allowAll_) external {
        allowAll = allowAll_;
        emit AllowAllSet(allowAll_);
    }

    /// @inheritdoc IHolderRegistry
    function isAllowed(address account) external view override returns (bool) {
        return allowAll || allowed[account];
    }
}
