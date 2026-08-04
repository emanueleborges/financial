package com.financialhub.domain.exception;

public class DailyLimitExceededException extends DomainException {
    public DailyLimitExceededException(String message) {
        super("DAILY_LIMIT_EXCEEDED", message);
    }
}
