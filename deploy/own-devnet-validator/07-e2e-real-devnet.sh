#!/usr/bin/env bash
# =============================================================================
# 07-e2e-real-devnet.sh — real-transaction end-to-end tests against a live participant.
#
#   CD_PROFILE=localnet   ./07-e2e-real-devnet.sh     # Track A: Splice LocalNet on this laptop
#   CD_PROFILE=own-devnet ./07-e2e-real-devnet.sh     # Track B: our DevNet validator (tunnel open)
#
# Drives the DESK's REST API (the product path: REST → LedgerService → gRPC Ledger API v2
# with the self-minted HMAC token) and checks the results against the participant's own
# JSON Ledger API. Every committed step prints the ledger UPDATE ID it produced, resolved
# from the returned contract id through /v2/events/events-by-contract-id →
# /v2/updates/update-by-offset, so the evidence does not depend on backend logs.
#
# Prerequisites: 05 (DAR, parties, rights), 06 (desk running / wired), and the base layer:
#   ../../scripts/bootstrap-devnet.sh "$CD_BACKEND_URL/api"
#
# Exit code = number of FAILED tests. SKIP is not a failure (e.g. no CBTC offer pending).
# E2E_TEST_PLAN.md documents each test's exact call and expected result.
# =============================================================================
# shellcheck source=lib/common.sh
. "$(dirname "$0")/lib/common.sh"
cd_load_profile

B="$CD_BACKEND_URL/api"
ROSTER="$(cat "$CD_HERE/.roster-$CD_PROFILE" 2>/dev/null)" || cd_die "run 05 first"
party() { printf '%s' "$ROSTER" | tr ',' '\n' | grep "^$1=" | cut -d= -f2; }
RUN="e2e$(date +%s)"

PASS=0; FAIL=0; SKIP=0; RESULTS=()
t_begin() { CUR="$1"; printf '\n\033[1;36m[%s] %s\033[0m\n' "$1" "$2"; }
t_pass()  { PASS=$((PASS+1)); RESULTS+=("PASS  $CUR  $*"); printf '  \033[32mPASS\033[0m %s\n' "$*"; }
t_fail()  { FAIL=$((FAIL+1)); RESULTS+=("FAIL  $CUR  $*"); printf '  \033[31mFAIL\033[0m %s\n' "$*"; }
t_skip()  { SKIP=$((SKIP+1)); RESULTS+=("SKIP  $CUR  $*"); printf '  \033[33mSKIP\033[0m %s\n' "$*"; }

# post <path> <json> → body + "\nHTTP <code>"
post() { curl -sS -m 180 -X POST "$B$1" -H 'Content-Type: application/json' -d "$2" -w '\nHTTP %{http_code}'; }
get()  { curl -sS -m 120 "$B$1"; }
field() { cd_body | cd_json "$1"; }

# update id that created <cid>, as seen by <full party id>
update_of() {
  local cid="$1" p="$2" tok off
  tok="$(cd_mint "$CD_BACKEND_USER")"
  off="$(curl -sS -m 60 -X POST -H "Authorization: Bearer $tok" -H 'Content-Type: application/json' \
      "$CD_JSON_API/v2/events/events-by-contract-id" \
      -d "{\"contractId\":\"$cid\",\"eventFormat\":{\"filtersByParty\":{\"$p\":{\"cumulative\":[]}},\"verbose\":false}}" \
      | cd_json "d['created']['createdEvent']['offset']" 2>/dev/null)" || { echo "?"; return; }
  curl -sS -m 60 -X POST -H "Authorization: Bearer $tok" -H 'Content-Type: application/json' \
      "$CD_JSON_API/v2/updates/update-by-offset" \
      -d "{\"offset\":$off,\"updateFormat\":{\"includeTransactions\":{\"eventFormat\":{\"filtersByParty\":{\"$p\":{\"cumulative\":[]}},\"verbose\":false},\"transactionShape\":\"TRANSACTION_SHAPE_ACS_DELTA\"}}}" \
      | cd_json "d['update']['Transaction']['value']['updateId']" 2>/dev/null || echo "?"
}

