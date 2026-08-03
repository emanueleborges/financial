package com.financialhub.application.port.out;

import com.financialhub.domain.model.Transaction;

public interface ReceiptStoragePort {

    /**
     * Gera o PDF do comprovante e tenta persistir no S3.
     * Sempre retorna os bytes gerados (mesmo se o upload falhar).
     */
    byte[] generateAndStore(ReceiptCommand command);

    record ReceiptCommand(
            Transaction transaction,
            String payerDocument,
            String payerName,
            String payeeDocument,
            String payeeName
    ) {}
}
