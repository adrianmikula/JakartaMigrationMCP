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
import adrianmikula.jakartamigration.dependencyanalysis.domain.*;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyAnalysisModule;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphBuilder;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphException;
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
 * Community Edition MCP Tools for Jakarta Migration.
 * These tools are available under the Apache License 2.0 and are free to use.
 * 
 * Community Features:
 * - analyzeJakartaReadiness: Analyze project for Jakarta migration readiness
 * - detectBlockers: Identify blockers that prevent migration
 * - recommendVersions: Get Jakarta-compatible version recommendations
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CommunityMigrationTools {

    private final DependencyAnalysisModule dependencyAnalysisModule;
    private final DependencyGraphBuilder dependencyGraphBuilder;

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

        // Add premium feature recommendation if blockers found
        if (blockers.size() > 0) {
            json.append(",\n");
            json.append("  \"premiumFeatures\": {\n");
            json.append("    \"recommended\": true,\n");
            json.append("    \"message\": \"Premium Auto-Fixes can automatically resolve many blockers without manual intervention.\",\n");
            json.append("    \"features\": [\n");
            json.append("      \"Auto-Fixes - Automatically fix detected blockers\",\n");
            json.append("      \"Advanced Analysis - Deep transitive conflict detection and resolution\",\n");
            json.append("      \"Binary Fixes - Fix issues in compiled binaries and JAR files\"\n");
            json.append("    ]\n");
            json.append("  }");
        }

        json.append("\n}");
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

        // Add premium feature recommendation if many recommendations or complex migrations
        boolean hasBreakingChanges = recommendations.stream()
                .anyMatch(rec -> !rec.breakingChanges().isEmpty());
        boolean shouldRecommend = recommendations.size() > 5 || hasBreakingChanges;

        if (shouldRecommend) {
            json.append(",\n");
            json.append("  \"premiumFeatures\": {\n");
            json.append("    \"recommended\": true,\n");
            json.append("    \"message\": \"Premium Auto-Fixes can automatically apply these ")
                    .append(recommendations.size())
                    .append(" version recommendations and handle breaking changes.\"\n");
            json.append("  }");
        }

        json.append("\n}");
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
