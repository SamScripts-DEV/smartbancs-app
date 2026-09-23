CREATE TABLE transaction (
    id BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(64) NOT NULL UNIQUE,
    source_account_id BIGINT NOT NULL REFERENCES account(id),
    destination_account_id BIGINT NOT NULL REFERENCES account(id),
    amount NUMERIC(19, 4) NOT NULL CHECK (amount > 0),
    status VARCHAR(20) NOT NULL,
    trace_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_transactions_source_account ON transaction (source_account_id);
CREATE INDEX idx_transactions_destination_account ON transaction (destination_account_id);