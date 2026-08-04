# Projeto Banco — Financial Hub

Backend + frontend com **Spec-Driven Development (SDD)**.

## SDD

Specs em [`specs/`](specs/) são a fonte de verdade. Guia de agentes: [`AGENTS.md`](AGENTS.md).

| Camada | Pasta | Specs |
|--------|-------|--------|
| Compartilhado | — | [`constitution.md`](specs/constitution.md), [`workflow.md`](specs/workflow.md) |
| API Spring Boot | [`backend/`](backend/) | [`specs/backend/`](specs/backend/) |
| Web Next.js | [`web/`](web/) | [`specs/frontend/`](specs/frontend/) |

Índice completo: [`specs/README.md`](specs/README.md)

## Subir local

```bash
# Backend
cd backend/docker && docker compose up --build -d

# Frontend
cd web && npm install && npm run dev
```

- API: http://localhost:8080  
- Web: http://localhost:3000  
- Swagger: http://localhost:8080/swagger-ui.html
