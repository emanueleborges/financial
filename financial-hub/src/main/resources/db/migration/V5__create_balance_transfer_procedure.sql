-- V5__create_balance_transfer_procedure.sql
-- Stored procedure com SELECT ... FOR UPDATE para atomicidade
CREATE OR REPLACE FUNCTION transfer_balance(
    p_payer_id UUID,
    p_payee_id UUID,
    p_amount   NUMERIC(19, 2)
) RETURNS VOID AS $$
DECLARE
    v_payer_balance NUMERIC(19, 2);
    v_payer_status  VARCHAR(20);
    v_payee_status  VARCHAR(20);
BEGIN
    -- Lock das linhas na ordem de ID para evitar deadlock
    IF p_payer_id < p_payee_id THEN
        SELECT balance, status INTO v_payer_balance, v_payer_status
        FROM users WHERE id = p_payer_id FOR UPDATE;

        SELECT status INTO v_payee_status
        FROM users WHERE id = p_payee_id FOR UPDATE;
    ELSE
        SELECT status INTO v_payee_status
        FROM users WHERE id = p_payee_id FOR UPDATE;

        SELECT balance, status INTO v_payer_balance, v_payer_status
        FROM users WHERE id = p_payer_id FOR UPDATE;
    END IF;

    IF v_payer_status IS NULL THEN
        RAISE EXCEPTION 'PAYER_NOT_FOUND';
    END IF;

    IF v_payee_status IS NULL THEN
        RAISE EXCEPTION 'PAYEE_NOT_FOUND';
    END IF;

    IF v_payer_status <> 'ACTIVE' THEN
        RAISE EXCEPTION 'PAYER_INACTIVE';
    END IF;

    IF v_payee_status <> 'ACTIVE' THEN
        RAISE EXCEPTION 'PAYEE_INACTIVE';
    END IF;

    IF v_payer_balance < p_amount THEN
        RAISE EXCEPTION 'INSUFFICIENT_BALANCE';
    END IF;

    UPDATE users SET balance = balance - p_amount, updated_at = NOW(), version = version + 1
    WHERE id = p_payer_id;

    UPDATE users SET balance = balance + p_amount, updated_at = NOW(), version = version + 1
    WHERE id = p_payee_id;
END;
$$ LANGUAGE plpgsql;
