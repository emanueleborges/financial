package com.financialhub.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "CPF/CNPJ é obrigatório") String document,
        @NotBlank(message = "Senha é obrigatória") String password
) {}
