package com.financialhub.application.port.out;

import java.util.UUID;

public interface TokenProviderPort {

    String generateAccessToken(UUID userId, String document, String email);

    String generateRefreshToken(UUID userId, String document, String email);

    UUID extractUserId(String token);

    String extractDocument(String token);

    String extractEmail(String token);

    boolean isValid(String token);

    long getAccessTokenExpirationMs();
}
