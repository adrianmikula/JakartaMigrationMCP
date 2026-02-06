package adrianmikula.jakartamigration.intellij.mcp;

import adrianmikula.jakartamigration.intellij.model.DependencyInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for McpClientService with the backend MCP server
 * Requires MCP server to be running on localhost:8080
 *
 * Set environment variable MCP_SERVER_URL to override default server URL
 * Set SKIP_MCP_INTEGRATION_TESTS=true to skip these tests
 */
public class McpClientServiceIntegrationTest {

    private McpClientService mcpClient;
    private String serverUrl;

    @BeforeEach
    void setUp() {
        // Allow skipping these tests via environment variable
        if ("true".equals(System.getenv("SKIP_MCP_INTEGRATION_TESTS"))) {
            return;
        }

        serverUrl = System.getenv("MCP_SERVER_URL");
        if (serverUrl == null || serverUrl.isEmpty()) {
            serverUrl = "http://localhost:8080";
        }

        mcpClient = new DefaultMcpClientService(serverUrl, 10, 60);
    }

    @Test
    @DisplayName("MCP server should be available")
    @EnabledIfEnvironmentVariable(named = "SKIP_MCP_INTEGRATION_TESTS", matches = "^(?!true).*$")
    void testServerAvailability() {
        if ("true".equals(System.getenv("SKIP_MCP_INTEGRATION_TESTS"))) {
            return;
        }

        CompletableFuture<Boolean> future = mcpClient.isServerAvailable();

        try {
            Boolean available = future.get(10, TimeUnit.SECONDS);
            assertThat(available)
                .as("MCP server should be available at %s", serverUrl)
                .isTrue();
        } catch (Exception e) {
            fail("MCP server is not available at " + serverUrl + ". Start the server with: ./gradlew :mcp-server:run");
        }
    }

    @Test
    @DisplayName("analyzeMigrationImpact should return valid response structure")
    @EnabledIfEnvironmentVariable(named = "SKIP_MCP_INTEGRATION_TESTS", matches = "^(?!true).*$")
    void testAnalyzeMigrationImpact() {
        if ("true".equals(System.getenv("SKIP_MCP_INTEGRATION_TESTS"))) {
            return;
        }

        String projectPath = System.getProperty("user.dir");

        CompletableFuture<AnalyzeMigrationImpactResponse> future =
            mcpClient.analyzeMigrationImpact(projectPath);

        try {
            AnalyzeMigrationImpactResponse response = future.get(60, TimeUnit.SECONDS);

            assertThat(response)
                .as("Response should not be null")
                .isNotNull();

            assertThat(response.getOverallImpact())
                .as("Overall impact should not be null")
                .isNotNull();

            assertThat(response.getDependencyImpact())
                .as("Dependency impact should not be null")
                .isNotNull();

            System.out.println("Migration impact analysis completed successfully");
            System.out.println("Impact level: " + response.getOverallImpact().getLevel());
            System.out.println("Affected dependencies: " +
                (response.getDependencyImpact().getAffectedDependencies() != null ?
                    response.getDependencyImpact().getAffectedDependencies().size() : 0));

        } catch (Exception e) {
            fail("Failed to analyze migration impact: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("detectBlockers should return list of blocking dependencies")
    @EnabledIfEnvironmentVariable(named = "SKIP_MCP_INTEGRATION_TESTS", matches = "^(?!true).*$")
    void testDetectBlockers() {
        if ("true".equals(System.getenv("SKIP_MCP_INTEGRATION_TESTS"))) {
            return;
        }

        String projectPath = System.getProperty("user.dir");

        CompletableFuture<List<DependencyInfo>> future = mcpClient.detectBlockers(projectPath);

        try {
            List<DependencyInfo> blockers = future.get(60, TimeUnit.SECONDS);

            assertThat(blockers)
                .as("Blockers list should not be null")
                .isNotNull();

            System.out.println("Found " + blockers.size() + " blockers");
            blockers.stream()
                .filter(DependencyInfo::isBlocker)
                .forEach(b -> System.out.println("  Blocker: " + b.getDisplayName()));

        } catch (Exception e) {
            fail("Failed to detect blockers: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("recommendVersions should return version recommendations")
    @EnabledIfEnvironmentVariable(named = "SKIP_MCP_INTEGRATION_TESTS", matches = "^(?!true).*$")
    void testRecommendVersions() {
        if ("true".equals(System.getenv("SKIP_MCP_INTEGRATION_TESTS"))) {
            return;
        }

        String projectPath = System.getProperty("user.dir");

        CompletableFuture<List<DependencyInfo>> future = mcpClient.recommendVersions(projectPath);

        try {
            List<DependencyInfo> recommendations = future.get(60, TimeUnit.SECONDS);

            assertThat(recommendations)
                .as("Recommendations list should not be null")
                .isNotNull();

            System.out.println("Found " + recommendations.size() + " version recommendations");
            recommendations.stream()
                .limit(5)
                .forEach(r -> System.out.println("  " + r.getDisplayName() + ": " +
                    r.getCurrentVersion() + " -> " + r.getRecommendedVersion()));

        } catch (Exception e) {
            fail("Failed to get version recommendations: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("McpClientService should return correct server URL")
    void testServerUrl() {
        assertThat(mcpClient.getServerUrl())
            .as("Server URL should be configured")
            .isNotNull()
            .isNotEmpty();
    }
}
