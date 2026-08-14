# Modelo de Dados

**SGBD:** PostgreSQL 16  
**Migrações:** Flyway em `backend/src/main/resources/db/migration/`

## Tabelas

### users
| Coluna | Tipo | Constraints |
|--------|------|-------------|
| id | UUID | PK |
| name | VARCHAR(150) | NOT NULL |
| email | VARCHAR(255) | UNIQUE NOT NULL |
| document | VARCHAR(14) | UNIQUE NOT NULL |
| password_hash | VARCHAR(255) | NOT NULL |
| balance | NUMERIC(19,2) | NOT NULL, CHECK >= 0 |
| status | VARCHAR(20) | ACTIVE\|INACTIVE\|BLOCKED |
| daily_limit | NUMERIC(19,2) | NOT NULL default 5000 |
| version | BIGINT | optimistic lock |
| created_at / updated_at | TIMESTAMPTZ | NOT NULL |

**Índices:** email, document, status

### transactions
| Coluna | Tipo | Constraints |
|--------|------|-------------|
| id | UUID | PK |
| payer_id / payee_id | UUID | FK users, payer ≠ payee |
| amount | NUMERIC(19,2) | > 0 |
| status | VARCHAR(30) | PENDING\|PROCESSING\|COMPLETED\|FAILED\|REVERSED |
| type | VARCHAR(30) | TRANSFER\|REVERSAL |
| idempotency_key | VARCHAR(100) | UNIQUE nullable |
| failure_reason | VARCHAR(500) | |
| original_tx_id | UUID | FK self (estorno) |
| created_at / updated_at / completed_at | TIMESTAMPTZ | |

**Índices:** payer, payee, status, created_at, (payer_id, created_at)

### transaction_audit
| Coluna | Tipo |
|--------|------|
| id | UUID PK |
| transaction_id | UUID FK |
| event | VARCHAR(100) |
| payload | JSONB |
| created_at | TIMESTAMPTZ |

### processed_events
Idempotência de consumidores Kafka **do financial-hub**: unique `(event_id, consumer_name)`.

## MongoDB — collection `favorites`

Não é ledger. Usado só para BR-015.

| Campo | Tipo |
|-------|------|
| ownerDocument | string (CPF/CNPJ) |
| payeeDocument | string |
| name | string |
| savedAt | ISODate |

Unique: `(ownerDocument, payeeDocument)`.

Oracle do inbox: [`oracle.md`](oracle.md).

## Procedure obrigatória

`transfer_balance(payer_id, payee_id, amount)`:
1. `SELECT ... FOR UPDATE` ordenando por UUID (anti-deadlock)
2. Valida existência e ACTIVE
3. Valida saldo
4. Debita pagador / credita recebedor
5. Incrementa `version`

## Regras de schema

- Novas colunas/tabelas → nova migração `V{n}__descricao.sql`
- Proibido alterar migrações já aplicadas
- `ddl-auto=validate` em runtime (não `update`)
