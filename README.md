# Intro
This repository contains the description of the interview tasks and their solution, for the interview with Andre and Biplob that took place on 7-th November 2025. 
Interview was based on two tasks, one for the SQL Syntax, the second one for the API programming and data structure. 


# Task 1 -SQL
First task is related to the SQL patterns in data engineering. 
The actual task can be found in [01_task.md](./task1-SQL/docs/01_task.md)
That task contains two scenarios. Further each of those scenarios would de described in details. 
### SQL Interview Practice — Scenario 1 (PostgreSQL)

This project provides a lightweight PostgreSQL setup (via Docker Compose) for practicing common **Data Engineering interview tasks** such as incremental data loading, append-only history tables, and analytical SQL.

#### 🚀 Quick Start

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

#### 🧩 Notes

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
