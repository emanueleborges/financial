package com.financialhub.notification.application;

import com.financialhub.notification.domain.NotificationRecord;
import com.financialhub.notification.infrastructure.jpa.NotificationEntity;
import com.financialhub.notification.infrastructure.jpa.NotificationJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationInboxService {

    private final NotificationJpaRepository repository;

    public void saveIfAbsent(String eventId, String email, String document, String message) {
        if (email == null || email.isBlank()) {
            return;
        }
        if (repository.existsByEventIdAndRecipientEmail(eventId, email)) {
            return;
        }
        repository.save(NotificationEntity.builder()
                .id(UUID.randomUUID().toString())
                .eventId(eventId)
                .recipientEmail(email)
                .recipientDocument(document)
                .message(message)
                .createdAt(Instant.now())
                .build());
        log.info("[EMAIL MOCK] Para: {} | {}", email, message);
    }

    public List<NotificationRecord> listByDocument(String document) {
        return repository.findByRecipientDocumentOrderByCreatedAtDesc(document, PageRequest.of(0, 50))
                .stream()
                .map(entity -> new NotificationRecord(
                        entity.getId(),
                        entity.getEventId(),
                        entity.getRecipientEmail(),
                        entity.getRecipientDocument(),
                        entity.getMessage(),
                        entity.getCreatedAt()
                ))
                .toList();
    }
}
