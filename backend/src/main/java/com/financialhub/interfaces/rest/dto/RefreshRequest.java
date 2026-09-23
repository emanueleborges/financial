package com.financialhub.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank(message = "refreshToken é obrigatório") String refreshToken
) {}
