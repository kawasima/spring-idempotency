package net.unit8.spring.idempotency.filter.fingerprint;

import jakarta.servlet.http.HttpServletRequest;
import net.unit8.spring.idempotency.IdempotencyFingerprint;
import net.unit8.spring.idempotency.filter.IdempotencyFingerprintStrategy;
import org.springframework.util.DigestUtils;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.WebUtils;

public class DigestFingerprintStrategy implements IdempotencyFingerprintStrategy {
    @Override
    public IdempotencyFingerprint create(HttpServletRequest request) {
        ContentCachingRequestWrapper wrapper = WebUtils.getNativeRequest(request, ContentCachingRequestWrapper.class);
        if (wrapper == null) {
            return null;
        }
        return new IdempotencyFingerprint(DigestUtils.md5DigestAsHex(wrapper.getContentAsByteArray()));
    }
}
