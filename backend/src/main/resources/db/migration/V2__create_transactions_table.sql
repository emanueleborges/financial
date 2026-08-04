-- V2__create_transactions_table.sql
CREATE TABLE transactions (
    id                UUID PRIMARY KEY,
    payer_id          UUID           NOT NULL REFERENCES users (id),
    payee_id          UUID           NOT NULL REFERENCES users (id),
    amount            NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    status            VARCHAR(30)    NOT NULL DEFAULT 'PENDING',
    type              VARCHAR(30)    NOT NULL DEFAULT 'TRANSFER',
    idempotency_key   VARCHAR(100)   UNIQUE,
    failure_reason    VARCHAR(500),
    original_tx_id    UUID           REFERENCES transactions (id),
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    completed_at      TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_tx_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'REVERSED')),
    CONSTRAINT chk_tx_type CHECK (type IN ('TRANSFER', 'REVERSAL')),
    CONSTRAINT chk_different_parties CHECK (payer_id <> payee_id)
);

CREATE INDEX idx_transactions_payer ON transactions (payer_id);
CREATE INDEX idx_transactions_payee ON transactions (payee_id);
CREATE INDEX idx_transactions_status ON transactions (status);
CREATE INDEX idx_transactions_created_at ON transactions (created_at);
CREATE INDEX idx_transactions_payer_created ON transactions (payer_id, created_at);
