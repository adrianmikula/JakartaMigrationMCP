package adrianmikula.jakartamigration.intellij.service;

import adrianmikula.jakartamigration.intellij.model.JakartaArtifactCoordinates;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Integration tests for Maven Central lookup functionality.
 * Tests real Maven Central API calls to verify fuzzy matching works correctly.
 * 
 * Note: These tests require internet connectivity and may be flaky due to external API availability.
 * They are designed to be resilient and provide meaningful failure information.
 */
public class MavenCentralServiceIntegrationTest {
    
    private static final Logger LOGGER = Logger.getLogger(MavenCentralServiceIntegrationTest.class.getName());
    
    private MavenCentralService mavenCentralService;
    
    @BeforeEach
    void setUp() {
        mavenCentralService = new MavenCentralService();
    }
    
    @Test
    void testRealMavenCentralLookup_JavaxServlet() throws Exception {
        // Test the working case - javax.servlet-api should find jakarta.servlet-api
        LOGGER.info("Testing javax.servlet-api lookup...");
        
        CompletableFuture<List<JakartaArtifactCoordinates>> future = 
            mavenCentralService.findJakartaEquivalents("javax.servlet", "javax.servlet-api");
        
        // Add timeout to prevent hanging
        List<JakartaArtifactCoordinates> results = future.get(30, TimeUnit.SECONDS);
        
        LOGGER.info("Found " + results.size() + " results for javax.servlet-api");
        for (JakartaArtifactCoordinates result : results) {
            LOGGER.info("  - " + result.groupId() + ":" + result.artifactId() + ":" + result.version() + " (" + result.status() + ")");
        }
        
        // Should find at least one Jakarta equivalent
        assertThat(results).isNotEmpty();
        
        // Should find jakarta.servlet:jakarta.servlet-api
        boolean foundJakartaServlet = results.stream()
            .anyMatch(coord -> coord.groupId().equals("jakarta.servlet") && 
                             coord.artifactId().equals("jakarta.servlet-api"));
        assertThat(foundJakartaServlet).isTrue();
    }
    
    @Test
    void testRealMavenCentralLookup_JavaxPersistence() throws Exception {
        // Test javax.persistence-api should find jakarta.persistence-api
        LOGGER.info("Testing javax.persistence-api lookup...");
        
        CompletableFuture<List<JakartaArtifactCoordinates>> future = 
            mavenCentralService.findJakartaEquivalents("javax.persistence", "javax.persistence-api");
        
        List<JakartaArtifactCoordinates> results = future.get(30, TimeUnit.SECONDS);
        
        LOGGER.info("Found " + results.size() + " results for javax.persistence-api");
        for (JakartaArtifactCoordinates result : results) {
            LOGGER.info("  - " + result.groupId() + ":" + result.artifactId() + ":" + result.version() + " (" + result.status() + ")");
        }
        
        // Should find at least one Jakarta equivalent
        assertThat(results).isNotEmpty();
        
        // Should find jakarta.persistence:jakarta.persistence-api
        boolean foundJakartaPersistence = results.stream()
            .anyMatch(coord -> coord.groupId().equals("jakarta.persistence") && 
                             coord.artifactId().equals("jakarta.persistence-api"));
        assertThat(foundJakartaPersistence).isTrue();
    }
    
    @Test
    void testRealMavenCentralLookup_JavaxValidation() throws Exception {
        // Test javax.validation-api should find jakarta.validation-api
        LOGGER.info("Testing javax.validation-api lookup...");
        
        CompletableFuture<List<JakartaArtifactCoordinates>> future = 
            mavenCentralService.findJakartaEquivalents("javax.validation", "javax.validation-api");
        
        List<JakartaArtifactCoordinates> results = future.get(30, TimeUnit.SECONDS);
        
        LOGGER.info("Found " + results.size() + " results for javax.validation-api");
        for (JakartaArtifactCoordinates result : results) {
            LOGGER.info("  - " + result.groupId() + ":" + result.artifactId() + ":" + result.version() + " (" + result.status() + ")");
        }
        
        // Should find at least one Jakarta equivalent
        assertThat(results).isNotEmpty();
        
        // Should find jakarta.validation:jakarta.validation-api
        boolean foundJakartaValidation = results.stream()
            .anyMatch(coord -> coord.groupId().equals("jakarta.validation") && 
                             coord.artifactId().equals("jakarta.validation-api"));
        assertThat(foundJakartaValidation).isTrue();
    }
    
