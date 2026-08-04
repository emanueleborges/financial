package com.financialhub.application;

import com.financialhub.application.port.out.DocumentValidationPort;
import com.financialhub.application.port.out.PasswordEncoderPort;
import com.financialhub.application.port.out.UserRepositoryPort;
import com.financialhub.application.service.CreateUserService;
import com.financialhub.domain.exception.DuplicateResourceException;
import com.financialhub.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.UUID;

import static com.financialhub.application.port.in.CreateUserUseCase.CreateUserCommand;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateUserServiceTest {

    @Mock private UserRepositoryPort userRepository;
    @Mock private PasswordEncoderPort passwordEncoder;
    @Mock private DocumentValidationPort documentValidation;

    @InjectMocks
    private CreateUserService createUserService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(createUserService, "defaultDailyLimit", new BigDecimal("5000.00"));
    }

    @Test
    void shouldCreateUser() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByDocument(any())).thenReturn(false);
        when(documentValidation.isValidDocument(any())).thenReturn(true);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateUserCommand cmd = new CreateUserCommand(
                "João Silva", "joao@email.com", "52998224725", "senha123", new BigDecimal("500.00"));

        User user = createUserService.execute(cmd);

        assertNotNull(user.getId());
        assertEquals("joao@email.com", user.getEmail());
        assertEquals(new BigDecimal("500.00"), user.getBalance());
        assertEquals(new BigDecimal("5000.00"), user.getDailyLimit());
    }

    @Test
    void shouldRejectDuplicateEmail() {
        when(userRepository.existsByEmail(any())).thenReturn(true);

        CreateUserCommand cmd = new CreateUserCommand(
                "João", "joao@email.com", "52998224725", "senha123", BigDecimal.ZERO);

        assertThrows(DuplicateResourceException.class, () -> createUserService.execute(cmd));
    }
}
