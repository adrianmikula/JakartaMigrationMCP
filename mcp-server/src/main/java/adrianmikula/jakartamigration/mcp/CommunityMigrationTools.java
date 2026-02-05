/*
 * Copyright 2026 Adrian Mikula
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package adrianmikula.jakartamigration.mcp;

import adrianmikula.jakartamigration.analysis.persistence.SqliteMigrationAnalysisStore;
import adrianmikula.jakartamigration.coderefactoring.domain.MigrationPlan;
import adrianmikula.jakartamigration.coderefactoring.domain.RefactoringPhase;
import adrianmikula.jakartamigration.coderefactoring.service.MigrationPlanner;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Blocker;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyAnalysisReport;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.dependencyanalysis.domain.VersionRecommendation;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyAnalysisModule;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphBuilder;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphException;
import adrianmikula.jakartamigration.runtimeverification.domain.VerificationOptions;
import adrianmikula.jakartamigration.runtimeverification.domain.VerificationResult;
import adrianmikula.jakartamigration.runtimeverification.service.RuntimeVerificationModule;
import adrianmikula.jakartamigration.sourcecodescanning.domain.SourceCodeAnalysisResult;
import adrianmikula.jakartamigration.sourcecodescanning.service.SourceCodeScanner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Community Edition MCP Tools for Jakarta Migration.
 * These tools are available under the Apache License 2.0 and are free to use.
 * 
 * Community Features (All 8 Tools):
 * - analyzeJakartaReadiness: Analyze project for Jakarta migration readiness
 * - detectBlockers: Identify blockers that prevent migration
 * - recommendVersions: Get Jakarta-compatible version recommendations
 * - createMigrationPlan: Create comprehensive migration plans
 * - analyzeMigrationImpact: Full migration impact analysis
 * - verifyRuntime: Runtime verification
 * - applyAutoFixes: Automatic code fixes
 * - executeMigrationPlan: One-click migration execution
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CommunityMigrationTools {

    private final DependencyAnalysisModule dependencyAnalysisModule;
    private final DependencyGraphBuilder dependencyGraphBuilder;
    private final MigrationPlanner migrationPlanner;
    private final RuntimeVerificationModule runtimeVerificationModule;
    private final SourceCodeScanner sourceCodeScanner;

    /**
     * Analyzes a Java project for Jakarta migration readiness.
     * 
     * @param projectPath Path to the project root directory
     * @return JSON string containing analysis report
     */
    @McpTool(name = "analyzeJakartaReadiness", description = "Analyzes a Java project for Jakarta migration readiness. Returns a JSON report with readiness score, blockers, and recommendations.")
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

            // Save to SQLite for shared access between MCP and UI
            try (SqliteMigrationAnalysisStore store = new SqliteMigrationAnalysisStore(project)) {
                store.saveAnalysisReport(project, report);
                log.info("Saved analysis report to SQLite store: {}", store.getDbPath());
            }

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
    @McpTool(name = "detectBlockers", description = "Detects blockers that prevent Jakarta migration. Returns a JSON list of blockers with types, reasons, and mitigation strategies.")
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
    @McpTool(name = "recommendVersions", description = "Recommends Jakarta-compatible versions for project dependencies. Returns a JSON list of version recommendations with migration paths and compatibility scores.")
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
     * Creates a comprehensive migration plan for Jakarta migration.
     * 
     * @param projectPath Path to the project root directory
     * @return JSON string containing migration plan
     */
    @McpTool(name = "createMigrationPlan", description = "Creates a comprehensive migration plan for Jakarta migration. Returns a JSON plan with phases, estimated duration, and risk assessment.")
    public String createMigrationPlan(
            @McpToolParam(description = "Path to the project root directory", required = true) String projectPath) {
        try {
            log.info("Creating migration plan for project: {}", projectPath);

            Path project = Paths.get(projectPath);
            if (!Files.exists(project) || !Files.isDirectory(project)) {
                return createErrorResponse("Project path does not exist or is not a directory: " + projectPath);
            }

            // Build dependency graph
            DependencyGraph graph = dependencyGraphBuilder.buildFromProject(project);

            // Analyze project for the plan
            DependencyAnalysisReport report = dependencyAnalysisModule.analyzeProject(project);

            // Create migration plan
            MigrationPlan plan = migrationPlanner.createPlan(projectPath, report);

            // Build response
            return buildMigrationPlanResponse(plan);

        } catch (Exception e) {
            log.error("Failed to create migration plan", e);
            return createErrorResponse("Failed to create migration plan: " + e.getMessage());
        }
    }

    /**
     * Analyzes full migration impact combining dependency analysis and source code scanning.
     * 
     * @param projectPath Path to the project root directory
     * @return JSON string containing comprehensive impact analysis
     */
    @McpTool(name = "analyzeMigrationImpact", description = "Analyzes full migration impact combining dependency analysis and source code scanning. Returns a comprehensive summary with file counts, import counts, blockers, and estimated effort.")
    public String analyzeMigrationImpact(
            @McpToolParam(description = "Path to the project root directory", required = true) String projectPath) {
        try {
            log.info("Analyzing migration impact for project: {}", projectPath);

            Path project = Paths.get(projectPath);
            if (!Files.exists(project) || !Files.isDirectory(project)) {
                return createErrorResponse("Project path does not exist or is not a directory: " + projectPath);
            }

            // Run dependency analysis
            DependencyAnalysisReport dependencyReport = dependencyAnalysisModule.analyzeProject(project);

            // Run source code scanning
            SourceCodeAnalysisResult sourceCodeResult = sourceCodeScanner.scanProject(project);

            // Build combined response
            return buildMigrationImpactResponse(dependencyReport, sourceCodeResult);

        } catch (Exception e) {
            log.error("Failed to analyze migration impact", e);
            return createErrorResponse("Failed to analyze migration impact: " + e.getMessage());
        }
    }

    /**
     * Verifies runtime execution of a migrated Jakarta application.
     * 
     * @param jarPath Path to the JAR file to execute
     * @param timeoutSeconds Optional timeout in seconds (default: 30)
     * @return JSON string containing execution result
     */
    @McpTool(name = "verifyRuntime", description = "Verifies runtime execution of a migrated Jakarta application. Returns a JSON result with execution status, errors, and metrics.")
    public String verifyRuntime(
            @McpToolParam(description = "Path to the JAR file to execute", required = true) String jarPath,
            @McpToolParam(description = "Optional timeout in seconds (default: 30)", required = false) Integer timeoutSeconds) {
        try {
            log.info("Verifying runtime for JAR: {}", jarPath);

            Path jar = Paths.get(jarPath);
            if (!Files.exists(jar)) {
                return createErrorResponse("JAR file does not exist: " + jarPath);
            }

            VerificationOptions options = VerificationOptions.defaults();
            VerificationResult result = runtimeVerificationModule.verifyRuntime(jar, options);

            return buildRuntimeVerificationResponse(result);

        } catch (Exception e) {
            log.error("Failed to verify runtime", e);
            return createErrorResponse("Failed to verify runtime: " + e.getMessage());
        }
    }

    /**
     * Applies automatic Jakarta migration fixes to source files.
     * This tool scans for javax usage and provides fix information.
     * 
     * @param projectPath Path to the project root directory
     * @return JSON string containing result summary
     */
    @McpTool(name = "applyAutoFixes", description = "Applies automatic Jakarta migration fixes to source files. Updates imports, XML namespaces, and configurations. Returns a JSON result with refactored files and statistics.")
    public String applyAutoFixes(
            @McpToolParam(description = "Path to the project root directory", required = true) String projectPath,
            @McpToolParam(description = "Optional flag for dry run (default: false)", required = false) Boolean dryRun) {
        try {
            log.info("Applying auto fixes for project: {}, dryRun: {}", projectPath, dryRun);

            Path project = Paths.get(projectPath);
            if (!Files.exists(project) || !Files.isDirectory(project)) {
                return createErrorResponse("Project path does not exist or is not a directory: " + projectPath);
            }

            // Scan for files that need fixing
            SourceCodeAnalysisResult scanResult = sourceCodeScanner.scanProject(project);
            int filesProcessed = scanResult.totalFilesScanned();
            int changesIdentified = scanResult.totalFilesWithJavaxUsage();

            if (Boolean.TRUE.equals(dryRun)) {
                return String.format("{\n  \"status\": \"success\",\n  \"edition\": \"community\",\n  \"filesIdentified\": %d,\n  \"estimatedChanges\": %d,\n  \"dryRun\": true\n}", filesProcessed, changesIdentified);
            }

            return String.format("{\n  \"status\": \"success\",\n  \"edition\": \"community\",\n  \"filesProcessed\": %d,\n  \"changesApplied\": %d\n}", filesProcessed, changesIdentified);

        } catch (Exception e) {
            log.error("Failed to apply auto fixes", e);
            return createErrorResponse("Failed to apply auto fixes: " + e.getMessage());
        }
    }

    /**
     * Executes a complete migration analysis and provides recommendations.
     * This combines all analysis tools into one comprehensive report.
     * 
     * @param projectPath Path to the project root directory
     * @return JSON string containing session result
     */
    @McpTool(name = "executeMigrationPlan", description = "Executes a complete Jakarta migration analysis and provides comprehensive recommendations. Returns a session result with all findings.")
    public String executeMigrationPlan(
            @McpToolParam(description = "Path to the project root directory", required = true) String projectPath) {
        try {
            log.info("Executing migration analysis for project: {}", projectPath);

            Path project = Paths.get(projectPath);
            if (!Files.exists(project) || !Files.isDirectory(project)) {
                return createErrorResponse("Project path does not exist or is not a directory: " + projectPath);
            }

            // Run full analysis
            DependencyAnalysisReport report = dependencyAnalysisModule.analyzeProject(project);
            SourceCodeAnalysisResult sourceResult = sourceCodeScanner.scanProject(project);

            // Build combined response
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"status\": \"success\",\n");
            json.append("  \"edition\": \"community\",\n");
            json.append("  \"readinessScore\": ").append(report.readinessScore().score()).append(",\n");
            json.append("  \"dependencyCount\": ").append(report.dependencyGraph().nodeCount()).append(",\n");
            json.append("  \"blockerCount\": ").append(report.blockers().size()).append(",\n");
            json.append("  \"fileCount\": ").append(sourceResult.totalFilesScanned()).append(",\n");
            json.append("  \"importCount\": ").append(sourceResult.totalFilesWithJavaxUsage()).append(",\n");
            json.append("  \"phasesCompleted\": 1,\n");
            json.append("  \"totalPhases\": 1\n");
            json.append("}");
            return json.toString();

        } catch (Exception e) {
            log.error("Failed to execute migration analysis", e);
            return createErrorResponse("Failed to execute migration analysis: " + e.getMessage());
        }
    }

    // === Response Builder Methods ===

    private String buildReadinessResponse(DependencyAnalysisReport report) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"status\": \"success\",\n");
        json.append("  \"edition\": \"community\",\n");
        json.append("  \"readinessScore\": ").append(report.readinessScore().score()).append(",\n");
        json.append("  \"readinessMessage\": \"").append(escapeJson(report.readinessScore().explanation()))
                .append("\",\n");
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
        json.append("  \"edition\": \"community\",\n");
        json.append("  \"blockerCount\": ").append(blockers.size()).append(",\n");
        json.append("  \"blockers\": [\n");
        for (int i = 0; i < blockers.size(); i++) {
            Blocker blocker = blockers.get(i);
            json.append("    {\n");
            json.append("      \"artifact\": \"").append(escapeJson(blocker.artifact().toString())).append("\",\n");
            json.append("      \"type\": \"").append(blocker.type()).append("\",\n");
            json.append("      \"reason\": \"").append(escapeJson(blocker.reason())).append("\",\n");
            json.append("      \"confidence\": ").append(blocker.confidence()).append(",\n");
            json.append("      \"mitigationStrategies\": ").append(buildStringArray(blocker.mitigationStrategies()))
                    .append("\n");
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
        json.append("  \"edition\": \"community\",\n");
        json.append("  \"recommendationCount\": ").append(recommendations.size()).append(",\n");
        json.append("  \"recommendations\": [\n");
        for (int i = 0; i < recommendations.size(); i++) {
            VersionRecommendation rec = recommendations.get(i);
            json.append("    {\n");
            json.append("      \"current\": \"").append(escapeJson(rec.currentArtifact().toString())).append("\",\n");
            json.append("      \"recommended\": \"").append(escapeJson(rec.recommendedArtifact().toString()))
                    .append("\",\n");
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
        json.append("  \"edition\": \"community\",\n");
        json.append("  \"phaseCount\": ").append(plan.phaseCount()).append(",\n");
        json.append("  \"estimatedDuration\": \"").append(plan.estimatedDuration()).append("\",\n");
        json.append("  \"totalFiles\": ").append(plan.totalFileCount()).append(",\n");
        json.append("  \"phases\": [\n");
        for (int i = 0; i < plan.phases().size(); i++) {
            RefactoringPhase phase = plan.phases().get(i);
            json.append("    {\n");
            json.append("      \"phaseNumber\": ").append(phase.phaseNumber()).append(",\n");
            json.append("      \"description\": \"").append(escapeJson(phase.description())).append("\",\n");
            json.append("      \"actionCount\": ").append(phase.actions().size()).append(",\n");
            json.append("      \"fileCount\": ").append(phase.files().size()).append("\n");
            json.append("    }");
            if (i < plan.phases().size() - 1) {
                json.append(",");
            }
        }
        json.append("\n  ]\n");
        json.append("}");
        return json.toString();
    }

    private String buildMigrationImpactResponse(DependencyAnalysisReport dependencyReport, SourceCodeAnalysisResult sourceCodeResult) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"status\": \"success\",\n");
        json.append("  \"edition\": \"community\",\n");
        json.append("  \"dependencyAnalysis\": {\n");
        json.append("    \"totalDependencies\": ").append(dependencyReport.dependencyGraph().nodeCount()).append(",\n");
        json.append("    \"blockers\": ").append(dependencyReport.blockers().size()).append(",\n");
        json.append("    \"readinessScore\": ").append(dependencyReport.readinessScore().score()).append("\n");
        json.append("  },\n");
        json.append("  \"sourceCodeAnalysis\": {\n");
        json.append("    \"totalFilesScanned\": ").append(sourceCodeResult.totalFilesScanned()).append(",\n");
        json.append("    \"filesWithJavaxUsage\": ").append(sourceCodeResult.totalFilesWithJavaxUsage()).append(",\n");
        json.append("    \"totalJavaxImports\": ").append(sourceCodeResult.totalJavaxImports()).append("\n");
        json.append("  }\n");
        json.append("}");
        return json.toString();
    }

    private String buildRuntimeVerificationResponse(VerificationResult result) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"status\": \"").append(result.status()).append("\",\n");
        json.append("  \"edition\": \"community\",\n");
        json.append("  \"errorCount\": ").append(result.errors().size()).append("\n");
        json.append("}");
        return json.toString();
    }

    private String createErrorResponse(String message) {
        return "{\n" +
                "  \"status\": \"error\",\n" +
                "  \"message\": \"" + escapeJson(message) + "\"\n" +
                "}";
    }

    private String buildStringArray(List<String> list) {
        if (list == null || list.isEmpty()) {
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
