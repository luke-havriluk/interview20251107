--Second approach with a FULL OUTER JOIN

-- Scenario 2: maintain one latest row per id

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