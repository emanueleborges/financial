package com.financialhub.notification.domain;

import java.time.Instant;

public record NotificationRecord(
        String id,
        String eventId,
        String email,
        String document,
        String message,
        Instant createdAt
) {}
