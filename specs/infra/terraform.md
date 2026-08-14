# Terraform + AWS local (LocalStack)

Dois alvos:

| Pasta | Alvo | Uso |
|-------|------|-----|
| `backend/terraform/` | AWS real | RDS + S3 + IAM (não aplicar sem conta) |
| `infra/terraform/localstack/` | LocalStack `:4566` | S3 + IAM user/keys — **é o que roda local** |

## LocalStack (obrigatório para dev)

Compose sobe LocalStack (`SERVICES=s3,iam,sts`). Terraform local cria:

- bucket `financial-hub-receipts`
- IAM user `fh-app` com policy `s3:PutObject/GetObject/ListBucket`
- access key de laboratório (`test` / `test` já funciona; Terraform gera as chaves no output)

```bash
cd infra/terraform/localstack
terraform init
terraform apply -auto-approve
```

Provider AWS aponta para `http://localhost:4566` (`skip_*` = true).  
A API usa `AWS_ENDPOINT=http://localhost:4566` (ou `http://localstack:4566` no compose).

## AWS real (opcional)

`backend/terraform/` permanece como o módulo “o que seria produção”: VPC, RDS PostgreSQL, S3, IAM role EC2.  
Não é aplicado neste laboratório.

## O que não está no laboratório

ECS/EKS/App Runner de verdade. O equivalente local é Kind/Helm (`specs/infra/kubernetes.md`).
