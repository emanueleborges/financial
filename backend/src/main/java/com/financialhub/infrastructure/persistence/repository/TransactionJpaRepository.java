package com.financialhub.infrastructure.persistence.repository;

import com.financialhub.domain.enums.TransactionType;
import com.financialhub.infrastructure.persistence.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionJpaRepository extends JpaRepository<TransactionEntity, UUID> {

    Optional<TransactionEntity> findByIdempotencyKey(String idempotencyKey);

    boolean existsByOriginalTxIdAndType(UUID originalTxId, TransactionType type);

    @Query(value = """
            SELECT * FROM transactions t
            WHERE t.payer_id = :userId OR t.payee_id = :userId
            ORDER BY t.created_at DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<TransactionEntity> findByUserInvolved(@Param("userId") UUID userId, @Param("limit") int limit);
}
