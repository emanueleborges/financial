package com.financialhub.infrastructure.kafka.producer;

import com.financialhub.application.port.out.TransactionEventPublisherPort;
import com.financialhub.domain.model.Transaction;
import com.financialhub.infrastructure.kafka.dto.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaTransactionEventPublisher implements TransactionEventPublisherPort {

    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    @Value("${app.kafka.topics.transaction-created}")
    private String createdTopic;

    @Value("${app.kafka.topics.transaction-completed}")
    private String completedTopic;

    @Value("${app.kafka.topics.transaction-failed}")
    private String failedTopic;

    @Override
    public void publishCreated(Transaction transaction) {
        publish(createdTopic, "transaction.created", transaction, null);
    }

    @Override
    public void publishCompleted(Transaction transaction) {
        publish(completedTopic, "transaction.completed", transaction, null);
    }

    @Override
    public void publishFailed(Transaction transaction, String reason) {
        publish(failedTopic, "transaction.failed", transaction, reason);
    }

    private void publish(String topic, String eventType, Transaction tx, String reason) {
        TransactionEvent event = TransactionEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .transactionId(tx.getId())
                .payerId(tx.getPayerId())
                .payeeId(tx.getPayeeId())
                .amount(tx.getAmount())
                .status(tx.getStatus())
                .type(tx.getType())
                .failureReason(reason != null ? reason : tx.getFailureReason())
                .occurredAt(Instant.now())
                .build();

        // Particionamento por transactionId garante ordem por transação
        kafkaTemplate.send(topic, tx.getId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Falha ao publicar evento {} no tópico {}: {}",
                                eventType, topic, ex.getMessage());
                    } else {
                        log.info("Evento {} publicado: tx={}, partition={}",
                                eventType, tx.getId(),
                                result.getRecordMetadata().partition());
                    }
                });
    }
}