# per-instrument totals over every holding visible to any CrossDesk party (deduped by cid)
supply() {
  local all="" lbl
  for lbl in Issuer Bank Alice Bob Auditor Venue Agent; do
    all="$all$(get "/holdings?party=$lbl")"$'\n'
  done
  printf '%s' "$all" | "$CD_PY" -c "
import sys, json
seen, tot = set(), {}
for line in sys.stdin.read().splitlines():
    line = line.strip()
    if not line: continue
    for h in json.loads(line):
        if h['contractId'] in seen: continue
        seen.add(h['contractId'])
        tot[h['instrumentId']] = round(tot.get(h['instrumentId'], 0) + float(h['amount']), 10)
print(json.dumps(tot, sort_keys=True))"
}
bal() {  # bal <label> <instrument> → amount owned by that party
  get "/holdings?party=$1" | "$CD_PY" -c "
import sys, json
print(round(sum(float(h['amount']) for h in json.load(sys.stdin) if h['instrumentId']=='$2' and h['owner'].startswith('$(party "$1" | cut -d: -f1)')), 10))"
}

cd_say "E2E against $CD_PROFILE — desk $B — run $RUN"
SUPPLY_BEFORE="$(supply)"
echo "  supply before: $SUPPLY_BEFORE"

# ----------------------------------------------------------------------------- T1
t_begin T1 "party read — roster, full ids, and the backend user's rights on the participant"
n="$(get /parties | cd_json "sum(1 for p in d if '::' in p['party'])")"
rights="$(cd_ledger GET "/v2/users/$CD_BACKEND_USER/rights" | cd_body | cd_json "len(d['rights'])")"
diag="$(curl -sS -m 60 "$CD_BACKEND_URL/api/diag" | cd_json "d.get('ledger',{}).get('reachable')")"
if [ "$n" -ge 8 ] && [ "$rights" -ge 16 ] && [ "$diag" = "True" ]; then
  t_pass "$n parties with full ids; $CD_BACKEND_USER holds $rights rights; /api/diag reachable"
else
  t_fail "parties=$n rights=$rights diag.reachable=$diag"
fi

# ----------------------------------------------------------------------------- T2
t_begin T2 "Canton Coin balance — validator wallet tap (DevNet: the faucet) and balance"
b0="$(cd_validator GET /api/validator/v0/wallet/balance | cd_body | cd_json "float(d['effective_unlocked_qty'])")" || b0=""
tap="$(cd_validator POST /api/validator/v0/wallet/tap "{\"amount\":\"100.0\",\"command_id\":\"$RUN-tap\"}")"
tapcid="$(echo "$tap" | cd_body | cd_json "d.get('contract_id','')" 2>/dev/null || true)"
b1="$(cd_validator GET /api/validator/v0/wallet/balance | cd_body | cd_json "float(d['effective_unlocked_qty'])")" || b1=""
if [ -n "$tapcid" ] && "$CD_PY" -c "import sys; sys.exit(0 if float('$b1') > float('${b0:-0}') else 1)"; then
  t_pass "CC unlocked $b0 → $b1; tap Amulet cid ${tapcid:0:24}… update $(update_of "$tapcid" "$(party Wallet)")"
else
  t_fail "tap=$(echo "$tap" | head -c 200) balance $b0 → $b1"
fi

# ----------------------------------------------------------------------------- T3
t_begin T3 "CIP-56 claim — accept a Token Standard TransferInstruction with the registry's choice context"
alice="$(party Alice)"
if [ "$CD_PROFILE" = "localnet" ]; then
  # STAND-IN for BitSafe CBTC: Amulet, whose registry is Scan (admin = DSO). Same interface
  # (TransferInstruction_Accept), same off-ledger choice-context + disclosed-contracts step.
  exp=$(( ($(date +%s) + 3600) * 1000000 ))
  mk="$(cd_validator POST /api/validator/v0/wallet/token-standard/transfers \
    "{\"receiver_party_id\":\"$alice\",\"amount\":\"25.0\",\"description\":\"CrossDesk E2E $RUN (LocalNet stand-in for CBTC)\",\"expires_at\":$exp,\"tracking_id\":\"$RUN-ts\"}")"
  [ "$(echo "$mk" | cd_code)" = "200" ] || t_fail "wallet transfer create: $(echo "$mk" | cd_body | head -c 300)"
  sleep 3
