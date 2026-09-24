// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import {ERC20} from "@openzeppelin/contracts/token/ERC20/ERC20.sol";
import {Ownable} from "@openzeppelin/contracts/access/Ownable.sol";

/// @title MockRestrictedToken
/// @notice Simulates an issuer-restricted tokenised stock (xStocks / Robinhood
///         stock-token style): every transfer REVERTS unless the receiver is on
///         the issuer's allowlist. Minting to a non-allowed address reverts too.
/// @dev This is the failure mode the vault inherits from its underlyings: if the
///      issuer has not whitelisted the VAULT address, `create` reverts inside
///      `safeTransferFrom` with {TransferToNonAllowed}(vault), and if the issuer
///      has not whitelisted the REDEEMING receiver, `redeem` reverts the same
///      way. The vault cannot work around this — it is the issuer's control.
contract MockRestrictedToken is ERC20, Ownable {
    uint8 private immutable _decimals;

    mapping(address => bool) public allowed;

    event AllowedSet(address indexed account, bool allowed);

    error TransferToNonAllowed(address receiver);

    constructor(
        string memory name_,
        string memory symbol_,
        uint8 decimals_
    ) ERC20(name_, symbol_) Ownable(msg.sender) {
        _decimals = decimals_;
    }

    function decimals() public view override returns (uint8) {
        return _decimals;
    }

    function setAllowed(address account, bool isAllowed_) external onlyOwner {
        allowed[account] = isAllowed_;
        emit AllowedSet(account, isAllowed_);
    }

    function mint(address to, uint256 amount) external onlyOwner {
        _mint(to, amount);
    }

    /// @dev Burns (to == address(0)) are always permitted, mirroring how real
    ///      restricted tokens let the issuer redeem from any holder.
    function _update(address from, address to, uint256 value) internal override {
        if (to != address(0) && !allowed[to]) revert TransferToNonAllowed(to);
        super._update(from, to, value);
    }
}
