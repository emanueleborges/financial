package com.financialhub.infrastructure.cache;

import com.financialhub.application.port.out.BalanceCachePort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fallback in-memory para profiles {@code test}/{@code it} (Redis desabilitado).
 */
@Component
@Profile({"test", "it"})
public class InMemoryBalanceCacheAdapter implements BalanceCachePort {

    private final Map<UUID, BigDecimal> cache = new ConcurrentHashMap<>();

    @Override
    public Optional<BigDecimal> get(UUID userId) {
        return Optional.ofNullable(cache.get(userId));
    }

    @Override
    public void put(UUID userId, BigDecimal balance) {
        cache.put(userId, balance);
    }

    @Override
    public void evict(UUID userId) {
        cache.remove(userId);
    }
}
