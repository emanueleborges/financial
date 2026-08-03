package com.financialhub.infrastructure.kafka.consumer;

import com.financialhub.application.port.out.BalanceCachePort;
import com.financialhub.application.port.out.EventIdempotencyPort;
import com.financialhub.application.port.out.ReceiptStoragePort;
import com.financialhub.application.port.out.TransactionRepositoryPort;
import com.financialhub.application.port.out.UserRepositoryPort;
import com.financialhub.domain.model.Transaction;
import com.financialhub.domain.model.User;
import com.financialhub.infrastructure.kafka.dto.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BalanceUpdateConsumer {

    private static final String CONSUMER_NAME = "BalanceUpdateConsumer";

    private final EventIdempotencyPort idempotencyPort;
    private final BalanceCachePort balanceCache;
    private final ReceiptStoragePort receiptStorage;
    private final TransactionRepositoryPort transactionRepository;
    private final UserRepositoryPort userRepository;
    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    @KafkaListener(
            topics = "${app.kafka.topics.transaction-completed}",
            groupId = "balance-update-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onCompleted(TransactionEvent event) {
        try {
            if (idempotencyPort.alreadyProcessed(event.getEventId(), CONSUMER_NAME)) {
                log.info("Evento já processado (idempotente): {}", event.getEventId());
                return;
            }

            balanceCache.evict(event.getPayerId());
            balanceCache.evict(event.getPayeeId());

            try {
                transactionRepository.findById(event.getTransactionId()).ifPresent(this::storeReceipt);
                log.info("Saldo atualizado e cache invalidado para tx={}", event.getTransactionId());
            } catch (Exception e) {
                log.warn("Falha ao gerar comprovante: {}", e.getMessage());
            }

            idempotencyPort.markProcessed(event.getEventId(), event.getTransactionId(), CONSUMER_NAME);
        } catch (Exception ex) {
            log.error("Erro no BalanceUpdateConsumer: {}", ex.getMessage());
            kafkaTemplate.send("transaction.dlq", event.getTransactionId().toString(), event);
        }
    }

    private void storeReceipt(Transaction tx) {
        User payer = userRepository.findById(tx.getPayerId()).orElse(null);
        User payee = userRepository.findById(tx.getPayeeId()).orElse(null);
        receiptStorage.generateAndStore(new ReceiptStoragePort.ReceiptCommand(
                tx,
                payer != null ? payer.getDocument() : null,
                payer != null ? payer.getName() : null,
                payee != null ? payee.getDocument() : null,
                payee != null ? payee.getName() : null
        ));
    }
}
