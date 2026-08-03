package com.financialhub.infrastructure.persistence;

import com.financialhub.application.port.out.AuditRepositoryPort;
import com.financialhub.domain.model.TransactionAudit;
import com.financialhub.infrastructure.persistence.mapper.PersistenceMapper;
import com.financialhub.infrastructure.persistence.repository.TransactionAuditJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuditRepositoryAdapter implements AuditRepositoryPort {

    private final TransactionAuditJpaRepository jpaRepository;
    private final PersistenceMapper mapper;

    @Override
    public TransactionAudit save(TransactionAudit audit) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(audit)));
    }
}
