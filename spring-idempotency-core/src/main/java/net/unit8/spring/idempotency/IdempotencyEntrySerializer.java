package net.unit8.spring.idempotency;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public interface IdempotencyEntrySerializer extends Function<IdempotencyEntry, String> {
    default String apply(IdempotencyEntry entry) {
        Optional<String> status = Optional.ofNullable(entry.response())
                .map(IdempotencyResponse::status)
                .map(String::valueOf);
        Optional<String> headers = Optional.ofNullable(entry.response())
                .map(IdempotencyResponse::headers)
                .map(hs -> {
                    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                         ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                        oos.writeObject(hs);
                        return Base64.getEncoder().encodeToString(baos.toByteArray());
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
        Optional<String> body = Optional.ofNullable(entry.response())
                .map(IdempotencyResponse::body)
                .map(b -> Base64.getEncoder().encodeToString(b));
        return  status.orElse("")
                + "." + headers.orElse("")
                + "." + body.orElse("")
                + "." + Objects.toString(entry.fingerprint(), "");
    }
}
