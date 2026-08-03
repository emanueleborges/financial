package com.financialhub.application.port.out;

public interface NotificationPort {
    void sendTransferNotification(String email, String message);
}
