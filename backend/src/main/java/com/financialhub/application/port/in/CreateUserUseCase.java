package com.financialhub.application.port.in;

import com.financialhub.domain.model.User;

import java.math.BigDecimal;

public interface CreateUserUseCase {

    User execute(CreateUserCommand command);

    record CreateUserCommand(
            String name,
            String email,
            String document,
            String password,
            BigDecimal initialBalance
    ) {}
}
