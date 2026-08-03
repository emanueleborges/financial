# Constituição do Financial Hub (SDD)

Este documento é a **fonte de verdade não negociável**. Qualquer código, PR ou sugestão de IA que conflite com esta constituição deve ser rejeitado ou ajustado.

## 1. Propósito

Plataforma de pagamentos instantâneos P2P (estilo Pix) com:
- consistência financeira forte no saldo;
- rastreabilidade total de eventos;
- suporte a alta concorrência;
- APIs REST versionadas;
- interface web Next.js para o cliente final.

## 2. Princípios imutáveis

1. **Spec-first**: nenhuma feature nova sem atualizar `specs/` antes do código.
2. **Saldo nunca negativo**: enforce em domínio + constraint SQL + lock pessimista.
3. **PostgreSQL é a fonte da verdade** do saldo; Redis é cache-aside (TTL ≤ 5 min).
4. **Transferência síncrona no banco**; Kafka só para side-effects (notificação, comprovante, relatório).
5. **Clean/Hexagonal Architecture** no backend: `domain` e `application` não dependem de frameworks de infra.
6. **Idempotência obrigatória** em transferências (`Idempotency-Key` ou `idempotency_key`).
7. **Segurança**: senhas com BCrypt; APIs protegidas por JWT (exceto criar usuário e login).
8. **Observabilidade**: logs estruturados JSON; métricas Actuator/Prometheus; health probes.
9. **Separação de specs**: backend em `specs/backend/`, frontend em `specs/frontend/`.

## 3. Escopo permitido

| Dentro do escopo | Fora do escopo (não inventar) |
|------------------|-------------------------------|
| Users, transferências, estorno | Open banking, cartões, boletos |
| Frontend Next.js (`web/`) conforme `specs/frontend/` | Features de UI sem spec |
| Kafka events definidos em `specs/backend/events/` | Novos brokers sem atualizar a spec |
| S3 para comprovantes PDF | Outros storage sem decisão em ADR |
| Limite diário configurável | Limites por produto/moeda sem spec |
| Multi-tenancy (só se spec existir) | Features “especialista” sem requisito |

## 4. Arquitetura obrigatória (backend)

```
interfaces/     → REST, filters
application/    → use cases + ports
domain/         → model, enums, exceptions
infrastructure/ → JPA, Kafka, Redis, AWS, Security
```

Dependências apontam **para dentro** (domínio no centro). Detalhes: `specs/backend/architecture/overview.md`.

## 5. Critérios de aceite globais

- Endpoints de `specs/backend/api/rest-v1.md` cobertos e documentados (OpenAPI).
- UI alinhada a `specs/frontend/overview.md`.
- Migrações Flyway versionadas; sem `ddl-auto=update` em produção.
- Testes unitários das regras de domínio e use cases críticos.
- Docker Compose sobe o stack local documentado no README.

## 6. Processo SDD (obrigatório para IA e humanos)

```
1. Ler specs/constitution.md + spec relevante (backend/ ou frontend/)
2. Atualizar/criar spec em specs/
3. Só então alterar código
4. Garantir que código e spec permanecem alinhados
5. Não “improvisar” comportamento fora da spec
```

## 7. Anti-padrões (proibidos)

- Alterar saldo sem lock / stored procedure acordada.
- Publicar evento Kafka como substituto da commit do saldo.
- Expor senha/hash em responses.
- Hardcode de limite diário sem propriedade configurável.
- Controllers com regra de negócio (deve ficar em use case/domínio).
- Misturar requisitos de UI em specs de backend (e vice-versa) sem necessidade.
