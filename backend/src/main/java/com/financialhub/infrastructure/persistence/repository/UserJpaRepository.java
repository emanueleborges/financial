package com.financialhub.infrastructure.persistence.repository;

import com.financialhub.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByDocument(String document);

    boolean existsByEmail(String email);

    boolean existsByDocument(String document);

    @Query(value = """
            SELECT COALESCE(SUM(t.amount), 0)
            FROM transactions t
            WHERE t.payer_id = :userId
              AND t.status IN ('COMPLETED', 'PROCESSING', 'PENDING')
              AND t.type = 'TRANSFER'
              AND DATE(t.created_at AT TIME ZONE 'UTC') = CURRENT_DATE
            """, nativeQuery = true)
    BigDecimal sumDailySpent(@Param("userId") UUID userId);
}
