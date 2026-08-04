package com.financialhub.domain.exception;

public class InvalidTransactionException extends DomainException {
    public InvalidTransactionException(String message) {
        super("INVALID_TRANSACTION", message);
    }
}
