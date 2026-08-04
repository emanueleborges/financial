package com.financialhub.infrastructure.kafka.consumer;

import com.financialhub.application.port.out.EventIdempotencyPort;
import com.financialhub.infrastructure.kafka.dto.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyReportConsumer {

    private static final String CONSUMER_NAME = "DailyReportConsumer";

    private final EventIdempotencyPort idempotencyPort;
    private final AtomicInteger dailyCount = new AtomicInteger(0);
    private final AtomicReference<BigDecimal> dailyVolume = new AtomicReference<>(BigDecimal.ZERO);
    private final Map<LocalDate, ReportSnapshot> history = new ConcurrentHashMap<>();

    @KafkaListener(
            topics = "${app.kafka.topics.transaction-completed}",
            groupId = "daily-report-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onCompleted(TransactionEvent event) {
        if (idempotencyPort.alreadyProcessed(event.getEventId(), CONSUMER_NAME)) {
            return;
        }

        dailyCount.incrementAndGet();
        dailyVolume.updateAndGet(v -> v.add(event.getAmount()));
        idempotencyPort.markProcessed(event.getEventId(), event.getTransactionId(), CONSUMER_NAME);

        log.debug("Relatório diário atualizado: count={}, volume={}",
                dailyCount.get(), dailyVolume.get());
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void finalizeDailyReport() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        ReportSnapshot snapshot = new ReportSnapshot(dailyCount.get(), dailyVolume.get());
        history.put(yesterday, snapshot);

        log.info("=== RELATÓRIO DIÁRIO {} === Transações: {} | Volume: R$ {}",
                yesterday, snapshot.count(), snapshot.volume());

        dailyCount.set(0);
        dailyVolume.set(BigDecimal.ZERO);
    }

    public ReportSnapshot getTodayReport() {
        return new ReportSnapshot(dailyCount.get(), dailyVolume.get());
    }

    public record ReportSnapshot(int count, BigDecimal volume) {}
}
