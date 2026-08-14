package com.financialhub.notification.infrastructure.jpa;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationJpaRepository extends JpaRepository<NotificationEntity, String> {

    boolean existsByEventIdAndRecipientEmail(String eventId, String recipientEmail);

    List<NotificationEntity> findByRecipientDocumentOrderByCreatedAtDesc(String document, Pageable pageable);
}
