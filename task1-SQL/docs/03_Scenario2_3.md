### Scenario 2
Here I will provide the additional approach to the Scenario 2, building up on the [03_Scenario2_2.md](./03_Scenario2_2.md) with FULL OUTER JOIN instead of UNION ALL

#### Scenario 2 definition
```text
Scenario 2: It should keep only the latest version for each ID (one row per ID, always the most up-to-date).
...
For scenario 2, we only want to keep the latest information per ID, replacing the old version if a newer one exists.
```
#### Scenario 2 solution
As previously mentioned, the `UNION ALL` is quite costly, and we could utilize the FULL OUTER JOIN approach, to avoid a full-table UNION by comparing top-1 per id from each side.

For that, let us stick with a single query, without any temporary table, but with computing a FULL OUTER JOIN of two CTEs(one for updates, one for history) and trying to match them manually with the CASE matching. Here we use the COALESCE(r.id, h.id) approach, delivering the first non NULL value from at least one of the CTEs results. 

```SQL 
-- get the CTE for the larger table first
-- Latest-per-id from HISTORY (likely larger): pick top-1 by (updated_at, created_at)
WITH h AS (
  SELECT DISTINCT ON (id) id, value, created_at, updated_at
  FROM all_history
  ORDER BY id, updated_at DESC, created_at DESC
),
-- CTE for the smaller table
-- Latest-per-id from RECENT (usually smaller): same tie-breaker
r AS (
  SELECT DISTINCT ON (id) id, value, created_at, updated_at
  FROM recent_updates
  ORDER BY id, updated_at DESC, created_at DESC
)
-- join them with the FULL OUTER JOIN, see below, and select based on the case conditioning
SELECT
  COALESCE(r.id, h.id) AS id,  -- id from whichever side exists
  CASE                         -- choose the winning VALUE
    WHEN r.updated_at IS NOT NULL  -- recent has a row
     AND (h.updated_at IS NULL       -- …and history doesn’t
       OR r.updated_at > h.updated_at   -- …or RECENT is newer
       OR (r.updated_at = h.updated_at AND r.created_at > h.created_at)) -- …or same updated_at but RECENT created later
    THEN r.value ELSE h.value END AS value,
  CASE
    WHEN r.updated_at IS NOT NULL
     AND (h.updated_at IS NULL
       OR r.updated_at > h.updated_at
       OR (r.updated_at = h.updated_at AND r.created_at > h.created_at))
    THEN r.created_at ELSE h.created_at END AS created_at,
  -- final UPDATED_AT = the newer timestamp between both sides
  GREATEST(COALESCE(r.updated_at, 'epoch'::timestamp),    -- treat NULL as very old
           COALESCE(h.updated_at, 'epoch'::timestamp))    
           AS updated_at
FROM h
FULL OUTER JOIN r USING (id);                          -- keep ids present in either source
```

Now let us better explain the second case conditions in our large select. 
We have two possible rows per id:
- one from h = all_history (older snapshot),
- one from r = recent_updates (potentially newer).

Our goal:
→ keep only one row per id — the newer one.

Each row has:

- `updated_at` → when the record was last modified

- `created_at` → when this version itself was created (helps if updated_at ties)

So we need to decide:

> Should the chosen record come from `recent_updates (r)` or `all_history (h)`?

So:

- `r.updated_at IS NOT NULL`
→ Means we actually have a row for this id in recent_updates.
If not, then obviously we can’t pick from r, so we’ll fall to `ELSE h.created_at`.

- `h.updated_at IS NULL`
→ This id didn’t exist in all_history yet. Hence it is a completely new record, so we should take it from r.

- `r.updated_at > h.updated_at`
→ The record in recent_updates is newer than the one in all_history.
We replace the history version.

- `r.updated_at = h.updated_at AND r.created_at > h.created_at`
→ Both versions were updated at the same logical time,
but the r version was generated slightly later. We use created_at as a deterministic tiebreaker.


That CASE is basically saying:

“If the recent record exists and is newer (or equally new but slightly later-created),
then use the timestamps and values from it.
Otherwise, keep the existing historical version.”