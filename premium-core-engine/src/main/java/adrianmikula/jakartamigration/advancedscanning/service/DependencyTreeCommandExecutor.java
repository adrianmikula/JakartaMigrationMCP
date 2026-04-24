package adrianmikula.jakartamigration.advancedscanning.service;

import adrianmikula.jakartamigration.advancedscanning.domain.DependencyTreeResult;

import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Executes build tool commands asynchronously to retrieve the full dependency tree.
 */
public interface DependencyTreeCommandExecutor {

    /**
     * Default timeout for command execution in seconds.
     */
    int DEFAULT_TIMEOUT_SECONDS = 120;

    /**
     * Executes Maven dependency:tree command asynchronously and parses the JSON output.
     *
     * @param pomXmlPath Path to the pom.xml file
     * @param scopes Set of scopes to include (e.g., "compile", "test", "provided", "runtime")
     * @return CompletableFuture with dependency tree result
     */
    CompletableFuture<DependencyTreeResult> executeMavenDependencyTreeAsync(Path pomXmlPath, Set<String> scopes);

    /**
     * Executes Gradle dependencies command asynchronously and parses the output.
     *
     * @param buildFilePath Path to the build.gradle or build.gradle.kts file
     * @param scopes Set of scopes to include (e.g., "compileClasspath", "testCompileClasspath")
     * @return CompletableFuture with dependency tree result
     */
    CompletableFuture<DependencyTreeResult> executeGradleDependenciesAsync(Path buildFilePath, Set<String> scopes);

    /**
     * Shuts down the executor service and releases resources.
     */
    void shutdown();
}
