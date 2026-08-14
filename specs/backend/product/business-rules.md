# Regras de Negócio

**ID prefix:** BR-  
**Camada de enforcement:** domain + application + DB constraints

## BR-001 — Saldo não negativo
- **Dado** um usuário com saldo `S`
- **Quando** debitar valor `A`
- **Então** só permite se `S >= A`; caso contrário `INSUFFICIENT_BALANCE`
- **Enforce:** domínio `User.debit`, procedure `transfer_balance`, `CHECK (balance >= 0)`

## BR-002 — Contas ativas
- Transferência só entre `status = ACTIVE`
- Conta `INACTIVE` ou `BLOCKED` → `INACTIVE_ACCOUNT`

## BR-003 — Partes distintas
- `payerId != payeeId`
- Violação → `INVALID_TRANSACTION`

## BR-004 — Valor positivo
- `amount > 0`
- Violação → `INVALID_TRANSACTION` / validação Bean Validation (`@DecimalMin("0.01")`)

## BR-005 — Limite diário
- Soma de transferências do pagador no dia UTC com status `PENDING|PROCESSING|COMPLETED` e type `TRANSFER`
- Se `gastoHoje + amount > dailyLimit` → `DAILY_LIMIT_EXCEEDED`
- Default `dailyLimit = 5000.00` (config: `app.transaction.daily-limit`)

## BR-006 — Idempotência de transferência
- Se `idempotency_key` já existir, retornar a transação existente sem novo débito
- Aceitar via body ou header `Idempotency-Key`

## BR-007 — Ciclo de vida da transação
```
PENDING → PROCESSING → COMPLETED
                    ↘ FAILED
COMPLETED → REVERSED (via estorno)
```
- Transições inválidas → `INVALID_TRANSACTION`

## BR-008 — Estorno
- Somente `type=TRANSFER` e `status=COMPLETED`
- Não permitir segundo estorno (`existsReversalFor`)
- Estorno cria nova TX `type=REVERSAL` invertendo payer/payee
- Original passa a `REVERSED`

## BR-009 — Unicidade de usuário
- `email` único (case-insensitive no cadastro)
- `document` único (somente dígitos)
- Duplicata → `DUPLICATE_RESOURCE` (HTTP 409)

## BR-010 — Documento válido
- CPF (11) ou CNPJ (14) com dígitos verificadores
- Inválido → `INVALID_DOCUMENT`
- Chamada externa mockada com Circuit Breaker `documentValidation`

## BR-011 — Timeout
- Operações de transferência/estorno: `@Transactional(timeout = 30)`

## BR-013 — Documento (CPF/CNPJ) como chave pública
- Toda operação de usuário na API usa **documento** (não UUID)
- Paths: `/users/{document}`, `/users/{document}/balance`, `/users/{document}/transactions`
- Transferência: `payerDocument` + `payeeDocument`
- JWT `sub` = documento; claim `userId` = UUID interno
- UUID de usuário só para FKs internas
- Documento inexistente → `USER_NOT_FOUND` (404)
- Operar conta de outro documento → `FORBIDDEN` (403)

## BR-014 — Estorno autorizado ao pagador
- Somente o documento autenticado igual ao `payer` da TX original pode estornar

## BR-015 — Favoritos
- Só o próprio documento autenticado lista/grava/remove favoritos
- Não é permitido favoritar o próprio CPF/CNPJ (`INVALID_TRANSACTION`)
- Recebedor deve existir (`USER_NOT_FOUND`)
- Persistência: Mongo (`favorites`); testes usam store in-memory
- Não altera saldo

## Matriz de status HTTP

| Código domínio | HTTP |
|----------------|------|
| USER_NOT_FOUND / TRANSACTION_NOT_FOUND | 404 |
| DUPLICATE_RESOURCE | 409 |
| INSUFFICIENT_BALANCE / DAILY_LIMIT_EXCEEDED / INACTIVE_ACCOUNT / INVALID_TRANSACTION | 422 |
| INVALID_CREDENTIALS | 401 |
| FORBIDDEN | 403 |
| VALIDATION_ERROR | 400 |
| RATE_LIMIT_EXCEEDED | 429 |
