package net.unit8.spring.idempotency.storage.redis;

import net.unit8.spring.idempotency.EnsureInboundIdempotency;
import net.unit8.spring.idempotency.IdempotencyEntry;
import net.unit8.spring.idempotency.IdempotencyFingerprint;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Optional;

public class RedisInboundIdempotency implements EnsureInboundIdempotency {
    private final RedisTemplate<String, String> redisTemplate;

    public RedisInboundIdempotency(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Optional<IdempotencyEntry> apply(String idempotencyKey, Optional<IdempotencyFingerprint> fingerprint) {
        IdempotencyEntry entry = new IdempotencyEntry(fingerprint.orElse(null), null);
        if (redisTemplate.boundValueOps(idempotencyKey)
                .setIfAbsent(entry.toString())) {
            return Optional.empty();
        } else {
            String value = redisTemplate.boundValueOps(idempotencyKey)
                    .get();
            return Optional.of(new IdempotencyEntry(fingerprint.orElse(null), value));
        }
    }
}
