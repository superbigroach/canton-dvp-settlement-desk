#!/usr/bin/env bash
# =============================================================================
# bootstrap-devnet.sh — create the base layer on a ledger where Test:initialize
# cannot be run.
# =============================================================================
#
# WHY THIS EXISTS. `Test:initialize` allocates parties and then creates the
# instruments and holdings. On the shared devnet node the parties ALREADY exist
# (allocated by the operator in July), so the script aborts on its first step —
# "Party already exists" — and never reaches the part that matters.
#
# Worse, a new package id starts with an EMPTY ledger from the app's point of
# view: contracts created under the old package are invisible to a backend bound
# to the new one. So after any package rename the desk comes up with no
# instruments, no balances, and a party picker that selects nothing.
#
# This does the base layer over REST instead, which needs only the actAs rights
# already granted. Run it once, then run seed-fund-demo.sh on top.
#
# Usage:  ./scripts/bootstrap-devnet.sh [BASE_URL]
#         defaults to the hosted devnet desk
# =============================================================================
set -euo pipefail

BASE="${1:-https://crossdesk-devnet-app.web.app/api}"

say()  { printf '\n\033[1m%s\033[0m\n' "$*"; }
post() { curl -sS -X POST "$BASE$1" -H 'Content-Type: application/json' -d "$2"; }

report() {
  local what="$1" body="$2"
  echo "$body" | python3 -c "
import sys,json
what='''$what'''
raw=sys.stdin.read()
try: d=json.loads(raw)
except Exception: print('  %-22s unreadable: %s' % (what, raw[:100])); raise SystemExit
if isinstance(d,dict) and d.get('status') and int(d.get('status',0))>=400:
    print('  %-22s FAILED %s: %s' % (what, d.get('status'), str(d.get('message',''))[:110]))
else:
    print('  %-22s ok' % what)
"
}

say "0 · desk reachable?"
curl -sSf "$BASE/health" >/dev/null && echo "  ok  $BASE"

# -----------------------------------------------------------------------------
say "1 · instruments"
# -----------------------------------------------------------------------------
# Mirrors what Test:initialize creates, so the desk behaves identically to a local
# sandbox. cETH/CBTC prices here are placeholders — step 3 of seed-fund-demo.sh
# replaces them with an attested mark at live spot.
existing="$(curl -sS "$BASE/instruments")"
mk() {  # id kind description referencePrice(or null)
  if echo "$existing" | grep -q "\"$1\""; then
    echo "  $1 already present"
  else
    report "$1" "$(post /instruments "{\"issuer\":\"Issuer\",\"id\":\"$1\",\"kind\":\"$2\",\"description\":\"$3\",\"referencePrice\":$4}")"
  fi
}
mk "USDC"          "Cash"          "Tokenised USD cash (USDC)"                                    null
mk "DEMO:AAPL"     "Equity"        "Demo tokenised equity (AAPL-like)"                            255
mk "cETH"          "CryptoWrapped" "Wrapped ETH on Canton (onRails cETH)"                         2400
mk "CBTC"          "CryptoWrapped" "Wrapped BTC on Canton (BitSafe CBTC)"                         64000
mk "MMF:USYC-REF"  "MoneyMarket"   "Money-market fund share modelled on Circle/Hashnote USYC (US Treasury reverse repo, ACT/360). MODEL ONLY - not a holding of the fund." 1.00

# -----------------------------------------------------------------------------
say "2 · opening balances"
# -----------------------------------------------------------------------------
# Sized so every demo path has room: enough cash to fund bids at the collar's top,
# enough asset to sell into an auction, and enough money-market units that a fund
# creation of 10 shares (1,000 units) does not exhaust anyone.
give() {  # owner instrument amount
  report "$3 $2 -> $1" "$(post /holdings "{\"issuer\":\"Issuer\",\"instrumentId\":\"$2\",\"owner\":\"$1\",\"amount\":$3}")"
}
give Alice USDC          152550
give Alice cETH               5
give Alice CBTC               1
give Alice "MMF:USYC-REF" 100000

give Bob   USDC           50000
give Bob   cETH               4
give Bob   CBTC               2
give Bob   "DEMO:AAPL"       10
give Bob   "MMF:USYC-REF" 100000

give Bank  USDC         2000000
give Bank  cETH             100
give Bank  CBTC              20
give Bank  "DEMO:AAPL"      500

say "3 · what the desk now holds"
for P in Alice Bob Bank; do
  curl -sS "$BASE/holdings?party=$P" | python3 -c "
import sys,json
d=json.load(sys.stdin); agg={}
for h in d: agg[h['instrumentId']]=agg.get(h['instrumentId'],0)+float(h['amount'])
print('  %-6s %s' % ('$P', {k: round(v,4) for k,v in sorted(agg.items())}))
"
done

cat <<'EOF'

Base layer done. Now run the fund demo on top:

    ./scripts/seed-fund-demo.sh https://crossdesk-devnet-app.web.app/api

EOF
