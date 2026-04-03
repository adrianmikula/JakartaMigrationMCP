package adrianmikula.jakartamigration.intellij.integration;

import adrianmikula.jakartamigration.platforms.service.PlatformDetectionService;
import adrianmikula.jakartamigration.platforms.model.PlatformScanResult;
import adrianmikula.jakartamigration.platforms.model.PlatformDetection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for platform detection with real application server projects from examples.yaml.
 * Uses ExampleProjectManager to test actual GitHub repositories.
 */
public class ApplicationServerDetectionTest extends IntegrationTestBase {
    
    private PlatformDetectionService platformDetectionService;
    
    @BeforeEach
    void setUp() throws IOException {
        super.setUp(); // Initialize projectManager from base class
        platformDetectionService = new PlatformDetectionService();
    }
    
    @Test
    @DisplayName("Platform detection should detect WildFly from real GitHub project")
    void testDetectWildFlyFromRealProject() throws IOException {
        // Get WildFly project from GitHub examples
        Path projectDir = projectManager.getExampleProject("WildFly", "application_servers");
        
        // Run platform detection
        PlatformScanResult result = platformDetectionService.scanProject(projectDir);
        
        // Verify result
        assertNotNull(result);
        assertNotNull(result.detectedPlatforms());
        
        // Check for WildFly detection
        boolean wildflyDetected = result.detectedPlatforms().stream()
            .anyMatch(detection -> detection.platformName().toLowerCase().contains("wildfly"));
        
        // Print detailed results for debugging
        System.out.println("=== WildFly Real Project Detection Results ===");
        System.out.println("Detected platforms: " + result.detectedPlatforms().size());
        result.detectedPlatforms().forEach(d -> 
            System.out.println("  - " + d.platformName() + " (type: " + d.platformType() + ", version: " + d.detectedVersion() + ")"));
        System.out.println("Total risk score: " + result.totalRiskScore());
        System.out.println("Recommendations: " + result.recommendations().size());
        System.out.println("WildFly detected: " + wildflyDetected);
        
        // At minimum should detect some platform
        assertTrue(result.detectedPlatforms().size() > 0, "Should detect at least one platform from real WildFly project");
        
        // Verify WildFly was detected (this is the key test)
        assertTrue(wildflyDetected, "Should detect WildFly platform from real GitHub project");
    }
    
    @Test
    @DisplayName("Platform detection should detect Tomcat from real GitHub project")
    void testDetectTomcatFromRealProject() throws IOException {
        // Get Tomcat project from GitHub examples
        Path projectDir = projectManager.getExampleProject("Apache Tomcat", "application_servers");
        
        // Run platform detection
        PlatformScanResult result = platformDetectionService.scanProject(projectDir);
        
        // Verify result
        assertNotNull(result);
        assertNotNull(result.detectedPlatforms());
        
        // Check for Tomcat detection
        boolean tomcatDetected = result.detectedPlatforms().stream()
            .anyMatch(detection -> detection.platformName().toLowerCase().contains("tomcat"));
        
        // Print detailed results for debugging
        System.out.println("=== Tomcat Real Project Detection Results ===");
        System.out.println("Detected platforms: " + result.detectedPlatforms().size());
        result.detectedPlatforms().forEach(d -> 
            System.out.println("  - " + d.platformName() + " (type: " + d.platformType() + ", version: " + d.detectedVersion() + ")"));
        System.out.println("Total risk score: " + result.totalRiskScore());
        System.out.println("Recommendations: " + result.recommendations().size());
        System.out.println("Tomcat detected: " + tomcatDetected);
        
        // At minimum should detect some platform
        assertTrue(result.detectedPlatforms().size() > 0, "Should detect at least one platform from real Tomcat project");
        
        // Verify Tomcat was detected
        assertTrue(tomcatDetected, "Should detect Tomcat platform from real GitHub project");
    }
    
