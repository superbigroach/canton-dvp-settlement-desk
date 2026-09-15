#!/usr/bin/env bash
# =============================================================================
# 06-wire-backend.sh — point the desk at the participant. Same env set for both tracks.
#
#   CD_PROFILE=localnet   ./06-wire-backend.sh            # runs backend/ jar on this laptop
#   CD_PROFILE=own-devnet ./06-wire-backend.sh            # PRINTS the Cloud Run update (dry run)
#   CD_PROFILE=own-devnet EXECUTE=1 ./06-wire-backend.sh  # applies it — ENV VARS ONLY
#   CD_PROFILE=own-devnet EXECUTE=1 SWITCH_SITE=1 ./06-wire-backend.sh
#                                                         # + Firebase rewrite → crossdesk-devnet-api
#
# The env set (identical keys on both tracks — this IS the config-only switch):
#   LEDGER_HOST/PORT/TLS/AUTHORITY   where the participant's gRPC Ledger API is
#   LEDGER_AUTH_MODE=hmac            the desk mints its own HS256 token and re-mints every
#   LEDGER_TOKEN_SUBJECT/AUDIENCE    LEDGER_REFRESH_SECONDS — nothing to expire
#   LEDGER_HMAC_SECRET               LocalNet: its public dev value. Cloud Run: Secret Manager
#                                    reference (--set-secrets), never a literal env value
#   LEDGER_PARTIES                   the roster 05 wrote (.roster-<profile>)
#   DEMO_SEED_FUND/COMMITTEE=false, SCHEDULER_ENABLED=false   (sandbox-only behaviours OFF)
#
# own-devnet prerequisites (01-create-vm.sh made them): VPC crossdesk-devnet-vpc, subnet
# crossdesk-run-egress (Cloud Run Direct VPC egress), firewall allowing ONLY that subnet to
# tcp:5001 on the VM, and secretAccessor on crossdesk-ledger-hmac-secret for the service SA.
#
# The Firebase rewrite is switched ONLY with SWITCH_SITE=1, and only after
# 07-e2e-real-devnet.sh has passed against the Cloud Run URL. Rollback: README "Rollback".
# =============================================================================
# shellcheck source=lib/common.sh
. "$(dirname "$0")/lib/common.sh"
cd_load_profile

ROSTER_FILE="$CD_HERE/.roster-$CD_PROFILE"
[ -f "$ROSTER_FILE" ] || cd_die "no $ROSTER_FILE — run 05-upload-dar-and-parties.sh first"
ROSTER="$(cat "$ROSTER_FILE")"

COMMON_ENV=(
  "LEDGER_HOST=$CD_LEDGER_HOST"
  "LEDGER_PORT=$CD_LEDGER_PORT"
  "LEDGER_TLS=$CD_LEDGER_TLS"
  "LEDGER_AUTHORITY=$CD_LEDGER_AUTHORITY"
  "LEDGER_AUTH_MODE=hmac"
  "LEDGER_TOKEN_SUBJECT=$CD_BACKEND_USER"
  "LEDGER_APPLICATION_ID=$CD_BACKEND_USER"
  "LEDGER_TOKEN_AUDIENCE=$CD_TOKEN_AUDIENCE"
  "LEDGER_TOKEN_TTL_SECONDS=3600"
  "LEDGER_REFRESH_SECONDS=1200"
  "LEDGER_PARTIES=$ROSTER"
  "REGISTRY_REMOTE_URLS=${CD_REGISTRY_REMOTE_URLS:-}"
  "DEMO_SEED_FUND=false"
  "DEMO_SEED_COMMITTEE=false"
  "SCHEDULER_ENABLED=false"
)

