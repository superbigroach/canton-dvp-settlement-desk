#!/usr/bin/env bash
# =============================================================================
# Bring up a ledger and a desk in one process tree, in the order they depend on.
# =============================================================================
# The desk cannot start before the ledger answers, and the ledger cannot hold the
# demo book before the script has run. Doing this in the entrypoint rather than
# in three containers is deliberate: the sandbox is IN-MEMORY, so the ledger and
# the desk share a lifetime whether or not the topology admits it. Splitting them
# would produce a desk that outlives its own data and reports 503 exactly the way
# the hosted deployment does today.
#
# Everything here fails loudly. A demo that boots into a degraded state is worse
# than one that does not boot, because the first person to find out is a visitor.
set -euo pipefail

DAML="${DAML_SDK}/daml/daml"
LEDGER_PORT="${LEDGER_PORT:-6900}"
PORT="${PORT:-8080}"

log() { printf '[demo] %s\n' "$*"; }

# --- 1. the ledger --------------------------------------------------------
# bind-all.conf puts the Ledger API on 0.0.0.0. Inside a single container it
# would be reachable on loopback anyway, but the config is shared with the local
# runbook and a second binding costs nothing.
log "starting Canton sandbox on ${LEDGER_PORT}"
"${DAML}" sandbox --port "${LEDGER_PORT}" -c /app/bind-all.conf \
  --no-legacy-assistant-warning > /app/sandbox.log 2>&1 &
SANDBOX_PID=$!

# --- 2. wait for it, but not forever --------------------------------------
# Cloud Run gives a container a bounded startup window. Hanging here until it is
# killed produces a deploy that "fails" with no reason in the log, so the wait is
# capped and says what it saw.
log "waiting for the ledger"
for i in $(seq 1 120); do
  if grep -q "Canton sandbox is ready" /app/sandbox.log 2>/dev/null; then
    log "ledger ready after ${i}s"
    break
  fi
  if ! kill -0 "${SANDBOX_PID}" 2>/dev/null; then
    log "FATAL: the sandbox exited during startup"
    tail -40 /app/sandbox.log || true
    exit 1
  fi
  sleep 1
done

if ! grep -q "Canton sandbox is ready" /app/sandbox.log 2>/dev/null; then
  log "FATAL: the ledger did not report ready within 120s"
  tail -40 /app/sandbox.log || true
  exit 1
fi

# --- 3. the package, then the book ----------------------------------------
log "uploading crossdesk.dar"
"${DAML}" ledger upload-dar --host localhost --port "${LEDGER_PORT}" \
  /app/crossdesk.dar --no-legacy-assistant-warning

# Test:initialize allocates the parties and seeds the instruments and holdings a
# visitor lands on. Without it the desk is correct and completely empty, which
# reads as broken to anyone who did not write it.
log "seeding the demo book (Test:initialize)"
"${DAML}" script --ledger-host localhost --ledger-port "${LEDGER_PORT}" \
  --dar /app/crossdesk.dar --script-name Test:initialize \
  --no-legacy-assistant-warning

# --- 4. the desk ----------------------------------------------------------
# exec, so the JVM becomes PID 1 and receives Cloud Run's SIGTERM directly. A
# wrapper that swallows the signal turns every revision swap into a hard kill of
# the Canton JVM, which the local runbook records as bad enough to wedge a host.
log "starting the desk on ${PORT}"
exec java \
  -XX:MaxRAMPercentage=45 \
  -Dserver.port="${PORT}" \
  -DLEDGER_HOST=localhost \
  -DLEDGER_PORT="${LEDGER_PORT}" \
  -DLEDGER_TLS=false \
  -jar /app/app.jar
