#!/usr/bin/env bash
# =============================================================================
# 04-onboard.sh — RUNS ON THE VM (sudo). Onboards the validator to DevNet.
#
#   sudo bash ~/own-devnet-validator/04-onboard.sh             # first time: self-serve secret
#   sudo RESTART=1 bash ~/own-devnet-validator/04-onboard.sh   # later restarts: -o ""
#
# Order matters (Splice 0.8.1 validator_onboarding.html): allowlist FIRST (03 passes), THEN
# the secret — the self-serve DevNet secret lives 1 hour and is single-use.
#
# Parameters (Splice 0.8.1 validator_compose.html + start.sh usage, verified 2026-09-15):
#   -s SPONSOR_SV_URL  https://sv.sv-1.dev.global.canton.network.sync.global  (the SV app, "sv.",
#                      NOT scan.; GSF is the self-serve DevNet endpoint)
#   -o SECRET          POST $SPONSOR/api/sv/v0/devnet/onboard/validator/prepare  (1 h, one use)
#   -p PARTY_HINT      crossdesk-validator-1.  NOT "crossdesk": since Splice 0.2.x a new
#                      validator's hint MUST be <organization>-<function>-<enumerator>
#                      (release notes). It becomes the validator operator party id and
#                      can never change.
#   -m MIGRATION_ID    OMITTED by default. In 0.8.1 start.sh: "no longer required, as the
#                      validator resolves it automatically ... if omitted, the database
#                      'participant' is used (recommended for new deployments)". DevNet's
#                      current value is 1 (docs.dev.global.canton.network.sync.global/info);
#                      set MIGRATION_ID=1 only to match an earlier deployment's DB name.
#   -w                 wait until healthy
#
# Secrets: the HMAC secret comes from Secret Manager via the VM's service account and the
# onboarding secret from the SV — both only in this process's environment. start.sh hands
# them to docker compose, so they end up in the containers' config (root-only on this VM).
# =============================================================================
set -euo pipefail
[ "$(id -u)" = "0" ] || { echo "run with sudo" >&2; exit 1; }

PROJECT=crossdesk-devnet-app
SPLICE_VERSION="${SPLICE_VERSION:-0.8.1}"
SPONSOR="${SPONSOR_SV_URL:-https://sv.sv-1.dev.global.canton.network.sync.global}"
SCAN="${SCAN_URL:-https://scan.sv-1.dev.global.canton.network.sync.global}"
PARTY_HINT="${PARTY_HINT:-crossdesk-validator-1}"
VAL=/opt/splice/splice-node/docker-compose/validator
HERE="$(cd "$(dirname "$0")" && pwd)"

[[ "$PARTY_HINT" =~ ^[A-Za-z0-9]+-[A-Za-z0-9]+-[1-9][0-9]*$ ]] || { echo "party hint must be <org>-<function>-<n>"; exit 1; }
[ -x "$VAL/start.sh" ] || { echo "run 02-bootstrap-node.sh first"; exit 1; }
grep -q compose-crossdesk.yaml "$VAL/start.sh" || { echo "start.sh is not patched — re-run 02"; exit 1; }

echo "== 1 · allowlist gate"
if [ "${SKIP_ALLOWLIST_CHECK:-0}" != "1" ]; then
  bash "$HERE/03-verify-allowlist.sh" || { echo "not allowlisted — stopping before a secret is wasted"; exit 1; }
fi

echo "== 2 · secrets and addresses (values never echoed)"
CROSSDESK_LEDGER_HMAC_SECRET="$(gcloud secrets versions access latest --secret crossdesk-ledger-hmac-secret --project "$PROJECT" | tr -d '\r\n')"
[ "${#CROSSDESK_LEDGER_HMAC_SECRET}" -ge 32 ] || { echo "HMAC secret missing/short"; exit 1; }
CROSSDESK_LEDGER_BIND_IP="$(curl -fsS -H 'Metadata-Flavor: Google' \
  http://metadata.google.internal/computeMetadata/v1/instance/network-interfaces/0/ip)"
export CROSSDESK_LEDGER_HMAC_SECRET CROSSDESK_LEDGER_BIND_IP
export CROSSDESK_LEDGER_AUDIENCE="${CROSSDESK_LEDGER_AUDIENCE:-https://ledger-api.crossdesk-devnet.internal}"
export CROSSDESK_SCAN_URL="$SCAN"
export IMAGE_TAG="$SPLICE_VERSION"
export CONTACT_POINT="${CONTACT_POINT:-s.borjas@lucilla.ca}"
echo "  ledger API will bind $CROSSDESK_LEDGER_BIND_IP:5001, audience $CROSSDESK_LEDGER_AUDIENCE"

if [ "${RESTART:-0}" = "1" ]; then
  SECRET=""
  echo "== 3 · restart (already onboarded: -o \"\")"
else
  echo "== 3 · self-serve DevNet onboarding secret (valid 1 hour, single use)"
  SECRET="$(curl -fsS -m 30 -X POST "$SPONSOR/api/sv/v0/devnet/onboard/validator/prepare")"
  [ -n "$SECRET" ] || { echo "the SV returned no secret"; exit 1; }
  echo "  obtained (length ${#SECRET})"
fi

echo "== 4 · start.sh"
cd "$VAL"
args=(-s "$SPONSOR" -o "$SECRET" -p "$PARTY_HINT" -w)
[ -n "${MIGRATION_ID:-}" ] && args+=(-m "$MIGRATION_ID")
./start.sh "${args[@]}"
unset SECRET CROSSDESK_LEDGER_HMAC_SECRET

echo "== 5 · health"
docker compose -f compose.yaml -f compose-disable-auth.yaml -f compose-crossdesk.yaml ps
curl -fsS -m 10 http://127.0.0.1:7575/v2/version && echo
ss -ltnp | grep -E ':5001|:5012|:7575|:80 ' || true
cat <<EOF

Onboarded. The validator operator party is ${PARTY_HINT}::<namespace>; its wallet user is
"administrator". From the LAPTOP, open the tunnel (profiles/own-devnet.env) and run:
  CD_PROFILE=own-devnet ./05-upload-dar-and-parties.sh

BACK UP THE IDENTITIES NOW (README "Backups"): losing them loses the validator's coins.
EOF
