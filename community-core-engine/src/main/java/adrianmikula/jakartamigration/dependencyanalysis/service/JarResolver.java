package adrianmikula.jakartamigration.dependencyanalysis.service;

import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Resolves JAR files from local Maven and Gradle caches for binary compatibility checking.
 */
@Slf4j
public class JarResolver {
    
    /**
     * Resolves a JAR file from the Gradle module cache.
     * 
     * Gradle's module cache structure:
     * ~/.gradle/caches/modules-2/files-2.1/{groupId}/{artifactId}/{version}/{hash}/{artifactId}-{version}.jar
     * 
     * IMPORTANT: Gradle preserves dots in groupId (e.g., "org.example" stays as "org.example",
     * not converted to "org/example"). This is different from Maven's local repository structure.
     *
     * @param artifact The artifact to resolve
     * @return Optional path to the JAR file if found, empty otherwise
     */
    public Optional<Path> resolveFromGradle(Artifact artifact) {
        String userHome = System.getProperty("user.home");
        if (userHome == null) {
            log.debug("Cannot resolve Gradle cache: user.home is not set");
            return Optional.empty();
        }
        
        // Gradle cache path: ~/.gradle/caches/modules-2/files-2.1/{groupId}/{artifactId}/{version}/
        // IMPORTANT: groupId is used AS-IS with dots preserved, NOT converted to path separators
        Path gradleCacheBase = Paths.get(userHome, ".gradle", "caches", "modules-2", "files-2.1");
        Path groupIdPath = gradleCacheBase.resolve(artifact.groupId()); // Keep dots as-is
        Path artifactPath = groupIdPath.resolve(artifact.artifactId());
        Path versionPath = artifactPath.resolve(artifact.version());
        
        if (!Files.exists(versionPath)) {
            log.debug("Gradle cache version directory not found: {}", versionPath);
            return Optional.empty();
        }
        
        // Search for JAR file in the version directory
        // Gradle stores files with hash subdirectories: {version}/{hash}/{artifactId}-{version}.jar
        try (java.util.stream.Stream<Path> versionStream = Files.list(versionPath)) {
            return versionStream
                .filter(Files::isDirectory)
                .flatMap(hashDir -> {
                    try (java.util.stream.Stream<Path> hashStream = Files.list(hashDir)) {
                        return hashStream;
                    } catch (Exception e) {
                        log.debug("Error listing hash directory {}: {}", hashDir, e.getMessage());
                        return java.util.stream.Stream.empty();
                    }
                })
                .filter(path -> {
                    String fileName = path.getFileName().toString();
                    return fileName.endsWith(".jar") && 
                           fileName.startsWith(artifact.artifactId() + "-");
                })
                .findFirst();
        } catch (Exception e) {
            log.debug("Error searching Gradle cache for {}: {}", artifact.toCoordinate(), e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Resolves a JAR file from the Maven local repository.
     * 
     * Maven's local repository structure:
     * ~/.m2/repository/{groupId with dots converted to slashes}/{artifactId}/{version}/{artifactId}-{version}.jar
     * 
     * Example: org.example:my-lib:1.0.0 -> ~/.m2/repository/org/example/my-lib/1.0.0/my-lib-1.0.0.jar
     *
     * @param artifact The artifact to resolve
     * @return Optional path to the JAR file if found, empty otherwise
     */
    public Optional<Path> resolveFromMaven(Artifact artifact) {
        String userHome = System.getProperty("user.home");
        if (userHome == null) {
            log.debug("Cannot resolve Maven repository: user.home is not set");
            return Optional.empty();
        }
        
        // Maven local repository path: ~/.m2/repository/{groupId}/{artifactId}/{version}/
        // Maven converts dots in groupId to path separators
        Path mavenRepoBase = Paths.get(userHome, ".m2", "repository");
        String[] groupIdParts = artifact.groupId().split("\\.");
        Path groupIdPath = mavenRepoBase;
        for (String part : groupIdParts) {
            groupIdPath = groupIdPath.resolve(part);
        }
        Path artifactPath = groupIdPath.resolve(artifact.artifactId());
        Path versionPath = artifactPath.resolve(artifact.version());
        Path jarPath = versionPath.resolve(artifact.artifactId() + "-" + artifact.version() + ".jar");
        
        if (Files.exists(jarPath) && Files.isRegularFile(jarPath)) {
            return Optional.of(jarPath);
        }
        
        log.debug("Maven repository JAR not found: {}", jarPath);
        return Optional.empty();
    }
    
    /**
     * Resolves a JAR file by trying Gradle cache first, then Maven repository as fallback.
     *
     * @param artifact The artifact to resolve
     * @return Optional path to the JAR file if found in either cache, empty otherwise
     */
    public Optional<Path> resolve(Artifact artifact) {
        // Try Gradle first (common for Gradle projects)
        Optional<Path> gradleJar = resolveFromGradle(artifact);
        if (gradleJar.isPresent()) {
            log.debug("Resolved JAR from Gradle cache: {}", gradleJar.get());
            return gradleJar;
        }
        
        // Fallback to Maven repository
        Optional<Path> mavenJar = resolveFromMaven(artifact);
        if (mavenJar.isPresent()) {
            log.debug("Resolved JAR from Maven repository: {}", mavenJar.get());
            return mavenJar;
        }
        
        log.debug("Could not resolve JAR for artifact: {}", artifact.toCoordinate());
        return Optional.empty();
    }
}

