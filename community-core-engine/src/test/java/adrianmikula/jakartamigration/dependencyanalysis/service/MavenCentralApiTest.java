package adrianmikula.jakartamigration.dependencyanalysis.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Simple test to verify Maven Central API functionality
 */
public class MavenCentralApiTest {
    
    public static void main(String[] args) {
        try {
            testMavenCentralApi();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void testMavenCentralApi() throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        
        // Test search for jakarta.servlet-api
        String searchQuery = "g:jakarta.servlet AND a:jakarta.servlet-api";
        String url = "https://search.maven.org/solrsearch/select?q=" + 
                   URLEncoder.encode(searchQuery, StandardCharsets.UTF_8) + 
                   "&rows=5&wt=json";
        
        System.out.println("Testing URL: " + url);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .header("User-Agent", "Jakarta-Migration-MCP/1.0")
                .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        System.out.println("Status Code: " + response.statusCode());
        System.out.println("Response Body: " + response.body());
        
        if (response.statusCode() == 200) {
            System.out.println("✅ Maven Central API is working!");
        } else {
            System.out.println("❌ Maven Central API failed with status: " + response.statusCode());
        }
    }
}
