-- V4__create_processed_events_table.sql
-- Idempotência de consumidores Kafka
CREATE TABLE processed_events (
    id              UUID PRIMARY KEY,
    event_id        VARCHAR(100) NOT NULL UNIQUE,
    transaction_id  UUID         NOT NULL,
    consumer_name   VARCHAR(100) NOT NULL,
    processed_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_processed_events_tx ON processed_events (transaction_id);
CREATE UNIQUE INDEX idx_processed_events_unique ON processed_events (event_id, consumer_name);
