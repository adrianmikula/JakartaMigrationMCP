package adrianmikula.jakartamigration.mcp;

import adrianmikula.jakartamigration.dependencyanalysis.domain.*;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyAnalysisModule;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphBuilder;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphException;
import adrianmikula.jakartamigration.config.FeatureFlag;
import adrianmikula.jakartamigration.config.FeatureFlagsService;
import adrianmikula.jakartamigration.config.FeatureFlagsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MCP Tools for Jakarta Migration (free build).
 * Exposes free tools only; pro tools (createMigrationPlan, analyzeMigrationImpact, verifyRuntime)
 * return upgrade_required. Premium build overrides this class with full implementations.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JakartaMigrationTools {

    private final DependencyAnalysisModule dependencyAnalysisModule;
    private final DependencyGraphBuilder dependencyGraphBuilder;
    private final FeatureFlagsService featureFlags;
    private final adrianmikula.jakartamigration.sourcecodescanning.service.SourceCodeScanner sourceCodeScanner;

    @McpTool(
        name = "analyzeJakartaReadiness",
        description = "Analyzes a Java project for Jakarta migration readiness. Returns a JSON report with readiness score, blockers, and recommendations."
    )
    public String analyzeJakartaReadiness(
            @McpToolParam(description = "Path to the project root directory", required = true) String projectPath) {
        try {
            log.info("Analyzing Jakarta readiness for project: {}", projectPath);

            Path project = Paths.get(projectPath);
            if (!Files.exists(project) || !Files.isDirectory(project)) {
                return createErrorResponse("Project path does not exist or is not a directory: " + projectPath);
            }

            DependencyAnalysisReport report = dependencyAnalysisModule.analyzeProject(project);
            return buildReadinessResponse(report);

        } catch (DependencyGraphException e) {
            log.error("Failed to analyze project: {}", e.getMessage(), e);
            return createErrorResponse("Failed to analyze project: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during analysis", e);
            return createErrorResponse("Unexpected error: " + e.getMessage());
        }
    }

    @McpTool(
        name = "detectBlockers",
        description = "Detects blockers that prevent Jakarta migration. Returns a JSON list of blockers with types, reasons, and mitigation strategies."
    )
    public String detectBlockers(
            @McpToolParam(description = "Path to the project root directory", required = true) String projectPath) {
        try {
            log.info("Detecting blockers for project: {}", projectPath);

            Path project = Paths.get(projectPath);
            if (!Files.exists(project) || !Files.isDirectory(project)) {
                return createErrorResponse("Project path does not exist or is not a directory: " + projectPath);
            }

            DependencyGraph graph = dependencyGraphBuilder.buildFromProject(project);
            List<Blocker> blockers = dependencyAnalysisModule.detectBlockers(graph);
            return buildBlockersResponse(blockers);

        } catch (DependencyGraphException e) {
            log.error("Failed to detect blockers: {}", e.getMessage(), e);
            return createErrorResponse("Failed to detect blockers: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during blocker detection", e);
            return createErrorResponse("Unexpected error: " + e.getMessage());
        }
    }

    @McpTool(
        name = "recommendVersions",
        description = "Recommends Jakarta-compatible versions for project dependencies. Returns a JSON list of version recommendations with migration paths and compatibility scores."
    )
    public String recommendVersions(
            @McpToolParam(description = "Path to the project root directory", required = true) String projectPath) {
        try {
            log.info("Recommending versions for project: {}", projectPath);

            Path project = Paths.get(projectPath);
            if (!Files.exists(project) || !Files.isDirectory(project)) {
                return createErrorResponse("Project path does not exist or is not a directory: " + projectPath);
            }

            DependencyGraph graph = dependencyGraphBuilder.buildFromProject(project);
            List<Artifact> artifacts = graph.getNodes().stream().collect(Collectors.toList());
            List<VersionRecommendation> recommendations = dependencyAnalysisModule.recommendVersions(artifacts);
            return buildRecommendationsResponse(recommendations);

        } catch (DependencyGraphException e) {
            log.error("Failed to recommend versions: {}", e.getMessage(), e);
            return createErrorResponse("Failed to recommend versions: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during version recommendation", e);
            return createErrorResponse("Unexpected error: " + e.getMessage());
        }
    }

    @McpTool(
        name = "createMigrationPlan",
        description = "Creates a comprehensive migration plan for Jakarta migration. Returns a JSON plan with phases, estimated duration, and risk assessment. Requires PREMIUM license."
    )
    public String createMigrationPlan(
            @McpToolParam(description = "Path to the project root directory", required = true) String projectPath) {
        log.info("createMigrationPlan called (free build – upgrade required)");
        return createUpgradeRequiredResponse(
            FeatureFlag.ONE_CLICK_REFACTOR,
            "The 'createMigrationPlan' tool requires a PREMIUM license. This tool creates comprehensive migration plans with detailed phases and risk assessment."
        );
    }

    @McpTool(
        name = "analyzeMigrationImpact",
        description = "Analyzes full migration impact combining dependency analysis and source code scanning. Returns a comprehensive summary with file counts, import counts, blockers, and estimated effort. Requires PREMIUM license."
    )
    public String analyzeMigrationImpact(
            @McpToolParam(description = "Path to the project root directory", required = true) String projectPath) {
        log.info("analyzeMigrationImpact called (free build – upgrade required)");
        return createUpgradeRequiredResponse(
            FeatureFlag.ADVANCED_ANALYSIS,
            "The 'analyzeMigrationImpact' tool requires a PREMIUM license. This tool provides comprehensive migration impact analysis combining dependency analysis and source code scanning."
        );
    }

    @McpTool(
        name = "verifyRuntime",
        description = "Verifies runtime execution of a migrated Jakarta application. Returns a JSON result with execution status, errors, and metrics. Requires PREMIUM license."
    )
    public String verifyRuntime(
            @McpToolParam(description = "Path to the JAR file to execute", required = true) String jarPath,
            @McpToolParam(description = "Optional timeout in seconds (default: 30)", required = false) Integer timeoutSeconds) {
        log.info("verifyRuntime called (free build – upgrade required)");
        return createUpgradeRequiredResponse(
            FeatureFlag.BINARY_FIXES,
            "The 'verifyRuntime' tool requires a PREMIUM license. This tool verifies runtime execution of migrated Jakarta applications."
        );
    }

    private String buildReadinessResponse(DependencyAnalysisReport report) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"status\": \"success\",\n");
        json.append("  \"readinessScore\": ").append(report.readinessScore().score()).append(",\n");
        json.append("  \"readinessMessage\": \"").append(escapeJson(report.readinessScore().explanation())).append("\",\n");
        json.append("  \"totalDependencies\": ").append(report.dependencyGraph().nodeCount()).append(",\n");
        json.append("  \"blockers\": ").append(report.blockers().size()).append(",\n");
        json.append("  \"recommendations\": ").append(report.recommendations().size()).append(",\n");
        json.append("  \"riskScore\": ").append(report.riskAssessment().riskScore()).append(",\n");
        json.append("  \"riskFactors\": ").append(buildStringArray(report.riskAssessment().riskFactors())).append("\n");
        json.append("}");
        return json.toString();
    }

    private String buildBlockersResponse(List<Blocker> blockers) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"status\": \"success\",\n");
        json.append("  \"blockerCount\": ").append(blockers.size()).append(",\n");
        json.append("  \"blockers\": [\n");
        for (int i = 0; i < blockers.size(); i++) {
            Blocker blocker = blockers.get(i);
            json.append("    {\n");
            json.append("      \"artifact\": \"").append(escapeJson(blocker.artifact().toString())).append("\",\n");
            json.append("      \"type\": \"").append(blocker.type()).append("\",\n");
            json.append("      \"reason\": \"").append(escapeJson(blocker.reason())).append("\",\n");
            json.append("      \"confidence\": ").append(blocker.confidence()).append(",\n");
            json.append("      \"mitigationStrategies\": ").append(buildStringArray(blocker.mitigationStrategies())).append("\n");
            json.append("    }");
            if (i < blockers.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("  ]");
        if (!blockers.isEmpty()) {
            json.append(",\n  \"premiumFeatures\": {\n");
            json.append("    \"recommended\": true,\n");
            json.append("    \"message\": \"Premium Auto-Fixes can automatically resolve many blockers without manual intervention.\",\n");
            json.append("    \"features\": [\"Auto-Fixes\", \"Advanced Analysis\", \"Binary Fixes\"],\n");
            json.append("    \"pricingUrl\": \"https://apify.com/adrian_m/jakartamigrationmcp#pricing\"\n  }");
        }
        json.append("\n}");
        return json.toString();
    }

    private String buildRecommendationsResponse(List<VersionRecommendation> recommendations) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"status\": \"success\",\n");
        json.append("  \"recommendationCount\": ").append(recommendations.size()).append(",\n");
        json.append("  \"recommendations\": [\n");
        for (int i = 0; i < recommendations.size(); i++) {
            VersionRecommendation rec = recommendations.get(i);
            json.append("    {\n");
            json.append("      \"current\": \"").append(escapeJson(rec.currentArtifact().toString())).append("\",\n");
            json.append("      \"recommended\": \"").append(escapeJson(rec.recommendedArtifact().toString())).append("\",\n");
            json.append("      \"migrationPath\": \"").append(escapeJson(rec.migrationPath())).append("\",\n");
            json.append("      \"compatibilityScore\": ").append(rec.compatibilityScore()).append(",\n");
            json.append("      \"breakingChanges\": ").append(buildStringArray(rec.breakingChanges())).append("\n");
            json.append("    }");
            if (i < recommendations.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("  ]\n}");
        return json.toString();
    }

    private String createErrorResponse(String message) {
        return "{\n  \"status\": \"error\",\n  \"message\": \"" + escapeJson(message) + "\"\n}";
    }

    private String createUpgradeRequiredResponse(FeatureFlag flag, String message) {
        FeatureFlagsService.UpgradeInfo upgradeInfo = featureFlags.getUpgradeInfo(flag);
        FeatureFlagsProperties.LicenseTier currentTier = featureFlags.getCurrentTier();
        String featureName = upgradeInfo != null ? upgradeInfo.getFeatureName() : flag.getName();
        String featureDescription = upgradeInfo != null ? upgradeInfo.getFeatureDescription() : flag.getDescription();
        FeatureFlagsProperties.LicenseTier requiredTier = upgradeInfo != null ? upgradeInfo.getRequiredTier() : flag.getRequiredTier();
        String paymentLink = upgradeInfo != null ? upgradeInfo.getPaymentLink() : null;
        String upgradeMessage = upgradeInfo != null ? upgradeInfo.getMessage() : message;

        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"status\": \"upgrade_required\",\n");
        json.append("  \"message\": \"").append(escapeJson(message)).append("\",\n");
        json.append("  \"featureName\": \"").append(escapeJson(featureName)).append("\",\n");
        json.append("  \"featureDescription\": \"").append(escapeJson(featureDescription)).append("\",\n");
        json.append("  \"currentTier\": \"").append(currentTier).append("\",\n");
        json.append("  \"requiredTier\": \"").append(requiredTier).append("\",\n");
        if (paymentLink != null && !paymentLink.isBlank()) {
            json.append("  \"paymentLink\": \"").append(escapeJson(paymentLink)).append("\",\n");
        }
        json.append("  \"upgradeMessage\": \"").append(escapeJson(upgradeMessage)).append("\"\n");
        json.append("}");
        return json.toString();
    }

    private String buildStringArray(List<String> list) {
        if (list == null || list.isEmpty()) return "[]";
        return "[" + list.stream().map(s -> "\"" + escapeJson(s) + "\"").collect(Collectors.joining(", ")) + "]";
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}
