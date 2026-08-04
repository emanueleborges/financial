package com.financialhub.application.port.in;

public interface GetBalanceUseCase {
    BalanceResult execute(String document);

    record BalanceResult(String document, java.math.BigDecimal balance, Long version) {}
}
