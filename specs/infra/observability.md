# Observabilidade e performance

## Escopo

O backend publica métricas Micrometer no Actuator e o Prometheus local faz scrape de `/actuator/prometheus`. O Grafana e o Zipkin sobem no Compose padrão (junto com a API e as UIs).

## Logs e correlação

Os logs da API são JSON via Logstash Logback. Cada requisição recebe um `correlationId` reutilizado quando enviado pelo cliente ou gerado pela API. O valor é colocado no MDC e devolvido no header `X-Correlation-Id`; o filtro sempre remove o MDC ao final da requisição.

Trace IDs do Micrometer continuam disponíveis separadamente para troubleshooting distribuído.

## Runtime

- HikariCP: pool configurável por ambiente, com timeout de aquisição, ciclo de vida e detecção de conexão retida.
- Tomcat: limites de threads, conexões e fila configuráveis.
- JVM: o container aceita `JAVA_OPTS` para heap, G1GC, limite de pausa e heap dump em OOM.

Os valores padrão são adequados ao ambiente local e podem ser substituídos por variáveis de ambiente. Nenhum endpoint de heap dump ou execução arbitrária de SQL é exposto pela API; essas operações permanecem responsabilidade operacional protegida do ambiente.

## Execução local

```bash
docker compose up --build -d
```

Sobe API, UIs, Postgres, Redis, Kafka, Mongo, LocalStack, Prometheus, Grafana e Zipkin. Oracle + notification (`--profile oracle`), SonarQube (`--profile sonar`) e o job Python (`--profile jobs`) são opcionais.

O endpoint Actuator de métricas não deve ser publicado além da rede confiável do ambiente.

## Latência simulada (laboratório)

A API aceita `SIMULATED_LATENCY_MS` (`app.latency.simulated-ms`, default `0`). Valor `> 0` atrasa cada request HTTP **antes** do use case, sem alterar saldo, Kafka nem regras BR-*. Health/probes Actuator e Swagger não sofrem o atraso. Teto: 30_000 ms.

```bash
SIMULATED_LATENCY_MS=800 docker compose up -d --force-recreate app
```

O header de resposta `X-Simulated-Latency-Ms` confirma o atraso aplicado. Em `0` o filtro é no-op. Zipkin mostra o tempo total da request.

## Teste de carga

O kit em `backend/load-test/` possui três modos independentes:

- `seed`: cria usuários via API com saldo inicial e grava os documentos em um arquivo JSON.
- `traffic`: autentica usuários e executa transferências concorrentes com `Idempotency-Key`.
- `kafka`: publica eventos sintéticos `transaction.completed` ou `transaction.failed` nos tópicos existentes, usando `transactionId` como chave.

Os comandos e parâmetros estão em `backend/load-test/README.md`. Eventos Kafka sintéticos não alteram o ledger; a carga financeira real deve usar `seed` seguido de `traffic`.
