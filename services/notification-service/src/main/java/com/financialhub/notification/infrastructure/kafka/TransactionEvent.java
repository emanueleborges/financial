package com.financialhub.notification.infrastructure.kafka;

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
    private String payerDocument;
    private String payeeDocument;
    private String payerEmail;
    private String payeeEmail;
    private String payerName;
    private String payeeName;
    private BigDecimal amount;
    private String status;
    private String type;
    private String failureReason;
    private Instant occurredAt;
}
