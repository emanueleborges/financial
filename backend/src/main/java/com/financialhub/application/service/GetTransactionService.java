package com.financialhub.application.service;

import com.financialhub.application.port.in.GetTransactionUseCase;
import com.financialhub.application.port.out.TransactionRepositoryPort;
import com.financialhub.domain.exception.TransactionNotFoundException;
import com.financialhub.domain.model.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetTransactionService implements GetTransactionUseCase {

    private final TransactionRepositoryPort transactionRepository;

    @Override
    @Transactional(readOnly = true)
    public Transaction execute(UUID id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id.toString()));
    }
}
