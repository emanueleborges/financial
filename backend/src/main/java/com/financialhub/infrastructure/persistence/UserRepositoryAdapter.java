package com.financialhub.infrastructure.persistence;

import com.financialhub.application.port.out.UserRepositoryPort;
import com.financialhub.domain.exception.DomainException;
import com.financialhub.domain.exception.InactiveAccountException;
import com.financialhub.domain.exception.InsufficientBalanceException;
import com.financialhub.domain.exception.UserNotFoundException;
import com.financialhub.domain.model.User;
import com.financialhub.infrastructure.persistence.mapper.PersistenceMapper;
import com.financialhub.infrastructure.persistence.repository.UserJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final UserJpaRepository jpaRepository;
    private final PersistenceMapper mapper;
    private final EntityManager entityManager;

    @Override
    public User save(User user) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(user)));
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByDocument(String document) {
        return jpaRepository.findByDocument(document).map(mapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByDocument(String document) {
        return jpaRepository.existsByDocument(document);
    }

    @Override
    public BigDecimal getDailySpent(UUID userId) {
        BigDecimal spent = jpaRepository.sumDailySpent(userId);
        return spent != null ? spent : BigDecimal.ZERO;
    }

    @Override
    public void transferBalance(UUID payerId, UUID payeeId, BigDecimal amount) {
        try {
            entityManager.createNativeQuery("SELECT transfer_balance(?1, ?2, ?3)")
                    .setParameter(1, payerId)
                    .setParameter(2, payeeId)
                    .setParameter(3, amount)
                    .getSingleResult();
        } catch (PersistenceException ex) {
            rethrowTransferFailure(rootMessage(ex));
        }
    }

    private static String rootMessage(PersistenceException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "";
        Throwable cause = ex.getCause();
        while (cause != null) {
            if (cause.getMessage() != null) {
                msg = cause.getMessage();
            }
            cause = cause.getCause();
        }
        return msg;
    }

    private static void rethrowTransferFailure(String msg) {
        if (msg.contains("INSUFFICIENT_BALANCE")) {
            throw new InsufficientBalanceException("Saldo insuficiente para a transferência");
        }
        if (msg.contains("PAYER_INACTIVE") || msg.contains("PAYEE_INACTIVE")) {
            throw new InactiveAccountException("Conta inativa ou bloqueada");
        }
        if (msg.contains("PAYER_NOT_FOUND") || msg.contains("PAYEE_NOT_FOUND")) {
            throw new UserNotFoundException("usuário da transferência");
        }
        throw new DomainException("TRANSFER_FAILED", "Falha na transferência de saldo: " + msg);
    }
}
