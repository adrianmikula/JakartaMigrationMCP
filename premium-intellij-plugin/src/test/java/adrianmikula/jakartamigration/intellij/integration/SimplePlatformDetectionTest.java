package adrianmikula.jakartamigration.intellij.integration;

import adrianmikula.jakartamigration.platforms.service.PlatformDetectionService;
import adrianmikula.jakartamigration.platforms.model.PlatformScanResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple integration test for platform detection without external dependencies.
 */
public class SimplePlatformDetectionTest {
    
    @TempDir
    Path tempDir;
    
    private PlatformDetectionService platformDetectionService;
    
    @BeforeEach
    void setUp() {
        platformDetectionService = new PlatformDetectionService();
    }
    
    @Test
    @DisplayName("Platform detection should handle empty project gracefully")
    void testPlatformDetectionEmptyProject() throws IOException {
        // Create a minimal project
        Path projectDir = tempDir.resolve("empty-project");
        Files.createDirectories(projectDir);
        
        // Run platform detection
        PlatformScanResult result = platformDetectionService.scanProject(projectDir);
        
        // Verify result
        assertNotNull(result);
        assertNotNull(result.detectedPlatforms());
        assertEquals(0, result.detectedPlatforms().size());
        assertEquals(0, result.totalRiskScore());
        assertNotNull(result.recommendations());
    }
    
    @Test
    @DisplayName("Platform detection should handle project with pom.xml")
    void testPlatformDetectionWithPom() throws IOException {
        // Create a project with pom.xml
        Path projectDir = tempDir.resolve("maven-project");
        Files.createDirectories(projectDir);
        
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
                     http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>test-project</artifactId>
                <version>1.0.0</version>
            </project>
            """;
        
        Files.write(projectDir.resolve("pom.xml"), pomContent.getBytes());
        
        // Run platform detection
        PlatformScanResult result = platformDetectionService.scanProject(projectDir);
        
        // Verify result
        assertNotNull(result);
        assertNotNull(result.detectedPlatforms());
        assertNotNull(result.recommendations());
        System.out.println("Platform detection completed successfully");
        System.out.println("Detected platforms: " + result.detectedPlatforms().size());
        System.out.println("Total risk score: " + result.totalRiskScore());
        System.out.println("Recommendations: " + result.recommendations().size());
    }
}
