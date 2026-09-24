// Anchor migration hook. Devnet-only project: the real deploy procedure is in README.md
// ("Devnet deploy"). This file exists so `anchor migrate` has an entry point; it does not
// initialise a vault because initialisation needs the constituent mints and attestor keys
// of a specific series, which are not known at deploy time.
const anchor = require("@coral-xyz/anchor");

module.exports = async function (provider) {
  anchor.setProvider(provider);
};
