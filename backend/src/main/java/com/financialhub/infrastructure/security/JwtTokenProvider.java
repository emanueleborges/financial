package com.financialhub.infrastructure.security;

import com.financialhub.application.port.out.TokenProviderPort;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider implements TokenProviderPort {

    private final SecretKey key;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long accessExpirationMs,
            @Value("${app.jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    @Override
    public String generateAccessToken(UUID userId, String document, String email) {
        return buildToken(userId, document, email, accessExpirationMs, "access");
    }

    @Override
    public String generateRefreshToken(UUID userId, String document, String email) {
        return buildToken(userId, document, email, refreshExpirationMs, "refresh");
    }

    private String buildToken(UUID userId, String document, String email, long expiration, String type) {
        Instant now = Instant.now();
        Date issuedAt = Date.from(now);
        Date expiresAt = Date.from(now.plusMillis(expiration));
        return Jwts.builder()
                .subject(document)
                .claim("userId", userId.toString())
                .claim("email", email)
                .claim("type", type)
                .issuedAt(issuedAt)
                .expiration(expiresAt)
                .signWith(key)
                .compact();
    }

    @Override
    public UUID extractUserId(String token) {
        return UUID.fromString(parseClaims(token).get("userId", String.class));
    }

    @Override
    public String extractDocument(String token) {
        return parseClaims(token).getSubject();
    }

    @Override
    public String extractEmail(String token) {
        return parseClaims(token).get("email", String.class);
    }

    @Override
    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public long getAccessTokenExpirationMs() {
        return accessExpirationMs;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
