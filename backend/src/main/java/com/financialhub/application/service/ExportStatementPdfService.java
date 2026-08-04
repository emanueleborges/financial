package com.financialhub.application.service;

import com.financialhub.application.port.in.ExportStatementPdfUseCase;
import com.financialhub.application.port.in.ListUserTransactionsUseCase;
import com.financialhub.application.port.out.StatementPdfGeneratorPort;
import com.financialhub.application.port.out.UserRepositoryPort;
import com.financialhub.domain.exception.UserNotFoundException;
import com.financialhub.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExportStatementPdfService implements ExportStatementPdfUseCase {

    private final ListUserTransactionsUseCase listUserTransactionsUseCase;
    private final UserRepositoryPort userRepository;
    private final StatementPdfGeneratorPort statementPdfGenerator;

    @Override
    @Transactional(readOnly = true)
    public StatementPdf execute(ExportCommand command) {
        var statement = listUserTransactionsUseCase.execute(
                new ListUserTransactionsUseCase.ListCommand(
                        command.document(),
                        command.requesterDocument(),
                        command.limit()
                )
        );

        String digits = statement.document();
        User user = userRepository.findByDocument(digits)
                .orElseThrow(() -> new UserNotFoundException("documento " + digits));

        byte[] pdf = statementPdfGenerator.generate(statement, user.getName());
        return new StatementPdf(pdf, "extrato-" + digits + ".pdf");
    }
}
