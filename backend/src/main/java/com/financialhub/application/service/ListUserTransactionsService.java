package com.financialhub.application.service;

import com.financialhub.application.port.in.ListUserTransactionsUseCase;
import com.financialhub.application.port.out.TransactionRepositoryPort;
import com.financialhub.application.port.out.UserRepositoryPort;
import com.financialhub.domain.enums.TransactionStatus;
import com.financialhub.domain.exception.DomainException;
import com.financialhub.domain.exception.UserNotFoundException;
import com.financialhub.domain.model.Transaction;
import com.financialhub.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListUserTransactionsService implements ListUserTransactionsUseCase {

    private final UserRepositoryPort userRepository;
    private final TransactionRepositoryPort transactionRepository;

    @Override
    @Transactional(readOnly = true)
    public StatementResult execute(ListCommand command) {
        String document = normalize(command.document());
        String requester = normalize(command.requesterDocument());

        if (!document.equals(requester)) {
            throw new DomainException("FORBIDDEN", "Só é possível listar as próprias transações");
        }

        User user = userRepository.findByDocument(document)
                .orElseThrow(() -> new UserNotFoundException("documento " + document));

        int limit = Math.min(Math.max(command.limit(), 1), 100);
        List<Transaction> newestFirst = transactionRepository.findByUserId(user.getId(), limit);

        List<StatementEntry> entries = buildEntries(user.getId(), user.getBalance(), newestFirst);
        return new StatementResult(user.getDocument(), user.getBalance(), entries);
    }

    /**
     * Reconstrói o saldo após cada lançamento a partir do saldo atual,
     * percorrendo do mais recente para o mais antigo.
     */
    public static List<StatementEntry> buildEntries(
            UUID userId,
            BigDecimal currentBalance,
            List<Transaction> newestFirst) {

        BigDecimal running = currentBalance;
        List<StatementEntry> entries = new ArrayList<>(newestFirst.size());

        for (Transaction tx : newestFirst) {
            BigDecimal signed = signedAmountFor(userId, tx);
            boolean affectsBalance = affectsLedger(tx);

            BigDecimal balanceAfter = affectsBalance ? running : null;
            if (affectsBalance && signed != null) {
                // Desfaz o efeito para obter o saldo anterior a este lançamento
                running = running.subtract(signed);
            }

            entries.add(new StatementEntry(tx, userId, signed, balanceAfter));
        }

        return entries;
    }

    static BigDecimal signedAmountFor(UUID userId, Transaction tx) {
        if (userId.equals(tx.getPayerId())) {
            return tx.getAmount().negate();
        }
        if (userId.equals(tx.getPayeeId())) {
            return tx.getAmount();
        }
        return BigDecimal.ZERO;
    }

    /**
     * COMPLETED altera o saldo; REVERSED também (foi concluída e depois estornada
     * por outra TX — as duas entram no extrato).
     */
    static boolean affectsLedger(Transaction tx) {
        TransactionStatus status = tx.getStatus();
        return status == TransactionStatus.COMPLETED || status == TransactionStatus.REVERSED;
    }

    private static String normalize(String document) {
        return document == null ? "" : document.replaceAll("\\D", "");
    }
}
