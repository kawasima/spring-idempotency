package net.unit8.spring.idempotency.storage.redis;

import net.unit8.spring.idempotency.*;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Ensure idempotency for inbound request.
 *
 * @author kawasima
 */
public class RedisInboundIdempotency implements IdempotencyKeyStore {
    private final RedisTemplate<String, String> redisTemplate;
    private IdempotencyEntrySerializer entrySerializer = new IdempotencyEntrySerializer() {};
    private IdempotencyEntryDeserializer entryDeserializer = new IdempotencyEntryDeserializer() {};

    public RedisInboundIdempotency(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public IdempotencyEntry getAndSet(String idempotencyKey, IdempotencyFingerprint fingerprint) {
        IdempotencyEntry entry = new IdempotencyEntry(idempotencyKey, fingerprint, null);
        if (redisTemplate.boundValueOps(idempotencyKey)
                .setIfAbsent(entrySerializer.apply(entry))) {
            return null;
        } else {
            String value = redisTemplate.boundValueOps(idempotencyKey)
                    .get();
            return entryDeserializer.apply(idempotencyKey, value);
        }
    }

    @Override
    public void save(IdempotencyEntry entry) {
        redisTemplate.boundValueOps(entry.idempotencyKey())
                .set(entrySerializer.apply(entry));
    }

    @Override
    public void delete(String idempotencyKey) {
        redisTemplate.delete(idempotencyKey);
    }

    /**
     * Set the serializer for idempotency entry.
     * @param entrySerializer The serializer for idempotency entry.
     */
    public void setIdempotencyEntrySerializer(IdempotencyEntrySerializer entrySerializer) {
        this.entrySerializer = entrySerializer;
    }

    /**
     * Set the deserializer for idempotency entry.
     * @param entryDeserializer The deserializer for idempotency entry.
     */
    public void setIdempotencyEntryDeserializer(IdempotencyEntryDeserializer entryDeserializer) {
        this.entryDeserializer = entryDeserializer;
    }
}
