package adrianmikula.jakartamigration.mcp;

import adrianmikula.jakartamigration.analysis.persistence.SqliteMigrationAnalysisStore;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyAnalysisReport;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyAnalysisModule;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphException;
import adrianmikula.jakartamigration.config.FeatureFlag;
import adrianmikula.jakartamigration.config.FeatureFlagsProperties;
import adrianmikula.jakartamigration.config.FeatureFlagsService;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * MCP Tools for Jakarta Migration.
 * 
 * This class combines both Community and Premium tools.
 * Community tools are delegated to {@link CommunityMigrationTools}.
 * Premium tools require a JetBrains Marketplace license.
 * 
 * Note: Code refactoring tools (applyAutoFixes, executeMigrationPlan) have been
 * temporarily disabled pending reimplementation via RecipeService (see
 * REFACTOR.md).
 * 
 * License Tiers:
 * - COMMUNITY (Free): analyzeJakartaReadiness, detectBlockers,
 * recommendVersions
 * - PREMIUM ($49/mo or $399/yr): All tools including auto-fixes, one-click
 * refactor
 */
@Component
@Slf4j
public class JakartaMigrationTools {

    // Community tools delegate
    private final CommunityMigrationTools communityTools;

    // Premium tools dependencies
    private final DependencyAnalysisModule dependencyAnalysisModule;
    private final FeatureFlagsService featureFlags;
    private final adrianmikula.jakartamigration.sourcecodescanning.service.SourceCodeScanner sourceCodeScanner;

    public JakartaMigrationTools(
            CommunityMigrationTools communityTools,
            DependencyAnalysisModule dependencyAnalysisModule,
            FeatureFlagsService featureFlags,
            adrianmikula.jakartamigration.sourcecodescanning.service.SourceCodeScanner sourceCodeScanner) {
        this.communityTools = communityTools;
        this.dependencyAnalysisModule = dependencyAnalysisModule;
        this.featureFlags = featureFlags;
        this.sourceCodeScanner = sourceCodeScanner;
    }

    // === COMMUNITY TOOLS (Delegated) ===

    /**
     * Analyzes a Java project for Jakarta migration readiness.
     * COMMUNITY TOOL - Free to use under Apache License 2.0
     */
    @McpTool(name = "analyzeJakartaReadiness", description = "Analyzes a Java project for Jakarta migration readiness. Returns a JSON report with readiness score, blockers, and recommendations.")
    public String analyzeJakartaReadiness(
            @McpToolParam(description = "Path to the project root directory", required = true) String projectPath) {
        return communityTools.analyzeJakartaReadiness(projectPath);
    }

    /**
     * Detects blockers that prevent Jakarta migration.
     * COMMUNITY TOOL - Free to use under Apache License 2.0
     */
    @McpTool(name = "detectBlockers", description = "Detects blockers that prevent Jakarta migration. Returns a JSON list of blockers with types, reasons, and mitigation strategies.")
    public String detectBlockers(
            @McpToolParam(description = "Path to the project root directory", required = true) String projectPath) {
        return communityTools.detectBlockers(projectPath);
    }

    /**
     * Recommends Jakarta-compatible versions for dependencies.
     * COMMUNITY TOOL - Free to use under Apache License 2.0
     */
    @McpTool(name = "recommendVersions", description = "Recommends Jakarta-compatible versions for project dependencies. Returns a JSON list of version recommendations with migration paths and compatibility scores.")
    public String recommendVersions(
            @McpToolParam(description = "Path to the project root directory", required = true) String projectPath) {
        return communityTools.recommendVersions(projectPath);
    }

    // === PREMIUM TOOLS ===

    /**
     * Creates a migration plan for Jakarta migration.
     * PREMIUM TOOL - Requires JetBrains Marketplace subscription ($49/mo or
     * $399/yr)
     */
    @McpTool(name = "createMigrationPlan", description = "Creates a comprehensive migration plan for Jakarta migration. Returns a JSON plan with phases, estimated duration, and risk assessment. Requires PREMIUM license.")
    public String createMigrationPlan(
            @McpToolParam(description = "Path to the project root directory", required = true) String projectPath) {
        // Check premium license
        if (!featureFlags.hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM)) {
            return createUpgradeRequiredResponse(
                    FeatureFlag.ONE_CLICK_REFACTOR,
                    "The 'createMigrationPlan' tool requires a PREMIUM license.");
        }

