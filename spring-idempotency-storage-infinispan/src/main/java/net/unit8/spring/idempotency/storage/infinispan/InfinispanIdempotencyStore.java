package net.unit8.spring.idempotency.storage.infinispan;

import net.unit8.spring.idempotency.IdempotencyEntry;
import net.unit8.spring.idempotency.IdempotencyKeyStore;
import org.infinispan.Cache;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class InfinispanIdempotencyStore implements IdempotencyKeyStore {
    private final Cache<String, IdempotencyEntry> cache;
    private Duration expiry = Duration.ofDays(1);

    public InfinispanIdempotencyStore(Cache<String, IdempotencyEntry> cache) {
        this.cache = cache;
    }

    @Override
    public IdempotencyEntry getAndSet(String idempotencyKey) {
        IdempotencyEntry newEntry = new IdempotencyEntry(idempotencyKey, null, null);
        return Optional.ofNullable(cache.putIfAbsent(idempotencyKey, newEntry, expiry.toMillis(), TimeUnit.MILLISECONDS))
                .orElse(newEntry);
    }

    @Override
    public void save(IdempotencyEntry entry) {
        cache.put(entry.idempotencyKey(), entry, expiry.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void delete(String idempotencyKey) {
        cache.remove(idempotencyKey);
    }

    public void setExpiry(Duration expiry) {
        this.expiry = expiry;
    }
}
