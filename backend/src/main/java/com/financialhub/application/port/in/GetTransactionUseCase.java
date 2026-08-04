package com.financialhub.application.port.in;

import com.financialhub.domain.model.Transaction;

import java.util.UUID;

public interface GetTransactionUseCase {
    Transaction execute(UUID id);
}
