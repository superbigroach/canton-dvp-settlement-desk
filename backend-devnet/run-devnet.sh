#!/usr/bin/env bash
# Run the CrossDesk backend against the HackCanton shared DEVNET node (NODERS).
# ---------------------------------------------------------------------------
# Prereqs:
#   1. Kiryl has uploaded the DAR to the node (admin-only step).
#   2. You have a JWT bearer token (see get-devnet-token.sh / the curl below).
#
# Endpoints (from the hackathon Materials tab, hackcanton-01 devnet node):
#   gRPC Ledger API : ledger-api-grpc.participant.hackcanton-01.devnet.naas.noders.services:443  (TLS)
#   Logs (Grafana)  : https://grafana.participant.hackcanton-01.devnet.naas.noders.services
#
# Usage:
#   export LEDGER_JWT="<paste your access_token>"
#   ./run-devnet.sh
# ---------------------------------------------------------------------------
set -euo pipefail

if [ -z "${LEDGER_JWT:-}" ]; then
  echo "ERROR: LEDGER_JWT is not set. Get a token first (see the curl in this repo), then:"
  echo "  export LEDGER_JWT=\"<access_token>\" && ./run-devnet.sh"
  exit 1
fi

export LEDGER_HOST="ledger-api-grpc.participant.hackcanton-01.devnet.naas.noders.services"
export LEDGER_PORT="443"
export LEDGER_TLS="true"
# Default to 8090 so this devnet backend can run ALONGSIDE the local (2.9.4)
# backend, which owns 8080. Override with SERVER_PORT=... if you like.
export SERVER_PORT="${SERVER_PORT:-8090}"
# LEDGER_JWT is taken from the environment (your Keycloak access_token).

# DEVNET party roster (allocated by NODERS for CrossDesk; the ::1220… namespace
# suffix is identical across all five). The 3.x bindings have no party-management
# admin service, so we supply the roster to the backend here.
SUFFIX="122003aa7c491e00a453145c4d2cd3dbf5db8908b4e663c9944baed57fd66effa668"
export LEDGER_PARTIES="Issuer=issuer-crossdesk::$SUFFIX,Bank=bank-crossdesk::$SUFFIX,Alice=alice-crossdesk::$SUFFIX,Bob=bob-crossdesk::$SUFFIX,Auditor=auditor-crossdesk::$SUFFIX"

echo "Starting CrossDesk backend -> DEVNET ($LEDGER_HOST:$LEDGER_PORT, TLS on, JWT set)"
exec /usr/bin/java -jar build/libs/canton-dvp-desk-1.0.0.jar
