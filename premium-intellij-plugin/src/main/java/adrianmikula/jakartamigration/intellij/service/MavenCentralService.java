package adrianmikula.jakartamigration.intellij.service;

import adrianmikula.jakartamigration.intellij.model.JakartaArtifactCoordinates;
import adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Service for querying Maven Central API to find Jakarta EE artifacts
 * that correspond to detected javax dependencies.
 */
public class MavenCentralService {
    
    private static final Logger LOGGER = Logger.getLogger(MavenCentralService.class.getName());
    
    private static final String MAVEN_CENTRAL_URL = "https://search.maven.org/solrsearch/select";
    
    /**
     * Finds Jakarta equivalent artifacts for given javax dependency with fuzzy matching
     * 
     * @param javaxGroupId the group ID of the javax dependency
     * @param javaxArtifactId the artifact ID of the javax dependency
     * @return CompletableFuture with list of Jakarta artifact coordinates
     */
    public CompletableFuture<List<JakartaArtifactCoordinates>> findJakartaEquivalents(
            @NotNull String javaxGroupId, 
            @NotNull String javaxArtifactId) {
        
        LOGGER.info("Searching for Jakarta equivalents for javax dependency: " + javaxGroupId + ":" + javaxArtifactId);
        
        return CompletableFuture.supplyAsync(() -> {
            List<JakartaArtifactCoordinates> allResults = new ArrayList<>();
            
            // Try multiple search strategies for fuzzy matching
            allResults.addAll(searchWithExactMatch(javaxGroupId, javaxArtifactId));
            allResults.addAll(searchWithArtifactNameMapping(javaxGroupId, javaxArtifactId));
            allResults.addAll(searchWithGroupNameMapping(javaxGroupId, javaxArtifactId));
            
            // Remove duplicates and sort by relevance
            List<JakartaArtifactCoordinates> uniqueResults = allResults.stream()
                    .distinct()
                    .sorted((a, b) -> compareRelevance(a, b, javaxGroupId, javaxArtifactId))
                    .collect(Collectors.toList());
            
            LOGGER.info("Found " + uniqueResults.size() + " unique Jakarta artifacts for " + javaxGroupId + ":" + javaxArtifactId);
            return uniqueResults;
        });
    }
    
    /**
     * Search with exact match strategy
     */
    private List<JakartaArtifactCoordinates> searchWithExactMatch(String javaxGroupId, String javaxArtifactId) {
        return performMavenCentralSearch(javaxGroupId, javaxArtifactId);
    }
    
    /**
     * Search with common artifact name mappings (javax → jakarta)
     */
    private List<JakartaArtifactCoordinates> searchWithArtifactNameMapping(String javaxGroupId, String javaxArtifactId) {
        List<JakartaArtifactCoordinates> results = new ArrayList<>();
        
        // Common artifact name mappings
        Map<String, String> artifactMappings = new HashMap<>();
        artifactMappings.put("javax.servlet-api", "jakarta.servlet-api");
        artifactMappings.put("javax.servlet.jsp-api", "jakarta.servlet.jsp-api");
        artifactMappings.put("javax.servlet.jsp.jstl-api", "jakarta.servlet.jsp.jstl-api");
        artifactMappings.put("javax.persistence-api", "jakarta.persistence-api");
        artifactMappings.put("javax.transaction-api", "jakarta.transaction-api");
        artifactMappings.put("javax.validation-api", "jakarta.validation-api");
        artifactMappings.put("javax.inject", "jakarta.inject");
        artifactMappings.put("javax.annotation-api", "jakarta.annotation-api");
        artifactMappings.put("javax.ejb-api", "jakarta.ejb-api");
        artifactMappings.put("javax.faces-api", "jakarta.faces-api");
        artifactMappings.put("javax.jms-api", "jakarta.jms-api");
        artifactMappings.put("javax.json-api", "jakarta.json-api");
        artifactMappings.put("javax.websocket-api", "jakarta.websocket-api");
        artifactMappings.put("javax.xml.bind-api", "jakarta.xml.bind-api");
        artifactMappings.put("javax.xml.ws-api", "jakarta.xml.ws-api");
        
        String mappedArtifactId = artifactMappings.get(javaxArtifactId);
        if (mappedArtifactId != null) {
            results.addAll(performMavenCentralSearch(javaxGroupId, mappedArtifactId));
        }
        
        return results;
    }
    
    /**
     * Search with common group name mappings (javax → jakarta)
     */
    private List<JakartaArtifactCoordinates> searchWithGroupNameMapping(String javaxGroupId, String javaxArtifactId) {
        List<JakartaArtifactCoordinates> results = new ArrayList<>();
        
        // Common group name mappings
        Map<String, String> groupMappings = new HashMap<>();
        groupMappings.put("javax.servlet", "jakarta.servlet");
        groupMappings.put("javax.persistence", "jakarta.persistence");
        groupMappings.put("javax.transaction", "jakarta.transaction");
        groupMappings.put("javax.validation", "jakarta.validation");
        groupMappings.put("javax.inject", "jakarta.inject");
        groupMappings.put("javax.annotation", "jakarta.annotation");
        groupMappings.put("javax.ejb", "jakarta.ejb");
        groupMappings.put("javax.faces", "jakarta.faces");
        groupMappings.put("javax.jms", "jakarta.jms");
        groupMappings.put("javax.json", "jakarta.json");
        groupMappings.put("javax.websocket", "jakarta.websocket");
        groupMappings.put("javax.xml.bind", "jakarta.xml.bind");
        groupMappings.put("javax.xml.ws", "jakarta.xml.ws");
        
        String mappedGroupId = groupMappings.get(javaxGroupId);
        if (mappedGroupId != null) {
            results.addAll(performMavenCentralSearch(mappedGroupId, javaxArtifactId));
        }
        
        return results;
    }
    
