package com.financialhub.interfaces.rest.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record TransferRequest(
        @NotBlank(message = "CPF/CNPJ do pagador é obrigatório")
        String payerDocument,

        @NotBlank(message = "CPF/CNPJ do recebedor é obrigatório")
        String payeeDocument,

        @NotNull(message = "Valor é obrigatório")
        @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
        BigDecimal amount,

        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 6, max = 100, message = "Senha deve ter entre 6 e 100 caracteres")
        String password,

        String idempotencyKey
) {
    @Override
    public String toString() {
        return "TransferRequest[payerDocument=" + payerDocument
                + ", payeeDocument=" + payeeDocument
                + ", amount=" + amount
                + ", password=***, idempotencyKey=" + idempotencyKey + "]";
    }
}
