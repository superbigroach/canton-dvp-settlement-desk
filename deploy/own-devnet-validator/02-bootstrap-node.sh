#!/usr/bin/env bash
# =============================================================================
# 02-bootstrap-node.sh — RUNS ON THE VM (sudo). Installs Docker + tools and stages the
# Splice validator compose bundle with the CrossDesk overlay. Does NOT start the node —
# that is 04-onboard.sh, after 03-verify-allowlist.sh passes.
#
#   sudo bash ~/own-devnet-validator/02-bootstrap-node.sh
#
# Idempotent. Pins:
#   SPLICE_VERSION=0.8.1 — what DevNet runs today (https://docs.dev.global.canton.network.sync.global/info
#   → {"sv":{"version":"0.8.1","migration_id":1}}, checked 2026-09-15). Re-check before running:
#   DevNet upgrades first, and the validator must match the network.
# =============================================================================
set -euo pipefail
[ "$(id -u)" = "0" ] || { echo "run with sudo" >&2; exit 1; }

SPLICE_VERSION="${SPLICE_VERSION:-0.8.1}"
GRPCURL_VERSION="${GRPCURL_VERSION:-1.9.3}"
HERE="$(cd "$(dirname "$0")" && pwd)"
NODE_DIR=/opt/splice
BUNDLE_URL="https://github.com/digital-asset/decentralized-canton-sync/releases/download/v${SPLICE_VERSION}/${SPLICE_VERSION}_splice-node.tar.gz"

say() { printf '\n\033[1m== %s\033[0m\n' "$*"; }

say "0 · network version check"
live="$(curl -fsS -m 15 https://docs.dev.global.canton.network.sync.global/info | python3 -c 'import sys,json; print(json.load(sys.stdin)["sv"]["version"])' || echo unknown)"
echo "  DevNet runs $live; this bootstrap pins $SPLICE_VERSION"
if [ "$live" != "unknown" ] && [ "$live" != "$SPLICE_VERSION" ]; then
  echo "  !! version mismatch — re-run with SPLICE_VERSION=$live (and re-read the release notes)" >&2
  exit 1
fi

say "1 · packages (docker from docker.com's apt repo, jq, grpcurl)"
if ! command -v docker >/dev/null; then
  apt-get update -y
  apt-get install -y ca-certificates curl gnupg jq python3
  install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/debian/gpg -o /etc/apt/keyrings/docker.asc
  chmod a+r /etc/apt/keyrings/docker.asc
  . /etc/os-release
  echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/debian $VERSION_CODENAME stable" \
    > /etc/apt/sources.list.d/docker.list
  apt-get update -y
  apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
fi
command -v jq >/dev/null || apt-get install -y jq
docker compose version     # Splice requires >= 2.26.0
if ! command -v grpcurl >/dev/null; then
  curl -fsSL "https://github.com/fullstorydev/grpcurl/releases/download/v${GRPCURL_VERSION}/grpcurl_${GRPCURL_VERSION}_linux_x86_64.tar.gz" \
    | tar xz -C /usr/local/bin grpcurl
fi
grpcurl -version

say "2 · docker log rotation (the participant is chatty; the disk is finite)"
if [ ! -f /etc/docker/daemon.json ]; then
  printf '{ "log-driver": "json-file", "log-opts": { "max-size": "50m", "max-file": "5" } }\n' > /etc/docker/daemon.json
  systemctl restart docker
fi

say "3 · Splice $SPLICE_VERSION bundle → $NODE_DIR"
mkdir -p "$NODE_DIR"
if [ ! -f "$NODE_DIR/splice-node/VERSION" ] || [ "$(cat "$NODE_DIR/splice-node/VERSION")" != "$SPLICE_VERSION" ]; then
  tmp="$(mktemp -d)"
  curl -fSL "$BUNDLE_URL" -o "$tmp/bundle.tgz"               # ~800 MB
  sha256sum "$tmp/bundle.tgz" | tee "$NODE_DIR/bundle-${SPLICE_VERSION}.sha256"
  tar xzf "$tmp/bundle.tgz" -C "$NODE_DIR" splice-node/VERSION splice-node/docker-compose/validator
  rm -rf "$tmp"
fi
VAL="$NODE_DIR/splice-node/docker-compose/validator"
ls "$VAL"

say "4 · CrossDesk overlay (HMAC ledger auth, Ledger API on the internal IP, Scan proxy)"
install -m 0644 "$HERE/node/compose-crossdesk.yaml" "$VAL/compose-crossdesk.yaml"
install -m 0644 "$HERE/node/scan-proxy.conf.template" "$VAL/scan-proxy.conf.template"
# start.sh takes a fixed list of compose files. Add ours LAST (so its environment entries
# override compose-disable-auth.yaml by key). Guarded, so re-running does not duplicate it.
if ! grep -q 'compose-crossdesk.yaml' "$VAL/start.sh"; then
  cp "$VAL/start.sh" "$VAL/start.sh.orig"
  sed -i 's#^extra_args=()$#extra_compose_files+=("-f" "${script_dir}/compose-crossdesk.yaml")\nextra_args=()#' "$VAL/start.sh"
fi
grep -n 'compose-crossdesk.yaml' "$VAL/start.sh"
# stop.sh must see the same file set or `down` leaves our sidecar running.
if ! grep -q 'compose-crossdesk.yaml' "$VAL/stop.sh"; then
  cp "$VAL/stop.sh" "$VAL/stop.sh.orig"
  # stop.sh (0.8.1) spells it "$script_dir/compose.yaml", without braces.
  sed -i 's#docker compose -f "$script_dir/compose.yaml"#docker compose -f "$script_dir/compose.yaml" -f "$script_dir/compose-crossdesk.yaml"#' "$VAL/stop.sh"
fi

say "5 · pull images now (so onboarding inside the 1-hour secret window is fast)"
( cd "$VAL" && IMAGE_TAG="$SPLICE_VERSION" CROSSDESK_LEDGER_BIND_IP=127.0.0.1 CROSSDESK_LEDGER_AUDIENCE=x \
    CROSSDESK_LEDGER_HMAC_SECRET=x CROSSDESK_SCAN_URL=https://example.invalid \
    docker compose -f compose.yaml -f compose-disable-auth.yaml -f compose-crossdesk.yaml pull -q )
df -h / | tail -1

echo
echo "Bootstrap done. Next, ON THE VM: bash $HERE/03-verify-allowlist.sh"
