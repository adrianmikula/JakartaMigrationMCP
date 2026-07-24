package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.domain.ThirdPartyLibProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.ThirdPartyLibUsage;
import adrianmikula.jakartamigration.advancedscanning.service.ThirdPartyLibScanner;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Namespace;
import adrianmikula.jakartamigration.dependencyanalysis.service.NamespaceClassifier;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.util.MavenPomParser;
import adrianmikula.jakartamigration.dependencyanalysis.util.GradleBuildParser;
import adrianmikula.jakartamigration.scanning.RecipeBasedClassifier;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import adrianmikula.jakartamigration.util.ProjectFileSystemScanner;

/**
 * Scanner implementation for detecting third-party libraries that haven't been
 * migrated to Jakarta EE.
 * Analyzes Maven pom.xml and Gradle build.gradle files.
 */
@Slf4j
public class ThirdPartyLibScannerImpl implements ThirdPartyLibScanner {

    private final ProjectFileSystemScanner fileScanner = new ProjectFileSystemScanner();
    private final NamespaceClassifier namespaceClassifier;

    public ThirdPartyLibScannerImpl() {
        this(new RecipeBasedClassifier());
    }

    public ThirdPartyLibScannerImpl(NamespaceClassifier namespaceClassifier) {
        this.namespaceClassifier = namespaceClassifier;
    }

    @Override
    public ThirdPartyLibProjectScanResult scanProject(Path projectPath) {
        log.info("Starting third-party library scan for project: {}", projectPath);

        List<ThirdPartyLibUsage> problematicLibs = new ArrayList<>();
        StringBuilder buildFilesFound = new StringBuilder();

        try {
            // Find ALL build files recursively including Dockerfile
            List<Path> buildFiles = fileScanner.findFiles(projectPath, path -> {
                String name = path.getFileName().toString();
                return name.equals("pom.xml") || name.startsWith("build.gradle") || name.equals("Dockerfile") || name.equals("dockerfile");
            });

            for (Path buildFile : buildFiles) {
                String name = buildFile.getFileName().toString();
                if (buildFilesFound.length() > 0)
                    buildFilesFound.append(", ");
                buildFilesFound.append(name);

                if (name.equals("pom.xml")) {
                    problematicLibs.addAll(scanMavenPom(buildFile));
                } else if (name.equals("Dockerfile") || name.equals("dockerfile")) {
                    problematicLibs.addAll(scanDockerfile(buildFile));
                } else {
                    problematicLibs.addAll(scanGradleBuild(buildFile));
                }
            }

        } catch (Exception e) {
            log.error("Error scanning build files: {}", e.getMessage());
        }

        log.info("Third-party library scan complete. Found {} problematic dependencies", problematicLibs.size());

        return new ThirdPartyLibProjectScanResult(
                projectPath.toString(),
                problematicLibs,
                buildFilesFound.toString());
    }

    @Override
    public ThirdPartyLibProjectScanResult scanProject(List<Path> filesToScan) {
        if (filesToScan == null) {
            return new ThirdPartyLibProjectScanResult("", List.of(), "");
        }
        List<ThirdPartyLibUsage> problematicLibs = new ArrayList<>();
        StringBuilder buildFilesFound = new StringBuilder();
        for (Path buildFile : filesToScan) {
            String name = buildFile.getFileName().toString();
            if (buildFilesFound.length() > 0) buildFilesFound.append(", ");
            buildFilesFound.append(name);
            if (name.equals("pom.xml")) {
                problematicLibs.addAll(scanMavenPom(buildFile));
            } else if (name.equals("Dockerfile") || name.equals("dockerfile")) {
                problematicLibs.addAll(scanDockerfile(buildFile));
            } else {
                problematicLibs.addAll(scanGradleBuild(buildFile));
            }
        }
        // Derive project path from first file's parent (approximate)
        String projectPathStr = "";
        if (!filesToScan.isEmpty()) {
            Path first = filesToScan.get(0);
            Path parent = first.getParent();
            if (parent != null) projectPathStr = parent.toString();
        }
        return new ThirdPartyLibProjectScanResult(projectPathStr, problematicLibs, buildFilesFound.toString());
    }

