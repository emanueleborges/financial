package com.financialhub.infrastructure.kafka.dto;

import com.financialhub.domain.enums.TransactionStatus;
import com.financialhub.domain.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEvent {

    private String eventId;
    private String eventType;
    private UUID transactionId;
    private UUID payerId;
    private UUID payeeId;
    private BigDecimal amount;
    private TransactionStatus status;
    private TransactionType type;
    private String failureReason;
    private Instant occurredAt;
}
