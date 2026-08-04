package com.financialhub.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserAuthIntegrationTest extends AbstractIntegrationTest {

    @Test
    void shouldCreateUserLoginAndConsultBalance() throws Exception {
        createUser("Alice Silva", "alice-" + UUID.randomUUID() + "@test.com",
                PAYER_DOC, new BigDecimal("1500.50"));

        String token = login(PAYER_DOC);

        mockMvc.perform(get("/api/v1/users/{document}", PAYER_DOC)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.document").value(PAYER_DOC))
                .andExpect(jsonPath("$.name").value("Alice Silva"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(get("/api/v1/users/{document}/balance", PAYER_DOC)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.document").value(PAYER_DOC))
                .andExpect(jsonPath("$.balance").value(1500.50));
    }

    @Test
    void shouldRejectDuplicateDocument() throws Exception {
        String email1 = "dup1-" + UUID.randomUUID() + "@test.com";
        String email2 = "dup2-" + UUID.randomUUID() + "@test.com";
        createUser("Alice", email1, PAYER_DOC, new BigDecimal("100.00"));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Outra",
                                "email", email2,
                                "document", PAYER_DOC,
                                "password", PASSWORD,
                                "initialBalance", 50
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", not(emptyOrNullString())));
    }

    @Test
    void shouldRejectInvalidLogin() throws Exception {
        createUser("Alice", "alice-" + UUID.randomUUID() + "@test.com",
                PAYER_DOC, new BigDecimal("100.00"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "document", PAYER_DOC,
                                "password", "senha-errada"
                        ))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectProtectedEndpointWithoutToken() throws Exception {
        createUser("Alice", "alice-" + UUID.randomUUID() + "@test.com",
                PAYER_DOC, new BigDecimal("100.00"));

        mockMvc.perform(get("/api/v1/users/{document}/balance", PAYER_DOC))
                .andExpect(status().isForbidden());
    }
}
