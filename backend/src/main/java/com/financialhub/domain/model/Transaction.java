package com.financialhub.domain.model;

import com.financialhub.domain.enums.TransactionStatus;
import com.financialhub.domain.enums.TransactionType;
import com.financialhub.domain.exception.InvalidTransactionException;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class Transaction {

    private final UUID id;
    private final UUID payerId;
    private final UUID payeeId;
    private final BigDecimal amount;
    private TransactionStatus status;
    private final TransactionType type;
    private final String idempotencyKey;
    private String failureReason;
    private final UUID originalTxId;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;

    public void markProcessing() {
        if (status != TransactionStatus.PENDING) {
            throw new InvalidTransactionException("Só é possível processar transações PENDING");
        }
        this.status = TransactionStatus.PROCESSING;
        this.updatedAt = Instant.now();
    }

    public void markCompleted() {
        if (status != TransactionStatus.PROCESSING && status != TransactionStatus.PENDING) {
            throw new InvalidTransactionException("Transação não pode ser completada no status: " + status);
        }
        this.status = TransactionStatus.COMPLETED;
        this.updatedAt = Instant.now();
        this.completedAt = Instant.now();
    }

    public void markFailed(String reason) {
        this.status = TransactionStatus.FAILED;
        this.failureReason = reason;
        this.updatedAt = Instant.now();
    }

    public void markReversed() {
        if (status != TransactionStatus.COMPLETED) {
            throw new InvalidTransactionException("Só é possível estornar transações COMPLETED");
        }
        this.status = TransactionStatus.REVERSED;
        this.updatedAt = Instant.now();
    }

    public boolean isReversible() {
        return status == TransactionStatus.COMPLETED && type == TransactionType.TRANSFER;
    }
}
