package fun.xuewei.codediary.ratelimit;

import fun.xuewei.codediary.exception.ServerBusyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 按客户端 IP 限制接口请求频率。
 *
 * @author 薛伟
 */
@Slf4j
@Component
public class IpRateLimitInterceptor implements HandlerInterceptor {

    private static final String UNKNOWN_IP = "unknown";

    private final RateLimitProperties properties;
    private final IpRateLimiter rateLimiter;

    public IpRateLimitInterceptor(RateLimitProperties properties, IpRateLimiter rateLimiter) {
        this.properties = properties;
        this.rateLimiter = rateLimiter;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!properties.isEnabled() || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String clientIp = resolveClientIp(request);
        if (rateLimiter.tryAcquire(clientIp)) {
            return true;
        }

        response.setHeader("Retry-After", "60");
        log.warn("IP 请求频率超过限制：ip={}，limit={}/min", clientIp, properties.getRequestsPerMinute());
        throw new ServerBusyException();
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (properties.isTrustForwardedHeaders()) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (hasText(forwardedFor)) {
                return forwardedFor.split(",", 2)[0].trim();
            }

            String realIp = request.getHeader("X-Real-IP");
            if (hasText(realIp)) {
                return realIp.trim();
            }
        }

        String remoteAddress = request.getRemoteAddr();
        return hasText(remoteAddress) ? remoteAddress : UNKNOWN_IP;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
