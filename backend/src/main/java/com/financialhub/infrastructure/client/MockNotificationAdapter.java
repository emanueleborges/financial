package com.financialhub.infrastructure.client;

import com.financialhub.application.port.out.NotificationPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MockNotificationAdapter implements NotificationPort {

    @Override
    public void sendTransferNotification(String email, String message) {
        log.info("[EMAIL MOCK] Para: {} | Mensagem: {}", email, message);
    }
}
