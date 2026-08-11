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
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 使用 JGit 加载「当前用户」的 Git 提交记录，支持时间范围。
 * 不依赖 git4idea，沙箱运行和未安装 Git 插件时也可用。
 */
public final class GitTodayCommitsLoader {

    private GitTodayCommitsLoader() {
    }

    @NotNull
    public static List<GitRepositoryInfo> scanRepositories(@NotNull Project project) {
        String basePath = resolveProjectBasePath(project);
        return basePath != null
            ? GitRepositoryScanner.scan(new File(basePath).toPath())
            : new ArrayList<>();
    }

    /** 加载项目目录及其子目录中全部 Git 仓库的当前用户提交。 */
    @NotNull
    public static List<CommitInfo> loadCommitsForCurrentUser(
        @NotNull Project project,
        long startMillis,
        long endMillis
    ) {
        return loadCommitsForCurrentUser(scanRepositories(project), startMillis, endMillis);
    }

    /** 加载指定 Git 仓库中，在 [startMillis, endMillis] 时间内且为当前用户的提交。 */
    @NotNull
    public static List<CommitInfo> loadCommitsForCurrentUser(
        @NotNull List<GitRepositoryInfo> repositories,
        long startMillis,
        long endMillis
    ) {
        List<CommitInfo> result = new ArrayList<>();
        for (GitRepositoryInfo repository : repositories) {
            result.addAll(loadCommitsForCurrentUser(repository, startMillis, endMillis));
        }
        result.sort(Comparator.comparingLong(CommitInfo::getCommitTimeMillis).reversed());
        return result;
    }

    /**
     * 读取各仓库最近的提交标题，作为生成新提交日志时的语言与格式参考。
     */
    @NotNull
    public static List<String> loadRecentCommitSubjects(@NotNull Project project, int limit) {
        return loadRecentCommitSubjects(scanRepositories(project), limit);
    }

    @NotNull
    static List<String> loadRecentCommitSubjects(
        @NotNull List<GitRepositoryInfo> repositories,
        int limit
    ) {
        if (limit <= 0) return new ArrayList<>();
        List<CommitInfo> commits = new ArrayList<>();
        for (GitRepositoryInfo repository : repositories) {
            try (Repository repo = openRepository(repository);
                 RevWalk walk = new RevWalk(repo)) {
                ObjectId head = repo.resolve("HEAD");
                if (head == null) continue;
                walk.markStart(walk.parseCommit(head));
                walk.sort(RevSort.COMMIT_TIME_DESC);
                int repositoryCount = 0;
                for (RevCommit commit : walk) {
                    commits.add(new CommitInfo(
                        commit.getName(),
                        commit.getShortMessage(),
                        commit.getCommitTime() * 1000L,
                        repository
                    ));
                    repositoryCount++;
                    if (repositoryCount >= limit) break;
                }
            } catch (IOException ignored) {
                // 单个仓库读取失败时继续读取其他仓库。
            }
        }
        return commits.stream()
            .sorted(Comparator.comparingLong(CommitInfo::getCommitTimeMillis).reversed())
            .limit(limit)
            .map(commit -> {
                GitRepositoryInfo repository = commit.getRepository();
                String prefix = repository != null ? "[" + repository.getDisplayName() + "] " : "";
                return prefix + commit.getSubject();
            })
            .collect(Collectors.toList());
    }

    @NotNull
    public static List<CommitInfo> loadCommitsForCurrentUser(
        @NotNull GitRepositoryInfo repositoryInfo,
        long startMillis,
        long endMillis
    ) {
        List<CommitInfo> result = new ArrayList<>();

        try (Repository repo = openRepository(repositoryInfo)) {
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
                    result.add(new CommitInfo(c.getName(), subject, timeMillis, repositoryInfo));
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
    public static String getCommitDetail(@NotNull CommitInfo commit) {
        GitRepositoryInfo repository = commit.getRepository();
        if (repository == null) return "无法确定提交所属的 Git 仓库";
        return getCommitDetail(repository, commit.getHash());
    }

    @NotNull
    public static String getCommitDetail(@NotNull GitRepositoryInfo repositoryInfo, @NotNull String hash) {
        try (Repository repo = openRepository(repositoryInfo)) {
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

    /** 兼容旧调用：在项目扫描到的仓库中查找该提交。 */
    @NotNull
    public static String getCommitDetail(@NotNull Project project, @NotNull String hash) {
        for (GitRepositoryInfo repository : scanRepositories(project)) {
            String detail = getCommitDetail(repository, hash);
            if (!detail.startsWith("未找到提交:")) return detail;
        }
        return "未找到提交: " + hash;
    }

    private static Repository openRepository(GitRepositoryInfo repositoryInfo) throws IOException {
        File workTree = repositoryInfo.getRootPath().toFile();
        return new FileRepositoryBuilder()
            .setWorkTree(workTree)
            .findGitDir(workTree)
            .build();
    }

    private static String resolveProjectBasePath(Project project) {
        String basePath = project.getBasePath();
        if (basePath == null && project.getBaseDir() != null) {
            basePath = project.getBaseDir().getPath();
        }
        return basePath;
    }

    private static boolean isCurrentUser(String authorName, String authorEmail, String userName, String userEmail) {
        if (userName != null && !userName.isEmpty() && userName.equals(authorName)) return true;
        if (userEmail != null && !userEmail.isEmpty() && userEmail.equals(authorEmail)) return true;
        return false;
    }
}
