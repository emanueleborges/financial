package com.financialhub.notification;

import com.financialhub.notification.application.NotificationInboxService;
import com.financialhub.notification.domain.NotificationRecord;
import com.financialhub.notification.interfaces.NotificationController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock private NotificationInboxService inbox;
    @InjectMocks private NotificationController controller;

    @Test
    void listReturnsInboxForAuthenticatedDocument() {
        NotificationRecord record = new NotificationRecord(
                "1", "evt", "a@test.com", "52998224725", "ok", Instant.now());
        when(inbox.listByDocument("52998224725")).thenReturn(List.of(record));

        ResponseEntity<NotificationController.InboxResponse> response = controller.list(
                new UsernamePasswordAuthenticationToken("52998224725", null, List.of()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().document()).isEqualTo("52998224725");
        assertThat(response.getBody().entries()).hasSize(1);
    }
}
