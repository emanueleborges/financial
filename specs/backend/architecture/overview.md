# Arquitetura

## Estilo

Clean Architecture / Hexagonal no módulo `financial-hub`.

```
interfaces  ──▶  application  ──▶  domain
                      ▲
infrastructure ───────┘ (implementa ports)
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

## Infra obrigatória (local)

Compose em `financial-hub/docker/`:
- app, postgres, redis, zookeeper, kafka, localstack

K8s em `financial-hub/k8s/`: Deployment(3), Service, ConfigMap, Secret, HPA, Ingress.

Terraform em `financial-hub/terraform/`: RDS, S3, IAM.

## Mudanças arquiteturais

Qualquer mudança de estilo (CQRS, Saga, Event Sourcing) exige:
1. Atualizar esta spec + `specs/constitution.md`
2. ADR novo em `specs/backend/architecture/decisions.md` (criar se necessário)
3. Só então implementar
