# GKE deployment runbook — Canton DvP Settlement Desk (app tier)

Copy-paste steps to build the backend image, push it to Artifact Registry, and
run it on GKE — with Helm or with plain YAML. **Read the [cost + teardown](#cost--teardown)
section first: a running GKE cluster costs real money by the hour.**

> **Scope — read this.** This package deploys **your Spring Boot application
> tier** (the REST desk) and points it at a Canton **Ledger API** endpoint you
> already have. It does **not** stand up a Canton participant/synchronizer.
> Running a full **production Canton participant** is a separate, heavier, and
> **potentially license-gated** step (see [Ledger options](#the-ledger-the-desk-talks-to)).
> For a live demo you point the desk at a **Devnet participant** or a **sandbox** —
> no Daml Enterprise license needed.

---

## 0. Prerequisites

- `gcloud` (Google Cloud SDK), `kubectl`, `docker`, and — for the Helm path — `helm`.
- A Google Cloud billing account.
- The image builds from this repo (`backend/Dockerfile`); the DAR is already built.

Set shell variables once (edit these):

```bash
export PROJECT=my-dvp-demo               # a NEW or existing GCP project id
export REGION=us-central1
export CLUSTER=dvp-desk
export REPO=dvp                          # Artifact Registry repo name
export IMAGE="$REGION-docker.pkg.dev/$PROJECT/$REPO/canton-dvp-desk:1.0.0"
```

---

## 1. Auth + project + APIs

```bash
gcloud auth login
gcloud projects create "$PROJECT"                      # skip if it exists
gcloud config set project "$PROJECT"
# Link billing (find your account: `gcloud billing accounts list`):
# gcloud billing projects link "$PROJECT" --billing-account=XXXXXX-XXXXXX-XXXXXX

gcloud services enable \
  container.googleapis.com \
  artifactregistry.googleapis.com
```

## 2. Artifact Registry + build & push the image

```bash
gcloud artifacts repositories create "$REPO" \
  --repository-format=docker --location="$REGION" \
  --description="Canton DvP desk images"

gcloud auth configure-docker "$REGION-docker.pkg.dev"

# Build from the REPO ROOT (context includes backend/). Buildx targets linux/amd64
# so it runs on GKE nodes regardless of your laptop's arch.
docker buildx build --platform linux/amd64 \
  -f backend/Dockerfile -t "$IMAGE" --push .
```

(Alternatively, let Google build it: `gcloud builds submit --tag "$IMAGE" .`)

## 3. Create the cluster

**Autopilot (recommended — cheapest, you pay per pod, Google manages nodes):**

```bash
gcloud container clusters create-auto "$CLUSTER" --region="$REGION"
gcloud container clusters get-credentials "$CLUSTER" --region="$REGION"
```

**Standard (you manage the node pool) — a small, cheap pool instead:**

```bash
gcloud container clusters create "$CLUSTER" --region="$REGION" \
  --num-nodes=1 --machine-type=e2-small
gcloud container clusters get-credentials "$CLUSTER" --region="$REGION"
```

## 4. Deploy

### Option A — Helm

```bash
helm install dvp deploy/helm/canton-dvp-desk \
  --set image.repository="$REGION-docker.pkg.dev/$PROJECT/$REPO/canton-dvp-desk" \
  --set image.tag=1.0.0 \
  --set ledger.host=YOUR_PARTICIPANT_HOST \
  --set ledger.port=6865 \
  --set ledger.tls=true \
  --set ledger.jwt.value='YOUR_LEDGER_API_JWT'      # omit both jwt flags for a no-auth sandbox

# For a real token, prefer an out-of-band secret:
#   kubectl create secret generic dvp-jwt --from-literal=jwt='...'
#   helm install ... --set ledger.jwt.existingSecret=dvp-jwt
```

### Option B — plain YAML

Edit the image in `deploy/k8s/deployment.yaml` and the ledger host in
`deploy/k8s/configmap.yaml`, then:

```bash
kubectl apply -f deploy/k8s/configmap.yaml
kubectl apply -f deploy/k8s/secret.yaml        # skip for a no-auth sandbox
kubectl apply -f deploy/k8s/deployment.yaml
kubectl apply -f deploy/k8s/service.yaml
kubectl apply -f deploy/k8s/ingress.yaml       # optional
```

## 5. Reach the desk

Quickest (no public IP):

```bash
kubectl get pods -l app.kubernetes.io/name=canton-dvp-desk   # Helm
# kubectl get pods -l app=canton-dvp-desk                    # plain YAML
kubectl port-forward svc/dvp-canton-dvp-desk 8080:80
curl -s localhost:8080/api/health | jq
```

With an ingress (Helm `--set ingress.enabled=true --set ingress.host=...`, or the
plain `ingress.yaml`), GKE provisions an external L7 load balancer + IP in a few
minutes:

```bash
kubectl get ingress          # wait for ADDRESS to populate
curl -s http://<ADDRESS>/api/health | jq
```

Then drive a full DvP with the same `curl` calls as `backend/run-local.md`,
substituting the ingress/port-forward base URL.

---

## The ledger the desk talks to

`ledger.host:ledger.port` must be a **Canton Ledger API** (gRPC) endpoint:

| Option | Auth | License | Notes |
|---|---|---|---|
| **Daml sandbox** (in-cluster or reachable host) | none | none | Fine for a demo of the app tier; not a real network. |
| **Canton Network Devnet participant** | JWT (+TLS) | none | Real, networked settlement; get onboarding + a participant per this repo's `DEPLOY.md`. Point `ledger.host` at it. |
| **Full production Canton participant** | JWT + mTLS | **Daml Enterprise (may be license-gated)** | Running your own production participant/synchronizer is a separate, heavier deployment — out of scope for this app-tier package. |

Be honest in a demo: this package proves the **application** deploys and drives a
Canton ledger over the Ledger API. Standing up the participant itself is the
separate piece above.

---

## Cost + teardown

**GKE costs real money while it runs. Spin up, demo, tear down — don't leave it on.**

- **Cluster management fee:** ~**$0.10/hour (~$73/month)** per cluster for the
  control plane (one zonal cluster per billing account is free; a *regional*
  Autopilot/Standard cluster is charged).
- **Compute:** Autopilot bills per pod's CPU/memory requests; this desk
  (250m CPU / 512Mi) is a few dollars a month if left running. Standard bills the
  **nodes** whether busy or not — a 24/7 small pool is roughly **$150–300/month**.
- **Networking:** an external L7 load balancer (ingress) adds ~**$18/month** plus
  egress. Use `port-forward` for a quick demo to avoid it.

A short **demo session (spin up, show it, tear down within an hour or two) costs
a few dollars.** The meter only stops when the cluster is deleted.

**Tear it ALL down when done:**

```bash
# App only (keep the cluster):
helm uninstall dvp                      # or: kubectl delete -f deploy/k8s/

# The expensive part — DELETE THE CLUSTER to stop the meter:
gcloud container clusters delete "$CLUSTER" --region="$REGION"

# Optional: remove the image repo, and the project entirely:
gcloud artifacts repositories delete "$REPO" --location="$REGION"
gcloud projects delete "$PROJECT"       # nukes everything under the project
```

Verify nothing is still billing:

```bash
gcloud container clusters list          # should be empty
gcloud compute forwarding-rules list    # no stray load balancers
```
