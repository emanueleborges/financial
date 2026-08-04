package com.financialhub.domain.exception;

public class TransactionNotFoundException extends DomainException {
    public TransactionNotFoundException(String id) {
        super("TRANSACTION_NOT_FOUND", "Transação não encontrada: " + id);
    }
}
