package net.unit8.spring.idempotency.storage.redis;

import net.unit8.spring.idempotency.IdempotencyEntry;
import org.junit.jupiter.api.*;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.testcontainers.containers.GenericContainer;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RedisInboundIdempotencyTest {
    GenericContainer redis;
    @BeforeAll
    void setup() {
        redis = new GenericContainer<>("redis:7-alpine")
                .withExposedPorts(6379);
        redis.start();
    }

    @AfterAll
    void tearDown() {
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
        Optional<IdempotencyEntry> response = sut.apply("jj");
        assertThat(response).isEmpty();
        connectionFactory.destroy();
    }
}