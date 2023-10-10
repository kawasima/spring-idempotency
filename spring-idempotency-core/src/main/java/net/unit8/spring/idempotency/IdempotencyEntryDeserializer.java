package net.unit8.spring.idempotency;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.UncheckedIOException;
import java.util.Base64;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

public interface IdempotencyEntryDeserializer extends BiFunction<String, String, IdempotencyEntry> {
    default IdempotencyEntry apply(String idempotencyKey, String encoded) {
        Base64.Decoder decoder = Base64.getDecoder();
        String[] tokens = encoded.split("\\.", 4);
        Optional<Integer> status = Optional.of(tokens[0])
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt);
        final Map<String, Collection<String>> headers;
        if (!tokens[1].isEmpty()) {
            try (ByteArrayInputStream bais = new ByteArrayInputStream(decoder.decode(tokens[1]));
                 ObjectInputStream ois = new ObjectInputStream(bais)) {
                //noinspection unchecked
                headers = (Map<String, Collection<String>>) ois.readObject();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        } else {
            headers = null;
        }
        Optional<byte[]> body = Optional.of(tokens[2])
                .filter(s -> !s.isEmpty())
                .map(decoder::decode);
        return new IdempotencyEntry(
                idempotencyKey,
                Optional.of(tokens[3]).filter(s -> !s.isEmpty())
                        .map(IdempotencyFingerprint::new).orElse(null),
                status.map(st -> new IdempotencyResponse(st, headers, body.orElse(null)))
                        .orElse(null));

    }
}

