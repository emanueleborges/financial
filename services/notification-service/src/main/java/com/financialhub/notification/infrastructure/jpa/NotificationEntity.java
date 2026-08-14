package com.financialhub.notification.infrastructure.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "NOTIFICATIONS")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEntity {

    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @Column(name = "EVENT_ID", nullable = false, length = 64)
    private String eventId;

    @Column(name = "RECIPIENT_EMAIL", nullable = false, length = 255)
    private String recipientEmail;

    @Column(name = "RECIPIENT_DOCUMENT", length = 14)
    private String recipientDocument;

    @Column(name = "MESSAGE", nullable = false, length = 1000)
    private String message;

    @Column(name = "CREATED_AT", nullable = false)
    private Instant createdAt;
}
