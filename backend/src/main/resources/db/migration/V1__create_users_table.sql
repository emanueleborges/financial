-- V1__create_users_table.sql
CREATE TABLE users (
    id              UUID PRIMARY KEY,
    name            VARCHAR(150) NOT NULL,
    email           VARCHAR(255) NOT NULL UNIQUE,
    document        VARCHAR(14)  NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    balance         NUMERIC(19, 2) NOT NULL DEFAULT 0 CHECK (balance >= 0),
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    daily_limit     NUMERIC(19, 2) NOT NULL DEFAULT 5000.00,
    version         BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'BLOCKED'))
);

CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_document ON users (document);
CREATE INDEX idx_users_status ON users (status);
