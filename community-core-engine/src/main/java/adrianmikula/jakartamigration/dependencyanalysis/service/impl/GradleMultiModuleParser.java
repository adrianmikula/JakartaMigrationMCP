package adrianmikula.jakartamigration.dependencyanalysis.service.impl;

import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Dependency;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Parses multi-module Gradle projects by reading settings.gradle(.kts)
 * to discover submodules, then parsing each submodule's build file.
 */
@Slf4j
public class GradleMultiModuleParser {

    private static final Pattern INCLUDE_PATTERN = Pattern.compile(
            "include\\s*\\(?\\s*['\"]([^'\"]+)['\"]\\s*\\)?"
    );

    private static final Pattern DEPENDENCY_PATTERN = Pattern.compile(
            "(?<![\\w-])(implementation|api|compile|runtime|testImplementation|testRuntime|compileOnly|runtimeOnly" +
            "|annotationProcessor|kapt|testCompileOnly|testRuntimeOnly" +
            "|implementation\\.platform|api\\.platform|compileClasspath)" +
            "\\s*[(]?\\s*['\"]([^'\"]+)['\"]\\s*[)]?"
    );

    private static final Pattern GROUP_ARTIFACT_VERSION = Pattern.compile(
            "([^:]+):([^:]+):([^'\"]+)"
    );

    private static final Pattern GROUP_ARTIFACT_ONLY = Pattern.compile(
            "([^:]+):([^:]+)"
    );

    private static final Pattern PROJECT_DEPENDENCY = Pattern.compile(
            "project\\s*\\(\\s*['\"]([^'\"]+)['\"]\\s*\\)"
    );

    private static final Pattern ROOT_PROJECT_NAME = Pattern.compile(
            "rootProject\\.name\\s*=\\s*['\"]([^'\"]+)['\"]"
    );

    private static final Pattern BASE_NAME = Pattern.compile(
            "(?:baseName|archivesBaseName)\\s*=\\s*['\"]([^'\"]+)['\"]"
    );

    /**
     * Parses a multi-module Gradle project and returns a merged dependency graph.
     *
     * @param projectRoot the root directory of the project
     * @return merged dependency graph containing all modules' dependencies
     */
    public DependencyGraph parseProject(Path projectRoot) {
        log.info("Parsing multi-module Gradle project at: {}", projectRoot);

        List<String> moduleNames = discoverModules(projectRoot);
        log.info("Discovered {} modules: {}", moduleNames.size(), moduleNames);

        DependencyGraph mergedGraph = new DependencyGraph();
        Set<String> seenArtifacts = new HashSet<>();

        // Parse root build file
        parseRootBuildFile(projectRoot, mergedGraph, seenArtifacts);

        // Parse each submodule
        for (String moduleName : moduleNames) {
            Path moduleDir = projectRoot.resolve(moduleName);
            if (!Files.isDirectory(moduleDir)) {
                log.warn("Module directory does not exist: {}", moduleDir);
                continue;
            }
            parseModuleBuildFile(moduleDir, moduleName, mergedGraph, seenArtifacts);
        }

        log.info("Multi-module parse complete: {} nodes, {} edges",
                mergedGraph.nodeCount(), mergedGraph.edgeCount());
        return mergedGraph;
    }

    /**
     * Discovers module names from settings.gradle or settings.gradle.kts.
     */
    public List<String> discoverModules(Path projectRoot) {
        List<String> modules = new ArrayList<>();

        Path settingsGradle = projectRoot.resolve("settings.gradle");
        Path settingsGradleKts = projectRoot.resolve("settings.gradle.kts");

        Path settingsFile = Files.exists(settingsGradleKts) ? settingsGradleKts :
                            Files.exists(settingsGradle) ? settingsGradle : null;

        if (settingsFile == null) {
            log.info("No settings.gradle(.kts) found, falling back to directory scan");
            return discoverModulesByDirectory(projectRoot);
        }

        try {
            String content = Files.readString(settingsFile);
            Matcher matcher = INCLUDE_PATTERN.matcher(content);
            while (matcher.find()) {
                String include = matcher.group(1);
                // Gradle includes use : separator for nested modules, convert to path
                String modulePath = include.replace(":", "/");
                modules.add(modulePath);
            }
        } catch (IOException e) {
            log.warn("Failed to read settings file: {}", settingsFile, e);
            return discoverModulesByDirectory(projectRoot);
        }

        if (modules.isEmpty()) {
            log.info("No includes found in settings file, falling back to directory scan");
            return discoverModulesByDirectory(projectRoot);
        }

        return modules;
    }

    /**
     * Fallback: discover modules by looking for build.gradle(.kts) in subdirectories.
     */
    private List<String> discoverModulesByDirectory(Path projectRoot) {
        List<String> modules = new ArrayList<>();
        try (Stream<Path> dirs = Files.list(projectRoot)) {
            dirs.filter(Files::isDirectory)
                .filter(dir -> {
                    String name = dir.getFileName().toString();
                    return !name.startsWith(".") && !name.equals("build") && !name.equals("out");
                })
                .forEach(dir -> {
                    Path buildFile = findBuildFile(dir);
                    if (buildFile != null) {
                        modules.add(dir.getFileName().toString());
                    }
                });
        } catch (IOException e) {
            log.warn("Failed to list directories in: {}", projectRoot, e);
        }
        return modules;
    }

