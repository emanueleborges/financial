# Qualidade — SonarQube / SonarCloud

Análise estática **não altera** saldo, ledger nem regras BR-*. PostgreSQL continua a fonte da verdade.

## Dois destinos

| Onde | Servidor | Como |
|------|----------|------|
| Máquina local | SonarQube Community (`:9000`) | Compose profile `sonar` + `./infra/sonar/scan-local.sh` |
| GitHub Actions | [SonarCloud](https://sonarcloud.io) | Job `sonar` em `.github/workflows/ci.yml` |

Os dashboards são **independentes**. O mesmo `sonar-project.properties` alimenta os dois; só mudam `sonar.host.url` e o token.

## Escopo analisado

- `backend/src/main/java` + JaCoCo (`backend/target/site/jacoco/jacoco.xml`)
- `services/notification-service/src/main/java` + JaCoCo
- `web-angular/src` (TypeScript; **fora da métrica de cobertura**; spec não exige testes de cobertura no Angular)

Meta de cobertura (JaCoCo / Quality Gate local): **≥ 60%** nas linhas Java da API e do notification-service.

## Local

```bash
cd backend/docker
docker compose --profile sonar up -d   # imagem sonarqube:community
# aguardar UP em http://localhost:9000 (primeiro boot: 1–3 min)
# login inicial: admin / admin — trocar senha e gerar um token

export SONAR_TOKEN=...   # token local (User → My Account → Security)
./infra/sonar/scan-local.sh
```

O script roda `mvn test` (JaCoCo) e o `sonar-scanner-cli` apontando para `http://host.docker.internal:9000`.

## GitHub Actions (SonarCloud)

Secrets / variables no repositório:

| Nome | Obrigatório | Uso |
|------|-------------|-----|
| `SONAR_TOKEN` | sim para o job publicar | token do SonarCloud |
| `SONAR_ORGANIZATION` (variable) | se diferente do default | org no SonarCloud |
| `SONAR_HOST_URL` (variable) | não | default `https://sonarcloud.io` |

Sem `SONAR_TOKEN`, o job **não falha o CI**: registra aviso e sai. Com token, o scan falha o job se o scanner falhar. Quality Gate no Actions é informativo (`continue-on-error`) até a baseline estabilizar.

Setup único no SonarCloud: importar o repo GitHub; `sonar.projectKey` deve coincidir com a chave do projeto (padrão `emanueleborges_financial`).

## O que não entra

- Quality Gate bloqueando merge sem baseline
- SonarQube self-hosted na nuvem
- Análise de `web/` (Next.js legado)
