CREATE INDEX IF NOT EXISTS idx_movements_user_date
    ON movements (user_email, date);

CREATE INDEX IF NOT EXISTS idx_movements_user_tag
    ON movements (user_email, tag_id);

CREATE INDEX IF NOT EXISTS idx_movements_user_type_date
    ON movements (user_email, movement_type, date);
