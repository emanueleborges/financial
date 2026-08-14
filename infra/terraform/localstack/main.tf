terraform {
  required_version = ">= 1.5.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region                      = "us-east-1"
  access_key                  = "test"
  secret_key                  = "test"
  skip_credentials_validation = true
  skip_metadata_api_check     = true
  skip_requesting_account_id  = true
  s3_use_path_style           = true

  endpoints {
    s3  = var.localstack_endpoint
    iam = var.localstack_endpoint
    sts = var.localstack_endpoint
  }
}

variable "localstack_endpoint" {
  type    = string
  default = "http://localhost:4566"
}

variable "bucket_name" {
  type    = string
  default = "financial-hub-receipts"
}

resource "aws_s3_bucket" "receipts" {
  bucket        = var.bucket_name
  force_destroy = true
}

resource "aws_iam_user" "app" {
  name = "fh-app"
}

resource "aws_iam_access_key" "app" {
  user = aws_iam_user.app.name
}

resource "aws_iam_user_policy" "app_s3" {
  name = "fh-app-s3"
  user = aws_iam_user.app.name
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["s3:PutObject", "s3:GetObject", "s3:ListBucket"]
      Resource = [aws_s3_bucket.receipts.arn, "${aws_s3_bucket.receipts.arn}/*"]
    }]
  })
}

output "s3_bucket" {
  value = aws_s3_bucket.receipts.bucket
}

output "iam_user" {
  value = aws_iam_user.app.name
}

output "access_key_id" {
  value     = aws_iam_access_key.app.id
  sensitive = true
}

output "secret_access_key" {
  value     = aws_iam_access_key.app.secret
  sensitive = true
}
