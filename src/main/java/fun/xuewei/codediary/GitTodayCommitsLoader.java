package fun.xuewei.codediary;

import com.intellij.openapi.project.Project;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * 使用 JGit 加载「当前用户」的 Git 提交记录，支持时间范围。
 * 不依赖 git4idea，沙箱运行和未安装 Git 插件时也可用。
 */
public final class GitTodayCommitsLoader {

    private GitTodayCommitsLoader() {
    }

    /**
     * 加载当前项目目录下 Git 仓库中，在 [startMillis, endMillis] 时间内且为当前用户的提交。
     *
     * @param startMillis 起始时间（毫秒，含）
     * @param endMillis   结束时间（毫秒，含）
     */
    @NotNull
    public static List<CommitInfo> loadCommitsForCurrentUser(@NotNull Project project, long startMillis, long endMillis) {
        List<CommitInfo> result = new ArrayList<>();
        String basePath = project.getBasePath();
        if (basePath == null && project.getBaseDir() != null) {
            basePath = project.getBaseDir().getPath();
        }
        if (basePath == null) return result;

        File gitDir = new File(basePath, ".git");
        if (!gitDir.exists()) return result;

        try (Repository repo = FileRepositoryBuilder.create(gitDir)) {
            String userName = repo.getConfig().getString("user", null, "name");
            String userEmail = repo.getConfig().getString("user", null, "email");
            userName = userName != null ? userName.trim() : "";
            userEmail = userEmail != null ? userEmail.trim() : "";

            try (RevWalk walk = new RevWalk(repo)) {
                ObjectId head = repo.resolve("HEAD");
                if (head == null) return result;
                walk.markStart(walk.parseCommit(head));
                walk.sort(RevSort.COMMIT_TIME_DESC);

                for (RevCommit c : walk) {
                    long timeMillis = c.getCommitTime() * 1000L;
                    if (timeMillis > endMillis) continue;    // 还未到范围（太新），跳过
                    if (timeMillis < startMillis) break;     // 已超出范围（太旧），停止

                    if (!userName.isEmpty() || !userEmail.isEmpty()) {
                        PersonIdent author = c.getAuthorIdent();
                        if (author == null || !isCurrentUser(author.getName(), author.getEmailAddress(), userName, userEmail)) continue;
                    }

                    String subject = c.getShortMessage();
                    result.add(new CommitInfo(c.getName(), subject, timeMillis));
                }
            }
        } catch (IOException ignored) {
            // 非 Git 项目或读取失败
        }
        return result;
    }

    /**
     * 根据 hash 获取单条提交的详情，用于悬浮展示。格式：
     * Hash: <完整 hash>
     * 作者: <name> <email>
     * 时间: yyyy-MM-dd HH:mm:ss
     *
     * <完整提交说明，含标题与正文>
     */
    @NotNull
    public static String getCommitDetail(@NotNull Project project, @NotNull String hash) {
        String basePath = project.getBasePath();
        if (basePath == null && project.getBaseDir() != null) basePath = project.getBaseDir().getPath();
        if (basePath == null) return "无法获取项目路径";

        File gitDir = new File(basePath, ".git");
        if (!gitDir.exists()) return "未找到 Git 仓库";

        try (Repository repo = FileRepositoryBuilder.create(gitDir)) {
            ObjectId id = repo.resolve(hash);
            if (id == null) return "未找到提交: " + hash;
            try (RevWalk walk = new RevWalk(repo)) {
                RevCommit c = walk.parseCommit(id);
                PersonIdent author = c.getAuthorIdent();
                String name = author != null ? (author.getName() != null ? author.getName() : "") : "";
                String email = author != null && author.getEmailAddress() != null ? author.getEmailAddress() : "";
                String authorStr = name + (email.isEmpty() ? "" : " <" + email + ">");
                String timeStr = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(c.getCommitTime() * 1000L));
                String fullMessage = c.getFullMessage();
                if (fullMessage == null) fullMessage = c.getShortMessage();
                if (fullMessage != null) fullMessage = fullMessage.trim();
                return "Hash: " + c.getName() + "\n"
                    + "作者: " + authorStr + "\n"
                    + "时间: " + timeStr + "\n\n"
                    + (fullMessage != null ? fullMessage : "");
            }
        } catch (IOException e) {
            return "读取失败: " + e.getMessage();
        }
    }

    private static boolean isCurrentUser(String authorName, String authorEmail, String userName, String userEmail) {
        if (userName != null && !userName.isEmpty() && userName.equals(authorName)) return true;
        if (userEmail != null && !userEmail.isEmpty() && userEmail.equals(authorEmail)) return true;
        return false;
    }
}
