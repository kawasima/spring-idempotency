# Spring Idempotency

This library provides a mechanism to ensure idempotency for spring framework, based on `draft-ietf-httpapi-idempotency-key-header-03`.

## Usage

### Maven

```xml
<dependency>
    <groupId>net.unit8.spring.idempotency</groupId>
    <artifactId>spring-idempotency-filter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

### Configuration

The processing of the idempotency key is achieved through the ServletFilter, so it is registered in the Configuration.

The minimal configuration for an idempotency key is as follows.
The IdempotencyKeyStore can be selected from server in-memory or Redis.

```java
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
```

## License

This library is released under the Apache License, Version 2.0.
