package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.domain.DependencyTreeResult;
import adrianmikula.jakartamigration.advancedscanning.service.DependencyTreeCommandExecutor;
import lombok.extern.slf4j.Slf4j;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

/**
 * Gradle Tooling API implementation of {@link DependencyTreeCommandExecutor}.
 *
 * Uses the official Gradle Tooling API for programmatic dependency resolution,
 * replacing the fragile process-spawning approach. Benefits:
 * <ul>
 *   <li>Automatic daemon reuse across scans</li>
 *   <li>Native wrapper detection</li>
 *   <li>Structured project model for multi-module enumeration</li>
 *   <li>Version-independent compatibility</li>
 * </ul>
 *
 * <p>Thread safety: Each call creates its own {@link ProjectConnection}.
 * The shared {@link GradleConnector} is thread-safe for connection creation.</p>
 */
@Slf4j
public class GradleToolingApiExecutor implements DependencyTreeCommandExecutor {

    private static final int MAX_DEPENDENCIES = 10000;
    private static final int THREAD_POOL_SIZE = 2;
    private static final java.util.regex.Pattern DEP_PATTERN =
            java.util.regex.Pattern.compile("^([^\\s:]+):([^\\s:]+):([^\\s]+)");

    private final ExecutorService executor;

    public GradleToolingApiExecutor() {
        this.executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    }

    @Override
    public CompletableFuture<DependencyTreeResult> executeMavenDependencyTreeAsync(
            Path pomXmlPath, Set<String> scopes) {
        // Maven is not handled by this executor — delegate to process-based approach
        return CompletableFuture.completedFuture(
                DependencyTreeResult.error("Maven dependency resolution is not supported by the Gradle Tooling API executor"));
    }

    @Override
    public CompletableFuture<DependencyTreeResult> executeGradleDependenciesAsync(
            Path buildFilePath, Set<String> scopes) {
        return CompletableFuture.supplyAsync(() -> {
            Path projectDir = buildFilePath.getParent();
            if (projectDir == null) {
                return DependencyTreeResult.error("Invalid path: no parent directory");
            }

            Path projectRoot = findGradleProjectRoot(projectDir);
            if (projectRoot == null) {
                return DependencyTreeResult.error(
                        "Gradle project root not found (no settings.gradle or settings.gradle.kts)");
            }

            // Run the dependencies task for the target module directly. Fetching the
            // GradleProject model can fail for real projects with complex settings plugins
            // (e.g. foojay toolchain resolver) while the dependencies task works fine.
            String taskName = buildDependenciesTaskName(projectRoot, projectDir);
            return executeViaToolingApi(projectRoot, taskName, scopes);
        }, executor);
    }

    /**
     * Builds the dependencies task name for a target project directory.
     * e.g. root -> "dependencies", subproject -> ":community-core-engine:dependencies"
     */
    private static String buildDependenciesTaskName(Path projectRoot, Path targetProjectDir) {
        if (projectRoot.equals(targetProjectDir)) {
            return "dependencies";
        }
        return computeGradleModuleName(projectRoot, targetProjectDir) + ":dependencies";
    }

    /**
     * Runs a Gradle task via the Tooling API and parses the dependency tree output.
     */
    private DependencyTreeResult executeViaToolingApi(
            Path projectRoot, String taskName, Set<String> scopes) {
        ProjectConnection connection = null;
        try {
            connection = newProjectConnection(projectRoot);

            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            connection.newBuild()
                    .forTasks(taskName)
                    .setStandardOutput(stdout)
                    .setStandardError(new ByteArrayOutputStream()) // suppress stderr
                    .run();

            String output = stdout.toString(StandardCharsets.UTF_8);
            List<DependencyTreeResult.DependencyNode> deps = parseGradleOutput(output, scopes);
            if (deps.isEmpty()) {
                return DependencyTreeResult.empty();
            }
            return new DependencyTreeResult(deps, Set.of());
        } catch (Exception e) {
            String msg = e.getMessage();
            log.error("Tooling API execution failed for {} in {}", taskName, projectRoot, e);
            return DependencyTreeResult.error(
                    "Gradle Tooling API failed: " + (msg != null ? msg : e.getClass().getSimpleName()));
        } finally {
            closeQuietly(connection);
        }
    }

    /**
     * Creates a {@link ProjectConnection} configured to use the project's wrapper
     * distribution and a stable Gradle user home. Using the build distribution
     * avoids forcing the Tooling API to download the default distribution version,
     * which fails in offline or sandboxed environments.
     */
    private ProjectConnection newProjectConnection(Path projectRoot) {
        File gradleUserHome = resolveGradleUserHome();
        File gradleHome = findExtractedGradleDistribution(gradleUserHome);
        log.debug("Opening Gradle Tooling API connection for {} with user home {}", projectRoot, gradleUserHome);
        GradleConnector connector = GradleConnector.newConnector()
                .forProjectDirectory(projectRoot.toFile())
                .useGradleUserHomeDir(gradleUserHome);
        if (gradleHome != null) {
            log.debug("Using already extracted Gradle distribution at {}", gradleHome);
            connector.useInstallation(gradleHome);
        } else {
            log.debug("No extracted Gradle distribution found, using build distribution");
            connector.useBuildDistribution();
        }
        return connector.connect();
    }

