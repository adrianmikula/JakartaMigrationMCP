package adrianmikula.jakartamigration.dependencyanalysis.service.impl;

import adrianmikula.jakartamigration.advancedscanning.domain.DependencyTreeResult;
import adrianmikula.jakartamigration.advancedscanning.service.DependencyTreeCommandExecutor;
import adrianmikula.jakartamigration.advancedscanning.service.impl.DependencyTreeCommandExecutorImpl;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Dependency;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphBuilder;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphException;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Premium dependency graph builder that uses actual build tool commands
 * (Maven dependency:tree / Gradle dependencies) to resolve the real dependency
 * graph with resolved versions, then falls back to the community regex parser
 * if build tools are unavailable.
 *
 * <p>This provides accurate dependency resolution for modern build files that
 * use version catalogs, BOM platforms, and property-based versions, unlike
 * the community {@link MavenDependencyGraphBuilder} which relies on naive
 * regex parsing.
 */
@Slf4j
public class PremiumDependencyGraphBuilder implements DependencyGraphBuilder {

    private final DependencyTreeCommandExecutor commandExecutor;
    private final MavenDependencyGraphBuilder fallbackBuilder;

    public PremiumDependencyGraphBuilder() {
        this.commandExecutor = new DependencyTreeCommandExecutorImpl();
        this.fallbackBuilder = new MavenDependencyGraphBuilder();
    }

    @Override
    public DependencyGraph buildFromMaven(Path pomXmlPath) {
        try {
            log.info("Running Maven dependency:tree for: {}", pomXmlPath);
            var future = commandExecutor.executeMavenDependencyTreeAsync(
                    pomXmlPath, Set.of("compile", "provided", "runtime", "test"));
            var result = future.get(DependencyTreeCommandExecutor.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (result.isSuccess() && !result.getDependencies().isEmpty()) {
                log.info("Successfully resolved {} dependencies via Maven for: {}",
                        result.getDependencies().size(), pomXmlPath);
                return convertToDependencyGraph(result);
            }

            log.warn("Maven dependency tree returned no results for: {}. Error: {}",
                    pomXmlPath, result.getErrorMessage());
            throw new DependencyGraphException("Maven dependency tree failed: " + result.getErrorMessage());
        } catch (Exception e) {
            log.warn("Maven command-based resolution failed for: {}, falling back to regex parser. Error: {}",
                    pomXmlPath, e.getMessage());
            return fallbackBuilder.buildFromMaven(pomXmlPath);
        }
    }

    @Override
    public DependencyGraph buildFromGradle(Path buildFilePath) {
        try {
            log.info("Running Gradle dependencies for: {}", buildFilePath);
            var future = commandExecutor.executeGradleDependenciesAsync(
                    buildFilePath, Set.of("compileClasspath", "runtimeClasspath"));
            var result = future.get(DependencyTreeCommandExecutor.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (result.isSuccess() && !result.getDependencies().isEmpty()) {
                log.info("Successfully resolved {} dependencies via Gradle for: {}",
                        result.getDependencies().size(), buildFilePath);
                return convertToDependencyGraph(result);
            }

            log.warn("Gradle dependencies returned no results for: {}. Error: {}",
                    buildFilePath, result.getErrorMessage());
            throw new DependencyGraphException("Gradle dependencies failed: " + result.getErrorMessage());
        } catch (Exception e) {
            log.warn("Gradle command-based resolution failed for: {}, falling back to regex parser. Error: {}",
                    buildFilePath, e.getMessage());
            return fallbackBuilder.buildFromGradle(buildFilePath);
        }
    }

    @Override
    public DependencyGraph buildFromProject(Path projectRoot) {
        Path pomXml = projectRoot.resolve("pom.xml");
        if (Files.exists(pomXml)) {
            return buildFromMaven(pomXml);
        }

        Path buildGradle = projectRoot.resolve("build.gradle");
        Path buildGradleKts = projectRoot.resolve("build.gradle.kts");
        if (Files.exists(buildGradle)) {
            return buildFromGradle(buildGradle);
        }
        if (Files.exists(buildGradleKts)) {
            return buildFromGradle(buildGradleKts);
        }

        throw new DependencyGraphException("No Maven or Gradle build file found in project: " + projectRoot);
    }

    /**
     * Converts a {@link DependencyTreeResult} into the community {@link DependencyGraph} format.
     */
    private DependencyGraph convertToDependencyGraph(DependencyTreeResult treeResult) {
        DependencyGraph graph = new DependencyGraph();
        Map<String, Artifact> artifactMap = new HashMap<>();

        // First pass: add all nodes
        for (DependencyTreeResult.DependencyNode node : treeResult.getDependencies()) {
            Artifact artifact = new Artifact(
                    node.getGroupId(),
                    node.getArtifactId(),
                    node.getVersion(),
                    node.getScope() != null ? node.getScope() : "compile",
                    node.isTransitive()
            );
            graph.addNode(artifact);
            artifactMap.put(node.getArtifactKey(), artifact);
        }

        // Second pass: add edges using parent references
        for (DependencyTreeResult.DependencyNode node : treeResult.getDependencies()) {
            if (node.getParentArtifactKey() != null) {
                Artifact parent = artifactMap.get(node.getParentArtifactKey());
                Artifact child = artifactMap.get(node.getArtifactKey());
                if (parent != null && child != null) {
                    graph.addEdge(new Dependency(parent, child, node.getScope(), false));
                }
            }
        }

        return graph;
    }
}
