package com.financialhub.application.service;

import com.financialhub.application.port.in.AuthenticateUseCase;
import com.financialhub.application.port.out.PasswordEncoderPort;
import com.financialhub.application.port.out.TokenProviderPort;
import com.financialhub.application.port.out.UserRepositoryPort;
import com.financialhub.domain.enums.UserStatus;
import com.financialhub.domain.exception.DomainException;
import com.financialhub.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthenticateServiceTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private InMemoryUsers users;
    private AuthenticateService service;

    @BeforeEach
    void setUp() {
        users = new InMemoryUsers();
        users.stored = User.builder()
                .id(USER_ID)
                .name("Alice")
                .email("alice@email.com")
                .document("52998224725")
                .passwordHash("hash")
                .balance(BigDecimal.TEN)
                .status(UserStatus.ACTIVE)
                .dailyLimit(new BigDecimal("5000"))
                .build();
        service = new AuthenticateService(users, new PasswordEncoderPort() {
            @Override
            public String encode(String rawPassword) {
                return "hash";
            }

            @Override
            public boolean matches(String rawPassword, String encodedPassword) {
                return "senha123".equals(rawPassword) && "hash".equals(encodedPassword);
            }
        }, new FakeTokens());
    }

    @Test
    void refreshIssuesNewTokens() {
        AuthenticateUseCase.AuthResult result = service.refresh("refresh-ok");
        assertEquals("access-new", result.accessToken());
        assertEquals("refresh-new", result.refreshToken());
    }

    @Test
    void refreshRejectsAccessToken() {
        DomainException ex = assertThrows(DomainException.class, () -> service.refresh("access-ok"));
        assertEquals("INVALID_CREDENTIALS", ex.getCode());
    }

    private static final class InMemoryUsers implements UserRepositoryPort {
        User stored;

        @Override
        public Optional<User> findByDocument(String document) {
            return stored != null && stored.getDocument().equals(document) ? Optional.of(stored) : Optional.empty();
        }

        @Override
        public Optional<User> findById(UUID id) {
            return Optional.empty();
        }

        @Override
        public Optional<User> findByEmail(String email) {
            return Optional.empty();
        }

        @Override
        public User save(User user) {
            return user;
        }

        @Override
        public boolean existsByEmail(String email) {
            return false;
        }

        @Override
        public boolean existsByDocument(String document) {
            return false;
        }

        @Override
        public java.math.BigDecimal getDailySpent(UUID userId) {
            return java.math.BigDecimal.ZERO;
        }

        @Override
        public void transferBalance(UUID payerId, UUID payeeId, java.math.BigDecimal amount) {
            // no-op
        }
    }

    private static final class FakeTokens implements TokenProviderPort {
        @Override
        public String generateAccessToken(UUID userId, String document, String email) {
            return "access-new";
        }

        @Override
        public String generateRefreshToken(UUID userId, String document, String email) {
            return "refresh-new";
        }

        @Override
        public UUID extractUserId(String token) {
            return USER_ID;
        }

        @Override
        public String extractDocument(String token) {
            return "52998224725";
        }

        @Override
        public String extractEmail(String token) {
            return "alice@email.com";
        }

        @Override
        public boolean isValid(String token) {
            return token != null && token.endsWith("-ok");
        }

        @Override
        public String extractType(String token) {
            return token.startsWith("refresh") ? "refresh" : "access";
        }

        @Override
        public long getAccessTokenExpirationMs() {
            return 3600000;
        }
    }
}