fi
pend="$(get "/token-standard/pending?party=Alice")"
cid="$(echo "$pend" | cd_json "next((p['instructionCid'] for p in d['pending'] if p['canAct'] and not p['expired']), '')")"
admin="$(echo "$pend" | cd_json "next((p['instrumentAdmin'] for p in d['pending'] if p['canAct'] and not p['expired']), '')")"
if [ -z "$cid" ]; then
  if [ "$CD_PROFILE" = "own-devnet" ]; then
    t_skip "no pending CBTC offer for Alice — request one at https://cbtc-faucet.bitsafe.finance to $alice, then re-run"
  else
    t_fail "no pending TransferInstruction for Alice after the wallet transfer: $(echo "$pend" | head -c 300)"
  fi
else
  acc="$(post /token-standard/accept "{\"party\":\"Alice\",\"instructionCid\":\"$cid\"}")"
  if [ "$(echo "$acc" | cd_code)" = "200" ]; then
    tok="$(cd_mint "$CD_BACKEND_USER")"
    held="$(curl -sS -m 60 -X POST -H "Authorization: Bearer $tok" -H 'Content-Type: application/json' "$CD_JSON_API/v2/state/active-contracts" \
      -d "{\"eventFormat\":{\"filtersByParty\":{\"$alice\":{\"cumulative\":[{\"identifierFilter\":{\"InterfaceFilter\":{\"value\":{\"interfaceId\":\"#splice-api-token-holding-v1:Splice.Api.Token.HoldingV1:Holding\",\"includeInterfaceView\":true,\"includeCreatedEventBlob\":false}}}}]}},\"verbose\":false},\"activeAtOffset\":$(curl -sS -H "Authorization: Bearer $tok" "$CD_JSON_API/v2/state/ledger-end" | cd_json "d['offset']")}" \
      | "$CD_PY" -c "
import sys, json
tot = {}
for row in json.load(sys.stdin):
    ev = row.get('contractEntry', {}).get('JsActiveContract', {}).get('createdEvent')
    if not ev: continue
    for iv in ev.get('interfaceViews', []):
        v = iv.get('viewValue') or {}
        if v.get('owner') == '$alice':
            k = v['instrumentId']['admin'].split('::')[0] + ':' + v['instrumentId']['id']
            tot[k] = round(tot.get(k, 0) + float(v['amount']), 10)
print(json.dumps(tot))")"
    t_pass "accepted ${cid:0:24}… (registrar ${admin%%::*}); Alice HoldingV1 by interface: $held; body: $(echo "$acc" | cd_body | head -c 160)"
  else
    t_fail "accept: $(echo "$acc" | cd_body | head -c 400)"
  fi
fi

# ----------------------------------------------------------------------------- T4
t_begin T4 "sealed auction round — orders dark to the auditor and to other traders; fills print"
o1="$(post /moc/order '{"trader":"Bob","side":"Sell","quantity":1,"instrumentId":"cETH","orderType":"Limit","limitPrice":2350}')"
o2="$(post /moc/order '{"trader":"Alice","side":"Buy","quantity":1,"instrumentId":"cETH","orderType":"Limit","limitPrice":2450}')"
sa="$(get "/moc/state?instrumentId=cETH&actingAs=Auditor")"
sb="$(get "/moc/state?instrumentId=cETH&actingAs=Alice")"
sv="$(get "/moc/state?instrumentId=cETH&actingAs=Venue")"
aud_orders="$(echo "$sa" | cd_json "len(d['orders'])")"; aud_hidden="$(echo "$sa" | cd_json "d['othersResting']")"
alice_orders="$(echo "$sb" | cd_json "sorted({o['trader'] for o in d['orders']})")"
venue_orders="$(echo "$sv" | cd_json "len(d['orders'])")"
auction="$(echo "$sv" | cd_json "d['auctionCid']")"
cl="$(post "/moc/$auction/close" '{}')"
fills="$(echo "$cl" | field "len(d.get('fills',[]))" 2>/dev/null || echo 0)"
batch="$(echo "$cl" | field "d.get('settlementBatchCid','')" 2>/dev/null || true)"
px="$(echo "$cl" | field "d.get('closingPrice')" 2>/dev/null || true)"
if [ "$aud_orders" = "0" ] && [ "$aud_hidden" -ge 2 ] && [ "$alice_orders" = "['alice-crossdesk']" ] \
   && [ "$venue_orders" -ge 2 ] && [ "$fills" -ge 2 ] && [ -n "$batch" ]; then
  t_pass "auditor sees 0 orders ($aud_hidden sealed), Alice sees only her own, venue sees $venue_orders; close @ $px, $fills fills; batch update $(update_of "$batch" "$(party Venue)")"
else
  t_fail "auditor=$aud_orders/$aud_hidden alice=$alice_orders venue=$venue_orders fills=$fills close=$(echo "$cl" | head -c 300)"
fi

# ----------------------------------------------------------------------------- T5
t_begin T5 "K-of-N committee NAV — 2-of-3 attest, finalize; finalize below quorum must fail"
cm="$(post /committee '{"admin":"Operator","members":["Bank","Issuer","Venue"],"threshold":2,"label":"E2E NAV Committee"}')"
cc="$(echo "$cm" | field "d.get('contractId','')")"
# 3.0.0 FixingSeries: one fixing per (instrument, session, asOfDate). Each run attests a later
# date (one day per minute since the 2026-09-22 epoch), so reruns never collide with the slot.
# A fixing is dated the day it observes, and the ledger allows ONE per (instrument, session,
# as-of date). Reruns must therefore vary the SESSION, never the date: an earlier version of
# this line advanced the date a day per minute and struck a real attested fixing dated
# 2039-02-03 on DevNet, which is permanent and had to be filtered out of the published series.
# A per-run session also keeps test fixings out of the published Close series entirely.
asof="$(date -u +%F)"
e2e_session="E2E-$(date -u +%H%M%S)"
p1="$(post "/committee/$cc/propose" "{\"proposer\":\"Operator\",\"instrumentId\":\"CBTC\",\"cashInstrument\":\"USDC\",\"session\":\"$e2e_session\",\"price\":65000,\"rationale\":\"E2E: committee-attested mark\",\"asOfDate\":\"$asof\"}")"
pc="$(echo "$p1" | field "d.get('contractId','')")"
# 3.0.0: the administrator (Operator) proposes and never attests; members attest WITH evidence
# (plain /confirm is retired → 410). Venue signs its traded range; Issuer signs one condition with numbers.
early="$(post "/fixing/$pc/finalize" '{"proposer":"Operator","publishTo":["Venue"]}' | cd_code)"
c1="$(post "/fixing/$pc/confirm-checked" '{"member":"Venue","role":"venue","checksPassed":["traded-range"],"observedLow":64500,"observedHigh":65500}' | field "d.get('contractId','')")"
c2="$(post "/fixing/${c1:-$pc}/confirm-checked" '{"member":"Issuer","role":"issuer","checksPassed":["redemption-queue-clear"],"evidence":{"redemption-queue-clear":{"queueDepth":0,"maxQueueDepth":10}}}' | field "d.get('contractId','')")"
fin="$(post "/fixing/${c2:-${c1:-$pc}}/finalize" '{"proposer":"Operator","publishTo":["Venue"]}')"
fcid="$(echo "$fin" | field "d.get('contractId','')" 2>/dev/null || true)"
fpx="$(echo "$fin" | field "d.get('attestedPrice')" 2>/dev/null || true)"
if [ -n "$cc" ] && [ "${early:0:1}" = "4" ] && [ -n "$fcid" ] && [ "${fpx%%.*}" = "65000" ]; then
  t_pass "finalize at 0-of-2 refused (HTTP $early); after Venue + Issuer confirmed with evidence → NavFixing @ $fpx; update $(update_of "$fcid" "$(party Bank)")"
else
  t_fail "committee=$cc early-finalize HTTP $early fin=$(echo "$fin" | head -c 300)"
fi

# ----------------------------------------------------------------------------- T6/T7
BASKET="E2E$(date +%H%M%S)"
t_begin T6 "in-kind create — define $BASKET, Alice delivers underlyings, shares minted atomically"
dfn="$(post /basket "{\"administrator\":\"Bank\",\"basketId\":\"$BASKET\",\"description\":\"E2E fund\",\"cashInstrument\":\"USDC\",\"components\":[{\"instrumentId\":\"cETH\",\"unitsPerShare\":0.1},{\"instrumentId\":\"CBTC\",\"unitsPerShare\":0.01}],\"participants\":[\"Alice\",\"Bob\"]}")"
eth0="$(bal Alice cETH)"; btc0="$(bal Alice CBTC)"
cr="$(post /basket/create "{\"basketId\":\"$BASKET\",\"ap\":\"Alice\",\"shares\":5}")"
rc="$(echo "$cr" | field "d.get('receiptCid','')" 2>/dev/null || true)"
eth1="$(bal Alice cETH)"; btc1="$(bal Alice CBTC)"; sh1="$(bal Alice "$BASKET")"
if [ -n "$rc" ] && "$CD_PY" -c "import sys; sys.exit(0 if abs(($eth0-$eth1)-0.5)<1e-9 and abs(($btc0-$btc1)-0.05)<1e-9 and abs($sh1-5)<1e-9 else 1)"; then
  t_pass "Alice cETH $eth0→$eth1, CBTC $btc0→$btc1, $BASKET shares $sh1; receipt update $(update_of "$rc" "$(party Alice)")"
else
  t_fail "define=$(echo "$dfn" | cd_code) create=$(echo "$cr" | head -c 300) cETH $eth0→$eth1 CBTC $btc0→$btc1 shares=$sh1"
fi

t_begin T7 "in-kind redeem — Alice returns 2 shares, receives underlyings back atomically"
rd="$(post /basket/redeem "{\"basketId\":\"$BASKET\",\"ap\":\"Alice\",\"shares\":2}")"
rr="$(echo "$rd" | field "d.get('receiptCid','')" 2>/dev/null || true)"
eth2="$(bal Alice cETH)"; btc2="$(bal Alice CBTC)"; sh2="$(bal Alice "$BASKET")"
if [ -n "$rr" ] && "$CD_PY" -c "import sys; sys.exit(0 if abs(($eth2-$eth1)-0.2)<1e-9 and abs(($btc2-$btc1)-0.02)<1e-9 and abs($sh2-3)<1e-9 else 1)"; then
  t_pass "cETH $eth1→$eth2, CBTC $btc1→$btc2, shares $sh1→$sh2; receipt update $(update_of "$rr" "$(party Alice)")"
else
  t_fail "redeem=$(echo "$rd" | head -c 300) cETH $eth1→$eth2 CBTC $btc1→$btc2 shares=$sh2"
fi

# ----------------------------------------------------------------------------- T8
t_begin T8 "atomic DvP — Bob buys 1 cETH from Alice for 2,400 USDC; an unfundable trade must fail with NO leg moving"
a_eth="$(bal Alice cETH)"; a_usd="$(bal Alice USDC)"; b_eth="$(bal Bob cETH)"; b_usd="$(bal Bob USDC)"
tr="$(post /trade '{"buyer":"Bob","seller":"Alice","assetInstrument":"cETH","assetAmount":1,"cashInstrument":"USDC","cashAmount":2400}')"
rcpt="$(echo "$tr" | field "d.get('receiptCid','')" 2>/dev/null || true)"
a_eth1="$(bal Alice cETH)"; a_usd1="$(bal Alice USDC)"; b_eth1="$(bal Bob cETH)"; b_usd1="$(bal Bob USDC)"
if [ -n "$rcpt" ] && "$CD_PY" -c "import sys; sys.exit(0 if abs(($a_eth-$a_eth1)-1)<1e-9 and abs(($a_usd1-$a_usd)-2400)<1e-9 and abs(($b_eth1-$b_eth)-1)<1e-9 and abs(($b_usd-$b_usd1)-2400)<1e-9 else 1)"; then
  t_pass "both legs moved together; receipt update $(update_of "$rcpt" "$(party Alice)")"
else
  t_fail "trade=$(echo "$tr" | head -c 300) Alice cETH $a_eth→$a_eth1 USDC $a_usd→$a_usd1 Bob cETH $b_eth→$b_eth1 USDC $b_usd→$b_usd1"
fi
# must-fail: Alice cannot deliver 1,000,000 cETH
bad="$(post /trade '{"buyer":"Bob","seller":"Alice","assetInstrument":"cETH","assetAmount":1000000,"cashInstrument":"USDC","cashAmount":1}')"
a_eth2="$(bal Alice cETH)"; a_usd2="$(bal Alice USDC)"; b_eth2="$(bal Bob cETH)"; b_usd2="$(bal Bob USDC)"
if [ "$(echo "$bad" | cd_code | cut -c1)" = "4" ] && [ "$a_eth2" = "$a_eth1" ] && [ "$a_usd2" = "$a_usd1" ] \
   && [ "$b_eth2" = "$b_eth1" ] && [ "$b_usd2" = "$b_usd1" ]; then
  t_pass "inconsistent trade refused (HTTP $(echo "$bad" | cd_code)): $(echo "$bad" | cd_body | cd_json "str(d.get('message',''))[:120]") — balances unchanged"
else
  t_fail "must-fail trade returned HTTP $(echo "$bad" | cd_code); Alice cETH $a_eth1→$a_eth2 USDC $a_usd1→$a_usd2 Bob cETH $b_eth1→$b_eth2 USDC $b_usd1→$b_usd2"
fi
# must-fail ON THE LEDGER (Test.daml testAtomicRollback over REST): the desk's pre-check above
# never lets an unfundable trade reach Canton, so this one does. The proposal states a cash
# amount that disagrees with the cash holding it names; propose and accept commit, and Settle
# must abort as ONE transaction — the asset leg may not move either.
hold() {  # hold <label> <instrument> → cid of that party's largest holding of it
  get "/holdings?party=$1" | "$CD_PY" -c "
import sys, json
hs = [h for h in json.load(sys.stdin) if h['instrumentId']=='$2' and h['owner'].startswith('$(party "$1" | cut -d: -f1)')]
hs.sort(key=lambda h: -float(h['amount']))
print(hs[0]['contractId'] + ' ' + str(hs[0]['amount']) if hs else '')"
}
read -r bob_aapl bob_aapl_amt <<<"$(hold Bob DEMO:AAPL)"
read -r alice_cash alice_cash_amt <<<"$(hold Alice USDC)"
if [ -z "$bob_aapl" ] || [ -z "$alice_cash" ]; then
  t_fail "ledger-level rollback: no Bob DEMO:AAPL or Alice USDC holding to use"
else
  wrong="$("$CD_PY" -c "print(float('$alice_cash_amt') + 50)")"
  aapl_amt="$("$CD_PY" -c "print(min(float('$bob_aapl_amt'), 10.0))")"
  a_usd3="$(bal Alice USDC)"; b_usd3="$(bal Bob USDC)"; b_aapl3="$(bal Bob DEMO:AAPL)"; a_aapl3="$(bal Alice DEMO:AAPL)"
  # The explicit /dvp/* endpoints take FULL party ids by design (Dtos.java: "no hidden
  # party-name magic") — a label here is refused by the participant as PERMISSION_DENIED
  # "Claims do not authorize to act as party 'Bob'" (seen in the participant log, 2026-09-15).
  pr="$(post /dvp/propose "{\"proposer\":\"$(party Bob)\",\"counterparty\":\"$(party Alice)\",\"auditor\":\"$(party Auditor)\",\"assetHoldingCid\":\"$bob_aapl\",\"cashHoldingCid\":\"$alice_cash\",\"assetInstrument\":\"DEMO:AAPL\",\"assetAmount\":$aapl_amt,\"cashInstrument\":\"USDC\",\"cashAmount\":$wrong}")"
  prc="$(echo "$pr" | field "d.get('contractId','')" 2>/dev/null || true)"
  ac="$(post "/dvp/$prc/accept" "{\"counterparty\":\"$(party Alice)\"}")"
  agc="$(echo "$ac" | field "d.get('contractId','')" 2>/dev/null || true)"
  st="$(post "/dvp/$agc/settle" "{\"proposer\":\"$(party Bob)\"}")"
  a_usd4="$(bal Alice USDC)"; b_usd4="$(bal Bob USDC)"; b_aapl4="$(bal Bob DEMO:AAPL)"; a_aapl4="$(bal Alice DEMO:AAPL)"
  if [ -n "$prc" ] && [ -n "$agc" ] && [ "$(echo "$st" | cd_code | cut -c1)" = "4" ] \
     && [ "$a_usd4" = "$a_usd3" ] && [ "$b_usd4" = "$b_usd3" ] && [ "$b_aapl4" = "$b_aapl3" ] && [ "$a_aapl4" = "$a_aapl3" ]; then
    t_pass "propose (update $(update_of "$prc" "$(party Bob)")) + accept (update $(update_of "$agc" "$(party Bob)")) committed; Settle aborted on-ledger (HTTP $(echo "$st" | cd_code): $(echo "$st" | cd_body | cd_json "str(d.get('message',''))[:110]")); no leg moved"
  else
    t_fail "propose=$(echo "$pr" | cd_code) accept=$(echo "$ac" | cd_code) settle=$(echo "$st" | head -c 300); Alice USDC $a_usd3→$a_usd4 AAPL $a_aapl3→$a_aapl4; Bob USDC $b_usd3→$b_usd4 AAPL $b_aapl3→$b_aapl4"
  fi
fi

# ----------------------------------------------------------------------------- T9
t_begin T9 "conservation — USDC, cETH and CBTC supply unchanged across all of the above"
SUPPLY_AFTER="$(supply)"
cons="$("$CD_PY" -c "
import json
b, a = json.loads('$SUPPLY_BEFORE'), json.loads('$SUPPLY_AFTER')
bad = {k: (b.get(k, 0), a.get(k, 0)) for k in ('USDC', 'cETH', 'CBTC') if abs(b.get(k, 0) - a.get(k, 0)) > 1e-9}
print('OK' if not bad else bad)")"
if [ "$cons" = "OK" ]; then
  t_pass "before $SUPPLY_BEFORE"; RESULTS+=("      after  $SUPPLY_AFTER")
else
  t_fail "supply moved: $cons (after $SUPPLY_AFTER)"
fi

# ----------------------------------------------------------------------------- T10
t_begin T10 "UI smoke — the desk's pages and public API answer"
if [ "$CD_PROFILE" = "localnet" ]; then
  c1="$(curl -sS -m 30 -o /dev/null -w '%{http_code}' "$CD_BACKEND_URL/")"
  c2="$(curl -sS -m 30 -o /dev/null -w '%{http_code}' "$B/benchmarks")"
  [ "$c1" = "200" ] && [ "$c2" = "200" ] && t_pass "GET / → $c1, /api/benchmarks → $c2 (browser walk-through: E2E_TEST_PLAN.md T10)" \
    || t_fail "GET / → $c1, /api/benchmarks → $c2"
else
  SITE=https://crossdesk-devnet-app.web.app
  c1="$(curl -sS -m 30 -o /dev/null -w '%{http_code}' "$SITE/desk")"
  c2="$(curl -sS -m 60 "$SITE/api/health" | cd_json "d.get('auth')" 2>/dev/null || echo none)"
  [ "$c1" = "200" ] && [ "$c2" = "hmac" ] && t_pass "$SITE/desk 200 and /api/** is served by the hmac-wired desk" \
    || t_skip "$SITE/desk → $c1, /api/health auth=$c2 (expected before SWITCH_SITE=1)"
fi

# ----------------------------------------------------------------------------- summary
cd_say "RESULT ($CD_PROFILE, run $RUN): $PASS passed, $FAIL failed, $SKIP skipped"
printf '  %s\n' "${RESULTS[@]}"
exit "$FAIL"
