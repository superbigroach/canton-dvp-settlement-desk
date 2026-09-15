#!/usr/bin/env bash
# =============================================================================
# localnet/up.sh — Track A: Splice LocalNet (the full Canton Network stack) on this machine.
#
#   LOCALNET_HOME=/e/crossdesk-localnet ./localnet/up.sh
#
# Brings up: SV (sequencer, mediator, Scan, SV app, DSO + Canton Coin), and the app-provider
# validator + participant that stands in for OUR validator. The app-user validator is
# switched off (not needed; saves memory). Then starts the Scan port sidecar the desk needs.
#
# Matches DevNet's version so the switch to Track B changes config, not software:
#   SPLICE_VERSION=0.8.1 (docs.dev.global.canton.network.sync.global/info, 2026-09-15)
#
# Disk: images ≈ 3.4 GB inside Docker's disk, bundle 800 MB (kept under LOCALNET_HOME).
# Then:  CD_PROFILE=localnet ../05-upload-dar-and-parties.sh && ../06-wire-backend.sh
#        ../../../scripts/bootstrap-devnet.sh http://localhost:8080/api
#        CD_PROFILE=localnet ../07-e2e-real-devnet.sh
# =============================================================================
set -euo pipefail
SPLICE_VERSION="${SPLICE_VERSION:-0.8.1}"
LOCALNET_HOME="${LOCALNET_HOME:-$HOME/crossdesk-localnet}"
BUNDLE="$LOCALNET_HOME/${SPLICE_VERSION}_splice-node.tar.gz"
URL="https://github.com/digital-asset/decentralized-canton-sync/releases/download/v${SPLICE_VERSION}/${SPLICE_VERSION}_splice-node.tar.gz"

docker info >/dev/null 2>&1 || { echo "Docker is not running (start Docker Desktop)"; exit 1; }
mkdir -p "$LOCALNET_HOME"
[ -f "$BUNDLE" ] || curl -fSL "$URL" -o "$BUNDLE"
if [ ! -f "$LOCALNET_HOME/splice-node/VERSION" ] || [ "$(cat "$LOCALNET_HOME/splice-node/VERSION")" != "$SPLICE_VERSION" ]; then
  tar xzf "$BUNDLE" -C "$LOCALNET_HOME" splice-node/VERSION splice-node/docker-compose/localnet splice-node/dars
fi

LN="$LOCALNET_HOME/splice-node/docker-compose/localnet"
# Docker Desktop on Windows wants a Windows-style path for bind mounts.
if command -v cygpath >/dev/null 2>&1; then export LOCALNET_DIR="$(cygpath -m "$LN")"; else export LOCALNET_DIR="$LN"; fi
export IMAGE_TAG="$SPLICE_VERSION" APP_USER_PROFILE=off
compose() {
  docker compose --env-file "$LN/compose.env" --env-file "$LN/env/common.env" \
    -f "$LN/compose.yaml" -f "$LN/resource-constraints.yaml" --profile sv --profile app-provider "$@"
}

compose up -d --wait --wait-timeout 900

# The desk's Java HTTP client can neither resolve *.localhost nor override Host, so Scan
# (inside the `splice` container on :5012, otherwise only behind nginx Host: scan.localhost)
# is published on 127.0.0.1:5012 for the Token Standard registry calls.
docker rm -f crossdesk-scan-proxy >/dev/null 2>&1 || true
docker run -d --name crossdesk-scan-proxy --network localnet --restart unless-stopped \
  -p 127.0.0.1:5012:5012 alpine/socat:1.8.0.3 tcp-listen:5012,fork,reuseaddr tcp-connect:splice:5012 >/dev/null

for _ in $(seq 1 60); do
  curl -fsS -m 3 http://127.0.0.1:5012/registry/metadata/v1/info >/dev/null 2>&1 && break; sleep 5
done
echo "Scan registry: $(curl -fsS -m 5 http://127.0.0.1:5012/registry/metadata/v1/info)"
echo "Participant  : $(curl -fsS -m 5 http://localhost:3975/v2/version | head -c 80)…"
compose ps --format 'table {{.Name}}\t{{.Status}}'
