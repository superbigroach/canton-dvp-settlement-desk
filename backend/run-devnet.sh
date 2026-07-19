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
export SERVER_PORT="8080"
# LEDGER_JWT is taken from the environment (your Keycloak access_token).

echo "Starting CrossDesk backend -> DEVNET ($LEDGER_HOST:$LEDGER_PORT, TLS on, JWT set)"
exec /usr/bin/java -jar build/libs/canton-dvp-desk-1.0.0.jar
