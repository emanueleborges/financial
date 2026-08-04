package com.financialhub.domain.model;

import com.financialhub.domain.enums.UserStatus;
import com.financialhub.domain.exception.InactiveAccountException;
import com.financialhub.domain.exception.InsufficientBalanceException;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class User {

    private final UUID id;
    private String name;
    private String email;
    private String document;
    private String passwordHash;
    private BigDecimal balance;
    private UserStatus status;
    private BigDecimal dailyLimit;
    private Long version;
    private Instant createdAt;
    private Instant updatedAt;

    public void assertActive() {
        if (status != UserStatus.ACTIVE) {
            throw new InactiveAccountException("Conta inativa ou bloqueada: " + id);
        }
    }

    public void debit(BigDecimal amount) {
        assertActive();
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    "Saldo insuficiente. Disponível: " + balance + ", solicitado: " + amount);
        }
        this.balance = this.balance.subtract(amount);
        this.updatedAt = Instant.now();
    }

    public void credit(BigDecimal amount) {
        assertActive();
        this.balance = this.balance.add(amount);
        this.updatedAt = Instant.now();
    }

    public boolean canSpend(BigDecimal amount, BigDecimal alreadySpentToday) {
        return alreadySpentToday.add(amount).compareTo(dailyLimit) <= 0;
    }
}
