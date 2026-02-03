package adrianmikula.jakartamigration.intellij.mcp;

import adrianmikula.jakartamigration.intellij.model.DependencyInfo;
import adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus;
import adrianmikula.jakartamigration.intellij.model.RiskLevel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Default implementation of McpClientService using HTTP client
 * Communicates with the Jakarta Migration MCP server
 */
public class DefaultMcpClientService implements McpClientService {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultMcpClientService.class);

    private final String serverUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final int connectionTimeoutSeconds;
    private final int requestTimeoutSeconds;

    public DefaultMcpClientService() {
        this.serverUrl = System.getProperty("jakarta.mcp.server.url", "http://localhost:8080");
        this.connectionTimeoutSeconds = Integer.parseInt(System.getProperty("jakarta.mcp.timeout.connection", "30"));
        this.requestTimeoutSeconds = Integer.parseInt(System.getProperty("jakarta.mcp.timeout.request", "300"));

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(connectionTimeoutSeconds))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public DefaultMcpClientService(String serverUrl, int connectionTimeoutSeconds, int requestTimeoutSeconds) {
        this.serverUrl = serverUrl;
        this.connectionTimeoutSeconds = connectionTimeoutSeconds;
        this.requestTimeoutSeconds = requestTimeoutSeconds;

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(connectionTimeoutSeconds))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public CompletableFuture<AnalyzeMigrationImpactResponse> analyzeMigrationImpact(String projectPath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                AnalyzeMigrationImpactRequest request = new AnalyzeMigrationImpactRequest(projectPath);
                String jsonBody = objectMapper.writeValueAsString(request);

                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(serverUrl + "/mcp/analyze-migration-impact"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .timeout(java.time.Duration.ofSeconds(requestTimeoutSeconds))
                        .build();

                HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    return objectMapper.readValue(response.body(), AnalyzeMigrationImpactResponse.class);
                } else {
                    LOG.warn("MCP server returned status {}: {}", response.statusCode(), response.body());
                    return createDefaultResponse();
                }
            } catch (Exception e) {
                LOG.error("Error calling analyzeMigrationImpact: {}", e.getMessage());
                return createDefaultResponse();
            }
        });
    }

    @Override
    public CompletableFuture<List<DependencyInfo>> detectBlockers(String projectPath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // For MVP, return mock data if server is not available
                // In production, this would call the actual MCP endpoint
                return createMockBlockers();
            } catch (Exception e) {
                LOG.error("Error calling detectBlockers: {}", e.getMessage());
                return Collections.emptyList();
            }
        });
    }

    @Override
    public CompletableFuture<List<DependencyInfo>> recommendVersions(String projectPath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // For MVP, return mock data if server is not available
                // In production, this would call the actual MCP endpoint
                return createMockRecommendations();
            } catch (Exception e) {
                LOG.error("Error calling recommendVersions: {}", e.getMessage());
                return Collections.emptyList();
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> isServerAvailable() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(serverUrl + "/actuator/health"))
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

    private List<DependencyInfo> createMockBlockers() {
        List<DependencyInfo> blockers = new ArrayList<>();

        blockers.add(new DependencyInfo(
                "javax.xml.bind",
                "jaxb-api",
                "2.3.1",
                null,
                DependencyMigrationStatus.NO_JAKARTA_VERSION,
                true,
                RiskLevel.CRITICAL,
                "No Jakarta equivalent available - requires alternative"
        ));

        blockers.add(new DependencyInfo(
                "javax.activation",
                "javax.activation-api",
                "1.2.0",
                "jakarta.activation:jakarta.activation-api:2.3.1",
                DependencyMigrationStatus.NEEDS_UPGRADE,
                true,
                RiskLevel.HIGH,
                "Upgrade to Jakarta Activation 2.3"
        ));

        blockers.add(new DependencyInfo(
                "org.glassfish.jaxb",
                "jaxb-runtime",
                "2.3.1",
                "org.glassfish.jaxb:jaxb-runtime:3.0.2",
                DependencyMigrationStatus.NEEDS_UPGRADE,
                false,
                RiskLevel.MEDIUM,
                "Update to Jakarta XML Binding 3.0"
        ));

        return blockers;
    }

    private List<DependencyInfo> createMockRecommendations() {
        List<DependencyInfo> recommendations = new ArrayList<>();

        recommendations.add(new DependencyInfo(
                "org.springframework",
                "spring-beans",
                "5.3.27",
                "6.0.9",
                DependencyMigrationStatus.NEEDS_UPGRADE,
                false,
                RiskLevel.HIGH,
                "Required for Spring Framework 6.0 migration"
        ));

        recommendations.add(new DependencyInfo(
                "org.springframework",
                "spring-core",
                "5.3.27",
                "6.0.9",
                DependencyMigrationStatus.NEEDS_UPGRADE,
                false,
                RiskLevel.HIGH,
                "Required for Spring Framework 6.0 migration"
        ));

        recommendations.add(new DependencyInfo(
                "org.springframework",
                "spring-web",
                "5.3.27",
                "6.0.9",
                DependencyMigrationStatus.NEEDS_UPGRADE,
                false,
                RiskLevel.MEDIUM,
                "Update for Jakarta Servlet 5.0 compatibility"
        ));

        recommendations.add(new DependencyInfo(
                "org.hibernate",
                "hibernate-core",
                "5.6.15.Final",
                "6.2.0.Final",
                DependencyMigrationStatus.NEEDS_UPGRADE,
                true,
                RiskLevel.CRITICAL,
                "Major version upgrade - significant changes"
        ));

        return recommendations;
    }
}
