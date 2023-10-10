package net.unit8.spring.idempotency;

public interface ResponseValidator {
    boolean validate(IdempotencyResponse response);
}
