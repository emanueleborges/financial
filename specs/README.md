# Índice SDD — Specs

Fonte de verdade do **Financial Hub**. Código deve refletir estas specs.

## Organização

```
specs/
├── constitution.md      # Compartilhado — princípios não negociáveis
├── workflow.md          # Compartilhado — ciclo SDD
├── backend/             # API Spring Boot (financial-hub/)
│   ├── product/
│   ├── api/
│   ├── data/
│   ├── events/
│   └── architecture/
└── frontend/            # Web Next.js (web/)
    └── overview.md
```

## Compartilhado

| Spec | Caminho |
|------|---------|
| Constituição | [`constitution.md`](constitution.md) |
| Fluxo de trabalho | [`workflow.md`](workflow.md) |

## Backend (`financial-hub/`)

| Spec | Caminho |
|------|---------|
| Produto / capabilities | [`backend/product/overview.md`](backend/product/overview.md) |
| Regras de negócio BR-* | [`backend/product/business-rules.md`](backend/product/business-rules.md) |
| API REST v1 | [`backend/api/rest-v1.md`](backend/api/rest-v1.md) |
| Schema SQL | [`backend/data/schema.md`](backend/data/schema.md) |
| Kafka | [`backend/events/kafka.md`](backend/events/kafka.md) |
| Arquitetura | [`backend/architecture/overview.md`](backend/architecture/overview.md) |

## Frontend (`web/`)

| Spec | Caminho |
|------|---------|
| Web Next.js | [`frontend/overview.md`](frontend/overview.md) |

## Ordem de leitura para agentes de IA

1. `constitution.md`
2. Spec do lado afetado (`backend/…` ou `frontend/…`)
3. Se cruzar API ↔ UI, ler **ambos** os contratos (`backend/api` + `frontend`)
4. Atualizar a spec **antes** do código
5. Manter código ≡ spec
