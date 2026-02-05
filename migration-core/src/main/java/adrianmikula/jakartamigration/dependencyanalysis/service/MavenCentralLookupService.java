package adrianmikula.jakartamigration.dependencyanalysis.service;

import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Service for looking up Maven artifacts from Maven Central Repository.
 * Provides Jakarta equivalent recommendations by querying Maven Central metadata.
 * Uses Java 11+ built-in HttpClient.
 */
@Slf4j
public class MavenCentralLookupService {

    private static final String MAVEN_CENTRAL_SEARCH_URL = "https://search.maven.org/solrsearch/select";
    private static final String MAVEN_CENTRAL_API_URL = "https://repo1.maven.org/maven2";
    private static final int CONNECTION_TIMEOUT_MS = 5000;

    private final HttpClient httpClient;
    private final Map<String, String> knownJakartaEquivalents;

    public MavenCentralLookupService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofMillis(CONNECTION_TIMEOUT_MS))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        
        this.knownJakartaEquivalents = initializeKnownMappings();
    }

    private Map<String, String> initializeKnownMappings() {
        Map<String, String> mappings = new HashMap<>();
        
        // Servlet
        mappings.put("javax.servlet:javax.servlet-api", "jakarta.servlet:jakarta.servlet-api:6.0.0");
        mappings.put("javax.servlet:javax.servlet-api", "jakarta.servlet:jakarta.servlet-api:6.0.0");
        
        // JSP
        mappings.put("javax.servlet.jsp:javax.servlet.jsp-api", "jakarta.servlet.jsp:jakarta.servlet.jsp-api:3.1.0");
        mappings.put("org.glassfish.web:javax.servlet.jsp", "jakarta.servlet.jsp:jakarta.servlet.jsp:3.1.0");
        
        // EL
        mappings.put("javax.el:javax.el-api", "jakarta.el:jakarta.el-api:5.0.0");
        mappings.put("org.glassfish:javax.el", "jakarta.el:jakarta.el:5.0.0");
        
        // WebSocket
        mappings.put("javax.websocket:javax.websocket-api", "jakarta.websocket:jakarta.websocket-api:2.1.0");
        mappings.put("org.glassfish.tyrus:javax.websocket", "jakarta.websocket:jakarta.websocket:2.1.0");
        
        // JPA
        mappings.put("javax.persistence:javax.persistence-api", "jakarta.persistence:jakarta.persistence-api:3.1.0");
        
        // JTA
        mappings.put("javax.transaction:javax.transaction-api", "jakarta.transaction:jakarta.transaction-api:2.3.3");
        
        // CDI
        mappings.put("javax.enterprise:javax.enterprise-api", "jakarta.enterprise:jakarta.enterprise-api:4.0.0");
        mappings.put("javax.inject:javax.inject-api", "jakarta.inject:jakarta.inject-api:2.0.0");
        
        // Bean Validation
        mappings.put("javax.validation:validation-api", "jakarta.validation:jakarta.validation-api:3.0.2");
        mappings.put("org.hibernate:hibernate-validator", "org.hibernate.validator:hibernate-validator:8.0.0");
        
        // JSON-B
        mappings.put("javax.json:javax.json-api", "jakarta.json:jakarta.json-api:2.1.2");
        mappings.put("org.glassfish:javax.json", "jakarta.json:jakarta.json:2.1.2");
        
        // JAX-RS
        mappings.put("javax.ws.rs:javax.ws.rs-api", "jakarta.ws.rs:jakarta.ws.rs-api:3.1.0");
        
        // JAXB
        mappings.put("javax.xml.bind:jaxb-api", "jakarta.xml.bind:jakarta.xml.bind-api:4.0.0");
        mappings.put("org.glassfish.jaxb:jaxb-runtime", "jakarta.xml.bind:jakarta.xml.bind-runtime:4.0.0");
        
        // Mail
        mappings.put("javax.mail:javax.mail-api", "jakarta.mail:jakarta.mail-api:2.1.2");
        mappings.put("com.sun.mail:javax.mail", "jakarta.mail:jakarta.mail:2.1.2");
        
        // Activation
        mappings.put("javax.activation:javax.activation-api", "jakarta.activation:jakarta.activation-api:2.3.1");
        mappings.put("com.sun.activation:javax.activation", "jakarta.activation:jakarta.activation-api:2.3.1");
        
        // JMS
        mappings.put("javax.jms:javax.jms-api", "jakarta.messaging:jakarta.messaging-api:3.1.2");
        
        // Common annotations
        mappings.put("javax.annotation:javax.annotation-api", "jakarta.annotation:jakarta.annotation-api:3.1.0");
        
        // JSON-P
        mappings.put("javax.json:javax.json", "jakarta.json:jakarta.json:2.1.2");
        
        return Collections.unmodifiableMap(mappings);
    }

    /**
     * Finds Jakarta equivalents for a javax artifact.
     * 
     * @param groupId    The javax group ID
     * @param artifactId The javax artifact ID
     * @return List of Jakarta equivalents found
     */
    public List<ArtifactInfo> findJakartaEquivalents(String groupId, String artifactId) {
        List<ArtifactInfo> results = new ArrayList<>();
        
        // First check our known mappings
        String key = groupId + ":" + artifactId;
        if (knownJakartaEquivalents.containsKey(key)) {
            String jakartaCoordinate = knownJakartaEquivalents.get(key);
            String[] parts = jakartaCoordinate.split(":");
            if (parts.length >= 2) {
                String jakartaGroupId = parts[0];
                String jakartaArtifactId = parts[1];
                String jakartaVersion = parts.length > 2 ? parts[2] : "latest";
                
                results.add(new ArtifactInfo(
                        jakartaGroupId,
                        jakartaArtifactId,
                        jakartaVersion,
                        true,
                        "Verified Jakarta EE equivalent"
                ));
            }
        }
        
        return results;
    }

    /**
     * Searches Maven Central for artifacts matching the query.
     */
    public List<ArtifactInfo> searchMavenCentral(String query) {
        List<ArtifactInfo> results = new ArrayList<>();
        
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = MAVEN_CENTRAL_SEARCH_URL + 
                    "?q=" + encodedQuery + 
                    "&rows=20" + 
                    "&wt=json" + 
                    "&core=gav";
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .timeout(java.time.Duration.ofSeconds(10))
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                results.addAll(parseSearchResults(response.body()));
            }
            
        } catch (Exception e) {
            log.debug("Maven Central search failed: {}", e.getMessage());
        }
        
        return results;
    }

    /**
     * Gets the latest version of an artifact from Maven Central.
     */
    public Optional<String> getLatestVersion(String groupId, String artifactId) {
        List<ArtifactInfo> searchResults = searchMavenCentral("g:" + groupId + " AND a:" + artifactId);
        return searchResults.isEmpty() ? Optional.empty() : Optional.of(searchResults.get(0).version());
    }

    /**
     * Checks if an artifact exists in Maven Central.
     */
    public boolean artifactExists(String groupId, String artifactId, String version) {
        try {
            String jarUrl = MAVEN_CENTRAL_API_URL.replace("https://repo1.maven.org/maven2", "") + 
                    groupId.replace(".", "/") + "/" + artifactId + "/" + version + "/" + 
                    artifactId + "-" + version + ".jar";
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(jarUrl))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .timeout(java.time.Duration.ofSeconds(5))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            return response.statusCode() == 200;
            
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Recommends the best Jakarta version to use based on the javax version.
     */
    public VersionRecommendation recommendVersion(String groupId, String artifactId, String currentVersion) {
        List<ArtifactInfo> equivalents = findJakartaEquivalents(groupId, artifactId);
        
        if (equivalents.isEmpty()) {
            // Try to search Maven Central for jakarta artifacts
            List<ArtifactInfo> searchResults = searchMavenCentral("g:jakarta.* AND a:" + artifactId);
            
            if (searchResults.isEmpty()) {
                return new VersionRecommendation(
                        groupId,
                        artifactId,
                        currentVersion,
                        null,
                        null,
                        null,
                        RecommendationType.NO_EQUIVALENT_FOUND,
                        "No Jakarta equivalent found in Maven Central"
                );
            }
            
            ArtifactInfo info = searchResults.get(0);
            return new VersionRecommendation(
                    groupId,
                    artifactId,
                    currentVersion,
                    info.groupId(),
                    info.artifactId(),
                    info.version(),
                    RecommendationType.DIRECT_EQUIVALENT,
                    "Found Jakarta equivalent via Maven Central search"
            );
        }
        
        ArtifactInfo bestMatch = equivalents.get(0);
        
        return new VersionRecommendation(
                groupId,
                artifactId,
                currentVersion,
                bestMatch.groupId(),
                bestMatch.artifactId(),
                bestMatch.version(),
                RecommendationType.KNOWN_EQUIVALENT,
                "Verified Jakarta EE equivalent with known migration path"
        );
    }

    /**
     * Gets recommended dependency updates for pom.xml.
     */
    public DependencyUpdateRecommendation getDependencyUpdate(String groupId, String artifactId, String currentVersion) {
        VersionRecommendation recommendation = recommendVersion(groupId, artifactId, currentVersion);
        
        if (recommendation.type() == RecommendationType.NO_EQUIVALENT_FOUND) {
            return new DependencyUpdateRecommendation(
                    groupId,
                    artifactId,
                    currentVersion,
                    null,
                    null,
                    null,
                    UpdateAction.SEARCH_MANUALLY,
                    "Manual search required - no automatic equivalent found"
            );
        }
        
        UpdateAction action = UpdateAction.REPLACE_DEPENDENCY;
        String explanation = "Replace with Jakarta equivalent";
        
        return new DependencyUpdateRecommendation(
                recommendation.javaxGroupId(),
                recommendation.javaxArtifactId(),
                recommendation.currentVersion(),
                recommendation.jakartaGroupId(),
                recommendation.jakartaArtifactId(),
                recommendation.recommendedVersion(),
                action,
                explanation
        );
    }

    private List<ArtifactInfo> parseSearchResults(String json) {
        List<ArtifactInfo> results = new ArrayList<>();
        
        try {
            // Simple JSON parsing without external dependencies
            int docsStart = json.indexOf("\"docs\":[");
            if (docsStart == -1) {
                return results;
            }
            
            int arrayEnd = findMatchingBracket(json, docsStart + 7);
            String docsArray = json.substring(docsStart + 7, arrayEnd);
            
            int pos = 0;
            while (pos < docsArray.length()) {
                int objStart = docsArray.indexOf("{", pos);
                if (objStart == -1) break;
                
                int objEnd = findMatchingBracket(docsArray, objStart);
                String doc = docsArray.substring(objStart, objEnd + 1);
                
                String g = extractJsonString(doc, "g");
                String a = extractJsonString(doc, "a");
                String v = extractJsonString(doc, "latestVersion");
                
                if (g != null && a != null) {
                    results.add(new ArtifactInfo(
                            g,
                            a,
                            v != null ? v : "unknown",
                            false,
                            null
                    ));
                }
                
                pos = objEnd + 1;
            }
            
        } catch (Exception e) {
            log.debug("Failed to parse Maven Central search results: {}", e.getMessage());
        }
        
        return results;
    }

    private String extractJsonString(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyPos = json.indexOf(searchKey);
        if (keyPos == -1) return null;
        
        int colonPos = json.indexOf(":", keyPos);
        if (colonPos == -1) return null;
        
        int valueStart = json.indexOf("\"", colonPos);
        if (valueStart == -1) return null;
        
        int valueEnd = json.indexOf("\"", valueStart + 1);
        if (valueEnd == -1) return null;
        
        return json.substring(valueStart + 1, valueEnd);
    }

    private int findMatchingBracket(String text, int start) {
        int count = 0;
        boolean inString = false;
        
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            
            if (c == '"' && (i == 0 || text.charAt(i - 1) != '\\')) {
                inString = !inString;
            }
            
            if (!inString) {
                if (c == '{' || c == '[') count++;
                if (c == '}' || c == ']') {
                    count--;
                    if (count == 0) return i;
                }
            }
        }
        
        return text.length() - 1;
    }

    /**
     * Represents a Maven artifact found in Maven Central.
     */
    public record ArtifactInfo(
            String groupId,
            String artifactId,
            String version,
            boolean knownEquivalent,
            String notes
    ) {}

    /**
     * Represents a version recommendation for migration.
     */
    public record VersionRecommendation(
            String javaxGroupId,
            String javaxArtifactId,
            String currentVersion,
            String jakartaGroupId,
            String jakartaArtifactId,
            String recommendedVersion,
            RecommendationType type,
            String explanation
    ) {}

    /**
     * Dependency update recommendation for Maven/Gradle.
     */
    public record DependencyUpdateRecommendation(
            String javaxGroupId,
            String javaxArtifactId,
            String currentVersion,
            String jakartaGroupId,
            String jakartaArtifactId,
            String recommendedVersion,
            UpdateAction action,
            String explanation
    ) {}

    /**
     * Type of version recommendation.
     */
    public enum RecommendationType {
        KNOWN_EQUIVALENT,
        DIRECT_EQUIVALENT,
        PARTIAL_COMPATIBLE,
        NO_EQUIVALENT_FOUND
    }

    /**
     * Action to take for dependency update.
     */
    public enum UpdateAction {
        REPLACE_DEPENDENCY,
        ADD_EXCLUSION,
        SEARCH_MANUALLY,
        NO_ACTION_NEEDED
    }
}
