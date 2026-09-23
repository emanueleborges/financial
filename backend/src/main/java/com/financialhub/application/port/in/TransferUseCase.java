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
            String password,
            String idempotencyKey
    ) {
        @Override
        public String toString() {
            return "TransferCommand[payerDocument=" + payerDocument
                    + ", payeeDocument=" + payeeDocument
                    + ", requesterDocument=" + requesterDocument
                    + ", amount=" + amount
                    + ", password=***, idempotencyKey=" + idempotencyKey + "]";
        }
    }
}
