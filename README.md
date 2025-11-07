# Intro
This repository contains the description of the interview tasks and their solution, for the interview with Andre and Biplob that took place on 7-th November 2025. 
Interview was based on two tasks, one for the SQL Syntax, the second one for the API programming and data structure. 


# Task 1 -SQL
First task is related to the SQL patterns in data engineering. 
The actual task can be found in [01_task.md](./task1-SQL/docs/01_task.md)
That task contains two scenarios. Further each of those scenarios would de described in details. 
### SQL Interview Practice — Scenario 1 (PostgreSQL)

This project provides a lightweight PostgreSQL setup (via Docker Compose) for practicing common **Data Engineering interview tasks** such as incremental data loading, append-only history tables, and analytical SQL.

---

#### 📂 Project Structure

```
task1-SQL/
├── containers/
│   ├── db/
│   │   └── init/
│   │       ├── 00_schema.sql   # Creates tables all_history & recent_updates
│   │       └── 01_seed.sql     # Inserts sample test data
│   ├── docker-compose.yml      # Defines the PostgreSQL container
│   └── .env                    # Container configuration (user, password, etc.)
└── sql/
    ├── 00_Scenario1.sql        # Append-only insert from recent_updates -> all_history
    └── checks.sql              # Verification queries after Scenario 1 run
```

---

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