"""Job Python: consome transaction.completed e grava relatório diário no S3 (LocalStack)."""
from __future__ import annotations

import json
import os
from collections import defaultdict
from datetime import datetime, timezone

from kafka import KafkaConsumer
import boto3

BOOTSTRAP = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
TOPIC = os.getenv("KAFKA_TOPIC", "transaction.completed")
GROUP = os.getenv("KAFKA_GROUP", "python-daily-report")
S3_ENDPOINT = os.getenv("AWS_ENDPOINT", "http://localhost:4566")
S3_BUCKET = os.getenv("S3_BUCKET", "financial-hub-receipts")
AWS_KEY = os.getenv("AWS_ACCESS_KEY_ID", "test")
AWS_SECRET = os.getenv("AWS_SECRET_ACCESS_KEY", "test")
AWS_REGION = os.getenv("AWS_REGION", "us-east-1")

totals: dict[str, dict[str, float | int]] = defaultdict(lambda: {"count": 0, "volume": 0.0})


def s3_client():
    return boto3.client(
        "s3",
        endpoint_url=S3_ENDPOINT or None,
        aws_access_key_id=AWS_KEY,
        aws_secret_access_key=AWS_SECRET,
        region_name=AWS_REGION,
    )


def flush(day: str) -> None:
    payload = {
        "day": day,
        "count": totals[day]["count"],
        "volume": totals[day]["volume"],
        "generatedAt": datetime.now(timezone.utc).isoformat(),
    }
    key = f"reports/daily-{day}.json"
    body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
    try:
        s3_client().put_object(Bucket=S3_BUCKET, Key=key, Body=body, ContentType="application/json")
        print(f"Relatório enviado s3://{S3_BUCKET}/{key} {payload}", flush=True)
    except Exception as ex:
        print(f"Falha ao enviar S3 ({ex}); payload={payload}", flush=True)


def main() -> None:
    print(f"daily-report ouvindo {TOPIC} em {BOOTSTRAP}", flush=True)
    consumer = KafkaConsumer(
        TOPIC,
        bootstrap_servers=BOOTSTRAP.split(","),
        group_id=GROUP,
        auto_offset_reset="earliest",
        enable_auto_commit=True,
        value_deserializer=lambda v: json.loads(v.decode("utf-8")),
    )
    for message in consumer:
        event = message.value or {}
        occurred = event.get("occurredAt") or datetime.now(timezone.utc).isoformat()
        day = occurred[:10]
        amount = float(event.get("amount") or 0)
        totals[day]["count"] = int(totals[day]["count"]) + 1
        totals[day]["volume"] = float(totals[day]["volume"]) + amount
        flush(day)


if __name__ == "__main__":
    main()
