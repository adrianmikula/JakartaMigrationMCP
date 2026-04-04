package adrianmikula.jakartamigration.util;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.Predicate;

/**
 * Efficient file system scanner for Jakarta migration tool.
 * Centralizes ignore logic and uses walkFileTree to prune subtrees for
 * performance.
 */
@Slf4j
public class ProjectFileSystemScanner {

    private static final Set<String> IGNORED_DIRECTORIES = Set.of(
            "target", "build", ".git", "node_modules", ".gradle", ".mvn",
            ".idea", ".vscode", "out", "bin", "dist", "vendor", "tmp", "temp");

    /**
     * Finds files in a project path that match the given extensions.
     * 
     * @param projectPath The root path to search
     * @param extensions  List of file extensions (e.g., ".java", ".xml")
     * @return List of matching Paths
     */
    public List<Path> findFiles(Path projectPath, List<String> extensions) {
        return findFiles(projectPath, path -> {
            String fileName = path.getFileName().toString().toLowerCase();
            return extensions.stream().anyMatch(fileName::endsWith);
        });
    }

    /**
     * Finds files in a project path that match the given predicate.
     * 
     * @param projectPath The root path to search
     * @param filter      Filter for files
     * @return List of matching Paths
     */
    public List<Path> findFiles(Path projectPath, Predicate<Path> filter) {
        List<Path> matchingFiles = new ArrayList<>();

        try {
            Files.walkFileTree(projectPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (isIgnored(dir)) {
                        log.debug("Skipping ignored directory: {}", dir);
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (filter.test(file)) {
                        matchingFiles.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    log.warn("Failed to visit file: {}. Reason: {}", file, exc.getMessage());
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.error("Error walking file tree for project: {}", projectPath, e);
        }

        return matchingFiles;
    }

    /**
     * Centralized ignore logic for directories.
     */
    public boolean isIgnored(Path path) {
        String name = path.getFileName().toString();
        String fullPath = path.toString().toLowerCase();
        
        // Exact match for common ignore directories
        if (IGNORED_DIRECTORIES.contains(name)) {
            return true;
        }
        
        // Skip IDE sandbox and temporary directories
        if (fullPath.contains("idea-sandbox") || fullPath.contains("system/tmp") || 
            fullPath.contains("temp") || fullPath.contains("tmp")) {
            return true;
        }

        // Also check if it's a hidden directory (starts with dot)
        // Except for those we specifically care about (if any)
        return name.startsWith(".") && !".github".equals(name) && !".gitlab".equals(name);
    }
}
