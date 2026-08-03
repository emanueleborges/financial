package com.financialhub.application.port.out;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface BalanceCachePort {

    Optional<BigDecimal> get(UUID userId);

    void put(UUID userId, BigDecimal balance);

    void evict(UUID userId);
}