    /**
     * Performs the actual Maven Central search
     */
    private List<JakartaArtifactCoordinates> performMavenCentralSearch(String groupId, String artifactId) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            
            // Search for Jakarta artifacts
            String searchQuery = "g:" + groupId + " AND a:" + artifactId;
            String url = MAVEN_CENTRAL_URL + "?q=" + URLEncoder.encode(searchQuery, "UTF-8") + "&rows=20&wt=json";
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    
            if (response.statusCode() == 200) {
                String responseBody = response.body();
                return parseMavenCentralResponse(responseBody, groupId, artifactId);
            } else {
                LOGGER.warning("Failed to query Maven Central: HTTP " + response.statusCode());
                return new ArrayList<>();
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error querying Maven Central for " + groupId + ":" + artifactId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Compare relevance of search results
     */
    private int compareRelevance(JakartaArtifactCoordinates a, JakartaArtifactCoordinates b, 
                               String originalGroupId, String originalArtifactId) {
        // Prefer exact group ID matches
        boolean aExactGroup = a.groupId().equals(originalGroupId.replace("javax", "jakarta"));
        boolean bExactGroup = b.groupId().equals(originalGroupId.replace("javax", "jakarta"));
        
        if (aExactGroup && !bExactGroup) return -1;
        if (!aExactGroup && bExactGroup) return 1;
        
        // Prefer exact artifact ID matches
        boolean aExactArtifact = a.artifactId().equals(originalArtifactId.replace("javax", "jakarta"));
        boolean bExactArtifact = b.artifactId().equals(originalArtifactId.replace("javax", "jakarta"));
        
        if (aExactArtifact && !bExactArtifact) return -1;
        if (!aExactArtifact && bExactArtifact) return 1;
        
        // Prefer compatible status
        if (a.status() == DependencyMigrationStatus.COMPATIBLE && 
            b.status() != DependencyMigrationStatus.COMPATIBLE) return -1;
        if (a.status() != DependencyMigrationStatus.COMPATIBLE && 
            b.status() == DependencyMigrationStatus.COMPATIBLE) return 1;
        
        return 0;
    }
    
    /**
     * Parses Maven Central Solr response to extract Jakarta artifact information
     * Uses proper JSON parsing for reliable results
     */
    private List<JakartaArtifactCoordinates> parseMavenCentralResponse(
            String responseBody, 
            String groupId, 
            String artifactId) {
        
        List<JakartaArtifactCoordinates> results = new ArrayList<>();
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(responseBody);
            
            LOGGER.info("Maven Central response structure: " + rootNode.toString().substring(0, Math.min(500, rootNode.toString().length())));
            
            // Navigate to the "docs" array
            JsonNode docsNode = rootNode.path("docs");
            LOGGER.info("Docs node found: " + docsNode.isMissingNode() + ", isArray: " + docsNode.isArray() + ", size: " + docsNode.size());
            
            if (docsNode.isArray() && docsNode.size() > 0) {
                for (JsonNode docNode : docsNode) {
                    LOGGER.info("Processing doc node: " + docNode.toString());
                    
                    // Extract groupId, artifactId, and version
                    String foundGroupId = docNode.path("g").asText();
                    String foundArtifactId = docNode.path("a").asText();
                    String version = docNode.path("latestVersion").asText();
                    
                    LOGGER.info("Extracted - groupId: " + foundGroupId + ", artifactId: " + foundArtifactId + ", version: " + version);
                    
                    if (!foundGroupId.isEmpty() && !foundArtifactId.isEmpty() && !version.isEmpty()) {
                        // Calculate compatibility status
                        DependencyMigrationStatus status = calculateCompatibilityStatus(foundGroupId, foundArtifactId, version);
                        
                        // Create coordinates object
                        JakartaArtifactCoordinates coordinates = new JakartaArtifactCoordinates(
                            foundGroupId, foundArtifactId, version, status
                        );
                        
                        results.add(coordinates);
                        LOGGER.info("Found Jakarta artifact: " + foundGroupId + ":" + foundArtifactId + ":" + version + " (" + status + ")");
                    }
                }
            } else {
                LOGGER.warning("No docs array found in Maven Central response or empty array");
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error parsing Maven Central response", e);
        }
        
        LOGGER.info("Found " + results.size() + " Jakarta artifacts for " + groupId + ":" + artifactId);
        return results;
    }
    
    /**
     * Calculates migration status based on version comparison
     */
    private DependencyMigrationStatus calculateCompatibilityStatus(String groupId, String artifactId, String version) {
        // This is a simplified compatibility calculation
        // In a production implementation, you'd use semantic version parsing
        try {
            String[] versionParts = version.split("\\.");
            if (versionParts.length >= 2) {
                int majorVersion = Integer.parseInt(versionParts[0]);
                // Major version changes require manual migration
                return DependencyMigrationStatus.REQUIRES_MANUAL_MIGRATION;
            }
            
            // For now, assume compatible if we can find a Jakarta version
            return DependencyMigrationStatus.COMPATIBLE;
        } catch (Exception e) {
            return DependencyMigrationStatus.NO_JAKARTA_VERSION;
        }
    }
}
