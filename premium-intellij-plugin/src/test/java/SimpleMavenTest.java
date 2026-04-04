import adrianmikula.jakartamigration.intellij.service.MavenCentralService;
import adrianmikula.jakartamigration.intellij.model.JakartaArtifactCoordinates;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SimpleMavenTest {
    
    @Test
    void testBasicFunctionality() throws Exception {
        MavenCentralService service = new MavenCentralService();
        
        System.out.println("Testing Maven Central Service...");
        
        // Test the search functionality
        CompletableFuture<List<JakartaArtifactCoordinates>> future = 
            service.findJakartaEquivalents("javax.servlet", "javax.servlet-api");
        
        List<JakartaArtifactCoordinates> results = future.get();
        
        System.out.println("Search completed. Found " + results.size() + " results:");
        
        if (!results.isEmpty()) {
            System.out.println("SUCCESS: Found Jakarta artifacts!");
            for (JakartaArtifactCoordinates result : results) {
                System.out.println("  - " + result.groupId() + ":" + result.artifactId() + ":" + result.version());
            }
        } else {
            System.out.println("FAILED: No Jakarta artifacts found");
        }
    }
}
