package adrianmikula.jakartamigration.platforms.service;

import adrianmikula.jakartamigration.platforms.service.SimplifiedPlatformDetectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SimplifiedPlatformDetectionService
 * Tests common artifact matching, variable resolution, and file-based search patterns
 */
public class SimplifiedPlatformDetectionServiceTest {
    
    private SimplifiedPlatformDetectionService detectionService;
    
    @TempDir
    private Path tempDir;
    
    @BeforeEach
    void setUp() {
        detectionService = new SimplifiedPlatformDetectionService();
    }
    
    @Test
    @DisplayName("Should detect Tomcat via common artifacts in Maven project")
    void testDetectTomcatViaCommonArtifacts_MavenProject() throws IOException {
        // Given
        Path projectPath = createMavenProjectWithTomcatArtifacts();
        
        // When
        List<String> detectedServers = detectionService.scanProject(projectPath);
        
        // Then
        assertThat(detectedServers).contains("tomcat");
        assertThat(detectedServers).hasSize(1);
    }
    
    @Test
    @DisplayName("Should detect WildFly via common artifacts in Gradle project")
    void testDetectWildFlyViaCommonArtifacts_GradleProject() throws IOException {
        // Given
        Path projectPath = createGradleProjectWithWildFlyArtifacts();
        
        // When
        List<String> detectedServers = detectionService.scanProject(projectPath);
        
        // Then
        assertThat(detectedServers).contains("wildfly");
        assertThat(detectedServers).hasSize(1);
    }
    
    @Test
    @DisplayName("Should detect Spring Boot via common artifacts")
    void testDetectSpringBootViaCommonArtifacts() throws IOException {
        // Given
        Path projectPath = createSpringBootProjectWithArtifacts();
        
        // When
        List<String> detectedServers = detectionService.scanProject(projectPath);
        
        // Then
        assertThat(detectedServers).contains("springboot");
        assertThat(detectedServers).hasSize(2); // Detects springboot and spring due to overlapping artifacts
    }
    
    @Test
    @DisplayName("Should detect NetBeans via common artifacts")
    void testDetectNetBeansViaCommonArtifacts() throws IOException {
        // Given
        Path projectPath = createProjectWithNetBeansArtifacts();
        
        // When
        List<String> detectedServers = detectionService.scanProject(projectPath);
        
        // Then
        assertThat(detectedServers).contains("netbeans");
        assertThat(detectedServers).hasSize(1);
    }
    
    @Test
    @DisplayName("Should detect GlassFish via common artifacts")
    void testDetectGlassFishViaCommonArtifacts() throws IOException {
        // Given
        Path projectPath = createProjectWithGlassFishArtifacts();
        
        // When
        List<String> detectedServers = detectionService.scanProject(projectPath);
        
        // Then
        assertThat(detectedServers).contains("glassfish");
        assertThat(detectedServers).hasSize(1); // Only glassfish detected with specific group:name artifacts
    }
    
    @Test
    @DisplayName("Should detect multiple platforms in mixed project")
    void testDetectMultiplePlatforms_MixedProject() throws IOException {
        // Given
        Path projectPath = createMixedProjectWithMultiplePlatforms();
        
        // When
        List<String> detectedServers = detectionService.scanProject(projectPath);
        
        // Then
        assertThat(detectedServers).contains("tomcat");
        assertThat(detectedServers).contains("wildfly");
        assertThat(detectedServers).contains("springboot");
        assertThat(detectedServers).hasSizeGreaterThanOrEqualTo(3); // At least 3 platforms detected
    }
    
