# Intro
This repository contains the description of the interview tasks and their solution, for the interview with Andre and Biplob that took place on 7-th November 2025. 
Interview was based on two tasks, one for the SQL Syntax, the second one for the API programming and data structure. 


# Task 1 -SQL
First task is related to the SQL patterns in data engineering. 
The actual task can be found in [01_task.md](./task1-SQL/docs/01_task.md)
That task contains two scenarios. Further each of those scenarios would de described in details. 
### SQL Interview Practice — Scenario 1 (PostgreSQL)

This project provides a lightweight PostgreSQL setup (via Docker Compose) for practicing common **Data Engineering interview tasks** such as incremental data loading, append-only history tables, and analytical SQL.

#### Quick Start

```bash
cd task1-SQL/containers
docker-compose up -d
```

Check database initialization:

```bash
docker exec -it task1_sql_pg psql -U luke -d interview -c "SELECT COUNT(*) FROM all_history;"
```

Run **Scenario 1** (append-only load):

```bash
docker exec -i task1_sql_pg psql -U luke -d interview < ../sql/00_Scenario1.sql
```

Validate results:

```bash
docker exec -i task1_sql_pg psql -U luke -d interview < ../sql/checks.sql
```

Expected output:
```
all_history count = 110
recent_updates count = 10
```

---

####  Notes

- Designed for PostgreSQL 16 (compatible with Redshift SQL dialects).
- Uses idempotent `LEFT JOIN … WHERE h.id IS NULL` pattern for incremental loading.

### Scenario 1 (PostgreSQL) solution
The solution in form of a document, that references the SQl statement as well with the simple test to the first Scenatio can be found [here](./task1-SQL/docs/02_Scenario1.md)

### Scenario 2 (PostgreSQL) solution
Maintaining the Latest Version per ID in scenario 2. 
The **Scenario 2** exercises explore how to maintain a *“latest snapshot”* table — one row per ID — while still keeping an append-only audit trail in `all_history`.

| File | Description |
|------|--------------|
| [03_Scenario2_1.md](./task1-SQL/docs/03_Scenario2_1.md) | **Upsert-based approach** — maintains an `all_latest` table that stores only the newest row per ID. Uses `ON CONFLICT (id)` upserts from `recent_updates`. ⚠️ Requires a one-time **backfill** from `all_history` before daily increments. |
| [03_Scenario2_2.md](./task1-SQL/docs/03_Scenario2_2.md) | **Transactional “replace-all” approach** — computes a compacted version of `(all_history ∪ recent_updates)` into a temporary table, truncates `all_history`, and inserts the latest rows. Simple but heavy-weight. |
| [03_Scenario2_3.md](./task1-SQL/docs/03_Scenario2_3.md) | **Optimized FULL OUTER JOIN approach** — avoids a full-table UNION by comparing only the newest row per ID from each side (`all_history` vs `recent_updates`). Produces the same results with less I/O. |

### ⚙️ Execution order (recommended)

1. **Initialize the database**
   ```bash
   cd task1-SQL/containers
   docker-compose up -d
   ```
2. Run Scenario 1 to populate `all_history`
   ```bash
   docker exec -i task1_sql_pg psql -U luke -d interview < ../sql/00_Scenario1.sql
   ```
3. Choose one Scenario 2 approach
   ``` bash
   #It is recommended to chose one of the following:
   # a) upsert to all_latest
    docker exec -i task1_sql_pg psql -U luke -d interview < ../sql/01_Scenario2_1.sql   
   # b) temp-table replace
    docker exec -i task1_sql_pg psql -U luke -d interview < ../sql/01_Scenario2_2.sql   
   # c) full-join compute
     docker exec -i task1_sql_pg psql -U luke -d interview < ../sql/01_Scenario2_3.sql   
   ```
4.  Validate 
    ``` bash
    docker exec -it task1_sql_pg psql -U luke -d interview -c "SELECT COUNT(*) FROM all_history;"
    docker exec -it task1_sql_pg psql -U luke -d interview -c "SELECT COUNT(*) FROM all_latest;"
    ```


---

# Task 2 – API, Persistence and CLI Implementations

The second interview task was a **data-engineering-style API programming exercise**.  
It focuses on fetching foreign-exchange rates from an external REST API and persisting them
into both relational (PostgreSQL) and non-relational (MongoDB) stores.

##  Structure

| Folder | Description |
|---------|--------------|
| `task2-API/python/` | Minimal Python 3.12 CLI with optional Postgres and MongoDB persistence. |
| `task2-API/java/`   | Lean Spring Boot CLI (Java 14, Maven) mirroring the Python workflow. |
| `task2-API/db/`     | `docker-compose.yml` and sub-folders (`postgres/`, `mongodb/`) to start databases. |

