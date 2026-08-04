package com.financialhub.domain;

import com.financialhub.domain.enums.UserStatus;
import com.financialhub.domain.exception.InactiveAccountException;
import com.financialhub.domain.exception.InsufficientBalanceException;
import com.financialhub.domain.model.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void shouldDebitBalanceSuccessfully() {
        User user = activeUser(new BigDecimal("1000.00"));
        user.debit(new BigDecimal("250.00"));
        assertEquals(new BigDecimal("750.00"), user.getBalance());
    }

    @Test
    void shouldThrowWhenInsufficientBalance() {
        User user = activeUser(new BigDecimal("100.00"));
        assertThrows(InsufficientBalanceException.class,
                () -> user.debit(new BigDecimal("150.00")));
    }

    @Test
    void shouldThrowWhenDebitingInactiveAccount() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .balance(new BigDecimal("1000.00"))
                .status(UserStatus.INACTIVE)
                .dailyLimit(new BigDecimal("5000.00"))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        assertThrows(InactiveAccountException.class,
                () -> user.debit(new BigDecimal("10.00")));
    }

    @Test
    void shouldCreditBalance() {
        User user = activeUser(new BigDecimal("100.00"));
        user.credit(new BigDecimal("50.00"));
        assertEquals(new BigDecimal("150.00"), user.getBalance());
    }

    @Test
    void shouldRespectDailyLimit() {
        User user = activeUser(new BigDecimal("10000.00"));
        assertTrue(user.canSpend(new BigDecimal("1000.00"), new BigDecimal("4000.00")));
        assertFalse(user.canSpend(new BigDecimal("1000.01"), new BigDecimal("4000.00")));
    }

    private User activeUser(BigDecimal balance) {
        return User.builder()
                .id(UUID.randomUUID())
                .name("Test")
                .email("test@test.com")
                .document("12345678909")
                .passwordHash("hash")
                .balance(balance)
                .status(UserStatus.ACTIVE)
                .dailyLimit(new BigDecimal("5000.00"))
                .version(0L)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
