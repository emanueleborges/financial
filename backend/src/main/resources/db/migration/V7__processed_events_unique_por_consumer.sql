-- V7__processed_events_unique_por_consumer.sql
-- A V4 criou event_id UNIQUE além do índice (event_id, consumer_name). Com isso só o primeiro
-- consumer de um evento conseguia registrá-lo; os demais falhavam com chave duplicada e o Kafka
-- reprocessava a mensagem (o DailyReportConsumer somava de novo a cada tentativa).
-- A idempotência é por evento E consumer: fica só o índice composto.
ALTER TABLE processed_events DROP CONSTRAINT IF EXISTS processed_events_event_id_key;
