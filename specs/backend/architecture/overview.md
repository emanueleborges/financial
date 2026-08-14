# Arquitetura

## Estilo

Clean Architecture / Hexagonal no módulo `backend`. O `notification-service` é um segundo deployable (mesmo estilo, bounded context de notificação).

```
interfaces  ──▶  application  ──▶  domain
                      ▲
    infrastructure ───────┘ (implementa ports)
```

```
financial-hub (8080)  --Kafka-->  notification-service (8081)
       | PostgreSQL + Redis + Mongo                          | Oracle
       | S3 (LocalStack)
```

## Pacotes

| Pacote | Responsabilidade | Pode depender de |
|--------|------------------|------------------|
| `domain` | Modelos, enums, exceções de negócio | nada externo |
| `application.port.in` | Use cases (comandos) | domain |
| `application.port.out` | Ports de saída | domain |
| `application.service` | Orquestração | ports + domain |
| `infrastructure.*` | Adapters | application ports + domain |
| `interfaces.*` | HTTP, filters | application ports + DTOs |

## Decisões (ADR resumido)

| ID | Decisão | Motivo |
|----|---------|--------|
| ADR-001 | Sync transfer + async events | Cliente recebe confirmação com ACID no saldo |
| ADR-002 | FOR UPDATE na procedure | Evita saldo negativo sob concorrência |
| ADR-003 | Cache-aside Redis | Performance de leitura; eviction em write |
| ADR-004 | JWT stateless | Escala horizontal sem sticky session |
| ADR-005 | LocalStack S3 no compose | Dev sem conta AWS |
| ADR-006 | notification-service separado | Microsserviço real; group `notification-group` |
| ADR-007 | Mongo só para favoritos | NoSQL sem tocar no ledger |
| ADR-008 | Oracle só no inbox | Atende “PostgreSQL e Oracle” sem mover o saldo |

## Infra obrigatória (local)

Compose em `backend/docker/`:
- app, notification, postgres, oracle, mongo, redis, zookeeper, kafka, localstack, daily-report, angular, prometheus, grafana, zipkin

K8s: `backend/k8s/` + Helm `infra/k8s/helm/financial-hub/` (Deployment, Service, probes Actuator).

Terraform: `backend/terraform/` (AWS real, opcional) e `infra/terraform/localstack/` (S3 + IAM local).

## Mudanças arquiteturais

Qualquer mudança de estilo (CQRS, Saga, Event Sourcing) exige:
1. Atualizar esta spec + `specs/constitution.md`
2. ADR novo em `specs/backend/architecture/decisions.md` (criar se necessário)
3. Só então implementar
