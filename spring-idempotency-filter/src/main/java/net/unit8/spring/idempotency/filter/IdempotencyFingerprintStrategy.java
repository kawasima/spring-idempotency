package net.unit8.spring.idempotency.filter;

import jakarta.servlet.http.HttpServletRequest;
import net.unit8.spring.idempotency.IdempotencyFingerprint;

public interface IdempotencyFingerprintStrategy {
    IdempotencyFingerprint create(HttpServletRequest request);
}
