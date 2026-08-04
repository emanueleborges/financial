package com.financialhub.infrastructure.kafka.consumer;

import com.financialhub.application.port.out.EventIdempotencyPort;
import com.financialhub.application.port.out.NotificationPort;
import com.financialhub.application.port.out.UserRepositoryPort;
import com.financialhub.infrastructure.kafka.dto.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private static final String CONSUMER_NAME = "NotificationConsumer";

    private final EventIdempotencyPort idempotencyPort;
    private final NotificationPort notificationPort;
    private final UserRepositoryPort userRepository;
    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    @KafkaListener(
            topics = "${app.kafka.topics.transaction-completed}",
            groupId = "notification-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onCompleted(TransactionEvent event) {
        try {
            if (idempotencyPort.alreadyProcessed(event.getEventId(), CONSUMER_NAME)) {
                return;
            }

            userRepository.findById(event.getPayerId()).ifPresent(payer ->
                    notificationPort.sendTransferNotification(
                            payer.getEmail(),
                            String.format("Transferência de R$ %s realizada com sucesso. ID: %s",
                                    event.getAmount(), event.getTransactionId())
                    )
            );

            userRepository.findById(event.getPayeeId()).ifPresent(payee ->
                    notificationPort.sendTransferNotification(
                            payee.getEmail(),
                            String.format("Você recebeu R$ %s. ID: %s",
                                    event.getAmount(), event.getTransactionId())
                    )
            );

            idempotencyPort.markProcessed(event.getEventId(), event.getTransactionId(), CONSUMER_NAME);
        } catch (Exception ex) {
            log.error("Erro no NotificationConsumer: {}", ex.getMessage());
            kafkaTemplate.send("transaction.dlq", event.getTransactionId().toString(), event);
        }
    }

    @KafkaListener(
            topics = "${app.kafka.topics.transaction-failed}",
            groupId = "notification-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onFailed(TransactionEvent event) {
        try {
            String eventKey = event.getEventId() + "-failed";
            if (idempotencyPort.alreadyProcessed(eventKey, CONSUMER_NAME)) {
                return;
            }

            userRepository.findById(event.getPayerId()).ifPresent(payer ->
                    notificationPort.sendTransferNotification(
                            payer.getEmail(),
                            String.format("Transferência falhou: %s. ID: %s",
                                    event.getFailureReason(), event.getTransactionId())
                    )
            );

            idempotencyPort.markProcessed(eventKey, event.getTransactionId(), CONSUMER_NAME);
        } catch (Exception ex) {
            log.error("Erro ao notificar falha: {}", ex.getMessage());
            kafkaTemplate.send("transaction.dlq", event.getTransactionId().toString(), event);
        }
    }
}
