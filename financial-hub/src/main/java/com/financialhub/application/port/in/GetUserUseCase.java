package com.financialhub.application.port.in;

import com.financialhub.domain.model.User;

public interface GetUserUseCase {
    User execute(String document);
}