    @Test
    void testRealMavenCentralLookup_JavaxInject() throws Exception {
        // Test javax.inject should find jakarta.inject
        LOGGER.info("Testing javax.inject lookup...");
        
        CompletableFuture<List<JakartaArtifactCoordinates>> future = 
            mavenCentralService.findJakartaEquivalents("javax.inject", "javax.inject");
        
        List<JakartaArtifactCoordinates> results = future.get(30, TimeUnit.SECONDS);
        
        LOGGER.info("Found " + results.size() + " results for javax.inject");
        for (JakartaArtifactCoordinates result : results) {
            LOGGER.info("  - " + result.groupId() + ":" + result.artifactId() + ":" + result.version() + " (" + result.status() + ")");
        }
        
        // Should find at least one Jakarta equivalent
        assertThat(results).isNotEmpty();
        
        // Should find jakarta.inject:jakarta.inject
        boolean foundJakartaInject = results.stream()
            .anyMatch(coord -> coord.groupId().equals("jakarta.inject") && 
                             coord.artifactId().equals("jakarta.inject"));
        assertThat(foundJakartaInject).isTrue();
    }
    
    @Test
    void testRealMavenCentralLookup_JavaxAnnotation() throws Exception {
        // Test javax.annotation-api should find jakarta.annotation-api
        LOGGER.info("Testing javax.annotation-api lookup...");
        
        CompletableFuture<List<JakartaArtifactCoordinates>> future = 
            mavenCentralService.findJakartaEquivalents("javax.annotation", "javax.annotation-api");
        
        List<JakartaArtifactCoordinates> results = future.get(30, TimeUnit.SECONDS);
        
        LOGGER.info("Found " + results.size() + " results for javax.annotation-api");
        for (JakartaArtifactCoordinates result : results) {
            LOGGER.info("  - " + result.groupId() + ":" + result.artifactId() + ":" + result.version() + " (" + result.status() + ")");
        }
        
        // Should find at least one Jakarta equivalent
        assertThat(results).isNotEmpty();
        
        // Should find jakarta.annotation:jakarta.annotation-api
        boolean foundJakartaAnnotation = results.stream()
            .anyMatch(coord -> coord.groupId().equals("jakarta.annotation") && 
                             coord.artifactId().equals("jakarta.annotation-api"));
        assertThat(foundJakartaAnnotation).isTrue();
    }
    
    @Test
    void testRealMavenCentralLookup_HibernateValidator() throws Exception {
        // Test a real-world case: Hibernate Validator
        LOGGER.info("Testing org.hibernate:hibernate-validator lookup...");
        
        CompletableFuture<List<JakartaArtifactCoordinates>> future = 
            mavenCentralService.findJakartaEquivalents("org.hibernate", "hibernate-validator");
        
        List<JakartaArtifactCoordinates> results = future.get(30, TimeUnit.SECONDS);
        
        LOGGER.info("Found " + results.size() + " results for hibernate-validator");
        for (JakartaArtifactCoordinates result : results) {
            LOGGER.info("  - " + result.groupId() + ":" + result.artifactId() + ":" + result.version() + " (" + result.status() + ")");
        }
        
        // Should find the Jakarta-compatible version
        assertThat(results).isNotEmpty();
    }
    
    @Test
    void testRealMavenCentralLookup_NonExistent() throws Exception {
        // Test with a non-existent artifact to verify graceful handling
        LOGGER.info("Testing non-existent artifact lookup...");
        
        CompletableFuture<List<JakartaArtifactCoordinates>> future = 
            mavenCentralService.findJakartaEquivalents("com.example", "non-existent-artifact");
        
        List<JakartaArtifactCoordinates> results = future.get(30, TimeUnit.SECONDS);
        
        LOGGER.info("Found " + results.size() + " results for non-existent artifact");
        
        // Should return empty list, not throw exception
        assertThat(results).isEmpty();
    }
    
    @Test
    @Disabled("This test is for manual verification of API connectivity issues")
    void testConnectivityDiagnostic() throws Exception {
        // Diagnostic test to check Maven Central API connectivity
        LOGGER.info("Testing Maven Central API connectivity...");
        
        // Test a simple known Jakarta artifact
        CompletableFuture<List<JakartaArtifactCoordinates>> future = 
            mavenCentralService.findJakartaEquivalents("jakarta.servlet", "jakarta.servlet-api");
        
        try {
            List<JakartaArtifactCoordinates> results = future.get(15, TimeUnit.SECONDS);
            LOGGER.info("Connectivity test successful: Found " + results.size() + " results");
            
            if (!results.isEmpty()) {
                LOGGER.info("Sample result: " + results.get(0).groupId() + ":" + results.get(0).artifactId() + ":" + results.get(0).version());
            }
        } catch (Exception e) {
            LOGGER.warning("Connectivity test failed: " + e.getMessage());
            throw e;
        }
    }
}
