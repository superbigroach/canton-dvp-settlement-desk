#!/usr/bin/env bash
# =============================================================================
# 01-create-vm.sh — network, secret, service account and the validator VM.
#
#   ./01-create-vm.sh              # DRY RUN: prints every gcloud command, changes nothing
#   EXECUTE=1 ./01-create-vm.sh    # creates what is missing (idempotent: existing = skipped)
#
# Creates, all in project crossdesk-devnet-app / us-central1:
#   VPC  crossdesk-devnet-vpc (custom mode — NOT the default network, whose default-allow-ssh
#        and default-allow-rdp rules are open to 0.0.0.0/0)
#   subnet crossdesk-validator-subnet 10.20.0.0/24  (the VM)
#   subnet crossdesk-run-egress       10.20.1.0/26  (Cloud Run Direct VPC egress; /26 minimum)
#   internal IP crossdesk-validator-internal-ip = 10.20.0.10
#   firewall: IAP range → tcp:22 (SSH only through IAP);
#             crossdesk-run-egress → tcp:5001 (gRPC Ledger API) + tcp:5012 (Scan proxy).
#             Nothing else inbound. No public ingress at all. Egress stays default-allow
#             (validators only need outbound 443 to the SVs).
#   secret crossdesk-ledger-hmac-secret — 32 random bytes, generated and piped straight into
#          Secret Manager; never printed, never on disk
#   SA crossdesk-validator-sa — log/metric writer + accessor on that one secret
#   VM crossdesk-validator — e2-standard-4, Debian 12, 100 GB pd-balanced, external IP =
#          the reserved static crossdesk-devnet-validator-ip (34.71.67.227) = our egress IP
#
# Machine size: README Phase 1 (Splice 0.8.1 "Validator Hardware Requirements").
# Cost: README "Cost" (~$115/month running 24/7; ~$17/month stopped).
# =============================================================================
# shellcheck source=lib/common.sh
. "$(dirname "$0")/lib/common.sh"

P=(--project "$CD_GCP_PROJECT")
REGION=us-central1
ZONE=us-central1-a
VPC=crossdesk-devnet-vpc
SUB_VM=crossdesk-validator-subnet;  SUB_VM_RANGE=10.20.0.0/24
SUB_RUN=crossdesk-run-egress;       SUB_RUN_RANGE=10.20.1.0/26
INTERNAL_IP_NAME=crossdesk-validator-internal-ip; INTERNAL_IP=10.20.0.10
EXTERNAL_IP_NAME=crossdesk-devnet-validator-ip;   EXPECTED_EXTERNAL_IP=34.71.67.227
VM=crossdesk-validator
MACHINE="${MACHINE:-e2-standard-4}"
DISK_GB="${DISK_GB:-100}"
SA_NAME=crossdesk-validator-sa
SA="$SA_NAME@$CD_GCP_PROJECT.iam.gserviceaccount.com"
SECRET=crossdesk-ledger-hmac-secret
RUN_SA="$(gcloud projects describe "$CD_GCP_PROJECT" --format='value(projectNumber)')-compute@developer.gserviceaccount.com"
TAG=crossdesk-validator

exists() { "$@" >/dev/null 2>&1; }
[ "${EXECUTE:-0}" = "1" ] || cd_warn "DRY RUN — nothing will be created. Re-run with EXECUTE=1."

cd_say "0 · the reserved egress IP is what we told the SVs"
ip="$(gcloud compute addresses describe "$EXTERNAL_IP_NAME" --region "$REGION" "${P[@]}" --format='value(address,status)')"
echo "  $EXTERNAL_IP_NAME = $ip"
[ "${ip%%[[:space:]]*}" = "$EXPECTED_EXTERNAL_IP" ] || cd_die "reserved IP is not $EXPECTED_EXTERNAL_IP"

cd_say "1 · APIs"
cd_run gcloud services enable compute.googleapis.com secretmanager.googleapis.com iap.googleapis.com "${P[@]}"

cd_say "2 · VPC, subnets, internal IP"
exists gcloud compute networks describe "$VPC" "${P[@]}" && cd_ok "$VPC exists" || \
  cd_run gcloud compute networks create "$VPC" --subnet-mode custom "${P[@]}"
exists gcloud compute networks subnets describe "$SUB_VM" --region "$REGION" "${P[@]}" && cd_ok "$SUB_VM exists" || \
  cd_run gcloud compute networks subnets create "$SUB_VM" --network "$VPC" --region "$REGION" \
    --range "$SUB_VM_RANGE" --enable-private-ip-google-access "${P[@]}"
exists gcloud compute networks subnets describe "$SUB_RUN" --region "$REGION" "${P[@]}" && cd_ok "$SUB_RUN exists" || \
  cd_run gcloud compute networks subnets create "$SUB_RUN" --network "$VPC" --region "$REGION" \
    --range "$SUB_RUN_RANGE" --enable-private-ip-google-access "${P[@]}"
