package adrianmikula.jakartamigration.dependencyanalysis.service;

import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Service for resolving JAR file paths for Maven/Gradle artifacts.
 * 
 * Searches in:
 * - Maven local repository (~/.m2/repository)
 * - Gradle cache (~/.gradle/caches/modules-2/files-2.1)
 */
@Slf4j
@Service
public class JarResolver {
    
    private static final String MAVEN_REPO_PROPERTY = "maven.repo.local";
    private static final String USER_HOME_PROPERTY = "user.home";
    
    /**
     * Resolves the JAR file path for a given artifact.
     * 
     * @param artifact The artifact to resolve
     * @return Path to the JAR file, or null if not found
     */
    public Path resolveJar(Artifact artifact) {
        log.debug("Resolving JAR for artifact: {}", artifact.toCoordinate());
        
        // Try Maven repository first
        Path mavenPath = resolveFromMaven(artifact);
        if (mavenPath != null && Files.exists(mavenPath)) {
            log.debug("Found JAR in Maven repository: {}", mavenPath);
            return mavenPath;
        }
        
        // Try Gradle cache
        Path gradlePath = resolveFromGradle(artifact);
        if (gradlePath != null && Files.exists(gradlePath)) {
            log.debug("Found JAR in Gradle cache: {}", gradlePath);
            return gradlePath;
        }
        
        log.warn("Could not resolve JAR for artifact: {}", artifact.toCoordinate());
        return null;
    }
    
    /**
     * Resolves JAR from Maven local repository.
     * Format: ~/.m2/repository/{groupId}/{artifactId}/{version}/{artifactId}-{version}.jar
     */
    private Path resolveFromMaven(Artifact artifact) {
        String mavenRepoPath = System.getProperty(MAVEN_REPO_PROPERTY);
        if (mavenRepoPath == null) {
            String userHome = System.getProperty(USER_HOME_PROPERTY);
            mavenRepoPath = userHome + File.separator + ".m2" + File.separator + "repository";
        }
        
        // Convert groupId to path (replace dots with slashes)
        String groupIdPath = artifact.groupId().replace('.', File.separatorChar);
        
        // Build JAR path
        String jarFileName = String.format("%s-%s.jar", artifact.artifactId(), artifact.version());
        String jarPath = mavenRepoPath + File.separator + groupIdPath + File.separator + 
                        artifact.artifactId() + File.separator + artifact.version() + File.separator + jarFileName;
        
        return Paths.get(jarPath);
    }
    
    /**
     * Resolves JAR from Gradle cache.
     * Format: ~/.gradle/caches/modules-2/files-2.1/{groupId}/{artifactId}/{version}/hash/{file}
     * 
     * Note: Gradle cache structure is more complex due to hash-based storage.
     * This is a simplified implementation that may not find all JARs.
     */
    private Path resolveFromGradle(Artifact artifact) {
        String userHome = System.getProperty(USER_HOME_PROPERTY);
        String gradleCacheBase = userHome + File.separator + ".gradle" + File.separator + 
                                "caches" + File.separator + "modules-2" + File.separator + "files-2.1";
        
        // Convert groupId to path
        String groupIdPath = artifact.groupId().replace('.', File.separatorChar);
        String artifactBasePath = gradleCacheBase + File.separator + groupIdPath + File.separator + 
                                  artifact.artifactId() + File.separator + artifact.version();
        
        Path artifactPath = Paths.get(artifactBasePath);
        if (!Files.exists(artifactPath)) {
            return null;
        }
        
        // Gradle stores files in hash-based subdirectories
        // We need to search for the JAR file
        try {
            return Files.walk(artifactPath, 2)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".jar"))
                .filter(p -> p.getFileName().toString().contains(artifact.artifactId()))
                .findFirst()
                .orElse(null);
        } catch (Exception e) {
            log.debug("Error searching Gradle cache: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Checks if a JAR file exists for the given artifact.
     * 
     * @param artifact The artifact to check
     * @return true if JAR file exists, false otherwise
     */
    public boolean jarExists(Artifact artifact) {
        Path jarPath = resolveJar(artifact);
        return jarPath != null && Files.exists(jarPath);
    }
}

