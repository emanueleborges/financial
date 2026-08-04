-- V6__create_daily_spent_view.sql
CREATE OR REPLACE VIEW v_daily_spent AS
SELECT
    payer_id AS user_id,
    DATE(created_at AT TIME ZONE 'UTC') AS spend_date,
    COALESCE(SUM(amount), 0) AS total_spent
FROM transactions
WHERE status IN ('COMPLETED', 'PROCESSING', 'PENDING')
  AND type = 'TRANSFER'
GROUP BY payer_id, DATE(created_at AT TIME ZONE 'UTC');
