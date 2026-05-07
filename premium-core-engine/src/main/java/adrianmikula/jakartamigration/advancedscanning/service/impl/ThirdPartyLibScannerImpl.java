package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.domain.ThirdPartyLibProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.ThirdPartyLibUsage;
import adrianmikula.jakartamigration.advancedscanning.service.ThirdPartyLibScanner;
import adrianmikula.jakartamigration.dependencyanalysis.config.CompatibilityConfigLoader;
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
    private final CompatibilityConfigLoader compatibilityConfigLoader;

    public ThirdPartyLibScannerImpl() {
        this(new CompatibilityConfigLoader());
    }

    public ThirdPartyLibScannerImpl(CompatibilityConfigLoader compatibilityConfigLoader) {
        this.compatibilityConfigLoader = compatibilityConfigLoader;
    }

    // Patterns for parsing Maven pom.xml
    private static final Pattern MAVEN_DEPENDENCY_PATTERN = Pattern.compile(
            "<dependency>\\s*<groupId>([^<]+)</groupId>\\s*<artifactId>([^<]+)</artifactId>\\s*<version>([^<]*)</version>");

    // Patterns for parsing Gradle build.gradle
    private static final Pattern GRADLE_DEPENDENCY_PATTERN = Pattern.compile(
            "(implementation|compile|api|compileOnly|runtimeOnly|testImplementation)\\s*['\"]([^:]+):([^:]+):([^'\"]+)['\"]");

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

    private List<ThirdPartyLibUsage> scanMavenPom(Path pomPath) {
        List<ThirdPartyLibUsage> usages = new ArrayList<>();

        try {
            String content = Files.readString(pomPath);
            Matcher matcher = MAVEN_DEPENDENCY_PATTERN.matcher(content);

            while (matcher.find()) {
                String groupId = matcher.group(1).trim();
                String artifactId = matcher.group(2).trim();
                String version = matcher.group(3).trim();

                // Classify using CompatibilityConfigLoader
                CompatibilityConfigLoader.ArtifactClassification classification = 
                        compatibilityConfigLoader.classifyArtifact(groupId, artifactId);

                // Only add usages for dependencies that need attention
                if (classification == CompatibilityConfigLoader.ArtifactClassification.JAKARTA_REQUIRED ||
                    classification == CompatibilityConfigLoader.ArtifactClassification.CONTEXT_DEPENDENT) {
                    
                    String issueType = classification == CompatibilityConfigLoader.ArtifactClassification.JAKARTA_REQUIRED 
                                      ? "outdated" : "partial-migration";
                    String suggestedReplacement = "Replace with Jakarta EE equivalent";
                    String libraryName = groupId + ":" + artifactId;

                    usages.add(new ThirdPartyLibUsage(
                            libraryName,
                            groupId,
                            artifactId,
                            version,
                            issueType,
                            suggestedReplacement));
                }
            }
        } catch (IOException e) {
            log.warn("Error reading pom.xml: {}", e.getMessage());
        }

        return usages;
    }

    private List<ThirdPartyLibUsage> scanGradleBuild(Path buildPath) {
        List<ThirdPartyLibUsage> usages = new ArrayList<>();

        try {
            String content = Files.readString(buildPath);
            Matcher matcher = GRADLE_DEPENDENCY_PATTERN.matcher(content);

            while (matcher.find()) {
                String groupId = matcher.group(2).trim();
                String artifactId = matcher.group(3).trim();
                String version = matcher.group(4).trim();

                // Classify using CompatibilityConfigLoader
                CompatibilityConfigLoader.ArtifactClassification classification = 
                        compatibilityConfigLoader.classifyArtifact(groupId, artifactId);

                // Only add usages for dependencies that need attention
                if (classification == CompatibilityConfigLoader.ArtifactClassification.JAKARTA_REQUIRED ||
                    classification == CompatibilityConfigLoader.ArtifactClassification.CONTEXT_DEPENDENT) {
                    
                    String issueType = classification == CompatibilityConfigLoader.ArtifactClassification.JAKARTA_REQUIRED 
                                      ? "outdated" : "partial-migration";
                    String suggestedReplacement = "Replace with Jakarta EE equivalent";
                    String libraryName = groupId + ":" + artifactId;

                    usages.add(new ThirdPartyLibUsage(
                            libraryName,
                            groupId,
                            artifactId,
                            version,
                            issueType,
                            suggestedReplacement));
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
