# Carga do FinancialHub

Este kit separa carga em três frentes: dados na API, tráfego autenticado e eventos Kafka. Execute com a API, PostgreSQL e Kafka disponíveis no Compose.

## 1. Dados

```bash
cd backend/load-test
python3 load_test.py seed --users 100 --workers 8 --output load-users.json
```

Os usuários são criados com saldo inicial alto para que a etapa de tráfego teste concorrência sem esgotar o saldo imediatamente. Reexecutar o comando é idempotente para os mesmos índices: respostas `409` são aceitas.

## 2. Tráfego da API

```bash
python3 load_test.py traffic --input load-users.json --requests 1000 --workers 32
```

Cada chamada faz login e executa uma transferência autenticada com `Idempotency-Key` exclusivo. A saída resume status HTTP e duração. Ajuste `--workers`, `--requests`, `--min-amount` e `--max-amount` conforme o ambiente.

## 3. Eventos Kafka

Instale a dependência somente para este modo:

```bash
python3 -m pip install -r jobs/daily-report/requirements.txt
python3 load_test.py kafka --bootstrap localhost:9092 --topic transaction.completed --events 1000
```

O produtor usa `acks=all`, retries e a chave `transactionId`. Os eventos seguem o contrato de `specs/backend/events/kafka.md`; não crie tópicos novos para carga. Para exercitar o fluxo de falha:

```bash
python3 load_test.py kafka --event-type transaction.failed --topic transaction.failed --events 100
```

## Observação

A carga Kafka é sintética e não altera o ledger. Para medir a transferência financeira real, use `seed` seguido de `traffic`. Durante o teste, acompanhe `/actuator/prometheus`, Prometheus/Grafana e Zipkin conforme `specs/infra/observability.md`.

## Latência e vazão das transferências

`medir_transferencias.py` cria usuários, faz login uma vez por usuário e mede só o `POST /api/v1/transactions` (JWT, conferência de senha com BCrypt, lock pessimista e evento no Kafka), em vários níveis de concorrência:

```bash
python3 medir_transferencias.py --base http://localhost:8080 --usuarios 100 --pedidos 2000 --concorrencias 1,4,8
```

O limite de 100 pedidos/min por IP barra o teste: suba a API com `APP_RATELIMIT_REQUESTSPERMINUTE=1000000`.

Resultado em 25/10/2026 (Docker Compose num MacBook, 10 CPUs, 8 GB para o Docker; 4.300 transferências, 100% com sucesso):

| Em paralelo | Vazão | p50 | p95 | p99 |
|---|---|---|---|---|
| 1 | 14/s | 66 ms | 85 ms | 130 ms |
| 4 | 53/s | 74 ms | 85 ms | 104 ms |
| 8 | 81/s | 96 ms | 126 ms | 158 ms |

**Limite conhecido:** cada transferência usa duas conexões ao mesmo tempo (a transação de `TransferService.execute` e a `REQUIRES_NEW` de `TransactionStatusService.createPending`). Com o pool de 20 conexões, a partir de ~10 transferências simultâneas as conexões se esgotam e os pedidos esperam o timeout de 30 s. A correção é criar a transação `PENDING` fora da transação principal.
