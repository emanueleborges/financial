DESAFIO: PLATAFORMA DE PAGAMENTOS INSTANTÂNEOS (FINANCIAL HUB)
Contexto
Você foi contratado para construir o backend de uma fintech que processa transações P2P (tipo Pix) em tempo real. O sistema precisa lidar com alta concorrência, consistência financeira e rastreabilidade total.

📋 REQUISITOS TÉCNICOS (OBRIGATÓRIOS)
1. Java + Spring Boot
Versão: Java 17+ / Spring Boot 3.x

Arquitetura: Hexagonal ou Clean Architecture (camadas bem separadas)

Use Spring Data JPA e Spring Security (JWT para autenticação)

2. APIs REST
Endpoints obrigatórios:

text
POST   /api/v1/users              → Criar usuário (com saldo inicial)
GET    /api/v1/users/{id}         → Buscar usuário
POST   /api/v1/transactions       → Realizar transferência
GET    /api/v1/transactions/{id}  → Consultar status da transação
GET    /api/v1/users/{id}/balance → Consultar saldo (com lock otimista)
POST   /api/v1/transactions/reverse → Estornar transação (caso falhe)
Regras de negócio:

Usuário não pode ter saldo negativo

Transferência só pode ser feita entre contas ativas

Limite diário de R$ 5.000,00 por usuário (configurável)

3. SQL (Banco de Dados)
Use PostgreSQL (ou MySQL) com migrações Flyway

Modelo relacional com:

users (id, name, email, document, balance, status, daily_limit, created_at)

transactions (id, payer_id, payee_id, amount, status, type, created_at)

transaction_audit (id, transaction_id, event, payload, created_at) → para rastreabilidade

Desafio extra: Crie uma stored procedure para atualizar saldos com SELECT ... FOR UPDATE e garantir atomicidade.

4. AWS (Cloud)
Use LocalStack para desenvolvimento ou uma conta free tier:

S3: Armazenar comprovantes de transações (PDFs gerados automaticamente)

RDS (PostgreSQL): Banco gerenciado (ou use LocalStack com RDS mock)

SQS/SNS: Filas para eventos (opcional, mas valorizado)

Desafio: Crie um script Terraform ou CloudFormation para provisionar:

RDS instance

S3 bucket

IAM roles para a aplicação

5. Docker + Kubernetes
Dockerfile multi-stage para a aplicação

docker-compose.yml com:

App Spring Boot

PostgreSQL

Kafka (Zookeeper + Broker)

Redis (para cache de saldo)

K8s (Minikube/Kind): Crie manifests para:

Deployment (com 3 réplicas)

Service (LoadBalancer)

ConfigMap (variáveis de ambiente)

Secret (credenciais)

HPA (Horizontal Pod Autoscaler) baseado em CPU/memória

Ingress (para roteamento)

6. Mensageria (Kafka)
Event-driven: Ao criar uma transação, publique eventos:

transaction.created (payload: dados da transação)

transaction.completed (após processamento)

transaction.failed (caso erro)

Consumidores:

Consumer 1: Atualiza saldo após confirmação

Consumer 2: Envia notificação por e-mail (mock)

Consumer 3: Gera relatório diário de transações (agregador)

Use Spring Kafka com:

@KafkaListener

Dead Letter Queue (DLQ) para mensagens com falha

Idempotência (evitar processamento duplicado com transaction_id)

🚀 DESAFIOS EXTRAS (DIFERENCIAL)
Nível Pleno/Sênior:
Circuit Breaker (Resilience4j) nas chamadas a serviços externos (ex: validação de CPF)

Distributed Tracing com Micrometer + Zipkin

Cache Distribuído com Redis para saldo (TTL 5 min)

Testes:

Unitários (JUnit + Mockito)

Integração (Testcontainers)

Contrato (Spring Cloud Contract)

Monitoramento:

Métricas com Micrometer + Prometheus

Logs estruturados (JSON) para ELK/CloudWatch

Rate Limiting com Bucket4j (limitar 100 requisições/min por usuário)

Nível Especialista (se quiser se destacar):
Saga Pattern para transações distribuídas (orquestração com Kafka)

CQRS separando leitura (Redis) e escrita (PostgreSQL)

Event Sourcing (armazenar eventos da transação no Kafka)

Multi-tenancy (isolamento por organização)

Observabilidade com OpenTelemetry

📦 ENTREGÁVEIS ESPERADOS
Código-fonte no GitHub (com README bem escrito)

Coleção Postman/Insomnia com todas as rotas

Diagramas:

Arquitetura (com AWS, K8s, Kafka)

Banco de dados (DER)

Fluxo da transação (sincrono + assincrono)

Instruções para rodar localmente (docker-compose up)

Relatório técnico:

Decisões arquiteturais

Trade-offs

Como garantir consistência financeira

Como escalar horizontalmente

🏆 CRITÉRIOS DE AVALIAÇÃO
Critério	Peso	O que vou olhar
Organização do código	20%	Clean Code, padrões, separação de responsabilidades
Modelagem de dados	15%	Relacionamentos, índices, constraints, normalização
APIs REST	15%	Verbos HTTP, status codes, validações, documentação (OpenAPI)
Kafka	15%	Configuração, consumidores produtores, tratamento de erros
Docker/K8s	15%	Dockerfile eficiente, health checks, probes, HPA
AWS	10%	Uso correto dos serviços, IAM, infra como código
Testes	10%	Cobertura > 70%, testes de integração reais
💡 DICAS PARA COMEÇAR
Estrutura de pastas sugerida:
text
financial-hub/
├── src/
│   ├── main/
│   │   ├── java/com/financialhub/
│   │   │   ├── application/       # Use cases
│   │   │   ├── domain/            # Entities, enums, exceptions
│   │   │   ├── infrastructure/    # Repositories, Kafka, AWS
│   │   │   └── interfaces/        # REST controllers
│   │   └── resources/
│   │       ├── db/migration/      # Flyway scripts
│   │       └── application.yml
│   └── test/                      # Testes unitários e integração
├── docker/
│   ├── Dockerfile
│   └── docker-compose.yml
├── k8s/
│   ├── deployment.yml
│   ├── service.yml
│   ├── configmap.yml
│   └── hpa.yml
├── terraform/                     # Infra AWS
│   ├── main.tf
│   └── variables.tf
└── README.md
⚡ PASSO A PASSO RECOMENDADO
Semana 1: Modelagem de dados + CRUD básico (Spring Boot + PostgreSQL)

Semana 2: Regras de negócio + transações com lock otimista

Semana 3: Kafka (produtores/consumidores) + eventos

Semana 4: Docker + docker-compose (ambiente local)

Semana 5: AWS (S3, RDS) + Terraform

Semana 6: Kubernetes (Deployment, Service, HPA)

Semana 7: Testes + documentação + refinamentos

🔥 BÔNUS: O QUE MAIS É VALORIZADO
Performance: Use índices corretos, evite N+1 queries

Segurança: Hash de senhas (BCrypt), tokens JWT com refresh

Idempotência: Use idempotency_key nas requisições

Deadline: Transações têm timeout de 30s (use @Transactional(timeout = 30))

Backup: Estratégia de restore point no RDS

❓ PERGUNTAS QUE VOCÊ DEVE SABER RESPONDER
Como garantir que duas transferências simultâneas não deixem o saldo negativo?

O que acontece se o Kafka estiver fora do ar? Como a aplicação se comporta?

Como você faria rollback de uma transação que já publicou evento no Kafka?

Qual estratégia de particionamento no Kafka você usaria e por quê?

Como monitorar a saúde dos consumidores Kafka?