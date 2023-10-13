package net.unit8.spring.idempotency.defaults;

import net.unit8.spring.idempotency.IdempotencyEntry;
import net.unit8.spring.idempotency.IdempotencyKeyStore;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

public class DefaultIdempotencyKeyStore implements IdempotencyKeyStore {
    private final CacheManager cacheManager;
    private static final String DEFAULT_CACHE_NAME = "idempotency";
    private String cacheName = DEFAULT_CACHE_NAME;

    public DefaultIdempotencyKeyStore(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public IdempotencyEntry getAndSet(String idempotencyKey) {
        IdempotencyEntry entry = new IdempotencyEntry(idempotencyKey, null, null);
        Cache.ValueWrapper existingEntry = cacheManager.getCache(cacheName)
                .putIfAbsent(idempotencyKey, entry);
        return existingEntry != null ? (IdempotencyEntry) existingEntry.get() : entry;
    }

    @Override
    public void save(IdempotencyEntry entry) {
        cacheManager.getCache(cacheName)
                .put(entry.idempotencyKey(), entry);
    }

    @Override
    public void delete(String idempotencyKey) {
        cacheManager.getCache(cacheName).evict(idempotencyKey);
    }

    public void setCacheName(String cacheName) {
        this.cacheName = cacheName;
    }
}
