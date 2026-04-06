package adrianmikula.jakartamigration.platforms.service;

import adrianmikula.jakartamigration.platforms.model.EnhancedPlatformScanResult;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for enhanced platform detection service
 */
public class EnhancedPlatformDetectionServiceTest {
    
    private EnhancedPlatformDetectionService service;
    
    @BeforeEach
    void setUp() {
        service = new EnhancedPlatformDetectionService();
    }
    
    @Test
    void testScanProjectWithArtifacts_EmptyPath() {
        // Test with non-existent path
        Path nonExistentPath = Paths.get("/non/existent/path");
        EnhancedPlatformScanResult result = service.scanProjectWithArtifacts(nonExistentPath);
        
        assertNotNull(result);
        assertTrue(result.getDetectedPlatforms().isEmpty());
        assertEquals(0, result.getWarCount());
        assertEquals(0, result.getEarCount());
        assertEquals(0, result.getJarCount());
        assertEquals(0, result.getTotalDeploymentCount());
    }
    
    @Test
    void testEnhancedPlatformScanResult_Creation() {
        // Test EnhancedPlatformScanResult creation and methods
        List<String> platforms = List.of("Tomcat", "WildFly");
        Map<String, Integer> deploymentArtifacts = new HashMap<>();
        deploymentArtifacts.put("war", 3);
        deploymentArtifacts.put("ear", 1);
        deploymentArtifacts.put("jar", 5);
        
        Map<String, Integer> platformSpecific = new HashMap<>();
        platformSpecific.put("web.xml", 2);
        platformSpecific.put("application.xml", 1);
        
        EnhancedPlatformScanResult result = new EnhancedPlatformScanResult(platforms, deploymentArtifacts, platformSpecific);
        
        assertEquals(2, result.getDetectedPlatforms().size());
        assertEquals(3, result.getWarCount());
        assertEquals(1, result.getEarCount());
        assertEquals(5, result.getJarCount());
        assertEquals(9, result.getTotalDeploymentCount());
        assertEquals(2, result.getPlatformSpecificArtifacts().get("web.xml").intValue());
        assertEquals(1, result.getPlatformSpecificArtifacts().get("application.xml").intValue());
    }
    
    @Test
    void testEnhancedPlatformScanResult_EmptyArtifacts() {
        // Test with no artifacts
        List<String> platforms = List.of("Tomcat");
        Map<String, Integer> deploymentArtifacts = new HashMap<>();
        Map<String, Integer> platformSpecific = new HashMap<>();
        
        EnhancedPlatformScanResult result = new EnhancedPlatformScanResult(platforms, deploymentArtifacts, platformSpecific);
        
        assertEquals(0, result.getWarCount());
        assertEquals(0, result.getEarCount());
        assertEquals(0, result.getJarCount());
        assertEquals(0, result.getTotalDeploymentCount());
        assertTrue(result.getPlatformSpecificArtifacts().isEmpty());
    }
    
    @Test
    void testEnhancedPlatformScanResult_ToString() {
        // Test toString method
        List<String> platforms = List.of("Tomcat");
        Map<String, Integer> deploymentArtifacts = new HashMap<>();
        deploymentArtifacts.put("war", 2);
        Map<String, Integer> platformSpecific = new HashMap<>();
        
        EnhancedPlatformScanResult result = new EnhancedPlatformScanResult(platforms, deploymentArtifacts, platformSpecific);
        String resultString = result.toString();
        
        assertTrue(resultString.contains("EnhancedPlatformScanResult"));
        assertTrue(resultString.contains("platforms=[Tomcat]"));
        assertTrue(resultString.contains("artifacts={war=2}"));
    }
    
    @Test
    @DisplayName("Should not return duplicate platforms when detected by multiple sources")
    void testNoDuplicatePlatforms_WhenDetectedByMultipleSources(@TempDir Path tempDir) throws IOException {
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
        EnhancedPlatformScanResult result = service.scanProjectWithArtifacts(projectPath);
        List<String> detectedPlatforms = result.getDetectedPlatforms();
        
        // Then - should contain tomcat but only once (no duplicates)
        assertThat(detectedPlatforms).contains("tomcat");
        assertThat(detectedPlatforms).hasSize(1);
        
        // Verify no duplicates by checking distinct count equals size
        assertThat(detectedPlatforms.stream().distinct().count())
            .isEqualTo(detectedPlatforms.size())
            .as("All platforms should be unique with no duplicates");
    }
}
