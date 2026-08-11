package fun.xuewei.codediary;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Objects;

/** 项目目录中扫描到的 Git 工作区。 */
public final class GitRepositoryInfo {

    private final Path rootPath;
    private final String name;
    private final String displayName;

    GitRepositoryInfo(@NotNull Path rootPath, @NotNull String name, @NotNull String displayName) {
        this.rootPath = rootPath.toAbsolutePath().normalize();
        this.name = name;
        this.displayName = displayName;
    }

    @NotNull
    public Path getRootPath() {
        return rootPath;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof GitRepositoryInfo)) return false;
        GitRepositoryInfo that = (GitRepositoryInfo) other;
        return rootPath.equals(that.rootPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rootPath);
    }
}
