# AGENTS.md — Guia para agentes de IA

Você está no repositório **projeto-banco** (Financial Hub).

## Metodologia: Spec-Driven Development (SDD)

1. **Specs primeiro** — leia e, se necessário, edite `specs/` antes de mudar código.
2. **Constituição** — `specs/constitution.md` não é negociável.
3. **Sem vibe coding** — não invente features, endpoints ou infra fora do escopo.
4. **Alinhamento** — ao terminar, código e specs devem contar a mesma história.
5. **Separação** — backend em `specs/backend/`, frontend em `specs/frontend/`, infra em `specs/infra/`.

## Mapa rápido

| Precisa de… | Abra… |
|-------------|--------|
| Princípios | `specs/constitution.md` |
| Processo SDD | `specs/workflow.md` |
| Escopo produto (API) | `specs/backend/product/overview.md` |
| Regras BR-* | `specs/backend/product/business-rules.md` |
| HTTP / REST | `specs/backend/api/rest-v1.md` |
| Schema/SQL | `specs/backend/data/schema.md` |
| Oracle (inbox) | `specs/backend/data/oracle.md` |
| Kafka | `specs/backend/events/kafka.md` |
| Camadas Java | `specs/backend/architecture/overview.md` |
| notification-service | `specs/backend/services/notification.md` |
| Web Angular (vaga) | `specs/frontend/angular.md` |
| Web Next.js | `specs/frontend/overview.md` |
| CI/CD | `specs/infra/cicd.md` |
| K8s / Helm | `specs/infra/kubernetes.md` |
| Terraform local | `specs/infra/terraform.md` |
| Código API | `backend/` |
| Código Angular | `web-angular/` |
| Código Next.js | `web/` |

## Comandos úteis

```bash
# Backend — testes
cd backend && mvn test

# notification-service
cd services/notification-service && mvn test

# Stack local (API, Angular, Oracle, Mongo, Kafka, LocalStack…)
cd backend/docker && docker compose up --build -d

# Angular (dev)
cd web-angular && npm install && npm start

# Next.js (alternativa)
cd web && npm install && npm run dev

# Terraform no LocalStack
cd infra/terraform/localstack && terraform init && terraform apply -auto-approve
```

## Resposta esperada ao implementar

- Citar qual spec foi seguida ou alterada (`backend/…`, `frontend/…` ou `infra/…`)
- Não expandir escopo “por precaução”
- Preferir mudanças mínimas alinhadas às BRs existentes
- **PostgreSQL continua o ledger**; Oracle/Mongo não guardam saldo
