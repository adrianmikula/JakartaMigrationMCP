package adrianmikula.jakartamigration.dependencyanalysis.service.impl;

import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphBuilder;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fallback dependency graph builder that crawls the directory for JAR files.
 * Useful for legacy projects without standard build systems (Maven/Gradle).
 */
@Slf4j
public class DirectoryCrawlerDependencyGraphBuilder implements DependencyGraphBuilder {

    // Common patterns for JAR filenames: name-version.jar or name.jar
    private static final Pattern JAR_VERSION_PATTERN = Pattern.compile("(.+)-(\\d+\\.[\\d\\.]+[\\w.-]*)\\.jar$");
    private static final Pattern SIMPLE_JAR_PATTERN = Pattern.compile("(.+)\\.jar$");

    @Override
    public DependencyGraph buildFromMaven(Path pomXmlPath) {
        throw new DependencyGraphException("Directory crawler does not support Maven-specific builds");
    }

    @Override
    public DependencyGraph buildFromGradle(Path buildFilePath) {
        throw new DependencyGraphException("Directory crawler does not support Gradle-specific builds");
    }

    @Override
    public DependencyGraph buildFromProject(Path projectPath) {
        log.info("Crawling directory for dependencies: {}", projectPath);

        Set<Artifact> artifacts = new HashSet<>();

        try {
            Files.walkFileTree(projectPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    // Skip hidden directories and obviously irrelevant ones
                    String name = dir.getFileName().toString();
                    if (name.startsWith(".") || name.equals("target") || name.equals("build") || name.equals("bin")) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith(".jar")) {
                        Artifact artifact = parseJarArtifact(file);
                        if (artifact != null) {
                            artifacts.add(artifact);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.error("Error crawling directory", e);
        }

        DependencyGraph graph = new DependencyGraph();
        for (Artifact artifact : artifacts) {
            graph.addNode(artifact);
        }

        log.info("Directory crawler found {} dependencies", artifacts.size());
        return graph;
    }

    private Artifact parseJarArtifact(Path jarPath) {
        String filename = jarPath.getFileName().toString();

        Matcher versionMatcher = JAR_VERSION_PATTERN.matcher(filename);
        if (versionMatcher.matches()) {
            String name = versionMatcher.group(1);
            String version = versionMatcher.group(2);
            return new Artifact("local-" + name, name, version, "compile", false);
        }

        Matcher simpleMatcher = SIMPLE_JAR_PATTERN.matcher(filename);
        if (simpleMatcher.matches()) {
            String name = simpleMatcher.group(1);
            return new Artifact("local-" + name, name, "unknown", "compile", false);
        }

        return null;
    }
}