if [ "$CD_PROFILE" = "localnet" ]; then
  JAR="$CD_REPO/backend/build/libs/canton-dvp-desk-1.0.0.jar"
  [ -f "$JAR" ] || cd_die "no $JAR — (cd backend && ./gradlew bootJar)"
  PORT="${CD_BACKEND_URL##*:}"
  if curl -sf -m 3 "$CD_BACKEND_URL/api/health" >/dev/null 2>&1; then
    cd_die "something already answers on $CD_BACKEND_URL — stop it first"
  fi
  DATA="${DATA_DIR:-$CD_HERE/.localnet-backend-data}"
  LOG="${BACKEND_LOG:-$CD_HERE/.localnet-backend.log}"
  mkdir -p "$DATA"
  cd_say "starting backend/ jar against LocalNet ($CD_LEDGER_HOST:$CD_LEDGER_PORT) → $LOG"
  (
    for kv in "${COMMON_ENV[@]}"; do export "${kv?}"; done
    export LEDGER_HMAC_SECRET="$(cd_secret)"
    export SERVER_PORT="$PORT" AUTH_MODE=sandbox DATA_DIR="$DATA"
    nohup java -jar "$JAR" >"$LOG" 2>&1 &
    echo $! > "$CD_HERE/.localnet-backend.pid"
  )
  for _ in $(seq 1 90); do
    curl -sf -m 2 "$CD_BACKEND_URL/api/health" >/dev/null 2>&1 && break
    sleep 2
  done
  curl -sf -m 5 "$CD_BACKEND_URL/api/health" | cd_json "d" || cd_die "backend did not come up — tail $LOG"
  cd_ok "pid $(cat "$CD_HERE/.localnet-backend.pid"); stop with: kill \$(cat $CD_HERE/.localnet-backend.pid)"
  exit 0
fi

# ------------------------------- own-devnet → Cloud Run ----------------------------------
SERVICE=crossdesk-devnet-api
REGION=us-central1
env_csv="$(IFS='|'; echo "${COMMON_ENV[*]}")"   # '|' delimiter: LEDGER_PARTIES contains commas

CMD=(gcloud run services update "$SERVICE"
  --project "$CD_GCP_PROJECT" --region "$REGION"
  --network crossdesk-devnet-vpc --subnet crossdesk-run-egress --vpc-egress private-ranges-only
  --update-env-vars "^|^$env_csv|AUTH_MODE=sandbox"
  # Noders-era credentials were plain env values on this service — remove them.
  --remove-env-vars "LEDGER_JWT,LEDGER_REFRESH_TOKEN,LEDGER_TOKEN_ENDPOINT,LEDGER_CLIENT_ID,Bank,Alice,Bob,Auditor,Venue,Agent"
  --update-secrets "LEDGER_HMAC_SECRET=${CD_HMAC_SECRET_REF#gsm:}:latest"
  --min-instances 1 --max-instances 1)

cd_say "Cloud Run update for $SERVICE (env vars + secret reference only; image unchanged)"
printf '  %q' "${CMD[@]}"; echo
echo
echo "  NOTE: the image must be a backend/ build carrying LEDGER_AUTH_MODE support (this commit)."
echo "        crossdesk-devnet-api currently runs a backend-devnet image — build & deploy backend/"
echo "        with: gcloud run deploy $SERVICE --source <dir with backend-devnet/cloudrun/Dockerfile + app.jar> ..."
echo "        before (or together with) this update. See README Phase 6."

if [ "${EXECUTE:-0}" != "1" ]; then
  echo; echo "  dry run — re-run with EXECUTE=1 to apply."; exit 0
fi
"${CMD[@]}"

URL="$(gcloud run services describe "$SERVICE" --project "$CD_GCP_PROJECT" --region "$REGION" --format='value(status.url)')"
cd_say "post-update checks against $URL"
curl -sS -m 30 "$URL/api/health" | cd_json "d"
curl -sS -m 60 "$URL/api/diag" | cd_json "{k: d[k] for k in ('status','ledger') if k in d}"

if [ "${SWITCH_SITE:-0}" = "1" ]; then
  cd_say "switching Firebase /api/** rewrite crossdesk-demo → $SERVICE"
  FB="$CD_REPO/frontend/firebase.json"
  "$CD_PY" - "$FB" "$SERVICE" <<'PY'
import json, sys
p, svc = sys.argv[1], sys.argv[2]
d = json.load(open(p, encoding='utf8'))
for r in d['hosting']['rewrites']:
    if r.get('source') == '/api/**':
        r['run']['serviceId'] = svc
json.dump(d, open(p, 'w', encoding='utf8'), indent=2)
PY
  (cd "$CD_REPO/frontend" && npm run build && firebase deploy --only hosting --project "$CD_GCP_PROJECT")
  cd_ok "site now rewrites /api/** to $SERVICE — scale crossdesk-demo to min 0 (README Cost controls)"
fi
