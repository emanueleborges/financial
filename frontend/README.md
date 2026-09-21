# Frontends — Financial Hub

Código das UIs, separado do backend. Contrato de telas: [`specs/frontend/overview.md`](../specs/frontend/overview.md).

O caminho padrão é **Docker**, junto com a API e a infra:

```bash
docker compose up --build -d
```

| UI | Pasta | URL no Docker |
|----|-------|----------------|
| Angular (UI da vaga) | [`angular/`](angular/) | http://localhost:4200 |
| Next.js / React | [`next/`](next/) | http://localhost:3000 |

Dev sem container (API já no Docker):

```bash
cd frontend/angular && npm install && npm start
cd frontend/next && npm install && npm run dev
```
