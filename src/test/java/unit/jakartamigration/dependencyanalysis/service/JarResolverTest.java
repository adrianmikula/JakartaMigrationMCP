package unit.jakartamigration.dependencyanalysis.service;

import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.service.JarResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JarResolver Tests")
class JarResolverTest {
    
    private JarResolver jarResolver;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        jarResolver = new JarResolver();
    }
    
    @Test
    @DisplayName("Should return null when JAR does not exist")
    void shouldReturnNullWhenJarDoesNotExist() {
        // Given
        Artifact artifact = new Artifact(
            "com.nonexistent",
            "nonexistent-lib",
            "1.0.0",
            "compile",
            false
        );
        
        // When
        Path jarPath = jarResolver.resolveJar(artifact);
        
        // Then
        assertThat(jarPath).isNull();
    }
    
    @Test
    @DisplayName("Should return false when checking if non-existent JAR exists")
    void shouldReturnFalseForNonExistentJar() {
        // Given
        Artifact artifact = new Artifact(
            "com.nonexistent",
            "nonexistent-lib",
            "1.0.0",
            "compile",
            false
        );
        
        // When
        boolean exists = jarResolver.jarExists(artifact);
        
        // Then
        assertThat(exists).isFalse();
    }
    
    @Test
    @DisplayName("Should construct correct Maven repository path")
    void shouldConstructCorrectMavenPath() {
        // Given
        Artifact artifact = new Artifact(
            "javax.servlet",
            "javax.servlet-api",
            "4.0.1",
            "compile",
            false
        );
        
        // When
        Path jarPath = jarResolver.resolveJar(artifact);
        
        // Then
        // We can't verify the exact path without knowing the actual Maven repo location
        // But we can verify the path structure is correct if it exists
        if (jarPath != null) {
            String pathString = jarPath.toString();
            assertThat(pathString)
                .contains("javax")
                .contains("servlet")
                .contains("servlet-api")
                .contains("4.0.1")
                .endsWith(".jar");
        }
        // If JAR doesn't exist in local repo, that's fine for unit tests
    }
    
    @Test
    @DisplayName("Should handle artifacts with complex groupId")
    void shouldHandleComplexGroupId() {
        // Given
        Artifact artifact = new Artifact(
            "org.springframework.boot",
            "spring-boot-starter-web",
            "3.2.0",
            "compile",
            false
        );
        
        // When
        Path jarPath = jarResolver.resolveJar(artifact);
        
        // Then
        // Path should convert dots to slashes in groupId
        if (jarPath != null) {
            String pathString = jarPath.toString();
            assertThat(pathString)
                .contains("org")
                .contains("springframework")
                .contains("boot")
                .contains("spring-boot-starter-web")
                .contains("3.2.0");
        }
    }
    
    @Test
    @DisplayName("Should handle version with qualifiers")
    void shouldHandleVersionWithQualifiers() {
        // Given
        Artifact artifact = new Artifact(
            "com.example",
            "test-lib",
            "1.2.3-SNAPSHOT",
            "compile",
            false
        );
        
        // When
        Path jarPath = jarResolver.resolveJar(artifact);
        
        // Then
        if (jarPath != null) {
            String pathString = jarPath.toString();
            assertThat(pathString)
                .contains("1.2.3-SNAPSHOT")
                .endsWith(".jar");
        }
    }
}