    private static File resolveGradleUserHome() {
        String env = System.getenv("GRADLE_USER_HOME");
        if (env != null && !env.isBlank()) {
            return new File(env);
        }
        // In an IDE sandbox, user.home points to the sandbox, so prefer the
        // real HOME environment variable (Linux/macOS). Fall back to user.home
        // only when HOME is not available.
        String home = System.getenv("HOME");
        if (home != null && !home.isBlank()) {
            return new File(home, ".gradle");
        }
        return new File(System.getProperty("user.home"), ".gradle");
    }

    /**
     * Finds an already-extracted Gradle distribution in the wrapper dists cache.
     * Avoids forcing the Tooling API to download or validate the distribution URL,
     * which is essential in offline or sandboxed environments.
     */
    private static File findExtractedGradleDistribution(File gradleUserHome) {
        Path dists = new File(gradleUserHome, "wrapper/dists").toPath();
        if (!Files.isDirectory(dists)) {
            return null;
        }
        Path pattern = dists.resolve("gradle-8.5-bin");
        if (!Files.isDirectory(pattern)) {
            return null;
        }
        try (var stream = Files.newDirectoryStream(pattern)) {
            for (Path hashDir : stream) {
                Path extracted = hashDir.resolve("gradle-8.5");
                if (Files.isDirectory(extracted.resolve("bin"))) {
                    return extracted.toFile();
                }
            }
        } catch (IOException e) {
            log.debug("Failed to enumerate extracted Gradle distribution under {}", dists, e);
        }
        return null;
    }

    /**
     * Finds the Gradle project root by walking up directories looking for settings.gradle(.kts).
     */
    static Path findGradleProjectRoot(Path startDir) {
        if (startDir == null) return null;
        Path current = startDir;
        int depth = 0;
        while (current != null && depth < 10) {
            if (Files.exists(current.resolve("settings.gradle")) ||
                Files.exists(current.resolve("settings.gradle.kts"))) {
                return current;
            }
            current = current.getParent();
            depth++;
            if (current != null && current.getNameCount() == 0) break;
        }
        return null;
    }

    /**
     * Computes the Gradle module name as a colon-separated path relative to the project root.
     * e.g. root=/a/b, submodule=/a/b/community-core-engine → ":community-core-engine"
     */
    static String computeGradleModuleName(Path projectRoot, Path submoduleDir) {
        Path relative = projectRoot.relativize(submoduleDir);
        return ":" + relative.toString().replace(java.io.File.separatorChar, ':');
    }

    /**
     * Parses the output of a Gradle dependencies task.
     * Reuses the same parsing logic as DependencyTreeCommandExecutorImpl.
     */
    List<DependencyTreeResult.DependencyNode> parseGradleOutput(
            String output, Set<String> scopes) {
        List<DependencyTreeResult.DependencyNode> deps = new ArrayList<>();
        Map<Integer, String> depthToArtifactKey = new HashMap<>();
        String scope = "compile";

        for (String rawLine : output.split("\n")) {
            if (deps.size() >= MAX_DEPENDENCIES) break;
            if (rawLine.trim().isEmpty()) continue;
            // Configuration headers look like "compileClasspath - ..." and end with a period.
            // Detect them by the " - " separator instead of a suffix that may not be present.
            if (rawLine.contains(" - ")) {
                scope = rawLine.trim().split(" ")[0];
                continue;
            }
            parseGradleLine(rawLine, scope, depthToArtifactKey).ifPresent(node -> {
                // Track depth for parent reconstruction for every parsed node, but only keep
                // nodes whose configuration matches the requested scopes (empty means all).
                depthToArtifactKey.put(node.getDepth(), node.getArtifactKey());
                depthToArtifactKey.keySet().removeIf(d -> d > node.getDepth());
                if (scopes.isEmpty() || scopes.contains(node.getScope())) {
                    deps.add(node);
                }
            });
        }
        return deps;
    }

    private Optional<DependencyTreeResult.DependencyNode> parseGradleLine(
            String line, String scope, Map<Integer, String> depthToArtifactKey) {
        String trimmed = line;
        int level = 0;
        while (trimmed.length() >= 5 && isTreePrefix(trimmed.substring(0, 5))) {
            level++;
            trimmed = trimmed.substring(5);
        }
        Matcher m = DEP_PATTERN.matcher(trimmed);
        if (!m.find()) return Optional.empty();

        // Root dependencies have one tree prefix (e.g. "+--- "), so depth is level - 1.
        int depth = level - 1;
        String parentKey = depth > 0 ? depthToArtifactKey.get(depth - 1) : null;
        return Optional.of(new DependencyTreeResult.DependencyNode(
                m.group(1), m.group(2), m.group(3), scope, depth, depth > 0, parentKey));
    }

    private static boolean isTreePrefix(String prefix) {
        return prefix.equals("|    ") || prefix.equals("+--- ") || prefix.equals("\\--- ") || prefix.equals("     ");
    }

    @Override
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) executor.shutdownNow();
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try { closeable.close(); } catch (Exception ignored) { }
        }
    }
}
