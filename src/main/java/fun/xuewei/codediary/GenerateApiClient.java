package fun.xuewei.codediary;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * 调用 CodeDiary 后端生成接口。
 */
public final class GenerateApiClient {

    private static final String BASE_URL = "http://xuewei.fun:8080/CodeDiary/generate";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

    /**
     * 请求体对应 GenerateDTO：machineCode, clientVersion, projectName, commits, reportFormat, otherContent
     */
    public static class GenerateRequest {
        public String machineCode;
        public String clientVersion;
        public String projectName;
        public List<String> commits;
        public String reportFormat;
        public String otherContent;
    }

    /**
     * 调用生成接口，返回生成的日报内容；失败时抛出异常或返回错误信息。
     */
    public static String generate(GenerateRequest req) throws Exception {
        String json = buildRequestBody(req);
        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .build();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL))
            .header("Content-Type", "application/json; charset=UTF-8")
            .timeout(REQUEST_TIMEOUT)
            .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String body = response.body();
        int code = response.statusCode();
        if (code < 200 || code >= 300) {
            throw new RuntimeException("请求失败: HTTP " + code + (body != null && !body.isEmpty() ? " " + body : ""));
        }
        return extractContent(body);
    }

    private static String buildRequestBody(GenerateRequest req) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"machineCode\":").append(escapeJson(req.machineCode));
        sb.append(",\"clientVersion\":").append(escapeJson(req.clientVersion));
        sb.append(",\"projectName\":").append(escapeJson(req.projectName != null ? req.projectName : ""));
        sb.append(",\"commits\":[");
        if (req.commits != null) {
            for (int i = 0; i < req.commits.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(escapeJson(req.commits.get(i)));
            }
        }
        sb.append("]");
        sb.append(",\"reportFormat\":").append(escapeJson(req.reportFormat != null ? req.reportFormat : ""));
        sb.append(",\"otherContent\":").append(escapeJson(req.otherContent != null ? req.otherContent : ""));
        sb.append("}");
        return sb.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) return "\"\"";
        StringBuilder sb = new StringBuilder(s.length() + 8);
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default: sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }

    /** 从响应 JSON 中取出内容字段（如 data/content），否则返回原始 body */
    private static String extractContent(String body) {
        if (body == null || body.isEmpty()) return "";
        body = body.trim();
        if (!body.startsWith("{")) return body;
        // 简单按 "data":" 或 "content":" 取第一个可打印内容
        for (String key : new String[] { "\"data\"", "\"content\"", "\"result\"" }) {
            int idx = body.indexOf(key);
            if (idx == -1) continue;
            int colon = body.indexOf(':', idx);
            if (colon == -1) continue;
            int start = body.indexOf('"', colon + 1);
            if (start == -1) continue;
            start += 1;
            StringBuilder value = new StringBuilder();
            for (int i = start; i < body.length(); i++) {
                char c = body.charAt(i);
                if (c == '\\' && i + 1 < body.length()) {
                    char next = body.charAt(i + 1);
                    if (next == '"') { value.append('"'); i++; continue; }
                    if (next == 'n') { value.append('\n'); i++; continue; }
                    if (next == 'r') { value.append('\r'); i++; continue; }
                    if (next == 't') { value.append('\t'); i++; continue; }
                    if (next == '\\') { value.append('\\'); i++; continue; }
                }
                if (c == '"') break;
                value.append(c);
            }
            return value.toString();
        }
        return body;
    }
}
