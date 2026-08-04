package com.financialhub.domain.exception;

public class InsufficientBalanceException extends DomainException {
    public InsufficientBalanceException(String message) {
        super("INSUFFICIENT_BALANCE", message);
    }
}