---

##  Goal

Fetch currency-exchange data for a given date, validate input, pretty-print the results,
and—on user confirmation—store the data into available persistence engines.

### Key requirements

1. **User prompt** for date (`YYYY-MM-DD`)  
2. **Validation** of format and graceful error handling  
3. **Single data fetch** from [https://api.frankfurter.app](https://api.frankfurter.app)  
4. **Pretty-printed JSON output** of the retrieved rates  
5. **Optional persistence**  
   - If Postgres is reachable → ask to persist  
   - If MongoDB is reachable → ask to persist  
6. **Dockerized databases** to test both RDBMS and NoSQL variants

---

##  Python Implementation

- Located under `task2-API/python/`
- Uses `requests`, `psycopg`, `pymongo`, `python-dotenv`
- Detects available DB services automatically
- Example run:

```bash
python -m src.main
Enter date (YYYY-MM-DD): 2025-11-06
Fetched 30 rates for 2025-11-06 base EUR
Persist data? (yes/no): yes
Persist into Postgres? (yes/no): yes
Persisted 30 rows into Postgres.
Persist into MongoDB? (yes/no): yes
Upserted 1 document into MongoDB.
```
---
---

## Java (Spring Boot 14) Implementation

Located under task2-API/java/

Built with Maven and Spring Boot 2.7.x (Java 14-compatible)

Uses RestTemplate, JdbcTemplate, and Mongo Java driver 4.6

Non-web CLI (spring.main.web-application-type=none)

Same interactive flow as the Python version
Run:
```bash
mvn -q -DskipTests clean package
java -jar target/fx-cli-0.0.1.jar
```
---

## Persistence Layer Details

PostgreSQL:
Table fx_rates with rate_date DATE, base_currency TEXT, currency TEXT, rate NUMERIC, and payload_json JSONB
Uses ON CONFLICT (rate_date, base_currency, currency) upsert pattern.

MongoDB:
Collection fx_daily, document-per-date structure:
```json
{
  "_id": "2025-11-06|EUR",
  "rate_date": "2025-11-06",
  "base": "EUR",
  "rates": { "USD": 1.1533, ... },
  "fetched_at": ISODate(...),
  "raw": { ...original JSON... }
}

```


Data-Model Design Considerations for Analysts

While storing the full API payload as `JSONB` or raw JSON documents is convenient
for ingestion and quick debugging, it’s sub-optimal for analytical use cases.

### PostgreSQL – Relational & Aggregation-Friendly
For analysts who will query, aggregate, or join with other datasets,
it’s better to normalize the rates table.

Instead of a single JSON payload per fetch, keep a **fact table** with a composite key:

| Column            | Type      | Example      | Description |
|-------------------|-----------|---------------|-------------|
| `rate_date`       | `DATE`    | `2025-11-06` | Trading date |
| `currency_from`   | `TEXT`    | `EUR`         | Base currency |
| `currency_to`     | `TEXT`    | `USD`         | Quote currency |
| `rate`            | `NUMERIC` | `1.1533`      | FX rate |
| `source`          | `TEXT`    | `frankfurter` | Data source |
| `fetched_at`      | `TIMESTAMPTZ` | `2025-11-07 16:21:00Z` | Ingestion time |

This structure supports:
- Time-series queries (e.g. moving averages, volatility)
- Grouping by base or quote currency
- Joins with macroeconomic or trade data

You can still keep the raw JSON in a side table for traceability,
but analytics should operate on this flattened structure.

Example query for a 7-day rolling average:
```sql
SELECT rate_date, currency_from, currency_to,
       AVG(rate) OVER (PARTITION BY currency_from, currency_to ORDER BY rate_date ROWS 6 PRECEDING) AS rate_avg_7d
FROM fx_rates_flat
WHERE currency_from = 'EUR' AND currency_to = 'USD';
```

### Mongodb 

For MongoDB, analysts may prefer a document-per-pair-per-day layout
instead of a single large object of rates:

``` json
{
  "_id": "2025-11-06|EUR|USD",
  "rate_date": "2025-11-06",
  "currency_from": "EUR",
  "currency_to": "USD",
  "rate": 1.1533,
  "source": "frankfurter",
  "fetched_at": ISODate("2025-11-07T16:21:00Z")
}

```

Create indexes on { rate_date: 1, currency_from: 1, currency_to: 1 }
to efficiently support time-series filtering and aggregation pipelines
such as:
``` javascript
db.fx_rates.aggregate([
  { $match: { currency_from: "EUR", currency_to: "USD" } },
  { $group: { _id: "$rate_date", avg_rate: { $avg: "$rate" } } },
  { $sort: { _id: 1 } }
]);
```