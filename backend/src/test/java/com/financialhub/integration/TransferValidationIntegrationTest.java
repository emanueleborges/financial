package com.financialhub.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransferValidationIntegrationTest extends AbstractIntegrationTest {

    @Test
    void shouldRejectInsufficientBalance() throws Exception {
        seedPayerAndPayee();
        String token = login(PAYER_DOC);

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "payerDocument", PAYER_DOC,
                                "payeeDocument", PAYEE_DOC,
                                "amount", new BigDecimal("99999.00"),
                                "password", PASSWORD
                        ))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code", not(emptyOrNullString())));
    }

    @Test
    void shouldRejectTransferAsAnotherUser() throws Exception {
        seedPayerAndPayee();
        String payeeToken = login(PAYEE_DOC);

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + payeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "payerDocument", PAYER_DOC,
                                "payeeDocument", PAYEE_DOC,
                                "amount", new BigDecimal("10.00"),
                                "password", PASSWORD
                        ))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void shouldRejectSelfTransfer() throws Exception {
        seedPayerAndPayee();
        String token = login(PAYER_DOC);

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "payerDocument", PAYER_DOC,
                                "payeeDocument", PAYER_DOC,
                                "amount", new BigDecimal("10.00"),
                                "password", PASSWORD
                        ))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void shouldRejectReverseByNonPayer() throws Exception {
        seedPayerAndPayee();
        String payerToken = login(PAYER_DOC);

        String txId = objectMapper.readTree(
                mockMvc.perform(post("/api/v1/transactions")
                                .header("Authorization", "Bearer " + payerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of(
                                        "payerDocument", PAYER_DOC,
                                        "payeeDocument", PAYEE_DOC,
                                        "amount", new BigDecimal("20.00"),
                                        "password", PASSWORD,
                                        "idempotencyKey", "rev-" + UUID.randomUUID()
                                ))))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString()
        ).get("id").asText();

        String payeeToken = login(PAYEE_DOC);
        mockMvc.perform(post("/api/v1/transactions/reverse")
                        .header("Authorization", "Bearer " + payeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "transactionId", txId,
                                "reason", "não autorizado"
                        ))))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectTransferWithoutPassword() throws Exception {
        seedPayerAndPayee();
        String token = login(PAYER_DOC);

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "payerDocument", PAYER_DOC,
                                "payeeDocument", PAYEE_DOC,
                                "amount", new BigDecimal("10.00")
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.password").exists());
    }

    @Test
    void shouldRejectTransferWithWrongPassword() throws Exception {
        seedPayerAndPayee();
        String token = login(PAYER_DOC);

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "payerDocument", PAYER_DOC,
                                "payeeDocument", PAYEE_DOC,
                                "amount", new BigDecimal("10.00"),
                                "password", "errada1"
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

        mockMvc.perform(get("/api/v1/users/{document}/balance", PAYER_DOC)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(2000.00));
    }
}
