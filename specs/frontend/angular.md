# Spec Frontend Angular — Financial Hub

**Status:** active  
**Stack:** Angular 19 (standalone components) + TypeScript  
**Código:** `web-angular/`  
**API:** `specs/backend/api/rest-v1.md`  
**Notificações:** `specs/backend/services/notification.md`

Esta é a **UI canônica para a vaga** (Java + Angular). O contrato de telas é o de [`overview.md`](overview.md).

## Rotas

| Path | Componente | Guard |
|------|------------|-------|
| `/` | Landing | público |
| `/login` | Login | público |
| `/register` | Cadastro | público |
| `/app` | Saldo | `authGuard` |
| `/app/transfer` | Transferência | `authGuard` |
| `/app/transactions` | Extrato | `authGuard` |

Rotas `/app/*` usam `AppShell` (nav Saldo / Transferir / Extrato / Sair).

## Serviços

| Serviço | Responsabilidade |
|---------|------------------|
| `AuthService` | login, logout, JWT no `localStorage`, `document` = `sub` |
| `ApiService` | HTTP para `/api/v1` (users, balance, transfer, reverse, PDFs, favorites) |
| `NotificationInboxService` | GET `http://localhost:8081/api/v1/notifications` |
| `Money` | máscara BRL (`1.234,56`) |
| `FavoritesService` | API Mongo + fallback `localStorage` |

## Ambiente

```ts
apiUrl: 'http://localhost:8080'
notificationUrl: 'http://localhost:8081'
```

## Build local

```bash
cd web-angular
npm install
npm start          # http://localhost:4200
npm run build
```

## Critérios extras (além de overview.md)

1. Standalone components; sem NgModules de feature
2. Interceptor HTTP adiciona `Authorization` quando houver token
3. Inbox de notificações na home (`/app`) se o notification-service estiver no ar; falha do inbox **não** bloqueia saldo
