package com.financialhub.interfaces.rest.mapper;

import com.financialhub.application.port.in.ListUserTransactionsUseCase;
import com.financialhub.application.port.out.UserRepositoryPort;
import com.financialhub.domain.model.Transaction;
import com.financialhub.domain.model.User;
import com.financialhub.interfaces.rest.dto.BalanceResponse;
import com.financialhub.interfaces.rest.dto.StatementEntryResponse;
import com.financialhub.interfaces.rest.dto.StatementResponse;
import com.financialhub.interfaces.rest.dto.TransactionResponse;
import com.financialhub.interfaces.rest.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RestMapper {

    private final UserRepositoryPort userRepository;

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getDocument(),
                user.getBalance(),
                user.getStatus(),
                user.getDailyLimit(),
                user.getCreatedAt()
        );
    }

    public TransactionResponse toResponse(Transaction tx) {
        User payer = userRepository.findById(tx.getPayerId()).orElse(null);
        User payee = userRepository.findById(tx.getPayeeId()).orElse(null);

        return new TransactionResponse(
                tx.getId(),
                tx.getPayerId(),
                tx.getPayeeId(),
                payer != null ? payer.getDocument() : null,
                payee != null ? payee.getDocument() : null,
                payer != null ? payer.getName() : null,
                payee != null ? payee.getName() : null,
                tx.getAmount(),
                tx.getStatus(),
                tx.getType(),
                tx.getFailureReason(),
                tx.getOriginalTxId(),
                tx.getCreatedAt(),
                tx.getCompletedAt()
        );
    }

    public StatementResponse toStatementResponse(ListUserTransactionsUseCase.StatementResult result) {
        return new StatementResponse(
                result.document(),
                result.currentBalance(),
                result.entries().stream()
                        .map(entry -> new StatementEntryResponse(
                                toResponse(entry.transaction()),
                                entry.signedAmount(),
                                entry.balanceAfter()
                        ))
                        .toList()
        );
    }

    public BalanceResponse toBalanceResponse(String document, java.math.BigDecimal balance, Long version) {
        return new BalanceResponse(document, balance, version);
    }
}
