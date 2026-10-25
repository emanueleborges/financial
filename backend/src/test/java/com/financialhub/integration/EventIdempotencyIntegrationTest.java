package com.financialhub.integration;

import com.financialhub.application.port.out.EventIdempotencyPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Um evento de transaction.completed é lido por vários consumers (saldo, relatório, notificação):
 * cada um precisa registrar o próprio processamento, sem chave duplicada.
 */
class EventIdempotencyIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EventIdempotencyPort idempotencyPort;

    @Test
    void shouldMarkSameEventForEachConsumer() {
        String eventId = UUID.randomUUID().toString();
        UUID transactionId = UUID.randomUUID();

        idempotencyPort.markProcessed(eventId, transactionId, "BalanceUpdateConsumer");
        assertThatCode(() -> idempotencyPort.markProcessed(eventId, transactionId, "DailyReportConsumer"))
                .doesNotThrowAnyException();

        assertThat(idempotencyPort.alreadyProcessed(eventId, "BalanceUpdateConsumer")).isTrue();
        assertThat(idempotencyPort.alreadyProcessed(eventId, "DailyReportConsumer")).isTrue();
        assertThat(idempotencyPort.alreadyProcessed(eventId, "NotificationConsumer")).isFalse();
    }
}