    private void parseRootBuildFile(Path projectRoot, DependencyGraph graph, Set<String> seenArtifacts) {
        Path buildFile = findBuildFile(projectRoot);
        if (buildFile == null) {
            log.warn("No root build file found in: {}", projectRoot);
            return;
        }
        parseBuildFile(projectRoot, buildFile, "root", graph, seenArtifacts);
    }

    private void parseModuleBuildFile(Path moduleDir, String moduleName,
                                       DependencyGraph graph, Set<String> seenArtifacts) {
        Path buildFile = findBuildFile(moduleDir);
        if (buildFile == null) {
            log.debug("No build file found for module: {}", moduleName);
            return;
        }
        parseBuildFile(moduleDir, buildFile, moduleName, graph, seenArtifacts);
    }

    private void parseBuildFile(Path projectDir, Path buildFile, String projectName,
                                 DependencyGraph graph, Set<String> seenArtifacts) {
        log.debug("Parsing build file: {}", buildFile);
        try {
            String content = Files.readString(buildFile);

            // Extract project artifact
            Artifact projectArtifact = extractProjectArtifact(content, projectName);
            graph.addNode(projectArtifact);

            // Parse dependencies
            List<Artifact> dependencies = parseDependencies(content);
            for (Artifact dep : dependencies) {
                String key = dep.toIdentifier();
                if (seenArtifacts.add(key)) {
                    graph.addNode(dep);
                }
                graph.addEdge(new Dependency(projectArtifact, dep, dep.scope(), false));
            }

            log.debug("Parsed {}: {} dependencies", projectName, dependencies.size());
        } catch (IOException e) {
            log.warn("Failed to read build file: {}", buildFile, e);
        }
    }

    private Artifact extractProjectArtifact(String content, String projectName) {
        // Try baseName/archivesBaseName
        Matcher baseMatcher = BASE_NAME.matcher(content);
        if (baseMatcher.find()) {
            return new Artifact("unknown", baseMatcher.group(1), "unknown", "compile", false);
        }

        // Try rootProject.name
        Matcher rootMatcher = ROOT_PROJECT_NAME.matcher(content);
        if (rootMatcher.find()) {
            return new Artifact("unknown", rootMatcher.group(1), "unknown", "compile", false);
        }

        // Fall back to directory name
        return new Artifact("unknown", projectName, "unknown", "compile", false);
    }

    /**
     * Parses dependency declarations from a build file content.
     * Handles both versioned (group:artifact:version) and versionless (group:artifact) deps,
     * as well as project() dependencies (excluded from graph).
     */
    public List<Artifact> parseDependencies(String content) {
        List<Artifact> artifacts = new ArrayList<>();

        Matcher matcher = DEPENDENCY_PATTERN.matcher(content);
        while (matcher.find()) {
            String configName = matcher.group(1);
            String coordinates = matcher.group(2);

            // Skip project() dependencies — they're internal modules
            if (coordinates.startsWith("project(")) {
                continue;
            }

            // Skip BOM/platform imports — they don't add direct dependencies
            if (coordinates.startsWith("platform(") || coordinates.startsWith("enforcedPlatform(")) {
                continue;
            }

            String scope = mapConfigurationToScope(configName);

            // Try group:artifact:version first
            Matcher versionedMatcher = GROUP_ARTIFACT_VERSION.matcher(coordinates);
            if (versionedMatcher.matches()) {
                artifacts.add(new Artifact(
                        versionedMatcher.group(1),
                        versionedMatcher.group(2),
                        versionedMatcher.group(3),
                        scope,
                        false
                ));
                continue;
            }

            // Try group:artifact (versionless — managed by BOM/platform)
            Matcher versionlessMatcher = GROUP_ARTIFACT_ONLY.matcher(coordinates);
            if (versionlessMatcher.matches()) {
                artifacts.add(new Artifact(
                        versionlessMatcher.group(1),
                        versionlessMatcher.group(2),
                        "unknown",
                        scope,
                        false
                ));
            }
        }

        return artifacts;
    }

    private String mapConfigurationToScope(String configName) {
        return switch (configName) {
            case "testImplementation", "testRuntime", "testCompileOnly", "testRuntimeOnly" -> "test";
            case "runtimeOnly", "runtime" -> "runtime";
            case "compileOnly" -> "provided";
            case "annotationProcessor", "kapt" -> "provided";
            default -> "compile"; // implementation, api, compile, platform variants
        };
    }

    private Path findBuildFile(Path directory) {
        Path kts = directory.resolve("build.gradle.kts");
        if (Files.exists(kts)) return kts;

        Path groovy = directory.resolve("build.gradle");
        if (Files.exists(groovy)) return groovy;

        return null;
    }
}
