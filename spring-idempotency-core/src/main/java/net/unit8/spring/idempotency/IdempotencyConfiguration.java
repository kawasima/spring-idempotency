package net.unit8.spring.idempotency;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class IdempotencyConfiguration {
    private static final Duration DEFAULT_EXPIRY = Duration.ofDays(1);
    private Duration expiry;

    public void setExpiry(Duration expiry) {
        this.expiry = expiry;
    }
    public Duration getExpiry() {
        return expiry;
    }
}
