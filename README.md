# Financial Hub

Plataforma P2P (estilo Pix): saldo consistente no PostgreSQL, eventos no Kafka, UI em Angular. O repositório usa **Spec-Driven Development** — a fonte da verdade é [`specs/`](specs/).

## Stack implementada

### Backend (API `backend/`)

| Área | Tecnologia |
|------|------------|
| Linguagem / build | Java 17, Maven |
| Framework | Spring Boot **3.2.5** (Web, Data JPA, Security, Validation, Actuator) |
| Arquitetura | Hexagonal / Clean Architecture (`domain` → `application` → `infrastructure` / `interfaces`) |
| API | REST `/api/v1`, OpenAPI / Swagger (`/swagger-ui.html`) |
| Auth | JWT (JJWT) + BCrypt; CORS para `:4200` e `:3000` |
| Ledger | PostgreSQL 16, Flyway, procedure `transfer_balance` (`SELECT FOR UPDATE`) |
| Cache | Redis (cache-aside de saldo, TTL ≤ 5 min) |
| NoSQL | MongoDB — favoritos (`FAVORITES_STORE=mongo`) |
| Mensageria | Apache Kafka (tópicos + DLQ + consumidores idempotentes) |
| Cloud (local) | AWS SDK v2 S3 via **LocalStack** |
| Resiliência | Resilience4j (circuit breaker), Bucket4j (rate limit) |
| PDF | OpenPDF (comprovante / extrato) |
| Observabilidade | Micrometer + Prometheus, Zipkin, logs JSON (Logstash encoder) |
| Testes | JUnit 5, Mockito, Testcontainers (Postgres/Kafka), JaCoCo (meta 70%) |

### Microsserviço (`services/notification-service/`)

| Área | Tecnologia |
|------|------------|
| Runtime | Spring Boot 3.2, Java 17, Maven |
| Integração | Consome `transaction.completed` / `transaction.failed` |
| Persistência | **Oracle Free 23** (inbox; **não** é ledger) |
| API | `GET /api/v1/notifications` (JWT compartilhado com a API) |
| Porta | `8081` |

### Frontends

| UI | Pasta | Stack | Porta |
|----|-------|-------|-------|
| **Angular (UI da vaga)** | `web-angular/` | Angular 19, standalone, TypeScript | `4200` |
| Next.js (alternativa) | `web/` | Next.js 15, React 19, TypeScript | `3000` |

Telas: login, cadastro, saldo, transferência, extrato (PDF, estorno, favoritos).

### Infra local

| Área | Tecnologia |
|------|------------|
| Containers | Docker Compose (`backend/docker/`) |
| Orquestração | Kubernetes manifests + Helm (`infra/k8s/helm/`) + Kind (`infra/k8s/kind-up.sh`) |
| IaC | Terraform: LocalStack em `infra/terraform/localstack/` (S3 + IAM); módulo AWS real opcional em `backend/terraform/` |
| CI/CD | GitHub Actions (`.github/workflows/ci.yml`, `homolog.yml`) |
| Job | Python 3.12 (`jobs/daily-report`) — Kafka → relatório JSON no S3 |

### Observabilidade no compose

Prometheus `:9090` · Grafana `:3001` (admin/admin) · Zipkin `:9411`

## Técnicas / padrões

- Spec-first (SDD) e constituição em `specs/constitution.md`
- Saldo nunca negativo: domínio + CHECK SQL + lock pessimista
- Transferência **síncrona** no banco; Kafka só para side-effects
- Idempotência de transferência (`Idempotency-Key`) e de consumidores Kafka
- JWT stateless; documento (CPF/CNPJ) como chave pública da API
- Controllers sem regra de negócio (use cases)
- Health probes Actuator (`liveness` / `readiness`) para K8s
- Bounded contexts: API (ledger) ≠ notification-service (inbox)

## Mapa do repositório

| Pasta | Função |
|-------|--------|
| `backend/` | API hexagonal, Flyway, Docker, k8s da API, Postman |
| `services/notification-service/` | Microsserviço Oracle + Kafka |
| `web-angular/` | Angular 19 |
| `web/` | Next.js 15 |
| `jobs/daily-report/` | Job Python |
| `infra/` | Helm, Kind, Terraform LocalStack |
| `specs/` | Contratos SDD |
| `.github/workflows/` | CI e homologação |

## Subir local

Pré-requisitos: Docker Desktop rodando, **≥ 10 GB livres no disco**, Java 17, Node 20+, Maven. Terraform (opcional): `brew tap hashicorp/tap && brew install hashicorp/tap/terraform`.

O compose padrão **não** baixa Oracle (~1 GB) nem Grafana/Zipkin. Angular roda em `npm start`.

```bash
cd backend/docker
docker compose up --build -d
```

Sobe: Postgres, Redis, Kafka, LocalStack, Mongo e a API (`:8080`).

```bash
# Angular em dev (API já em :8080)
cd web-angular && npm install && npm start
```

Perfis extras (quando houver disco):

```bash
cd backend/docker
docker compose --profile oracle up -d    # Oracle + notification-service :8081
docker compose --profile obs up -d       # Prometheus, Grafana, Zipkin
docker compose --profile ui up --build -d
docker compose --profile jobs up --build -d
# tudo:
docker compose --profile oracle --profile obs --profile ui --profile jobs up --build -d
```

| Serviço | URL |
|---------|-----|
| Angular (`npm start` ou profile `ui`) | http://localhost:4200 |
| API | http://localhost:8080 |
| Swagger | http://localhost:8080/swagger-ui.html |
| notification-service (profile `oracle`) | http://localhost:8081 |
| Grafana (profile `obs`) | http://localhost:3001 |
| Prometheus (profile `obs`) | http://localhost:9090 |
| Zipkin (profile `obs`) | http://localhost:9411 |
| LocalStack | http://localhost:4566 |

```bash
cd backend && mvn test
cd services/notification-service && mvn test

cd infra/terraform/localstack && terraform init && terraform apply -auto-approve
```

Se o Docker falhar com `input/output error` no pull: o disco está cheio ou o store do Docker corrompeu. Liberar espaço, **Quit Docker Desktop**, e em Settings → Troubleshoot → **Clean / Purge data**. Depois `docker compose up --build -d` de novo (sem `--profile oracle` até ter ~15 GB livres).

## O que isto cobre numa vaga pleno Java / full stack

**Dá para citar:** Java, Spring Boot, Spring Data/Security, REST, hexagonal, PostgreSQL, Oracle, MongoDB, Redis, Kafka, Angular, Docker, Kubernetes/Helm, Terraform, AWS SDK (S3) + LocalStack, Maven, JUnit/Mockito, GitHub Actions, Prometheus/Grafana.

**Não é produção AWS:** sem ECS/EKS/RDS reais; cloud é laboratório (LocalStack + Terraform). Sem Quarkus, Payara ou Jenkins neste repo.
