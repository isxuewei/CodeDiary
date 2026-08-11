package fun.xuewei.codediary;

import com.intellij.openapi.diff.impl.patch.FilePatch;
import com.intellij.openapi.diff.impl.patch.IdeaTextPatchBuilder;
import com.intellij.openapi.diff.impl.patch.UnifiedDiffWriter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangesUtil;
import com.intellij.openapi.vcs.changes.CurrentContentRevision;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 把 Commit 面板中勾选的变更转换成适合发送给 AI 的文件清单和 unified diff。
 */
public final class GitChangeSummaryBuilder {

    static final int MAX_CHANGE_COUNT = 50;
    static final int MAX_PATCH_CHARS = 40_000;

    private GitChangeSummaryBuilder() {
    }

    @NotNull
    public static String build(
        @NotNull Project project,
        @NotNull Collection<? extends Change> selectedChanges,
        @NotNull Collection<? extends FilePath> selectedUnversionedFiles
    ) {
        List<Change> changes = new ArrayList<>();
        for (Change change : selectedChanges) {
            if (changes.size() >= MAX_CHANGE_COUNT) break;
            changes.add(change);
        }
        for (FilePath file : selectedUnversionedFiles) {
            if (changes.size() >= MAX_CHANGE_COUNT) break;
            changes.add(new Change(null, CurrentContentRevision.create(file)));
        }

        StringBuilder result = new StringBuilder();
        appendFileSummary(result, project, changes);
        int totalCount = selectedChanges.size() + selectedUnversionedFiles.size();
        if (totalCount > changes.size()) {
            result.append("\n注意：共选择 ").append(totalCount)
                .append(" 个文件，仅发送前 ").append(changes.size()).append(" 个文件的差异。\n");
        }

        if (changes.isEmpty()) return result.toString();
        result.append("\n代码差异（unified diff）：\n");
        String basePath = project.getBasePath();
        if (basePath == null || basePath.trim().isEmpty()) {
            java.io.File ancestor = ChangesUtil.findCommonAncestor(changes);
            basePath = ancestor != null ? ancestor.getAbsolutePath() : ".";
        }

        try {
            List<FilePatch> patches = IdeaTextPatchBuilder.buildPatch(
                project,
                changes,
                Paths.get(basePath),
                false,
                true
            );
            LimitedStringWriter writer = new LimitedStringWriter(MAX_PATCH_CHARS);
            UnifiedDiffWriter.write(project, patches, writer, "\n", null);
            result.append(writer.getText());
            if (writer.isTruncated()) {
                result.append("\n...（代码差异过长，已截断）");
            }
        } catch (VcsException | IOException | RuntimeException e) {
            result.append("无法读取完整代码差异：")
                .append(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }
        return result.toString();
    }

    private static void appendFileSummary(StringBuilder result, Project project, List<Change> changes) {
        result.append("变更文件清单：\n");
        for (Change change : changes) {
            String relativePath = ChangesUtil.getProjectRelativePath(
                project,
                ChangesUtil.getFilePath(change).getIOFile()
            );
            result.append("- ").append(change.getType().name()).append(' ')
                .append(relativePath != null ? relativePath : ChangesUtil.getFilePath(change).getPath())
                .append('\n');
        }
    }

    /** 限制 diff 在内存及请求体中的大小，超出后继续接收写入但不再保存。 */
    private static final class LimitedStringWriter extends Writer {
        private final int limit;
        private final StringBuilder value = new StringBuilder();
        private boolean truncated;

        private LimitedStringWriter(int limit) {
            this.limit = limit;
        }

        @Override
        public void write(char[] chars, int offset, int length) {
            int remaining = limit - value.length();
            if (remaining <= 0) {
                truncated = true;
                return;
            }
            int accepted = Math.min(remaining, length);
            value.append(chars, offset, accepted);
            if (accepted < length) truncated = true;
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }

        private String getText() {
            return value.toString();
        }

        private boolean isTruncated() {
            return truncated;
        }
    }
}
