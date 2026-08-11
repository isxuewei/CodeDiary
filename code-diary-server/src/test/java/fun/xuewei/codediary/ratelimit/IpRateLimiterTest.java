package fun.xuewei.codediary.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IpRateLimiterTest {

    private IpRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setRequestsPerMinute(2);
        rateLimiter = new IpRateLimiter(properties);
    }

    @Test
    void shouldLimitEachIpIndependentlyWithinTheSameMinute() {
        long minute = 100L;

        assertTrue(rateLimiter.tryAcquire("192.168.1.10", minute));
        assertTrue(rateLimiter.tryAcquire("192.168.1.10", minute));
        assertFalse(rateLimiter.tryAcquire("192.168.1.10", minute));
        assertTrue(rateLimiter.tryAcquire("192.168.1.11", minute));
    }

    @Test
    void shouldResetQuotaInTheNextMinute() {
        assertTrue(rateLimiter.tryAcquire("192.168.1.10", 100L));
        assertTrue(rateLimiter.tryAcquire("192.168.1.10", 100L));
        assertFalse(rateLimiter.tryAcquire("192.168.1.10", 100L));

        assertTrue(rateLimiter.tryAcquire("192.168.1.10", 101L));
    }
}
