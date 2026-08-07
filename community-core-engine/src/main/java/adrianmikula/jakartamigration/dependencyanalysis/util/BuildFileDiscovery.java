package adrianmikula.jakartamigration.dependencyanalysis.util;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * Shared build file discovery utility.
 * Consolidates build file discovery logic across scanners.
 */
@Slf4j
public final class BuildFileDiscovery {

    private static final Set<String> MAVEN_FILE_NAMES = Set.of("pom.xml");
    private static final Set<String> GRADLE_FILE_PREFIXES = Set.of("build.gradle");

    /** Directories that contain build output, caches, or non-project build files. */
    private static final Set<String> EXCLUDED_DIRECTORIES = Set.of(
            // Build outputs
            "target",   // Maven
            "build",    // Gradle
            "out",      // IntelliJ / Gradle
            "bin",      // general
            // Version control / IDE
            ".git", ".svn", ".hg",
            ".gradle",
            ".mvn",
            ".idea", ".vscode", ".eclipse",
            // Vendor / temp
            "vendor", "tmp", "temp",
            // Generated
            "site", "apidocs",
            "docker-build"
    );

    private BuildFileDiscovery() {
        // Utility class
    }

    /**
     * Discovers all build files (Maven and Gradle) in a directory tree.
     * Excludes build output directories, caches, and non-project build files.
     */
    public static List<Path> discoverBuildFiles(Path projectPath) {
        return discoverBuildFiles(projectPath, Integer.MAX_VALUE);
    }

    /**
     * Discovers all build files in a directory tree with max depth.
     * Excludes build output directories, caches, and non-project build files.
     * Uses file tree walking with directory pruning for efficiency.
     */
    public static List<Path> discoverBuildFiles(Path projectPath, int maxDepth) {
        List<Path> buildFiles = new ArrayList<>();

        try {
            Files.walkFileTree(projectPath, new SimpleFileVisitor<>() {
                private int currentDepth = 0;

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (currentDepth >= maxDepth) return FileVisitResult.SKIP_SUBTREE;
                    currentDepth++;
                    String dirName = dir.getFileName().toString();
                    if (isExcludedDirectoryName(dirName)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    currentDepth--;
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (BuildFileDiscovery.isBuildFile(file)) {
                        buildFiles.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.debug("Error walking directory tree for build files: {}", e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        Collections.sort(buildFiles);
        return buildFiles;
    }

    /**
     * Checks if a directory name should be excluded from build file scanning.
     */
    private static boolean isExcludedDirectoryName(String dirName) {
        return EXCLUDED_DIRECTORIES.contains(dirName);
    }

    /**
     * Discovers Maven POM files only.
     */
    public static List<Path> discoverMavenFiles(Path projectPath) {
        return discoverBuildFiles(projectPath).stream()
                .filter(BuildFileDiscovery::isMavenFile)
                .toList();
    }

    /**
     * Discovers Gradle build files only.
     */
    public static List<Path> discoverGradleFiles(Path projectPath) {
        return discoverBuildFiles(projectPath).stream()
                .filter(BuildFileDiscovery::isGradleFile)
                .toList();
    }

    /**
     * Checks if a path is a build file (Maven or Gradle).
     */
    public static boolean isBuildFile(Path path) {
        return isMavenFile(path) || isGradleFile(path);
    }

    /**
     * Checks if a path is a Maven POM file.
     */
    public static boolean isMavenFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        return MAVEN_FILE_NAMES.contains(fileName);
    }

    /**
     * Checks if a path is a Gradle build file.
     * Only matches actual build files (build.gradle, build.gradle.kts),
     * not arbitrary .gradle files like settings.gradle or test resources.
     */
    public static boolean isGradleFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        return GRADLE_FILE_PREFIXES.stream().anyMatch(fileName::startsWith);
    }

    /**
     * Finds common project root from a list of build files.
     */
    public static Optional<Path> findCommonProjectRoot(List<Path> buildFiles) {
        if (buildFiles.isEmpty()) {
            return Optional.empty();
        }

        Path commonRoot = buildFiles.get(0).getParent();
        if (commonRoot == null) {
            return Optional.empty();
        }

        for (Path file : buildFiles) {
            Path parent = file.getParent();
            if (parent == null) {
                return Optional.empty();
            }
            commonRoot = findCommonAncestor(commonRoot, parent);
            if (commonRoot == null) {
                return Optional.empty();
            }
        }

        return findRootDirectory(commonRoot);
    }

    /**
     * Detects if a project is multi-module.
     */
    public static boolean detectMultiModuleProject(Path projectPath) {
        // Check for Maven multi-module
        Path settingsGradle = projectPath.resolve("settings.gradle");
        Path settingsGradleKts = projectPath.resolve("settings.gradle.kts");
        if (Files.exists(settingsGradle) || Files.exists(settingsGradleKts)) {
            return true;
        }

        // Check for Maven multi-module via pom.xml with <modules>
        Path rootPom = projectPath.resolve("pom.xml");
        if (Files.exists(rootPom)) {
            try {
                String content = Files.readString(rootPom);
                return content.contains("<modules>");
            } catch (IOException e) {
                log.debug("Error reading pom.xml for multi-module check: {}", e.getClass().getSimpleName() + ": " + e.getMessage());
                return false;
            }
        }

        return false;
    }

    private static Path findCommonAncestor(Path path1, Path path2) {
        Path p1 = path1.normalize().toAbsolutePath();
        Path p2 = path2.normalize().toAbsolutePath();

        int maxCommon = Math.min(p1.getNameCount(), p2.getNameCount());
        int commonCount = 0;
        for (int i = 0; i < maxCommon; i++) {
            if (!p1.getName(i).equals(p2.getName(i))) {
                break;
            }
            commonCount++;
        }

        if (commonCount == 0) {
            return null;
        }

        return p1.getRoot().resolve(p1.subpath(0, commonCount));
    }

    private static Optional<Path> findRootDirectory(Path startPath) {
        Path current = startPath;
        if (!Files.isDirectory(current)) {
            current = current.getParent();
        }

        while (current != null) {
            // Check for Gradle settings files
            if (Files.exists(current.resolve("settings.gradle")) ||
                    Files.exists(current.resolve("settings.gradle.kts"))) {
                return Optional.of(current);
            }

            // Check for Maven parent POM with modules
            Path pomPath = current.resolve("pom.xml");
            if (Files.exists(pomPath)) {
                try {
                    String content = Files.readString(pomPath);
                    if (content.contains("<modules>")) {
                        return Optional.of(current);
                    }
                } catch (IOException e) {
                    log.debug("Error reading pom.xml while finding root directory: {}", e.getClass().getSimpleName() + ": " + e.getMessage());
                }
            }

            current = current.getParent();
        }

        return Optional.of(startPath);
    }
}
