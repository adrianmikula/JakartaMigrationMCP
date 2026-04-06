package adrianmikula.jakartamigration.dependencyanalysis.service;

import adrianmikula.jakartamigration.dependencyanalysis.service.ImprovedMavenCentralLookupService.JakartaArtifactMatch;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * Simple verification test for our TDD Jakarta lookup improvements
 */
@DisplayName("Jakarta Lookup Improvements Verification")
public class JakartaLookupVerificationTest {
    
    private ImprovedMavenCentralLookupService lookupService;
    
    @BeforeEach
    void setUp() {
        lookupService = new ImprovedMavenCentralLookupService();
    }
    
    @Test
    @DisplayName("Should handle input validation gracefully")
    void shouldHandleInputValidation() throws Exception {
        // Test empty coordinates
        CompletableFuture<List<JakartaArtifactMatch>> result1 = 
            lookupService.findJakartaEquivalents("", "invalid-artifact");
        
        List<JakartaArtifactMatch> artifacts1 = result1.get(10, TimeUnit.SECONDS);
        assertThat(artifacts1).isEmpty();
        
        // Test null coordinates
        CompletableFuture<List<JakartaArtifactMatch>> result2 = 
            lookupService.findJakartaEquivalents(null, null);
        
        List<JakartaArtifactMatch> artifacts2 = result2.get(10, TimeUnit.SECONDS);
        assertThat(artifacts2).isEmpty();
        
        System.out.println("✅ Input validation works correctly");
    }
    
    @Test
    @DisplayName("Should handle case insensitive variations")
    void shouldHandleCaseInsensitive() throws Exception {
        // Test case insensitive matching
        CompletableFuture<List<JakartaArtifactMatch>> result = 
            lookupService.findJakartaEquivalents("JAVAX.SERVLET", "JAVAX.SERVLET-API");
        
        List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
        
        // Should find results due to case insensitive matching
        assertThat(artifacts).isNotEmpty();
        
        System.out.println("✅ Case insensitive matching works");
        System.out.println("   Found " + artifacts.size() + " results for JAVAX.SERVLET-API");
    }
    
    @Test
    @DisplayName("Should handle naming variations")
    void shouldHandleNamingVariations() throws Exception {
        // Test both with and without -api suffix
        CompletableFuture<List<JakartaArtifactMatch>> result1 = 
            lookupService.findJakartaEquivalents("javax.servlet", "javax.servlet");
        CompletableFuture<List<JakartaArtifactMatch>> result2 = 
            lookupService.findJakartaEquivalents("javax.servlet", "javax.servlet-api");
        
        List<JakartaArtifactMatch> artifacts1 = result1.get(30, TimeUnit.SECONDS);
        List<JakartaArtifactMatch> artifacts2 = result2.get(30, TimeUnit.SECONDS);
        
        // Both should find results
        assertThat(artifacts1).isNotEmpty();
        assertThat(artifacts2).isNotEmpty();
        
        System.out.println("✅ Naming variations work correctly");
        System.out.println("   javax.servlet: " + artifacts1.size() + " results");
        System.out.println("   javax.servlet-api: " + artifacts2.size() + " results");
    }
    
    @Test
    @DisplayName("Should find Jakarta equivalents for common javax artifacts")
    void shouldFindJakartaEquivalents() throws Exception {
        // Test a few common mappings
        String[] testCases = {
            "javax.persistence:javax.persistence-api",
            "javax.validation:javax.validation-api",
            "javax.annotation:javax.annotation-api"
        };
        
        for (String testCase : testCases) {
            String[] parts = testCase.split(":");
            String groupId = parts[0];
            String artifactId = parts[1];
            
            CompletableFuture<List<JakartaArtifactMatch>> result = 
                lookupService.findJakartaEquivalents(groupId, artifactId);
            
            List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
            
            assertThat(artifacts).isNotEmpty();
            
            // Check if we found Jakarta equivalents
            boolean foundJakarta = artifacts.stream()
                .anyMatch(match -> match.groupId() != null && match.groupId().startsWith("jakarta."));
                
            assertThat(foundJakarta).isTrue();
            
            System.out.println("✅ " + testCase + " → " + artifacts.size() + " Jakarta results found");
        }
    }
}
