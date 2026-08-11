package fun.xuewei.codediary.client;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import fun.xuewei.codediary.config.AiProperties;
import org.springframework.stereotype.Service;

/**
 * Anthropic Messages 兼容 AI 客户端。
 *
 * @author 薛伟
 */
@Service
public class AiClient {

    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final int MAX_TOKENS = 2048;
    private static final int TIMEOUT_MS = 120_000;

    private final AiProperties properties;

    public AiClient(AiProperties properties) {
        this.properties = properties;
    }

    /**
     * 发送问题，并同步等待模型返回完整回答。
     *
     * @param question 用户问题
     * @return 模型回答
     */
    public String ask(String question) {
        validateConfiguration();
        if (!hasText(question)) {
            throw new IllegalArgumentException("问题不能为空");
        }

        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", question);

        JSONArray messages = new JSONArray();
        messages.add(message);

        JSONObject requestBody = new JSONObject();
        requestBody.put("model", properties.getModel().trim());
        requestBody.put("max_tokens", MAX_TOKENS);
        requestBody.put("messages", messages);

        try (HttpResponse response = HttpRequest.post(properties.getUrl().trim())
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + properties.getKey().trim())
                .header("anthropic-version", ANTHROPIC_VERSION)
                .body(requestBody.toJSONString())
                .timeout(TIMEOUT_MS)
                .execute()) {
            if (!response.isOk()) {
                throw new IllegalStateException("AI 接口调用失败，HTTP " + response.getStatus());
            }

            JSONObject responseBody = JSONObject.parseObject(response.body());
            return extractAnswer(responseBody);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("AI 接口调用异常", e);
        }
    }

    private String extractAnswer(JSONObject response) {
        JSONArray content = response.getJSONArray("content");
        if (content == null || content.isEmpty()) {
            throw new IllegalStateException("AI 响应中缺少 content");
        }

        for (int i = 0; i < content.size(); i++) {
            JSONObject block = content.getJSONObject(i);
            if (block != null && hasText(block.getString("text"))) {
                return block.getString("text");
            }
        }

        throw new IllegalStateException("AI 响应中没有文本内容");
    }

    private void validateConfiguration() {
        if (!hasText(properties.getUrl())) {
            throw new IllegalStateException("未配置 ai.url");
        }
        if (!hasText(properties.getKey())) {
            throw new IllegalStateException("未配置 ai.key");
        }
        if (!hasText(properties.getModel())) {
            throw new IllegalStateException("未配置 ai.model");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
