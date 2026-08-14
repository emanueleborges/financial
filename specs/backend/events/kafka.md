# Eventos Kafka

## Tópicos

| Tópico | Produtor | Quando |
|--------|----------|--------|
| `transaction.created` | TransferService | Após persistir PENDING |
| `transaction.completed` | Transfer / Reverse | Após saldo atualizado |
| `transaction.failed` | Transfer / Reverse | Em falha de processamento |
| `transaction.dlq` | Consumers | Após erro no handler |

## Payload (`TransactionEvent`)

```json
{
  "eventId": "uuid-string",
  "eventType": "transaction.completed",
  "transactionId": "uuid",
  "payerId": "uuid",
  "payeeId": "uuid",
  "payerDocument": "52998224725",
  "payeeDocument": "39053344705",
  "payerEmail": "alice@email.com",
  "payeeEmail": "bob@email.com",
  "payerName": "Alice Silva",
  "payeeName": "Bob Santos",
  "amount": 150.00,
  "status": "COMPLETED",
  "type": "TRANSFER",
  "failureReason": null,
  "occurredAt": "2026-08-03T18:00:00Z"
}
```

Campos de documento/e-mail/nome permitem o `notification-service` operar **sem** acessar o PostgreSQL.

## Particionamento

- **Key:** `transactionId` (ordem garantida por transação)

## Consumidores

| Consumer | Onde | Group | Ação | Idempotência |
|----------|------|-------|------|--------------|
| BalanceUpdateConsumer | `financial-hub` | balance-update-group | Evict Redis | `processed_events` (Postgres) |
| NotificationConsumer | `notification-service` (docker) ou embutido se `app.consumers.notification.enabled=true` | notification-group | Inbox Oracle + e-mail mock | unique `(event_id, email)` no Oracle / `processed_events` no embutido |
| DailyReportConsumer | `financial-hub` + job Python | daily-report-group / `python-daily-report` | Agrega count/volume | `processed_events` / arquivo JSONL |
| Daily report Python | `jobs/daily-report` | `python-daily-report` | Grava JSONL e envia ao S3 LocalStack | offset Kafka |

## Contratos de falha

- Producer: publish assíncrono; falha de broker **não** desfaz commit do saldo
- Consumer: retry (backoff) → DLQ
- Recuperação: reprocessar a partir de `transaction_audit` (outbox é melhoria futura — exige nova spec)

## Proibições

- Não usar Kafka como ledger de saldo
- Não criar tópico novo sem atualizar esta spec e `KafkaTopicConfig`
