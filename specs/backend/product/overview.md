# Spec de Produto — Financial Hub

**Status:** active  
**Origem:** `DESAFIO.MD`  
**Código:** `financial-hub/`  
**UI:** ver `specs/frontend/overview.md` (`web/`)

## Visão

Backend de fintech que processa transferências P2P em tempo real, com consistência financeira, auditoria e integração event-driven.

## Personas

| Persona | Necessidade |
|---------|-------------|
| Cliente final | Transferir e consultar saldo/status com confiança |
| Operações | Estornar transferência COMPLETED e auditar eventos |
| Engenheiro | Rodar local via Docker; observar métricas e saúde |

## Capacidades (capabilities)

### C1 — Gestão de usuários
- Criar usuário com saldo inicial ≥ 0
- Validar documento (CPF/CNPJ)
- Buscar usuário por ID
- Consultar saldo com versão (optimistic lock metadata)

### C2 — Autenticação
- Login por e-mail/senha
- Emitir access + refresh JWT
- Proteger rotas autenticadas

### C3 — Transferência P2P
- Transferir entre contas ACTIVE
- Respeitar limite diário (default R$ 5.000)
- Idempotência por chave
- Timeout transacional 30s
- Publicar eventos Kafka

### C4 — Estorno
- Estornar apenas TRANSFER COMPLETED
- Um estorno por transação original
- Inverter payer/payee e devolver valor

### C5 — Side-effects assíncronos
- Invalidar cache de saldo
- Notificação e-mail (mock)
- Relatório diário agregado
- Comprovante PDF no S3
- DLQ para falhas de consumo

### C6 — Contrato com o frontend
- A UI Next.js consome esta API; detalhes de telas em `specs/frontend/overview.md`
- Mudanças de contrato REST devem atualizar `specs/backend/api/rest-v1.md` e a spec frontend quando afetarem a UX

## Fora de escopo (v1)

- PIX Bacen real / SPI
- Multi-moeda
- Multi-tenancy
- Open banking
- Paginação cursor-based avançada (lista limitada das últimas N TX)

## Critérios de sucesso

1. Duas transferências concorrentes não geram saldo negativo.
2. Replay de mensagem Kafka não duplica side-effect (idempotência por `event_id` + consumer).
3. Stack local sobe com `docker compose` em `financial-hub/docker`.
