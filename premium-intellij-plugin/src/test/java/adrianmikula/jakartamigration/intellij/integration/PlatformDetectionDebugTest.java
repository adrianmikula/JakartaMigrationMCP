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
 * Debug test to understand what's happening with platform detection.
 */
public class PlatformDetectionDebugTest {
    
    @TempDir
    Path tempDir;
    
    private PlatformDetectionService platformDetectionService;
    
    @BeforeEach
    void setUp() {
        platformDetectionService = new PlatformDetectionService();
    }
    
    @Test
    @DisplayName("Debug platform detection patterns")
    void debugPlatformDetection() throws IOException {
        // Create a simple project with EJB packaging
        Path projectDir = tempDir.resolve("debug-project");
        Files.createDirectories(projectDir);
        
        // Create pom.xml with EJB packaging
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
                <packaging>ejb</packaging>
                
                <dependencies>
                    <dependency>
                        <groupId>javax.ejb</groupId>
                        <artifactId>ejb-api</artifactId>
                        <version>3.0</version>
                        <scope>provided</scope>
                    </dependency>
                </dependencies>
            </project>
            """;
        
        Files.write(projectDir.resolve("pom.xml"), pomContent.getBytes());
        
        // Verify the file exists and print its content
        assertTrue(Files.exists(projectDir.resolve("pom.xml")), "pom.xml should exist");
        
        String actualContent = Files.readString(projectDir.resolve("pom.xml"));
        System.out.println("=== DEBUG: pom.xml content ===");
        System.out.println(actualContent);
        System.out.println("=== END DEBUG ===");
        
        // Run platform detection
        PlatformScanResult result = platformDetectionService.scanProject(projectDir);
        
        // Verify result
        assertNotNull(result);
        assertNotNull(result.detectedPlatforms());
        
        // Print detailed results
        System.out.println("=== Platform Detection Results ===");
        System.out.println("Detected platforms: " + result.detectedPlatforms().size());
        if (result.detectedPlatforms().isEmpty()) {
            System.out.println("NO PLATFORMS DETECTED - This is the problem!");
        } else {
            result.detectedPlatforms().forEach(d -> 
                System.out.println("  - " + d.platformName() + " (type: " + d.platformType() + ", version: " + d.detectedVersion() + ")"));
        }
        System.out.println("Total risk score: " + result.totalRiskScore());
        System.out.println("Recommendations: " + result.recommendations().size());
        
        // At minimum should detect some platform
        assertTrue(result.detectedPlatforms().size() > 0, "Should detect at least one platform");
    }
}
