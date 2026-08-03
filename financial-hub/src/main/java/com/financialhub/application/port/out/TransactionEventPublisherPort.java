package com.financialhub.application.port.out;

import com.financialhub.domain.model.Transaction;

public interface TransactionEventPublisherPort {

    void publishCreated(Transaction transaction);

    void publishCompleted(Transaction transaction);

    void publishFailed(Transaction transaction, String reason);
}
