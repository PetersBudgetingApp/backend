ALTER TABLE simplefin_connections
    ADD COLUMN backfill_cursor_date DATE;

-- Force historical backfill for existing connections created before cursor support existed.
UPDATE simplefin_connections
SET initial_sync_completed = FALSE,
    backfill_cursor_date = NULL
WHERE initial_sync_completed = TRUE;

