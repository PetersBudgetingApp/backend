UPDATE simplefin_connections
SET initial_sync_completed = FALSE
WHERE initial_sync_completed = TRUE
  AND backfill_cursor_date IS NOT NULL;

