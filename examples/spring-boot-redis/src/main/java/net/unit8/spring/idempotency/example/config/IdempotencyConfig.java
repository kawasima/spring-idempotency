package net.unit8.spring.idempotency.example.config;

import net.unit8.spring.idempotency.IdempotencyKeyStore;
import net.unit8.spring.idempotency.filter.IdempotencyFilter;
import net.unit8.spring.idempotency.storage.inmemory.InMemoryIdempotencyStore;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class IdempotencyConfig {
    @Bean
    public IdempotencyKeyStore idempotencyKeyStore() {
        InMemoryIdempotencyStore store = new InMemoryIdempotencyStore();
        store.setExpiry(Duration.ofDays(1));
        return store;
    }

    @Bean
    public IdempotencyFilter idempotencyFilter(IdempotencyKeyStore idempotencyKeyStore) {
        IdempotencyFilter filter = new IdempotencyFilter();
        filter.setIdempotencyKeyStore(idempotencyKeyStore);
        return filter;
    }
    @Bean
    public FilterRegistrationBean<IdempotencyFilter> idempotencyFilterRegistration(IdempotencyFilter filter) {
        FilterRegistrationBean<IdempotencyFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(filter);
        return registrationBean;
    }
}
