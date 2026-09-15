#!/usr/bin/env bash
# =============================================================================
# lib/common.sh — shared helpers for the own-node / LocalNet scripts.
#
# Source it; do not run it. Every script picks its target with ONE variable:
#
#     CD_PROFILE=localnet     ./05-upload-dar-and-parties.sh     # Track A, today
#     CD_PROFILE=own-devnet   ./05-upload-dar-and-parties.sh     # Track B, after allowlisting
#
# which loads profiles/<CD_PROFILE>.env. Nothing else changes between the two.
#
# SECRETS: profiles carry a REFERENCE (CD_HMAC_SECRET_REF=gsm:<name> or, for LocalNet's
# published development value only, literal:unsafe). The value is resolved into a
# shell variable at run time and is never echoed, written to disk or passed on a
# command line visible in `ps`.
# =============================================================================
set -euo pipefail

CD_HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CD_REPO="$(cd "$CD_HERE/../.." && pwd)"          # canton-dvp-settlement-desk/
CD_GCP_PROJECT="crossdesk-devnet-app"

cd_die()  { printf '\033[1;31mFAIL: %s\033[0m\n' "$*" >&2; exit 1; }
# cd_run <cmd...> — prints the command; executes it only when EXECUTE=1 (GCP-mutating scripts).
cd_run()  { printf '  $'; printf ' %q' "$@"; echo; if [ "${EXECUTE:-0}" = "1" ]; then "$@"; fi; }
cd_say()  { printf '\n\033[1m== %s\033[0m\n' "$*"; }
cd_ok()   { printf '  \033[32mok\033[0m  %s\n' "$*"; }
cd_warn() { printf '  \033[33mwarn\033[0m %s\n' "$*"; }

# ---- python (JSON) ----------------------------------------------------------
# jq is not on the Windows laptop; python is on both the laptop and the VM.
if python3 -c 'import json' >/dev/null 2>&1; then CD_PY=python3
elif python -c 'import json' >/dev/null 2>&1; then CD_PY=python
else cd_die "python3 (or python) is required"; fi
cd_json() { "$CD_PY" -c "import sys,json; d=json.load(sys.stdin); print($1)"; }

# ---- profile ----------------------------------------------------------------
cd_load_profile() {
  local p="${CD_PROFILE:-}"
  [ -n "$p" ] || cd_die "set CD_PROFILE=localnet or CD_PROFILE=own-devnet"
  local f="$CD_HERE/profiles/$p.env"
  [ -f "$f" ] || cd_die "no profile $f"
  set -a; # shellcheck disable=SC1090
  . "$f"; set +a
  : "${CD_NGINX:?}" "${CD_JSON_API:?}" "${CD_TOKEN_AUDIENCE:?}" "${CD_HMAC_SECRET_REF:?}" "${CD_ADMIN_USER:?}" \
    "${CD_BACKEND_USER:?}" "${CD_WALLET_USER:?}" "${CD_PARTY_HINTS:?}" "${CD_PARTY_SUFFIX:?}"
}

cd_secret() {  # resolve CD_HMAC_SECRET_REF into stdout (callers capture it, never echo it)
  case "$CD_HMAC_SECRET_REF" in
    literal:*) printf '%s' "${CD_HMAC_SECRET_REF#literal:}" ;;
    gsm:*)     gcloud secrets versions access latest --secret "${CD_HMAC_SECRET_REF#gsm:}" \
                 --project "$CD_GCP_PROJECT" ;;
    *) cd_die "CD_HMAC_SECRET_REF must be literal:<v> or gsm:<secret-name>" ;;
  esac
}

# ---- tokens -----------------------------------------------------------------
cd_b64url() { openssl base64 -A | tr '+/' '-_' | tr -d '='; }

# cd_mint <sub> [aud] — HS256 {sub, aud, iat, exp}; the exact shape HmacTokenMinter.java mints.
cd_mint() {
  local sub="$1" aud="${2:-$CD_TOKEN_AUDIENCE}" secret now h p s
  secret="$(cd_secret)"
  now="$(date +%s)"
  h="$(printf '{"alg":"HS256","typ":"JWT"}' | cd_b64url)"
  p="$(printf '{"sub":"%s","aud":"%s","iat":%d,"exp":%d}' "$sub" "$aud" "$now" $((now + 3600)) | cd_b64url)"
  s="$(printf '%s.%s' "$h" "$p" | openssl dgst -sha256 -hmac "$secret" -binary | cd_b64url)"
  printf '%s.%s.%s' "$h" "$p" "$s"
}

# ---- JSON Ledger API (direct to the participant's HTTP port) ----------------
# NOT through nginx: the Splice nginx keeps the default 1 MB client_max_body_size and
# answers a 3 MB DAR upload with 413 (hit on LocalNet 2026-09-15).
# cd_ledger <METHOD> <path> [json-body|@file] — prints body, then a final line "HTTP <code>".
cd_ledger() {
  local method="$1" path="$2" body="${3:-}" token
  token="$(cd_mint "$CD_ADMIN_USER")"
  local args=(-sS -m 300 -X "$method" -H "Authorization: Bearer $token" -w '\nHTTP %{http_code}')
  if [ -n "$body" ]; then
    if [ "${body#@}" != "$body" ]; then
      args+=(-H 'Content-Type: application/octet-stream' --data-binary "$body")
    else
      args+=(-H 'Content-Type: application/json' -d "$body")
    fi
  fi
  curl "${args[@]}" "$CD_JSON_API$path"
}
cd_code() { tail -n1 | awk '{print $2}'; }       # pipe cd_ledger output → status code
cd_body() { sed '$d'; }                          # pipe cd_ledger output → body

# ---- Validator app API (wallet), routed by Host wallet.localhost -------------
# The validator API has its own audience (VALIDATOR_AUTH_AUDIENCE) and, on both LocalNet and
# the compose validator, its own development secret "unsafe" — which is why it is only ever
# reachable on 127.0.0.1 / through the SSH tunnel, never from the VPC.
cd_validator() {
  local method="$1" path="$2" body="${3:-}" token now h p s
  now="$(date +%s)"
  h="$(printf '{"alg":"HS256","typ":"JWT"}' | cd_b64url)"
  p="$(printf '{"sub":"%s","aud":"%s","iat":%d,"exp":%d}' "$CD_WALLET_USER" "$CD_VALIDATOR_AUDIENCE" "$now" $((now + 3600)) | cd_b64url)"
  s="$(printf '%s.%s' "$h" "$p" | openssl dgst -sha256 -hmac "unsafe" -binary | cd_b64url)"
  token="$h.$p.$s"
  local args=(-sS -m 120 -X "$method" -H "Host: wallet.localhost" -H "Authorization: Bearer $token"
              -w '\nHTTP %{http_code}')
  [ -n "$body" ] && args+=(-H 'Content-Type: application/json' -d "$body")
  curl "${args[@]}" "$CD_NGINX$path"
}

# ---- party ids ---------------------------------------------------------------
cd_namespace() {   # the participant's namespace fingerprint (1220…)
  cd_ledger GET /v2/parties/participant-id | cd_body | cd_json "d['participantId'].split('::')[1]"
}
cd_full_party() { printf '%s%s::%s' "$1" "$CD_PARTY_SUFFIX" "$2"; }   # hint namespace
cd_label() { printf '%s' "$1" | "$CD_PY" -c "import sys; s=sys.stdin.read(); print(s[:1].upper()+s[1:])"; }
