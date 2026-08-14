package com.financialhub.notification;

import com.financialhub.notification.application.NotificationInboxService;
import com.financialhub.notification.infrastructure.kafka.NotificationConsumer;
import com.financialhub.notification.infrastructure.kafka.TransactionEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationConsumerTest {

    @Mock private NotificationInboxService inbox;
    @Mock private KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    private NotificationConsumer consumer;
    private TransactionEvent event;

    @BeforeEach
    void setUp() {
        consumer = new NotificationConsumer(inbox, kafkaTemplate);
        event = TransactionEvent.builder()
                .eventId("evt-9")
                .transactionId(UUID.randomUUID())
                .payerEmail("alice@test.com")
                .payeeEmail("bob@test.com")
                .payerDocument("52998224725")
                .payeeDocument("39053344705")
                .amount(new BigDecimal("25.00"))
                .failureReason("limite")
                .build();
    }

    @Test
    void onCompletedSavesPayerAndPayeeInbox() {
        consumer.onCompleted(event);

        verify(inbox).saveIfAbsent(eq("evt-9"), eq("alice@test.com"), eq("52998224725"), any());
        verify(inbox).saveIfAbsent(eq("evt-9"), eq("bob@test.com"), eq("39053344705"), any());
    }

    @Test
    void onFailedSavesPayerInbox() {
        consumer.onFailed(event);

        verify(inbox).saveIfAbsent(eq("evt-9-failed"), eq("alice@test.com"), eq("52998224725"), any());
    }

    @Test
    void onCompletedSendsDlqWhenInboxFails() {
        doThrow(new RuntimeException("oracle")).when(inbox).saveIfAbsent(any(), any(), any(), any());

        consumer.onCompleted(event);

        verify(kafkaTemplate).send("transaction.dlq", event.getTransactionId().toString(), event);
    }

    @Test
    void onFailedSendsDlqWhenInboxFails() {
        doThrow(new RuntimeException("oracle")).when(inbox).saveIfAbsent(any(), any(), any(), any());

        consumer.onFailed(event);

        verify(kafkaTemplate).send("transaction.dlq", event.getTransactionId().toString(), event);
    }
}
