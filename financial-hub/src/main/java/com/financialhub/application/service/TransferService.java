package com.financialhub.application.service;

import com.financialhub.application.port.in.TransferUseCase;
import com.financialhub.application.port.out.*;
import com.financialhub.domain.exception.*;
import com.financialhub.domain.model.Transaction;
import com.financialhub.domain.model.TransactionAudit;
import com.financialhub.domain.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService implements TransferUseCase {

    private final UserRepositoryPort userRepository;
    private final TransactionRepositoryPort transactionRepository;
    private final AuditRepositoryPort auditRepository;
    private final TransactionEventPublisherPort eventPublisher;
    private final BalanceCachePort balanceCache;
    private final TransactionStatusService transactionStatusService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(timeout = 30)
    public Transaction execute(TransferCommand command) {
        validateAmount(command);

        String payerDoc = normalize(command.payerDocument());
        String payeeDoc = normalize(command.payeeDocument());
        String requester = normalize(command.requesterDocument());

        if (!payerDoc.equals(requester)) {
            throw new DomainException("FORBIDDEN", "Só é possível transferir como o próprio CPF/CNPJ autenticado");
        }
        if (payerDoc.equals(payeeDoc)) {
            throw new InvalidTransactionException("Pagador e recebedor devem ser diferentes");
        }
        if (!isValidDocumentLength(payerDoc) || !isValidDocumentLength(payeeDoc)) {
            throw new InvalidTransactionException("CPF/CNPJ inválido");
        }

        if (command.idempotencyKey() != null && !command.idempotencyKey().isBlank()) {
            var existing = transactionRepository.findByIdempotencyKey(command.idempotencyKey());
            if (existing.isPresent()) {
                log.info("Transação idempotente retornada: {}", existing.get().getId());
                return existing.get();
            }
        }

        User payer = userRepository.findByDocument(payerDoc)
                .orElseThrow(() -> new UserNotFoundException("documento " + payerDoc));
        User payee = userRepository.findByDocument(payeeDoc)
                .orElseThrow(() -> new UserNotFoundException("documento " + payeeDoc));

        payer.assertActive();
        payee.assertActive();

        BigDecimal dailySpent = userRepository.getDailySpent(payer.getId());
        if (!payer.canSpend(command.amount(), dailySpent)) {
            throw new DailyLimitExceededException(
                    String.format("Limite diário excedido. Limite: %s, já gasto: %s, solicitado: %s",
                            payer.getDailyLimit(), dailySpent, command.amount()));
        }

        Transaction transaction = transactionStatusService.createPending(
                payer.getId(), payee.getId(), command.amount(), command.idempotencyKey());
        eventPublisher.publishCreated(transaction);

        try {
            transaction.markProcessing();
            transaction = transactionRepository.save(transaction);

            userRepository.transferBalance(payer.getId(), payee.getId(), command.amount());

            transaction.markCompleted();
            transaction = transactionRepository.save(transaction);

            balanceCache.evict(payer.getId());
            balanceCache.evict(payee.getId());

            auditRepository.save(TransactionAudit.builder()
                    .id(UUID.randomUUID())
                    .transactionId(transaction.getId())
                    .event("TRANSACTION_COMPLETED")
                    .payload(objectMapper.writeValueAsString(Map.of(
                            "status", "COMPLETED",
                            "completedAt", transaction.getCompletedAt()
                    )))
                    .createdAt(Instant.now())
                    .build());

            eventPublisher.publishCompleted(transaction);
            log.info("Transferência concluída: {} -> {} valor={}",
                    payerDoc, payeeDoc, command.amount());

            return transaction;
        } catch (Exception ex) {
            log.error("Falha na transferência {}: {}", transaction.getId(), ex.getMessage());
            eventPublisher.publishFailed(transaction, ex.getMessage());
            try {
                transactionStatusService.markFailed(transaction.getId(), ex.getMessage());
            } catch (Exception e) {
                log.warn("Não foi possível marcar FAILED: {}", e.getMessage());
            }

            if (ex instanceof DomainException domainEx) {
                throw domainEx;
            }
            throw new DomainException("TRANSFER_FAILED",
                    "Falha ao processar transferência: " + ex.getMessage());
        }
    }

    private void validateAmount(TransferCommand command) {
        if (command.amount() == null || command.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Valor deve ser maior que zero");
        }
    }

    private static String normalize(String document) {
        return document == null ? "" : document.replaceAll("\\D", "");
    }

    private static boolean isValidDocumentLength(String digits) {
        return digits.length() == 11 || digits.length() == 14;
    }
}
