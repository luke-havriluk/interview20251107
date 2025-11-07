from __future__ import annotations
import requests
from datetime import date
from typing import Dict, Any

class ApiError(Exception):
    pass

def fetch_rates_once(api_base: str, d: date, base: str = "EUR") -> Dict[str, Any]:
    url = f"{api_base.rstrip('/')}/{d.isoformat()}"
    params = {"from": base}
    try:
        r = requests.get(url, params=params, timeout=15)
        r.raise_for_status()
        data = r.json()
    except Exception as e:
        raise ApiError(f"Failed to fetch rates: {e}") from e

    if not isinstance(data, dict) or "rates" not in data or "date" not in data or "base" not in data:
        raise ApiError("Unexpected API payload structure")

    return data
