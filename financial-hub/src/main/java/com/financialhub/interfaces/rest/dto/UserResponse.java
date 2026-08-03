package com.financialhub.interfaces.rest.dto;

import com.financialhub.domain.enums.UserStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        String document,
        BigDecimal balance,
        UserStatus status,
        BigDecimal dailyLimit,
        Instant createdAt
) {}
