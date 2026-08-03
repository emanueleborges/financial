package com.financialhub.application.port.in;

import java.util.UUID;

public interface GetReceiptUseCase {

    ReceiptPdf execute(ReceiptCommand command);

    record ReceiptCommand(UUID transactionId, String requesterDocument) {}

    record ReceiptPdf(byte[] content, String filename) {}
}
