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

import adrianmikula.jakartamigration.dependencyanalysis.domain.*;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyAnalysisModule;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphBuilder;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphException;
import lombok.RequiredArgsConstructor;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import adrianmikula.jakartamigration.mcp.util.JsonUtils;

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
@RequiredArgsConstructor
public class CommunityMigrationTools {

    private static final Logger log = LoggerFactory.getLogger(CommunityMigrationTools.class);

    private final DependencyAnalysisModule dependencyAnalysisModule;
    private final DependencyGraphBuilder dependencyGraphBuilder;

    /**
     * Scans for Jakarta EE usage with basic analysis capabilities.
     * COMMUNITY TOOL - Free to use under Apache License 2.0
     * @deprecated Community scanning is deprecated. Use the premium scan endpoint.
     */
    @Deprecated
    @McpTool(name = "scanForJavaxBasic", description = "Performs basic Jakarta EE usage scanning with source code, dependencies, and configuration file analysis. Returns findings with migration recommendations.")
    public String scanForJavaxBasic(
            @McpToolParam(description = "Path to project root directory", required = true) String projectPath,
            @McpToolParam(description = "Scan types to run (source, dependencies, config)", required = false) String scanTypes) {
        return createUnavailableResponse(projectPath);
    }

    /**
     * Analyzes a Java project for Jakarta migration readiness.
     * COMMUNITY TOOL - Free to use under Apache License 2.0
     * @deprecated Community analysis is deprecated. Use the premium scan endpoint.
     */
    @Deprecated
    @McpTool(name = "analyzeJakartaReadiness", description = "Analyzes a Java project for Jakarta migration readiness. Returns a JSON report with readiness score, blockers, and recommendations.")
    public String analyzeJakartaReadiness(
            @McpToolParam(description = "Path to project root directory", required = true) String projectPath) {
        return createUnavailableResponse(projectPath);
    }

    /**
     * Detects blockers that prevent Jakarta migration.
     * 
     * @param projectPath Path to project root directory
     * @return JSON string containing blockers list
     * @deprecated Community blocker detection is deprecated. Use the premium scan endpoint.
     */
    @Deprecated
    @McpTool(name = "detectBlockers", description = "Detects blockers that prevent Jakarta migration. Returns a JSON list of blockers with types, reasons, and mitigation strategies.")
    public String detectBlockers(
            @McpToolParam(description = "Path to project root directory", required = true) String projectPath) {
        return createUnavailableResponse(projectPath);
    }

    /**
     * Recommends Jakarta-compatible versions for project dependencies.
     * 
     * @param projectPath Path to project root directory
     * @return JSON string containing version recommendations
     * @deprecated Community version recommendation is deprecated. Use the premium scan endpoint.
     */
    @Deprecated
    @McpTool(name = "recommendVersions", description = "Recommends Jakarta-compatible versions for project dependencies. Returns a JSON list of version recommendations with migration paths and compatibility scores.")
    public String recommendVersions(
            @McpToolParam(description = "Path to project root directory", required = true) String projectPath) {
        return createUnavailableResponse(projectPath);
    }

    // === Response Builder Methods ===

    private String buildReadinessResponse(DependencyAnalysisReport report) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"status\": \"success\",\n");
        json.append("  \"edition\": \"community\",\n");
        json.append("  \"readinessScore\": ").append(report.readinessScore().score()).append(",\n");
        json.append("  \"readinessMessage\": \"").append(JsonUtils.escapeJson(report.readinessScore().explanation())).append("\",\n");
        json.append("  \"totalDependencies\": ").append(report.dependencyGraph().nodeCount()).append(",\n");
        json.append("  \"blockers\": ").append(report.blockers().size()).append(",\n");
        json.append("  \"recommendations\": ").append(report.recommendations().size()).append(",\n");
        json.append("  \"riskScore\": ").append(report.riskAssessment().riskScore()).append(",\n");
        json.append("  \"riskFactors\": ").append(JsonUtils.buildStringArray(report.riskAssessment().riskFactors())).append("\n");
        json.append("}");
        return json.toString();
    }

    private String createUnavailableResponse(String projectPath) {
        return JsonUtils.createErrorResponse(
                "Community scanning tools are deprecated. " +
                        "Please use the Premium scan endpoint for source-first Jakarta migration scans. " +
                        "Requested project: " + projectPath);
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
            json.append("      \"artifact\": \"").append(JsonUtils.escapeJson(blocker.artifact().toString())).append("\",\n");
            json.append("      \"type\": \"").append(blocker.type()).append("\",\n");
            json.append("      \"reason\": \"").append(JsonUtils.escapeJson(blocker.reason())).append("\",\n");
            json.append("      \"confidence\": ").append(blocker.confidence()).append(",\n");
            json.append("      \"mitigationStrategies\": ").append(JsonUtils.buildStringArray(blocker.mitigationStrategies()))
                    .append("\n");
            json.append("    }");
            if (i < blockers.size() - 1) {
                json.append(",");
            }
        }
        json.append("  ]\n");
        json.append("}");
        return json.toString();
    }

}
