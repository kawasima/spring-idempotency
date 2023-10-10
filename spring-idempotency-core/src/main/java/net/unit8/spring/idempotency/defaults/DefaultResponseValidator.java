package net.unit8.spring.idempotency.defaults;

import net.unit8.spring.idempotency.IdempotencyResponse;
import net.unit8.spring.idempotency.ResponseValidator;

public class DefaultResponseValidator implements ResponseValidator {
    @Override
    public boolean validate(IdempotencyResponse response) {
        return response.status() >= 200 && response.status() < 300;
    }
}
