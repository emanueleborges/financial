package com.financialhub.application.service;

import com.financialhub.application.port.in.GetBalanceUseCase;
import com.financialhub.application.port.out.BalanceCachePort;
import com.financialhub.application.port.out.UserRepositoryPort;
import com.financialhub.domain.exception.UserNotFoundException;
import com.financialhub.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class GetBalanceService implements GetBalanceUseCase {

    private final UserRepositoryPort userRepository;
    private final BalanceCachePort balanceCache;

    @Override
    @Transactional(readOnly = true)
    public BalanceResult execute(String document) {
        String digits = document == null ? "" : document.replaceAll("\\D", "");
        User user = userRepository.findByDocument(digits)
                .orElseThrow(() -> new UserNotFoundException("documento " + digits));

        BigDecimal balance = balanceCache.get(user.getId()).orElseGet(() -> {
            balanceCache.put(user.getId(), user.getBalance());
            return user.getBalance();
        });

        return new BalanceResult(user.getDocument(), balance, user.getVersion());
    }
}
