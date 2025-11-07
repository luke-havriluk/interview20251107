### Scenario 2
Here I will provide the further approach to the Scenario 2, building up on the [03_Scenario2_1.md](./03_Scenario2_1.md)

#### Scenario 2 definition
```text
Scenario 2: It should keep only the latest version for each ID (one row per ID, always the most up-to-date).
...
For scenario 2, we only want to keep the latest information per ID, replacing the old version if a newer one exists.
```
#### Scenario 2 solution
One could also think about another variation of that approach, by using the transactional logic and doing a <b>very costly</b> transaction, which would first compute the
compacted version of the `all_history` in a temp table, clean the previous version, and write the compacted temp table back into the `all_history`, thus keeping the constantly alternating version of the `all_history` table. 

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


One even further option would be to use the CTEs, and just perform the computation of the latest values with the following query: 

``` SQL 
WITH h AS (
  SELECT DISTINCT ON (id) id, value, created_at, updated_at
  FROM all_history
  ORDER BY id, updated_at DESC, created_at DESC
),
r AS (
  SELECT DISTINCT ON (id) id, value, created_at, updated_at
  FROM recent_updates
  ORDER BY id, updated_at DESC, created_at DESC
)
SELECT DISTINCT ON (id) id, value, created_at, updated_at
FROM (
  SELECT * FROM h
  UNION ALL
  SELECT * FROM r
) s
ORDER BY id, updated_at DESC, created_at DESC;
```
Here we compute the costly union of the two tables, in memory and then select the distinct values from that union, with the 

``` SQL
SELECT DISTINCT ON (id) id, value, created_at, updated_at
```
part. To cover the sorting during that slection from the computed union better we could introduce the additional indexes in our RDBMS: 

```SQL 
CREATE INDEX IF NOT EXISTS ix_all_history_id_updated_created
  ON all_history (id, updated_at DESC, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_recent_updates_id_updated_created
  ON recent_updates (id, updated_at DESC, created_at DESC);
```
This then helps to speed up the union computation due to "co-location option"(Composite indexes (id, updated_at DESC, created_at DESC) can reduce sort work and enable index-backed scans for the DISTINCT ON ordering.), and keeps the SQL short, readable, and efficient without a big UNION  or FULL JOIN over all rows.

HINT: Please still note, that as the tables grow, the resources for computing the UNION ALL as well as for maintainng the indexes would also grow. 



