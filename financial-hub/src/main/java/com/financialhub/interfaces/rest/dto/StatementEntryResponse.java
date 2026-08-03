package com.financialhub.interfaces.rest.dto;

import java.math.BigDecimal;

public record StatementEntryResponse(
        TransactionResponse transaction,
        BigDecimal signedAmount,
        BigDecimal balanceAfter
) {}
