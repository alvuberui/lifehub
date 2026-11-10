CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    owner_email VARCHAR(255) NOT NULL,
    name VARCHAR(120) NOT NULL,
    color VARCHAR(32),
    icon VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_home_category_owner_name UNIQUE (owner_email, name)
);

CREATE INDEX idx_home_categories_owner ON categories (owner_email);

CREATE TABLE items (
    id BIGSERIAL PRIMARY KEY,
    owner_email VARCHAR(255) NOT NULL,
    name VARCHAR(180) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    category_id BIGINT NOT NULL REFERENCES categories(id),
    brand VARCHAR(120) NOT NULL,
    model VARCHAR(120) NOT NULL,
    serial_number VARCHAR(160),
    location VARCHAR(160),
    supplier VARCHAR(180) NOT NULL,
    status VARCHAR(32) NOT NULL,
    price NUMERIC(14, 2),
    currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
    purchase_date DATE NOT NULL,
    warranty_end_date DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    archived_at TIMESTAMPTZ,
    CONSTRAINT ck_home_item_status CHECK (status IN ('ACTIVE', 'SOLD', 'DONATED', 'LOST', 'RETIRED')),
    CONSTRAINT ck_home_item_dates CHECK (warranty_end_date >= purchase_date),
    CONSTRAINT ck_home_item_price CHECK (price IS NULL OR price >= 0)
);

CREATE INDEX idx_home_items_owner ON items (owner_email);
CREATE INDEX idx_home_items_owner_status ON items (owner_email, status);
CREATE INDEX idx_home_items_owner_warranty ON items (owner_email, warranty_end_date);
CREATE INDEX idx_home_items_category ON items (category_id);

CREATE TABLE attachments (
    id BIGSERIAL PRIMARY KEY,
    item_id BIGINT NOT NULL REFERENCES items(id) ON DELETE CASCADE,
    owner_email VARCHAR(255) NOT NULL,
    document_type VARCHAR(32) NOT NULL DEFAULT 'OTHER',
    original_filename VARCHAR(255) NOT NULL,
    storage_key VARCHAR(500) NOT NULL UNIQUE,
    content_type VARCHAR(120) NOT NULL,
    size_bytes BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_home_attachments_item ON attachments (item_id);
CREATE INDEX idx_home_attachments_owner ON attachments (owner_email);
