package com.financialhub.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class TransactionAudit {

    private final UUID id;
    private final UUID transactionId;
    private final String event;
    private final String payload;
    private final Instant createdAt;
}
