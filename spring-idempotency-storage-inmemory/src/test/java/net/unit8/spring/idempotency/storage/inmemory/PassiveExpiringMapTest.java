package net.unit8.spring.idempotency.storage.inmemory;

import org.junit.jupiter.api.Test;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class PassiveExpiringMapTest {
    @Test
    void mapNormally() {
        PassiveExpiringMap<String, String> sut = new PassiveExpiringMap<>();
        sut.put("key", "value");
        assertThat(sut).hasSize(1).containsEntry("key", "value");
        sut.remove("key");
        assertThat(sut).isEmpty();
    }

    @Test
    void eviction() throws InterruptedException {
        PassiveExpiringMap<String, String> sut = new PassiveExpiringMap<>(1L);
        sut.put("key", "value");
        TimeUnit.of(ChronoUnit.MILLIS).sleep(10);
        assertThat(sut).isEmpty();
    }
}