        try {
            log.info("Creating migration plan for project: {}", projectPath);

            Path project = Paths.get(projectPath);
            if (!Files.exists(project) || !Files.isDirectory(project)) {
                return createErrorResponse("Project path does not exist or is not a directory: " + projectPath);
            }

            // Analyze project
            DependencyAnalysisReport report = dependencyAnalysisModule.analyzeProject(project);

            // Save to SQLite for shared access between MCP and UI
            try (SqliteMigrationAnalysisStore store = new SqliteMigrationAnalysisStore(project)) {
                store.saveAnalysisReport(project, report);
                log.info("Saved analysis report to SQLite store: {}", store.getDbPath());
            }

            // TODO: Migration plan creation pending RecipeService reimplementation (see
            // REFACTOR.md)
            return createErrorResponse(
                    "Migration plan creation is being reimplemented. Use the Refactor tab in the IntelliJ plugin.");

        } catch (DependencyGraphException e) {
            log.error("Failed to create migration plan: {}", e.getMessage(), e);
            return createErrorResponse("Failed to create migration plan: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during migration plan creation", e);
            return createErrorResponse("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Analyzes full migration impact combining dependency analysis and source code
     * scanning.
     * PREMIUM TOOL - Requires JetBrains Marketplace subscription
     */
    @McpTool(name = "analyzeMigrationImpact", description = "Analyzes full migration impact combining dependency analysis and source code scanning. Returns a comprehensive summary. Requires PREMIUM license.")
    public String analyzeMigrationImpact(
            @McpToolParam(description = "Path to the project root directory", required = true) String projectPath) {
        if (!featureFlags.hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM)) {
            return createUpgradeRequiredResponse(
                    FeatureFlag.ADVANCED_ANALYSIS,
                    "The 'analyzeMigrationImpact' tool requires a PREMIUM license.");
        }

        try {
            log.info("Analyzing migration impact for project: {}", projectPath);

            Path project = Paths.get(projectPath);
            if (!Files.exists(project) || !Files.isDirectory(project)) {
                return createErrorResponse("Project path does not exist or is not a directory: " + projectPath);
            }

            // Run dependency analysis
            DependencyAnalysisReport depReport = dependencyAnalysisModule.analyzeProject(project);

            // Run source code scan
            var scanResult = sourceCodeScanner.scanProject(project);

            // Build response from available data
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"status\": \"success\",\n");
            json.append("  \"edition\": \"premium\",\n");
            json.append("  \"readinessScore\": ").append(depReport.readinessScore().score()).append(",\n");
            json.append("  \"readinessMessage\": \"").append(escapeJson(depReport.readinessScore().explanation()))
                    .append("\",\n");
            json.append("  \"totalBlockers\": ").append(depReport.blockers().size()).append(",\n");
            json.append("  \"totalRecommendations\": ").append(depReport.recommendations().size()).append(",\n");
            json.append("  \"totalDependencies\": ").append(depReport.dependencyGraph().nodeCount()).append(",\n");
            json.append("  \"totalFilesScanned\": ").append(scanResult.totalFilesScanned()).append(",\n");
            json.append("  \"riskScore\": ").append(depReport.riskAssessment().riskScore()).append("\n");
            json.append("}");
            return json.toString();

        } catch (Exception e) {
            log.error("Unexpected error during migration impact analysis", e);
            return createErrorResponse("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Verifies runtime execution of a migrated application.
     * PREMIUM TOOL
     */
    @McpTool(name = "verifyRuntime", description = "Verifies runtime execution of a migrated Jakarta application. Requires PREMIUM license.")
    public String verifyRuntime(
            @McpToolParam(description = "Path to the JAR file to execute", required = true) String jarPath,
            @McpToolParam(description = "Optional timeout in seconds (default: 30)", required = false) Integer timeoutSeconds) {
        return createUpgradeRequiredResponse(
                FeatureFlag.BINARY_FIXES,
                "The 'verifyRuntime' tool requires a PREMIUM license.");
    }

    /**
     * Applies automatic fixes to Jakarta migration issues.
     * PREMIUM TOOL - Currently being reimplemented via RecipeService
     */
    @McpTool(name = "applyAutoFixes", description = "Applies automatic Jakarta migration fixes. Requires PREMIUM license.")
    public String applyAutoFixes(
            @McpToolParam(description = "Path to the project root directory", required = true) String projectPath,
            @McpToolParam(description = "Optional list of relative file paths to fix", required = false) List<String> files,
            @McpToolParam(description = "Optional flag for dry run (default: false)", required = false) Boolean dryRun) {
        if (!featureFlags.hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM)) {
            return createUpgradeRequiredResponse(
                    FeatureFlag.AUTO_FIXES,
                    "The 'applyAutoFixes' tool requires a PREMIUM license.");
        }

        // TODO: Reimplement via RecipeService (see REFACTOR.md)
        return createErrorResponse("Auto-fixes are being reimplemented. Use the Refactor tab in the IntelliJ plugin.");
    }

    /**
     * Executes a comprehensive Jakarta migration plan.
     * PREMIUM TOOL - Currently being reimplemented via RecipeService
     */
    @McpTool(name = "executeMigrationPlan", description = "Executes a complete Jakarta migration plan. Requires PREMIUM license.")
    public String executeMigrationPlan(
            @McpToolParam(description = "Path to the project root directory", required = true) String projectPath) {
        if (!featureFlags.hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM)) {
            return createUpgradeRequiredResponse(
                    FeatureFlag.ONE_CLICK_REFACTOR,
                    "The 'executeMigrationPlan' tool requires a PREMIUM license.");
        }

        // TODO: Reimplement via RecipeService (see REFACTOR.md)
        return createErrorResponse(
                "Migration plan execution is being reimplemented. Use the Refactor tab in the IntelliJ plugin.");
    }

    // === Response Builder Methods ===

    private String createErrorResponse(String message) {
        return "{\n" +
                "  \"status\": \"error\",\n" +
                "  \"message\": \"" + escapeJson(message) + "\"\n" +
                "}";
    }

    private String createUpgradeRequiredResponse(FeatureFlag flag, String message) {
        FeatureFlagsService.UpgradeInfo upgradeInfo = featureFlags.getUpgradeInfo(flag);
        FeatureFlagsProperties.LicenseTier currentTier = featureFlags.getCurrentTier();

        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"status\": \"upgrade_required\",\n");
        json.append("  \"edition\": \"premium\",\n");
        json.append("  \"message\": \"").append(escapeJson(message)).append("\",\n");
        json.append("  \"featureName\": \"").append(escapeJson(upgradeInfo.getFeatureName())).append("\",\n");
        json.append("  \"featureDescription\": \"").append(escapeJson(upgradeInfo.getFeatureDescription()))
                .append("\",\n");
        json.append("  \"currentTier\": \"").append(currentTier).append("\",\n");
        json.append("  \"requiredTier\": \"").append(upgradeInfo.getRequiredTier()).append("\",\n");
        json.append("  \"upgradeMessage\": \"").append(escapeJson(upgradeInfo.getMessage())).append("\"\n");
        json.append("}");

        return json.toString();
    }

    private String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
