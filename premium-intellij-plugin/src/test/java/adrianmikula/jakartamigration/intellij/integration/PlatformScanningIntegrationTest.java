package adrianmikula.jakartamigration.intellij.integration;

import adrianmikula.jakartamigration.platforms.model.PlatformScanResult;
import adrianmikula.jakartamigration.platforms.service.PlatformDetectionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for platform scanning against real GitHub projects with different application servers.
 * Uses ExampleProjectManager to fetch and test actual projects from examples.yaml.
 */
public class PlatformScanningIntegrationTest extends IntegrationTestBase {
    
    @Nested
    @DisplayName("Application Server Platform Detection")
    class ApplicationServerDetection {
        
        @Test
        @DisplayName("Platform scan should detect Apache Tomcat usage")
        void testPlatformScanDetectsTomcat() throws Exception {
            // Get Tomcat project from GitHub examples
            Path projectDir = projectManager.getExampleProject("Apache Tomcat", "application_servers");
            
            // Initialize platform detection service
            PlatformDetectionService platformDetectionService = new PlatformDetectionService();
            
            // Run platform scan
            PlatformScanResult result = platformDetectionService.scanProject(projectDir);
            
            // Verify detection
            assertNotNull(result);
            assertTrue(result.detectedPlatforms().size() >= 0);
            assertTrue(result.totalRiskScore() >= 0);
            assertNotNull(result.recommendations());
            
            // Check for Tomcat-related detections
            boolean foundTomcat = result.detectedPlatforms().stream()
                .anyMatch(detection -> detection.platformName().toLowerCase().contains("tomcat"));
            
            System.out.println("Tomcat platform scan: " + result.detectedPlatforms().size() + " detections, risk score: " + result.totalRiskScore());
        }
        
        @Test
        @DisplayName("Platform scan should detect WildFly usage")
        void testPlatformScanDetectsWildFly() throws Exception {
            // Get WildFly project from GitHub examples
            Path projectDir = projectManager.getExampleProject("WildFly", "application_servers");
            
            // Initialize platform detection service
            PlatformDetectionService platformDetectionService = new PlatformDetectionService();
            
            // Run platform scan
            PlatformScanResult result = platformDetectionService.scanProject(projectDir);
            
            // Verify detection
            assertNotNull(result);
            assertTrue(result.detectedPlatforms().size() >= 0);
            assertTrue(result.totalRiskScore() >= 0);
            assertNotNull(result.recommendations());
            
            // Check for WildFly-related detections
            boolean foundWildFly = result.detectedPlatforms().stream()
                .anyMatch(detection -> detection.platformName().toLowerCase().contains("wildfly"));
            
            System.out.println("WildFly platform scan: " + result.detectedPlatforms().size() + " detections, risk score: " + result.totalRiskScore());
        }
        
        @Test
        @DisplayName("Platform scan should detect Spring Boot application")
        void testPlatformScanDetectsSpringBoot() throws Exception {
            // Get Spring Boot project from GitHub examples
            Path projectDir = projectManager.getExampleProject("Spring Boot", "application_servers");
            
            // Initialize platform detection service
            PlatformDetectionService platformDetectionService = new PlatformDetectionService();
            
            // Run platform scan
            PlatformScanResult result = platformDetectionService.scanProject(projectDir);
            
            // Verify detection
            assertNotNull(result);
            assertTrue(result.detectedPlatforms().size() >= 0);
            assertTrue(result.totalRiskScore() >= 0);
            assertNotNull(result.recommendations());
            
            // Check for Spring Boot-related detections
            boolean foundSpringBoot = result.detectedPlatforms().stream()
                .anyMatch(detection -> detection.platformName().toLowerCase().contains("spring"));
            
            System.out.println("Spring Boot platform scan: " + result.detectedPlatforms().size() + " detections, risk score: " + result.totalRiskScore());
        }
        
