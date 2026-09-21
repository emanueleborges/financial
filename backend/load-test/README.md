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
