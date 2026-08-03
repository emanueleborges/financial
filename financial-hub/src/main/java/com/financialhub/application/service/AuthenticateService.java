package com.financialhub.application.service;

import com.financialhub.application.port.in.AuthenticateUseCase;
import com.financialhub.application.port.out.PasswordEncoderPort;
import com.financialhub.application.port.out.TokenProviderPort;
import com.financialhub.application.port.out.UserRepositoryPort;
import com.financialhub.domain.exception.DomainException;
import com.financialhub.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticateService implements AuthenticateUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final TokenProviderPort tokenProvider;

    @Override
    @Transactional(readOnly = true)
    public AuthResult execute(AuthCommand command) {
        String document = command.document() == null ? "" : command.document().replaceAll("\\D", "");
        User user = userRepository.findByDocument(document)
                .orElseThrow(() -> new DomainException("INVALID_CREDENTIALS", "Credenciais inválidas"));

        if (!passwordEncoder.matches(command.password(), user.getPasswordHash())) {
            throw new DomainException("INVALID_CREDENTIALS", "Credenciais inválidas");
        }

        String accessToken = tokenProvider.generateAccessToken(
                user.getId(), user.getDocument(), user.getEmail());
        String refreshToken = tokenProvider.generateRefreshToken(
                user.getId(), user.getDocument(), user.getEmail());

        return new AuthResult(
                accessToken,
                refreshToken,
                "Bearer",
                tokenProvider.getAccessTokenExpirationMs() / 1000
        );
    }
}
