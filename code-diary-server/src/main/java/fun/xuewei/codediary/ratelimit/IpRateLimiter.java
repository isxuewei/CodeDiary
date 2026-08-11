package fun.xuewei.codediary.ratelimit;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于内存的每 IP 固定分钟窗口限流器。
 *
 * @author 薛伟
 */
@Component
public class IpRateLimiter {

    private static final long MILLIS_PER_MINUTE = 60_000L;
    private static final long CLEANUP_INTERVAL = 256L;

    private final RateLimitProperties properties;
    private final Map<String, RequestCounter> counters = new ConcurrentHashMap<>();
    private final AtomicLong requestSequence = new AtomicLong();

    public IpRateLimiter(RateLimitProperties properties) {
        this.properties = properties;
    }

    /**
     * 尝试为指定 IP 获取一次请求配额。
     *
     * @param ip 客户端 IP
     * @return 是否允许本次请求
     */
    public boolean tryAcquire(String ip) {
        return tryAcquire(ip, System.currentTimeMillis() / MILLIS_PER_MINUTE);
    }

    boolean tryAcquire(String ip, long currentMinute) {
        RequestCounter counter = counters.compute(ip, (key, current) -> {
            if (current == null || current.minute != currentMinute) {
                return new RequestCounter(currentMinute, 1);
            }
            return new RequestCounter(currentMinute, current.count + 1);
        });

        cleanupExpiredCounters(currentMinute);
        return counter.count <= properties.getRequestsPerMinute();
    }

    private void cleanupExpiredCounters(long currentMinute) {
        if (requestSequence.incrementAndGet() % CLEANUP_INTERVAL != 0) {
            return;
        }
        counters.entrySet().removeIf(entry -> entry.getValue().minute < currentMinute);
    }

    private static final class RequestCounter {

        private final long minute;
        private final int count;

        private RequestCounter(long minute, int count) {
            this.minute = minute;
            this.count = count;
        }
    }
}
