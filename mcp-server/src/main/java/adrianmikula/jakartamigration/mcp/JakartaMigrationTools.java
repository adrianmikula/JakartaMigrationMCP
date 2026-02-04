package adrianmikula.jakartamigration.mcp;

import adrianmikula.jakartamigration.analysis.persistence.SqliteMigrationAnalysisStore;
import adrianmikula.jakartamigration.coderefactoring.domain.MigrationPlan;
import adrianmikula.jakartamigration.coderefactoring.domain.RefactoringResult;
import adrianmikula.jakartamigration.coderefactoring.service.CodeRefactoringModule;
import adrianmikula.jakartamigration.coderefactoring.service.RecipeLibrary;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyAnalysisReport;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyAnalysisModule;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphException;
import adrianmikula.jakartamigration.config.FeatureFlag;
import adrianmikula.jakartamigration.config.FeatureFlagsProperties;
import adrianmikula.jakartamigration.config.FeatureFlagsService;
import adrianmikula.jakartamigration.runtimeverification.domain.VerificationOptions;
import adrianmikula.jakartamigration.runtimeverification.domain.VerificationResult;
import adrianmikula.jakartamigration.runtimeverification.service.RuntimeVerificationModule;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MCP Tools for Jakarta Migration.
 * 
 * This class combines both Community and Premium tools.
 * Community tools are delegated to {@link CommunityMigrationTools}.
 * Premium tools require a JetBrains Marketplace license.
 * 
 * Uses Spring AI 1.0.0 MCP Server annotations to expose tools via the MCP
 * protocol.
 * 
 * License Tiers:
 * - COMMUNITY (Free): analyzeJakartaReadiness, detectBlockers, recommendVersions
 * - PREMIUM ($49/mo or $399/yr): All tools including auto-fixes, one-click refactor
 */
@Component
@Slf4j
public class JakartaMigrationTools {

    // Community tools delegate
    private final CommunityMigrationTools communityTools;
    
    // Premium tools dependencies
    private final DependencyAnalysisModule dependencyAnalysisModule;
    private final RuntimeVerificationModule runtimeVerificationModule;
    private final RecipeLibrary recipeLibrary;
    private final FeatureFlagsService featureFlags;
    private final adrianmikula.jakartamigration.sourcecodescanning.service.SourceCodeScanner sourceCodeScanner;
    private final CodeRefactoringModule codeRefactoringModule;

