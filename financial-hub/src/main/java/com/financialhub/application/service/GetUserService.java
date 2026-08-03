package com.financialhub.application.service;

import com.financialhub.application.port.in.GetUserUseCase;
import com.financialhub.application.port.out.UserRepositoryPort;
import com.financialhub.domain.exception.UserNotFoundException;
import com.financialhub.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetUserService implements GetUserUseCase {

    private final UserRepositoryPort userRepository;

    @Override
    @Transactional(readOnly = true)
    public User execute(String document) {
        String digits = normalize(document);
        return userRepository.findByDocument(digits)
                .orElseThrow(() -> new UserNotFoundException("documento " + digits));
    }

    private static String normalize(String document) {
        return document == null ? "" : document.replaceAll("\\D", "");
    }
}
