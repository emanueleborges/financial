package com.financialhub.infrastructure.persistence.mapper;

import com.financialhub.domain.model.Transaction;
import com.financialhub.domain.model.TransactionAudit;
import com.financialhub.domain.model.User;
import com.financialhub.infrastructure.persistence.entity.TransactionAuditEntity;
import com.financialhub.infrastructure.persistence.entity.TransactionEntity;
import com.financialhub.infrastructure.persistence.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class PersistenceMapper {

    public User toDomain(UserEntity entity) {
        return User.builder()
                .id(entity.getId())
                .name(entity.getName())
                .email(entity.getEmail())
                .document(entity.getDocument())
                .passwordHash(entity.getPasswordHash())
                .balance(entity.getBalance())
                .status(entity.getStatus())
                .dailyLimit(entity.getDailyLimit())
                .version(entity.getVersion())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public UserEntity toEntity(User user) {
        return UserEntity.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .document(user.getDocument())
                .passwordHash(user.getPasswordHash())
                .balance(user.getBalance())
                .status(user.getStatus())
                .dailyLimit(user.getDailyLimit())
                .version(user.getVersion())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public Transaction toDomain(TransactionEntity entity) {
        return Transaction.builder()
                .id(entity.getId())
                .payerId(entity.getPayerId())
                .payeeId(entity.getPayeeId())
                .amount(entity.getAmount())
                .status(entity.getStatus())
                .type(entity.getType())
                .idempotencyKey(entity.getIdempotencyKey())
                .failureReason(entity.getFailureReason())
                .originalTxId(entity.getOriginalTxId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .completedAt(entity.getCompletedAt())
                .build();
    }

    public TransactionEntity toEntity(Transaction tx) {
        return TransactionEntity.builder()
                .id(tx.getId())
                .payerId(tx.getPayerId())
                .payeeId(tx.getPayeeId())
                .amount(tx.getAmount())
                .status(tx.getStatus())
                .type(tx.getType())
                .idempotencyKey(tx.getIdempotencyKey())
                .failureReason(tx.getFailureReason())
                .originalTxId(tx.getOriginalTxId())
                .createdAt(tx.getCreatedAt())
                .updatedAt(tx.getUpdatedAt())
                .completedAt(tx.getCompletedAt())
                .build();
    }

    public TransactionAudit toDomain(TransactionAuditEntity entity) {
        return TransactionAudit.builder()
                .id(entity.getId())
                .transactionId(entity.getTransactionId())
                .event(entity.getEvent())
                .payload(entity.getPayload())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public TransactionAuditEntity toEntity(TransactionAudit audit) {
        return TransactionAuditEntity.builder()
                .id(audit.getId())
                .transactionId(audit.getTransactionId())
                .event(audit.getEvent())
                .payload(audit.getPayload())
                .createdAt(audit.getCreatedAt())
                .build();
    }
}
