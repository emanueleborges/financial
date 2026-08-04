package com.financialhub.domain;

import com.financialhub.domain.enums.TransactionStatus;
import com.financialhub.domain.enums.TransactionType;
import com.financialhub.domain.exception.InvalidTransactionException;
import com.financialhub.domain.model.Transaction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    @Test
    void shouldTransitionPendingToCompleted() {
        Transaction tx = pendingTransfer();
        tx.markProcessing();
        assertEquals(TransactionStatus.PROCESSING, tx.getStatus());
        tx.markCompleted();
        assertEquals(TransactionStatus.COMPLETED, tx.getStatus());
        assertNotNull(tx.getCompletedAt());
    }

    @Test
    void shouldMarkFailed() {
        Transaction tx = pendingTransfer();
        tx.markFailed("timeout");
        assertEquals(TransactionStatus.FAILED, tx.getStatus());
        assertEquals("timeout", tx.getFailureReason());
    }

    @Test
    void shouldReverseCompletedTransfer() {
        Transaction tx = pendingTransfer();
        tx.markProcessing();
        tx.markCompleted();
        assertTrue(tx.isReversible());
        tx.markReversed();
        assertEquals(TransactionStatus.REVERSED, tx.getStatus());
    }

    @Test
    void shouldNotReversePending() {
        Transaction tx = pendingTransfer();
        assertFalse(tx.isReversible());
        assertThrows(InvalidTransactionException.class, tx::markReversed);
    }

    private Transaction pendingTransfer() {
        return Transaction.builder()
                .id(UUID.randomUUID())
                .payerId(UUID.randomUUID())
                .payeeId(UUID.randomUUID())
                .amount(new BigDecimal("100.00"))
                .status(TransactionStatus.PENDING)
                .type(TransactionType.TRANSFER)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
