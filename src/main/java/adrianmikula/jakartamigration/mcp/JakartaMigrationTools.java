package adrianmikula.jakartamigration.mcp;

import adrianmikula.jakartamigration.coderefactoring.domain.MigrationPlan;
import adrianmikula.jakartamigration.coderefactoring.service.MigrationPlanner;
import adrianmikula.jakartamigration.coderefactoring.service.RecipeLibrary;
import adrianmikula.jakartamigration.dependencyanalysis.domain.*;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyAnalysisModule;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphBuilder;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphException;
import adrianmikula.jakartamigration.config.ApifyBillingService;
import adrianmikula.jakartamigration.config.FeatureFlag;
import adrianmikula.jakartamigration.config.FeatureFlagsService;
import adrianmikula.jakartamigration.runtimeverification.domain.VerificationOptions;
import adrianmikula.jakartamigration.runtimeverification.domain.VerificationResult;
import adrianmikula.jakartamigration.runtimeverification.service.RuntimeVerificationModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// ISSUE FOUND: Spring AI 1.1.2's spring-ai-mcp-annotations module wraps org.springaicommunity:mcp-annotations
// but does NOT re-export the annotation classes in the org.springframework.ai.mcp.annotation package.
// The actual annotations are in the org.springaicommunity.mcp.annotations package.
// This is a mismatch between the documentation and the actual implementation in 1.1.2.
import org.springaicommunity.mcp.annotations.McpTool;
import org.springaicommunity.mcp.annotations.McpToolParam;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MCP Tools for Jakarta Migration.
 * Exposes Jakarta migration functionality as MCP tools that can be called by AI assistants.
 * 
 * Uses Spring AI 1.0.0 MCP Server annotations to expose tools via the MCP protocol.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JakartaMigrationTools {
    
    private final DependencyAnalysisModule dependencyAnalysisModule;
    private final DependencyGraphBuilder dependencyGraphBuilder;
    private final MigrationPlanner migrationPlanner;
    private final RecipeLibrary recipeLibrary;
    private final RuntimeVerificationModule runtimeVerificationModule;
    private final FeatureFlagsService featureFlags;
    private final ApifyBillingService apifyBillingService;
    
    /**
     * Analyzes a Java project for Jakarta migration readiness.
     * 
     * @param projectPath Path to the project root directory
     * @return JSON string containing analysis report
     */
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
            
            // Analyze project dependencies
            DependencyAnalysisReport report = dependencyAnalysisModule.analyzeProject(project);
            
            // Build response
            return buildReadinessResponse(report);
            
        } catch (DependencyGraphException e) {
            log.error("Failed to analyze project: {}", e.getMessage(), e);
            return createErrorResponse("Failed to analyze project: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during analysis", e);
            return createErrorResponse("Unexpected error: " + e.getMessage());
        }
    }
    
    /**
     * Detects blockers that prevent Jakarta migration.
     * 
     * @param projectPath Path to the project root directory
     * @return JSON string containing blockers list
     */
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
            
            // Build dependency graph
            DependencyGraph graph = dependencyGraphBuilder.buildFromProject(project);
            
            // Detect blockers
            List<Blocker> blockers = dependencyAnalysisModule.detectBlockers(graph);
            
            // Build response
            return buildBlockersResponse(blockers);
            
        } catch (DependencyGraphException e) {
            log.error("Failed to detect blockers: {}", e.getMessage(), e);
            return createErrorResponse("Failed to detect blockers: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during blocker detection", e);
            return createErrorResponse("Unexpected error: " + e.getMessage());
        }
    }
    
    /**
     * Recommends Jakarta-compatible versions for dependencies.
     * 
     * @param projectPath Path to the project root directory
     * @return JSON string containing version recommendations
     */
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
            
            // Build dependency graph
            DependencyGraph graph = dependencyGraphBuilder.buildFromProject(project);
            
            // Get all artifacts
            List<Artifact> artifacts = graph.getNodes().stream().collect(Collectors.toList());
            
            // Get recommendations
            List<VersionRecommendation> recommendations = dependencyAnalysisModule.recommendVersions(artifacts);
            
            // Build response
            return buildRecommendationsResponse(recommendations);
            
        } catch (DependencyGraphException e) {
            log.error("Failed to recommend versions: {}", e.getMessage(), e);
            return createErrorResponse("Failed to recommend versions: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during version recommendation", e);
            return createErrorResponse("Unexpected error: " + e.getMessage());
        }
    }
    
    /**
     * Creates a migration plan for Jakarta migration.
     * 
     * @param projectPath Path to the project root directory
     * @return JSON string containing migration plan
     */
    @McpTool(
        name = "createMigrationPlan",
        description = "Creates a comprehensive migration plan for Jakarta migration. Returns a JSON plan with phases, estimated duration, and risk assessment."
    )
    public String createMigrationPlan(
            @McpToolParam(description = "Path to the project root directory", required = true) String projectPath) {
        try {
            log.info("Creating migration plan for project: {}", projectPath);
            
            Path project = Paths.get(projectPath);
            if (!Files.exists(project) || !Files.isDirectory(project)) {
                return createErrorResponse("Project path does not exist or is not a directory: " + projectPath);
            }
            
            // Analyze project first
            DependencyAnalysisReport report = dependencyAnalysisModule.analyzeProject(project);
            
            // Create migration plan
            MigrationPlan plan = migrationPlanner.createPlan(projectPath, report);
            
            // Charge for billable event (premium feature)
            apifyBillingService.chargeEvent("migration-plan-created");
            
            // Build response
            return buildMigrationPlanResponse(plan);
            
        } catch (DependencyGraphException e) {
            log.error("Failed to create migration plan: {}", e.getMessage(), e);
            return createErrorResponse("Failed to create migration plan: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during migration plan creation", e);
            return createErrorResponse("Unexpected error: " + e.getMessage());
        }
    }
    
    /**
     * Verifies runtime execution of a migrated application.
     * 
     * @param jarPath Path to the JAR file to execute
     * @param timeoutSeconds Optional timeout in seconds (default: 30)
     * @return JSON string containing verification result
     */
    @McpTool(
        name = "verifyRuntime",
        description = "Verifies runtime execution of a migrated Jakarta application. Returns a JSON result with execution status, errors, and metrics."
    )
    public String verifyRuntime(
            @McpToolParam(description = "Path to the JAR file to execute", required = true) String jarPath,
            @McpToolParam(description = "Optional timeout in seconds (default: 30)", required = false) Integer timeoutSeconds) {
        try {
            log.info("Verifying runtime for JAR: {}", jarPath);
            
            Path jar = Paths.get(jarPath);
            if (!Files.exists(jar) || !Files.isRegularFile(jar)) {
                return createErrorResponse("JAR file does not exist or is not a file: " + jarPath);
            }
            
            // Create verification options
            VerificationOptions options = timeoutSeconds != null 
                ? new VerificationOptions(
                    java.time.Duration.ofSeconds(timeoutSeconds),
                    VerificationOptions.defaults().maxMemoryBytes(),
                    VerificationOptions.defaults().captureStdout(),
                    VerificationOptions.defaults().captureStderr(),
                    VerificationOptions.defaults().jvmArgs()
                )
                : VerificationOptions.defaults();
            
            // Verify runtime
            VerificationResult result = runtimeVerificationModule.verifyRuntime(jar, options);
            
            // Charge for billable event (premium feature)
            apifyBillingService.chargeEvent("runtime-verification-executed");
            
            // Build response
            return buildVerificationResponse(result);
            
        } catch (Exception e) {
            log.error("Unexpected error during runtime verification", e);
            return createErrorResponse("Unexpected error: " + e.getMessage());
        }
    }
    
    // Helper methods to build JSON responses
    
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
            if (i < blockers.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        json.append("  ]\n");
        json.append("}");
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
            if (i < recommendations.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        json.append("  ]\n");
        json.append("}");
        return json.toString();
    }
    
    private String buildMigrationPlanResponse(MigrationPlan plan) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"status\": \"success\",\n");
        json.append("  \"phaseCount\": ").append(plan.phases().size()).append(",\n");
        json.append("  \"estimatedDuration\": \"").append(plan.estimatedDuration().toMinutes()).append(" minutes\",\n");
        json.append("  \"riskScore\": ").append(plan.overallRisk().riskScore()).append(",\n");
        json.append("  \"riskFactors\": ").append(buildStringArray(plan.overallRisk().riskFactors())).append(",\n");
        json.append("  \"prerequisites\": ").append(buildStringArray(plan.prerequisites())).append(",\n");
        json.append("  \"phases\": [\n");
        for (int i = 0; i < plan.phases().size(); i++) {
            var phase = plan.phases().get(i);
            json.append("    {\n");
            json.append("      \"number\": ").append(phase.phaseNumber()).append(",\n");
            json.append("      \"description\": \"").append(escapeJson(phase.description())).append("\",\n");
            json.append("      \"fileCount\": ").append(phase.files().size()).append(",\n");
            json.append("      \"estimatedDuration\": \"").append(phase.estimatedDuration().toMinutes()).append(" minutes\"\n");
            json.append("    }");
            if (i < plan.phases().size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        json.append("  ]\n");
        json.append("}");
        return json.toString();
    }
    
    private String buildVerificationResponse(VerificationResult result) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"status\": \"").append(result.status()).append("\",\n");
        json.append("  \"errorCount\": ").append(result.errors().size()).append(",\n");
        json.append("  \"warningCount\": ").append(result.warnings().size()).append(",\n");
        json.append("  \"executionTime\": \"").append(result.metrics().executionTime().toSeconds()).append(" seconds\",\n");
        json.append("  \"exitCode\": ").append(result.metrics().exitCode()).append(",\n");
        if (result.errors().isEmpty()) {
            json.append("  \"message\": \"Runtime verification passed\"\n");
        } else {
            json.append("  \"message\": \"Runtime verification found issues\",\n");
            json.append("  \"errors\": [\n");
            for (int i = 0; i < Math.min(result.errors().size(), 5); i++) {
                var error = result.errors().get(i);
                json.append("    \"").append(escapeJson(error.message())).append("\"");
                if (i < Math.min(result.errors().size(), 5) - 1) {
                    json.append(",");
                }
                json.append("\n");
            }
            json.append("  ]\n");
        }
        json.append("}");
        return json.toString();
    }
    
    private String createErrorResponse(String message) {
        return "{\n" +
               "  \"status\": \"error\",\n" +
               "  \"message\": \"" + escapeJson(message) + "\"\n" +
               "}";
    }
    
    private String createUpgradeRequiredResponse(FeatureFlag flag, String message) {
        String upgradeMessage = featureFlags.getUpgradeMessage(flag);
        return "{\n" +
               "  \"status\": \"upgrade_required\",\n" +
               "  \"message\": \"" + escapeJson(message) + "\",\n" +
               "  \"upgradeMessage\": \"" + escapeJson(upgradeMessage) + "\",\n" +
               "  \"requiredTier\": \"" + flag.getRequiredTier() + "\",\n" +
               "  \"currentTier\": \"" + featureFlags.getCurrentTier() + "\"\n" +
               "}";
    }
    
    private String buildStringArray(List<String> list) {
        if (list.isEmpty()) {
            return "[]";
        }
        return "[" + list.stream()
            .map(s -> "\"" + escapeJson(s) + "\"")
            .collect(Collectors.joining(", ")) + "]";
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

