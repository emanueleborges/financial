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
  "amount": 150.00,
  "status": "COMPLETED",
  "type": "TRANSFER",
  "failureReason": null,
  "occurredAt": "2026-08-03T18:00:00Z"
}
```

## Particionamento

- **Key:** `transactionId` (ordem garantida por transação)

## Consumidores

| Consumer | Group | Ação | Idempotência |
|----------|-------|------|--------------|
| BalanceUpdateConsumer | balance-update-group | Evict Redis; (comprovante) | `processed_events` |
| NotificationConsumer | notification-group | E-mail mock payer/payee | `processed_events` |
| DailyReportConsumer | daily-report-group | Agrega count/volume | `processed_events` |

## Contratos de falha

- Producer: publish assíncrono; falha de broker **não** desfaz commit do saldo
- Consumer: retry (backoff) → DLQ
- Recuperação: reprocessar a partir de `transaction_audit` (outbox é melhoria futura — exige nova spec)

## Proibições

- Não usar Kafka como ledger de saldo
- Não criar tópico novo sem atualizar esta spec e `KafkaTopicConfig`
