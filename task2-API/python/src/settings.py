from __future__ import annotations
import os
from dataclasses import dataclass
from dotenv import load_dotenv

load_dotenv()

@dataclass
class Settings:
    api_base: str = os.getenv("API_BASE", "https://api.frankfurter.app")
    base_currency: str = os.getenv("BASE_CURRENCY", "EUR")

    pg_dsn: str = os.getenv("PG_DSN", "postgresql://app:app@localhost:5432/rates")
    mongo_uri: str = os.getenv("MONGO_URI", "mongodb://app:app@localhost:27017/rates")

settings = Settings()
