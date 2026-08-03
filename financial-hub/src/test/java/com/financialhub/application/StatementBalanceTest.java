package com.financialhub.application;

import com.financialhub.application.service.ListUserTransactionsService;
import com.financialhub.domain.enums.TransactionStatus;
import com.financialhub.domain.enums.TransactionType;
import com.financialhub.domain.model.Transaction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class StatementBalanceTest {

    @Test
    void shouldComputeBalanceAfterWalkingNewestFirst() {
        UUID user = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        // Mais recente: recebeu 100 (COMPLETED) → saldo atual 1000
        // Mais antiga: enviou 200 (COMPLETED)
        Transaction inbound = tx(other, user, "100.00", TransactionStatus.COMPLETED, TransactionType.TRANSFER);
        Transaction outbound = tx(user, other, "200.00", TransactionStatus.COMPLETED, TransactionType.TRANSFER);

        var entries = ListUserTransactionsService.buildEntries(
                user,
                new BigDecimal("1000.00"),
                List.of(inbound, outbound)
        );

        assertEquals(new BigDecimal("1000.00"), entries.get(0).balanceAfter());
        assertEquals(new BigDecimal("100.00"), entries.get(0).signedAmount());

        // Antes do crédito de 100 o saldo era 900; após o débito de 200 o saldo era 900
        assertEquals(new BigDecimal("900.00"), entries.get(1).balanceAfter());
        assertEquals(new BigDecimal("-200.00"), entries.get(1).signedAmount());
    }

    @Test
    void shouldIgnorePendingInBalanceChain() {
        UUID user = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        Transaction pending = tx(user, other, "50.00", TransactionStatus.PENDING, TransactionType.TRANSFER);
        Transaction completed = tx(other, user, "20.00", TransactionStatus.COMPLETED, TransactionType.TRANSFER);

        var entries = ListUserTransactionsService.buildEntries(
                user,
                new BigDecimal("500.00"),
                List.of(pending, completed)
        );

        assertNull(entries.get(0).balanceAfter());
        assertEquals(new BigDecimal("500.00"), entries.get(1).balanceAfter());
    }

    private Transaction tx(
            UUID payer,
            UUID payee,
            String amount,
            TransactionStatus status,
            TransactionType type) {
        Instant now = Instant.now();
        return Transaction.builder()
                .id(UUID.randomUUID())
                .payerId(payer)
                .payeeId(payee)
                .amount(new BigDecimal(amount))
                .status(status)
                .type(type)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
