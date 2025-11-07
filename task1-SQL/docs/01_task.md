We have the following tables:

recent_updates, which contains all new inserts and updates from the last 7 days.

all_history, which has all present and past rows for every ID, depending on the scenario:

Scenario 1: It should keep every change (every version) for every ID.

Scenario 2: It should keep only the latest version for each ID (one row per ID, always the most up-to-date).

Each day, we'd like to move new information (inserts or updates) from recent_updates into all_history. For scenario 1, we must make sure to copy every new change without overwriting or deleting old rows, and to avoid duplicates.
For scenario 2, we only want to keep the latest information per ID, replacing the old version if a newer one exists.

How would you design a process that moves the right rows from recent_updates to all_history for each scenario?
What would your queries look like?



Scenario
Table 1: recent_updates (contains data from the last 7 days, uses TTL for auto-deletion)
Table 2: all_history (append-only, contains all historical data, including everything from recent_updates)
Objective
Periodically move new records from recent_updates to all_history
Do not delete or modify any existing records in all_history ("append-only" policy)
Ensure there is no duplication
Sample Schema
``` SQL
-- Recent 7-day data (auto-TTL purges old rows)
CREATE TABLE recent_updates (
    id INT,
    value VARCHAR,
    creat_at TIMESTMAP,
    updated TIMESTAMP
);
-- Full historical append-only table
CREATE TABLE all_history (
    id INT,
    value VARCHAR,
    create_at TIMESTAMP,
    updated TIMESTAMP
);
```