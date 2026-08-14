package com.financialhub.notification.infrastructure.kafka;

import com.financialhub.notification.application.NotificationInboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationInboxService inbox;
    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    @KafkaListener(topics = "transaction.completed", groupId = "notification-group")
    public void onCompleted(TransactionEvent event) {
        try {
            inbox.saveIfAbsent(
                    event.getEventId(),
                    event.getPayerEmail(),
                    event.getPayerDocument(),
                    String.format("Transferência de R$ %s realizada com sucesso. ID: %s",
                            event.getAmount(), event.getTransactionId())
            );
            inbox.saveIfAbsent(
                    event.getEventId(),
                    event.getPayeeEmail(),
                    event.getPayeeDocument(),
                    String.format("Você recebeu R$ %s. ID: %s",
                            event.getAmount(), event.getTransactionId())
            );
        } catch (Exception ex) {
            log.error("Erro no NotificationConsumer: {}", ex.getMessage());
            kafkaTemplate.send("transaction.dlq",
                    event.getTransactionId() == null ? "unknown" : event.getTransactionId().toString(),
                    event);
        }
    }

    @KafkaListener(topics = "transaction.failed", groupId = "notification-group")
    public void onFailed(TransactionEvent event) {
        try {
            inbox.saveIfAbsent(
                    event.getEventId() + "-failed",
                    event.getPayerEmail(),
                    event.getPayerDocument(),
                    String.format("Transferência falhou: %s. ID: %s",
                            event.getFailureReason(), event.getTransactionId())
            );
        } catch (Exception ex) {
            log.error("Erro ao notificar falha: {}", ex.getMessage());
            kafkaTemplate.send("transaction.dlq",
                    event.getTransactionId() == null ? "unknown" : event.getTransactionId().toString(),
                    event);
        }
    }
}
