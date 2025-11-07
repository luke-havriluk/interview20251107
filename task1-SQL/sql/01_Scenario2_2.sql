--Second approach with UNION ALL

-- Scenario 2: maintain one latest row per id
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
