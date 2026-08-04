package com.financialhub.domain.exception;

public class UserNotFoundException extends DomainException {
    public UserNotFoundException(String id) {
        super("USER_NOT_FOUND", "Usuário não encontrado: " + id);
    }
}
