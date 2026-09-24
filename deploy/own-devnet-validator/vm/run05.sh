#!/usr/bin/env bash
# run05.sh — run step 05 ON THE VM against the local JSON Ledger API (no IAP tunnel needed).
# The profile file wins over env, so derive a VM profile from own-devnet.env with local ports.
set -euo pipefail
cd "$HOME/own-devnet-validator"
sed -e 's#http://127.0.0.1:17575#http://127.0.0.1:7575#' -e 's#http://127.0.0.1:18080#http://127.0.0.1:80#' \
    -e 's#^CD_PARTY_HINTS=.*#CD_PARTY_HINTS="issuer bank alice bob auditor venue agent operator"#' \
    profiles/own-devnet.env > profiles/own-devnet-vm.env
grep -n "CD_JSON_API\|CD_NGINX\|CD_PARTY_HINTS" profiles/own-devnet-vm.env
export CD_PROFILE=own-devnet-vm
export DAR="$HOME/crossdesk-3.0.0.dar"
bash ./05-upload-dar-and-parties.sh 2>&1 | sed 's/secret[^,]*/secret=x/g'
echo "=== roster file"
cp .roster-own-devnet-vm .roster-own-devnet
tr ',' '\n' < .roster-own-devnet
