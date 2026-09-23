#!/usr/bin/env python3
"""Realistic FinancialHub load generator: seed users, exercise API, or publish Kafka events."""
from __future__ import annotations

import argparse
import json
import os
import random
import sys
import time
import urllib.error
import urllib.request
import uuid
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime, timezone

PASSWORD = os.getenv("LOAD_TEST_PASSWORD", "senha123")


def request_json(base_url: str, method: str, path: str, payload: dict | None = None, headers: dict | None = None) -> tuple[int, dict]:
    body = json.dumps(payload).encode() if payload is not None else None
    request_headers = {"Accept": "application/json"}
    if payload is not None:
        request_headers["Content-Type"] = "application/json"
    request_headers.update(headers or {})
    request = urllib.request.Request(base_url.rstrip("/") + path, data=body, headers=request_headers, method=method)
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            content = response.read().decode()
            return response.status, json.loads(content) if content else {}
    except urllib.error.HTTPError as error:
        content = error.read().decode()
        try:
            return error.code, json.loads(content) if content else {}
        except json.JSONDecodeError:
            return error.code, {"message": content}


def cpf(seed: int) -> str:
    """Generate a unique CPF with valid check digits for the API validator."""
    digits = [int(value) for value in f"{seed % 10_000_000_00:09d}"]
    first_check = sum((10 - index) * value for index, value in enumerate(digits)) % 11
    digits.append(0 if first_check < 2 else 11 - first_check)
    second_check = sum((11 - index) * value for index, value in enumerate(digits)) % 11
    digits.append(0 if second_check < 2 else 11 - second_check)
    return "".join(str(value) for value in digits)


def create_user(base_url: str, index: int, initial_balance: str) -> dict:
    document = cpf(100000000 + index)
    payload = {
        "name": f"Load User {index}",
        "email": f"load-{index}@example.test",
        "document": document,
        "password": PASSWORD,
        "initialBalance": float(initial_balance),
    }
    status, response = request_json(base_url, "POST", "/api/v1/users", payload)
    if status not in (201, 409):
        raise RuntimeError(f"user {index}: HTTP {status}: {response}")
    return {"document": document, "email": payload["email"], "password": PASSWORD}


def seed(args: argparse.Namespace) -> None:
    started = time.perf_counter()
    users: list[dict] = []
    with ThreadPoolExecutor(max_workers=args.workers) as executor:
        futures = [executor.submit(create_user, args.base_url, index, args.initial_balance) for index in range(args.users)]
        for future in as_completed(futures):
            users.append(future.result())
    users.sort(key=lambda user: user["document"])
    with open(args.output, "w", encoding="utf-8") as output:
        json.dump(users, output, indent=2)
    print(f"seeded={len(users)} output={args.output} elapsed={time.perf_counter() - started:.2f}s")


def login(base_url: str, user: dict) -> str:
    status, response = request_json(base_url, "POST", "/api/v1/auth/login", {
        "document": user["document"],
        "password": user["password"],
    })
    if status != 200:
        raise RuntimeError(f"login {user['document']}: HTTP {status}: {response}")
    return response["accessToken"]


def exercise_one(args: argparse.Namespace, payer: dict, payee: dict, sequence: int) -> tuple[int, float]:
    token = login(args.base_url, payer)
    amount = round(random.uniform(args.min_amount, args.max_amount), 2)
    headers = {
        "Authorization": f"Bearer {token}",
        "Idempotency-Key": f"load-{payer['document']}-{payee['document']}-{sequence}",
    }
    status, _ = request_json(args.base_url, "POST", "/api/v1/transactions", {
        "payerDocument": payer["document"],
        "payeeDocument": payee["document"],
        "amount": amount,
        "password": payer["password"],
    }, headers)
    return status, amount


