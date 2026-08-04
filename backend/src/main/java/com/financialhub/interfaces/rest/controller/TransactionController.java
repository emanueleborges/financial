package com.financialhub.interfaces.rest.controller;

import com.financialhub.application.port.in.GetReceiptUseCase;
import com.financialhub.application.port.in.GetTransactionUseCase;
import com.financialhub.application.port.in.ReverseTransactionUseCase;
import com.financialhub.application.port.in.TransferUseCase;
import com.financialhub.infrastructure.security.AuthenticatedUser;
import com.financialhub.interfaces.rest.dto.ReverseRequest;
import com.financialhub.interfaces.rest.dto.TransactionResponse;
import com.financialhub.interfaces.rest.dto.TransferRequest;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Transferências P2P (chave = CPF/CNPJ)")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private final TransferUseCase transferUseCase;
    private final GetTransactionUseCase getTransactionUseCase;
    private final ReverseTransactionUseCase reverseTransactionUseCase;
    private final GetReceiptUseCase getReceiptUseCase;
    private final RestMapper mapper;

    @PostMapping
    @Operation(summary = "Realizar transferência por CPF/CNPJ")
    public ResponseEntity<TransactionResponse> transfer(
            @Valid @RequestBody TransferRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyHeader,
            Authentication authentication) {

        AuthenticatedUser auth = (AuthenticatedUser) authentication.getPrincipal();
        String idempotencyKey = request.idempotencyKey() != null
                ? request.idempotencyKey()
                : idempotencyHeader;

        var tx = transferUseCase.execute(new TransferUseCase.TransferCommand(
                request.payerDocument(),
                request.payeeDocument(),
                auth.document(),
                request.amount(),
                idempotencyKey
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(tx));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar status da transação")
    public ResponseEntity<TransactionResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(mapper.toResponse(getTransactionUseCase.execute(id)));
    }

    @GetMapping(value = "/{id}/receipt", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Exportar comprovante PDF da transação")
    public ResponseEntity<byte[]> downloadReceipt(
            @PathVariable UUID id,
            Authentication authentication) {

        AuthenticatedUser auth = (AuthenticatedUser) authentication.getPrincipal();
        var receipt = getReceiptUseCase.execute(
                new GetReceiptUseCase.ReceiptCommand(id, auth.document())
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + receipt.filename() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(receipt.content());
    }

    @PostMapping("/reverse")
    @Operation(summary = "Estornar transação (pagador original)")
    public ResponseEntity<TransactionResponse> reverse(
            @Valid @RequestBody ReverseRequest request,
            Authentication authentication) {

        AuthenticatedUser auth = (AuthenticatedUser) authentication.getPrincipal();
        var tx = reverseTransactionUseCase.execute(
                new ReverseTransactionUseCase.ReverseCommand(
                        request.transactionId(),
                        request.reason(),
                        auth.document()
                )
        );
        return ResponseEntity.ok(mapper.toResponse(tx));
    }
}
