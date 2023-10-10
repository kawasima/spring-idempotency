package net.unit8.spring.idempotency.storage.inmemory;

import net.unit8.spring.idempotency.*;
import org.springframework.beans.factory.InitializingBean;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class InMemoryIdempotencyStore implements IdempotencyKeyStore, InitializingBean {
    private PassiveExpiringMap<String, String> map;

    private Duration expiry = Duration.ofDays(1);
    private IdempotencyEntrySerializer entrySerializer = new IdempotencyEntrySerializer() {};
    private IdempotencyEntryDeserializer entryDeserializer = new IdempotencyEntryDeserializer() {};

    public InMemoryIdempotencyStore() {
    }

    @Override
    public IdempotencyEntry getAndSet(String idempotencyKey, IdempotencyFingerprint fingerprint) {
        String encodedEntry = map.get(idempotencyKey);
        if (encodedEntry != null) {
            return entryDeserializer.apply(idempotencyKey, encodedEntry);
        }
        String entry = entrySerializer.apply(new IdempotencyEntry(idempotencyKey, fingerprint, null));
        map.put(idempotencyKey, entry);
        return null;
    }

    @Override
    public void save(IdempotencyEntry entry) {
        map.put(entry.idempotencyKey(), entrySerializer.apply(entry));
    }

    @Override
    public void delete(String idempotencyKey) {
        map.remove(idempotencyKey);
    }

    public void setEntrySerializer(IdempotencyEntrySerializer entrySerializer) {
        this.entrySerializer = entrySerializer;
    }

    public void setEntryDeserializer(IdempotencyEntryDeserializer entryDeserializer) {
        this.entryDeserializer = entryDeserializer;
    }

    public void setExpiry(Duration expiry) {
        this.expiry = expiry;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.map = new PassiveExpiringMap<>(expiry.toMillis(), TimeUnit.MILLISECONDS);
    }
}