exists gcloud compute addresses describe "$INTERNAL_IP_NAME" --region "$REGION" "${P[@]}" && cd_ok "$INTERNAL_IP_NAME exists" || \
  cd_run gcloud compute addresses create "$INTERNAL_IP_NAME" --region "$REGION" --subnet "$SUB_VM" \
    --addresses "$INTERNAL_IP" "${P[@]}"

cd_say "3 · firewall (ingress: IAP SSH + Cloud Run egress subnet only)"
exists gcloud compute firewall-rules describe crossdesk-allow-iap-ssh "${P[@]}" && cd_ok "iap-ssh exists" || \
  cd_run gcloud compute firewall-rules create crossdesk-allow-iap-ssh --network "$VPC" --direction INGRESS \
    --action ALLOW --rules tcp:22 --source-ranges 35.235.240.0/20 --target-tags "$TAG" "${P[@]}"
exists gcloud compute firewall-rules describe crossdesk-allow-run-ledger "${P[@]}" && cd_ok "run-ledger exists" || \
  cd_run gcloud compute firewall-rules create crossdesk-allow-run-ledger --network "$VPC" --direction INGRESS \
    --action ALLOW --rules tcp:5001,tcp:5012 --source-ranges "$SUB_RUN_RANGE" --target-tags "$TAG" "${P[@]}"
# Custom-mode VPCs have an implied deny-all-ingress, so nothing else can reach the VM.

cd_say "4 · the ledger HMAC secret (value never shown)"
if exists gcloud secrets describe "$SECRET" "${P[@]}"; then
  cd_ok "$SECRET exists (not rotating it — rotation: README Phase 3)"
else
  echo "  \$ openssl rand -hex 32 | gcloud secrets create $SECRET --data-file=- --replication-policy automatic ${P[*]}"
  if [ "${EXECUTE:-0}" = "1" ]; then
    openssl rand -hex 32 | tr -d '\n' | gcloud secrets create "$SECRET" --data-file=- --replication-policy automatic "${P[@]}"
  fi
fi

cd_say "5 · service accounts"
exists gcloud iam service-accounts describe "$SA" "${P[@]}" && cd_ok "$SA exists" || \
  cd_run gcloud iam service-accounts create "$SA_NAME" --display-name "CrossDesk DevNet validator VM" "${P[@]}"
for role in roles/logging.logWriter roles/monitoring.metricWriter; do
  cd_run gcloud projects add-iam-policy-binding "$CD_GCP_PROJECT" --member "serviceAccount:$SA" --role "$role" --condition None --quiet
done
for member in "serviceAccount:$SA" "serviceAccount:$RUN_SA"; do
  cd_run gcloud secrets add-iam-policy-binding "$SECRET" --member "$member" --role roles/secretmanager.secretAccessor "${P[@]}"
done

cd_say "6 · the VM"
if exists gcloud compute instances describe "$VM" --zone "$ZONE" "${P[@]}"; then
  cd_ok "$VM exists: $(gcloud compute instances describe "$VM" --zone "$ZONE" "${P[@]}" --format='value(status,machineType.basename(),networkInterfaces[0].accessConfigs[0].natIP)')"
else
  cd_run gcloud compute instances create "$VM" --zone "$ZONE" "${P[@]}" \
    --machine-type "$MACHINE" \
    --image-family debian-12 --image-project debian-cloud \
    --boot-disk-size "${DISK_GB}GB" --boot-disk-type pd-balanced --boot-disk-auto-delete=no \
    --network-interface "subnet=$SUB_VM,private-network-ip=$INTERNAL_IP_NAME,address=$EXTERNAL_IP_NAME,network-tier=PREMIUM" \
    --service-account "$SA" --scopes cloud-platform \
    --tags "$TAG" \
    --shielded-secure-boot --shielded-vtpm --shielded-integrity-monitoring \
    --metadata enable-oslogin=TRUE \
    --deletion-protection \
    --labels app=crossdesk,env=devnet,component=validator
fi

cd_say "7 · (opt-in) daily disk snapshots, 7-day retention — SNAPSHOTS=1"
if [ "${SNAPSHOTS:-0}" = "1" ]; then
  exists gcloud compute resource-policies describe crossdesk-validator-daily --region "$REGION" "${P[@]}" || \
    cd_run gcloud compute resource-policies create snapshot-schedule crossdesk-validator-daily --region "$REGION" \
      --daily-schedule --start-time 04:00 --max-retention-days 7 --on-source-disk-delete keep-auto-snapshots "${P[@]}"
  cd_run gcloud compute disks add-resource-policies "$VM" --zone "$ZONE" --resource-policies crossdesk-validator-daily "${P[@]}"
fi

cat <<EOF

Next:
  gcloud compute scp --recurse --tunnel-through-iap --zone $ZONE ${P[*]} \\
      "$CD_HERE" $VM:~/own-devnet-validator
  gcloud compute ssh $VM --zone $ZONE ${P[*]} --tunnel-through-iap -- \\
      'sudo bash ~/own-devnet-validator/02-bootstrap-node.sh'
EOF
