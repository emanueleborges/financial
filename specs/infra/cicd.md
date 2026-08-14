# CI/CD — GitHub Actions

Tudo roda em CI pública **sem conta AWS**. Homologação local usa Kind (Kubernetes) no runner ou na máquina do desenvolvedor.

## Workflows

| Arquivo | Quando | O que faz |
|---------|--------|-----------|
| `.github/workflows/ci.yml` | push / PR | `mvn test` (API + notification-service), `npm run build` (Angular), lint Helm, build imagens Docker, **SonarCloud** (se `SONAR_TOKEN`) |
| `.github/workflows/homolog.yml` | `workflow_dispatch` e push em `main` | build imagens, `helm lint` + `helm template`, sobe Kind e aplica o chart da API (smoke `/actuator/health` se o cluster tiver Postgres de teste) |

## Jobs do `ci.yml`

1. **backend** — JDK 17, `mvn -B test` em `backend/`
2. **notification** — JDK 17, `mvn -B test` em `services/notification-service/`
3. **angular** — Node 20, `npm ci && npm run build` em `web-angular/`
4. **images** — `docker build` da API, notification-service e Angular (sem push obrigatório)
5. **sonar** — JaCoCo + SonarScanner → SonarCloud (ver [`sonar.md`](sonar.md)); local usa SonarQube Community no compose

## Homologação

Não há deploy em ECS/EKS real neste repositório (roda local). Equivalente:

```bash
# imagens + Kind
./infra/k8s/kind-up.sh
```

Push de imagens para GHCR é opcional (`GHCR_PUSH=true` + `GITHUB_TOKEN`).

## O que não entra no CI

- Terraform contra AWS de produção
- `terraform apply` em LocalStack (feito no compose local)
- Quarkus / Payara / Jenkins
