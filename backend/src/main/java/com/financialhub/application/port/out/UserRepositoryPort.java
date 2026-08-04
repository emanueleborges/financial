package com.financialhub.application.port.out;

import com.financialhub.domain.model.User;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface UserRepositoryPort {

    User save(User user);

    Optional<User> findById(UUID id);

    Optional<User> findByEmail(String email);

    Optional<User> findByDocument(String document);

    boolean existsByEmail(String email);

    boolean existsByDocument(String document);

    BigDecimal getDailySpent(UUID userId);

    void transferBalance(UUID payerId, UUID payeeId, BigDecimal amount);
}
