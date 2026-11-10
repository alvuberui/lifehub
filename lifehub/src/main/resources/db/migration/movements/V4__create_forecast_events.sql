CREATE TABLE IF NOT EXISTS forecast_events (
    id BIGSERIAL PRIMARY KEY,
    user_email VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    event_month DATE NOT NULL,
    amount NUMERIC(15, 2) NOT NULL CHECK (amount > 0),
    event_type VARCHAR(20) NOT NULL CHECK (event_type IN ('EXPENSE', 'INCOME')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_forecast_events_user_month
    ON forecast_events (user_email, event_month);
