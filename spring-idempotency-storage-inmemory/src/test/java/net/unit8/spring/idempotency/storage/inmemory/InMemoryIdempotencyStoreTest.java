package net.unit8.spring.idempotency.storage.inmemory;

import net.unit8.spring.idempotency.IdempotencyEntry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryIdempotencyStoreTest {
    @Test
    void getAndSet() throws Exception {
        InMemoryIdempotencyStore sut = new InMemoryIdempotencyStore();
        sut.afterPropertiesSet();
        IdempotencyEntry entry = sut.getAndSet("key", null);
        assertThat(entry).isNull();
        entry = sut.getAndSet("key", null);
        System.out.println(entry);
    }

    @Test
    void s() throws Exception {
        InMemoryIdempotencyStore sut = new InMemoryIdempotencyStore();
        sut.afterPropertiesSet();

        sut.save(new IdempotencyEntry("key", null, null));
        IdempotencyEntry entry = sut.getAndSet("key", null);
        assertThat(entry).isNotNull()
                .hasFieldOrPropertyWithValue("idempotencyKey", "key")
                .hasFieldOrPropertyWithValue("fingerprint", null);
    }
}