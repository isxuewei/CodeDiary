package fun.xuewei.codediary.client;

import com.alibaba.fastjson2.JSONObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import fun.xuewei.codediary.config.AiProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiClientTest {

    private HttpServer server;
    private AiProperties properties;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        properties = new AiProperties();
        properties.setUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/v1/messages");
        properties.setKey("test-key");
        properties.setModel("test-model");
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void shouldSendAnthropicMessageAndReturnText() {
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> anthropicVersion = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        server.createContext("/v1/messages", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            anthropicVersion.set(exchange.getRequestHeaders().getFirst("anthropic-version"));
            requestBody.set(readBody(exchange));
            respond(exchange, 200, "{\"content\":[{\"type\":\"text\",\"text\":\"生成成功\"}]}");
        });
        server.start();

        String answer = new AiClient(properties).ask("请生成日报");

        assertEquals("生成成功", answer);
        assertEquals("Bearer test-key", authorization.get());
        assertEquals("2023-06-01", anthropicVersion.get());

        JSONObject json = JSONObject.parseObject(requestBody.get());
        assertEquals("test-model", json.getString("model"));
        assertEquals(2048, json.getIntValue("max_tokens"));
        assertFalse(json.containsKey("stream"));
        assertEquals("user", json.getJSONArray("messages").getJSONObject(0).getString("role"));
        assertEquals("请生成日报", json.getJSONArray("messages").getJSONObject(0).getString("content"));
    }

    @Test
    void shouldRejectMissingConfiguration() {
        properties.setUrl(" ");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> new AiClient(properties).ask("问题")
        );

        assertEquals("未配置 ai.url", exception.getMessage());
    }

    @Test
    void shouldExposeHttpFailure() {
        server.createContext("/v1/messages", exchange -> respond(exchange, 503, "服务不可用"));
        server.start();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> new AiClient(properties).ask("问题")
        );

        assertEquals("AI 接口调用失败，HTTP 503", exception.getMessage());
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream input = exchange.getRequestBody();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = input.read(buffer)) != -1) {
                output.write(buffer, 0, length);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }
}
