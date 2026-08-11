package fun.xuewei.codediary;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** 递归扫描项目目录下的 Git 工作区。 */
public final class GitRepositoryScanner {

    private GitRepositoryScanner() {
    }

    @NotNull
    public static List<GitRepositoryInfo> scan(@NotNull Path projectRoot) {
        Path normalizedRoot = projectRoot.toAbsolutePath().normalize();
        Set<Path> repositoryRoots = new LinkedHashSet<>();
        if (!Files.isDirectory(normalizedRoot)) return new ArrayList<>();

        try {
            Files.walkFileTree(normalizedRoot, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (isGitMarker(dir)) {
                        addRepositoryRoot(repositoryRoots, dir.getParent());
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (isGitMarker(file)) {
                        addRepositoryRoot(repositoryRoots, file.getParent());
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ignored) {
            // 返回已成功扫描到的仓库，单个目录不可访问不应影响整个项目。
        }

        List<Path> sortedRoots = new ArrayList<>(repositoryRoots);
        sortedRoots.sort((left, right) -> relativePath(normalizedRoot, left)
            .compareToIgnoreCase(relativePath(normalizedRoot, right)));

        Map<String, Integer> nameCounts = new HashMap<>();
        for (Path repositoryRoot : sortedRoots) {
            String name = repositoryName(repositoryRoot);
            String key = name.toLowerCase(Locale.ROOT);
            nameCounts.put(key, nameCounts.getOrDefault(key, 0) + 1);
        }

        List<GitRepositoryInfo> repositories = new ArrayList<>();
        for (Path repositoryRoot : sortedRoots) {
            String name = repositoryName(repositoryRoot);
            boolean duplicated = nameCounts.get(name.toLowerCase(Locale.ROOT)) > 1;
            String displayName = duplicated ? relativePath(normalizedRoot, repositoryRoot) : name;
            repositories.add(new GitRepositoryInfo(repositoryRoot, name, displayName));
        }
        return repositories;
    }

    private static boolean isGitMarker(Path path) {
        Path fileName = path.getFileName();
        return fileName != null && ".git".equals(fileName.toString());
    }

    private static void addRepositoryRoot(Set<Path> roots, Path root) {
        if (root != null) roots.add(root.toAbsolutePath().normalize());
    }

    private static String repositoryName(Path repositoryRoot) {
        Path fileName = repositoryRoot.getFileName();
        return fileName != null ? fileName.toString() : repositoryRoot.toString();
    }

    private static String relativePath(Path projectRoot, Path repositoryRoot) {
        if (projectRoot.equals(repositoryRoot)) return repositoryName(repositoryRoot);
        try {
            return projectRoot.relativize(repositoryRoot).toString().replace('\\', '/');
        } catch (IllegalArgumentException ignored) {
            return repositoryRoot.toString();
        }
    }
}
