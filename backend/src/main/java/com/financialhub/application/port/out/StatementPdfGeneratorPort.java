package com.financialhub.application.port.out;

import com.financialhub.application.port.in.ListUserTransactionsUseCase;

public interface StatementPdfGeneratorPort {

    byte[] generate(ListUserTransactionsUseCase.StatementResult statement, String accountName);
}
