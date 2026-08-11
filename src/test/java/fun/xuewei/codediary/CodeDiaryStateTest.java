package fun.xuewei.codediary;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CodeDiaryStateTest {

    @Test
    void shouldUseDefaultGitCommitLogFormatWhenValueIsBlank() {
        CodeDiaryState state = new CodeDiaryState();

        state.setGitCommitLogFormat("  ");

        assertEquals(CodeDiaryState.DEFAULT_GIT_COMMIT_LOG_FORMAT, state.getGitCommitLogFormat());
    }

    @Test
    void shouldKeepCustomGitCommitLogFormat() {
        CodeDiaryState state = new CodeDiaryState();

        state.setGitCommitLogFormat("fix: {summary}");

        assertEquals("fix: {summary}", state.getGitCommitLogFormat());
    }
}
