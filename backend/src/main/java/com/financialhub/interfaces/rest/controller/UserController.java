package com.financialhub.interfaces.rest.controller;

import com.financialhub.application.port.in.CreateUserUseCase;
import com.financialhub.application.port.in.ExportStatementPdfUseCase;
import com.financialhub.application.port.in.GetBalanceUseCase;
import com.financialhub.application.port.in.GetUserUseCase;
import com.financialhub.application.port.in.ListUserTransactionsUseCase;
import com.financialhub.infrastructure.security.AuthenticatedUser;
import com.financialhub.interfaces.rest.dto.BalanceResponse;
import com.financialhub.interfaces.rest.dto.CreateUserRequest;
import com.financialhub.interfaces.rest.dto.StatementResponse;
import com.financialhub.interfaces.rest.dto.UserResponse;
import com.financialhub.interfaces.rest.mapper.RestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Gestão de usuários (chave = CPF/CNPJ)")
public class UserController {

    private final CreateUserUseCase createUserUseCase;
    private final GetUserUseCase getUserUseCase;
    private final GetBalanceUseCase getBalanceUseCase;
    private final ListUserTransactionsUseCase listUserTransactionsUseCase;
    private final ExportStatementPdfUseCase exportStatementPdfUseCase;
    private final RestMapper mapper;

    @PostMapping
    @Operation(summary = "Criar usuário com saldo inicial")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        var user = createUserUseCase.execute(new CreateUserUseCase.CreateUserCommand(
                request.name(),
                request.email(),
                request.document(),
                request.password(),
                request.initialBalance()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(user));
    }

    @GetMapping("/{document}")
    @Operation(summary = "Buscar usuário por CPF/CNPJ")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<UserResponse> getByDocument(@PathVariable String document) {
        return ResponseEntity.ok(mapper.toResponse(getUserUseCase.execute(document)));
    }

    @GetMapping("/{document}/balance")
    @Operation(summary = "Consultar saldo por CPF/CNPJ")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable String document) {
        var result = getBalanceUseCase.execute(document);
        return ResponseEntity.ok(mapper.toBalanceResponse(result.document(), result.balance(), result.version()));
    }

    @GetMapping("/{document}/transactions")
    @Operation(summary = "Extrato de movimentações com saldo após cada lançamento")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<StatementResponse> listTransactions(
            @PathVariable String document,
            @RequestParam(defaultValue = "50") int limit,
            Authentication authentication) {

        AuthenticatedUser auth = (AuthenticatedUser) authentication.getPrincipal();
        var statement = listUserTransactionsUseCase.execute(
                new ListUserTransactionsUseCase.ListCommand(document, auth.document(), limit)
        );
        return ResponseEntity.ok(mapper.toStatementResponse(statement));
    }

    @GetMapping(value = "/{document}/transactions/export", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Exportar extrato em PDF")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<byte[]> exportStatementPdf(
            @PathVariable String document,
            @RequestParam(defaultValue = "50") int limit,
            Authentication authentication) {

        AuthenticatedUser auth = (AuthenticatedUser) authentication.getPrincipal();
        var pdf = exportStatementPdfUseCase.execute(
                new ExportStatementPdfUseCase.ExportCommand(document, auth.document(), limit)
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + pdf.filename() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf.content());
    }
}