    public JakartaMigrationTools(
            CommunityMigrationTools communityTools,
            DependencyAnalysisModule dependencyAnalysisModule,
            RuntimeVerificationModule runtimeVerificationModule,
            RecipeLibrary recipeLibrary,
            FeatureFlagsService featureFlags,
            adrianmikula.jakartamigration.sourcecodescanning.service.SourceCodeScanner sourceCodeScanner,
            CodeRefactoringModule codeRefactoringModule) {
        this.communityTools = communityTools;
        this.dependencyAnalysisModule = dependencyAnalysisModule;
        this.runtimeVerificationModule = runtimeVerificationModule;
        this.recipeLibrary = recipeLibrary;
        this.featureFlags = featureFlags;
        this.sourceCodeScanner = sourceCodeScanner;
        this.codeRefactoringModule = codeRefactoringModule;
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
     * PREMIUM TOOL - Requires JetBrains Marketplace subscription ($49/mo or $399/yr)
     */
    @McpTool(name = "createMigrationPlan", description = "Creates a comprehensive migration plan for Jakarta migration. Returns a JSON plan with phases, estimated duration, and risk assessment. Requires PREMIUM license.")
    public String createMigrationPlan(
            @McpToolParam(description = "Path to the project root directory", required = true) String projectPath) {
        // Check premium license
        if (!featureFlags.hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM)) {
            return createUpgradeRequiredResponse(
                    FeatureFlag.ONE_CLICK_REFACTOR,
                    "The 'createMigrationPlan' tool requires a PREMIUM license. This tool creates comprehensive migration plans with detailed phases and risk assessment.");
        }

        try {
            log.info("Creating migration plan for project: {}", projectPath);

            Path project = Paths.get(projectPath);
            if (!Files.exists(project) || !Files.isDirectory(project)) {
                return createErrorResponse("Project path does not exist or is not a directory: " + projectPath);
            }

            // Analyze project first
            DependencyAnalysisReport report = dependencyAnalysisModule.analyzeProject(project);

            // Create migration plan
            MigrationPlan plan = codeRefactoringModule.createMigrationPlan(projectPath, report);

            // Save to SQLite for shared access between MCP and UI
            try (SqliteMigrationAnalysisStore store = new SqliteMigrationAnalysisStore(project)) {
                store.saveAnalysisReport(project, report);
                store.saveMigrationPlan(project, plan);
                log.info("Saved migration plan to SQLite store: {}", store.getDbPath());
            }

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
     * Analyzes full migration impact combining dependency analysis and source code scanning.
     * PREMIUM TOOL - Requires JetBrains Marketplace subscription
     */
    @McpTool(name = "analyzeMigrationImpact", description = "Analyzes full migration impact combining dependency analysis and source code scanning. Returns a comprehensive summary with file counts, import counts, blockers, and estimated effort. Requires PREMIUM license.")
    public String analyzeMigrationImpact(
            @McpToolParam(description = "Path to the project root directory", required = true) String projectPath) {
        // Check premium license
        if (!featureFlags.hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM)) {
            return createUpgradeRequiredResponse(
                    FeatureFlag.ADVANCED_ANALYSIS,
                    "The 'analyzeMigrationImpact' tool requires a PREMIUM license. This tool provides comprehensive migration impact analysis combining dependency analysis and source code scanning.");
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
            adrianmikula.jakartamigration.sourcecodescanning.domain.SourceCodeAnalysisResult scanResult = sourceCodeScanner
                    .scanProject(project);

            // Create impact summary
            adrianmikula.jakartamigration.coderefactoring.domain.MigrationImpactSummary summary = adrianmikula.jakartamigration.coderefactoring.domain.MigrationImpactSummary
                    .from(depReport, scanResult);

            // Build response
            return buildImpactSummaryResponse(summary);

        } catch (Exception e) {
            log.error("Unexpected error during migration impact analysis", e);
            return createErrorResponse("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Verifies runtime execution of a migrated application.
     * PREMIUM TOOL - Requires JetBrains Marketplace subscription
     */
    @McpTool(name = "verifyRuntime", description = "Verifies runtime execution of a migrated Jakarta application. Returns a JSON result with execution status, errors, and metrics. Requires PREMIUM license.")
    public String verifyRuntime(
            @McpToolParam(description = "Path to the JAR file to execute", required = true) String jarPath,
            @McpToolParam(description = "Optional timeout in seconds (default: 30)", required = false) Integer timeoutSeconds) {
        // Check premium license
        if (!featureFlags.hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM)) {
            return createUpgradeRequiredResponse(
                    FeatureFlag.BINARY_FIXES,
                    "The 'verifyRuntime' tool requires a PREMIUM license. This tool verifies runtime execution of migrated Jakarta applications.");
        }

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
                            VerificationOptions.defaults().jvmArgs())
                    : VerificationOptions.defaults();

            // Verify runtime
            VerificationResult result = runtimeVerificationModule.verifyRuntime(jar, options);

            // Build response
            return buildVerificationResponse(result);

        } catch (Exception e) {
            log.error("Unexpected error during runtime verification", e);
            return createErrorResponse("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Applies automatic fixes to Jakarta migration issues.
     * PREMIUM TOOL - Requires JetBrains Marketplace subscription
     */
    @McpTool(name = "applyAutoFixes", description = "Applies automatic Jakarta migration fixes to source files. Updates imports, XML namespaces, and configurations. Returns a JSON result with refactored files and statistics. Requires PREMIUM license.")
    public String applyAutoFixes(
            @McpToolParam(description = "Path to the project root directory", required = true) String projectPath,
            @McpToolParam(description = "Optional list of relative file paths to fix", required = false) List<String> files,
            @McpToolParam(description = "Optional flag for dry run (default: false)", required = false) Boolean dryRun) {
        // Check premium license
        if (!featureFlags.hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM)) {
            return createUpgradeRequiredResponse(
                    FeatureFlag.AUTO_FIXES,
                    "The 'applyAutoFixes' tool requires a PREMIUM license. This tool automatically applies Jakarta migration fixes to your source code.");
        }

        try {
            log.info("Applying auto-fixes for project: {}", projectPath);

            Path project = Paths.get(projectPath);
            if (!Files.exists(project) || !Files.isDirectory(project)) {
                return createErrorResponse("Project path does not exist or is not a directory: " + projectPath);
            }

            // If no files specified, find all Java and XML files
            List<String> filesToFix = files;
            if (filesToFix == null || filesToFix.isEmpty()) {
                filesToFix = findFilesToMigrate(project);
            }

            // Get all Jakarta recipes
            List<adrianmikula.jakartamigration.coderefactoring.domain.Recipe> recipes = recipeLibrary.getJakartaRecipes();

            // Create refactoring options
            adrianmikula.jakartamigration.coderefactoring.domain.RefactoringOptions options = new adrianmikula.jakartamigration.coderefactoring.domain.RefactoringOptions(
                    project,
                    true, // createCheckpoints
                    true, // validateAfterRefactoring
                    dryRun != null ? dryRun : false,
                    List.of(), // excludedFiles
                    3 // maxRetries
            );

            // Run refactoring
            RefactoringResult result = codeRefactoringModule
                    .refactorBatch(filesToFix, recipes, options);

            // Build response
            return buildRefactoringResponse(result);

        } catch (Exception e) {
            log.error("Unexpected error during auto-fixes application", e);
            return createErrorResponse("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Executes a comprehensive Jakarta migration plan.
     * PREMIUM TOOL - Requires JetBrains Marketplace subscription
     */
    @McpTool(name = "executeMigrationPlan", description = "Executes a complete Jakarta migration plan in phases. Applies all refactoring recipes according to the plan. Returns a JSON session result. Requires PREMIUM license.")
    public String executeMigrationPlan(
            @McpToolParam(description = "Path to the project root directory", required = true) String projectPath) {
        // Check premium license
        if (!featureFlags.hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM)) {
            return createUpgradeRequiredResponse(
                    FeatureFlag.ONE_CLICK_REFACTOR,
                    "The 'executeMigrationPlan' tool requires a PREMIUM license. This tool executes your complete migration plan automatically.");
        }

        try {
            log.info("Executing migration plan for project: {}", projectPath);

            Path project = Paths.get(projectPath);
            if (!Files.exists(project) || !Files.isDirectory(project)) {
                return createErrorResponse("Project path does not exist or is not a directory: " + projectPath);
            }

            // 1. Analyze project
            DependencyAnalysisReport report = dependencyAnalysisModule.analyzeProject(project);

            // 2. Create/Get plan
            MigrationPlan plan = codeRefactoringModule.createMigrationPlan(projectPath, report);

            // 3. Execute phases
            List<RefactoringResult> results = new ArrayList<>();
            for (var phase : plan.phases()) {
                log.info("Executing migration phase {}: {}", phase.phaseNumber(), phase.description());

                // Get recipes for this phase
                List<adrianmikula.jakartamigration.coderefactoring.domain.Recipe> recipes = recipeLibrary.getJakartaRecipes();

                adrianmikula.jakartamigration.coderefactoring.domain.RefactoringOptions options = new adrianmikula.jakartamigration.coderefactoring.domain.RefactoringOptions(
                        project,
                        true, // createCheckpoints
                        true, // validateAfterRefactoring
                        false, // dryRun
                        List.of(), // excludedFiles
                        3 // maxRetries
                );

                RefactoringResult result = codeRefactoringModule
                        .refactorBatch(phase.files(), recipes, options);
                results.add(result);

                if (result.statistics().failedFiles() > 0) {
                    log.warn("Phase {} had {} failures", phase.phaseNumber(), result.statistics().failedFiles());
                }
            }

            // Build response
            return buildExecutionResponse(results, plan);

        } catch (Exception e) {
            log.error("Unexpected error during migration plan execution", e);
            return createErrorResponse("Unexpected error: " + e.getMessage());
        }
    }

    // === Response Builder Methods ===

    private String buildExecutionResponse(
            List<RefactoringResult> results, MigrationPlan plan) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"status\": \"success\",\n");
        json.append("  \"edition\": \"premium\",\n");
        json.append("  \"totalPhases\": ").append(plan.phases().size()).append(",\n");
        json.append("  \"executedPhases\": ").append(results.size()).append(",\n");

        int totalRefactored = results.stream().mapToInt(r -> r.statistics().successfulFiles()).sum();
        int totalFailures = results.stream().mapToInt(r -> r.statistics().failedFiles()).sum();

        json.append("  \"totalRefactoredFiles\": ").append(totalRefactored).append(",\n");
        json.append("  \"totalFailures\": ").append(totalFailures).append(",\n");
        json.append("  \"message\": \"Migration plan execution completed with ").append(totalFailures)
                .append(" failures.\"\n");
        json.append("}");
        return json.toString();
    }

    private List<String> findFilesToMigrate(Path projectPath) throws IOException {
        try (java.util.stream.Stream<Path> stream = Files.walk(projectPath)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String s = p.toString();
                        return s.endsWith(".java") || s.endsWith(".xml") || s.endsWith(".properties");
                    })
                    .map(p -> projectPath.relativize(p).toString())
                    .collect(Collectors.toList());
        }
    }

    private String buildRefactoringResponse(RefactoringResult result) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"status\": \"success\",\n");
        json.append("  \"edition\": \"premium\",\n");
        json.append("  \"refactoredFiles\": ").append(buildStringArray(result.refactoredFiles())).append(",\n");
        json.append("  \"failureCount\": ").append(result.failures().size()).append(",\n");
        if (!result.failures().isEmpty()) {
            json.append("  \"failures\": [\n");
            for (int i = 0; i < result.failures().size(); i++) {
                var failure = result.failures().get(i);
                json.append("    {\n");
                json.append("      \"filePath\": \"").append(escapeJson(failure.filePath())).append("\",\n");
                json.append("      \"errorType\": \"").append(failure.errorType()).append("\",\n");
                json.append("      \"message\": \"").append(escapeJson(failure.errorMessage())).append("\"\n");
                json.append("    }");
                if (i < result.failures().size() - 1) {
                    json.append(",");
                }
                json.append("\n");
            }
            json.append("  ],\n");
        }
        json.append("  \"statistics\": {\n");
        json.append("    \"totalProcessed\": ").append(result.statistics().totalFiles()).append(",\n");
        json.append("    \"successCount\": ").append(result.statistics().successfulFiles()).append(",\n");
        json.append("    \"failureCount\": ").append(result.statistics().failedFiles()).append("\n");
        json.append("  },\n");
        json.append("  \"checkpointId\": \"").append(result.checkpointId()).append("\"\n");
        json.append("}");
        return json.toString();
    }

    private String buildMigrationPlanResponse(MigrationPlan plan) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"status\": \"success\",\n");
        json.append("  \"edition\": \"premium\",\n");
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
            json.append("      \"estimatedDuration\": \"").append(phase.estimatedDuration().toMinutes())
                    .append(" minutes\"\n");
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
        json.append("  \"edition\": \"premium\",\n");
        json.append("  \"errorCount\": ").append(result.errors().size()).append(",\n");
        json.append("  \"warningCount\": ").append(result.warnings().size()).append(",\n");
        json.append("  \"executionTime\": \"").append(result.metrics().executionTime().toSeconds())
                .append(" seconds\",\n");
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

    private String buildImpactSummaryResponse(
            adrianmikula.jakartamigration.coderefactoring.domain.MigrationImpactSummary summary) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"status\": \"success\",\n");
        json.append("  \"edition\": \"premium\",\n");
        json.append("  \"totalFilesToMigrate\": ").append(summary.totalFilesToMigrate()).append(",\n");
        json.append("  \"totalJavaxImports\": ").append(summary.totalJavaxImports()).append(",\n");
        json.append("  \"totalBlockers\": ").append(summary.totalBlockers()).append(",\n");
        json.append("  \"totalRecommendations\": ").append(summary.totalRecommendations()).append(",\n");
        json.append("  \"estimatedEffortMinutes\": ").append(summary.estimatedEffort().toMinutes()).append(",\n");
        json.append("  \"complexity\": \"").append(summary.complexity()).append("\",\n");
        json.append("  \"riskScore\": ").append(summary.overallRisk().riskScore()).append(",\n");
        json.append("  \"riskFactors\": ").append(buildStringArray(summary.overallRisk().riskFactors())).append(",\n");
        json.append("  \"readinessScore\": ").append(summary.dependencyAnalysis().readinessScore().score())
                .append(",\n");
        json.append("  \"readinessMessage\": \"")
                .append(escapeJson(summary.dependencyAnalysis().readinessScore().explanation())).append("\",\n");
        json.append("  \"totalFilesScanned\": ").append(summary.sourceCodeAnalysis().totalFilesScanned()).append(",\n");
        json.append("  \"totalDependencies\": ").append(summary.dependencyAnalysis().dependencyGraph().nodeCount());
        json.append("\n}");
        return json.toString();
    }

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
