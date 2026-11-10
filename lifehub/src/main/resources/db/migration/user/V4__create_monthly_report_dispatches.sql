CREATE TABLE IF NOT EXISTS monthly_report_dispatches (
    id BIGSERIAL PRIMARY KEY,
    user_email VARCHAR(255) NOT NULL,
    report_month DATE NOT NULL,
    status VARCHAR(32) NOT NULL,
    last_attempt_at TIMESTAMPTZ,
    sent_at TIMESTAMPTZ,
    provider_message_id VARCHAR(255),
    error_message VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_monthly_report_user_month UNIQUE (user_email, report_month)
);

CREATE INDEX IF NOT EXISTS idx_monthly_report_dispatches_status
    ON monthly_report_dispatches (status, report_month);
