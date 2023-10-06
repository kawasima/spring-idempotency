package net.unit8.spring.idempotency;

import java.util.Objects;
import java.util.Optional;

public class IdempotencyFingerprint {
    private final String value;
    public IdempotencyFingerprint(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object another) {
        return Optional.ofNullable(another)
                .filter(IdempotencyFingerprint.class::isInstance)
                .map(IdempotencyFingerprint.class::cast)
                .map(o -> Objects.equals(o.value, value))
                .orElse(false);

    }
}
