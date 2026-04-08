package adrianmikula.jakartamigration.mcp;

import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyAnalysisReport;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyAnalysisModule;
import adrianmikula.jakartamigration.config.FeatureFlagsService;
import adrianmikula.jakartamigration.mcp.util.JsonUtils;
import adrianmikula.jakartamigration.mcp.CommunityMigrationTools;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * MCP Tools for Jakarta Migration.
 * 
 * This class combines both Community and Premium tools.
 * Community tools are delegated to CommunityMigrationTools.
 * Premium tools require a JetBrains Marketplace license.
 * 
 * License Tiers:
 * - COMMUNITY (Free): analyzeJakartaReadiness, recommendVersions, listDependenciesCompatibility
 * - PREMIUM ($49/mo or $399/yr): All tools including auto-fixes, one-click refactor
 */
@Component
public class JakartaMigrationTools {

    private static final Logger log = LoggerFactory.getLogger(JakartaMigrationTools.class);

    // Community tools delegate
    private final CommunityMigrationTools communityTools;

    // Premium tools dependencies
    private final DependencyAnalysisModule dependencyAnalysisModule;
    private final FeatureFlagsService featureFlags;

    public JakartaMigrationTools(
            CommunityMigrationTools communityTools,
            DependencyAnalysisModule dependencyAnalysisModule,
            FeatureFlagsService featureFlags) {
        this.communityTools = communityTools;
        this.dependencyAnalysisModule = dependencyAnalysisModule;
        this.featureFlags = featureFlags;
    }

    // === COMMUNITY TOOLS (Delegated) ===

    /**
     * Scans for Jakarta EE usage with basic analysis capabilities.
     * COMMUNITY TOOL - Free to use under Apache License 2.0
     */
    @McpTool(name = "scanForJavaxBasic", description = "Performs basic Jakarta EE usage scanning with source code, dependencies, and configuration file analysis. Returns findings with migration recommendations.")
    public String scanForJavaxBasic(
            @McpToolParam(description = "Path to project root directory", required = true) String projectPath,
            @McpToolParam(description = "Scan types to run (source, dependencies, config)", required = false) String scanTypes) {
        try {
            log.info("Performing basic Jakarta EE scanning for project: {}", projectPath);

            Path project = Paths.get(projectPath);
            if (!Files.exists(project) || !Files.isDirectory(project)) {
            return JsonUtils.createErrorResponse("Project path does not exist or is not a directory: " + projectPath);
            }

            // Default to basic scan types if not specified
            if (scanTypes == null || scanTypes.trim().isEmpty()) {
                scanTypes = "source,dependencies,config";
            }

            // Delegate to CommunityMigrationTools for the actual scanning logic
            return communityTools.scanForJavaxBasic(projectPath, scanTypes);

        } catch (Exception e) {
            log.error("Unexpected error during basic Jakarta EE scanning", e);
            return JsonUtils.createErrorResponse("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Recommends Jakarta-compatible versions for dependencies.
     * COMMUNITY TOOL - Free to use under Apache License 2.0
     */
    @McpTool(name = "recommendVersions", description = "Recommends Jakarta-compatible versions for project dependencies. Returns a JSON list of version recommendations with migration paths and compatibility scores.")
    public String recommendVersions(
            @McpToolParam(description = "Path to project root directory", required = true) String projectPath) {
        return communityTools.recommendVersions(projectPath);
    }

    /**
     * Lists dependency compatibility information for Jakarta migration.
     * COMMUNITY TOOL - Free to use under Apache License 2.0
     */
    @McpTool(name = "listDependenciesCompatibility", description = "Lists dependency compatibility information for Jakarta migration. Returns JSON with compatibility matrix and migration paths.")
    public String listDependenciesCompatibility(
            @McpToolParam(description = "Path to project root directory", required = true) String projectPath) {
        try {
            log.info("Listing dependency compatibility for project: {}", projectPath);

            Path project = Paths.get(projectPath);
            if (!Files.exists(project) || !Files.isDirectory(project)) {
                return JsonUtils.createErrorResponse("Project path does not exist or is not a directory: " + projectPath);
            }

            // Run dependency analysis
            DependencyAnalysisReport report = dependencyAnalysisModule.analyzeProject(project);

            // Build compatibility response
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"status\": \"success\",\n");
            json.append("  \"edition\": \"community\",\n");
            json.append("  \"totalDependencies\": ").append(report.dependencyGraph().nodeCount()).append(",\n");
            json.append("  \"compatibleCount\": ").append(report.dependencyGraph().getNodes().stream()
                .filter(node -> node.isJakartaCompatible()).count()).append(",\n");
            json.append("  \"incompatibleCount\": ").append(report.dependencyGraph().getNodes().stream()
                .filter(node -> !node.isJakartaCompatible()).count()).append("\n");
            json.append("}");

            return json.toString();

        } catch (Exception e) {
            log.error("Unexpected error during dependency compatibility listing", e);
            return JsonUtils.createErrorResponse("Unexpected error: " + e.getMessage());
        }
    }
}
