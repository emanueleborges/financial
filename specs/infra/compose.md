# Docker Compose (raiz do repositório)

O stack local sobe a partir da **raiz**: `docker compose up --build -d`.

## Layout

| Caminho | Função |
|---------|--------|
| `docker-compose.yml` | Orquestração do laboratório |
| `docker/` | Prometheus, Grafana, init LocalStack, dumps da JVM |
| `backend/Dockerfile` | Imagem da API |
| `frontend/angular/Dockerfile` | UI Angular |
| `frontend/next/Dockerfile` | UI Next.js |
| `services/notification-service/Dockerfile` | Inbox Oracle (profile `oracle`) |
| `jobs/daily-report/Dockerfile` | Job Python (profile `jobs`) |

## Serviços padrão

API, Angular, Next.js, Postgres, Redis, Kafka/Zookeeper, Mongo, LocalStack, Prometheus, Grafana, Zipkin.

PostgreSQL continua o ledger. Oracle e Mongo **não** guardam saldo.

Latência HTTP de laboratório: `SIMULATED_LATENCY_MS` (ver [`observability.md`](observability.md)).

## Opcionais (imagens grandes)

| Profile | Serviços | Motivo |
|---------|----------|--------|
| `oracle` | Oracle Free 23 + notification-service `:8081` | imagem Oracle pesada |
| `sonar` | SonarQube Community `:9000` | imagem e RAM |
| `jobs` | daily-report Python | extra |

```bash
docker compose --profile oracle up --build -d
docker compose --profile sonar up --build -d
docker compose --profile jobs up --build -d
```

## Limpeza

Este projeto:

```bash
docker compose --profile oracle --profile sonar --profile jobs down -v --rmi all
```

Docker da máquina (remove **tudo**, não só o Financial Hub):

```bash
docker system prune -af --volumes
```
