package net.unit8.spring.idempotency.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.unit8.spring.idempotency.EnsureInboundIdempotency;
import net.unit8.spring.idempotency.IdempotencyFingerprint;
import net.unit8.spring.idempotency.IdempotencyEntry;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

public class IdempotencyFilter extends OncePerRequestFilter {
    private EnsureInboundIdempotency ensureInboundIdempotency;
    private IdempotencyFingerprintStrategy idempotencyFingerprintStrategy;
    private String idempotencyKeyHeaderName;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String idempotencyKey = request.getHeader(idempotencyKeyHeaderName);
        if (idempotencyKey == null) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<IdempotencyFingerprint> fingerprint = Optional.ofNullable(idempotencyFingerprintStrategy)
                .map(strategy -> strategy.create(request));
        Optional<IdempotencyEntry> idempotencyResponse = ensureInboundIdempotency.apply(idempotencyKey, fingerprint);
        if (idempotencyResponse.isEmpty()) {
            filterChain.doFilter(request, response);
        } else {
            response.sendError(409);
        }
    }

    private void processFingerprint(HttpServletRequest request) {

    }

    public void setEnsureInboundIdempotency(EnsureInboundIdempotency ensureInboundIdempotency) {
        this.ensureInboundIdempotency = ensureInboundIdempotency;
    }

    public void setIdempotencyFingerprintStrategy(IdempotencyFingerprintStrategy fingerprintStrategy) {
        this.idempotencyFingerprintStrategy = fingerprintStrategy;
    }

    public void setIdempotencyKeyHeaderName(String idempotencyKeyHeaderName) {
        this.idempotencyKeyHeaderName = idempotencyKeyHeaderName;
    }
}
