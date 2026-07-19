#!/usr/bin/env bash
# ===========================================================================
# smoke-devnet.sh — fire ONE real atomic DvP settlement on the HackCanton node.
# ===========================================================================
# Prereq: the CrossDesk devnet backend is already running (./run-devnet.sh) and
# the node admin (NODERS/Kiryl) has granted your user CanActAs on the 5 parties.
#
# It drives the desk's REST API — which translates each call into a Ledger API
# command against the real participant — to:
#   1. publish two instruments   (cETH = the asset, USDC = the cash)
#   2. issue the two holdings     (Alice holds cETH, Bob holds USDC)
#   3. run an atomic trade        (Bob buys 10 cETH from Alice for 32,000 USDC)
# On success the ledger produces a SettlementReceipt — a REAL contract on the node.
#
# Usage:   ./smoke-devnet.sh            (defaults to http://localhost:8090)
#          BASE=http://localhost:8090 ./smoke-devnet.sh
# ===========================================================================
set -euo pipefail
BASE="${BASE:-http://localhost:8090}"

say()  { printf "\n\033[1;36m== %s\033[0m\n" "$1"; }
post() { curl -sS -X POST "$BASE$1" -H "Content-Type: application/json" -d "$2"; echo; }
get()  { curl -sS "$BASE$1"; echo; }

say "0) Parties the desk knows about"
get /api/parties

say "1) Publish instruments  (cETH asset, USDC cash)"
post /api/instruments '{"issuer":"Issuer","id":"cETH","kind":"CryptoWrapped","description":"Wrapped ETH on Canton (onRails)","referencePrice":3200.0}'
post /api/instruments '{"issuer":"Bank","id":"USDC","kind":"Cash","description":"Tokenised USD cash"}'

say "2) Issue holdings  (Alice: 10 cETH, Bob: 32,000 USDC)"
post /api/holdings '{"issuer":"Issuer","instrumentId":"cETH","owner":"Alice","amount":10}'
post /api/holdings '{"issuer":"Bank","instrumentId":"USDC","owner":"Bob","amount":32000}'

say "3) ATOMIC TRADE  (Bob buys 10 cETH from Alice for 32,000 USDC — propose→accept→settle)"
post /api/trade '{"buyer":"Bob","seller":"Alice","assetInstrument":"cETH","assetAmount":10,"cashInstrument":"USDC","cashAmount":32000}'

say "4) Settlement receipts now on the node (proof)"
get "/api/receipts"

printf "\n\033[1;32mDone — a SettlementReceipt above = a real contract settled on the HackCanton node.\033[0m\n"
