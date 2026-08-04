package com.financialhub.application.port.in;

import com.financialhub.domain.model.Transaction;

import java.math.BigDecimal;

public interface TransferUseCase {

    Transaction execute(TransferCommand command);

    record TransferCommand(
            String payerDocument,
            String payeeDocument,
            String requesterDocument,
            BigDecimal amount,
            String idempotencyKey
    ) {}
}
