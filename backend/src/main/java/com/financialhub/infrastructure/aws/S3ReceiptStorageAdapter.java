package com.financialhub.infrastructure.aws;

import com.financialhub.application.port.out.ReceiptStoragePort;
import com.financialhub.domain.model.Transaction;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3ReceiptStorageAdapter implements ReceiptStoragePort {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneOffset.UTC);

    private final S3Client s3Client;

    @Value("${app.aws.s3.bucket}")
    private String bucket;

    @Override
    public byte[] generateAndStore(ReceiptCommand command) {
        byte[] pdf = generatePdf(command);
        Transaction tx = command.transaction();
        try {
            String key = receiptKey(tx);
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType("application/pdf")
                            .build(),
                    RequestBody.fromBytes(pdf)
            );
            log.info("Comprovante armazenado no S3: s3://{}/{}", bucket, key);
        } catch (Exception e) {
            log.warn("Falha ao armazenar comprovante no S3 (PDF ainda será retornado): {}", e.getMessage());
        }
        return pdf;
    }

    private static String receiptKey(Transaction transaction) {
        return String.format("receipts/%s/%s.pdf",
                transaction.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate(),
                transaction.getId());
    }

    private byte[] generatePdf(ReceiptCommand cmd) {
        try {
            Transaction tx = cmd.transaction();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document();
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
            Font sectionFont = new Font(Font.HELVETICA, 12, Font.BOLD);
            Font normalFont = new Font(Font.HELVETICA, 11);

            document.add(new Paragraph("Financial Hub", titleFont));
            document.add(new Paragraph("Comprovante de Transação", sectionFont));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("ID da transação: " + tx.getId(), normalFont));
            document.add(new Paragraph("Tipo: " + tx.getType(), normalFont));
            document.add(new Paragraph("Status: " + tx.getStatus(), normalFont));
            document.add(new Paragraph("Valor: " + formatMoney(tx.getAmount()), normalFont));
            document.add(new Paragraph("Data (UTC): " + DATE_FMT.format(tx.getCreatedAt()), normalFont));
            if (tx.getCompletedAt() != null) {
                document.add(new Paragraph("Concluída em (UTC): " + DATE_FMT.format(tx.getCompletedAt()), normalFont));
            }

            document.add(new Paragraph(" "));
            document.add(new Paragraph("Pagador", sectionFont));
            document.add(new Paragraph("Nome: " + nullToDash(cmd.payerName()), normalFont));
            document.add(new Paragraph("CPF/CNPJ: " + nullToDash(cmd.payerDocument()), normalFont));

            document.add(new Paragraph(" "));
            document.add(new Paragraph("Recebedor", sectionFont));
            document.add(new Paragraph("Nome: " + nullToDash(cmd.payeeName()), normalFont));
            document.add(new Paragraph("CPF/CNPJ: " + nullToDash(cmd.payeeDocument()), normalFont));

            if (tx.getOriginalTxId() != null) {
                document.add(new Paragraph(" "));
                document.add(new Paragraph("Transação original: " + tx.getOriginalTxId(), normalFont));
            }
            if (tx.getFailureReason() != null && !tx.getFailureReason().isBlank()) {
                document.add(new Paragraph("Motivo: " + tx.getFailureReason(), normalFont));
            }

            document.add(new Paragraph(" "));
            document.add(new Paragraph(
                    "Documento gerado automaticamente pelo Financial Hub.",
                    new Font(Font.HELVETICA, 9, Font.ITALIC)));

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao gerar PDF do comprovante", e);
        }
    }

    private static String formatMoney(BigDecimal amount) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(PT_BR);
        return nf.format(amount);
    }

    private static String nullToDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
