CREATE INDEX IF NOT EXISTS idx_tags_user_email
    ON tags (user_email);

CREATE INDEX IF NOT EXISTS idx_tags_user_name
    ON tags (user_email, name);
