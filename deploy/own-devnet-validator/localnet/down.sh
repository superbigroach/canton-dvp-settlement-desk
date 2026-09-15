#!/usr/bin/env bash
# localnet/down.sh — stop Track A.
#   ./localnet/down.sh           stop containers, KEEP the ledger (postgres volume)
#   WIPE=1 ./localnet/down.sh    also delete volumes: a fresh network next time (parties,
#                                DAR and the .roster-localnet file must then be redone by 05)
set -euo pipefail
SPLICE_VERSION="${SPLICE_VERSION:-0.8.1}"
LOCALNET_HOME="${LOCALNET_HOME:-$HOME/crossdesk-localnet}"
LN="$LOCALNET_HOME/splice-node/docker-compose/localnet"
HERE="$(cd "$(dirname "$0")/.." && pwd)"
if command -v cygpath >/dev/null 2>&1; then export LOCALNET_DIR="$(cygpath -m "$LN")"; else export LOCALNET_DIR="$LN"; fi
export IMAGE_TAG="$SPLICE_VERSION" APP_USER_PROFILE=off

# the desk first, gracefully (never kill -9 a JVM holding ledger state)
[ -f "$HERE/.localnet-backend.pid" ] && kill "$(cat "$HERE/.localnet-backend.pid")" 2>/dev/null || true
docker rm -f crossdesk-scan-proxy >/dev/null 2>&1 || true
flags=(); [ "${WIPE:-0}" = "1" ] && flags=(-v)
docker compose --env-file "$LN/compose.env" --env-file "$LN/env/common.env" \
  -f "$LN/compose.yaml" -f "$LN/resource-constraints.yaml" --profile sv --profile app-provider down "${flags[@]}"
[ "${WIPE:-0}" = "1" ] && rm -f "$HERE/.roster-localnet" && rm -rf "$HERE/.localnet-backend-data"
echo "LocalNet stopped${WIPE:+ and wiped}."
