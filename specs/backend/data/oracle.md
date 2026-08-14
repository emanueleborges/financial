# Oracle — notification-service

**SGBD:** Oracle Free 23 (`gvenzl/oracle-free`)  
**Uso:** persistência do inbox de notificações  
**Proibido:** saldo, transferências, ledger

PostgreSQL permanece a fonte da verdade financeira (`specs/constitution.md`).

## Tabela `NOTIFICATIONS`

| Coluna | Tipo | Constraints |
|--------|------|-------------|
| ID | VARCHAR2(36) | PK |
| EVENT_ID | VARCHAR2(64) | NOT NULL |
| RECIPIENT_EMAIL | VARCHAR2(255) | NOT NULL |
| RECIPIENT_DOCUMENT | VARCHAR2(14) | |
| MESSAGE | VARCHAR2(1000) | NOT NULL |
| CREATED_AT | TIMESTAMP | NOT NULL default SYSTIMESTAMP |

**Unique:** `(EVENT_ID, RECIPIENT_EMAIL)` — idempotência do consumidor.

## Migrações

Flyway em `services/notification-service/src/main/resources/db/migration/`  
locations Oracle: `classpath:db/migration`

## Conexão local

```
jdbc:oracle:thin:@localhost:1521/FREEPDB1
user: notif / password: notif123
```
