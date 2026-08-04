# Relatório Técnico — Financial Hub

## 1. Decisões arquiteturais

### Clean Architecture / Hexagonal
Separação em quatro camadas:
- **domain**: regras puras, sem dependência de framework
- **application**: use cases e ports (in/out)
- **infrastructure**: adapters (JPA, Kafka, Redis, S3, JWT)
- **interfaces**: controllers REST e filters

Isso permite trocar Redis por outro cache, ou Kafka por SQS, sem alterar regras de negócio.

### Transferência síncrona + eventos assíncronos
A transferência atualiza saldo no banco **sincronamente** (fonte da verdade) e publica eventos Kafka para side-effects (notificação, comprovante, relatório). Assim o cliente recebe confirmação imediata com garantia ACID.

### Lock pessimista via stored procedure
`transfer_balance` usa `SELECT ... FOR UPDATE` ordenando locks por UUID para evitar deadlock em transferências cruzadas A↔B.

## 2. Trade-offs

| Decisão | Prós | Contras |
|---------|------|---------|
| Sync transfer + async events | Consistência forte no saldo | Acoplamento temporal no request |
| Redis cache-aside | Menos carga no PG | Possível stale (TTL 5min + eviction) |
| Kafka fire-and-forget no publish | Não bloqueia a API | Evento pode se perder se broker cair após commit |
| 3 réplicas K8s | Alta disponibilidade | Precisa sticky? Não — JWT stateless |
| LocalStack no compose | Dev sem conta AWS | Diferenças vs AWS real |

## 3. Consistência financeira

1. Constraint `CHECK (balance >= 0)` no PostgreSQL
2. Stored procedure com row-level lock
3. `@Version` (optimistic lock) no UserEntity para leituras de saldo
4. `@Transactional(timeout = 30)` evita locks longos
5. Tabela `transaction_audit` para rastreabilidade completa
6. Idempotency key evita débito duplicado

**Duas transferências simultâneas:** a segunda espera o `FOR UPDATE` da primeira; se saldo insuficiente, a procedure lança `INSUFFICIENT_BALANCE`.

## 4. Escalabilidade horizontal

- **API**: HPA 3–10 pods baseado em CPU/memória; JWT stateless
- **Kafka**: 3 partições, key = `transactionId` (ordem por transação)
- **PostgreSQL**: connection pool HikariCP; índices em payer/payee/created_at
- **Redis**: alivia leituras de saldo sob pico
- **S3**: comprovantes desacoplados do request path

## 5. Respostas às perguntas do desafio

### Kafka fora do ar?
A transferência no banco já commitou. O publish falha de forma assíncrona (log de erro). Recuperação: reprocessar a partir de `transaction_audit` ou outbox pattern (melhoria futura).

### Rollback após evento publicado?
Eventos `transaction.failed` e estorno (`/transactions/reverse`) compensam. Saga com orquestração via Kafka é o caminho para cenários mais complexos.

### Particionamento Kafka?
Por `transactionId` — garante ordem dos eventos de uma mesma transferência sem hot-partition por usuário.

### Saúde dos consumidores?
Actuator + métricas Micrometer (`kafka.consumer.*`); lag monitorável via Prometheus. DLQ para mensagens com falha após retries.
