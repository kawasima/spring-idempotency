package net.unit8.spring.idempotency.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.unit8.spring.idempotency.*;
import net.unit8.spring.idempotency.defaults.DefaultResponseValidator;
import net.unit8.spring.idempotency.filter.fingerprint.DigestFingerprintStrategy;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class IdempotencyFilter extends OncePerRequestFilter {
    private static final String DEFAULT_IDEMPOTENCY_KEY_HEADER_NAME = "Idempotency-Key";
    private IdempotencyKeyStore idempotencyKeyStore;
    private boolean enabledResponseIdempotency;
    /**
     * The strategy to create idempotency fingerprint.
     */
    private IdempotencyFingerprintStrategy idempotencyFingerprintStrategy;

    private ResponseValidator responseValidator;

    /**
     * The name of the header that contains the idempotency key.
     */
    private String idempotencyKeyHeaderName;

    private Set<String> headerWhitelist = Set.of("content-type");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String idempotencyKey = request.getHeader(idempotencyKeyHeaderName);
        if (idempotencyKey == null) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<IdempotencyFingerprint> fingerprint = Optional.ofNullable(idempotencyFingerprintStrategy)
                .map(strategy -> strategy.create(request));
        IdempotencyEntry idempotencyEntry = idempotencyKeyStore.getAndSet(idempotencyKey, fingerprint.orElse(null));
        if (idempotencyEntry == null) {
            processIdempotencyRequest(request, response, filterChain, idempotencyKey, fingerprint.orElse(null));
        } else {
            if (fingerprint.filter(f -> !Objects.equals(f, idempotencyEntry.fingerprint())).isPresent()) {
                response.sendError(422);
                return;
            }
            processRetry(request, response, filterChain, idempotencyEntry);
        }

    }

    private void processIdempotencyRequest(HttpServletRequest request,
                                           HttpServletResponse response,
                                           FilterChain filterChain,
                                           String idempotencyKey,
                                           IdempotencyFingerprint fingerprint)
            throws IOException, ServletException {
        filterChain.doFilter(request, response);
        ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(response);
        IdempotencyResponse idempotencyResponse = new IdempotencyResponse(wrapper.getStatus(),
                wrapper.getHeaderNames().stream()
                        .filter(name -> headerWhitelist.contains(name))
                        .collect(Collectors.toMap(
                                Function.identity(),
                                response::getHeaders
                        )),
                wrapper.getContentAsByteArray());
        wrapper.copyBodyToResponse();
        if (responseValidator.validate(idempotencyResponse)) {
            idempotencyKeyStore.save(new IdempotencyEntry(
                    idempotencyKey,
                    fingerprint,
                    idempotencyResponse
            ));
        } else {
            idempotencyKeyStore.delete(idempotencyKey);
        }
    }
    private void processRetry(HttpServletRequest request,
                              HttpServletResponse response,
                              FilterChain filterChain,
                              IdempotencyEntry entry) throws IOException, ServletException {
        idempotencyFingerprintStrategy.create(request);
        if (entry.response() == null) {
            response.sendError(409);
        } else {
            response.setStatus(entry.response().status());
            Optional.ofNullable(entry.response().headers())
                    .ifPresent(headers -> headers.forEach((name, values) -> {
                        values.forEach(value -> response.addHeader(name, value));
                    }));

            if(entry.response().body() != null) {
                response.getOutputStream().write(entry.response().body());
            }
            response.getOutputStream().close();
        }
    }

    public void setIdempotencyKeyStore(IdempotencyKeyStore idempotencyKeyStore) {
        this.idempotencyKeyStore = idempotencyKeyStore;
    }

    public void setIdempotencyFingerprintStrategy(IdempotencyFingerprintStrategy fingerprintStrategy) {
        this.idempotencyFingerprintStrategy = fingerprintStrategy;
    }

    public void setIdempotencyKeyHeaderName(String idempotencyKeyHeaderName) {
        this.idempotencyKeyHeaderName = idempotencyKeyHeaderName;
    }

    @Override
    public void afterPropertiesSet() {
        if (idempotencyKeyHeaderName == null) {
            idempotencyKeyHeaderName = DEFAULT_IDEMPOTENCY_KEY_HEADER_NAME;
        }
        if (responseValidator == null) {
            responseValidator = new DefaultResponseValidator();
        }
        if (idempotencyFingerprintStrategy == null) {
            idempotencyFingerprintStrategy = new DigestFingerprintStrategy();
        }
    }
}
