#!/usr/bin/env bash
# dump-identities.sh — RUNS ON THE VM. Saves the participant identities (validator admin API)
# to a root-only file. Never prints the contents. Admin endpoints need the VALIDATOR user
# (ledger-api-user), not the wallet user, under the validator audience with the dev secret.
set -euo pipefail
cd "$HOME/own-devnet-validator"
. ./lib/common.sh
export CD_PROFILE=own-devnet-vm
cd_load_profile
now="$(date +%s)"
h="$(printf '{"alg":"HS256","typ":"JWT"}' | cd_b64url)"
p="$(printf '{"sub":"%s","aud":"%s","iat":%d,"exp":%d}' "$CD_ADMIN_USER" "$CD_VALIDATOR_AUDIENCE" "$now" $((now + 3600)) | cd_b64url)"
s="$(printf '%s.%s' "$h" "$p" | openssl dgst -sha256 -hmac "unsafe" -binary | cd_b64url)"
tok="$h.$p.$s"
out=/tmp/identities.json
code="$(curl -sS -m 120 -o "$out" -w '%{http_code}' -H "Host: wallet.localhost" -H "Authorization: Bearer $tok" "$CD_NGINX/api/validator/v0/admin/participant/identities")"
chmod 600 "$out"
echo "HTTP $code bytes=$(wc -c < "$out") keys=$(python3 -c "import json; d=json.load(open('$out')); print(','.join(sorted(d.keys()))[:160])" 2>/dev/null || echo not-json)"
