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

    private static final String BASE_URL = "https://code-diary.xuewei.fun/CodeDiary/generate";
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
        int statusCode = response.statusCode();
        if (statusCode < 200 || statusCode >= 300) {
            throw new RuntimeException("请求失败: HTTP " + statusCode + (body != null && !body.isEmpty() ? " " + body : ""));
        }
        return parseResponse(body);
    }

    /**
     * 解析后端响应：code 为 OK 时返回 data；否则返回「生成失败：message」供界面展示。
     */
    private static String parseResponse(String body) {
        if (body == null || body.isEmpty()) return "";
        body = body.trim();
        if (!body.startsWith("{")) return body;
        String code = extractJsonStringValue(body, "code");
        if (code != null && !"OK".equals(code.trim())) {
            String message = extractJsonStringValue(body, "message");
            return "生成失败：" + (message != null && !message.isEmpty() ? message : code);
        }
        String data = extractJsonStringValue(body, "data");
        return data != null ? data : body;
    }

    /** 从 JSON 字符串中取出指定 key 的字符串值（支持 \" \n \r \t \\ 等转义） */
    private static String extractJsonStringValue(String body, String key) {
        String search = "\"" + key + "\"";
        int idx = body.indexOf(search);
        if (idx == -1) return null;
        int colon = body.indexOf(':', idx);
        if (colon == -1) return null;
        int start = body.indexOf('"', colon + 1);
        if (start == -1) return null;
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

}
