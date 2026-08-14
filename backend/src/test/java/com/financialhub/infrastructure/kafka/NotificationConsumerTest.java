package com.financialhub.infrastructure.kafka;

import com.financialhub.application.port.out.EventIdempotencyPort;
import com.financialhub.application.port.out.NotificationPort;
import com.financialhub.application.port.out.UserRepositoryPort;
import com.financialhub.domain.enums.UserStatus;
import com.financialhub.domain.model.User;
import com.financialhub.infrastructure.kafka.consumer.NotificationConsumer;
import com.financialhub.infrastructure.kafka.dto.TransactionEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationConsumerTest {

    @Mock private EventIdempotencyPort idempotencyPort;
    @Mock private NotificationPort notificationPort;
    @Mock private UserRepositoryPort userRepository;
    @Mock private KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    private NotificationConsumer consumer;
    private UUID payerId;
    private UUID payeeId;
    private TransactionEvent event;

    @BeforeEach
    void setUp() {
        consumer = new NotificationConsumer(idempotencyPort, notificationPort, userRepository, kafkaTemplate);
        payerId = UUID.randomUUID();
        payeeId = UUID.randomUUID();
        event = TransactionEvent.builder()
                .eventId("evt-1")
                .transactionId(UUID.randomUUID())
                .payerId(payerId)
                .payeeId(payeeId)
                .amount(new BigDecimal("10.00"))
                .failureReason("saldo")
                .occurredAt(Instant.now())
                .build();
    }

    @Test
    void onCompletedNotifiesPayerAndPayee() {
        when(idempotencyPort.alreadyProcessed("evt-1", "NotificationConsumer")).thenReturn(false);
        when(userRepository.findById(payerId)).thenReturn(Optional.of(user(payerId, "alice@test.com")));
        when(userRepository.findById(payeeId)).thenReturn(Optional.of(user(payeeId, "bob@test.com")));

        consumer.onCompleted(event);

        verify(notificationPort).sendTransferNotification(eq("alice@test.com"), any());
        verify(notificationPort).sendTransferNotification(eq("bob@test.com"), any());
        verify(idempotencyPort).markProcessed("evt-1", event.getTransactionId(), "NotificationConsumer");
    }

    @Test
    void onCompletedSkipsDuplicate() {
        when(idempotencyPort.alreadyProcessed("evt-1", "NotificationConsumer")).thenReturn(true);

        consumer.onCompleted(event);

        verify(notificationPort, never()).sendTransferNotification(any(), any());
    }

    @Test
    void onFailedNotifiesPayer() {
        when(idempotencyPort.alreadyProcessed("evt-1-failed", "NotificationConsumer")).thenReturn(false);
        when(userRepository.findById(payerId)).thenReturn(Optional.of(user(payerId, "alice@test.com")));

        consumer.onFailed(event);

        verify(notificationPort).sendTransferNotification(eq("alice@test.com"), any());
        verify(idempotencyPort).markProcessed("evt-1-failed", event.getTransactionId(), "NotificationConsumer");
    }

    @Test
    void onCompletedSendsDlqOnError() {
        when(idempotencyPort.alreadyProcessed("evt-1", "NotificationConsumer")).thenThrow(new RuntimeException("db"));

        consumer.onCompleted(event);

        verify(kafkaTemplate).send("transaction.dlq", event.getTransactionId().toString(), event);
    }

    private static User user(UUID id, String email) {
        return User.builder()
                .id(id)
                .name("N")
                .email(email)
                .document("52998224725")
                .passwordHash("h")
                .balance(new BigDecimal("1"))
                .status(UserStatus.ACTIVE)
                .dailyLimit(new BigDecimal("100"))
                .version(0L)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
