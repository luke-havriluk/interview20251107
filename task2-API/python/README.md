# Python CLI – FX fetch & persist


## Setup
python3 -m venv .venv && source .venv/bin/activate
pip3 install -r requirements.txt
# adjust if needed
# cp .env.example .env 


## Run (interactive)
python -m src.main


## Run (non-interactive)
# Provide date and persist decision via flags
python -m src.main --date 2025-01-10 --persist yes --store postgres


# Options
# --date YYYY-MM-DD (required in non-interactive)
# --persist yes|no
# --store postgres|mongo|auto (auto = prefer Postgres when available, else Mongo)