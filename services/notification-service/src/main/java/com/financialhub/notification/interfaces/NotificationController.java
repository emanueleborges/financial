package com.financialhub.notification.interfaces;

import com.financialhub.notification.application.NotificationInboxService;
import com.financialhub.notification.domain.NotificationRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationInboxService inbox;

    @GetMapping
    public ResponseEntity<InboxResponse> list(Authentication authentication) {
        String document = authentication.getName();
        List<NotificationRecord> entries = inbox.listByDocument(document);
        return ResponseEntity.ok(new InboxResponse(document, entries));
    }

    public record InboxResponse(String document, List<NotificationRecord> entries) {}
}
