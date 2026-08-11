package fun.xuewei.codediary;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GenerateGitCommitLogActionTest {

    @Test
    void shouldRemoveMarkdownFenceFromGeneratedMessage() {
        String normalized = GenerateGitCommitLogAction.normalizeGeneratedMessage(
            "```text\nfeat(core): add commit generator\n```"
        );

        assertEquals("feat(core): add commit generator", normalized);
    }

    @Test
    void shouldKeepPlainGeneratedMessage() {
        assertEquals(
            "fix: 修复空指针",
            GenerateGitCommitLogAction.normalizeGeneratedMessage("  fix: 修复空指针  ")
        );
    }
}
