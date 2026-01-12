package adrianmikula.jakartamigration.mcp;

import adrianmikula.jakartamigration.dependencyanalysis.domain.*;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyAnalysisModule;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphBuilder;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphException;
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
import java.util.List;
import java.util.stream.Collectors;

/**
 * Jakarta Migration MCP Tools - FREE VERSION
 * 
 * This class provides free analysis tools only.
 * Premium features (refactoring, verification, planning) are available
 * in @jakarta-migration/mcp-server-premium package.
 * 
 * FREE Tools:
 * - analyzeJakartaReadiness - Analyzes project for Jakarta migration readiness
 * - detectBlockers - Detects blockers preventing Jakarta migration
 * - recommendVersions - Recommends Jakarta-compatible dependency versions
 * - analyzeMigrationImpact - Full migration impact analysis
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JakartaMigrationTools {
    
    private final DependencyAnalysisModule dependencyAnalysisModule;
    private final DependencyGraphBuilder dependencyGraphBuilder;
    private final SourceCodeScanner sourceCodeScanner;
    
    /**
     * Analyzes a Java project for Jakarta migration readiness.
     * 
     * @param projectPath Path to the project root directory
     * @return JSON string containing analysis report
     */
    @McpTool(
        name = "analyzeJakartaReadiness",
        description = "Analyzes a Java project for Jakarta migration readiness. Returns a JSON report with readiness score, blockers, and recommendations. FREE tool - analysis only."
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
        description = "Detects blockers that prevent Jakarta migration. Returns a JSON list of blockers with types, reasons, and mitigation strategies. FREE tool - analysis only."
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
        description = "Recommends Jakarta-compatible versions for project dependencies. Returns a JSON list of version recommendations with migration paths and compatibility scores. FREE tool - analysis only."
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
     * Analyzes full migration impact combining dependency analysis and source code scanning.
     * 
     * @param projectPath Path to the project root directory
     * @return JSON string containing comprehensive migration impact summary
     */
    @McpTool(
        name = "analyzeMigrationImpact",
        description = "Analyzes full migration impact combining dependency analysis and source code scanning. Returns a comprehensive summary with file counts, import counts, blockers, and estimated effort. FREE tool - analysis only."
    )
    public String analyzeMigrationImpact(
            @McpToolParam(description = "Path to the project root directory", required = true) String projectPath) {
        try {
            log.info("Analyzing migration impact for project: {}", projectPath);
            
            Path project = Paths.get(projectPath);
            if (!Files.exists(project) || !Files.isDirectory(project)) {
                return createErrorResponse("Project path does not exist or is not a directory: " + projectPath);
            }
            
            // Run dependency analysis
            DependencyAnalysisReport depReport = dependencyAnalysisModule.analyzeProject(project);
            
            // Run source code scan
            SourceCodeAnalysisResult scanResult = sourceCodeScanner.scanProject(project);
            
            // Build response (simplified version without MigrationImpactSummary from premium package)
            return buildImpactSummaryResponse(depReport, scanResult);
            
        } catch (Exception e) {
            log.error("Unexpected error during migration impact analysis", e);
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
    
    private String buildImpactSummaryResponse(DependencyAnalysisReport depReport, SourceCodeAnalysisResult scanResult) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"status\": \"success\",\n");
        json.append("  \"totalFilesScanned\": ").append(scanResult.totalFilesScanned()).append(",\n");
        json.append("  \"totalFilesWithJavaxUsage\": ").append(scanResult.totalFilesWithJavaxUsage()).append(",\n");
        json.append("  \"totalJavaxImports\": ").append(scanResult.totalJavaxImports()).append(",\n");
        json.append("  \"totalDependencies\": ").append(depReport.dependencyGraph().nodeCount()).append(",\n");
        json.append("  \"totalBlockers\": ").append(depReport.blockers().size()).append(",\n");
        json.append("  \"totalRecommendations\": ").append(depReport.recommendations().size()).append(",\n");
        json.append("  \"readinessScore\": ").append(depReport.readinessScore().score()).append(",\n");
        json.append("  \"readinessMessage\": \"").append(escapeJson(depReport.readinessScore().explanation())).append("\",\n");
        json.append("  \"riskScore\": ").append(depReport.riskAssessment().riskScore()).append(",\n");
        json.append("  \"riskFactors\": ").append(buildStringArray(depReport.riskAssessment().riskFactors())).append("\n");
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
