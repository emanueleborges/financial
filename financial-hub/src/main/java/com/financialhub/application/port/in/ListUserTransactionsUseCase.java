package com.financialhub.application.port.in;

import com.financialhub.domain.model.Transaction;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ListUserTransactionsUseCase {

    StatementResult execute(ListCommand command);

    record ListCommand(String document, String requesterDocument, int limit) {}

    record StatementResult(
            String document,
            BigDecimal currentBalance,
            List<StatementEntry> entries
    ) {}

    record StatementEntry(
            Transaction transaction,
            UUID viewerUserId,
            BigDecimal signedAmount,
            BigDecimal balanceAfter
    ) {}
}
