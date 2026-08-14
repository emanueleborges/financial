# notification-service

**Status:** active  
**Código:** `services/notification-service/`  
**Porta:** `8081`  
**SGBD:** Oracle Free 23 (container local) — **não** é ledger de saldo  
**Broker:** Kafka (`transaction.completed`, `transaction.failed`, DLQ)

Segundo bounded context: o jar `financial-hub` **não** envia e-mail no profile `docker`. O consumidor vive neste serviço.

## Responsabilidade

1. Consumir eventos de transferência (payload enriquecido com e-mail/documento)
2. Persistir notificação em Oracle (idempotente por `event_id` + e-mail)
3. Logar envio mock de e-mail
4. Expor inbox REST autenticado por JWT (mesmo secret da API)

## API

Base: `/api/v1`

### GET `/notifications` — autenticado

Lista as notificações do documento do JWT (`sub`).

```json
{
  "document": "52998224725",
  "entries": [
    {
      "id": "uuid",
      "eventId": "...",
      "email": "alice@email.com",
      "document": "52998224725",
      "message": "Transferência de R$ 150.00 realizada com sucesso. ID: ...",
      "createdAt": "2026-08-14T18:00:00Z"
    }
  ]
}
```

- **200** lista (mais recentes primeiro, máx. 50)
- **401** sem JWT

Público: `GET /actuator/health`, `/actuator/prometheus`.

## Schema Oracle

Ver [`../data/oracle.md`](../data/oracle.md).

## Kafka

| Tópico | Group | Ação |
|--------|-------|------|
| `transaction.completed` | `notification-group` | gravar 1 notificação pagador + 1 recebedor |
| `transaction.failed` | `notification-group` | gravar 1 notificação pagador |
| `transaction.dlq` | (producer) | após erro no handler |

Idempotência: unique `(event_id, recipient_email)` em Oracle. Replay não duplica.

## JWT

Mesmo `JWT_SECRET` da API principal. Claim `sub` = documento.

## Local

Compose: serviço `notification` + `oracle`. Profile `docker` no financial-hub desliga o `NotificationConsumer` embutido (`app.consumers.notification.enabled=false`).

Testes do serviço usam H2 `MODE=Oracle` (sem container Oracle no `mvn test`).
