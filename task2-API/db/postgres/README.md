# Postgres quick start

## Start only Postgres
docker compose up -d db-postgres

## Connection
Host: localhost
Port: 5432 (or ${PG_PORT})
DB:   rates (or ${PG_DB})
User: app (or ${PG_USER})
Pass: app (or ${PG_PASSWORD})

## CLI check
psql "postgresql://app:app@localhost:5432/rates" -c "\dt"
psql "postgresql://app:app@localhost:5432/rates" -c "select count(*) from fx_rates;"
