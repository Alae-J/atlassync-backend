#!/usr/bin/env python3
"""
Generate fake purchases.completed events to enrich the dashboard.

Reads real product barcodes/names/prices from product-service Postgres,
emits N synthetic shopping sessions spread across the past D days, and
writes each as a JSON line on stdout — exactly the schema session-service
produces. Pipe the output to kafka-console-producer to replay through
the live pipeline (streaming -> bronze -> DAG2 -> silver -> gold).

Usage:
    python3 analytics/scripts/seed_fake_purchases.py [--sessions N] [--days D]

    # Replay through Kafka (run from the backend repo root):
    python3 analytics/scripts/seed_fake_purchases.py | \\
      docker exec -i atlassync-backend_kafka_1 \\
      kafka-console-producer --bootstrap-server localhost:9092 \\
      --topic purchases.completed
"""
from __future__ import annotations

import argparse
import json
import os
import random
import subprocess
import sys
import uuid
from datetime import datetime, timedelta, timezone
from typing import List, Tuple

# Read products via `docker exec psql` to avoid a psycopg2 dependency on the host.
POSTGRES_CONTAINER = os.getenv("POSTGRES_CONTAINER", "atlassync-backend_postgres_1")
PG_USER = os.getenv("PG_USER", "atlassync")
PG_DB = os.getenv("PG_DB", "atlassync_product")

# How many synthetic users to spread purchases across. Some get more sessions
# than others to give the "active shoppers per day" line some texture.
NUM_USERS = 15
# Bias toward smaller carts but allow occasional big shops.
CART_SIZE_DIST = [1, 2, 2, 3, 3, 3, 4, 4, 5, 6, 7]
QTY_DIST = [1, 1, 1, 1, 2, 2, 3]


def load_products() -> List[Tuple[str, str, float]]:
    """Pull (barcode, name, price) from product-service Postgres without
    requiring psycopg2 on the host — shells out to the container's psql."""
    sql = (
        "COPY (SELECT barcode, name, price FROM products WHERE price IS NOT NULL) "
        "TO STDOUT WITH (FORMAT csv)"
    )
    result = subprocess.run(
        ["docker", "exec", POSTGRES_CONTAINER, "psql", "-U", PG_USER, "-d", PG_DB, "-A", "-t", "-c", sql],
        check=True, capture_output=True, text=True,
    )
    rows: List[Tuple[str, str, float]] = []
    for line in result.stdout.splitlines():
        if not line.strip():
            continue
        # Names can contain commas; the COPY CSV format quotes them. Use the
        # csv module to parse a single line safely.
        import csv
        from io import StringIO
        for parts in csv.reader(StringIO(line)):
            barcode, name, price = parts
            rows.append((barcode, name, float(price)))
    if not rows:
        sys.exit("no products in product-service Postgres — run the catalog seeder first")
    return rows


def make_event(products: List[Tuple[str, str, float]],
               event_time: datetime,
               user_id: int) -> dict:
    cart_size = random.choice(CART_SIZE_DIST)
    picks = random.sample(products, k=min(cart_size, len(products)))
    items = []
    total = 0.0
    for barcode, name, unit_price in picks:
        qty = random.choice(QTY_DIST)
        items.append({
            "barcode": barcode,
            "name": name,
            "unitPrice": round(unit_price, 2),
            "qty": qty,
        })
        total += unit_price * qty
    return {
        "eventId": str(uuid.uuid4()),
        "eventTime": event_time.strftime("%Y-%m-%dT%H:%M:%S.%fZ"),
        "sessionId": str(uuid.uuid4()),
        "userId": user_id,
        "storeId": 1,
        "currency": "MAD",
        "total": round(total, 2),
        "items": items,
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--sessions", type=int, default=250)
    parser.add_argument("--days", type=int, default=60)
    parser.add_argument("--seed", type=int, default=None, help="random seed for reproducibility")
    args = parser.parse_args()

    if args.seed is not None:
        random.seed(args.seed)

    products = load_products()
    print(f"loaded {len(products)} products", file=sys.stderr)

    # User weights — first few users get most of the traffic, tail-end users
    # show up only occasionally. Makes the active-shoppers line non-flat.
    user_weights = [10, 8, 7, 5, 4, 4, 3, 3, 2, 2, 2, 2, 1, 1, 1][:NUM_USERS]

    now = datetime.now(tz=timezone.utc)
    for _ in range(args.sessions):
        # Uniform across the last D days, with biased time-of-day for realism.
        day_offset = random.uniform(0, args.days)
        hour = random.choices(
            [9, 10, 11, 12, 13, 14, 17, 18, 19, 20],
            weights=[2, 4, 7, 9, 6, 3, 5, 7, 8, 4],
            k=1,
        )[0]
        minute = random.randint(0, 59)
        event_time = now - timedelta(days=day_offset)
        event_time = event_time.replace(hour=hour, minute=minute, second=random.randint(0, 59), microsecond=random.randint(0, 999_999))
        user_id = random.choices(range(1, NUM_USERS + 1), weights=user_weights, k=1)[0]
        event = make_event(products, event_time, user_id)
        sys.stdout.write(json.dumps(event) + "\n")

    print(f"emitted {args.sessions} sessions across {args.days} days", file=sys.stderr)


if __name__ == "__main__":
    main()
