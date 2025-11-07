-- should become 110 after first run
SELECT COUNT(*) AS all_history_count FROM all_history;

-- re-run the INSERT above -> count stays the same
SELECT COUNT(*) AS all_history_after_second_run FROM all_history;

-- optional: prove only new versions landed (ids 91..100)
SELECT id, value, updated_at
FROM all_history
WHERE id BETWEEN 91 AND 100
ORDER BY id, updated_at;

-- prove no duplicates
SELECT id, updated, COUNT(*) c
FROM all_history
GROUP BY 1,2
HAVING COUNT(*) > 1;
