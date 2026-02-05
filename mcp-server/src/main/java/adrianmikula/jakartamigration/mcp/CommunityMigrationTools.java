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
import adrianmikula.jakartamigration.dependencyanalysis.service.MavenCentralLookupService;
import adrianmikula.jakartamigration.sourcecodescanning.domain.ReflectionUsage;
import adrianmikula.jakartamigration.sourcecodescanning.service.SourceCodeScanner;
import adrianmikula.jakartamigration.runtimeverification.domain.*;
import adrianmikula.jakartamigration.runtimeverification.service.ErrorAnalyzer;
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
    private final MavenCentralLookupService mavenCentralLookupService;
    private final SourceCodeScanner sourceCodeScanner;
    private final ErrorAnalyzer errorAnalyzer;

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
     * Looks up Jakarta equivalents for a specific javax artifact from Maven Central.
     * 
     * @param groupId The javax group ID (e.g., "javax.servlet")
     * @param artifactId The javax artifact ID (e.g., "javax.servlet-api")
     * @param currentVersion Optional current version (e.g., "4.0.1")
     * @return JSON string containing artifact lookup results
     */
    @McpTool(name = "lookupArtifact", description = "Looks up Jakarta equivalents for a javax artifact from Maven Central. Returns artifact info, latest versions, and migration recommendations.")
    public String lookupArtifact(
            @McpToolParam(description = "The javax group ID (e.g., 'javax.servlet')", required = true) String groupId,
            @McpToolParam(description = "The javax artifact ID (e.g., 'javax.servlet-api')", required = true) String artifactId,
            @McpToolParam(description = "Optional current version to compare against", required = false) String currentVersion) {
        try {
            log.info("Looking up artifact: {}:{} version {}", groupId, artifactId, currentVersion);
            
            // Get Jakarta equivalents
            var equivalents = mavenCentralLookupService.findJakartaEquivalents(groupId, artifactId);
            
            // Get version recommendation
            String version = currentVersion != null ? currentVersion : "latest";
            var recommendation = mavenCentralLookupService.recommendVersion(groupId, artifactId, version);
            
            // Build response
            return buildArtifactLookupResponse(groupId, artifactId, equivalents, recommendation);
            
        } catch (Exception e) {
            log.error("Failed to lookup artifact: {}:{}", groupId, artifactId, e);
            return createErrorResponse("Failed to lookup artifact: " + e.getMessage());
        }
    }

    /**
     * Detects reflection-based javax usage in source code.
     * Finds patterns like Class.forName("javax.servlet.Filter") that regex misses.
     * 
     * @param projectPath Path to the project root directory
     * @return JSON string containing reflection usages found
     */
    @McpTool(name = "detectReflectionUsage", description = "Detects reflection-based javax usage in source code. Finds patterns like Class.forName() that standard import scanning misses. Returns JSON with critical usages that may break migration.")
    public String detectReflectionUsage(
            @McpToolParam(description = "Path to the project root directory", required = true) String projectPath) {
        try {
            log.info("Detecting reflection usage in project: {}", projectPath);
            
            Path project = Paths.get(projectPath);
            if (!Files.exists(project) || !Files.isDirectory(project)) {
                return createErrorResponse("Project path does not exist or is not a directory: " + projectPath);
            }
            
            // Use the scanner's reflection detection
            SourceCodeScannerImpl scanner = new SourceCodeScannerImpl();
            var summary = scanner.scanForReflectionUsages(project);
            
            // Build response
            return buildReflectionDetectionResponse(summary);
            
        } catch (Exception e) {
            log.error("Failed to detect reflection usage: {}", e.getMessage(), e);
            return createErrorResponse("Failed to detect reflection usage: " + e.getMessage());
        }
    }

    /**
     * Scans for XML namespace issues in configuration files.
     * Checks persistence.xml, web.xml, faces-config.xml for javax namespaces.
     * 
     * @param projectPath Path to the project root directory
     * @return JSON string containing XML namespace issues found
     */
    @McpTool(name = "scanXmlNamespaces", description = "Scans XML configuration files for javax namespaces that need updating. Checks persistence.xml, web.xml, faces-config.xml for namespace declarations and class references.")
    public String scanXmlNamespaces(
            @McpToolParam(description = "Path to the project root directory", required = true) String projectPath) {
        try {
            log.info("Scanning XML namespaces in project: {}", projectPath);
            
            Path project = Paths.get(projectPath);
            if (!Files.exists(project) || !Files.isDirectory(project)) {
                return createErrorResponse("Project path does not exist or is not a directory: " + projectPath);
            }
            
            // Scan XML files
            var xmlUsages = sourceCodeScanner.scanXmlFiles(project);
            
            // Build response
            return buildXmlNamespaceResponse(xmlUsages);
            
        } catch (Exception e) {
            log.error("Failed to scan XML namespaces: {}", e.getMessage(), e);
            return createErrorResponse("Failed to scan XML namespaces: " + e.getMessage());
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
    
    // === Artifact Lookup Response Builders ===
    
    private String buildArtifactLookupResponse(String groupId, String artifactId, 
            List<MavenCentralLookupService.ArtifactInfo> equivalents,
            MavenCentralLookupService.VersionRecommendation recommendation) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"status\": \"success\",\n");
        json.append("  \"edition\": \"community\",\n");
        json.append("  \"query\": {\n");
        json.append("    \"groupId\": \"").append(escapeJson(groupId)).append("\",\n");
        json.append("    \"artifactId\": \"").append(escapeJson(artifactId)).append("\"\n");
        json.append("  },\n");
        
        json.append("  \"equivalents\": [\n");
        for (int i = 0; i < equivalents.size(); i++) {
            var eq = equivalents.get(i);
            json.append("    {\n");
            json.append("      \"groupId\": \"").append(escapeJson(eq.groupId())).append("\",\n");
            json.append("      \"artifactId\": \"").append(escapeJson(eq.artifactId())).append("\",\n");
            json.append("      \"version\": \"").append(escapeJson(eq.version())).append("\",\n");
            json.append("      \"knownEquivalent\": ").append(eq.knownEquivalent()).append("\n");
            json.append("    }");
            if (i < equivalents.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("  ],\n");
        
        json.append("  \"recommendation\": {\n");
        json.append("    \"type\": \"").append(recommendation.type()).append("\",\n");
        if (recommendation.jakartaGroupId() != null) {
            json.append("    \"jakartaGroupId\": \"").append(escapeJson(recommendation.jakartaGroupId())).append("\",\n");
            json.append("    \"jakartaArtifactId\": \"").append(escapeJson(recommendation.jakartaArtifactId())).append("\",\n");
            json.append("    \"recommendedVersion\": \"").append(escapeJson(recommendation.recommendedVersion())).append("\",\n");
        }
        json.append("    \"explanation\": \"").append(escapeJson(recommendation explanation())).append("\"\n");
        json.append("  }\n");
        json.append("}");
        
        return json.toString();
    }
    
    // === Reflection Detection Response Builders ===
    
    private String buildReflectionDetectionResponse(SourceCodeScannerImpl.ReflectionSummary summary) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"status\": \"success\",\n");
        json.append("  \"edition\": \"community\",\n");
        json.append("  \"totalFilesScanned\": ").append(summary.totalFiles()).append(",\n");
        json.append("  \"filesWithReflection\": ").append(summary.filesWithReflection()).append(",\n");
        json.append("  \"totalReflectionUsages\": ").append(summary.getTotalReflectionCount()).append(",\n");
        json.append("  \"hasCriticalUsages\": ").append(summary.hasCriticalUsages()).append(",\n");
        json.append("  \"criticalTypes\": ").append(buildStringArray(summary.criticalTypes())).append(",\n");
        
        // Premium upgrade recommendation for critical usages
        if (summary.hasCriticalUsages()) {
            json.append(",\n");
            json.append("  \"premiumFeatures\": {\n");
            json.append("    \"recommended\": true,\n");
            json.append("    \"message\": \"Premium Auto-Fixes can automatically update reflection-based references like Class.forName() to use Jakarta equivalents.\"\n");
            json.append("  }");
        }
        
        json.append("\n}");
        return json.toString();
    }
    
    // === XML Namespace Response Builders ===
    
    private String buildXmlNamespaceResponse(
            List<adrianmikula.jakartamigration.sourcecodescanning.domain.XmlFileUsage> xmlUsages) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"status\": \"success\",\n");
        json.append("  \"edition\": \"community\",\n");
        json.append("  \"filesWithIssues\": ").append(xmlUsages.size()).append(",\n");
        
        int totalNamespaces = xmlUsages.stream()
                .mapToInt(u -> u.namespaceUsages().size())
                .sum();
        int totalClassRefs = xmlUsages.stream()
                .mapToInt(u -> u.classReferences().size())
                .sum();
        
        json.append("  \"totalNamespaceIssues\": ").append(totalNamespaces).append(",\n");
        json.append("  \"totalClassReferenceIssues\": ").append(totalClassRefs).append(",\n");
        
        // Premium upgrade recommendation
        if (totalNamespaces > 0 || totalClassRefs > 0) {
            json.append(",\n");
            json.append("  \"premiumFeatures\": {\n");
            json.append("    \"recommended\": true,\n");
            json.append("    \"message\": \"Premium Auto-Fixes can automatically update XML namespace declarations and class references in persistence.xml, web.xml, and other configuration files.\"\n");
            json.append("  }");
        }
        
        json.append("\n}");
        return json.toString();
    }
    
    // === Runtime Failure Explanation Tool ===
    
    /**
     * Analyzes a runtime failure stack trace and explains the root cause with remediation steps.
     * This tool helps developers understand and fix Jakarta EE migration issues.
     * 
     * @param stackTrace The stack trace from the runtime error
     * @param projectPath Optional path to the project root for context
     * @return JSON string containing error analysis and remediation suggestions
     */
    @McpTool(name = "explainRuntimeFailure", description = "Analyzes a runtime failure stack trace and explains the root cause with remediation steps. Returns error category, contributing factors, and step-by-step fixes.")
    public String explainRuntimeFailure(
            @McpToolParam(description = "The stack trace from the runtime error", required = true) String stackTrace,
            @McpToolParam(description = "Optional path to the project root for additional context", required = false) String projectPath) {
        try {
            log.info("Explaining runtime failure");
            
            // Parse the stack trace into errors
            List<String> lines = List.of(stackTrace.split("\\r?\\n"));
            List<RuntimeError> errors = errorAnalyzer.parseErrorsFromOutput(lines, List.of());
            
            if (errors.isEmpty()) {
                return createErrorResponse("No errors found in the provided stack trace");
            }
            
            // Create migration context
            MigrationContext context = new MigrationContext(
                projectPath != null ? Paths.get(projectPath) : Paths.get("."),
                false, // isPostMigration - assume pre-migration by default
                ""
            );
            
            // Analyze errors
            ErrorAnalysis analysis = errorAnalyzer.analyzeErrors(errors, context);
            
            // Build response
            return buildRuntimeFailureResponse(errors, analysis);
            
        } catch (Exception e) {
            log.error("Failed to explain runtime failure", e);
            return createErrorResponse("Failed to analyze stack trace: " + e.getMessage());
        }
    }
    
    // === Runtime Failure Response Builder ===
    
    private String buildRuntimeFailureResponse(List<RuntimeError> errors, ErrorAnalysis analysis) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"status\": \"success\",\n");
        json.append("  \"edition\": \"community\",\n");
        json.append("  \"errorCount\": ").append(errors.size()).append(",\n");
        json.append("  \"category\": \"").append(analysis.category()).append("\",\n");
        json.append("  \"rootCause\": \"").append(escapeJson(analysis.rootCause())).append("\",\n");
        json.append("  \"confidence\": ").append(analysis.confidence()).append(",\n");
        
        // Contributing factors
        json.append("  \"contributingFactors\": [");
        for (int i = 0; i < analysis.contributingFactors().size(); i++) {
            json.append("\"").append(escapeJson(analysis.contributingFactors().get(i))).append("\"");
            if (i < analysis.contributingFactors().size() - 1) {
                json.append(", ");
            }
        }
        json.append("],\n");
        
        // Remediation steps
        json.append("  \"remediationSteps\": [");
        for (int i = 0; i < analysis.suggestedFixes().size(); i++) {
            RemediationStep step = analysis.suggestedFixes().get(i);
            json.append("    {\n");
            json.append("      \"priority\": ").append(step.priority()).append(",\n");
            json.append("      \"description\": \"").append(escapeJson(step.description())).append("\",\n");
            json.append("      \"action\": \"").append(escapeJson(step.action())).append("\",\n");
            json.append("      \"details\": [");
            for (int j = 0; j < step.details().size(); j++) {
                json.append("\"").append(escapeJson(step.details().get(j))).append("\"");
                if (j < step.details().size() - 1) {
                    json.append(", ");
                }
            }
            json.append("]\n");
            json.append("    }");
            if (i < analysis.suggestedFixes().size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        json.append("  ],\n");
        
        // Premium feature recommendation for complex errors
        if (errors.size() > 1 || analysis.confidence() < 0.8) {
            json.append(",\n");
            json.append("  \"premiumFeatures\": {\n");
            json.append("    \"recommended\": true,\n");
            json.append("    \"message\": \"Premium Auto-Fixes can automatically resolve many runtime failures and verify fixes.\"\n");
            json.append("  }");
        }
        
        json.append("\n}");
        return json.toString();
    }
}
