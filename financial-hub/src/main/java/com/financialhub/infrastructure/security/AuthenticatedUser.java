package com.financialhub.infrastructure.security;

import java.util.UUID;

/**
 * Principal autenticado: documento (CPF/CNPJ) é a chave pública; UUID é interno.
 */
public record AuthenticatedUser(UUID userId, String document, String email) {}
