from __future__ import annotations
from typing import Dict, Any
from datetime import datetime
from pymongo import MongoClient

COLLECTION = "fx_daily"

def is_available(mongo_uri: str) -> bool:
    try:
        client = MongoClient(mongo_uri, serverSelectionTimeoutMS=3000)
        client.admin.command("ping")
        client.close()
        return True
    except Exception:
        return False

def persist_payload(mongo_uri: str, payload: Dict[str, Any]) -> int:
    rate_date = payload["date"]
    base = payload["base"]
    rates = payload.get("rates", {})

    if not rates:
        return 0

    doc_id = f"{rate_date}|{base}"

    client = MongoClient(mongo_uri)
    try:
        db = client.get_default_database()
        coll = db[COLLECTION]
        coll.update_one(
            {"_id": doc_id},
            {
                "$set": {
                    "_id": doc_id,
                    "rate_date": rate_date,
                    "base": base,
                    "rates": rates,
                    "fetched_at": datetime.utcnow(),
                    "raw": payload,
                }
            },
            upsert=True,
        )
        return 1
    finally:
        client.close()
