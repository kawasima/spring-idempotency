package net.unit8.spring.idempotency;

import java.util.Optional;
import java.util.function.BiFunction;

@FunctionalInterface
public interface EnsureInboundIdempotency extends BiFunction<String, Optional<IdempotencyFingerprint>,Optional<IdempotencyEntry>> {
}
