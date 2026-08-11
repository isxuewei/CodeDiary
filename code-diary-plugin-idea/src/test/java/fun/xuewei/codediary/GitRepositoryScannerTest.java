package fun.xuewei.codediary;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GitRepositoryScannerTest {

    @TempDir
    Path projectRoot;

    @Test
    void scan_shouldFindRepositoriesInNestedDirectories() throws IOException {
        Files.createDirectories(projectRoot.resolve("FastDDD/.git/objects"));
        Files.createDirectories(projectRoot.resolve("services/SdYTH-Factory/.git/refs"));

        List<GitRepositoryInfo> repositories = GitRepositoryScanner.scan(projectRoot);

        assertEquals(List.of("FastDDD", "SdYTH-Factory"), repositoryNames(repositories));
    }

    @Test
    void scan_shouldRecognizeGitFileUsedByWorktree() throws IOException {
        Path worktree = Files.createDirectories(projectRoot.resolve("FastDDD-worktree"));
        Files.writeString(worktree.resolve(".git"), "gitdir: ../FastDDD/.git/worktrees/FastDDD-worktree\n");

        List<GitRepositoryInfo> repositories = GitRepositoryScanner.scan(projectRoot);

        assertEquals(List.of("FastDDD-worktree"), repositoryNames(repositories));
    }

    @Test
    void scan_shouldUseRelativePathsWhenRepositoryNamesAreDuplicated() throws IOException {
        Files.createDirectories(projectRoot.resolve("services/a/shared/.git"));
        Files.createDirectories(projectRoot.resolve("services/b/shared/.git"));

        List<GitRepositoryInfo> repositories = GitRepositoryScanner.scan(projectRoot);

        assertEquals(
            List.of("services/a/shared", "services/b/shared"),
            repositories.stream().map(GitRepositoryInfo::getDisplayName).collect(Collectors.toList())
        );
    }

    private static List<String> repositoryNames(List<GitRepositoryInfo> repositories) {
        return repositories.stream().map(GitRepositoryInfo::getName).collect(Collectors.toList());
    }
}
