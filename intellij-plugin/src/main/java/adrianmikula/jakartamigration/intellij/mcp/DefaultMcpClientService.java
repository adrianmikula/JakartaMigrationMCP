package adrianmikula.jakartamigration.intellij.mcp;

import adrianmikula.jakartamigration.intellij.model.DependencyInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Default implementation of McpClientService using MCP JSON-RPC protocol over HTTP.
 * Communicates with the Jakarta Migration MCP server using the MCP protocol.
 * 
 * Reference: https://modelcontextprotocol.io/docs/specification/transport
 */
public class DefaultMcpClientService implements McpClientService {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultMcpClientService.class);

    private final String serverUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final int connectionTimeoutSeconds;
    private final int requestTimeoutSeconds;
    private final AtomicLong requestId;
    private final Map<Long, CompletableFuture<String>> pendingRequests;

    public DefaultMcpClientService() {
        this.serverUrl = System.getProperty("jakarta.mcp.server.url", "http://localhost:8080");
        this.connectionTimeoutSeconds = Integer.parseInt(System.getProperty("jakarta.mcp.timeout.connection", "30"));
        this.requestTimeoutSeconds = Integer.parseInt(System.getProperty("jakarta.mcp.timeout.request", "300"));

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(connectionTimeoutSeconds))
                .build();
        this.objectMapper = new ObjectMapper();
        this.requestId = new AtomicLong(1);
        this.pendingRequests = new ConcurrentHashMap<>();
    }

    public DefaultMcpClientService(String serverUrl, int connectionTimeoutSeconds, int requestTimeoutSeconds) {
        this.serverUrl = serverUrl;
        this.connectionTimeoutSeconds = connectionTimeoutSeconds;
        this.requestTimeoutSeconds = requestTimeoutSeconds;

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(connectionTimeoutSeconds))
                .build();
        this.objectMapper = new ObjectMapper();
        this.requestId = new AtomicLong(1);
        this.pendingRequests = new ConcurrentHashMap<>();
    }

    @Override
    public CompletableFuture<String> analyzeReadiness(String projectPath) {
        LOG.info("Analyzing Jakarta readiness for project: {}", projectPath);
        return callTool("analyzeJakartaReadiness", Map.of("projectPath", projectPath))
                .thenApply(responseJson -> {
                    LOG.debug("Received readiness response: {}", responseJson);
                    return responseJson;
                })
                .exceptionally(ex -> {
                    LOG.error("analyzeReadiness call failed: {}", ex.getMessage());
                    return createErrorJson("Failed to analyze readiness: " + ex.getMessage());
                });
    }

    @Override
    public CompletableFuture<AnalyzeMigrationImpactResponse> analyzeMigrationImpact(String projectPath) {
        LOG.info("Analyzing migration impact for project: {}", projectPath);
        
        return callTool("analyzeMigrationImpact", Map.of("projectPath", projectPath))
                .thenApply(responseJson -> {
                    LOG.debug("Received response: {}", responseJson);
                    
                    try {
                        // The tool returns JSON as a string, parse it
                        JsonNode root = objectMapper.readTree(responseJson);
                        
                        // Check if this is an upgrade required response
                        if (root.has("error") && "PREMIUM_REQUIRED".equals(root.path("error").path("code").asText())) {
                            LOG.info("Premium feature requested - falling back to free analysis");
                            // Fall back to analyzeJakartaReadiness for free tier
                            return null; // Signal to use fallback
                        }
                        
                        return parseAnalyzeMigrationImpactResponse(root);
                    } catch (JsonProcessingException e) {
                        LOG.error("Failed to parse analyzeMigrationImpact response: {}", e.getMessage());
                        return createDefaultResponse();
                    }
                })
                .thenCompose(result -> {
                    // If null (premium required), fall back to free tool
                    if (result == null) {
                        LOG.info("Falling back to analyzeJakartaReadiness (free tier)");
                        return callTool("analyzeJakartaReadiness", Map.of("projectPath", projectPath))
                                .thenApply(freeResponse -> {
                                    LOG.debug("Free tier response: {}", freeResponse);
                                    try {
                                        JsonNode root = objectMapper.readTree(freeResponse);
                                        return parseAnalyzeMigrationImpactResponse(root);
                                    } catch (JsonProcessingException e) {
                                        LOG.error("Failed to parse analyzeJakartaReadiness response: {}", e.getMessage());
                                        return createDefaultResponse();
                                    }
                                });
                    }
                    return CompletableFuture.completedFuture(result);
                })
                .exceptionally(ex -> {
                    LOG.error("analyzeMigrationImpact call failed: {}", ex.getMessage());
                    return createDefaultResponse();
                });
    }

    @Override
    public CompletableFuture<List<DependencyInfo>> detectBlockers(String projectPath) {
        return callTool("detectBlockers", Map.of("projectPath", projectPath))
                .thenApply(responseJson -> {
                    try {
                        JsonNode root = objectMapper.readTree(responseJson);
                        return parseDependencyInfoList(root);
                    } catch (JsonProcessingException e) {
                        LOG.error("Failed to parse detectBlockers response: {}", e.getMessage());
                        return new ArrayList<DependencyInfo>();
                    }
                })
                .exceptionally(ex -> {
                    LOG.error("detectBlockers call failed: {}", ex.getMessage());
                    return new ArrayList<DependencyInfo>();
                });
    }

    @Override
    public CompletableFuture<List<DependencyInfo>> recommendVersions(String projectPath) {
        return callTool("recommendVersions", Map.of("projectPath", projectPath))
                .thenApply(responseJson -> {
                    try {
                        JsonNode root = objectMapper.readTree(responseJson);
                        return parseDependencyInfoList(root);
                    } catch (JsonProcessingException e) {
                        LOG.error("Failed to parse recommendVersions response: {}", e.getMessage());
                        return new ArrayList<DependencyInfo>();
                    }
                })
                .exceptionally(ex -> {
                    LOG.error("recommendVersions call failed: {}", ex.getMessage());
                    return new ArrayList<DependencyInfo>();
                });
    }

    /**
     * Call an MCP tool with the given name and arguments.
     * Uses the MCP JSON-RPC protocol over HTTP.
     */
    private CompletableFuture<String> callTool(String toolName, Map<String, Object> arguments) {
        long id = requestId.getAndIncrement();
        
        // Build JSON-RPC request
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", id);
        request.put("method", "tools/call");
        
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("name", toolName);
        params.put("arguments", arguments);
        request.put("params", params);
        
        CompletableFuture<String> future = new CompletableFuture<>();
        pendingRequests.put(id, future);
        
        try {
            String jsonBody = objectMapper.writeValueAsString(request);
            
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(java.net.URI.create(serverUrl + "/mcp/sse"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(java.time.Duration.ofSeconds(requestTimeoutSeconds))
                    .build();
            
            httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        try {
                            if (response.statusCode() == 200) {
                                String body = response.body();
                                // Parse JSON-RPC response
                                JsonNode root = objectMapper.readTree(body);
                                
                                // Check for error
                                if (root.has("error")) {
                                    JsonNode error = root.get("error");
                                    String errorMessage = error.has("message") ? error.get("message").asText() : "Unknown error";
                                    future.completeExceptionally(new RuntimeException(errorMessage));
                                    return;
                                }
                                
                                // Extract result
                                if (root.has("result")) {
                                    JsonNode result = root.get("result");
                                    // The content is typically in result.content[0].text
                                    String content = extractContentFromResult(result);
                                    future.complete(content);
                                } else {
                                    future.completeExceptionally(new RuntimeException("No result in response"));
                                }
                            } else {
                                LOG.warn("MCP server returned status {}: {}", response.statusCode(), response.body());
                                future.completeExceptionally(new RuntimeException("Server error: " + response.statusCode()));
                            }
                        } catch (JsonProcessingException e) {
                            LOG.error("Failed to parse response: {}", e.getMessage());
                            future.completeExceptionally(e);
                        } finally {
                            pendingRequests.remove(id);
                        }
                    })
                    .exceptionally(ex -> {
                        pendingRequests.remove(id);
                        future.completeExceptionally(ex);
                        return null;
                    });
            
        } catch (JsonProcessingException e) {
            pendingRequests.remove(id);
            future.completeExceptionally(e);
        }
        
        return future;
    }

    /**
     * Extract content text from MCP JSON-RPC result.
     */
    private String extractContentFromResult(JsonNode result) {
        if (result.has("content")) {
            JsonNode content = result.get("content");
            if (content.isArray() && content.size() > 0) {
                JsonNode firstContent = content.get(0);
                if (firstContent.has("text")) {
                    return firstContent.get("text").asText();
                }
            }
        }
        // If no content structure, return the whole result as string
        return result.toString();
    }

    /**
     * Parse analyzeMigrationImpact response into AnalyzeMigrationImpactResponse object.
     */
    private AnalyzeMigrationImpactResponse parseAnalyzeMigrationImpactResponse(JsonNode root) {
        AnalyzeMigrationImpactResponse response = new AnalyzeMigrationImpactResponse();
        
        try {
            // The response might be a nested JSON string
            String jsonStr = root.asText();
            if (jsonStr.startsWith("{") || jsonStr.startsWith("[")) {
                root = objectMapper.readTree(jsonStr);
            }
            
            AnalyzeMigrationImpactResponse.DependencyImpactDetails impactDetails = 
                new AnalyzeMigrationImpactResponse.DependencyImpactDetails();
            
            if (root.has("dependencyImpact")) {
                JsonNode depImpact = root.get("dependencyImpact");
                
                if (depImpact.has("affectedDependencies")) {
                    List<DependencyInfo> deps = parseDependencyInfoList(depImpact.get("affectedDependencies"));
                    impactDetails.setAffectedDependencies(deps);
                }
                
                if (depImpact.has("transitiveDependencyChanges")) {
                    impactDetails.setTransitiveDependencyChanges(depImpact.get("transitiveDependencyChanges").asInt());
                }
                
                if (depImpact.has("breakingChanges")) {
                    List<String> changes = new ArrayList<>();
                    JsonNode breakingChanges = depImpact.get("breakingChanges");
                    if (breakingChanges.isArray()) {
                        for (JsonNode change : breakingChanges) {
                            changes.add(change.asText());
                        }
                    }
                    impactDetails.setBreakingChanges(changes);
                }
            }
            
            response.setDependencyImpact(impactDetails);
            
            // Parse overall impact
            if (root.has("overallImpact")) {
                JsonNode impact = root.get("overallImpact");
                AnalyzeMigrationImpactResponse.ImpactAssessment assessment = 
                    new AnalyzeMigrationImpactResponse.ImpactAssessment();
                
                if (impact.has("level")) {
                    assessment.setLevel(impact.get("level").asText());
                }
                if (impact.has("description")) {
                    assessment.setDescription(impact.get("description").asText());
                }
                if (impact.has("riskFactors")) {
                    List<String> factors = new ArrayList<>();
                    JsonNode riskFactors = impact.get("riskFactors");
                    if (riskFactors.isArray()) {
                        for (JsonNode factor : riskFactors) {
                            factors.add(factor.asText());
                        }
                    }
                    assessment.setRiskFactors(factors);
                }
                response.setOverallImpact(assessment);
            }
            
            // Parse estimated effort
            if (root.has("estimatedEffort")) {
                JsonNode effort = root.get("estimatedEffort");
                AnalyzeMigrationImpactResponse.EffortEstimate estimate = 
                    new AnalyzeMigrationImpactResponse.EffortEstimate();
                
                if (effort.has("estimatedHours")) {
                    estimate.setEstimatedHours(effort.get("estimatedHours").asInt());
                }
                if (effort.has("confidence")) {
                    estimate.setConfidence(effort.get("confidence").asText());
                }
                response.setEstimatedEffort(estimate);
            }
            
        } catch (JsonProcessingException e) {
            LOG.error("Failed to parse analyzeMigrationImpact response: {}", e.getMessage());
        }
        
        return response;
    }

    /**
     * Parse a JSON node into a list of DependencyInfo objects.
     */
    private List<DependencyInfo> parseDependencyInfoList(JsonNode node) {
        List<DependencyInfo> dependencies = new ArrayList<>();
        
        // Handle nested string
        if (node.isTextual()) {
            try {
                node = objectMapper.readTree(node.asText());
            } catch (JsonProcessingException e) {
                LOG.error("Failed to parse nested JSON: {}", e.getMessage());
                return dependencies;
            }
        }
        
        // Handle array
        if (node.isArray()) {
            for (JsonNode item : node) {
                DependencyInfo info = parseDependencyInfo(item);
                if (info != null) {
                    dependencies.add(info);
                }
            }
        }
        
        return dependencies;
    }

    /**
     * Parse a single DependencyInfo from JSON.
     */
    private DependencyInfo parseDependencyInfo(JsonNode node) {
        if (node == null || !node.isObject()) {
            return null;
        }
        
        try {
            String groupId = node.has("groupId") ? node.get("groupId").asText() : "";
            String artifactId = node.has("artifactId") ? node.get("artifactId").asText() : "";
            String currentVersion = node.has("currentVersion") ? node.get("currentVersion").asText() : "";
            String recommendedVersion = node.has("recommendedVersion") ? node.get("recommendedVersion").asText() : null;
            
            adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus status = 
                adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus.NEEDS_UPGRADE;
            if (node.has("migrationStatus")) {
                String statusStr = node.get("migrationStatus").asText();
                status = adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus.valueOf(statusStr);
            }
            
            boolean isBlocker = node.has("isBlocker") && node.get("isBlocker").asBoolean();
            
            adrianmikula.jakartamigration.intellij.model.RiskLevel riskLevel = 
                adrianmikula.jakartamigration.intellij.model.RiskLevel.MEDIUM;
            if (node.has("riskLevel")) {
                String riskStr = node.get("riskLevel").asText();
                riskLevel = adrianmikula.jakartamigration.intellij.model.RiskLevel.valueOf(riskStr);
            }
            
            String migrationStrategy = node.has("migrationStrategy") ? node.get("migrationStrategy").asText() : "";
            
            return new DependencyInfo(groupId, artifactId, currentVersion, recommendedVersion, 
                status, isBlocker, riskLevel, migrationStrategy);
            
        } catch (Exception e) {
            LOG.error("Failed to parse DependencyInfo: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public CompletableFuture<Boolean> isServerAvailable() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(java.net.URI.create(serverUrl + "/actuator/health"))
                        .timeout(java.time.Duration.ofSeconds(5))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                return response.statusCode() == 200;
            } catch (Exception e) {
                LOG.debug("MCP server not available: {}", e.getMessage());
                return false;
            }
        });
    }

    @Override
    public String getServerUrl() {
        return serverUrl;
    }

    private AnalyzeMigrationImpactResponse createDefaultResponse() {
        AnalyzeMigrationImpactResponse response = new AnalyzeMigrationImpactResponse();
        response.setDependencyImpact(new AnalyzeMigrationImpactResponse.DependencyImpactDetails());
        response.setOverallImpact(new AnalyzeMigrationImpactResponse.ImpactAssessment());
        response.setEstimatedEffort(new AnalyzeMigrationImpactResponse.EffortEstimate());
        return response;
    }

    private String createErrorJson(String message) {
        return String.format("{\"error\":\"%s\"}", message);
    }
}
