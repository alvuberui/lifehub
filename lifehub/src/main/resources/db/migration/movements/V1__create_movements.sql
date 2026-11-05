CREATE TABLE IF NOT EXISTS movements (
    id BIGSERIAL PRIMARY KEY,
    user_email VARCHAR(255) NOT NULL,
    movement_type VARCHAR(255) NOT NULL,
    amount NUMERIC NOT NULL,
    date DATE NOT NULL,
    tag_id BIGINT,
    description VARCHAR(255) NOT NULL
);
