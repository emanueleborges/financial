package com.financialhub.application.service;

import com.financialhub.application.port.out.AuditRepositoryPort;
import com.financialhub.application.port.out.TransactionRepositoryPort;
import com.financialhub.domain.enums.TransactionStatus;
import com.financialhub.domain.enums.TransactionType;
import com.financialhub.domain.model.Transaction;
import com.financialhub.domain.model.TransactionAudit;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionStatusService {

    private final TransactionRepositoryPort transactionRepository;
    private final AuditRepositoryPort auditRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Transaction createPending(UUID payerId, UUID payeeId, BigDecimal amount, String idempotencyKey) {
        Instant now = Instant.now();
        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .payerId(payerId)
                .payeeId(payeeId)
                .amount(amount)
                .status(TransactionStatus.PENDING)
                .type(TransactionType.TRANSFER)
                .idempotencyKey(idempotencyKey)
                .createdAt(now)
                .updatedAt(now)
                .build();

        transaction = transactionRepository.save(transaction);
        saveAudit(transaction, "TRANSACTION_CREATED", Map.of(
                "payerId", payerId,
                "payeeId", payeeId,
                "amount", amount
        ));
        return transaction;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Transaction markFailed(UUID transactionId, String reason) {
        Transaction tx = transactionRepository.findById(transactionId).orElseThrow();
        tx.markFailed(reason);
        tx = transactionRepository.save(tx);
        saveAudit(tx, "TRANSACTION_FAILED", Map.of("reason", reason != null ? reason : "unknown"));
        return tx;
    }

    private void saveAudit(Transaction tx, String event, Map<String, Object> payload) {
        try {
            auditRepository.save(TransactionAudit.builder()
                    .id(UUID.randomUUID())
                    .transactionId(tx.getId())
                    .event(event)
                    .payload(objectMapper.writeValueAsString(payload))
                    .createdAt(Instant.now())
                    .build());
        } catch (Exception e) {
            log.warn("Falha ao auditar: {}", e.getMessage());
        }
    }
}
