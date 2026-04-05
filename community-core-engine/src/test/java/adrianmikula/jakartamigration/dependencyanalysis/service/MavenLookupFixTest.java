package adrianmikula.jakartamigration.dependencyanalysis.service;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * Test to verify Maven Central lookup fix
 */
@Tag("slow")  // Makes real network calls to Maven Central
@DisplayName("Maven Central Lookup Fix Verification")
public class MavenLookupFixTest {
    
    private ImprovedMavenCentralLookupService lookupService;
    
    @BeforeEach
    void setUp() {
        lookupService = new ImprovedMavenCentralLookupService();
    }
    
    @Test
    @DisplayName("Should find Jakarta equivalents for javax.servlet-api")
    void shouldFindJakartaForJavaxServletApi() throws Exception {
        // Retry up to 3 times for network resilience
        List<JakartaArtifactMatch> artifacts = null;
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                System.out.println("Attempt " + attempt + " for javax.servlet-api lookup...");
                CompletableFuture<List<JakartaArtifactMatch>> result = 
                    lookupService.findJakartaEquivalents("javax.servlet", "javax.servlet-api");
                
                artifacts = result.get(30, TimeUnit.SECONDS);
                
                if (!artifacts.isEmpty()) {
                    break; // Success!
                }
                
                System.out.println("Attempt " + attempt + " returned empty results, waiting before retry...");
                Thread.sleep(1000); // Wait 1 second before retry
                
            } catch (Exception e) {
                lastException = e;
                System.out.println("Attempt " + attempt + " failed: " + e.getMessage());
                Thread.sleep(1000);
            }
        }
        
        // Debug output
        System.out.println("Final artifacts list: " + (artifacts == null ? "null" : artifacts.size() + " items"));
        if (artifacts != null) {
            artifacts.forEach(a -> System.out.println("  - " + a.groupId() + ":" + a.artifactId() + ":" + a.version() + " (found=" + a.found() + ")"));
        }
        
        // Should find Jakarta equivalents
        assertThat(artifacts)
            .withFailMessage("Should find Jakarta equivalents for javax.servlet-api. Last error: " + lastException)
            .isNotNull()
            .isNotEmpty();
        
        // Check if we found jakarta.servlet:jakarta.servlet-api
        boolean foundJakartaServlet = artifacts.stream()
            .anyMatch(match -> 
                "jakarta.servlet".equals(match.groupId()) && 
                "jakarta.servlet-api".equals(match.artifactId()));
                
        assertThat(foundJakartaServlet)
            .withFailMessage("Should find jakarta.servlet:jakarta.servlet-api in results")
            .isTrue();
        
        System.out.println("✅ Found Jakarta equivalent for javax.servlet-api:");
        artifacts.forEach(a -> System.out.println("  - " + a.groupId() + ":" + a.artifactId() + ":" + a.version()));
    }
    
    @Test
    @DisplayName("Should find Jakarta equivalents for javax.persistence-api")
    void shouldFindJakartaForJavaxPersistenceApi() throws Exception {
        // Retry up to 3 times for network resilience
        List<JakartaArtifactMatch> artifacts = null;
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                System.out.println("Attempt " + attempt + " for javax.persistence-api lookup...");
                CompletableFuture<List<JakartaArtifactMatch>> result = 
                    lookupService.findJakartaEquivalents("javax.persistence", "javax.persistence-api");
                
                artifacts = result.get(30, TimeUnit.SECONDS);
                
                if (!artifacts.isEmpty()) {
                    break; // Success!
                }
                
                System.out.println("Attempt " + attempt + " returned empty results, waiting before retry...");
                Thread.sleep(1000);
                
            } catch (Exception e) {
                lastException = e;
                System.out.println("Attempt " + attempt + " failed: " + e.getMessage());
                Thread.sleep(1000);
            }
        }
        
        // Debug output
        System.out.println("Final artifacts list: " + (artifacts == null ? "null" : artifacts.size() + " items"));
        if (artifacts != null) {
            artifacts.forEach(a -> System.out.println("  - " + a.groupId() + ":" + a.artifactId() + ":" + a.version()));
        }
        
        assertThat(artifacts)
            .withFailMessage("Should find Jakarta equivalents for javax.persistence-api. Last error: " + lastException)
            .isNotNull()
            .isNotEmpty();
        
        boolean foundJakartaPersistence = artifacts.stream()
            .anyMatch(match -> 
                "jakarta.persistence".equals(match.groupId()) && 
                "jakarta.persistence-api".equals(match.artifactId()));
                
        assertThat(foundJakartaPersistence)
            .withFailMessage("Should find jakarta.persistence:jakarta.persistence-api in results")
            .isTrue();
        
        System.out.println("✅ Found Jakarta equivalent for javax.persistence-api:");
        artifacts.forEach(a -> System.out.println("  - " + a.groupId() + ":" + a.artifactId() + ":" + a.version()));
    }
}
