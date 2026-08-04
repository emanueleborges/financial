package com.financialhub.infrastructure.pdf;

import com.financialhub.application.port.in.ListUserTransactionsUseCase;
import com.financialhub.application.port.out.StatementPdfGeneratorPort;
import com.financialhub.application.port.out.UserRepositoryPort;
import com.financialhub.domain.enums.TransactionType;
import com.financialhub.domain.model.Transaction;
import com.financialhub.domain.model.User;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OpenPdfStatementGenerator implements StatementPdfGeneratorPort {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter WHEN_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZONE);

    private final UserRepositoryPort userRepository;

    @Override
    public byte[] generate(ListUserTransactionsUseCase.StatementResult statement, String accountName) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
            Font subtitleFont = new Font(Font.HELVETICA, 11, Font.NORMAL);
            Font headerFont = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
            Font cellFont = new Font(Font.HELVETICA, 8, Font.NORMAL);
            Font smallFont = new Font(Font.HELVETICA, 8, Font.ITALIC);

            document.add(new Paragraph("Financial Hub — Extrato", titleFont));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Titular: " + nullToDash(accountName), subtitleFont));
            document.add(new Paragraph("CPF/CNPJ: " + statement.document(), subtitleFont));
            document.add(new Paragraph(
                    "Saldo atual: " + formatMoney(statement.currentBalance()), subtitleFont));
            document.add(new Paragraph(
                    "Gerado em: " + WHEN_FMT.format(java.time.Instant.now()), smallFont));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(new float[]{4.2f, 1.6f, 1.6f, 1.8f});
            table.setWidthPercentage(100);
            addHeader(table, "Movimentação", headerFont);
            addHeader(table, "Valor", headerFont);
            addHeader(table, "Saldo", headerFont);
            addHeader(table, "Quando", headerFont);

            for (ListUserTransactionsUseCase.StatementEntry entry : statement.entries()) {
                Transaction tx = entry.transaction();
                User payer = userRepository.findById(tx.getPayerId()).orElse(null);
                User payee = userRepository.findById(tx.getPayeeId()).orElse(null);

                String movement = movementLabel(entry.viewerUserId(), tx, payer, payee)
                        + "\n" + tx.getType() + " · " + tx.getStatus();

                addCell(table, movement, cellFont, Element.ALIGN_LEFT);
                addCell(table, formatSigned(entry.signedAmount()), cellFont, Element.ALIGN_RIGHT);
                addCell(table,
                        entry.balanceAfter() == null ? "—" : formatMoney(entry.balanceAfter()),
                        cellFont, Element.ALIGN_RIGHT);
                addCell(table, WHEN_FMT.format(tx.getCreatedAt()), cellFont, Element.ALIGN_RIGHT);
            }

            document.add(table);
            document.add(new Paragraph(" "));
            document.add(new Paragraph(
                    "Documento gerado automaticamente. Horários em America/Sao_Paulo.",
                    smallFont));

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao gerar PDF do extrato", e);
        }
    }

    private static String movementLabel(UUID viewerId, Transaction tx, User payer, User payee) {
        boolean outbound = viewerId.equals(tx.getPayerId());
        String payerName = payer != null ? payer.getName() : "—";
        String payeeName = payee != null ? payee.getName() : "—";

        if (tx.getType() == TransactionType.REVERSAL) {
            return outbound ? "Estorno para " + payeeName : "Estorno de " + payerName;
        }
        return outbound ? "Transferência para " + payeeName : "Transferência de " + payerName;
    }

    private static void addHeader(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new Color(20, 90, 85));
        cell.setPadding(6);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(cell);
    }

    private static void addCell(PdfPTable table, String text, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    private static String formatMoney(BigDecimal amount) {
        return NumberFormat.getCurrencyInstance(PT_BR).format(amount);
    }

    private static String formatSigned(BigDecimal amount) {
        if (amount == null) return "—";
        String money = formatMoney(amount.abs());
        if (amount.signum() > 0) return "+ " + money;
        if (amount.signum() < 0) return "− " + money;
        return money;
    }

    private static String nullToDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
