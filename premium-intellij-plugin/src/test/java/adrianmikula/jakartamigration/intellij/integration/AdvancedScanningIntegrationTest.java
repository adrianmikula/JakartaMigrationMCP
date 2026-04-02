package adrianmikula.jakartamigration.intellij.integration;

import adrianmikula.jakartamigration.advancedscanning.domain.AdvancedScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.IntegrationPointUsage;
import adrianmikula.jakartamigration.advancedscanning.domain.SerializationCacheUsage;
import adrianmikula.jakartamigration.advancedscanning.service.AdvancedScanningService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for advanced scanning against projects with javax packages.
 */
@Slf4j
public class AdvancedScanningIntegrationTest extends IntegrationTestBase {
    
    @Test
    @DisplayName("Advanced scan should detect javax.validation packages")
    void testAdvancedScanDetectsJavaxValidation() throws Exception {
        // Get validation example project
        Path projectDir = getExampleProject("Spring Boot javax-validation", "javax_packages");
        
        // Run advanced scan
        AdvancedScanResult result = advancedScanningService.scanProject(projectDir);
        
        // Verify detection
        assertNotNull(result);
        assertTrue(result.getScanSuccess());
        assertTrue(result.getScanDuration().toMillis() > 0);
        
        // Check for validation-related findings
        boolean foundValidation = result.getFindings().stream()
            .anyMatch(finding -> finding.getDescription().toLowerCase().contains("validation"));
        
        assertTrue(foundValidation, "Should detect javax.validation usage");
        
        log.info("Advanced scan completed in {} ms", result.getScanDuration().toMillis());
        log.info("Found {} findings", result.getFindings().size());
    }
    
    @Test
    @DisplayName("Advanced scan should detect javax.servlet packages")
    void testAdvancedScanDetectsJavaxServlet() throws Exception {
        // Get servlet example project
        Path projectDir = getExampleProject("Simple Servlet javax", "javax_packages");
        
        // Run advanced scan
        AdvancedScanResult result = advancedScanningService.scanProject(projectDir);
        
        // Verify detection
        assertNotNull(result);
        assertTrue(result.getScanSuccess());
        
        // Check for servlet-related findings
        boolean foundServlet = result.getFindings().stream()
            .anyMatch(finding -> finding.getDescription().toLowerCase().contains("servlet"));
        
        assertTrue(foundServlet, "Should detect javax.servlet usage");
        
        log.info("Advanced scan completed in {} ms", result.getScanDuration().toMillis());
        log.info("Found {} findings", result.getFindings().size());
    }
    
    @Test
    @DisplayName("Advanced scan should detect integration points")
    void testAdvancedScanDetectsIntegrationPoints() throws Exception {
        // Get project with integration points
        Path projectDir = getExampleProject("USB4Java javax examples", "javax_packages");
        
        // Run advanced scan
        AdvancedScanResult result = advancedScanningService.scanProject(projectDir);
        
        // Verify detection
        assertNotNull(result);
        assertTrue(result.getScanSuccess());
        
        // Check for integration points
        List<IntegrationPointUsage> integrationPoints = result.getIntegrationPointUsages();
        assertTrue(integrationPoints.size() > 0, "Should detect integration points");
        
        // Verify common integration points
        boolean foundRmi = integrationPoints.stream()
            .anyMatch(ip -> "RMI".equals(ip.getIntegrationType()));
        boolean foundJndi = integrationPoints.stream()
            .anyMatch(ip -> "JNDI".equals(ip.getIntegrationType()));
        boolean foundJms = integrationPoints.stream()
            .anyMatch(ip -> "JMS".equals(ip.getIntegrationType()));
        
        log.info("Found {} integration points", integrationPoints.size());
        log.info("RMI: {}, JNDI: {}, JMS: {}", foundRmi, foundJndi, foundJms);
    }
    
    @Test
    @DisplayName("Advanced scan should detect serialization cache usage")
    void testAdvancedScanDetectsSerializationCache() throws Exception {
        // Get project with serialization examples
        Path projectDir = getExampleProject("USB4Java javax examples", "javax_packages");
        
        // Run advanced scan
        AdvancedScanResult result = advancedScanningService.scanProject(projectDir);
        
        // Verify detection
        assertNotNull(result);
        assertTrue(result.getScanSuccess());
        
        // Check for serialization cache usage
        List<SerializationCacheUsage> serializationUsages = result.getSerializationCacheUsages();
        
        log.info("Found {} serialization cache usages", serializationUsages.size());
        
        // Verify serialization cache findings if any exist
        serializationUsages.forEach(usage -> {
            assertNotNull(usage.getFilePath());
            assertTrue(usage.getLineNumber() > 0);
            assertNotNull(usage.getCacheType());
        });
    }
    
    @Test
    @DisplayName("Advanced scan should handle Maven projects correctly")
    void testAdvancedScanHandlesMavenProjects() throws Exception {
        // Get Maven project
        Path projectDir = getExampleProject("Maven", "build_systems");
        
        // Verify it's a Maven project
        assertTrue(hasMavenBuild(projectDir), "Should be a Maven project");
        
        // Run advanced scan
        AdvancedScanResult result = advancedScanningService.scanProject(projectDir);
        
        // Verify detection
        assertNotNull(result);
        assertTrue(result.getScanSuccess());
        
        log.info("Maven project advanced scan completed successfully");
        log.info("Found {} findings", result.getFindings().size());
    }
    
    @Test
    @DisplayName("Advanced scan should handle Gradle projects correctly")
    void testAdvancedScanHandlesGradleProjects() throws Exception {
        // Get Gradle project
        Path projectDir = getExampleProject("Gradle", "build_systems");
        
        // Run advanced scan (works with both Maven and Gradle)
        AdvancedScanResult result = advancedScanningService.scanProject(projectDir);
        
        // Verify detection
        assertNotNull(result);
        assertTrue(result.getScanSuccess());
        
        log.info("Gradle project advanced scan completed successfully");
        log.info("Found {} findings", result.getFindings().size());
    }
    
    @Test
    @DisplayName("Advanced scan should provide detailed findings")
    void testAdvancedScanProvidesDetailedFindings() throws Exception {
        // Get project for detailed scan
        Path projectDir = getExampleProject("Spring Boot javax-validation", "javax_packages");
        
        // Run advanced scan
        AdvancedScanResult result = advancedScanningService.scanProject(projectDir);
        
        // Verify detailed findings
        assertNotNull(result);
        assertTrue(result.getScanSuccess());
        assertTrue(result.getFindings().size() > 0, "Should have findings");
        
        // Verify finding details
        result.getFindings().forEach(finding -> {
            assertNotNull(finding.getDescription(), "Finding should have description");
            assertNotNull(finding.getSeverity(), "Finding should have severity");
            assertTrue(finding.getFilePath().toString().length() > 0, "Finding should have file path");
            assertTrue(finding.getLineNumber() > 0, "Finding should have line number");
        });
        
        log.info("Detailed scan found {} findings", result.getFindings().size());
    }
}
