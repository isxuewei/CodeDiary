package fun.xuewei.codediary;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenerateApiClientTest {

    @Test
    void shouldBuildGitCommitLogRequestWithEscapedDiff() {
        GenerateApiClient.GitCommitLogRequest request = new GenerateApiClient.GitCommitLogRequest();
        request.machineCode = "machine";
        request.clientVersion = "1.0";
        request.projectName = "demo";
        request.recentCommits = Collections.singletonList("feat: 初始化");
        request.changes = Arrays.asList("+\"name\"", "line1\nline2");
        request.commitLogFormat = "<type>: <描述>";

        String json = GenerateApiClient.buildGitCommitLogRequestBody(request);

        assertTrue(json.contains("\"recentCommits\":[\"feat: 初始化\"]"));
        assertTrue(json.contains("\"changes\":[\"+\\\"name\\\"\",\"line1\\nline2\"]"));
        assertTrue(json.contains("\"commitLogFormat\":\"<type>: <描述>\""));
    }

    @Test
    void shouldResolveLocalGitCommitLogUrl() {
        String oldValue = System.getProperty("codediary.environment");
        try {
            System.setProperty("codediary.environment", "local");
            assertEquals(
                "http://localhost:8080/CodeDiary/generateGitCommitLog",
                GenerateApiClient.resolveGitCommitLogUrl()
            );
        } finally {
            if (oldValue == null) {
                System.clearProperty("codediary.environment");
            } else {
                System.setProperty("codediary.environment", oldValue);
            }
        }
    }
}
