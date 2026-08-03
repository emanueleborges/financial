package com.financialhub.application.port.out;

import com.financialhub.domain.model.Transaction;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepositoryPort {

    Transaction save(Transaction transaction);

    Optional<Transaction> findById(UUID id);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    boolean existsReversalFor(UUID originalTxId);

    List<Transaction> findByUserId(UUID userId, int limit);
}
