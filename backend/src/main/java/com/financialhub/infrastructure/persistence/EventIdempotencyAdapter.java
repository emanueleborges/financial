package com.financialhub.infrastructure.persistence;

import com.financialhub.application.port.out.EventIdempotencyPort;
import com.financialhub.infrastructure.persistence.entity.ProcessedEventEntity;
import com.financialhub.infrastructure.persistence.repository.ProcessedEventJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EventIdempotencyAdapter implements EventIdempotencyPort {

    private final ProcessedEventJpaRepository repository;

    @Override
    public boolean alreadyProcessed(String eventId, String consumerName) {
        return repository.existsByEventIdAndConsumerName(eventId, consumerName);
    }

    @Override
    public void markProcessed(String eventId, UUID transactionId, String consumerName) {
        repository.save(ProcessedEventEntity.builder()
                .id(UUID.randomUUID())
                .eventId(eventId)
                .transactionId(transactionId)
                .consumerName(consumerName)
                .processedAt(Instant.now())
                .build());
    }
}