    @Test
    @DisplayName("Platform detection should detect Spring Boot from real GitHub project")
    void testDetectSpringBootFromRealProject() throws IOException {
        // Get Spring Boot project from GitHub examples
        Path projectDir = projectManager.getExampleProject("Spring Boot", "application_servers");
        
        // Run platform detection
        PlatformScanResult result = platformDetectionService.scanProject(projectDir);
        
        // Verify result
        assertNotNull(result);
        assertNotNull(result.detectedPlatforms());
        
        // Check for Spring detection
        boolean springDetected = result.detectedPlatforms().stream()
            .anyMatch(detection -> detection.platformName().toLowerCase().contains("spring"));
        
        // Print detailed results for debugging
        System.out.println("=== Spring Boot Real Project Detection Results ===");
        System.out.println("Detected platforms: " + result.detectedPlatforms().size());
        result.detectedPlatforms().forEach(d -> 
            System.out.println("  - " + d.platformName() + " (type: " + d.platformType() + ", version: " + d.detectedVersion() + ")"));
        System.out.println("Total risk score: " + result.totalRiskScore());
        System.out.println("Recommendations: " + result.recommendations().size());
        System.out.println("Spring detected: " + springDetected);
        
        // At minimum should detect some platform
        assertTrue(result.detectedPlatforms().size() > 0, "Should detect at least one platform from real Spring Boot project");
        
        // Verify Spring was detected
        assertTrue(springDetected, "Should detect Spring platform from real GitHub project");
    }
    
    @Test
    @DisplayName("Platform detection should detect Payara from real GitHub project")
    void testDetectPayaraFromRealProject() throws IOException {
        // Get Payara project from GitHub examples
        Path projectDir = projectManager.getExampleProject("Payara", "application_servers");
        
        // Run platform detection
        PlatformScanResult result = platformDetectionService.scanProject(projectDir);
        
        // Verify result
        assertNotNull(result);
        assertNotNull(result.detectedPlatforms());
        
        // Check for Payara detection
        boolean payaraDetected = result.detectedPlatforms().stream()
            .anyMatch(detection -> detection.platformName().toLowerCase().contains("payara"));
        
        // Print detailed results for debugging
        System.out.println("=== Payara Real Project Detection Results ===");
        System.out.println("Detected platforms: " + result.detectedPlatforms().size());
        result.detectedPlatforms().forEach(d -> 
            System.out.println("  - " + d.platformName() + " (type: " + d.platformType() + ", version: " + d.detectedVersion() + ")"));
        System.out.println("Total risk score: " + result.totalRiskScore());
        System.out.println("Recommendations: " + result.recommendations().size());
        System.out.println("Payara detected: " + payaraDetected);
        
        // At minimum should detect some platform
        assertTrue(result.detectedPlatforms().size() > 0, "Should detect at least one platform from real Payara project");
        
        // Verify Payara was detected
        assertTrue(payaraDetected, "Should detect Payara platform from real GitHub project");
    }
    
    @Test
    @DisplayName("Platform detection should detect Jetty from real GitHub project")
    void testDetectJettyFromRealProject() throws IOException {
        // Get Jetty project from GitHub examples
        Path projectDir = projectManager.getExampleProject("Jetty", "application_servers");
        
        // Run platform detection
        PlatformScanResult result = platformDetectionService.scanProject(projectDir);
        
        // Verify result
        assertNotNull(result);
        assertNotNull(result.detectedPlatforms());
        
        // Check for Jetty detection
        boolean jettyDetected = result.detectedPlatforms().stream()
            .anyMatch(detection -> detection.platformName().toLowerCase().contains("jetty"));
        
        // Print detailed results for debugging
        System.out.println("=== Jetty Real Project Detection Results ===");
        System.out.println("Detected platforms: " + result.detectedPlatforms().size());
        result.detectedPlatforms().forEach(d -> 
            System.out.println("  - " + d.platformName() + " (type: " + d.platformType() + ", version: " + d.detectedVersion() + ")"));
        System.out.println("Total risk score: " + result.totalRiskScore());
        System.out.println("Recommendations: " + result.recommendations().size());
        System.out.println("Jetty detected: " + jettyDetected);
        
        // At minimum should detect some platform
        assertTrue(result.detectedPlatforms().size() > 0, "Should detect at least one platform from real Jetty project");
        
        // Verify Jetty was detected
        assertTrue(jettyDetected, "Should detect Jetty platform from real GitHub project");
    }
}
