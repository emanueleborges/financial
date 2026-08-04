package com.financialhub.infrastructure.persistence.repository;

import com.financialhub.infrastructure.persistence.entity.ProcessedEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedEventJpaRepository extends JpaRepository<ProcessedEventEntity, UUID> {

    boolean existsByEventIdAndConsumerName(String eventId, String consumerName);
}
