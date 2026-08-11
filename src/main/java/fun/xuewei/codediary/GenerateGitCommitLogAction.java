package fun.xuewei.codediary;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.CommitMessageI;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.vcs.commit.AbstractCommitWorkflowHandler;
import com.intellij.vcs.commit.CommitWorkflowHandler;
import com.intellij.vcs.commit.CommitWorkflowUi;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Commit 消息工具栏入口：根据当前勾选的 Git 变更生成提交日志并回填消息框。
 */
public final class GenerateGitCommitLogAction extends AnAction {

    private static final String CLIENT_VERSION = "26.08.12";
    private static final int RECENT_COMMIT_LIMIT = 10;

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        boolean gitActive = project != null
            && ProjectLevelVcsManager.getInstance(project).checkVcsIsActive("Git");
        CommitTarget target = resolveCommitTarget(e);
        e.getPresentation().setVisible(gitActive);
        e.getPresentation().setEnabled(gitActive && target.canWriteMessage() && target.hasIncludedFiles());
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        CommitTarget target = resolveCommitTarget(e);
        if (!target.hasIncludedFiles()) {
            Messages.showInfoMessage(project, "请先勾选要提交的代码变更。", "CodeDiary");
            return;
        }
        if (!target.canWriteMessage()) {
            Messages.showWarningDialog(project, "未找到可回填的提交消息输入框。", "CodeDiary");
            return;
        }

        CodeDiarySettings settings = CodeDiarySettings.getInstance();
        String machineCode = settings.getMachineCode();
        if (machineCode == null || machineCode.trim().isEmpty()) {
            machineCode = UUID.randomUUID().toString();
            settings.setMachineCode(machineCode);
        }
        String finalMachineCode = machineCode;
        String commitLogFormat = settings.getGitCommitLogFormat();

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "正在生成 Git 提交日志", true) {
            private String generatedMessage;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(false);
                indicator.setText("正在读取已选择的代码变更");
                indicator.setFraction(0.15);
                String changeSummary = GitChangeSummaryBuilder.build(
                    project,
                    target.changes,
                    target.unversionedFiles
                );

                indicator.checkCanceled();
                indicator.setText("正在读取最近的 Git 提交记录");
                indicator.setFraction(0.45);
                List<String> recentCommits = GitTodayCommitsLoader.loadRecentCommitSubjects(
                    project,
                    RECENT_COMMIT_LIMIT
                );

                indicator.checkCanceled();
                indicator.setText("正在生成提交日志");
                indicator.setFraction(0.65);
                GenerateApiClient.GitCommitLogRequest request = new GenerateApiClient.GitCommitLogRequest();
                request.machineCode = finalMachineCode;
                request.clientVersion = CLIENT_VERSION;
                request.projectName = project.getName();
                request.recentCommits = recentCommits;
                request.changes = Collections.singletonList(changeSummary);
                request.commitLogFormat = commitLogFormat;
                try {
                    generatedMessage = GenerateApiClient.generateGitCommitLog(request);
                } catch (Exception exception) {
                    throw new RuntimeException(exception.getMessage(), exception);
                }
                if (generatedMessage == null || generatedMessage.trim().isEmpty()) {
                    throw new IllegalStateException("生成结果为空");
                }
                if (generatedMessage.startsWith("生成失败：")) {
                    throw new IllegalStateException(generatedMessage);
                }
                generatedMessage = normalizeGeneratedMessage(generatedMessage);
                if (generatedMessage.isEmpty()) {
                    throw new IllegalStateException("生成结果为空");
                }
                indicator.setFraction(1.0);
            }

            @Override
            public void onSuccess() {
                if (!project.isDisposed()) {
                    target.setCommitMessage(generatedMessage);
                }
            }

            @Override
            public void onThrowable(@NotNull Throwable error) {
                if (!project.isDisposed()) {
                    String message = error.getMessage() != null ? error.getMessage() : error.getClass().getSimpleName();
                    Messages.showErrorDialog(project, "Git 提交日志生成失败：" + message, "CodeDiary");
                }
            }
        });
    }

    private static CommitTarget resolveCommitTarget(AnActionEvent e) {
        CommitMessageI messageControl = e.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL);
        CommitWorkflowHandler workflowHandler = e.getData(VcsDataKeys.COMMIT_WORKFLOW_HANDLER);
        List<Change> changes = new ArrayList<>();
        List<FilePath> unversionedFiles = new ArrayList<>();

        if (workflowHandler instanceof AbstractCommitWorkflowHandler) {
            AbstractCommitWorkflowHandler<?, ?> handler = (AbstractCommitWorkflowHandler<?, ?>) workflowHandler;
            CommitWorkflowUi ui = handler.getUi();
            changes.addAll(ui.getIncludedChanges());
            unversionedFiles.addAll(ui.getIncludedUnversionedFiles());
        } else {
            Change[] contextChanges = e.getData(VcsDataKeys.CHANGES);
            if (contextChanges != null) changes.addAll(Arrays.asList(contextChanges));
        }
        return new CommitTarget(messageControl, workflowHandler, changes, unversionedFiles);
    }

    static String normalizeGeneratedMessage(String message) {
        String normalized = message != null ? message.trim() : "";
        if (!normalized.startsWith("```")) return normalized;
        int firstLineEnd = normalized.indexOf('\n');
        if (firstLineEnd < 0) return normalized;
        normalized = normalized.substring(firstLineEnd + 1);
        int closingFence = normalized.lastIndexOf("```");
        if (closingFence >= 0 && normalized.substring(closingFence + 3).trim().isEmpty()) {
            normalized = normalized.substring(0, closingFence);
        }
        return normalized.trim();
    }

    private static final class CommitTarget {
        private final CommitMessageI messageControl;
        private final CommitWorkflowHandler workflowHandler;
        private final List<Change> changes;
        private final List<FilePath> unversionedFiles;

        private CommitTarget(
            CommitMessageI messageControl,
            CommitWorkflowHandler workflowHandler,
            List<Change> changes,
            List<FilePath> unversionedFiles
        ) {
            this.messageControl = messageControl;
            this.workflowHandler = workflowHandler;
            this.changes = changes;
            this.unversionedFiles = unversionedFiles;
        }

        private boolean hasIncludedFiles() {
            return !changes.isEmpty() || !unversionedFiles.isEmpty();
        }

        private boolean canWriteMessage() {
            return messageControl != null || workflowHandler instanceof AbstractCommitWorkflowHandler;
        }

        private void setCommitMessage(String message) {
            if (messageControl != null) {
                messageControl.setCommitMessage(message);
            } else if (workflowHandler instanceof AbstractCommitWorkflowHandler) {
                ((AbstractCommitWorkflowHandler<?, ?>) workflowHandler).setCommitMessage(message);
            }
        }
    }
}
