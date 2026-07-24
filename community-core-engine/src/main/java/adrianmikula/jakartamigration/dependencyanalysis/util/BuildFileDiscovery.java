package adrianmikula.jakartamigration.dependencyanalysis.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * Shared build file discovery utility.
 * Consolidates build file discovery logic across scanners.
 */
public final class BuildFileDiscovery {

    private static final Set<String> MAVEN_FILE_NAMES = Set.of("pom.xml");
    private static final Set<String> GRADLE_FILE_EXTENSIONS = Set.of(".gradle", ".gradle.kts");
    private static final Set<String> GRADLE_FILE_PREFIXES = Set.of("build.gradle");

    private BuildFileDiscovery() {
        // Utility class
    }

    /**
     * Discovers all build files (Maven and Gradle) in a directory tree.
     */
    public static List<Path> discoverBuildFiles(Path projectPath) {
        return discoverBuildFiles(projectPath, Integer.MAX_VALUE);
    }

    /**
     * Discovers all build files in a directory tree with max depth.
     */
    public static List<Path> discoverBuildFiles(Path projectPath, int maxDepth) {
        List<Path> buildFiles = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(projectPath, maxDepth)) {
            paths.filter(Files::isRegularFile)
                    .filter(BuildFileDiscovery::isBuildFile)
                    .sorted()
                    .forEach(buildFiles::add);
        } catch (IOException e) {
            // Return what we found so far
        }

        return buildFiles;
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
     */
    public static boolean isGradleFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        if (GRADLE_FILE_PREFIXES.stream().anyMatch(fileName::startsWith)) {
            return true;
        }
        return GRADLE_FILE_EXTENSIONS.stream().anyMatch(fileName::endsWith);
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
                    // Continue walking up
                }
            }

            current = current.getParent();
        }

        return Optional.of(startPath);
    }
}
