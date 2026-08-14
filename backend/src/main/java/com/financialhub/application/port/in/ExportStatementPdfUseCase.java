package com.financialhub.application.port.in;

import java.util.Arrays;
import java.util.Objects;

public interface ExportStatementPdfUseCase {

    StatementPdf execute(ExportCommand command);

    record ExportCommand(String document, String requesterDocument, int limit) {}

    record StatementPdf(byte[] content, String filename) {
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof StatementPdf that)) {
                return false;
            }
            return Arrays.equals(content, that.content) && Objects.equals(filename, that.filename);
        }

        @Override
        public int hashCode() {
            return 31 * Objects.hash(filename) + Arrays.hashCode(content);
        }

        @Override
        public String toString() {
            return "StatementPdf[content=" + Arrays.toString(content) + ", filename=" + filename + "]";
        }
    }
}
