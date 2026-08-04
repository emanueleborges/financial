package com.financialhub.infrastructure.persistence;

import com.financialhub.application.port.out.TransactionRepositoryPort;
import com.financialhub.domain.enums.TransactionType;
import com.financialhub.domain.model.Transaction;
import com.financialhub.infrastructure.persistence.mapper.PersistenceMapper;
import com.financialhub.infrastructure.persistence.repository.TransactionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TransactionRepositoryAdapter implements TransactionRepositoryPort {

    private final TransactionJpaRepository jpaRepository;
    private final PersistenceMapper mapper;

    @Override
    public Transaction save(Transaction transaction) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(transaction)));
    }

    @Override
    public Optional<Transaction> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Transaction> findByIdempotencyKey(String idempotencyKey) {
        return jpaRepository.findByIdempotencyKey(idempotencyKey).map(mapper::toDomain);
    }

    @Override
    public boolean existsReversalFor(UUID originalTxId) {
        return jpaRepository.existsByOriginalTxIdAndType(originalTxId, TransactionType.REVERSAL);
    }

    @Override
    public List<Transaction> findByUserId(UUID userId, int limit) {
        return jpaRepository.findByUserInvolved(userId, limit).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
