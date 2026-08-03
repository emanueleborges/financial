package com.financialhub.domain.exception;

public class InactiveAccountException extends DomainException {
    public InactiveAccountException(String message) {
        super("INACTIVE_ACCOUNT", message);
    }
}
