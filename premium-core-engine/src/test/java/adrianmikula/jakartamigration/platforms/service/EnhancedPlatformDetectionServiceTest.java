package adrianmikula.jakartamigration.platforms.service;

import adrianmikula.jakartamigration.platforms.model.EnhancedPlatformScanResult;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

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
}
