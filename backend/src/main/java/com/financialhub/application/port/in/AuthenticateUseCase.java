package com.financialhub.application.port.in;

public interface AuthenticateUseCase {

    AuthResult execute(AuthCommand command);

    AuthResult refresh(String refreshToken);

    record AuthCommand(String document, String password) {}

    record AuthResult(String accessToken, String refreshToken, String tokenType, long expiresIn) {}
}
