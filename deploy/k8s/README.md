# Plain Kubernetes manifests (no Helm)

The same deployment as `deploy/helm/canton-dvp-desk`, as static YAML for anyone
not using Helm. Edit the two placeholders, then apply:

1. In `deployment.yaml`, set `image:` to your pushed image
   (`REGION-docker.pkg.dev/PROJECT/REPO/canton-dvp-desk:1.0.0`).
2. In `configmap.yaml`, set the ledger host/port/TLS to your participant.
3. For a real participant with auth, put the JWT in `secret.yaml` (or delete it
   and the `LEDGER_JWT` env block for a no-auth sandbox).
4. In `ingress.yaml`, set your host (or delete it and use `port-forward`).

```bash
kubectl apply -f deploy/k8s/configmap.yaml
kubectl apply -f deploy/k8s/secret.yaml        # skip for a no-auth sandbox
kubectl apply -f deploy/k8s/deployment.yaml
kubectl apply -f deploy/k8s/service.yaml
kubectl apply -f deploy/k8s/ingress.yaml       # optional

kubectl get pods -l app=canton-dvp-desk
kubectl port-forward svc/canton-dvp-desk 8080:80
curl localhost:8080/api/health
```
