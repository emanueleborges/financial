package com.financialhub.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TransferFlowIntegrationTest extends AbstractIntegrationTest {

    @Test
    void shouldTransferUpdateBalancesReverseAndExportStatement() throws Exception {
        seedPayerAndPayee();
        String payerToken = login(PAYER_DOC);

        MvcResult transferResult = mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + payerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "payerDocument", PAYER_DOC,
                                "payeeDocument", PAYEE_DOC,
                                "amount", new BigDecimal("250.00"),
                                "idempotencyKey", "it-transfer-" + UUID.randomUUID()
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.type").value("TRANSFER"))
                .andExpect(jsonPath("$.amount").value(250.00))
                .andReturn();

        JsonNode tx = objectMapper.readTree(transferResult.getResponse().getContentAsString());
        UUID txId = UUID.fromString(tx.get("id").asText());

        mockMvc.perform(get("/api/v1/users/{document}/balance", PAYER_DOC)
                        .header("Authorization", "Bearer " + payerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1750.00));

        String payeeToken = login(PAYEE_DOC);
        mockMvc.perform(get("/api/v1/users/{document}/balance", PAYEE_DOC)
                        .header("Authorization", "Bearer " + payeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(750.00));

        mockMvc.perform(get("/api/v1/transactions/{id}", txId)
                        .header("Authorization", "Bearer " + payerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(txId.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(get("/api/v1/users/{document}/transactions", PAYER_DOC)
                        .header("Authorization", "Bearer " + payerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.document").value(PAYER_DOC))
                .andExpect(jsonPath("$.entries", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.entries[0].signedAmount").value(-250.00));

        mockMvc.perform(get("/api/v1/users/{document}/transactions/export", PAYER_DOC)
                        .header("Authorization", "Bearer " + payerToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(result -> assertThat(result.getResponse().getContentAsByteArray())
                        .isNotEmpty());

        mockMvc.perform(get("/api/v1/transactions/{id}/receipt", txId)
                        .header("Authorization", "Bearer " + payerToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(result -> assertThat(result.getResponse().getContentAsByteArray())
                        .isNotEmpty());

        mockMvc.perform(post("/api/v1/transactions/reverse")
                        .header("Authorization", "Bearer " + payerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "transactionId", txId,
                                "reason", "estorno IT"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.type").value("REVERSAL"))
                .andExpect(jsonPath("$.originalTxId").value(txId.toString()));

        mockMvc.perform(get("/api/v1/users/{document}/balance", PAYER_DOC)
                        .header("Authorization", "Bearer " + payerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(2000.00));

        mockMvc.perform(get("/api/v1/transactions/{id}", txId)
                        .header("Authorization", "Bearer " + payerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVERSED"));
    }

    @Test
    void shouldHonorIdempotencyKey() throws Exception {
        seedPayerAndPayee();
        String token = login(PAYER_DOC);
        String key = "idem-" + UUID.randomUUID();

        MvcResult first = mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "payerDocument", PAYER_DOC,
                                "payeeDocument", PAYEE_DOC,
                                "amount", new BigDecimal("10.00"),
                                "idempotencyKey", key
                        ))))
                .andExpect(status().isCreated())
                .andReturn();

        MvcResult second = mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "payerDocument", PAYER_DOC,
                                "payeeDocument", PAYEE_DOC,
                                "amount", new BigDecimal("10.00"),
                                "idempotencyKey", key
                        ))))
                .andExpect(status().isCreated())
                .andReturn();

        String id1 = objectMapper.readTree(first.getResponse().getContentAsString()).get("id").asText();
        String id2 = objectMapper.readTree(second.getResponse().getContentAsString()).get("id").asText();
        assertThat(id1).isEqualTo(id2);

        mockMvc.perform(get("/api/v1/users/{document}/balance", PAYER_DOC)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1990.00));
    }
}
