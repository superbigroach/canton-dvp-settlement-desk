#!/usr/bin/env bash
# install-utility-dars.sh — RUNS ON THE VM. Downloads the DA Utilities (Registry App) DAR bundle
# that DevNet runs (0.14.4, per docs.digitalasset.com/registry/releases/daml-models, 24 Sep 2026),
# verifies the SHA-256, and uploads every DAR to our participant through the JSON Ledger API.
# Needed so a CIP-56 transfer of CBTC (BitSafe registry) can name our party: the faucet refused
# with UNRESOLVED_PACKAGE_NAME utility-registry-app-v0 until these packages are vetted here.
set -euo pipefail
V=0.14.4
BASE=https://get.digitalasset.com/utility-dars
W="$HOME/utility-dars-$V"; mkdir -p "$W"; cd "$W"
curl -fsSL -o bundle.tar.gz "$BASE/canton-network-utility-dars-$V.tar.gz"
curl -fsSL -o bundle.sha256 "$BASE/canton-network-utility-dars-$V.tar.gz.sha256"
echo "checksum file says: $(cat bundle.sha256)"
want="$(awk '{print $1}' bundle.sha256)"; have="$(sha256sum bundle.tar.gz | awk '{print $1}')"
[ "$want" = "$have" ] || { echo "SHA-256 MISMATCH ($have)"; exit 1; }
echo "sha256 ok"
tar xzf bundle.tar.gz
find . -name '*.dar' | sort
cd "$HOME/own-devnet-validator"
. ./lib/common.sh
export CD_PROFILE=own-devnet-vm
cd_load_profile
for d in $(find "$W" -name '*.dar' | sort); do
  code="$(cd_ledger POST /v2/packages "@$d" | cd_code)"
  echo "upload $(basename "$d") -> HTTP $code"
done
echo "=== packages now on the participant containing 'utility'"
cd_ledger GET /v2/packages | cd_body | "$CD_PY" -c "import sys,json; d=json.load(sys.stdin); print(len(d.get('packageIds',[])), 'packages total')"
