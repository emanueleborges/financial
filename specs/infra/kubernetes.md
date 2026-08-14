# Kubernetes / Helm

Manifests em `backend/k8s/` (API) e `services/notification-service/k8s/`.  
Chart Helm em `infra/k8s/helm/financial-hub/`.

## Recursos

| Kind | Nome | Notas |
|------|------|-------|
| Deployment | `financial-hub` | 3 réplicas; probes Actuator (`/liveness`, `/readiness`, `/health`) |
| Service | `financial-hub-service` | porta 80 → 8080 |
| Deployment | `notification-service` | 1 réplica; porta 8081 |
| Service | `notification-service` | porta 80 → 8081 |
| ConfigMap / Secret | credenciais locais | **não** commitar segredos reais |
| HPA | CPU 70% / mem 80% | só API |
| Ingress | `financial-hub.local` | nginx |

## Probes (obrigatórias)

- liveness: `GET /actuator/health/liveness`
- readiness: `GET /actuator/health/readiness`
- startup: `GET /actuator/health`

## Local com Kind

```bash
./infra/k8s/kind-up.sh
```

O script cria o cluster, carrega as imagens locais e aplica o Helm chart.  
Postgres/Redis/Kafka/Oracle/Mongo do dia a dia continuam no Docker Compose; Kind demonstra Deployment + Service + probes.

## Helm

```bash
helm lint infra/k8s/helm/financial-hub
helm template fh infra/k8s/helm/financial-hub
```
