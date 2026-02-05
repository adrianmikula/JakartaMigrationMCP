/*
 * Copyright © 2026 Adrian Mikula
 *
 * All rights reserved.
 *
 * This software is proprietary and may not be used, copied,
 * modified, or distributed except under the terms of a
 * separate commercial license agreement.
 */
package adrianmikula.jakartamigration.intellij.mcp;

import adrianmikula.jakartamigration.intellij.model.DependencyInfo;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * MCP client service interface for communicating with the Jakarta Migration MCP server.
 * From TypeSpec: mcp-integration.tsp
 */
public interface McpClientService {

    /**
     * Analyze Jakarta migration readiness for a project.
     * @param projectPath Path to the project root directory
     * @return CompletableFuture containing JSON response with readiness data
     */
    CompletableFuture<String> analyzeReadiness(String projectPath);

    /**
     * Analyze migration impact for a project.
     * @param projectPath Path to the project root directory
     * @return CompletableFuture containing the migration impact analysis response
     */
    CompletableFuture<AnalyzeMigrationImpactResponse> analyzeMigrationImpact(String projectPath);

    /**
     * Detect migration blockers in a project.
     * @param projectPath Path to the project root directory
     * @return CompletableFuture containing list of detected blockers
     */
    CompletableFuture<List<DependencyInfo>> detectBlockers(String projectPath);

    /**
     * Get version recommendations for dependencies.
     * @param projectPath Path to the project root directory
     * @return CompletableFuture containing list of version recommendations
     */
    CompletableFuture<List<DependencyInfo>> recommendVersions(String projectPath);

    /**
     * Check if the MCP server is available.
     * @return CompletableFuture containing availability status
     */
    CompletableFuture<Boolean> isServerAvailable();

    /**
     * Get the server URL.
     * @return The MCP server URL
     */
    String getServerUrl();

    /**
     * Apply OpenRewrite refactoring for javax to jakarta migration.
     * @param projectPath Path to the project root directory
     * @param filePatterns List of file patterns to refactor
     * @return CompletableFuture containing JSON result with refactoring changes
     */
    CompletableFuture<String> applyOpenRewriteRefactoring(String projectPath, List<String> filePatterns);

    /**
     * Scan a binary dependency for Jakarta EE compatibility issues.
     * @param jarPath Path to the JAR file
     * @return CompletableFuture containing JSON scan results
     */
    CompletableFuture<String> scanBinaryDependency(String jarPath);

    /**
     * Update a dependency to the recommended version.
     * @param projectPath Path to the project root directory
     * @param groupId Dependency group ID
     * @param artifactId Dependency artifact ID
     * @param currentVersion Current version
     * @param recommendedVersion Recommended version
     * @return CompletableFuture containing JSON result with update status
     */
    CompletableFuture<String> updateDependency(
            String projectPath,
            String groupId,
            String artifactId,
            String currentVersion,
            String recommendedVersion
    );
}
