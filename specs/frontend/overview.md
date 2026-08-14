# Spec Frontend — Financial Hub Web

**Status:** active  
**UI da vaga:** Angular 19 (`web-angular/`) — [`angular.md`](angular.md)  
**Alternativa:** Next.js 15 (`web/`)  
**API:** `specs/backend/api/rest-v1.md` → código `backend/`  
**Notificações:** `specs/backend/services/notification.md` → `services/notification-service/`

## Objetivo

Interface web para o cliente final operar a plataforma P2P: cadastrar-se, autenticar, consultar saldo, transferir e estornar.

Há **duas UIs** com o mesmo contrato de telas. A vaga pede Angular; Next.js permanece como implementação equivalente.

| UI | Pasta | Porta | Spec |
|----|-------|-------|------|
| Angular (canônica para a vaga) | `web-angular/` | `:4200` | [`angular.md`](angular.md) |
| Next.js (alternativa) | `web/` | `:3000` | este arquivo (escopo v1) |

## Identidade

**CPF/CNPJ é a chave em toda a UI** (alinhado à API):

| Ação | Identificador |
|------|----------------|
| Login | documento + senha |
| Sessão | JWT `sub` = documento |
| Saldo / perfil / lista TX | documento da sessão |
| Transferência | CPF do recebedor (+ pagador = sessão) |
| Estorno | seleciona TX da lista (UUID interno da TX) |

A UI **não** usa UUID de usuário.

## Escopo v1 (ambas as UIs)

| Tela | Rota | Auth | Capacidades |
|------|------|------|-------------|
| Landing + entrada | `/` | público | Brand + CTAs login/cadastro |
| Cadastro | `/register` | público | criar usuário |
| Login | `/login` | público | JWT por **CPF/CNPJ** |
| Home / saldo | `/app` | JWT | saldo do documento logado |
| Transferir | `/app/transfer` | JWT | favoritos ou novo CPF/CNPJ; identificar recebedor antes de confirmar |
| Transações / Extrato | `/app/transactions` | JWT | extrato com saldos + exportar PDF + estorno + favoritar + comprovante |

## Favoritos

- Persistidos via API Mongo (`GET/POST/DELETE /users/{document}/favorites`) quando o backend estiver com `FAVORITES_STORE=mongo`
- Fallback em `localStorage` (`fh_favorites_{document}`) se a API de favoritos falhar
- Na transferência: escolher favorito **ou** informar novo CPF/CNPJ
- Em transações: favoritar pagador/recebedor da TX selecionada

## Fora de escopo (frontend v1)

- Paginação avançada / filtros complexos
- Busca de usuário por e-mail
- Refresh token automático em background
- PWA / mobile nativo
- Admin / multi-tenancy

## Contrato com o backend

- Base URL API: `API_URL` / `NEXT_PUBLIC_API_URL` (default `http://localhost:8080`)
- Base URL notificações: `NOTIFICATION_URL` (default `http://localhost:8081`) — só Angular lista o inbox
- Auth: header `Authorization: Bearer <accessToken>`
- Transferência: header `Idempotency-Key` gerado no cliente (UUID)
- Erros: envelope `{ code, message, timestamp, path, fields? }`
- Sessão: `document` = claim `sub` do JWT

## CORS

Backend deve permitir origens `http://localhost:3000` (Next) e `http://localhost:4200` (Angular).  
notification-service deve permitir `http://localhost:4200`.

## UX / Design

- Marca **Financial Hub** como sinal principal nas telas públicas
- Visual fintech sóbrio (ink/teal + accent âmbar)
- Tipografia expressiva (não Inter/Roboto/Arial/system default)
- Formulários com feedback claro de erros de negócio (BR-*)
- Campos monetários no formato brasileiro (`1.234,56` / `R$`)
- Mobile-first responsivo

## Critérios de aceite

1. Fluxo completo: cadastro → login (CPF) → saldo → transferir (CPF) → listar → estornar
2. Token persistido; rotas `/app/*` protegidas
3. Nenhum fluxo de usuário depende de UUID de conta
4. Transferência identifica o recebedor (nome) ou bloqueia se CPF/CNPJ não existir
5. Transferência permite selecionar favorito ou incluir novo CPF/CNPJ
6. Transações permitem favoritar CPF/CNPJ da contraparte
7. Cada transação permite exportar comprovante em PDF
8. Erros `VALIDATION_ERROR` com `fields` no campo correspondente
9. Next.js: `npm run dev` em `:3000` com API em `:8080`
10. Angular: `ng serve` em `:4200` com API em `:8080`
