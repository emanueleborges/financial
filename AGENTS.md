# AGENTS.md — Guia para agentes de IA

Você está no repositório **projeto-banco** (Financial Hub).

## Metodologia: Spec-Driven Development (SDD)

1. **Specs primeiro** — leia e, se necessário, edite `specs/` antes de mudar código.
2. **Constituição** — `specs/constitution.md` não é negociável.
3. **Sem vibe coding** — não invente features, endpoints ou infra fora do escopo.
4. **Alinhamento** — ao terminar, código e specs devem contar a mesma história.
5. **Separação** — backend em `specs/backend/`, frontend em `specs/frontend/`.

## Mapa rápido

| Precisa de… | Abra… |
|-------------|--------|
| Princípios | `specs/constitution.md` |
| Processo SDD | `specs/workflow.md` |
| Escopo produto (API) | `specs/backend/product/overview.md` |
| Regras BR-* | `specs/backend/product/business-rules.md` |
| HTTP / REST | `specs/backend/api/rest-v1.md` |
| Schema/SQL | `specs/backend/data/schema.md` |
| Kafka | `specs/backend/events/kafka.md` |
| Camadas Java | `specs/backend/architecture/overview.md` |
| Web Next.js | `specs/frontend/overview.md` |
| Desafio original (histórico) | `DESAFIO.MD` |
| Código API | `backend/` |
| Código Web | `web/` |

## Comandos úteis

```bash
# Backend — testes
cd backend && mvn test

# Backend — stack local
cd backend/docker && docker compose up --build -d

# Frontend
cd web && npm install && npm run dev
```

## Resposta esperada ao implementar

- Citar qual spec foi seguida ou alterada (`backend/…` ou `frontend/…`)
- Não expandir escopo “por precaução”
- Preferir mudanças mínimas alinhadas às BRs existentes
