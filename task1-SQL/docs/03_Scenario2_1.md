### Scenario 2
Here I will build the scenario 2 based on the scenario 1 answer. 
So , let us first start with the definition of the scenario 2

#### Scenario 2 definition
```text
Scenario 2: It should keep only the latest version for each ID (one row per ID, always the most up-to-date).
...
For scenario 2, we only want to keep the latest information per ID, replacing the old version if a newer one exists.
```
#### Scenario 2 solution
In order keep the Scenario 1 query working , and not change the `all_history` table with those queries, I would create a “current state” table — a compact version of `all_history` that keeps only the most up-to-date row per id.

It’s not replacing `all_history`; it complements it, like:

```text
recent_updates  →  all_history  →  all_latest
   (daily feed)      (full log)     (latest snapshot)
```

In this way, we have the option for the following setup: 
- `all_history` keeps every version (Scenario 1): an audit trail.
   It grows indefinitely, append-only.
- `all_latest` table keeps only the most recent version of each record (Scenario 2):
   One row per id, updated as new data arrives.

So:

- Scenario 1 = build the full timeline (`all_history`)
- Scenario 2 = maintain the current truth (`all_latest`),“one-row-per-id” table,
               always reflecting the most up-to-date data.

For that I would write the query, that works as follows: 
1. From `recent_updates` , take the newest record per `id`.
2. Insert it into `all_latest`.
3. If the `id` already exists in `all_latest`, replace it <b>only if</b> the new row’s `updated_at` is newer.

Here is the corresponding query(for daily increments): 

``` SQL
INSERT INTO all_latest (id, value, created_at, updated_at)

SELECT DISTINCT ON (r.id)
       r.id, r.value, r.created_at, r.updated_at
FROM recent_updates r

ORDER BY r.id, r.updated_at DESC, r.created_at DESC

ON CONFLICT (id) DO UPDATE

SET value      = EXCLUDED.value,
    created_at = EXCLUDED.created_at,
    updated_at = EXCLUDED.updated_at

WHERE EXCLUDED.updated_at > all_latest.updated_at;
```

Thus, every id in `all_latest` represents the latest known state of that entity. 
The query is idempotent and always ends up with the same latest snapshot.


### Notes
One could also think about another variation of that approach, by using the transactional logic and doing a <b>very costly</b> transaction, which would first compute the
compacted version of the `all_history` in a temp table, clean the previousl version, and write the compacted into the `all_history`, thus keeping the constantly altercating version of the `all_history` table. 

``` SQL 
BEGIN;

-- Build the desired end-state: newest row per id from (all_history ∪ recent_updates)
CREATE TEMP TABLE _latest AS
SELECT DISTINCT ON (id)
       id, value, created_at, updated_at
FROM (
  SELECT * FROM all_history
  UNION ALL
  SELECT * FROM recent_updates
) s
ORDER BY id, updated_at DESC, created_at DESC;

-- Replace all_history with that exact snapshot
TRUNCATE all_history;
INSERT INTO all_history (id, value, created_at, updated_at)
SELECT id, value, created_at, updated_at
FROM _latest;

DROP TABLE _latest;

COMMIT;
```

However, that approach has following downsides: 
- `all_history` naming is now messed up
- operation is very costly
- this could affect the other queries, that have previusly relied on the correctness of the `all_history`



## ⚠️ IMPORTANT: `all_latest` only updates IDs present in `recent_updates`

The upsert query presented above processes **only the IDs that appear in `recent_updates`**.  
If you want `all_latest` to contain the **latest row for every ID**, you must **backfill from `all_history` first** (one row per ID), and then run the daily upsert from `recent_updates`.

### One-time setup (create + backfill from history)
```sql
-- Create the one-row-per-id table if needed
CREATE TABLE IF NOT EXISTS all_latest (
  id INT PRIMARY KEY,
  value VARCHAR NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);

-- Backfill latest row per id from all_history (idempotent)
INSERT INTO all_latest AS t (id, value, created_at, updated_at)
SELECT DISTINCT ON (h.id)
       h.id, h.value, h.created_at, h.updated_at
FROM all_history h
ORDER BY h.id, h.updated_at DESC, h.created_at DESC
ON CONFLICT (id) DO UPDATE
SET value      = EXCLUDED.value,
    created_at = EXCLUDED.created_at,
    updated_at = EXCLUDED.updated_at
WHERE EXCLUDED.updated_at > t.updated_at
   OR (EXCLUDED.updated_at = t.updated_at AND EXCLUDED.created_at > t.created_at);
```

So should you desire, at some point to include all IDs from both tables into the `allo_latest` table with this approach:
1. create the `all_latest`table 
2. Backfill latest row per id from all_history 
3. run daily update with the first query (for daily increments) presented above