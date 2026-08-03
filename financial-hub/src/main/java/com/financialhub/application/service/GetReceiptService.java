package com.financialhub.application.service;

import com.financialhub.application.port.in.GetReceiptUseCase;
import com.financialhub.application.port.out.ReceiptStoragePort;
import com.financialhub.application.port.out.TransactionRepositoryPort;
import com.financialhub.application.port.out.UserRepositoryPort;
import com.financialhub.domain.exception.DomainException;
import com.financialhub.domain.exception.TransactionNotFoundException;
import com.financialhub.domain.model.Transaction;
import com.financialhub.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetReceiptService implements GetReceiptUseCase {

    private final TransactionRepositoryPort transactionRepository;
    private final UserRepositoryPort userRepository;
    private final ReceiptStoragePort receiptStorage;

    @Override
    @Transactional(readOnly = true)
    public ReceiptPdf execute(ReceiptCommand command) {
        Transaction tx = transactionRepository.findById(command.transactionId())
                .orElseThrow(() -> new TransactionNotFoundException(command.transactionId().toString()));

        User payer = userRepository.findById(tx.getPayerId()).orElse(null);
        User payee = userRepository.findById(tx.getPayeeId()).orElse(null);

        String requester = normalize(command.requesterDocument());
        String payerDoc = payer != null ? normalize(payer.getDocument()) : "";
        String payeeDoc = payee != null ? normalize(payee.getDocument()) : "";

        if (!requester.equals(payerDoc) && !requester.equals(payeeDoc)) {
            throw new DomainException("FORBIDDEN", "Só participantes da transação podem baixar o comprovante");
        }

        byte[] pdf = receiptStorage.generateAndStore(new ReceiptStoragePort.ReceiptCommand(
                tx,
                payer != null ? payer.getDocument() : null,
                payer != null ? payer.getName() : null,
                payee != null ? payee.getDocument() : null,
                payee != null ? payee.getName() : null
        ));

        return new ReceiptPdf(pdf, "comprovante-" + tx.getId() + ".pdf");
    }

    private static String normalize(String document) {
        return document == null ? "" : document.replaceAll("\\D", "");
    }
}
