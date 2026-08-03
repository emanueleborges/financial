package com.financialhub.application.port.out;

import com.financialhub.domain.model.TransactionAudit;

public interface AuditRepositoryPort {
    TransactionAudit save(TransactionAudit audit);
}
