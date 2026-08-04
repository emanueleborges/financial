package com.financialhub.infrastructure.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DocumentValidationClientTest {

    private final DocumentValidationClient client = new DocumentValidationClient();

    @Test
    void shouldValidateValidCpf() {
        assertTrue(client.isValidDocument("529.982.247-25"));
    }

    @Test
    void shouldRejectInvalidCpf() {
        assertFalse(client.isValidDocument("111.111.111-11"));
        assertFalse(client.isValidDocument("123.456.789-00"));
    }

    @Test
    void shouldRejectWrongLength() {
        assertFalse(client.isValidDocument("12345"));
    }
}
