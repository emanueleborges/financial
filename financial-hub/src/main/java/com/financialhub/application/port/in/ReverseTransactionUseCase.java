package com.financialhub.application.port.in;

import com.financialhub.domain.model.Transaction;

import java.util.UUID;

public interface ReverseTransactionUseCase {

    Transaction execute(ReverseCommand command);

    record ReverseCommand(UUID transactionId, String reason, String requesterDocument) {}
}
