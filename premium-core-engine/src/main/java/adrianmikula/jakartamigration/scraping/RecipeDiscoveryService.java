package adrianmikula.jakartamigration.scraping;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for discovering Jakarta migration recipes from external APIs.
 * PREMIUM FEATURE - Available only in premium edition.
 * 
 * This service implements the approaches documented in docs/roadmap/scraping-all-jakarta-migration-recipes.md
 */
public class RecipeDiscoveryService {
    
    private static final Logger log = LoggerFactory.getLogger(RecipeDiscoveryService.class);
    
    private static final String MODERNE_API_URL = "https://api.moderne.io/query";
    private static final String GITHUB_API_URL = "https://api.github.com/repos/openrewrite/rewrite-migrate-java/contents/src/main/resources/META-INF/rewrite";
    private static final String MAVEN_SEARCH_URL = "https://search.maven.org/solrsearch/select";
    
    private final ObjectMapper objectMapper;
    
    public RecipeDiscoveryService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    /**
     * Fetches Jakarta migration recipes from the Moderne GraphQL API.
     * 
     * @return List of recipe metadata
     */
    public List<RecipeMetadata> fetchJakartaRecipesFromModerne() {
        log.info("Fetching Jakarta recipes from Moderne API");
        
        try {
            String graphqlQuery = """
                query GetJakartaRecipes {
                  recipes(filter: "jakarta") {
                    name
                    displayName
                    description
                    recipeList {
                      name
                    }
                  }
                }
                """;
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("query", graphqlQuery);
            
            String jsonRequest = objectMapper.writeValueAsString(requestBody);
            String jsonResponse = makeHttpRequest(MODERNE_API_URL, "POST", jsonRequest);
            
            List<RecipeMetadata> recipes = new ArrayList<>();
            if (jsonResponse != null) {
                JsonNode response = objectMapper.readTree(jsonResponse);
                if (response.has("data")) {
                    JsonNode recipesNode = response.get("data").get("recipes");
                    for (JsonNode recipeNode : recipesNode) {
                        RecipeMetadata metadata = new RecipeMetadata(
                            recipeNode.get("name").asText(),
                            recipeNode.get("displayName").asText(),
                            recipeNode.get("description").asText(),
                            LocalDateTime.now()
                        );
                        recipes.add(metadata);
                    }
                }
            }
            
            log.info("Successfully fetched {} Jakarta recipes from Moderne API", recipes.size());
            return recipes;
            
        } catch (Exception e) {
            log.error("Failed to fetch recipes from Moderne API", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Fetches raw recipe definitions from GitHub API.
     * 
     * @return List of raw recipe mappings
     */
    public List<RecipeMapping> fetchRecipeMappingsFromGitHub() {
        log.info("Fetching recipe mappings from GitHub API");
        
        try {
            // Fetch the directory contents first
            String directoryResponse = makeHttpRequest(GITHUB_API_URL, "GET", null);
            
            List<RecipeMapping> mappings = new ArrayList<>();
            
            if (directoryResponse != null) {
                JsonNode directory = objectMapper.readTree(directoryResponse);
                if (directory.isArray()) {
                    for (JsonNode fileNode : directory) {
                        String fileName = fileNode.get("name").asText();
                        if (fileName.contains("jakarta") && fileName.endsWith(".yml")) {
                            String downloadUrl = fileNode.get("download_url").asText();
                            String yamlContent = makeHttpRequest(downloadUrl, "GET", null);
                            
                            // Parse YAML content to extract mappings
                            List<RecipeMapping> fileMappings = parseYamlRecipe(yamlContent, fileName);
                            mappings.addAll(fileMappings);
                        }
                    }
                }
            }
            
            log.info("Successfully parsed {} recipe mappings from GitHub", mappings.size());
            return mappings;
            
        } catch (Exception e) {
            log.error("Failed to fetch recipe mappings from GitHub API", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Discovers new Jakarta artifacts from Maven Central.
     * 
     * @return List of newly discovered Jakarta artifacts
     */
    public List<JakartaArtifact> discoverNewJakartaArtifacts() {
        log.info("Discovering new Jakarta artifacts from Maven Central");
        
        try {
            String query = String.format("%s?q=g:jakarta.*&rows=50&sort=timestamp desc", MAVEN_SEARCH_URL);
            String response = makeHttpRequest(query, "GET", null);
            
            List<JakartaArtifact> artifacts = new ArrayList<>();
            
            if (response != null) {
                JsonNode jsonResponse = objectMapper.readTree(response);
                if (jsonResponse.has("response")) {
                    JsonNode docs = jsonResponse.get("response").get("docs");
                    for (JsonNode doc : docs) {
                        JakartaArtifact artifact = new JakartaArtifact(
                            doc.get("g").asText(), // groupId
                            doc.get("a").asText(), // artifactId
                            doc.get("v").asText(), // version
                            doc.get("timestamp").asLong()
                        );
                        artifacts.add(artifact);
                    }
                }
            }
            
            log.info("Successfully discovered {} Jakarta artifacts", artifacts.size());
            return artifacts;
            
        } catch (Exception e) {
            log.error("Failed to discover Jakarta artifacts from Maven Central", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Comprehensive discovery that combines all approaches.
     */
    public DiscoveryResult discoverAllJakartaRecipes() {
        log.info("Starting comprehensive Jakarta recipe discovery");
        
        List<RecipeMetadata> moderneRecipes = fetchJakartaRecipesFromModerne();
        List<RecipeMapping> githubMappings = fetchRecipeMappingsFromGitHub();
        List<JakartaArtifact> newArtifacts = discoverNewJakartaArtifacts();
        
        DiscoveryResult result = new DiscoveryResult(moderneRecipes, githubMappings, newArtifacts, LocalDateTime.now());
        
        log.info("Discovery completed: {} recipes, {} mappings, {} new artifacts", 
                moderneRecipes.size(), githubMappings.size(), newArtifacts.size());
        
        return result;
    }
    
    /**
     * Makes HTTP requests to external APIs.
     */
    private String makeHttpRequest(String urlString, String method, String body) {
        try {
            URL url = URI.create(urlString).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "Jakarta-Migration-MCP/1.0");
            
            if (body != null && !body.isEmpty()) {
                connection.setDoOutput(true);
                connection.getOutputStream().write(body.getBytes());
            }
            
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                return response.toString();
            } else {
                log.warn("HTTP request failed with code: {}", responseCode);
                return null;
            }
        } catch (Exception e) {
            log.error("HTTP request failed", e);
            return null;
        }
    }
    
    /**
     * Parses YAML recipe content to extract package and dependency mappings.
     */
    private List<RecipeMapping> parseYamlRecipe(String yamlContent, String fileName) {
        List<RecipeMapping> mappings = new ArrayList<>();
        
        if (yamlContent == null) return mappings;
        
        // Simple YAML parsing for ChangePackage and ChangeDependency patterns
        String[] lines = yamlContent.split("\n");
        String currentOldPackage = null;
        String currentNewPackage = null;
        
        for (String line : lines) {
            line = line.trim();
            
            if (line.startsWith("oldPackageName:")) {
                currentOldPackage = line.substring(line.indexOf(":") + 1).trim().replace("\"", "");
            } else if (line.startsWith("newPackageName:")) {
                currentNewPackage = line.substring(line.indexOf(":") + 1).trim().replace("\"", "");
                
                // Create mapping when both old and new packages are found
                if (currentOldPackage != null && currentNewPackage != null) {
                    mappings.add(new RecipeMapping(currentOldPackage, currentNewPackage, fileName));
                    currentOldPackage = null;
                    currentNewPackage = null;
                }
            }
        }
        
        return mappings;
    }
    
    // Record classes for structured data
    
    public record RecipeMetadata(String name, String displayName, String description, LocalDateTime discoveredAt) {}
    
    public record RecipeMapping(String oldPackage, String newPackage, String sourceFile) {}
    
    public record JakartaArtifact(String groupId, String artifactId, String version, long timestamp) {}
    
    public record DiscoveryResult(
        List<RecipeMetadata> recipes,
        List<RecipeMapping> mappings, 
        List<JakartaArtifact> newArtifacts,
        LocalDateTime discoveryTime
    ) {}
}
