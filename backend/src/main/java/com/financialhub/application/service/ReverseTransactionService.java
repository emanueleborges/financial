package com.financialhub.application.service;

import com.financialhub.application.port.in.ReverseTransactionUseCase;
import com.financialhub.application.port.out.*;
import com.financialhub.domain.enums.TransactionStatus;
import com.financialhub.domain.enums.TransactionType;
import com.financialhub.domain.exception.DomainException;
import com.financialhub.domain.exception.InvalidTransactionException;
import com.financialhub.domain.exception.TransactionNotFoundException;
import com.financialhub.domain.model.Transaction;
import com.financialhub.domain.model.TransactionAudit;
import com.financialhub.domain.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReverseTransactionService implements ReverseTransactionUseCase {

    private final TransactionRepositoryPort transactionRepository;
    private final UserRepositoryPort userRepository;
    private final AuditRepositoryPort auditRepository;
    private final TransactionEventPublisherPort eventPublisher;
    private final BalanceCachePort balanceCache;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(timeout = 30)
    public Transaction execute(ReverseCommand command) {
        Transaction original = transactionRepository.findById(command.transactionId())
                .orElseThrow(() -> new TransactionNotFoundException(command.transactionId().toString()));

        String requester = command.requesterDocument() == null
                ? ""
                : command.requesterDocument().replaceAll("\\D", "");

        User originalPayer = userRepository.findById(original.getPayerId())
                .orElseThrow(() -> new DomainException("USER_NOT_FOUND", "Pagador original não encontrado"));

        if (!originalPayer.getDocument().equals(requester)) {
            throw new DomainException("FORBIDDEN", "Só o pagador original pode estornar a transferência");
        }

        if (!original.isReversible()) {
            throw new InvalidTransactionException(
                    "Transação não pode ser estornada. Status: " + original.getStatus());
        }

        if (transactionRepository.existsReversalFor(original.getId())) {
            throw new InvalidTransactionException("Já existe estorno para esta transação");
        }

        Instant now = Instant.now();
        Transaction reversal = Transaction.builder()
                .id(UUID.randomUUID())
                .payerId(original.getPayeeId())
                .payeeId(original.getPayerId())
                .amount(original.getAmount())
                .status(TransactionStatus.PENDING)
                .type(TransactionType.REVERSAL)
                .originalTxId(original.getId())
                .idempotencyKey("REV-" + original.getId())
                .createdAt(now)
                .updatedAt(now)
                .build();

        reversal = transactionRepository.save(reversal);

        try {
            reversal.markProcessing();
            userRepository.transferBalance(
                    original.getPayeeId(),
                    original.getPayerId(),
                    original.getAmount()
            );

            reversal.markCompleted();
            reversal = transactionRepository.save(reversal);

            original.markReversed();
            transactionRepository.save(original);

            balanceCache.evict(original.getPayerId());
            balanceCache.evict(original.getPayeeId());

            auditRepository.save(TransactionAudit.builder()
                    .id(UUID.randomUUID())
                    .transactionId(reversal.getId())
                    .event("TRANSACTION_REVERSED")
                    .payload(objectMapper.writeValueAsString(Map.of(
                            "originalTxId", original.getId(),
                            "reason", command.reason() != null ? command.reason() : "Estorno solicitado"
                    )))
                    .createdAt(Instant.now())
                    .build());

            eventPublisher.publishCompleted(reversal);
            log.info("Estorno concluído: original={}, reversal={}", original.getId(), reversal.getId());
            return reversal;
        } catch (Exception ex) {
            reversal.markFailed(ex.getMessage());
            transactionRepository.save(reversal);
            eventPublisher.publishFailed(reversal, ex.getMessage());
            throw new InvalidTransactionException("Falha no estorno: " + ex.getMessage());
        }
    }
}
