-- V3__create_transaction_audit_table.sql
CREATE TABLE transaction_audit (
    id              UUID PRIMARY KEY,
    transaction_id  UUID         NOT NULL REFERENCES transactions (id),
    event           VARCHAR(100) NOT NULL,
    payload         JSONB,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_transaction ON transaction_audit (transaction_id);
CREATE INDEX idx_audit_event ON transaction_audit (event);
CREATE INDEX idx_audit_created_at ON transaction_audit (created_at);
