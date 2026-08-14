#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"

HOST_URL="${SONAR_HOST_URL:-http://host.docker.internal:9000}"
TOKEN="${SONAR_TOKEN:-}"

if [[ -z "$TOKEN" && -f "$ROOT/.sonar-local-token" ]]; then
  TOKEN="$(tr -d '\n' < "$ROOT/.sonar-local-token")"
fi

if [[ -z "$TOKEN" ]]; then
  echo "Defina SONAR_TOKEN (token do SonarQube em http://localhost:9000) ou grave em .sonar-local-token"
  echo "Primeiro acesso: admin / admin — troque a senha e gere um token em My Account → Security."
  exit 1
fi

echo "==> Testes + JaCoCo (API)"
(cd backend && mvn -B test)

echo "==> Testes + JaCoCo (notification-service)"
(cd services/notification-service && mvn -B test)

echo "==> SonarScanner → ${HOST_URL}"
docker run --rm \
  -e SONAR_HOST_URL="${HOST_URL}" \
  -e SONAR_TOKEN="${TOKEN}" \
  -v "${ROOT}:/usr/src" \
  -w /usr/src \
  sonarsource/sonar-scanner-cli:11

echo "Dashboard local: http://localhost:9000"