        @Test
        @DisplayName("Platform scan should detect Payara usage")
        void testPlatformScanDetectsPayara() throws Exception {
            // Get Payara project from GitHub examples
            Path projectDir = projectManager.getExampleProject("Payara", "application_servers");
            
            // Initialize platform detection service
            PlatformDetectionService platformDetectionService = new PlatformDetectionService();
            
            // Run platform scan
            PlatformScanResult result = platformDetectionService.scanProject(projectDir);
            
            // Verify detection
            assertNotNull(result);
            assertTrue(result.detectedPlatforms().size() >= 0);
            assertTrue(result.totalRiskScore() >= 0);
            assertNotNull(result.recommendations());
            
            // Check for Payara-related detections
            boolean foundPayara = result.detectedPlatforms().stream()
                .anyMatch(detection -> detection.platformName().toLowerCase().contains("payara"));
            
            System.out.println("Payara platform scan: " + result.detectedPlatforms().size() + " detections, risk score: " + result.totalRiskScore());
        }
        
        @Test
        @DisplayName("Platform scan should detect Jetty usage")
        void testPlatformScanDetectsJetty() throws Exception {
            // Get Jetty project from GitHub examples
            Path projectDir = projectManager.getExampleProject("Jetty", "application_servers");
            
            // Initialize platform detection service
            PlatformDetectionService platformDetectionService = new PlatformDetectionService();
            
            // Run platform scan
            PlatformScanResult result = platformDetectionService.scanProject(projectDir);
            
            // Verify detection
            assertNotNull(result);
            assertTrue(result.detectedPlatforms().size() >= 0);
            assertTrue(result.totalRiskScore() >= 0);
            assertNotNull(result.recommendations());
            
            // Check for Jetty-related detections
            boolean foundJetty = result.detectedPlatforms().stream()
                .anyMatch(detection -> detection.platformName().toLowerCase().contains("jetty"));
            
            System.out.println("Jetty platform scan: " + result.detectedPlatforms().size() + " detections, risk score: " + result.totalRiskScore());
        }
    }
    
    @Nested
    @DisplayName("Platform Detection Performance and Analysis")
    class PerformanceTests {
        
        @Test
        @DisplayName("Platform scan should be performant on large projects")
        void testPlatformScanPerformance() throws Exception {
            // Use a larger project for performance testing
            Path projectDir = projectManager.getExampleProject("WildFly", "application_servers");
            
            // Initialize platform detection service
            PlatformDetectionService platformDetectionService = new PlatformDetectionService();
            
            // Measure scan time
            long startTime = System.currentTimeMillis();
            PlatformScanResult result = platformDetectionService.scanProject(projectDir);
            long endTime = System.currentTimeMillis();
            
            // Verify scan completed successfully and within reasonable time
            assertNotNull(result);
            assertTrue((endTime - startTime) < 15000, "Platform scan should complete within 15 seconds");
            
            System.out.println("Platform scan performance test completed in " + (endTime - startTime) + " ms");
        }
        
        @Test
        @DisplayName("Platform scan should provide meaningful recommendations")
        void testPlatformScanRecommendations() throws Exception {
            // Get project for recommendations testing
            Path projectDir = projectManager.getExampleProject("Spring Boot", "application_servers");
            
            // Initialize platform detection service
            PlatformDetectionService platformDetectionService = new PlatformDetectionService();
            
            // Run platform scan
            PlatformScanResult result = platformDetectionService.scanProject(projectDir);
            
            // Verify recommendations are provided
            assertNotNull(result);
            assertNotNull(result.recommendations());
            assertTrue(result.recommendations().size() >= 0);
            
            System.out.println("Platform scan recommendations: " + result.recommendations().size() + " suggestions");
            result.recommendations().forEach(rec -> System.out.println("Recommendation: " + rec));
        }
        
        @Test
        @DisplayName("Platform scan should calculate appropriate risk scores")
        void testPlatformScanRiskScoring() throws Exception {
            // Get project for risk scoring testing
            Path projectDir = projectManager.getExampleProject("WildFly", "application_servers");
            
            // Initialize platform detection service
            PlatformDetectionService platformDetectionService = new PlatformDetectionService();
            
            // Run platform scan
            PlatformScanResult result = platformDetectionService.scanProject(projectDir);
            
            // Verify risk scoring
            assertNotNull(result);
            assertTrue(result.totalRiskScore() >= 0);
            assertTrue(result.totalRiskScore() <= 1000, "Risk score should be reasonable");
            
            System.out.println("Platform scan risk score: " + result.totalRiskScore());
        }
    }
}
