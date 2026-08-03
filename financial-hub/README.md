# Financial Hub — Plataforma de Pagamentos Instantâneos

Backend de fintech P2P (estilo Pix) com Clean Architecture, Spring Boot 3, PostgreSQL, Kafka, Redis, Docker/K8s e Terraform.

> **SDD:** specs do backend em [`../specs/backend/`](../specs/backend/). Constituição e fluxo em [`../specs/`](../specs/). Veja [`../AGENTS.md`](../AGENTS.md).

## Stack

| Camada | Tecnologia |
|--------|------------|
| Linguagem | Java 17 |
| Framework | Spring Boot 3.2 |
| Arquitetura | Hexagonal / Clean Architecture |
| Persistência | PostgreSQL 16 + Flyway + JPA |
| Cache | Redis (TTL 5 min) |
| Mensageria | Apache Kafka |
| Segurança | Spring Security + JWT (BCrypt) |
| Cloud | AWS S3 (LocalStack) + Terraform |
| Container | Docker + docker-compose |
| Orquestração | Kubernetes (Deployment, HPA, Ingress) |
| Observabilidade | Micrometer + Prometheus + Zipkin |
| Resiliência | Resilience4j Circuit Breaker + Bucket4j |

## Como rodar localmente

### Pré-requisitos
- Docker e Docker Compose
- Java 17+ (opcional, para rodar fora do container)
- Maven 3.9+ (opcional)

### Subir todo o ambiente

```bash
cd financial-hub/docker
chmod +x localstack-init.sh
docker compose up --build -d
```

Serviços:
- **API**: http://localhost:8080
- **Swagger**: http://localhost:8080/swagger-ui.html
- **Prometheus metrics**: http://localhost:8080/actuator/prometheus
- **PostgreSQL**: localhost:5432
- **Redis**: localhost:6379
- **Kafka**: localhost:9092
- **LocalStack S3**: localhost:4566

### Fluxo rápido de teste

```bash
# 1. Criar usuário pagador
curl -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Alice Silva",
    "email": "alice@email.com",
    "document": "52998224725",
    "password": "senha123",
    "initialBalance": 2000.00
  }'

# 2. Criar usuário recebedor
curl -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Bob Santos",
    "email": "bob@email.com",
    "document": "39053344705",
    "password": "senha123",
    "initialBalance": 500.00
  }'

# 3. Login (CPF)
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"document":"52998224725","password":"senha123"}' | jq -r .accessToken)

# 4. Transferir por CPF
curl -X POST http://localhost:8080/api/v1/transactions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: transfer-001" \
  -d '{
    "payerDocument": "52998224725",
    "payeeDocument": "39053344705",
    "amount": 150.00
  }'
```

Coleção Postman: `postman/FinancialHub.postman_collection.json`

## Endpoints

| Método | Path | Descrição |
|--------|------|-----------|
| POST | `/api/v1/users` | Criar usuário |
| GET | `/api/v1/users/{document}` | Buscar usuário por CPF/CNPJ |
| GET | `/api/v1/users/{document}/balance` | Consultar saldo |
| GET | `/api/v1/users/{document}/transactions` | Extrato com saldos |
| POST | `/api/v1/auth/login` | Login por CPF/CNPJ + JWT |
| POST | `/api/v1/transactions` | Transferência (documentos) |
| GET | `/api/v1/transactions/{id}` | Status da transação |
| GET | `/api/v1/transactions/{id}/receipt` | Comprovante PDF |
| POST | `/api/v1/transactions/reverse` | Estorno |

## Arquitetura

```
┌─────────────┐     ┌──────────────┐     ┌─────────────┐
│   Client    │────▶│  REST API    │────▶│  Use Cases  │
│  (Postman)  │     │  Controllers │     │  (App)      │
└─────────────┘     └──────────────┘     └──────┬──────┘
                                                │
                    ┌───────────────────────────┼───────────────────────┐
                    ▼                           ▼                       ▼
             ┌────────────┐             ┌─────────────┐         ┌──────────┐
             │ PostgreSQL │             │    Kafka    │         │  Redis   │
             │ + Flyway   │             │  Events     │         │  Cache   │
             │ FOR UPDATE │             │  + DLQ      │         └──────────┘
             └────────────┘             └──────┬──────┘
                    ▲                          │
                    │              ┌───────────┼───────────┐
                    │              ▼           ▼           ▼
                    │         Balance     Notification  Daily
                    │         Consumer    Consumer      Report
                    │
             ┌────────────┐
             │  S3 / AWS  │  ← comprovantes PDF
             └────────────┘
```

Ver diagramas detalhados em [`docs/`](docs/).

## Regras de negócio

- Saldo nunca fica negativo (CHECK constraint + `SELECT FOR UPDATE`)
- Transferência apenas entre contas `ACTIVE`
- Limite diário configurável (default R$ 5.000)
- Idempotência via `Idempotency-Key`
- Timeout de 30s em `@Transactional`
- Estorno só de transferências `COMPLETED`

## Kafka — Eventos

| Tópico | Quando |
|--------|--------|
| `transaction.created` | Ao criar transferência |
| `transaction.completed` | Após saldo atualizado |
| `transaction.failed` | Em caso de erro |
| `transaction.dlq` | Dead Letter Queue |

Consumidores com idempotência (`processed_events`) e DLQ.

## Kubernetes

```bash
kubectl apply -f k8s/
```

Inclui: Deployment (3 réplicas), Service LoadBalancer, ConfigMap, Secret, HPA (CPU/memória), Ingress.

## Terraform (AWS)

```bash
cd terraform
terraform init
terraform plan -var="db_password=SUA_SENHA"
terraform apply -var="db_password=SUA_SENHA"
```

Provisiona: VPC, RDS PostgreSQL, S3 bucket, IAM role/policy.

## Testes

```bash
cd financial-hub
mvn test
```

- Unitários: JUnit 5 + Mockito (domínio e use cases)
- Validação de CPF
- Jacoco para cobertura

## Decisões arquiteturais

Ver [`docs/RELATORIO_TECNICO.md`](docs/RELATORIO_TECNICO.md).

### Consistência financeira
Stored procedure `transfer_balance` com `SELECT ... FOR UPDATE`, ordenando locks por UUID para evitar deadlock. Constraint `CHECK (balance >= 0)` como última linha de defesa.

### Kafka fora do ar
Publicação assíncrona (`whenComplete`); a transferência síncrona no banco já foi commitada. Eventos perdidos podem ser reprocessados via `transaction_audit`.

### Escalabilidade
HPA no K8s (3–10 pods), Kafka particionado por `transactionId`, Redis para aliviar leituras de saldo, pool HikariCP.

## Estrutura do projeto

```
financial-hub/
├── src/main/java/com/financialhub/
│   ├── application/       # Use cases e ports
│   ├── domain/            # Entities, enums, exceptions
│   ├── infrastructure/    # JPA, Kafka, Redis, AWS, Security
│   └── interfaces/        # REST controllers
├── src/main/resources/db/migration/
├── docker/
├── k8s/
├── terraform/
├── postman/
└── docs/
```
