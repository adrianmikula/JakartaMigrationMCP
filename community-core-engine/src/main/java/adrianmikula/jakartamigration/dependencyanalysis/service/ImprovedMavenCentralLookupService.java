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
    
    private static final String MAVEN_CENTRAL_URL = "https://search.maven.org/solrsearch/select";
    private static final String MAVEN_CENTRAL_API_URL = "https://api.sonatype.org/service/local/lucene/search";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
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
        
        return CompletableFuture.supplyAsync(() -> {
            List<JakartaArtifactMatch> allResults = new ArrayList<>();
            
            // Try multiple search strategies for fuzzy matching
            allResults.addAll(searchWithExactMatch(javaxGroupId, javaxArtifactId));
            allResults.addAll(searchWithArtifactNameMapping(javaxGroupId, javaxArtifactId));
            allResults.addAll(searchWithGroupNameMapping(javaxGroupId, javaxArtifactId));
            
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
            results.addAll(performMavenCentralSearch(groupId, mappedArtifactId));
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
     * Performs the actual Maven Central search with fallback endpoints
     */
    private List<JakartaArtifactMatch> performMavenCentralSearch(String groupId, String artifactId) {
        List<JakartaArtifactMatch> results = new ArrayList<>();
        
        // Try the primary endpoint first
        results.addAll(performSearchWithEndpoint(MAVEN_CENTRAL_URL, groupId, artifactId));
        
        // If no results, try alternative endpoint
        if (results.isEmpty()) {
            log.info("No results from primary endpoint, trying alternative...");
            results.addAll(performSearchWithEndpoint(MAVEN_CENTRAL_API_URL, groupId, artifactId));
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
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .header("User-Agent", "Jakarta-Migration-MCP/1.0")
                    .build();
            
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                    
            if (response.statusCode() == 200) {
                return parseMavenCentralResponse(response.body());
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
            
            // Navigate to the "docs" array
            JsonNode docsNode = rootNode.path("docs");
            
            if (docsNode.isArray() && docsNode.size() > 0) {
                for (JsonNode docNode : docsNode) {
                    // Extract groupId, artifactId, and version
                    String foundGroupId = docNode.path("g").asText();
                    String foundArtifactId = docNode.path("a").asText();
                    String version = docNode.path("latestVersion").asText();
                    
                    if (!foundGroupId.isEmpty() && !foundArtifactId.isEmpty() && !version.isEmpty()) {
                        results.add(JakartaArtifactMatch.of(foundGroupId, foundArtifactId, version));
                        log.debug("Found Jakarta artifact: {}:{}", foundGroupId, foundArtifactId);
                    }
                }
            }
            
        } catch (Exception e) {
            log.warn("Error parsing Maven Central response", e);
        }
        
        return results;
    }
}
