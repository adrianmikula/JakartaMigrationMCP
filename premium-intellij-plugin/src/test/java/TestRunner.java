import adrianmikula.jakartamigration.intellij.service.MavenCentralService;
import adrianmikula.jakartamigration.intellij.model.JakartaArtifactCoordinates;

public class TestRunner {
    public static void main(String[] args) {
        try {
            MavenCentralService service = new MavenCentralService();
            
            System.out.println("Testing Maven Central Service...");
            
            // Test the search functionality
            var future = service.findJakartaEquivalents("javax.servlet", "javax.servlet-api");
            var results = future.get();
            
            System.out.println("Search completed. Found " + results.size() + " results:");
            
            if (!results.isEmpty()) {
                System.out.println("SUCCESS: Found Jakarta artifacts!");
                for (var result : results) {
                    System.out.println("  - " + result.groupId() + ":" + result.artifactId() + ":" + result.version());
                }
            } else {
                System.out.println("FAILED: No Jakarta artifacts found");
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
