package com.financialhub.infrastructure.persistence.repository;

import com.financialhub.infrastructure.persistence.entity.TransactionAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TransactionAuditJpaRepository extends JpaRepository<TransactionAuditEntity, UUID> {
}
