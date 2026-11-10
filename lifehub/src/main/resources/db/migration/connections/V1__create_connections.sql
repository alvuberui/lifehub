CREATE TABLE IF NOT EXISTS connections.connection_requests (
    id BIGSERIAL PRIMARY KEY,
    requester_email VARCHAR(320) NOT NULL,
    recipient_email VARCHAR(320) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT connection_requests_different_users CHECK (LOWER(requester_email) <> LOWER(recipient_email)),
    CONSTRAINT connection_requests_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELLED'))
);

CREATE INDEX IF NOT EXISTS idx_connection_requests_recipient_status
    ON connections.connection_requests (recipient_email, status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_connection_requests_requester_status
    ON connections.connection_requests (requester_email, status, created_at DESC);
CREATE UNIQUE INDEX IF NOT EXISTS uq_connection_requests_pending_pair
    ON connections.connection_requests (LEAST(LOWER(requester_email), LOWER(recipient_email)), GREATEST(LOWER(requester_email), LOWER(recipient_email)))
    WHERE status = 'PENDING';

CREATE TABLE IF NOT EXISTS connections.connections (
    id BIGSERIAL PRIMARY KEY,
    user_a_email VARCHAR(320) NOT NULL,
    user_b_email VARCHAR(320) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT connections_different_users CHECK (LOWER(user_a_email) < LOWER(user_b_email)),
    CONSTRAINT connections_unique_pair UNIQUE (user_a_email, user_b_email)
);

CREATE INDEX IF NOT EXISTS idx_connections_user_a ON connections.connections (user_a_email);
CREATE INDEX IF NOT EXISTS idx_connections_user_b ON connections.connections (user_b_email);

CREATE TABLE IF NOT EXISTS connections.module_permissions (
    id BIGSERIAL PRIMARY KEY,
    connection_id BIGINT NOT NULL REFERENCES connections.connections(id) ON DELETE CASCADE,
    module_key VARCHAR(40) NOT NULL,
    permission VARCHAR(30) NOT NULL DEFAULT 'NONE',
    granted_by_email VARCHAR(320) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT module_permissions_unique_module UNIQUE (connection_id, module_key),
    CONSTRAINT module_permissions_permission CHECK (permission IN ('NONE', 'SUMMARY_READ', 'DETAIL_READ', 'EDIT', 'ADMIN'))
);

CREATE TABLE IF NOT EXISTS connections.blocked_users (
    id BIGSERIAL PRIMARY KEY,
    blocker_email VARCHAR(320) NOT NULL,
    blocked_email VARCHAR(320) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT blocked_users_different_users CHECK (LOWER(blocker_email) <> LOWER(blocked_email)),
    CONSTRAINT blocked_users_unique_pair UNIQUE (blocker_email, blocked_email)
);

CREATE INDEX IF NOT EXISTS idx_blocked_users_lookup
    ON connections.blocked_users (blocker_email, blocked_email);
