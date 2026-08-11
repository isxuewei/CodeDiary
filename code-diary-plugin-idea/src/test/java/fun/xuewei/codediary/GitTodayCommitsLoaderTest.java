package fun.xuewei.codediary;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.StoredConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class GitTodayCommitsLoaderTest {

    @TempDir
    Path projectRoot;

    @Test
    void loadCommitsForCurrentUser_shouldKeepRepositoryIdentity() throws Exception {
        Path repositoryRoot = Files.createDirectories(projectRoot.resolve("services/FastDDD"));
        try (Git git = Git.init().setDirectory(repositoryRoot.toFile()).call()) {
            StoredConfig config = git.getRepository().getConfig();
            config.setString("user", null, "name", "CodeDiary Tester");
            config.setString("user", null, "email", "codediary@example.com");
            config.save();

            Files.writeString(repositoryRoot.resolve("README.md"), "FastDDD");
            git.add().addFilepattern("README.md").call();
            git.commit()
                .setMessage("feat: initialize repository")
                .setAuthor("CodeDiary Tester", "codediary@example.com")
                .setCommitter("CodeDiary Tester", "codediary@example.com")
                .call();
        }

        GitRepositoryInfo repository = GitRepositoryScanner.scan(projectRoot).get(0);
        List<CommitInfo> commits = GitTodayCommitsLoader.loadCommitsForCurrentUser(
            repository,
            0,
            Long.MAX_VALUE
        );

        assertEquals(1, commits.size());
        CommitInfo commit = commits.get(0);
        assertAll(
            () -> assertEquals("feat: initialize repository", commit.getSubject()),
            () -> assertSame(repository, commit.getRepository())
        );
    }

    @Test
    void loadRecentCommitSubjects_shouldReturnNewestCommitFirst() throws Exception {
        Path repositoryRoot = Files.createDirectories(projectRoot.resolve("demo"));
        try (Git git = Git.init().setDirectory(repositoryRoot.toFile()).call()) {
            Files.writeString(repositoryRoot.resolve("README.md"), "one");
            git.add().addFilepattern("README.md").call();
            git.commit().setMessage("feat: first").setAuthor("Tester", "test@example.com").call();

            Files.writeString(repositoryRoot.resolve("README.md"), "two");
            git.add().addFilepattern("README.md").call();
            git.commit().setMessage("fix: second").setAuthor("Tester", "test@example.com").call();
        }

        List<String> subjects = GitTodayCommitsLoader.loadRecentCommitSubjects(
            GitRepositoryScanner.scan(projectRoot),
            2
        );

        assertEquals(List.of("[demo] fix: second", "[demo] feat: first"), subjects);
    }
}
