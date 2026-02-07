ALTER TABLE simplefin_connections
    ADD COLUMN initial_sync_completed BOOLEAN NOT NULL DEFAULT FALSE;

