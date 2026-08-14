#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
CLUSTER="${KIND_CLUSTER:-fh-local}"

if ! command -v kind >/dev/null; then
  echo "Instale kind: https://kind.sigs.k8s.io/docs/user/quick-start/"
  exit 1
fi
if ! command -v helm >/dev/null; then
  echo "Instale helm: https://helm.sh/docs/intro/install/"
  exit 1
fi

kind create cluster --name "$CLUSTER" --wait 60s || true
docker build -t financial-hub:1.0.0 "$ROOT/backend"
docker build -t notification-service:1.0.0 "$ROOT/services/notification-service"
kind load docker-image financial-hub:1.0.0 --name "$CLUSTER"
kind load docker-image notification-service:1.0.0 --name "$CLUSTER"
helm upgrade --install fh "$ROOT/infra/k8s/helm/financial-hub" --set notification.enabled=true
echo "Chart aplicado no Kind ($CLUSTER). Probes: /actuator/health/*"
echo "Nota: Postgres/Kafka/Oracle do dia a dia continuam no docker compose."
