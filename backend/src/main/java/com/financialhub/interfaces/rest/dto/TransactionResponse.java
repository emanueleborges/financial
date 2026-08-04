package com.financialhub.interfaces.rest.dto;

import com.financialhub.domain.enums.TransactionStatus;
import com.financialhub.domain.enums.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        UUID payerId,
        UUID payeeId,
        String payerDocument,
        String payeeDocument,
        String payerName,
        String payeeName,
        BigDecimal amount,
        TransactionStatus status,
        TransactionType type,
        String failureReason,
        UUID originalTxId,
        Instant createdAt,
        Instant completedAt
) {}
