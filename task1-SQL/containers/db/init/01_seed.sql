-- Seed data:
-- - 100 rows in all_history (ids 1..100), older timestamps
-- - 10 rows in recent_updates (ids 91..100) with newer timestamps (simulate updates)

-- 100 historical rows (updated_at ~ 10 days ago, spaced by minutes)
INSERT INTO all_history (id, value, created_at, updated_at)
SELECT
    gs AS id,
    'value_' || gs AS value,
    (NOW() - INTERVAL '10 days') + (gs * INTERVAL '1 minute') AS created_at,
    (NOW() - INTERVAL '10 days') + (gs * INTERVAL '1 minute') AS updated_at
FROM generate_series(1, 100) AS gs;

-- 10 recent updates for ids 91..100 (newer updated_at ~ 1 day ago)
INSERT INTO recent_updates (id, value, created_at, updated_at)
SELECT
    gs AS id,
    'value_' || gs || '_new' AS value,
    (NOW() - INTERVAL '1 day') + (gs * INTERVAL '1 minute') AS created_at,
    (NOW() - INTERVAL '1 day') + (gs * INTERVAL '1 minute') AS updated_at
FROM generate_series(91, 100) AS gs;

-- Optional sanity check (safe to keep; Postgres init will ignore the output)
-- SELECT count(*) AS all_history_rows FROM all_history;
-- SELECT count(*) AS recent_updates_rows FROM recent_updates;
