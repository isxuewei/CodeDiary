package fun.xuewei.codediary.ratelimit;

import fun.xuewei.codediary.exception.ServerBusyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IpRateLimitInterceptorTest {

    private RateLimitProperties properties;
    private IpRateLimitInterceptor interceptor;

    @BeforeEach
    void setUp() {
        properties = new RateLimitProperties();
        properties.setRequestsPerMinute(1);
        interceptor = new IpRateLimitInterceptor(properties, new IpRateLimiter(properties));
    }

    @Test
    void shouldRejectRequestsAfterIpQuotaIsExhausted() {
        MockHttpServletRequest firstRequest = request("10.0.0.1");
        MockHttpServletRequest secondRequest = request("10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(firstRequest, new MockHttpServletResponse(), new Object()));
        assertThrows(
                ServerBusyException.class,
                () -> interceptor.preHandle(secondRequest, response, new Object())
        );
        assertEquals("60", response.getHeader("Retry-After"));
    }

    @Test
    void shouldIgnoreForwardedHeaderByDefault() {
        MockHttpServletRequest firstRequest = request("10.0.0.1");
        firstRequest.addHeader("X-Forwarded-For", "203.0.113.10");
        MockHttpServletRequest secondRequest = request("10.0.0.1");
        secondRequest.addHeader("X-Forwarded-For", "203.0.113.11");

        assertTrue(interceptor.preHandle(firstRequest, new MockHttpServletResponse(), new Object()));
        assertThrows(
                ServerBusyException.class,
                () -> interceptor.preHandle(secondRequest, new MockHttpServletResponse(), new Object())
        );
    }

    @Test
    void shouldUseForwardedHeaderWhenExplicitlyTrusted() {
        properties.setTrustForwardedHeaders(true);
        MockHttpServletRequest firstRequest = request("10.0.0.1");
        firstRequest.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.2");
        MockHttpServletRequest secondRequest = request("10.0.0.1");
        secondRequest.addHeader("X-Forwarded-For", "203.0.113.11, 10.0.0.2");

        assertTrue(interceptor.preHandle(firstRequest, new MockHttpServletResponse(), new Object()));
        assertTrue(interceptor.preHandle(secondRequest, new MockHttpServletResponse(), new Object()));
    }

    @Test
    void shouldNotCountCorsPreflightRequests() {
        MockHttpServletRequest request = request("10.0.0.1");
        request.setMethod("OPTIONS");

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
    }

    private MockHttpServletRequest request(String remoteAddress) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRemoteAddr(remoteAddress);
        return request;
    }
}
