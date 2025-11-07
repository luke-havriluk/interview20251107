# MongoDB quick start

## Start only Mongo
docker compose up -d db-mongo

## Connection (app user)
URI (no authSource needed because user is on the app DB):
mongodb://app:app@localhost:27017/rates

## CLI check (requires mongosh locally)
mongosh "mongodb://root:root@localhost:27017/admin" --eval "db.adminCommand({ ping: 1 })"
mongosh "mongodb://app:app@localhost:27017/rates" --eval "db.fx_daily.stats()"