    private List<ThirdPartyLibUsage> scanMavenPom(Path pomPath) {
        List<ThirdPartyLibUsage> usages = new ArrayList<>();

        try {
            List<Map<String, String>> deps = MavenPomParser.parseDependencies(pomPath);
            for (Map<String, String> dep : deps) {
                String groupId = dep.get("groupId");
                String artifactId = dep.get("artifactId");
                String version = dep.getOrDefault("version", "unknown");

                Namespace ns = classify(groupId, artifactId);
                if (ns == Namespace.JAVAX) {
                    usages.add(new ThirdPartyLibUsage(
                            groupId + ":" + artifactId,
                            groupId,
                            artifactId,
                            version,
                            "outdated",
                            "Replace with Jakarta EE equivalent"));
                } else if (ns == Namespace.MIXED) {
                    usages.add(new ThirdPartyLibUsage(
                            groupId + ":" + artifactId,
                            groupId,
                            artifactId,
                            version,
                            "partial-migration",
                            "Replace with Jakarta EE equivalent"));
                }
            }
        } catch (IOException e) {
            log.warn("Error reading pom.xml: {}", e.getMessage());
        }

        return usages;
    }

    private Namespace classify(String groupId, String artifactId) {
        if (namespaceClassifier == null) {
            return Namespace.UNKNOWN;
        }
        try {
            return namespaceClassifier.classify(new Artifact(groupId, artifactId, "unknown", "compile", false));
        } catch (Exception e) {
            log.warn("Namespace classification failed for {}:{}: {}", groupId, artifactId, e.getClass().getSimpleName() + ": " + e.getMessage());
            return Namespace.UNKNOWN;
        }
    }

    private List<ThirdPartyLibUsage> scanGradleBuild(Path buildPath) {
        List<ThirdPartyLibUsage> usages = new ArrayList<>();

        try {
            String content = Files.readString(buildPath);
            List<Map<String, String>> deps = GradleBuildParser.parseDependencies(content);
            for (Map<String, String> dep : deps) {
                String groupId = dep.get("groupId");
                String artifactId = dep.get("artifactId");
                String version = dep.getOrDefault("version", "unknown");

                Namespace ns = classify(groupId, artifactId);
                if (ns == Namespace.JAVAX) {
                    usages.add(new ThirdPartyLibUsage(
                            groupId + ":" + artifactId,
                            groupId,
                            artifactId,
                            version,
                            "outdated",
                            "Replace with Jakarta EE equivalent"));
                } else if (ns == Namespace.MIXED) {
                    usages.add(new ThirdPartyLibUsage(
                            groupId + ":" + artifactId,
                            groupId,
                            artifactId,
                            version,
                            "partial-migration",
                            "Replace with Jakarta EE equivalent"));
                }
            }
        } catch (IOException e) {
            log.warn("Error reading build.gradle: {}", e.getMessage());
        }

        return usages;
    }

    /**
     * Scans Dockerfile for Jakarta EE migration issues
     */
    private List<ThirdPartyLibUsage> scanDockerfile(Path dockerfilePath) {
        List<ThirdPartyLibUsage> usages = new ArrayList<>();
        
        try {
            String content = Files.readString(dockerfilePath);
            
            // Check for Java EE base images that need Jakarta EE migration
            if (content.contains("payara/") || content.contains("glassfish/") || 
                content.contains("wildfly/") || content.contains("tomcat:") || 
                content.contains("jetty:") || content.contains("openliberty/")) {
                
                usages.add(new ThirdPartyLibUsage(
                        "Java EE Application Server",
                        "javax.server",
                        "appserver",
                        "legacy",
                        "container-migration",
                        "Use Jakarta EE compatible server image"));
            }
            
            // Check for Maven builds that might need Jakarta dependencies
            if (content.contains("mvn") && content.contains("javax")) {
                usages.add(new ThirdPartyLibUsage(
                        "Maven Build with javax dependencies",
                        "javax.build",
                        "maven",
                        "detected",
                        "dependency-migration",
                        "Review pom.xml for Jakarta migration"));
            }
            
        } catch (IOException e) {
            log.warn("Error reading Dockerfile: {}", e.getMessage());
        }
        
        return usages;
    }
}
