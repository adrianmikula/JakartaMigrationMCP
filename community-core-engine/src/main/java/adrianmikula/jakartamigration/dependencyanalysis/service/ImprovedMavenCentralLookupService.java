package adrianmikula.jakartamigration.dependencyanalysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Simplified Maven Central lookup service with fuzzy matching capabilities.
 * This is a lightweight version of the MavenCentralService for the community module.
 */
@Slf4j
public class ImprovedMavenCentralLookupService {
    
    private static final String MAVEN_CENTRAL_API = "https://search.maven.org/solrsearch/select";
    private static final String MAVEN_CENTRAL_FALLBACK = "https://search.maven.org/solrsearch/select";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    // HTTP client is instance field to allow mocking in tests
    private HttpClient httpClient;
    
    public ImprovedMavenCentralLookupService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }
    
    // Package-private constructor for testing with mocked HTTP client
    ImprovedMavenCentralLookupService(HttpClient httpClient) {
        this.httpClient = httpClient;
    }
    
    // Common artifact name mappings for fuzzy matching
    private static final Map<String, String> ARTIFACT_MAPPINGS = new HashMap<>();
    static {
        ARTIFACT_MAPPINGS.put("javax.servlet-api", "jakarta.servlet-api");
        ARTIFACT_MAPPINGS.put("javax.servlet.jsp-api", "jakarta.servlet.jsp-api");
        ARTIFACT_MAPPINGS.put("javax.servlet.jsp.jstl-api", "jakarta.servlet.jsp.jstl-api");
        ARTIFACT_MAPPINGS.put("javax.persistence-api", "jakarta.persistence-api");
        ARTIFACT_MAPPINGS.put("javax.transaction-api", "jakarta.transaction-api");
        ARTIFACT_MAPPINGS.put("javax.validation-api", "jakarta.validation-api");
        ARTIFACT_MAPPINGS.put("javax.inject", "jakarta.inject");
        ARTIFACT_MAPPINGS.put("javax.annotation-api", "jakarta.annotation-api");
        ARTIFACT_MAPPINGS.put("javax.ejb-api", "jakarta.ejb-api");
        ARTIFACT_MAPPINGS.put("javax.faces-api", "jakarta.faces-api");
        ARTIFACT_MAPPINGS.put("javax.jms-api", "jakarta.jms-api");
        ARTIFACT_MAPPINGS.put("javax.json-api", "jakarta.json-api");
        ARTIFACT_MAPPINGS.put("javax.websocket-api", "jakarta.websocket-api");
        ARTIFACT_MAPPINGS.put("javax.xml.bind-api", "jakarta.xml.bind-api");
        ARTIFACT_MAPPINGS.put("javax.xml.ws-api", "jakarta.xml.ws-api");
        // Additional mappings for comprehensive coverage
        ARTIFACT_MAPPINGS.put("javax.ws.rs-api", "jakarta.ws.rs-api");
        ARTIFACT_MAPPINGS.put("javax.mail-api", "jakarta.mail-api");
        ARTIFACT_MAPPINGS.put("javax.enterprise.cdi-api", "jakarta.enterprise.cdi-api");
        ARTIFACT_MAPPINGS.put("javax.security.enterprise-api", "jakarta.security.enterprise-api");
    }
    
    // Common group name mappings for fuzzy matching
    private static final Map<String, String> GROUP_MAPPINGS = new HashMap<>();
    static {
        GROUP_MAPPINGS.put("javax.servlet", "jakarta.servlet");
        GROUP_MAPPINGS.put("javax.persistence", "jakarta.persistence");
        GROUP_MAPPINGS.put("javax.transaction", "jakarta.transaction");
        GROUP_MAPPINGS.put("javax.validation", "jakarta.validation");
        GROUP_MAPPINGS.put("javax.inject", "jakarta.inject");
        GROUP_MAPPINGS.put("javax.annotation", "jakarta.annotation");
        GROUP_MAPPINGS.put("javax.ejb", "jakarta.ejb");
        GROUP_MAPPINGS.put("javax.faces", "jakarta.faces");
        GROUP_MAPPINGS.put("javax.jms", "jakarta.jms");
        GROUP_MAPPINGS.put("javax.json", "jakarta.json");
        GROUP_MAPPINGS.put("javax.websocket", "jakarta.websocket");
        GROUP_MAPPINGS.put("javax.xml.bind", "jakarta.xml.bind");
        GROUP_MAPPINGS.put("javax.xml.ws", "jakarta.xml.ws");
        // Additional group mappings for comprehensive coverage
        GROUP_MAPPINGS.put("javax.ws.rs", "jakarta.ws.rs");
        GROUP_MAPPINGS.put("javax.mail", "jakarta.mail");
        GROUP_MAPPINGS.put("javax.enterprise", "jakarta.enterprise");
        GROUP_MAPPINGS.put("javax.security", "jakarta.security");
        GROUP_MAPPINGS.put("javax.servlet.jsp", "jakarta.servlet.jsp");
        GROUP_MAPPINGS.put("javax.servlet.jsp.jstl", "jakarta.servlet.jsp.jstl");
    }
    
    /**
     * Result of an artifact lookup containing Jakarta artifact information.
     */
    public record JakartaArtifactMatch(
            String groupId,
            String artifactId,
            String version,
            boolean found
    ) {
        public static JakartaArtifactMatch notFound() {
            return new JakartaArtifactMatch(null, null, null, false);
        }
        
        public static JakartaArtifactMatch of(String groupId, String artifactId, String version) {
            return new JakartaArtifactMatch(groupId, artifactId, version, true);
        }
    }
    
    /**
     * Finds Jakarta equivalent artifacts with fuzzy matching strategies.
     */
    public CompletableFuture<List<JakartaArtifactMatch>> findJakartaEquivalents(
            String javaxGroupId, 
            String javaxArtifactId) {
        
        log.info("Searching for Jakarta equivalents for javax dependency: {}:{}", javaxGroupId, javaxArtifactId);
        
        // Input validation
        if (javaxGroupId == null || javaxGroupId.trim().isEmpty() || 
            javaxArtifactId == null || javaxArtifactId.trim().isEmpty()) {
            log.warn("Invalid coordinates provided: groupId='{}', artifactId='{}'", javaxGroupId, javaxArtifactId);
            return CompletableFuture.completedFuture(List.of());
        }
        
        return CompletableFuture.supplyAsync(() -> {
            List<JakartaArtifactMatch> allResults = new ArrayList<>();
            
            // Try multiple search strategies for fuzzy matching
            allResults.addAll(searchWithExactMatch(javaxGroupId, javaxArtifactId));
            allResults.addAll(searchWithArtifactNameMapping(javaxGroupId, javaxArtifactId));
            allResults.addAll(searchWithGroupNameMapping(javaxGroupId, javaxArtifactId));
            allResults.addAll(searchWithNamingVariations(javaxGroupId, javaxArtifactId));
            allResults.addAll(searchWithCaseInsensitiveVariations(javaxGroupId, javaxArtifactId));
            
            // Remove duplicates and return first few results
            List<JakartaArtifactMatch> uniqueResults = allResults.stream()
                    .distinct()
                    .limit(5) // Limit to top 5 results
                    .toList();
            
            log.info("Found {} unique Jakarta artifacts for {}:{}", uniqueResults.size(), javaxGroupId, javaxArtifactId);
            return uniqueResults;
        });
    }
    
    /**
     * Search with exact match strategy
     */
    private List<JakartaArtifactMatch> searchWithExactMatch(String groupId, String artifactId) {
        return performMavenCentralSearch(groupId, artifactId);
    }
    
    /**
     * Search with common artifact name mappings (javax → jakarta)
     */
    private List<JakartaArtifactMatch> searchWithArtifactNameMapping(String groupId, String artifactId) {
        List<JakartaArtifactMatch> results = new ArrayList<>();
        
        String mappedArtifactId = ARTIFACT_MAPPINGS.get(artifactId);
        if (mappedArtifactId != null) {
            // Also map the groupId if it's a javax group
            String mappedGroupId = GROUP_MAPPINGS.get(groupId);
            if (mappedGroupId != null) {
                results.addAll(performMavenCentralSearch(mappedGroupId, mappedArtifactId));
            } else {
                results.addAll(performMavenCentralSearch(groupId, mappedArtifactId));
            }
        }
        
        return results;
    }
    
    /**
     * Search with common group name mappings (javax → jakarta)
     */
    private List<JakartaArtifactMatch> searchWithGroupNameMapping(String groupId, String artifactId) {
        List<JakartaArtifactMatch> results = new ArrayList<>();
        
        String mappedGroupId = GROUP_MAPPINGS.get(groupId);
        if (mappedGroupId != null) {
            results.addAll(performMavenCentralSearch(mappedGroupId, artifactId));
        }
        
        return results;
    }
    
    /**
     * Search with naming variations (e.g., "javax.servlet" vs "javax.servlet-api")
     */
    private List<JakartaArtifactMatch> searchWithNamingVariations(String groupId, String artifactId) {
        List<JakartaArtifactMatch> results = new ArrayList<>();
        
        // Try removing -api suffix if present
        if (artifactId.endsWith("-api")) {
            String baseArtifactId = artifactId.substring(0, artifactId.length() - 4);
            results.addAll(performMavenCentralSearch(groupId, baseArtifactId));
        }
        // Try adding -api suffix if not present
        else if (!artifactId.endsWith("-api")) {
            String apiArtifactId = artifactId + "-api";
            results.addAll(performMavenCentralSearch(groupId, apiArtifactId));
        }
        
        return results;
    }
    
    /**
     * Search with case insensitive variations
     */
    private List<JakartaArtifactMatch> searchWithCaseInsensitiveVariations(String groupId, String artifactId) {
        List<JakartaArtifactMatch> results = new ArrayList<>();
        
        // Try lowercase versions
        String lowerGroupId = groupId.toLowerCase();
        String lowerArtifactId = artifactId.toLowerCase();
        
        if (!groupId.equals(lowerGroupId) || !artifactId.equals(lowerArtifactId)) {
            results.addAll(performMavenCentralSearch(lowerGroupId, lowerArtifactId));
        }
        
        return results;
    }
    
    /**
     * Performs the actual Maven Central search with fallback endpoints
     */
    private List<JakartaArtifactMatch> performMavenCentralSearch(String groupId, String artifactId) {
        List<JakartaArtifactMatch> results = new ArrayList<>();
        
        // Try the primary endpoint first
        results.addAll(performSearchWithEndpoint(MAVEN_CENTRAL_API, groupId, artifactId));
        
        // If no results, try alternative endpoint
        if (results.isEmpty()) {
            log.info("No results from primary endpoint, trying alternative...");
            results.addAll(performSearchWithEndpoint(MAVEN_CENTRAL_FALLBACK, groupId, artifactId));
        }
        
        return results;
    }
    
    /**
     * Performs search with a specific endpoint
     */
    private List<JakartaArtifactMatch> performSearchWithEndpoint(String endpoint, String groupId, String artifactId) {
        try {
            String searchQuery = "g:" + groupId + " AND a:" + artifactId;
            String url = endpoint + "?q=" + URLEncoder.encode(searchQuery, "UTF-8") + "&rows=5&wt=json";
            
            log.info("Querying Maven Central: {}", url);
            
            System.out.println("[MavenLookup] Querying: " + url);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .header("User-Agent", "Jakarta-Migration-MCP/1.0")
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            log.info("Maven Central response status: {} for query: {}", response.statusCode(), searchQuery);
            
            System.out.println("[MavenLookup] Response status: " + response.statusCode());
            
            if (response.statusCode() == 200) {
                String body = response.body();
                System.out.println("[MavenLookup] Response body (first 300 chars): " + body.substring(0, Math.min(300, body.length())));
                List<JakartaArtifactMatch> matches = parseMavenCentralResponse(body);
                System.out.println("[MavenLookup] Parsed " + matches.size() + " matches");
                return matches;
            } else {
                log.warn("Failed to query Maven Central endpoint {}: HTTP {}", endpoint, response.statusCode());
                return new ArrayList<>();
            }
        } catch (java.net.ConnectException e) {
            log.warn("Connection failed to Maven Central endpoint {}: {}", endpoint, e.getMessage());
            return new ArrayList<>();
        } catch (java.net.SocketTimeoutException e) {
            log.warn("Timeout connecting to Maven Central endpoint {}: {}", endpoint, e.getMessage());
            return new ArrayList<>();
        } catch (Exception e) {
            log.warn("Error querying Maven Central endpoint {} for {}:{}", endpoint, groupId, artifactId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Parses Maven Central response to extract Jakarta artifact information
     */
    private List<JakartaArtifactMatch> parseMavenCentralResponse(String responseBody) {
        List<JakartaArtifactMatch> results = new ArrayList<>();
        
        try {
            JsonNode rootNode = OBJECT_MAPPER.readTree(responseBody);
            
            // Navigate to the "docs" array - it's inside the "response" object
            JsonNode responseNode = rootNode.path("response");
            JsonNode docsNode = responseNode.path("docs");
            
            System.out.println("[MavenLookup] Response numFound: " + responseNode.path("numFound").asInt());
            System.out.println("[MavenLookup] Docs array size: " + docsNode.size());
            
            if (docsNode.isArray() && docsNode.size() > 0) {
                for (JsonNode docNode : docsNode) {
                    // Extract groupId, artifactId, and version
                    String foundGroupId = docNode.path("g").asText();
                    String foundArtifactId = docNode.path("a").asText();
                    String version = docNode.path("latestVersion").asText();
                    
                    System.out.println("[MavenLookup] Found artifact: " + foundGroupId + ":" + foundArtifactId + ":" + version);
                    
                    if (!foundGroupId.isEmpty() && !foundArtifactId.isEmpty() && !version.isEmpty()) {
                        results.add(JakartaArtifactMatch.of(foundGroupId, foundArtifactId, version));
                        log.debug("Found Jakarta artifact: {}:{}", foundGroupId, foundArtifactId);
                    }
                }
            }
            
        } catch (Exception e) {
            log.warn("Error parsing Maven Central response", e);
            System.out.println("[MavenLookup] Parse error: " + e.getMessage());
        }
        
        return results;
    }
}
