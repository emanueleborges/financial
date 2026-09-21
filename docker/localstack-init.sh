#!/bin/sh
awslocal s3 mb s3://financial-hub-receipts || true
echo "S3 bucket financial-hub-receipts criado"
