package com.financialhub.application.port.in;

import java.util.UUID;

public interface ExportStatementPdfUseCase {

    StatementPdf execute(ExportCommand command);

    record ExportCommand(String document, String requesterDocument, int limit) {}

    record StatementPdf(byte[] content, String filename) {}
}
