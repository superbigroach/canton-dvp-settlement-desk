#!/usr/bin/env bash
# =============================================================================
# 03-verify-allowlist.sh — RUNS ON THE VM. "TRACE 6": is our egress IP allowlisted by
# enough Super Validators to onboard? Read-only; safe to run as often as you like.
#
#   bash ~/own-devnet-validator/03-verify-allowlist.sh
#
# Source: Splice 0.8.1 docs, validator_operator/validator_onboarding.html "Validating that
# your IP has been approved" (docs.dev.sync.global, fetched 2026-09-15), with the host set
# for DevNet (scan.sv-1.dev.global...).
#
#   step 1  egress IP == 34.71.67.227           (from THIS VM; a laptop proves nothing)
#   step 2  every SV Scan answers /api/scan/version   (a TIMEOUT or 403 = not allowlisted yet)
#   step 3  every SV sequencer answers grpc.health.v1.Health/Check with SERVING
#           (a SEPARATE allowlist entry: Scan OK + sequencers timing out = partial allowlisting)
#   verdict the default BFT config needs >= 2/3 of SVs reachable for BOTH scans and sequencers
#
# Exit 0 only when both are >= 2/3. Laptop note: from a non-allowlisted IP DevNet Scan answers
# HTTP 403 (observed 2026-09-15), not a timeout — both mean "not on the list".
# =============================================================================
set -uo pipefail
EXPECTED_IP="${EXPECTED_IP:-34.71.67.227}"
SEED="${SEED_SCAN:-https://scan.sv-1.dev.global.canton.network.sync.global}"
CURL='curl -fsS -m 5 --connect-timeout 5'
for t in curl jq grpcurl; do command -v "$t" >/dev/null || { echo "missing $t (run 02-bootstrap-node.sh)"; exit 2; }; done

echo "== step 1 · egress IP"
ip="$($CURL http://checkip.amazonaws.com | tr -d '[:space:]')"
echo "  egress $ip (expected $EXPECTED_IP)"
[ "$ip" = "$EXPECTED_IP" ] || { echo "  FAIL: traffic leaves from $ip — fix the VM's external IP before anything else"; exit 1; }

echo "== step 2 · Scans"
scans="$($CURL "$SEED/api/scan/v0/scans" | jq -r '.scans[].scans[].publicUrl')" || {
  echo "  FAIL: the seed Scan $SEED itself refused us — not allowlisted by that SV yet"; exit 1; }
s_ok=0; s_all=0
for url in $scans; do
  s_all=$((s_all+1))
  if v="$($CURL "$url/api/scan/version" | jq -r '.version')"; then s_ok=$((s_ok+1)); echo "  ok      $url: $v"
  else echo "  BLOCKED $url"; fi
done

echo "== step 3 · sequencers"
seqs="$($CURL "$SEED/api/scan/v0/dso-sequencers" | jq -r '.domainSequencers[].sequencers[].url | sub("https://"; "")')"
q_ok=0; q_all=0
for url in $seqs; do
  q_all=$((q_all+1))
  if grpcurl --max-time 10 "$url:443" grpc.health.v1.Health/Check 2>/dev/null | grep -q SERVING; then
    q_ok=$((q_ok+1)); echo "  SERVING $url"
  else echo "  BLOCKED $url"; fi
done

echo "== verdict"
need() { echo $(( ($1 * 2 + 2) / 3 )); }   # ceil(2n/3)
echo "  scans      $s_ok / $s_all  (need $(need "$s_all"))"
echo "  sequencers $q_ok / $q_all  (need $(need "$q_all"))"
if [ "$s_all" -gt 0 ] && [ "$q_all" -gt 0 ] && [ "$s_ok" -ge "$(need "$s_all")" ] && [ "$q_ok" -ge "$(need "$q_all")" ]; then
  echo "  READY — request the 1-hour onboarding secret NOW and onboard: bash 04-onboard.sh"
  exit 0
fi
echo "  NOT READY — wait for the SVs (2–7 days after the request) or chase the sponsor. Do NOT request a secret yet."
exit 1