def traffic(args: argparse.Namespace) -> None:
    with open(args.input, encoding="utf-8") as source:
        users = json.load(source)
    if len(users) < 2:
        raise RuntimeError("traffic requires at least two users in the seed file")
    started = time.perf_counter()
    results: list[tuple[int, float]] = []
    with ThreadPoolExecutor(max_workers=args.workers) as executor:
        futures = []
        for sequence in range(args.requests):
            payer = users[sequence % len(users)]
            payee = users[(sequence + 1) % len(users)]
            futures.append(executor.submit(exercise_one, args, payer, payee, sequence))
        for future in as_completed(futures):
            results.append(future.result())
    counts: dict[int, int] = {}
    for status, _ in results:
        counts[status] = counts.get(status, 0) + 1
    print(f"requests={len(results)} statuses={counts} elapsed={time.perf_counter() - started:.2f}s")


def publish(args: argparse.Namespace) -> None:
    try:
        from kafka import KafkaProducer
    except ImportError as error:
        raise RuntimeError("Kafka mode requires kafka-python; install jobs/daily-report/requirements.txt") from error
    producer = KafkaProducer(
        bootstrap_servers=args.bootstrap.split(","),
        value_serializer=lambda value: json.dumps(value).encode("utf-8"),
        key_serializer=lambda value: value.encode("utf-8"),
        acks="all",
        retries=3,
    )
    now = datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")
    for index in range(args.events):
        transaction_id = str(uuid.uuid4())
        event_type = args.event_type
        event = {
            "eventId": str(uuid.uuid4()),
            "eventType": event_type,
            "transactionId": transaction_id,
            "payerId": str(uuid.uuid4()),
            "payeeId": str(uuid.uuid4()),
            "payerDocument": cpf(200000000 + index),
            "payeeDocument": cpf(300000000 + index),
            "payerEmail": f"payer-{index}@example.test",
            "payeeEmail": f"payee-{index}@example.test",
            "payerName": f"Load Payer {index}",
            "payeeName": f"Load Payee {index}",
            "amount": float(10 + (index % 100)),
            "status": "COMPLETED" if event_type == "transaction.completed" else "FAILED",
            "type": "TRANSFER",
            "failureReason": None if event_type == "transaction.completed" else "load-test event",
            "occurredAt": now,
        }
        producer.send(args.topic, key=transaction_id, value=event)
    producer.flush()
    producer.close()
    print(f"published={args.events} topic={args.topic} event_type={args.event_type}")


def parser() -> argparse.ArgumentParser:
    result = argparse.ArgumentParser(description=__doc__)
    subparsers = result.add_subparsers(dest="mode", required=True)

    seed_parser = subparsers.add_parser("seed", help="create API users concurrently")
    seed_parser.add_argument("--base-url", default=os.getenv("BASE_URL", "http://localhost:8080"))
    seed_parser.add_argument("--users", type=int, default=100)
    seed_parser.add_argument("--workers", type=int, default=8)
    seed_parser.add_argument("--initial-balance", default="100000.00")
    seed_parser.add_argument("--output", default="load-users.json")
    seed_parser.set_defaults(handler=seed)

    traffic_parser = subparsers.add_parser("traffic", help="issue authenticated transfers concurrently")
    traffic_parser.add_argument("--base-url", default=os.getenv("BASE_URL", "http://localhost:8080"))
    traffic_parser.add_argument("--input", default="load-users.json")
    traffic_parser.add_argument("--requests", type=int, default=1000)
    traffic_parser.add_argument("--workers", type=int, default=32)
    traffic_parser.add_argument("--min-amount", type=float, default=1.00)
    traffic_parser.add_argument("--max-amount", type=float, default=25.00)
    traffic_parser.set_defaults(handler=traffic)

    kafka_parser = subparsers.add_parser("kafka", help="publish contract-compatible events")
    kafka_parser.add_argument("--bootstrap", default=os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092"))
    kafka_parser.add_argument("--topic", default="transaction.completed")
    kafka_parser.add_argument("--events", type=int, default=1000)
    kafka_parser.add_argument("--event-type", choices=["transaction.completed", "transaction.failed"], default="transaction.completed")
    kafka_parser.set_defaults(handler=publish)
    return result


if __name__ == "__main__":
    try:
        arguments = parser().parse_args()
        arguments.handler(arguments)
    except (OSError, RuntimeError, ValueError) as error:
        print(f"load-test error: {error}", file=sys.stderr)
        sys.exit(1)
