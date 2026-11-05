ALTER TABLE tags
    ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS tag_deletion_operations (
    id BIGSERIAL PRIMARY KEY,
    tag_id BIGINT NOT NULL,
    user_email VARCHAR(255) NOT NULL,
    state VARCHAR(32) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_error VARCHAR(1000),
    CONSTRAINT uk_tag_deletion_operation_tag_user UNIQUE (tag_id, user_email)
);

CREATE INDEX IF NOT EXISTS idx_tag_deletion_operations_pending
    ON tag_deletion_operations (state, next_attempt_at);
