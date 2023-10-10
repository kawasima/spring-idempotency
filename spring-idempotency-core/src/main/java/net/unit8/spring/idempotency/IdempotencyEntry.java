package net.unit8.spring.idempotency;

public record IdempotencyEntry(
        String idempotencyKey,
        IdempotencyFingerprint fingerprint,
        IdempotencyResponse response
) {
}
