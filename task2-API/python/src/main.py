from __future__ import annotations
import argparse
import json
from datetime import datetime, date

from .settings import settings
from .api import fetch_rates_once, ApiError
from . import persist_postgres as pgp
from . import persist_mongo as mgp

def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description="Fetch FX rates for a date and optionally persist to multiple DBs" )
    p.add_argument("--date", dest="date", help="YYYY-MM-DD", required=False)
    # Global decision is optional via flag; per-DB is always interactive by design
    p.add_argument("--persist", dest="persist", choices=["yes", "no", "y", "n", "true", "false", "1", "0"], required=False)
    return p.parse_args()

def prompt_date() -> date:
    while True:
        s = input("Enter date (YYYY-MM-DD): ").strip()
        try:
            return datetime.strptime(s, "%Y-%m-%d").date()
        except ValueError:
            print("Invalid format. Please use YYYY-MM-DD.")

def prompt_yes_no(prompt: str) -> bool:
    yes = {"y", "yes", "true", "1"}
    no = {"n", "no", "false", "0"}
    while True:
        ans = input(prompt).strip().lower()
        if ans in yes:
            return True
        if ans in no:
            return False
        print("Please answer yes or no.")

def parse_yes_no(val: str | None) -> bool | None:
    if val is None:
        return None
    mapping = {
        "y": True, "yes": True, "true": True, "1": True,
        "n": False, "no": False, "false": False, "0": False,
    }
    return mapping.get(val.strip().lower())

def main() -> None:
    args = parse_args()

    # 1) Date
    if args.date:
        try:
            d = datetime.strptime(args.date, "%Y-%m-%d").date()
        except ValueError:
            print("Error: --date must be YYYY-MM-DD")
            return
    else:
        d = prompt_date()

    # 2) Fetch
    try:
        payload = fetch_rates_once(settings.api_base, d, base=settings.base_currency)
    except ApiError as e:
        print(f"Fetch error: {e}")
        return

    print(f"Fetched {len(payload.get('rates', {}))} rates for {payload['date']} base {payload['base']}")
    print(json.dumps(payload.get("rates", {}), indent=2, sort_keys=True))

    # 3) Global persist decision (yes/no)
    decision = parse_yes_no(args.persist)
    if decision is None:
        decision = prompt_yes_no("Persist data? (yes/no): ")

    if not decision:
        print("Not persisted.")
        return

    # 4) Postgres branch (ask only if service is up)
    pg_ok = pgp.is_available(settings.pg_dsn)
    if pg_ok:
        if prompt_yes_no("Persist into Postgres? (yes/no): "):
            try:
                n = pgp.persist_payload(settings.pg_dsn, payload)
                print(f"Persisted {n} rows into Postgres.")
            except Exception as e:
                print(f"Postgres persist failed: {e}")
    else:
        print("Postgres is not available (skipping).")

    # 5) MongoDB branch (ask only if service is up)
    mg_ok = mgp.is_available(settings.mongo_uri)
    if mg_ok:
        if prompt_yes_no("Persist into MongoDB? (yes/no): "):
            try:
                n = mgp.persist_payload(settings.mongo_uri, payload)
                print(f"Upserted {n} document into MongoDB.")
            except Exception as e:
                print(f"MongoDB persist failed: {e}")
    else:
        print("MongoDB is not available (skipping).")

if __name__ == "__main__":
    main()
