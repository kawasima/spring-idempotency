package net.unit8.spring.idempotency.storage.redis;

import net.unit8.spring.idempotency.IdempotencyEntry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.testcontainers.containers.GenericContainer;

import static org.assertj.core.api.Assertions.assertThat;

class RedisInboundIdempotencyTest {
    static GenericContainer<?> redis;

    @SuppressWarnings("resource")
    @BeforeAll
    static void setup() {
        redis = new GenericContainer<>("redis:7-alpine")
                .withExposedPorts(6379);
        redis.start();
    }

    @AfterAll
    static void tearDown() {
        redis.stop();
    }

    @Test
    void firstAccess() {
        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(
                redis.getHost(),
                redis.getMappedPort(6379)
        );
        connectionFactory.afterPropertiesSet();
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.afterPropertiesSet();
        RedisInboundIdempotency sut = new RedisInboundIdempotency(redisTemplate);
        IdempotencyEntry response = sut.getAndSet("jj", null);
        assertThat(response).isNull();
        connectionFactory.destroy();
    }

}