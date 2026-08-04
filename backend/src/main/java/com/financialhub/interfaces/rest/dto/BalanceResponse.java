package com.financialhub.interfaces.rest.dto;

import java.math.BigDecimal;

public record BalanceResponse(
        String document,
        BigDecimal balance,
        Long version
) {}
