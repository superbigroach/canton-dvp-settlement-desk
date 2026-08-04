#!/usr/bin/env bash
# =============================================================================
# seed-fund-demo.sh — stand up the FUND demo with real, sourced numbers.
# =============================================================================
#
# Everything here goes through the desk's public REST API, so it changes NO Daml
# and therefore does NOT change the DAR. That matters: the DAR handed to the node
# operator stays byte-identical while this script is iterated on.
#
# What it builds:
#
#   1. MMF:USYC-REF — a money-market instrument modelled on Circle/Hashnote USYC,
#      THE tokenised money-market fund on Canton Network (Hashnote brought it to
#      Canton in Oct 2024 for the privacy property; Circle acquired it Jan 2025;
#      now the largest tokenised fund by AUM). It holds US Treasury reverse repo.
#
#      ⚠️  WE DO NOT HOLD USYC. It is KYC- and qualified-investor-gated, and it is
#      not on the HackCanton devnet node. This is a MODEL carrying USYC's published
#      parameters — say that out loud. What we DO hold is real BitSafe CBTC.
#
#   2. A K-of-N committee that attests the marks. There is no oracle anywhere in
#      this system: a price is official only once the threshold has SIGNED it, and
#      the signature set is what makes it provable. `GET /api/marks/live` fetches
#      candidates so a member proposes today's real number instead of one from
#      memory — the feed proposes, the committee disposes.
#
#   3. Real marks for cETH and CBTC, taken from that live feed (Coinbase spot).
#      A wrapped token is marked at its underlying's spot; any basis is a committee
#      judgement, which is exactly why the feed does not write to the ledger.
#
#   4. An in-kind fund whose NAV per share = Σ(unitsPerShare × attested mark).
#
# Usage:  ./scripts/seed-fund-demo.sh [BASE_URL]
#         BASE_URL defaults to http://localhost:8080/api
# =============================================================================
set -euo pipefail

BASE="${1:-http://localhost:8080/api}"
CASH="USDC"

# USYC's published net yield. No free public endpoint exists for it, so this is a
# DEFAULT the committee then attests — refresh it from circle.com/usyc before a demo.
USYC_RATE="0.0320"
USYC_DAYCOUNT="ACT/360"   # every USD money-market instrument is quoted ACT/360
USYC_BASE_NAV="1.00"

say()  { printf '\n\033[1m%s\033[0m\n' "$*"; }
post() { curl -sS -X POST "$BASE$1" -H 'Content-Type: application/json' -d "$2"; }

need() {
  command -v "$1" >/dev/null 2>&1 || { echo "need $1 on PATH" >&2; exit 1; }
}
need curl
need python3

json() { python3 -c "import sys,json;d=json.load(sys.stdin);print($1)"; }

# Report what actually happened. An earlier version swallowed errors into "already
# exists", which hid a real UNKNOWN_SUBMITTERS failure for a whole run — never mask a
# POST's outcome, or the seed lies about the state it left behind.
report() {
  local what="$1" body="$2"
  echo "$body" | python3 -c "
import sys,json
what = '''$what'''
raw = sys.stdin.read()
try:
    d = json.loads(raw)
except Exception:
    print('  %-16s unreadable response: %s' % (what, raw[:120])); raise SystemExit
if isinstance(d, dict) and d.get('status') and int(d.get('status', 0)) >= 400:
    msg = str(d.get('message', ''))[:150]
    if 'DUPLICATE' in msg.upper() or 'already' in msg.lower():
        print('  %-16s already present (fine)' % what)
    else:
        print('  %-16s FAILED %s: %s' % (what, d.get('status'), msg))
else:
    print('  %-16s ok' % what)
"
}

say "0 · desk reachable?"
curl -sSf "$BASE/health" >/dev/null && echo "  ok  $BASE"

# -----------------------------------------------------------------------------
say "1 · the money-market instrument (modelled on USYC)"
# -----------------------------------------------------------------------------
# kind=Cash would make it unmarkable; it is a FUND SHARE, so it carries a price.
if curl -sS "$BASE/instruments" | grep -q '"MMF:USYC-REF"'; then
  echo "  MMF:USYC-REF already on the ledger (fine)"
else
  report "MMF:USYC-REF" "$(post /instruments "$(cat <<JSON
{"issuer":"Issuer","id":"MMF:USYC-REF","kind":"MoneyMarket",
 "description":"Money-market fund share modelled on Circle/Hashnote USYC (US Treasury reverse repo, ACT/360). MODEL ONLY - not a holding of the fund.",
 "referencePrice":$USYC_BASE_NAV}
JSON
)")"
fi

# -----------------------------------------------------------------------------
say "2 · live candidate marks (feed proposes, committee disposes)"
# -----------------------------------------------------------------------------
MARKS="$(curl -sS "$BASE/marks/live")"
echo "$MARKS" | python3 -c "
import sys,json
d=json.load(sys.stdin)
if not d:
    print('  (feed unreachable - marks will need typing in)')
for m in d:
    print('  %-6s %-8s %12s   %s' % (m['instrumentId'], m['symbol'], m['price'], m['source']))
"
ceth_px="$(echo "$MARKS" | python3 -c "
import sys,json
d={m['instrumentId']:m['price'] for m in json.load(sys.stdin)}
print(d.get('cETH',''))")"
cbtc_px="$(echo "$MARKS" | python3 -c "
import sys,json
d={m['instrumentId']:m['price'] for m in json.load(sys.stdin)}
print(d.get('CBTC',''))")"

