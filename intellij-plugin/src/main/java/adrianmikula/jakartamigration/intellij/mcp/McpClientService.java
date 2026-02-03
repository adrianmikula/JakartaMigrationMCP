package adrianmikula.jakartamigration.intellij.mcp;

import adrianmikula.jakartamigration.intellij.model.DependencyInfo;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * MCP client service interface for communicating with the Jakarta Migration MCP server
 * From TypeSpec: mcp-integration.tsp
 */
public interface McpClientService {

    /**
     * Analyze migration impact for a project
     */
    CompletableFuture<AnalyzeMigrationImpactResponse> analyzeMigrationImpact(String projectPath);

    /**
     * Detect migration blockers in a project
     */
    CompletableFuture<List<DependencyInfo>> detectBlockers(String projectPath);

    /**
     * Get version recommendations for dependencies
     */
    CompletableFuture<List<DependencyInfo>> recommendVersions(String projectPath);

    /**
     * Check if the MCP server is available
     */
    CompletableFuture<Boolean> isServerAvailable();

    /**
     * Get the server URL
     */
    String getServerUrl();
}
