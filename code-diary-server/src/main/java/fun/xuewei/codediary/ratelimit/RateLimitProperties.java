package fun.xuewei.codediary.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Min;

/**
 * 接口限流配置。
 *
 * @author 薛伟
 */
@Validated
@Component
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {

    /**
     * 是否启用限流。
     */
    private boolean enabled = true;

    /**
     * 每个 IP 每分钟允许的最大请求数。
     */
    @Min(value = 1, message = "rate-limit.requests-per-minute 必须大于 0")
    private int requestsPerMinute = 60;

    /**
     * 是否信任 X-Forwarded-For 和 X-Real-IP 请求头。
     * 仅当服务位于可信反向代理之后时开启。
     */
    private boolean trustForwardedHeaders = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getRequestsPerMinute() {
        return requestsPerMinute;
    }

    public void setRequestsPerMinute(int requestsPerMinute) {
        this.requestsPerMinute = requestsPerMinute;
    }

    public boolean isTrustForwardedHeaders() {
        return trustForwardedHeaders;
    }

    public void setTrustForwardedHeaders(boolean trustForwardedHeaders) {
        this.trustForwardedHeaders = trustForwardedHeaders;
    }
}
