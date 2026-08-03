package com.financialhub.infrastructure.cache;

import com.financialhub.application.port.out.BalanceCachePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class RedisBalanceCacheAdapter implements BalanceCachePort {

    private static final String KEY_PREFIX = "balance:";

    private final StringRedisTemplate redisTemplate;

    @Value("${app.redis.balance-ttl-minutes:5}")
    private long ttlMinutes;

    @Override
    public Optional<BigDecimal> get(UUID userId) {
        try {
            String value = redisTemplate.opsForValue().get(KEY_PREFIX + userId);
            return value != null ? Optional.of(new BigDecimal(value)) : Optional.empty();
        } catch (Exception e) {
            log.warn("Redis indisponível para get: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void put(UUID userId, BigDecimal balance) {
        try {
            redisTemplate.opsForValue().set(
                    KEY_PREFIX + userId,
                    balance.toPlainString(),
                    Duration.ofMinutes(ttlMinutes)
            );
        } catch (Exception e) {
            log.warn("Redis indisponível para put: {}", e.getMessage());
        }
    }

    @Override
    public void evict(UUID userId) {
        try {
            redisTemplate.delete(KEY_PREFIX + userId);
        } catch (Exception e) {
            log.warn("Redis indisponível para evict: {}", e.getMessage());
        }
    }
}
