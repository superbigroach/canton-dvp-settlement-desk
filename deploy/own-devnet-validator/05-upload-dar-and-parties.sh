#!/usr/bin/env bash
# =============================================================================
# 05-upload-dar-and-parties.sh — DAR, parties, the desk's ledger user and its rights.
#
#   CD_PROFILE=localnet   ./05-upload-dar-and-parties.sh
#   CD_PROFILE=own-devnet ./05-upload-dar-and-parties.sh     (IAP tunnel open, see profile)
#
# Idempotent: re-running uploads the same DAR (a no-op on the participant), skips parties
# that exist, and re-grants rights (granting an existing right is a no-op).
#
# 1. upload .daml/dist/crossdesk-2.1.0.dar   (SDK 3.4.11 → Daml-LF 2.2; a Canton 3.5.17
#    participant accepts LF 2.1/2.2 — verified on LocalNet 0.8.1, 2026-09-15)
# 2. allocate <hint>-crossdesk for every hint in CD_PARTY_HINTS on THIS participant
# 3. create user CD_BACKEND_USER (primary party = issuer) with CanActAs + CanReadAs on
#    every CrossDesk party, and CanReadAs + CanActAs on the wallet party
# 4. print the LEDGER_PARTIES roster line the backend needs (06-wire-backend.sh reads
#    it from .roster-<profile>, which holds party ids only — no secrets)
#
# Lesson baked in (Ledger API v2): the backend's applicationId MUST equal this user id,
# and actAs must carry FULL party ids. The backend enforces the first in hmac mode.
# =============================================================================
# shellcheck source=lib/common.sh
. "$(dirname "$0")/lib/common.sh"
cd_load_profile

DAR="${DAR:-$CD_REPO/.daml/dist/crossdesk-2.1.0.dar}"
[ -f "$DAR" ] || cd_die "no DAR at $DAR — run 'daml build' (SDK $(grep '^sdk-version' "$CD_REPO/daml.yaml" | awk '{print $2}'))"

cd_say "0 · participant"
ver="$(cd_ledger GET /v2/version | cd_body | cd_json "d['version']")" || cd_die "JSON Ledger API unreachable at $CD_NGINX"
ns="$(cd_namespace)"
cd_ok "Canton $ver, namespace ${ns:0:16}…  ($CD_PROFILE)"

cd_say "1 · upload DAR  $(basename "$DAR")"
out="$(cd_ledger POST /v2/packages "@$DAR")"
[ "$(echo "$out" | cd_code)" = "200" ] || cd_die "DAR upload: $(echo "$out" | cd_body | head -c 600)"
cd_ok "uploaded (sha256 $(sha256sum "$DAR" | cut -c1-16)…)"

cd_say "2 · parties"
roster=""
first_party=""
for hint in $CD_PARTY_HINTS; do
  full="$(cd_full_party "$hint" "$ns")"
  known="$(cd_ledger GET "/v2/parties/$full" | cd_body | cd_json "len(d.get('partyDetails',[]))")"
  if [ "$known" = "0" ]; then
    out="$(cd_ledger POST /v2/parties "{\"partyIdHint\":\"$hint$CD_PARTY_SUFFIX\",\"identityProviderId\":\"\"}")"
    [ "$(echo "$out" | cd_code)" = "200" ] || cd_die "allocate $hint: $(echo "$out" | cd_body | head -c 400)"
    got="$(echo "$out" | cd_body | cd_json "d['partyDetails']['party']")"
    [ "$got" = "$full" ] || cd_die "allocated $got, expected $full"
    cd_ok "allocated $full"
  else
    cd_ok "exists    $full"
  fi
  roster="${roster:+$roster,}$(cd_label "$hint")=$full"
  [ -n "$first_party" ] || first_party="$full"
done

wallet_party="$(cd_ledger GET "/v2/users/$CD_WALLET_USER" | cd_body | cd_json "d['user'].get('primaryParty','')")"
[ -n "$wallet_party" ] || cd_die "wallet user $CD_WALLET_USER has no primary party (is the validator fully up?)"
cd_ok "wallet    $wallet_party  (user $CD_WALLET_USER)"
roster="$roster,Wallet=$wallet_party"

cd_say "3 · ledger user $CD_BACKEND_USER and its rights"
rights="$("$CD_PY" - "$roster" <<'PY'
import json, sys
parties = sorted({p.split('=', 1)[1] for p in sys.argv[1].split(',')})
r = []
for p in parties:
    r.append({"kind": {"CanActAs": {"value": {"party": p}}}})
    r.append({"kind": {"CanReadAs": {"value": {"party": p}}}})
print(json.dumps(r))
PY
)"
code="$(cd_ledger GET "/v2/users/$CD_BACKEND_USER" | cd_code)"
if [ "$code" = "404" ]; then
  out="$(cd_ledger POST /v2/users "{\"user\":{\"id\":\"$CD_BACKEND_USER\",\"primaryParty\":\"$first_party\",\"isDeactivated\":false,\"identityProviderId\":\"\",\"metadata\":{\"resourceVersion\":\"\",\"annotations\":{}}},\"rights\":$rights}")"
  [ "$(echo "$out" | cd_code)" = "200" ] || cd_die "create user: $(echo "$out" | cd_body | head -c 500)"
  cd_ok "created $CD_BACKEND_USER"
else
  out="$(cd_ledger POST "/v2/users/$CD_BACKEND_USER/rights" "{\"userId\":\"$CD_BACKEND_USER\",\"rights\":$rights,\"identityProviderId\":\"\"}")"
  [ "$(echo "$out" | cd_code)" = "200" ] || cd_die "grant rights: $(echo "$out" | cd_body | head -c 500)"
  cd_ok "rights re-granted to existing $CD_BACKEND_USER"
fi
n="$(cd_ledger GET "/v2/users/$CD_BACKEND_USER/rights" | cd_body | cd_json "len(d['rights'])")"
cd_ok "$CD_BACKEND_USER holds $n rights (expect $(( $(echo "$roster" | tr ',' '\n' | cut -d= -f2 | sort -u | wc -l) * 2 )))"
# NOT granted: ParticipantAdmin / IdentityProviderAdmin. The desk never needs them — the
# roster replaces the party listing — and a leaked backend token then cannot mint users.

cd_say "4 · backend roster"
echo "$roster" > "$CD_HERE/.roster-$CD_PROFILE"
echo "  LEDGER_PARTIES=$roster"
echo "  (saved to $CD_HERE/.roster-$CD_PROFILE — party ids only, gitignored)"
