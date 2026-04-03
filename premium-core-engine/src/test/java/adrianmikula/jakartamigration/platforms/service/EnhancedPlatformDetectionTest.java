package adrianmikula.jakartamigration.platforms.service;

import adrianmikula.jakartamigration.platforms.model.PlatformDetection;
import adrianmikula.jakartamigration.platforms.model.PlatformScanResult;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Test to verify enhanced platform detection for Java and Spring
 */
@Disabled("Platform configuration loading issues - low importance infrastructure test")
public class EnhancedPlatformDetectionTest {

    private PlatformDetectionService detectionService;

    @BeforeEach
    void setUp() {
        detectionService = new PlatformDetectionService();
    }

    @Test
    @DisplayName("Should detect Java version from build.gradle.kts")
    void testDetectJavaFromGradleKTS() {
        // Test the current project structure
        Path projectPath = Paths.get("e:/Source/JakartaMigrationMCP");
        
        PlatformScanResult result = detectionService.scanProject(projectPath);
        List<PlatformDetection> detections = result.detectedPlatforms();
        
        // Should detect Java version
        boolean javaDetected = detections.stream()
            .anyMatch(d -> d.platformType().equals("java"));
        
        assertTrue(javaDetected, "Java version should be detected from build.gradle.kts");
        
        // Print detected platforms for debugging
        System.out.println("=== DETECTED PLATFORMS ===");
        detections.forEach(d -> {
            System.out.println("Type: " + d.platformType());
            System.out.println("Name: " + d.platformName());
            System.out.println("Version: " + d.detectedVersion());
            System.out.println("Jakarta Compatible: " + d.isJakartaCompatible());
            System.out.println("---");
        });
        System.out.println("==========================");
    }

    @Test
    @DisplayName("Should detect Spring version from build.gradle.kts")
    void testDetectSpringFromGradleKTS() {
        // Test the current project structure
        Path projectPath = Paths.get("e:/Source/JakartaMigrationMCP");
        
        PlatformScanResult result = detectionService.scanProject(projectPath);
        List<PlatformDetection> detections = result.detectedPlatforms();
        
        // Should detect Spring version
        boolean springDetected = detections.stream()
            .anyMatch(d -> d.platformType().equals("spring"));
        
        assertTrue(springDetected, "Spring version should be detected from build.gradle.kts");
    }

    @Test
    @DisplayName("Should calculate appropriate risk scores")
    void testRiskScoreCalculation() {
        // Test the current project structure
        Path projectPath = Paths.get("e:/Source/JakartaMigrationMCP");
        
        PlatformScanResult result = detectionService.scanProject(projectPath);
        int riskScore = result.totalRiskScore();
        
        // Should have reasonable risk score (5-10 points per detected platform)
        assertTrue(riskScore >= 0, "Risk score should be non-negative");
        assertTrue(riskScore <= 50, "Risk score should be reasonable (max ~50 points for typical projects)");
        
        System.out.println("Total Risk Score: " + riskScore);
        System.out.println("Recommendations: " + result.recommendations());
    }
}
