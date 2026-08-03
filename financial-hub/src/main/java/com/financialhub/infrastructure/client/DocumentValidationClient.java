package com.financialhub.infrastructure.client;

import com.financialhub.application.port.out.DocumentValidationPort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Mock de validação de CPF/CNPJ com Circuit Breaker (simula serviço externo).
 */
@Slf4j
@Component
public class DocumentValidationClient implements DocumentValidationPort {

    @Override
    @CircuitBreaker(name = "documentValidation", fallbackMethod = "fallbackValidation")
    public boolean isValidDocument(String document) {
        String digits = document.replaceAll("\\D", "");

        // Simula latência de serviço externo
        if (digits.length() == 11) {
            return isValidCpf(digits);
        }
        if (digits.length() == 14) {
            return isValidCnpj(digits);
        }
        return false;
    }

    @SuppressWarnings("unused")
    private boolean fallbackValidation(String document, Throwable ex) {
        log.warn("Circuit breaker aberto para validação de documento: {}", ex.getMessage());
        // Em fallback, aceita documentos com tamanho válido
        String digits = document.replaceAll("\\D", "");
        return digits.length() == 11 || digits.length() == 14;
    }

    private boolean isValidCpf(String cpf) {
        if (cpf.matches("(\\d)\\1{10}")) return false;
        try {
            int sum = 0;
            for (int i = 0; i < 9; i++) sum += Character.getNumericValue(cpf.charAt(i)) * (10 - i);
            int check1 = 11 - (sum % 11);
            if (check1 >= 10) check1 = 0;
            if (check1 != Character.getNumericValue(cpf.charAt(9))) return false;

            sum = 0;
            for (int i = 0; i < 10; i++) sum += Character.getNumericValue(cpf.charAt(i)) * (11 - i);
            int check2 = 11 - (sum % 11);
            if (check2 >= 10) check2 = 0;
            return check2 == Character.getNumericValue(cpf.charAt(10));
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isValidCnpj(String cnpj) {
        if (cnpj.matches("(\\d)\\1{13}")) return false;
        int[] weights1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] weights2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        try {
            int sum = 0;
            for (int i = 0; i < 12; i++) sum += Character.getNumericValue(cnpj.charAt(i)) * weights1[i];
            int check1 = sum % 11 < 2 ? 0 : 11 - (sum % 11);
            if (check1 != Character.getNumericValue(cnpj.charAt(12))) return false;

            sum = 0;
            for (int i = 0; i < 13; i++) sum += Character.getNumericValue(cnpj.charAt(i)) * weights2[i];
            int check2 = sum % 11 < 2 ? 0 : 11 - (sum % 11);
            return check2 == Character.getNumericValue(cnpj.charAt(13));
        } catch (Exception e) {
            return false;
        }
    }
}
