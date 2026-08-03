package com.financialhub.application.service;

import com.financialhub.application.port.in.CreateUserUseCase;
import com.financialhub.application.port.out.DocumentValidationPort;
import com.financialhub.application.port.out.PasswordEncoderPort;
import com.financialhub.application.port.out.UserRepositoryPort;
import com.financialhub.domain.enums.UserStatus;
import com.financialhub.domain.exception.DomainException;
import com.financialhub.domain.exception.DuplicateResourceException;
import com.financialhub.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateUserService implements CreateUserUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final DocumentValidationPort documentValidation;

    @Value("${app.transaction.daily-limit:5000.00}")
    private BigDecimal defaultDailyLimit;

    @Override
    @Transactional
    public User execute(CreateUserCommand command) {
        if (userRepository.existsByEmail(command.email())) {
            throw new DuplicateResourceException("E-mail já cadastrado: " + command.email());
        }
        if (userRepository.existsByDocument(command.document())) {
            throw new DuplicateResourceException("Documento já cadastrado: " + command.document());
        }
        if (!documentValidation.isValidDocument(command.document())) {
            throw new DomainException("INVALID_DOCUMENT", "Documento inválido: " + command.document());
        }

        BigDecimal initialBalance = command.initialBalance() != null
                ? command.initialBalance()
                : BigDecimal.ZERO;

        if (initialBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new DomainException("INVALID_BALANCE", "Saldo inicial não pode ser negativo");
        }

        Instant now = Instant.now();
        User user = User.builder()
                .id(UUID.randomUUID())
                .name(command.name())
                .email(command.email().toLowerCase())
                .document(command.document().replaceAll("\\D", ""))
                .passwordHash(passwordEncoder.encode(command.password()))
                .balance(initialBalance)
                .status(UserStatus.ACTIVE)
                .dailyLimit(defaultDailyLimit)
                .version(0L)
                .createdAt(now)
                .updatedAt(now)
                .build();

        return userRepository.save(user);
    }
}
