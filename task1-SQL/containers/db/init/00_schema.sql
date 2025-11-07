-- Schema for interview practice (PostgreSQL)
-- Tables:
--   - all_history: append-only, keeps every version (Scenario 1 base)
--   - recent_updates: last 7 days of inserts/updates (staging)



-- All historical versions
CREATE TABLE IF NOT EXISTS all_history (
    id          INT         NOT NULL,
    value       VARCHAR     NOT NULL,
    created_at  TIMESTAMP   NOT NULL,
    updated_at  TIMESTAMP   NOT NULL,
    -- Prevent exact-duplicate versions per (id, updated_at)
    CONSTRAINT all_history_pk PRIMARY KEY (id, updated_at)
);

-- Staging: last 7 days
CREATE TABLE IF NOT EXISTS recent_updates (
    id          INT         NOT NULL,
    value       VARCHAR     NOT NULL,
    created_at  TIMESTAMP   NOT NULL,
    updated_at  TIMESTAMP   NOT NULL
);
-- For the scenario 2 we create a separate table, 
-- that keeps only the most recent version of each record (Scenario 2)
-- here the simplest DDL for it
CREATE TABLE IF NOT EXISTS all_latest (
    id INT PRIMARY KEY,
    value VARCHAR NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

--- For union computation in the second scenario we could use the additional indexes
CREATE INDEX IF NOT EXISTS ix_all_history_id_updated_created
  ON all_history (id, updated_at DESC, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_recent_updates_id_updated_created
  ON recent_updates (id, updated_at DESC, created_at DESC);

-- Helpful indexes for join/merge operations
CREATE INDEX IF NOT EXISTS ix_recent_updates_id_updated
    ON recent_updates (id, updated_at);

CREATE INDEX IF NOT EXISTS ix_all_history_id_updated
    ON all_history (id, updated_at);
