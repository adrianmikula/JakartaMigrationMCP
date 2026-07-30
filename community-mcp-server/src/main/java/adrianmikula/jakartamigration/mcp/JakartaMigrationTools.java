package adrianmikula.jakartamigration.mcp;

import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyAnalysisReport;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyAnalysisModule;
import adrianmikula.jakartamigration.config.FeatureFlagsService;
import adrianmikula.jakartamigration.mcp.util.JsonUtils;
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

    // Premium tools dependencies
    private final DependencyAnalysisModule dependencyAnalysisModule;
    private final FeatureFlagsService featureFlags;

    public JakartaMigrationTools(
            DependencyAnalysisModule dependencyAnalysisModule,
            FeatureFlagsService featureFlags) {
        this.dependencyAnalysisModule = dependencyAnalysisModule;
        this.featureFlags = featureFlags;
    }

    // === COMMUNITY TOOLS (Delegated to CommunityMigrationTools) ===
    // Note: scanForJavaxBasic, analyzeJakartaReadiness, detectBlockers, and recommendVersions
    // are registered via CommunityMigrationTools @McpTool annotations.
    // Only listDependenciesCompatibility is unique to this class.

    /**
     * Lists dependency compatibility information for Jakarta migration.
     * COMMUNITY TOOL - Free to use under Apache License 2.0
     * @deprecated Community dependency compatibility is deprecated. Use the premium scan endpoint.
     */
    @Deprecated
    @McpTool(name = "listDependenciesCompatibility", description = "Lists dependency compatibility information for Jakarta migration. Returns JSON with compatibility matrix and migration paths.")
    public String listDependenciesCompatibility(
            @McpToolParam(description = "Path to project root directory", required = true) String projectPath) {
        return JsonUtils.createErrorResponse(
                "Community scanning tools are deprecated. " +
                        "Please use the Premium scan endpoint for source-first Jakarta migration scans. " +
                        "Requested project: " + projectPath);
    }
}
