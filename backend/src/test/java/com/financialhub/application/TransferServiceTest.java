package com.financialhub.application;

import com.financialhub.application.port.out.*;
import com.financialhub.application.service.TransactionStatusService;
import com.financialhub.application.service.TransferService;
import com.financialhub.domain.enums.UserStatus;
import com.financialhub.domain.exception.DailyLimitExceededException;
import com.financialhub.domain.exception.InvalidTransactionException;
import com.financialhub.domain.model.Transaction;
import com.financialhub.domain.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static com.financialhub.application.port.in.TransferUseCase.TransferCommand;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock private UserRepositoryPort userRepository;
    @Mock private TransactionRepositoryPort transactionRepository;
    @Mock private AuditRepositoryPort auditRepository;
    @Mock private TransactionEventPublisherPort eventPublisher;
    @Mock private BalanceCachePort balanceCache;
    @Mock private TransactionStatusService transactionStatusService;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private TransferService transferService;

    private UUID payerId;
    private UUID payeeId;
    private static final String PAYER_DOC = "52998224725";
    private static final String PAYEE_DOC = "39053344705";

    @BeforeEach
    void setUp() {
        payerId = UUID.randomUUID();
        payeeId = UUID.randomUUID();
    }

    @Test
    void shouldTransferByDocument() throws Exception {
        User payer = user(payerId, PAYER_DOC, new BigDecimal("1000.00"));
        User payee = user(payeeId, PAYEE_DOC, new BigDecimal("100.00"));

        Transaction pending = Transaction.builder()
                .id(UUID.randomUUID())
                .payerId(payerId)
                .payeeId(payeeId)
                .amount(new BigDecimal("50.00"))
                .status(com.financialhub.domain.enums.TransactionStatus.PENDING)
                .type(com.financialhub.domain.enums.TransactionType.TRANSFER)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(userRepository.findByDocument(PAYER_DOC)).thenReturn(Optional.of(payer));
        when(userRepository.findByDocument(PAYEE_DOC)).thenReturn(Optional.of(payee));
        when(userRepository.getDailySpent(payerId)).thenReturn(BigDecimal.ZERO);
        when(transactionStatusService.createPending(any(), any(), any(), any())).thenReturn(pending);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        TransferCommand cmd = new TransferCommand(
                PAYER_DOC, PAYEE_DOC, PAYER_DOC, new BigDecimal("50.00"), null);
        Transaction result = transferService.execute(cmd);

        assertNotNull(result);
        verify(userRepository).transferBalance(payerId, payeeId, new BigDecimal("50.00"));
    }

    @Test
    void shouldRejectSameDocuments() {
        TransferCommand cmd = new TransferCommand(
                PAYER_DOC, PAYER_DOC, PAYER_DOC, new BigDecimal("10.00"), null);
        assertThrows(InvalidTransactionException.class, () -> transferService.execute(cmd));
    }

    @Test
    void shouldRejectDailyLimitExceeded() {
        User payer = user(payerId, PAYER_DOC, new BigDecimal("10000.00"));
        User payee = user(payeeId, PAYEE_DOC, new BigDecimal("0.00"));

        when(userRepository.findByDocument(PAYER_DOC)).thenReturn(Optional.of(payer));
        when(userRepository.findByDocument(PAYEE_DOC)).thenReturn(Optional.of(payee));
        when(userRepository.getDailySpent(payerId)).thenReturn(new BigDecimal("4900.00"));

        TransferCommand cmd = new TransferCommand(
                PAYER_DOC, PAYEE_DOC, PAYER_DOC, new BigDecimal("200.00"), null);
        assertThrows(DailyLimitExceededException.class, () -> transferService.execute(cmd));
    }

    @Test
    void shouldReturnExistingOnIdempotencyKey() {
        Transaction existing = Transaction.builder()
                .id(UUID.randomUUID())
                .payerId(payerId)
                .payeeId(payeeId)
                .amount(new BigDecimal("50.00"))
                .status(com.financialhub.domain.enums.TransactionStatus.COMPLETED)
                .type(com.financialhub.domain.enums.TransactionType.TRANSFER)
                .idempotencyKey("dup-key")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(transactionRepository.findByIdempotencyKey("dup-key")).thenReturn(Optional.of(existing));

        TransferCommand cmd = new TransferCommand(
                PAYER_DOC, PAYEE_DOC, PAYER_DOC, new BigDecimal("50.00"), "dup-key");
        Transaction result = transferService.execute(cmd);

        assertEquals(existing.getId(), result.getId());
        verify(userRepository, never()).transferBalance(any(), any(), any());
    }

    private User user(UUID id, String document, BigDecimal balance) {
        return User.builder()
                .id(id)
                .name("User")
                .email(id + "@test.com")
                .document(document)
                .passwordHash("hash")
                .balance(balance)
                .status(UserStatus.ACTIVE)
                .dailyLimit(new BigDecimal("5000.00"))
                .version(0L)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
