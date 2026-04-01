package adrianmikula.jakartamigration.intellij.service;

import adrianmikula.jakartamigration.intellij.model.JakartaArtifactCoordinates;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Simple test to verify Maven Central JSON parsing fix
 */
public class MavenCentralJsonParsingTest {
    
    @Test
    void testJsonParsingFix() throws Exception {
        MavenCentralService service = new MavenCentralService();
        
        // Test with a known Jakarta artifact that should be found
        CompletableFuture<List<JakartaArtifactCoordinates>> future = 
            service.findJakartaEquivalents("javax.servlet", "javax.servlet-api");
        
        List<JakartaArtifactCoordinates> results = future.get();
        
        // Should find at least one result
        assert !results.isEmpty() : "Should find jakarta.servlet:jakarta.servlet-api";
        
        // Should find the specific artifact we're looking for
        boolean foundJakartaServlet = results.stream()
            .anyMatch(coord -> coord.groupId().equals("jakarta.servlet") && 
                             coord.artifactId().equals("jakarta.servlet-api"));
        
        assert foundJakartaServlet : "Should find jakarta.servlet:jakarta.servlet-api";
        
        // Print results for debugging
        System.out.println("Found " + results.size() + " results:");
        for (JakartaArtifactCoordinates result : results) {
            System.out.println("  - " + result.groupId() + ":" + result.artifactId() + ":" + result.version());
        }
    }
}
