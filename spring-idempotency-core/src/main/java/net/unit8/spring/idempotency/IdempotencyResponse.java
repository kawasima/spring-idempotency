package net.unit8.spring.idempotency;

import java.util.Collection;
import java.util.Map;

public record IdempotencyResponse(
        int status,
        Map<String, Collection<String>> headers,
        byte[] body
) {
}
