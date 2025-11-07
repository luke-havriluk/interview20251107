from __future__ import annotations
import argparse
import json
from datetime import datetime, date

from .settings import settings
from .api import fetch_rates_once, ApiError
from . import persist_postgres as pgp
from . import persist_mongo as mgp

def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description="Fetch FX rates for a date and optionally persist")
    p.add_argument("--date", dest="date", help="YYYY-MM-DD", required=False)
    p.add_argument("--persist", dest="persist", choices=["yes", "no"], required=False)
    p.add_argument("--store", dest="store", choices=["postgres", "mongo", "auto"], default="auto")
    return p.parse_args()

def prompt_date() -> date:
    while True:
        s = input("Enter date (YYYY-MM-DD): ").strip()
        try:
            return datetime.strptime(s, "%Y-%m-%d").date()
        except ValueError:
            print("Invalid format. Please use YYYY-MM-DD.")

def main() -> None:
    args = parse_args()

    if args.date:
        try:
            d = datetime.strptime(args.date, "%Y-%m-%d").date()
        except ValueError:
            print("Error: --date must be YYYY-MM-DD")
            return
    else:
        d = prompt_date()

    try:
        payload = fetch_rates_once(settings.api_base, d, base=settings.base_currency)
    except ApiError as e:
        print(f"Fetch error: {e}")
        return

    print(f"Fetched {len(payload.get('rates', {}))} rates for {payload['date']} base {payload['base']}")
    
    print(json.dumps(payload["rates"], indent=2, sort_keys=True))

    persist_decision = args.persist
    if not persist_decision:
        persist_decision = input("Persist data? (yes/no): ").strip().lower()

    if persist_decision != "yes":
        print("Not persisted.")
        return

    pg_ok = pgp.is_available(settings.pg_dsn)
    mg_ok = mgp.is_available(settings.mongo_uri)

    store = args.store
    if store == "auto":
        store = "postgres" if pg_ok else ("mongo" if mg_ok else None)

    if store is None:
        print("No persistence engines are available (Postgres and Mongo are down).")
        return

    if store == "postgres" and not pg_ok:
        print("Postgres is not available. Try --store mongo or start the service.")
        return
    if store == "mongo" and not mg_ok:
        print("Mongo is not available. Try --store postgres or start the service.")
        return

    if store == "postgres":
        n = pgp.persist_payload(settings.pg_dsn, payload)
        print(f"Persisted {n} rows into Postgres.")
    else:
        n = mgp.persist_payload(settings.mongo_uri, payload)
        print(f"Upserted {n} document into MongoDB.")

if __name__ == "__main__":
    main()
