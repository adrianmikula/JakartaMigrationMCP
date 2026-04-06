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
 * Simple test to verify our TDD improvements work
 */
@DisplayName("Jakarta Lookup TDD Verification")
public class SimpleJakartaLookupTest {
    
    private ImprovedMavenCentralLookupService lookupService;
    
    @BeforeEach
    void setUp() {
        lookupService = new ImprovedMavenCentralLookupService();
    }
    
    @Test
    @DisplayName("Should handle naming variations - javax.servlet vs javax.servlet-api")
    void shouldHandleNamingVariations() throws Exception {
        // Test both variations
        CompletableFuture<List<JakartaArtifactMatch>> result1 = 
            lookupService.findJakartaEquivalents("javax.servlet", "javax.servlet");
        CompletableFuture<List<JakartaArtifactMatch>> result2 = 
            lookupService.findJakartaEquivalents("javax.servlet", "javax.servlet-api");
        
        List<JakartaArtifactMatch> artifacts1 = result1.get(30, TimeUnit.SECONDS);
        List<JakartaArtifactMatch> artifacts2 = result2.get(30, TimeUnit.SECONDS);
        
        // Both should find results
        assertThat(artifacts1).isNotEmpty();
        assertThat(artifacts2).isNotEmpty();
        
        // Log results for verification
        System.out.println("Results for javax.servlet:");
        artifacts1.forEach(a -> System.out.println("  " + a.groupId() + ":" + a.artifactId() + ":" + a.version()));
        
        System.out.println("Results for javax.servlet-api:");
        artifacts2.forEach(a -> System.out.println("  " + a.groupId() + ":" + a.artifactId() + ":" + a.version()));
    }
    
    @Test
    @DisplayName("Should handle case insensitive matching")
    void shouldHandleCaseInsensitive() throws Exception {
        CompletableFuture<List<JakartaArtifactMatch>> result = 
            lookupService.findJakartaEquivalents("JAVAX.SERVLET", "JAVAX.SERVLET-API");
        
        List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
        
        assertThat(artifacts).isNotEmpty();
        
        System.out.println("Results for JAVAX.SERVLET (case insensitive):");
        artifacts.forEach(a -> System.out.println("  " + a.groupId() + ":" + a.artifactId() + ":" + a.version()));
    }
    
    @Test
    @DisplayName("Should handle malformed coordinates gracefully")
    void shouldHandleMalformedCoordinates() throws Exception {
        CompletableFuture<List<JakartaArtifactMatch>> result = 
            lookupService.findJakartaEquivalents("", "invalid-artifact");
        
        List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
        
        // Should not crash, return empty list
        assertThat(artifacts).isEmpty();
        
        System.out.println("Results for malformed coordinates: " + artifacts.size());
    }
    
    @Test
    @DisplayName("Should find Jakarta equivalent for javax.persistence-api")
    void shouldFindJakartaForJavaxPersistenceApi() throws Exception {
        CompletableFuture<List<JakartaArtifactMatch>> result = 
            lookupService.findJakartaEquivalents("javax.persistence", "javax.persistence-api");
        
        List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
        
        assertThat(artifacts).isNotEmpty();
        
        System.out.println("Results for javax.persistence-api:");
        artifacts.forEach(a -> System.out.println("  " + a.groupId() + ":" + a.artifactId() + ":" + a.version()));
    }
}
