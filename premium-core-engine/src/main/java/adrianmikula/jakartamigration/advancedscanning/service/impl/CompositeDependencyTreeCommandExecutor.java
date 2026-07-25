package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.domain.DependencyTreeResult;
import adrianmikula.jakartamigration.advancedscanning.service.DependencyTreeCommandExecutor;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Composite executor that routes Maven commands to the process-based executor
 * and Gradle commands to the Tooling API executor.
 *
 * <p>This bridges the migration period, allowing the existing
 * {@link TransitiveDependencyScannerImpl} to use the appropriate implementation
 * for each build tool without breaking the shared {@link DependencyTreeCommandExecutor}
 * interface.</p>
 */
@Slf4j
public class CompositeDependencyTreeCommandExecutor implements DependencyTreeCommandExecutor {

    private final DependencyTreeCommandExecutor mavenExecutor;
    private final DependencyTreeCommandExecutor gradleExecutor;

    public CompositeDependencyTreeCommandExecutor() {
        this.mavenExecutor = new DependencyTreeCommandExecutorImpl();
        this.gradleExecutor = new GradleToolingApiExecutor();
    }

    public CompositeDependencyTreeCommandExecutor(DependencyTreeCommandExecutor mavenExecutor,
                                                  DependencyTreeCommandExecutor gradleExecutor) {
        this.mavenExecutor = mavenExecutor;
        this.gradleExecutor = gradleExecutor;
    }

    @Override
    public CompletableFuture<DependencyTreeResult> executeMavenDependencyTreeAsync(Path pomXmlPath, Set<String> scopes) {
        return mavenExecutor.executeMavenDependencyTreeAsync(pomXmlPath, scopes);
    }

    @Override
    public CompletableFuture<DependencyTreeResult> executeGradleDependenciesAsync(Path buildFilePath, Set<String> scopes) {
        return gradleExecutor.executeGradleDependenciesAsync(buildFilePath, scopes);
    }

    @Override
    public void shutdown() {
        mavenExecutor.shutdown();
        gradleExecutor.shutdown();
    }
}
