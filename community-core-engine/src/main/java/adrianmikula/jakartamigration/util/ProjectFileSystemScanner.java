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

    // Java-focused default exclusions - no npm/Python folders
    private static final Set<String> DEFAULT_IGNORED_DIRECTORIES = Set.of(
            // Build outputs
            "target",   // Maven
            "build",    // Gradle
            "out",      // IntelliJ / Gradle
            "bin",      // general
            "dist",     // distribution archives
            
            // Version control
            ".git", ".svn", ".hg",
            
            // Gradle cache (hidden) - wrapper directory "gradle" is NOT excluded
            ".gradle",
            
            // Maven wrapper (hidden)
            ".mvn",
            
            // IDE
            ".idea", ".vscode", ".eclipse",
            "idea-sandbox", "system", "compile-server",
            
            // Vendor, temp
            "vendor", "tmp", "temp", "tmpfiles", "var",
            
            // Generated docs/reports (Java tools)
            "site", "apidocs",
            
            // Logs
            "logs", "log",
            
            // OS artifacts
            ".DS_Store", "Thumbs.db",
            
            // Docker build temp
            "docker-build"
    );

    private final Set<String> ignoredDirNames;
    private final Path projectRoot;
    private final GitIgnoreService gitIgnoreService;

    // Private constructor for default scanner
    private ProjectFileSystemScanner(Set<String> ignoredDirNames, GitIgnoreService gitIgnoreService) {
        this.ignoredDirNames = ignoredDirNames;
        this.projectRoot = null;
        this.gitIgnoreService = gitIgnoreService;
    }

    // Private constructor for gitignore-enabled scanner
    private ProjectFileSystemScanner(Path projectRoot, GitIgnoreService gitIgnoreService) {
        this.ignoredDirNames = DEFAULT_IGNORED_DIRECTORIES;
        this.projectRoot = projectRoot;
        this.gitIgnoreService = gitIgnoreService;
    }

    /**
     * Creates a ProjectFileSystemScanner with .gitignore support.
     * If a .gitignore file exists at the project root, it will be used
     * for exclusion patterns (in addition to VCS directories).
     *
     * @param projectRoot path to project root
     * @return configured scanner
     */
    public static ProjectFileSystemScanner withGitIgnore(Path projectRoot) {
        Objects.requireNonNull(projectRoot, "projectRoot cannot be null");
        Path gitignoreFile = projectRoot.resolve(".gitignore");
        GitIgnoreService gitIgnoreService = null;
        if (Files.exists(gitignoreFile)) {
            gitIgnoreService = new GitIgnoreService(projectRoot);
        }
        return new ProjectFileSystemScanner(projectRoot, gitIgnoreService);
    }

    /**
     * Creates a ProjectFileSystemScanner with default exclusions only.
     * No .gitignore processing.
     */
    public ProjectFileSystemScanner() {
        this(DEFAULT_IGNORED_DIRECTORIES, null);
    }

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
        
        // 1. Check simple directory name set (e.g., "target", "build")
        if (ignoredDirNames.contains(name)) {
            return true;
        }
        
        // 2. Check gitignore patterns (if available)
        if (gitIgnoreService != null) {
            Path relative = projectRoot.relativize(path);
            if (gitIgnoreService.isIgnored(relative)) {
                return true;
            }
        }
        
        // 3. Special absolute-path checks (always apply)
        if (fullPath.contains("idea-sandbox") || fullPath.contains("system/tmp")) {
            return true;
        }
        if ((fullPath.startsWith("/tmp/") || fullPath.startsWith("/var/tmp/")) && !fullPath.contains("junit")) {
            return true;
        }
        
        // 4. Hidden directory rule — apply only in default mode (no gitignore)
        // When gitignore is active, we rely on it for pattern matching
        if (gitIgnoreService == null) {
            if (name.startsWith(".") && !".github".equals(name) && !".gitlab".equals(name)) {
                return true;
            }
        }
        
        return false;
    }
}
