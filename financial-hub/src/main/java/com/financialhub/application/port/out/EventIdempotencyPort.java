package com.financialhub.application.port.out;

public interface EventIdempotencyPort {
    boolean alreadyProcessed(String eventId, String consumerName);

    void markProcessed(String eventId, java.util.UUID transactionId, String consumerName);
}
