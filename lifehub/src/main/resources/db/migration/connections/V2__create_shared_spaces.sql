CREATE TABLE IF NOT EXISTS connections.shared_spaces (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    owner_email VARCHAR(320) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS connections.shared_space_members (
    id BIGSERIAL PRIMARY KEY,
    space_id BIGINT NOT NULL REFERENCES connections.shared_spaces(id) ON DELETE CASCADE,
    user_email VARCHAR(320) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT shared_space_members_unique UNIQUE (space_id, user_email),
    CONSTRAINT shared_space_members_role CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER'))
);

CREATE INDEX IF NOT EXISTS idx_shared_space_members_user ON connections.shared_space_members (user_email);

CREATE TABLE IF NOT EXISTS connections.space_module_permissions (
    id BIGSERIAL PRIMARY KEY,
    space_id BIGINT NOT NULL REFERENCES connections.shared_spaces(id) ON DELETE CASCADE,
    module_key VARCHAR(40) NOT NULL,
    permission VARCHAR(30) NOT NULL DEFAULT 'NONE',
    granted_by_email VARCHAR(320) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT space_module_permissions_unique UNIQUE (space_id, module_key),
    CONSTRAINT space_module_permissions_permission CHECK (permission IN ('NONE', 'SUMMARY_READ', 'DETAIL_READ', 'EDIT', 'ADMIN'))
);
