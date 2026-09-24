#!/usr/bin/env bash
# verify-holdingv1.sh — RUNS ON THE VM. Lists Alice's token-standard (HoldingV1) holdings on our
# participant through the JSON Ledger API, by interface, so real registry assets (CBTC) show up.
set -euo pipefail
cd "$HOME/own-devnet-validator"
. ./lib/common.sh
export CD_PROFILE=own-devnet-vm
cd_load_profile
alice="$(grep -o 'Alice=[^,]*' .roster-own-devnet-vm | cut -d= -f2)"
tok="$(cd_mint "$CD_BACKEND_USER")"   # the desk user holds readAs for every roster party
end="$(curl -sS -H "Authorization: Bearer $tok" "$CD_JSON_API/v2/state/ledger-end" | python3 -c 'import sys,json; print(json.load(sys.stdin)["offset"])')"
echo "ledger end $end, party $alice"
curl -sS -m 60 -X POST -H "Authorization: Bearer $tok" -H 'Content-Type: application/json' "$CD_JSON_API/v2/state/active-contracts" \
  -d "{\"eventFormat\":{\"filtersByParty\":{\"$alice\":{\"cumulative\":[{\"identifierFilter\":{\"InterfaceFilter\":{\"value\":{\"interfaceId\":\"#splice-api-token-holding-v1:Splice.Api.Token.HoldingV1:Holding\",\"includeInterfaceView\":true,\"includeCreatedEventBlob\":false}}}}]}},\"verbose\":false},\"activeAtOffset\":$end}" \
  > /tmp/acs.json
head -c 400 /tmp/acs.json; echo
python3 - <<'PY'
import json
raw = json.load(open('/tmp/acs.json'))
rows = raw if isinstance(raw, list) else raw.get('result', raw.get('contracts', []))
n = 0
for r in rows:
    if not isinstance(r, dict):
        continue
    ev = (r.get("contractEntry") or {}).get("JsActiveContract", {}).get("createdEvent", {})
    for iv in ev.get("interfaceViews", []):
        v = iv.get("viewValue", {})
        inst = v.get("instrumentId", {})
        n += 1
        print("HOLDING", inst.get("id"), v.get("amount"), "admin=" + str(inst.get("admin", ""))[:44])
print("holdings:", n)
PY
