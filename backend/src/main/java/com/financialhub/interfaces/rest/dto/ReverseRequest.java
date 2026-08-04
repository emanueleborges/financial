package com.financialhub.interfaces.rest.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReverseRequest(
        @NotNull UUID transactionId,
        String reason
) {}
