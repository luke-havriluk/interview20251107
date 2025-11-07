-- Scenario 1: append-only load (idempotent) from recent_updates -> all_history

--As discussed in the interview, 
-- let us use the shortest possible SQL Query for that
-- we first join the tables with the LEFT JOIN on record id
-- AND updated_at field
-- for those where the updated field is NULL we do the insertion, 
-- as it means we are having the new data for that id




INSERT INTO all_history (id, value, created_at, updated_at)
SELECT r.id, r.value, r.created_at, r.updated_at
FROM recent_updates r
LEFT JOIN all_history h
  ON h.id = r.id AND h.updated_at = r.updated_at
WHERE h.id IS NULL;
-- WHERE h.id IS NULL filters out existing versions,
-- so only brand-new (id, updated_at) pairs are inserted.


--Idempotency:
--   Running this multiple times won’t add duplicates because of the 
--   PRIMARY KEY (id, updated_at) on all_history.