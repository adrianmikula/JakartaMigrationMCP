package adrianmikula.jakartamigration.dependencyanalysis.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Service that looks up Jakarta EE artifact versions from Maven Central.
 * Maps javax.* artifacts to their jakarta.* equivalents.
 */
public class JakartaArtifactLookupService {
    
    private static final String MAVEN_CENTRAL_SEARCH_API = "https://search.maven.org/solrsearch/select";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    private static final String[][] KNOWN_JAVAX_TO_JAKARTA_MAPPINGS = {
            {"javax.servlet", "jakarta.servlet"},
            {"javax.annotation", "jakarta.annotation"},
            {"javax.xml.bind", "jakarta.xml.bind"},
            {"javax.ejb", "jakarta.ejb"},
            {"javax.enterprise", "jakarta.enterprise"},
            {"javax.faces", "jakarta.faces"},
            {"javax.inject", "jakarta.inject"},
            {"javax.jms", "jakarta.jms"},
            {"javax.json", "jakarta.json"},
            {"javax.mail", "jakarta.mail"},
            {"javax.persistence", "jakarta.persistence"},
            {"javax.security", "jakarta.security"},
            {"javax.transaction", "jakarta.transaction"},
            {"javax.validation", "jakarta.validation"},
            {"javax.websocket", "jakarta.websocket"},
            {"javax.ws", "jakarta.ws"},
            {"javax.xml.ws", "jakarta.xml.ws"}
    };
    
    public JakartaArtifactLookupService() {
    }
    
    /**
     * Result of an artifact lookup containing the Jakarta artifact information.
     */
    public record JakartaArtifactMatch(
            String groupId,
            String artifactId,
            String latestVersion,
            String description,
            boolean found
    ) {
        public static JakartaArtifactMatch notFound() {
            return new JakartaArtifactMatch(null, null, null, null, false);
        }
        
        public static JakartaArtifactMatch of(String groupId, String artifactId, String version, String description) {
            return new JakartaArtifactMatch(groupId, artifactId, version, description, true);
        }
    }
    
    /**
     * Looks up the Jakarta EE equivalent of a javax artifact.
     * 
     * @param javaxGroupId The javax group ID (e.g., "javax.servlet")
     * @param javaxArtifactId The javax artifact ID (e.g., "javax.servlet-api")
     * @return JakartaArtifactMatch containing the Jakarta artifact information, or notFound() if not available
     */
    public JakartaArtifactMatch lookupJakartaArtifact(String javaxGroupId, String javaxArtifactId) {
        if (javaxGroupId == null || javaxArtifactId == null) {
            return JakartaArtifactMatch.notFound();
        }
        
        String jakartaGroupId = mapGroupId(javaxGroupId);
        if (jakartaGroupId == null) {
            return JakartaArtifactMatch.notFound();
        }
        
        String jakartaArtifactId = mapArtifactId(javaxArtifactId, jakartaGroupId);
        
        return lookupInMavenCentral(jakartaGroupId, jakartaArtifactId);
    }
    
    /**
     * Maps a javax group ID to its jakarta equivalent.
     */
    private String mapGroupId(String javaxGroupId) {
        if (javaxGroupId == null) return null;
        
        for (String[] mapping : KNOWN_JAVAX_TO_JAKARTA_MAPPINGS) {
            if (javaxGroupId.equals(mapping[0])) {
                return mapping[1];
            }
        }
        
        if (javaxGroupId.startsWith("javax.")) {
            return "jakarta." + javaxGroupId.substring(7);
        }
        
        return null;
    }
    
    /**
     * Maps a javax artifact ID to its jakarta equivalent.
     */
    private String mapArtifactId(String javaxArtifactId, String jakartaGroupId) {
        if (javaxArtifactId == null) return null;
        
        String artifactId = javaxArtifactId;
        
        if (artifactId.startsWith("javax-")) {
            artifactId = "jakarta-" + artifactId.substring(6);
        } else if (artifactId.contains("javax")) {
            artifactId = artifactId.replace("javax", "jakarta");
        }
        
        return artifactId;
    }
    
    /**
     * Looks up an artifact in Maven Central.
     */
    private JakartaArtifactMatch lookupInMavenCentral(String groupId, String artifactId) {
        try {
            String query = String.format("g:%s AND a:%s", groupId, artifactId);
            String url = MAVEN_CENTRAL_SEARCH_API + "?q=" + 
                    java.net.URLEncoder.encode(query, "UTF-8") + 
                    "&rows=1&wt=json";
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            
            HttpResponse<String> response = HTTP_CLIENT.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                return parseMavenCentralResponse(response.body());
            }
            
            return JakartaArtifactMatch.notFound();
            
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return JakartaArtifactMatch.notFound();
        }
    }
    
    /**
     * Parses the Maven Central API response.
     */
    private JakartaArtifactMatch parseMavenCentralResponse(String json) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(json);
            JsonNode response = root.get("response");
            
            if (response == null || response.get("numFound").asInt(0) == 0) {
                return JakartaArtifactMatch.notFound();
            }
            
            JsonNode docs = response.get("docs");
            if (docs == null || docs.isEmpty()) {
                return JakartaArtifactMatch.notFound();
            }
            
            JsonNode doc = docs.get(0);
            String g = doc.has("g") ? doc.get("g").asText() : null;
            String a = doc.has("a") ? doc.get("a").asText() : null;
            String latestVersion = doc.has("latestVersion") ? doc.get("latestVersion").asText() : null;
            String desc = null;
            if (doc.has("ec")) {
                JsonNode ec = doc.get("ec");
                java.util.Iterator<JsonNode> elements = ec.elements();
                java.util.List<String> ecList = new java.util.ArrayList<>();
                while (elements.hasNext()) {
                    ecList.add(elements.next().asText());
                }
                desc = String.join(", ", ecList);
            }
            
            if (g != null && a != null && latestVersion != null) {
                return JakartaArtifactMatch.of(g, a, latestVersion, desc);
            }
            
            return JakartaArtifactMatch.notFound();
            
        } catch (IOException e) {
            return JakartaArtifactMatch.notFound();
        }
    }
    
    /**
     * Checks if a group ID has a known jakarta equivalent.
     */
    public boolean hasJakartaEquivalent(String javaxGroupId) {
        return mapGroupId(javaxGroupId) != null;
    }
    
    /**
     * Gets all known javax to jakarta group ID mappings.
     */
    public List<String[]> getKnownMappings() {
        List<String[]> mappings = new ArrayList<>();
        for (String[] mapping : KNOWN_JAVAX_TO_JAKARTA_MAPPINGS) {
            mappings.add(mapping.clone());
        }
        return Collections.unmodifiableList(mappings);
    }
}
