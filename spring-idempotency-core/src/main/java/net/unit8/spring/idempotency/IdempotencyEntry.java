package net.unit8.spring.idempotency;

public record IdempotencyEntry(
        IdempotencyFingerprint fingerprint,
        String body
) {
}
