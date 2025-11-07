--First approach with another table all_latest

-- Scenario 2: maintain one latest row per id
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