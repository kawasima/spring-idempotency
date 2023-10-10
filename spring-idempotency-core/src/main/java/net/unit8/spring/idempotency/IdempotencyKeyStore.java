package net.unit8.spring.idempotency;

public interface IdempotencyKeyStore {
    IdempotencyEntry getAndSet(String idempotencyKey);
    void save(IdempotencyEntry entry);
    void delete(String idempotencyKey);
}
