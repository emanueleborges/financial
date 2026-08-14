# Índice SDD — Specs

Fonte de verdade do **Financial Hub**. Código deve refletir estas specs.

## Organização

```
specs/
├── constitution.md      # Compartilhado — princípios não negociáveis
├── workflow.md          # Compartilhado — ciclo SDD
├── backend/             # API Spring Boot (backend/) + notification-service
│   ├── product/
│   ├── api/
│   ├── data/
│   ├── events/
│   ├── architecture/
│   └── services/
├── frontend/            # Angular (web-angular/) e Next.js (web/)
└── infra/               # CI/CD, K8s/Helm, Terraform local
```

## Compartilhado

| Spec | Caminho |
|------|---------|
| Constituição | [`constitution.md`](constitution.md) |
| Fluxo de trabalho | [`workflow.md`](workflow.md) |

## Backend (`backend/` + `services/`)

| Spec | Caminho |
|------|---------|
| Produto / capabilities | [`backend/product/overview.md`](backend/product/overview.md) |
| Regras BR-* | [`backend/product/business-rules.md`](backend/product/business-rules.md) |
| API REST v1 | [`backend/api/rest-v1.md`](backend/api/rest-v1.md) |
| Schema SQL (PostgreSQL) | [`backend/data/schema.md`](backend/data/schema.md) |
| Oracle (notification-service) | [`backend/data/oracle.md`](backend/data/oracle.md) |
| Kafka | [`backend/events/kafka.md`](backend/events/kafka.md) |
| Arquitetura | [`backend/architecture/overview.md`](backend/architecture/overview.md) |
| notification-service | [`backend/services/notification.md`](backend/services/notification.md) |

## Frontend

| Spec | Caminho |
|------|---------|
| Visão geral (Angular + Next.js) | [`frontend/overview.md`](frontend/overview.md) |
| Angular (UI da vaga) | [`frontend/angular.md`](frontend/angular.md) |

## Infra (local)

| Spec | Caminho |
|------|---------|
| CI/CD GitHub Actions | [`infra/cicd.md`](infra/cicd.md) |
| SonarQube / SonarCloud | [`infra/sonar.md`](infra/sonar.md) |
| Kubernetes / Helm | [`infra/kubernetes.md`](infra/kubernetes.md) |
| Terraform + LocalStack | [`infra/terraform.md`](infra/terraform.md) |

## Ordem de leitura para agentes de IA

1. `constitution.md`
2. Spec do lado afetado (`backend/…`, `frontend/…` ou `infra/…`)
3. Se cruzar API ↔ UI, ler **ambos** os contratos (`backend/api` + `frontend`)
4. Atualizar a spec **antes** do código
5. Manter código ≡ spec