# -----------------------------------------------------------------------------
say "3 · the NAV committee (2-of-3)"
# -----------------------------------------------------------------------------
COMMITTEE="$(post /committee '{"admin":"Bank","members":["Bank","Issuer","Auditor"],"threshold":2,"label":"NAV Committee"}')"
CC="$(echo "$COMMITTEE" | json "d.get('contractId','')")"
echo "  committee $CC  (2 of 3 must sign)"

# attest_snapshot <instrumentId> <price> <rationale>
attest_snapshot() {
  local inst="$1" px="$2" why="$3"
  [ -z "$px" ] && { echo "  skip $inst (no live mark)"; return 0; }
  local prop cid cid2 fin
  prop="$(post "/committee/$CC/propose" "{\"proposer\":\"Bank\",\"instrumentId\":\"$inst\",\"cashInstrument\":\"$CASH\",\"session\":\"Close\",\"price\":$px,\"rationale\":\"$why\"}")"
  cid="$(echo "$prop" | json "d.get('contractId','')")"
  # CONFIRM IS CONSUMING — each attestation ADDS a member to the signatory set, which
  # means a new contract. The threshold is provable precisely because the signatures
  # accumulate on-ledger rather than being counted in a field, so the cid MUST be
  # threaded forward; finalising the pre-confirm cid fails with CONTRACT_NOT_FOUND.
  cid2="$(post "/fixing/$cid/confirm" '{"member":"Issuer"}' | json "d.get('contractId','')")"
  fin="$(post "/fixing/${cid2:-$cid}/finalize" '{"proposer":"Bank","publishTo":["Bank"]}')"
  echo "$fin" | python3 -c "
import sys,json;d=json.load(sys.stdin)
print('  %-14s attested %s  | mark republished: %s' % ('$inst', d.get('attestedPrice'), d.get('markUpdated')))
print('     note:', d.get('note'))"
}

say "4 · attest the crypto marks at today's real spot"
attest_snapshot cETH "$ceth_px" "Coinbase ETH-USD spot; wrapped token marked at underlying"
attest_snapshot CBTC "$cbtc_px" "Coinbase BTC-USD spot; wrapped token marked at underlying"

# -----------------------------------------------------------------------------
say "5 · attest the money-market fund as an ACCRUING fix"
# -----------------------------------------------------------------------------
# THIS is the one that never goes stale: the committee attests base + rate +
# day-count, and the ledger derives the value at every instant in between. An MMF
# has no live price to stream — it has a NAV struck once and accrued. A websocket
# would be the wrong architecture here, which is the point worth making out loud.
PROP="$(post "/committee/$CC/propose-accruing" "$(cat <<JSON
{"proposer":"Bank","instrumentId":"MMF:USYC-REF","cashInstrument":"$CASH","session":"Close",
 "price":$USYC_BASE_NAV,"ratePerAnnum":$USYC_RATE,"dayCount":"$USYC_DAYCOUNT",
 "rationale":"Modelled on Circle/Hashnote USYC published net yield (US Treasury reverse repo, ACT/360). Model, not a holding."}
JSON
)")"
PC="$(echo "$PROP" | json "d.get('contractId','')")"
# Same accumulating-multisig rule as above: confirm consumes, so thread the new cid.
PC2="$(post "/fixing/$PC/confirm" '{"member":"Issuer"}' | json "d.get('contractId','')")"
post "/fixing/${PC2:-$PC}/finalize" '{"proposer":"Bank","publishTo":["Bank"]}' | python3 -c "
import sys,json;d=json.load(sys.stdin)
print('  MMF:USYC-REF  base %s @ $USYC_RATE/yr $USYC_DAYCOUNT | mark republished: %s' % (d.get('attestedPrice'), d.get('markUpdated')))"

# -----------------------------------------------------------------------------
say "6 · the fund — an in-kind basket over the attested marks"
# -----------------------------------------------------------------------------
if curl -sS "$BASE/baskets?party=Auditor" | grep -q '"LX1"'; then
  echo "  LX1 already defined (fine)"
else
  report "LX1" "$(post /basket "$(cat <<'JSON'
{"administrator":"Bank","basketId":"LX1","description":"Tokenised multi-asset fund: money-market core plus wrapped crypto",
 "cashInstrument":"USDC",
 "components":[{"instrumentId":"MMF:USYC-REF","unitsPerShare":100},
               {"instrumentId":"cETH","unitsPerShare":0.05},
               {"instrumentId":"CBTC","unitsPerShare":0.002}],
 "participants":["Alice","Bob"]}
JSON
)")"
fi

say "7 · NAV per share, from the attested marks"
curl -sS "$BASE/basket/nav?basketId=LX1&party=Auditor" 2>/dev/null | python3 -c "
import sys,json
try: d=json.load(sys.stdin)
except Exception: print('  (create the basket in the desk UI, then re-run)'); raise SystemExit
for l in d.get('legs',[]):
    print('  %-14s %10s x %-12s = %s' % (l.get('instrumentId'), l.get('unitsPerShare'), l.get('price'), l.get('value')))
print('  NAV/share:', d.get('navPerShare'))
" || true

cat <<'EOF'

------------------------------------------------------------------------------
Say this out loud, in this order:

  "The committee attests the mark - there is no oracle. The feed only proposes;
   two of three sign, and the signatures are on the ledger."

  "cETH and CBTC are marked at today's real spot. The money-market leg is
   modelled on USYC - the tokenised MMF that actually lives on Canton - using
   its published rate. We don't hold USYC; it's KYC-gated. We DO hold real
   BitSafe cBTC."

  "The fund's NAV is not a number someone types. It's the sum of attested marks,
   and the money-market leg accrues continuously between fixings."
------------------------------------------------------------------------------
EOF
