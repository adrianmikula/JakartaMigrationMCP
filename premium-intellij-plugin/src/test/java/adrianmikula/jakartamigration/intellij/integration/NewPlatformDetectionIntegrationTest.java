package adrianmikula.jakartamigration.intellij.integration;

import adrianmikula.jakartamigration.platforms.service.SimplifiedPlatformDetectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for new platform detection using test projects
 * Tests NetBeans, GlassFish, and Spring Boot detection with created projects
 * that simulate real-world scenarios
 */
public class NewPlatformDetectionIntegrationTest {
    
    private SimplifiedPlatformDetectionService detectionService;
    private Path tempDir;
    
    @BeforeEach
    void setUp() throws Exception {
        tempDir = Path.of("/tmp/integration-test");
        Files.createDirectories(tempDir);
        detectionService = new SimplifiedPlatformDetectionService();
    }
    
    @Test
    @DisplayName("Should detect NetBeans from test project")
    void testDetectNetBeansFromTestProject() throws Exception {
        // Given - Create a NetBeans test project
        Path projectPath = createNetBeansTestProject();
        
        // When
        List<String> detectedServers = detectionService.scanProject(projectPath);
        
        // Then
        assertThat(detectedServers).contains("netbeans");
        assertThat(detectedServers).hasSize(1);
    }
    
    @Test
    @DisplayName("Should detect Spring Boot from test project")
    void testDetectSpringBootFromTestProject() throws Exception {
        // Given - Create a Spring Boot test project
        Path projectPath = createSpringBootTestProject();
        
        // When
        List<String> detectedServers = detectionService.scanProject(projectPath);
        
        // Then
        assertThat(detectedServers).contains("springboot");
        assertThat(detectedServers).hasSizeGreaterThanOrEqualTo(1);
    }
    
    @Test
    @DisplayName("Should detect GlassFish from test project")
    void testDetectGlassFishFromTestProject() throws Exception {
        // Given - Create a GlassFish test project
        Path projectPath = createGlassFishTestProject();
        
        // When
        List<String> detectedServers = detectionService.scanProject(projectPath);
        
        // Then
        assertThat(detectedServers).contains("glassfish");
        assertThat(detectedServers).hasSizeGreaterThanOrEqualTo(1);
    }
    
    @Test
    @DisplayName("Should detect multiple platforms from mixed test project")
    void testDetectMultiplePlatformsFromTestProject() throws Exception {
        // Given - Create a mixed test project
        Path projectPath = createMixedTestProject();
        
        // When
        List<String> detectedServers = detectionService.scanProject(projectPath);
        
        // Then
        assertThat(detectedServers).hasSizeGreaterThanOrEqualTo(2);
        // Should detect multiple platforms
    }
    
    /**
     * Helper method to create a NetBeans test project
     */
    private Path createNetBeansTestProject() throws Exception {
        Path projectPath = tempDir.resolve("netbeans-test");
        Files.createDirectories(projectPath);
        
        String buildContent = """
            dependencies {
                implementation 'org.netbeans.modules:org-netbeans-api:12.6'
                implementation 'org.netbeans.swing:org-netbeans-swing:12.6'
            }
            """;
        
        Files.write(projectPath.resolve("build.gradle"), buildContent.getBytes());
        return projectPath;
    }
    
    /**
     * Helper method to create a Spring Boot test project
     */
    private Path createSpringBootTestProject() throws Exception {
        Path projectPath = tempDir.resolve("springboot-test");
        Files.createDirectories(projectPath);
        
        String buildContent = """
            dependencies {
                implementation 'org.springframework.boot:spring-boot-starter:3.1.5'
                implementation 'org.springframework.boot:spring-boot-starter-web:3.1.5'
            }
            """;
        
        Files.write(projectPath.resolve("build.gradle"), buildContent.getBytes());
        return projectPath;
    }
    
    /**
     * Helper method to create a GlassFish test project
     */
    private Path createGlassFishTestProject() throws Exception {
        Path projectPath = tempDir.resolve("glassfish-test");
        Files.createDirectories(projectPath);
        
        String buildContent = """
            dependencies {
                implementation 'org.glassfish.main:glassfish:7.0.0'
                implementation 'org.glassfish.jersey:jersey-server:3.1.0'
            }
            """;
        
        Files.write(projectPath.resolve("build.gradle"), buildContent.getBytes());
        return projectPath;
    }
    
    /**
     * Helper method to create a mixed test project
     */
    private Path createMixedTestProject() throws Exception {
        Path projectPath = tempDir.resolve("mixed-test");
        Files.createDirectories(projectPath);
        
        String buildContent = """
            dependencies {
                implementation 'org.apache.tomcat.embed:tomcat-embed-core:10.1.15'
                implementation 'org.springframework.boot:spring-boot-starter:3.1.5'
                implementation 'org.glassfish.main:glassfish:7.0.0'
            }
            """;
        
        Files.write(projectPath.resolve("build.gradle"), buildContent.getBytes());
        return projectPath;
    }
}
