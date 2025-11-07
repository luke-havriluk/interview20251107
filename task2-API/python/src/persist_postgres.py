from __future__ import annotations
from typing import Dict, Any
import psycopg
from psycopg.types.json import Json

DDL_HINT = """
CREATE TABLE IF NOT EXISTS fx_rates (
  rate_date      date           NOT NULL,
  base_currency  text           NOT NULL,
  currency       text           NOT NULL,
  rate           numeric(18,8)  NOT NULL,
  fetched_at     timestamptz    NOT NULL DEFAULT now(),
  source_etag    text,
  payload_json   jsonb,
  PRIMARY KEY (rate_date, base_currency, currency)
);
"""

UPSERT_SQL = """
INSERT INTO fx_rates (rate_date, base_currency, currency, rate, fetched_at, payload_json)
VALUES (%(rate_date)s, %(base)s, %(currency)s, %(rate)s, NOW(), %(payload_json)s)
ON CONFLICT (rate_date, base_currency, currency)
DO UPDATE SET rate = EXCLUDED.rate,
              fetched_at = NOW(),
              payload_json = EXCLUDED.payload_json;
"""

def is_available(pg_dsn: str) -> bool:
    try:
        with psycopg.connect(pg_dsn, connect_timeout=3) as conn:
            with conn.cursor() as cur:
                cur.execute("SELECT 1")
                cur.fetchone()
        return True
    except Exception:
        return False

def persist_payload(pg_dsn: str, payload: Dict[str, Any]) -> int:
    """Persist API payload into Postgres. Returns number of upserts."""
    rate_date = payload["date"]
    base = payload["base"]
    rates = payload.get("rates", {})

    if not rates:
        return 0

    json_payload = Json(payload)  # ✅ convert dict → jsonb for psycopg

    rows = [
        {
            "rate_date": rate_date,
            "base": base,
            "currency": ccy,
            "rate": float(val),
            "payload_json": json_payload,
        }
        for ccy, val in rates.items()
    ]

    with psycopg.connect(pg_dsn) as conn:
        with conn.cursor() as cur:
            cur.execute(DDL_HINT)
            cur.executemany(UPSERT_SQL, rows)
        conn.commit()

    return len(rows)
