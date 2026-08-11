package fun.xuewei.codediary.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 客户端配置。
 *
 * @author 薛伟
 */
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    /**
     * 完整的 Anthropic Messages 兼容接口地址。
     */
    private String url;

    /**
     * API Key。
     */
    private String key;

    /**
     * 模型名称。
     */
    private String model;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
