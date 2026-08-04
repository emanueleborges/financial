package com.financialhub.interfaces.rest.controller;

import com.financialhub.application.port.in.AuthenticateUseCase;
import com.financialhub.interfaces.rest.dto.AuthResponse;
import com.financialhub.interfaces.rest.dto.LoginRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Autenticação JWT por CPF/CNPJ")
public class AuthController {

    private final AuthenticateUseCase authenticateUseCase;

    @PostMapping("/login")
    @Operation(summary = "Login por CPF/CNPJ e obtenção de token JWT")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        var result = authenticateUseCase.execute(
                new AuthenticateUseCase.AuthCommand(request.document(), request.password())
        );
        return ResponseEntity.ok(new AuthResponse(
                result.accessToken(),
                result.refreshToken(),
                result.tokenType(),
                result.expiresIn()
        ));
    }
}
