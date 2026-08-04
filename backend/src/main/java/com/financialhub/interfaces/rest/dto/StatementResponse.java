package com.financialhub.interfaces.rest.dto;

import java.math.BigDecimal;
import java.util.List;

public record StatementResponse(
        String document,
        BigDecimal currentBalance,
        List<StatementEntryResponse> entries
) {}
