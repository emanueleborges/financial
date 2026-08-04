# Contrato API REST v1

**Base path:** `/api/v1`  
**Content-Type:** `application/json`  
**Auth:** `Authorization: Bearer <accessToken>` (exceto onde marcado)

## Identidade pública: CPF/CNPJ

O **documento (CPF 11 ou CNPJ 14 dígitos)** é a chave pública de todas as operações de usuário.

| Conceito | Identificador |
|----------|----------------|
| Usuário (consulta, saldo, lista TX) | `{document}` no path |
| Transferência | `payerDocument` + `payeeDocument` |
| Sessão JWT | `sub` = documento; claim `userId` = UUID interno |
| Transação / estorno | `transactionId` (UUID da TX — não é pessoa) |

UUID de usuário permanece **apenas interno** (FK, auditoria). A API externa não exige UUID de usuário.

Documentos no path/body são normalizados (somente dígitos).

## Auth

### POST `/auth/login` — público
```json
// request
{ "document": "52998224725", "password": "senha123" }

// 200
{
  "accessToken": "...",
  "refreshToken": "...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

JWT access:
- `sub`: documento
- `userId`: UUID interno
- `email`: e-mail

## Users

### POST `/users` — público
```json
{
  "name": "Alice Silva",
  "email": "alice@email.com",
  "document": "52998224725",
  "password": "senha123",
  "initialBalance": 2000.00
}
```
- **201** `UserResponse` (nunca retornar `passwordHash`)
- **409** e-mail/documento duplicado
- **400** documento inválido / validação

### GET `/users/{document}` — autenticado
- **200** `UserResponse`
- **404** não encontrado

### GET `/users/{document}/balance` — autenticado
```json
{ "document": "52998224725", "balance": 1850.00, "version": 3 }
```

### GET `/users/{document}/transactions` — autenticado
Extrato de movimentações (mais recentes primeiro) com saldo após cada lançamento.

```json
{
  "document": "52998224725",
  "currentBalance": 1850.00,
  "entries": [
    {
      "transaction": { "...": "TransactionResponse" },
      "signedAmount": -150.00,
      "balanceAfter": 1850.00
    }
  ]
}
```

- `signedAmount`: negativo = saída; positivo = entrada (visão do `{document}`)
- `balanceAfter`: saldo da conta após o lançamento (`null` se PENDING/FAILED)
- Query opcional: `limit` (default 50, máx. 100)
- **403** se o JWT não for do próprio `{document}`
- **404** usuário inexistente

### GET `/users/{document}/transactions/export` — autenticado
- Retorna **PDF** do extrato (`application/pdf`)
- Colunas: Movimentação, Valor, Saldo, Quando
- Inclui titular, CPF/CNPJ e saldo atual
- Query opcional: `limit` (default 50, máx. 100)
- **200** arquivo `extrato-{document}.pdf`
- **403** se o JWT não for do próprio `{document}`

## Transactions

### POST `/transactions` — autenticado
**Headers opcionais:** `Idempotency-Key: <string>`

```json
{
  "payerDocument": "52998224725",
  "payeeDocument": "39053344705",
  "amount": 150.00,
  "idempotencyKey": "transfer-001"
}
```
- `payerDocument` deve coincidir com o documento do JWT
- **201** `TransactionResponse`
- **403** se `payerDocument` ≠ documento autenticado
- **422** regras de negócio
- **404** pagador/recebedor não encontrado

### GET `/transactions/{id}` — autenticado
- **200** status atual (com CPF/CNPJ e nome das partes)
- **404** não encontrado

### GET `/transactions/{id}/receipt` — autenticado
- Retorna **PDF** (`application/pdf`) do comprovante
- Conteúdo: ID, tipo, status, valor, data, CPF/CNPJ e nome do pagador/recebedor
- Gera sob demanda e também tenta persistir no S3
- **200** arquivo `comprovante-{id}.pdf`
- **403** se o JWT não for do pagador nem do recebedor
- **404** transação inexistente

### `TransactionResponse` (campos de identidade)
```json
{
  "payerDocument": "52998224725",
  "payeeDocument": "39053344705",
  "payerName": "Alice Silva",
  "payeeName": "Bob Santos"
}
```
UUID (`payerId`/`payeeId`) permanece interno na resposta, mas a UI deve privilegiar documento + nome.

### POST `/transactions/reverse` — autenticado
```json
{ "transactionId": "<uuid>", "reason": "Estorno solicitado" }
```
- Só o **pagador original** (documento do JWT) pode estornar
- **200** transação de estorno (`type=REVERSAL`)
- **403** se não for o pagador
- **422** não reversível / já estornada

## Error envelope

```json
{
  "code": "INSUFFICIENT_BALANCE",
  "message": "...",
  "timestamp": "2026-08-03T18:00:00Z",
  "path": "/api/v1/transactions",
  "fields": null
}
```

### Validação de campos (`VALIDATION_ERROR`)

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Um ou mais campos são inválidos",
  "timestamp": "2026-08-03T18:00:00Z",
  "path": "/api/v1/users",
  "fields": {
    "document": "Informe um CPF (11 dígitos) ou CNPJ (14 dígitos)"
  }
}
```

## Fora do contrato v1

Não adicionar endpoints sem atualizar esta spec e o Postman em `backend/postman/`.
