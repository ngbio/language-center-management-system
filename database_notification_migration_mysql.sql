-- Run once on an existing database created before student notifications were implemented.
ALTER TABLE notification
    ADD COLUMN dedup_key VARCHAR(160) NULL AFTER read_at;

CREATE UNIQUE INDEX uq_notification_dedup_key
    ON notification(dedup_key);