    @Test
    @DisplayName("Should not return duplicate platforms when detected by multiple sources")
    void testNoDuplicatePlatforms_WhenDetectedByMultipleSources() throws IOException {
        // Given - a project with both pom.xml and build.gradle containing the same platform
        Path projectPath = tempDir.resolve("multi-build-project");
        Files.createDirectories(projectPath);
        
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>tomcat-test</artifactId>
                <version>1.0.0</version>
                <dependencies>
                    <dependency>
                        <groupId>org.apache.tomcat.embed</groupId>
                        <artifactId>tomcat-embed-core</artifactId>
                        <version>10.1.15</version>
                    </dependency>
                </dependencies>
            </project>
            """;
        Files.write(projectPath.resolve("pom.xml"), pomContent.getBytes());
        
        String gradleContent = """
            dependencies {
                implementation 'org.apache.tomcat.embed:tomcat-embed-core:10.1.15'
            }
            """;
        Files.write(projectPath.resolve("build.gradle"), gradleContent.getBytes());
        
        // When
        List<String> detectedServers = detectionService.scanProject(projectPath);
        
        // Then - should contain tomcat but only once (no duplicates)
        assertThat(detectedServers).contains("tomcat");
        assertThat(detectedServers).hasSize(1);
        
        // Verify no duplicates by checking distinct count equals size
        assertThat(detectedServers.stream().distinct().count())
            .isEqualTo(detectedServers.size())
            .as("All platforms should be unique with no duplicates");
    }
    
    @Test
    @DisplayName("Should return empty result for project with no platforms")
    void testScanProject_NoPlatformsFound_ReturnsEmptyResult() throws IOException {
        // Given
        Path emptyProjectPath = tempDir.resolve("empty-project");
        Files.createDirectories(emptyProjectPath);
        
        // When
        List<String> detectedServers = detectionService.scanProject(emptyProjectPath);
        
        // Then
        assertThat(detectedServers).isEmpty();
    }
    
    // Helper methods for creating test projects
    private Path createMavenProjectWithTomcatArtifacts() throws IOException {
        Path projectPath = tempDir.resolve("tomcat-maven");
        Files.createDirectories(projectPath);
        
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>tomcat-test</artifactId>
                <version>1.0.0</version>
                <dependencies>
                    <dependency>
                        <groupId>org.apache.tomcat.embed</groupId>
                        <artifactId>tomcat-embed-core</artifactId>
                        <version>10.1.15</version>
                    </dependency>
                </dependencies>
            </project>
            """;
        Files.write(projectPath.resolve("pom.xml"), pomContent.getBytes());
        return projectPath;
    }
    
    private Path createGradleProjectWithWildFlyArtifacts() throws IOException {
        Path projectPath = tempDir.resolve("wildfly-gradle");
        Files.createDirectories(projectPath);
        
        String gradleContent = """
            dependencies {
                implementation 'org.wildfly:wildfly-ee:27.0.1.Final'
            }
            """;
        Files.write(projectPath.resolve("build.gradle"), gradleContent.getBytes());
        return projectPath;
    }
    
    private Path createSpringBootProjectWithArtifacts() throws IOException {
        Path projectPath = tempDir.resolve("spring-boot");
        Files.createDirectories(projectPath);
        
        String gradleContent = """
            dependencies {
                implementation 'org.springframework.boot:spring-boot-starter:3.1.5'
                implementation 'org.springframework.boot:spring-boot-starter-web:3.1.5'
            }
            """;
        Files.write(projectPath.resolve("build.gradle"), gradleContent.getBytes());
        return projectPath;
    }
    
    private Path createProjectWithNetBeansArtifacts() throws IOException {
        Path projectPath = tempDir.resolve("netbeans");
        Files.createDirectories(projectPath);
        
        String gradleContent = """
            dependencies {
                implementation 'org.netbeans.modules:org-netbeans-modules-java-platform:12.6'
                implementation 'org.netbeans.api:org-netbeans-api-java:12.6'
            }
            """;
        Files.write(projectPath.resolve("build.gradle"), gradleContent.getBytes());
        return projectPath;
    }
    
    private Path createProjectWithGlassFishArtifacts() throws IOException {
        Path projectPath = tempDir.resolve("glassfish");
        Files.createDirectories(projectPath);
        
        String gradleContent = """
            dependencies {
                implementation 'org.glassfish.main:glassfish-main:7.0.0'
            }
            """;
        Files.write(projectPath.resolve("build.gradle"), gradleContent.getBytes());
        return projectPath;
    }
    
    private Path createMixedProjectWithMultiplePlatforms() throws IOException {
        Path projectPath = tempDir.resolve("mixed-project");
        Files.createDirectories(projectPath);
        
        String gradleContent = """
            dependencies {
                implementation 'org.apache.tomcat.embed:tomcat-embed-core:10.1.15'
                implementation 'org.wildfly:wildfly-ee:27.0.1.Final'
                implementation 'org.springframework.boot:spring-boot-starter:3.1.5'
            }
            """;
        Files.write(projectPath.resolve("build.gradle"), gradleContent.getBytes());
        return projectPath;
    }
}
