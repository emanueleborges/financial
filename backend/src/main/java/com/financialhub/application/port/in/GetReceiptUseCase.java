package com.financialhub.application.port.in;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public interface GetReceiptUseCase {

    ReceiptPdf execute(ReceiptCommand command);

    record ReceiptCommand(UUID transactionId, String requesterDocument) {}

    record ReceiptPdf(byte[] content, String filename) {
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ReceiptPdf that)) {
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
            return "ReceiptPdf[content=" + Arrays.toString(content) + ", filename=" + filename + "]";
        }
    }
}
