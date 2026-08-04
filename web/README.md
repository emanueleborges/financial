# Financial Hub — Web (Next.js)

Frontend conforme [`specs/frontend/overview.md`](../specs/frontend/overview.md).  
Contrato da API: [`specs/backend/api/rest-v1.md`](../specs/backend/api/rest-v1.md).

## Pré-requisitos

- Backend em `http://localhost:8080` (Docker ou local)
- Node 20+

## Subir

```bash
cd web
npm install
npm run dev
```

Abra http://localhost:3000

Variável: `NEXT_PUBLIC_API_URL` (default em `.env.local`).

## Fluxo

1. Criar conta (`/register`) — CPF válido, ex.: `52998224725`
2. Login automático → `/app` (saldo)
3. Transferir com UUID do recebedor
4. Consultar TX e estornar se `COMPLETED`

## CORS

O backend precisa permitir `http://localhost:3000`. Já configurado em `SecurityConfig` + `app.cors.allowed-origins`.

Após alterar o backend:

```bash
cd backend/docker
docker compose up --build -d app
```
