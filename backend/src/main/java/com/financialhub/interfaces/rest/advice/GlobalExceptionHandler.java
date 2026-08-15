package com.financialhub.interfaces.rest.advice;

import com.financialhub.domain.exception.*;
import com.financialhub.interfaces.rest.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getCode(), ex.getMessage(), req, null);
    }

    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTxNotFound(TransactionNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getCode(), ex.getMessage(), req, null);
    }

    @ExceptionHandler({
            InsufficientBalanceException.class,
            DailyLimitExceededException.class,
            InactiveAccountException.class,
            InvalidTransactionException.class,
            DuplicateResourceException.class
    })
    public ResponseEntity<ErrorResponse> handleBusiness(DomainException ex, HttpServletRequest req) {
        HttpStatus status = ex instanceof DuplicateResourceException
                ? HttpStatus.CONFLICT
                : HttpStatus.UNPROCESSABLE_ENTITY;
        return build(status, ex.getCode(), ex.getMessage(), req, null);
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomain(DomainException ex, HttpServletRequest req) {
        HttpStatus status = switch (ex.getCode()) {
            case "INVALID_CREDENTIALS" -> HttpStatus.UNAUTHORIZED;
            case "FORBIDDEN" -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.BAD_REQUEST;
        };
        return build(status, ex.getCode(), ex.getMessage(), req, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fields.putIfAbsent(error.getField(), error.getDefaultMessage())
        );
        return build(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Um ou mais campos são inválidos",
                req,
                fields
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Erro interno em {}", req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Erro interno inesperado", req, null);
    }

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest req,
            Map<String, String> fields) {
        return ResponseEntity.status(status).body(new ErrorResponse(
                code, message, Instant.now(), req.getRequestURI(), fields
        ));
    }
}
