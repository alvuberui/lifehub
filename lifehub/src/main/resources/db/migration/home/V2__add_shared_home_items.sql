ALTER TABLE items
    ADD COLUMN shared BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_home_items_shared_owner
    ON items (shared, owner_email, archived_at, warranty_end_date);
